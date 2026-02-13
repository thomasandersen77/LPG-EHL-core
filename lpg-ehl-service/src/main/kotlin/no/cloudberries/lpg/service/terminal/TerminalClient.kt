package no.cloudberries.lpg.service.terminal

/**
 * Abstraction for talking to a payment terminal provider (real Nets Cloud Connect, simulator, etc.).
 */
interface TerminalClient {
    /**
     * Open the payment terminal before performing financial operations.
     */
    fun openTerminal(): TerminalSimpleResponse

    /**
     * Perform a purchase (card payment) on the terminal.
     */
    fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse

    /**
     * Attempt to reverse the current/last operation.
     */
    fun reversal(operationId: String? = null): TerminalOperationResponse
}

data class TerminalSimpleResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

data class TerminalOperationResponse(
    val success: Boolean,
    val operationId: String? = null,
    val callResult: Int? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class TerminalPurchaseRequest(
    val amountMinor: Int,
    val operatorId: String = "0000",
    val currency: String = "NOK",
    val optionalData: String? = null,
    val clientRequestId: String? = null,
    val preAvstemming: TerminalPreAvstemmingConfig? = null
)

data class TerminalPreAvstemmingConfig(
    val enabled: Boolean = false,
    val password: String = "0000",
    val timeoutSeconds: Int? = null
)
