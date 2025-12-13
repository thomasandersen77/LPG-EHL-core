package no.cloudberries.lpg.api.repository

import no.cloudberries.lpg.api.model.DispenserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface DispenserStatusRepository : JpaRepository<DispenserStatus, Int> {

    /**
     * Find dispensers seen after a specific time
     */
    fun findByLastSeenAfter(since: LocalDateTime): List<DispenserStatus>

    /**
     * Find active dispensers (seen in last N minutes)
     */
    @Query(
        """
        SELECT ds FROM DispenserStatus ds 
        WHERE ds.lastSeen > :cutoffTime
        ORDER BY ds.lastSeen DESC
        """
    )
    fun findActiveDispensers(cutoffTime: LocalDateTime): List<DispenserStatus>

    /**
     * Check if dispenser exists
     */
    fun existsByDispenserAddress(dispenserAddress: Int): Boolean
}
