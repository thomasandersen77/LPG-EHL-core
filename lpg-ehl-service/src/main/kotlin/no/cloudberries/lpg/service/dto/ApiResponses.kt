package no.cloudberries.lpg.service.dto

import no.cloudberries.lpg.service.model.DailySummary
import no.cloudberries.lpg.service.pump.DispenserStatus
import no.cloudberries.lpg.service.transaction.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Transaction response DTO
 */
data class TransactionResponse(
    val transactionId: UUID,
    val dispenserAddress: Int,
    val nozzleNumber: Int,
    val productCode: String?,
    val volumeLiters: BigDecimal,
    val amountKr: BigDecimal,
    val pricePerLiter: BigDecimal?,
    val paymentType: String?,
    val paymentStatus: String,
    val customerId: UUID?,
    val customerName: String?,
    val includesRoadTax: Boolean,
    val timestamp: LocalDateTime,
    val decodedData: Map<String, Any>?
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            transactionId = transaction.transactionId ?: UUID.randomUUID(), // Fallback for new transactions
            dispenserAddress = transaction.dispenserAddress,
            nozzleNumber = transaction.nozzleNumber,
            productCode = transaction.productCode,
            volumeLiters = transaction.volumeLiters,
            amountKr = transaction.amountKr,
            pricePerLiter = transaction.pricePerLiter,
            paymentType = transaction.paymentType,
            paymentStatus = transaction.paymentStatus,
            customerId = transaction.customerId,
            customerName = transaction.customerName,
            includesRoadTax = transaction.includesRoadTax,
            timestamp = transaction.timestamp,
            decodedData = transaction.decodedData
        )
    }
}

/**
 * Paginated response wrapper
 */
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Dispenser status response DTO
 */
data class DispenserStatusResponse(
    val dispenserAddress: Int,
    val state: String,
    val lastTransactionId: UUID?,
    val errorCode: Int?,
    val lastSeen: LocalDateTime
) {
    companion object {
        fun from(status: DispenserStatus) = DispenserStatusResponse(
            dispenserAddress = status.address,
            state = status.state,
            lastTransactionId = status.currentTransactionId,
            errorCode = status.errorCode,
            lastSeen = status.lastActive
        )
    }
}

/**
 * Daily summary response DTO
 */
data class DailySummaryResponse(
    val summaryDate: LocalDate,
    val dispenserAddress: Int,
    val transactionCount: Int,
    val totalVolumeLiters: BigDecimal,
    val totalAmountKr: BigDecimal,
    val averagePricePerLiter: BigDecimal?
) {
    companion object {
        fun from(summary: DailySummary) = DailySummaryResponse(
            summaryDate = summary.summaryDate,
            dispenserAddress = summary.dispenserAddress,
            transactionCount = summary.transactionCount,
            totalVolumeLiters = summary.totalVolumeLiters,
            totalAmountKr = summary.totalAmountKr,
            averagePricePerLiter = summary.averagePricePerLiter
        )
    }
}

/**
 * Period summary response (aggregated from multiple days)
 */
data class PeriodSummaryResponse(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val dispenserAddress: Int?,
    val totalTransactions: Int,
    val totalVolumeLiters: BigDecimal,
    val totalAmountKr: BigDecimal,
    val averagePricePerLiter: BigDecimal?,
    val dailySummaries: List<DailySummaryResponse>
)

/**
 * Azure sync status response
 */
data class SyncStatusResponse(
    val pendingCount: Long,
    val syncedCount: Long,
    val failedCount: Long,
    val lastSyncTime: LocalDateTime?
)

/**
 * Create transaction request (from emulator)
 */
data class CreateTransactionRequest(
    val stationId: String? = null,  // Multi-station support
    val edgeId: String? = null,      // Edge device ID
    val dispenserId: String? = null, // Dispenser ID within station
    val dispenserAddress: Int,
    val nozzleNumber: Int = 1,
    val volumeDeciliters: Int,
    val amountOre: Int,
    val pricePerLiter: Int,
    val paymentType: String? = null, // null = awaiting payment
    val productCode: String? = "LPG",
    val includesRoadTax: Boolean = true
)

/**
 * Azure Queue message DTO (for viewing messages in frontend)
 */
data class AzureQueueMessageDto(
    val messageId: String,
    val insertionTime: LocalDateTime,
    val expirationTime: LocalDateTime,
    val dequeueCount: Long,
    val entityType: String?,
    val entityId: String?,
    val status: String?,
    val retryCount: Int,
    val transaction: TransactionData?
) {
    data class TransactionData(
        val dispenserAddress: Int?,
        val volumeLiters: Double?,
        val amountKr: Double?,
        val pricePerLiter: Double?,
        val paymentType: String?,
        val paymentStatus: String?,
        val timestamp: String?
    )
}

/**
 * Azure Queue messages grouped by date
 */
data class AzureQueueByDateResponse(
    val dates: Map<String, List<AzureQueueMessageDto>>,
    val totalMessages: Int
)

/**
 * Error response
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
