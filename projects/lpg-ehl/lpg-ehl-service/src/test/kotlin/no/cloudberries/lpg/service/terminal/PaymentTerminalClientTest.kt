package no.cloudberries.lpg.service.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Contract tests for PaymentTerminalClient interface.
 * Any implementation should satisfy these behavioral expectations.
 */
class PaymentTerminalClientTest {

    @Test
    fun `PaymentTerminalClient interface should have all required lifecycle methods`() {
        val client = TestPaymentTerminalClient()

        runBlocking {
            // Lifecycle
            val openResponse = client.open()
            assertNotNull(openResponse)

            val closeResponse = client.close()
            assertNotNull(closeResponse)

            val status = client.getStatus()
            assertNotNull(status)

            val health = client.getHealth()
            assertNotNull(health)
        }
    }

    @Test
    fun `PaymentTerminalClient interface should have all required payment methods`() {
        val client = TestPaymentTerminalClient()

        runBlocking {
            // Payment operations
            val reserveResponse = client.reserve(100_00, "test-correlation-id")
            assertNotNull(reserveResponse)

            val captureResponse = client.capture(50_00, "test-correlation-id")
            assertNotNull(captureResponse)

            val reversalResponse = client.reversal("test-correlation-id")
            assertNotNull(reversalResponse)
        }
    }

    @Test
    fun `PaymentTerminalClient should emit terminal events`() {
        val client = TestPaymentTerminalClient()

        val events = client.terminalEvents()
        assertNotNull(events)
    }

    @Test
    fun `TerminalOperationResponse should have success flag`() {
        val response = TerminalOperationResponse(
            success = true,
            operationId = "op-123"
        )

        assertTrue(response.success)
        assertEquals("op-123", response.operationId)
    }

    @Test
    fun `TerminalNotReadyException should be throwable`() {
        assertThrows(TerminalNotReadyException::class.java) {
            throw TerminalNotReadyException("Terminal not ready")
        }
    }

    @Test
    fun `TerminalBusyException should be throwable`() {
        assertThrows(TerminalBusyException::class.java) {
            throw TerminalBusyException("Terminal busy")
        }
    }

    // ============================================================================
    // Test Implementation
    // ============================================================================

    /**
     * Minimal test implementation of PaymentTerminalClient for contract testing.
     */
    private class TestPaymentTerminalClient : PaymentTerminalClient {
        override suspend fun open(): TerminalSimpleResponse {
            return TerminalSimpleResponse(success = true, message = "opened")
        }

        override suspend fun close(): TerminalSimpleResponse {
            return TerminalSimpleResponse(success = true, message = "closed")
        }

        override fun getStatus(): TerminalStatusResponse {
            return TerminalStatusResponse(
                terminalOpen = true,
                terminalReady = true,
                connectionState = "connected"
            )
        }

        override fun getHealth(): TerminalHealthResponse {
            return TerminalHealthResponse(
                status = "healthy",
                configLoaded = true
            )
        }

        override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "reserve-$correlationId",
                callResult = 1
            )
        }

        override suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "capture-$correlationId",
                callResult = 1
            )
        }

        override suspend fun reversal(correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "reversal-$correlationId",
                callResult = 1
            )
        }

        override fun terminalEvents(): Flow<TerminalEvent> {
            return emptyFlow()
        }
    }
}
