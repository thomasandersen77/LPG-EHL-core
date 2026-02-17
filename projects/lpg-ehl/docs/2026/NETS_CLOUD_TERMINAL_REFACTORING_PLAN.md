# Nets Cloud Connect Terminal - Refactoring Plan
**Dato:** 2026-02-17
**Status:** Planlegging
**Formål:** Koble LPG service-modulen sin betalingsflyt fullstendig til Nets Cloud Connect

---

## Executive Summary

Dagens implementasjon har følgende utfordringer:
1. **Ingen event-basert kortflyt** - kun manuell frigiving fra stasjonseier
2. **Gammel SSE terminal consumer** kjører parallelt og kan forstyrre Nets-flyten
3. **Manglende abstraksjon** - service-modulen er tett koblet til én terminal-type
4. **"Terminal er opptatt"** håndteres ikke robust med retry
5. **Logging mangler correlationId** og flow-type for troubleshooting

---

## Del 1: PaymentTerminalClient Abstraction Layer

### Mål
Lag et rent abstraksjonslag mellom service-modulen og terminal-implementasjoner.

### Nåværende situasjon
```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/
class PumpPaymentOrchestrator(
    private val terminalClient: TerminalClient?  // ✗ Direkte avhengighet
)
```

`TerminalClient` er allerede definert i service-modulen, men:
- Brukes kun for `TerminalClient?` optional injeksjon
- Ingen støtte for event streams (card tapped, terminal ready)
- Mangler reserve/capture/reversal flow

### Foreslått design

```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/PaymentTerminalClient.kt
interface PaymentTerminalClient {
    // Lifecycle
    suspend fun open(): TerminalSimpleResponse
    suspend fun close(): TerminalSimpleResponse
    fun getStatus(): TerminalStatusResponse
    fun getHealth(): TerminalHealthResponse

    // Payment operations
    suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse
    suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse
    suspend fun reversal(correlationId: String): TerminalOperationResponse

    // Event stream for card events
    fun terminalEvents(): Flow<TerminalEvent>
}

sealed class TerminalEvent {
    data class CardPresented(val cardType: String) : TerminalEvent()
    data class TerminalReady(val address: String) : TerminalEvent()
    data class TransactionResult(val approved: Boolean, val amount: Int) : TerminalEvent()
    data class Error(val message: String) : TerminalEvent()
}
```

### Implementasjoner

#### NetsCloudTerminalAdapter
```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/adapter/NetsCloudTerminalAdapter.kt
@Service
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-connect")
class NetsCloudTerminalAdapter(
    private val netsClient: NetsCloudConnectTerminalClient,
    private val responseParser: NetsResponseParser
) : PaymentTerminalClient {

    private val eventFlow = MutableSharedFlow<TerminalEvent>()

    override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        // Nets: "purchase" er både reserve og capture i én operasjon
        val request = TerminalPurchaseRequest(
            amountMinor = amountMinor,
            operatorId = correlationId
        )
        return netsClient.purchase(request)
    }

    override suspend fun capture(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        // Nets: capture skjer automatisk, returner success
        return TerminalOperationResponse(success = true, operationId = correlationId)
    }

    override fun terminalEvents(): Flow<TerminalEvent> = eventFlow.asSharedFlow()

    // Listen to Nets WebSocket and emit events
    private fun startEventListener() {
        // Parse Dfs13JsonReceived, Dfs13TerminalReady, etc.
        // Emit to eventFlow
    }
}
```

---

## Del 2: Betalingsflyt - To Frigivingsmåter

### 2A: Kortstyrt flyt (event-basert frigiving)

#### Sekvens
```
1. Kunde tapper kort → TerminalEvent.CardPresented
2. PumpPaymentOrchestrator.onCardPresented()
   - Opprett Payment Authorization (status: AUTH_PENDING)
   - terminal.reserve(150000, correlationId)  // 1500 NOK reserve
3. Ved success reserve:
   - Sett status AUTHORIZED
   - fuelPumpService.releaseDispenser()
   - Sett pump til READY_TO_PUMP
4. Kunde fyller:
   - UI oppdateres live (liter + kr)
5. Pumping stopper:
   - Sett status PENDING_CAPTURE
   - terminal.capture(actualAmountMinor, correlationId)
6. Ved success capture:
   - Sett status PAID
   - Pump → BLOCKED/IDLE
7. Ved failure:
   - terminal.reversal(correlationId)
   - Sett status PAYMENT_FAILED
   - Notify operator
```

#### Kodeendringer
```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/PumpPaymentOrchestrator.kt
class PumpPaymentOrchestrator(
    private val terminalClient: PaymentTerminalClient,
    // ... existing deps
) {

    init {
        // Subscribe to terminal events
        CoroutineScope(Dispatchers.IO).launch {
            terminalClient.terminalEvents().collect { event ->
                when (event) {
                    is TerminalEvent.CardPresented -> handleCardPresented(event)
                    is TerminalEvent.TerminalReady -> handleTerminalReady(event)
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleCardPresented(event: TerminalEvent.CardPresented) {
        log.info("🎴 Card presented - starting reserve flow (FLOW=CARD_EVENT)")

        val correlationId = UUID.randomUUID().toString()
        val reserveAmountMinor = 150_000  // 1500 NOK reserve

        // 1. Create payment authorization
        val auth = createPaymentAuthorization(
            dispenserAddress = 33,  // From config
            amountMinor = reserveAmountMinor,
            correlationId = correlationId,
            flow = "CARD_EVENT"
        )

        // 2. Reserve amount on terminal
        val reserveResponse = terminalClient.reserve(reserveAmountMinor, correlationId)

        if (reserveResponse.success) {
            auth.status = AuthStatus.AUTHORIZED
            log.info("✅ Reserve successful - releasing pump (correlationId={})", correlationId)

            // 3. Release pump
            fuelPumpService.releaseDispenser(dispenserAddress = 33)

        } else {
            auth.status = AuthStatus.FAILED
            log.error("❌ Reserve failed: {} (correlationId={})",
                reserveResponse.error, correlationId)
        }
    }

    fun onPumpingStopped(
        dispenserAddress: Int,
        volumeLitres: Double,
        amountKr: Double
    ) {
        val auth = findActiveAuthorization(dispenserAddress)

        if (auth == null) {
            log.warn("⚠️  No active authorization - using manual flow")
            handleManualPaymentPending(dispenserAddress, amountKr)
            return
        }

        log.info("💳 Pumping stopped - capturing payment (FLOW=CARD_EVENT, correlationId={})",
            auth.correlationId)

        val actualAmountMinor = (amountKr * 100).toInt()

        // Capture actual amount
        val captureResponse = terminalClient.capture(actualAmountMinor, auth.correlationId)

        if (captureResponse.success) {
            auth.status = AuthStatus.PAID
            log.info("✅ Capture successful - transaction complete")

        } else {
            log.error("❌ Capture failed - attempting reversal")
            terminalClient.reversal(auth.correlationId)
            auth.status = AuthStatus.PAYMENT_FAILED
        }
    }
}
```

### 2B: Manuell frigiving (stasjonseier)

#### Sekvens
```
1. Stasjonseier klikker "FRI PUMPE":
   - Ingen terminal reserve
   - Frigi pumpa direkte
   - Start fylling
2. Pumping stopper:
   - Sett transaksjon PAYMENT_PENDING
   - GUI lar operator velge "CARD" eller "CREDIT"
3. Operator velger metode:
   - "CARD": Manuell markering (ingen terminal-capture)
   - "CREDIT": Trigger Azure-kredittlogikk
```

#### Kodeendringer
```kotlin
fun handleManualRelease(dispenserAddress: Int) {
    log.info("🔓 Manual release by station owner (FLOW=MANUAL_RELEASE, dispenser={})",
        dispenserAddress)

    // No terminal reserve - direct release
    fuelPumpService.releaseDispenser(dispenserAddress)
}

private fun handleManualPaymentPending(dispenserAddress: Int, amountKr: Double) {
    log.info("💰 Settle pending transaction without active auth (FLOW=MANUAL_RELEASE)")

    val transaction = transactionService.createPendingTransaction(
        dispenserAddress = dispenserAddress,
        amountKr = amountKr
    )

    // Notify UI: operator must select payment method
    notifyOperator(transaction)
}

fun confirmManualPayment(transactionId: String, method: PaymentMethod) {
    log.info("✅ Manual payment confirmed (method={}, txId={})", method, transactionId)

    when (method) {
        PaymentMethod.CARD -> {
            // Mark as paid - no terminal capture
            transactionService.markAsPaid(transactionId, "MANUAL_CARD")
        }
        PaymentMethod.CREDIT -> {
            // Trigger Azure credit logic
            creditService.processCredit(transactionId)
        }
    }
}
```

---

## Del 3: Terminal State Management & "Terminal er opptatt"

### Problem
Fra loggene:
```
Terminal er opptatt / callResult=2 / cancelled
```

### Løsning: Retry med backoff

```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/adapter/NetsCloudTerminalAdapter.kt
class NetsCloudTerminalAdapter(/*...*/) : PaymentTerminalClient {

    private val terminalMutex = Mutex()
    private var lastOperationTime = 0L

    override suspend fun reserve(amountMinor: Int, correlationId: String): TerminalOperationResponse {
        terminalMutex.withLock {
            // Settling delay: wait 500ms after last operation
            val timeSinceLastOp = System.currentTimeMillis() - lastOperationTime
            if (timeSinceLastOp < 500) {
                delay(500 - timeSinceLastOp)
            }

            val response = retryWithBackoff(
                maxAttempts = 5,
                initialDelayMs = 300,
                maxDelayMs = 2000
            ) {
                val status = netsClient.getStatus()
                if (!status.terminalReady) {
                    log.warn("⚠️  Terminal not ready, waiting... (attempt {})", it)
                    throw TerminalNotReadyException()
                }

                netsClient.purchase(TerminalPurchaseRequest(
                    amountMinor = amountMinor,
                    operatorId = correlationId
                ))
            }

            lastOperationTime = System.currentTimeMillis()
            return response
        }
    }

    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        block: suspend (attempt: Int) -> T
    ): T {
        var attempt = 1
        var delayMs = initialDelayMs

        while (true) {
            try {
                return block(attempt)
            } catch (e: TerminalNotReadyException) {
                if (attempt >= maxAttempts) {
                    throw e
                }
                log.warn("Retry attempt {}/{} after {}ms", attempt, maxAttempts, delayMs)
                delay(delayMs)
                delayMs = minOf(delayMs * 2, maxDelayMs)
                attempt++
            }
        }
    }
}
```

---

## Del 4: Disable Legacy SSE Terminal Consumer

### Nåværende situasjon
```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/PaymentTerminalEventConsumer.kt
@Service
class PaymentTerminalEventConsumer(/*...*/) {
    // Kobler til http://192.168.0.9:18080 med SSE
    // Reconnecter stadig
}
```

### Løsning: Conditional activation

```kotlin
// lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/PaymentTerminalEventConsumer.kt
@Service
@ConditionalOnProperty(
    name = ["terminal.provider"],
    havingValue = "legacy-sse-sim",
    matchIfMissing = true  // Default for backward compatibility
)
class PaymentTerminalEventConsumer(/*...*/) {
    // Kun aktiv når terminal.provider = legacy-sse-sim
}
```

### application.yaml
```yaml
terminal:
  provider: nets-cloud-connect  # or: legacy-sse-sim
```

---

## Del 5: Testplan

### Unit Tests
```kotlin
// lpg-ehl-service/src/test/kotlin/no/cloudberries/lpg/service/terminal/PumpPaymentOrchestratorTest.kt
class PumpPaymentOrchestratorTest {

    private lateinit var mockTerminal: PaymentTerminalClient
    private lateinit var orchestrator: PumpPaymentOrchestrator

    @Test
    fun `card event flow - reserve ok - release - capture ok - PAID`() {
        // Mock terminal events
        val eventFlow = MutableSharedFlow<TerminalEvent>()
        every { mockTerminal.terminalEvents() } returns eventFlow
        every { mockTerminal.reserve(150_000, any()) } returns success()
        every { mockTerminal.capture(5_000, any()) } returns success()

        // Emit card presented
        eventFlow.emit(TerminalEvent.CardPresented("VISA"))

        // Verify reserve called
        verify { mockTerminal.reserve(150_000, any()) }

        // Simulate pumping stopped
        orchestrator.onPumpingStopped(33, 3.5, 50.0)

        // Verify capture called with actual amount
        verify { mockTerminal.capture(5_000, any()) }
    }

    @Test
    fun `terminal busy - retry - success`() {
        var attempts = 0
        every { mockTerminal.reserve(any(), any()) } answers {
            if (++attempts < 3) {
                TerminalOperationResponse(success = false, errorCode = "terminal_busy")
            } else {
                success()
            }
        }

        val response = orchestrator.reserveWithRetry(100_000, "test-id")

        assertTrue(response.success)
        assertEquals(3, attempts)
    }

    @Test
    fun `capture failure - reversal - PAYMENT_FAILED`() {
        every { mockTerminal.reserve(any(), any()) } returns success()
        every { mockTerminal.capture(any(), any()) } returns failure("Capture failed")
        every { mockTerminal.reversal(any()) } returns success()

        // Start with reserve
        orchestrator.handleCardPresented(TerminalEvent.CardPresented("MC"))

        // Stop with capture failure
        orchestrator.onPumpingStopped(33, 5.0, 75.0)

        // Verify reversal called
        verify { mockTerminal.reversal(any()) }
    }
}
```

### Integration Test
```kotlin
// Use WireMock to simulate Nets Cloud Connect API
@ExtendWith(WireMockExtension::class)
class NetsCloudTerminalIntegrationTest {

    @Test
    fun `full flow with fake Nets terminal`() {
        // Setup WireMock stubs for Nets endpoints
        stubFor(post("/v1/login")
            .willReturn(okJson("""{"token":"fake-jwt"}""")))

        stubFor(post("/v1/purchase")
            .willReturn(okJson("""{"success":true}""")))

        // Run flow
        val adapter = NetsCloudTerminalAdapter(/*...*/)
        val response = adapter.reserve(100_000, "test-id")

        assertTrue(response.success)
    }
}
```

### Manual Test Improvements
```kotlin
// lpg-nets-cloud-connect/src/test/kotlin/.../NetsCloudConnectTerminalClientTestManual.kt
@AfterEach
fun teardown() = runTest {
    // Clean shutdown to avoid UncompletedCoroutinesError
    terminalClient.closeTerminal()
    authClient.close()
    messageBuilder.resetEcrId()  // Reset cached ECRID

    // Give coroutines time to cancel
    delay(500)
}
```

---

## Del 6: Logging & Observability

### Structured Logging
```kotlin
log.info("💳 Payment operation",
    kv("flow", flow),  // CARD_EVENT or MANUAL_RELEASE
    kv("correlationId", correlationId),
    kv("dispenserAddress", dispenserAddress),
    kv("transactionId", transactionId),
    kv("operation", "reserve|capture|reversal"),
    kv("amountMinor", amountMinor),
    kv("attempt", attemptCount)
)
```

### Key Metrics
- Reserve success rate per flow type
- Average reserve-to-capture time
- Busy retry count distribution
- Reversal frequency

---

## Leveransekrav

### PR Structure (små, fokuserte commits)
1. **Commit 1:** `PaymentTerminalClient` interface + `NetsCloudTerminalAdapter`
2. **Commit 2:** Event-basert kortflyt i `PumpPaymentOrchestrator`
3. **Commit 3:** Manuell frigivingsflyt + payment pending handling
4. **Commit 4:** Terminal state management + busy retry
5. **Commit 5:** Disable SSE consumer når Nets aktiv
6. **Commit 6:** Unit tests + integration tests
7. **Commit 7:** Logging improvements + observability

### Configuration
```yaml
# application.yaml (production with Nets)
terminal:
  provider: nets-cloud-connect

nets:
  cloud:
    base-url: https://connectcloud.aws.nets.eu
    username: ${NETS_USERNAME}
    password: ${NETS_PASSWORD}
    terminal-id: ${NETS_TERMINAL_ID}
    websocket:
      ping-interval-ms: 20000
    timeouts:
      purchase-timeout-ms: 60000

# application.yaml (dev/simulator)
terminal:
  provider: legacy-sse-sim

payment:
  terminal:
    enabled: true
    base-url: http://localhost:18080
```

---

## Timeline Estimate

| Del | Oppgave | Estimat | Status |
|-----|---------|---------|--------|
| 1 | PaymentTerminalClient + Adapter | 4t | ⏳ Ikke startet |
| 2A | Kortstyrt flyt | 6t | ⏳ Ikke startet |
| 2B | Manuell flyt | 3t | ⏳ Ikke startet |
| 3 | Terminal state + retry | 4t | ⏳ Ikke startet |
| 4 | Disable SSE consumer | 2t | ⏳ Ikke startet |
| 5 | Tests | 6t | ⏳ Ikke startet |
| 6 | Logging | 2t | ⏳ Ikke startet |
| **Total** | | **27t** (~3-4 dager) | |

---

## Risiko & Mitigering

### Risiko 1: Nets Terminal State Race Conditions
**Mitigering:** Mutex + settling delay + robust retry

### Risiko 2: Existing Manual Flow Regression
**Mitigering:** Bevar eksisterende GUI flow, legg til tests

### Risiko 3: SSE Consumer Interference
**Mitigering:** Conditional activation basert på terminal.provider

### Risiko 4: CoroutineScope Leaks i Tests
**Mitigering:** Proper cleanup i @AfterEach med delay

---

## Referanser
- Instruksjonsfil: `instructions/terminal/refactor_terminal_service_flow.md`
- Nets Cloud Connect analyse: `docs/2026/KOTLIN_VS_MONO_TERMINAL_ANALYSE.md`
- Eksisterende orchestrator: `lpg-ehl-service/src/main/kotlin/.../PumpPaymentOrchestrator.kt`
- Nets Kotlin modul: `lpg-nets-cloud-connect/`

---

**Neste steg:** Review denne planen med teamet før implementering starter.
