package no.cloudberries.lpg.protocol

import org.slf4j.LoggerFactory

/**
 * Domain State Mapper - Translates raw EHL protocol bytes to domain DispenserStatus.
 * 
 * ## Responsibility:
 * - Parse the payload from 0x4B (STATE) responses
 * - Apply VB6-compatible bit-mask logic to extract hardware flags
 * - Map to clean domain states (IDLE, AUTHORIZED, PUMPING, STOPPED, ERROR)
 * 
 * ## Protocol Contract:
 * Input: Byte array from STATE (0x4B) response
 * Output: DispenserStatus sealed interface instance
 * 
 * ## VB6 Bit Mapping (pumpekontroll.frm lines 2734-2805):
 * ```vb
 * state_string = decimaltobinn(x(4))
 * If Mid(state_string, 5, 1) = "1" Then disp_automode = True           ' bit3 = 0x08
 * If Mid(state_string, 6, 1) = "1" Then DISP_startbuttonpressed = True ' bit2 = 0x04
 * If Mid(state_string, 7, 1) = "1" Then DISP_openfordelivery = True    ' bit1 = 0x02
 * ```
 * 
 * ## VB6-Compatible State Decision Logic:
 * ```
 * ERROR_FLAG (0x80) set                              → ERROR
 * AUTOMODE (0x08) set                                → PAYMENT_PENDING (trans complete)
 * START_BUTTON (0x04) && OPEN_FOR_DELIVERY (0x02)   → PUMPING
 * START_BUTTON (0x04) && !OPEN_FOR_DELIVERY         → AUTHORIZED
 * !START_BUTTON && !OPEN_FOR_DELIVERY               → IDLE
 * ```
 */
object DispenserStateMapper {
    private val logger = LoggerFactory.getLogger(DispenserStateMapper::class.java)
    
    /**
     * Map raw protocol bytes to domain DispenserStatus.
     * 
     * VB6-compatible implementation based on pumpekontroll.frm.
     * 
     * @param payload Byte array from 0x4B (STATE) response
     * @return DispenserStatus - the interpreted domain state
     * 
     * @throws IllegalArgumentException if payload is empty
     */
    fun mapToDispenserStatus(payload: ByteArray): DispenserStatus {
        if (payload.isEmpty()) {
            logger.warn("Empty payload for STATE response - returning UNKNOWN")
            return DispenserStatus.UNKNOWN(0x00)
        }
        
        // First byte contains the status flags
        val statusByte = payload[0]
        
        // Extract VB6-compatible bit flags
        val hasError = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.ERROR_FLAG)
        val automode = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.AUTOMODE)
        val startButtonPressed = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.START_BUTTON_PRESSED)
        val openForDelivery = StatusBitMasks.isBitSet(statusByte, StatusBitMasks.OPEN_FOR_DELIVERY)
        
        // Log bit state for debugging (trace level)
        if (logger.isTraceEnabled) {
            val bits = StatusBitMasks.extractBits(statusByte)
            logger.trace("Status byte 0x${"%02X".format(statusByte)}: $bits")
        }
        
        // VB6-compatible decision tree - order matters!
        return when {
            // Priority 1: Error state always takes precedence
            hasError -> {
                val errorCode = if (payload.size > 1) payload[1].toInt() else 0
                logger.debug("Error state detected: code=$errorCode")
                DispenserStatus.ERROR(errorCode)
            }
            
            // Priority 2: Automode bit indicates transaction state
            // VB6: When trans_unaccounted or trans_finished, automode bit is often set
            // This represents PAYMENT_PENDING state where totals are frozen
            automode && !startButtonPressed && !openForDelivery -> {
                logger.debug("Automode set, no activity - PAYMENT_PENDING state")
                DispenserStatus.PAYMENT_PENDING
            }
            
            // Priority 3: Active delivery (pumping)
            // VB6: DISP_startbuttonpressed AND DISP_openfordelivery
            startButtonPressed && openForDelivery -> {
                logger.debug("Start button + open for delivery - PUMPING state")
                DispenserStatus.PUMPING
            }
            
            // Priority 4: Authorized but not yet pumping
            // VB6: DISP_startbuttonpressed AND NOT DISP_openfordelivery
            startButtonPressed && !openForDelivery -> {
                logger.debug("Start button pressed, waiting for delivery - AUTHORIZED state")
                DispenserStatus.AUTHORIZED
            }
            
            // Priority 5: Idle - default state when nothing is happening
            // VB6: NOT DISP_startbuttonpressed AND NOT DISP_openfordelivery
            !startButtonPressed && !openForDelivery -> {
                logger.debug("All flags clear - IDLE state")
                DispenserStatus.IDLE
            }
            
            // Fallback: Unknown bit combination (e.g., openForDelivery without startButton)
            else -> {
                logger.warn("Unknown bit combination in status byte: 0x${"%02X".format(statusByte)}")
                DispenserStatus.UNKNOWN(statusByte)
            }
        }
    }
    
    /**
     * Convenience method to map from EhlPacket response directly.
     * Validates that the packet is a STATE_POLL response.
     * 
     * @param packet EhlPacket from dispenser
     * @return DispenserStatus or UNKNOWN if packet is not STATE_POLL
     */
    fun mapFromPacket(packet: EhlPacket): DispenserStatus {
        if (packet.command != EhlCommand.STATE) {
            logger.warn("mapFromPacket called with non-STATE command: ${packet.command.name}")
            return DispenserStatus.UNKNOWN(0x00)
        }
        
        return mapToDispenserStatus(packet.data)
    }
    
    /**
     * Validate if a state transition is valid according to the state machine.
     * Useful for detecting protocol violations or hardware malfunctions.
     * 
     * @param from Current state
     * @param to New state
     * @return true if transition is valid
     */
    fun isValidTransition(from: DispenserStatus, to: DispenserStatus): Boolean {
        return when (from) {
            is DispenserStatus.IDLE -> to is DispenserStatus.AUTHORIZED || to is DispenserStatus.ERROR
            is DispenserStatus.AUTHORIZED -> to is DispenserStatus.PUMPING || to is DispenserStatus.IDLE || to is DispenserStatus.ERROR
            is DispenserStatus.PUMPING -> to is DispenserStatus.STOPPED || to is DispenserStatus.PAYMENT_PENDING || to is DispenserStatus.ERROR
            is DispenserStatus.STOPPED -> to is DispenserStatus.IDLE || to is DispenserStatus.PAYMENT_PENDING || to is DispenserStatus.ERROR
            is DispenserStatus.PAYMENT_PENDING -> to is DispenserStatus.IDLE || to is DispenserStatus.ERROR  // Only IDLE after reset
            is DispenserStatus.ERROR -> to is DispenserStatus.IDLE  // Can recover to IDLE
            is DispenserStatus.UNKNOWN -> true  // Allow any transition from UNKNOWN
        }
    }
}
