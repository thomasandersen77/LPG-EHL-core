# Baxi Payment Terminal - Master Guide

**Prosjekt:** NorgesGass Baxi Payment Terminal Integration  
**Versjon:** 1.0  
**Dato:** 9. februar 2026

---

## 1. Executive Summary

### Hva er dette?

Et **payment terminal integrasjonsprosjekt** som erstatter en legacy VB6-applikasjon med en moderne HTTP API-arkitektur. Systemet lar Kotlin/JVM-applikasjoner snakke med NETS Baxi betalingsterminaler uten direkte vendor DLL-avhengigheter.

### Tre hovedkomponenter

| Komponent | Teknologi | Formål |
|-----------|-----------|--------|
| **PaymentTerminalMonoServer** | C# / .NET 4.8 / Mono | HTTP REST wrapper rundt vendor DLL, produksjonsserver |
| **PaymentTerminalFieldTest** | C# / .NET 4.8 / Mono | CLI-verktøy for testing, protokoll-analyse, wire capture |
| **Kotlin Simulator** (ny) | Kotlin / JVM / WireMock | Testbar simulator uten fysisk terminal eller Mono |

### Anbefalt strategi

Bygg en **Kotlin HTTP-klient** som snakker med PaymentTerminalMonoServer (produksjon) eller en **WireMock-basert simulator** (testing/utvikling). Ikke reimplementer wire-protokollen - wrap vendor DLL via HTTP.

```
Kotlin App  ──HTTP──>  PaymentTerminalMonoServer (C#/Mono) ──DLL──> Terminal
                ╰──HTTP──>  WireMock Simulator (testing/dev)
```

---

## 2. Arkitektur

### Overordnet flyt (produksjon)

```
┌───────────────────────────────────────────────┐
│  CLIENT APPLICATION (Kotlin/JVM)              │
│  - LPG-EHL Service                            │
│  - Fueling / pump / business logic            │
└──────────────────┬────────────────────────────┘
                   │ HTTP REST API (JSON)
                   ▼
┌───────────────────────────────────────────────┐
│  PaymentTerminalMonoServer (C#/Mono)          │
│                                               │
│  HttpServer ──> TerminalService ──> BaxiAdapter│
│  (HttpListener)  (orchestration)   (reflection)│
│       │               │                │      │
│  Json.NET      OperationLock      Assembly    │
│  409/503/408   Idempotency       .LoadFrom()  │
│                SQLite/Receipts                 │
└──────────────────┬────────────────────────────┘
                   │ .NET Reflection → vendor DLL
                   ▼
┌───────────────────────────────────────────────┐
│  baxi_dotnet.dll  (BBS.BAXI.BaxiCtrl)        │
│  TLD Protocol over TCP/IP or RS-232           │
└──────────────────┬────────────────────────────┘
                   ▼
┌───────────────────────────────────────────────┐
│  Fysisk Baxi Terminal (NETS Certified)        │
│  Card reader / PIN pad / Display / Printer    │
└───────────────────────────────────────────────┘
```

### Terminal state machine

```
CLOSED ──(Open)──> OPENED ──(TerminalReady event)──> READY
READY  ──(Purchase/Admin)──> BUSY ──(Completion event)──> READY
READY  ──(Close)──> CLOSED
```

### Event flow (en typisk purchase)

```
TransferAmount(amount=10000, type=0x30)
    │
    ├──> OnDisplayText("SETT INN KORT")
    ├──> OnDisplayText("LES KORT")
    ├──> OnDisplayText("TAST PIN")
    ├──> OnPrintText("NETS AS\nTRANS...")
    └──> OnLocalMode(Result=0, ResponseCode="00")  ← COMPLETION
```

---

## 3. Repository-struktur

```
BaxiExperiments/
├── PaymentTerminalMonoServer/          # Produksjonsserver (C#/Mono)
│   ├── Services/
│   │   ├── BaxiAdapter.cs              # Reflection wrapper rundt vendor DLL
│   │   ├── TerminalService.cs          # Orkestrering, state, idempotency
│   │   ├── HttpServer.cs              # REST API (HttpListener)
│   │   ├── OperationLock.cs           # Single-operation enforcement
│   │   ├── AdminCodeMapper.cs         # VB6 admin code → hex
│   │   ├── ReportParser.cs            # Parse Z/avstemming output
│   │   └── PreAvstemmingOrchestrator.cs
│   ├── Models/
│   │   ├── Requests.cs                # PurchaseRequest, RefundRequest, ...
│   │   ├── Responses.cs               # OperationResponse, TerminalStatusResponse
│   │   └── CapturedResult.cs
│   ├── Persistence/
│   │   ├── Database.cs                # SQLite
│   │   ├── EventStore.cs             # In-memory event stream
│   │   └── ReceiptStorage.cs         # Filesystem receipts
│   ├── Configuration/ServerConfig.cs  # server.json loader
│   ├── Program.cs
│   ├── API_CONTRACT.md               # HTTP API spec
│   ├── CLIENT_GUIDE_VB6_REPLACEMENT.md
│   └── DEPLOYMENT.md
│
├── PaymentTerminalFieldTest/           # CLI test/diagnostikk (C#/Mono)
│   ├── BaxiAdapter.cs                  # Reflection wrapper (kopi)
│   ├── WireProxy.cs                   # TCP proxy for wire capture
│   ├── Program.cs                     # CLI commands
│   ├── baxi.ini                       # Terminal config (source of truth)
│   └── README.md
│
├── additional-files/
│   ├── baxi_dotnet.dll                # Vendor DLL (proprietaer)
│   ├── Baxi.NewtonSoft.Json.dll
│   ├── baxi.ini                       # Alejandros config
│   ├── BAXI_PROTOCOL_COMPREHENSIVE_ANALYSIS.md
│   └── BAXI_VB6_BEHAVIOR_REIMPLEMENTATION_EVIDENCE_REPORT.md
│
├── Baxi NET/                           # Vendor docs + sample app
│   └── baxi.net_1.11.3.0-Docs/Nets-Baxi.NetProgrammersGuide_v1.11.3.0.pdf
│
├── legacy-vb6-code-for-reference-read-only/
│   └── norgesgass_legacy/              # 2196 filer, inkl. pumpekontroll.frm
│
└── Docs/
    └── MASTER_PAYMENT_TERMINAL_GUIDE.md  # <-- DENNE FILEN
```

---

## 4. Komplett API-kontrakt

**Base URL:** `http://127.0.0.1:8080` (konfigurerbar)

### Endpoint-oversikt

| Method | Path | Formål | Response type |
|--------|------|--------|---------------|
| GET | `/health` | Server health | `HealthResponse` (lowercase!) |
| GET | `/v1/terminal/status` | Terminal readiness | `TerminalStatusResponse` |
| POST | `/v1/terminal/open` | Aapne terminal | `SimpleResponse` |
| POST | `/v1/terminal/close` | Lukke terminal | `SimpleResponse` |
| POST | `/v1/payments/purchase` | Purchase (0x30) | `OperationResponse` |
| POST | `/v1/payments/refund` | Refund (0x31) | `OperationResponse` |
| POST | `/v1/payments/cashback` | Purchase + cashback | `OperationResponse` |
| POST | `/v1/admin/avstemming` | Reconciliation (0x3130) | `OperationResponse` |
| POST | `/v1/admin/cancel` | Cancel (0x3132) | `OperationResponse` |
| POST | `/v1/admin/reversal` | Reversal (0x3134) | `OperationResponse` |
| POST | `/v1/admin/z-report` | Z-report (0x3137) | `OperationResponse` |
| POST | `/v1/admin/last-receipt` | Last receipt (0x313C) | `OperationResponse` |
| POST | `/v1/admin/software` | SW download (0x313E) | `OperationResponse` |
| POST | `/v1/admin/dataset` | Dataset download (0x313F) | `OperationResponse` |
| POST | `/v1/admin/code` | Arbitrary admin code | `OperationResponse` |
| GET | `/v1/events/stream` | SSE event stream | `text/event-stream` |
| GET | `/v1/events?since=X` | Polling events | `EventEnvelope[]` |

### JSON casing (viktig!)

- **`/health`** returnerer **lowercase** keys: `status`, `timestamp`, `configLoaded`
- **Alle andre endpoints** returnerer **PascalCase**: `Success`, `OperationId`, `AmountMinor`

Forklaring: `/health` bygger JSON manuelt. Resten serialiseres med Json.NET uten camelCase-resolver.

**Anbefaling for Kotlin-klient:** Bruk `ignoreUnknownKeys = true` og enten separate DTO-klasser eller case-insensitive parsing.

### Error-modell

| HTTP | ErrorCode | Betydning |
|------|-----------|-----------|
| 409 | `terminal_busy` | Annen operasjon kjoerer |
| 503 | `terminal_not_ready` | Terminal ikke aapnet/klar |
| 408 | `operation_timeout` | Timeout - ingen completion event |
| 500 | `vendor_call_failure` | Vendor DLL call feilet |
| 400 | `invalid_request` | Ugyldig request |
| 403 | `diagnostics_disabled` | Diagnostikk deaktivert |

Error body:
```json
{ "Error": "...", "ErrorCode": "terminal_busy", "OperationId": "...", "Details": "..." }
```

**Viktig:** `Success=false` i `OperationResponse` er **ikke** det samme som HTTP 4xx/5xx. En vellykket HTTP 200 med `Success=false` betyr at terminalen behandlet operasjonen men den ble avvist (feil PIN, brukeravbrudd, declined kort).

### Success-logikk

```
Financial: Success = (CallResult==1 && !TimedOut && (LocalModeResult==0 || ResponseCode=="00"))
Admin:     Success = (CallResult==1 && !TimedOut)
```

### Idempotency

Send `ClientRequestId` i request body. Hvis samme ID sendes igjen returnerer serveren det cached resultatet.

### Timeouts

- Financial: 180 sekunder (konfigurerbar)
- Admin: 300 sekunder (konfigurerbar)
- Timeout starter etter `CallResult==1` (vendor call akseptert)
- Terminal-operasjonen fortsetter selv om HTTP returnerer 408

---

## 5. Request/Response-eksempler

### Health

```http
GET /health
```
```json
{"status": "ok", "timestamp": "2026-02-09T12:00:00Z", "configLoaded": true}
```

### Terminal Status

```http
GET /v1/terminal/status
```
```json
{
  "VendorDllLoadable": true,
  "TerminalOpen": true,
  "TerminalReady": true,
  "LastError": null,
  "TerminalIdentity": {"TerminalID": "12345678", "MerchantId": "12345678901234"}
}
```

### Purchase - Approved

```http
POST /v1/payments/purchase
Content-Type: application/json

{
  "AmountMinor": 10000,
  "Currency": "NOK",
  "OperatorId": "0000",
  "OptionalData": "LPG Autogas",
  "ClientRequestId": "fueling-session-abc123",
  "PreAvstemming": {"Enabled": false, "Password": "0000", "TimeoutSeconds": 300}
}
```
```json
{
  "Success": true,
  "OperationId": "op-uuid-12345",
  "StartedAt": "2026-02-09T12:00:00Z",
  "CompletedAt": "2026-02-09T12:00:05Z",
  "DurationMs": 5000,
  "CallResult": 1,
  "MethodRejectCode": 0,
  "ResultEventName": "OnLocalMode",
  "LocalModeResult": 0,
  "ResponseCode": "00",
  "RejectionSource": "0",
  "RejectionReason": null,
  "LocalModeFields": {"TerminalID": "12345678", "MerchantId": "12345678901234", "TotalAmount": "10000"},
  "PrintTextRaw": "NETS AS\nTRANSAKSJON GODKJENT\n...",
  "PrintTextSanitized": "NETS AS\nTRANSAKSJON GODKJENT\n...",
  "LastDisplayText": "GODKJENT",
  "Error": null,
  "ErrorCode": null,
  "DbRowId": 1,
  "ReceiptFileId": "2026-02-09/op-uuid-12345"
}
```

### Purchase - Wrong PIN

HTTP 200 men `Success=false`:
```json
{
  "Success": false,
  "LocalModeResult": 2,
  "ResponseCode": "Z1",
  "RejectionSource": "3",
  "RejectionReason": "3:2:Z1",
  "LastDisplayText": "PIN FEIL"
}
```

### Purchase - User Cancel

HTTP 200 men `Success=false`:
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

### Purchase - Terminal Busy

HTTP 409:
```json
{"Error": "Terminal is busy with another operation", "ErrorCode": "terminal_busy"}
```

### Purchase - Terminal Not Ready

HTTP 503:
```json
{"Error": "Terminal is not ready", "ErrorCode": "terminal_not_ready"}
```

---

## 6. VB6 til HTTP mapping

### Financial operasjoner

| VB6 kall | Hex | HTTP endpoint |
|----------|-----|---------------|
| `TransferAmount_V2("0000", &H30, amount, ...)` | 0x30 | `POST /v1/payments/purchase` |
| `TransferAmount_V2("0000", &H31, amount, ...)` | 0x31 | `POST /v1/payments/refund` |

### Admin operasjoner

| VB6 kall | Hex | Decimal | HTTP endpoint |
|----------|-----|---------|---------------|
| `Administration &H3130` | 0x3130 | 12592 | `POST /v1/admin/avstemming` |
| `Administration &H3131` | 0x3131 | 12593 | `POST /v1/admin/code` (KLAR/clear buffer) |
| `Administration &H3132` | 0x3132 | 12594 | `POST /v1/admin/cancel` |
| `Administration &H3134` | 0x3134 | 12596 | `POST /v1/admin/reversal` |
| `Administration &H3136` | 0x3136 | 12598 | `POST /v1/admin/code` (X-report) |
| `Administration &H3137` | 0x3137 | 12599 | `POST /v1/admin/z-report` |
| `Administration &H313C` | 0x313C | 12604 | `POST /v1/admin/last-receipt` |
| `Administration &H313E` | 0x313E | 12606 | `POST /v1/admin/software` |
| `Administration &H313F` | 0x313F | 12607 | `POST /v1/admin/dataset` |

---

## 7. Server-komponenter (C#)

### BaxiAdapter.cs

Reflection-wrapper rundt `BBS.BAXI.BaxiCtrl`:

- Laster `baxi_dotnet.dll` med `Assembly.LoadFrom()`
- Subscribes til events via `Expression.Lambda`
- Completion detection: venter paa `OnLocalMode` eller `OnLastFinancialResult`
- Samler `PrintText`, `DisplayText`, `LocalModeFields` under operasjon
- Mono-kompatibel (.NET Framework 4.8 paa Linux)

### TerminalService.cs

Orkestrering:

- Single-operation enforcement via `OperationLock`
- Terminal readiness gating
- Idempotency: `ClientRequestId` -> cached `OperationResponse`
- Timeout handling (konfigurerbare per operasjonstype)
- Persist til SQLite + receipts til filesystem

### HttpServer.cs

- `System.Net.HttpListener`
- JSON via Newtonsoft.Json (PascalCase default, ingen camelCase resolver)
- `/health` returnerer lowercase manuelt

### Database.cs (SQLite)

```sql
CREATE TABLE operations (
    id INTEGER PRIMARY KEY,
    operation_id TEXT UNIQUE,
    client_request_id TEXT,
    operation_type TEXT,
    started_at TEXT, completed_at TEXT,
    success INTEGER, call_result INTEGER,
    local_mode_result INTEGER, response_code TEXT,
    rejection_reason TEXT, print_text TEXT,
    last_display_text TEXT, receipt_file_id TEXT
);
CREATE INDEX idx_client_request_id ON operations(client_request_id);
```

### ReceiptStorage.cs

```
./receipts/YYYY-MM-DD/{operationId}.raw.txt
./receipts/YYYY-MM-DD/{operationId}.sanitized.txt
```

---

## 8. PaymentTerminalFieldTest (CLI-verktoy)

Headless CLI for testing mot fysisk terminal. Bruker reflection-adapter (kopi av serveren).

### Scope

- Open/close terminal, financial transactions, admin commands
- Observe DisplayText/PrintText/LocalMode/Error
- Dump events til logs
- Wire proxy for protokoll-capture

### Krav paa Debian-maskin

```bash
sudo apt-get install -y mono-complete
```

Filer ved siden av executable:
- `baxi_dotnet.dll`, `Baxi.NewtonSoft.Json.dll`, `baxi.ini`

### Bruk

```bash
# REPL
mono payment-terminal-fieldtest.exe repl --auto-open --workdir .

# One-shot payment
mono payment-terminal-fieldtest.exe pay --amount 1250 --type1 0x30 --workdir .

# Admin
mono payment-terminal-fieldtest.exe admin avstemming --password 0000 --workdir .
mono payment-terminal-fieldtest.exe admin z --password 0000 --workdir .
```

### Evidence capture (for Wireshark-korrelasjon)

```bash
mono payment-terminal-fieldtest.exe pay --amount 1250 --type1 0x30 --workdir . \
  --trace-level 5 --csv ./logs/ops.csv --eventlog ./logs/events.tsv
```

- `--csv`: Eén rad per operasjon (LocalMode, print, JSON, stdrsp som base64)
- `--eventlog`: Tidslinje av kall + events (TSV)
- `--trace-level 5`: Maksimal vendor-logging

### Wire proxy

Bruker 2-byte big-endian length prefix framing. Terminal sender tekst i Latin-1/CP1252 (norske tegn: AA=0xC5, OE=0xD8).

NB: Ved bruk av `--wire-proxy-*` settes `SocketListenerPort` midlertidig. Tilbakestill til 6001 etter bruk.

### Nettverksoppsett (Ethernet-terminal paa separat NIC)

```bash
sudo sysctl -w net.ipv4.ip_forward=1
sudo iptables -t nat -A POSTROUTING -s 192.168.50.0/24 -o enp0s25 -j MASQUERADE
sudo iptables -A FORWARD -i ens32 -o enp0s25 -s 192.168.50.0/24 -j ACCEPT
sudo iptables -A FORWARD -i enp0s25 -o ens32 -d 192.168.50.0/24 -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
```

---

## 9. Kotlin-implementering

### Prosjektstruktur

```
payment-terminal-simulator/
├── build.gradle.kts
├── src/main/kotlin/no/norgesgass/payment/
│   ├── client/
│   │   ├── PaymentTerminalClient.kt
│   │   ├── dto/
│   │   │   ├── Requests.kt
│   │   │   ├── Responses.kt
│   │   │   └── ErrorResponse.kt
│   │   └── exceptions/TerminalException.kt
│   ├── simulator/
│   │   ├── TerminalSimulatorApp.kt
│   │   ├── TerminalStateManager.kt
│   │   └── scenarios/
│   │       ├── ApprovedScenario.kt
│   │       ├── WrongPinScenario.kt
│   │       └── TimeoutScenario.kt
│   └── wiremock/
│       ├── WireMockSetup.kt
│       └── scenarios/ScenarioManager.kt
├── src/test/kotlin/no/norgesgass/payment/
│   ├── client/PaymentTerminalClientTest.kt
│   └── simulator/SimulatorIntegrationTest.kt
└── resources/wiremock/
    ├── mappings/
    └── __files/
        ├── purchase-approved.json
        ├── purchase-wrong-pin.json
        └── receipt-approved.txt
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "no.norgesgass"
version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.wiremock:wiremock:3.3.1")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

tasks.test { useJUnitPlatform() }
application { mainClass.set("no.norgesgass.payment.simulator.TerminalSimulatorAppKt") }
kotlin { jvmToolchain(21) }
```

### DTO-klasser

**Requests.kt:**
```kotlin
@Serializable
data class PurchaseRequest(
    val AmountMinor: Int,
    val Currency: String = "NOK",
    val OperatorId: String = "4321",
    val OptionalData: String? = null,
    val PreAvstemming: PreAvstemmingConfig? = null,
    val ClientRequestId: String? = null
)

@Serializable data class RefundRequest(val AmountMinor: Int, val Currency: String = "NOK", val OperatorId: String = "4321", val OptionalData: String? = null, val ClientRequestId: String? = null)
@Serializable data class CashbackRequest(val PurchaseMinor: Int, val CashbackMinor: Int, val Currency: String = "NOK", val OperatorId: String = "4321", val ClientRequestId: String? = null)
@Serializable data class AdminRequest(val Password: String = "0000")
@Serializable data class PreAvstemmingConfig(val Enabled: Boolean = false, val Password: String = "0000", val TimeoutSeconds: Int? = null)
```

**Responses.kt:**
```kotlin
@Serializable data class HealthResponse(val status: String, val timestamp: String, val configLoaded: Boolean)

@Serializable
data class TerminalStatusResponse(
    val VendorDllLoadable: Boolean, val TerminalOpen: Boolean, val TerminalReady: Boolean,
    val LastError: String? = null, val TerminalIdentity: Map<String, String>? = null
)

@Serializable
data class OperationResponse(
    val Success: Boolean, val OperationId: String, val StartedAt: String,
    val CompletedAt: String? = null, val DurationMs: Int? = null,
    val CallResult: Int, val MethodRejectCode: Int = 0, val MethodRejectInfo: String? = null,
    val ResultEventName: String? = null, val LocalModeResult: Int = 0,
    val ResponseCode: String? = null, val RejectionSource: String? = null, val RejectionReason: String? = null,
    val LocalModeFields: Map<String, String>? = null,
    val PrintTextRaw: String? = null, val PrintTextSanitized: String? = null,
    val LastDisplayText: String? = null, val Error: String? = null, val ErrorCode: String? = null,
    val DbRowId: Long? = null, val ReceiptFileId: String? = null, val ReportFields: Map<String, String>? = null
)

@Serializable data class SimpleResponse(val Success: Boolean, val Message: String? = null, val Error: String? = null)
@Serializable data class ErrorResponse(val Error: String, val ErrorCode: String, val OperationId: String? = null, val Details: String? = null)
```

### PaymentTerminalClient.kt

```kotlin
class PaymentTerminalClient(private val baseUrl: String = "http://127.0.0.1:8080") : AutoCloseable {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    suspend fun health(): HealthResponse = handleResponse(httpClient.get("$baseUrl/health"))
    suspend fun terminalStatus(): TerminalStatusResponse = handleResponse(httpClient.get("$baseUrl/v1/terminal/status"))
    suspend fun openTerminal(): SimpleResponse = handleResponse(httpClient.post("$baseUrl/v1/terminal/open"))
    suspend fun closeTerminal(): SimpleResponse = handleResponse(httpClient.post("$baseUrl/v1/terminal/close"))

    suspend fun purchase(request: PurchaseRequest): OperationResponse =
        handleOperationResponse(httpClient.post("$baseUrl/v1/payments/purchase") {
            contentType(ContentType.Application.Json); setBody(request)
        })

    suspend fun refund(request: RefundRequest): OperationResponse =
        handleOperationResponse(httpClient.post("$baseUrl/v1/payments/refund") {
            contentType(ContentType.Application.Json); setBody(request)
        })

    suspend fun avstemming(password: String = "0000"): OperationResponse =
        handleOperationResponse(httpClient.post("$baseUrl/v1/admin/avstemming") {
            contentType(ContentType.Application.Json); setBody(AdminRequest(password))
        })

    suspend fun reversal(password: String = "0000"): OperationResponse =
        handleOperationResponse(httpClient.post("$baseUrl/v1/admin/reversal") {
            contentType(ContentType.Application.Json); setBody(AdminRequest(password))
        })

    suspend fun zReport(password: String = "0000"): OperationResponse =
        handleOperationResponse(httpClient.post("$baseUrl/v1/admin/z-report") {
            contentType(ContentType.Application.Json); setBody(AdminRequest(password))
        })

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T =
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            else -> throw TerminalException("HTTP ${response.status.value}", "terminal_error", response.bodyAsText())
        }

    private suspend fun handleOperationResponse(response: HttpResponse): OperationResponse =
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Conflict -> throw TerminalBusyException(response.body<ErrorResponse>())
            HttpStatusCode.ServiceUnavailable -> throw TerminalNotReadyException(response.body<ErrorResponse>())
            HttpStatusCode.RequestTimeout -> throw OperationTimeoutException(response.body<ErrorResponse>())
            HttpStatusCode.InternalServerError -> throw VendorCallFailureException(response.body<ErrorResponse>())
            else -> throw TerminalException("HTTP ${response.status.value}", "unknown_error", response.bodyAsText())
        }

    override fun close() { httpClient.close() }
}
```

### Exceptions

```kotlin
open class TerminalException(message: String, val errorCode: String, val details: String? = null) : Exception(message)
class TerminalBusyException(e: ErrorResponse) : TerminalException(e.Error, e.ErrorCode, e.Details)
class TerminalNotReadyException(e: ErrorResponse) : TerminalException(e.Error, e.ErrorCode, e.Details)
class OperationTimeoutException(e: ErrorResponse) : TerminalException(e.Error, e.ErrorCode, e.Details)
class VendorCallFailureException(e: ErrorResponse) : TerminalException(e.Error, e.ErrorCode, e.Details)
```

### Simulator main

```kotlin
fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 8080
    val wireMock = WireMockSetup(port)
    Runtime.getRuntime().addShutdownHook(Thread { wireMock.stop() })
    wireMock.start()
    println("Simulator ready on port $port. Ctrl+C to stop.")
    Thread.currentThread().join()
}
```

---

## 10. WireMock-integrasjon

### Stateful scenarios

```kotlin
// READY -> BUSY -> READY
stubFor(post(urlEqualTo("/v1/payments/purchase"))
    .inScenario("Terminal State").whenScenarioStateIs(Scenario.STARTED)
    .willReturn(aResponse().withStatus(200).withBodyFile("purchase-approved.json"))
    .willSetStateTo("BUSY"))

stubFor(post(urlEqualTo("/v1/payments/purchase"))
    .inScenario("Terminal State").whenScenarioStateIs("BUSY")
    .willReturn(aResponse().withStatus(409)
        .withBody("""{"Error":"Terminal is busy","ErrorCode":"terminal_busy"}""")))
```

### Control endpoint (lab-modus)

```
POST /__control/scenario
Body: {"scenario": "APPROVED" | "DECLINED" | "WRONG_PIN" | "USER_CANCEL" | "TIMEOUT" | "BUSY"}
```

### Header-styrt scenario (CI-modus)

Klient sender `X-Terminal-Scenario: WRONG_PIN` header. Mock velger respons basert paa dette.

### Scenarier som maa stoettes

| Scenario | HTTP | Success | LocalModeResult | ResponseCode | RejectionReason |
|----------|------|---------|-----------------|--------------|-----------------|
| APPROVED | 200 | true | 0 | "00" | null |
| DECLINED | 200 | false | 2 | "05" | varies |
| WRONG_PIN | 200 | false | 2 | "Z1" | "3:2:Z1" |
| USER_CANCEL | 200 | false | 2 | "" | "2:1" |
| BUSY | 409 | - | - | - | - |
| NOT_READY | 503 | - | - | - | - |
| TIMEOUT | 408 | - | - | - | - |

---

## 11. Testing

### Testpakke (minimum)

1. `GET /health` - parse lowercase keys
2. `GET /v1/terminal/status` - parse PascalCase
3. `POST /v1/payments/purchase` - happy path (200 + `Success=true`)
4. `POST /v1/payments/purchase` - busy (409)
5. `POST /v1/payments/purchase` - not ready (503)
6. `POST /v1/payments/purchase` - timeout (408)
7. `POST /v1/payments/purchase` - wrong PIN (200, `Success=false`, `ResponseCode=Z1`)
8. `POST /v1/admin/reversal` - happy path + busy

### Integrasjonstest-eksempel

```kotlin
@Test
fun `full purchase flow`() = runBlocking {
    PaymentTerminalClient("http://localhost:$port").use { client ->
        val health = client.health()
        assertEquals("ok", health.status)

        val status = client.terminalStatus()
        assertTrue(status.TerminalReady)

        val response = client.purchase(PurchaseRequest(
            AmountMinor = 10000, OperatorId = "0000", ClientRequestId = "test-123"
        ))
        assertTrue(response.Success)
        assertEquals(1, response.CallResult)
        assertEquals("00", response.ResponseCode)
    }
}
```

### Testdata som filer

Legg i `src/test/resources/payment-terminal/`:
- `health-ok.json`
- `terminal-status-ready.json`
- `purchase-approved.json`
- `purchase-wrong-pin.json`
- `error-terminal-busy.json`
- `error-terminal-not-ready.json`
- `error-operation-timeout.json`

---

## 12. Implementeringsplan

### Fase 1: MVP

- `/health`, `/v1/terminal/status`, `/v1/terminal/open`, `/v1/terminal/close`
- `/v1/payments/purchase` (approved path)
- Basic WireMock mappings
- Integrasjonstester (happy path + 2 feil)

### Fase 2: Edge cases

- `terminal_busy` (409), `terminal_not_ready` (503), `operation_timeout` (408)
- Wrong PIN, User cancel, Declined
- Idempotency (`ClientRequestId`)
- Scenario-styring via header + control endpoint

### Fase 3: Full feature parity

- Alle admin endpoints
- SSE event stream
- Receipt storage/parsing
- Persistence (in-memory -> SQLite)

---

## 13. Deployment

### Simulator (Kotlin WireMock)

```bash
./gradlew run --args="8080"
```

### Standalone WireMock

```bash
java -jar wiremock-standalone-3.3.1.jar --port 8080 --root-dir ./resources/wiremock
```

### Docker

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/payment-terminal-simulator-1.0.0.jar app.jar
COPY resources/wiremock /app/wiremock
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "8080"]
```

### Produksjonsserver (MonoServer)

```bash
cd PaymentTerminalMonoServer/bin/Release/net48
mono payment-terminal-mono-server.exe [server.json path]
```

Krav: `baxi.ini`, `baxi_dotnet.dll`, `Baxi.NewtonSoft.Json.dll` i working directory.

---

## 14. Kjente styrker og svakheter

### Styrker

- Separation of concerns: terminal isolert bak HTTP
- Robust error handling med strukturerte error codes
- Idempotency via `ClientRequestId`
- Evidence capture: receipts + SQLite audit trail + event stream
- Mono-kompatibel (Linux)
- Veldokumentert API kontrakt

### Svakheter

- Casing-inkonsistens (`/health` lowercase vs PascalCase)
- Ingen OpenAPI spec (kun markdown)
- Single terminal per prosess
- Mono dependency for prod
- Kun stdout logging (ingen Serilog/metrics)
- Ingen autentisering

---

## 15. Langsiktig roadmap

1. **OpenAPI 3.0 Spec** - generer fra eksisterende kontrakt
2. **Standardiser JSON casing** - velg PascalCase eller camelCase konsistent
3. **Strukturert logging** - Serilog + correlation IDs
4. **Metrics** - Prometheus `/metrics` endpoint
5. **Autentisering** - API key eller JWT
6. **Multi-terminal support** - `/v1/terminals/{id}/status`
7. **Cloud deployment** - Docker + Kubernetes

---

## 16. Agent-instruksjoner

### Fase 1 prompt (copy-paste til agent)

```
Implementer en Kotlin-basert payment terminal simulator:

1. Opprett Kotlin-prosjekt med build.gradle.kts fra MASTER_PAYMENT_TERMINAL_GUIDE.md seksjon 9
2. Implementer DTO-klasser: Requests.kt, Responses.kt, ErrorResponse.kt (se seksjon 9)
3. Implementer PaymentTerminalClient.kt med Ktor + kotlinx.serialization (se seksjon 9)
4. Implementer exception types (se seksjon 9)
5. Sett opp WireMock embedded server med mappings for:
   - GET /health (lowercase keys!)
   - GET /v1/terminal/status (PascalCase)
   - POST /v1/terminal/open, /v1/terminal/close
   - POST /v1/payments/purchase (approved response)
6. Lag TerminalSimulatorApp.kt main som starter WireMock paa port 8080
7. Skriv integrasjonstester:
   - Full purchase flow (happy path)
   - Terminal busy (409)
   - Terminal not ready (503)

JSON casing: /health er lowercase, alt annet er PascalCase.
Error modell: 409=busy, 503=not_ready, 408=timeout, 500=vendor_failure.
```

### Fase 2 prompt

```
Utvid simulatoren med edge cases:

1. Scenario-basert simulering: APPROVED, DECLINED, WRONG_PIN, USER_CANCEL, TIMEOUT
   - Bruk WireMock Scenario API
   - Control endpoint: POST /__control/scenario
2. Idempotency: Track ClientRequestId, returner cached response
3. Stateful terminal: CLOSED -> OPEN -> READY -> BUSY state machine
4. Scenario-tester for alle edge cases
5. Header-styrt scenario via X-Terminal-Scenario header
```

---

## 17. Referanser

| Dokument | Plassering |
|----------|------------|
| HTTP API Kontrakt | `PaymentTerminalMonoServer/API_CONTRACT.md` |
| VB6 Replacement Guide | `PaymentTerminalMonoServer/CLIENT_GUIDE_VB6_REPLACEMENT.md` |
| Mono Deployment | `PaymentTerminalMonoServer/DEPLOYMENT.md` |
| Functional Spec | `agent-os/specs/2026-02-09-payment-terminal-mono-server/spec.md` |
| Baxi Protocol Analysis | `additional-files/BAXI_PROTOCOL_COMPREHENSIVE_ANALYSIS.md` |
| VB6 Evidence Report | `additional-files/BAXI_VB6_BEHAVIOR_REIMPLEMENTATION_EVIDENCE_REPORT.md` |
| Programmer's Guide (PDF) | `Baxi NET/baxi.net_1.11.3.0-Docs/Nets-Baxi.NetProgrammersGuide_v1.11.3.0.pdf` |

---

**Sist oppdatert:** 9. februar 2026
