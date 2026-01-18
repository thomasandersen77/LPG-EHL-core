package no.cloudberries.lpg.service.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "price_history")
class PriceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    var id: UUID? = null,

    @Column(name = "product_code", nullable = false)
    var productCode: String,

    @Column(name = "product_name", nullable = false)
    var productName: String,

    @Column(name = "price_per_liter", nullable = false)
    var pricePerLiter: BigDecimal,

    @Column(name = "vat_rate", nullable = false)
    var vatRate: BigDecimal,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDateTime = LocalDateTime.now(),

    @Column(name = "effective_until")
    var effectiveUntil: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: String? = null
) {
    override fun toString(): String {
        return "PriceHistory(id=$id, product=$productCode, price=$pricePerLiter kr/L, effectiveFrom=$effectiveFrom)"
    }
}
