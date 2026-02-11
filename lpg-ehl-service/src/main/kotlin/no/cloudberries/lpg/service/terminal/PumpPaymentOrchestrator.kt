package no.cloudberries.lpg.service.terminal

import no.cloudberries.lpg.service.price.PriceService
import no.cloudberries.lpg.service.pump.FuelPumpService
import no.cloudberries.lpg.service.pump.PumpStateService
import no.cloudberries.lpg.service.pump.StartFuelingResult
import no.cloudberries.lpg.service.transaction.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Orkestrerer koblingen mellom betaling og pumping.
 * 
 * Flow (reservation):
 * 1. Terminal reservation godkjent -> unblock pump (UNBLOCK via PumpStateService)
 * 2. Bruker fyller (PLS knapp eller webapp)
 * 3. Pump stoppet -> terminal capture + settle
 * 
 * Flow (full purchase - legacy):
 * 1. Terminal purchase godkjent -> FuelPumpService.startFueling
 * 4. Hvis feil -> reverser betaling på terminal
 */
@Service
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class PumpPaymentOrchestrator(
    private val fuelPumpService: FuelPumpService,
    private val pumpStateService: PumpStateService,
    private val transactionService: TransactionService,
    private val priceService: PriceService,
    private val terminalClient: TerminalClient?,
    private val sessionStore: TerminalPumpSession
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Unblock pump after reservation approval.
     * Sends UNBLOCK to pump (PLS/emulator). User can then start filling via PLS GUI or webapp.
     */
    fun unblockPumpAfterReservation(operationId: String, amountMinor: Int, pumpId: Int = 1) {
        log.info("⛽ Unblocking pump {} after reservation (operationId={}, reserved={} kr)", pumpId, operationId, amountMinor / 100.0)

        sessionStore.put(pumpId, operationId, amountMinor)

        val result = pumpStateService.unblock(pumpId)
        result.fold(
            onSuccess = {
                log.info("✅ Pump {} unblocked - user can start filling (PLS Start or webapp)", pumpId)
            },
            onFailure = { e ->
                log.error("❌ Failed to unblock pump {}: {}", pumpId, e.message)
                sessionStore.remove(pumpId)
                terminalClient?.reversal(operationId)
            }
        )
    }

    /**
     * Start pumping sequence etter at betaling er godkjent.
     * 
     * @param amountCents Beløp i øre (ikke brukt for volume, kun for logging)
     * @param pumpId Dispenser address (1-255)
     * @param productId Produkt/grade (1=Regular, 2=Premium, etc.)
     * @param operationId Terminal operation ID for logging/tracking
     */
    fun startPumpingAfterPayment(
        amountCents: Long,
        pumpId: Int,
        productId: Int = 1,
        operationId: String
    ) {
        log.info("⛽ Starting pumping for pump {} after approved payment (operationId={}, amount={} kr)", 
            pumpId, operationId, amountCents / 100.0)

        try {
            // Hent gjeldende pris
            val priceHistory = priceService.getCurrentPrice()
            val pricePerLitre = priceHistory?.pricePerLiter?.toDouble() ?: 15.90
            
            log.debug("Current price: {} kr/L", pricePerLitre)

            // Opprett transaksjon
            val transaction = transactionService.createStartedTransaction(
                dispenserAddress = pumpId,
                pricePerLiterKr = pricePerLitre
            )
            
            log.info("📝 Created transaction: ID={}, dispenser={}, price={} kr/L", 
                transaction.transactionId, pumpId, pricePerLitre)

            // Start fueling via EHL-protokoll
            val result = fuelPumpService.startFueling(
                pumpId = pumpId,
                productId = productId,
                pricePerLitre = pricePerLitre
            )

            when (result) {
                is StartFuelingResult.Success -> {
                    log.info("✅ Pump {} authorized for fueling. Transaction: {}", pumpId, transaction.transactionId)
                    log.info("🟢 Customer can now lift nozzle and start fueling")
                }
                is StartFuelingResult.PumpNotIdle -> {
                    log.error("❌ Pump {} is not idle (current status: {})", pumpId, result.currentStatus)
                    reversalOnError(operationId, "Pump not idle: ${result.currentStatus}")
                }
                is StartFuelingResult.NoResponse -> {
                    log.error("❌ No response from pump {}", pumpId)
                    reversalOnError(operationId, "No response from pump")
                }
                is StartFuelingResult.StateTransitionFailed -> {
                    log.error("❌ Pump {} state transition failed (current: {})", pumpId, result.currentStatus)
                    reversalOnError(operationId, "State transition failed: ${result.currentStatus}")
                }
            }

        } catch (e: Exception) {
            log.error("💥 Fatal error during pump start after payment", e)
            reversalOnError(operationId, "Exception: ${e.message}")
            throw e
        }
    }

    /**
     * Reverser betaling på terminal ved feil.
     */
    private fun reversalOnError(operationId: String, reason: String) {
        log.warn("🔄 Reversing payment on terminal (OperationId={}, Reason={})", operationId, reason)

        try {
            val reversalResponse = terminalClient?.reversal(operationId)

            if (reversalResponse?.success == true) {
                log.info("✅ Reversal successful: {}", reversalResponse.operationId)
            } else {
                log.error(
                    "❌ Reversal failed! Manual intervention required. OperationId={}, Error={}",
                    operationId,
                    reversalResponse?.error
                )
            }
        } catch (e: Exception) {
            log.error("💥 Reversal call failed! Manual intervention required.", e)
        }
    }
}
