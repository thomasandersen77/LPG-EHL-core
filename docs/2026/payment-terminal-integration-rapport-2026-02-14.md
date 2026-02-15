# Payment Terminal Integrasjonsrapport
**Dato:** 2026-02-14  
**Utarbeidet av:** Thomas Andersen (med Warp AI)  
**Formål:** Vurdere servertest, simulator-avvik, og integrasjonsstrategi for Kotlin service-modulen

---

## 1. Vurdering av Alejandros arbeid

### Hva Alejandro har levert
Alejandro har gjort betydelig arbeid i `BaxiExperiments/nets-cloud-solution/`:

1. **PaymentTerminalNetsCloudKotlinServer** — En modulær Kotlin-port av Mono-serveren, delt i tre Gradle-moduler:
   - `payment-terminal-library` — **Ferdig**. Selvstendig library med TerminalService interface, ConnectCloudAdapter, persistence (H2), EventStore, OperationLock, ReceiptStorage.
   - `connect-cloud-client` — **Ferdig**. WebSocket-klient, auth-klient, Nets-protokollparser.
   - `payment-terminal-server` — HTTP-servermodul (wrapper).

2. **Ryddet BaxiExperiments** — Alt relevant er samlet i `nets-cloud-solution/`.

3. **Begynt RS-485 Kotlin-script** — Ikke ferdig, men lover godt for felttesting.

### Anbefaling
Alejandros `payment-terminal-library`-modul er det riktige svaret for EHL core-integrasjon. Den er designet som et library (ikke en HTTP-service), med et rent `TerminalService`-interface som kan injiseres direkte i Spring-konteksten. Dette eliminerer behovet for HTTP mellom EHL core og betalingsterminalen.

**Neste steg:**
- Publiser `payment-terminal-library` som en lokal Maven-artifact eller inkluder den som et Gradle-modul i lpg-ehl
- Erstatt `SimulatedTerminalClient` i lpg-ehl-service med en adapter som wrapper Alejandros `TerminalService`
- Behold Kotlin-simulatoren for utviklingsmiljøet

---

## 2. Live-test mot serveren (ARKen)

### Serverinformasjon
- **Host:** saklink.tplinkdns.com (SSH port 2222)
- **OS:** Debian 12 (Linux 6.1.0-42-amd64)
- **Mono:** 6.8.0.105
- **Serverbane:** `/home/thomas/payment-terminal/PaymentTerminalNetsCloud/`
- **Konfigurert port:** 18081 (NB: ikke 18080 som antatt!)
- **Bind address:** 127.0.0.1 (kun lokal tilgang)

### Systemressurser
- Oppetid: 3+ timer
- Disk: 64% brukt (2.1GB ledig av 6.1GB)
- Minne: 1.7GB ledig av 1.9GB — Ingen problemer

### Testresultater

#### ✅ GET /health
```json
{"status":"ok","timestamp":"2026-02-14T14:01:19.1521690Z","configLoaded":true}
```
**Observasjoner:**
- Lowercase-nøkler som forventet (`status`, `timestamp`, `configLoaded`)
- Server kjører og config er lastet

#### ✅ GET /v1/terminal/status (før open)
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": false,
  "terminalReady": false,
  "lastError": null,
  "terminalIdentity": {"terminalID": "42696609"}
}
```
**Observasjoner:**
- Camel case-nøkler (via CamelCasePropertyNamesContractResolver)
- `connectionState` mangler i responsen (!) — dette er en forskjell fra simulatoren
- `terminalIdentity` returneres alltid, også før open (med statisk terminalID fra config)

#### ❌ POST /v1/terminal/open
```json
{"success":false,"message":null,"error":"Connect@Cloud login failed: 401 Unauthorized"}
```
**Viktig funn:** Nets Cloud-legitimasjonen i server.json er utløpt eller ugyldig. Brukernavn `cranberries_shared` med passord i klartext. Dette må oppdateres.

#### ❌ POST /v1/payments/purchase (terminal not ready)
```json
{
  "success": false,
  "operationId": null,
  "startedAt": "0001-01-01T00:00:00",
  "callResult": 0,
  "error": "Terminal is not ready",
  "errorCode": "terminal_not_ready",
  ...alle andre felter null/0...
}
```
**Viktige observasjoner for simulator-sammenligning:**
1. Server returnerer **alle felter** i OperationResponse, inkludert null-verdier
2. `operationId` er `null` (ikke en tom string)
3. `startedAt` er `"0001-01-01T00:00:00"` (C# DateTime.MinValue) — ikke null
4. `callResult` er `0` (int default), `localModeResult` er `0`
5. HTTP-statuskode mangler (men basert på kode: 503 for terminal_not_ready)

#### ✅ GET /v1/events?since=0
```json
[]
```
Tom array (ingen hendelser ennå — forventet).

#### ❌ GET /v1/diag/schema (diagnostikk deaktivert)
```json
{
  "error": "Diagnostics are disabled",
  "errorCode": "diagnostics_disabled",
  "operationId": null,
  "details": null
}
```
Korrekt ErrorResponse-format.

---

## 3. Simulator vs. ekte server — Avviksanalyse

### 3.1 JSON-casing

| Aspekt | Ekte server (C#) | Simulator (Kotlin) | Avvik? |
|--------|------------------|---------------------|--------|
| Health endpoint | lowercase (`status`, `configLoaded`) | lowercase (manuelt bygget) | ✅ Korrekt |
| Terminal status | camelCase (`terminalOpen`, `vendorDllLoadable`) | PascalCase (`TerminalOpen`, `VendorDllLoadable`) | ⚠️ **AVVIK** |
| Operation response | camelCase (`success`, `operationId`, `callResult`) | PascalCase (`Success`, `OperationId`, `CallResult`) | ⚠️ **AVVIK** |
| Simple response | camelCase (`success`, `message`, `error`) | PascalCase (`Success`, `Message`, `Error`) | ⚠️ **AVVIK** |
| Error response | camelCase (`error`, `errorCode`) | PascalCase (`Error`, `ErrorCode`) | ⚠️ **AVVIK** |

**Alvorlighetsgrad: MEDIUM-HØY**

Serveren bruker `CamelCasePropertyNamesContractResolver` i Newtonsoft.Json, som konverterer alle C#-properties (PascalCase) til camelCase i JSON-output. Simulatoren bruker PascalCase direkte i Kotlin data class-feltnavn (`val Success: Boolean`).

`SimulatedTerminalClient` i lpg-ehl-service håndterer dette ved å sjekke begge casing-varianter (`readBoolean(json, "Success", "success")`), men dette er en workaround, ikke en korrekt løsning.

**Fix:** Legg til Jackson-konfigurasjon i simulatoren: `PropertyNamingStrategies.LOWER_CAMEL_CASE`.

### 3.2 Respons-felter

#### OperationResponse — felt som server returnerer men simulator mangler/avviker

| Felt | Ekte server | Simulator | Status |
|------|-------------|-----------|--------|
| `connectionState` | Returnert i status | Returnert | ✅ |
| `startedAt` ved feil | `"0001-01-01T00:00:00"` (DateTime.MinValue) | Faktisk tidspunkt | ⚠️ Avvik |
| `operationId` ved feil | `null` | Generert UUID | ⚠️ Avvik |
| `dbRowId` | Returnert (SQLite row ID) | Aldri returnert | ⚠️ Avvik |
| `receiptFileId` | Returnert (filnavn) | `"<opId>.txt"` | ✅ Tilnærmet |
| `reportFields` | Parsed fra kvittering | Aldri returnert for admin | ⚠️ Avvik |
| Null-felter | Inkludert med `null` verdi | Noen utelatt | ⚠️ Avvik |
| `localModeResult` (admin success) | Varierer basert på faktisk resultat | Alltid `1` | ⚠️ Avvik |

#### TerminalStatusResponse — avvik

| Felt | Ekte server | Simulator |
|------|-------------|-----------|
| `connectionState` | Faktisk WS-tilstand ("Open", "Aborted", "None") | State machine-basert ("Open"/"None") |
| `terminalIdentity` | Alltid returnert (fra config) | Kun når open | ⚠️ |
| `lastError` | Faktisk feilmelding fra Nets Cloud | Alltid null | ⚠️ |

### 3.3 HTTP Status-koder

| Scenario | Ekte server | Simulator | Match? |
|----------|-------------|-----------|--------|
| Godkjent purchase | 200 | 200 | ✅ |
| Avvist purchase | 200 (men `success=false`) | 422 (UNPROCESSABLE_ENTITY) | ❌ **AVVIK** |
| Terminal busy | 409 | 409 | ✅ |
| Terminal not ready | 503 | 503 (via exception handler) | ✅ |
| Operation timeout | 408 | 408 | ✅ |

**Kritisk avvik:** Ekte server returnerer **200** for avviste operasjoner (wrong PIN, user cancel, declined) med `success=false` i body. Simulatoren returnerer **422**. `SimulatedTerminalClient` i lpg-ehl-service mapper kun på `success`-feltet i JSON-body, men klienter som baserer seg på HTTP-statuskode vil oppføre seg feil.

### 3.4 Funksjonelle avvik

#### Operasjonslås (OperationLock)
- **Server:** Bruker en TryAcquire/Release-pattern. Returnerer `terminal_busy` error.
- **Simulator:** Bruker `beginOperation()` som kaster `TerminalBusyException`. Effektivt likt, men:
  - Server returnerer full OperationResponse med errorCode="terminal_busy"
  - Simulator returnerer ErrorResponse (annet format)

#### Auto-reopen ved terminal_not_ready
- **Server:** Forsøker automatisk `_adapter.Open()` før purchase/refund/cashback/admin
- **Simulator:** Gjør ingenting — kaster TerminalNotReadyException direkte
- **Impakt:** PumpPaymentOrchestrator håndterer dette selv, men direkte klienter kan oppleve forskjell

#### Idempotency (clientRequestId)
- **Server:** Sjekker database for eksisterende operasjon, returnerer den direkte
- **Simulator:** Bruker in-memory ConcurrentHashMap (mistes ved restart)
- **Impakt:** Lav for testing, men viktig å vite

#### PreAvstemming
- **Server:** Full implementasjon med egen PreAvstemmingOrchestrator
- **Simulator:** Ignorert (ingen støtte)
- **Impakt:** Middels — viktig for produksjon

#### BusyRetry-logikk
- **Server:** Har innebygd busy-retry i `RunTransferAmountWithBusyRetry()` (konfigurert via `busyRetry` i server.json)
- **Simulator:** Ingen retry — returnerer umiddelbart

#### SSE Event Stream
- **Server:** Sender `event: connected` med `data: {"since":"<cursor>"}`, deretter keepalive-kommentarer
- **Simulator:** Sender heartbeat via Spring SseEmitter
- **Impakt:** Formatforskjeller kan bryte SSE-klienter

### 3.5 Ekstra endpoints i simulatoren (ikke i ekte server)

- `POST /v1/payments/reservation` — Reservasjonsflyt (pre-auth)
- `POST /v1/payments/completion` — Fullføringsendpoint for reservasjoner

Disse finnes **ikke** i den ekte serveren eller OpenAPI-spesifikasjonen. De er lagt til i simulatoren for fremtidig bruk.

---

## 4. Integrasjonsstrategi for lpg-ehl-service

### Nåværende arkitektur
```
lpg-ehl-service
  └── terminal/
      ├── TerminalClient.kt (interface)
      ├── SimulatedTerminalClient.kt (HTTP-klient mot sim/server)
      ├── TerminalConfiguration.kt (Spring config)
      ├── PumpPaymentOrchestrator.kt (forretningslogikk)
      ├── TerminalEventPoller.kt
      └── TerminalPumpCompletionListener.kt
```

### Anbefalt arkitektur

#### Alternativ A: Bruk Alejandros library direkte (ANBEFALT)
```
lpg-ehl-service
  └── terminal/
      ├── TerminalClient.kt (interface — beholdes)
      ├── NetsCloudTerminalClient.kt (NY — wrapper rundt Alejandros TerminalService)
      ├── SimulatedTerminalClient.kt (beholdes for dev)
      ├── TerminalConfiguration.kt (oppdatert — velger implementasjon basert på profil)
      ├── PumpPaymentOrchestrator.kt (uendret)
      └── ...

Dependency:
  lpg-ehl-service -> payment-terminal-library (Gradle/Maven)
```

**Implementasjon:**
```kotlin
@Component
@ConditionalOnProperty(name = ["payment.terminal.provider"], havingValue = "netscloud")
class NetsCloudTerminalClient(
    private val terminalService: TerminalService  // Fra Alejandros library
) : TerminalClient {
    
    override fun openTerminal(): TerminalSimpleResponse {
        val result = runBlocking { terminalService.open() }
        return TerminalSimpleResponse(
            success = result.success,
            message = result.message,
            error = result.error
        )
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        val netsRequest = PurchaseRequest(
            amountMinor = request.amountMinor,
            operatorId = request.operatorId,
            currency = request.currency,
            optionalData = request.optionalData,
            clientRequestId = request.clientRequestId
        )
        val result = runBlocking { terminalService.purchase(netsRequest) }
        return mapToTerminalOperationResponse(result)
    }
    // ... etc
}
```

**Fordeler:**
- Ingen HTTP overhead mellom EHL core og terminal
- Direkte tilgang til events via `TerminalService.eventStream()`
- Alle Nets Cloud-protokolldetaljer er innkapslet
- Kan kjøre i samme JVM/prosess

**Ulemper:**
- Krever at library bygges med kompatibel Kotlin/Java-versjon
- Introduserer H2-avhengighet (kan konfigureres)

#### Alternativ B: Behold HTTP-basert integrasjon
Kun aktuelt hvis Mono-serveren skal fortsette å kjøre som separat prosess. **Ikke anbefalt** for produksjon.

### Konfigurasjonsforslag (application.yaml)
```yaml
payment:
  terminal:
    enabled: true
    provider: netscloud  # eller "simulator"
    # Brukes kun med provider=simulator
    base-url: http://localhost:18080
    # Brukes kun med provider=netscloud
    netscloud:
      environment: PROD
      base-url: https://connectcloud.aws.nets.eu
      username: ${NETS_CLOUD_USERNAME}
      password: ${NETS_CLOUD_PASSWORD}
      terminal-id: "42696609"
```

---

## 5. Kritiske funn og handlingspunkter

### 🔴 Kritisk

1. **Nets Cloud-legitimasjon utløpt** — server.json på ARKen har ugyldig passord. Terminalen kan ikke åpnes. Oppdater med gyldig legitimasjon fra Nets.

2. **JSON casing-mismatch i simulatoren** — Simulatoren bruker PascalCase mens ekte server bruker camelCase. Legg til Jackson-konfigurasjon for å fikse dette.

3. **HTTP 200 vs 422 for avviste operasjoner** — Simulatoren returnerer 422 for avviste betalinger, men ekte server returnerer 200. Service-klienten håndterer det via `success`-feltet, men dette kan forstyrre andre integrasjoner.

### 🟡 Viktig

4. **Sikkerhetsrisiko** — Passord ligger i klartext i server.json på serveren. Bør flyttes til miljøvariabel eller secrets manager.

5. **Ingen systemd-service** — Serveren startes manuelt og overlever ikke reboot. Opprett en systemd-unit.

6. **Simulatoren mangler preAvstemming** — Viktig for LPG-stasjoner som trenger avstemming før kjøp.

7. **Simulatoren mangler auto-reopen** — Ekte server prøver automatisk å reopne terminalen. Simulatoren gjør det ikke.

### 🟢 Lavprioritet

8. Simulatoren har reservation/completion-endpoints som ikke finnes i serveren — OK for fremtidsplanlegging
9. EventStore-format divergerer mellom simulator og server
10. `lastError` i terminalstatus er alltid null i simulatoren

---

## 6. Sammendrag

### Er simulatoren 100% riktig?
**Nei**, men den er tilstrekkelig for utviklingsarbeid. De viktigste avvikene er:

1. **JSON-casing** (PascalCase vs camelCase) — mest kritisk, lett å fikse
2. **HTTP-statuskoder** for avviste operasjoner (422 vs 200)
3. **Mangel på preAvstemming og auto-reopen**
4. **Null-håndtering** (server returnerer alle felter, simulator utelater noen)

### Anbefalt prioritert handlingsliste

1. ✅ Fiks JSON-casing i simulatoren (1 time)
2. ✅ Rett HTTP-statuskoder for avviste operasjoner (30 min)
3. ✅ Oppdater Nets Cloud-legitimasjon på ARKen
4. ✅ Opprett systemd-service for automatisk oppstart
5. 🔜 Integrer Alejandros `payment-terminal-library` i lpg-ehl-service
6. 🔜 Legg til preAvstemming-støtte i simulatoren
7. 🔜 Legg til auto-reopen-logikk i simulatoren
