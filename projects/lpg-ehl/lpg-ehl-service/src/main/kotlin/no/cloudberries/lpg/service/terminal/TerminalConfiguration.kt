package no.cloudberries.lpg.service.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Terminal integration configuration.
 * Provides a fallback NoOp PaymentTerminalClient when no real provider
 * (e.g. NetsCloudTerminalAdapter) is active.
 */
@Configuration
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalConfiguration {

    /**
     * Fallback PaymentTerminalClient when no provider-specific adapter (e.g. NetsCloudTerminalAdapter) is active.
     * Logs warnings on operations but does not block startup.
     */
    @Bean
    @ConditionalOnMissingBean(PaymentTerminalClient::class)
    fun noOpPaymentTerminalClient(): PaymentTerminalClient {
        return NoOpPaymentTerminalClient()
    }
}

/**
 * No-op PaymentTerminalClient used when no real terminal provider is configured.
 * All payment operations return failure with a descriptive message.
 */
class NoOpPaymentTerminalClient : PaymentTerminalClient {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.warn("⚠️  Using NoOpPaymentTerminalClient - no terminal provider configured (set terminal.provider to enable)")
    }

    override suspend fun open(): TerminalSimpleResponse {
        log.warn("open() called on NoOpPaymentTerminalClient")
        return TerminalSimpleResponse(success = true, message = "No-op terminal (no provider configured)")
    }

    override suspend fun close(): TerminalSimpleResponse {
        return TerminalSimpleResponse(success = true, message = "No-op terminal closed")
    }

    override fun getStatus(): TerminalStatusResponse {
        return TerminalStatusResponse(
            terminalOpen = false,
            terminalReady = false,
            connectionState = "NO_PROVIDER_CONFIGURED"
        )
    }

    override fun getHealth(): TerminalHealthResponse {
        return TerminalHealthResponse(
            status = "NO_PROVIDER",
            configLoaded = false
        )
    }

    override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        log.warn("reserve() called but no terminal provider configured (correlationId={})", correlationId)
        return TerminalOperationResponse(success = false, error = "No terminal provider configured")
    }

    override suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        log.warn("capture() called but no terminal provider configured (correlationId={})", correlationId)
        return TerminalOperationResponse(success = false, error = "No terminal provider configured")
    }

    override suspend fun reversal(correlationId: String): TerminalOperationResponse {
        log.warn("reversal() called but no terminal provider configured (correlationId={})", correlationId)
        return TerminalOperationResponse(success = false, error = "No terminal provider configured")
    }

    override fun terminalEvents(): Flow<TerminalEvent> = emptyFlow()
}
