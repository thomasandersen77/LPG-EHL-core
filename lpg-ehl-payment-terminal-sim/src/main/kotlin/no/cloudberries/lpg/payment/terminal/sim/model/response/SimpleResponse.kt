package no.cloudberries.lpg.payment.terminal.sim.model.response

/**
 * Simple response (PascalCase).
 *
 * Used by /v1/terminal/open and /v1/terminal/close.
 */
data class SimpleResponse(
    val Success: Boolean,
    val Message: String? = null,
    val Error: String? = null
)
