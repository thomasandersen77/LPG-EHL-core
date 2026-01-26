package no.cloudberries.lpg.service.system

import no.cloudberries.lpg.communication.HardwareWatchdogCapable
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * PART 4: HARDWARE WATCHDOG SERVICE
 * 
 * Monitors RS-485 serial connection health and automatically attempts reconnection
 * when the connection appears dead (no data received for 60 seconds).
 * 
 * This handles production scenarios like:
 * - USB-to-RS485 adapter unplugged
 * - Driver hangs/crashes
 * - Cable disconnection
 * - Power loss to RS-485 bus
 * 
 * The service runs every 30 seconds and checks if data has been received.
 * If watchdog timeout is exceeded, it triggers automatic reconnection.
 */
@Service
class HardwareWatchdogService(
    private val watchdogCapable: HardwareWatchdogCapable? = null  // Optional - for production use
) {
    private val logger = LoggerFactory.getLogger(HardwareWatchdogService::class.java)
    
    private val consecutiveFailures = AtomicInteger(0)
    private val lastSuccessfulCheck = AtomicLong(System.currentTimeMillis())
    private val reconnectAttempts = AtomicInteger(0)
    
    private val maxConsecutiveFailures = 3  // Try 3 times before giving up
    private val reconnectCooldownMs = 300_000L  // 5 minutes cooldown after max failures
    
    @Volatile
    private var lastReconnectAttempt: Long = 0
    
    /**
     * Initialize the watchdog (enable monitoring).
     * Call this after serial port is successfully opened.
     */
    fun initialize() {
        watchdogCapable?.enableWatchdog()
        logger.info("🐕 Hardware watchdog initialized")
    }
    
    /**
     * Periodic health check - runs every 30 seconds.
     * Checks if RS-485 connection is alive and triggers reconnection if needed.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)  // Check every 30s, start after 1 min
    fun performHealthCheck() {
        if (watchdogCapable == null) {
            // No watchdog capable component configured (local dev mode)
            return
        }
        
        try {
            val isHealthy = watchdogCapable.checkWatchdog()
            
            if (isHealthy) {
                // Connection is healthy
                if (consecutiveFailures.get() > 0) {
                    logger.info("✅ Connection recovered - watchdog check passed")
                    consecutiveFailures.set(0)
                }
                lastSuccessfulCheck.set(System.currentTimeMillis())
                
            } else {
                // Connection appears dead
                handleConnectionFailure()
            }
            
        } catch (e: Exception) {
            logger.error("Watchdog health check failed with exception: ${e.message}", e)
            handleConnectionFailure()
        }
    }
    
    /**
     * Handle connection failure - attempt reconnection with backoff.
     */
    private fun handleConnectionFailure() {
        val failures = consecutiveFailures.incrementAndGet()
        val timeSinceLastData = watchdogCapable?.getTimeSinceLastData() ?: 0
        
        logger.error(
            "❌ Watchdog health check failed (consecutive failures: $failures). " +
            "Time since last data: ${timeSinceLastData}ms"
        )
        
        // Check if we're in cooldown period after max failures
        val timeSinceLastReconnect = System.currentTimeMillis() - lastReconnectAttempt
        if (failures > maxConsecutiveFailures && timeSinceLastReconnect < reconnectCooldownMs) {
            logger.warn(
                "⏸️ Max reconnect attempts reached ($maxConsecutiveFailures). " +
                "Cooling down for ${(reconnectCooldownMs - timeSinceLastReconnect) / 1000}s before retrying."
            )
            return
        }
        
        // Reset failure counter if we're past cooldown
        if (failures > maxConsecutiveFailures && timeSinceLastReconnect >= reconnectCooldownMs) {
            logger.info("Cooldown period expired, resetting failure counter")
            consecutiveFailures.set(1)
        }
        
        // Attempt reconnection
        attemptReconnection()
    }
    
    /**
     * Attempt to reconnect to the serial port.
     */
    private fun attemptReconnection() {
        if (watchdogCapable == null) {
            return
        }
        
        val attempt = reconnectAttempts.incrementAndGet()
        lastReconnectAttempt = System.currentTimeMillis()
        
        logger.warn("🔧 Attempting automatic reconnection (attempt #$attempt)...")
        
        try {
            val success = watchdogCapable.reconnect()
            
            if (success) {
                logger.info("🎉 Automatic reconnection successful (attempt #$attempt)")
                consecutiveFailures.set(0)
                lastSuccessfulCheck.set(System.currentTimeMillis())
            } else {
                logger.error("💥 Automatic reconnection failed (attempt #$attempt)")
            }
            
        } catch (e: Exception) {
            logger.error("Reconnection attempt #$attempt failed with exception: ${e.message}", e)
        }
    }
    
    /**
     * Get watchdog statistics for monitoring/health endpoints.
     */
    fun getStatistics(): WatchdogStatistics {
        return WatchdogStatistics(
            isEnabled = watchdogCapable != null,
            consecutiveFailures = consecutiveFailures.get(),
            reconnectAttempts = reconnectAttempts.get(),
            lastSuccessfulCheckTime = lastSuccessfulCheck.get(),
            timeSinceLastData = watchdogCapable?.getTimeSinceLastData() ?: 0
        )
    }
    
    /**
     * Manual reconnect trigger (for admin/debug purposes).
     */
    fun forceReconnect(): Boolean {
        logger.info("🔨 Manual reconnect triggered")
        consecutiveFailures.set(0)  // Reset counter for manual action
        return watchdogCapable?.reconnect() ?: false
    }
}

/**
 * Watchdog statistics for monitoring.
 */
data class WatchdogStatistics(
    val isEnabled: Boolean,
    val consecutiveFailures: Int,
    val reconnectAttempts: Int,
    val lastSuccessfulCheckTime: Long,
    val timeSinceLastData: Long
)
