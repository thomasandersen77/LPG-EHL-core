package no.cloudberries.lpg.protocol

import org.slf4j.LoggerFactory

/**
 * Parser for EHL-x4 display data.
 * 
 * The display on the EHL-x4 counter shows fault codes in the "unit price" field.
 * According to MM Petro manual section 7.4, faults appear as "E-01", "E-02", etc.
 * 
 * ## Architecture:
 * - **lpg-ehl-core**: This parser extracts fault codes from display bytes (protocol layer)
 * - **lpg-ehl-api**: Service layer reacts to critical faults (business logic layer)
 * 
 * ## Display Data Format:
 * Display data from EHL protocol contains ASCII-encoded text fields.
 * When a fault occurs, the unit price field contains "E-XX" instead of a price.
 * 
 * ## Usage:
 * ```kotlin
 * val displayData = packet.data
 * val result = EhlDisplayParser.parseDisplayData(displayData)
 * 
 * when (result) {
 *     is DisplayParseResult.Fault -> {
 *         // Handle fault: result.fault
 *         if (result.fault.level == EhlErrorLevel.CRITICAL) {
 *             // Block dispenser, set ERROR state
 *         }
 *     }
 *     is DisplayParseResult.Normal -> {
 *         // Display shows normal data
 *     }
 * }
 * ```
 */
object EhlDisplayParser {
    private val logger = LoggerFactory.getLogger(EhlDisplayParser::class.java)
    
    /**
     * Parse display data from EHL protocol response.
     * 
     * Display data contains ASCII-encoded fields. This method searches for
     * fault code patterns (E-XX) in the data.
     * 
     * @param data Raw display data bytes from EHL response
     * @return DisplayParseResult indicating fault or normal operation
     */
    fun parseDisplayData(data: ByteArray): DisplayParseResult {
        if (data.isEmpty()) {
            logger.debug("Empty display data")
            return DisplayParseResult.Normal
        }
        
        // Convert bytes to ASCII string for pattern matching
        val displayText = data.decodeToString(throwOnInvalidSequence = false)
        
        logger.trace("Display data: '$displayText' (${data.size} bytes)")
        
        // Search for fault code pattern in display text
        val faultMatch = Regex("""E[-\s]?\d{2}""", RegexOption.IGNORE_CASE)
            .find(displayText)
        
        return if (faultMatch != null) {
            val faultCode = faultMatch.value
            val fault = EhlFault.fromDisplayCode(faultCode)
            
            logger.info("Fault detected in display: '$faultCode' -> ${fault.code} (${fault.level})")
            DisplayParseResult.Fault(fault)
        } else {
            DisplayParseResult.Normal
        }
    }
    
    /**
     * Check if display data contains a fault code pattern.
     * 
     * @param data Raw display data bytes
     * @return true if fault pattern detected
     */
    fun containsFaultCode(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        
        val displayText = data.decodeToString(throwOnInvalidSequence = false)
        return EhlFault.isFaultCode(displayText.trim())
    }
    
    /**
     * Extract fault code string from display data if present.
     * 
     * @param data Raw display data bytes
     * @return Fault code string (e.g. "E-05") or null if not found
     */
    fun extractFaultCode(data: ByteArray): String? {
        if (data.isEmpty()) return null
        
        val displayText = data.decodeToString(throwOnInvalidSequence = false)
        val faultMatch = Regex("""E[-\s]?\d{2}""", RegexOption.IGNORE_CASE)
            .find(displayText)
        
        return faultMatch?.value
    }
}

/**
 * Result of parsing display data.
 * 
 * Sealed interface for type-safe handling of display parsing results.
 */
sealed interface DisplayParseResult {
    /**
     * Display shows normal operation data (price, volume, etc.)
     */
    data object Normal : DisplayParseResult
    
    /**
     * Display shows a fault code
     * 
     * @param fault The parsed fault with severity and recommended action
     */
    data class Fault(val fault: EhlFault) : DisplayParseResult
}
