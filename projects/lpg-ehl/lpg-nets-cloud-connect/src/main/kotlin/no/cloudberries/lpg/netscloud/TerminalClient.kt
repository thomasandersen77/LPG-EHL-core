package no.cloudberries.lpg.netscloud

import kotlinx.serialization.Serializable

/**
 * Abstraction for talking to a payment terminal provider (real Nets Cloud Connect, simulator, etc.).
 */
interface TerminalClient {
    /**
     * Open the payment terminal before performing financial operations.
     */
    fun openTerminal(): TerminalSimpleResponse

    /**
     * Health check for terminal service.
     */
    fun getHealth(): TerminalHealthResponse

    /**
     * Get terminal readiness and connection status.
     */
    fun getStatus(): TerminalStatusResponse

    /**
     * Close terminal connection.
     */
    fun closeTerminal(): TerminalSimpleResponse

    /**
     * Perform a purchase (card payment) on the terminal.
     */
    fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse

    /**
     * Attempt to reverse the current/last operation.
     */
    fun reversal(operationId: String? = null): TerminalOperationResponse
}

@Serializable
data class TerminalSimpleResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class TerminalHealthResponse(
    val status: String,
    val configLoaded: Boolean
)

@Serializable
data class TerminalStatusResponse(
    val terminalOpen: Boolean,
    val terminalReady: Boolean,
    val connectionState: String? = null,
    val lastError: String? = null
)

@Serializable
data class TerminalOperationResponse(
    val success: Boolean,
    val operationId: String? = null,
    val callResult: Int? = null,
    val entryMode: String? = null,
    val entryModeCode: String? = null,
    val localModeResultData: String? = null,
    val responseCode: String? = null,
    val rejectionReason: String? = null,
    val printTextRaw: String? = null,
    val printTextSanitized: String? = null,
    val lastDisplayText: String? = null,
    val localModeResult: Int? = null,
    val durationMs: Long? = null,
    val error: String? = null,
    val errorCode: String? = null
)

@Serializable
data class TerminalPurchaseRequest(
    val amountMinor: Int,
    val operatorId: String = "0000",
    val currency: String = "NOK",
    val optionalData: String? = null,
    val clientRequestId: String? = null,
    val preAvstemming: TerminalPreAvstemmingConfig? = null
)

@Serializable
data class TerminalPreAvstemmingConfig(
    val enabled: Boolean = false,
    val password: String = "0000",
    val timeoutSeconds: Int? = null
)
