package no.cloudberries.lpg.payment.terminal.sim.service

import jakarta.servlet.http.HttpServletRequest
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.exception.OperationTimeoutException
import no.cloudberries.lpg.payment.terminal.sim.exception.TerminalBusyException
import no.cloudberries.lpg.payment.terminal.sim.exception.TerminalNotReadyException
import no.cloudberries.lpg.payment.terminal.sim.model.domain.Scenario
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * Scenario selection and management.
 *
 * Scenarios can be selected via:
 * 1. X-Terminal-Scenario HTTP header (if enabled)
 * 2. Default configuration (payment-terminal-sim.default-scenario)
 */
@Service
class ScenarioManager(
    private val config: SimulatorConfig,
    private val scenarioLoader: ScenarioLoader
) {
    private val log = LoggerFactory.getLogger(ScenarioManager::class.java)

    companion object {
        const val SCENARIO_HEADER = "X-Terminal-Scenario"
    }

    /**
     * Select scenario for the current request.
     *
     * Priority:
     * 1. X-Terminal-Scenario header (if enabled)
     * 2. Default configuration
     */
    fun selectScenario(request: HttpServletRequest): ScenarioSelection {
        val scenarioName = resolveScenarioName(request)
        val definition = scenarioLoader.loadScenario(scenarioName)
        val enumScenario = definition?.name?.let { toEnum(it) } ?: toEnum(scenarioName) ?: Scenario.APPROVED

        log.debug("Scenario selected: name={}, enum={}, yamlLoaded={}", scenarioName, enumScenario, definition != null)
        return ScenarioSelection(scenarioName, enumScenario, definition)
    }

    /**
     * Get default scenario from configuration.
     */
    private fun resolveScenarioName(request: HttpServletRequest): String {
        if (config.allowScenarioHeader) {
            val headerValue = request.getHeader(SCENARIO_HEADER)?.trim()
            if (!headerValue.isNullOrBlank()) {
                return headerValue.uppercase()
            }
        }

        return config.defaultScenario.uppercase()
    }

    private fun toEnum(value: String): Scenario? {
        return try {
            Scenario.valueOf(value.uppercase())
        } catch (ex: IllegalArgumentException) {
            null
        }
    }

    /**
     * Get simulated operation delay in milliseconds.
     *
     * Returns configured delay, optionally with random jitter.
     */
    fun getOperationDelay(selection: ScenarioSelection? = null): Long {
        val timing = selection?.definition?.timing
        val baseDelay = timing?.operationDelayMs ?: config.operationDelayMs
        val jitterEnabled = timing?.jitter ?: config.enableRandomJitter
        return if (jitterEnabled) {
            Random.nextLong(baseDelay / 2, baseDelay)
        } else {
            baseDelay
        }
    }

    fun applyScenarioTerminalState(selection: ScenarioSelection) {
        val terminalState = selection.definition?.terminalState
        when {
            terminalState?.timeout == true -> throw OperationTimeoutException("Simulated timeout")
            terminalState?.busy == true -> throw TerminalBusyException("Simulated terminal busy")
            terminalState?.notReady == true -> throw TerminalNotReadyException("Simulated terminal not ready")
            selection.enumScenario == Scenario.TIMEOUT -> throw OperationTimeoutException("Simulated timeout")
            selection.enumScenario == Scenario.BUSY -> throw TerminalBusyException("Simulated terminal busy")
            selection.enumScenario == Scenario.NOT_READY -> throw TerminalNotReadyException("Simulated terminal not ready")
        }
    }
}

data class ScenarioSelection(
    val name: String,
    val enumScenario: Scenario,
    val definition: ScenarioDefinition?
)
