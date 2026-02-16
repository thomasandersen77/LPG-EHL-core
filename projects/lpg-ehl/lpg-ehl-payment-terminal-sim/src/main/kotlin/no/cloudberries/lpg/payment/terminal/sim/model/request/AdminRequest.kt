package no.cloudberries.lpg.payment.terminal.sim.model.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Admin request (for operations requiring password).
 */
data class AdminRequest(
    @JsonProperty("Password")
    val password: String = "0000"
)

/**
 * Generic admin code request.
 *
 * Used for arbitrary admin codes via /v1/admin/code endpoint.
 */
data class AdminCodeRequest(
    @JsonProperty("Code")
    val code: Int,

    @JsonProperty("Password")
    val password: String = "0000"
)
