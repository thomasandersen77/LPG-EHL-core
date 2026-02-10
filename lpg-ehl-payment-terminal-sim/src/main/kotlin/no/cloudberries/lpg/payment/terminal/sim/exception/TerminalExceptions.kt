package no.cloudberries.lpg.payment.terminal.sim.exception

/**
 * Base exception for terminal operations.
 */
open class TerminalException(
    message: String,
    val errorCode: String,
    val details: String? = null
) : RuntimeException(message)

/**
 * Terminal is busy with another operation (HTTP 409).
 */
class TerminalBusyException(
    message: String = "Terminal is busy with another operation",
    details: String? = null
) : TerminalException(message, "terminal_busy", details)

/**
 * Terminal is not ready for operations (HTTP 503).
 */
class TerminalNotReadyException(
    message: String = "Terminal is not ready for operations",
    details: String? = null
) : TerminalException(message, "terminal_not_ready", details)

/**
 * Operation timeout (HTTP 408).
 */
class OperationTimeoutException(
    message: String = "Terminal operation did not complete within timeout",
    details: String? = null
) : TerminalException(message, "operation_timeout", details)

/**
 * Operation rejected by terminal (HTTP 422).
 */
class OperationRejectedException(
    message: String = "Terminal rejected the operation",
    details: String? = null
) : TerminalException(message, "operation_rejected", details)

/**
 * Vendor call failure (HTTP 500).
 */
class VendorCallFailureException(
    message: String = "Vendor DLL call failed",
    details: String? = null
) : TerminalException(message, "vendor_call_failure", details)

/**
 * Invalid request (HTTP 400).
 */
class InvalidRequestException(
    message: String = "Invalid request",
    details: String? = null
) : TerminalException(message, "invalid_request", details)
