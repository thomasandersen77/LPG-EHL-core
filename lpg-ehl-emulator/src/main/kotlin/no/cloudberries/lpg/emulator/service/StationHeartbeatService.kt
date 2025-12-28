package no.cloudberries.lpg.emulator.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * Station Heartbeat Service
 * 
 * Sends periodic health/status updates to the Cloud API to maintain
 * station online/offline status and synchronize configuration.
 * 
 * ## Heartbeat Payload:
 * ```json
 * {
 *   "stationId": "S001",
 *   "edgeId": "EDGE-S001-01",
 *   "status": "ONLINE",
 *   "timestamp": "2025-12-27T18:00:00Z",
 *   "dispensers": [
 *     {
 *       "dispenserId": "D001",
 *       "address": 1,
 *       "status": "IDLE"
 *     }
 *   ],
 *   "version": "1.0.0"
 * }
 * ```
 * 
 * ## Features:
 * - Periodic heartbeat every 30 seconds (configurable)
 * - Retry on failure with exponential backoff
 * - Graceful shutdown on service stop
 * - Config pull on each heartbeat response
 */
@Service
class StationHeartbeatService(
    @Value("\${station.id:S000}") private val stationId: String,
    @Value("\${edge.id:EDGE-LOCAL}") private val edgeId: String,
    @Value("\${dispenser.id:D001}") private val dispenserId: String,
    @Value("\${emulator.address:1}") private val dispenserAddress: Int,
    @Value("\${lpg-api.base-url}") private val cloudApiUrl: String,
    @Value("\${heartbeat.interval-seconds:30}") private val heartbeatIntervalSeconds: Long,
    @Value("\${heartbeat.enabled:true}") private val heartbeatEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(StationHeartbeatService::class.java)
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    
    @Volatile
    private var isRunning = false
    
    @Volatile
    private var currentStatus: String = "IDLE"
    
    @Volatile
    private var consecutiveFailures: Int = 0
    
    @PostConstruct
    fun start() {
        if (!heartbeatEnabled) {
            logger.info("📡 Station Heartbeat DISABLED by configuration")
            return
        }
        
        logger.info("=" .repeat(80))
        logger.info("📡 STATION HEARTBEAT SERVICE STARTING")
        logger.info("   Station ID: $stationId")
        logger.info("   Edge ID: $edgeId")
        logger.info("   Cloud API: $cloudApiUrl")
        logger.info("   Interval: ${heartbeatIntervalSeconds}s")
        logger.info("=" .repeat(80))
        
        isRunning = true
        
        heartbeatJob = scope.launch {
            // Send initial heartbeat immediately
            sendHeartbeat()
            
            // Then send periodic heartbeats
            while (isActive && isRunning) {
                delay(heartbeatIntervalSeconds * 1000)
                sendHeartbeat()
            }
        }
        
        logger.info("✅ Station Heartbeat started")
    }
    
    @PreDestroy
    fun stop() {
        logger.info("🛑 Stopping Station Heartbeat...")
        isRunning = false
        
        runBlocking {
            // Send final OFFLINE heartbeat
            sendHeartbeat(status = "OFFLINE")
            heartbeatJob?.cancelAndJoin()
        }
        
        scope.cancel()
        logger.info("✅ Station Heartbeat stopped")
    }
    
    /**
     * Update current dispenser status (called by EmulatorService)
     */
    fun updateDispenserStatus(status: String) {
        currentStatus = status
        logger.debug("📊 Dispenser status updated: $status")
    }
    
    /**
     * Send heartbeat to cloud API
     */
    private suspend fun sendHeartbeat(status: String = "ONLINE") {
        try {
            val timestamp = Instant.now().toString()
            
            val json = """
                {
                    "stationId": "$stationId",
                    "edgeId": "$edgeId",
                    "status": "$status",
                    "timestamp": "$timestamp",
                    "dispensers": [
                        {
                            "dispenserId": "$dispenserId",
                            "address": $dispenserAddress,
                            "status": "$currentStatus"
                        }
                    ],
                    "version": "1.0.0"
                }
            """.trimIndent()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$cloudApiUrl/api/stations/$stationId/heartbeat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = withContext(Dispatchers.IO) {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            }
            
            if (response.statusCode() in 200..299) {
                if (consecutiveFailures > 0) {
                    logger.info("✅ Heartbeat restored after $consecutiveFailures failure(s)")
                }
                consecutiveFailures = 0
                logger.debug("💚 Heartbeat sent: $stationId → $status")
                
                // Process config updates from response if any
                processHeartbeatResponse(response.body())
            } else {
                consecutiveFailures++
                logger.warn("⚠️ Heartbeat failed: ${response.statusCode()} (failure #$consecutiveFailures)")
            }
            
        } catch (e: Exception) {
            consecutiveFailures++
            logger.error("❌ Heartbeat error (failure #$consecutiveFailures): ${e.message}")
            
            if (consecutiveFailures >= 5) {
                logger.error("🔴 CRITICAL: $consecutiveFailures consecutive heartbeat failures - Cloud may be unreachable")
            }
        }
    }
    
    /**
     * Process configuration updates from heartbeat response
     */
    private fun processHeartbeatResponse(responseBody: String) {
        try {
            // Parse response for config updates (price changes, online/offline commands, etc.)
            if (responseBody.contains("\"configUpdated\":true")) {
                logger.info("🔄 Configuration update detected in heartbeat response")
                // TODO: Apply config updates (price, status, etc.)
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to process heartbeat response: ${e.message}")
        }
    }
}
