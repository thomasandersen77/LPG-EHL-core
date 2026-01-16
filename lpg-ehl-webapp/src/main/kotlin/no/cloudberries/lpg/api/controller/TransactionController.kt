package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.api.client.EmulatorClient
import no.cloudberries.lpg.service.dto.CreateTransactionRequest
import no.cloudberries.lpg.service.dto.ErrorResponse
import no.cloudberries.lpg.service.dto.PageResponse
import no.cloudberries.lpg.service.dto.TransactionResponse
import no.cloudberries.lpg.service.model.Transaction
import no.cloudberries.lpg.service.service.TransactionService
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
    @Autowired(required = false)
    private var emulatorClient: EmulatorClient? = null
    private val logger = LoggerFactory.getLogger(TransactionController::class.java)

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

        @Parameter(description = "Filter by payment status (PENDING, PAID)")
        @RequestParam(required = false)
        paymentStatus: String?,

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
        logger.info("📋 List transactions: page=$page, size=$size, dispenser=$dispenserAddress, paymentType=$paymentType, paymentStatus=$paymentStatus")
        val pageSize = size.coerceAtMost(100)
        val transactions = transactionService.getTransactions(from, to, dispenserAddress, paymentType, paymentStatus, customerId, page, pageSize)
        logger.info("✅ Returned ${transactions.content.size} transactions (total=${transactions.totalElements})")
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
        logger.info("🔍 Get transaction by ID: $id")
        val transaction = transactionService.getTransactionById(id)
            ?: run {
                logger.warn("⚠️ Transaction not found: $id")
                return ResponseEntity.notFound().build()
            }
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

    @PatchMapping("/{id}/payment")
    @Operation(
        summary = "Update payment status",
        description = "Update the payment status and method for a transaction after settlement"
    )
    fun updatePaymentStatus(
        @PathVariable id: UUID,
        @RequestParam paymentMethod: String,
        @RequestParam(defaultValue = "PAID") paymentStatus: String
    ): ResponseEntity<TransactionResponse> {
        logger.info("💳 Update payment status: id=$id, method=$paymentMethod, status=$paymentStatus")
        
        val updated = transactionService.updatePaymentStatus(id, paymentMethod, paymentStatus)
            ?: run {
                logger.warn("⚠️ Transaction not found: $id")
                return ResponseEntity.notFound().build()
            }
        
        logger.info("✅ Payment status updated for transaction $id")
        
        // Notify emulator to reset dispenser and broadcast to Windows (LAB MODE only)
        if (emulatorClient != null) {
            logger.info("📢 Notifying emulator to settle dispenser #${updated.dispenserAddress}")
            val settled = emulatorClient!!.settleDispenser(updated.dispenserAddress, paymentMethod)
            if (settled) {
                logger.info("✅ Emulator reset broadcast sent to Windows")
            } else {
                logger.warn("⚠️ Failed to notify emulator (Windows may still show old values)")
            }
        } else {
            logger.info("🏭 FIELD MODE: Skipping emulator notification (not available)")
        }
        
        return ResponseEntity.ok(TransactionResponse.from(updated))
    }

    @PostMapping
    @Operation(
        summary = "Create transaction",
        description = "Create a new transaction (typically called from emulator when dispensing stops)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun createTransaction(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Transaction data from emulator"
        )
        @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("➥ Create transaction: dispenser=${request.dispenserAddress}, volume=${request.volumeDeciliters/10.0}L, amount=${request.amountOre/100.0} NOK, paymentType=${request.paymentType ?: "PENDING"}, paymentStatus=PENDING")
        
        // Convert request to Transaction entity
        val transaction = Transaction(
            dispenserAddress = request.dispenserAddress,
            nozzleNumber = request.nozzleNumber,
            volumeDeciliters = request.volumeDeciliters,
            amountOre = request.amountOre,
            pricePerLiter = BigDecimal.valueOf(request.pricePerLiter.toLong()).divide(BigDecimal(100)), // Convert øre to kr
            paymentType = request.paymentType,
            productCode = request.productCode,
            includesRoadTax = request.includesRoadTax
        )
        
        val saved = transactionService.saveTransaction(transaction)
        logger.info("✅ Transaction created: id=${saved.transactionId}")
        return ResponseEntity.status(201).body(TransactionResponse.from(saved))
    }
}
