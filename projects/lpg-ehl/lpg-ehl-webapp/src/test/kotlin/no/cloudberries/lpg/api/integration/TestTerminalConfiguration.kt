package no.cloudberries.lpg.api.integration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import no.cloudberries.lpg.service.terminal.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration for PaymentTerminalClient.
 * Provides a mock implementation that always succeeds for integration tests.
 */
@TestConfiguration
class TestTerminalConfiguration {

    @Bean
    @Primary
    fun mockPaymentTerminalClient(): PaymentTerminalClient {
        return MockPaymentTerminalClient()
    }

    private class MockPaymentTerminalClient : PaymentTerminalClient {
        override suspend fun open(): TerminalSimpleResponse {
            return TerminalSimpleResponse(success = true, message = "Mock terminal opened")
        }

        override suspend fun close(): TerminalSimpleResponse {
            return TerminalSimpleResponse(success = true, message = "Mock terminal closed")
        }

        override fun getStatus(): TerminalStatusResponse {
            return TerminalStatusResponse(
                terminalOpen = true,
                terminalReady = true,
                connectionState = "MOCK_CONNECTED"
            )
        }

        override fun getHealth(): TerminalHealthResponse {
            return TerminalHealthResponse(
                status = "MOCK_HEALTHY",
                configLoaded = true
            )
        }

        override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "mock-op-$correlationId"
            )
        }

        override suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "mock-op-$correlationId"
            )
        }

        override suspend fun reversal(correlationId: String): TerminalOperationResponse {
            return TerminalOperationResponse(
                success = true,
                operationId = "mock-reversal-$correlationId"
            )
        }

        override fun terminalEvents(): Flow<TerminalEvent> {
            // Return empty flow for tests
            return flowOf()
        }
    }
}
