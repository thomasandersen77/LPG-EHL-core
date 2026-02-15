package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.payment.terminal.sim.baxi.BaxiTcpServer
import no.cloudberries.lpg.payment.terminal.sim.config.BaxiConfig
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BaxiIntegrationTest {

    private lateinit var tcpServer: BaxiTcpServer
    private lateinit var terminalClient: BaxiTerminalClient
    private lateinit var stateManager: TerminalStateManager
    private val port = 7202 // Bruk nc port

    @BeforeEach
    fun setup() {
        val simulatorConfig = mock<SimulatorConfig>()
        whenever(simulatorConfig.baxi).thenReturn(BaxiConfig(enabled = true, port = port))
        whenever(simulatorConfig.defaultScenario).thenReturn("APPROVED")
        whenever(simulatorConfig.operationDelayMs).thenReturn(100L)
        whenever(simulatorConfig.enableRandomJitter).thenReturn(false)
        whenever(simulatorConfig.field).thenReturn(no.cloudberries.lpg.payment.terminal.sim.config.FieldModeConfig())
        whenever(simulatorConfig.profile).thenReturn("lab")

        stateManager = TerminalStateManager(simulatorConfig)
        val scenarioManager = ScenarioManager(simulatorConfig, mock())

        tcpServer = BaxiTcpServer(simulatorConfig, stateManager, scenarioManager)
        tcpServer.start()

        terminalClient = BaxiTerminalClient("127.0.0.1", port)
    }

    @AfterEach
    fun teardown() {
        terminalClient.closeTerminal()
        tcpServer.stop()
    }

    @Test
    fun `should complete purchase against simulator`() {
        val start = System.currentTimeMillis()
        val openResult = terminalClient.openTerminal()
        println("Open result took ${System.currentTimeMillis() - start} ms: $openResult")
        assertThat(openResult.success).isTrue()

        val request = TerminalPurchaseRequest(amountMinor = 1000)
        val response = terminalClient.purchase(request)

        assertThat(response.success).isTrue()
        assertThat(response.responseCode).isEqualTo("00")
        assertThat(response.printTextSanitized).contains("GODKJENT")
    }
}
