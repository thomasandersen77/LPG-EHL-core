package no.cloudberries.lpg.api

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.service.event.EventPublisher
import no.cloudberries.lpg.service.event.NoOpEventPublisher
import no.cloudberries.lpg.transport.SerialTransport
import no.cloudberries.lpg.service.payment.Payment
import no.cloudberries.lpg.service.payment.PaymentGateway
import no.cloudberries.lpg.service.payment.PaymentRequest
import no.cloudberries.lpg.service.payment.PaymentStatus
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Test configuration for API parity tests.
 *
 * The ApiParityTest verifies request mappings across the "web" entry points.
 * To discover request mappings Spring must be able to instantiate the controllers,
 * which means their service-layer dependencies also need to be available.
 */
@SpringBootApplication(
    scanBasePackages = [
        "no.cloudberries.lpg.api",
        "no.cloudberries.lpg.service"
    ]
)
@Profile("test")
class ApiTestConfiguration {
    @Bean
    fun eventPublisher(): EventPublisher = NoOpEventPublisher()

    @Bean
    fun paymentGateway(): PaymentGateway = object : PaymentGateway {
        private val payments = ConcurrentHashMap<UUID, Payment>()

        override fun startPayment(request: PaymentRequest): Payment {
            // For API parity tests we don't need real payment processing.
            val payment = Payment(
                amountCents = request.amountCents,
                method = request.method,
                status = PaymentStatus.APPROVED,
                reference = request.reference,
                metadata = request.metadata,
                completedAt = Instant.now()
            )
            payments[payment.id] = payment
            return payment
        }

        override fun getPayment(id: UUID): Payment? = payments[id]
    }

    /**
     * API parity tests only need the Spring context to start so that request mappings are registered.
     * We therefore provide a minimal, always-connected transport + communicator.
     */
    @Bean
    fun serialTransport(): SerialTransport = object : SerialTransport {
        override val isConnected: Boolean = true
        override fun connect(): Boolean = true
        override fun disconnect() = Unit
        override fun write(data: ByteArray): Int = data.size
        override fun readAvailable(maxBytes: Int): ByteArray = ByteArray(0)
        override fun flush() = Unit
    }

    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator =
        EhlCommunicator(transport = transport, enableRawLogging = false)
}
