package no.cloudberries.lpg.service.terminal

import java.time.Instant
import java.util.UUID

/**
 * Represents a payment authorization (reserve) for a dispenser.
 *
 * Tracks the lifecycle of a card-initiated payment from reserve → capture/reversal.
 */
data class PaymentAuthorization(
    val authId: String = UUID.randomUUID().toString(),
    val correlationId: String,
    val dispenserAddress: Int,
    val reservedAmountMinor: Int,
    var capturedAmountMinor: Int? = null,
    var status: AuthStatus,
    val flow: PaymentFlow,
    val createdAt: Instant = Instant.now(),
    var completedAt: Instant? = null,
    var errorMessage: String? = null
)

/**
 * Payment authorization status.
 */
enum class AuthStatus {
    /** Reserve request sent, awaiting response */
    AUTH_PENDING,

    /** Reserve successful, pump can be released */
    AUTHORIZED,

    /** Fueling complete, capture request sent */
    PENDING_CAPTURE,

    /** Capture successful, payment complete */
    PAID,

    /** Reserve or capture failed */
    FAILED,

    /** Payment failed, awaiting operator action */
    PAYMENT_FAILED,

    /** Transaction reversed/cancelled */
    REVERSED
}

/**
 * Payment flow type for logging and analytics.
 */
enum class PaymentFlow {
    /** Card-initiated: customer tapped card, system auto-reserves */
    CARD_EVENT,

    /** Manual: station owner manually released pump, no card reserve */
    MANUAL_RELEASE
}
