package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * WebApp Polling Service
 *
 * Dette er en enkel polling-service som holder webapp live og
 * sørger for at PumpStateService.pollVolume() kalles regelmessig
 * for å oppdatere WebSocket-klienter med sanntids volum/beløp.
 *
 * Funksjoner:
 * - Kaller PumpStateService.pollVolume() hver 500ms
 * - Sender WebSocket-oppdateringer til kontrollpanelet
 * - Fungerer både i LAB-modus (emulator) og FIELD-modus (real serial)
 *
 * Forskjell fra HeadlessPollingService:
 * - HeadlessPollingService: Kjører bare i headless-appen, håndterer autorisasjoner
 * - WebAppPollingService: Kjører bare i webapp, kun for live UI-oppdateringer
 */
@Service
class WebAppPollingService(
    private val pumpStateService: PumpStateService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var isRunning = false

    /**
     * Scheduled task som poller pump status for live UI-oppdateringer.
     * Kjører automatisk hver 500ms for smidig real-time opplevelse.
     *
     * Denne tasken kaller PumpStateService.pollVolume() som:
     * 1. Poller volum fra pumpe/emulator
     * 2. Oppdaterer intern state
     * 3. Broadcaster status via WebSocket til kontrollpanelet
     */
    @Scheduled(fixedRate = 500, initialDelay = 2000)
    fun pollForUiUpdates() {
        if (!isRunning) {
            logger.info("🚀 WebApp polling service started - UI live updates enabled")
            isRunning = true
        }

        try {
            // Poll for state transitions (READY_TO_PUMP → PUMPING)
            pumpStateService.pollStateForReadyPumps()
            
            // Poll for volume updates (PUMPING state)
            pumpStateService.pollVolume()
        } catch (e: Exception) {
            // Ignorer feil - pollVolume() håndterer exceptions internt
            logger.trace("Poll cycle completed with exception: ${e.message}")
        }
    }
}
