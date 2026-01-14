package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

/**
 * EHL Dispenser Emulator - simulates a real LPG dispenser.
 * 
 * This emulator behaves like a physical pump:
 * - STOP/BLOCK freezes totals in PAYMENT_PENDING state
 * - UNBLOCK after STOP does not start new filling until reset/clear
 * - No race conditions - atomic stop mechanism
 * 
 * State machine: IDLE → AUTHORIZED → DELIVERING → PAYMENT_PENDING → IDLE
 * 
 * @param address Dispenser address (1-255)
 * @param pricePerLitreCents Price in cents (e.g., 1590 = 15.90 kr/L)
 * @param litresPerSecond Flow rate for simulation (default: 0.5 L/s)
 */
class EhlDispenserEmulator(
    private val address: Int = 1,
    pricePerLitreCents: Int = 1590,
    private val litresPerSecond: Double = 0.5
) {
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulator::class.java)
    
    // Mutable price - can be changed dynamically
    @Volatile
    var currentPricePerLitreCents: Int = pricePerLitreCents
        private set
    
    private var state: EmulatorState = EmulatorState.IDLE
    private var activeTx: ActiveTransaction? = null
    private var completedTx: CompletedTransaction? = null
    private val simulator = DispenserSimulator(litresPerSecond, pricePerLitreCents)
    
    init {
        require(address in 1..255) { "Address must be 1-255" }
        logger.info("EHL Dispenser Emulator initialized: address=$address, price=${currentPricePerLitreCents/100.0} kr/L")
    }
    
    /**
     * Update the price per litre.
     * Takes effect immediately for new transactions.
     */
    fun setPrice(pricePerLitreCents: Int) {
        logger.info("💰 Pris oppdatert: ${this.currentPricePerLitreCents/100.0} kr/L → ${pricePerLitreCents/100.0} kr/L")
        this.currentPricePerLitreCents = pricePerLitreCents
        simulator.updatePrice(pricePerLitreCents)
    }
    
    /**
     * Get the current price per litre in kr.
     */
    fun getPricePerLitreKr(): Double = currentPricePerLitreCents / 100.0
    
    /**
     * Process bytes from controller and return response packets.
     * 
     * @param bytes Raw bytes from controller
     * @return List of response packet bytes
     */
    fun onBytesFromHost(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()
        
        return when (val parsed = EhlCodec.decode(bytes)) {
            is EhlPacketParseResult.Success -> {
                logger.debug("Emulator received: ${EhlPacketFormatter.formatPacketForLogging(parsed.packet, EhlPacketFormatter.Direction.RECEIVING)}")
                handlePacket(parsed.packet).map { EhlCodec.encode(it) }
            }
            is EhlPacketParseResult.Incomplete -> {
                logger.warn("Emulator received incomplete packet")
                emptyList()
            }
            is EhlPacketParseResult.ChecksumError -> {
                logger.warn("Emulator checksum error: expected ${parsed.expected}, got ${parsed.actual}")
                listOf(EhlCodec.encode(buildErrorPacket(0x01))) // Checksum error
            }
            is EhlPacketParseResult.InvalidFormat -> {
                logger.warn("Emulator invalid format: ${parsed.reason}")
                listOf(EhlCodec.encode(buildErrorPacket(0x02))) // Format error
            }
        }
    }
    
    private fun handlePacket(packet: EhlPacket): List<EhlPacket> {
        if (packet.address != address) {
            // Wrong address - ignore
            return emptyList()
        }
        
        return when (packet.command) {
            EhlCommand.STATE -> listOf(buildStateResponse())
            EhlCommand.UNBLOCK -> handleUnblock()
            EhlCommand.STOP, EhlCommand.BLOCK -> handleStop()
            EhlCommand.VOLUME -> listOf(buildVolumeResponse())
            EhlCommand.PRICE -> listOf(buildPriceResponse())
            EhlCommand.PRODUCT_SELECT -> handleProductSelect()
            EhlCommand.LINETEST -> listOf(buildLinetestResponse())
            else -> {
                logger.warn("Unsupported command: ${packet.command}")
                listOf(buildErrorPacket(0x10)) // Unsupported command
            }
        }
    }
    
    /**
     * Handles product selection; transitions to authorized state
     */
    private fun handleProductSelect(): List<EhlPacket> {
        logger.info("Product selected")
        if (state == EmulatorState.IDLE) {
            state = EmulatorState.AUTHORIZED
            logger.info("State: IDLE → AUTHORIZED")
        }
        return listOf(
            EhlPacket(address, EhlCommand.OK),
            buildStateResponse()
        )
    }
    
    private fun handleUnblock(): List<EhlPacket> {
        return when (state) {
            EmulatorState.IDLE, EmulatorState.AUTHORIZED -> {
                // Start new transaction
                logger.info("UNBLOCK: Starting new transaction")
                val startMs = System.currentTimeMillis()
                activeTx = ActiveTransaction(startMs)
                state = EmulatorState.DELIVERING
                
                // Start simulation
                simulator.start(activeTx!!)
                
                logger.info("State: ${if (state == EmulatorState.IDLE) "IDLE" else "AUTHORIZED"} → DELIVERING")
                listOf(
                    EhlPacket(address, EhlCommand.OK),
                    buildStateResponse()
                )
            }
            EmulatorState.PAYMENT_PENDING -> {
                // CRITICAL: Deny start when payment pending
                logger.error("❌ UNBLOCK DENIED: Transaction awaiting payment (PAYMENT_PENDING)")
                logger.error("❌ Totals frozen: ${completedTx?.volumeLitres} L, ${completedTx?.amountCents?.let { it/100.0 }} kr")
                logger.error("❌ Must settle payment before starting new transaction")
                logger.error("❌ Call settle endpoint (/api/v1/emulator/settle/{dispenserId}?method=CARD|CREDIT) to complete payment")
                
                // Return deterministic response: ACK + PAYMENT_PENDING state
                listOf(
                    EhlPacket(address, EhlCommand.OK),
                    buildStateResponse() // Will show PAYMENT_PENDING
                )
            }
            EmulatorState.DELIVERING -> {
                // Already delivering
                logger.warn("UNBLOCK received while already delivering - ignoring")
                listOf(EhlPacket(address, EhlCommand.OK))
            }
        }
    }
    
    private fun handleStop(): List<EhlPacket> {
        return when (state) {
            EmulatorState.DELIVERING -> {
                // CRITICAL: Stop simulation atomically
                logger.info("STOP/BLOCK: Stopping delivery")
                simulator.stopImmediately()
                
                // Freeze totals in completedTx
                val active = activeTx!!
                val stopMs = System.currentTimeMillis()
                completedTx = CompletedTransaction(
                    id = "TX-${System.currentTimeMillis()}",
                    volumeLitres = active.volumeLitres,
                    amountCents = active.amountCents,
                    unitPriceCents = currentPricePerLitreCents,
                    startedAt = active.startMs,
                    stoppedAt = stopMs
                )
                
                logger.info("Transaction completed: ${completedTx!!.volumeLitres} L, ${completedTx!!.amountCents/100.0} kr")
                logger.info("Totals FROZEN - requires reset before next transaction")
                
                // Clear active, set PAYMENT_PENDING
                activeTx = null
                state = EmulatorState.PAYMENT_PENDING
                logger.info("State: DELIVERING → PAYMENT_PENDING")
                
                listOf(
                    EhlPacket(address, EhlCommand.OK),
                    buildStateResponse(),
                    buildVolumeResponse()
                )
            }
            else -> {
                logger.warn("STOP/BLOCK received in state $state - ignoring")
                listOf(EhlPacket(address, EhlCommand.OK))
            }
        }
    }
    
    private fun buildStateResponse(): EhlPacket {
        // Update active transaction if delivering
        if (state == EmulatorState.DELIVERING && activeTx != null) {
            val elapsedSeconds = (System.currentTimeMillis() - activeTx!!.startMs) / 1000.0
            activeTx!!.volumeLitres = elapsedSeconds * litresPerSecond
            activeTx!!.amountCents = (activeTx!!.volumeLitres * currentPricePerLitreCents).roundToInt()
        }
        
        // VB6-compatible state byte using bit flags:
        // Bit 1 (0x02): OPEN_FOR_DELIVERY - nozzle lifted, fuel flowing
        // Bit 2 (0x04): START_BUTTON_PRESSED - authorization active
        // Bit 3 (0x08): AUTOMODE - transaction complete/pending
        val stateCode = when (state) {
            EmulatorState.IDLE -> 0x00                        // No flags
            EmulatorState.AUTHORIZED -> 0x04                  // START_BUTTON_PRESSED
            EmulatorState.DELIVERING -> 0x06                  // START_BUTTON_PRESSED + OPEN_FOR_DELIVERY
            EmulatorState.PAYMENT_PENDING -> 0x08             // AUTOMODE (transaction complete)
        }
        
        val data = byteArrayOf(stateCode.toByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }
    
    private fun buildVolumeResponse(): EhlPacket {
        // Get volume in liters
        val volumeLitres = when {
            completedTx != null -> completedTx!!.volumeLitres
            activeTx != null -> activeTx!!.volumeLitres
            else -> 0.0
        }
        
        // VB6 format: 5 ASCII bytes LSB-first
        // Example: 45.50 L -> 4550 centilitres -> "04550" -> bytes ['0','5','5','4','0']
        val volumeCentilitres = (volumeLitres * 100).roundToInt().coerceIn(0, 99999)
        val volumeString = "%05d".format(volumeCentilitres)  // "04550"
        
        val data = ByteArray(5)
        for (i in 0..4) {
            // LSB-first: reverse the string order
            data[i] = volumeString[4 - i].code.toByte()
        }
        
        logger.debug("VOLUME response: $volumeLitres L -> '$volumeString' -> ${data.map { "%02X".format(it) }}")
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }
    
    private fun buildPriceResponse(): EhlPacket {
        // VB6 format: 4 ASCII bytes LSB-first
        // Example: 15.90 kr/L -> 1590 øre -> "1590" -> bytes ['0','9','5','1']
        // VB6 reads: dispris.Caption = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
        val priceString = "%04d".format(currentPricePerLitreCents.coerceIn(0, 9999))  // "1590"
        
        val data = ByteArray(4)
        for (i in 0..3) {
            // LSB-first: reverse the string order
            data[i] = priceString[3 - i].code.toByte()
        }
        
        logger.debug("PRICE response: $currentPricePerLitreCents øre -> '$priceString' -> ${data.map { "%02X".format(it) }}")
        return EhlPacket(address, EhlCommand.PRICE, data)
    }
    
    private fun buildLinetestResponse(): EhlPacket {
        // VB6 format: magic bytes 0x55 0xAA to confirm line is working
        // VB6 fra_dispenser.bas: y(5) = &H55, y(6) = &HAA
        val data = byteArrayOf(0x55, 0xAA.toByte())
        logger.debug("LINETEST response: 0x55 0xAA")
        return EhlPacket(address, EhlCommand.LINETEST, data)
    }
    
    private fun buildErrorPacket(code: Int): EhlPacket {
        return EhlPacket(address, EhlCommand.ERROR, byteArrayOf(code.toByte()))
    }
    
    // ============================================================
    // Admin API for reset/clear operations
    // ============================================================
    
    /**
     * Get current completed transaction (if any).
     */
    fun getCurrentTransaction(): CompletedTransaction? = completedTx
    
    /**
     * Get current state.
     */
    fun getCurrentState(): EmulatorState = state
    
    /**
     * Mark transaction as paid and reset to IDLE.
     * This simulates the operator clearing the pump after payment.
     */
    fun markTransactionPaid(): Boolean {
        if (state != EmulatorState.PAYMENT_PENDING || completedTx == null) {
            logger.warn("markTransactionPaid: No transaction pending (state=$state)")
            return false
        }
        
        logger.info("Transaction marked as PAID: ${completedTx!!.id}")
        logger.info("Final totals: ${completedTx!!.volumeLitres} L, ${completedTx!!.amountCents/100.0} kr")
        
        resetToIdle()
        return true
    }
    
    /**
     * Clear transaction without payment (for testing).
     */
    fun clearTransaction(): Boolean {
        if (state != EmulatorState.PAYMENT_PENDING || completedTx == null) {
            logger.warn("clearTransaction: No transaction pending (state=$state)")
            return false
        }
        
        logger.info("Transaction CLEARED: ${completedTx!!.id}")
        resetToIdle()
        return true
    }
    
    /**
     * Reset to IDLE state with cleared totals.
     */
    fun resetToIdle() {
        completedTx = null
        activeTx = null
        simulator.stopImmediately()
        state = EmulatorState.IDLE
        logger.info("Emulator reset to IDLE - ready for new transaction")
    }
}

/**
 * Emulator state machine.
 */
enum class EmulatorState {
    IDLE,           // No transaction, ready to start
    AUTHORIZED,     // Product selected, ready for UNBLOCK
    DELIVERING,     // Active delivery in progress
    PAYMENT_PENDING // Transaction complete, totals frozen, awaiting reset
}
