package no.cloudberries.lpg.service.azure

import com.azure.core.util.BinaryData
import com.azure.storage.queue.QueueClient
import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.logging.MdcActor
import no.cloudberries.lpg.service.dto.SyncStatusResponse
import no.cloudberries.lpg.service.azure.AzureSyncQueue
import no.cloudberries.lpg.service.azure.SyncStatus
import no.cloudberries.lpg.service.azure.AzureSyncQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min
import kotlin.math.pow

@Service
@ConditionalOnProperty(name = ["azure.enabled"], havingValue = "true")
class AzureSyncService(
    private val queueClient: QueueClient,
    private val syncQueueRepository: AzureSyncQueueRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${azure.sync.batch-size}") private val batchSize: Int,
    @Value("\${azure.sync.max-retries}") private val maxRetries: Int
) {
    private val logger = LoggerFactory.getLogger(AzureSyncService::class.java)

    /**
     * Scheduled task that runs every interval to sync pending items
     */
    @Scheduled(fixedDelayString = "\${azure.sync.interval-seconds}000")
    @Transactional
    fun syncPendingItems() {
        MdcActor.runWithActor(MdcActor.Actor.SYSTEM) {
        logger.debug("Starting Azure sync job...")
        
        try {
            val pendingItems = syncQueueRepository.findPendingItems(SyncStatus.PENDING, maxRetries)
                .take(batchSize)
            
            if (pendingItems.isEmpty()) {
                logger.debug("No pending items to sync")
                return
            }

            logger.info("Processing ${pendingItems.size} pending items for Azure sync")
            
            var successCount = 0
            var failCount = 0

            for (item in pendingItems) {
                try {
                    syncItem(item)
                    successCount++
                } catch (e: Exception) {
                    logger.error("Failed to sync item ${item.queueId}: ${e.message}", e)
                    handleSyncFailure(item, e)
                    failCount++
                }
            }

            logger.info("Azure sync completed: $successCount succeeded, $failCount failed")
            
        } catch (e: Exception) {
            logger.error("Azure sync job failed: ${e.message}", e)
        }
        }
    }

    /**
     * Sync a single item to Azure Storage Queue
     */
    @Transactional
    fun syncItem(item: AzureSyncQueue) {
        logger.debug("Syncing item ${item.queueId} (type=${item.entityType}, entityId=${item.entityId})")
        
        // Mark as IN_PROGRESS
        item.status = SyncStatus.IN_PROGRESS
        syncQueueRepository.save(item)

        try {
            // Serialize payload to JSON
            val messageBody = objectMapper.writeValueAsString(item.payload)
            
            // Send to Azure Storage Queue
            queueClient.sendMessage(BinaryData.fromString(messageBody))
            
            // Mark as SYNCED
            item.status = SyncStatus.SYNCED
            item.syncedAt = LocalDateTime.now()
            item.lastError = null
            syncQueueRepository.save(item)
            
            logger.info("✅ Successfully synced ${item.entityType} ${item.entityId} to Azure")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to sync ${item.entityType} ${item.entityId}: ${e.message}")
            throw e
        }
    }

    /**
     * Handle sync failure with exponential backoff
     */
    @Transactional
    fun handleSyncFailure(item: AzureSyncQueue, error: Exception) {
        item.retryCount++
        item.lastError = error.message?.take(500)
        
        if (item.retryCount >= maxRetries) {
            item.status = SyncStatus.FAILED
            logger.error("⛔ Item ${item.queueId} exceeded max retries ($maxRetries), marking as FAILED")
        } else {
            item.status = SyncStatus.PENDING
            val backoffSeconds = calculateBackoff(item.retryCount)
            logger.warn("⚠️ Item ${item.queueId} failed (retry ${item.retryCount}/$maxRetries), " +
                    "will retry in ~${backoffSeconds}s")
        }
        
        syncQueueRepository.save(item)
    }

    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoff(retryCount: Int): Long {
        // Exponential backoff: 2^retry * base (e.g., 30s, 60s, 120s)
        val baseDelaySeconds = 30L
        return min((2.0.pow(retryCount) * baseDelaySeconds).toLong(), 600L) // Max 10 minutes
    }

    /**
     * Get sync status statistics
     */
    fun getSyncStatus(): SyncStatusResponse {
        val pendingCount = syncQueueRepository.countByStatus(SyncStatus.PENDING)
        val syncedCount = syncQueueRepository.countByStatus(SyncStatus.SYNCED)
        val failedCount = syncQueueRepository.countByStatus(SyncStatus.FAILED)
        
        val lastSyncedItem = syncQueueRepository.findByStatus(SyncStatus.SYNCED)
            .maxByOrNull { it.syncedAt ?: LocalDateTime.MIN }
        
        return SyncStatusResponse(
            pendingCount = pendingCount,
            syncedCount = syncedCount,
            failedCount = failedCount,
            lastSyncTime = lastSyncedItem?.syncedAt
        )
    }

    /**
     * Manually trigger sync for a specific item
     */
    @Transactional
    fun retrySyncItem(queueId: java.util.UUID): Boolean {
        val item = syncQueueRepository.findById(queueId).orElse(null) ?: return false
        
        if (item.status == SyncStatus.SYNCED) {
            logger.warn("Item $queueId is already synced")
            return false
        }
        
        try {
            syncItem(item)
            return true
        } catch (e: Exception) {
            handleSyncFailure(item, e)
            return false
        }
    }

    /**
     * Clean up old synced items (run daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    fun cleanupOldSyncedItems() {
        logger.info("Starting cleanup of old synced items...")
        
        try {
            val cutoffDate = LocalDateTime.now().minusDays(7) // Keep 7 days of history
            val deletedCount = syncQueueRepository.deleteSyncedItemsOlderThan(cutoffDate)
            
            if (deletedCount > 0) {
                logger.info("Cleaned up $deletedCount old synced items")
            }
        } catch (e: Exception) {
            logger.error("Failed to cleanup old synced items: ${e.message}", e)
        }
    }
}
