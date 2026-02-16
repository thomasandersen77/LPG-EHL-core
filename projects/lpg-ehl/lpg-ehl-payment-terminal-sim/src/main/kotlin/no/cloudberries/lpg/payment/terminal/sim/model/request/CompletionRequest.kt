package no.cloudberries.lpg.payment.terminal.sim.model.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Completion (capture) request.
 * Charges the actual amount against a prior reservation.
 */
data class CompletionRequest(
    @JsonProperty("OperationId")
    val operationId: String,

    @JsonProperty("AmountMinor")
    val amountMinor: Int,

    @JsonProperty("Currency")
    val currency: String = "NOK",

    @JsonProperty("OperatorId")
    val operatorId: String = "0000",

    @JsonProperty("ClientRequestId")
    val clientRequestId: String? = null
)
