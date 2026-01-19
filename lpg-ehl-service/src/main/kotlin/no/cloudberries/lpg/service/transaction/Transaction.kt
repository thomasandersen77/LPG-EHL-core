package no.cloudberries.lpg.service.transaction

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "transactions")
class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transaction_id")
    var transactionId: UUID? = null,

    @Column(name = "dispenser_address", nullable = false)
    var dispenserAddress: Int,

    @Column(name = "nozzle_number", nullable = false)
    var nozzleNumber: Int,

    @Column(name = "product_code")
    var productCode: String? = null,

    @Column(name = "volume_deciliters", nullable = false)
    var volumeDeciliters: Int,

    @Column(name = "amount_ore", nullable = false)
    var amountOre: Int,

    @Column(name = "price_per_liter")
    var pricePerLiter: BigDecimal? = null,

    @Column(name = "payment_type")
    var paymentType: String? = null, // CASH, CARD, CREDIT (null = awaiting payment)

    @Column(name = "payment_status")
    var paymentStatus: String = "PENDING", // PENDING, PAID

    @Column(name = "customer_id")
    var customerId: UUID? = null,

    @Column(name = "customer_name")
    var customerName: String? = null,

    @Column(name = "includes_road_tax")
    var includesRoadTax: Boolean = true,

    @Column(name = "road_tax_per_liter_ore")
    var roadTaxPerLiterOre: Int? = null,

    @Column(name = "timestamp", nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now(),

    @Type(JsonBinaryType::class)
    @Column(name = "decoded_data", columnDefinition = "jsonb")
    var decodedData: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    // Convenience properties for API responses
    val volumeLiters: BigDecimal
        get() = BigDecimal(volumeDeciliters).divide(BigDecimal(10))

    val amountKr: BigDecimal
        get() = BigDecimal(amountOre).divide(BigDecimal(100))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transaction) return false
        return transactionId == other.transactionId
    }

    override fun hashCode(): Int = transactionId.hashCode()

    override fun toString(): String {
        return "Transaction(id=$transactionId, dispenser=$dispenserAddress, nozzle=$nozzleNumber, " +
                "volume=${volumeLiters}L, amount=${amountKr}kr, timestamp=$timestamp)"
    }
}
