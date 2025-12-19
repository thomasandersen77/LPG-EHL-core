package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

/**
 * Emulator for an EHL-protocol LPG dispenser.
 * 
 * This emulator simulates a physical dispenser's behavior, including:
 * - State machine (IDLE → AUTHORIZED → PUMPING → STOPPED)
 * - EHL protocol packet handling (STATE, UNBLOCK, STOP, VOLUME)
 * - Simulated fuel delivery with configurable flow rate
 * - Checksum validation and error responses
 * - VB6-compatible bit-flag responses for STATE command
 * 
 * ## State Machine:
 * ```
 * IDLE ──(PRODUCT_SELECT)──> AUTHORIZED ──(UNBLOCK + nozzle lift)──> PUMPING
 *   ↑                                                                    │
 *   └────────────────────(ZER/RESET)────────────────── STOPPED <────────┘
 * ```
 * 
 * ## Bit-Flags for STATE Response (VB6 Compatible):
 * - Bit 0 (0x01): Start Switch Active - Ready for fuel
 * - Bit 1 (0x02): Nozzle Lifted
 * - Bit 2 (0x04): Delivery in Progress
 * - Bit 3 (0x08): Transaction Complete
 * - Bit 7 (0x80): Error Flag
 * 
 * @property address Dispenser address (1-255)
 * @property pricePerLitreCents Price per litre in cents (øre)
 * @property litresPerSecond Simulated flow rate for testing
 */
class EhlDispenserEmulator(
    private val address: Int = 1,
    private val pricePerLitreCents: Int = 1126,      // 11.26 kr/l
    private val litresPerSecond: Double = 0.5,       // Simulated flow rate
    // Fault injection toggles for testing
    var disconnectAfterSeconds: Double? = null,      // Simulate disconnect after N seconds
    var badChecksumRate: Double = 0.0,               // Probability of corrupted response (0.0-1.0)
    var powerfaultAfterSeconds: Double? = null       // Simulate powerfault after N seconds
) {
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulator::class.java)

    private var state: DispenserState = DispenserState.IDLE
    private var startedAtMs: Long? = null
    private var nozzleLifted: Boolean = false        // Track nozzle state separately
    private var productSelected: Boolean = false     // Track if product was selected

    private var volumeLitres: Double = 0.0
    private var amountCents: Int = 0
    private var currentPricePerLitreCents: Int = pricePerLitreCents
    
    // Fault injection state
    private var disconnected: Boolean = false

    /**
     * Dispenser state machine states.
     * Maps to VB6 protocol states and DispenserStateMapper domain states.
     */
    enum class DispenserState {
        /** Pump idle - nozzle holstered, no activity */
        IDLE,
        /** Product selected, authorized for fueling - waiting for UNBLOCK + nozzle lift */
        AUTHORIZED,
        /** Active fuel delivery - volume incrementing */
        PUMPING,
        /** Delivery stopped - transaction data available */
        STOPPED,
        /** Error condition */
        ERROR
    }
    
    /**
     * Build the status byte using VB6-compatible bit-flags.
     * This ensures DispenserStateMapper can correctly interpret the response.
     */
    private fun buildStatusByte(): Byte {
        var statusByte = 0
        
        when (state) {
            DispenserState.IDLE -> {
                // All flags clear = IDLE
                statusByte = 0x00
            }
            DispenserState.AUTHORIZED -> {
                // Start switch active, nozzle NOT lifted = AUTHORIZED
                statusByte = 0x01  // START_SWITCH_ACTIVE
            }
            DispenserState.PUMPING -> {
                // Start switch + nozzle lifted + delivery active = PUMPING
                statusByte = 0x01 or 0x02 or 0x04  // START_SWITCH + NOZZLE_LIFTED + DELIVERY_ACTIVE
            }
            DispenserState.STOPPED -> {
                // Transaction complete flag set = STOPPED
                statusByte = 0x08  // TRANSACTION_COMPLETE
            }
            DispenserState.ERROR -> {
                // Error flag set
                statusByte = 0x80  // ERROR_FLAG
            }
        }
        
        // Override with actual nozzle state if lifted during AUTHORIZED
        if (nozzleLifted && state == DispenserState.AUTHORIZED) {
            statusByte = statusByte or 0x02  // Add NOZZLE_LIFTED
        }
        
        return statusByte.toByte()
    }

    /**
     * Reset emulator to initial state.
     */
    fun reset() {
        state = DispenserState.IDLE
        startedAtMs = null
        nozzleLifted = false
        productSelected = false
        volumeLitres = 0.0
        amountCents = 0
        disconnected = false
    }
    
    /**
     * Simulate nozzle lift/holster.
     * Used for testing the complete fueling lifecycle.
     * 
     * @param lifted true = nozzle lifted from holster, false = nozzle holstered
     */
    fun simulateNozzleLift(lifted: Boolean) {
        val previousNozzle = nozzleLifted
        nozzleLifted = lifted
        
        if (lifted && !previousNozzle) {
            logger.info("🚰 NOZZLE LIFTED - Ready to pump")
            // If authorized and nozzle lifted, transition to PUMPING
            if (state == DispenserState.AUTHORIZED) {
                state = DispenserState.PUMPING
                startedAtMs = System.currentTimeMillis()
                volumeLitres = 0.0
                amountCents = 0
                logger.info(EhlPacketFormatter.formatStateTransition(
                    "AUTHORIZED",
                    "PUMPING",
                    "Nozzle lifted while authorized"
                ))
            }
        } else if (!lifted && previousNozzle) {
            logger.info("🚰 NOZZLE HOLSTERED")
            // If pumping and nozzle holstered, stop delivery
            if (state == DispenserState.PUMPING) {
                updateDelivery()
                state = DispenserState.STOPPED
                logger.info(EhlPacketFormatter.formatStateTransition(
                    "PUMPING",
                    "STOPPED",
                    "Nozzle holstered during delivery"
                ))
            }
        }
    }
    
    /**
     * Check if emulator should simulate disconnect (fault injection).
     */
    private fun shouldSimulateDisconnect(): Boolean {
        val disconnectAt = disconnectAfterSeconds ?: return false
        val start = startedAtMs ?: return false
        val elapsed = (System.currentTimeMillis() - start) / 1000.0
        return elapsed >= disconnectAt
    }
    
    /**
     * Check if emulator should simulate powerfault (fault injection).
     */
    private fun shouldSimulatePowerfault(): Boolean {
        val powerfaultAt = powerfaultAfterSeconds ?: return false
        val start = startedAtMs ?: return false
        val elapsed = (System.currentTimeMillis() - start) / 1000.0
        return elapsed >= powerfaultAt
    }

    /**
     * Process raw bytes from the controller and return response packets.
     * 
     * @param bytes Raw bytes received from controller
     * @return List of raw response packets to send back
     */
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()

        return when (val parsed = EhlCodec.decode(bytes)) {
            is EhlPacketParseResult.Success -> {
                logger.info("\n" + "=".repeat(80))
                logger.info(EhlPacketFormatter.formatPacketForLogging(
                    parsed.packet,
                    EhlPacketFormatter.Direction.RECEIVING
                ))
                
                val responses = handlePacket(parsed.packet)
                responses.forEach { response ->
                    logger.info(EhlPacketFormatter.formatPacketForLogging(
                        response,
                        EhlPacketFormatter.Direction.SENDING
                    ))
                }
                logger.info("=".repeat(80))
                
                responses.map { EhlCodec.encode(it, fromController = false) }
            }
            is EhlPacketParseResult.Incomplete -> {
                logger.warn("⚠️ EMULATOR: Received incomplete packet (${bytes.size} bytes)")
                emptyList()
            }
            is EhlPacketParseResult.ChecksumError -> {
                logger.warn(EhlPacketFormatter.formatError(
                    "EMULATOR Checksum Error",
                    "Expected 0x%02X, got 0x%02X".format(parsed.expected, parsed.actual)
                ))
                listOf(EhlCodec.encode(buildErrorPacket(0x01), fromController = false))
            }
            is EhlPacketParseResult.InvalidFormat -> {
                logger.warn(EhlPacketFormatter.formatError(
                    "EMULATOR Invalid Format",
                    parsed.reason
                ))
                listOf(EhlCodec.encode(buildErrorPacket(0x02), fromController = false))
            }
        }
    }

    private fun handlePacket(packet: EhlPacket): List<EhlPacket> {
        if (packet.address != address) {
            logger.warn("📪 IGNORED: Packet addressed to #${packet.address} (I am #$address)")
            return emptyList()
        }

        return when (packet.command) {
            EhlCommand.STATE     -> {
                logger.info("📊 Processing STATE query")
                listOf(buildStateResponse())
            }
            EhlCommand.UNBLOCK   -> handleUnblock(packet)
            EhlCommand.STOP      -> handleStop(packet)
            EhlCommand.BLOCK     -> handleBlock(packet)
            EhlCommand.VOLUME    -> {
                updateDelivery() // Update live values
                logger.info("📊 Processing VOLUME query")
                listOf(buildVolumeResponse())
            }
            EhlCommand.PRICE     -> {
                logger.info("📊 Processing PRICE query")
                listOf(buildPriceResponse())
            }
            EhlCommand.PROG_PRC     -> handlePriceProgram(packet)
            EhlCommand.PROG_AMOUNT  -> handleAmountPreset(packet)
            EhlCommand.PROG_VOLUME  -> handleVolumePreset(packet)
            EhlCommand.ERROR_QUERY  -> {
                logger.info("🔍 Processing ERROR_QUERY")
                // VB6 format: 2 ASCII bytes (main code + sub code)
                listOf(EhlPacket(address, EhlCommand.ERROR, byteArrayOf('0'.code.toByte(), '0'.code.toByte()))) // No error: "00"
            }
            EhlCommand.TANK      -> {
                logger.info("🛢️ Processing TANK query")
                listOf(buildTankResponse())
            }
            EhlCommand.PRODUCT_SELECT -> handleProductSelect(packet)
            EhlCommand.LINETEST  -> {
                logger.info("🔌 Processing LINETEST - communication OK")
                listOf(EhlPacket(address, EhlCommand.OK))
            }
            EhlCommand.ZER       -> handleReset(packet)
            else                 -> {
                logger.warn("⚠️ Unsupported command: ${packet.command.name} (code=${packet.command.code})")
                listOf(buildErrorPacket(0x10))
            }
        }
    }

    private fun handleUnblock(packet: EhlPacket): List<EhlPacket> {
        val previousState = state.name
        
        // VB6 flow: UNBLOCK enables fuel delivery
        // If nozzle is already lifted → go directly to PUMPING
        // If nozzle is NOT lifted → go to AUTHORIZED (wait for lift)
        when (state) {
            DispenserState.IDLE, DispenserState.AUTHORIZED, DispenserState.STOPPED -> {
                if (nozzleLifted) {
                    // Nozzle already up - start pumping immediately
                    state = DispenserState.PUMPING
                    startedAtMs = System.currentTimeMillis()
                    volumeLitres = 0.0
                    amountCents = 0
                    
                    logger.info(EhlPacketFormatter.formatStateTransition(
                        previousState,
                        state.name,
                        "UNBLOCK + nozzle lifted → PUMPING"
                    ))
                    logger.info("🚀 DELIVERY STARTED: Price=%.2f kr/L | Flow rate=%.2f L/s".format(
                        currentPricePerLitreCents / 100.0,
                        litresPerSecond
                    ))
                } else {
                    // Nozzle down - wait in AUTHORIZED state
                    state = DispenserState.AUTHORIZED
                    productSelected = true
                    
                    logger.info(EhlPacketFormatter.formatStateTransition(
                        previousState,
                        state.name,
                        "UNBLOCK received - waiting for nozzle lift"
                    ))
                    logger.info("✅ AUTHORIZED: Waiting for customer to lift nozzle")
                }
            }
            DispenserState.PUMPING -> {
                logger.warn("⚠️ UNBLOCK ignored - already pumping")
            }
            DispenserState.ERROR -> {
                logger.warn("⚠️ UNBLOCK ignored - dispenser in ERROR state")
            }
        }
        
        // Respond with OK + STATE
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }

    private fun handleStop(packet: EhlPacket): List<EhlPacket> {
        val previousState = state.name
        
        if (state == DispenserState.PUMPING) {
            updateDelivery() // Calculate final volume/amount
            state = DispenserState.STOPPED
            nozzleLifted = false
            
            logger.info(EhlPacketFormatter.formatStateTransition(
                previousState,
                state.name,
                "STOP command received"
            ))
            logger.info(EhlPacketFormatter.formatDeliveryProgress(
                volumeLitres,
                amountCents,
                currentPricePerLitreCents
            ))
            logger.info("🏁 DELIVERY FINISHED: %.2f L delivered for %.2f kr".format(
                volumeLitres,
                amountCents / 100.0
            ))
        } else {
            logger.warn("⚠️ STOP received but not pumping (state=$previousState)")
        }
        
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse(),
            buildVolumeResponse()
        )
    }
    
    private fun handleBlock(packet: EhlPacket): List<EhlPacket> {
        val previousState = state.name
        
        // BLOCK stops delivery and returns to IDLE
        when (state) {
            DispenserState.PUMPING -> {
                updateDelivery() // Calculate final volume/amount
                state = DispenserState.STOPPED
                nozzleLifted = false
                
                logger.info(EhlPacketFormatter.formatStateTransition(
                    previousState,
                    state.name,
                    "BLOCK command during pumping"
                ))
                logger.info("🛑 DELIVERY BLOCKED: %.2f L | %.2f kr".format(
                    volumeLitres,
                    amountCents / 100.0
                ))
            }
            DispenserState.AUTHORIZED -> {
                state = DispenserState.IDLE
                productSelected = false
                logger.info(EhlPacketFormatter.formatStateTransition(
                    previousState,
                    state.name,
                    "BLOCK command - authorization cancelled"
                ))
            }
            else -> {
                state = DispenserState.IDLE
                productSelected = false
                nozzleLifted = false
                logger.info(EhlPacketFormatter.formatStateTransition(
                    previousState,
                    state.name,
                    "BLOCK command - dispenser blocked"
                ))
            }
        }
        
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }
    
    private fun handlePriceProgram(packet: EhlPacket): List<EhlPacket> {
        if (packet.data.size != 4) {
            logger.warn(EhlPacketFormatter.formatError(
                "Invalid PROG_PRC Data",
                "Expected 4 bytes, got ${packet.data.size}"
            ))
            return listOf(buildErrorPacket(0x03))
        }
        
        // Parse price from ASCII digits (reversed: pennies, dimes, ones, tens)
        try {
            val digit1 = (packet.data[3].toInt() and 0xFF).toChar()
            val digit2 = (packet.data[2].toInt() and 0xFF).toChar()
            val digit3 = (packet.data[1].toInt() and 0xFF).toChar()
            val digit4 = (packet.data[0].toInt() and 0xFF).toChar()
            
            if (!digit1.isDigit() || !digit2.isDigit() || !digit3.isDigit() || !digit4.isDigit()) {
                logger.warn(EhlPacketFormatter.formatError(
                    "Invalid Price Format",
                    "Non-digit ASCII characters in price data"
                ))
                return listOf(buildErrorPacket(0x04))
            }
            
            val oldPrice = currentPricePerLitreCents / 100.0
            val priceString = "$digit1$digit2.$digit3$digit4"
            currentPricePerLitreCents = (priceString.toDouble() * 100).toInt()
            
            logger.info("💰 PRICE PROGRAMMED: %.2f kr/L → %.2f kr/L".format(oldPrice, currentPricePerLitreCents / 100.0))
            
            return listOf(
                EhlPacket(address, EhlCommand.OK),
                buildPriceResponse()
            )
        } catch (e: Exception) {
            logger.error(EhlPacketFormatter.formatError(
                "Price Parse Failed",
                e.message ?: "Unknown error"
            ))
            return listOf(buildErrorPacket(0x04))
        }
    }
    
    private fun handleAmountPreset(packet: EhlPacket): List<EhlPacket> {
        // PROG_AMOUNT (VB6: &H75): Program amount preset (5 ASCII bytes, LSB-first)
        if (packet.data.size == 5) {
            // Decode VB6-style LSB-first ASCII digits
            val digits = packet.data.map { (it.toInt() and 0xFF).toChar() }.reversed().joinToString("")
            logger.info("💳 AMOUNT PRESET (VB6): $digits øre - Acknowledged but not enforced in emulator")
        } else {
            val hex = packet.data.joinToString("") { "%02X".format(it) }
            logger.info("💳 AMOUNT PRESET: $hex - Acknowledged but not enforced in emulator")
        }
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleVolumePreset(packet: EhlPacket): List<EhlPacket> {
        // PROG_VOLUME (VB6: &H70): Program volume preset (6 ASCII bytes, LSB-first)
        if (packet.data.size == 6) {
            // Decode VB6-style LSB-first ASCII digits
            val digits = packet.data.map { (it.toInt() and 0xFF).toChar() }.reversed().joinToString("")
            logger.info("⛽ VOLUME PRESET (VB6): $digits (hundredths L) - Acknowledged but not enforced in emulator")
        } else {
            val hex = packet.data.joinToString(" ") { "%02X".format(it) }
            logger.info("⛽ VOLUME PRESET: Volume=$hex - Acknowledged but not enforced in emulator")
        }
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleProductSelect(packet: EhlPacket): List<EhlPacket> {
        // PRODUCT_SELECT (VB6: 0xC3): Product/pistol selection
        if (packet.data.size == 1) {
            val product = (packet.data[0].toInt() and 0xFF).toChar()
            logger.info("🧑‍💼 PRODUCT SELECT: Product '$product' selected - Acknowledged")
        } else {
            logger.info("🧑‍💼 PRODUCT SELECT: Invalid data size ${packet.data.size}, expected 1 byte")
        }
        // VB6 doesn't explicitly handle response, just acknowledge
        return listOf(EhlPacket(address, EhlCommand.OK))
    }
    
    private fun handleReset(packet: EhlPacket): List<EhlPacket> {
        val previousState = state.name
        reset()
        
        logger.info(EhlPacketFormatter.formatStateTransition(
            previousState,
            state.name,
            "ZER (Reset) command received"
        ))
        logger.info("🔄 DISPENSER RESET: All counters cleared, state → IDLE")
        
        // VB6 expects RESET response with 1 data-byte = 0x1E (OK)
        return listOf(
            EhlPacket(address, EhlCommand.ZER, byteArrayOf(0x1E)),
            buildStateResponse()
        )
    }

    /**
     * Update volume and amount based on time since delivery started.
     * This simulates fuel flowing at the configured rate.
     */
    private fun updateDelivery() {
        val start = startedAtMs ?: return
        val seconds = (System.currentTimeMillis() - start) / 1000.0
        volumeLitres = (seconds * litresPerSecond).coerceAtLeast(0.0)
        amountCents = (volumeLitres * currentPricePerLitreCents).roundToInt()
    }

    private fun buildStateResponse(): EhlPacket {
        // Update during pumping for "live" status
        if (state == DispenserState.PUMPING) {
            updateDelivery()
            if (logger.isDebugEnabled) {
                logger.debug(EhlPacketFormatter.formatDeliveryProgress(
                    volumeLitres,
                    amountCents,
                    currentPricePerLitreCents
                ))
            }
        }
        
        // Use VB6-compatible bit-flags instead of simple state codes
        val data = byteArrayOf(buildStatusByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }

    private fun buildVolumeResponse(): EhlPacket {
        // Update during pumping for "live" volume
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        // Format: volume in deciliters (2 bytes) + amount in cents (2 bytes)
        val volDeci = (volumeLitres * 10).roundToInt()
        val data = ByteArray(4)
        data[0] = ((volDeci shr 8) and 0xFF).toByte()
        data[1] = (volDeci and 0xFF).toByte()
        data[2] = ((amountCents shr 8) and 0xFF).toByte()
        data[3] = (amountCents and 0xFF).toByte()
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }

    private fun buildPriceResponse(): EhlPacket {
        // Format: Price as 4 ASCII digits (reversed: pennies, dimes, ones, tens)
        val priceString = "%.2f".format(currentPricePerLitreCents / 100.0)
        val parts = priceString.split(".")
        val data = byteArrayOf(
            parts[1][1].code.toByte(),  // Pennies
            parts[1][0].code.toByte(),  // Dimes
            parts[0][parts[0].length - 1].code.toByte(),  // Ones
            parts[0][parts[0].length - 2].code.toByte()   // Tens
        )
        return EhlPacket(address, EhlCommand.PRICE, data)
    }
    
    private fun buildTankResponse(): EhlPacket {
        // VB6 TANK response format
        // Bit 0 (0x01): trans_finished_powerfault
        // Bit 3 (0x08): trans_unaccounted
        var tankStatus = 0x00
        
        // Set trans_unaccounted bit when delivery is stopped but not reset
        if (state == DispenserState.STOPPED && volumeLitres > 0) {
            tankStatus = tankStatus or 0x08
        }
        
        // Simulate powerfault if configured
        if (shouldSimulatePowerfault()) {
            tankStatus = tankStatus or 0x01  // trans_finished_powerfault
            tankStatus = tankStatus or 0x08  // trans_unaccounted
            logger.warn("💥 SIMULATED POWERFAULT - Transaction unaccounted!")
        }
        
        val data = byteArrayOf(tankStatus.toByte())
        return EhlPacket(address, EhlCommand.TANK, data)
    }
    
    private fun buildErrorPacket(code: Int): EhlPacket {
        val data = byteArrayOf(code.toByte())
        return EhlPacket(address, EhlCommand.ERROR, data)
    }
}
