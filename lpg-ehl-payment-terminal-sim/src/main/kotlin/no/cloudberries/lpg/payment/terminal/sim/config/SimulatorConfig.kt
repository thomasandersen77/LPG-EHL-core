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
    val scenariosPath: String? = null
)
