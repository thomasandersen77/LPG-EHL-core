package no.cloudberries.lpg.payment.terminal.sim.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.service.SseEventSender
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventPublisher
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventStore
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventStreamRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketExtension
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.net.URI
import java.security.Principal
import java.util.concurrent.CopyOnWriteArrayList

class TerminalEventsWebSocketHandlerTest {

    @Test
    fun `connection with since gets backlog over websocket`() {
        val store = TerminalEventStore(SimulatorConfig(eventBufferSize = 100))
        val registry = TerminalEventStreamRegistry()
        val publisher = TerminalEventPublisher(
            eventStore = store,
            streamRegistry = registry,
            objectMapper = ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE),
            sseEventSender = SseEventSender { _, _ -> true },
            config = SimulatorConfig(eventBufferSize = 100)
        )

        publisher.publish("OperationStarted", "op-1", mapOf("OperationType" to "purchase"))
        publisher.publish("DisplayText", "op-1", mapOf("text" to "SETT INN KORTET"))
        publisher.publish("OperationCompleted", "op-1", mapOf("Success" to true))

        val handler = TerminalEventsWebSocketHandler(publisher)
        val session = FakeWebSocketSession(
            id = "ws-1",
            uri = URI.create("ws://localhost:18080/v1/events/ws?since=1")
        )

        handler.afterConnectionEstablished(session)

        val payloads = session.sentMessages.map { it.payload }
        assertTrue(payloads.any { it.contains("\"type\":\"connected\"") })
        assertTrue(payloads.any { it.contains("\"Cursor\":2") })
        assertTrue(payloads.any { it.contains("\"Cursor\":3") })

        handler.afterConnectionClosed(session, CloseStatus.NORMAL)
    }

    private class FakeWebSocketSession(
        private val id: String,
        private val uri: URI
    ) : WebSocketSession {
        private var open = true
        val sentMessages = CopyOnWriteArrayList<TextMessage>()

        override fun getId(): String = id

        override fun getUri(): URI = uri

        override fun getHandshakeHeaders(): HttpHeaders = HttpHeaders()

        override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

        override fun getPrincipal(): Principal? = null

        override fun getLocalAddress(): InetSocketAddress? = null

        override fun getRemoteAddress(): InetSocketAddress? = null

        override fun getAcceptedProtocol(): String? = null

        override fun setTextMessageSizeLimit(messageSizeLimit: Int) {}

        override fun getTextMessageSizeLimit(): Int = 65536

        override fun setBinaryMessageSizeLimit(messageSizeLimit: Int) {}

        override fun getBinaryMessageSizeLimit(): Int = 65536

        override fun getExtensions(): MutableList<WebSocketExtension> = mutableListOf()

        override fun isOpen(): Boolean = open

        override fun sendMessage(message: WebSocketMessage<*>) {
            if (message is TextMessage) {
                sentMessages += message
            }
        }

        override fun close() {
            open = false
        }

        override fun close(status: CloseStatus) {
            open = false
        }
    }
}
