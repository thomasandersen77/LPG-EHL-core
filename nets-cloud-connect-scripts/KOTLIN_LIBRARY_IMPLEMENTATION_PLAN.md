# Nets Cloud Connect - Kotlin Library Implementation Plan

**Dato:** 2026-02-15  
**Forfatter:** WARP  
**Status:** Anbefaling

---

## 📋 Oversikt

Denne planen beskriver hvordan du implementerer Nets Cloud Connect som et **Kotlin-bibliotek** integrert direkte i **lpg-ehl-service** (mono-serveren), basert på de vellykkede test-scriptene i `nets-cloud-connect-scripts/`.

### 🎯 Målsetning

✅ Implementer Nets Cloud Connect som et Kotlin-bibliotek (ikke separat server)  
✅ Integrer med eksisterende `TerminalClient`-interface  
✅ Bruk eksisterende OpenAPI-spesifikasjoner  
✅ Følg Clean Architecture / Ports & Adapters  
✅ Gjenbruk koden fra test-scriptene som fungerer mot production  

---

## 🏗️ Arkitektur

### Nåværende Situasjon

```
┌─────────────────────────────────────────────────────────────┐
│  lpg-ehl-service (Kotlin + Spring Boot)                     │
│                                                              │
│  ┌──────────────────────────────────┐                       │
│  │ TerminalClient (interface)       │                       │
│  │                                  │                       │
│  │  - openTerminal()                │                       │
│  │  - purchase(request)             │                       │
│  │  - reversal()                    │                       │
│  │  - closeTerminal()               │                       │
│  └──────────────────────────────────┘                       │
│                                                              │
│  Implementasjoner:                                           │
│  - BaxiTerminalClient (JNI → norgesgass-baxi-client JAR)   │
│  - MockPaymentGateway (test)                                │
│  - SimulatedPaymentGateway (test)                           │
└─────────────────────────────────────────────────────────────┘
```

### Anbefalt Løsning: NetsCloudConnectTerminalClient

```
┌──────────────────────────────────────────────────────────────────┐
│  lpg-ehl-service (Kotlin + Spring Boot)                          │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ TerminalClient (interface)                                  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                      ▲                                            │
│                      │ implements                                 │
│  ┌───────────────────┴──────────────────────────────────────┐   │
│  │ NetsCloudConnectTerminalClient                            │   │
│  │                                                            │   │
│  │  Uses:                                                     │   │
│  │  - NetsCloudAuthClient (HTTP login → JWT)                 │   │
│  │  - NetsCloudWebSocketClient (WSS with bearer token)       │   │
│  │  - NetsMessageBuilder (XML/JSON message creation)         │   │
│  │  - NetsResponseParser (parse Dfs13* responses)            │   │
│  └────────────────────────────────────────────────────────────┘  │
│                      │                                            │
│                      │ Ktor HTTP + WebSockets                     │
│                      ▼                                            │
│              [Nets Cloud Connect]                                 │
│         https://connectcloud.aws.nets.eu                          │
│         wss://connectcloud.aws.nets.eu/ws/json                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📦 Modul-struktur

### Anbefalt Plassering

**Alternativ 1: Ny modul (anbefalt)**
```
lpg-ehl/
├── lpg-ehl-core/
├── lpg-ehl-service/
├── lpg-nets-cloud-connect/  ← NY MODUL (bibliotek)
│   ├── src/main/kotlin/
│   │   └── no/cloudberries/lpg/netscloud/
│   │       ├── NetsCloudConnectTerminalClient.kt  (implements TerminalClient)
│   │       ├── NetsCloudAuthClient.kt             (HTTP login)
│   │       ├── NetsCloudWebSocketClient.kt        (WSS + heartbeat)
│   │       ├── NetsMessageBuilder.kt              (XML/JSON builder)
│   │       ├── NetsResponseParser.kt              (parse Dfs13* XML)
│   │       └── models/
│   │           ├── NetsRequest.kt
│   │           ├── NetsResponse.kt
│   │           └── Dfs13Models.kt
│   └── pom.xml
│
└── lpg-ehl-service/
    └── pom.xml  (add dependency: lpg-nets-cloud-connect)
```

**Alternativ 2: Direkte i lpg-ehl-service**
```
lpg-ehl-service/
└── src/main/kotlin/
    └── no/cloudberries/lpg/service/
        └── terminal/
            ├── TerminalClient.kt (interface - eksisterer allerede)
            ├── BaxiTerminalClient.kt (eksisterende)
            └── netscloud/  ← NY PAKKE
                ├── NetsCloudConnectTerminalClient.kt
                ├── NetsCloudAuthClient.kt
                ├── NetsCloudWebSocketClient.kt
                └── ... (resten)
```

**🎯 Anbefaling:** Bruk **Alternativ 1** (ny modul) fordi:
- ✅ Klar separasjon av ansvar
- ✅ Kan testes uavhengig
- ✅ Enklere å vedlikeholde
- ✅ Kan gjenbrukes i andre prosjekter (f.eks. MinLPG)

---

## 🧩 Komponenter

### 1. NetsCloudConnectTerminalClient

**Ansvar:** Implementerer `TerminalClient`-interface med Nets Cloud Connect

**Viktigste metoder:**
```kotlin
@Component
@ConditionalOnProperty(name = ["terminal.provider"], havingValue = "nets-cloud-connect")
class NetsCloudConnectTerminalClient(
    private val config: NetsCloudConnectConfig,
    private val authClient: NetsCloudAuthClient,
    private val webSocketClient: NetsCloudWebSocketClient,
    private val messageBuilder: NetsMessageBuilder,
    private val responseParser: NetsResponseParser
) : TerminalClient {
    
    override fun openTerminal(): TerminalSimpleResponse {
        // 1. Login (HTTP) → JWT token
        // 2. Connect WebSocket
        // 3. Send Open command → wait for TerminalReady
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        // 1. Build Dfs13TransferAmount XML/JSON
        // 2. Send via WebSocket
        // 3. Wait for Dfs13LocalMode response
        // 4. Accumulate Dfs13DisplayText and Dfs13PrintText
        // 5. Return TerminalOperationResponse
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse {
        // Send Dfs13Reversal command
    }
    
    override fun closeTerminal(): TerminalSimpleResponse {
        // Close WebSocket gracefully
    }
    
    override fun getHealth(): TerminalHealthResponse
    override fun getStatus(): TerminalStatusResponse
}
```

---

### 2. NetsCloudAuthClient

**Ansvar:** HTTP login og JWT token-håndtering

**Basert på:** `TestLogin.kt` (allerede testet og fungerer)

```kotlin
class NetsCloudAuthClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    suspend fun login(): NetsLoginResponse {
        val response = httpClient.post("$baseUrl/v1/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "username" to username,
                "password" to password
            ))
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw NetsCloudAuthException("Login failed: ${response.status}")
        }
        
        return response.body<NetsLoginResponse>()
    }
}

data class NetsLoginResponse(
    val token: String,
    val username: String,
    val terminals: List<String>
)
```

---

### 3. NetsCloudWebSocketClient

**Ansvar:** WebSocket-tilkobling, sending og mottak av meldinger

**Basert på:** `TestWebSocket.kt` og `TestCompleteFlow.kt`

```kotlin
class NetsCloudWebSocketClient(
    private val baseUrl: String,
    private val token: String
) {
    private val httpClient = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000 // 20 sekunder
        }
    }
    
    private var session: DefaultClientWebSocketSession? = null
    private val messageListeners = mutableListOf<(String) -> Unit>()
    
    suspend fun connect() {
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        
        httpClient.webSocket(
            request = {
                url.takeFrom(wsUrl)
                headers.append(HttpHeaders.Authorization, "bearer $token")
            }
        ) {
            session = this
            
            // Start listening for incoming messages
            launch { listenForMessages() }
            
            // Keep session alive
            while (isActive) {
                delay(1000)
            }
        }
    }
    
    private suspend fun listenForMessages() {
        for (frame in session!!.incoming) {
            when (frame) {
                is Frame.Text -> {
                    val message = frame.readText()
                    notifyListeners(message)
                }
                is Frame.Binary -> {
                    val message = frame.readBytes().decodeToString()
                    notifyListeners(message)
                }
                is Frame.Close -> {
                    logger.info("WebSocket closed: ${frame.readReason()}")
                    break
                }
                else -> { /* Ignore Ping/Pong */ }
            }
        }
    }
    
    suspend fun sendMessage(json: String) {
        session?.send(Frame.Text(json))
            ?: throw IllegalStateException("WebSocket not connected")
    }
    
    fun addMessageListener(listener: (String) -> Unit) {
        messageListeners.add(listener)
    }
    
    private fun notifyListeners(message: String) {
        messageListeners.forEach { it(message) }
    }
}
```

---

### 4. NetsMessageBuilder

**Ansvar:** Bygge XML/JSON-meldinger for Nets Cloud Connect

```kotlin
class NetsMessageBuilder(
    private val terminalId: String
) {
    
    fun buildOpenRequest(): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "$terminalId",
                "VersionNumber": "1"
              }
            },
            "Open": {}
          }
        }
        """.trimIndent()
    }
    
    fun buildPurchaseRequest(
        amountMinor: Int,
        operatorId: String = "0000"
    ): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "$terminalId",
                "VersionNumber": "1"
              }
            },
            "Dfs13TransferAmount": {
              "TransactionType": "48",
              "OperId": "$operatorId",
              "Amount1": "$amountMinor",
              "Amount2": "0",
              "Amount3": "0",
              "Type2": "48",
              "Type3": "48",
              "HostData": "",
              "OptionalData": ""
            }
          }
        }
        """.trimIndent()
    }
    
    fun buildReversalRequest(): String {
        val ecrId = generateEcrId()
        return """
        {
          "NetsRequest": {
            "MessageHeader": {
              "$": {
                "ECRID": "$ecrId",
                "TerminalID": "$terminalId",
                "VersionNumber": "1"
              }
            },
            "Dfs13Reversal": {}
          }
        }
        """.trimIndent()
    }
    
    private fun generateEcrId(): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "POS-$timestamp-$suffix"
    }
}
```

---

### 5. NetsResponseParser

**Ansvar:** Parse XML/JSON-responser fra Nets Cloud Connect

```kotlin
class NetsResponseParser {
    
    fun isTerminalReady(message: String): Boolean =
        message.contains("Dfs13TerminalReady") || message.contains("ALREADY_OPEN")
    
    fun isTransactionComplete(message: String): Boolean =
        message.contains("Dfs13LocalMode")
    
    fun parseLocalMode(message: String): Dfs13LocalMode? {
        // Parse Result, TotalAmount, ResponseCode, etc.
        val resultMatch = Regex(""""Result"\s*:\s*"(\d+)"""").find(message)
        val amountMatch = Regex(""""TotalAmount"\s*:\s*"(\d+)"""").find(message)
        val responseCodeMatch = Regex(""""ResponseCode"\s*:\s*"(\w+)"""").find(message)
        
        return if (resultMatch != null) {
            Dfs13LocalMode(
                result = resultMatch.groupValues[1].toInt(),
                totalAmount = amountMatch?.groupValues?.get(1)?.toLongOrNull(),
                responseCode = responseCodeMatch?.groupValues?.get(1)
            )
        } else null
    }
    
    fun parseDisplayText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)?.replace("\\r", "")?.replace("\\n", "")
    }
    
    fun parsePrintText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
    }
}

data class Dfs13LocalMode(
    val result: Int,         // 1 = Godkjent, 2 = Avvist
    val totalAmount: Long?,
    val responseCode: String?
)
```

---

## 🔧 Konfigurasjon

### application.yaml

```yaml
terminal:
  provider: nets-cloud-connect  # eller "baxi" for legacy

nets-cloud-connect:
  base-url: https://connectcloud.aws.nets.eu
  username: ${NETS_USERNAME:cloudberries_shared}
  password: ${NETS_PASSWORD}  # Fra miljøvariabel
  terminal-id: ${NETS_TERMINAL_ID}  # Fra miljøvariabel
  
  websocket:
    ping-interval-ms: 20000
    reconnect-delay-ms: 5000
    max-reconnect-attempts: 10
    
  timeouts:
    login-timeout-ms: 10000
    open-terminal-timeout-ms: 30000
    purchase-timeout-ms: 120000  # 2 minutter for å tappe kort
    reversal-timeout-ms: 60000
```

### Spring Boot Configuration

```kotlin
@Configuration
@ConfigurationProperties(prefix = "nets-cloud-connect")
data class NetsCloudConnectConfig(
    val baseUrl: String = "https://connectcloud.aws.nets.eu",
    val username: String = "",
    val password: String = "",
    val terminalId: String = "",
    val websocket: WebSocketConfig = WebSocketConfig(),
    val timeouts: TimeoutConfig = TimeoutConfig()
)

data class WebSocketConfig(
    val pingIntervalMs: Long = 20000,
    val reconnectDelayMs: Long = 5000,
    val maxReconnectAttempts: Int = 10
)

data class TimeoutConfig(
    val loginTimeoutMs: Long = 10000,
    val openTerminalTimeoutMs: Long = 30000,
    val purchaseTimeoutMs: Long = 120000,
    val reversalTimeoutMs: Long = 60000
)
```

---

## 📝 OpenAPI Spesifikasjon

Du nevnte at du ønsker å bruke OpenAPI-spesifikasjonen for terminalen. Her er hvordan du integrerer:

### Eksisterende OpenAPI Endpoint (lpg-ehl-service)

```yaml
# docs/openapi.yaml eller lpg-web/openapi.yaml

paths:
  /api/v1/terminal/open:
    post:
      summary: Open payment terminal
      operationId: openTerminal
      responses:
        '200':
          description: Terminal opened successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalSimpleResponse'
  
  /api/v1/terminal/purchase:
    post:
      summary: Perform card payment
      operationId: purchase
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TerminalPurchaseRequest'
      responses:
        '200':
          description: Purchase completed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TerminalOperationResponse'

components:
  schemas:
    TerminalPurchaseRequest:
      type: object
      required:
        - amountMinor
      properties:
        amountMinor:
          type: integer
          description: Amount in øre (minor units)
          example: 100
        operatorId:
          type: string
          default: "0000"
        currency:
          type: string
          default: "NOK"
          
    TerminalOperationResponse:
      type: object
      properties:
        success:
          type: boolean
        operationId:
          type: string
        callResult:
          type: integer
        responseCode:
          type: string
        printTextSanitized:
          type: string
        error:
          type: string
```

### REST Controller

```kotlin
@RestController
@RequestMapping("/api/v1/terminal")
class TerminalController(
    private val terminalClient: TerminalClient  // Spring vil auto-wire riktig implementasjon
) {
    
    @PostMapping("/open")
    fun openTerminal(): TerminalSimpleResponse {
        return terminalClient.openTerminal()
    }
    
    @PostMapping("/purchase")
    fun purchase(@RequestBody request: TerminalPurchaseRequest): TerminalOperationResponse {
        return terminalClient.purchase(request)
    }
    
    @PostMapping("/reversal")
    fun reversal(@RequestParam operationId: String?): TerminalOperationResponse {
        return terminalClient.reversal(operationId)
    }
    
    @PostMapping("/close")
    fun closeTerminal(): TerminalSimpleResponse {
        return terminalClient.closeTerminal()
    }
    
    @GetMapping("/status")
    fun getStatus(): TerminalStatusResponse {
        return terminalClient.getStatus()
    }
    
    @GetMapping("/health")
    fun getHealth(): TerminalHealthResponse {
        return terminalClient.getHealth()
    }
}
```

---

## 🧪 Testing

### Unit Tests

```kotlin
@Test
fun `should login and get JWT token`() = runBlocking {
    val config = NetsCloudConnectConfig(
        baseUrl = "https://connectcloud.aws.nets.eu",
        username = "cloudberries_shared",
        password = System.getenv("NETS_PASSWORD")
    )
    
    val authClient = NetsCloudAuthClient(
        config.baseUrl,
        config.username,
        config.password
    )
    
    val response = authClient.login()
    
    assertThat(response.token).isNotEmpty()
    assertThat(response.terminals).isNotEmpty()
}
```

### Integration Tests

```kotlin
@SpringBootTest
@TestPropertySource(properties = [
    "terminal.provider=nets-cloud-connect",
    "nets-cloud-connect.username=cloudberries_shared",
    "nets-cloud-connect.password=\${NETS_PASSWORD}"
])
class NetsCloudConnectIntegrationTest {
    
    @Autowired
    lateinit var terminalClient: TerminalClient
    
    @Test
    fun `complete flow - open, purchase, close`() {
        // Open terminal
        val openResult = terminalClient.openTerminal()
        assertThat(openResult.success).isTrue()
        
        // Purchase
        val purchaseRequest = TerminalPurchaseRequest(
            amountMinor = 100,  // 1 krone
            operatorId = "0001"
        )
        val purchaseResult = terminalClient.purchase(purchaseRequest)
        assertThat(purchaseResult.success).isTrue()
        
        // Close
        val closeResult = terminalClient.closeTerminal()
        assertThat(closeResult.success).isTrue()
    }
}
```

---

## 📋 Implementasjons-sjekkliste

### Fase 1: Grunnleggende struktur ✅
- [ ] Opprett ny modul: `lpg-nets-cloud-connect`
- [ ] Kopier relevante testscripts til ny modul (som referanse)
- [ ] Legg til Ktor dependencies i pom.xml
- [ ] Definer konfigurasjon (NetsCloudConnectConfig)

### Fase 2: Auth og WebSocket 🔄
- [ ] Implementer `NetsCloudAuthClient` (basert på TestLogin.kt)
- [ ] Implementer `NetsCloudWebSocketClient` (basert på TestWebSocket.kt)
- [ ] Skriv unit tests for login
- [ ] Skriv unit tests for WebSocket-tilkobling

### Fase 3: Message Building og Parsing 🔄
- [ ] Implementer `NetsMessageBuilder`
- [ ] Implementer `NetsResponseParser`
- [ ] Skriv unit tests for message building
- [ ] Skriv unit tests for response parsing

### Fase 4: TerminalClient Implementasjon 🔄
- [ ] Implementer `NetsCloudConnectTerminalClient`
- [ ] Implementer `openTerminal()` (Open → TerminalReady)
- [ ] Implementer `purchase()` (TransferAmount → LocalMode)
- [ ] Implementer `reversal()`
- [ ] Implementer `closeTerminal()`
- [ ] Implementer `getHealth()` og `getStatus()`

### Fase 5: Integrasjon med lpg-ehl-service 🔄
- [ ] Legg til dependency i lpg-ehl-service/pom.xml
- [ ] Konfigurer Spring Boot auto-configuration
- [ ] Test med eksisterende REST API
- [ ] Verifiser at OpenAPI-spesifikasjonen fortsatt er gyldig

### Fase 6: Testing og Validering ✅
- [ ] Skriv integration tests
- [ ] Test mot production (1 krone test-kjøp)
- [ ] Verifiser feilhåndtering
- [ ] Verifiser reconnect-logikk
- [ ] Lag performance tests (latency, timeout)

### Fase 7: Dokumentasjon 📝
- [ ] Oppdater README.md
- [ ] Lag TROUBLESHOOTING.md
- [ ] Lag MIGRATION_GUIDE.md (fra BaxiTerminalClient)
- [ ] Oppdater OpenAPI-dokumentasjon

---

## 🎯 Fordeler med denne løsningen

✅ **Ingen separat server:** Alt kjører i lpg-ehl-service  
✅ **Samme API:** Gjenbruker eksisterende `TerminalClient`-interface  
✅ **Testet kode:** Basert på scripts som allerede fungerer mot production  
✅ **Spring Boot native:** Ingen ekstra Docker-containere eller .NET runtime  
✅ **Type-safe:** Full Kotlin type safety  
✅ **Testbart:** Kan mockes og testes uavhengig  
✅ **OpenAPI-kompatibel:** Bruker eksisterende REST API-spesifikasjoner  

---

## ⚠️ Viktige hensyn

### 1. Secrets Management
**ALDRI hardcode credentials!**

```kotlin
// ❌ FEIL
val password = "Gf&DW*8-IN7Lx6pE"

// ✅ RIKTIG
val password = System.getenv("NETS_PASSWORD")
    ?: throw IllegalStateException("NETS_PASSWORD not set")
```

### 2. Timeout-håndtering
Bruk forskjellige timeouts for ulike operasjoner:
- Login: 10 sekunder
- Open: 30 sekunder
- Purchase: 120 sekunder (bruker må tappe kort)
- Reversal: 60 sekunder

### 3. Reconnect-logikk
WebSocket kan koble fra - implementer automatisk reconnect:
```kotlin
var reconnectAttempts = 0
while (reconnectAttempts < maxReconnectAttempts) {
    try {
        connect()
        break
    } catch (e: Exception) {
        reconnectAttempts++
        delay(reconnectDelayMs)
    }
}
```

### 4. Concurrency
Kun én terminal-operasjon om gangen:
```kotlin
private val operationLock = Mutex()

suspend fun purchase(request: TerminalPurchaseRequest) = operationLock.withLock {
    // Perform purchase
}
```

---

## 🔗 Relaterte Dokumenter

- `nets-cloud-connect-scripts/README.md` - Test-scripts
- `lpg-ehl-service/README_Service.md` - Service-dokumentasjon
- `lpg-ehl-core/WARP.md` - Core-dokumentasjon
- `docs/openapi.yaml` - OpenAPI-spesifikasjon

---

## 🚀 Neste Steg

1. **Lag ny modul:** `lpg-nets-cloud-connect`
2. **Kopier testscripts som referanse**
3. **Implementer fase 1-2** (Auth + WebSocket)
4. **Test mot production** (1 krone test-kjøp)
5. **Integrer med lpg-ehl-service**

**Estimert tid:** 3-5 dager for komplett implementasjon

---

**Sist oppdatert:** 2026-02-15  
**Status:** Klar for implementering 🚀
