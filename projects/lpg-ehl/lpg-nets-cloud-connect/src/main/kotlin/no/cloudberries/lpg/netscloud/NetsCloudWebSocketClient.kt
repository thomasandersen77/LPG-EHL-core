package no.cloudberries.lpg.netscloud

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service

@Service
class NetsCloudWebSocketClient(
    private val config: NetsCloudConnectConfig,
    private val responseParser: NetsResponseParser
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = config.websocket.pingIntervalMs
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    private var isConnected = false
    private var listenerJob: Job? = null
    private var websocketJob: Job? = null
    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun connect(bearerToken: String) {
        if (isConnected) {
            logger.debug("Already connected to WebSocket")
            return
        }

        val wsUrl = config.baseUrl.replace("https://", "wss://") + "/ws/json"
        logger.info("Connecting to WebSocket: $wsUrl")

        val connectionReady = CompletableDeferred<Unit>()

        try {
            // Start WebSocket in separate coroutine scope so connect() can return
            websocketJob = connectionScope.launch {
                try {
                    httpClient.webSocket(
                        request = {
                            url.takeFrom(wsUrl)
                            headers.append(HttpHeaders.Authorization, "bearer $bearerToken")
                            headers.append(HttpHeaders.SecWebSocketProtocol, "json")  // KRITISK 2: Sub-protokoll
                        }
                    ) {
                        session = this
                        isConnected = true
                        logger.info("✅ WebSocket connected!")

                        // Start listening for incoming messages
                        listenerJob = launch { listenForMessages() }

                        // Signal that connection is ready
                        connectionReady.complete(Unit)

                        // Keep session alive until closed
                        while (isActive && isConnected) {
                            delay(1000)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("WebSocket connection failed", e)
                    isConnected = false
                    connectionReady.completeExceptionally(e)
                }
            }

            // Wait for connection to be established before returning
            connectionReady.await()

        } catch (e: Exception) {
            logger.error("WebSocket connection failed", e)
            isConnected = false
            throw NetsCloudWebSocketException("Failed to connect", e)
        }
    }
    
    private suspend fun listenForMessages() {
        val currentSession = session ?: run {
            logger.error("Cannot listen for messages: session is null")
            return
        }

        try {
            for (frame in currentSession.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val message = frame.readText()
                        logger.debug("📨 Received TEXT: ${message.take(100)}...")
                        handleIncomingMessage(currentSession, message)
                    }
                    is Frame.Binary -> {
                        val message = frame.readBytes().decodeToString()
                        logger.debug("📨 Received BINARY: ${message.take(100)}...")
                        handleIncomingMessage(currentSession, message)
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket closed: ${frame.readReason()}")
                        isConnected = false
                        break
                    }
                    else -> { /* Ignore Ping/Pong */ }
                }
            }
        } catch (e: Exception) {
            logger.error("Error listening for messages", e)
            isConnected = false
        }
    }

    private suspend fun handleIncomingMessage(session: DefaultClientWebSocketSession, message: String) {
        // TerminalID filtering: ignore messages for other terminals
        val messageTerminalId = responseParser.extractTerminalId(message)
        if (messageTerminalId != null && messageTerminalId != config.terminalId) {
            logger.debug("Ignoring message for different terminal: $messageTerminalId")
            return
        }

        // MANGLER 3: Auto-confirm Dfs13JsonReceived interactive prompts
        if (responseParser.isJsonReceived(message)) {
            val confirmJson = responseParser.parseJsonReceivedConfirm(message)
            if (confirmJson != null) {
                logger.info("🔔 Auto-confirming interactive prompt")
                session.send(Frame.Text(confirmJson))
            }
        }

        // Send message to channel for processing
        messageChannel.send(message)
    }
    
    suspend fun sendMessage(json: String) {
        val currentSession = session ?: throw IllegalStateException("WebSocket not connected")

        if (!isConnected) {
            throw IllegalStateException("WebSocket not connected")
        }

        logger.debug("📤 Sending: ${json.take(100)}...")
        currentSession.send(Frame.Text(json))
    }
    
    suspend fun receiveMessage(timeoutMs: Long = 30000): String? {
        return withTimeoutOrNull(timeoutMs) {
            messageChannel.receive()
        }
    }
    
    fun isConnected(): Boolean = isConnected
    
    suspend fun close() {
        logger.info("Closing WebSocket connection...")
        isConnected = false
        listenerJob?.cancel()
        websocketJob?.cancel()
        session?.close()
        httpClient.close()
        messageChannel.close()
    }
}

class NetsCloudWebSocketException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
