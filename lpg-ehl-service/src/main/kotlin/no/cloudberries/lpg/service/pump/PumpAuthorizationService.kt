package no.cloudberries.lpg.service.pump

import no.cloudberries.lpg.service.price.PriceService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service for å håndtere pumpe-autorisasjoner ("kortdragning").
 * 
 * Denne servicen er broen mellom "betaling" og "pumping":
 * 
 * 1. simulateCardSwipe() - Kalles når kort dras (eller simuleres i lab/test)
 *    → Oppretter PENDING autorisasjon i database
 * 
 * 2. processPendingAuthorization() - Kalles av HeadlessPollingService
 *    → Sender UNBLOCK og oppdaterer til AUTHORIZED
 * 
 * 3. updatePumpingStatus() - Kalles under pumping
 *    → Oppdaterer volum/beløp
 * 
 * 4. stopPumping() - Kalles når pumping stopper
 *    → Oppdaterer til STOPPED, venter på betaling
 * 
 * 5. confirmPayment() - Kalles når betaling er bekreftet
 *    → Oppdaterer til COMPLETED
 */
@Service
@Transactional
class PumpAuthorizationService(
    private val authorizationRepository: PumpAuthorizationRepository,
    private val priceService: PriceService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // Aktive statuser som blokkerer ny autorisasjon
    private val activeStatuses = listOf(
        AuthorizationStatus.PENDING,
        AuthorizationStatus.AUTHORIZED,
        AuthorizationStatus.PUMPING,
        AuthorizationStatus.STOPPED
    )
    
    /**
     * Simuler kortdragning - oppretter en PENDING autorisasjon.
     * 
     * Kalles fra:
     * - Webapp GUI ("Simuler kortdragning" knapp)
     * - CLI (for testing)
     * - Eventuelt fra Nets-integrasjon i fremtiden
     * 
     * @param dispenserAddress Pumpe-adresse (default 1)
     * @param maxAmountKr Maks beløp å reservere (default 2000 kr)
     * @param triggeredBy Hvem/hva som trigget (for logging)
     * @param paymentMethod Betalingsmetode (CARD, CREDIT)
     */
    fun simulateCardSwipe(
        dispenserAddress: Int = 1,
        maxAmountKr: Double = 2000.0,
        triggeredBy: String = "WEBAPP_GUI",
        paymentMethod: String = "CARD",
        cardNumberMasked: String? = null
    ): PumpAuthorization {
        // Sjekk om det allerede finnes en aktiv autorisasjon
        val existing = authorizationRepository.findFirstByDispenserAddressAndStatusInOrderByCreatedAtDesc(
            dispenserAddress, activeStatuses
        )
        if (existing != null) {
            logger.warn("⚠️ Dispenser $dispenserAddress har allerede aktiv autorisasjon: ${existing.authorizationId} (${existing.status})")
            throw IllegalStateException("Dispenser $dispenserAddress har allerede en aktiv autorisasjon")
        }
        
        // Hent gjeldende pris
        val currentPrice = priceService.getCurrentPrice("LPG")?.pricePerLiter?.toDouble() ?: 15.90
        
        val authorization = PumpAuthorization(
            dispenserAddress = dispenserAddress,
            status = AuthorizationStatus.PENDING,
            maxAmountKr = maxAmountKr,
            pricePerLiterKr = currentPrice,
            triggeredBy = triggeredBy,
            paymentMethod = paymentMethod,
            cardNumberMasked = cardNumberMasked
        )
        
        val saved = authorizationRepository.save(authorization)
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("💳 KORTDRAGNING SIMULERT")
        logger.info("   Dispenser: $dispenserAddress")
        logger.info("   Auth ID: ${saved.authorizationId}")
        logger.info("   Maks beløp: $maxAmountKr kr")
        logger.info("   Pris: $currentPrice kr/L")
        logger.info("   Metode: $paymentMethod")
        logger.info("   Status: PENDING → Venter på UNBLOCK")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return saved
    }
    
    /**
     * Finn alle PENDING autorisasjoner (for headless polling).
     */
    @Transactional(readOnly = true)
    fun findPendingAuthorizations(): List<PumpAuthorization> {
        return authorizationRepository.findByStatus(AuthorizationStatus.PENDING)
    }
    
    /**
     * Finn aktiv autorisasjon for en dispenser.
     */
    @Transactional(readOnly = true)
    fun findActiveAuthorization(dispenserAddress: Int): PumpAuthorization? {
        return authorizationRepository.findFirstByDispenserAddressAndStatusInOrderByCreatedAtDesc(
            dispenserAddress, activeStatuses
        )
    }
    
    /**
     * Marker autorisasjon som AUTHORIZED (UNBLOCK er sendt).
     * Kalles av headless etter vellykket UNBLOCK.
     */
    fun markAuthorized(authorizationId: UUID): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        auth.status = AuthorizationStatus.AUTHORIZED
        auth.authorizedAt = LocalDateTime.now()
        
        val saved = authorizationRepository.save(auth)
        logger.info("✅ Autorisasjon $authorizationId markert som AUTHORIZED")
        
        return saved
    }
    
    /**
     * Marker autorisasjon som PUMPING (pumping har startet).
     */
    fun markPumping(authorizationId: UUID, transactionId: UUID? = null): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        auth.status = AuthorizationStatus.PUMPING
        if (transactionId != null) {
            auth.transactionId = transactionId
        }
        
        val saved = authorizationRepository.save(auth)
        logger.info("⛽ Autorisasjon $authorizationId markert som PUMPING")
        
        return saved
    }
    
    /**
     * Oppdater volum og beløp underveis i pumping.
     */
    fun updateVolume(authorizationId: UUID, volumeLiters: Double, amountKr: Double): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        auth.actualVolumeLiters = volumeLiters
        auth.actualAmountKr = amountKr
        
        return authorizationRepository.save(auth)
    }
    
    /**
     * Marker pumping som stoppet - venter på betaling.
     */
    fun markStopped(
        authorizationId: UUID, 
        finalVolumeLiters: Double, 
        finalAmountKr: Double
    ): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        auth.status = AuthorizationStatus.STOPPED
        auth.actualVolumeLiters = finalVolumeLiters
        auth.actualAmountKr = finalAmountKr
        
        val saved = authorizationRepository.save(auth)
        
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("🛑 PUMPING STOPPET")
        logger.info("   Auth ID: $authorizationId")
        logger.info("   Volum: $finalVolumeLiters L")
        logger.info("   Beløp: $finalAmountKr kr")
        logger.info("   Status: STOPPED → Venter på betaling")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return saved
    }
    
    /**
     * Bekreft betaling - marker som COMPLETED.
     * Kalles fra GUI ("Simuler betaling" knapp) eller fra Nets-callback.
     */
    fun confirmPayment(authorizationId: UUID, paymentMethod: String = "SIMULATION"): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        if (auth.status != AuthorizationStatus.STOPPED) {
            throw IllegalStateException("Kan bare bekrefte betaling for STOPPED autorisasjoner (nåværende: ${auth.status})")
        }

        return completeAuthorization(auth, paymentMethod, "CONFIRM_PAYMENT")
    }

    /**
     * Fullfør STOPPED-autorisasjon ved betaling via transaksjon.
     */
    fun completeStoppedAuthorizationByTransaction(
        transactionId: UUID,
        paymentMethod: String = "SIMULATION",
        source: String = "TRANSACTION_PAYMENT"
    ): PumpAuthorization? {
        val auth = authorizationRepository.findFirstByTransactionId(transactionId) ?: return null
        return completeStoppedAuthorization(auth, paymentMethod, source)
    }

    /**
     * Fullfør STOPPED-autorisasjon ved å slå opp siste stoppede autorisasjon for dispenser.
     */
    fun completeStoppedAuthorizationByDispenser(
        dispenserAddress: Int,
        paymentMethod: String = "SIMULATION",
        source: String = "TRANSACTION_PAYMENT"
    ): PumpAuthorization? {
        val auth = authorizationRepository.findFirstByDispenserAddressAndStatusOrderByCreatedAtDesc(
            dispenserAddress,
            AuthorizationStatus.STOPPED
        ) ?: return null

        return completeStoppedAuthorization(auth, paymentMethod, source)
    }

    /**
     * Fullfør alle STOPPED-autorisasjoner (brukes ved reset/cleanup).
     */
    fun completeStoppedAuthorizations(reason: String = "ADMIN_RESET"): Int {
        val stoppedAuths = authorizationRepository.findByStatus(AuthorizationStatus.STOPPED)

        if (stoppedAuths.isEmpty()) {
            logger.info("ℹ️ No STOPPED authorizations found to complete")
            return 0
        }

        logger.warn("🧾 Fullfører ${stoppedAuths.size} STOPPED autorisasjon(er) - årsak: $reason")

        stoppedAuths.forEach { auth ->
            auth.status = AuthorizationStatus.COMPLETED
            auth.completedAt = LocalDateTime.now()
            authorizationRepository.save(auth)
            logger.info("   ✅ Fullført: ${auth.authorizationId}")
        }

        return stoppedAuths.size
    }

    private fun completeStoppedAuthorization(
        auth: PumpAuthorization,
        paymentMethod: String,
        source: String
    ): PumpAuthorization? {
        if (auth.status != AuthorizationStatus.STOPPED) {
            logger.warn("⚠️ Kan ikke fullføre autorisasjon ${auth.authorizationId} med status ${auth.status} (kilde: $source)")
            return null
        }

        return completeAuthorization(auth, paymentMethod, source)
    }

    private fun completeAuthorization(
        auth: PumpAuthorization,
        paymentMethod: String,
        source: String
    ): PumpAuthorization {
        auth.status = AuthorizationStatus.COMPLETED
        auth.completedAt = LocalDateTime.now()

        val saved = authorizationRepository.save(auth)

        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("💳 BETALING BEKREFTET")
        logger.info("   Auth ID: ${auth.authorizationId}")
        logger.info("   Volum: ${auth.actualVolumeLiters} L")
        logger.info("   Beløp: ${auth.actualAmountKr} kr")
        logger.info("   Metode: $paymentMethod")
        logger.info("   Kilde: $source")
        logger.info("   Status: COMPLETED ✅")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return saved
    }
    
    /**
     * Kanseller autorisasjon.
     */
    fun cancel(authorizationId: UUID, reason: String = "Manuell kansellering"): PumpAuthorization {
        val auth = authorizationRepository.findById(authorizationId)
            .orElseThrow { IllegalArgumentException("Autorisasjon ikke funnet: $authorizationId") }
        
        auth.status = AuthorizationStatus.CANCELLED
        auth.errorMessage = reason
        auth.completedAt = LocalDateTime.now()
        
        val saved = authorizationRepository.save(auth)
        logger.info("❌ Autorisasjon $authorizationId kansellert: $reason")
        
        return saved
    }
    
    /**
     * Hent status for en autorisasjon.
     */
    @Transactional(readOnly = true)
    fun getStatus(authorizationId: UUID): PumpAuthorization? {
        return authorizationRepository.findById(authorizationId).orElse(null)
    }
    
    /**
     * Hent siste autorisasjon for en dispenser.
     */
    @Transactional(readOnly = true)
    fun getLatestAuthorization(dispenserAddress: Int): PumpAuthorization? {
        return authorizationRepository.findFirstByDispenserAddressOrderByCreatedAtDesc(dispenserAddress)
    }
    
    /**
     * Kanseller ALLE stuck autorisasjoner.
     * 
     * Brukes for cleanup når autorisasjoner har hengt seg fra tidligere kjøringer.
     * VIKTIG: Denne metoden er ment for admin/debugging - bruk med forsiktighet!
     * 
     * @return Antall autorisasjoner som ble kansellert
     */
    fun cancelAllStuckAuthorizations(): Int {
        val stuckStatuses = listOf(
            AuthorizationStatus.PENDING,
            AuthorizationStatus.AUTHORIZED,
            AuthorizationStatus.PUMPING
        )
        
        val stuckAuths = authorizationRepository.findAll()
            .filter { it.status in stuckStatuses }
        
        if (stuckAuths.isEmpty()) {
            logger.info("ℹ️ No stuck authorizations found")
            return 0
        }
        
        logger.warn("🧹 Cancelling ${stuckAuths.size} stuck authorization(s)...")
        
        stuckAuths.forEach { auth ->
            auth.status = AuthorizationStatus.CANCELLED
            auth.errorMessage = "Admin cleanup - stuck from previous session"
            auth.completedAt = LocalDateTime.now()
            authorizationRepository.save(auth)
            
            logger.info("   ❌ Cancelled: ${auth.authorizationId} (was ${auth.status})")
        }
        
        return stuckAuths.size
    }
}
