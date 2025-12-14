package no.cloudberries.lpg.api.payment

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
@Profile("local", "dev")
class SimulatedPaymentGateway : PaymentGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    // Simple in-memory store for demo/testing
    private val payments: MutableMap<UUID, Payment> = ConcurrentHashMap()

    // How long a card payment should stay in PENDING before auto-resolving
    private val cardProcessingDelay: Duration = Duration.ofSeconds(2)

    override fun startPayment(request: PaymentRequest): Payment {
        val payment = when (request.method) {
            PaymentMethod.CASH -> {
                // Cash is instantly "approved" in this simulation
                Payment(
                    amountCents = request.amountCents,
                    method = request.method,
                    status = PaymentStatus.APPROVED,
                    reference = request.reference,
                    completedAt = Instant.now(),
                    metadata = request.metadata
                )
            }
            PaymentMethod.CARD, PaymentMethod.CREDIT -> {
                Payment(
                    amountCents = request.amountCents,
                    method = request.method,
                    status = PaymentStatus.PENDING,
                    reference = request.reference,
                    metadata = request.metadata
                )
            }
        }

        payments[payment.id] = payment
        log.info("Simulated payment started: {}", payment)

        // For CARD/CREDIT, we kick off a simple background simulation that
        // will resolve the payment after cardProcessingDelay.
        if (payment.status == PaymentStatus.PENDING) {
            simulateAsyncResolution(payment.id)
        }

        return payment
    }

    override fun getPayment(id: UUID): Payment? = payments[id]

    private fun simulateAsyncResolution(id: UUID) {
        Thread {
            try {
                Thread.sleep(cardProcessingDelay.toMillis())
            } catch (_: InterruptedException) {
                return@Thread
            }

            val existing = payments[id] ?: return@Thread

            // Simple rule: approve everything unless explicitly overridden
            val shouldDecline = existing.metadata["simulateDecline"] == "true"

            val updated = existing.copy(
                status = if (shouldDecline) PaymentStatus.DECLINED else PaymentStatus.APPROVED,
                completedAt = Instant.now()
            )

            payments[id] = updated
            log.info("Simulated payment resolved: {}", updated)
        }.start()
    }
}
