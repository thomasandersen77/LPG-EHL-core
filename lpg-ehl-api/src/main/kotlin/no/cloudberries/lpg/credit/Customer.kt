package no.cloudberries.lpg.credit

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "customers")
class Customer(
    @Id
    @Column(name = "id")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "customer_number", unique = true, nullable = false, length = 50)
    var customerNumber: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "phone", length = 50)
    var phone: String? = null,

    @Column(name = "address_line1")
    var addressLine1: String? = null,

    @Column(name = "address_line2")
    var addressLine2: String? = null,

    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null,

    @Column(name = "city", length = 100)
    var city: String? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Customer) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Customer(id=$id, customerNumber='$customerNumber', name='$name')"
    }
}
