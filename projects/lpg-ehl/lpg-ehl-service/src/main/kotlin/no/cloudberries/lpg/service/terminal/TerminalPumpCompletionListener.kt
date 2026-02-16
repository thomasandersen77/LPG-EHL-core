package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.service.event.PumpStoppedEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Listens for PumpStoppedEvent. In open->purchase flow, payment is already
 * finalized before fueling, so no capture step is needed here.
 */
@Component
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalPumpCompletionListener(
    private val terminalClient: TerminalClient?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onPumpStopped(event: PumpStoppedEvent) {
        if (terminalClient == null) {
            log.debug("Terminal client disabled; ignoring pump stop event for pump {}", event.pumpId)
            return
        }

        log.info(
            "🛑 Pump {} stopped with {} L = {} kr - no terminal capture required in open->purchase flow",
            event.pumpId, event.volumeLitres, event.amountKr
        )
    }
}
