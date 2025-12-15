package no.cloudberries.lpg.credit

import no.cloudberries.lpg.api.model.Transaction
import no.cloudberries.lpg.api.repository.TransactionRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

// DTOs
data class CreditAccountDto(
    val id: UUID,
    val customerName: String,
    val customerNumber: String,
    val balanceNok: BigDecimal,
    val creditLimitNok: BigDecimal,
    val availableCreditNok: BigDecimal,
    val lastActivityAt: LocalDateTime?
)

data class CreateCreditAccountRequest(
    val customerName: String,
    val customerNumber: String,
    val initialBalanceNok: Double = 0.0,
    val creditLimitNok: Double = 10000.0,
    val email: String? = null,
    val phone: String? = null
)

data class CreditTransactionDto(
    val id: UUID,
    val dispenserAddress: Int,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime?,
    val litres: BigDecimal,
    val amountNok: BigDecimal,
    val pricePerLitreNok: BigDecimal?
)

@RestController
@RequestMapping("/api/v1/credit")
class CreditController(
    private val customerRepository: CustomerRepository,
    private val creditAccountRepository: CreditAccountRepository,
    private val transactionRepository: TransactionRepository
) {

    @GetMapping("/accounts")
    fun getAllAccounts(): ResponseEntity<List<CreditAccountDto>> {
        val accounts = creditAccountRepository.findAllActiveWithCustomer()
            .map { account ->
                CreditAccountDto(
                    id = account.id,
                    customerName = account.customer.name,
                    customerNumber = account.customer.customerNumber,
                    balanceNok = account.balanceNok,
                    creditLimitNok = account.creditLimitNok,
                    availableCreditNok = account.availableCreditNok,
                    lastActivityAt = account.lastActivityAt
                )
            }
        return ResponseEntity.ok(accounts)
    }

    @GetMapping("/accounts/{id}")
    fun getAccount(@PathVariable id: UUID): ResponseEntity<CreditAccountDto> {
        val account = creditAccountRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val dto = CreditAccountDto(
            id = account.id,
            customerName = account.customer.name,
            customerNumber = account.customer.customerNumber,
            balanceNok = account.balanceNok,
            creditLimitNok = account.creditLimitNok,
            availableCreditNok = account.availableCreditNok,
            lastActivityAt = account.lastActivityAt
        )
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/accounts")
    @Transactional
    fun createAccount(@RequestBody request: CreateCreditAccountRequest): ResponseEntity<CreditAccountDto> {
        // Check if customer already exists
        val existingCustomer = customerRepository.findByCustomerNumber(request.customerNumber)
        if (existingCustomer != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // Create customer
        val customer = Customer(
            customerNumber = request.customerNumber,
            name = request.customerName,
            email = request.email,
            phone = request.phone
        )
        customerRepository.save(customer)

        // Create credit account
        val account = CreditAccount(
            customer = customer,
            balanceCents = (request.initialBalanceNok * 100).toInt(),
            creditLimitCents = (request.creditLimitNok * 100).toInt()
        )
        creditAccountRepository.save(account)

        val dto = CreditAccountDto(
            id = account.id,
            customerName = customer.name,
            customerNumber = customer.customerNumber,
            balanceNok = account.balanceNok,
            creditLimitNok = account.creditLimitNok,
            availableCreditNok = account.availableCreditNok,
            lastActivityAt = account.lastActivityAt
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(dto)
    }

    @GetMapping("/accounts/{id}/transactions")
    fun getAccountTransactions(@PathVariable id: UUID): ResponseEntity<List<CreditTransactionDto>> {
        val account = creditAccountRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        val transactions = transactionRepository.findByCustomerIdOrderByTimestampDesc(
            account.customer.id
        ).map { tx ->
            CreditTransactionDto(
                id = tx.transactionId ?: UUID.randomUUID(), // Fallback for new transactions
                dispenserAddress = tx.dispenserAddress,
                startedAt = tx.timestamp,
                finishedAt = tx.timestamp, // Simplified - should track finish time
                litres = tx.volumeLiters,
                amountNok = tx.amountKr,
                pricePerLitreNok = tx.pricePerLiter
            )
        }

        return ResponseEntity.ok(transactions)
    }
}
