package no.cloudberries.lpg.payment.terminal.sim.model.scenario

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioDefinition(
    val version: Int = 1,
    val name: String,
    val result: ScenarioResult? = null,
    val terminalState: ScenarioTerminalState = ScenarioTerminalState(),
    val flow: List<ScenarioFlowEvent> = emptyList(),
    val receipt: ScenarioReceipt? = null,
    val timing: ScenarioTiming? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioResult(
    val success: Boolean = true,
    val responseCode: String? = null,
    val localModeResult: Int? = null,
    val rejectionSource: String? = null,
    val rejectionReason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioTerminalState(
    val requiresReady: Boolean = true,
    val busy: Boolean = false,
    val notReady: Boolean = false,
    val timeout: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioFlowEvent(
    val event: String,
    val text: String? = null,
    val displayTextId: Int? = null,
    val delayMs: Long? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioReceipt(
    val template: String = "purchase",
    val approved: Boolean? = null,
    val responseCode: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioTiming(
    val operationDelayMs: Long? = null,
    val jitter: Boolean? = null
)