package no.cloudberries.lpg.service.repository

import no.cloudberries.lpg.service.model.DailySummary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class DailySummaryRepository(private val jdbcTemplate: JdbcTemplate) {

    /**
     * Get daily summary for a specific date and dispenser
     */
    fun findByDateAndDispenser(date: LocalDate, dispenserAddress: Int): DailySummary? {
        val sql = """
            SELECT summary_date, dispenser_address, transaction_count,
                   total_volume_deciliters, total_amount_ore, avg_price_per_liter
            FROM daily_summary
            WHERE summary_date = ? AND dispenser_address = ?
        """
        
        return jdbcTemplate.query(sql, { rs, _ ->
            DailySummary(
                summaryDate = rs.getDate("summary_date").toLocalDate(),
                dispenserAddress = rs.getInt("dispenser_address"),
                transactionCount = rs.getInt("transaction_count"),
                totalVolumeDeciliters = rs.getLong("total_volume_deciliters"),
                totalAmountOre = rs.getLong("total_amount_ore"),
                averagePricePerLiter = rs.getBigDecimal("avg_price_per_liter")
            )
        }, date, dispenserAddress).firstOrNull()
    }

    /**
     * Get daily summary for a specific date (all dispensers)
     */
    fun findByDate(date: LocalDate): List<DailySummary> {
        val sql = """
            SELECT summary_date, dispenser_address, transaction_count,
                   total_volume_deciliters, total_amount_ore, avg_price_per_liter
            FROM daily_summary
            WHERE summary_date = ?
            ORDER BY dispenser_address
        """
        
        return jdbcTemplate.query(sql, { rs, _ ->
            DailySummary(
                summaryDate = rs.getDate("summary_date").toLocalDate(),
                dispenserAddress = rs.getInt("dispenser_address"),
                transactionCount = rs.getInt("transaction_count"),
                totalVolumeDeciliters = rs.getLong("total_volume_deciliters"),
                totalAmountOre = rs.getLong("total_amount_ore"),
                averagePricePerLiter = rs.getBigDecimal("avg_price_per_liter")
            )
        }, date)
    }

    /**
     * Get daily summaries for a date range
     */
    fun findByDateRange(from: LocalDate, to: LocalDate, dispenserAddress: Int? = null): List<DailySummary> {
        val sql = if (dispenserAddress != null) {
            """
            SELECT summary_date, dispenser_address, transaction_count,
                   total_volume_deciliters, total_amount_ore, avg_price_per_liter
            FROM daily_summary
            WHERE summary_date BETWEEN ? AND ? AND dispenser_address = ?
            ORDER BY summary_date DESC, dispenser_address
            """
        } else {
            """
            SELECT summary_date, dispenser_address, transaction_count,
                   total_volume_deciliters, total_amount_ore, avg_price_per_liter
            FROM daily_summary
            WHERE summary_date BETWEEN ? AND ?
            ORDER BY summary_date DESC, dispenser_address
            """
        }
        
        return if (dispenserAddress != null) {
            jdbcTemplate.query(sql, { rs, _ ->
                DailySummary(
                    summaryDate = rs.getDate("summary_date").toLocalDate(),
                    dispenserAddress = rs.getInt("dispenser_address"),
                    transactionCount = rs.getInt("transaction_count"),
                    totalVolumeDeciliters = rs.getLong("total_volume_deciliters"),
                    totalAmountOre = rs.getLong("total_amount_ore"),
                    averagePricePerLiter = rs.getBigDecimal("avg_price_per_liter")
                )
            }, from, to, dispenserAddress)
        } else {
            jdbcTemplate.query(sql, { rs, _ ->
                DailySummary(
                    summaryDate = rs.getDate("summary_date").toLocalDate(),
                    dispenserAddress = rs.getInt("dispenser_address"),
                    transactionCount = rs.getInt("transaction_count"),
                    totalVolumeDeciliters = rs.getLong("total_volume_deciliters"),
                    totalAmountOre = rs.getLong("total_amount_ore"),
                    averagePricePerLiter = rs.getBigDecimal("avg_price_per_liter")
                )
            }, from, to)
        }
    }
}
