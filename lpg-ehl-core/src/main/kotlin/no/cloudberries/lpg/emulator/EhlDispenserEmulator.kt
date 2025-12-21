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
    private val pricePerLitreCents: Int = 1590,
    private val litresPerSecond: Double = 0.5
) {
    private val logger = LoggerFactory.getLogger(EhlDispenserEmulator::class.java)
    
    private var state: EmulatorState = EmulatorState.IDLE
    private var activeTx: ActiveTransaction? = null
    private var completedTx: CompletedTransaction? = null
    private val simulator = DispenserSimulator(litresPerSecond, pricePerLitreCents)
    
    init {
        require(address in 1..255) { "Address must be 1-255" }
        logger.info("EHL Dispenser Emulator initialized: address=$address, price=${pricePerLitreCents/100.0} kr/L")
    }
    
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
            else -> {
                logger.warn("Unsupported command: ${packet.command}")
                listOf(buildErrorPacket(0x10)) // Unsupported command
            }
        }
    }
    
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
                logger.warn("UNBLOCK denied: Transaction awaiting payment (PAYMENT_PENDING)")
                logger.warn("Totals frozen: ${completedTx?.volumeLitres} L, ${completedTx?.amountCents?.let { it/100.0 }} kr")
                logger.warn("Call markPaid() or clear() to reset before new transaction")
                
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
                    unitPriceCents = pricePerLitreCents,
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
            activeTx!!.amountCents = (activeTx!!.volumeLitres * pricePerLitreCents).roundToInt()
        }
        
        val stateCode = when (state) {
            EmulatorState.IDLE -> 0x00
            EmulatorState.AUTHORIZED -> 0x01
            EmulatorState.DELIVERING -> 0x02
            EmulatorState.PAYMENT_PENDING -> 0x08  // State code 8
        }
        
        val data = byteArrayOf(stateCode.toByte())
        return EhlPacket(address, EhlCommand.STATE, data)
    }
    
    private fun buildVolumeResponse(): EhlPacket {
        val (volDeci, amount) = when {
            completedTx != null -> {
                // Return frozen totals from completed transaction
                val vol = (completedTx!!.volumeLitres * 10).roundToInt()
                Pair(vol, completedTx!!.amountCents)
            }
            activeTx != null -> {
                // Return current totals from active transaction
                val vol = (activeTx!!.volumeLitres * 10).roundToInt()
                Pair(vol, activeTx!!.amountCents)
            }
            else -> {
                // No transaction - return zeros
                Pair(0, 0)
            }
        }
        
        val data = ByteArray(4)
        data[0] = ((volDeci shr 8) and 0xFF).toByte()
        data[1] = (volDeci and 0xFF).toByte()
        data[2] = ((amount shr 8) and 0xFF).toByte()
        data[3] = (amount and 0xFF).toByte()
        
        return EhlPacket(address, EhlCommand.VOLUME, data)
    }
    
    private fun buildPriceResponse(): EhlPacket {
        // Return current price
        val data = ByteArray(2)
        data[0] = ((pricePerLitreCents shr 8) and 0xFF).toByte()
        data[1] = (pricePerLitreCents and 0xFF).toByte()
        return EhlPacket(address, EhlCommand.PRICE, data)
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
