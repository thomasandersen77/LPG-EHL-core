package no.cloudberries.lpg.protocol

import java.time.Instant

/**
 * Diagnostics snapshot for EHL dispenser.
 * 
 * Provides operational health information for monitoring and troubleshooting.
 * 
 * ## Use Cases:
 * - Admin dashboard showing dispenser status
 * - Alert system monitoring for CRITICAL faults
 * - Service technician diagnostics
 * - Historical fault tracking
 * 
 * ## Architecture:
 * - **lpg-ehl-core**: Data model (this file)
 * - **lpg-ehl-api**: Controller that exposes /admin/ehl/diagnostics endpoint
 */
data class EhlDiagnosticsSnapshot(
    /**
     * Dispenser address on RS-485 bus
     */
    val address: Int,
    
    /**
     * Connection status
     * 
     * True if dispenser has communicated within reasonable threshold
     * (e.g. last 60 seconds for active systems)
     */
    val connected: Boolean,
    
    /**
     * Last time data was received from dispenser
     */
    val lastRxAt: Instant?,
    
    /**
     * Last time data was sent to dispenser
     */
    val lastTxAt: Instant?,
    
    /**
     * Current operational state
     */
    val state: DispenserStatus,
    
    /**
     * Last detected fault (if any)
     * 
     * Null if no faults detected since last reset/startup
     */
    val lastFault: FaultInfo?,
    
    /**
     * RS-485 configuration and troubleshooting hints
     * 
     * Static or dynamic information about serial communication health
     */
    val rs485Hints: String
) {
    /**
     * Check if dispenser is in error state requiring intervention
     */
    fun isInError(): Boolean = state is DispenserStatus.ERROR
    
    /**
     * Check if there's an unresolved CRITICAL fault
     */
    fun hasCriticalFault(): Boolean = 
        lastFault != null && lastFault.level == EhlErrorLevel.CRITICAL
    
    /**
     * Get human-readable status summary
     */
    fun getStatusSummary(): String {
        return when {
            !connected -> "DISCONNECTED - No communication"
            isInError() -> "ERROR - Dispenser blocked: ${lastFault?.code ?: "Unknown"}"
            hasCriticalFault() -> "CRITICAL FAULT - ${lastFault?.code}: ${lastFault?.description}"
            lastFault != null -> "WARNING - ${lastFault?.code}: ${lastFault?.description}"
            else -> "OPERATIONAL - ${state::class.simpleName}"
        }
    }
}

/**
 * Fault information for diagnostics.
 * 
 * Serializable subset of EhlFault for API responses.
 */
data class FaultInfo(
    /**
     * Fault code (e.g. "E-05")
     */
    val code: String,
    
    /**
     * Human-readable description
     */
    val description: String,
    
    /**
     * Severity level
     */
    val level: EhlErrorLevel,
    
    /**
     * Recommended action for service technician
     */
    val recommendedAction: String,
    
    /**
     * Whether fault may auto-clear after retry
     */
    val autoRetryable: Boolean,
    
    /**
     * When this fault was first detected
     */
    val detectedAt: Instant
) {
    companion object {
        /**
         * Create FaultInfo from EhlFault
         */
        fun fromFault(fault: EhlFault, detectedAt: Instant = Instant.now()): FaultInfo {
            return FaultInfo(
                code = fault.code,
                description = fault.description,
                level = fault.level,
                recommendedAction = fault.recommendedAction,
                autoRetryable = fault.autoRetryable,
                detectedAt = detectedAt
            )
        }
    }
}

/**
 * RS-485 configuration hints for diagnostics.
 * 
 * Static information about proper RS-485 setup and common issues.
 */
object Rs485Hints {
    /**
     * Default RS-485 troubleshooting hints
     */
    const val DEFAULT = """
        RS-485 Configuration:
        - Baud Rate: 4800 bps (default for EHL protocol)
        - Data Bits: 8, Parity: None, Stop Bits: 1
        - Termination: 120Ω resistor between A/B at both ends of bus
        - Max Cable Length: 1200m (4000ft) for 4800 baud
        - Max Nodes: 32 devices on single bus
        
        Common Issues:
        - Check A/B wiring polarity (swap if unreliable)
        - Ensure 120Ω termination at first and last device
        - Verify grounding and shielding
        - Check for electrical noise near cables
        - Confirm baud rate matches dispenser DIP switches
    """.trimIndent()
    
    /**
     * Generate dynamic hints based on connection health
     */
    fun generateHints(
        connected: Boolean,
        lastRxDuration: Long?,
        errorRate: Double? = null
    ): String {
        val hints = mutableListOf<String>()
        
        if (!connected) {
            hints.add("⚠️ NO CONNECTION - Check physical wiring and power")
            hints.add("   - Verify RS-485 A/B connections")
            hints.add("   - Check dispenser power supply")
            hints.add("   - Verify baud rate (4800 bps)")
        }
        
        if (lastRxDuration != null && lastRxDuration > 30000) {
            hints.add("⚠️ SLOW RESPONSE - Potential bus contention or noise")
            hints.add("   - Check for other devices on bus")
            hints.add("   - Verify termination resistors")
        }
        
        if (errorRate != null && errorRate > 0.05) {
            hints.add("⚠️ HIGH ERROR RATE (${String.format("%.1f%%", errorRate * 100)}) - Check signal quality")
            hints.add("   - Verify cable shielding")
            hints.add("   - Check for nearby electrical interference")
        }
        
        return if (hints.isEmpty()) {
            "✅ RS-485 communication healthy"
        } else {
            hints.joinToString("\n")
        }
    }
}
