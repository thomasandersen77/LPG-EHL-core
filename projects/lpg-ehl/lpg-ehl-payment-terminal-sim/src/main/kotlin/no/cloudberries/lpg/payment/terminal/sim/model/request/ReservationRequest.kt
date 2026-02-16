package no.cloudberries.lpg.payment.terminal.sim.model.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Reservation (pre-auth) request.
 * Reserves an amount on the card. Actual charge happens via completion.
 */
data class ReservationRequest(
    @JsonProperty("AmountMinor")
    val amountMinor: Int,

    @JsonProperty("Currency")
    val currency: String = "NOK",

    @JsonProperty("OperatorId")
    val operatorId: String = "0000",

    @JsonProperty("OptionalData")
    val optionalData: String? = null,

    @JsonProperty("ClientRequestId")
    val clientRequestId: String? = null
)
