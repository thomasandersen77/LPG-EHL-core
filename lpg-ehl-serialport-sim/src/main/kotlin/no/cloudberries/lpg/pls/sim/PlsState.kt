package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Manages PLS state for dispensers with auto-pumping simulation.
 * 
 * Now includes VB6-compatible state machine and nozzle tracking for realistic EHL protocol simulation.
 * State flow: IDLE → AUTHORIZED → PUMPING → STOPPED → (PAYMENT_PENDING) → IDLE
 * 
 * Supports both standard addresses (1-8) and legacy addresses (32+n).
 * Based on Alejandro's field testing findings.
 * 
 * @param defaultAddress Default dispenser address (1-8 or 33-40 for legacy)
 * @param priceCents Price per liter in cents (e.g., 1590 = 15.90 kr/L)
 * @param initiallyBlocked Whether dispensers start in blocked state (IDLE)
 * @param flowRateMlPerSecond Simulated flow rate in ml/second (default: 500 = 0.5 L/s)
 * @param legacyAddressEnabled If true, also responds to legacy address (32 + defaultAddress)
 */
class PlsState(
    private val defaultAddress: Int = 1,
    private val priceCents: Int = 1590,
    initiallyBlocked: Boolean = true,
    private val flowRateMlPerSecond: Int = 500,
    private val legacyAddressEnabled: Boolean = true,
    // Fault injection for testing
    val disconnectAfterSeconds: Double? = null,
    val badChecksumRate: Double = 0.0,
    val powerfaultAfterSeconds: Double? = null,
    private val onDisconnect: (() -> Unit)? = null,  // Callback to disconnect serial port
    private val onPowerfault: (() -> Unit)? = null,  // Callback for power fault
    val manualNozzleControl: Boolean = false         // When true, UNBLOCK stays AUTHORIZED; GUI button controls nozzle
) {
    private val log = LoggerFactory.getLogger(PlsState::class.java)
    
    // State machine
    private var state: DispenserState = if (initiallyBlocked) DispenserState.IDLE else DispenserState.AUTHORIZED
    private var nozzleLifted: Boolean = false
    private var startedAtMs: Long? = null
    
    private val currentVolumeMl = AtomicLong(0L)  // Volume in milliliters
    private val currentPriceCents = AtomicInteger(priceCents)
    
    // Transaction freeze for payment pending
    @Volatile
    private var frozenTransaction: FrozenTransaction? = null
    
    // Auto-pumping simulation
    private val pumpingActive = AtomicBoolean(false)
    @Volatile private var pumpingThread: Thread? = null
    @Volatile private var running = true
    
    // Fault injection state
    private val startTimeMs = System.currentTimeMillis()
    @Volatile private var disconnected = false
    @Volatile private var powerfaulted = false

    // Legacy address = 32 + defaultAddress (Alejandro's finding)
    private val legacyAddress: Int = 32 + defaultAddress
    
    init {
        val addressInfo = if (legacyAddressEnabled) 
            "address=$defaultAddress (+ legacy=$legacyAddress)" 
        else 
            "address=$defaultAddress"
        
        val faultInfo = buildString {
            if (disconnectAfterSeconds != null) append(", disconnect=${"%.1f".format(disconnectAfterSeconds)}s")
            if (badChecksumRate > 0.0) append(", badChecksum=${"%.1f".format(badChecksumRate * 100)}%")
            if (powerfaultAfterSeconds != null) append(", powerfault=${"%.1f".format(powerfaultAfterSeconds)}s")
        }
        
        log.info("PLS State initialized: $addressInfo, price=${priceCents/100.0} kr/L, state=$state, flowRate=${flowRateMlPerSecond}ml/s$faultInfo")
        
        // Start auto-pumping simulation thread
        startAutoPumpingThread()
        
        // Start fault injection threads if configured
        startFaultInjectionThreads()
        
        // If starting unblocked, enable auto-pumping immediately
        if (state == DispenserState.AUTHORIZED || state == DispenserState.PUMPING) {
            pumpingActive.set(true)
            log.info("▶️ Auto-pumping enabled at startup ($state mode)")
        }
    }
    
    /**
     * Check if address matches this dispenser (standard or legacy).
     */
    fun matchesAddress(address: Int): Boolean {
        return address == defaultAddress || (legacyAddressEnabled && address == legacyAddress)
    }
    
    /**
     * Auto-pumping thread: increments volume when pump is unblocked.
     */
    private fun startAutoPumpingThread() {
        pumpingThread = thread(name = "pls-sim-pumping", isDaemon = true) {
            val updateIntervalMs = 100L // Update every 100ms
            val mlPerUpdate = (flowRateMlPerSecond * updateIntervalMs / 1000).toInt()
            
            while (running) {
                try {
                    Thread.sleep(updateIntervalMs)
                    
                    if (pumpingActive.get() && state == DispenserState.PUMPING) {
                        val newVolume = currentVolumeMl.addAndGet(mlPerUpdate.toLong())
                        
                        // Log every ~1 liter (1000ml)
                        if (newVolume % 1000 < mlPerUpdate) {
                            val litres = newVolume / 1000.0
                            val amountKr = litres * getPrice() / 100.0
                            log.info("⛽ PUMPING: ${"%.2f".format(litres)} L / ${"%.2f".format(amountKr)} kr")
                        }
                    }
                } catch (e: InterruptedException) {
                    // Shutdown signal
                    break
                }
            }
        }
    }
    
    /**
     * Start fault injection threads if configured.
     */
    private fun startFaultInjectionThreads() {
        // Disconnect after N seconds
        disconnectAfterSeconds?.let { seconds ->
            thread(name = "pls-sim-disconnect", isDaemon = true) {
                try {
                    Thread.sleep((seconds * 1000).toLong())
                    if (!disconnected) {
                        disconnected = true
                        log.error("🔌 FAULT INJECTION: DISCONNECT after ${"%.1f".format(seconds)}s")
                        onDisconnect?.invoke()
                    }
                } catch (_: InterruptedException) {}
            }
        }
        
        // Power fault after N seconds
        powerfaultAfterSeconds?.let { seconds ->
            thread(name = "pls-sim-powerfault", isDaemon = true) {
                try {
                    Thread.sleep((seconds * 1000).toLong())
                    if (!powerfaulted) {
                        powerfaulted = true
                        log.error("⚡ FAULT INJECTION: POWER FAULT after ${"%.1f".format(seconds)}s")
                        log.error("⚡ Resetting state to IDLE and disconnecting...")
                        
                        // Reset state
                        state = DispenserState.IDLE
                        nozzleLifted = false
                        resetVolume()
                        frozenTransaction = null
                        pumpingActive.set(false)
                        
                        // Disconnect
                        onPowerfault?.invoke()
                    }
                } catch (_: InterruptedException) {}
            }
        }
    }
    
    /**
     * Stop the auto-pumping thread (call on shutdown).
     */
    fun shutdown() {
        running = false
        pumpingThread?.interrupt()
    }

    fun getState(): DispenserState = state
    
    fun isBlocked(): Boolean = state == DispenserState.IDLE
    
    fun getPrice(): Int = currentPriceCents.get()
    
    fun setPrice(cents: Int) {
        val previous = currentPriceCents.getAndSet(cents)
        if (previous != cents) {
            log.info("Price changed: ${previous/100.0} -> ${cents/100.0} kr/L")
        }
    }
    
    fun getVolumeMl(): Long = currentVolumeMl.get()
    
    fun setVolumeMl(ml: Long) {
        currentVolumeMl.set(ml)
    }
    
    fun addVolumeMl(ml: Long) {
        currentVolumeMl.addAndGet(ml)
    }
    
    fun resetVolume() {
        currentVolumeMl.set(0L)
        log.info("Volume reset to 0")
    }

    /**
     * Simulate nozzle lift/holster.
     * Used for testing the complete fueling lifecycle.
     */
    fun simulateNozzleLift(lifted: Boolean) {
        val previousNozzle = nozzleLifted
        nozzleLifted = lifted
        
        if (lifted && !previousNozzle) {
            log.info("🚰 NOZZLE LIFTED")
            // If authorized and nozzle lifted, transition to PUMPING
            if (state == DispenserState.AUTHORIZED) {
                state = DispenserState.PUMPING
                startedAtMs = System.currentTimeMillis()
                pumpingActive.set(true)
                log.info("🔄 STATE: AUTHORIZED → PUMPING (nozzle lifted)")
                log.info("▶️ Auto-pumping STARTED at ${flowRateMlPerSecond}ml/s")
            }
        } else if (!lifted && previousNozzle) {
            log.info("🚰 NOZZLE HOLSTERED")
            // If pumping and nozzle holstered, stop delivery
            if (state == DispenserState.PUMPING) {
                pumpingActive.set(false)
                state = DispenserState.IDLE  // Go directly to IDLE - payment is webapp's responsibility
                log.info("🔄 STATE: PUMPING → IDLE (nozzle holstered, hardware reset)")
                log.info("⏹️ Auto-pumping STOPPED - Final volume: ${"%.2f".format(getVolumeMl()/1000.0)} L")
                // Volume remains for webapp to read, but hardware is ready for next transaction
            }
        }
    }
    
    /**
     * Freeze current transaction for payment pending.
     */
    private fun freezeTransaction() {
        val volumeL = getVolumeMl() / 1000.0
        val amountKr = volumeL * getPrice() / 100.0
        
        frozenTransaction = FrozenTransaction(
            volumeLitres = volumeL,
            amountKr = amountKr,
            pricePerLitreKr = getPrice() / 100.0
        )
        
        state = DispenserState.PAYMENT_PENDING
        log.info("🧊 Transaction frozen: ${"%.2f".format(volumeL)} L @ ${"%.2f".format(getPrice()/100.0)} kr/L = ${"%.2f".format(amountKr)} kr")
        log.info("🔄 STATE: STOPPED → PAYMENT_PENDING")
    }
    
    /**
     * Settle pending transaction and reset to IDLE.
     */
    fun settleAndReset(): FrozenTransaction? {
        val tx = frozenTransaction ?: return null
        
        log.info("💳 SETTLEMENT: ${"%.2f".format(tx.volumeLitres)} L for ${"%.2f".format(tx.amountKr)} kr")
        
        frozenTransaction = null
        resetVolume()
        state = DispenserState.IDLE
        nozzleLifted = false
        
        log.info("🔄 STATE: PAYMENT_PENDING → IDLE")
        log.info("✅ Dispenser reset - ready for next customer")
        
        return tx
    }

    fun processCommand(command: String): CommandResult {
        val upperCmd = command.uppercase().trim()
        
        return when {
            upperCmd.contains("FREE") || upperCmd.contains("UNBLOCK") -> {
                if (state == DispenserState.IDLE || state == DispenserState.STOPPED) {
                    state = DispenserState.AUTHORIZED
                    log.info("Text command: UNBLOCK -> state=$state")
                }
                CommandResult.OK
            }
            upperCmd.contains("STOP") || upperCmd.contains("BLOCK") -> {
                if (state == DispenserState.PUMPING) {
                    pumpingActive.set(false)
                    state = DispenserState.STOPPED
                    log.info("Text command: BLOCK -> state=$state")
                }
                CommandResult.OK
            }
            upperCmd.contains("STATUS") -> {
                CommandResult.Status(state == DispenserState.IDLE)
            }
            upperCmd.isEmpty() -> {
                CommandResult.Ignored
            }
            else -> {
                log.debug("Unknown command: {}", command)
                CommandResult.ACK
            }
        }
    }

    /**
     * Process EHL binary command frame.
     * 
     * Supports all commands tested by Alejandro:
     * - STATE (0x4B) - Returns pump status
     * - ERROR_QUERY (0x4C) - Returns error status (no errors = empty data)
     * - VOLUME (0x45) - Returns current volume
     * - TANKBIT (0xC5) - Returns tank level status
     * 
     * Also responds to legacy addresses (32 + defaultAddress).
     */
    fun processEhlCommand(frame: EhlFrame): EhlCommandResult? {
        val cmd = frame.cmd
        // Binary address (Core/VB6 sends raw address byte, not ASCII)
        val addrInt = frame.addr.toInt() and 0xFF
        
        // Check if we should respond to this address
        if (!matchesAddress(addrInt)) {
            log.debug("🚫 Ignoring command for address $addrInt (not our address)")
            return null  // Don't respond to wrong address
        }

        return when (cmd) {
            EhlFrameCodec.CMD_LINETEST -> {
                log.debug("🔗 LINETEST from address $addrInt")
                EhlCommandResult.LinetestResponse(frame.addr)
            }
            EhlFrameCodec.CMD_STATE -> {
                val statusByte = buildStatusByte()
                val statusName = state.name
                log.debug("📊 STATE addr=$addrInt -> $statusName (0x${statusByte.toHex()}, vol=${getVolumeMl()}ml)")
                EhlCommandResult.StateResponse(frame.addr, byteArrayOf(statusByte))
            }
            EhlFrameCodec.CMD_ERROR_QUERY -> {
                // Alejandro tested: ERROR_QUERY (0x4C) - returns error status
                // Return empty data = no errors
                log.debug("⚠️ ERROR_QUERY addr=$addrInt -> No errors")
                EhlCommandResult.ErrorQueryResponse(frame.addr, byteArrayOf(0x00))  // No errors
            }
            EhlFrameCodec.CMD_VOLUME -> {
                // Core expects 5 ASCII digits in LSB-first order (centiliters)
                val volumeCl = (getVolumeMl() / 10).toInt().coerceIn(0, 99999)
                val volumeStr = "%05d".format(volumeCl)
                val volumeBytes = volumeStr.reversed().map { it.code.toByte() }.toByteArray()
                log.debug("⛽ VOLUME addr=$addrInt -> ${getVolumeMl()/1000.0} L (cl=$volumeCl)")
                EhlCommandResult.VolumeResponse(frame.addr, volumeBytes)
            }
            EhlFrameCodec.CMD_TANKBIT -> {
                // Alejandro tested: TANKBIT (0xC5) - returns tank level status
                // Return 0x01 = tank OK (not empty)
                log.debug("🚨 TANKBIT addr=$addrInt -> Tank OK")
                EhlCommandResult.TankbitResponse(frame.addr, byteArrayOf(0x01))  // Tank OK
            }
            EhlFrameCodec.CMD_PRICE, EhlFrameCodec.CMD_PRICE_ALT -> {
                if (frame.data.isNotEmpty()) {
                    // SET PRICE: data contains 4 ASCII digits in LSB-first order (cents)
                    val priceStr = frame.data.reversed().map { (it.toInt() and 0xFF).toChar() }.joinToString("")
                    val priceCents = priceStr.toIntOrNull() ?: getPrice()
                    setPrice(priceCents)
                    log.info("💰 PRICE SET addr=$addrInt: ${priceCents/100.0} kr/L")
                    EhlCommandResult.OkAck(frame.addr)
                } else {
                    // GET PRICE: return current price as 4 ASCII digits in LSB-first order (cents)
                    val priceStr = "%04d".format(getPrice().coerceIn(0, 9999))
                    val priceBytes = priceStr.reversed().map { it.code.toByte() }.toByteArray()
                    log.debug("💰 PRICE GET addr=$addrInt -> ${getPrice()/100.0} kr/L")
                    EhlCommandResult.PriceResponse(frame.addr, priceBytes)
                }
            }
            EhlFrameCodec.CMD_BLOCK -> {
                val previousState = state.name
                
                if (state == DispenserState.PUMPING) {
                    pumpingActive.set(false)
                    state = DispenserState.IDLE  // Go directly to IDLE - payment is webapp's responsibility
                    log.info("🛑 BLOCK addr=$addrInt")
                    log.info("🔄 STATE: $previousState → IDLE (hardware reset)")
                    log.info("⏹️ Auto-pumping STOPPED - Final volume: ${"%.2f".format(getVolumeMl()/1000.0)} L")
                    // Volume remains for webapp to read, but hardware is ready for next transaction
                } else {
                    log.info("🛑 BLOCK addr=$addrInt (already stopped, state=$state)")
                }
                
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_UNBLOCK -> {
                val previousState = state.name
                log.info("🔓 UNBLOCK received addr=$addrInt, current state=$state")
                
                try {
                    // Hardware doesn't care about payment - always allow UNBLOCK
                    // Payment is webapp/service layer's responsibility
                    when (state) {
                        DispenserState.IDLE, DispenserState.AUTHORIZED, DispenserState.STOPPED -> {
                            if (manualNozzleControl) {
                                // GUI mode: stay AUTHORIZED, wait for GUI button to lift nozzle
                                nozzleLifted = false
                                state = DispenserState.AUTHORIZED
                                resetVolume()
                                pumpingActive.set(false)
                                log.info("✅ UNBLOCK addr=$addrInt from $previousState")
                                log.info("🔄 STATE: $previousState → AUTHORIZED (venter på GUI-knapp)")
                            } else {
                                // Auto-simulate nozzle lift after UNBLOCK
                                nozzleLifted = true
                                state = DispenserState.PUMPING
                                startedAtMs = System.currentTimeMillis()
                                resetVolume()
                                pumpingActive.set(true)
                                log.info("✅ UNBLOCK addr=$addrInt from $previousState")
                                log.info("🔄 STATE: $previousState → PUMPING (auto-simulated nozzle lift)")
                                log.info("▶️ Auto-pumping STARTED at ${flowRateMlPerSecond}ml/s")
                            }
                            log.info("📊 After UNBLOCK: state=$state, volume=${getVolumeMl()}ml, pumpingActive=${pumpingActive.get()}")
                        }
                        DispenserState.PUMPING -> {
                            log.warn("⚠️ UNBLOCK ignored addr=$addrInt - already PUMPING")
                        }
                        DispenserState.PAYMENT_PENDING -> {
                            // Reset and start new transaction - payment is webapp's concern
                            log.info("🔄 PAYMENT_PENDING → clearing for new transaction")
                            if (manualNozzleControl) {
                                nozzleLifted = false
                                state = DispenserState.AUTHORIZED
                            } else {
                                nozzleLifted = true
                                state = DispenserState.PUMPING
                                startedAtMs = System.currentTimeMillis()
                                resetVolume()
                                pumpingActive.set(true)
                            }
                            frozenTransaction = null
                            if (!manualNozzleControl) {
                                log.info("▶️ Auto-pumping STARTED at ${flowRateMlPerSecond}ml/s")
                            }
                            log.info("✅ UNBLOCK addr=$addrInt (payment pending cleared)")
                            log.info("🔄 STATE: $previousState → $state")
                        }
                    }
                } catch (e: Exception) {
                    log.error("❌ UNBLOCK EXCEPTION: ${e.message}", e)
                }
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STOP -> {
                val previousState = state.name
                
                if (state == DispenserState.PUMPING) {
                    pumpingActive.set(false)
                    state = DispenserState.IDLE  // Go directly to IDLE - payment is webapp's responsibility
                    log.info("⏹️ STOP addr=$addrInt")
                    log.info("🔄 STATE: $previousState → IDLE (hardware reset)")
                    log.info("⏹️ Auto-pumping STOPPED - Final volume: ${"%.2f".format(getVolumeMl()/1000.0)} L")
                    // Volume remains for webapp to read, but hardware is ready for next transaction
                } else {
                    log.warn("⚠️ STOP ignored addr=$addrInt - not pumping (state=$state)")
                }
                
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_RESET -> {
                val previousState = state.name
                resetVolume()
                frozenTransaction = null
                state = DispenserState.IDLE
                nozzleLifted = false
                pumpingActive.set(false)
                log.info("🔄 RESET addr=$addrInt")
                log.info("🔄 STATE: $previousState → IDLE")
                EhlCommandResult.OkAck(frame.addr)
            }
            else -> {
                log.info("❓ Unknown cmd=0x${cmd.toHex()} addr=$addrInt -> ACK")
                EhlCommandResult.OkAck(frame.addr)  // Generic ACK
            }
        }
    }

    private fun Byte.toHex(): String = String.format("%02X", this.toInt() and 0xFF)
    
    /**
     * Build VB6-compatible status byte for STATE response.
     * 
     * Bit flags:
     * - 0x01 = START_SWITCH_ACTIVE (authorized)
     * - 0x02 = NOZZLE_LIFTED
     * - 0x04 = DELIVERY_ACTIVE (pumping)
     * - 0x08 = TRANSACTION_COMPLETE (stopped/payment pending)
     */
    private fun buildStatusByte(): Byte {
        var statusByte = 0
        
        when (state) {
            DispenserState.IDLE -> {
                // All flags clear = IDLE
                statusByte = 0x00
            }
            DispenserState.AUTHORIZED -> {
                // Start switch active = AUTHORIZED
                statusByte = 0x01  // START_SWITCH_ACTIVE
                // Add nozzle lifted flag if lifted
                if (nozzleLifted) {
                    statusByte = statusByte or 0x02
                }
            }
            DispenserState.PUMPING -> {
                // Start switch + nozzle lifted + delivery active = PUMPING
                statusByte = 0x01 or 0x02 or 0x04  // START_SWITCH + NOZZLE_LIFTED + DELIVERY_ACTIVE
            }
            DispenserState.STOPPED -> {
                // Transaction complete flag = STOPPED
                statusByte = 0x08  // TRANSACTION_COMPLETE
            }
            DispenserState.PAYMENT_PENDING -> {
                // Payment pending - same as STOPPED for VB6 compatibility
                statusByte = 0x08  // TRANSACTION_COMPLETE
            }
        }
        
        return statusByte.toByte()
    }

    /**
     * Check if checksum should be corrupted (fault injection).
     */
    fun shouldCorruptChecksum(): Boolean {
        return badChecksumRate > 0.0 && Math.random() < badChecksumRate
    }
    
    /**
     * Generates a heartbeat status line for periodic logging.
     * Shows simulator is alive and current state at a glance.
     */
    fun heartbeatLine(): String {
        val volumeL = getVolumeMl() / 1000.0
        val priceKr = getPrice() / 100.0
        val pendingTx = if (frozenTransaction != null) " | TX PENDING: ${"%.2f".format(frozenTransaction!!.amountKr)} kr" else ""
        val faultStatus = buildString {
            if (disconnected) append(" | ⚠️ DISCONNECTED")
            if (powerfaulted) append(" | ⚡ POWER FAULT")
        }
        return "💓 SIM HEARTBEAT | addr=$defaultAddress | state=$state | vol=${"%.2f".format(volumeL)} L | price=${"%.2f".format(priceKr)} kr/L$pendingTx$faultStatus"
    }
}

/**
 * Dispenser state machine states.
 * Maps to VB6 protocol states and DispenserStateMapper domain states.
 */
enum class DispenserState {
    /** Pump idle - nozzle holstered, no activity */
    IDLE,
    /** Authorized for fueling - waiting for nozzle lift */
    AUTHORIZED,
    /** Active fuel delivery - volume incrementing */
    PUMPING,
    /** Delivery stopped - transaction data available */
    STOPPED,
    /** Payment pending - transaction frozen, waiting for settlement */
    PAYMENT_PENDING
}

/**
 * Frozen transaction data for payment pending state.
 */
data class FrozenTransaction(
    val volumeLitres: Double,
    val amountKr: Double,
    val pricePerLitreKr: Double
)

sealed class CommandResult {
    object OK : CommandResult()
    object ACK : CommandResult()
    object Ignored : CommandResult()
    data class Status(val blocked: Boolean) : CommandResult()
}

sealed class EhlCommandResult {
    /** 
     * VB6-compliant ACK: OK (0x1E) with data byte 0x30 (ASCII '0')
     * This matches VB6 behavior where ACK responses include payload byte 0x30.
     */
    data class OkAck(val addr: Byte) : EhlCommandResult()
    
    data class StateResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StateResponse
            return addr == other.addr && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * addr.toInt() + data.contentHashCode()
    }
    
    data class VolumeResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as VolumeResponse
            return addr == other.addr && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * addr.toInt() + data.contentHashCode()
    }
    
    data class PriceResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PriceResponse
            return addr == other.addr && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * addr.toInt() + data.contentHashCode()
    }
    
    /** ERROR_QUERY response (Alejandro tested) */
    data class ErrorQueryResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ErrorQueryResponse
            return addr == other.addr && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * addr.toInt() + data.contentHashCode()
    }
    
    /** TANKBIT response (Alejandro tested) */
    data class TankbitResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as TankbitResponse
            return addr == other.addr && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * addr.toInt() + data.contentHashCode()
    }
    
    /** LINETEST response - VB6 expects special pattern 0x55 0xAA */
    data class LinetestResponse(val addr: Byte) : EhlCommandResult()
}
