package no.cloudberries.lpg.api

import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * Global Exception Handler for LPG-EHL API.
 * 
 * Ensures that:
 * - Stack traces are NEVER leaked to clients
 * - All errors return consistent JSON structure
 * - Errors are logged with correlation IDs for debugging
 * - Security-sensitive errors reveal minimal information
 * 
 * Response format:
 * ```json
 * {
 *   "error": "ERROR_CODE",
 *   "message": "Human-readable message",
 *   "correlationId": "uuid-for-debugging",
 *   "timestamp": "2026-02-07T12:00:00Z"
 * }
 * ```
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    /**
     * Standard error response structure.
     */
    data class ErrorResponse(
        val error: String,
        val message: String,
        val correlationId: String = UUID.randomUUID().toString(),
        val timestamp: Instant = Instant.now()
    )
    
    // ==================== Serial/Communication Errors ====================
    
    /**
     * Handle serial communication timeouts (after all retries exhausted).
     */
    @ExceptionHandler(TimeoutCancellationException::class)
    fun handleTimeout(ex: TimeoutCancellationException): ResponseEntity<ErrorResponse> {
        val correlationId = UUID.randomUUID().toString()
        logger.error("[$correlationId] Serial timeout (all retries exhausted): ${ex.message}")
        
        return ResponseEntity
            .status(HttpStatus.GATEWAY_TIMEOUT)
            .body(ErrorResponse(
                error = "SERIAL_TIMEOUT",
                message = "Kommunikasjon med dispenser tidsavbrutt. Sjekk seriell tilkobling.",
                correlationId = correlationId
            ))
    }
    
    /**
     * Handle serial port I/O errors.
     */
    @ExceptionHandler(IOException::class)
    fun handleIOException(ex: IOException): ResponseEntity<ErrorResponse> {
        val correlationId = UUID.randomUUID().toString()
        logger.error("[$correlationId] I/O error: ${ex.message}", ex)
        
        // Determine if this is a serial port issue
        val isSerialError = ex.message?.contains("serial", ignoreCase = true) == true ||
                ex.message?.contains("port", ignoreCase = true) == true ||
                ex.message?.contains("tty", ignoreCase = true) == true
        
        return if (isSerialError) {
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse(
                    error = "SERIAL_ERROR",
                    message = "Seriell port-feil. Sjekk at porten er tilgjengelig.",
                    correlationId = correlationId
                ))
        } else {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    error = "IO_ERROR",
                    message = "Intern kommunikasjonsfeil oppstod.",
                    correlationId = correlationId
                ))
        }
    }
    
    // ==================== Validation Errors ====================
    
    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        logger.warn("Missing parameter: ${ex.parameterName}")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "MISSING_PARAMETER",
                message = "Mangler påkrevd parameter: ${ex.parameterName}"
            ))
    }
    
    /**
     * Handle invalid parameter types.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch for parameter '${ex.name}': ${ex.value}")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "INVALID_PARAMETER",
                message = "Ugyldig verdi for parameter '${ex.name}': ${ex.value}"
            ))
    }
    
    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed: $errors")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "VALIDATION_FAILED",
                message = "Validering feilet: ${errors.joinToString(", ")}"
            ))
    }
    
    /**
     * Handle malformed JSON in request body.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Malformed JSON: ${ex.message}")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "MALFORMED_JSON",
                message = "Ugyldig JSON i forespørsel"
            ))
    }
    
    // ==================== Business Logic Errors ====================
    
    /**
     * Handle illegal state (e.g., pump already pumping).
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        val correlationId = UUID.randomUUID().toString()
        logger.warn("[$correlationId] Illegal state: ${ex.message}")
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                error = "INVALID_STATE",
                message = ex.message ?: "Operasjonen kan ikke utføres i nåværende tilstand",
                correlationId = correlationId
            ))
    }
    
    /**
     * Handle illegal arguments.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message}")
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "INVALID_ARGUMENT",
                message = ex.message ?: "Ugyldig argument"
            ))
    }
    
    // ==================== Generic Fallback ====================
    
    /**
     * Catch-all handler for unexpected exceptions.
     * 
     * IMPORTANT: Never expose stack traces or internal details to clients.
     * Log the full exception for debugging, return generic message to client.
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val correlationId = UUID.randomUUID().toString()
        
        // Log full exception for debugging
        logger.error("[$correlationId] Unexpected error: ${ex.javaClass.simpleName}: ${ex.message}", ex)
        
        // Return generic message to client - NO stack trace, NO internal details
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                error = "INTERNAL_ERROR",
                message = "En uventet feil oppstod. Referanse: $correlationId",
                correlationId = correlationId
            ))
    }
}
