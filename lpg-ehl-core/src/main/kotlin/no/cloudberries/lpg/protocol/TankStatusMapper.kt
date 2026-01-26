package no.cloudberries.lpg.protocol

/**
 * TANK Status from EHL command 197 (0xC5)
 * 
 * Maps raw protocol bytes to transaction status flags.
 * Based on VB6 legacy implementation (pumpekontroll.frm lines 2850-2590).
 * 
 * VB6 code:
 * ```vb
 * state_string_Tank = decimaltobinn_tank(x(4))
 * If CInt(Mid(state_string_Tank, 8, 1)) = 1 Then trans_finished_powerfault = True  ' bit0 = 0x01
 * If CInt(Mid(state_string_Tank, 5, 1)) = 1 Then trans_unaccounted = True          ' bit3 = 0x08
 * ```
 */
data class TankStatus(
    /** 
     * Transaction not yet accounted for (bit3 = 0x08)
     * 
     * VB6: trans_unaccounted
     * Indicates that a completed transaction has not been recorded/settled.
     */
    val transactionUnaccounted: Boolean,
    
    /** 
     * Transaction finished due to power fault (bit0 = 0x01)
     * 
     * VB6: trans_finished_powerfault
     * Indicates that the previous transaction ended abnormally due to power loss.
     */
    val transactionFinishedPowerFault: Boolean,
    
    /** Raw status byte for debugging */
    val rawByte: Byte
) {
    override fun toString(): String {
        return "TankStatus(unaccounted=$transactionUnaccounted, powerFault=$transactionFinishedPowerFault, raw=0x${"%02X".format(rawByte)})"
    }
}

/**
 * VB6-Compatible TANK Status Mapper
 * 
 * Parses the response from TANK command (0xC5 / 197) which provides
 * transaction completion status information.
 */
object TankStatusMapper {
    /** 
     * Bit 3 (0x08): Transaction unaccounted
     * 
     * VB6: Mid(state_string_Tank, 5, 1) = "1"
     * Set when a transaction has completed but not been accounted for.
     */
    const val TRANS_UNACCOUNTED = 0x08
    
    /** 
     * Bit 0 (0x01): Transaction finished due to power fault
     * 
     * VB6: Mid(state_string_Tank, 8, 1) = "1"  
     * Set when the previous transaction ended due to power failure.
     */
    const val TRANS_POWER_FAULT = 0x01
    
    /**
     * Parse TANK status response data.
     * 
     * @param data Raw data bytes from TANK (0xC5) response
     * @return TankStatus with parsed flags
     * @throws IllegalArgumentException if data is empty
     */
    fun parseTankStatus(data: ByteArray): TankStatus {
        require(data.isNotEmpty()) { "TANK status requires at least 1 byte" }
        
        val statusByte = data[0]
        val byteValue = statusByte.toInt() and 0xFF
        
        return TankStatus(
            transactionUnaccounted = (byteValue and TRANS_UNACCOUNTED) != 0,
            transactionFinishedPowerFault = (byteValue and TRANS_POWER_FAULT) != 0,
            rawByte = statusByte
        )
    }
    
    /**
     * Check if a specific bit is set in the status byte.
     */
    fun isBitSet(statusByte: Byte, mask: Int): Boolean {
        return (statusByte.toInt() and mask) != 0
    }
    
    /**
     * Extract all tank bits as a boolean map.
     */
    fun extractBits(statusByte: Byte): Map<String, Boolean> {
        return mapOf(
            "transactionUnaccounted" to isBitSet(statusByte, TRANS_UNACCOUNTED),
            "transactionFinishedPowerFault" to isBitSet(statusByte, TRANS_POWER_FAULT)
        )
    }
}
