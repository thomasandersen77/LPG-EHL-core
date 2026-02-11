package no.cloudberries.lpg.service.terminal

/**
 * Abstraction for talking to a payment terminal provider (real Nets Cloud Connect, simulator, etc.).
 */
interface TerminalClient {
    /**
     * Reserve an amount on the card (pre-auth).
     * Pump can be freed after successful reservation.
     */
    fun reserve(amountMinor: Int): ReservationResponse

    /**
     * Complete a prior reservation by charging the actual amount.
     */
    fun capture(operationId: String, amountMinor: Int): CaptureResponse

    /**
     * Attempt to reverse the current/last operation.
     */
    fun reversal(operationId: String? = null): ReversalResponse
}

data class ReservationResponse(
    val success: Boolean,
    val operationId: String? = null,
    val error: String? = null
)

data class CaptureResponse(
    val success: Boolean,
    val operationId: String? = null,
    val error: String? = null
)

data class ReversalResponse(
    val success: Boolean,
    val operationId: String? = null,
    val error: String? = null
)
