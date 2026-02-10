package no.cloudberries.lpg.payment.terminal.sim.model.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Purchase request.
 *
 * VB6 equivalent: TransferAmount_V2("0000", 0x30, amountMinor, ...)
 */
data class PurchaseRequest(
    @JsonProperty("AmountMinor")
    val amountMinor: Int,

    @JsonProperty("Currency")
    val currency: String = "NOK",

    @JsonProperty("OperatorId")
    val operatorId: String = "0000",

    @JsonProperty("OptionalData")
    val optionalData: String? = null,

    @JsonProperty("ClientRequestId")
    val clientRequestId: String? = null,

    @JsonProperty("PreAvstemming")
    val preAvstemming: PreAvstemmingConfig? = null
)

/**
 * Refund request.
 *
 * VB6 equivalent: TransferAmount_V2("0000", 0x31, amountMinor, ...)
 */
data class RefundRequest(
    @JsonProperty("AmountMinor")
    val amountMinor: Int,

    @JsonProperty("Currency")
    val currency: String = "NOK",

    @JsonProperty("OperatorId")
    val operatorId: String = "0000",

    @JsonProperty("OptionalData")
    val optionalData: String? = null,

    @JsonProperty("ClientRequestId")
    val clientRequestId: String? = null,

    @JsonProperty("PreAvstemming")
    val preAvstemming: PreAvstemmingConfig? = null
)

/**
 * Cashback (purchase + cashback) request.
 */
data class CashbackRequest(
    @JsonProperty("PurchaseMinor")
    val purchaseMinor: Int,

    @JsonProperty("CashbackMinor")
    val cashbackMinor: Int,

    @JsonProperty("Currency")
    val currency: String = "NOK",

    @JsonProperty("OperatorId")
    val operatorId: String = "4321",

    @JsonProperty("OptionalData")
    val optionalData: String? = null,

    @JsonProperty("ClientRequestId")
    val clientRequestId: String? = null
)

/**
 * Pre-avstemming configuration (reconciliation before financial operation).
 */
data class PreAvstemmingConfig(
    @JsonProperty("Enabled")
    val enabled: Boolean = false,

    @JsonProperty("Password")
    val password: String = "0000",

    @JsonProperty("TimeoutSeconds")
    val timeoutSeconds: Int? = null
)
