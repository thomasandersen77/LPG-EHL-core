package no.cloudberries.lpg.service.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "dispenser_status")
class DispenserStatus(
    @Id
    @Column(name = "address")
    var address: Int,

    @Column(name = "state", nullable = false)
    var state: String,

    @Column(name = "last_active", nullable = false)
    var lastActive: LocalDateTime = LocalDateTime.now(),

    @Column(name = "current_transaction_id")
    var currentTransactionId: java.util.UUID? = null,

    @Column(name = "error_code")
    var errorCode: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DispenserStatus) return false
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()

    override fun toString(): String {
        return "DispenserStatus(address=$address, state=$state, lastActive=$lastActive)"
    }
}
