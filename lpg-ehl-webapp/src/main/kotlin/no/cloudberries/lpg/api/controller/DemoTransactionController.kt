package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.dto.TransactionResponse
import no.cloudberries.lpg.service.transaction.TransactionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/demo/transactions")
@Tag(name = "Demo Transactions", description = "Demo endpoints for manipulating transactions")
class DemoTransactionController(
    private val transactionService: TransactionService
) {

    @PutMapping("/{transactionId}/payment-type")
    @Operation(summary = "Update payment type", description = "Update payment type for a transaction (Demo only)")
    fun updatePaymentType(
        @PathVariable transactionId: UUID,
        @RequestParam paymentType: String,
        @RequestParam(required = false) customerId: UUID?
    ): ResponseEntity<TransactionResponse> {
        val updated = transactionService.updatePaymentType(transactionId, paymentType, customerId)
        return ResponseEntity.ok(TransactionResponse.from(updated))
    }
}
