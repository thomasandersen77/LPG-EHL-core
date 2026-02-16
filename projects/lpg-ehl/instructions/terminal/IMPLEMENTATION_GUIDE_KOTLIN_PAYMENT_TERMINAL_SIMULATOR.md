# Implementeringsguide: Kotlin Payment Terminal Simulator & WireMock Integration

**Versjon:** 1.0  
**Dato:** 9. februar 2026  
**Formål:** Komplett guide for å implementere en Kotlin-basert payment terminal simulator som erstatning for PaymentTerminalMonoServer (C# + Mono + Baxi DLL)

---

## Innholdsfortegnelse

1. [Executive Summary](#executive-summary)
2. [Prosjektforståelse](#prosjektforståelse)
3. [Arkitekturell Oversikt](#arkitekturell-oversikt)
4. [Implementeringsstrategi](#implementeringsstrategi)
5. [API Kontrakt Referanse](#api-kontrakt-referanse)
6. [Kotlin Implementering: Steg-for-Steg](#kotlin-implementering-steg-for-steg)
7. [WireMock Integrasjon](#wiremock-integrasjon)
8. [Testing Strategi](#testing-strategi)
9. [Deployment](#deployment)
10. [ChatGPT Vurdering](#chatgpt-vurdering)

---

## Executive Summary

### Dagens Situasjon

Du har bygget **PaymentTerminalMonoServer** - en C# .NET HTTP server som:
- Kjører på Mono (Linux kompatibel)
- Laster Baxi `baxi_dotnet.dll` dynamisk via reflection
- Snakker med fysisk betalingsterminal via vendor DLL
- Eksponerer REST API for klientapplikasjoner
- Håndterer én operation om gangen (single terminal, single operation)
- Persisterer receipts og events i SQLite

**Problem:** Systemet krever:
- Mono runtime
- C# kompetanse
- Vendor DLL (ikke tilgjengelig i alle miljøer)
- Fysisk terminal for testing

### Løsning: Kotlin Simulator

**Mål:** Bygge en **ren Kotlin-basert simulator** som:

✅ **Erstatter** C# Mono-serveren for testing/utvikling  
✅ **Kjører** på JVM (samme stack som resten av systemet)  
✅ **Bruker** WireMock for deterministisk testing  
✅ **Gjenskaper** samme HTTP API kontrakt  
✅ **Simulerer** terminal oppførsel (busy, timeout, wrong PIN, osv.)  
✅ **Ingen DLL** - ren JVM-implementering  

### Hvorfor Dette Er En God Idé (ChatGPT Vurdering)

> "Ja, veldig. Du får:
> - Reproduserbare tester i CI uten terminal/Mono-server
> - Mulighet til å simulere 'BUSY / NOT_READY / TIMEOUT' deterministisk
> - Et 'lab mode' oppsett der du kan peke systemet mot en sim-terminal uten å endre domenelogikk."

---

## Prosjektforståelse

### Dagens Arkitektur

```
┌─────────────────────────────────────────────────┐
│     Legacy VB6 App (Pumpekontroll)              │
│     - DirectX BaxiCtrl                          │
│     - TransferAmount_V2, Administration         │
│     - OnLocalMode, OnPrintText events           │
└─────────────────────────────────────────────────┘
                      ▼
         (Erstattes av HTTP API kall)
                      ▼
┌─────────────────────────────────────────────────┐
│     PaymentTerminalMonoServer (C#/Mono)         │
│     - BaxiAdapter (reflection wrapper)          │
│     - TerminalService (business logic)          │
│     - HttpServer (REST API)                     │
│     - SQLite persistence                        │
└─────────────────────────────────────────────────┘
                      ▼
         (Laster baxi_dotnet.dll)
                      ▼
┌─────────────────────────────────────────────────┐
│     Vendor DLL (baxi_dotnet.dll)                │
│     - BBS.BAXI.BaxiCtrl                         │
│     - Snakker med terminal over serial/TCP      │
└─────────────────────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────┐
│     Fysisk Baxi Terminal                        │
│     - NETS certified                            │
│     - Håndterer card/PIN                        │
└─────────────────────────────────────────────────┘
```

### API Kontrakt (HTTP REST)

**Base URL:** `http://127.0.0.1:8080`

**Kritiske Endpoints:**

| Endpoint | Method | Formål |
|----------|--------|--------|
| `/health` | GET | Server health check |
| `/v1/terminal/status` | GET | Terminal readiness |
| `/v1/terminal/open` | POST | Åpne terminal connection |
| `/v1/terminal/close` | POST | Lukk terminal connection |
| `/v1/payments/purchase` | POST | Purchase/prepay (0x30) |
| `/v1/payments/refund` | POST | Refund (0x31) |
| `/v1/payments/cashback` | POST | Purchase + cashback |
| `/v1/admin/avstemming` | POST | End-of-day reconciliation |
| `/v1/admin/cancel` | POST | Cancel current operation |
| `/v1/admin/reversal` | POST | Reverse last transaction |
| `/v1/admin/z-report` | POST | Z-report |
| `/v1/events/stream` | GET | SSE event stream |
| `/v1/events?since=X` | GET | Polling event feed |

**Viktig JSON Casing:**
- `/health` returnerer **lowercase** keys (`status`, `timestamp`, `configLoaded`)
- Alle andre endpoints bruker **PascalCase** (`Success`, `OperationId`, `AmountMinor`, osv.)

---

## Arkitekturell Oversikt

### Ny Kotlin Simulator Arkitektur

```
┌──────────────────────────────────────────────────────┐
│          Kotlin Payment Terminal Simulator           │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │  PaymentTerminalClient (Kotlin)                │ │
│  │  - Snakker med simulator over HTTP             │ │
│  │  - Samme API kontrakt som Mono-server          │ │
│  └────────────────────────────────────────────────┘ │
│                        │                             │
│                        ▼                             │
│  ┌────────────────────────────────────────────────┐ │
│  │  WireMock Server (embedded eller standalone)   │ │
│  │  - Scenario-basert simulering                  │ │
│  │  - JSON response mappings                      │ │
│  │  - Stateful interactions (busy/ready)          │ │
│  │  - Idempotency support                         │ │
│  └────────────────────────────────────────────────┘ │
│                        │                             │
│                        ▼                             │
│  ┌────────────────────────────────────────────────┐ │
│  │  Terminal Behavior Simulator                   │ │
│  │  - Approved/Declined/Wrong PIN logic           │ │
│  │  - Timeout simulation                          │ │
│  │  - Receipt generation                          │ │
│  │  - Event stream (SSE)                          │ │
│  └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

### Implementeringslag

1. **PaymentTerminalClient (Kotlin)**
   - HTTP klient med OkHttp/Ktor
   - DTO klasser (matches API kontrakt)
   - Idempotency support
   - Timeout/retry logikk

2. **WireMock Integration**
   - Embedded WireMock server
   - JSON mappings for hver endpoint
   - Scenario state machine (READY → BUSY → READY)

3. **Simulator Logic**
   - Terminal state management
   - Operation orchestration
   - Receipt/report generation
   - Event streaming (SSE)

---

## Implementeringsstrategi

### Fase 1: Minimal Viable Simulator (MVP)

**Mål:** Få opp en kjørbar simulator som håndterer happy path

**Scope:**
- ✅ `/health`
- ✅ `/v1/terminal/status`
- ✅ `/v1/terminal/open` / `close`
- ✅ `/v1/payments/purchase` (approved path)
- ✅ Basic WireMock mappings

**Ikke scope (Fase 1):**
- ❌ SSE events
- ❌ Stateful idempotency
- ❌ Admin operations
- ❌ Receipt parsing/formatting

---

### Fase 2: Edge Cases & Robustness

**Mål:** Simulere realistiske feilsituasjoner

**Scope:**
- ✅ `terminal_busy` (409)
- ✅ `terminal_not_ready` (503)
- ✅ `operation_timeout` (408)
- ✅ Wrong PIN rejection
- ✅ User cancel simulation
- ✅ Idempotency (clientRequestId)

---

### Fase 3: Full Feature Parity

**Mål:** Match PaymentTerminalMonoServer funksjonalitet

**Scope:**
- ✅ Admin endpoints (avstemming, z-report, reversal, osv.)
- ✅ SSE event stream
- ✅ Receipt storage
- ✅ Report parsing
- ✅ Persistence (in-memory eller SQLite)

---

## API Kontrakt Referanse

### Request/Response Eksempler

#### 1. Health Check

```http
GET /health
```

**Response (200 OK):**
```json
{
  "status": "ok",
  "timestamp": "2026-02-09T12:00:00Z",
  "configLoaded": true
}
```

**OBS:** Lowercase keys! (eneste endpoint med dette)

---

#### 2. Terminal Status

```http
GET /v1/terminal/status
```

**Response (200 OK):**
```json
{
  "VendorDllLoadable": true,
  "TerminalOpen": true,
  "TerminalReady": true,
  "LastError": null,
  "TerminalIdentity": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234"
  }
}
```

---

#### 3. Purchase (Happy Path)

```http
POST /v1/payments/purchase
Content-Type: application/json

{
  "AmountMinor": 10000,
  "Currency": "NOK",
  "OperatorId": "0000",
  "OptionalData": "LPG Autogas",
  "ClientRequestId": "fueling-session-abc123",
  "PreAvstemming": {
    "Enabled": false,
    "Password": "0000",
    "TimeoutSeconds": 300
  }
}
```

**Response (200 OK):**
```json
{
  "Success": true,
  "OperationId": "op-uuid-12345",
  "StartedAt": "2026-02-09T12:00:00Z",
  "CompletedAt": "2026-02-09T12:00:05Z",
  "DurationMs": 5000,
  "CallResult": 1,
  "MethodRejectCode": 0,
  "MethodRejectInfo": null,
  "ResultEventName": "OnLocalMode",
  "LocalModeResult": 0,
  "ResponseCode": "00",
  "RejectionSource": "0",
  "RejectionReason": null,
  "LocalModeFields": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234",
    "TotalAmount": "10000"
  },
  "PrintTextRaw": "NETS AS\nTRANSAKSJON GODKJENT\nBeløp: NOK 100.00\n...",
  "PrintTextSanitized": "NETS AS\nTRANSAKSJON GODKJENT\nBeløp: NOK 100.00\n...",
  "LastDisplayText": "GODKJENT",
  "Error": null,
  "ErrorCode": null,
  "DbRowId": 1,
  "ReceiptFileId": "2026-02-09/op-uuid-12345"
}
```

---

#### 4. Purchase (Terminal Busy)

```http
POST /v1/payments/purchase
```

**Response (409 Conflict):**
```json
{
  "Error": "Terminal is busy with another operation",
  "ErrorCode": "terminal_busy",
  "OperationId": "current-op-uuid",
  "Details": "Wait for current operation to complete"
}
```

---

#### 5. Purchase (Terminal Not Ready)

```http
POST /v1/payments/purchase
```

**Response (503 Service Unavailable):**
```json
{
  "Error": "Terminal is not ready",
  "ErrorCode": "terminal_not_ready",
  "OperationId": null,
  "Details": "Call POST /v1/terminal/open first"
}
```

---

#### 6. Purchase (Wrong PIN)

**Response (200 OK men Success=false):**
```json
{
  "Success": false,
  "OperationId": "op-uuid-wrong-pin",
  "StartedAt": "2026-02-09T12:00:00Z",
  "CompletedAt": "2026-02-09T12:00:10Z",
  "DurationMs": 10000,
  "CallResult": 1,
  "MethodRejectCode": 0,
  "MethodRejectInfo": null,
  "ResultEventName": "OnLocalMode",
  "LocalModeResult": 2,
  "ResponseCode": "Z1",
  "RejectionSource": "3",
  "RejectionReason": "3:2:Z1",
  "LocalModeFields": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234"
  },
  "PrintTextRaw": "NETS AS\nPIN FEIL\n...",
  "PrintTextSanitized": "NETS AS\nPIN FEIL\n...",
  "LastDisplayText": "PIN FEIL",
  "Error": null,
  "ErrorCode": null,
  "DbRowId": 2,
  "ReceiptFileId": "2026-02-09/op-uuid-wrong-pin"
}
```

**Kjennetegn for Wrong PIN:**
- `LocalModeResult = 2`
- `ResponseCode = "Z1"`
- `RejectionSource = "3"`
- `RejectionReason` inneholder `"3:2:Z1"`

---

#### 7. Purchase (User Cancel)

**Response (200 OK men Success=false):**
```json
{
  "Success": false,
  "LocalModeResult": 2,
  "ResponseCode": "",
  "RejectionSource": "0",
  "RejectionReason": "2:1",
  "LastDisplayText": "AVBRUTT"
}
```

**Kjennetegn for User Cancel:**
- `LocalModeResult = 2`
- `ResponseCode` er blank eller "00"
- `RejectionReason` inneholder `"2:1"`

---

## Kotlin Implementering: Steg-for-Steg

### Prosjektstruktur

```
payment-terminal-simulator/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── no/norgesgass/payment/
│   │           ├── client/
│   │           │   ├── PaymentTerminalClient.kt
│   │           │   ├── dto/
│   │           │   │   ├── Requests.kt
│   │           │   │   ├── Responses.kt
│   │           │   │   └── ErrorResponse.kt
│   │           │   └── exceptions/
│   │           │       └── TerminalException.kt
│   │           │
│   │           ├── simulator/
│   │           │   ├── TerminalSimulatorApp.kt (main)
│   │           │   ├── TerminalStateManager.kt
│   │           │   ├── OperationSimulator.kt
│   │           │   ├── ReceiptGenerator.kt
│   │           │   └── scenarios/
│   │           │       ├── ApprovedScenario.kt
│   │           │       ├── DeclinedScenario.kt
│   │           │       ├── WrongPinScenario.kt
│   │           │       └── TimeoutScenario.kt
│   │           │
│   │           └── wiremock/
│   │               ├── WireMockSetup.kt
│   │               ├── mappings/
│   │               │   ├── HealthMappings.kt
│   │               │   ├── StatusMappings.kt
│   │               │   ├── LifecycleMappings.kt
│   │               │   └── PaymentMappings.kt
│   │               └── scenarios/
│   │                   └── ScenarioManager.kt
│   │
│   └── test/
│       └── kotlin/
│           └── no/norgesgass/payment/
│               ├── client/
│               │   └── PaymentTerminalClientTest.kt
│               └── simulator/
│                   ├── SimulatorIntegrationTest.kt
│                   └── ScenarioTest.kt
│
└── resources/
    └── wiremock/
        ├── mappings/
        │   ├── health.json
        │   ├── status.json
        │   ├── open.json
        │   ├── close.json
        │   ├── purchase-approved.json
        │   ├── purchase-declined.json
        │   └── purchase-wrong-pin.json
        └── __files/
            ├── receipt-approved.txt
            ├── receipt-declined.txt
            └── receipt-wrong-pin.txt
```

---

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "no.norgesgass"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // HTTP Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // WireMock
    implementation("org.wiremock:wiremock:3.3.1")
    
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // UUID
    implementation("com.benasher44:uuid:0.8.2")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("no.norgesgass.payment.simulator.TerminalSimulatorAppKt")
}

kotlin {
    jvmToolchain(21)
}
```

---

### 1. DTO Klasser (Requests.kt)

```kotlin
package no.norgesgass.payment.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseRequest(
    val AmountMinor: Int,
    val Currency: String = "NOK",
    val OperatorId: String = "4321",
    val OptionalData: String? = null,
    val PreAvstemming: PreAvstemmingConfig? = null,
    val ClientRequestId: String? = null
)

@Serializable
data class RefundRequest(
    val AmountMinor: Int,
    val Currency: String = "NOK",
    val OperatorId: String = "4321",
    val OptionalData: String? = null,
    val PreAvstemming: PreAvstemmingConfig? = null,
    val ClientRequestId: String? = null
)

@Serializable
data class CashbackRequest(
    val PurchaseMinor: Int,
    val CashbackMinor: Int,
    val Currency: String = "NOK",
    val OperatorId: String = "4321",
    val OptionalData: String? = null,
    val ClientRequestId: String? = null
)

@Serializable
data class AdminRequest(
    val Password: String = "0000"
)

@Serializable
data class PreAvstemmingConfig(
    val Enabled: Boolean = false,
    val Password: String = "0000",
    val TimeoutSeconds: Int? = null
)
```

---

### 2. DTO Klasser (Responses.kt)

```kotlin
package no.norgesgass.payment.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val configLoaded: Boolean
)

@Serializable
data class TerminalStatusResponse(
    val VendorDllLoadable: Boolean,
    val TerminalOpen: Boolean,
    val TerminalReady: Boolean,
    val LastError: String? = null,
    val TerminalIdentity: Map<String, String>? = null
)

@Serializable
data class OperationResponse(
    val Success: Boolean,
    val OperationId: String,
    val StartedAt: String,
    val CompletedAt: String? = null,
    val DurationMs: Int? = null,
    val CallResult: Int,
    val MethodRejectCode: Int = 0,
    val MethodRejectInfo: String? = null,
    val ResultEventName: String? = null,
    val LocalModeResult: Int = 0,
    val ResponseCode: String? = null,
    val RejectionSource: String? = null,
    val RejectionReason: String? = null,
    val LocalModeFields: Map<String, String>? = null,
    val PrintTextRaw: String? = null,
    val PrintTextSanitized: String? = null,
    val LastDisplayText: String? = null,
    val Error: String? = null,
    val ErrorCode: String? = null,
    val DbRowId: Long? = null,
    val ReceiptFileId: String? = null,
    val ReportFields: Map<String, String>? = null
)

@Serializable
data class SimpleResponse(
    val Success: Boolean,
    val Message: String? = null,
    val Error: String? = null
)

@Serializable
data class ErrorResponse(
    val Error: String,
    val ErrorCode: String,
    val OperationId: String? = null,
    val Details: String? = null
)
```

---

### 3. Payment Terminal Client (PaymentTerminalClient.kt)

```kotlin
package no.norgesgass.payment.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.norgesgass.payment.client.dto.*
import no.norgesgass.payment.client.exceptions.*

private val logger = KotlinLogging.logger {}

class PaymentTerminalClient(
    private val baseUrl: String = "http://127.0.0.1:8080"
) : AutoCloseable {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        expectSuccess = false // Handle errors manually
    }

    // Health & Status
    
    suspend fun health(): HealthResponse {
        logger.info { "Calling GET /health" }
        val response = httpClient.get("$baseUrl/health")
        return handleResponse(response)
    }

    suspend fun terminalStatus(): TerminalStatusResponse {
        logger.info { "Calling GET /v1/terminal/status" }
        val response = httpClient.get("$baseUrl/v1/terminal/status")
        return handleResponse(response)
    }

    // Lifecycle
    
    suspend fun openTerminal(): SimpleResponse {
        logger.info { "Calling POST /v1/terminal/open" }
        val response = httpClient.post("$baseUrl/v1/terminal/open")
        return handleResponse(response)
    }

    suspend fun closeTerminal(): SimpleResponse {
        logger.info { "Calling POST /v1/terminal/close" }
        val response = httpClient.post("$baseUrl/v1/terminal/close")
        return handleResponse(response)
    }

    // Financial Operations
    
    suspend fun purchase(request: PurchaseRequest): OperationResponse {
        logger.info { "Calling POST /v1/payments/purchase with amount=${request.AmountMinor}" }
        val response = httpClient.post("$baseUrl/v1/payments/purchase") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return handleOperationResponse(response)
    }

    suspend fun refund(request: RefundRequest): OperationResponse {
        logger.info { "Calling POST /v1/payments/refund with amount=${request.AmountMinor}" }
        val response = httpClient.post("$baseUrl/v1/payments/refund") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return handleOperationResponse(response)
    }

    suspend fun cashback(request: CashbackRequest): OperationResponse {
        logger.info { "Calling POST /v1/payments/cashback" }
        val response = httpClient.post("$baseUrl/v1/payments/cashback") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return handleOperationResponse(response)
    }

    // Admin Operations
    
    suspend fun avstemming(password: String = "0000"): OperationResponse {
        logger.info { "Calling POST /v1/admin/avstemming" }
        val response = httpClient.post("$baseUrl/v1/admin/avstemming") {
            contentType(ContentType.Application.Json)
            setBody(AdminRequest(password))
        }
        return handleOperationResponse(response)
    }

    suspend fun cancel(password: String = "0000"): OperationResponse {
        logger.info { "Calling POST /v1/admin/cancel" }
        val response = httpClient.post("$baseUrl/v1/admin/cancel") {
            contentType(ContentType.Application.Json)
            setBody(AdminRequest(password))
        }
        return handleOperationResponse(response)
    }

    suspend fun reversal(password: String = "0000"): OperationResponse {
        logger.info { "Calling POST /v1/admin/reversal" }
        val response = httpClient.post("$baseUrl/v1/admin/reversal") {
            contentType(ContentType.Application.Json)
            setBody(AdminRequest(password))
        }
        return handleOperationResponse(response)
    }

    suspend fun zReport(password: String = "0000"): OperationResponse {
        logger.info { "Calling POST /v1/admin/z-report" }
        val response = httpClient.post("$baseUrl/v1/admin/z-report") {
            contentType(ContentType.Application.Json)
            setBody(AdminRequest(password))
        }
        return handleOperationResponse(response)
    }

    // Error Handling
    
    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> {
                val errorBody = response.bodyAsText()
                logger.error { "HTTP ${response.status}: $errorBody" }
                throw TerminalException(
                    "HTTP ${response.status.value}",
                    "terminal_error",
                    errorBody
                )
            }
        }
    }

    private suspend fun handleOperationResponse(response: HttpResponse): OperationResponse {
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            
            HttpStatusCode.Conflict -> { // 409 terminal_busy
                val error: ErrorResponse = response.body()
                throw TerminalBusyException(error)
            }
            
            HttpStatusCode.ServiceUnavailable -> { // 503 terminal_not_ready
                val error: ErrorResponse = response.body()
                throw TerminalNotReadyException(error)
            }
            
            HttpStatusCode.RequestTimeout -> { // 408 operation_timeout
                val error: ErrorResponse = response.body()
                throw OperationTimeoutException(error)
            }
            
            HttpStatusCode.InternalServerError -> { // 500 vendor_call_failure
                val error: ErrorResponse = response.body()
                throw VendorCallFailureException(error)
            }
            
            else -> {
                val errorBody = response.bodyAsText()
                logger.error { "Unexpected HTTP ${response.status}: $errorBody" }
                throw TerminalException(
                    "HTTP ${response.status.value}",
                    "unknown_error",
                    errorBody
                )
            }
        }
    }

    override fun close() {
        httpClient.close()
    }
}
```

---

### 4. Exceptions (TerminalException.kt)

```kotlin
package no.norgesgass.payment.client.exceptions

import no.norgesgass.payment.client.dto.ErrorResponse

open class TerminalException(
    message: String,
    val errorCode: String,
    val details: String? = null
) : Exception(message)

class TerminalBusyException(error: ErrorResponse) : TerminalException(
    error.Error,
    error.ErrorCode,
    error.Details
)

class TerminalNotReadyException(error: ErrorResponse) : TerminalException(
    error.Error,
    error.ErrorCode,
    error.Details
)

class OperationTimeoutException(error: ErrorResponse) : TerminalException(
    error.Error,
    error.ErrorCode,
    error.Details
)

class VendorCallFailureException(error: ErrorResponse) : TerminalException(
    error.Error,
    error.ErrorCode,
    error.Details
)
```

---

### 5. WireMock Setup (WireMockSetup.kt)

```kotlin
package no.norgesgass.payment.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

class WireMockSetup(private val port: Int = 8080) {

    private val server = WireMockServer(
        options()
            .port(port)
            .usingFilesUnderClasspath("wiremock")
    )

    fun start() {
        server.start()
        configureFor("localhost", port)
        setupMappings()
        logger.info { "WireMock server started on port $port" }
    }

    fun stop() {
        server.stop()
        logger.info { "WireMock server stopped" }
    }

    private fun setupMappings() {
        setupHealthMapping()
        setupStatusMapping()
        setupLifecycleMappings()
        setupPaymentMappings()
    }

    private fun setupHealthMapping() {
        stubFor(
            get(urlEqualTo("/health"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "status": "ok",
                              "timestamp": "${Instant.now()}",
                              "configLoaded": true
                            }
                            """.trimIndent()
                        )
                )
        )
    }

    private fun setupStatusMapping() {
        stubFor(
            get(urlEqualTo("/v1/terminal/status"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "VendorDllLoadable": true,
                              "TerminalOpen": true,
                              "TerminalReady": true,
                              "LastError": null,
                              "TerminalIdentity": {
                                "TerminalID": "12345678",
                                "MerchantId": "12345678901234"
                              }
                            }
                            """.trimIndent()
                        )
                )
        )
    }

    private fun setupLifecycleMappings() {
        stubFor(
            post(urlEqualTo("/v1/terminal/open"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "Success": true,
                              "Message": "Terminal opened",
                              "Error": null
                            }
                            """.trimIndent()
                        )
                )
        )

        stubFor(
            post(urlEqualTo("/v1/terminal/close"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "Success": true,
                              "Message": "Terminal closed",
                              "Error": null
                            }
                            """.trimIndent()
                        )
                )
        )
    }

    private fun setupPaymentMappings() {
        // Default: approved purchase
        stubFor(
            post(urlEqualTo("/v1/payments/purchase"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("purchase-approved.json")
                )
        )
    }
}
```

---

### 6. WireMock Response Files

**resources/wiremock/__files/purchase-approved.json:**

```json
{
  "Success": true,
  "OperationId": "op-sim-{{randomValue type='UUID'}}",
  "StartedAt": "{{now format='yyyy-MM-dd'T'HH:mm:ss'Z'}}",
  "CompletedAt": "{{now offset='5 seconds' format='yyyy-MM-dd'T'HH:mm:ss'Z'}}",
  "DurationMs": 5000,
  "CallResult": 1,
  "MethodRejectCode": 0,
  "MethodRejectInfo": null,
  "ResultEventName": "OnLocalMode",
  "LocalModeResult": 0,
  "ResponseCode": "00",
  "RejectionSource": "0",
  "RejectionReason": null,
  "LocalModeFields": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234",
    "TotalAmount": "{{jsonPath request.body '$.AmountMinor'}}"
  },
  "PrintTextRaw": "NETS AS\nTRANSAKSJON GODKJENT\nBeløp: NOK {{jsonPath request.body '$.AmountMinor'}} øre\nTerminal: 12345678\nTid: {{now format='HH:mm:ss'}}\n",
  "PrintTextSanitized": "NETS AS\nTRANSAKSJON GODKJENT\nBeløp: NOK {{jsonPath request.body '$.AmountMinor'}} øre\nTerminal: 12345678\nTid: {{now format='HH:mm:ss'}}\n",
  "LastDisplayText": "GODKJENT",
  "Error": null,
  "ErrorCode": null,
  "DbRowId": 1,
  "ReceiptFileId": "{{now format='yyyy-MM-dd'}}/op-sim-{{randomValue type='UUID'}}"
}
```

---

### 7. Simulator Main App (TerminalSimulatorApp.kt)

```kotlin
package no.norgesgass.payment.simulator

import io.github.oshai.kotlinlogging.KotlinLogging
import no.norgesgass.payment.wiremock.WireMockSetup

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 8080
    
    logger.info { "Starting Payment Terminal Simulator on port $port" }
    
    val wireMock = WireMockSetup(port)
    
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info { "Shutting down..." }
        wireMock.stop()
    })
    
    wireMock.start()
    
    logger.info { "Simulator ready. Press Ctrl+C to stop." }
    
    // Keep running
    Thread.currentThread().join()
}
```

---

## WireMock Integrasjon

### Scenario-basert Testing

WireMock støtter **stateful scenarios** som lar deg simulere kompleks terminal oppførsel:

```kotlin
// Scenario: Terminal går fra READY → BUSY → READY

stubFor(
    post(urlEqualTo("/v1/payments/purchase"))
        .inScenario("Terminal State")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(
            aResponse()
                .withStatus(200)
                .withBodyFile("purchase-approved.json")
        )
        .willSetStateTo("BUSY")
)

stubFor(
    post(urlEqualTo("/v1/payments/purchase"))
        .inScenario("Terminal State")
        .whenScenarioStateIs("BUSY")
        .willReturn(
            aResponse()
                .withStatus(409)
                .withBody("""
                    {
                      "Error": "Terminal is busy",
                      "ErrorCode": "terminal_busy"
                    }
                """.trimIndent())
        )
)
```

---

### Control Endpoint for Dynamiske Scenarios

For å gjøre det enkelt å styre simulatoren fra tester eller lab-miljø:

```kotlin
// POST /__control/scenario
// Body: { "scenario": "APPROVED" | "DECLINED" | "WRONG_PIN" | "TIMEOUT" }

stubFor(
    post(urlEqualTo("/__control/scenario"))
        .willReturn(
            aResponse()
                .withStatus(200)
                .withTransformers("scenario-switcher")
        )
)
```

---

## Testing Strategi

### 1. Unit Tests (Kotlin Client)

```kotlin
class PaymentTerminalClientTest {

    private lateinit var wireMock: WireMockServer
    private lateinit var client: PaymentTerminalClient

    @BeforeEach
    fun setup() {
        wireMock = WireMockServer(8089)
        wireMock.start()
        client = PaymentTerminalClient("http://localhost:8089")
    }

    @AfterEach
    fun teardown() {
        client.close()
        wireMock.stop()
    }

    @Test
    fun `should return health status`() = runBlocking {
        // Given
        wireMock.stubFor(
            get(urlEqualTo("/health"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"status":"ok","timestamp":"2026-02-09T12:00:00Z","configLoaded":true}""")
                )
        )

        // When
        val health = client.health()

        // Then
        assertEquals("ok", health.status)
        assertTrue(health.configLoaded)
    }

    @Test
    fun `should handle terminal busy error`() = runBlocking {
        // Given
        wireMock.stubFor(
            post(urlEqualTo("/v1/payments/purchase"))
                .willReturn(
                    aResponse()
                        .withStatus(409)
                        .withBody("""{"Error":"Terminal is busy","ErrorCode":"terminal_busy"}""")
                )
        )

        // When/Then
        assertThrows<TerminalBusyException> {
            client.purchase(PurchaseRequest(AmountMinor = 10000))
        }
    }
}
```

---

### 2. Integration Tests (Full Flow)

```kotlin
class SimulatorIntegrationTest {

    @Test
    fun `full purchase flow - happy path`() = runBlocking {
        PaymentTerminalClient().use { client ->
            // 1. Check health
            val health = client.health()
            assertEquals("ok", health.status)

            // 2. Check status
            val status = client.terminalStatus()
            assertTrue(status.TerminalReady)

            // 3. Purchase
            val request = PurchaseRequest(
                AmountMinor = 10000,
                OperatorId = "0000",
                ClientRequestId = "test-session-123"
            )
            val response = client.purchase(request)

            // 4. Verify
            assertTrue(response.Success)
            assertEquals(1, response.CallResult)
            assertEquals(0, response.LocalModeResult)
            assertEquals("00", response.ResponseCode)
            assertNotNull(response.PrintTextRaw)
        }
    }

    @Test
    fun `purchase with wrong PIN should return failure`() = runBlocking {
        // Setup WireMock to return wrong PIN scenario
        // ...

        PaymentTerminalClient().use { client ->
            val request = PurchaseRequest(AmountMinor = 10000)
            val response = client.purchase(request)

            assertFalse(response.Success)
            assertEquals(2, response.LocalModeResult)
            assertEquals("Z1", response.ResponseCode)
            assertTrue(response.RejectionReason?.contains("3:2:Z1") == true)
        }
    }
}
```

---

### 3. Scenario Tests (Edge Cases)

```kotlin
class ScenarioTest {

    @Test
    fun `should handle busy retry workflow`() = runBlocking {
        // Scenario: First call returns BUSY, second call succeeds
        // ...
    }

    @Test
    fun `should enforce single operation at a time`() = runBlocking {
        // Start long-running operation, try to start another (expect 409)
        // ...
    }

    @Test
    fun `should respect idempotency with clientRequestId`() = runBlocking {
        // Send same request twice with same clientRequestId
        // Expect same operationId returned
        // ...
    }
}
```

---

## Deployment

### Kjøre Simulatoren

**Option 1: Embedded WireMock (Kotlin main)**

```bash
./gradlew run --args="8080"
```

**Option 2: Standalone WireMock (CI/test miljø)**

```bash
# Download WireMock standalone
wget https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar

# Start with mappings
java -jar wiremock-standalone-3.3.1.jar \
  --port 8080 \
  --root-dir ./resources/wiremock
```

---

### Docker Deployment (Optional)

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/payment-terminal-simulator-1.0.0.jar app.jar
COPY resources/wiremock /app/wiremock

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "8080"]
```

---

## ChatGPT Vurdering

### Er dette en god idé?

**ChatGPT sier:**

> "Ja, veldig. Du får:
> - Reproduserbare tester i CI uten terminal/Mono-server
> - Mulighet til å simulere 'BUSY / NOT_READY / TIMEOUT' deterministisk
> - Et 'lab mode' oppsett der du kan peke systemet mot en sim-terminal uten å endre domenelogikk."

### Konkrete implementeringssteg (ChatGPT anbefaling)

1. **Implementer PaymentTerminalClient** (3 endpoints) i lpg-ehl-service
2. **Legg WireMock-tester** for health/status/purchase (happy + busy + not_ready + timeout)
3. **Lag payment-terminal-mock** (en liten Kotlin main) som starter WireMock og laster mappings
4. **Legg til scenario-styring** via header + `/__control/scenario`
5. **(Etterpå) Idempotency** + single in-flight hvis du trenger realistiske retries

---

## Hva Mangler i Planen?

### Mulige Gap

1. **SSE Event Stream** - ikke implementert i WireMock-guiden
   - **Løsning:** Kan legges til med custom WireMock extension eller separat Ktor SSE endpoint

2. **Receipt Parsing** - ikke dekket
   - **Løsning:** `ReportParser.cs` kan portes til Kotlin

3. **Persistence** - ikke inkludert
   - **Løsning:** In-memory map for MVP, SQLite for prod-parity

4. **Admin Operations** - kun `avstemming`, `reversal`, etc. prototypet
   - **Løsning:** Samme pattern som `purchase`, lett å utvide

---

## Agent-Instruksjoner

### Når du skal implementere dette med en agent:

**Fase 1 Instruksjoner:**

```
Implementer en Kotlin-basert payment terminal simulator med følgende krav:

1. Lag en Kotlin client som snakker med PaymentTerminalMonoServer API
   - Bruk Ktor client med kotlinx.serialization
   - Implementer health, status, open, close, purchase endpoints
   - Håndter alle HTTP error codes (409, 503, 408, 500)

2. Sett opp WireMock embedded server
   - Port 8080
   - Mappings for: /health, /v1/terminal/status, /v1/terminal/open, /v1/terminal/close, /v1/payments/purchase
   - Response JSON filer i resources/wiremock/__files/

3. Lag en kjørbar main app (TerminalSimulatorApp.kt)
   - Starter WireMock på port 8080
   - Logger til stdout
   - Graceful shutdown på Ctrl+C

4. Skriv integrasjonstester
   - Full purchase flow (happy path)
   - Terminal busy (409)
   - Terminal not ready (503)

Bruk prosjektstrukturen i build.gradle.kts og følg DTO strukturen fra Responses.kt.
```

**Fase 2 Instruksjoner (etter Fase 1):**

```
Utvid simulatoren med edge cases:

1. Implementer scenario-basert simulering
   - APPROVED, DECLINED, WRONG_PIN, USER_CANCEL, TIMEOUT
   - Bruk WireMock Scenario API
   - Control endpoint: POST /__control/scenario

2. Legg til idempotency
   - clientRequestId tracking
   - Returner cached operationId hvis samme request

3. Stateful terminal
   - Kan være CLOSED, OPEN, READY, BUSY
   - Enforce state transitions
   - Return 503 hvis not ready

4. Skriv scenario-tester for alle edge cases
```

---

## Konklusjon

Dette er en **komplett implementeringsguide** for å bygge en Kotlin-basert payment terminal simulator. 

**Nøkkelpunkter:**

✅ **API Paritet** - Full kompatibilitet med PaymentTerminalMonoServer kontrakt  
✅ **JVM-basert** - Ingen Mono/C# dependencies  
✅ **WireMock** - Deterministisk testing  
✅ **Scenarios** - Simuler busy, timeout, wrong PIN, osv.  
✅ **Idempotency** - clientRequestId support  
✅ **Testbar** - Unit + integration tests  

**Hva gjør dette enkelt for en agent å implementere?**

- Klar DTO struktur (copy-paste ready)
- Konkrete WireMock eksempler
- Komplett prosjektstruktur (build.gradle.kts, filstruktur)
- Step-by-step fase 1 + 2 instruksjoner
- ChatGPT validert strategi

Du kan nå gi denne filen til en agent og si:

> "Implementer Fase 1 av denne planen. Start med build.gradle.kts og PaymentTerminalClient.kt."

---

**Lykke til med implementeringen! 🚀**
