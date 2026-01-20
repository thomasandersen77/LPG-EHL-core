package no.cloudberries.lpg.service.pump

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Pump Authorization Entity
 * 
 * Representerer en "kortdragning" / autorisasjon for å frigjøre pumpen.
 * 
 * Flyten:
 * 1. PENDING   - Kort dratt, venter på at pumpen skal frigjøres
 * 2. AUTHORIZED - Pumpen er frigjort (UNBLOCK sendt)
 * 3. PUMPING   - Aktivt pumping pågår
 * 4. COMPLETED - Pumping ferdig, transaksjonen er opprettet
 * 5. CANCELLED - Autorisasjonen ble kansellert (timeout, feil, etc.)
 * 
 * Headless-applikasjonen poller denne tabellen og sender UNBLOCK
 * når den ser en PENDING autorisasjon.
 */
@Entity
@Table(name = "pump_authorization")
data class PumpAuthorization(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val authorizationId: UUID = UUID.randomUUID(),
    
    @Column(nullable = false)
    val dispenserAddress: Int,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: AuthorizationStatus = AuthorizationStatus.PENDING,
    
    /** Maks beløp som kan brukes (reservert beløp) */
    @Column(nullable = false)
    val maxAmountKr: Double = 2000.0,
    
    /** Pris per liter ved autorisasjonstidspunkt */
    @Column(nullable = false)
    val pricePerLiterKr: Double,
    
    /** Bruker/kilde som trigget autorisasjonen */
    @Column(nullable = true)
    val triggeredBy: String? = null,
    
    /** Betalingsmetode (CARD, CREDIT, CASH, SIMULATION) */
    @Column(nullable = false)
    val paymentMethod: String = "SIMULATION",
    
    /** Kortnummer (masket) hvis relevant */
    @Column(nullable = true)
    val cardNumberMasked: String? = null,
    
    /** Referanse til transaksjonen som ble opprettet */
    @Column(nullable = true)
    var transactionId: UUID? = null,
    
    /** Faktisk volum levert (oppdateres underveis og ved stopp) */
    @Column(nullable = false)
    var actualVolumeLiters: Double = 0.0,
    
    /** Faktisk beløp (oppdateres underveis og ved stopp) */
    @Column(nullable = false)
    var actualAmountKr: Double = 0.0,
    
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = true)
    var authorizedAt: LocalDateTime? = null,
    
    @Column(nullable = true)
    var completedAt: LocalDateTime? = null,
    
    @Column(nullable = true)
    var errorMessage: String? = null
)

enum class AuthorizationStatus {
    /** Kort dratt, venter på UNBLOCK fra controller */
    PENDING,
    /** Pumpen er frigjort (UNBLOCK sendt) */
    AUTHORIZED,
    /** Aktivt pumping pågår */
    PUMPING,
    /** Pumping ferdig, venter på betaling */
    STOPPED,
    /** Betaling mottatt, alt OK */
    COMPLETED,
    /** Autorisasjon kansellert eller feilet */
    CANCELLED,
    /** Timeout - autorisasjonen utløp */
    EXPIRED
}
