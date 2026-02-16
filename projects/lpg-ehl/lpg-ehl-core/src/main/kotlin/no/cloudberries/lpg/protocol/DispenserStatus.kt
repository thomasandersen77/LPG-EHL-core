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
     * Payment pending - transaction complete, totals frozen.
     * Requires reset/clear before new transaction can begin.
     * This is the state after STOP/BLOCK when totals are finalized.
     * 
     * State code: 8 (0x08)
     */
    data object PAYMENT_PENDING : DispenserStatus
    
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
 * VB6-Compatible Status Bit Masks for parsing 0x4B (STATE) response payload.
 * 
 * Based on VB6 legacy code (pumpekontroll.frm lines 2734-2805):
 * ```vb
 * state_string = decimaltobinn(x(4))
 * If Mid(state_string, 5, 1) = "1" Then disp_automode = True      ' bit3 = 0x08
 * If Mid(state_string, 6, 1) = "1" Then DISP_startbuttonpressed = True  ' bit2 = 0x04
 * If Mid(state_string, 7, 1) = "1" Then DISP_openfordelivery = True     ' bit1 = 0x02
 * ```
 * 
 * VB6 Bit Mapping:
 * - Bit 1 (0x02): Open for Delivery (DISP_openfordelivery)
 * - Bit 2 (0x04): Start Button Pressed (DISP_startbuttonpressed) 
 * - Bit 3 (0x08): Auto Mode (disp_automode)
 * - Bit 7 (0x80): Error Flag
 */
object StatusBitMasks {
    /** Bit 1: Dispenser is open for delivery / nozzle lifted */
    const val OPEN_FOR_DELIVERY: Int = 0x02
    
    /** Bit 2: Start button has been pressed */
    const val START_BUTTON_PRESSED: Int = 0x04
    
    /** Bit 3: Auto mode enabled */
    const val AUTOMODE: Int = 0x08
    
    /** Bit 7: Error condition */
    const val ERROR_FLAG: Int = 0x80
    
    // Legacy aliases for backward compatibility
    @Deprecated("Use START_BUTTON_PRESSED instead", ReplaceWith("START_BUTTON_PRESSED"))
    const val START_SWITCH_ACTIVE: Int = START_BUTTON_PRESSED
    
    @Deprecated("Use OPEN_FOR_DELIVERY instead", ReplaceWith("OPEN_FOR_DELIVERY"))
    const val NOZZLE_LIFTED: Int = OPEN_FOR_DELIVERY
    
    @Deprecated("Use AUTOMODE instead", ReplaceWith("AUTOMODE"))
    const val DELIVERY_IN_PROGRESS: Int = AUTOMODE
    
    @Deprecated("Use AUTOMODE instead - VB6 uses automode bit for transaction state", ReplaceWith("AUTOMODE"))
    const val TRANSACTION_COMPLETE: Int = AUTOMODE
    
    /**
     * Check if a specific bit is set in the status byte
     */
    fun isBitSet(statusByte: Byte, mask: Int): Boolean {
        return (statusByte.toInt() and mask) != 0
    }
    
    /**
     * Extract VB6-compatible bits as a boolean map
     */
    fun extractBits(statusByte: Byte): Map<String, Boolean> {
        return mapOf(
            "openForDelivery" to isBitSet(statusByte, OPEN_FOR_DELIVERY),
            "startButtonPressed" to isBitSet(statusByte, START_BUTTON_PRESSED),
            "automode" to isBitSet(statusByte, AUTOMODE),
            "error" to isBitSet(statusByte, ERROR_FLAG)
        )
    }
}
