package no.cloudberries.lpg.payment.terminal.sim.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

class TerminalEventPublisherTest {

    @Test
    fun `publishing event delivers to sse subscriber`() {
        val store = TerminalEventStore(SimulatorConfig(eventBufferSize = 100))
        val registry = TerminalEventStreamRegistry()
        val receivedCursors = CopyOnWriteArrayList<Long>()

        val publisher = TerminalEventPublisher(
            eventStore = store,
            streamRegistry = registry,
            objectMapper = ObjectMapper(),
            sseEventSender = SseEventSender { _, event ->
                receivedCursors += event.Cursor
                true
            },
            config = SimulatorConfig(eventBufferSize = 100)
        )

        val emitter = SseEmitter(Long.MAX_VALUE)
        assertTrue(publisher.registerSse(emitter, "0"))

        publisher.publish(
            eventType = "DisplayText",
            operationId = "op-1",
            payload = mapOf("text" to "SETT INN KORTET")
        )

        assertEquals(1, receivedCursors.size)
    }

    @Test
    fun `registerWs sends backlog using since cursor`() {
        val store = TerminalEventStore(SimulatorConfig(eventBufferSize = 100))
        val registry = TerminalEventStreamRegistry()
        val publisher = TerminalEventPublisher(
            eventStore = store,
            streamRegistry = registry,
            objectMapper = ObjectMapper(),
            sseEventSender = SseEventSender { _, _ -> true },
            config = SimulatorConfig(eventBufferSize = 100)
        )

        publisher.publish("OperationStarted", "op-1", mapOf("OperationType" to "purchase"))
        publisher.publish("DisplayText", "op-1", mapOf("text" to "VENTER PÅ KORTET"))
        publisher.publish("OperationCompleted", "op-1", mapOf("Success" to true))

        val received = mutableListOf<Long>()
        val wsSender = WebSocketSessionSender { event ->
            received += event.Cursor
            true
        }

        assertTrue(publisher.registerWs(wsSender, "1"))
        assertEquals(listOf(2L, 3L), received)
    }

    @Test
    fun `registerWs returns false when backlog sender fails`() {
        val store = TerminalEventStore(SimulatorConfig(eventBufferSize = 100))
        val registry = TerminalEventStreamRegistry()
        val publisher = TerminalEventPublisher(
            eventStore = store,
            streamRegistry = registry,
            objectMapper = ObjectMapper(),
            sseEventSender = SseEventSender { _, _ -> true },
            config = SimulatorConfig(eventBufferSize = 100)
        )

        publisher.publish("OperationStarted", "op-1", mapOf("OperationType" to "purchase"))

        val wsSender = WebSocketSessionSender { false }
        assertFalse(publisher.registerWs(wsSender, "0"))
    }

    @Test
    fun `publishing event delivers to ws subscriber`() {
        val store = TerminalEventStore(SimulatorConfig(eventBufferSize = 100))
        val registry = TerminalEventStreamRegistry()
        val publisher = TerminalEventPublisher(
            eventStore = store,
            streamRegistry = registry,
            objectMapper = ObjectMapper(),
            sseEventSender = SseEventSender { _, _ -> true },
            config = SimulatorConfig(eventBufferSize = 100)
        )

        val received = CopyOnWriteArrayList<Long>()
        val wsSender = WebSocketSessionSender { event ->
            received += event.Cursor
            true
        }

        assertTrue(publisher.registerWs(wsSender, "0"))

        publisher.publish(
            eventType = "OperationCompleted",
            operationId = "op-live",
            payload = mapOf("Success" to true)
        )

        assertEquals(1, received.size)
    }
}
