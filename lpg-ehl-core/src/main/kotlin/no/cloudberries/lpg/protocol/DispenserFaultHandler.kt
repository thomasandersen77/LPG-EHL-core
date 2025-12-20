package no.cloudberries.lpg.protocol

import org.slf4j.LoggerFactory

/**
 * Handler for EHL-x4 fault detection and reaction.
 * 
 * ## Responsibility:
 * - Detect faults from display data or ERROR_QUERY responses
 * - Determine appropriate DispenserStatus based on fault severity
 * - Provide fault information for diagnostics
 * 
 * ## Architecture Context:
 * - **lpg-ehl-core** (this): Protocol-level fault detection and status mapping
 * - **lpg-ehl-api**: Business logic that blocks commands and persists fault events
 * 
 * ## Fault Handling Strategy:
 * ### CRITICAL Faults (E-05, E-06, E-07, E-08):
 * - Entire dispenser is blocked
 * - Transition to ERROR state immediately
 * - Prevent all fueling commands (UNBLOCK, PRODUCT_SELECT, etc.)
 * - Require service intervention or manual reset
 * 
 * ### WARNING Faults (E-01, E-02, E-09):
 * - Hose-specific issue, other hoses may continue
 * - May auto-clear after retry or nozzle cycle
 * - Do NOT block entire dispenser
 * - Log for diagnostics and monitoring
 * 
 * ## Usage Example:
 * ```kotlin
 * val packet = connection.queryState()
 * 
 * // Check for faults in display data
 * val faultResult = DispenserFaultHandler.checkForFault(packet.data)
 * 
 * when (faultResult) {
 *     is FaultCheckResult.FaultDetected -> {
 *         if (faultResult.fault.level == EhlErrorLevel.CRITICAL) {
 *             // Block dispenser, set ERROR state
 *             currentStatus = DispenserStatus.ERROR(faultResult.fault.code)
 *             blockAllCommands()
 *         } else {
 *             // Log warning, continue operation
 *             logWarning(faultResult.fault)
 *         }
 *     }
 *     is FaultCheckResult.NoFault -> {
 *         // Continue normal operation
 *     }
 * }
 * ```
 */
object DispenserFaultHandler {
    private val logger = LoggerFactory.getLogger(DispenserFaultHandler::class.java)
    
    /**
     * Check packet data for fault codes.
     * 
     * Inspects display data or ERROR response data for fault codes.
     * 
     * @param data Packet data from EHL response
     * @return FaultCheckResult with detected fault or NoFault
     */
    fun checkForFault(data: ByteArray): FaultCheckResult {
        if (data.isEmpty()) {
            return FaultCheckResult.NoFault
        }
        
        val displayResult = EhlDisplayParser.parseDisplayData(data)
        
        return when (displayResult) {
            is DisplayParseResult.Fault -> {
                logger.info(
                    "Fault detected: ${displayResult.fault.code} " +
                    "(${displayResult.fault.level}) - ${displayResult.fault.description}"
                )
                FaultCheckResult.FaultDetected(displayResult.fault)
            }
            is DisplayParseResult.Normal -> {
                FaultCheckResult.NoFault
            }
        }
    }
    
    /**
     * Determine appropriate DispenserStatus based on fault.
     * 
     * Maps fault severity to dispenser status:
     * - CRITICAL faults -> ERROR status (blocks dispenser)
     * - WARNING faults -> Continue with current status (log only)
     * 
     * @param fault Detected fault
     * @param currentStatus Current dispenser status before fault
     * @return New DispenserStatus reflecting fault state
     */
    fun determineStatusFromFault(
        fault: EhlFault,
        currentStatus: DispenserStatus = DispenserStatus.IDLE
    ): DispenserStatus {
        return when (fault.level) {
            EhlErrorLevel.CRITICAL -> {
                logger.error(
                    "CRITICAL fault detected: ${fault.code} - Blocking dispenser. " +
                    "Action: ${fault.recommendedAction}"
                )
                // For critical faults, we'll use a synthetic error code
                // In real protocol, this might come from ERROR_QUERY (0x4C) response
                DispenserStatus.ERROR(errorCode = fault.code.hashCode())
            }
            EhlErrorLevel.WARNING -> {
                logger.warn(
                    "WARNING fault detected: ${fault.code} - Continuing operation. " +
                    "Action: ${fault.recommendedAction}"
                )
                // For warnings, maintain current status but log the fault
                currentStatus
            }
        }
    }
    
    /**
     * Check if a status transition should be blocked due to ERROR state.
     * 
     * @param currentStatus Current dispenser status
     * @param targetCommand Command attempting to be executed
     * @return true if command should be blocked
     */
    fun shouldBlockCommand(
        currentStatus: DispenserStatus,
        targetCommand: EhlCommand
    ): Boolean {
        // Block all operational commands if in ERROR state
        if (currentStatus is DispenserStatus.ERROR) {
            val operationalCommands = setOf(
                EhlCommand.UNBLOCK,
                EhlCommand.PRODUCT_SELECT,
                EhlCommand.PROG_PRC,
                EhlCommand.PROG_AMOUNT,
                EhlCommand.PROG_VOLUME
            )
            
            val shouldBlock = targetCommand in operationalCommands
            
            if (shouldBlock) {
                logger.warn(
                    "Blocking command ${targetCommand.name} due to ERROR state " +
                    "(errorCode=${currentStatus.errorCode})"
                )
            }
            
            return shouldBlock
        }
        
        return false
    }
    
    /**
     * Check if dispenser can accept fueling commands.
     * 
     * @param currentStatus Current dispenser status
     * @param lastFault Last detected fault (if any)
     * @return true if fueling is allowed
     */
    fun canFuel(currentStatus: DispenserStatus, lastFault: EhlFault?): Boolean {
        // Cannot fuel if in ERROR state
        if (currentStatus is DispenserStatus.ERROR) {
            return false
        }
        
        // Cannot fuel if last fault was CRITICAL
        if (lastFault != null && lastFault.level == EhlErrorLevel.CRITICAL) {
            logger.warn("Fueling blocked due to unresolved CRITICAL fault: ${lastFault.code}")
            return false
        }
        
        return true
    }
}

/**
 * Result of fault check operation.
 */
sealed interface FaultCheckResult {
    /**
     * No fault detected in data
     */
    data object NoFault : FaultCheckResult
    
    /**
     * Fault detected with details
     * 
     * @param fault The detected fault with severity and action
     */
    data class FaultDetected(val fault: EhlFault) : FaultCheckResult
}
