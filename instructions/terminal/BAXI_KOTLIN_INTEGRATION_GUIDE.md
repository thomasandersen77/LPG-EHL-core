# Baxi-Kotlin Integrasjonsguide for LPG Service

## Oversikt

Dette dokumentet forklarer hvordan du integrerer `baxi-kotlin` JAR-filen i `lpg-ehl-service` modulen, og gir en komplett prompt for AI-assistert implementasjon.

## Dagens situasjon

Din **lpg-ehl-service** modul har allerede `baxi-kotlin` (v0.1.0-SNAPSHOT) som Maven dependency:

```xml
<dependency>
    <groupId>no.cloudberries.norgesgass</groupId>
    <artifactId>baxi-kotlin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Eksisterende arkitektur

**LPG Service** har:
- **Interface**: `TerminalClient` - definerer terminalkommunikasjon
- **Implementasjon**: `SimulatedTerminalClient` - HTTP-basert simulator-klient
- **Konfigurasjon**: `TerminalConfiguration` - Spring bean setup

**Baxi-kotlin JAR** eksporterer:
- **BaxiClient** interface - hovedgrensesnitt for terminalkommunikasjon
- **BaxiEventListener** interface - asynkrone callbacks fra terminal
- Data classes for requests og responses

## Hva du trenger

1. **Produksjonsimplementasjon**: `BaxiTerminalClient` som bruker `BaxiClient` fra JAR
2. **Mock-implementasjon**: `MockBaxiTerminalClient` for enkel testing uten avhengigheter
3. **Oppdatert konfigurasjon**: Støtte for flere implementasjoner via properties

---

## Baxi-Kotlin API Referanse

### BaxiClient Interface

```kotlin
package no.cloudberries.norgesgass.baxi.client

import java.io.Closeable
import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener

interface BaxiClient : Closeable {
    /**
     * Opens transport and starts internal loops.
     * Return 1 on accepted, 0 on immediate failure.
     * Terminal readiness is signaled via BaxiEventListener.onTerminalReady.
     */
    fun open(config: BaxiIniConfig): OpenResult

    /**
     * Mirrors vendor Close() semantics: return 1 on accepted.
     */
    fun closeTerminal(): CloseResult

    fun transferAmount(args: TransferAmountArgs): CallAcceptResult

    fun administration(args: AdministrationArgs): CallAcceptResult

    fun sendTld(args: SendTldArgs): CallAcceptResult

    fun sendJson(args: SendJsonArgs): CallAcceptResult

    fun confirm(args: ConfirmArgs): CallAcceptResult

    fun setEventListener(listener: BaxiEventListener?)
}

data class OpenResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class CloseResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class CallAcceptResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class TransferAmountArgs(
    val operId: String,
    val type1: Int,
    val amount1: Int,
    val type2: Int,
    val amount2: Int,
    val type3: Int,
    val amount3: Int,
    val optionalData: String? = null,
)

data class AdministrationArgs(
    val admCode: Int,
    val operId: String = "0000",
    val optionalData: String? = null,
)

data class SendTldArgs(
    val tldType: String,
    val tldField: ByteArray,
)

data class SendJsonArgs(
    val jsonData: String,
)

data class ConfirmArgs(
    val id: Int,
    val allow: Boolean,
)
```

### BaxiEventListener Interface

```kotlin
package no.cloudberries.norgesgass.baxi.events

/**
 * Server-facing event interface similar to vendor IBaxiEvents.
 * The Kotlin implementation guarantees callbacks are serialized (single-threaded).
 */
interface BaxiEventListener {
    fun onTerminalReady() {}

    fun onDisplayText(
        displayText: String,
        displayTextSourceId: Int? = null,
        displayTextId: Int? = null
    ) {}

    fun onPrintText(printText: String) {}

    fun onError(errorCode: Int, errorString: String?) {}

    fun onLocalMode(event: LocalModeEvent) {}

    fun onLastFinancialResult(event: LastFinancialResultEvent) {}

    fun onStdRsp(stdRsp: ByteArray) {}

    fun onTldReceived(tldType: Int, tldData: ByteArray) {}

    fun onJsonReceived(json: String) {}
}

data class LocalModeEvent(
    val result: Int?,
    val responseCode: String?,
    val rejectionSource: String?,
    val rejectionReason: String?,
    val localModeResultData: String?,
    val fields: Map<String, String>,
)

data class LastFinancialResultEvent(
    val result: Int?,
    val resultData: String?,
)
```

### BaxiIniConfig

```kotlin
package no.cloudberries.norgesgass.baxi.config

data class BaxiIniConfig(
    val hostIpAddress: String,
    val hostPort: Int,
    val vendorInfoExtended: String?,
    val socketListenerEnabled: Boolean,
    val socketListenerPort: Int?
)
```

---

## Prompt for Cursor/IntelliJ AI

Kopier følgende prompt og lim inn i Cursor eller IntelliJ AI Chat:

```markdown
# OPPGAVE: Implementer Baxi Kotlin-integrasjon i LPG Service

## KONTEKST

Jeg jobber med en Spring Boot-basert service (`lpg-ehl-service`) som allerede har `baxi-kotlin:0.1.0-SNAPSHOT` som Maven dependency. Prosjektet er skrevet i Kotlin og har et eksisterende `TerminalClient` interface.

**Eksisterende struktur**:
- Interface: `no.cloudberries.lpg.service.terminal.TerminalClient`
- Simulert implementasjon: `SimulatedTerminalClient` (HTTP-basert)
- Konfigurasjon: `TerminalConfiguration.kt` med Spring @ConditionalOnProperty

**TerminalClient Interface** (eksisterende):
```kotlin
package no.cloudberries.lpg.service.terminal

interface TerminalClient {
    fun openTerminal(): TerminalSimpleResponse
    fun getHealth(): TerminalHealthResponse
    fun getStatus(): TerminalStatusResponse
    fun closeTerminal(): TerminalSimpleResponse
    fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse
    fun reversal(operationId: String? = null): TerminalOperationResponse
}

data class TerminalSimpleResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

data class TerminalHealthResponse(
    val status: String,
    val configLoaded: Boolean
)

data class TerminalStatusResponse(
    val terminalOpen: Boolean,
    val terminalReady: Boolean,
    val connectionState: String? = null,
    val lastError: String? = null
)

data class TerminalOperationResponse(
    val success: Boolean,
    val operationId: String? = null,
    val callResult: Int? = null,
    val entryMode: String? = null,
    val entryModeCode: String? = null,
    val localModeResultData: String? = null,
    val responseCode: String? = null,
    val rejectionReason: String? = null,
    val printTextRaw: String? = null,
    val printTextSanitized: String? = null,
    val lastDisplayText: String? = null,
    val localModeResult: Int? = null,
    val durationMs: Long? = null,
    val error: String? = null,
    val errorCode: String? = null
)

data class TerminalPurchaseRequest(
    val amountMinor: Int,
    val operatorId: String = "0000",
    val currency: String = "NOK",
    val optionalData: String? = null,
    val clientRequestId: String? = null,
    val preAvstemming: TerminalPreAvstemmingConfig? = null
)

data class TerminalPreAvstemmingConfig(
    val enabled: Boolean = false,
    val password: String = "0000",
    val timeoutSeconds: Int? = null
)
```

**Baxi-kotlin JAR API** (importert fra dependency):
```kotlin
package no.cloudberries.norgesgass.baxi.client

interface BaxiClient : Closeable {
    fun open(config: BaxiIniConfig): OpenResult
    fun closeTerminal(): CloseResult
    fun transferAmount(args: TransferAmountArgs): CallAcceptResult
    fun administration(args: AdministrationArgs): CallAcceptResult
    fun sendTld(args: SendTldArgs): CallAcceptResult
    fun sendJson(args: SendJsonArgs): CallAcceptResult
    fun confirm(args: ConfirmArgs): CallAcceptResult
    fun setEventListener(listener: BaxiEventListener?)
}

package no.cloudberries.norgesgass.baxi.events

interface BaxiEventListener {
    fun onTerminalReady() {}
    fun onDisplayText(displayText: String, displayTextSourceId: Int? = null, displayTextId: Int? = null) {}
    fun onPrintText(printText: String) {}
    fun onError(errorCode: Int, errorString: String?) {}
    fun onLocalMode(event: LocalModeEvent) {}
    fun onLastFinancialResult(event: LastFinancialResultEvent) {}
    fun onStdRsp(stdRsp: ByteArray) {}
    fun onTldReceived(tldType: Int, tldData: ByteArray) {}
    fun onJsonReceived(json: String) {}
}

data class LocalModeEvent(
    val result: Int?,
    val responseCode: String?,
    val rejectionSource: String?,
    val rejectionReason: String?,
    val localModeResultData: String?,
    val fields: Map<String, String>
)

data class LastFinancialResultEvent(
    val result: Int?,
    val resultData: String?
)

package no.cloudberries.norgesgass.baxi.config

data class BaxiIniConfig(
    val hostIpAddress: String,
    val hostPort: Int,
    val vendorInfoExtended: String?,
    val socketListenerEnabled: Boolean,
    val socketListenerPort: Int?
)
```

**Implementasjonsklasse fra JAR**:
```kotlin
import no.cloudberries.norgesgass.baxi.client.BaxiClientImpl

val client = BaxiClientImpl()  // Konkret implementasjon
```

---

## OPPGAVE 1: Lag produksjonsimplementasjon BaxiTerminalClient

**Filplassering**: `src/main/kotlin/no/cloudberries/lpg/service/terminal/BaxiTerminalClient.kt`

**Krav**:
1. Implementer `TerminalClient` interface
2. Bruk `BaxiClientImpl()` internt som field
3. Registrer en `BaxiEventListener` for å håndtere asynkrone callbacks
4. Map mellom `TerminalClient` metoder og `BaxiClient` metoder
5. Håndter asynkron→synkron konvertering med `CompletableFuture` eller `CountDownLatch`
6. Spring annotations: `@Component`, `@ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "baxi")`

**Properties fra application.yaml**:
```kotlin
@Value("\${payment.terminal.baxi.host:192.168.1.100}")
private val terminalHost: String

@Value("\${payment.terminal.baxi.port:7200}")
private val terminalPort: Int
```

**Viktige implementasjonsdetaljer**:

### openTerminal()
```kotlin
override fun openTerminal(): TerminalSimpleResponse {
    // 1. Opprett BaxiIniConfig fra properties
    // 2. Registrer BaxiEventListener
    // 3. Kall baxiClient.open(config)
    // 4. Vent på onTerminalReady() callback (timeout 30 sekunder)
    // 5. Returner TerminalSimpleResponse
}
```

### purchase()
```kotlin
override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
    // Map til TransferAmountArgs:
    // type1 = 10 (purchase/salg)
    // amount1 = request.amountMinor
    // type2, type3 = 0
    // operId = request.operatorId
    // optionalData = request.optionalData
    
    // Kall baxiClient.transferAmount()
    // Vent på onLocalMode() OG onLastFinancialResult() callbacks
    // Map til TerminalOperationResponse
    // Timeout: 60 sekunder
}
```

### reversal()
```kotlin
override fun reversal(operationId: String?): TerminalOperationResponse {
    // Map til AdministrationArgs:
    // admCode = 9100 (reversal)
    // operId = "0000"
    
    // Kall baxiClient.administration()
    // Vent på completion callbacks
    // Timeout: 30 sekunder
}
```

### getStatus()
```kotlin
override fun getStatus(): TerminalStatusResponse {
    // Returner internal state basert på:
    // - Om baxiClient er åpnet
    // - Om onTerminalReady() er mottatt
    // - Siste error fra onError() callback
}
```

### getHealth()
```kotlin
override fun getHealth(): TerminalHealthResponse {
    // Returner "healthy" hvis baxiClient er åpen og ready
    // Ellers "unhealthy"
}
```

### closeTerminal()
```kotlin
override fun closeTerminal(): TerminalSimpleResponse {
    // Kall baxiClient.closeTerminal()
    // Clear internal state
}
```

**Asynkron håndtering - eksempel**:
```kotlin
private var operationFuture: CompletableFuture<TerminalOperationResponse>? = null
private var localModeEvent: LocalModeEvent? = null
private var lastFinancialResult: LastFinancialResultEvent? = null

private val eventListener = object : BaxiEventListener {
    override fun onTerminalReady() {
        // Signal til openTerminal()
    }
    
    override fun onLocalMode(event: LocalModeEvent) {
        localModeEvent = event
        checkOperationComplete()
    }
    
    override fun onLastFinancialResult(event: LastFinancialResultEvent) {
        lastFinancialResult = event
        checkOperationComplete()
    }
    
    override fun onError(errorCode: Int, errorString: String?) {
        // Complete future with error
    }
    
    private fun checkOperationComplete() {
        if (localModeEvent != null && lastFinancialResult != null) {
            val response = mapToTerminalOperationResponse()
            operationFuture?.complete(response)
        }
    }
}

private fun mapToTerminalOperationResponse(): TerminalOperationResponse {
    val local = localModeEvent!!
    val financial = lastFinancialResult!!
    return TerminalOperationResponse(
        success = local.result == 1,
        callResult = local.result,
        responseCode = local.responseCode,
        rejectionReason = local.rejectionReason,
        localModeResultData = local.localModeResultData,
        localModeResult = financial.result,
        // Map printText from onPrintText callback
        // Map displayText from onDisplayText callback
    )
}
```

**Logger**:
```kotlin
private val log = LoggerFactory.getLogger(javaClass)
```

---

## OPPGAVE 2: Lag mock-implementasjon MockBaxiTerminalClient

**Filplassering**: `src/main/kotlin/no/cloudberries/lpg/service/terminal/MockBaxiTerminalClient.kt`

**Krav**:
1. Enkel in-memory mock - ingen eksterne avhengigheter
2. Returner predefinerte responses umiddelbart
3. Logg alle kall
4. Spring annotations: `@Component`, `@ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "mock")`

**Implementasjon**:
```kotlin
package no.cloudberries.lpg.service.terminal

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "mock")
class MockBaxiTerminalClient : TerminalClient {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private var isOpen = false
    private var isReady = false
    
    override fun openTerminal(): TerminalSimpleResponse {
        log.info("MockBaxiTerminalClient: openTerminal()")
        isOpen = true
        isReady = true
        return TerminalSimpleResponse(
            success = true,
            message = "Mock Baxi terminal opened"
        )
    }
    
    override fun getHealth(): TerminalHealthResponse {
        log.info("MockBaxiTerminalClient: getHealth()")
        return TerminalHealthResponse(
            status = if (isOpen) "healthy" else "not_open",
            configLoaded = true
        )
    }
    
    override fun getStatus(): TerminalStatusResponse {
        log.info("MockBaxiTerminalClient: getStatus()")
        return TerminalStatusResponse(
            terminalOpen = isOpen,
            terminalReady = isReady,
            connectionState = if (isReady) "CONNECTED" else "DISCONNECTED"
        )
    }
    
    override fun closeTerminal(): TerminalSimpleResponse {
        log.info("MockBaxiTerminalClient: closeTerminal()")
        isOpen = false
        isReady = false
        return TerminalSimpleResponse(
            success = true,
            message = "Mock Baxi terminal closed"
        )
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        log.info("MockBaxiTerminalClient: purchase(amountMinor={}, operatorId={})", 
            request.amountMinor, request.operatorId)
        
        if (!isReady) {
            return TerminalOperationResponse(
                success = false,
                error = "Terminal not ready",
                errorCode = "terminal_not_ready"
            )
        }
        
        return TerminalOperationResponse(
            success = true,
            operationId = "mock-op-${UUID.randomUUID()}",
            callResult = 1,
            responseCode = "00",
            localModeResult = 1,
            printTextSanitized = "MOCK RECEIPT\nAmount: ${request.amountMinor / 100.0} NOK\nApproved",
            durationMs = 1500
        )
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse {
        log.info("MockBaxiTerminalClient: reversal(operationId={})", operationId)
        
        return TerminalOperationResponse(
            success = true,
            operationId = operationId ?: "mock-reversal-${UUID.randomUUID()}",
            callResult = 1,
            responseCode = "00",
            localModeResult = 1,
            durationMs = 1000
        )
    }
}
```

---

## OPPGAVE 3: Oppdater TerminalConfiguration

**Filplassering**: `src/main/kotlin/no/cloudberries/lpg/service/terminal/TerminalConfiguration.kt`

**Endre fra**:
```kotlin
@Configuration
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalConfiguration {
    @Bean
    @Primary
    fun terminalClient(simulatedClient: SimulatedTerminalClient): TerminalClient = simulatedClient
}
```

**Til**:
```kotlin
package no.cloudberries.lpg.service.terminal

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class TerminalConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "baxi")
    fun baxiTerminalClient(baxiClient: BaxiTerminalClient): TerminalClient = baxiClient
    
    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "simulated")
    fun simulatedTerminalClient(simulatedClient: SimulatedTerminalClient): TerminalClient = simulatedClient
    
    @Bean
    @ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "mock")
    fun mockTerminalClient(mockClient: MockBaxiTerminalClient): TerminalClient = mockClient
}
```

---

## OPPGAVE 4: Lag unit tests

**Filplassering**: `src/test/kotlin/no/cloudberries/lpg/service/terminal/BaxiTerminalClientTest.kt`

**Test-struktur**:
```kotlin
package no.cloudberries.lpg.service.terminal

import no.cloudberries.norgesgass.baxi.client.BaxiClient
import no.cloudberries.norgesgass.baxi.client.OpenResult
import no.cloudberries.norgesgass.baxi.client.CallAcceptResult
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class BaxiTerminalClientTest {
    
    @Test
    fun `openTerminal should wait for onTerminalReady callback`() {
        // Given: Mock BaxiClient som kaller onTerminalReady umiddelbart
        // When: Kall openTerminal()
        // Then: Skal returnere success=true
    }
    
    @Test
    fun `purchase should map to transferAmount with type1=10`() {
        // Given: Mock BaxiClient
        // When: Kall purchase(amountMinor=5000)
        // Then: Verifiser at transferAmount ble kalt med type1=10, amount1=5000
    }
    
    @Test
    fun `purchase should wait for both LocalMode and LastFinancialResult`() {
        // Given: Mock som sender callbacks asynkront
        // When: Kall purchase()
        // Then: Skal vente til begge callbacks er mottatt
    }
    
    @Test
    fun `purchase should timeout if callbacks not received`() {
        // Given: Mock som aldri sender callbacks
        // When: Kall purchase()
        // Then: Skal kaste TimeoutException eller returnere error
    }
    
    @Test
    fun `reversal should map to administration with admCode=9100`() {
        // Given: Mock BaxiClient
        // When: Kall reversal()
        // Then: Verifiser at administration ble kalt med admCode=9100
    }
    
    @Test
    fun `getStatus should reflect terminal ready state`() {
        // Given: Terminal åpnet og onTerminalReady mottatt
        // When: Kall getStatus()
        // Then: Skal returnere terminalReady=true
    }
}
```

---

## OPPGAVE 5: Oppdater application.yaml

**Filplassering**: `src/main/resources/application.yaml` (eller `application-local.yaml`)

**Legg til**:
```yaml
payment:
  terminal:
    enabled: true
    implementation: mock  # Alternativer: baxi, simulated, mock
    baxi:
      host: 192.168.1.100
      port: 7200
```

**For produksjon** (`application-prod.yaml`):
```yaml
payment:
  terminal:
    enabled: true
    implementation: baxi
    baxi:
      host: ${BAXI_TERMINAL_HOST}  # Fra environment variable
      port: ${BAXI_TERMINAL_PORT:7200}
```

---

## VIKTIGE IMPLEMENTASJONSDETALJER

### Mapping: Purchase → TransferAmount

Baxi `transferAmount()` støtter opptil 3 beløpstyper. For kort-purchase:
```kotlin
TransferAmountArgs(
    operId = request.operatorId,
    type1 = 10,  // 10 = Purchase/Salg
    amount1 = request.amountMinor,  // Beløp i øre
    type2 = 0,   // Ingen ekstra beløp
    amount2 = 0,
    type3 = 0,
    amount3 = 0,
    optionalData = request.optionalData
)
```

### Mapping: Reversal → Administration

```kotlin
AdministrationArgs(
    admCode = 9100,  // 9100 = Reversal/Tilbakeføring
    operId = request.operatorId,
    optionalData = null
)
```

### Asynkron → Synkron Pattern

Baxi-kotlin er event-driven. Du må vente på callbacks:

```kotlin
private val operationLatch = CountDownLatch(2)  // Vent på 2 callbacks
private var capturedLocalMode: LocalModeEvent? = null
private var capturedFinancialResult: LastFinancialResultEvent? = null

override fun onLocalMode(event: LocalModeEvent) {
    capturedLocalMode = event
    operationLatch.countDown()
}

override fun onLastFinancialResult(event: LastFinancialResultEvent) {
    capturedFinancialResult = event
    operationLatch.countDown()
}

fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
    operationLatch = CountDownLatch(2)
    capturedLocalMode = null
    capturedFinancialResult = null
    
    baxiClient.transferAmount(...)
    
    val completed = operationLatch.await(60, TimeUnit.SECONDS)
    if (!completed) {
        throw TimeoutException("Terminal operation timed out")
    }
    
    return buildResponse(capturedLocalMode!!, capturedFinancialResult!!)
}
```

**Alternativt med CompletableFuture**:
```kotlin
private var operationFuture = CompletableFuture<TerminalOperationResponse>()

override fun onLastFinancialResult(event: LastFinancialResultEvent) {
    val response = mapToResponse(event)
    operationFuture.complete(response)
}

fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
    operationFuture = CompletableFuture()
    baxiClient.transferAmount(...)
    return operationFuture.get(60, TimeUnit.SECONDS)
}
```

### Thread-safety

`BaxiEventListener` callbacks kommer fra en egen tråd. Bruk:
- `@Volatile` for shared state
- `AtomicReference` for komplekse objekter
- `synchronized` blokker hvis nødvendig

### Error-håndtering

```kotlin
override fun onError(errorCode: Int, errorString: String?) {
    log.error("Terminal error: code={}, message={}", errorCode, errorString)
    
    // Complete ongoing operation med error
    operationFuture?.completeExceptionally(
        RuntimeException("Terminal error $errorCode: $errorString")
    )
}
```

---

## FORVENTET FILSTRUKTUR

```
lpg-ehl-service/
├── src/main/kotlin/no/cloudberries/lpg/service/terminal/
│   ├── TerminalClient.kt                  (eksisterer)
│   ├── SimulatedTerminalClient.kt         (eksisterer)
│   ├── BaxiTerminalClient.kt              (NY - produksjon)
│   ├── MockBaxiTerminalClient.kt          (NY - enkel mock)
│   └── TerminalConfiguration.kt           (OPPDATER)
│
├── src/test/kotlin/no/cloudberries/lpg/service/terminal/
│   ├── BaxiTerminalClientTest.kt          (NY - unit tests)
│   └── MockBaxiTerminalClientTest.kt      (NY - mock tests)
│
└── src/main/resources/
    └── application.yaml                   (OPPDATER)
```

---

## VERIFISERING

1. **Kompilering**: `mvn clean install` - skal kompilere uten feil
2. **Unit tests**: `mvn test` - alle tester skal være grønne
3. **Start med mock**: 
   ```bash
   java -jar target/lpg-ehl-service.jar \
     --payment.terminal.enabled=true \
     --payment.terminal.implementation=mock
   ```
4. **Test med ekte terminal** (krever fysisk hardware):
   ```bash
   java -jar target/lpg-ehl-service.jar \
     --payment.terminal.enabled=true \
     --payment.terminal.implementation=baxi \
     --payment.terminal.baxi.host=192.168.1.100 \
     --payment.terminal.baxi.port=7200
   ```

---

## SPØRSMÅL TIL DEG

Før du starter implementasjonen, avklar:

1. **Timeout-verdier**: Er 30 sek for `openTerminal()` og 60 sek for `purchase()` OK?
2. **Error-håndtering**: Skal `onError()` callback kaste exception eller returnere error-response?
3. **PreAvstemming**: Skal `TerminalPurchaseRequest.preAvstemming` støttes? (Krever `administration()` før `transferAmount()`)
4. **Logging**: Skal vi logge alle callbacks eller bare errors?
5. **Retry**: Skal det være automatisk retry ved midlertidige feil?
6. **Display/Print text**: Skal vi akkumulere alle `onDisplayText()` og `onPrintText()` callbacks, eller bare siste?

---

## EKSTRA OPPGAVER (VALGFRITT)

### A. Støtte for PreAvstemming

Hvis `request.preAvstemming.enabled == true`:
```kotlin
// 1. Kjør avstemming først
baxiClient.administration(AdministrationArgs(
    admCode = 9000,  // Avstemming
    operId = request.preAvstemming.password
))
// Vent på completion

// 2. Deretter kjør purchase
baxiClient.transferAmount(...)
```

### B. Metrics og observability

Legg til Micrometer metrics:
```kotlin
@Component
class BaxiTerminalClientMetrics(private val registry: MeterRegistry) {
    
    private val purchaseTimer = registry.timer("terminal.purchase")
    private val purchaseSuccessCounter = registry.counter("terminal.purchase.success")
    private val purchaseFailureCounter = registry.counter("terminal.purchase.failure")
    
    fun recordPurchase(durationMs: Long, success: Boolean) {
        purchaseTimer.record(durationMs, TimeUnit.MILLISECONDS)
        if (success) purchaseSuccessCounter.increment()
        else purchaseFailureCounter.increment()
    }
}
```

### C. Circuit breaker

Bruk Resilience4j for å unngå å bombardere en defekt terminal:
```kotlin
@CircuitBreaker(name = "baxiTerminal", fallbackMethod = "purchaseFallback")
override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
    // ... normal implementation
}

fun purchaseFallback(request: TerminalPurchaseRequest, ex: Exception): TerminalOperationResponse {
    log.error("Circuit breaker triggered for purchase", ex)
    return TerminalOperationResponse(
        success = false,
        error = "Terminal temporarily unavailable",
        errorCode = "circuit_open"
    )
}
```

---

## RESSURSER

- **Baxi-kotlin source**: `/Users/tandersen/git/NorgesGass/BaxiExperiments/physically-connected-to-ethernet-experiments/baxi-kotlin`
- **LPG Service**: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service`
- **Eksisterende SimulatedTerminalClient**: God referanse for struktur
- **WireMock test**: Se `TerminalClientWireMockTest.kt` for test-patterns

```

Denne prompten gir AI-assistenten all nødvendig kontekst for å implementere integrasjonen korrekt. Kopier hele blokken ovenfor og lim inn i Cursor eller IntelliJ AI Chat.

---

## Bruksinstruksjoner

1. **Kopier prompten** fra seksjonen "Prompt for Cursor/IntelliJ AI" ovenfor
2. **Lim inn i AI-chat** (Cursor eller IntelliJ med AI plugin)
3. **AI vil generere**:
   - `BaxiTerminalClient.kt`
   - `MockBaxiTerminalClient.kt`
   - Oppdatert `TerminalConfiguration.kt`
   - Unit tests
   - YAML-konfigurasjon
4. **Gjennomgå koden** før du committer
5. **Kjør tester**: `mvn clean test`
6. **Test med mock**: Start applikasjon med `payment.terminal.implementation=mock`
7. **Test med ekte terminal** når du er klar

## Vedlikehold

- **Versjonering**: Når `baxi-kotlin` oppdateres, sjekk om API-endringer påvirker mappingen
- **Logging**: Bruk DEBUG-nivå for callbacks, INFO for operasjoner, ERROR for feil
- **Monitoring**: Legg til metrics for purchase success rate og duration
