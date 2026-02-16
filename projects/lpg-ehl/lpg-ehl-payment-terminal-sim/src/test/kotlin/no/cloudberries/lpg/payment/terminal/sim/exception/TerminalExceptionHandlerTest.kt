package no.cloudberries.lpg.payment.terminal.sim.exception

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test

class TerminalExceptionHandlerTest {

    @Test
    fun `operation rejected returns http 200 with operation response`() {
        val handler = TerminalExceptionHandler()
        val response = handler.handleOperationRejected(OperationRejectedException("Rejected"))

        assertEquals(200, response.statusCode.value())
        val body = response.body
        assertNotNull(body)
        assertFalse(body.Success)
        assertEquals("operation_rejected", body.ErrorCode)
        assertEquals("Rejected", body.Error)
    }
}