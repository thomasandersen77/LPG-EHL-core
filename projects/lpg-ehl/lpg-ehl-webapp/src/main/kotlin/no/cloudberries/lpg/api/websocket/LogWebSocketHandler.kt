package no.cloudberries.lpg.api.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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
 * Supports four log channels:
 * - api: REST API related logs
 * - service: Service layer business logic logs (always active)
 * - emulator: Emulator state machine logs (LAB mode only)
 * - protocol: EHL protocol packet logs
 * 
 * Clients can subscribe to specific channels by sending:
 * {"action": "subscribe", "channels": ["api", "service", "emulator", "protocol"]}
 * 
 * Log entries are broadcast as:
 * {"channel": "service", "timestamp": "...", "level": "INFO", "logger": "...", "message": "..."}
 */
@Component
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
        SERVICE,  // Service layer business logic logs (always active)
        EMULATOR, // Emulator state machine logs (LAB mode only)
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
     */
    fun broadcastPriceUpdate(pricePerLiterKr: Double) {
        if (allSessions.isEmpty()) return
        
        val priceEvent = mapOf(
            "type" to "price_update",
            "timestamp" to Instant.now().toString(),
            "pricePerLiterKr" to pricePerLiterKr,
            "message" to "Pris oppdatert til ${"%.2f".format(pricePerLiterKr)} kr/L"
        )
        
        val json = objectMapper.writeValueAsString(priceEvent)
        
        sessions.values.forEach { sessionInfo ->
            try {
                if (sessionInfo.session.isOpen) {
                    sessionInfo.session.sendMessage(TextMessage(json))
                }
            } catch (e: Exception) {
                logger.debug("Failed to send price update: ${e.message}")
            }
        }
    }
    
    /**
     * Broadcast a pump status update to all connected clients.
     */
    fun broadcastPumpUpdate(status: Map<String, Any>) {
        if (allSessions.isEmpty()) return
        
        val event = mapOf(
            "type" to "pump_update",
            "timestamp" to Instant.now().toString()
        ) + status
        
        val json = objectMapper.writeValueAsString(event)
        
        sessions.values.forEach { sessionInfo ->
            try {
                if (sessionInfo.session.isOpen) {
                    sessionInfo.session.sendMessage(TextMessage(json))
                }
            } catch (e: Exception) {
                logger.debug("Failed to send pump update: ${e.message}")
            }
        }
    }
}
