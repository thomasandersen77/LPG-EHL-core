package no.cloudberries.lpg.payment.terminal.sim.exception

import no.cloudberries.lpg.payment.terminal.sim.model.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler for REST controllers.
 *
 * Maps exceptions to appropriate HTTP status codes and ErrorResponse.
 */
@RestControllerAdvice
class TerminalExceptionHandler {

    private val log = LoggerFactory.getLogger(TerminalExceptionHandler::class.java)

    @ExceptionHandler(TerminalBusyException::class)
    fun handleTerminalBusy(ex: TerminalBusyException): ResponseEntity<ErrorResponse> {
        log.warn("Terminal busy: {}", ex.message)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(
                Error = ex.message ?: "Terminal busy",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(TerminalNotReadyException::class)
    fun handleTerminalNotReady(ex: TerminalNotReadyException): ResponseEntity<ErrorResponse> {
        log.warn("Terminal not ready: {}", ex.message)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse(
                Error = ex.message ?: "Terminal not ready",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(OperationTimeoutException::class)
    fun handleOperationTimeout(ex: OperationTimeoutException): ResponseEntity<ErrorResponse> {
        log.warn("Operation timeout: {}", ex.message)
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(
            ErrorResponse(
                Error = ex.message ?: "Operation timeout",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(OperationRejectedException::class)
    fun handleOperationRejected(ex: OperationRejectedException): ResponseEntity<ErrorResponse> {
        log.warn("Operation rejected: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse(
                Error = ex.message ?: "Operation rejected",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(VendorCallFailureException::class)
    fun handleVendorCallFailure(ex: VendorCallFailureException): ResponseEntity<ErrorResponse> {
        log.error("Vendor call failure: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                Error = ex.message ?: "Vendor call failure",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(ex: InvalidRequestException): ResponseEntity<ErrorResponse> {
        log.warn("Invalid request: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                Error = ex.message ?: "Invalid request",
                ErrorCode = ex.errorCode,
                Details = ex.details
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                Error = "Internal server error",
                ErrorCode = "internal_error",
                Details = ex.message
            )
        )
    }
}
