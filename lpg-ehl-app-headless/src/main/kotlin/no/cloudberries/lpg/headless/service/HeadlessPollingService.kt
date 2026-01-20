package no.cloudberries.lpg.headless.service

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.service.pump.AuthorizationStatus
import no.cloudberries.lpg.service.pump.DispenserService
import no.cloudberries.lpg.service.pump.PumpAuthorizationService
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/**
 * Headless Polling Service
 * 
 * Denne servicen holder headless-applikasjonen i live ved å kontinuerlig
 * polle dispenser status og prosessere autorisasjoner fra databasen.
 * Den kjører automatisk som en @Scheduled task.
 * 
 * Funksjoner:
 * - Poller dispenser STATE og VOLUME hver 2. sekund
 * - Sjekker for PENDING autorisasjoner ("kortdragninger") fra databasen
 * - Sender UNBLOCK og oppdaterer status til AUTHORIZED når kortdragning registreres
 * - Logger regelmessig aktivitet (hver 10. poll)
 * - Holder Spring Boot-applikasjonen kjørende som daemon
 * 
 * Flyt ved kortdragning:
 * 1. Webapp/ekstern klient setter inn PENDING autorisasjon i DB
 * 2. HeadlessPollingService oppdager PENDING autorisasjon
 * 3. Sender UNBLOCK til pumpe via EhlCommunicator
 * 4. Oppdaterer autorisasjon til AUTHORIZED
 * 5. Fortsetter å polle STATE/VOLUME under pumping
 */
@Service
class HeadlessPollingService(
    private val ehlCommunicator: EhlCommunicator,
    private val dispenserService: DispenserService,
    private val authorizationService: PumpAuthorizationService,
    private val pumpStateService: PumpStateService,
    @Value("\${lpg.dispenser.address:1}") private val dispenserAddress: Int,
    @Value("\${lpg.polling.interval-ms:2000}") private val pollingIntervalMs: Long
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    private val pollCount = AtomicLong(0)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    private var isRunning = false
    
    /**
     * Scheduled task som poller dispenser status.
     * Kjører automatisk hver ${pollingIntervalMs} millisekund (default 2000ms = 2 sekunder).
     * 
     * Denne tasken holder applikasjonen i live - så lenge den kjører,
     * vil Spring Boot-prosessen fortsette.
     */
    @Scheduled(fixedDelayString = "\${lpg.polling.interval-ms:2000}", initialDelay = 5000)
    fun pollDispenserStatus() {
        if (!isRunning) {
            logger.info("🚀 Starting dispenser polling loop...")
            isRunning = true
        }
        
        val count = pollCount.incrementAndGet()
        
        // Sjekk for PENDING autorisasjoner først
        processPendingAuthorizations()
        
        try {
            // 1. Poll STATE
            val statePacket = EhlPacket(
                address = dispenserAddress,
                command = EhlCommand.STATE,
                data = byteArrayOf()
            )
            
            val stateResponse = runBlocking {
                ehlCommunicator.sendAndReceive(statePacket, 1000)
            }
            
            // Send til DispenserService for state machine processing
            dispenserService.handlePacket(stateResponse)
            
            // 2. Poll VOLUME
            val volumePacket = EhlPacket(
                address = dispenserAddress,
                command = EhlCommand.VOLUME,
                data = byteArrayOf()
            )
            
            val volumeResponse = runBlocking {
                ehlCommunicator.sendAndReceive(volumePacket, 1000)
            }
            
            // Send til DispenserService for state machine processing
            dispenserService.handlePacket(volumeResponse)
            
            // Oppdater volum i aktive autorisasjoner
            updateActiveAuthorizationVolume()
            
            // Logg hver 10. poll for å vise at det fungerer uten å spamme
            if (count % 10 == 0L) {
                val time = LocalDateTime.now().format(timeFormatter)
                val dispenserState = dispenserService.getDispenserState(dispenserAddress)
                logger.info("📊 [$time] Polling #$count - Dispenser $dispenserAddress state: $dispenserState")
            }
            
        } catch (e: Exception) {
            // Logg feil, men fortsett polling (ikke crash appen)
            logger.error("❌ Polling error (poll #$count): ${e.message}")
            
            // Logg full stacktrace hvert 30. forsøk for debugging
            if (count % 30 == 0L) {
                logger.error("Full error details:", e)
            }
        }
    }
    
    /**
     * Heartbeat task som logger hver 30. sekund for å bekrefte at appen lever.
     * Dette er nyttig for overvåking og debugging.
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    fun heartbeat() {
        val time = LocalDateTime.now().format(timeFormatter)
        logger.info("💓 [$time] Headless app is alive - Poll count: ${pollCount.get()}")
    }
    
    /**
     * Sjekk for PENDING autorisasjoner i databasen og prosesser dem.
     * 
     * Denne funksjonen kjøres som del av hver polling-syklus.
     * Når en PENDING autorisasjon finnes:
     * 1. Sender UNBLOCK til dispenser
     * 2. Oppdaterer status til AUTHORIZED
     * 3. Pumping kan starte
     */
    private fun processPendingAuthorizations() {
        try {
            val pendingAuths = authorizationService.findPendingAuthorizations()
            
            for (auth in pendingAuths) {
                if (auth.dispenserAddress != dispenserAddress) {
                    // Denne headless-instansen håndterer ikke denne dispenseren
                    continue
                }
                
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                logger.info("💳 PENDING autorisasjon funnet: ${auth.authorizationId}")
                logger.info("   Dispenser: ${auth.dispenserAddress}")
                logger.info("   Maks beløp: ${auth.maxAmountKr} kr")
                logger.info("   Trigget av: ${auth.triggeredBy}")
                logger.info("   Prosesserer...")
                
                // Send UNBLOCK til pumpe via PumpStateService
                val unblockResult = pumpStateService.unblock(auth.dispenserAddress)
                
                if (unblockResult.isSuccess) {
                    // Oppdater autorisasjon til AUTHORIZED
                    authorizationService.markAuthorized(auth.authorizationId)
                    
                    // Oppdater til PUMPING siden UNBLOCK var vellykket
                    val pumpStatus = unblockResult.getOrNull()
                    if (pumpStatus != null && pumpStatus.state == "PUMPING") {
                        authorizationService.markPumping(auth.authorizationId)
                    }
                    
                    logger.info("✅ UNBLOCK vellykket - Pumpe ${auth.dispenserAddress} frigjort")
                    logger.info("   Auth status: PENDING → AUTHORIZED → PUMPING")
                } else {
                    // UNBLOCK feilet - kanseller autorisasjon
                    val error = unblockResult.exceptionOrNull()?.message ?: "Ukjent feil"
                    authorizationService.cancel(auth.authorizationId, "UNBLOCK feilet: $error")
                    
                    logger.error("❌ UNBLOCK feilet: $error")
                    logger.info("   Auth status: CANCELLED")
                }
                
                logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        } catch (e: Exception) {
            // Ikke logg feil for hver poll - bare periodisk
            if (pollCount.get() % 30 == 0L) {
                logger.warn("⚠️ Kunne ikke sjekke autorisasjoner: ${e.message}")
            }
        }
    }
    
    /**
     * Overvåk aktive autorisasjoner og oppdater volum/beløp.
     * Kjøres som del av polling for å holde autorisasjonen oppdatert.
     */
    private fun updateActiveAuthorizationVolume() {
        try {
            val activeAuth = authorizationService.findActiveAuthorization(dispenserAddress)
            if (activeAuth != null && activeAuth.status == AuthorizationStatus.PUMPING) {
                val pumpStatus = pumpStateService.getStatus(dispenserAddress)
                
                // Oppdater volum i autorisasjonen
                if (pumpStatus.volumeLitres > 0) {
                    authorizationService.updateVolume(
                        activeAuth.authorizationId,
                        pumpStatus.volumeLitres,
                        pumpStatus.amountKr
                    )
                }
                
                // Sjekk om pumping har stoppet
                if (pumpStatus.state == "STOPPED" || pumpStatus.state == "IDLE") {
                    authorizationService.markStopped(
                        activeAuth.authorizationId,
                        pumpStatus.volumeLitres,
                        pumpStatus.amountKr
                    )
                }
            }
        } catch (e: Exception) {
            // Ignorer feil her - ikke kritisk
        }
    }
}
