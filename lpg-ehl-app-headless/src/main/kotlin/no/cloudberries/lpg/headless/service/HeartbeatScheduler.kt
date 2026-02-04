package no.cloudberries.lpg.headless.service

import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Heartbeat scheduler for periodic status logging.
 * 
 * Logs pump status and system state at regular intervals for:
 * - Monitoring system health
 * - Post-mortem log analysis
 * - Debugging communication issues
 * 
 * Heartbeat interval: 60 seconds (configurable via lpg.heartbeat.interval-ms)
 */
@Component
class HeartbeatScheduler(
    private val pumpStateService: PumpStateService,
    @Value("\${lpg.dispenser.address:1}") private val dispenserAddress: Int,
    @Value("\${lpg.station.id:STATION-001}") private val stationId: String,
    @Value("\${ehl.transport.mode:HARDWARE}") private val transportMode: String
) {
    private val logger = LoggerFactory.getLogger(HeartbeatScheduler::class.java)
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    @Volatile
    private var heartbeatCount: Long = 0
    
    @Volatile
    private var lastStateTransition: String = "STARTUP"
    
    /**
     * Log heartbeat every 60 seconds (1 minute).
     * Shows current pump state, volume, price, and system info.
     */
    @Scheduled(fixedRateString = "\${lpg.heartbeat.interval-ms:60000}")
    fun logHeartbeat() {
        heartbeatCount++
        val now = LocalDateTime.now().format(timestampFormat)
        
        try {
            val status = pumpStateService.getStatus(dispenserAddress)
            
            logger.info("═══════════════════════════════════════════════════════════════")
            logger.info("💓 HEARTBEAT #$heartbeatCount | $now")
            logger.info("═══════════════════════════════════════════════════════════════")
            logger.info("📍 Station: $stationId | Mode: $transportMode | Dispenser: #$dispenserAddress")
            logger.info("🔵 State: ${status.state} | Last transition: $lastStateTransition")
            logger.info("⛽ Volume: %.2f L | Amount: %.2f kr | Price: %.2f kr/L".format(
                status.volumeLitres, status.amountKr, status.pricePerLitreKr
            ))
            logger.info("🔧 Nozzle: ${if (status.nozzleLifted) "LIFTED" else "holstered"} | Pending TX: ${status.hasPendingTransaction}")
            logger.info("═══════════════════════════════════════════════════════════════")
            
        } catch (e: Exception) {
            logger.warn("💓 HEARTBEAT #$heartbeatCount | $now | ERROR: ${e.message}")
        }
    }
    
    /**
     * Log state transition for tracking.
     * Called by PumpStateService when state changes.
     */
    fun recordStateTransition(fromState: String, toState: String, reason: String) {
        lastStateTransition = "$fromState → $toState ($reason)"
        logger.info("📊 STATE TRANSITION RECORDED: $lastStateTransition")
    }
    
    /**
     * Quick status log every 10 seconds during active pumping.
     * Only logs when pump is in PUMPING or READY_TO_PUMP state.
     */
    @Scheduled(fixedRate = 10000)
    fun logActiveStatus() {
        try {
            val status = pumpStateService.getStatus(dispenserAddress)
            
            when (status.state) {
                "PUMPING" -> {
                    logger.info("⛽ PUMPING: %.2f L | %.2f kr | %.2f kr/L".format(
                        status.volumeLitres, status.amountKr, status.pricePerLitreKr
                    ))
                }
                "READY_TO_PUMP" -> {
                    logger.info("⏳ READY_TO_PUMP: Waiting for customer to start pumping...")
                }
                // Other states: silent (logged in heartbeat)
            }
        } catch (e: Exception) {
            // Ignore errors in active status - heartbeat will catch issues
        }
    }
}
