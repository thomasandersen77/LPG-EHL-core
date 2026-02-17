package no.cloudberries.lpg.service.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.cloudberries.lpg.service.price.PriceService
import no.cloudberries.lpg.service.pump.FuelPumpService
import no.cloudberries.lpg.service.pump.PumpStateService
import no.cloudberries.lpg.service.pump.StartFuelingResult
import no.cloudberries.lpg.service.transaction.TransactionService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Orkestrerer koblingen mellom betaling og pumping.
 *
 * Støtter to flyter:
 * A) Kortstyrt flyt (CARD_EVENT): Kunde tapper kort → reserve → frigjør pumpe → fylling → capture
 * B) Manuell flyt (MANUAL_RELEASE): Stasjonseier frigjør pumpe → fylling → manuell betaling
 */
@Service
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class PumpPaymentOrchestrator(
    private val fuelPumpService: FuelPumpService,
    private val pumpStateService: PumpStateService,
    private val transactionService: TransactionService,
    private val priceService: PriceService,
    private val paymentTerminalClient: PaymentTerminalClient,
    private val terminalClient: TerminalClient? = null  // Legacy support
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val orchestratorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeAuthorizations = ConcurrentHashMap<Int, PaymentAuthorization>()  // dispenserAddress -> auth

    companion object {
        private const val DEFAULT_RESERVE_AMOUNT_MINOR = 150_000  // 1500 NOK
        private const val DISPENSER_ADDRESS = 33  // TODO: from config
    }

    @PostConstruct
    fun init() {
        log.info("🚀 PumpPaymentOrchestrator initializing - subscribing to terminal events")

        orchestratorScope.launch {
            try {
                paymentTerminalClient.open()
                log.info("✅ Payment terminal opened successfully")
            } catch (e: Exception) {
                log.error("❌ Failed to open payment terminal on startup", e)
            }
        }

        // Subscribe to card events
        paymentTerminalClient.terminalEvents()
            .onEach { event ->
                when (event) {
                    is TerminalEvent.CardPresented -> handleCardPresented(event)
                    is TerminalEvent.TerminalReady -> log.info("✅ Terminal ready: ${event.terminalId}")
                    is TerminalEvent.Error -> log.error("❌ Terminal error: ${event.message}")
                    is TerminalEvent.TransactionResult -> log.info("💳 Transaction result: approved=${event.approved}, amount=${event.amountMinor / 100.0} kr")
                    is TerminalEvent.InteractivePrompt -> log.info("💬 Interactive prompt: ${event.message}")
                }
            }
            .launchIn(orchestratorScope)
    }

    @PreDestroy
    fun cleanup() {
        log.info("🛑 PumpPaymentOrchestrator shutting down")
        runBlocking {
            try {
                paymentTerminalClient.close()
            } catch (e: Exception) {
                log.warn("Error closing terminal on shutdown", e)
            }
        }
        orchestratorScope.cancel()
    }

    /**
     * Del 2A: Kortstyrt flyt - håndter kort-tap event
     */
    private suspend fun handleCardPresented(event: TerminalEvent.CardPresented) {
        val correlationId = UUID.randomUUID().toString()
        log.info("💳 FLOW=CARD_EVENT | Card presented (correlationId={}, cardType={}, maskedPan={})",
            correlationId, event.cardType, event.maskedPan)

        try {
            // Opprett authorization record
            val auth = PaymentAuthorization(
                authId = UUID.randomUUID().toString(),
                correlationId = correlationId,
                dispenserAddress = DISPENSER_ADDRESS,
                reservedAmountMinor = DEFAULT_RESERVE_AMOUNT_MINOR,
                capturedAmountMinor = null,
                status = AuthStatus.AUTH_PENDING,
                flow = PaymentFlow.CARD_EVENT
            )
            activeAuthorizations[DISPENSER_ADDRESS] = auth

            log.info("📝 FLOW=CARD_EVENT | Created authorization (authId={}, dispenser={}, amount={} kr)",
                auth.authId, DISPENSER_ADDRESS, DEFAULT_RESERVE_AMOUNT_MINOR / 100.0)

            // Reserve beløp på terminal
            val reserveResponse = paymentTerminalClient.reserve(DEFAULT_RESERVE_AMOUNT_MINOR, correlationId)

            if (reserveResponse.success) {
                auth.status = AuthStatus.AUTHORIZED
                log.info("✅ FLOW=CARD_EVENT | Reserve successful (correlationId={}, amount={} kr)",
                    correlationId, DEFAULT_RESERVE_AMOUNT_MINOR / 100.0)

                // Frigi pumpe
                releasePumpAfterReserve(DISPENSER_ADDRESS, correlationId)
            } else {
                auth.status = AuthStatus.FAILED
                log.error("❌ FLOW=CARD_EVENT | Reserve failed (correlationId={}, error={})",
                    correlationId, reserveResponse.error ?: reserveResponse.errorCode)
                activeAuthorizations.remove(DISPENSER_ADDRESS)
            }
        } catch (e: Exception) {
            log.error("💥 FLOW=CARD_EVENT | Exception during card event handling (correlationId={})", correlationId, e)
            activeAuthorizations.remove(DISPENSER_ADDRESS)
        }
    }

    /**
     * Frigjør pumpe etter vellykket reserve
     */
    private suspend fun releasePumpAfterReserve(dispenserAddress: Int, correlationId: String) {
        log.info("⛽ FLOW=CARD_EVENT | Releasing pump (dispenser={}, correlationId={})",
            dispenserAddress, correlationId)

        try {
            val priceHistory = priceService.getCurrentPrice()
            val pricePerLitre = priceHistory?.pricePerLiter?.toDouble() ?: 15.90

            val transaction = transactionService.createStartedTransaction(
                dispenserAddress = dispenserAddress,
                pricePerLiterKr = pricePerLitre
            )

            log.info("📝 FLOW=CARD_EVENT | Created transaction (ID={}, dispenser={}, price={} kr/L, correlationId={})",
                transaction.transactionId, dispenserAddress, pricePerLitre, correlationId)

            val result = fuelPumpService.startFueling(
                pumpId = dispenserAddress,
                productId = 1,  // TODO: configurable
                pricePerLitre = pricePerLitre
            )

            when (result) {
                is StartFuelingResult.Success -> {
                    log.info("✅ FLOW=CARD_EVENT | Pump released - ready to pump (dispenser={}, correlationId={})",
                        dispenserAddress, correlationId)
                }
                else -> {
                    log.error("❌ FLOW=CARD_EVENT | Pump release failed (dispenser={}, result={}, correlationId={})",
                        dispenserAddress, result, correlationId)
                    // Reversal on pump failure
                    performReversal(correlationId, "Pump release failed: $result")
                    activeAuthorizations[dispenserAddress]?.status = AuthStatus.PAYMENT_FAILED
                }
            }
        } catch (e: Exception) {
            log.error("💥 FLOW=CARD_EVENT | Exception during pump release (correlationId={})", correlationId, e)
            performReversal(correlationId, "Exception: ${e.message}")
            activeAuthorizations[dispenserAddress]?.status = AuthStatus.PAYMENT_FAILED
        }
    }

    /**
     * Del 2A: Når pumping stopper - capture faktisk beløp
     */
    suspend fun onPumpingStopped(dispenserAddress: Int, actualAmountMinor: Int, actualLitres: Double) {
        val auth = activeAuthorizations[dispenserAddress]

        if (auth == null) {
            log.warn("⚠️  Settle pending transaction without active auth (dispenser={}, amount={} kr) - FLOW=MANUAL_RELEASE",
                dispenserAddress, actualAmountMinor / 100.0)
            return
        }

        log.info("🛑 FLOW={} | Pumping stopped (dispenser={}, correlationId={}, actualAmount={} kr, litres={} L)",
            auth.flow, dispenserAddress, auth.correlationId, actualAmountMinor / 100.0, actualLitres)

        if (auth.flow == PaymentFlow.CARD_EVENT) {
            handleCardFlowCapture(auth, actualAmountMinor)
        } else {
            log.info("ℹ️  FLOW=MANUAL_RELEASE | Manual payment pending (dispenser={}, amount={} kr)",
                dispenserAddress, actualAmountMinor / 100.0)
        }
    }

    private suspend fun handleCardFlowCapture(auth: PaymentAuthorization, actualAmountMinor: Int) {
        auth.status = AuthStatus.PENDING_CAPTURE
        auth.capturedAmountMinor = actualAmountMinor

        log.info("💰 FLOW=CARD_EVENT | Capturing actual amount (correlationId={}, amount={} kr)",
            auth.correlationId, actualAmountMinor / 100.0)

        try {
            val captureResponse = paymentTerminalClient.capture(actualAmountMinor, auth.correlationId)

            if (captureResponse.success) {
                auth.status = AuthStatus.PAID
                log.info("✅ FLOW=CARD_EVENT | Capture successful (correlationId={}, amount={} kr)",
                    auth.correlationId, actualAmountMinor / 100.0)
                activeAuthorizations.remove(auth.dispenserAddress)
            } else {
                log.error("❌ FLOW=CARD_EVENT | Capture failed (correlationId={}, error={}) - attempting reversal",
                    auth.correlationId, captureResponse.error ?: captureResponse.errorCode)

                // Attempt reversal on capture failure
                performReversal(auth.correlationId, "Capture failed: ${captureResponse.error}")
                auth.status = AuthStatus.PAYMENT_FAILED
            }
        } catch (e: Exception) {
            log.error("💥 FLOW=CARD_EVENT | Exception during capture (correlationId={})", auth.correlationId, e)
            performReversal(auth.correlationId, "Capture exception: ${e.message}")
            auth.status = AuthStatus.PAYMENT_FAILED
        }
    }

    private suspend fun performReversal(correlationId: String, reason: String) {
        log.warn("🔄 Performing reversal (correlationId={}, reason={})", correlationId, reason)

        try {
            val reversalResponse = paymentTerminalClient.reversal(correlationId)

            if (reversalResponse.success) {
                log.info("✅ Reversal successful (correlationId={})", correlationId)
            } else {
                log.error("❌ Reversal failed! Manual intervention required (correlationId={}, error={})",
                    correlationId, reversalResponse.error ?: reversalResponse.errorCode)
            }
        } catch (e: Exception) {
            log.error("💥 Reversal exception! Manual intervention required (correlationId={})", correlationId, e)
        }
    }

    // ========================================
    // Del 2B: Manuell flyt (legacy support)
    // ========================================

    /**
     * Del 2B: Station owner manual flow - purchase then start pump.
     * This creates a MANUAL_RELEASE authorization.
     */
    fun openTerminalAndPurchase(
        amountMinor: Int,
        pumpId: Int = 1,
        productId: Int = 1,
        optionalData: String? = null,
        clientRequestId: String? = null
    ): TerminalOperationResponse {
        if (terminalClient == null) {
            log.warn("FLOW=MANUAL_RELEASE | Terminal client not configured; cannot perform purchase")
            return TerminalOperationResponse(success = false, error = "Terminal client not configured")
        }

        val correlationId = clientRequestId ?: UUID.randomUUID().toString()
        log.info("FLOW=MANUAL_RELEASE | Preparing terminal for purchase (pumpId={}, amountMinor={}, correlationId={})",
            pumpId, amountMinor, correlationId)

        // Create manual flow authorization
        val auth = PaymentAuthorization(
            authId = UUID.randomUUID().toString(),
            correlationId = correlationId,
            dispenserAddress = pumpId,
            reservedAmountMinor = amountMinor,
            capturedAmountMinor = null,
            status = AuthStatus.AUTH_PENDING,
            flow = PaymentFlow.MANUAL_RELEASE
        )
        activeAuthorizations[pumpId] = auth
        if (!prepareTerminalForPurchase()) {
            return TerminalOperationResponse(
                success = false,
                error = "Terminal not ready for purchase",
                errorCode = "terminal_not_ready"
            )
        }

        val purchaseRequest = TerminalPurchaseRequest(
            amountMinor = amountMinor,
            optionalData = optionalData,
            clientRequestId = clientRequestId
        )

        val purchaseResponse = performPurchaseWithRetry(purchaseRequest)

        purchaseResponse.operationId?.let { opId ->
            if (purchaseResponse.success) {
                auth.status = AuthStatus.AUTHORIZED
                log.info("✅ FLOW=MANUAL_RELEASE | Purchase approved (correlationId={}, operationId={})",
                    correlationId, opId)

                startPumpingAfterPayment(
                    amountCents = amountMinor.toLong(),
                    pumpId = pumpId,
                    productId = productId,
                    operationId = opId
                )
            } else {
                auth.status = AuthStatus.FAILED
                activeAuthorizations.remove(pumpId)
                log.error("❌ FLOW=MANUAL_RELEASE | Purchase failed (correlationId={}, error={})",
                    correlationId, purchaseResponse.error ?: purchaseResponse.errorCode)
            }
        } ?: run {
            if (purchaseResponse.success) {
                log.warn("⚠️  FLOW=MANUAL_RELEASE | Purchase succeeded but no operationId returned (correlationId={})",
                    correlationId)
            } else {
                auth.status = AuthStatus.FAILED
                activeAuthorizations.remove(pumpId)
                log.error("❌ FLOW=MANUAL_RELEASE | Purchase failed (correlationId={}, error={})",
                    correlationId, purchaseResponse.error ?: purchaseResponse.errorCode)
            }
        }

        return purchaseResponse
    }

    private fun prepareTerminalForPurchase(): Boolean {
        val status = terminalClient?.getStatus() ?: return false
        var needsOpen = false

        if (status.connectionState.equals("Aborted", ignoreCase = true)) {
            log.warn("Terminal connection aborted; closing and reopening")
            terminalClient.closeTerminal()
            needsOpen = true
        }

        if (!status.terminalReady) {
            needsOpen = true
        }

        if (needsOpen) {
            log.info("Opening terminal")
            val openResponse = terminalClient.openTerminal()
            if (!openResponse.success) {
                log.error("Terminal open failed: {}", openResponse.error)
                return false
            }
        } else {
            log.info("Terminal already ready; skipping open")
        }

        return waitForReady(timeoutMs = 60_000)
    }

    private fun waitForReady(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val status = terminalClient?.getStatus() ?: return false
            if (status.terminalReady) {
                return true
            }
            Thread.sleep(1000)
        }
        log.warn("Terminal not ready after waiting {} ms", timeoutMs)
        return false
    }

    private fun performPurchaseWithRetry(request: TerminalPurchaseRequest): TerminalOperationResponse {
        val maxBusyRetries = 3
        var busyAttempts = 0
        var notReadyAttempts = 0

        while (true) {
            val response = terminalClient?.purchase(request)
                ?: return TerminalOperationResponse(success = false, error = "Terminal client not configured")

            if (response.success) {
                return response
            }

            when (response.errorCode) {
                "terminal_busy" -> {
                    busyAttempts++
                    if (busyAttempts >= maxBusyRetries) {
                        return response
                    }
                    log.warn("Terminal busy, retrying purchase (attempt {} of {})", busyAttempts, maxBusyRetries)
                    Thread.sleep(1500)
                }
                "terminal_not_ready" -> {
                    notReadyAttempts++
                    if (notReadyAttempts >= maxBusyRetries) {
                        return response
                    }
                    log.warn("Terminal not ready, reopening before retry (attempt {} of {})", notReadyAttempts, maxBusyRetries)
                    if (!prepareTerminalForPurchase()) {
                        return response
                    }
                }
                else -> return response
            }
        }
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
