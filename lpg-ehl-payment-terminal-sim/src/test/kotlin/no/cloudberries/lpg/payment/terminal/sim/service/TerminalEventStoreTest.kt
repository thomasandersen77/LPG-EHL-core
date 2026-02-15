package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerminalEventStoreTest {

    @Test
    fun `append stores event and makes it available for polling`() {
        val store = TerminalEventStore(
            config = SimulatorConfig(
                eventBufferSize = 100
            )
        )

        val event = store.append(
            eventType = "OperationCompleted",
            operationId = "op-1",
            payload = mapOf("Success" to true)
        )

        val events = store.getEventsSince(0)

        assertEquals(1, events.size)
        assertEquals(event.Cursor, events.first().Cursor)
        assertEquals("OperationCompleted", events.first().EventType)
        assertEquals("op-1", events.first().OperationId)
        assertTrue(events.first().Payload["Success"] as Boolean)
    }
}
