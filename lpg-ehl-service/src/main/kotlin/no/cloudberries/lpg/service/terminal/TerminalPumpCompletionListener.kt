package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.service.event.PumpStoppedEvent
import no.cloudberries.lpg.service.pump.PumpStateService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

/**
 * Listens for PumpStoppedEvent and performs terminal capture when an active
 * reservation session exists for the pump.
 *
 * Flow:
 * 1. User drew card -> reservation approved -> unblockPumpAfterReservation
 * 2. User fills (PLS Start / webapp)
 * 3. User stops (PLS Stop / webapp block) -> PumpStoppedEvent
 * 4. This listener: capture(operationId, amountMinor), settle, remove session
 */
@Component
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalPumpCompletionListener(
    private val terminalClient: TerminalClient,
    private val sessionStore: TerminalPumpSession,
    private val pumpStateService: PumpStateService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onPumpStopped(event: PumpStoppedEvent) {
        val pumpId = event.pumpId
        val session = sessionStore.get(pumpId) ?: return

        val amountMinor = (event.amountKr * 100).roundToInt()
        log.info(
            "🛑 Pump {} stopped with {} L = {} kr - capturing reservation {}",
            pumpId, event.volumeLitres, event.amountKr, session.operationId
        )

        try {
            val capture = terminalClient.capture(session.operationId, amountMinor)
            if (capture.success) {
                log.info("✅ Terminal capture success: operationId={}", capture.operationId)
                pumpStateService.settle(pumpId, "CARD")
            } else {
                log.error("❌ Terminal capture failed: {} - manual intervention required", capture.error)
            }
        } catch (e: Exception) {
            log.error("💥 Terminal capture exception: {}", e.message, e)
        } finally {
            sessionStore.remove(pumpId)
        }
    }
}
