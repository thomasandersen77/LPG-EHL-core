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
}

sealed class CommandResult {
    object OK : CommandResult()
    object ACK : CommandResult()
    object Ignored : CommandResult()
    data class Status(val blocked: Boolean) : CommandResult()
}
