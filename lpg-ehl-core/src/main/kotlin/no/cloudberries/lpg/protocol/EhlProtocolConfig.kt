package no.cloudberries.lpg.protocol

/**
 * EHL Protocol Configuration
 * 
 * Supports different EHL protocol variants based on hardware/manufacturer.
 * This allows the same codebase to work with different dispenser types.
 * 
 * ## Background:
 * Analysis revealed two different protocol variants in legacy code:
 * - VB6 code uses: STX=0x02, ETX=0x03 (Standard EHL)
 * - Python code uses: STX=0x10/0x20, ETX=0x36 (Norges Gass variant)
 * 
 * This class provides runtime configuration to support both.
 * 
 * ## Usage Examples:
 * 
 * ### Default Configuration (Norges Gass):
 * ```kotlin
 * val config = EhlProtocolConfig()  // Uses Norges Gass variant by default
 * val packet = EhlPacket(address = 1, EhlCommand.STATE)
 * val encoded = EhlCodec.encode(packet, config = config)
 * ```
 * 
 * ### Standard EHL:
 * ```kotlin
 * val config = EhlProtocolConfig.standardEhl()
 * // Or explicitly:
 * val config = EhlProtocolConfig(variant = ProtocolVariant.STANDARD_EHL)
 * ```
 * 
 * ### Custom Timing for Slow Hardware:
 * ```kotlin
 * val config = EhlProtocolConfig(
 *     variant = ProtocolVariant.NORGES_GASS,
 *     interCommandDelayMs = 200,  // Slower RS-485 adapter
 *     responseTimeoutMs = 5000,   // Allow longer response time
 *     maxRetries = 5              // More retries in noisy environment
 * )
 * ```
 * 
 * ### Debug Mode with Packet Logging:
 * ```kotlin
 * val config = EhlProtocolConfig(
 *     enablePacketLogging = true  // Enables hex dump of all packets
 * )
 * ```
 * 
 * ## When to Use Which Variant:
 * 
 * **Use NORGES_GASS (default) if:**
 * - Working with Norges Gass installations
 * - Python tests from python-test/ directory pass
 * - STX bytes are 0x10 (controller) and 0x20 (dispenser)
 * 
 * **Use STANDARD_EHL if:**
 * - Working with generic European EHL dispensers
 * - Legacy VB6 code (using STX=0x02, ETX=0x03) is the reference
 * - Documentation specifies "Standard EHL Protocol"
 * 
 * ## Troubleshooting:
 * 
 * **No responses from dispenser:**
 * - Try increasing `interCommandDelayMs` to 200ms
 * - Check that correct variant is selected
 * - Verify RS-485 wiring (A/B swap is common mistake)
 * 
 * **Checksum errors:**
 * - Enable `enablePacketLogging = true`
 * - Verify STX/ETX bytes match hardware expectations
 * - Check for correct protocol variant
 * 
 * **Timeouts:**
 * - Increase `responseTimeoutMs` for slow hardware
 * - Check baud rate matches dispenser configuration
 * - Verify RS-485 termination and biasing
 */
data class EhlProtocolConfig(
    /**
     * Protocol variant to use
     */
    val variant: ProtocolVariant = ProtocolVariant.NORGES_GASS,
    
    /**
     * Inter-command delay in milliseconds
     * 
     * Legacy VB6 code had Sleep(100) between commands to prevent overwhelming
     * the PLS or to allow RS-485 converter direction switching time.
     * 
     * Default: 100ms (safe for most hardware)
     * Adjust down to 50ms if stable, up to 200ms if timeouts occur.
     */
    val interCommandDelayMs: Long = 100,
    
    /**
     * Response timeout in milliseconds
     */
    val responseTimeoutMs: Long = 2000,
    
    /**
     * Maximum retry attempts for failed commands
     */
    val maxRetries: Int = 3,
    
    /**
     * Enable hex dump logging of all packets (for debugging)
     */
    val enablePacketLogging: Boolean = false
) {
    
    /**
     * Get STX byte for controller-to-dispenser communication
     */
    val stxController: Byte
        get() = when (variant) {
            ProtocolVariant.STANDARD_EHL -> 0x02
            ProtocolVariant.NORGES_GASS -> 0x10
        }
    
    /**
     * Get STX byte for dispenser-to-controller communication
     */
    val stxDispenser: Byte
        get() = when (variant) {
            ProtocolVariant.STANDARD_EHL -> 0x02  // Same as controller in standard
            ProtocolVariant.NORGES_GASS -> 0x20
        }
    
    /**
     * Get ETX byte (end of transmission)
     */
    val etx: Byte
        get() = when (variant) {
            ProtocolVariant.STANDARD_EHL -> 0x03
            ProtocolVariant.NORGES_GASS -> 0x36
        }
    
    /**
     * Validate if a received STX byte is acceptable
     */
    fun isValidStx(stx: Byte): Boolean {
        return stx == stxController || stx == stxDispenser
    }
    
    companion object {
        /**
         * Create configuration for Standard EHL protocol (0x02/0x03)
         */
        fun standardEhl(
            interCommandDelayMs: Long = 100,
            responseTimeoutMs: Long = 2000
        ) = EhlProtocolConfig(
            variant = ProtocolVariant.STANDARD_EHL,
            interCommandDelayMs = interCommandDelayMs,
            responseTimeoutMs = responseTimeoutMs
        )
        
        /**
         * Create configuration for Norges Gass variant (0x10/0x20/0x36)
         */
        fun norgesGass(
            interCommandDelayMs: Long = 100,
            responseTimeoutMs: Long = 2000
        ) = EhlProtocolConfig(
            variant = ProtocolVariant.NORGES_GASS,
            interCommandDelayMs = interCommandDelayMs,
            responseTimeoutMs = responseTimeoutMs
        )
    }
}

/**
 * EHL Protocol Variants
 */
enum class ProtocolVariant(val description: String) {
    /**
     * Standard EHL Protocol
     * STX: 0x02, ETX: 0x03
     * Used by: Most European dispenser manufacturers
     */
    STANDARD_EHL("Standard EHL (0x02/0x03)"),
    
    /**
     * Norges Gass Variant
     * STX Controller: 0x10, STX Dispenser: 0x20, ETX: 0x36
     * Used by: Norges Gass installations (based on Python legacy code)
     */
    NORGES_GASS("Norges Gass Variant (0x10/0x20/0x36)")
}
