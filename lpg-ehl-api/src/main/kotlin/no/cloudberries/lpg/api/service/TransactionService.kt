package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.api.dto.PageResponse
import no.cloudberries.lpg.api.dto.TransactionResponse
import no.cloudberries.lpg.api.model.Transaction
import no.cloudberries.lpg.api.repository.TransactionRepository
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
}
