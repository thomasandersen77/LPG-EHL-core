package no.cloudberries.lpg.api.repository

import no.cloudberries.lpg.api.model.RoadTaxSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface RoadTaxSettingsRepository : JpaRepository<RoadTaxSettings, UUID> {
    
    /**
     * Find the most recent road tax setting
     */
    fun findFirstByOrderByEffectiveFromDesc(): RoadTaxSettings?
    
    /**
     * Find road tax settings effective at a given time
     */
    @Query("""
        SELECT r FROM RoadTaxSettings r 
        WHERE r.effectiveFrom <= :time 
        AND (r.effectiveUntil IS NULL OR r.effectiveUntil > :time)
        ORDER BY r.effectiveFrom DESC
    """)
    fun findEffectiveAt(time: LocalDateTime): List<RoadTaxSettings>
    
    /**
     * Find all road tax settings ordered by effective date
     */
    fun findAllByOrderByEffectiveFromDesc(): List<RoadTaxSettings>
}
