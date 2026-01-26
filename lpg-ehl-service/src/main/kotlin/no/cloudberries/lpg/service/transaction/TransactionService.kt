package no.cloudberries.lpg.service.transaction

import no.cloudberries.lpg.service.dto.PageResponse
import no.cloudberries.lpg.service.dto.TransactionResponse
import no.cloudberries.lpg.service.transaction.Transaction
import no.cloudberries.lpg.service.transaction.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val transactionSyncService: TransactionSyncService?
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getTransactions(
        from: LocalDateTime?,
        to: LocalDateTime?,
        dispenserAddress: Int?,
        paymentType: String?,
        paymentStatus: String?,
        customerId: UUID?,
        page: Int = 0,
        size: Int = 50
    ): PageResponse<TransactionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        
        val resultPage = transactionRepository.findWithFilters(
            paymentType = paymentType,
            paymentStatus = paymentStatus,
            customerId = customerId,
            from = from,
            to = to,
            pageable = pageable
        )

        return PageResponse(
            content = resultPage.content.map { TransactionResponse.from(it) },
            totalElements = resultPage.totalElements,
            totalPages = resultPage.totalPages,
            currentPage = resultPage.number,
            pageSize = resultPage.size,
            hasNext = resultPage.hasNext(),
            hasPrevious = resultPage.hasPrevious()
        )
    }

    fun getTransactionById(id: UUID): TransactionResponse? {
        return transactionRepository.findById(id)
            .map { TransactionResponse.from(it) }
            .orElse(null)
    }

    fun getUnsyncedTransactions(limit: Int = 100): List<TransactionResponse> {
        return transactionRepository.findUnsyncedTransactions(limit)
            .map { TransactionResponse.from(it) }
    }

    fun getTransactionCount(dispenserAddress: Int? = null): Long {
        return if (dispenserAddress != null) {
            transactionRepository.countByDispenserAddress(dispenserAddress)
        } else {
            transactionRepository.count()
        }
    }

    @Transactional
    fun saveTransaction(transaction: Transaction): Transaction {
        logger.info("Saving transaction: {} liters, {} øre", transaction.volumeDeciliters / 10.0, transaction.amountOre)
        
        val saved = transactionRepository.save(transaction)
        
        // Queue for Azure sync
        transactionSyncService?.queueTransactionForSync(saved, "CREATED")
        
        logger.info("Transaction saved successfully with ID: {}", saved.transactionId)
        
        return saved
    }

    @Transactional
    fun updatePaymentType(transactionId: UUID, paymentType: String, customerId: UUID? = null): Transaction {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { IllegalArgumentException("Transaction not found: $transactionId") }

        transaction.paymentType = paymentType
        if (customerId != null) {
            transaction.customerId = customerId
        }

        return transactionRepository.save(transaction)
    }

    @Transactional
    fun updatePaymentStatus(transactionId: UUID, paymentMethod: String, paymentStatus: String): Transaction? {
        val transaction = transactionRepository.findById(transactionId).orElse(null) ?: return null
        
        transaction.paymentType = paymentMethod
        transaction.paymentStatus = paymentStatus
        
        val saved = transactionRepository.save(transaction)
        
        // Queue for Azure sync when payment is updated
        transactionSyncService?.queueTransactionForSync(saved, "PAYMENT_UPDATED")
        
        logger.info("✅ Updated transaction {} payment: method={}, status={}", transactionId, paymentMethod, paymentStatus)
        
        return saved
    }
    
    // ============================================================
    // PUMP LIFECYCLE METHODS
    // ============================================================
    
    /**
     * Create a new transaction when pump is unblocked (STARTED status).
     * Called when FRI PUMPE is pressed.
     */
    @Transactional
    fun createStartedTransaction(
        dispenserAddress: Int,
        pricePerLiterKr: Double
    ): Transaction {
        val transaction = Transaction(
            dispenserAddress = dispenserAddress,
            nozzleNumber = 1,
            volumeDeciliters = 0,
            amountOre = 0,
            pricePerLiter = java.math.BigDecimal.valueOf(pricePerLiterKr),
            paymentType = null,
            paymentStatus = "STARTED",
            productCode = "LPG",
            includesRoadTax = true
        )
        
        val saved = transactionRepository.save(transaction)
        logger.info("⛽ Transaksjon opprettet: ID={}, dispenser={}, pris={} kr/L, status=STARTED", 
            saved.transactionId, dispenserAddress, pricePerLiterKr)
        
        return saved
    }
    
    /**
     * Update transaction volume and amount during/after pumping.
     * Called periodically and when pump is stopped.
     */
    @Transactional
    fun updateTransactionVolume(
        transactionId: UUID,
        volumeLiters: Double,
        amountKr: Double,
        newStatus: String? = null
    ): Transaction? {
        val transaction = transactionRepository.findById(transactionId).orElse(null) ?: return null
        
        transaction.volumeDeciliters = (volumeLiters * 10).toInt()
        transaction.amountOre = (amountKr * 100).toInt()
        
        if (newStatus != null) {
            transaction.paymentStatus = newStatus
        }
        
        val saved = transactionRepository.save(transaction)
        
        if (newStatus == "PENDING") {
            logger.info("🛑 Transaksjon stoppet: ID={}, volum={} L, beløp={} kr, status=PENDING",
                transactionId, volumeLiters, amountKr)
        }
        
        return saved
    }
    
    /**
     * Mark transaction as paid.
     * Called when SIMULER BETALING is pressed.
     */
    @Transactional
    fun markTransactionPaid(
        transactionId: UUID,
        paymentMethod: String = "CARD"
    ): Transaction? {
        val transaction = transactionRepository.findById(transactionId).orElse(null) ?: return null
        
        transaction.paymentType = paymentMethod
        transaction.paymentStatus = "PAID"
        
        val saved = transactionRepository.save(transaction)
        
        // Queue for Azure sync
        transactionSyncService?.queueTransactionForSync(saved, "PAID")
        
        logger.info("💳 Transaksjon betalt: ID={}, volum={} L, beløp={} kr, metode={}",
            transactionId, 
            saved.volumeDeciliters / 10.0,
            saved.amountOre / 100.0,
            paymentMethod)
        
        return saved
    }
    
    /**
     * Pay all pending transactions.
     * Useful for clearing out unpaid transactions in test/demo scenarios.
     */
    @Transactional
    fun payAllPendingTransactions(paymentMethod: String = "CARD"): List<Transaction> {
        val pendingTransactions = transactionRepository.findByPaymentStatus("PENDING")
        
        logger.info("💳 Betaler {} ventende transaksjoner med metode: {}", pendingTransactions.size, paymentMethod)
        
        val paidTransactions = pendingTransactions.map { transaction ->
            transaction.paymentType = paymentMethod
            transaction.paymentStatus = "PAID"
            val saved = transactionRepository.save(transaction)
            
            // Queue for Azure sync
            transactionSyncService?.queueTransactionForSync(saved, "PAID")
            
            logger.info("✅ Transaksjon betalt: ID={}, volum={} L, beløp={} kr",
                saved.transactionId,
                saved.volumeDeciliters / 10.0,
                saved.amountOre / 100.0)
            
            saved
        }
        
        logger.info("✅ Totalt {} transaksjoner betalt", paidTransactions.size)
        
        return paidTransactions
    }
}
