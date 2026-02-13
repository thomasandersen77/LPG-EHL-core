# Payment Terminal Simulator & Service Integration
## Problem
Simulatoren (`lpg-ehl-payment-terminal-sim`) og service-integrasjonen (`lpg-ehl-service/terminal/`) har avvik fra C# produksjonsserveren (`PaymentTerminalNetsCloudMonoServer`). Disse avvikene gjør det vanskelig å utvikle og teste uten tilgang til ekte terminal.
## Testet mot ekte server (port 18081)
* **Health**: OK – `{"status":"ok","configLoaded":true}`
* **Terminal status**: camelCase – `{"vendorDllLoadable":true,"terminalOpen":false,"terminalReady":false,"connectionState":"None"}`
* **Terminal open**: Auth lyktes (ingen 401), men WebSocket gikk til `Aborted` – terminalen er trolig av eller tilkoblet fra annet ECR
* **Viktig observasjon**: C# bruker `CamelCasePropertyNamesContractResolver` → alle JSON-nøkler er **camelCase**, IKKE PascalCase
## Del 1: Simulator-forbedringer (lpg-ehl-payment-terminal-sim)
### 1.1 Legg til manglende felt i OperationResponse
Fil: `lpg-ehl-payment-terminal-sim/.../model/response/OperationResponse.kt`
Legg til disse feltene som C# returnerer men simulatoren mangler:
* `LocalModeResultData: String?` – Semicolon-delimited raw data (f.eks. `"D  ;card_hash;2;more_data"`)
* `EntryMode: String?` – "CHIP", "CONTACTLESS", "UNKNOWN_X"
* `EntryModeCode: String?` – "0"=CHIP, "2"=CONTACTLESS
Oppdater `OperationResponse.approved()` factory-metoden til å inkludere:
* `EntryMode = "CONTACTLESS"` (default for simulator)
* `EntryModeCode = "2"`
* `LocalModeResultData = "D  ;************8408;2;..."` (mock data med riktig format)
### 1.2 Legg til ConnectionState i TerminalStatusResponse
Fil: `lpg-ehl-payment-terminal-sim/.../model/response/TerminalStatusResponse.kt`
Legg til:
* `ConnectionState: String? = null` – Mulige verdier: "None", "Open", "Aborted", "Closed"
Fil: `lpg-ehl-payment-terminal-sim/.../service/TerminalStateManager.kt`
Legg til metode `getConnectionState(): String` som mapper TerminalState til connection state:
* CLOSED → "None"
* OPEN → "Open"
* READY → "Open"
* BUSY → "Open"
Fil: `lpg-ehl-payment-terminal-sim/.../controller/TerminalController.kt`
Oppdater `getStatus()` til å inkludere `ConnectionState`.
### 1.3 Rett admin-successlogikk
C# server bruker `LocalModeResult = 1` for admin-suksess (ikke null/0).
Fil: `lpg-ehl-payment-terminal-sim/.../model/response/OperationResponse.kt`
Oppdater `adminSuccess()` factory-metoden:
* Sett `LocalModeResult = 1` (i stedet for null)
### 1.4 camelCase-kompatibilitetsprofil (valgfritt)
C# serveren returnerer camelCase, simulatoren returnerer PascalCase.
Legg til konfigurasjon i `SimulatorConfig`:
* `responseCasing: String = "PascalCase"` – Støtt verdier "PascalCase" og "camelCase"
I JacksonConfig, velg naming strategy basert på config.
Dette er lavere prioritet fordi `SimulatedTerminalClient` allerede leser begge varianter.
## Del 2: Service-modul (lpg-ehl-service/terminal/)
### 2.1 Utvid TerminalOperationResponse med nye felt
Fil: `lpg-ehl-service/.../terminal/TerminalClient.kt`
Legg til i `TerminalOperationResponse`:
* `entryMode: String?` – CHIP/CONTACTLESS
* `entryModeCode: String?` – "0"/"2"
* `localModeResultData: String?`
* `responseCode: String?` – "00"=approved, "Z1"=wrong PIN
* `rejectionReason: String?`
* `printTextRaw: String?` – kvitteringstekst
* `printTextSanitized: String?`
* `lastDisplayText: String?` – siste display-tekst
* `localModeResult: Int?`
* `durationMs: Long?`
### 2.2 Utvid SimulatedTerminalClient til å lese nye felt
Fil: `lpg-ehl-service/.../terminal/SimulatedTerminalClient.kt`
I `purchase()` metoden, les alle nye felt fra JSON-response:
* `entryMode`, `entryModeCode`, `localModeResultData`
* `responseCode`, `rejectionReason`
* `printTextRaw`, `printTextSanitized`, `lastDisplayText`
* `localModeResult`, `durationMs`
Bruk `readText(json, "PascalCase", "camelCase")` mønsteret som allerede brukes.
### 2.3 Legg til status-polling i TerminalClient
Legg til i `TerminalClient`-interfacet:
* `fun getHealth(): TerminalHealthResponse`
* `fun getStatus(): TerminalStatusResponse`
* `fun closeTerminal(): TerminalSimpleResponse`
Nye DTOer:
```warp-runnable-command
data class TerminalHealthResponse(val status: String, val configLoaded: Boolean)
data class TerminalStatusResponse(val terminalOpen: Boolean, val terminalReady: Boolean, val connectionState: String?, val lastError: String?)
```
### 2.4 Robust terminal-lifecycle i PumpPaymentOrchestrator
Fil: `lpg-ehl-service/.../terminal/PumpPaymentOrchestrator.kt`
Forbedre `openTerminalAndPurchase()`:
1. Sjekk status FØRST med `getStatus()` → hvis allerede ready, skip open
2. Hvis `connectionState == "Aborted"`, close og re-open
3. Etter open, poll status inntil `terminalReady=true` (maks 60s)
4. Først da, utfør purchase
Legg til retry-logikk:
* Ved `terminal_busy` (409): vent 1-2s, prøv igjen (maks 3 ganger)
* Ved `terminal_not_ready` (503): forsøk open → poll → retry
### 2.5 Event-polling for display-oppdateringer
Fil: `lpg-ehl-service/.../terminal/TerminalEventPoller.kt` (eksisterer allerede)
Sikre at event-poller leser DisplayText-events og logger dem, slik at operatøren kan se "SETT INN KORTET", "Kode + OK" etc. i sanntid.
## Del 3: Anbefalt integrasjonsflyt
### Happy path: LPG Purchase
```warp-runnable-command
1. GET  /v1/terminal/status     → sjekk terminalReady
2. POST /v1/terminal/open        → (kun hvis !terminalReady)
3. Poll /v1/terminal/status      → vent til terminalReady=true
4. POST /v1/payments/purchase    → amountMinor=<beløp i øre>
   ↓ (venter 5-30 sekunder – terminal interagerer med kunde)
5. Respons: success=true, responseCode="00"
6. Start pumping via EHL-protokoll
7. Pump stopper → regn ut faktisk beløp
8. (Eventuelt: reversal hvis pumping feiler)
```
### Error handling
```warp-runnable-command
Purchase feiler:
- errorCode="terminal_busy"      → vent 1-2s, retry
- errorCode="terminal_not_ready"  → open → poll → retry
- errorCode="operation_rejected"  → vis feilmelding, tillat nytt forsøk
- errorCode="operation_timeout"   → sjekk events, eventuelt retry
- errorCode="vendor_call_failure" → alvorlig feil, logg og varsle
```
### Reservation-flyt (alternativ for LPG)
```warp-runnable-command
1. POST /v1/payments/reservation  → reserve maks-beløp
2. Start pumping
3. Pump stopper → regn ut faktisk beløp
4. POST /v1/payments/completion   → charge faktisk beløp ≤ reservert
```
Denne flyten er kun implementert i simulatoren, ikke i C# serveren.
## Implementasjonsrekkefølge
1. **Simulator-forbedringer** (Del 1.1–1.3) – rask, ingen risiko
2. **Service DTO-utvidelser** (Del 2.1–2.2) – utvider eksisterende kode
3. **Terminal lifecycle** (Del 2.3–2.4) – viktigst for robusthet
4. **Event-polling** (Del 2.5) – nice-to-have for UX
5. **camelCase-profil** (Del 1.4) – lavest prioritet
