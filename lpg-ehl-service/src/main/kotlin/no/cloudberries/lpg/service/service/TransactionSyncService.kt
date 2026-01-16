package no.cloudberries.lpg.service.service

import no.cloudberries.lpg.service.model.AzureSyncQueue
import no.cloudberries.lpg.service.model.SyncStatus
import no.cloudberries.lpg.service.model.Transaction
import no.cloudberries.lpg.service.repository.AzureSyncQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Service for queuing transactions to be synced to Azure Storage Queue
 */
@Service
@ConditionalOnProperty(name = ["azure.enabled"], havingValue = "true")
class TransactionSyncService(
    private val syncQueueRepository: AzureSyncQueueRepository
) {
    private val logger = LoggerFactory.getLogger(TransactionSyncService::class.java)

    /**
     * Queue a transaction for Azure sync
     * Called when a transaction is created or updated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun queueTransactionForSync(transaction: Transaction, eventType: String) {
        try {
            val transactionId = transaction.transactionId ?: run {
                logger.warn("Cannot queue transaction without ID")
                return
            }

            // Create payload with transaction data
            val payload = mapOf(
                "transactionId" to transactionId.toString(),
                "dispenserAddress" to transaction.dispenserAddress,
                "nozzleNumber" to transaction.nozzleNumber,
                "volumeLiters" to transaction.volumeLiters.toString(),
                "amountKr" to transaction.amountKr.toString(),
                "pricePerLiter" to (transaction.pricePerLiter?.toString() ?: "0"),
                "paymentType" to (transaction.paymentType ?: "PENDING"),
                "paymentStatus" to transaction.paymentStatus,
                "customerId" to (transaction.customerId?.toString() ?: ""),
                "customerName" to (transaction.customerName ?: ""),
                "productCode" to (transaction.productCode ?: ""),
                "includesRoadTax" to transaction.includesRoadTax,
                "timestamp" to transaction.timestamp.toString(),
                "eventType" to eventType // "CREATED", "PAYMENT_UPDATED"
            )

            val syncItem = AzureSyncQueue(
                entityType = "TRANSACTION",
                entityId = transactionId,
                payload = payload,
                status = SyncStatus.PENDING
            )

            syncQueueRepository.save(syncItem)
            
            logger.info("📤 Queued transaction $transactionId for Azure sync (eventType=$eventType)")
            
        } catch (e: Exception) {
            logger.error("Failed to queue transaction for sync: ${e.message}", e)
            // Don't throw - we don't want to fail the main transaction
        }
    }
}
