package no.cloudberries.lpg.service.terminal.adapter

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.cloudberries.lpg.netscloud.NetsCloudConnectTerminalClient
import no.cloudberries.lpg.netscloud.NetsResponseParser
import no.cloudberries.lpg.service.terminal.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Adapter that bridges service-layer PaymentTerminalClient interface to Nets Cloud Connect implementation.
 *
 * Key responsibilities:
 * - Maps domain concepts (reserve/capture/reversal) to Nets Cloud Connect operations
 * - Implements robust retry logic for "terminal busy" scenarios
 * - Emits terminal events for reactive payment flows
 * - Enforces settling delays between operations
 */
@Service
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-connect")
class NetsCloudTerminalAdapter(
    private val netsClient: NetsCloudConnectTerminalClient,
    private val responseParser: NetsResponseParser
) : PaymentTerminalClient {

    private val log = LoggerFactory.getLogger(javaClass)

    // Event flow for card-initiated payments
    private val _terminalEvents = MutableSharedFlow<TerminalEvent>(replay = 0, extraBufferCapacity = 10)

    // Mutual exclusion for terminal operations
    private val terminalMutex = Mutex()

    // Track last operation time for settling delay
    private var lastOperationTimeMs = 0L

    // Coroutine scope for event listening
    private val eventScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Start listening to Nets WebSocket events
        startEventListener()
    }

    // ============================================================================
    // Lifecycle Management
    // ============================================================================

    override suspend fun open(): TerminalSimpleResponse {
        log.info("🔌 Opening Nets Cloud Connect terminal")
        return netsClient.openTerminal()
    }

    override suspend fun close(): TerminalSimpleResponse {
        log.info("🔌 Closing Nets Cloud Connect terminal")
        eventScope.cancel()
        return netsClient.closeTerminal()
    }

    override fun getStatus(): TerminalStatusResponse {
        return netsClient.getStatus()
    }

    override fun getHealth(): TerminalHealthResponse {
        return netsClient.getHealth()
    }

    // ============================================================================
    // Payment Operations
    // ============================================================================

    override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        log.info("💳 OPERATION=reserve | amount={} kr | correlationId={}", amountMinor / 100.0, correlationId)

        return terminalMutex.withLock {
            // Settling delay: prevent rapid-fire operations
            enforceSettlingDelay()

            // Retry with backoff for "busy" conditions
            val response = retryWithBackoff(
                operation = "reserve",
                correlationId = correlationId,
                maxAttempts = 5,
                initialDelayMs = 300,
                maxDelayMs = 2000
            ) { attempt ->
                // Check terminal is ready before attempting
                val status = netsClient.getStatus()
                if (!status.terminalReady) {
                    log.warn("⚠️  OPERATION=reserve | attempt={}/5 | status=not_ready | correlationId={}",
                        attempt, correlationId)
                    throw TerminalNotReadyException()
                }

                log.debug("💳 OPERATION=reserve | attempt={}/5 | status=attempting | correlationId={}",
                    attempt, correlationId)

                // Nets Cloud Connect: "purchase" is both reserve and capture in one operation
                // We use the full amount as "reserve" amount
                val request = TerminalPurchaseRequest(
                    amountMinor = amountMinor,
                    operatorId = correlationId
                )

                netsClient.purchase(request)
            }

            lastOperationTimeMs = System.currentTimeMillis()
            log.info("✅ OPERATION=reserve | status=success | amount={} kr | correlationId={} | operationId={}",
                amountMinor / 100.0, correlationId, response.operationId)
            response
        }
    }

    override suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        log.info("✅ OPERATION=capture | amount={} kr | correlationId={} | note=no-op (Nets auto-captures)",
            amountMinor / 100.0, correlationId)

        // Nets Cloud Connect: capture happens automatically during purchase
        // The actual amount is already charged, so this is a no-op
        // Just return success to maintain interface compatibility
        return TerminalOperationResponse(
            success = true,
            operationId = correlationId,
            callResult = 1,
            responseCode = "00"
        )
    }

    override suspend fun reversal(correlationId: String): TerminalOperationResponse {
        log.warn("🔄 OPERATION=reversal | correlationId={}", correlationId)

        return terminalMutex.withLock {
            enforceSettlingDelay()

            val response = retryWithBackoff(
                operation = "reversal",
                correlationId = correlationId,
                maxAttempts = 3,
                initialDelayMs = 500,
                maxDelayMs = 1500
            ) { attempt ->
                log.debug("🔄 OPERATION=reversal | attempt={}/3 | status=attempting | correlationId={}",
                    attempt, correlationId)
                netsClient.reversal()
            }

            lastOperationTimeMs = System.currentTimeMillis()
            if (response.success) {
                log.info("✅ OPERATION=reversal | status=success | correlationId={} | operationId={}",
                    correlationId, response.operationId)
            } else {
                log.error("❌ OPERATION=reversal | status=failed | correlationId={} | error={}",
                    correlationId, response.error)
            }
            response
        }
    }

    // ============================================================================
    // Event Stream
    // ============================================================================

    override fun terminalEvents(): Flow<TerminalEvent> = _terminalEvents.asSharedFlow()

    /**
     * Listen to Nets Cloud Connect WebSocket and emit domain events.
     *
     * Note: This is a placeholder. In production, we'd hook into the WebSocket
     * message stream from NetsCloudWebSocketClient and parse Nets-specific events:
     * - Dfs13JsonReceived (card presented)
     * - Dfs13TerminalReady
     * - Dfs13LocalMode (transaction result)
     * - etc.
     */
    private fun startEventListener() {
        // TODO: Hook into NetsCloudWebSocketClient message stream
        // For now, this is a placeholder that would emit events like:
        //
        // eventScope.launch {
        //     netsWebSocketClient.messages.collect { message ->
        //         when {
        //             responseParser.isJsonReceived(message) -> {
        //                 _terminalEvents.emit(TerminalEvent.CardPresented())
        //             }
        //             responseParser.isTerminalReady(message) -> {
        //                 _terminalEvents.emit(TerminalEvent.TerminalReady(config.terminalId))
        //             }
        //             responseParser.isTransactionComplete(message) -> {
        //                 val result = responseParser.parseLocalMode(message)
        //                 _terminalEvents.emit(TerminalEvent.TransactionResult(
        //                     approved = result?.result == 1,
        //                     amountMinor = result?.totalAmount?.toInt() ?: 0,
        //                     correlationId = "...",
        //                     responseCode = result?.responseCode
        //                 ))
        //             }
        //         }
        //     }
        // }

        log.info("📡 Nets Cloud Connect event listener started (placeholder)")
    }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    /**
     * Enforce a settling delay between operations to avoid "terminal busy" errors.
     * Nets terminals need a brief pause after operations before accepting new commands.
     */
    private suspend fun enforceSettlingDelay() {
        val settlingDelayMs = 500L
        val timeSinceLastOp = System.currentTimeMillis() - lastOperationTimeMs

        if (timeSinceLastOp < settlingDelayMs) {
            val waitTime = settlingDelayMs - timeSinceLastOp
            log.debug("⏸️  Settling delay: waiting {}ms", waitTime)
            delay(waitTime)
        }
    }

    /**
     * Retry a terminal operation with exponential backoff.
     *
     * Handles transient failures like:
     * - Terminal not ready
     * - Terminal busy
     * - Temporary connection issues
     *
     * @param operation Operation name for logging
     * @param correlationId Correlation ID for logging
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay before first retry
     * @param maxDelayMs Maximum delay between retries
     * @param block The operation to retry
     * @return Response from successful operation
     * @throws Exception if all retries exhausted
     */
    private suspend fun retryWithBackoff(
        operation: String,
        correlationId: String,
        maxAttempts: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        block: suspend (attempt: Int) -> TerminalOperationResponse
    ): TerminalOperationResponse {
        var attempt = 1
        var delayMs = initialDelayMs
        var lastError: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                val response = block(attempt)

                // Check for terminal busy in response
                if (!response.success && response.errorCode == "terminal_busy") {
                    throw TerminalBusyException(response.error ?: "Terminal busy")
                }

                // Success - return response
                if (response.success || attempt >= maxAttempts) {
                    if (!response.success) {
                        log.error("❌ {} failed after {} attempts (correlationId={}): {}",
                            operation, maxAttempts, correlationId, response.error)
                    }
                    return response
                }

                // Other error - retry
                log.warn("⚠️  {} attempt {}/{} failed (correlationId={}): {}",
                    operation, attempt, maxAttempts, correlationId, response.error)

            } catch (e: TerminalNotReadyException) {
                lastError = e
                log.warn("⚠️  {} attempt {}/{}: Terminal not ready (correlationId={})",
                    operation, attempt, maxAttempts, correlationId)

            } catch (e: TerminalBusyException) {
                lastError = e
                log.warn("⚠️  {} attempt {}/{}: Terminal busy (correlationId={})",
                    operation, attempt, maxAttempts, correlationId)

            } catch (e: Exception) {
                lastError = e
                log.error("❌ {} attempt {}/{} failed with exception (correlationId={})",
                    operation, attempt, maxAttempts, correlationId, e)

                // Don't retry on unexpected exceptions
                throw e
            }

            // Reached max attempts?
            if (attempt >= maxAttempts) {
                log.error("❌ {} exhausted all {} attempts (correlationId={})",
                    operation, maxAttempts, correlationId)
                throw lastError ?: Exception("$operation failed after $maxAttempts attempts")
            }

            // Wait before retry with exponential backoff
            log.info("🔄 Retrying {} in {}ms (attempt {}/{}, correlationId={})",
                operation, delayMs, attempt + 1, maxAttempts, correlationId)
            delay(delayMs)

            delayMs = minOf(delayMs * 2, maxDelayMs)
            attempt++
        }

        // Should never reach here, but just in case
        throw lastError ?: Exception("$operation failed")
    }
}
