package no.cloudberries.lpg.credit

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
    fun findByCustomerNumber(customerNumber: String): Customer?
    fun findByActiveTrue(): List<Customer>
    fun findByNameContainingIgnoreCase(name: String): List<Customer>
}

@Repository
interface CreditAccountRepository : JpaRepository<CreditAccount, UUID> {
    fun findByCustomerId(customerId: UUID): CreditAccount?
    fun findByActiveTrue(): List<CreditAccount>
    
    @Query("""
        SELECT ca FROM CreditAccount ca
        JOIN FETCH ca.customer c
        WHERE ca.active = true
        ORDER BY c.name
    """)
    fun findAllActiveWithCustomer(): List<CreditAccount>
}
