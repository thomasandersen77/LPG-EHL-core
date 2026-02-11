package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.service.terminal.dto.EventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Håndterer terminal events og trigge business logic.
 * 
 * Flow:
 * - Reservation godkjent -> frigjør pumpe (UNBLOCK)
 * - Pump stoppet -> terminal capture med faktisk beløp
 */
@Service
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalEventHandler(
    private val orchestrator: PumpPaymentOrchestrator?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleOperationStarted(event: EventEnvelope) {
        val payload = event.payload
        val operationType = payload["OperationType"] as? String
        val amountMinor = (payload["AmountMinor"] as? Number)?.toLong()
        log.info("🟡 Terminal operation started: OperationId={}, Type={}, Amount={} kr",
            event.operationId, operationType, (amountMinor ?: 0) / 100.0)
    }

    fun handleOperationCompleted(event: EventEnvelope) {
        val payload = event.payload
        val success = (payload["Success"] as? Boolean)
            ?: (payload["success"] as? Boolean)
            ?: false
        val operationType = payload["OperationType"] as? String ?: "purchase"
        val amountMinor = (payload["AmountMinor"] as? Number)?.toInt() ?: 0

        log.info("🟢 Terminal operation completed: OperationId={}, Success={}, Type={}, Amount={} kr",
            event.operationId, success, operationType, amountMinor / 100)

        if (success && operationType == "reservation") {
            log.info("✅ Reservation approved - freeing pump for filling")
            try {
                orchestrator?.unblockPumpAfterReservation(
                    operationId = event.operationId ?: "unknown",
                    amountMinor = amountMinor,
                    pumpId = 1
                )
            } catch (e: Exception) {
                log.error("Failed to unblock pump after reservation", e)
            }
        } else if (success && (operationType == "purchase" || operationType == null)) {
            // Legacy: full purchase approval
            log.info("✅ Purchase approved - triggering pump authorization")
            try {
                orchestrator?.startPumpingAfterPayment(
                    amountCents = amountMinor.toLong(),
                    pumpId = 1,
                    productId = 1,
                    operationId = event.operationId ?: "unknown"
                )
            } catch (e: Exception) {
                log.error("Failed to start pumping after payment approval", e)
            }
        } else if (!success) {
            val rejectionReason = payload["RejectionReason"] as? String
            log.warn("❌ Payment declined: Reason={}", rejectionReason)
        }
    }

    fun handleOperationTimeout(event: EventEnvelope) {
        log.warn("⏱️ Terminal operation timeout: OperationId={}", event.operationId)
    }
}
