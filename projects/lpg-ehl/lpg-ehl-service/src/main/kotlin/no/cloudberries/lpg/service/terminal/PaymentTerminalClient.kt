package no.cloudberries.lpg.service.terminal

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction layer for payment terminal operations.
 *
 * Supports both event-driven (card-initiated) and manual (station owner) payment flows.
 * Implementations can be Nets Cloud Connect, legacy simulators, or other terminal types.
 */
interface PaymentTerminalClient {

    // ============================================================================
    // Lifecycle Management
    // ============================================================================

    /**
     * Open/initialize the terminal connection.
     * Should be idempotent - multiple calls should not cause errors.
     */
    suspend fun open(): TerminalSimpleResponse

    /**
     * Close the terminal connection and release resources.
     */
    suspend fun close(): TerminalSimpleResponse

    /**
     * Get current terminal status (ready, connected, etc.)
     */
    fun getStatus(): TerminalStatusResponse

    /**
     * Get terminal health check information.
     */
    fun getHealth(): TerminalHealthResponse

    // ============================================================================
    // Payment Operations
    // ============================================================================

    /**
     * Reserve (pre-authorize) an amount on the customer's card.
     *
     * Used in card-initiated flow:
     * 1. Customer taps card
     * 2. Reserve maximum amount (e.g., 1500 NOK)
     * 3. Release pump
     * 4. Capture actual amount after fueling
     *
     * @param amountMinor Amount in minor units (øre for NOK)
     * @param correlationId Unique ID to track this payment through the full lifecycle
     * @return Response with success status and operation ID
     */
    suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse

    /**
     * Capture (finalize) a previously reserved amount.
     *
     * Called after fueling stops to charge the actual amount.
     * Amount may be less than or equal to the reserved amount.
     *
     * @param amountMinor Actual amount to charge in minor units
     * @param correlationId Same ID as used in reserve()
     * @return Response with success status
     */
    suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse

    /**
     * Reverse/cancel a payment (full or partial).
     *
     * Used when:
     * - Pump fails to start after reserve
     * - Capture fails
     * - Need to cancel an authorized transaction
     *
     * @param correlationId ID of the payment to reverse
     * @return Response with success status
     */
    suspend fun reversal(correlationId: String): TerminalOperationResponse

    // ============================================================================
    // Event Stream
    // ============================================================================

    /**
     * Stream of terminal events for reactive payment flow.
     *
     * Emit events like:
     * - Card presented/tapped
     * - Terminal ready/idle
     * - Transaction results
     * - Errors
     *
     * Subscribe to this flow to implement card-initiated payment flows.
     */
    fun terminalEvents(): Flow<TerminalEvent>
}

/**
 * Events emitted by the payment terminal.
 */
sealed class TerminalEvent {
    /**
     * Card was presented/tapped/inserted by customer.
     * Triggers the reserve → release pump flow.
     */
    data class CardPresented(
        val cardType: String? = null,
        val maskedPan: String? = null
    ) : TerminalEvent()

    /**
     * Terminal is ready and idle, can accept new transactions.
     */
    data class TerminalReady(
        val terminalId: String
    ) : TerminalEvent()

    /**
     * A transaction completed (approved or declined).
     */
    data class TransactionResult(
        val approved: Boolean,
        val amountMinor: Int,
        val correlationId: String,
        val responseCode: String? = null
    ) : TerminalEvent()

    /**
     * Terminal error occurred.
     */
    data class Error(
        val message: String,
        val errorCode: String? = null
    ) : TerminalEvent()

    /**
     * Interactive prompt from terminal (e.g., PIN bypass, signature).
     * Most implementations auto-confirm these.
     */
    data class InteractivePrompt(
        val promptType: String,
        val message: String
    ) : TerminalEvent()
}

/**
 * Exception thrown when terminal is not ready for operations.
 */
class TerminalNotReadyException(message: String = "Terminal not ready") : Exception(message)

/**
 * Exception thrown when terminal is busy with another operation.
 */
class TerminalBusyException(message: String = "Terminal busy") : Exception(message)
