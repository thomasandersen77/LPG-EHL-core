package no.cloudberries.lpg.service.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Read-only data class for daily_summary view
 */
data class DailySummary(
    val summaryDate: LocalDate,
    val dispenserAddress: Int,
    val transactionCount: Int,
    val totalVolumeDeciliters: Long,
    val totalAmountOre: Long,
    val averagePricePerLiter: BigDecimal?
) {
    val totalVolumeLiters: BigDecimal
        get() = BigDecimal(totalVolumeDeciliters).divide(BigDecimal(10))

    val totalAmountKr: BigDecimal
        get() = BigDecimal(totalAmountOre).divide(BigDecimal(100))
}
