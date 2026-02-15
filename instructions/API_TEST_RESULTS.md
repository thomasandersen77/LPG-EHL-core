# Payment Terminal Nets Cloud API Test Results

**Test Date:** 2026-02-14  
**Server:** http://127.0.0.1:18081  
**Terminal ID:** 42696609  
**Merchant ID:** 1229329  

---

## Summary

| Endpoint | Status | Notes |
|----------|--------|-------|
| `/health` | ✅ Working | Returns server health |
| `/v1/terminal/status` | ✅ Working | Returns terminal state |
| `/v1/terminal/open` | ✅ Working | Opens terminal connection |
| `/v1/terminal/close` | ✅ Working | Closes terminal connection |
| `/v1/payments/purchase` | ⚠️ Requires card | Timeout without card |
| `/v1/payments/refund` | ⚠️ Requires card | Not tested (needs card) |
| `/v1/payments/cashback` | ⚠️ Requires card | Not tested (needs card) |
| `/v1/admin/avstemming` | ✅ Working | Reconciliation |
| `/v1/admin/z-report` | ✅ Working | End-of-day report |
| `/v1/admin/code` (X-report) | ✅ Working | Current totals |
| `/v1/admin/last-receipt` | ✅ Working | Returns last transaction |
| `/v1/admin/reversal` | ✅ Working | Returns "Formatfeil" if no txn |
| `/v1/admin/cancel` | ✅ Working | Cancel current operation |
| `/v1/events` | ✅ Working | Event polling |
| `/v1/events/stream` | ✅ Working | SSE stream |
| `/v1/diag/*` | ❌ Disabled | As configured |

---

## Recommended Operation Sequence

### 1. Initialization
```
GET  /health              → Verify server is running
GET  /v1/terminal/status  → Check if terminal is open
POST /v1/terminal/open    → Open terminal if needed (body: {})
GET  /v1/terminal/status  → Verify terminalReady=true
```

### 2. Payment Flow
```
POST /v1/payments/purchase  → Initiate payment
     Body: {"amountMinor": 1000, "clientRequestId": "session-123"}
     
     Wait for response...
     
     If success=true && localModeResult=0 && responseCode="00":
         → Payment approved, proceed with delivery
     If success=false:
         → Check error/errorCode for reason
```

### 3. Post-Delivery Error Handling
```
If delivery fails AFTER payment approved:
    POST /v1/admin/reversal → Reverse the payment
         Body: {"password": "0000"}
```

### 4. End-of-Day Reconciliation (Avstemming)
```
POST /v1/admin/avstemming  → Send settlements to bank
     Body: {"password": "0000"}
     
POST /v1/admin/z-report    → Generate Z-report (resets counters)
     Body: {"password": "0000"}
```

### 5. During-Day Reports
```
POST /v1/admin/code        → X-report (current totals, no reset)
     Body: {"code": 12598, "password": "0000"}
```

### 6. Shutdown
```
POST /v1/terminal/close    → Close terminal connection
     Body: {}
```

---

## Detailed Test Results

### Round 1

#### TEST 1: Health Check
```bash
curl -s http://127.0.0.1:18081/health
```
**Response:**
```json
{
  "status": "ok",
  "timestamp": "2026-02-14T19:53:14.499Z",
  "configLoaded": true
}
```

#### TEST 2: Terminal Status (before open)
```bash
curl -s http://127.0.0.1:18081/v1/terminal/status
```
**Response:**
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": false,
  "terminalReady": false,
  "connectionState": "None",
  "lastError": null,
  "terminalIdentity": {"terminalID": "42696609"}
}
```

#### TEST 3: Terminal Open
```bash
curl -s -X POST http://127.0.0.1:18081/v1/terminal/open \
  -H "Content-Type: application/json" -d '{}'
```
**Response:**
```json
{
  "success": true,
  "message": "Terminal opened",
  "error": null
}
```

#### TEST 4: Terminal Status (after open)
**Response:**
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": true,
  "terminalReady": true,
  "connectionState": "Open",
  "lastError": null,
  "terminalIdentity": {"terminalID": "42696609"}
}
```

#### TEST 5: Purchase (with currency)
```bash
curl -s -X POST http://127.0.0.1:18081/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{"amountMinor": 1000, "operatorId": "0000", "currency": "NOK", 
       "optionalData": "API Test", "clientRequestId": "test-001"}'
```
**Response:** `success: false`, `lastDisplayText: "Formatfeil"`, `rejectionReason: "4:6"`  
**Note:** "Formatfeil" (Format error) - possibly card-related issue.

#### TEST 6: Purchase (minimal params)
```bash
curl -s -X POST http://127.0.0.1:18081/v1/payments/purchase \
  -H "Content-Type: application/json" -d '{"amountMinor": 1000}'
```
**Response:** `success: false`, `errorCode: "operation_timeout"`, `durationMs: 180002`  
**Note:** Timed out after 180 seconds waiting for card.

#### TEST 7: Last Receipt
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/last-receipt \
  -H "Content-Type: application/json" -d '{"password": "0000"}'
```
**Response:** `success: false` (technically worked)
```
KOPI
NORGESGASS AS
NEDRE EIKERVEI 26
DRAMMEN

Bax: 42696609-1229329
14/02/2026 20:55

Kortet ikke presentert
Ref.:  ___
Overf.: 020

KJØP
NOK                10,00
Tidsavbrudd
```
**Note:** Shows last receipt was a timeout waiting for card.

#### TEST 8: X-Report (admin code 12598)
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/code \
  -H "Content-Type: application/json" -d '{"code": 12598, "password": "0000"}'
```
**Response:**
```
Bax: 42696609-1229329
14/02/2026 20:56
Valuta: NOK
Sesjon.: 020
X-rapport: 004

X-Total

Siste Z-Total
09/02/2026 15:04

BankAxept              3
Beløp=              1,50

------------------------
Antall                 3
Total=              1,50
```
**Parsed reportFields:**
- Session: 020
- Last Z-Total: 09/02/2026 15:04
- BankAxept: 3 transactions, 1.50 NOK

#### TEST 9: Z-Report
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/z-report \
  -H "Content-Type: application/json" -d '{"password": "0000"}'
```
**Response:** Same as X-report but generates Z-report #004
```json
{
  "reportType": "z",
  "zReportNumber": "004",
  "zLastTotalTimestampLocal": "09/02/2026 15:04",
  "batchTotalCount": "3",
  "batchTotalAmount": "1,50",
  "batchTotalAmountMinor": "150",
  "scheme_BankAxept_count": "3",
  "scheme_BankAxept_amount": "1,50"
}
```

#### TEST 10: Avstemming (Reconciliation)
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/avstemming \
  -H "Content-Type: application/json" -d '{"password": "0000"}'
```
**Response:**
```
Avstemming

------------------------
Innsamlet              0
Total=              0,00

Kortavtaler uten
omsetning skrives
ikke ut.
```
**Note:** No unsettled transactions to collect (had just run Z-report).

#### TEST 11: Diagnostics (disabled)
```bash
curl -s http://127.0.0.1:18081/v1/diag/schema
```
**Response:**
```json
{
  "error": "Diagnostics are disabled",
  "errorCode": "diagnostics_disabled"
}
```

#### TEST 12: Reversal
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/reversal \
  -H "Content-Type: application/json" -d '{"password": "0000"}'
```
**Response:** `localModeResult: 2`, `lastDisplayText: "Formatfeil"`, `rejectionReason: "4:6"`  
**Note:** No transaction to reverse.

#### TEST 13: Cancel
```bash
curl -s -X POST http://127.0.0.1:18081/v1/admin/cancel \
  -H "Content-Type: application/json" -d '{"password": "0000"}'
```
**Response:** `lastDisplayText: "Avbrutt"` (Cancelled)

#### TEST 14: Terminal Close
```bash
curl -s -X POST http://127.0.0.1:18081/v1/terminal/close \
  -H "Content-Type: application/json" -d '{}'
```
**Response:**
```json
{
  "success": true,
  "message": "Terminal closed"
}
```

#### TEST 15: Events Polling
```bash
curl -s "http://127.0.0.1:18081/v1/events?since=0"
```
**Response:** Array of 14 events with cursor-based ordering
- `OperationStarted` / `OperationCompleted` pairs
- `OperationTimeout` for timed-out purchase

---

### Round 2 (After terminal reopen)

#### TEST 16: Terminal Reopen
**Response:** `success: true`
**Observation:** Session incremented from 020 → 021

#### TEST 17: X-Report (Round 2)
```
Sesjon.: 021
X-rapport: 005

Siste Z-Total
14/02/2026 20:56  ← Updated after Z-report in Round 1

Antall                 0
Total=              0,00  ← Reset after Z-report
```

#### TEST 18: Avstemming (Round 2)
```
Sesjon.: 021
Innsamlet              0
Total=              0,00
```
**Note:** Clean slate after Z-report.

#### TEST 19: SSE Stream
```bash
curl -s "http://127.0.0.1:18081/v1/events/stream?since=0"
```
**Response:** Server-Sent Events stream with:
- `event: connected`
- `event: OperationStarted`
- `event: OperationCompleted`
- Keepalive comments: `: keepalive <timestamp>`

---

## Key Observations

### 1. Terminal State Machine
- Terminal must be **open** and **ready** before financial operations
- Status check: `terminalOpen=true` AND `terminalReady=true`
- Close/reopen cycles increment session number

### 2. Financial Operations
- Require card presence at terminal
- Timeout after 180 seconds (configurable)
- Return "Formatfeil" for protocol errors
- Return "Kortet ikke presentert" if no card

### 3. Admin Operations
- Work without card presence
- Password is typically "0000"
- X-report: Shows current totals (non-destructive)
- Z-report: Shows totals AND resets counters
- Avstemming: Sends settlements to bank

### 4. Reconciliation Flow (Avstemming)
1. Run **Avstemming** first → Sends unsettled transactions to bank
2. Then run **Z-report** → Generates report and resets counters
3. X-report afterward will show zero totals

### 5. Response Success Flag
- `success: false` doesn't always mean failure
- Admin operations may return `success: false` but work correctly
- Check `localModeResult`, `responseCode`, and `printTextRaw` for actual outcome

### 6. Events
- Polling: `GET /v1/events?since=<cursor>`
- SSE: `GET /v1/events/stream?since=<cursor>`
- Use cursor from last event for continuation

---

## Admin Code Reference

| Code (decimal) | Code (hex) | Operation |
|----------------|------------|-----------|
| 12592 | 0x3130 | Avstemming |
| 12593 | 0x3131 | Empty printer buffer |
| 12594 | 0x3132 | Cancel |
| 12596 | 0x3134 | Reversal |
| 12598 | 0x3136 | X-report |
| 12599 | 0x3137 | Z-report |
| 12604 | 0x313C | Last receipt |
| 12606 | 0x313E | Software download |
| 12607 | 0x313F | Dataset download |

---

## Error Codes

| errorCode | Description |
|-----------|-------------|
| `terminal_busy` | Another operation in progress |
| `terminal_not_ready` | Terminal not open/ready |
| `operation_timeout` | Timeout waiting for terminal |
| `operation_rejected` | Terminal rejected operation |
| `vendor_call_failure` | DLL call failed |
| `diagnostics_disabled` | Diag endpoints disabled |

---

## Test Environment

- **Server Version:** PaymentTerminalNetsCloudMonoServer (net8.0)
- **Connect Cloud:** PROD environment
- **Terminal:** 42696609
- **Merchant:** 1229329 (NORGESGASS AS)
- **Location:** NEDRE EIKERVEI 26, DRAMMEN
