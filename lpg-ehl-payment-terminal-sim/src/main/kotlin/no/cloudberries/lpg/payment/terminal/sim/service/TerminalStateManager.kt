package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.exception.TerminalBusyException
import no.cloudberries.lpg.payment.terminal.sim.exception.TerminalNotReadyException
import no.cloudberries.lpg.payment.terminal.sim.model.domain.TerminalState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Terminal state machine manager.
 *
 * Manages terminal state transitions and enforces single-operation concurrency.
 *
 * State transitions:
 * - CLOSED -> OPEN (via open())
 * - OPEN -> READY (automatically after open())
 * - READY -> BUSY (via beginOperation())
 * - BUSY -> READY (via endOperation())
 * - READY -> CLOSED (via close())
 */
@Service
class TerminalStateManager(
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(TerminalStateManager::class.java)
    private val lock = ReentrantLock()

    @Volatile
    private var currentState: TerminalState = TerminalState.CLOSED

    @Volatile
    private var currentOperationId: String? = null

    /**
     * Get current terminal state.
     */
    fun getState(): TerminalState = currentState

    /**
     * Check if terminal is ready for operations.
     */
    fun isReady(): Boolean = currentState == TerminalState.READY

    /**
     * Check if terminal is open (OPEN or READY).
     */
    fun isOpen(): Boolean = currentState == TerminalState.OPEN || currentState == TerminalState.READY

    /**
     * Check if terminal is busy.
     */
    fun isBusy(): Boolean = currentState == TerminalState.BUSY

    /**
     * Open terminal.
     *
     * Transitions: CLOSED -> OPEN -> READY
     */
    fun open() {
        lock.withLock {
            when (currentState) {
                TerminalState.CLOSED -> {
                    log.info("Opening terminal...")
                    currentState = TerminalState.OPEN
                    // Auto-transition to READY
                    Thread.sleep(100) // Simulate initialization
                    currentState = TerminalState.READY
                    log.info("Terminal ready")
                }
                TerminalState.OPEN, TerminalState.READY -> {
                    log.warn("Terminal already open")
                }
                TerminalState.BUSY -> {
                    throw TerminalBusyException("Cannot open terminal while operation in progress")
                }
            }
        }
    }

    /**
     * Close terminal.
     *
     * Transitions: READY -> CLOSED
     */
    fun close() {
        lock.withLock {
            when (currentState) {
                TerminalState.READY, TerminalState.OPEN -> {
                    log.info("Closing terminal...")
                    currentState = TerminalState.CLOSED
                    log.info("Terminal closed")
                }
                TerminalState.CLOSED -> {
                    log.warn("Terminal already closed")
                }
                TerminalState.BUSY -> {
                    throw TerminalBusyException("Cannot close terminal while operation in progress")
                }
            }
        }
    }

    /**
     * Begin an operation (transitions READY -> BUSY).
     *
     * Throws TerminalNotReadyException if terminal not ready.
     * Throws TerminalBusyException if another operation in progress.
     */
    fun beginOperation(operationId: String) {
        lock.withLock {
            when (currentState) {
                TerminalState.READY -> {
                    currentState = TerminalState.BUSY
                    currentOperationId = operationId
                    log.debug("Operation started: {}", operationId)
                }
                TerminalState.BUSY -> {
                    throw TerminalBusyException(
                        "Terminal is busy with another operation",
                        "Current operation: $currentOperationId"
                    )
                }
                TerminalState.CLOSED, TerminalState.OPEN -> {
                    throw TerminalNotReadyException("Terminal is not ready for operations (state: $currentState)")
                }
            }
        }
    }

    /**
     * End an operation (transitions BUSY -> READY).
     */
    fun endOperation(operationId: String) {
        lock.withLock {
            if (currentState == TerminalState.BUSY && currentOperationId == operationId) {
                currentState = TerminalState.READY
                currentOperationId = null
                log.debug("Operation completed: {}", operationId)
            } else {
                log.warn("Attempted to end operation {} but current state is {} (current op: {})",
                    operationId, currentState, currentOperationId)
            }
        }
    }

    /**
     * Require terminal to be ready (throws TerminalNotReadyException if not).
     */
    fun requireReady() {
        if (!isReady()) {
            throw TerminalNotReadyException("Terminal is not ready for operations (state: $currentState)")
        }
    }

    /**
     * Get terminal identity information.
     */
    fun getTerminalIdentity(): Map<String, String> {
        return mapOf(
            "TerminalID" to config.terminalId,
            "MerchantId" to config.merchantId
        )
    }
}
