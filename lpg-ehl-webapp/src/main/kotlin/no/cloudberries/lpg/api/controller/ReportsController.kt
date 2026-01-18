package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.dto.DailySummaryResponse
import no.cloudberries.lpg.service.dto.PeriodSummaryResponse
import no.cloudberries.lpg.service.service.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reporting and analytics endpoints")
// @SecurityRequirement(name = "bearer-token") // Disabled for local demo testing
class ReportsController(
    private val reportService: ReportService
) {

    @GetMapping("/daily")
    @Operation(
        summary = "Get daily summary",
        description = "Get transaction summary for a specific date"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getDailySummary(
        @Parameter(description = "Date (YYYY-MM-DD), defaults to today")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,

        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?
    ): ResponseEntity<List<DailySummaryResponse>> {
        val summaryDate = date ?: LocalDate.now()
        val summary = reportService.getDailySummary(summaryDate, dispenserAddress)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/period")
    @Operation(
        summary = "Get period summary",
        description = "Get aggregated transaction summary for a date range"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "400", description = "Invalid date range"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getPeriodSummary(
        @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,

        @Parameter(description = "End date (YYYY-MM-DD)", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate,

        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?
    ): ResponseEntity<PeriodSummaryResponse> {
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().build()
        }
        val summary = reportService.getPeriodSummary(from, to, dispenserAddress)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/month/{year}/{month}")
    @Operation(
        summary = "Get monthly summary",
        description = "Get transaction summary for a specific month"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "400", description = "Invalid year or month"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getMonthSummary(
        @Parameter(description = "Year (e.g., 2024)")
        @PathVariable year: Int,

        @Parameter(description = "Month (1-12)")
        @PathVariable month: Int,

        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?
    ): ResponseEntity<PeriodSummaryResponse> {
        if (month !in 1..12) {
            return ResponseEntity.badRequest().build()
        }
        val summary = reportService.getMonthSummary(year, month, dispenserAddress)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/year/{year}")
    @Operation(
        summary = "Get yearly summary",
        description = "Get transaction summary for a specific year"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getYearSummary(
        @Parameter(description = "Year (e.g., 2024)")
        @PathVariable year: Int,

        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?
    ): ResponseEntity<PeriodSummaryResponse> {
        val summary = reportService.getYearSummary(year, dispenserAddress)
        return ResponseEntity.ok(summary)
    }
}
