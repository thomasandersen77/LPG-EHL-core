package no.cloudberries.lpg.transaction

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Transaction Watchdog
 * 
 * Monitors active transactions and enforces preset limits by actively polling
 * the dispenser and sending STOP commands when limits are reached.
 * 
 * ## Use Case:
 * If the PLS (dispenser) doesn't support hardware preset limits, the PC must
 * actively monitor the transaction and send a STOP command when the maximum
 * amount or volume is reached.
 * 
 * ## Legacy Behavior:
 * VB6 code (Tankinger_form.frm) had logic to poll VOLUME responses and compare
 * against preset limits, then send BLOCK/STOP command when limit reached.
 * 
 * ## Usage:
 * ```kotlin
 * val watchdog = TransactionWatchdog(communicator)
 * 
 * watchdog.monitorTransaction(
 *     dispenserId = 1,
 *     maxAmountCents = 50000,  // 500 kr
 *     onMaxReached = { actualAmount ->
 *         logger.info("Stopped at $actualAmount øre")
 *         // Handle payment, print receipt, etc.
 *     }
 * )
 * ```
 */
class TransactionWatchdog(
    /**
     * Poll interval for checking transaction progress
     * Default: 500ms (2 times per second)
     */
    private val pollInterval: Duration = Duration.ofMillis(500),
    
    /**
     * Tolerance in øre for overshoot
     * Some dispensers may slightly overshoot the target due to flow rate
     */
    private val overshootToleranceCents: Int = 50,  // 50 øre = 0.50 kr
    
    /**
     * KRITISK SIKKERHET: Absolute transaction timeout
     * Maximum duration for any transaction before forced stop
     * Default: 2 minutes (safety limit for hardware failure scenarios)
     */
    private val absoluteTimeoutSeconds: Int = 120,
    
    /**
     * KRITISK SIKKERHET: Max consecutive null responses from volume provider
     * If this many consecutive polls return null, assume communication failure
     * and trigger emergency stop. Default: 5 (2.5 seconds at 500ms interval)
     */
    private val maxConsecutiveNulls: Int = 5
) {
    private val logger = LoggerFactory.getLogger(TransactionWatchdog::class.java)
    
    /**
     * KRITISK SIKKERHET: Fail-safe callback triggered on watchdog failures
     * Called when watchdog crashes or communication fails critically
     * Can be overridden by subclasses or via composition pattern
     */
    var failSafeCallback: suspend (String, Int) -> Unit = { reason, dispenserId ->
        logger.error("WATCHDOG FAIL-SAFE TRIGGERED: $reason (dispenser=$dispenserId)")
        // Default: just log. Override in production to:
        // - Send SMS alert to operations
        // - Log to Azure with HIGH priority
        // - Trigger physical alarm if available
    }
    
    /**
     * Monitor strategy for stopping transactions
     */
    enum class MonitorStrategy {
        /** Stop when amount (kr) reaches limit */
        AMOUNT,
        /** Stop when volume (liters) reaches limit */
        VOLUME,
        /** Stop when either amount OR volume reaches limit (first wins) */
        AMOUNT_OR_VOLUME
    }
    
    /**
     * Watchdog configuration for a single transaction
     */
    data class WatchdogConfig(
        val dispenserId: Int,
        val strategy: MonitorStrategy = MonitorStrategy.AMOUNT,
        val maxAmountCents: Int? = null,
        val maxVolumeLiters: Double? = null,
        val pollIntervalOverride: Duration? = null,
        /**
         * KRITISK SIKKERHET: Override absolute timeout for this transaction
         * If null, uses watchdog's default absolute timeout
         */
        val absoluteTimeoutSecondsOverride: Int? = null
    ) {
        init {
            when (strategy) {
                MonitorStrategy.AMOUNT -> require(maxAmountCents != null) {
                    "maxAmountCents must be set when using AMOUNT strategy"
                }
                MonitorStrategy.VOLUME -> require(maxVolumeLiters != null) {
                    "maxVolumeLiters must be set when using VOLUME strategy"
                }
                MonitorStrategy.AMOUNT_OR_VOLUME -> require(
                    maxAmountCents != null || maxVolumeLiters != null
                ) {
                    "At least one limit must be set when using AMOUNT_OR_VOLUME strategy"
                }
            }
        }
    }
    
    /**
     * Result of watchdog monitoring
     */
    sealed class WatchdogResult {
        data class MaxReached(
            val actualAmountCents: Int,
            val actualVolumeLiters: Double,
            val reason: String
        ) : WatchdogResult()
        
        data class Cancelled(val reason: String) : WatchdogResult()
        data class Error(val exception: Exception) : WatchdogResult()
    }
    
    /**
     * Monitor a transaction and stop when limits are reached
     * 
     * This is a suspending function that will block until:
     * - Limit is reached and dispenser is stopped
     * - Transaction is cancelled
     * - Error occurs
     * 
     * @param config Watchdog configuration
     * @param volumeProvider Function to poll current volume/amount from dispenser
     * @param stopCommand Function to send STOP command to dispenser
     * @return WatchdogResult indicating outcome
     */
    suspend fun monitorTransaction(
        config: WatchdogConfig,
        volumeProvider: suspend () -> Pair<Double, Int>?,  // (volumeLiters, amountCents)
        stopCommand: suspend () -> Unit,
        /**
         * KRITISK SIKKERHET: Stop command with retry and escalation
         * If provided, this will be used instead of simple stopCommand for emergency stops.
         * Should implement retry logic and escalation on failure.
         */
        emergencyStopCommand: suspend () -> Boolean = {
            try {
                stopCommand()
                true
            } catch (e: Exception) {
                logger.error("Emergency stop failed", e)
                false
            }
        }
    ): WatchdogResult = coroutineScope {
        logger.info(
            "Starting watchdog for dispenser ${config.dispenserId}: " +
            "strategy=${config.strategy}, " +
            "maxAmount=${config.maxAmountCents}øre, " +
            "maxVolume=${config.maxVolumeLiters}L, " +
            "absoluteTimeout=${config.absoluteTimeoutSecondsOverride ?: absoluteTimeoutSeconds}s"
        )
        
        val effectivePollInterval = config.pollIntervalOverride ?: pollInterval
        val effectiveAbsoluteTimeout = config.absoluteTimeoutSecondsOverride ?: absoluteTimeoutSeconds
        var lastVolume = 0.0
        var lastAmount = 0
        var stableCount = 0  // Count of consecutive identical readings
        var consecutiveNulls = 0  // KRITISK: Track consecutive null responses
        
        // KRITISK SIKKERHET: Absolute timeout tracking
        var absoluteTimeoutReached = false
        var shouldStop = false
        
        // KRITISK SIKKERHET: Absolute timeout job
        // This runs in parallel and will force-stop the transaction if it exceeds max duration
        val absoluteTimeoutJob = launch {
            delay(effectiveAbsoluteTimeout * 1000L)
            
            logger.error(
                "ABSOLUTE TIMEOUT REACHED after ${effectiveAbsoluteTimeout}s " +
                "for dispenser ${config.dispenserId} - Force stopping"
            )
            
            absoluteTimeoutReached = true
            shouldStop = true
            
            // Attempt emergency stop
            val stopped = try {
                emergencyStopCommand()
            } catch (e: Exception) {
                logger.error("Emergency stop failed during absolute timeout", e)
                false
            }
            
            if (!stopped) {
                // FAIL-SAFE: Emergency stop failed
                failSafeCallback(
                    "Emergency stop failed during absolute timeout",
                    config.dispenserId
                )
            }
        }
        
        try {
            while (isActive && !shouldStop) {
                try {
                    // Poll current values with timeout and null tracking
                    val volumeData = volumeProvider()
                    
                    if (volumeData == null) {
                        consecutiveNulls++
                        logger.warn(
                            "Volume provider returned null (${consecutiveNulls}/${maxConsecutiveNulls}) " +
                            "for dispenser ${config.dispenserId}"
                        )
                        
                        // KRITISK SIKKERHET: Too many consecutive nulls = communication failure
                        if (consecutiveNulls >= maxConsecutiveNulls) {
                            logger.error(
                                "COMMUNICATION FAILURE: Lost contact with dispenser ${config.dispenserId} " +
                                "for ${consecutiveNulls * effectivePollInterval.toMillis()}ms - Emergency stopping"
                            )
                            
                            // SEND EMERGENCY STOP
                            val stopped = try {
                                emergencyStopCommand()
                            } catch (e: Exception) {
                                logger.error("Emergency stop failed after communication loss", e)
                                false
                            }
                            
                            if (!stopped) {
                                // FAIL-SAFE: Stop command failed after losing contact
                                failSafeCallback(
                                    "Stop command failed after losing contact with dispenser",
                                    config.dispenserId
                                )
                            }
                            
                            absoluteTimeoutJob.cancel()
                            return@coroutineScope WatchdogResult.Error(
                                Exception("Lost contact with dispenser after ${consecutiveNulls} polls")
                            )
                        }
                        
                        delay(effectivePollInterval.toMillis())
                        continue
                    }
                    
                    // Reset null counter on successful read
                    consecutiveNulls = 0
                    val (volume, amount) = volumeData
                    
                    logger.trace(
                        "Watchdog poll: dispenser=${config.dispenserId}, " +
                        "volume=${String.format("%.2f", volume)}L, " +
                        "amount=$amount øre"
                    )
                    
                    // Check if values are stable (transaction might be finished)
                    if (volume == lastVolume && amount == lastAmount) {
                        stableCount++
                    } else {
                        stableCount = 0
                    }
                    
                    lastVolume = volume
                    lastAmount = amount
                    
                    // Check limits based on strategy
                    val limitReached = when (config.strategy) {
                        MonitorStrategy.AMOUNT -> {
                            config.maxAmountCents?.let { maxAmount ->
                                amount >= (maxAmount - overshootToleranceCents)
                            } ?: false
                        }
                        MonitorStrategy.VOLUME -> {
                            config.maxVolumeLiters?.let { maxVolume ->
                                volume >= (maxVolume - 0.05)  // 50ml tolerance
                            } ?: false
                        }
                        MonitorStrategy.AMOUNT_OR_VOLUME -> {
                            val amountReached = config.maxAmountCents?.let { maxAmount ->
                                amount >= (maxAmount - overshootToleranceCents)
                            } ?: false
                            
                            val volumeReached = config.maxVolumeLiters?.let { maxVolume ->
                                volume >= (maxVolume - 0.05)
                            } ?: false
                            
                            amountReached || volumeReached
                        }
                    }
                    
                    if (limitReached) {
                        val reason = when (config.strategy) {
                            MonitorStrategy.AMOUNT -> 
                                "Amount limit reached: $amount øre >= ${config.maxAmountCents} øre"
                            MonitorStrategy.VOLUME -> 
                                "Volume limit reached: $volume L >= ${config.maxVolumeLiters} L"
                            MonitorStrategy.AMOUNT_OR_VOLUME -> 
                                "Limit reached (amount=$amount øre, volume=$volume L)"
                        }
                        
                        logger.info("MAX REACHED! Stopping dispenser ${config.dispenserId}: $reason")
                        
                        // Send STOP command with emergency retry
                        val stopped = try {
                            emergencyStopCommand()
                        } catch (e: Exception) {
                            logger.error("Failed to stop dispenser after max reached", e)
                            false
                        }
                        
                        if (!stopped) {
                            logger.error("WARNING: Stop command failed but limit was reached")
                            // Continue - result will still be MaxReached
                        }
                        
                        absoluteTimeoutJob.cancel()
                        return@coroutineScope WatchdogResult.MaxReached(
                            actualAmountCents = amount,
                            actualVolumeLiters = volume,
                            reason = reason
                        )
                    }
                    
                    // If values stable for 5 consecutive polls, transaction might be done
                    if (stableCount >= 5 && volume > 0) {
                        logger.info(
                            "Transaction appears complete (values stable): " +
                            "volume=$volume L, amount=$amount øre"
                        )
                        // Don't stop - let normal flow handle this
                    }
                    
                    delay(effectivePollInterval.toMillis())
                    
                } catch (e: CancellationException) {
                    throw e  // Propagate cancellation
                } catch (e: Exception) {
                    logger.error("Error in watchdog poll cycle", e)
                    // Continue monitoring despite errors
                    delay(effectivePollInterval.toMillis())
                }
            }
            
            // If we get here, coroutine was cancelled
            absoluteTimeoutJob.cancel()
            
            if (absoluteTimeoutReached) {
                WatchdogResult.Cancelled("Absolute timeout reached after ${effectiveAbsoluteTimeout}s")
            } else {
                WatchdogResult.Cancelled("Watchdog monitoring cancelled")
            }
            
        } catch (e: CancellationException) {
            logger.info("Watchdog cancelled for dispenser ${config.dispenserId}")
            absoluteTimeoutJob.cancel()
            
            if (absoluteTimeoutReached) {
                WatchdogResult.Cancelled("Absolute timeout reached after ${effectiveAbsoluteTimeout}s")
            } else {
                WatchdogResult.Cancelled("Monitoring cancelled")
            }
        } catch (e: Exception) {
            // KRITISK SIKKERHET: Watchdog crashed - trigger fail-safe
            logger.error("FATAL ERROR: Watchdog crashed for dispenser ${config.dispenserId}", e)
            
            // Attempt emergency stop
            try {
                val stopped = emergencyStopCommand()
                if (!stopped) {
                    failSafeCallback(
                        "Emergency stop failed after watchdog crash: ${e.message}",
                        config.dispenserId
                    )
                }
            } catch (stopException: Exception) {
                logger.error("Emergency stop attempt failed after watchdog crash", stopException)
                failSafeCallback(
                    "All emergency stop attempts failed after crash",
                    config.dispenserId
                )
            }
            
            absoluteTimeoutJob.cancel()
            WatchdogResult.Error(e)
        } finally {
            absoluteTimeoutJob.cancel()
            logger.info("Watchdog stopped for dispenser ${config.dispenserId}")
        }
    }
    
    /**
     * Start monitoring in the background as a Job
     * 
     * Returns a Job that can be cancelled to stop monitoring.
     */
    fun startMonitoring(
        scope: CoroutineScope,
        config: WatchdogConfig,
        volumeProvider: suspend () -> Pair<Double, Int>?,
        stopCommand: suspend () -> Unit,
        emergencyStopCommand: suspend () -> Boolean = {
            try {
                stopCommand()
                true
            } catch (e: Exception) {
                logger.error("Emergency stop failed in background monitoring", e)
                false
            }
        },
        onResult: (WatchdogResult) -> Unit
    ): Job {
        return scope.launch {
            val result = monitorTransaction(config, volumeProvider, stopCommand, emergencyStopCommand)
            onResult(result)
        }
    }
}
