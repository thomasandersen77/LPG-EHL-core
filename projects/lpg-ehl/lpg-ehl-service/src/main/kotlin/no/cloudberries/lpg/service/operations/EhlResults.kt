package no.cloudberries.lpg.service.operations

import no.cloudberries.lpg.protocol.EhlPacket

/**
 * Domain result classes for EHL operations.
 * Used by EhlOperationsService to return rich, type-safe results.
 */

/**
 * Result of volume query.
 */
data class VolumeResult(
    val volumeLitres: Double,
    val pumpNumber: Int,
    val raw: EhlPacket
)

/**
 * Result of price query.
 */
data class PriceResult(
    val pricePerLitreCents: Int,
    val pricePerLitreKr: Double = pricePerLitreCents / 100.0,
    val raw: EhlPacket
)

/**
 * Result of error query.
 */
data class ErrorResult(
    val hasError: Boolean,
    val errorCode: Int?,
    val errorDescription: String?,
    val raw: EhlPacket
)

/**
 * Result of tank query.
 */
data class TankResult(
    val tankLevelPercent: Int,
    val raw: EhlPacket
)

/**
 * Result of VB6 sequence test.
 */
data class SequenceResult(
    val allPassed: Boolean,
    val failedAt: String?,
    val testsRun: Int,
    val totalTests: Int,
    val steps: List<SequenceStep>
)

data class SequenceStep(
    val command: String,
    val passed: Boolean,
    val duration: Long,
    val errorMessage: String? = null
)
