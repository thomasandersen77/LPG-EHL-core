package no.cloudberries.lpg.api.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.service.event.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * WebSocket Event Publisher (Hexagonal Adapter)
 * 
 * Dette er en DRIVING ADAPTER som implementerer EventPublisher-porten.
 * 
 * Rolle i Hexagonal Architecture:
 * - Service-laget kaller EventPublisher-interfacet
 * - Denne adapteren mottar events og sender dem via WebSocket
 * - Service-laget vet IKKE om WebSocket - det er isolert
 * 
 * WebSocket-endepunkt: ws://localhost:8080/ws
 * 
 * Klienter kan subscribe til kanaler:
 * {"action": "subscribe", "channels": ["api", "emulator", "protocol"]}
 */
@Component
class WebSocketEventPublisher(
    private val objectMapper: ObjectMapper
) : TextWebSocketHandler(), EventPublisher {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // Active sessions with their subscribed channels
    private val sessions = ConcurrentHashMap<String, SessionInfo>()
    
    // All connected sessions for broadcast
    private val allSessions = CopyOnWriteArraySet<WebSocketSession>()
    
    data class SessionInfo(
        val session: WebSocketSession,
        val subscribedChannels: MutableSet<LogChannel> = mutableSetOf()
    )
    
    // ========================================================================
    // WebSocket Handler Methods (Driving Side - handles incoming connections)
    // ========================================================================
    
    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("🔌 WebSocket connected: ${session.id}")
        sessions[session.id] = SessionInfo(session)
        allSessions.add(session)
        
        // Send welcome message (with safety check for already-closed connections)
        trySendMessage(session) {
            val welcome = mapOf(
                "type" to "connected",
                "message" to "Tilkoblet logger-websocket",
                "availableChannels" to LogChannel.values().map { it.name.lowercase() }
            )
            objectMapper.writeValueAsString(welcome)
        }
    }
    
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("🔌 WebSocket disconnected: ${session.id} (${status.reason ?: "normal"})")
        sessions.remove(session.id)
        allSessions.remove(session)
    }
    
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // Early exit if session is already closed or not tracked
        if (!session.isOpen || !sessions.containsKey(session.id)) {
            logger.debug("Ignoring message for closed/unknown session: ${session.id}")
            return
        }
        
        try {
            val payload = objectMapper.readTree(message.payload)
            val action = payload.get("action")?.asText()
            
            when (action) {
                "subscribe" -> {
                    val channels = payload.get("channels")?.map { 
                        LogChannel.valueOf(it.asText().uppercase()) 
                    }?.toMutableSet() ?: mutableSetOf()
                    
                    // Update subscriptions atomically
                    sessions.computeIfPresent(session.id) { _, sessionInfo ->
                        sessionInfo.subscribedChannels.clear()
                        sessionInfo.subscribedChannels.addAll(channels)
                        sessionInfo
                    }
                    
                    logger.info("📝 Session ${session.id} subscribed to: $channels")
                    
                    trySendMessage(session) {
                        objectMapper.writeValueAsString(mapOf(
                            "type" to "subscribed",
                            "channels" to channels.map { it.name.lowercase() }
                        ))
                    }
                }
                "unsubscribe" -> {
                    sessions.computeIfPresent(session.id) { _, sessionInfo ->
                        sessionInfo.subscribedChannels.clear()
                        sessionInfo
                    }
                    trySendMessage(session) {
                        objectMapper.writeValueAsString(mapOf("type" to "unsubscribed"))
                    }
                }
                "ping" -> {
                    trySendMessage(session) {
                        objectMapper.writeValueAsString(mapOf("type" to "pong", "timestamp" to java.time.Instant.now().toString()))
                    }
                }
                else -> {
                    logger.warn("Unknown WebSocket action: $action")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling WebSocket message", e)
        }
    }
    
    // ========================================================================
    // EventPublisher Implementation (Driven Side - receives events from service)
    // ========================================================================
    
    override fun publishPriceUpdate(pricePerLiterKr: Double) {
        if (allSessions.isEmpty()) return
        
        val priceEvent = mapOf(
            "type" to "price_update",
            "timestamp" to java.time.Instant.now().toString(),
            "pricePerLiterKr" to pricePerLiterKr,
            "message" to "Pris oppdatert til ${"%.2f".format(pricePerLiterKr)} kr/L"
        )
        
        broadcastToAll(priceEvent)
    }
    
    override fun publishPumpStatusUpdate(pumpStatus: PumpStatusEvent) {
        if (allSessions.isEmpty()) return
        
        val event = mapOf(
            "type" to "pump_update",
            "timestamp" to pumpStatus.timestamp.toString(),
            "address" to pumpStatus.address,
            "state" to pumpStatus.state,
            "volumeLitres" to pumpStatus.volumeLitres,
            "amountKr" to pumpStatus.amountKr,
            "pricePerLitreKr" to pumpStatus.pricePerLitreKr,
            "nozzleLifted" to pumpStatus.nozzleLifted,
            "hasPendingTransaction" to pumpStatus.hasPendingTransaction
        )
        
        broadcastToAll(event)
    }
    
    override fun publishLogEvent(logEvent: LogEvent) {
        if (allSessions.isEmpty()) return
        
        val entry = mapOf(
            "type" to "log",
            "channel" to logEvent.channel.name.lowercase(),
            "level" to logEvent.level.name,
            "logger" to logEvent.logger.substringAfterLast('.'), // Short logger name
            "message" to logEvent.message,
            "timestamp" to logEvent.timestamp.toString()
        )
        
        // Send to sessions subscribed to this channel
        broadcastToSubscribed(logEvent.channel, entry)
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    private fun broadcastToAll(event: Map<String, Any>) {
        val json = objectMapper.writeValueAsString(event)
        
        sessions.values.forEach { sessionInfo ->
            try {
                if (sessionInfo.session.isOpen) {
                    sessionInfo.session.sendMessage(TextMessage(json))
                }
            } catch (e: Exception) {
                logger.debug("Failed to send to session ${sessionInfo.session.id}: ${e.message}")
            }
        }
    }
    
    private fun broadcastToSubscribed(channel: LogChannel, event: Map<String, Any>) {
        val json = objectMapper.writeValueAsString(event)
        
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
     * Safely send a message to a WebSocket session.
     * Handles ClosedChannelException and other errors gracefully.
     * 
     * Uses synchronized send to prevent concurrent access issues with Undertow.
     */
    private inline fun trySendMessage(session: WebSocketSession, messageProvider: () -> String) {
        try {
            if (session.isOpen) {
                synchronized(session) {
                    if (session.isOpen) { // Double-check after acquiring lock
                        session.sendMessage(TextMessage(messageProvider()))
                    }
                }
            }
        } catch (e: java.nio.channels.ClosedChannelException) {
            // Expected when client disconnects rapidly - silently clean up
            cleanupSession(session)
        } catch (e: java.io.IOException) {
            // Connection reset or broken pipe - normal during disconnect
            logger.debug("IO error sending to session ${session.id}: ${e.message}")
            cleanupSession(session)
        } catch (e: Exception) {
            logger.debug("Failed to send to session ${session.id}: ${e.message}")
            cleanupSession(session)
        }
    }
    
    /**
     * Clean up a disconnected session from tracking collections.
     */
    private fun cleanupSession(session: WebSocketSession) {
        sessions.remove(session.id)
        allSessions.remove(session)
    }
}
