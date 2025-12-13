package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.api.dto.PageResponse
import no.cloudberries.lpg.api.dto.TransactionResponse
import no.cloudberries.lpg.api.repository.TransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class TransactionService(
    private val transactionRepository: TransactionRepository
) {

    fun getTransactions(
        from: LocalDateTime?,
        to: LocalDateTime?,
        dispenserAddress: Int?,
        page: Int = 0,
        size: Int = 50
    ): PageResponse<TransactionResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        
        val resultPage = when {
            from != null && to != null && dispenserAddress != null -> {
                transactionRepository.findByDispenserAndTimeRange(dispenserAddress, from, to, pageable)
            }
            from != null && to != null -> {
                transactionRepository.findByTimestampBetween(from, to, pageable)
            }
            dispenserAddress != null -> {
                transactionRepository.findByDispenserAddress(dispenserAddress, pageable)
            }
            else -> {
                transactionRepository.findAll(pageable)
            }
        }

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
}
