package no.cloudberries.lpg.service.credit

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "credit_accounts")
class CreditAccount(
    @Id
    @Column(name = "id")
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer,

    @Column(name = "balance_cents", nullable = false)
    var balanceCents: Int = 0,

    @Column(name = "credit_limit_cents", nullable = false)
    var creditLimitCents: Int = 0,

    @Column(name = "last_activity_at")
    var lastActivityAt: LocalDateTime? = null,

    @Column(name = "last_transaction_id")
    var lastTransactionId: UUID? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Convenience properties for API responses
    val balanceNok: BigDecimal
        get() = BigDecimal(balanceCents).divide(BigDecimal(100))

    val creditLimitNok: BigDecimal
        get() = BigDecimal(creditLimitCents).divide(BigDecimal(100))

    val availableCreditNok: BigDecimal
        get() = creditLimitNok.subtract(balanceNok)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreditAccount) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "CreditAccount(id=$id, balance=${balanceNok}kr, limit=${creditLimitNok}kr)"
    }
}
