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
        val (scenarioName, source) = resolveScenarioName(request)
        val definition = scenarioLoader.loadScenario(scenarioName)
        val enumScenario = definition?.name?.let { toEnum(it) } ?: toEnum(scenarioName) ?: Scenario.APPROVED

        log.debug("Scenario selected: name={}, enum={}, yamlLoaded={}", scenarioName, enumScenario, definition != null)
        return ScenarioSelection(scenarioName, enumScenario, definition, source)
    }

    /**
     * Get default scenario from configuration.
     */
    private fun resolveScenarioName(request: HttpServletRequest): Pair<String, ScenarioSource> {
        if (config.allowScenarioHeader) {
            val headerValue = request.getHeader(SCENARIO_HEADER)?.trim()
            if (!headerValue.isNullOrBlank()) {
                return headerValue.uppercase() to ScenarioSource.HEADER
            }
        }

        return config.defaultScenario.uppercase() to ScenarioSource.DEFAULT
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
        val scenarioDelay = timing?.operationDelayMs
        if (scenarioDelay != null) {
            val jitterEnabled = timing.jitter ?: config.enableRandomJitter
            return if (jitterEnabled) {
                Random.nextLong((scenarioDelay / 2).coerceAtLeast(1), scenarioDelay + 1)
            } else {
                scenarioDelay
            }
        }

        if (config.field.isEnabled(config.profile)) {
            val minDelay = config.field.operationDelayMinMs.coerceAtLeast(0)
            val maxDelay = config.field.operationDelayMaxMs.coerceAtLeast(minDelay + 1)
            return Random.nextLong(minDelay, maxDelay + 1)
        }

        val baseDelay = config.operationDelayMs
        val jitterEnabled = config.enableRandomJitter
        return if (jitterEnabled) {
            Random.nextLong((baseDelay / 2).coerceAtLeast(1), baseDelay + 1)
        } else {
            baseDelay
        }
    }

    fun applyScenarioTerminalState(selection: ScenarioSelection) {
        if (config.field.isEnabled(config.profile) && selection.source == ScenarioSource.DEFAULT) {
            if (Random.nextDouble() < config.field.notReadyProbability) {
                throw TerminalNotReadyException("Simulated terminal not ready (field)")
            }
        }
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

    fun selectFieldRejection(selection: ScenarioSelection): Scenario? {
        if (!config.field.isEnabled(config.profile)) {
            return null
        }
        if (selection.source != ScenarioSource.DEFAULT) {
            return null
        }
        if (selection.enumScenario != Scenario.APPROVED) {
            return null
        }
        if (Random.nextDouble() >= config.field.rejectionProbability) {
            return null
        }
        val wrongPin = Random.nextDouble() < config.field.rejectionWrongPinProbability
        return if (wrongPin) Scenario.WRONG_PIN else Scenario.USER_CANCEL
    }
}

data class ScenarioSelection(
    val name: String,
    val enumScenario: Scenario,
    val definition: ScenarioDefinition?,
    val source: ScenarioSource
)

enum class ScenarioSource {
    HEADER,
    DEFAULT
}
