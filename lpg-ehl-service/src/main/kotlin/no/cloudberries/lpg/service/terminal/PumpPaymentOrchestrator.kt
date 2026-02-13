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
 * Flow (station owner):
 * 1. Open terminal
 * 2. Purchase approved
 * 3. Start fueling
 * 4. If pump fails -> reversal on terminal
 */
@Service
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class PumpPaymentOrchestrator(
    private val fuelPumpService: FuelPumpService,
    private val pumpStateService: PumpStateService,
    private val transactionService: TransactionService,
    private val priceService: PriceService,
    private val terminalClient: TerminalClient?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Station owner flow: open terminal then purchase before starting pump.
     */
    fun openTerminalAndPurchase(
        amountMinor: Int,
        pumpId: Int = 1,
        productId: Int = 1,
        optionalData: String? = null,
        clientRequestId: String? = null
    ): TerminalOperationResponse {
        if (terminalClient == null) {
            log.warn("Terminal client not configured; cannot perform purchase")
            return TerminalOperationResponse(success = false, error = "Terminal client not configured")
        }

        log.info("Opening terminal for purchase (pumpId={}, amountMinor={})", pumpId, amountMinor)
        val openResponse = terminalClient.openTerminal()
        if (!openResponse.success) {
            log.error("Terminal open failed: {}", openResponse.error)
            return TerminalOperationResponse(success = false, error = openResponse.error)
        }

        val purchaseResponse = terminalClient.purchase(
            TerminalPurchaseRequest(
                amountMinor = amountMinor,
                optionalData = optionalData,
                clientRequestId = clientRequestId
            )
        )

        if (purchaseResponse.success && !purchaseResponse.operationId.isNullOrBlank()) {
            startPumpingAfterPayment(
                amountCents = amountMinor.toLong(),
                pumpId = pumpId,
                productId = productId,
                operationId = purchaseResponse.operationId
            )
        } else {
            log.error("Purchase failed: {}", purchaseResponse.error ?: purchaseResponse.errorCode)
        }

        return purchaseResponse
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
