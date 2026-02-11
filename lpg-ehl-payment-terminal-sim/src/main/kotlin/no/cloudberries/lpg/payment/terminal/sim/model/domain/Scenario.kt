package no.cloudberries.lpg.payment.terminal.sim.model.domain

/**
 * Predefined scenarios for simulating terminal behavior.
 *
 * Scenarios can be selected via:
 * 1. Default configuration (payment-terminal-sim.default-scenario)
 * 2. HTTP header: X-Terminal-Scenario
 * 3. Control endpoint (future enhancement)
 */
enum class Scenario {
    /**
     * Purchase approved (Success=true, ResponseCode="00")
     */
    APPROVED,

    /**
     * Card declined by issuer (Success=false, ResponseCode="05")
     */
    DECLINED,

    /**
     * Wrong PIN entered (Success=false, ResponseCode="Z1", RejectionReason="3:2:Z1")
     */
    WRONG_PIN,

    /**
     * User cancelled transaction (Success=false, ResponseCode="", RejectionReason="2:1")
     */
    USER_CANCEL,

    /**
     * Operation timeout (HTTP 408)
     */
    TIMEOUT,

    /**
     * Terminal busy with another operation (HTTP 409)
     */
    BUSY,

    /**
     * Terminal not ready (HTTP 503)
     */
    NOT_READY
}
