package no.cloudberries.lpg.service.transaction

import no.cloudberries.lpg.service.transaction.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {

    /**
     * Find transactions by dispenser address with pagination
     */
    fun findByDispenserAddress(dispenserAddress: Int, pageable: Pageable): Page<Transaction>

    /**
     * Find transactions within a time range with pagination
     */
    fun findByTimestampBetween(
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction>

    /**
     * Find transactions by dispenser and time range
     */
    @Query(
        """
        SELECT t FROM Transaction t 
        WHERE t.dispenserAddress = :dispenserAddress 
        AND t.timestamp BETWEEN :from AND :to
        ORDER BY t.timestamp DESC
        """
    )
    fun findByDispenserAndTimeRange(
        @Param("dispenserAddress") dispenserAddress: Int,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction>

    /**
     * Get unsynced transactions (query the view)
     */
    @Query(
        nativeQuery = true,
        value = """
        SELECT t.* FROM transactions t
        INNER JOIN azure_sync_queue asq ON asq.entity_id = t.transaction_id
        WHERE asq.status != 'SYNCED'
        ORDER BY t.timestamp DESC
        LIMIT :limit
        """
    )
    fun findUnsyncedTransactions(@Param("limit") limit: Int): List<Transaction>

    /**
     * Count transactions by dispenser
     */
    fun countByDispenserAddress(dispenserAddress: Int): Long

    /**
     * Get latest transaction for a dispenser
     */
    fun findFirstByDispenserAddressOrderByTimestampDesc(dispenserAddress: Int): Transaction?

    /**
     * Find transactions with flexible filtering
     */
    @Query(
        nativeQuery = true,
        value = """
        SELECT * FROM transactions t
        WHERE (COALESCE(:dispenserAddress, -999) = -999 OR t.dispenser_address = :dispenserAddress)
        AND (COALESCE(:paymentType, '') = '' OR t.payment_type = :paymentType)
        AND (COALESCE(:paymentStatus, '') = '' OR t.payment_status = :paymentStatus)
        AND (COALESCE(CAST(:customerId AS VARCHAR), '') = '' OR t.customer_id = :customerId)
        AND (COALESCE(:fromDate, TIMESTAMP '1900-01-01 00:00:00') = TIMESTAMP '1900-01-01 00:00:00' OR t.timestamp >= :fromDate)
        AND (COALESCE(:toDate, TIMESTAMP '9999-12-31 23:59:59') = TIMESTAMP '9999-12-31 23:59:59' OR t.timestamp <= :toDate)
        ORDER BY t.timestamp DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM transactions t
        WHERE (COALESCE(:dispenserAddress, -999) = -999 OR t.dispenser_address = :dispenserAddress)
        AND (COALESCE(:paymentType, '') = '' OR t.payment_type = :paymentType)
        AND (COALESCE(:paymentStatus, '') = '' OR t.payment_status = :paymentStatus)
        AND (COALESCE(CAST(:customerId AS VARCHAR), '') = '' OR t.customer_id = :customerId)
        AND (COALESCE(:fromDate, TIMESTAMP '1900-01-01 00:00:00') = TIMESTAMP '1900-01-01 00:00:00' OR t.timestamp >= :fromDate)
        AND (COALESCE(:toDate, TIMESTAMP '9999-12-31 23:59:59') = TIMESTAMP '9999-12-31 23:59:59' OR t.timestamp <= :toDate)
        """
    )
    fun findWithFilters(
        @Param("dispenserAddress") dispenserAddress: Int?,
        @Param("paymentType") paymentType: String?,
        @Param("paymentStatus") paymentStatus: String?,
        @Param("customerId") customerId: UUID?,
        @Param("fromDate") from: LocalDateTime?,
        @Param("toDate") to: LocalDateTime?,
        pageable: Pageable
    ): Page<Transaction>

    /**
     * Find transactions by customer ID, ordered by timestamp descending
     */
    fun findByCustomerIdOrderByTimestampDesc(customerId: UUID): List<Transaction>
    
    /**
     * Check if there are any unpaid transactions for a dispenser
     */
    fun existsByDispenserAddressAndPaymentStatus(
        dispenserAddress: Int,
        paymentStatus: String
    ): Boolean
    
    /**
     * Find latest unpaid transaction for a dispenser
     */
    fun findFirstByDispenserAddressAndPaymentStatusOrderByTimestampDesc(
        dispenserAddress: Int,
        paymentStatus: String
    ): Transaction?
    
    /**
     * Find all transactions with a specific payment status
     */
    fun findByPaymentStatus(paymentStatus: String): List<Transaction>
}
