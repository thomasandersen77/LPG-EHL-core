package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.dto.ErrorResponse
import no.cloudberries.lpg.api.dto.PageResponse
import no.cloudberries.lpg.api.dto.TransactionResponse
import no.cloudberries.lpg.api.service.TransactionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
// @SecurityRequirement(name = "bearer-token") // Disabled for local demo testing
class TransactionController(
    private val transactionService: TransactionService
) {

    @GetMapping
    @Operation(
        summary = "List transactions",
        description = "Get a paginated list of transactions with optional filters"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun getTransactions(
        @Parameter(description = "Start date (ISO format)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,

        @Parameter(description = "End date (ISO format)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,

        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?,

        @Parameter(description = "Filter by payment type (CASH, CARD, CREDIT)")
        @RequestParam(required = false)
        paymentType: String?,

        @Parameter(description = "Filter by customer ID")
        @RequestParam(required = false)
        customerId: UUID?,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "50")
        size: Int
    ): ResponseEntity<PageResponse<TransactionResponse>> {
        val pageSize = size.coerceAtMost(100)
        val transactions = transactionService.getTransactions(from, to, dispenserAddress, paymentType, customerId, page, pageSize)
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieve a single transaction by its UUID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction found"),
            ApiResponse(responseCode = "404", description = "Transaction not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getTransactionById(
        @Parameter(description = "Transaction UUID")
        @PathVariable id: UUID
    ): ResponseEntity<TransactionResponse> {
        val transaction = transactionService.getTransactionById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(transaction)
    }

    @GetMapping("/unsynced")
    @Operation(
        summary = "List unsynced transactions",
        description = "Get transactions that have not been synced to Azure yet"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getUnsyncedTransactions(
        @Parameter(description = "Maximum number of results")
        @RequestParam(defaultValue = "100")
        limit: Int
    ): ResponseEntity<List<TransactionResponse>> {
        val transactions = transactionService.getUnsyncedTransactions(limit.coerceAtMost(1000))
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/count")
    @Operation(
        summary = "Count transactions",
        description = "Get the total number of transactions, optionally filtered by dispenser"
    )
    fun getTransactionCount(
        @Parameter(description = "Filter by dispenser address")
        @RequestParam(required = false)
        dispenserAddress: Int?
    ): ResponseEntity<Map<String, Long>> {
        val count = transactionService.getTransactionCount(dispenserAddress)
        return ResponseEntity.ok(mapOf("count" to count))
    }
}
