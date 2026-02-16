# PaymentTerminalMonoServer client guide (VB6 replacement flows)
This guide is for the developer reimplementing the legacy VB6 application as a **client** of `PaymentTerminalMonoServer/`.

Scope: **how to reproduce the VB6 “call patterns” and flows** using the HTTP API, including how to interpret results, handle “busy/not-ready/timeout”, and which admin codes VB6 used.

References:
- `PaymentTerminalMonoServer/API_CONTRACT.md`
- `agent-os/specs/2026-02-09-payment-terminal-mono-server/spec.md`
- Legacy VB6 reference: `legacy-vb6-code-for-reference-read-only/legacy/norgesgass_legacy/pumpekontroll.frm` + `administration.frm`

---

## Core concepts (what the client must assume)

- **JSON casing (important)**
    - The server uses Json.NET defaults (no camel-case resolver configured).
    - **Responses from typed models are PascalCase** (e.g. `Success`, `OperationId`, `ErrorCode`, ...).
    - **Requests are case-insensitive** on input, so you can send either `AmountMinor` or `amountMinor`, etc.
    - `/health` is an exception: it returns lower-case keys (`status`, `timestamp`, `configLoaded`).

- **Single terminal, single in-flight operation**
    - The server enforces a single operation at a time. Concurrent requests return **HTTP 409** with `ErrorCode=terminal_busy`.
- **Terminal readiness is mandatory**
    - Financial/admin endpoints require `terminalReady=true`. Otherwise you get **HTTP 503** with `ErrorCode=terminal_not_ready`.
- **Operations are non-cancellable by disconnect**
    - If your HTTP client disconnects mid-operation, the terminal operation **continues** on the server. Use the events feed for visibility.
- **Result completion**
    - The server waits for a terminal “completion event” and returns a single `OperationResponse` containing:
        - `PrintTextRaw` (receipt/report text)
        - `LastDisplayText` (last seen display prompt)
        - `LocalModeResult`, `ResponseCode`, and rejection fields
- **Idempotency (recommended)**
    - For `purchase`, `refund`, `cashback`, send `ClientRequestId`.
    - If you retry the same request with the same `ClientRequestId`, the server will return the already-stored result instead of running a second terminal operation.

---

## API quickstart (what a VB6 replacement should do on boot)

### 1) Verify server process
- `GET /health`

### 2) Verify terminal readiness
- `GET /v1/terminal/status`
    - Expect `vendorDllLoadable=true`
    - Expect `terminalOpen=true` and `terminalReady=true` before doing anything else

### 3) Open the terminal (if needed)
- `POST /v1/terminal/open`

### 4) Subscribe to events (optional but recommended)
- **SSE**: `GET /v1/events/stream?since=<cursor-or-iso>`
    - Server emits SSE `event:` equal to the event type and `data:` as a JSON envelope:
        - `Cursor` (monotonic sequence), `OperationId`, `Timestamp`, `EventType`, `Payload`
- **Polling**: `GET /v1/events?since=<cursor-or-iso>`

Minimal SSE cursor strategy:
- On first connect, set `since=0`
- Persist the last delivered `Cursor` and reconnect with that value after restarts/disconnects

---

## Response interpretation (what to key off)

All financial/admin endpoints return `OperationResponse`:

- **Success**: boolean, computed server-side.
- **OperationId**: server-generated string id for this operation.
- **CallResult**:
    - `1` means the vendor DLL call started successfully.
    - Any other value means the call failed to start (see `ErrorCode=vendor_call_failure`).
- **ErrorCode**:
    - `terminal_busy` (HTTP 409)
    - `terminal_not_ready` (HTTP 503)
    - `operation_timeout` (HTTP 408)
    - `vendor_call_failure` (HTTP 500)
- **Terminal outcome fields** (these are what you use for UI + business logic):
    - `LocalModeResult`
    - `ResponseCode`
    - `RejectionSource`
    - `RejectionReason`
- **Evidence you should store**:
    - `PrintTextRaw` (and `PrintTextSanitized` where present)
    - `LocalModeFields` (terminal + merchant identifiers, etc.)
    - `DbRowId`, `ReceiptFileId` (server persistence references)

### “Approved” vs “rejected” (practical rules)
The server marks `Success=true` for financial operations when:
- `CallResult==1`, and not timed out, and
- (`LocalModeResult==0` **or** `ResponseCode=="00"`)

Client-side guidance:
- Treat `Success=true` as **authorized/approved** for the pay/refund/cashback request.
- When `Success=false` but the call did run (`CallResult==1`), use `ResponseCode`/`RejectionReason` to decide whether it was:
    - **user cancel**
    - **wrong PIN**
    - **decline/other**

Evidence-backed examples from field runs (see `additional-files/BAXI_VB6_BEHAVIOR_REIMPLEMENTATION_EVIDENCE_REPORT.md`):
- **Wrong PIN**: typically `LocalModeResult=2`, `ResponseCode="Z1"`, `RejectionSource="3"`, `RejectionReason` contains `3:2:Z1`
- **User cancel during PIN prompt**: typically `LocalModeResult=2`, blank `ResponseCode`, `RejectionReason` contains `2:1`

---

## VB6 → HTTP mapping (high-signal table)

### Financial

| Legacy VB6 call | Meaning in VB6 | HTTP endpoint | Notes |
|---|---|---|---|
| `TransferAmount_V2("0000", &H30, amountMinor, ...)` | Purchase/prepay (used before enabling dispenser) | `POST /v1/payments/purchase` | Pass `OperatorId="0000"` to match VB6. `AmountMinor` is already minor units (øre). |
| `TransferAmount_V2("0000", &H31, amountMinor, ...)` | “Return” / refund (VB6 calls it cashback/technical return) | `POST /v1/payments/refund` | VB6 often sets a different receipt header text via `OptionalData`. |
| (not explicit in VB6) | Purchase + cashback | `POST /v1/payments/cashback` | Only use if your new client explicitly needs this composite flow. |

### Administration

| Legacy VB6 call | Meaning | HTTP endpoint | Admin code |
|---|---|---|---|
| `Administration &H3130` | Avstemming / end-of-day reconciliation | `POST /v1/admin/avstemming` | `0x3130` |
| `Administration &H3132` | Cancel current operation | `POST /v1/admin/cancel` | `0x3132` |
| `Administration &H3134` | Reversal of last transaction | `POST /v1/admin/reversal` | `0x3134` |
| `Administration &H3137` | Z-report (VB6 timer driven) | `POST /v1/admin/z-report` | `0x3137` |
| `Administration &H313C` | Last receipt / last financial | `POST /v1/admin/last-receipt` | `0x313C` |
| `Administration &H313E` | Software download | `POST /v1/admin/software` | `0x313E` |
| `Administration &H313F` | Dataset download | `POST /v1/admin/dataset` | `0x313F` |
| `Administration &H3136` | X-report (seen in `administration.frm`) | `POST /v1/admin/code` | `0x3136` |
| `Administration &H3131` | Empty printer buffer (seen in `administration.frm`) | `POST /v1/admin/code` | `0x3131` |
| `Administration &H313B` | Unknown/other (seen in `pumpekontroll.frm`) | `POST /v1/admin/code` | `0x313B` |

Admin codes (hex + decimal) for `/v1/admin/code`:
- `0x3131` = `12593` (empty printer buffer, VB6)
- `0x3136` = `12598` (X-report, VB6)
- `0x313B` = `12603` (unknown, VB6)

---

## Flow cookbook (reimplementing VB6 behavior)

### Flow A: “Prepay purchase then enable dispenser” (VB6 `pre_sum`)
This is the core VB6 pattern: **authorize first**, then open the dispenser. If dispenser comms fail after approval, VB6 reverses.

Legacy VB6 reference (simplified):
- `TransferAmount_V2("0000", &H30, amountMinor, ..., "LPG Autogas", "")`
- Busy-wait loop until `OnLocalMode` sets `Bank_answer=True`
- If approved: proceed to preset + unblock dispenser
- If dispenser comm fails: print error + do `Administration &H3134` (reversal)

Client sequence:
- **Step 1**: ensure server is ready
    - `GET /v1/terminal/status` → require `terminalReady=true`
- **Step 2**: start purchase
    - `POST /v1/payments/purchase`

Example request body (match VB6 defaults deliberately):

```json
{
  "AmountMinor": 1250,
  "OperatorId": "0000",
  "Currency": "NOK",
  "OptionalData": "LPG Autogas",
  "ClientRequestId": "fuelingSession:<your-session-id>",
  "PreAvstemming": { "Enabled": false }
}
```

- **Step 3**: interpret result
    - If `Success==true`: proceed to your dispenser steps (preset amount, unblock, etc.)
    - If `Success==false`:
        - Show user-facing message using `LastDisplayText`
        - Persist `OperationId` + `PrintTextRaw` for audit (even on failures)
        - Do **not** attempt dispenser operations

### Flow B: “Cancel current payment flow” (VB6 `avbryt_Click`)
VB6 issues terminal cancel:
- `Administration &H3132`

Client sequence:
- `POST /v1/admin/cancel`

```json
{ "Password": "0000" }
```

Notes:
- This is intended to cancel the *current* bank mode/operation (e.g., user cancels at the terminal).
- If nothing is in progress, the vendor may reject; treat it as non-fatal UI-wise.

### Flow C: “Reversal after downstream failure” (VB6 dispenser-comm error path)
VB6 does reversal when the payment succeeded but dispenser steps failed afterward.

Client trigger condition (recommended):
- Purchase returned `Success==true`, but your dispenser/pump control failed before delivery could start (or you must compensate).

Client sequence:
- `POST /v1/admin/reversal`

```json
{ "Password": "0000" }
```

Important:
- VB6 warns this is not reversible (“Annuler siste korttransaksjon? Dette er ikke reversibelt!”). Your new UI should do the same.

### Flow D: “Manual return / technical refund” (VB6 `cashback(...)` → `&H31`)
Despite the VB6 naming, the terminal operation used is `&H31` (return/refund-style).

Client sequence:
- `POST /v1/payments/refund`

```json
{
  "AmountMinor": 5000,
  "OperatorId": "0000",
  "OptionalData": "Return LPG Autogas",
  "ClientRequestId": "manualRefund:<id>"
}
```

Notes:
- VB6 sets `return_amount=True` and `manual_bank=True` around this; that’s app logic you’ll carry in your own state machine.

### Flow E: “Manual purchase” (VB6 `cash(cashstr)` → `&H30`)
VB6 allows a manual `&H30` call for an entered amount.

Client sequence:
- `POST /v1/payments/purchase` with `AmountMinor` set from user input.

### Flow F: “Scheduled Z-report, then avstemming” (VB6 `task_timer_Timer`)
VB6 does scheduled admin operations, roughly:
- if task says Z: run `Administration &H3137`
- wait until `Baxi.LocalMode == 1` (or timeout ~5 minutes)
- if task says avstemming: run `Administration &H3130`

Client sequence:
- `POST /v1/admin/z-report` then `POST /v1/admin/avstemming`
    - Both return `OperationResponse` with `PrintTextSanitized` and (when parseable) `ReportFields`.

What VB6 added on top (you must reimplement client-side):
- After Z-report, VB6 appends a “technical refunds since last Z” section from its own DB and computes “amount to book”.
    - This is **not** a terminal function; keep it in your replacement app.

### Flow G: “X-report” and “Empty printer buffer” (VB6 `administration.frm`)
These codes are not first-class endpoints; use `/v1/admin/code`.

X-report:

```json
{ "Code": 12598, "Password": "0000" }
```

Empty printer buffer:

```json
{ "Code": 12593, "Password": "0000" }
```

### Flow H: “Dataset download” / “Software download”
VB6 buttons call:
- `Administration &H313F` (dataset)
- `Administration &H313E` (software)

Client sequence:
- `POST /v1/admin/dataset`
- `POST /v1/admin/software`

Notes:
- Expect these to be long-running relative to payments. Treat `operation_timeout` as “inconclusive”; use vendor logs/terminal display and retry with care.

### Flow I: “Restart terminal” (VB6 `restart_baxi`)
VB6 closes and opens the control:
- `Baxi.Close` → sleep → `Baxi.Open`

Client sequence:
- `POST /v1/terminal/close`
- `POST /v1/terminal/open`

If the terminal stack is wedged, you may need to restart the server process as well (deployment concern).

---

## Error handling playbook (what to do, not just what happened)

- **HTTP 409 `terminal_busy`**
    - You attempted an operation while another is running.
    - Action: surface a “terminal busy” UI state; retry only if it makes sense for your UX.
- **HTTP 503 `terminal_not_ready`**
    - Action: call `POST /v1/terminal/open` (if `terminalOpen=false`), or show “terminal not ready” and alert operator.
- **HTTP 408 `operation_timeout`**
    - Means the server started the vendor call but did not receive completion within configured timeout.
    - Action: do *not* assume success or failure; use the terminal screen/receipt behavior and your operational policy (and check evidence via receipts/events).
- **HTTP 500 `vendor_call_failure`**
    - The vendor DLL call failed to start or the adapter threw.
    - Action: surface operator message; consider `close/open` sequence; preserve the error for support.

---

## Diagnostics endpoints (use only when explicitly enabled)

These exist for investigation and should not be used by normal business logic.

- `GET /v1/diag/schema`
- `POST /v1/diag/sendjson` with body `{ "json": "<string>" }`
- `POST /v1/diag/sendtld` with body `{ "tldType": "custom", "tldData": "<base64>" }`
- `POST /v1/diag/confirm` with body `{ "id": 123, "allow": true }`

If diagnostics are disabled, calls return **HTTP 403** with `ErrorCode=diagnostics_disabled`.

---

## Implementation stance (recommended client structure)

- Build a small **terminal orchestration module** in the replacement app:
    - “ensure ready” (health/status/open)
    - “run operation” (purchase/refund/admin) with:
        - `ClientRequestId` tied to your domain id (fueling session, report run id, etc.)
        - persistence of the returned `OperationResponse`
    - optional SSE subscription for real-time operator display mirroring
- Treat terminal receipts (`PrintText*`) as authoritative evidence:
    - persist them externally in your client DB as well (even though the server stores them)

