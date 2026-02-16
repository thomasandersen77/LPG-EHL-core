package no.cloudberries.lpg.netscloud

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-connect")
class NetsCloudConnectTerminalClient(
    private val config: NetsCloudConnectConfig,
    private val authClient: NetsCloudAuthClient,
    private val messageBuilder: NetsMessageBuilder,
    private val responseParser: NetsResponseParser
) : TerminalClient {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val operationLock = Mutex()
    
    private var webSocketClient: NetsCloudWebSocketClient? = null
    private var isTerminalOpen = false
    
    override fun openTerminal(): TerminalSimpleResponse = runBlocking {
        operationLock.withLock {
            try {
                logger.info("━".repeat(60))
                logger.info("🔌 Opening Nets Cloud Connect terminal...")
                logger.info("━".repeat(60))
                
                // 1. Login and get JWT token
                logger.info("STEP 1/3: Login")
                val loginResponse = authClient.login()
                logger.info("   ✅ Got JWT token")
                logger.info("   ✅ Terminals: ${loginResponse.terminals}")
                
                // 2. Connect WebSocket
                logger.info("STEP 2/3: WebSocket Connect")
                val wsClient = NetsCloudWebSocketClient(config, responseParser)
                wsClient.connect(loginResponse.token)
                webSocketClient = wsClient
                logger.info("   ✅ WebSocket connected")

                // 3. Send Open command
                logger.info("STEP 3/3: Send Open command")
                val openRequest = messageBuilder.buildOpenRequest()
                wsClient.sendMessage(openRequest)
                logger.info("   ✅ Open command sent")

                // 4. Wait for TerminalReady
                logger.info("⏳ Waiting for TerminalReady...")
                val primaryTimeout = config.timeouts.openTerminalTimeoutMs
                var attempts = 0
                val maxAttempts = 5

                while (attempts < maxAttempts) {
                    val message = wsClient.receiveMessage(primaryTimeout)

                    if (message == null) {
                        // MANGLER 1: Priming/fallback - send admin 12605 to trigger response
                        if (attempts == 0) {
                            logger.warn("⚠️  Primary timeout - trying priming with LastResult command...")
                            val lastResultRequest = messageBuilder.buildLastResultRequest()
                            wsClient.sendMessage(lastResultRequest)
                            logger.info("   ✅ LastResult priming command sent")

                            // Give it shorter timeout for priming attempt
                            val primingMessage = wsClient.receiveMessage(10_000)
                            if (primingMessage != null && responseParser.isTerminalReady(primingMessage)) {
                                isTerminalOpen = true
                                logger.info("━".repeat(60))
                                logger.info("✅ TERMINAL READY (via priming)!")
                                logger.info("━".repeat(60))
                                return@runBlocking TerminalSimpleResponse(
                                    success = true,
                                    message = "Terminal opened successfully (priming)"
                                )
                            }
                        }

                        logger.error("❌ Timeout waiting for TerminalReady")
                        return@runBlocking TerminalSimpleResponse(
                            success = false,
                            error = "Timeout waiting for TerminalReady"
                        )
                    }

                    logger.debug("   Message #${attempts + 1}: ${message.take(100)}...")

                    if (responseParser.isTerminalReady(message)) {
                        isTerminalOpen = true
                        logger.info("━".repeat(60))
                        logger.info("✅ TERMINAL READY!")
                        logger.info("━".repeat(60))
                        return@runBlocking TerminalSimpleResponse(
                            success = true,
                            message = "Terminal opened successfully"
                        )
                    }

                    if (responseParser.isError(message)) {
                        val error = responseParser.parseError(message)
                        logger.error("❌ Terminal error: $error")
                        return@runBlocking TerminalSimpleResponse(
                            success = false,
                            error = "Terminal error: $error"
                        )
                    }

                    attempts++
                }

                logger.error("❌ Failed to open terminal after $attempts attempts")
                TerminalSimpleResponse(
                    success = false,
                    error = "Failed to open terminal after $attempts attempts"
                )
                
            } catch (e: Exception) {
                logger.error("💥 Failed to open terminal", e)
                TerminalSimpleResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse = runBlocking {
        operationLock.withLock {
            val wsClient = webSocketClient ?: run {
                logger.error("❌ Terminal not open")
                return@runBlocking TerminalOperationResponse(
                    success = false,
                    error = "Terminal not open"
                )
            }

            if (!isTerminalOpen) {
                logger.error("❌ Terminal not open")
                return@runBlocking TerminalOperationResponse(
                    success = false,
                    error = "Terminal not open"
                )
            }

            try {
                logger.info("━".repeat(60))
                logger.info("💳 Starting purchase: ${request.amountMinor} øre (${request.amountMinor / 100.0} kr)")
                logger.info("━".repeat(60))
                
                // 1. Send Purchase command
                val purchaseRequest = messageBuilder.buildPurchaseRequest(
                    request.amountMinor,
                    request.operatorId
                )
                wsClient.sendMessage(purchaseRequest)
                logger.info("✅ Purchase command sent")
                logger.info("⏳ Waiting for card tap...")

                // 2. Accumulate responses
                val displayTexts = mutableListOf<String>()
                val printTexts = mutableListOf<String>()

                // 3. Wait for transaction complete
                val startTime = System.currentTimeMillis()
                val timeout = config.timeouts.purchaseTimeoutMs

                var messageCount = 0
                while ((System.currentTimeMillis() - startTime) < timeout) {
                    val message = wsClient.receiveMessage(timeout)
                    
                    if (message == null) {
                        logger.error("❌ Timeout waiting for transaction result")
                        return@runBlocking TerminalOperationResponse(
                            success = false,
                            error = "Timeout waiting for transaction result"
                        )
                    }
                    
                    messageCount++
                    logger.debug("   Message #$messageCount: ${message.take(100)}...")
                    
                    // Accumulate display/print text
                    if (responseParser.isDisplayText(message)) {
                        responseParser.parseDisplayText(message)?.let { 
                            displayTexts.add(it)
                            logger.info("📺 Display: $it")
                        }
                    }
                    if (responseParser.isPrintText(message)) {
                        responseParser.parsePrintText(message)?.let { 
                            printTexts.add(it)
                            logger.debug("🖨️  Print: $it")
                        }
                    }
                    
                    // Check for completion (Dfs13LocalMode or Dfs13LastFinancialResult)
                    if (responseParser.isTransactionComplete(message)) {
                        val localMode = if (message.contains("Dfs13LocalMode")) {
                            responseParser.parseLocalMode(message)
                        } else {
                            responseParser.parseLastFinancialResult(message)
                        }
                        val duration = System.currentTimeMillis() - startTime

                        logger.info("━".repeat(60))
                        if (localMode?.result == 1) {
                            logger.info("✅ TRANSACTION APPROVED!")
                        } else {
                            logger.error("❌ TRANSACTION DECLINED!")
                        }
                        logger.info("   Result: ${localMode?.result}")
                        logger.info("   Amount: ${localMode?.totalAmount} øre")
                        logger.info("   Response Code: ${localMode?.responseCode}")
                        logger.info("   Duration: ${duration}ms")
                        logger.info("━".repeat(60))

                        return@runBlocking TerminalOperationResponse(
                            success = localMode?.result == 1,
                            callResult = localMode?.result,
                            responseCode = localMode?.responseCode,
                            printTextSanitized = printTexts.joinToString("\n"),
                            lastDisplayText = displayTexts.lastOrNull(),
                            durationMs = duration
                        )
                    }
                    
                    // Check for error
                    if (responseParser.isError(message)) {
                        val error = responseParser.parseError(message)
                        logger.error("❌ Terminal error: $error")
                        return@runBlocking TerminalOperationResponse(
                            success = false,
                            error = "Terminal error: $error"
                        )
                    }
                }
                
                logger.error("❌ Transaction timeout")
                TerminalOperationResponse(
                    success = false,
                    error = "Transaction timeout"
                )
                
            } catch (e: Exception) {
                logger.error("💥 Purchase failed", e)
                TerminalOperationResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse = runBlocking {
        operationLock.withLock {
            val wsClient = webSocketClient ?: run {
                logger.error("❌ Terminal not open")
                return@runBlocking TerminalOperationResponse(
                    success = false,
                    error = "Terminal not open"
                )
            }

            try {
                logger.info("━".repeat(60))
                logger.info("🔄 Starting reversal...")
                logger.info("━".repeat(60))

                val reversalRequest = messageBuilder.buildReversalRequest()
                wsClient.sendMessage(reversalRequest)
                logger.info("✅ Reversal command sent")

                // Wait for response
                val message = wsClient.receiveMessage(config.timeouts.reversalTimeoutMs)
                
                if (message == null) {
                    logger.error("❌ Timeout waiting for reversal result")
                    return@runBlocking TerminalOperationResponse(
                        success = false,
                        error = "Timeout waiting for reversal result"
                    )
                }
                
                if (responseParser.isTransactionComplete(message)) {
                    val localMode = if (message.contains("Dfs13LocalMode")) {
                        responseParser.parseLocalMode(message)
                    } else {
                        responseParser.parseLastFinancialResult(message)
                    }
                    logger.info("━".repeat(60))
                    if (localMode?.result == 1) {
                        logger.info("✅ REVERSAL APPROVED!")
                    } else {
                        logger.error("❌ REVERSAL DECLINED!")
                    }
                    logger.info("━".repeat(60))

                    return@runBlocking TerminalOperationResponse(
                        success = localMode?.result == 1,
                        callResult = localMode?.result,
                        responseCode = localMode?.responseCode
                    )
                }
                
                logger.error("❌ Reversal failed")
                TerminalOperationResponse(
                    success = false,
                    error = "Reversal failed"
                )
                
            } catch (e: Exception) {
                logger.error("💥 Reversal failed", e)
                TerminalOperationResponse(
                    success = false,
                    error = "Exception: ${e.message}"
                )
            }
        }
    }
    
    override fun closeTerminal(): TerminalSimpleResponse = runBlocking {
        try {
            logger.info("━".repeat(60))
            logger.info("🔌 Closing terminal...")
            logger.info("━".repeat(60))
            
            webSocketClient?.close()
            webSocketClient = null
            isTerminalOpen = false
            
            logger.info("✅ Terminal closed")
            TerminalSimpleResponse(
                success = true,
                message = "Terminal closed"
            )
            
        } catch (e: Exception) {
            logger.error("💥 Failed to close terminal", e)
            TerminalSimpleResponse(
                success = false,
                error = "Exception: ${e.message}"
            )
        }
    }
    
    override fun getHealth(): TerminalHealthResponse {
        return TerminalHealthResponse(
            status = if (isTerminalOpen) "healthy" else "unhealthy",
            configLoaded = true
        )
    }
    
    override fun getStatus(): TerminalStatusResponse {
        return TerminalStatusResponse(
            terminalOpen = isTerminalOpen,
            terminalReady = isTerminalOpen,
            connectionState = if (webSocketClient?.isConnected() == true) "CONNECTED" else "DISCONNECTED"
        )
    }
}
