package no.cloudberries.lpg.payment.terminal.sim.model.response

/**
 * Error response (PascalCase).
 *
 * Used for HTTP 4xx/5xx error responses.
 */
data class ErrorResponse(
    val Error: String,
    val ErrorCode: String,
    val OperationId: String? = null,
    val Details: String? = null
)
