package no.cloudberries.lpg.service.repository

import no.cloudberries.lpg.service.model.AzureSyncQueue
import no.cloudberries.lpg.service.model.SyncStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AzureSyncQueueRepository : JpaRepository<AzureSyncQueue, UUID> {

    /**
     * Find pending items ready for sync (not in progress, under retry limit)
     */
    @Query(
        """
        SELECT asq FROM AzureSyncQueue asq 
        WHERE asq.status = :status 
        AND asq.retryCount < :maxRetries
        ORDER BY asq.createdAt ASC
        """
    )
    fun findPendingItems(
        @Param("status") status: SyncStatus,
        @Param("maxRetries") maxRetries: Int
    ): List<AzureSyncQueue>

    /**
     * Find items by status
     */
    fun findByStatus(status: SyncStatus): List<AzureSyncQueue>

    /**
     * Count pending items
     */
    fun countByStatus(status: SyncStatus): Long

    /**
     * Find failed items that exceeded retry limit
     */
    @Query(
        """
        SELECT asq FROM AzureSyncQueue asq 
        WHERE asq.status = :status 
        AND asq.retryCount >= :maxRetries
        """
    )
    fun findFailedItems(
        @Param("status") status: SyncStatus,
        @Param("maxRetries") maxRetries: Int
    ): List<AzureSyncQueue>

    /**
     * Delete synced items older than cutoff
     */
    @Modifying
    @Query(
        """
        DELETE FROM AzureSyncQueue asq 
        WHERE asq.status = 'SYNCED' 
        AND asq.syncedAt < :cutoffDate
        """
    )
    fun deleteSyncedItemsOlderThan(@Param("cutoffDate") cutoffDate: java.time.LocalDateTime): Int

    /**
     * Check if entity is already queued
     */
    fun existsByEntityIdAndStatusNot(entityId: UUID, status: SyncStatus): Boolean
}
