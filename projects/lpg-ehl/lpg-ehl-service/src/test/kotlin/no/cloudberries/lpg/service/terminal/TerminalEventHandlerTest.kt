package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.service.terminal.dto.EventEnvelope
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TerminalEventHandlerTest {

    @Test
    fun `purchase completed with lowercase success triggers pump start`() {
        val orchestrator = mock<PumpPaymentOrchestrator>()
        val handler = TerminalEventHandler(orchestrator)

        val event = EventEnvelope(
            cursor = 1,
            eventId = "event-1",
            operationId = "op-123",
            timestamp = "2026-02-11T00:59:00Z",
            eventType = "OperationCompleted",
            payload = mapOf(
                "success" to true,
                "OperationType" to "purchase",
                "AmountMinor" to 150000
            )
        )

        handler.handleOperationCompleted(event)

        verify(orchestrator).startPumpingAfterPayment(
            amountCents = 150000,
            pumpId = 1,
            productId = 1,
            operationId = "op-123"
        )
    }
}