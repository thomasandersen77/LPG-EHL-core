package no.cloudberries.lpg.service.azure

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "azure_sync_queue")
class AzureSyncQueue(
    @Id
    @Column(name = "queue_id")
    var queueId: UUID = UUID.randomUUID(),

    @Column(name = "entity_type", nullable = false)
    var entityType: String,

    @Column(name = "entity_id", nullable = false)
    var entityId: UUID,

    @Type(JsonBinaryType::class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    var payload: Map<String, Any>,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SyncStatus = SyncStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "synced_at")
    var syncedAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AzureSyncQueue) return false
        return queueId == other.queueId
    }

    override fun hashCode(): Int = queueId.hashCode()

    override fun toString(): String {
        return "AzureSyncQueue(id=$queueId, type=$entityType, entityId=$entityId, " +
                "status=$status, retries=$retryCount)"
    }
}

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    SYNCED,
    FAILED
}
