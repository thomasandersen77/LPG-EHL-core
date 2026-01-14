package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages PLS state for dispensers.
 */
class PlsState {
    private val log = LoggerFactory.getLogger(PlsState::class.java)
    
    private val dispenserBlocked = ConcurrentHashMap<Int, Boolean>()

    init {
        // Initialize dispenser 1 as blocked by default
        dispenserBlocked[1] = true
    }

    fun isBlocked(dispenserId: Int): Boolean = dispenserBlocked.getOrDefault(dispenserId, true)

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
        
        return when (cmd) {
            EhlFrameCodec.CMD_LINETEST -> {
                log.debug("LINETEST from addr 0x{}", frame.addr.toHex())
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STATE -> {
                log.debug("STATE from addr 0x{}", frame.addr.toHex())
                // Return STATE response with 1 byte: 0x30 = ready/idle state
                EhlCommandResult.StateResponse(frame.addr, byteArrayOf(0x30))
            }
            EhlFrameCodec.CMD_VOLUME -> {
                log.debug("VOLUME from addr 0x{}", frame.addr.toHex())
                // Return VOLUME response with 4 bytes of zeros (no volume delivered)
                EhlCommandResult.VolumeResponse(frame.addr, byteArrayOf(0x30, 0x30, 0x30, 0x30))
            }
            EhlFrameCodec.CMD_BLOCK -> {
                setBlocked(1, true)
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_UNBLOCK -> {
                setBlocked(1, false)
                EhlCommandResult.OkAck(frame.addr)
            }
            EhlFrameCodec.CMD_STOP -> {
                setBlocked(1, true)
                EhlCommandResult.OkAck(frame.addr)
            }
            else -> {
                log.debug("Unknown EHL command: 0x{}", cmd.toHex())
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
            if (addr != other.addr) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }
        override fun hashCode(): Int {
            var result = addr.toInt()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
    data class VolumeResponse(val addr: Byte, val data: ByteArray) : EhlCommandResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as VolumeResponse
            if (addr != other.addr) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }
        override fun hashCode(): Int {
            var result = addr.toInt()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
