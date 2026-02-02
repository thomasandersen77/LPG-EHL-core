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
 * Supports both standard addresses (1-8) and legacy addresses (32+n).
 * Based on Alejandro's field testing findings.
 * 
 * @param defaultAddress Default dispenser address (1-8 or 33-40 for legacy)
 * @param priceCents Price per liter in cents (e.g., 1590 = 15.90 kr/L)
 * @param initiallyBlocked Whether dispensers start in blocked state
 * @param flowRateMlPerSecond Simulated flow rate in ml/second (default: 500 = 0.5 L/s)
 * @param legacyAddressEnabled If true, also responds to legacy address (32 + defaultAddress)
 */
class PlsState(
    private val defaultAddress: Int = 1,
    private val priceCents: Int = 1590,
    initiallyBlocked: Boolean = true,
    private val flowRateMlPerSecond: Int = 500,
    private val legacyAddressEnabled: Boolean = true
) {
    private val log = LoggerFactory.getLogger(PlsState::class.java)
    
    private val dispenserBlocked = ConcurrentHashMap<Int, Boolean>()
    private val currentVolumeMl = AtomicLong(0L)  // Volume in milliliters
    private val currentPriceCents = AtomicInteger(priceCents)
    
    // Auto-pumping simulation
    private val pumpingActive = AtomicBoolean(false)
    @Volatile private var pumpingThread: Thread? = null
    @Volatile private var running = true

    // Legacy address = 32 + defaultAddress (Alejandro's finding)
    private val legacyAddress: Int = 32 + defaultAddress
    
    init {
        // Initialize default dispenser with configured state
        dispenserBlocked[defaultAddress] = initiallyBlocked
        if (legacyAddressEnabled) {
            dispenserBlocked[legacyAddress] = initiallyBlocked
        }
        
        val addressInfo = if (legacyAddressEnabled) 
            "address=$defaultAddress (+ legacy=$legacyAddress)" 
        else 
            "address=$defaultAddress"
        log.info("PLS State initialized: $addressInfo, price=${priceCents/100.0} kr/L, blocked=$initiallyBlocked, flowRate=${flowRateMlPerSecond}ml/s")
        
        // Start auto-pumping simulation thread
        startAutoPumpingThread()
        
        // If starting unblocked, enable auto-pumping immediately
        if (!initiallyBlocked) {
            pumpingActive.set(true)
            log.info("▶️ Auto-pumping enabled at startup (unblocked mode)")
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
                    
                    if (pumpingActive.get() && !isBlocked(defaultAddress)) {
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
     * Stop the auto-pumping thread (call on shutdown).
     */
    fun shutdown() {
        running = false
        pumpingThread?.interrupt()
    }

    fun isBlocked(dispenserId: Int): Boolean = dispenserBlocked.getOrDefault(dispenserId, true)
    
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

    fun setBlocked(dispenserId: Int, blocked: Boolean) {
        val previous = dispenserBlocked.put(dispenserId, blocked)
        if (previous != blocked) {
            log.info("Dispenser {} state changed: {}", dispenserId, if (blocked) "BLOCKED" else "UNBLOCKED")
            
            // Control auto-pumping based on blocked state
            if (dispenserId == defaultAddress) {
                if (blocked) {
                    // Stop pumping when blocked
                    pumpingActive.set(false)
                    log.info("⏹️ Auto-pumping STOPPED - Final volume: ${"%.2f".format(getVolumeMl()/1000.0)} L")
                } else {
                    // Start pumping when unblocked
                    pumpingActive.set(true)
                    log.info("▶️ Auto-pumping STARTED at ${flowRateMlPerSecond}ml/s")
                }
            }
        }
    }

    fun processCommand(command: String): CommandResult {
        val upperCmd = command.uppercase().trim()
        
        return when {
            upperCmd.contains("FREE") || upperCmd.contains("UNBLOCK") -> {
                setBlocked(1, false)
                CommandResult.OK
            }
            upperCmd.contains("STOP") || upperCmd.contains("BLOCK") -> {
                setBlocked(1, true)
                CommandResult.OK
            }
            upperCmd.contains("STATUS") -> {
                CommandResult.Status(isBlocked(1))
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
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STATE -> {
                val blocked = isBlocked(addrInt)
                val volumeMl = getVolumeMl()
                // Core expects bitmask:
                // 0x00 = IDLE (blocked)
                // 0x04 = AUTHORIZED/READY (unblocked, volume=0)
                // 0x06 = PUMPING (unblocked, volume>0, nozzle lifted + delivery active)
                val statusByte: Byte = when {
                    blocked -> 0x00
                    volumeMl > 0 -> 0x06  // DELIVERY_ACTIVE (0x04) + NOZZLE_LIFTED (0x02)
                    else -> 0x04  // Just AUTHORIZED/READY
                }
                val statusName = when (statusByte.toInt()) {
                    0x00 -> "IDLE"
                    0x04 -> "READY"
                    0x06 -> "PUMPING"
                    else -> "UNKNOWN"
                }
                log.debug("📊 STATE addr=$addrInt -> $statusName (0x${statusByte.toHex()}, vol=${volumeMl}ml)")
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
                setBlocked(addrInt, true)
                log.info("🛑 BLOCK addr=$addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_UNBLOCK -> {
                setBlocked(addrInt, false)
                log.info("✅ UNBLOCK addr=$addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STOP -> {
                setBlocked(addrInt, true)
                resetVolume()
                log.info("⏹️ STOP addr=$addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_RESET -> {
                resetVolume()
                log.info("🔄 RESET addr=$addrInt")
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
     * Generates a heartbeat status line for periodic logging.
     * Shows simulator is alive and current state at a glance.
     */
    fun heartbeatLine(): String {
        val blocked = isBlocked(defaultAddress)
        val volumeL = getVolumeMl() / 1000.0
        val priceKr = getPrice() / 100.0
        return "💓 SIM HEARTBEAT | addr=$defaultAddress | blocked=$blocked | vol=${"%.2f".format(volumeL)} L | price=${"%.2f".format(priceKr)} kr/L"
    }
}

sealed class CommandResult {
    object OK : CommandResult()
    object ACK : CommandResult()
    object Ignored : CommandResult()
    data class Status(val blocked: Boolean) : CommandResult()
}

sealed class EhlCommandResult {
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
}
