package no.cloudberries.lpg.netscloud

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

class NetsCloudWebSocketClient(
    private val baseUrl: String,
    private val token: String,
    private val config: NetsCloudConnectConfig
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
    
    suspend fun connect() {
        if (isConnected) {
            logger.debug("Already connected to WebSocket")
            return
        }
        
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        logger.info("Connecting to WebSocket: $wsUrl")
        
        try {
            withContext(Dispatchers.IO) {
                httpClient.webSocket(
                    request = {
                        url.takeFrom(wsUrl)
                        headers.append(HttpHeaders.Authorization, "bearer $token")
                    }
                ) {
                    session = this
                    isConnected = true
                    logger.info("✅ WebSocket connected!")
                    
                    // Start listening for incoming messages
                    listenerJob = launch { listenForMessages() }
                    
                    // Keep session alive
                    while (isActive && isConnected) {
                        delay(1000)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket connection failed", e)
            isConnected = false
            throw NetsCloudWebSocketException("Failed to connect", e)
        }
    }
    
    private suspend fun listenForMessages() {
        try {
            for (frame in session!!.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val message = frame.readText()
                        logger.debug("📨 Received TEXT: ${message.take(100)}...")
                        messageChannel.send(message)
                    }
                    is Frame.Binary -> {
                        val message = frame.readBytes().decodeToString()
                        logger.debug("📨 Received BINARY: ${message.take(100)}...")
                        messageChannel.send(message)
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
    
    suspend fun sendMessage(json: String) {
        if (!isConnected || session == null) {
            throw IllegalStateException("WebSocket not connected")
        }
        
        logger.debug("📤 Sending: ${json.take(100)}...")
        session!!.send(Frame.Text(json))
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
        session?.close()
        httpClient.close()
        messageChannel.close()
    }
}

class NetsCloudWebSocketException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
