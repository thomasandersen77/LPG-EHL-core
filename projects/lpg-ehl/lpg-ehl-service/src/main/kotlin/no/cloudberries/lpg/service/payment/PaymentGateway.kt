package no.cloudberries.lpg.service.payment

import java.time.Instant
import java.util.UUID

enum class PaymentMethod {
    CARD,
    CREDIT
}

enum class PaymentStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED
}

data class PaymentRequest(
    val amountCents: Long,
    val method: PaymentMethod,
    val reference: String,
    val metadata: Map<String, String> = emptyMap()
)

data class Payment(
    val id: UUID = UUID.randomUUID(),
    val requestedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val amountCents: Long,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val reference: String,
    val metadata: Map<String, String> = emptyMap()
)

interface PaymentGateway {
    /**
     * Start a new payment request. Implementations should create a new Payment
     * with status = PENDING and return it.
     */
    fun startPayment(request: PaymentRequest): Payment

    /**
     * Look up a payment by its id.
     */
    fun getPayment(id: UUID): Payment?
}
