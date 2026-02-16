package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

enum class EmulatorScenario {
    NORMAL,
    TIMEOUT,
    CHECKSUM_ERROR,
    NO_CONNECTION,
    EARLY_STOP
}

data class EmulatorStatus(
    val dispenserAddress: Int,
    val scenario: EmulatorScenario,
    val lastMessage: String?,
    val lastError: String?,
    val connected: Boolean
)

/**
 * Service for controlling emulator scenarios and behavior.
 * This allows testing of various error conditions and edge cases.
 * Only loaded when emulator.standalone.enabled=true.
 */
@Service
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class EmulatorScenarioService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val scenarios: MutableMap<Int, EmulatorScenario> = ConcurrentHashMap()
    private val lastMessages: MutableMap<Int, String?> = ConcurrentHashMap()
    private val lastErrors: MutableMap<Int, String?> = ConcurrentHashMap()
    private val connections: MutableMap<Int, Boolean> = ConcurrentHashMap()

    fun setScenario(dispenserAddress: Int, scenario: EmulatorScenario) {
        log.info("Setting emulator scenario for {} to {}", dispenserAddress, scenario)
        scenarios[dispenserAddress] = scenario
    }

    fun getScenario(dispenserAddress: Int): EmulatorScenario =
        scenarios[dispenserAddress] ?: EmulatorScenario.NORMAL

    fun updateLastMessage(dispenserAddress: Int, message: String) {
        lastMessages[dispenserAddress] = message
    }

    fun updateLastError(dispenserAddress: Int, error: String) {
        lastErrors[dispenserAddress] = error
    }

    fun setConnected(dispenserAddress: Int, connected: Boolean) {
        connections[dispenserAddress] = connected
    }

    fun reset(dispenserAddress: Int) {
        log.info("Reset emulator state for dispenser {}", dispenserAddress)
        scenarios.remove(dispenserAddress)
        lastMessages.remove(dispenserAddress)
        lastErrors.remove(dispenserAddress)
        connections.remove(dispenserAddress)
    }

    fun status(dispenserAddress: Int): EmulatorStatus =
        EmulatorStatus(
            dispenserAddress = dispenserAddress,
            scenario = getScenario(dispenserAddress),
            lastMessage = lastMessages[dispenserAddress],
            lastError = lastErrors[dispenserAddress],
            connected = connections[dispenserAddress] ?: false
        )
}
