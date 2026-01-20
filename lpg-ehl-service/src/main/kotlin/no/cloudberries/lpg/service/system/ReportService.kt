package no.cloudberries.lpg.service.system

import no.cloudberries.lpg.service.dto.DailySummaryResponse
import no.cloudberries.lpg.service.dto.PeriodSummaryResponse
import no.cloudberries.lpg.service.repository.DailySummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ReportService(
    private val dailySummaryRepository: DailySummaryRepository
) {

    fun getDailySummary(date: LocalDate, dispenserAddress: Int? = null): List<DailySummaryResponse> {
        return if (dispenserAddress != null) {
            listOfNotNull(
                dailySummaryRepository.findByDateAndDispenser(date, dispenserAddress)
                    ?.let { DailySummaryResponse.from(it) }
            )
        } else {
            dailySummaryRepository.findByDate(date)
                .map { DailySummaryResponse.from(it) }
        }
    }

    fun getPeriodSummary(
        from: LocalDate,
        to: LocalDate,
        dispenserAddress: Int? = null
    ): PeriodSummaryResponse {
        val dailySummaries = dailySummaryRepository.findByDateRange(from, to, dispenserAddress)
            .map { DailySummaryResponse.from(it) }

        val totalTransactions = dailySummaries.sumOf { it.transactionCount }
        val totalVolume = dailySummaries.fold(BigDecimal.ZERO) { acc, summary ->
            acc.add(summary.totalVolumeLiters)
        }
        val totalAmount = dailySummaries.fold(BigDecimal.ZERO) { acc, summary ->
            acc.add(summary.totalAmountKr)
        }

        val averagePrice = if (totalVolume > BigDecimal.ZERO) {
            totalAmount.divide(totalVolume, 2, RoundingMode.HALF_UP)
        } else {
            null
        }

        return PeriodSummaryResponse(
            fromDate = from,
            toDate = to,
            dispenserAddress = dispenserAddress,
            totalTransactions = totalTransactions,
            totalVolumeLiters = totalVolume,
            totalAmountKr = totalAmount,
            averagePricePerLiter = averagePrice,
            dailySummaries = dailySummaries
        )
    }

    fun getMonthSummary(year: Int, month: Int, dispenserAddress: Int? = null): PeriodSummaryResponse {
        val from = LocalDate.of(year, month, 1)
        val to = from.plusMonths(1).minusDays(1)
        return getPeriodSummary(from, to, dispenserAddress)
    }

    fun getYearSummary(year: Int, dispenserAddress: Int? = null): PeriodSummaryResponse {
        val from = LocalDate.of(year, 1, 1)
        val to = LocalDate.of(year, 12, 31)
        return getPeriodSummary(from, to, dispenserAddress)
    }
}
