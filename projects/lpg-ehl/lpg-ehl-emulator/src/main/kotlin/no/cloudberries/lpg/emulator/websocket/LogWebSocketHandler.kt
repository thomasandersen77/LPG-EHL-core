package no.cloudberries.lpg.emulator.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * WebSocket handler for real-time log streaming.
 * 
 * Supports three log channels:
 * - api: REST API related logs
 * - emulator: Emulator state machine logs
 * - protocol: EHL protocol packet logs
 * 
 * Clients can subscribe to specific channels by sending:
 * {"action": "subscribe", "channels": ["api", "emulator", "protocol"]}
 * 
 * Log entries are broadcast as:
 * {"channel": "emulator", "timestamp": "...", "level": "INFO", "logger": "...", "message": "..."}
 * 
 * Only loaded when emulator.standalone.enabled=true.
 */
@Component
@ConditionalOnProperty(
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class LogWebSocketHandler(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler() {
    
    private val logger = LoggerFactory.getLogger(LogWebSocketHandler::class.java)
    
    // Active sessions with their subscribed channels
    private val sessions = ConcurrentHashMap<String, SessionInfo>()
    
    // All connected sessions for broadcast
    private val allSessions = CopyOnWriteArraySet<WebSocketSession>()
    
    data class SessionInfo(
        val session: WebSocketSession,
        val subscribedChannels: MutableSet<LogChannel> = mutableSetOf()
    )
    
    enum class LogChannel {
        API,      // REST API logs
        EMULATOR, // Emulator state machine logs
        PROTOCOL  // EHL protocol packet logs
    }
    
    data class LogEntry(
        val channel: String,
        val timestamp: String = Instant.now().toString(),
        val level: String,
        val logger: String,
        val message: String
    )
    
    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("🔌 WebSocket connected: ${session.id}")
        sessions[session.id] = SessionInfo(session)
        allSessions.add(session)
        
        // Send welcome message
        val welcome = mapOf(
            "type" to "connected",
            "message" to "Tilkoblet logger-websocket",
            "availableChannels" to LogChannel.values().map { it.name.lowercase() }
        )
        session.sendMessage(TextMessage(objectMapper.writeValueAsString(welcome)))
    }
    
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("🔌 WebSocket disconnected: ${session.id} (${status.reason ?: "normal"})")
        sessions.remove(session.id)
        allSessions.remove(session)
    }
    
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val payload = objectMapper.readTree(message.payload)
            val action = payload.get("action")?.asText()
            
            when (action) {
                "subscribe" -> {
                    val channels = payload.get("channels")?.map { 
                        LogChannel.valueOf(it.asText().uppercase()) 
                    }?.toMutableSet() ?: mutableSetOf()
                    
                    sessions[session.id]?.subscribedChannels?.clear()
                    sessions[session.id]?.subscribedChannels?.addAll(channels)
                    
                    logger.info("📝 Session ${session.id} subscribed to: $channels")
                    
                    val response = mapOf(
                        "type" to "subscribed",
                        "channels" to channels.map { it.name.lowercase() }
                    )
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(response)))
                }
                "unsubscribe" -> {
                    sessions[session.id]?.subscribedChannels?.clear()
                    val response = mapOf("type" to "unsubscribed")
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(response)))
                }
                "ping" -> {
                    val response = mapOf("type" to "pong", "timestamp" to Instant.now().toString())
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(response)))
                }
                else -> {
                    logger.warn("Unknown WebSocket action: $action")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message", e)
        }
    }
    
    /**
     * Broadcast a log entry to all subscribed sessions.
     * Called by LogBufferAppender when a log event is captured.
     */
    fun broadcast(channel: LogChannel, level: String, loggerName: String, message: String) {
        if (allSessions.isEmpty()) return
        
        val entry = LogEntry(
            channel = channel.name.lowercase(),
            level = level,
            logger = loggerName.substringAfterLast('.'), // Short logger name
            message = message
        )
        
        val json = objectMapper.writeValueAsString(entry)
        
        sessions.values.forEach { sessionInfo ->
            try {
                // Send to all if no specific subscription, or if channel matches
                if (sessionInfo.subscribedChannels.isEmpty() || channel in sessionInfo.subscribedChannels) {
                    if (sessionInfo.session.isOpen) {
                        sessionInfo.session.sendMessage(TextMessage(json))
                    }
                }
            } catch (e: Exception) {
                logger.debug("Failed to send to session ${sessionInfo.session.id}: ${e.message}")
            }
        }
    }
    
    /**
     * Broadcast a price update event to all connected WebSocket clients.
     * Used for real-time price sync in the Control panel.
     * 
     * @param pricePerLiterKr New price per liter in kroner
     */
    fun broadcastPriceUpdate(pricePerLiterKr: Double) {
        if (allSessions.isEmpty()) {
            logger.debug("💰 No WebSocket clients - skipping price broadcast")
            return
        }
        
        val priceEvent = mapOf(
            "type" to "price_update",
            "timestamp" to Instant.now().toString(),
            "pricePerLiterKr" to pricePerLiterKr,
            "message" to "Pris oppdatert til ${"%,.2f".format(pricePerLiterKr)} kr/L"
        )
        
        val json = objectMapper.writeValueAsString(priceEvent)
        
        var sentCount = 0
        sessions.values.forEach { sessionInfo ->
            try {
                if (sessionInfo.session.isOpen) {
                    sessionInfo.session.sendMessage(TextMessage(json))
                    sentCount++
                }
            } catch (e: Exception) {
                logger.debug("Failed to send price update to session ${sessionInfo.session.id}: ${e.message}")
            }
        }
        
        logger.info("💰 Price update broadcast to $sentCount client(s): $pricePerLiterKr kr/L")
    }
    
    /**
     * Broadcast a transaction event to all connected WebSocket clients.
     * Used for real-time transaction updates in the Control panel.
     * 
     * @param transactionId Transaction ID
     * @param volumeLiters Volume in liters
     * @param amountNok Amount in kroner
     * @param status Transaction status (e.g., "PENDING", "PAID")
     */
    fun broadcastTransactionUpdate(
        transactionId: String,
        volumeLiters: Double,
        amountNok: Double,
        status: String
    ) {
        if (allSessions.isEmpty()) return
        
        val txEvent = mapOf(
            "type" to "transaction_update",
            "timestamp" to Instant.now().toString(),
            "transactionId" to transactionId,
            "volumeLiters" to volumeLiters,
            "amountNok" to amountNok,
            "status" to status
        )
        
        val json = objectMapper.writeValueAsString(txEvent)
        
        sessions.values.forEach { sessionInfo ->
            try {
                if (sessionInfo.session.isOpen) {
                    sessionInfo.session.sendMessage(TextMessage(json))
                }
            } catch (e: Exception) {
                logger.debug("Failed to send transaction update: ${e.message}")
            }
        }
    }
    
    /**
     * Determine log channel based on logger name.
     */
    companion object {
        fun determineChannel(loggerName: String): LogChannel {
            return when {
                // API logs
                loggerName.contains("Controller") -> LogChannel.API
                loggerName.contains("Service") && !loggerName.contains("Emulator") -> LogChannel.API
                
                // Protocol logs
                loggerName.contains("protocol", ignoreCase = true) -> LogChannel.PROTOCOL
                loggerName.contains("Codec") -> LogChannel.PROTOCOL
                loggerName.contains("Packet") -> LogChannel.PROTOCOL
                loggerName.contains("EhlCommand") -> LogChannel.PROTOCOL
                
                // Emulator logs
                loggerName.contains("Emulator") -> LogChannel.EMULATOR
                loggerName.contains("Dispenser") -> LogChannel.EMULATOR
                loggerName.contains("ClientHandler") -> LogChannel.EMULATOR
                
                else -> LogChannel.API
            }
        }
    }
}
