package no.cloudberries.lpg.protocol

/**
 * Domain representation of dispenser physical status.
 * 
 * This is the Domain Model representation derived from the raw 0x4B (STATE_POLL) 
 * protocol response bytes. It represents the actual physical state of the pump hardware.
 * 
 * ## State Transitions (Happy Path):
 * IDLE → AUTHORIZED → PUMPING → STOPPED → IDLE
 * 
 * ## Protocol Mapping:
 * Raw byte flags from 0x4B response are interpreted by DispenserStateMapper
 * to produce these domain states.
 */
sealed interface DispenserStatus {
    /**
     * Pump is idle - nozzle holstered, no activity.
     * Ready to accept PRODUCT_SELECT command.
     * 
     * Protocol bits: Start Switch = 0, Nozzle Lifted = 0, Ready = 1
     */
    data object IDLE : DispenserStatus
    
    /**
     * Product selected and dispenser authorized for fueling.
     * Waiting for UNBLOCK command or nozzle lift.
     * 
     * Protocol bits: Start Switch = 1, Nozzle Lifted = 0, Ready = 1
     */
    data object AUTHORIZED : DispenserStatus
    
    /**
     * Active fueling in progress - volume incrementing.
     * Nozzle is lifted and fuel is flowing.
     * 
     * Protocol bits: Start Switch = 1, Nozzle Lifted = 1, Delivery Active = 1
     */
    data object PUMPING : DispenserStatus
    
    /**
     * Fueling stopped - either by BLOCK command or nozzle holstered.
     * Transaction data available via TRANSACTION_STATUS.
     * 
     * Protocol bits: Start Switch = 0, Delivery Active = 0, Transaction Complete = 1
     */
    data object STOPPED : DispenserStatus
    
    /**
     * Error or fault state - requires inspection.
     * Check ERROR_QUERY (0x4C) for details.
     * 
     * Protocol bits: Error Flag = 1
     */
    data class ERROR(val errorCode: Int = 0) : DispenserStatus
    
    /**
     * Unknown/unparseable state from protocol bytes.
     * Should never occur in production - indicates protocol mismatch.
     */
    data class UNKNOWN(val rawByte: Byte) : DispenserStatus
}

/**
 * Status bit masks for parsing 0x4B (STATE_POLL) response payload.
 * 
 * Based on legacy VB6 analysis:
 * - Bit 0 (0x01): Start Switch Active / Ready to Fuel
 * - Bit 1 (0x02): Nozzle Lifted
 * - Bit 2 (0x04): Delivery in Progress
 * - Bit 3 (0x08): Transaction Complete
 * - Bit 7 (0x80): Error Flag
 */
object StatusBitMasks {
    const val START_SWITCH_ACTIVE: Int = 0x01
    const val NOZZLE_LIFTED: Int = 0x02
    const val DELIVERY_IN_PROGRESS: Int = 0x04
    const val TRANSACTION_COMPLETE: Int = 0x08
    const val ERROR_FLAG: Int = 0x80
    
    /**
     * Check if a specific bit is set in the status byte
     */
    fun isBitSet(statusByte: Byte, mask: Int): Boolean {
        return (statusByte.toInt() and mask) != 0
    }
    
    /**
     * Extract multiple bits as a boolean map
     */
    fun extractBits(statusByte: Byte): Map<String, Boolean> {
        return mapOf(
            "startSwitch" to isBitSet(statusByte, START_SWITCH_ACTIVE),
            "nozzleLifted" to isBitSet(statusByte, NOZZLE_LIFTED),
            "deliveryActive" to isBitSet(statusByte, DELIVERY_IN_PROGRESS),
            "transactionComplete" to isBitSet(statusByte, TRANSACTION_COMPLETE),
            "error" to isBitSet(statusByte, ERROR_FLAG)
        )
    }
}
