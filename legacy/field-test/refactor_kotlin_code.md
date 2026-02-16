Perfekt. Her får du alle fire – optimalisert for ulike bruksområder.

---

### 1️⃣ Minimal “Cursor Chat Version” (kort og presis)

Bruk denne når du sitter i Cursor og vil ha raske, kontrollerte endringer uten overengineering.

#### 🎯 Mål

Sikre full felt-paritet mellom Python (`test-python`) og Kotlin (`lpg-ehl-*`) uten arkitekturendringer.

Ikke redesign. Kun herding.

#### P0 – Serial parity må fikses

Python åpnet pumpe med `9600 8N1`.
Kotlin har `8E1` default + hardcoded `EVEN` i adapter.

Gjør:

- Fjern ALL hardcoded parity (spesielt i `RealSerialPortAdapter`)
- Bruk kun `SerialPortConfig` som source-of-truth
- Default feltprofil = `9600 / 8N1`
- Logg resolved serial config ved startup

Ikke gjør noe annet før dette er korrekt.

#### P1 – UNBLOCK timeout diagnose

Hvis `verifyOpenForDelivery` timer ut:

- Logg siste observerte `STATE`-byte + hvilke frames som ble ignorert.

Ikke implementer global echo-drop.
Ikke bruk nanoTime-barriere.

#### P1 – Serial simulator dirty mode

Legg til knobs:

- `localEchoProbability`
- `staleVolumeAfterUnblockProbability`
- `randomNoiseBurstProbability`
- `fragmentResponseProbability`

Logg når en fault trigges.

#### P2 – Terminal simulator

Implementer per-transaction state machine:

`IDLE → WAITING_FOR_CARD → AUTHORIZING → APPROVED/DECLINED → CAPTURED`

Legg til:

- `slowAuthorizationProbability`
- `offlineProbability`
- `duplicateEventProbability`

Ikke legg state machine i klient – kun i simulator.

#### Returner

- Liste over endrede filer
- Kort forklaring på hva som ble gjort

---

### 2️⃣ Super-streng CI Patch Generation Prompt

Bruk denne når du vil ha deterministisk patch fra AI.

Du skal generere en kompilerbar patch for Kotlin-prosjektet.

Ikke redesign arkitektur. Kun følgende tillatt:

#### P0 – Serial SSOT

1. Fjern hardcoded parity i:
   - `RealSerialPortAdapter`
2. Sørg for at `SerialPortConfig` styrer:
   - `baud`
   - `dataBits`
   - `parity`
   - `stopBits`
3. Default feltprofil:
   - `9600`
   - `8`
   - `NONE`
   - `1`
4. Legg til startup-log:

`Serial profile resolved: ...`

#### Akseptansekriterier

- Ingen hardcoded `EVEN` finnes igjen
- Parity kan styres via Spring config
- Prosjektet kompilerer

#### P1 – Timeout Diagnose

I `PumpStateService`:

Hvis unblock verify feiler:

Logg:

- siste `STATE` byte
- hvilke frame-typer som ble ignorert

Ingen nye dependencies.
Ingen globale filtre.

#### P1.5 – Serial simulator

Legg til config properties:

- `localEchoProbability`
- `staleVolumeAfterUnblockProbability`
- `fragmentResponseProbability`
- `randomNoiseBurstProbability`

Krav:

- Startup dump av aktive knobs
- Logging når fault trigges
- Default = `0`

#### Outputformat

Returner:

1. Liste over endrede filer
2. Unified diff patch
3. Hvordan teste lokalt

Ikke returner forklaringer.
Kun patch og testinstruksjoner.

---

### 3️⃣ Ren Simulator-Fokusert Prompt

Bruk denne når du kun vil gjøre simulator “Drammen-skitten”.

Du skal forbedre `lpg-ehl-serialport-sim`.

Mål: Simulere ekte RS-485 feltoppførsel på ARK-maskin.

Ikke endre EHL-protokoll.
Ikke endre frame-format.

#### Implementer følgende fault knobs

1. **Local Echo**

Sannsynlighet for å sende eksakt mottatte bytes tilbake før svar.

2. **Stale Injection**

- 5% sjanse for å sende gammel `VOLUME` rett etter `UNBLOCK`
- 5% sjanse for å sende gammel `STATE` før ny `STATE`

3. **Fragmentering**

Del gyldig response i 2–3 tilfeldige chunks.

4. **Noise Burst**

Send 3–10 tilfeldige bytes før gyldig `STX`.

5. **CRC corruption**

Konfigurerbar sannsynlighet.

#### Krav

- Alle knobs skal kunne styres via properties
- Ved startup: logg aktive knobs
- Ved trigging: logg hvilken fault som ble injisert
- Snill modus = alle `0`

#### Returner

- Nye properties
- Endrede klasser
- Eksempel config for “dirty mode”

---

### 4️⃣ Terminal-Simulator-Only Prompt

Bruk denne når du vil gjøre betalingssimulator realistisk.

Du skal forbedre `lpg-ehl-payment-terminal-sim`.

Mål: Realistisk selvbetjent-stasjon.

Ikke endre API-kontrakt.
Ikke endre OpenAPI.

#### Implementer per-transaction state machine

- `IDLE`
- `WAITING_FOR_CARD`
- `AUTHORIZING`
- `APPROVED | DECLINED`
- `CAPTURED`
- `REVERSED`

#### Fault knobs

- `slowAuthorizationProbability`
- `offlineProbability`
- `duplicateEventProbability`
- `outOfOrderEventProbability`
- `stuckInAuthorizingProbability`

#### Krav

- State machine ligger i simulator, ikke i client
- Alle faults kan aktiveres via config
- Logging av:
  - state transitions
  - injected faults
  - Kontrakt fortsatt kompatibel med `openapi-payment-terminal.yaml`

#### Returner

- Nye state enums
- Nye config properties
- Endrede services
- Eksempel “dirty mode” config

---

#### 🎯 Oppsummert

Du har nå:

1. ⚡ Rask Cursor-versjon
2. 🔒 Streng CI-patch versjon
3. 🔥 Serial dirty-mode prompt
4. 💳 Terminal state-machine prompt

Hvis du vil kan jeg nå lage:

- En “Drammen test-day checklist”
- En produksjons readiness audit
- Eller en ekstrem “industrial hardening” versjon som tar deg fra 95% → 99.9% robusthet

Hva er neste steg – simulator i kveld eller parity-fiks først?

---

## Scope reviewed
- Python reference: `test-python/` (scripts + `python-server/server.py`) and `_field-test/PYTHON_KOTLIN_ALIGNMENT_ANALYSIS.md`
- Kotlin modules: `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-service`, `lpg-ehl-webapp`, `lpg-ehl-emulator`, `lpg-web`
- Simulators: `lpg-ehl-serialport-sim`, `lpg-ehl-payment-terminal-sim`

---

### 1) Core protocol alignment (Python vs Kotlin)

#### ✅ Strong alignment points
1. **Frame protocol (Norges Gass variant) is aligned**
    - Python: STX controller/dispenser = `0x10/0x20`, ETX = `0x36`, XOR checksum.
    - Kotlin core: `EhlProtocolConfig` default variant = `NORGES_GASS` (`0x10/0x20/0x36`), codec logic matches XOR framing.

2. **UNBLOCK verification strategy is aligned with Python field behavior**
    - Python `unblock_verified()`: send UNBLOCK, then verify with STATE polling (`open_for_delivery` bit).
    - Kotlin `PumpStateService.unblock()`: `withExclusive { drain(100); send(UNBLOCK); send(STATE)+receiveUntil(...) loop }` until `OPEN_FOR_DELIVERY (0x02)`.
    - This matches the critical field reality where dispenser may return STATE instead of OK.

3. **State bit semantics are aligned**
    - Python: `open_for_delivery=0x02`, `startbutton=0x04`, `automode=0x08`.
    - Kotlin `StatusBitMasks`: same values + error bit `0x80`.

4. **Transport robustness patterns exist in Kotlin**
    - `EhlCommunicator`: `withExclusive`, `drain`, `receiveUntil`, retry in `sendAndReceive`, parse recovery for noise.
    - Tests exist (`EhlCommunicatorReceiveUntilTest`) for interleaved VOLUME, concatenated frames, missing OK, delayed open-bit.

#### ⚠️ Important mismatch to resolve
5. **Serial parity mismatch (likely critical in real hardware mode)**
    - Python field kit configures Linux serial as **8N1** (`_set_raw_8n1`), and field scripts default to 9600 8N1.
    - Kotlin `lpg-transport/SerialPortConfig` defaults to **EVEN parity (8E1)**.
    - Kotlin `RealSerialPortAdapter` hardcodes EVEN parity as well.
    - This can break true Python↔Kotlin hardware parity if the dispenser/adapter actually expects 8N1 (as Python success suggests).

**Recommendation (high priority):**
- Make field parity explicit and environment-driven everywhere (no hardcoded EVEN defaults in adapter layer).
- Set production default parity to match validated field profile (if Python-opened pump used 8N1, default to NONE/8N1 for this station profile).

---

### 2) Evaluation of your "Gemini/ChatGPT refactoring suggestions" text

#### Overall verdict: **Largely well aligned with actual Python behavior and Kotlin needs**

Your critique is mostly correct and nuanced. Specific evaluation:

1. **"Stale data trap" concern** — ✅ valid
    - Kotlin has `drain()` and robust parser, but stale/echo data can still leak across command epochs.
    - Suggestion to add logical TX barriers/epochs is directionally good.

2. **Watchdog based on passive silence is wrong** — ✅ valid and already partially handled
    - `SerialPortManager.checkWatchdog()` is attempt-window based (silence alone is tolerated), which aligns with your point.
    - However `HardwareWatchdogService` logs `timeSinceLastData`, so policy docs should clearly emphasize attempts/failures as primary signal.

3. **Last-seen-state on timeout** — ✅ useful, not fully present in user-facing diagnostics
    - Would significantly improve field triage.

4. **Echo filter byte-for-byte absolute rule is risky** — ✅ your nuance is correct
    - Must be heuristic (timing + direction + context), not global hard drop.

5. **Timestamp nanoTime barrier limits** — ✅ correct nuance
    - Better to do logical command epoching than assume precise physical arrival ordering at packet-level callbacks.

6. **Global ignore of controller STX** — ✅ your caution is correct
    - Contextual filtering during expected response windows is preferable to global drop.

7. **PascalCase strategy** — ✅ your nuance is correct
    - Service side already maps with `@JsonProperty` in DTOs.
    - Simulator side also uses PascalCase model directly (`EventEnvelope` with PascalCase fields), so naming mismatch risk is low for that contract.

---

### 3) Simulator realism vs hardware behavior

## 3A) `lpg-ehl-serialport-sim` (pump/serial simulator)

#### ✅ Good current capabilities
- Field profile knobs already exist: drop response, concat frames, inter-character delay, optional no-ack-on-unblock/block, unsolicited volume bursts.
- EHL frame codec and binary mode implemented.

#### Gaps vs harsh real-world RS-485 behavior
- No explicit **local echo** injection knob (self-hearing TX).
- No explicit **stale previous transaction frame injection** (e.g., stale VOLUME right after UNBLOCK).
- No explicit **fragment response probability** abstraction (although chunking exists, but not tightly modeled per-response fault mode).
- No explicit **random noise burst before frame**.
- CRC/checksum corruption exists via `state.shouldCorruptChecksum()` path, but needs clearer config observability at startup and per-event logging consistency.

**Recommendation:** Implement your proposed Drammen-style knobs with controlled defaults and startup log dump of all active fault knobs.

## 3B) `lpg-ehl-payment-terminal-sim`

#### ✅ Strengths
- Structured Spring app with scenario management, state manager, event store, admin endpoints.
- PascalCase event envelope in simulator output model aligns with terminal contract style.

#### Gaps relative to proposed realistic terminal simulation
- `lpg-ehl-service` `SimulatedTerminalClient` is still thin HTTP wrapper (reserve/capture/reversal), not a full robust transaction-state simulator client contract in itself.
- Terminal sim state manager (`CLOSED/OPEN/READY/BUSY`) is infrastructure-level terminal state, not full per-transaction lifecycle (`WAITING_FOR_CARD`, `AUTHORIZING`, `APPROVED/DECLINED`, `SETTLED`, etc.).
- Missing explicit fault knobs for out-of-order/duplicate events, slow responses, stuck-in-waiting, offline probabilities in a unified model exposed to tests.
- Contract tests against `openapi-payment-terminal.yaml` are not evident in reviewed files.

**Recommendation:** Your prompt for terminal simulator upgrade is well justified and should be treated as a roadmap item.

---

### 4) Cross-module integration observations

1. **`lpg-ehl-webapp` is mostly orchestration/config wrapper**
    - Correctly wires field/lab transport profiles.

2. **Potential config conflict risk**
    - `TransportConfiguration` supports configurable parity string.
    - But `RealSerialPortAdapter` still hardcodes EVEN parity; if this bean path is used anywhere, it can silently diverge from validated Python behavior.

3. **Python open-pump success path is now mirrored in Kotlin service logic**
    - UNBLOCK verification via STATE open bit is in place.

---

### 5) Are you aligned with Python that opened dispenser pump?

## **Short answer: Yes—mostly aligned, with one critical caveat.**

You are aligned on the most important behavioral points:
- UNBLOCK success criteria via STATE open bit,
- robust handling of missing OK/interleaved frames,
- contextual critique of echo/timing assumptions,
- watchdog philosophy for low-traffic installations.

### Critical caveat to fix before claiming full hardware parity:
- **Ensure serial parity/line settings in Kotlin field runtime match the exact Python-tested hardware settings (likely 8N1).**
- Remove/avoid hardcoded 8E1 defaults in paths that may be used in production field mode.

With that addressed, your current direction is strongly consistent with the Python implementation that successfully opened the pump.

---

### 6) Concrete refactoring actions (prioritized)

## P0 (Do now)
1. **Unify serial settings source-of-truth**
    - Eliminate hardcoded EVEN parity in `RealSerialPortAdapter`.
    - Use shared config object everywhere.
    - Add startup log line: `serial profile resolved: baud=X dataBits=Y parity=Z stopBits=W`.

2. **Add field profile presets matching validated Python setup**
    - e.g., `field-profile=norgesgass-python-validated` => 9600/8N1 unless site requires otherwise.

## P1 (Next)
3. **Add command-epoch barrier metadata in transport/service logs**
    - Command id/epoch for send + receiveUntil filtering/diagnostics.

4. **Enhance timeout diagnostics**
    - Return/log last-seen STATE + raw bytes when verify loops timeout.

5. **Serial simulator fault knobs expansion**
    - localEchoProbability, staleInjectionProbability, staleStateProbability,
      fragmentResponseProbability, randomNoiseBurstProbability,
      explicit per-trigger logging.

## P2 (After P1)
6. **Terminal simulator realism uplift**
    - Per-transaction state machine and deterministic dirty-mode knobs.
    - Contract tests vs `openapi-payment-terminal.yaml` examples.

---

### 7) Final verdict

- The **main Kotlin pump-control path is now conceptually aligned** with the proven Python approach that opened the dispenser.
- Your proposed critique/refactoring directions are **substantially correct and pragmatic**.
- To reach practical field parity confidence, prioritize **serial parity/config unification** and then simulator dirty-mode enhancements.

---

### 8) Payment terminal API capture (WireMock)

# Payment Terminal API — WireMock Capture Report

**Date:** 2026-02-12  
**Server:** `http://127.0.0.1:8080`  
**OpenAPI spec:** `openapi-payment-terminal.yaml` (v1.0.0)  
**Capture method:** Direct curl requests against live server, responses used to generate WireMock stubs.

## Summary

| # | Endpoint | Method | HTTP Status | Stub File |
|---|----------|--------|-------------|-----------|
| 1 | `/health` | GET | 200 | `01-health-get-200.json` |
| 2 | `/v1/terminal/status` | GET | 200 | `02-terminal-status-get-200.json` |
| 3 | `/v1/terminal/open` | POST | 503 | `03-terminal-open-post.json` |
| 4 | `/v1/terminal/close` | POST | 200 | `04-terminal-close-post-200.json` |
| 5 | `/v1/payments/purchase` | POST | 503 | `05-purchase-post-503.json` |
| 6 | `/v1/payments/refund` | POST | 503 | `06-refund-post-503.json` |
| 7 | `/v1/payments/cashback` | POST | 503 | `07-cashback-post-503.json` |
| 8 | `/v1/admin/avstemming` | POST | 503 | `08-admin-avstemming-post-503.json` |
| 9 | `/v1/admin/cancel` | POST | 503 | `09-admin-cancel-post-503.json` |
| 10 | `/v1/admin/reversal` | POST | 503 | `10-admin-reversal-post-503.json` |
| 11 | `/v1/admin/z-report` | POST | 503 | `11-admin-z-report-post-503.json` |
| 12 | `/v1/admin/last-receipt` | POST | 503 | `12-admin-last-receipt-post-503.json` |
| 13 | `/v1/admin/software` | POST | 503 | `13-admin-software-post-503.json` |
| 14 | `/v1/admin/dataset` | POST | 503 | `14-admin-dataset-post-503.json` |
| 15 | `/v1/admin/code` | POST | 503 | `15-admin-code-post-503.json` |
| 16 | `/v1/events` | GET | 200 | `16-events-get-200.json` |
| 17 | `/v1/events/stream` | GET | 200* | `17-events-stream-get-200.json` |
| 18 | `/v1/diag/schema` | GET | 403 | `18-diag-schema-get-403.json` |
| 19 | `/v1/diag/sendjson` | POST | 403 | `19-diag-sendjson-post-403.json` |
| 20 | `/v1/diag/sendtld` | POST | 403 | `20-diag-sendtld-post-403.json` |
| 21 | `/v1/diag/confirm` | POST | 403 | `21-diag-confirm-post-403.json` |

**Happy-path stubs (simulated):**
- `02-terminal-status-get-200-ready.json` — terminal open & ready
- `03-terminal-open-post-200-happy.json` — successful terminal open
- `05-purchase-post-200-happy.json` — approved purchase
- `06-refund-post-200-happy.json` — approved refund
- `08-admin-avstemming-post-200-happy.json` — successful avstemming

---

## Detailed Endpoint Report

### 1. Health Check

**Endpoint:** `GET /health`  
**Tag:** Health  
**Description:** Server health and readiness check. Returns lowercase keys (exception to PascalCase convention).

**curl:**
```bash
curl -s http://127.0.0.1:8080/health
```

**Response (200):**
```json
{
  "status": "ok",
  "timestamp": "2026-02-12T17:57:10.0474590Z",
  "configLoaded": true
}
```

**Notes:** Always returns 200 as long as the server process is running. Lowercase response keys.

---

### 2. Terminal Status

**Endpoint:** `GET /v1/terminal/status`  
**Tag:** Terminal  
**Description:** Check terminal readiness. Must verify `terminalOpen=true` and `terminalReady=true` before financial operations.

**curl:**
```bash
curl -s http://127.0.0.1:8080/v1/terminal/status
```

**Response (200):**
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": false,
  "terminalReady": false,
  "connectionState": "None",
  "lastError": null,
  "terminalIdentity": {
    "terminalID": "12345678"
  }
}
```

**Notes:**
- `vendorDllLoadable=true` means the Baxi DLL is available.
- `terminalOpen=false` and `terminalReady=false` — the terminal was not connected (missing Connect@Cloud credentials).
- Extra fields not in OpenAPI: `connectionState`, `terminalIdentity`.

---

### 3. Terminal Open

**Endpoint:** `POST /v1/terminal/open`  
**Tag:** Terminal  
**Description:** Initialize the terminal connection. Required before any financial operation.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/terminal/open \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response (503 — captured):**
```json
{
  "success": false,
  "message": null,
  "error": "Connect@Cloud login failed (baseUrl='https://connectcloud-test.aws.nets.eu'): username and password are required"
}
```

**Notes:**
- Requires `CONNECTCLOUD_USERNAME` and `CONNECTCLOUD_PASSWORD` environment variables.
- Server requires `Content-Length` header (send empty `{}` body).
- Returns `SimpleResponse` schema.

---

### 4. Terminal Close

**Endpoint:** `POST /v1/terminal/close`  
**Tag:** Terminal  
**Description:** Close the terminal connection.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/terminal/close \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Response (200 — captured):**
```json
{
  "success": true,
  "message": "Terminal closed",
  "error": null
}
```

**Notes:** Succeeds even when terminal was not open.

---

### 5. Purchase

**Endpoint:** `POST /v1/payments/purchase`  
**Tag:** Payments  
**Description:** Perform a card purchase. Core financial operation.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{
    "AmountMinor": 100,
    "OperatorId": "0000",
    "Currency": "NOK",
    "OptionalData": "WireMock Test",
    "ClientRequestId": "wiremock-capture-001"
  }'
```

**Response (503 — captured, terminal not ready):**
```json
{
  "success": false,
  "operationId": null,
  "startedAt": "0001-01-01T00:00:00",
  "callResult": 0,
  "error": "Connect@Cloud login failed ...: username and password are required",
  "errorCode": "terminal_not_ready"
}
```

**Notes:**
- `AmountMinor` is in øre (100 = NOK 1,00).
- `ClientRequestId` enables idempotent retries. Recommended format: `fuelingSession:<id>`.
- When terminal is not ready, the server returns the full `OperationResponse` schema with all null fields.
- The `terminal_not_ready` check happens before request body validation.

---

### 6. Refund

**Endpoint:** `POST /v1/payments/refund`  
**Tag:** Payments  
**Description:** Refund/return operation.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/payments/refund \
  -H "Content-Type: application/json" \
  -d '{
    "AmountMinor": 50,
    "OperatorId": "0000",
    "OptionalData": "WireMock Refund Test",
    "ClientRequestId": "wiremock-capture-002"
  }'
```

**Response (503):** Same `terminal_not_ready` response as purchase.

**Notes:** Despite VB6 calling this "cashback" in its code, this is the actual refund endpoint.

---

### 7. Cashback (Purchase + Cashback)

**Endpoint:** `POST /v1/payments/cashback`  
**Tag:** Payments  
**Description:** Combined purchase + cashback operation.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/payments/cashback \
  -H "Content-Type: application/json" \
  -d '{
    "PurchaseMinor": 200,
    "CashbackMinor": 100,
    "Currency": "NOK",
    "OperatorId": "4321",
    "ClientRequestId": "wiremock-capture-003"
  }'
```

**Response (503):** Same `terminal_not_ready` response.

**Notes:**
- Uses `PurchaseMinor` and `CashbackMinor` (not `AmountMinor`).
- Default `OperatorId` is `"4321"` (different from purchase/refund which use `"0000"`).

---

### 8. Avstemming (Reconciliation)

**Endpoint:** `POST /v1/admin/avstemming`  
**Tag:** Administration  
**Description:** End-of-day reconciliation. Admin code `0x3130`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/avstemming \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready` — same pattern as all financial/admin operations.

**Notes:** `Password` defaults to `"0000"`.

---

### 9. Cancel Current Operation

**Endpoint:** `POST /v1/admin/cancel`  
**Tag:** Administration  
**Description:** Cancel the current in-flight terminal operation. Admin code `0x3132`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/cancel \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

---

### 10. Reversal

**Endpoint:** `POST /v1/admin/reversal`  
**Tag:** Administration  
**Description:** Reverse the last successful transaction. **Not reversible.** Admin code `0x3134`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/reversal \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

**Notes:** Use when payment succeeded but dispenser communication failed.

---

### 11. Z-Report

**Endpoint:** `POST /v1/admin/z-report`  
**Tag:** Administration  
**Description:** End-of-day Z-report generation. Admin code `0x3137`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/z-report \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

**Notes:** VB6 client appends additional data (technical refunds since last Z) — must be handled client-side.

---

### 12. Last Receipt

**Endpoint:** `POST /v1/admin/last-receipt`  
**Tag:** Administration  
**Description:** Print/retrieve the last financial receipt. Admin code `0x313C`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/last-receipt \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

---

### 13. Software Download

**Endpoint:** `POST /v1/admin/software`  
**Tag:** Administration  
**Description:** Download software update to terminal. Long-running operation. Admin code `0x313E`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/software \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

---

### 14. Dataset Download

**Endpoint:** `POST /v1/admin/dataset`  
**Tag:** Administration  
**Description:** Download dataset to terminal. Admin code `0x313F`.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/dataset \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

---

### 15. Generic Admin Code

**Endpoint:** `POST /v1/admin/code`  
**Tag:** Administration  
**Description:** Execute a generic admin code not covered by specific endpoints.

**curl (X-report, code 12598 / 0x3136):**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/admin/code \
  -H "Content-Type: application/json" \
  -d '{"Code": 12598, "Password": "0000"}'
```

**Response (503):** `terminal_not_ready`.

**Common codes:**
- `12593` (0x3131) — Empty printer buffer
- `12598` (0x3136) — X-report
- `12603` (0x313B) — Unknown/other

---

### 16. Events Polling

**Endpoint:** `GET /v1/events?since=0`  
**Tag:** Events  
**Description:** Poll for events since a given cursor. Alternative to SSE.

**curl:**
```bash
curl -s "http://127.0.0.1:8080/v1/events?since=0"
```

**Response (200 — captured):**
```json
[]
```

**Notes:** Empty array when no events have occurred. Use `since=<last-cursor>` for incremental polling.

---

### 17. Events SSE Stream

**Endpoint:** `GET /v1/events/stream?since=0`  
**Tag:** Events  
**Description:** Subscribe to real-time events via Server-Sent Events. Long-lived connection.

**curl:**
```bash
curl -s -N "http://127.0.0.1:8080/v1/events/stream?since=0"
```

**Notes:** SSE stream — not captured as regular request/response. Simulated stub provides a heartbeat event. Use `-N` (no-buffer) with curl for SSE.

---

### 18. Diagnostics — Schema

**Endpoint:** `GET /v1/diag/schema`  
**Tag:** Diagnostics  
**Description:** Retrieve terminal schema information.

**curl:**
```bash
curl -s http://127.0.0.1:8080/v1/diag/schema
```

**Response (403 — captured):**
```json
{
  "error": "Diagnostics are disabled",
  "errorCode": "diagnostics_disabled",
  "operationId": null,
  "details": null
}
```

**Notes:** Diagnostics are disabled by default. Enable with `"enableDiagnostics": true` in `server.json`.

---

### 19. Diagnostics — Send JSON

**Endpoint:** `POST /v1/diag/sendjson`  
**Tag:** Diagnostics  
**Description:** Send raw JSON command to terminal for debugging.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/diag/sendjson \
  -H "Content-Type: application/json" \
  -d '{"json": "{\"test\":true}"}'
```

**Response (403):** `diagnostics_disabled` — same as schema.

---

### 20. Diagnostics — Send TLD

**Endpoint:** `POST /v1/diag/sendtld`  
**Tag:** Diagnostics  
**Description:** Send raw TLD (Tag-Length-Data) to terminal.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/diag/sendtld \
  -H "Content-Type: application/json" \
  -d '{"tldType": "test", "tldData": "dGVzdA=="}'
```

**Response (403):** `diagnostics_disabled`.

**Notes:** `tldData` is base64-encoded.

---

### 21. Diagnostics — Confirm

**Endpoint:** `POST /v1/diag/confirm`  
**Tag:** Diagnostics  
**Description:** Confirm or deny a pending diagnostic operation.

**curl:**
```bash
curl -s -X POST http://127.0.0.1:8080/v1/diag/confirm \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "allow": true}'
```

**Response (403):** `diagnostics_disabled`.

---

## Observations from Capture

1. **Terminal not ready** — All financial and admin operations return 503 with `errorCode: "terminal_not_ready"` because `CONNECTCLOUD_USERNAME` and `CONNECTCLOUD_PASSWORD` are not set.

2. **Response casing** — The actual server uses **camelCase** (e.g. `vendorDllLoadable`, `terminalOpen`, `errorCode`), not PascalCase as documented in the OpenAPI spec. The OpenAPI spec notes this discrepancy: `/health` uses lowercase, but actual observation shows *all* endpoints use camelCase.

3. **Extra fields** — The server returns fields not in the OpenAPI spec:
   - `connectionState` on terminal status
   - `terminalIdentity` with `terminalID` on terminal status
   - `methodRejectCode`, `methodRejectInfo`, `resultEventName`, `localModeResultData`, `entryMode`, `entryModeCode` on operation responses

4. **Content-Length required** — POST endpoints that have no defined request body (terminal open/close) require a non-empty body (`{}`) or the server returns 411 Length Required.

5. **Diagnostics disabled** — All `/v1/diag/*` endpoints return 403 with `diagnostics_disabled`. The `server.json` has `"enableDiagnostics": true`, but the server startup log shows diagnostics are still disabled (possibly overridden at runtime).

6. **Terminal close always succeeds** — Even when the terminal was never opened, close returns `success: true`.

---

## Running WireMock with These Stubs

### Standalone mode (happy path)

```bash
# Download WireMock if needed
curl -L -o wiremock-standalone-3.3.1.jar \
  https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar

# Run WireMock standalone with captured stubs
java -jar wiremock-standalone-3.3.1.jar \
  --port 9090 \
  --root-dir=./wiremock
```

### Proxy/recording mode (capture new responses)

```bash
java -jar wiremock-standalone-3.3.1.jar \
  --port 9090 \
  --proxy-all="http://localhost:8080" \
  --record-mappings \
  --root-dir=./wiremock \
  --verbose
```

### Switching between captured and happy-path stubs

The stubs use WireMock priorities:
- **Priority 1 (default):** Captured real responses (503 errors, 403 diagnostics)
- **Priority 2:** Happy-path simulated responses

To use happy-path stubs, either:
1. Remove the captured 503 stubs, or
2. Use WireMock scenarios to switch between states

### Verify stubs are loaded

```bash
curl http://localhost:9090/__admin/mappings | python3 -m json.tool
```