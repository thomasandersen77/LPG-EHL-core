package no.cloudberries.lpg.emulator

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

enum class EmulatorScenario {
    NORMAL,
    TIMEOUT,
    CHECKSUM_ERROR,
    NO_CONNECTION
}

data class EmulatorStatus(
    val dispenserAddress: Int,
    val scenario: EmulatorScenario,
    val lastMessage: String?,
    val lastError: String?,
    val connected: Boolean
)

/**
 * Simple in-memory emulator configuration service. This does NOT simulate
 * the EHL protocol by itself – it is intended to be read by your
 * SerialPortIO / emulator implementation so that you can inject faults,
 * timeouts etc.
 */
@Service
@Profile("local", "dev", "h2")
class EmulatorService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val scenarios: MutableMap<Int, EmulatorScenario> = mutableMapOf()
    private val lastMessages: MutableMap<Int, String?> = mutableMapOf()
    private val lastErrors: MutableMap<Int, String?> = mutableMapOf()
    private val connections: MutableMap<Int, Boolean> = mutableMapOf()

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
