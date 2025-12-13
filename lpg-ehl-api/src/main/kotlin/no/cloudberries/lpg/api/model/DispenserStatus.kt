package no.cloudberries.lpg.api.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "dispenser_status")
class DispenserStatus(
    @Id
    @Column(name = "dispenser_address")
    var dispenserAddress: Int,

    @Column(name = "last_transaction_id")
    var lastTransactionId: java.util.UUID? = null,

    @Column(name = "total_transactions", nullable = false)
    var totalTransactions: Int = 0,

    @Column(name = "total_volume_deciliters", nullable = false)
    var totalVolumeDeciliters: Long = 0L,

    @Column(name = "total_amount_ore", nullable = false)
    var totalAmountOre: Long = 0L,

    @Column(name = "last_seen", nullable = false)
    var lastSeen: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Convenience properties for API responses
    val totalVolumeLiters: BigDecimal
        get() = BigDecimal(totalVolumeDeciliters).divide(BigDecimal(10))

    val totalAmountKr: BigDecimal
        get() = BigDecimal(totalAmountOre).divide(BigDecimal(100))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DispenserStatus) return false
        return dispenserAddress == other.dispenserAddress
    }

    override fun hashCode(): Int = dispenserAddress.hashCode()

    override fun toString(): String {
        return "DispenserStatus(address=$dispenserAddress, transactions=$totalTransactions, " +
                "volume=${totalVolumeLiters}L, amount=${totalAmountKr}kr, lastSeen=$lastSeen)"
    }
}
