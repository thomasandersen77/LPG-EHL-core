package no.cloudberries.lpg.protocol

/**
 * LINETEST Response Validator
 * 
 * Validates the response from LINETEST command (0x6A / 106).
 * A valid response contains the magic bytes 0x55 0xAA.
 * 
 * Based on VB6/Python legacy implementation:
 * ```python
 * def on_linetest(self, data: bytes) -> None:
 *     if len(data) >= 2 and data[0] == 0x55 and data[1] == 0xAA:
 *         self.disp_init = True
 *     else:
 *         self.disp_init = False
 * ```
 * 
 * VB6 fra_dispenser.bas (emulator response):
 * ```vb
 * y(5) = &H55
 * y(6) = &HAA
 * ```
 */
object LinetestValidator {
    /** First magic byte: 0x55 */
    const val EXPECTED_BYTE_1: Byte = 0x55
    
    /** Second magic byte: 0xAA (as signed byte = -86) */
    val EXPECTED_BYTE_2: Byte = 0xAA.toByte()
    
    /**
     * Validate LINETEST response data.
     * 
     * A valid LINETEST response must contain the magic bytes 0x55 0xAA
     * which confirms that the communication line is working correctly.
     * 
     * @param data Raw data bytes from LINETEST response
     * @return true if response contains valid magic bytes (0x55 0xAA)
     */
    fun validateLinetestResponse(data: ByteArray): Boolean {
        return data.size >= 2 && 
               data[0] == EXPECTED_BYTE_1 && 
               data[1] == EXPECTED_BYTE_2
    }
    
    /**
     * Get a human-readable description of the validation result.
     * 
     * @param data Raw data bytes from LINETEST response
     * @return Description of validation result
     */
    fun getValidationDescription(data: ByteArray): String {
        return when {
            data.isEmpty() -> "LINETEST FAILED: No data received"
            data.size < 2 -> "LINETEST FAILED: Incomplete response (${data.size} bytes, need 2)"
            data[0] != EXPECTED_BYTE_1 -> "LINETEST FAILED: First byte 0x${"%02X".format(data[0])} != expected 0x55"
            data[1] != EXPECTED_BYTE_2 -> "LINETEST FAILED: Second byte 0x${"%02X".format(data[1])} != expected 0xAA"
            else -> "LINETEST OK: Communication verified (0x55 0xAA)"
        }
    }
}

/**
 * Result of LINETEST validation
 */
sealed class LinetestResult {
    /** Line test passed - communication is working */
    data object Success : LinetestResult()
    
    /** Line test failed - no data received */
    data object NoData : LinetestResult()
    
    /** Line test failed - incomplete response */
    data class IncompleteResponse(val bytesReceived: Int) : LinetestResult()
    
    /** Line test failed - invalid magic bytes */
    data class InvalidMagicBytes(val received: ByteArray) : LinetestResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InvalidMagicBytes) return false
            return received.contentEquals(other.received)
        }
        override fun hashCode(): Int = received.contentHashCode()
    }
}

/**
 * Parse LINETEST response and return structured result.
 */
fun LinetestValidator.parseLinetestResponse(data: ByteArray): LinetestResult {
    return when {
        data.isEmpty() -> LinetestResult.NoData
        data.size < 2 -> LinetestResult.IncompleteResponse(data.size)
        validateLinetestResponse(data) -> LinetestResult.Success
        else -> LinetestResult.InvalidMagicBytes(data.take(2).toByteArray())
    }
}
