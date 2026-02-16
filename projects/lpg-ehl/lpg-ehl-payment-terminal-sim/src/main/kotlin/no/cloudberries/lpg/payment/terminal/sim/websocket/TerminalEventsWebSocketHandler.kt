package no.cloudberries.lpg.payment.terminal.sim.websocket

import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventPublisher
import no.cloudberries.lpg.payment.terminal.sim.service.WebSocketSessionSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

@Component
class TerminalEventsWebSocketHandler(
    private val eventPublisher: TerminalEventPublisher
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(TerminalEventsWebSocketHandler::class.java)
    private val activeSessions = ConcurrentHashMap<String, WebSocketSessionSender>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val since = extractSince(session.uri)

        val sender = WebSocketSessionSender { event ->
            try {
                if (!session.isOpen) {
                    false
                } else {
                    synchronized(session) {
                        if (!session.isOpen) {
                            false
                        } else {
                            session.sendMessage(TextMessage(eventPublisher.formatJson(event)))
                            true
                        }
                    }
                }
            } catch (ex: Exception) {
                log.debug("WS send failed session={} reason={}", session.id, ex.message)
                false
            }
        }

        activeSessions[session.id] = sender

        val helloPayload = """
            {"type":"connected","channel":"terminal-events","since":"$since"}
        """.trimIndent()
        try {
            session.sendMessage(TextMessage(helloPayload))
        } catch (_: Exception) {
            // Ignore hello failures and continue with regular lifecycle handling.
        }

        if (!eventPublisher.registerWs(sender, since)) {
            try {
                session.close(CloseStatus.SERVER_ERROR)
            } catch (_: Exception) {
                // ignore
            }
            removeSession(session)
            return
        }
        log.info("WS connected session={} since={}", session.id, since)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        removeSession(session)
        log.info("WS disconnected session={} status={}", session.id, status.code)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        log.debug("WS transport error session={} reason={}", session.id, exception.message)
        removeSession(session)
    }

    private fun removeSession(session: WebSocketSession) {
        activeSessions.remove(session.id)?.let { sender ->
            eventPublisher.unregisterWs(sender)
        }
    }

    private fun extractSince(uri: URI?): String {
        val query = uri?.query ?: return "0"
        return query
            .split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2 && parts[0] == "since") parts[1] else null
            }
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "0"
    }
}
