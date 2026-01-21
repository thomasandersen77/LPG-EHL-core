package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages PLS state for dispensers.
 * 
 * @param defaultAddress Default dispenser address (1-8)
 * @param priceCents Price per liter in cents (e.g., 1590 = 15.90 kr/L)
 * @param initiallyBlocked Whether dispensers start in blocked state
 */
class PlsState(
    private val defaultAddress: Int = 1,
    private val priceCents: Int = 1590,
    initiallyBlocked: Boolean = true
) {
    private val log = LoggerFactory.getLogger(PlsState::class.java)
    
    private val dispenserBlocked = ConcurrentHashMap<Int, Boolean>()
    private val currentVolumeMl = AtomicLong(0L)  // Volume in milliliters
    private val currentPriceCents = AtomicInteger(priceCents)

    init {
        // Initialize default dispenser with configured state
        dispenserBlocked[defaultAddress] = initiallyBlocked
        log.info("PLS State initialized: address=$defaultAddress, price=${priceCents/100.0} kr/L, blocked=$initiallyBlocked")
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
     */
    fun processEhlCommand(frame: EhlFrame): EhlCommandResult {
        val cmd = frame.cmd
        val addrInt = (frame.addr.toInt() and 0xFF) - 0x30  // Convert ASCII '1' (0x31) to 1

        return when (cmd) {
            EhlFrameCodec.CMD_LINETEST -> {
                log.info("🔗 LINETEST from dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STATE -> {
                val blocked = isBlocked(addrInt)
                // State codes: 0x30=ready, 0x31=blocked, 0x32=pumping, etc.
                val stateCode = if (blocked) 0x31.toByte() else 0x30.toByte()
                log.info("📊 STATE request from dispenser $addrInt -> ${if(blocked) "BLOCKED" else "READY"}")
                EhlCommandResult.StateResponse(frame.addr, byteArrayOf(stateCode))
            }
            EhlFrameCodec.CMD_VOLUME -> {
                // Return VOLUME response with 4 ASCII digits representing liters * 100
                val volumeL100 = (getVolumeMl() / 10).toInt()  // Convert mL to cL
                val volumeStr = "%04d".format(volumeL100.coerceIn(0, 9999))
                val volumeBytes = volumeStr.map { it.code.toByte() }.toByteArray()
                log.info("⛽ VOLUME request from dispenser $addrInt -> ${getVolumeMl()/1000.0} L")
                EhlCommandResult.VolumeResponse(frame.addr, volumeBytes)
            }
            EhlFrameCodec.CMD_PRICE -> {
                // Return PRICE response with 4 ASCII digits representing price in cents
                val priceStr = "%04d".format(getPrice().coerceIn(0, 9999))
                val priceBytes = priceStr.map { it.code.toByte() }.toByteArray()
                log.info("💰 PRICE request from dispenser $addrInt -> ${getPrice()/100.0} kr/L")
                EhlCommandResult.PriceResponse(frame.addr, priceBytes)
            }
            EhlFrameCodec.CMD_BLOCK -> {
                setBlocked(addrInt, true)
                log.info("🛑 BLOCK command for dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_UNBLOCK -> {
                setBlocked(addrInt, false)
                log.info("✅ UNBLOCK command for dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STOP -> {
                setBlocked(addrInt, true)
                resetVolume()
                log.info("⏹️ STOP command for dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_RESET -> {
                resetVolume()
                log.info("🔄 RESET command for dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)
            }
            else -> {
                log.info("❓ Unknown EHL command: 0x${cmd.toHex()} from dispenser $addrInt")
                EhlCommandResult.OkAck(frame.addr)  // Generic ACK
            }
        }
    }

    private fun Byte.toHex(): String = "%02X".format(this)
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
}
