package no.cloudberries.lpg.payment.terminal.sim.model.domain

/**
 * Terminal state machine states.
 *
 * State transitions:
 * - CLOSED -> OPEN (via /v1/terminal/open)
 * - OPEN -> READY (automatically after successful open)
 * - READY -> BUSY (when operation starts)
 * - BUSY -> READY (when operation completes)
 * - READY -> CLOSED (via /v1/terminal/close)
 */
enum class TerminalState {
    CLOSED,  // Terminal not initialized
    OPEN,    // Terminal opened but not yet ready
    READY,   // Terminal ready for operations
    BUSY     // Operation in progress (returns 409 Conflict)
}
