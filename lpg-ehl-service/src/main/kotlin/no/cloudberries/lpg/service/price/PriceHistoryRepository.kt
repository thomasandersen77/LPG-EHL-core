package no.cloudberries.lpg.service.price

import no.cloudberries.lpg.service.price.PriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface PriceHistoryRepository : JpaRepository<PriceHistory, UUID> {

    /**
     * Find all price changes for a product, ordered by effective date descending
     */
    fun findByProductCodeOrderByEffectiveFromDesc(productCode: String): List<PriceHistory>

    /**
     * Find the current active price for a product
     */
    @Query(
        """
        SELECT ph FROM PriceHistory ph 
        WHERE ph.productCode = :productCode 
        AND ph.effectiveFrom <= :now
        AND (ph.effectiveUntil IS NULL OR ph.effectiveUntil > :now)
        ORDER BY ph.effectiveFrom DESC
        """
    )
    fun findCurrentPrice(
        @Param("productCode") productCode: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): PriceHistory?

    /**
     * Find price history within a date range
     */
    @Query(
        """
        SELECT ph FROM PriceHistory ph 
        WHERE ph.productCode = :productCode 
        AND ph.effectiveFrom BETWEEN :from AND :to
        ORDER BY ph.effectiveFrom DESC
        """
    )
    fun findPriceHistoryInRange(
        @Param("productCode") productCode: String,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): List<PriceHistory>
}
