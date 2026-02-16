package no.cloudberries.lpg.netscloud

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WireMockTest
class NetsCloudConnectTerminalClientIntegrationTest {

    private lateinit var config: NetsCloudConnectConfig
    private lateinit var authClient: NetsCloudAuthClient
    private lateinit var messageBuilder: NetsMessageBuilder
    private lateinit var responseParser: NetsResponseParser
    private lateinit var terminalClient: NetsCloudConnectTerminalClient
    private lateinit var mockWebSocketClient: NetsCloudWebSocketClient

    @BeforeEach
    fun setup(wmRuntimeInfo: WireMockRuntimeInfo) {
        // Setup config
        config = NetsCloudConnectConfig(
            baseUrl = "http://localhost:${wmRuntimeInfo.httpPort}",
            username = "testuser",
            password = "testpass",
            terminalId = "TEST-001",
            websocket = WebSocketConfig(),
            timeouts = TimeoutConfig(
                openTerminalTimeoutMs = 5000L,
                purchaseTimeoutMs = 30000L,
                reversalTimeoutMs = 10000L
            )
        )

        // Setup auth client
        authClient = mockk {
            coEvery { login() } returns NetsLoginResponse(
                token = "mock-jwt-token",
                username = "testuser",
                terminals = listOf("TERMINAL-001")
            )
        }

        // Setup message builder
        messageBuilder = mockk {
            every { buildOpenRequest() } returns """{"type":"open"}"""
            every { buildPurchaseRequest(any(), any()) } returns """{"type":"purchase"}"""
            every { buildReversalRequest() } returns """{"type":"reversal"}"""
        }

        // Setup response parser
        responseParser = mockk {
            every { isTerminalReady(any()) } answers { firstArg<String>().contains("TerminalReady") }
            every { isError(any()) } answers { firstArg<String>().contains("\"error\"") }
            every { parseError(any()) } returns "Mock error"
            every { isTransactionComplete(any()) } answers { firstArg<String>().contains("TransactionComplete") }
            every { isDisplayText(any()) } answers { firstArg<String>().contains("DisplayText") }
            every { isPrintText(any()) } answers { firstArg<String>().contains("PrintText") }
            every { isJsonReceived(any()) } returns false
            every { extractTerminalId(any()) } returns null
            every { parseDisplayText(any()) } returns "Mock display text"
            every { parsePrintText(any()) } returns "Mock print text"
            every { parseLocalMode(any()) } returns mockk {
                every { result } returns 1
                every { totalAmount } returns 10000
                every { responseCode } returns "00"
            }
            every { parseLastFinancialResult(any()) } returns mockk {
                every { result } returns 1
                every { totalAmount } returns 10000
                every { responseCode } returns "00"
            }
        }

        terminalClient = NetsCloudConnectTerminalClient(
            config,
            authClient,
            messageBuilder,
            responseParser
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    inner class OpenTerminalTests {

        @Test
        fun `should successfully open terminal when TerminalReady received`() = runTest {
            // Mock WebSocket client behavior
            val mockWsClient = mockk<NetsCloudWebSocketClient>(relaxed = true) {
                coEvery { connect(any()) } just Runs
                coEvery { sendMessage(any()) } just Runs
                coEvery { receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
                every { isConnected() } returns true
            }

            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            val response = terminalClient.openTerminal()

            assertTrue(response.success)
            assertEquals("Terminal opened successfully", response.message)
            coVerify { authClient.login() }
            coVerify { anyConstructed<NetsCloudWebSocketClient>().connect("mock-jwt-token") }
            coVerify { anyConstructed<NetsCloudWebSocketClient>().sendMessage("""{"type":"open"}""") }
        }

        @Test
        fun `should fail to open terminal when timeout occurs`() = runTest {
            every { messageBuilder.buildLastResultRequest() } returns """{"type":"lastResult"}"""

            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            // Return null for both primary and priming attempts
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns null

            val response = terminalClient.openTerminal()

            assertFalse(response.success)
            assertEquals("Timeout waiting for TerminalReady", response.error)

            // Verify priming was attempted
            verify { messageBuilder.buildLastResultRequest() }
        }

        @Test
        fun `should fail to open terminal when error received`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"error":"connection_failed"}"""

            val response = terminalClient.openTerminal()

            assertFalse(response.success)
            assertNotNull(response.error)
            assertTrue(response.error!!.contains("Terminal error"))
        }

        @Test
        fun `should fail to open terminal when login fails`() = runTest {
            coEvery { authClient.login() } throws RuntimeException("Login failed")

            val response = terminalClient.openTerminal()

            assertFalse(response.success)
            assertNotNull(response.error)
            assertTrue(response.error!!.contains("Login failed"))
        }

        @Test
        fun `should retry up to 5 attempts before giving up`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"other"}"""

            val response = terminalClient.openTerminal()

            assertFalse(response.success)
            assertTrue(response.error!!.contains("Failed to open terminal after 5 attempts"))
        }

        @Test
        fun `should successfully open terminal via priming when primary timeout occurs`() = runTest {
            every { messageBuilder.buildLastResultRequest() } returns """{"type":"lastResult"}"""

            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            // First call times out (null), second call (priming) returns TerminalReady
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returnsMany listOf(
                null,  // Primary timeout
                """{"type":"TerminalReady"}"""  // Priming success
            )

            val response = terminalClient.openTerminal()

            assertTrue(response.success)
            assertEquals("Terminal opened successfully (priming)", response.message)
            verify { messageBuilder.buildLastResultRequest() }
        }
    }

    @Nested
    inner class PurchaseTests {

        @BeforeEach
        fun setupOpenTerminal() = runTest {
            // Ensure terminal is open before purchase tests
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            terminalClient.openTerminal()
        }

        @Test
        fun `should successfully complete purchase transaction`() = runTest {
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""

            val request = TerminalPurchaseRequest(amountMinor = 10000, operatorId = "TEST")
            val response = terminalClient.purchase(request)

            assertTrue(response.success)
            assertEquals(1, response.callResult)
            assertEquals("00", response.responseCode)
            verify { messageBuilder.buildPurchaseRequest(10000, "TEST") }
        }

        @Test
        fun `should fail purchase when terminal not open`() = runTest {
            terminalClient.closeTerminal()

            val request = TerminalPurchaseRequest(amountMinor = 10000)
            val response = terminalClient.purchase(request)

            assertFalse(response.success)
            assertEquals("Terminal not open", response.error)
        }

        @Test
        fun `should accumulate display and print texts during purchase`() = runTest {
            val messages = listOf(
                """{"type":"DisplayText"}""",
                """{"type":"PrintText"}""",
                """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""
            )
            var callCount = 0
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } answers {
                messages[callCount++]
            }

            val request = TerminalPurchaseRequest(amountMinor = 5000)
            val response = terminalClient.purchase(request)

            assertTrue(response.success)
            assertNotNull(response.printTextSanitized)
            assertNotNull(response.lastDisplayText)
        }

        @Test
        fun `should timeout when no response received`() = runTest {
            config.timeouts.purchaseTimeoutMs = 100L
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns null

            val request = TerminalPurchaseRequest(amountMinor = 10000)
            val response = terminalClient.purchase(request)

            assertFalse(response.success)
            assertEquals("Timeout waiting for transaction result", response.error)
        }

        @Test
        fun `should handle error response during purchase`() = runTest {
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"error":"card_read_failed"}"""

            val request = TerminalPurchaseRequest(amountMinor = 10000)
            val response = terminalClient.purchase(request)

            assertFalse(response.success)
            assertTrue(response.error!!.contains("Terminal error"))
        }

        @Test
        fun `should handle declined transaction`() = runTest {
            every { responseParser.parseLocalMode(any()) } returns mockk {
                every { result } returns 0
                every { totalAmount } returns 10000
                every { responseCode } returns "51"
            }
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""

            val request = TerminalPurchaseRequest(amountMinor = 10000)
            val response = terminalClient.purchase(request)

            assertFalse(response.success)
            assertEquals(0, response.callResult)
        }
    }

    @Nested
    inner class ReversalTests {

        @BeforeEach
        fun setupOpenTerminal() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            terminalClient.openTerminal()
        }

        @Test
        fun `should successfully complete reversal`() = runTest {
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""

            val response = terminalClient.reversal()

            assertTrue(response.success)
            assertEquals(1, response.callResult)
            verify { messageBuilder.buildReversalRequest() }
        }

        @Test
        fun `should fail reversal when terminal not open`() = runTest {
            terminalClient.closeTerminal()

            val response = terminalClient.reversal()

            assertFalse(response.success)
            assertEquals("Terminal not open", response.error)
        }

        @Test
        fun `should timeout when no reversal response received`() = runTest {
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns null

            val response = terminalClient.reversal()

            assertFalse(response.success)
            assertEquals("Timeout waiting for reversal result", response.error)
        }

        @Test
        fun `should handle declined reversal`() = runTest {
            every { responseParser.parseLocalMode(any()) } returns mockk {
                every { result } returns 0
                every { totalAmount } returns null
                every { responseCode } returns "12"
            }
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""

            val response = terminalClient.reversal()

            assertFalse(response.success)
            assertEquals(0, response.callResult)
        }

        @Test
        fun `should handle exception during reversal`() = runTest {
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } throws RuntimeException("Network error")

            val response = terminalClient.reversal()

            assertFalse(response.success)
            assertTrue(response.error!!.contains("Network error"))
        }
    }

    @Nested
    inner class CloseTerminalTests {

        @Test
        fun `should successfully close terminal`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true
            coEvery { anyConstructed<NetsCloudWebSocketClient>().close() } just Runs

            terminalClient.openTerminal()
            val response = terminalClient.closeTerminal()

            assertTrue(response.success)
            assertEquals("Terminal closed", response.message)
        }

        @Test
        fun `should handle exception when closing terminal`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true
            coEvery { anyConstructed<NetsCloudWebSocketClient>().close() } throws RuntimeException("Close failed")

            terminalClient.openTerminal()
            val response = terminalClient.closeTerminal()

            assertFalse(response.success)
            assertTrue(response.error!!.contains("Close failed"))
        }
    }

    @Nested
    inner class HealthAndStatusTests {

        @Test
        fun `should return unhealthy when terminal not open`() {
            val health = terminalClient.getHealth()

            assertEquals("unhealthy", health.status)
            assertTrue(health.configLoaded)
        }

        @Test
        fun `should return healthy when terminal open`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            terminalClient.openTerminal()
            val health = terminalClient.getHealth()

            assertEquals("healthy", health.status)
            assertTrue(health.configLoaded)
        }

        @Test
        fun `should return correct status when terminal closed`() {
            val status = terminalClient.getStatus()

            assertFalse(status.terminalOpen)
            assertFalse(status.terminalReady)
            assertEquals("DISCONNECTED", status.connectionState)
        }

        @Test
        fun `should return correct status when terminal open`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            terminalClient.openTerminal()
            val status = terminalClient.getStatus()

            assertTrue(status.terminalOpen)
            assertTrue(status.terminalReady)
            assertEquals("CONNECTED", status.connectionState)
        }
    }

    @Nested
    inner class ConcurrencyTests {

        @Test
        fun `should handle sequential operations with mutex lock`() = runTest {
            mockkConstructor(NetsCloudWebSocketClient::class)
            coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs
            coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns """{"type":"TerminalReady"}""" andThen """{"type":"TransactionComplete","Dfs13LocalMode":{}}"""
            every { anyConstructed<NetsCloudWebSocketClient>().isConnected() } returns true

            terminalClient.openTerminal()
            val request = TerminalPurchaseRequest(amountMinor = 10000)
            val purchaseResponse = terminalClient.purchase(request)

            assertTrue(purchaseResponse.success)
        }
    }
}