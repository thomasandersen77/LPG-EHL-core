package no.cloudberries.lpg.netscloud

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NetsCloudConnectTerminalClientTest {

    private lateinit var config: NetsCloudConnectConfig
    private lateinit var authClient: NetsCloudAuthClient
    private lateinit var messageBuilder: NetsMessageBuilder
    private lateinit var responseParser: NetsResponseParser
    private lateinit var terminalClient: NetsCloudConnectTerminalClient

    @BeforeEach
    fun setUp() {
        config = NetsCloudConnectConfig(
            baseUrl = "https://test.nets.eu",
            username = "testuser",
            password = "testpass",
            terminalId = "12345678"
        )
        config.timeouts.openTerminalTimeoutMs = 5000
        config.timeouts.purchaseTimeoutMs = 30000
        config.timeouts.reversalTimeoutMs = 10000

        authClient = mockk()
        messageBuilder = mockk()
        responseParser = mockk()

        terminalClient = NetsCloudConnectTerminalClient(
            config = config,
            authClient = authClient,
            messageBuilder = messageBuilder,
            responseParser = responseParser
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `openTerminal should successfully open terminal and return success`() = runTest {
        // Given
        val mockToken = "mock-jwt-token"
        val loginResponse = NetsLoginResponse(
            token = mockToken,
            username = "testuser",
            terminals = listOf("12345678")
        )
        val openRequestJson = """{"command":"open"}"""
        val terminalReadyMessage = """{"status":"ready"}"""

        coEvery { authClient.login() } returns loginResponse
        every { messageBuilder.buildOpenRequest() } returns openRequestJson
        every { responseParser.isTerminalReady(terminalReadyMessage) } returns true
        every { responseParser.isError(any()) } returns false

        // Mock WebSocket client
        mockkConstructor(NetsCloudWebSocketClient::class)
        coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(mockToken) } just Runs
        coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(openRequestJson) } just Runs
        coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns terminalReadyMessage

        // When
        val result = terminalClient.openTerminal()

        // Then
        assertTrue(result.success)
        assertEquals("Terminal opened successfully", result.message)
        assertNull(result.error)

        coVerify(exactly = 1) { authClient.login() }
        verify(exactly = 1) { messageBuilder.buildOpenRequest() }
        coVerify(exactly = 1) { anyConstructed<NetsCloudWebSocketClient>().connect(mockToken) }
        coVerify(exactly = 1) { anyConstructed<NetsCloudWebSocketClient>().sendMessage(openRequestJson) }
    }

    @Test
    fun `openTerminal should return error when login fails`() = runTest {
        // Given
        coEvery { authClient.login() } throws Exception("Login failed")

        // When
        val result = terminalClient.openTerminal()

        // Then
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Login failed"))

        coVerify(exactly = 1) { authClient.login() }
    }

    @Test
    fun `openTerminal should return error when timeout waiting for TerminalReady`() = runTest {
        // Given
        val mockToken = "mock-jwt-token"
        val loginResponse = NetsLoginResponse(
            token = mockToken,
            username = "testuser",
            terminals = listOf("12345678")
        )
        val openRequestJson = """{"command":"open"}"""

        coEvery { authClient.login() } returns loginResponse
        every { messageBuilder.buildOpenRequest() } returns openRequestJson
        every { messageBuilder.buildLastResultRequest() } returns """{"command":"lastResult"}"""

        mockkConstructor(NetsCloudWebSocketClient::class)
        coEvery { anyConstructed<NetsCloudWebSocketClient>().connect(mockToken) } just Runs
        coEvery { anyConstructed<NetsCloudWebSocketClient>().sendMessage(any()) } just Runs  // Accept any message
        coEvery { anyConstructed<NetsCloudWebSocketClient>().receiveMessage(any()) } returns null

        // When
        val result = terminalClient.openTerminal()

        // Then
        assertFalse(result.success)
        assertEquals("Timeout waiting for TerminalReady", result.error)
    }

    @Test
    fun `purchase should return success when transaction is approved`() = runTest {
        // Given
        val request = TerminalPurchaseRequest(
            amountMinor = 10000,
            operatorId = "0001"
        )
        val purchaseRequestJson = """{"command":"purchase","amount":10000}"""
        val transactionCompleteMessage = """{"status":"complete","result":1,"Dfs13LocalMode":{}}"""
        val localMode = Dfs13LocalMode(
            result = 1,
            totalAmount = 10000,
            responseCode = "00"
        )

        every { messageBuilder.buildPurchaseRequest(10000, "0001") } returns purchaseRequestJson
        every { responseParser.isDisplayText(any()) } returns false
        every { responseParser.isPrintText(any()) } returns false
        every { responseParser.isTransactionComplete(transactionCompleteMessage) } returns true
        every { responseParser.parseLocalMode(transactionCompleteMessage) } returns localMode
        every { responseParser.isError(any()) } returns false

        // Setup WebSocket client mock
        val wsClient = mockk<NetsCloudWebSocketClient>()
        coEvery { wsClient.sendMessage(purchaseRequestJson) } just Runs
        coEvery { wsClient.receiveMessage(any()) } returns transactionCompleteMessage

        // Set the webSocketClient via reflection (since it's initialized in openTerminal)
        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        val isOpenField = terminalClient.javaClass.getDeclaredField("isTerminalOpen")
        isOpenField.isAccessible = true
        isOpenField.set(terminalClient, true)

        // When
        val result = terminalClient.purchase(request)

        // Then
        assertTrue(result.success)
        assertEquals(1, result.callResult)
        assertEquals("00", result.responseCode)
        assertNull(result.error)

        verify(exactly = 1) { messageBuilder.buildPurchaseRequest(10000, "0001") }
        coVerify(exactly = 1) { wsClient.sendMessage(purchaseRequestJson) }
    }

    @Test
    fun `purchase should return error when terminal is not open`() = runTest {
        // Given
        val request = TerminalPurchaseRequest(amountMinor = 10000)

        // When
        val result = terminalClient.purchase(request)

        // Then
        assertFalse(result.success)
        assertEquals("Terminal not open", result.error)
    }

    @Test
    fun `purchase should return error when transaction is declined`() = runTest {
        // Given
        val request = TerminalPurchaseRequest(amountMinor = 10000)
        val purchaseRequestJson = """{"command":"purchase","amount":10000}"""
        val transactionCompleteMessage = """{"status":"complete","result":0,"Dfs13LocalMode":{}}"""
        val localMode = Dfs13LocalMode(
            result = 0,
            totalAmount = 10000,
            responseCode = "05"
        )

        every { messageBuilder.buildPurchaseRequest(10000, "0000") } returns purchaseRequestJson
        every { responseParser.isDisplayText(any()) } returns false
        every { responseParser.isPrintText(any()) } returns false
        every { responseParser.isTransactionComplete(transactionCompleteMessage) } returns true
        every { responseParser.parseLocalMode(transactionCompleteMessage) } returns localMode
        every { responseParser.isError(any()) } returns false

        val wsClient = mockk<NetsCloudWebSocketClient>()
        coEvery { wsClient.sendMessage(purchaseRequestJson) } just Runs
        coEvery { wsClient.receiveMessage(any()) } returns transactionCompleteMessage

        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        val isOpenField = terminalClient.javaClass.getDeclaredField("isTerminalOpen")
        isOpenField.isAccessible = true
        isOpenField.set(terminalClient, true)

        // When
        val result = terminalClient.purchase(request)

        // Then
        assertFalse(result.success)
        assertEquals(0, result.callResult)
        assertEquals("05", result.responseCode)
    }

    @Test
    fun `reversal should return success when reversal is approved`() = runTest {
        // Given
        val reversalRequestJson = """{"command":"reversal"}"""
        val reversalCompleteMessage = """{"status":"complete","result":1,"Dfs13LocalMode":{}}"""
        val localMode = Dfs13LocalMode(
            result = 1,
            totalAmount = 10000,
            responseCode = "00"
        )

        every { messageBuilder.buildReversalRequest() } returns reversalRequestJson
        every { responseParser.isTransactionComplete(reversalCompleteMessage) } returns true
        every { responseParser.parseLocalMode(reversalCompleteMessage) } returns localMode

        val wsClient = mockk<NetsCloudWebSocketClient>()
        coEvery { wsClient.sendMessage(reversalRequestJson) } just Runs
        coEvery { wsClient.receiveMessage(any()) } returns reversalCompleteMessage

        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        // When
        val result = terminalClient.reversal()

        // Then
        assertTrue(result.success)
        assertEquals(1, result.callResult)
        assertEquals("00", result.responseCode)

        verify(exactly = 1) { messageBuilder.buildReversalRequest() }
        coVerify(exactly = 1) { wsClient.sendMessage(reversalRequestJson) }
    }

    @Test
    fun `reversal should return error when terminal is not open`() = runTest {
        // When
        val result = terminalClient.reversal()

        // Then
        assertFalse(result.success)
        assertEquals("Terminal not open", result.error)
    }

    @Test
    fun `reversal should return error when timeout occurs`() = runTest {
        // Given
        val reversalRequestJson = """{"command":"reversal"}"""

        every { messageBuilder.buildReversalRequest() } returns reversalRequestJson

        val wsClient = mockk<NetsCloudWebSocketClient>()
        coEvery { wsClient.sendMessage(reversalRequestJson) } just Runs
        coEvery { wsClient.receiveMessage(any()) } returns null

        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        // When
        val result = terminalClient.reversal()

        // Then
        assertFalse(result.success)
        assertEquals("Timeout waiting for reversal result", result.error)
    }

    @Test
    fun `closeTerminal should close WebSocket client and reset state`() = runTest {
        // Given
        val wsClient = mockk<NetsCloudWebSocketClient>()
        coEvery { wsClient.close() } just Runs

        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        val isOpenField = terminalClient.javaClass.getDeclaredField("isTerminalOpen")
        isOpenField.isAccessible = true
        isOpenField.set(terminalClient, true)

        // When
        val result = terminalClient.closeTerminal()

        // Then
        assertTrue(result.success)
        assertEquals("Terminal closed", result.message)

        coVerify(exactly = 1) { wsClient.close() }
    }

    @Test
    fun `getStatus should return CONNECTED when WebSocket client is connected`() = runTest {
        // Given
        val wsClient = mockk<NetsCloudWebSocketClient>()
        every { wsClient.isConnected() } returns true

        val field = terminalClient.javaClass.getDeclaredField("webSocketClient")
        field.isAccessible = true
        field.set(terminalClient, wsClient)

        val isOpenField = terminalClient.javaClass.getDeclaredField("isTerminalOpen")
        isOpenField.isAccessible = true
        isOpenField.set(terminalClient, true)

        // When
        val result = terminalClient.getStatus()

        // Then
        assertTrue(result.terminalOpen)
        assertTrue(result.terminalReady)
        assertEquals("CONNECTED", result.connectionState)
    }

    @Test
    fun `getStatus should return DISCONNECTED when WebSocket client is not connected`() = runTest {
        // When
        val result = terminalClient.getStatus()

        // Then
        assertFalse(result.terminalOpen)
        assertFalse(result.terminalReady)
        assertEquals("DISCONNECTED", result.connectionState)
    }

    @Test
    fun `getHealth should return healthy when terminal is open`() = runTest {
        // Given
        val isOpenField = terminalClient.javaClass.getDeclaredField("isTerminalOpen")
        isOpenField.isAccessible = true
        isOpenField.set(terminalClient, true)

        // When
        val result = terminalClient.getHealth()

        // Then
        assertEquals("healthy", result.status)
        assertTrue(result.configLoaded)
    }

    @Test
    fun `getHealth should return unhealthy when terminal is closed`() = runTest {
        // When
        val result = terminalClient.getHealth()

        // Then
        assertEquals("unhealthy", result.status)
        assertTrue(result.configLoaded)
    }
}
