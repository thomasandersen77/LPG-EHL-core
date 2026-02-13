package no.cloudberries.lpg.payment.terminal.sim.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment-terminal-sim")
data class SimulatorConfig(
    val defaultScenario: String = "APPROVED",
    val operationDelayMs: Long = 2000,
    val enableRandomJitter: Boolean = true,
    val terminalId: String = "12345678",
    val merchantId: String = "12345678901234",
    val allowScenarioHeader: Boolean = true,
    val eventBufferSize: Int = 1000,
    val scenariosEnabled: Boolean = true,
    val scenariosPath: String? = null,
    val profile: String = "lab",
    val responseCasing: String = "PascalCase",
    val field: FieldModeConfig = FieldModeConfig()
)

data class FieldModeConfig(
    val operationDelayMinMs: Long = 2000,
    val operationDelayMaxMs: Long = 10000,
    val notReadyProbability: Double = 0.05,
    val rejectionProbability: Double = 0.1,
    val rejectionWrongPinProbability: Double = 0.5
) {
    fun isEnabled(profile: String): Boolean = profile.equals("field", ignoreCase = true)
}
