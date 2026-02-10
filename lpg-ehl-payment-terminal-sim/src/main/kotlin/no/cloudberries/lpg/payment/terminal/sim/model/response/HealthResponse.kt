package no.cloudberries.lpg.payment.terminal.sim.model.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Health response.
 *
 * NOTE: This response uses lowercase keys (exception to PascalCase rule).
 * Must be serialized manually to ensure lowercase keys.
 */
data class HealthResponse(
    @JsonProperty("status")
    val status: String,

    @JsonProperty("timestamp")
    val timestamp: String,

    @JsonProperty("configLoaded")
    val configLoaded: Boolean
)
