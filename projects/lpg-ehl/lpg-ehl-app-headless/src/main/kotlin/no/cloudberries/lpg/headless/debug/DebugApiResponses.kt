package no.cloudberries.lpg.headless.debug

import java.time.Instant

/**
 * DTOs for Debug API responses.
 * 
 * These provide clean JSON responses for curl-based field testing.
 */

data class HealthResponse(
    val status: String = "UP",
    val timestamp: Instant = Instant.now(),
    val mode: String,               // LAB or FIELD
    val serialPort: String?,        // Current serial port
    val emulatorEnabled: Boolean
)

data class StateResponse(
    val address: Int,
    val state: String,              // IDLE, READY_TO_PUMP, PUMPING, STOPPED, PAYMENT_PENDING
    val volumeLitres: Double,
    val amountKr: Double,
    val pricePerLitreKr: Double,
    val nozzleLifted: Boolean,
    val hasPendingTransaction: Boolean,
    val timestamp: Instant = Instant.now()
)

data class VolumeResponse(
    val address: Int,
    val volumeLitres: Double,
    val volumeCentilitres: Int,
    val raw: String?,               // Raw hex bytes from dispenser
    val timestamp: Instant = Instant.now()
)

data class CommandResponse(
    val command: String,            // LINETEST, UNBLOCK, BLOCK
    val address: Int,
    val success: Boolean,
    val message: String,
    val responseCode: String? = null,  // Raw response from dispenser
    val timestamp: Instant = Instant.now()
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now()
)
