package no.cloudberries.lpg.api.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "road_tax_settings")
class RoadTaxSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    var id: UUID? = null,

    @Column(name = "tax_per_liter_ore", nullable = false)
    var taxPerLiterOre: Int,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDateTime = LocalDateTime.now(),

    @Column(name = "effective_until")
    var effectiveUntil: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by")
    var createdBy: String? = null,

    @Column(name = "description")
    var description: String? = null
) {
    override fun toString(): String {
        return "RoadTaxSettings(id=$id, tax=${taxPerLiterOre} øre/L, effectiveFrom=$effectiveFrom, description=$description)"
    }
}
