package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.emulator.service.CompletedTransaction
import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import java.time.Instant
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
 * ## Multi-Station Support:
 * Each emulator instance represents a specific dispenser at a specific station.
 * 
 * @property stationId Station identifier (e.g., "S001") - from environment
 * @property edgeId Edge device identifier (unique per station) - from environment
 * @property dispenserId Dispenser ID (e.g., "D001") - from environment
 * @property address Dispenser EHL address (1-255) - for protocol compatibility
 * @property pricePerLitreCents Price per litre in cents (øre)
 * @property litresPerSecond Simulated flow rate for testing
 */
class EhlDispenserEmulator(
    private val stationId: String = System.getenv("STATION_ID") ?: "S000",
    private val edgeId: String = System.getenv("EDGE_ID") ?: "EDGE-UNKNOWN",
    private val dispenserId: String = System.getenv("DISPENSER_ID") ?: "D001",
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
    
    /**
     * Update the price per litre.
     * Only takes effect for FUTURE deliveries (not ongoing ones).
     * 
     * @param newPriceCents New price in cents (øre)
     */
    fun setPrice(newPriceCents: Int) {
        currentPricePerLitreCents = newPriceCents
        logger.info("💰 Emulator price updated to ${newPriceCents / 100.0} NOK/L")
    }
    
    /**
     * Get current price per litre in cents.
     */
    fun getPriceCents(): Int = currentPricePerLitreCents
    
    /**
     * Get current price per litre in kr.
     */
    fun getPricePerLitreKr(): Double = currentPricePerLitreCents / 100.0
    
    // Heartbeat counter for periodic logging
    @Volatile
    private var commandCount: Long = 0
    private var lastHeartbeatTime: Long = System.currentTimeMillis()
    
    /**
     * Log heartbeat status (called periodically by external scheduler).
     * Shows current state, volume, price, and command count.
     */
    fun logHeartbeat() {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastHeartbeatTime) / 1000
        lastHeartbeatTime = now
        
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        
        logger.info("━━━━━━━━━━━━━ SIMULATOR HEARTBEAT ━━━━━━━━━━━━━━")
        logger.info("📍 Station: $stationId | Dispenser: $dispenserId (#$address)")
        logger.info("🟢 State: ${state.name} | Raw: 0x%02X".format(buildStatusByte().toInt() and 0xFF))
        logger.info("⛽ Volume: %.2f L | Amount: %.2f kr".format(volumeLitres, amountCents / 100.0))
        logger.info("🏷️ Price: %.2f kr/L".format(currentPricePerLitreCents / 100.0))
        logger.info("🛠️ Nozzle: ${if (nozzleLifted) "LIFTED" else "holstered"} | Blocked: ${state == DispenserState.IDLE}")
        logger.info("📦 Commands processed: $commandCount")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    // Transaction freeze for payment pending
    @Volatile
    private var pendingTransaction: CompletedTransaction? = null
    
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
        /** Payment pending - transaction frozen, waiting for settlement */
        PAYMENT_PENDING,
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
            DispenserState.PAYMENT_PENDING -> {
                // Payment pending - like STOPPED but indicates waiting for settlement
                statusByte = 0x08  // TRANSACTION_COMPLETE (same as STOPPED for Windows compatibility)
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
        pendingTransaction = null
        disconnected = false
    }
    
    /**
     * Settle pending transaction and reset dispenser to IDLE.
     * This is called after payment is complete (CARD capture or CREDIT settlement).
     * 
     * @param method Payment method used ("CARD" or "CREDIT")
     * @return The settled transaction, or null if no transaction was pending
     */
    fun settleAndReset(method: String = "CARD"): CompletedTransaction? {
        val tx = pendingTransaction ?: run {
            logger.warn("⚠️ No pending transaction to settle")
            return null
        }
        
        logger.info("┌────────────────────────────────────────────────────────────")
        logger.info("│ 💳 SETTLEMENT: $method")
        logger.info("│ Transaction: ${tx.idempotencyKey}")
        logger.info("│ Volume: ${tx.liters} L")
        logger.info("│ Amount: ${tx.amountNok} NOK")
        logger.info("│ Unit Price: ${tx.unitPrice} NOK/L")
        logger.info("└────────────────────────────────────────────────────────────")
        
        // Clear pending transaction and reset to IDLE
        pendingTransaction = null
        volumeLitres = 0.0
        amountCents = 0
        state = DispenserState.IDLE
        nozzleLifted = false
        productSelected = false
        
        logger.info("✅ Dispenser reset to IDLE - ready for next customer")
        
        return tx
    }
    
    /**
     * Get the current pending transaction, if any.
     * Used by EmulatorService to enqueue transactions to TransactionSink.
     */
    fun getPendingTransaction(): CompletedTransaction? = pendingTransaction
    
    /**
     * Freeze current transaction totals for payment pending state.
     * Called by STOP/BLOCK handlers to create immutable transaction snapshot.
     * 
     * Includes station and edge identifiers for multi-station cloud synchronization.
     * 
     * @return The frozen transaction
     */
    fun freezeTransaction(): CompletedTransaction {
        val tx = CompletedTransaction(
            stationId = stationId,
            edgeId = edgeId,
            dispenserId = dispenserId,
            dispenserAddress = address,
            liters = volumeLitres,
            amountNok = amountCents / 100.0,
            unitPrice = currentPricePerLitreCents / 100.0,
            finishedAt = Instant.now()
        )
        pendingTransaction = tx
        logger.info("🧊 Transaction frozen: [$stationId/$dispenserId] ${tx.liters} L @ ${tx.unitPrice} NOK/L = ${tx.amountNok} NOK (${tx.idempotencyKey})")
        return tx
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
    
    // ==========================================================================
    // FRI PUMPE API - Direct control methods for field testing without PLS
    // ==========================================================================
    
    /**
     * Data class for pump status returned by API.
     */
    data class PumpStatus(
        val state: String,
        val address: Int,
        val volumeLitres: Double,
        val amountKr: Double,
        val pricePerLitreKr: Double,
        val nozzleLifted: Boolean,
        val hasPendingTransaction: Boolean
    )
    
    /**
     * Get current pump status for API.
     */
    fun getPumpStatus(): PumpStatus {
        if (state == DispenserState.PUMPING) {
            updateDelivery()
        }
        return PumpStatus(
            state = state.name,
            address = address,
            volumeLitres = volumeLitres,
            amountKr = amountCents / 100.0,
            pricePerLitreKr = currentPricePerLitreCents / 100.0,
            nozzleLifted = nozzleLifted,
            hasPendingTransaction = pendingTransaction != null
        )
    }
    
    // Protocol logger for WebSocket streaming
    private val protocolLogger = LoggerFactory.getLogger("no.cloudberries.lpg.protocol.EhlPacketStream")
    
    /**
     * Format bytes as hex string for protocol logging.
     * Example output: [10 07 01 77 6F 36]
     */
    private fun ByteArray.toHexBrackets(): String {
        return "[" + joinToString(" ") { "%02X".format(it) } + "]"
    }
    
    /**
     * Build a simulated TX packet for logging purposes.
     * Creates realistic EHL packet bytes for visual debugging.
     */
    private fun buildSimulatedTxPacket(command: EhlCommand): ByteArray {
        val packet = EhlPacket(address, command)
        return EhlCodec.encode(packet, fromController = true)
    }
    
    /**
     * Build a simulated RX packet for logging purposes.
     * Creates realistic EHL response bytes including current state data.
     */
    private fun buildSimulatedRxPacket(command: EhlCommand, data: ByteArray = ByteArray(0)): ByteArray {
        val packet = EhlPacket(address, command, data)
        return EhlCodec.encode(packet, fromController = false)
    }
    
    /**
     * "Fri pumpe" - Direct UNBLOCK without going through EHL protocol.
     * Used for field testing when no PLS/terminal is available.
     * 
     * This method:
     * 1. Checks for pending transactions (must be settled first)
     * 2. Transitions to AUTHORIZED state
     * 3. Simulates nozzle lift to start PUMPING
     * 
     * @return Result with new state or error message
     */
    fun directUnblock(): Result<PumpStatus> {
        logger.info("┌" + "─".repeat(60) + "┐")
        logger.info("│ 🔓 FRI PUMPE: UNBLOCK REQUEST (Direct API)" + " ".repeat(16) + "│")
        logger.info("├" + "─".repeat(60) + "┤")
        
        // Build and log RAW HEX for the simulated UNBLOCK command
        val txPacket = buildSimulatedTxPacket(EhlCommand.UNBLOCK)
        protocolLogger.info("📤 TX ${txPacket.toHexBrackets()} -> UNBLOCK (0x77) to address #$address")
        
        // Check for pending transaction
        if (pendingTransaction != null) {
            logger.warn("│ ❌ REJECTED: Pending transaction exists" + " ".repeat(19) + "│")
            logger.warn("│    Amount: %.2f NOK".format(pendingTransaction!!.amountNok).padEnd(59) + "│")
            logger.info("└" + "─".repeat(60) + "┘")
            return Result.failure(IllegalStateException("Må betale forrige transaksjon først (${pendingTransaction!!.amountNok} NOK)"))
        }
        
        val previousState = state.name
        
        when (state) {
            DispenserState.IDLE, DispenserState.STOPPED -> {
                // Transition to AUTHORIZED then simulate nozzle lift
                state = DispenserState.AUTHORIZED
                productSelected = true
                
                // Simulate nozzle lift to start pumping immediately
                nozzleLifted = true
                state = DispenserState.PUMPING
                startedAtMs = System.currentTimeMillis()
                volumeLitres = 0.0
                amountCents = 0
                
                logger.info("│ ✅ State: $previousState → PUMPING" + " ".repeat(maxOf(0, 59 - ("│ ✅ State: $previousState → PUMPING").length)) + "│")
                logger.info("│ 🚀 Delivery started @ %.2f kr/L".format(currentPricePerLitreCents / 100.0).padEnd(59) + "│")
            }
            DispenserState.AUTHORIZED -> {
                // Already authorized, just lift nozzle
                nozzleLifted = true
                state = DispenserState.PUMPING
                startedAtMs = System.currentTimeMillis()
                volumeLitres = 0.0
                amountCents = 0
                
                logger.info("│ ✅ State: AUTHORIZED → PUMPING" + " ".repeat(28) + "│")
            }
            DispenserState.PUMPING -> {
                logger.info("│ ⚠️ Already pumping - no action needed" + " ".repeat(20) + "│")
            }
            DispenserState.PAYMENT_PENDING -> {
                logger.warn("│ ❌ Cannot unblock - payment pending" + " ".repeat(23) + "│")
                logger.info("└" + "─".repeat(60) + "┘")
                return Result.failure(IllegalStateException("Betaling venter - kan ikke starte ny fylling"))
            }
            DispenserState.ERROR -> {
                logger.warn("│ ❌ Cannot unblock - dispenser in ERROR state" + " ".repeat(13) + "│")
                logger.info("└" + "─".repeat(60) + "┘")
                return Result.failure(IllegalStateException("Dispenser i feilmodus"))
            }
        }
        
        logger.info("└" + "─".repeat(60) + "┘")
        
        // Build and log RAW HEX for the simulated STATE response
        val stateData = byteArrayOf(buildStatusByte())
        val rxPacket = buildSimulatedRxPacket(EhlCommand.STATE, stateData)
        protocolLogger.info("📥 RX ${rxPacket.toHexBrackets()} <- STATE=${state.name} from address #$address")
        
        return Result.success(getPumpStatus())
    }
    
    /**
     * "Stopp pumpe" - Direct BLOCK without going through EHL protocol.
     * Used for field testing when no PLS/terminal is available.
     * 
     * This method:
     * 1. Stops any ongoing delivery
     * 2. Calculates final volume and amount
     * 3. Freezes transaction for payment
     * 
     * @return Result with final state and transaction data
     */
    fun directBlock(): Result<PumpStatus> {
        logger.info("┌" + "─".repeat(60) + "┐")
        logger.info("│ 🛑 FRI PUMPE: BLOCK REQUEST (Direct API)" + " ".repeat(18) + "│")
        logger.info("├" + "─".repeat(60) + "┤")
        
        // Build and log RAW HEX for the simulated BLOCK command
        val txPacket = buildSimulatedTxPacket(EhlCommand.BLOCK)
        protocolLogger.info("📤 TX ${txPacket.toHexBrackets()} -> BLOCK (0x69) to address #$address")
        
        val previousState = state.name
        
        when (state) {
            DispenserState.PUMPING -> {
                updateDelivery() // Calculate final volume/amount
                nozzleLifted = false
                
                if (volumeLitres > 0.0) {
                    freezeTransaction()
                    state = DispenserState.PAYMENT_PENDING
                    
                    logger.info("│ ✅ State: PUMPING → PAYMENT_PENDING" + " ".repeat(24) + "│")
                    logger.info("│ 🏁 Final: %.2f L @ %.2f kr".format(volumeLitres, amountCents / 100.0).padEnd(59) + "│")
                    logger.info("│ 💳 Awaiting payment settlement" + " ".repeat(28) + "│")
                } else {
                    state = DispenserState.IDLE
                    logger.info("│ ✅ State: PUMPING → IDLE (no fuel dispensed)" + " ".repeat(14) + "│")
                }
            }
            DispenserState.AUTHORIZED -> {
                state = DispenserState.IDLE
                productSelected = false
                nozzleLifted = false
                logger.info("│ ✅ State: AUTHORIZED → IDLE (cancelled)" + " ".repeat(18) + "│")
            }
            DispenserState.IDLE, DispenserState.STOPPED -> {
                logger.info("│ ⚠️ Already stopped - no action needed" + " ".repeat(20) + "│")
            }
            DispenserState.PAYMENT_PENDING -> {
                logger.info("│ ⚠️ Payment pending - already blocked" + " ".repeat(21) + "│")
            }
            DispenserState.ERROR -> {
                state = DispenserState.IDLE
                logger.info("│ 🔄 Error cleared, state → IDLE" + " ".repeat(28) + "│")
            }
        }
        
        logger.info("└" + "─".repeat(60) + "┘")
        
        // Build and log RAW HEX for the simulated STATE response with volume/amount data
        val stateData = byteArrayOf(buildStatusByte())
        val rxPacket = buildSimulatedRxPacket(EhlCommand.STATE, stateData)
        protocolLogger.info("📥 RX ${rxPacket.toHexBrackets()} <- STATE=${state.name} | VOL=${"%.2f".format(volumeLitres)}L | AMT=${"%.2f".format(amountCents/100.0)}kr")
        
        return Result.success(getPumpStatus())
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
                logger.info("┌" + "─".repeat(78) + "┐")
                logger.info("│ 📦 CORE: PROCESSING EHL PACKET" + " ".repeat(78 - 31) + "│")
                logger.info("├" + "─".repeat(78) + "┤")
                logger.info("│ " + EhlPacketFormatter.formatPacketForLogging(
                    parsed.packet,
                    EhlPacketFormatter.Direction.RECEIVING
                ).padEnd(77) + "│")
                
                val responses = handlePacket(parsed.packet)
                
                if (responses.isNotEmpty()) {
                    logger.info("├" + "─".repeat(78) + "┤")
                    logger.info("│ 📤 CORE: SENDING ${responses.size} RESPONSE(S)" + " ".repeat(78 - (26 + responses.size.toString().length)) + "│")
                    responses.forEach { response ->
                        logger.info("│ " + EhlPacketFormatter.formatPacketForLogging(
                            response,
                            EhlPacketFormatter.Direction.SENDING
                        ).padEnd(77) + "│")
                    }
                }
                logger.info("└" + "─".repeat(78) + "┘")
                
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
            logger.warn("📫 CORE: IGNORED packet addressed to #${packet.address} (I am #$address)")
            return emptyList()
        }
        
        commandCount++
        
        // Log received command with input hex
        val inputHex = if (packet.data.isNotEmpty()) {
            packet.data.joinToString(" ") { "%02X".format(it) }
        } else "(no data)"
        
        logger.info("│ ⚡ CMD: ${packet.command.name} (0x%02X) | Input: $inputHex".format(packet.command.code))
        
        return when (packet.command) {
            EhlCommand.STATE     -> {
                logger.info("│    └─ STATE query: Current state = $state" + " ".repeat(maxOf(0, 77 - (40 + state.name.length))) + "│")
                listOf(buildStateResponse())
            }
            EhlCommand.UNBLOCK   -> handleUnblock(packet)
            EhlCommand.STOP      -> handleStop(packet)
            EhlCommand.BLOCK     -> handleBlock(packet)
            EhlCommand.VOLUME    -> {
                updateDelivery() // Update live values
                logger.info("│    └─ VOLUME query: %.2f L / %.2f kr".format(volumeLitres, amountCents / 100.0) + " ".repeat(maxOf(0, 77 - String.format("    └─ VOLUME query: %.2f L / %.2f kr", volumeLitres, amountCents / 100.0).length - 2)) + "│")
                listOf(buildVolumeResponse())
            }
            EhlCommand.PRICE     -> {
                logger.info("│    └─ PRICE query: %.2f kr/L".format(currentPricePerLitreCents / 100.0) + " ".repeat(maxOf(0, 77 - String.format("    └─ PRICE query: %.2f kr/L", currentPricePerLitreCents / 100.0).length - 2)) + "│")
                listOf(buildPriceResponse())
            }
            EhlCommand.PROG_PRC     -> handlePriceProgram(packet)
            EhlCommand.PROG_AMOUNT  -> handleAmountPreset(packet)
            EhlCommand.PROG_VOLUME  -> handleVolumePreset(packet)
            EhlCommand.ERROR_QUERY  -> {
                logger.info("│    └─ ERROR_QUERY: No errors (00)" + " ".repeat(maxOf(0, 77 - 37)) + "│")
                // VB6 format: 2 ASCII bytes (main code + sub code)
                listOf(EhlPacket(address, EhlCommand.ERROR, byteArrayOf('0'.code.toByte(), '0'.code.toByte()))) // No error: "00"
            }
            EhlCommand.TANK      -> {
                logger.info("│    └─ TANK query: Fuel data requested" + " ".repeat(maxOf(0, 77 - 40)) + "│")
                listOf(buildTankResponse())
            }
            EhlCommand.PRODUCT_SELECT -> handleProductSelect(packet)
            EhlCommand.LINETEST  -> {
                logger.info("│    └─ LINETEST: Communication OK" + " ".repeat(maxOf(0, 77 - 36)) + "│")
                listOf(EhlPacket(address, EhlCommand.OK))
            }
            EhlCommand.ZER       -> handleReset(packet)
            else                 -> {
                logger.warn("│    └─ UNSUPPORTED: ${packet.command.name}" + " ".repeat(maxOf(0, 77 - (21 + packet.command.name.length))) + "│")
                listOf(buildErrorPacket(0x10))
            }
        }
    }

    private fun handleUnblock(packet: EhlPacket): List<EhlPacket> {
        val previousState = state.name
        
        // Check if payment is pending - reject UNBLOCK until settled
        if (state == DispenserState.PAYMENT_PENDING || pendingTransaction != null) {
            logger.warn("│    ⚠️ UNBLOCK REJECTED: Payment pending (${pendingTransaction?.amountNok} NOK)" + " ".repeat(maxOf(0, 77 - String.format("    ⚠️ UNBLOCK REJECTED: Payment pending (%.2f NOK)", pendingTransaction?.amountNok ?: 0.0).length - 2)) + "│")
            logger.warn("│    💳 Please settle transaction via /api/emulator/$address/settle" + " ".repeat(maxOf(0, 77 - String.format("    💳 Please settle transaction via /api/emulator/%d/settle", address).length - 2)) + "│")
            
            // Return OK but keep PAYMENT_PENDING state
            return listOf(
                EhlPacket(address, EhlCommand.OK),
                buildStateResponse() // Will show TRANSACTION_COMPLETE flag (0x08)
            )
        }
        
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
                    
                    logger.info("│    └─ " + EhlPacketFormatter.formatStateTransition(
                        previousState,
                        state.name,
                        "UNBLOCK + nozzle lifted"
                    ) + " ".repeat(maxOf(0, 77 - String.format("    └─ 🔄 STATE CHANGE: %s → %s | Reason: UNBLOCK + nozzle lifted", previousState, state.name).length - 2)) + "│")
                    logger.info("│    🚀 DELIVERY ACTIVE: %.2f kr/L @ %.2f L/s".format(
                        currentPricePerLitreCents / 100.0,
                        litresPerSecond
                    ) + " ".repeat(maxOf(0, 77 - String.format("    🚀 DELIVERY ACTIVE: %.2f kr/L @ %.2f L/s", currentPricePerLitreCents / 100.0, litresPerSecond).length - 2)) + "│")
                } else {
                    // Nozzle down - wait in AUTHORIZED state
                    state = DispenserState.AUTHORIZED
                    productSelected = true
                    
                    logger.info("│    └─ " + EhlPacketFormatter.formatStateTransition(
                        previousState,
                        state.name,
                        "Waiting for nozzle lift"
                    ) + " ".repeat(maxOf(0, 77 - String.format("    └─ 🔄 STATE CHANGE: %s → %s | Reason: Waiting for nozzle lift", previousState, state.name).length - 2)) + "│")
                    logger.info("│    ✅ AUTHORIZED: Ready for customer" + " ".repeat(maxOf(0, 77 - 40)) + "│")
                }
            }
            DispenserState.PUMPING -> {
                logger.warn("│    ⚠️ UNBLOCK ignored - already in PUMPING state" + " ".repeat(maxOf(0, 77 - 54)) + "│")
            }
            DispenserState.PAYMENT_PENDING -> {
                // Should not reach here - already handled at top of function
                logger.warn("│    ⚠️ UNBLOCK blocked - payment pending (should be caught earlier)" + " ".repeat(maxOf(0, 77 - 72)) + "│")
            }
            DispenserState.ERROR -> {
                logger.warn("│    ⚠️ UNBLOCK blocked - dispenser in ERROR state" + " ".repeat(maxOf(0, 77 - 56)) + "│")
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
            
            // Freeze transaction and enter PAYMENT_PENDING state
            if (volumeLitres > 0.0) {
                freezeTransaction()
                state = DispenserState.PAYMENT_PENDING
            } else {
                // No fuel dispensed - go directly to IDLE
                state = DispenserState.STOPPED
            }
            
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
            
            if (state == DispenserState.PAYMENT_PENDING) {
                logger.info("🔒 STATE: PAYMENT_PENDING - Awaiting settlement")
            }
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
        logger.info("│    └─ BLOCK: Stopping dispenser" + " ".repeat(maxOf(0, 77 - 35)) + "│")
        val previousState = state.name
        
        // BLOCK stops delivery and enters PAYMENT_PENDING (like STOP)
        when (state) {
            DispenserState.PUMPING -> {
                updateDelivery() // Calculate final volume/amount
                
                // Freeze transaction and enter PAYMENT_PENDING state
                if (volumeLitres > 0.0) {
                    freezeTransaction()
                    state = DispenserState.PAYMENT_PENDING
                } else {
                    state = DispenserState.STOPPED
                }
                
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
                
                if (state == DispenserState.PAYMENT_PENDING) {
                    logger.info("🔒 STATE: PAYMENT_PENDING - Awaiting settlement")
                }
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
            DispenserState.PAYMENT_PENDING -> {
                // CRITICAL: Do NOT reset to IDLE when payment pending!
                logger.warn("⚠️ BLOCK ignored - payment pending")
                logger.info("🔒 STATE: PAYMENT_PENDING - Must settle before new transaction")
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
        
        // VB6 format: 5 ASCII bytes LSB-first representing centilitres
        // Example: 45.50 L = 4550 cL = "04550" reversed = [0x30, 0x35, 0x35, 0x34, 0x30]
        val centilitres = (volumeLitres * 100).roundToInt()
        val volumeStr = "%05d".format(centilitres)  // "04550"
        
        // Convert to ASCII bytes in LSB-first order (reverse)
        val data = ByteArray(5)
        for (i in 0..4) {
            data[i] = volumeStr[4 - i].code.toByte()  // Reverse order
        }
        
        // Log the response hex for debugging
        val responseHex = data.joinToString(" ") { "%02X".format(it) }
        logger.info("│    └─ VOLUME response: %.2f L (%d cL) | HEX: [$responseHex]".format(volumeLitres, centilitres))
        
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
