# Terminal API Exploration Log

Tested against real Nets Cloud Mono Server on `http://127.0.0.1:18081` (2026-02-13).

Terminal: **42696609** | Merchant: **1229329** | Location: **NORGESGASS AS, NEDRE EIKERVEI 26, DRAMMEN**

---

## Critical Finding: camelCase, NOT PascalCase

The OpenAPI spec documents PascalCase (`Success`, `OperationId`, `TerminalReady`), but the **real server returns camelCase** (`success`, `operationId`, `terminalReady`). This is the single most important divergence from the spec.

---

## 1. GET /health → 200

```
Request:  GET http://127.0.0.1:18081/health
Response: 200 OK
```

```json
{
    "status": "ok",
    "timestamp": "2026-02-13T15:35:59.5053840Z",
    "configLoaded": true
}
```

**Notes:** Lowercase keys as expected. Matches spec.

---

## 2. GET /v1/terminal/status → 200

### Before open (terminal closed)

```
Request:  GET http://127.0.0.1:18081/v1/terminal/status
Response: 200 OK
```

```json
{
    "vendorDllLoadable": true,
    "terminalOpen": false,
    "terminalReady": false,
    "connectionState": "Aborted",
    "lastError": "WebSocket is not connected (state=Aborted closeStatus=null closeDesc='' url='wss://connectcloud.aws.nets.eu:443/ws/json')",
    "terminalIdentity": {
        "terminalID": "42696609"
    }
}
```

### After open (terminal ready)

```json
{
    "vendorDllLoadable": true,
    "terminalOpen": true,
    "terminalReady": true,
    "connectionState": "Open",
    "lastError": "WebSocket is not connected (...)",
    "terminalIdentity": {
        "terminalID": "42696609"
    }
}
```

**New fields not in OpenAPI spec:**
- `connectionState` — string: "Open", "Aborted"
- `terminalIdentity` — object with `terminalID`

**Divergence from spec:**
- `lastError` persists from previous session even when terminal is open and ready

---

## 3. POST /v1/terminal/open → 200

```
Request:  POST http://127.0.0.1:18081/v1/terminal/open
Headers:  Content-Type: application/json
Body:     {}
Response: 200 OK
```

```json
{
    "success": true,
    "message": "Terminal opened",
    "error": null
}
```

**IMPORTANT:** Server requires `Content-Length` header — returns `411 Length Required` without a body. Always send `{}` as minimum body.

---

## 4. POST /v1/terminal/close → 200

```
Request:  POST http://127.0.0.1:18081/v1/terminal/close
Headers:  Content-Type: application/json
Body:     {}
Response: 200 OK
```

```json
{
    "success": true,
    "message": "Terminal closed",
    "error": null
}
```

---

## 5. POST /v1/payments/purchase → 422

```
Request:  POST http://127.0.0.1:18081/v1/payments/purchase
Headers:  Content-Type: application/json
Body:     {"AmountMinor": 100, "OperatorId": "0000", "Currency": "NOK",
           "OptionalData": "API Test", "ClientRequestId": "test-explore-001"}
Response: 422 Unprocessable Entity
Time:     1.47s
```

```json
{
    "success": false,
    "operationId": "6fb88b5b4ef34c619f2168017392a740",
    "startedAt": "2026-02-13T15:36:44.185228Z",
    "completedAt": "2026-02-13T15:36:45.64783Z",
    "durationMs": 1462,
    "callResult": 1,
    "methodRejectCode": 0,
    "methodRejectInfo": null,
    "resultEventName": "OnLocalMode",
    "localModeResult": 2,
    "responseCode": "2",
    "rejectionSource": null,
    "rejectionReason": "4:6",
    "localModeResultData": "D!000;20260213163643;2;020;;000000;00000000100;03;4:6;;;42696609;1229329;;;___;;;;;;;;;;Undefined;;004;0",
    "localModeFields": null,
    "printTextRaw": "",
    "printTextSanitized": "",
    "lastDisplayText": "Formatfeil\r",
    "entryMode": "CONTACTLESS",
    "entryModeCode": "2",
    "error": "Rejected by terminal. Display='Formatfeil' RejectionSource= RejectionReason=4:6 ResponseCode=2",
    "errorCode": "operation_rejected",
    "dbRowId": 1,
    "receiptFileId": null,
    "reportFields": null
}
```

**New fields not in OpenAPI spec:**
- `startedAt` / `completedAt` — ISO timestamps
- `durationMs` — operation duration in ms
- `methodRejectCode` — integer (0)
- `methodRejectInfo` — string nullable
- `resultEventName` — "OnLocalMode"
- `localModeResultData` — raw semicolon-separated protocol data (very useful for debugging)
- `entryMode` — "CONTACTLESS", null
- `entryModeCode` — "2", null

**Notes:**
- 100 øre (1 NOK) triggered "Formatfeil" (Format error) — may be minimum amount or format issue
- `localModeResultData` contains raw BAX protocol response with semicolons
- `responseCode: "2"` differs from documented codes ("00", "Z1", "05")
- Rejection reason "4:6" appears to be a terminal-side error code

---

## 6. POST /v1/payments/purchase (terminal closed) → 408

```
Request:  POST http://127.0.0.1:18081/v1/payments/purchase
Body:     {"AmountMinor": 100}
Response: 408 Request Timeout
Time:     180s (3 minutes!)
```

```json
{
    "success": false,
    "operationId": "66efb00668f84e30a9f9ad29896da18a",
    "durationMs": 180001,
    "callResult": 0,
    "error": "Operation timeout",
    "errorCode": "operation_timeout",
    "dbRowId": 6
}
```

**Critical behavioral difference:**
- OpenAPI spec suggests 503 `terminal_not_ready` when terminal is closed
- Real server: tries the operation and times out after **180 seconds** (returns 408)
- `callResult: 0` — vendor call failed to start (terminal was closed)

---

## 7. POST /v1/admin/last-receipt → 500

```
Request:  POST http://127.0.0.1:18081/v1/admin/last-receipt
Body:     {"Password": "0000"}
Response: 500 Internal Server Error
Time:     ~0.9s
```

```json
{
    "success": false,
    "operationId": "e11703bee5d64c96ab2c3482fc706b27",
    "durationMs": 900,
    "callResult": 1,
    "localModeResult": 0,
    "printTextRaw": "************************\r\n          KOPI\r\n************************\r\nNORGESGASS AS\r\nNEDRE EIKERVEI 26\r\nDRAMMEN\r\n\r\nBax: 42696609-1229329\r\n13/02/2026 16:36\r\n\r\nKortet ikke presentert\r\nRef.:  ___\r\nOverf.: 020\r\n\r\nKJØP\r\nNOK                 1,00\r\nFormatfeil\r\n\r\n\r\n\r\n\f\n",
    "lastDisplayText": "Formatfeil\r",
    "receiptFileId": "2026-02-13/e11703bee5d64c96ab2c3482fc706b27",
    "reportFields": {
        "reportTerminalId": "42696609",
        "reportMerchantId": "1229329",
        "reportTimestampLocal": "13/02/2026 16:36"
    }
}
```

**Critical issue:** Returns HTTP 500 even though `localModeResult: 0` (approved) and receipt was successfully retrieved. Admin operations consistently return 500 from the real server — the integration must NOT treat 500 as a fatal error for admin ops.

---

## 8. POST /v1/admin/cancel → 500

```
Request:  POST http://127.0.0.1:18081/v1/admin/cancel
Body:     {"Password": "0000"}
Response: 500 Internal Server Error
```

```json
{
    "success": false,
    "localModeResult": 2,
    "lastDisplayText": "Avbrutt\r",
    "receiptFileId": "2026-02-13/14a68cd47d7d444b8c5312a6c47bf403"
}
```

**Notes:** Same 500 pattern. `lastDisplayText: "Avbrutt"` (Cancelled).

---

## 9. POST /v1/admin/code (X-report, code 12598) → 500

```
Request:  POST http://127.0.0.1:18081/v1/admin/code
Body:     {"Code": 12598, "Password": "0000"}
Response: 500 Internal Server Error
Time:     ~1.4s
```

Receipt text (X-report):
```
Bax: 42696609-1229329
13/02/2026 16:37
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

Report fields:
```json
{
    "reportTerminalId": "42696609",
    "reportMerchantId": "1229329",
    "reportTimestampLocal": "13/02/2026 16:37",
    "reportCurrency": "NOK",
    "reportSessionNumber": "020"
}
```

---

## 10. POST /v1/admin/reversal → 500

```
Request:  POST http://127.0.0.1:18081/v1/admin/reversal
Body:     {"Password": "0000"}
Response: 500 Internal Server Error
```

- `localModeResult: 2`, `responseCode: "0"`, `rejectionReason: "4:6"`
- `lastDisplayText: "Formatfeil"` — rejection because no valid transaction to reverse

---

## 11. GET /v1/diag/schema → 403

```
Request:  GET http://127.0.0.1:18081/v1/diag/schema
Response: 403 Forbidden
```

```json
{
    "error": "Diagnostics are disabled",
    "errorCode": "diagnostics_disabled",
    "operationId": null,
    "details": null
}
```

**Notes:** Diagnostics disabled as expected. Error response also uses camelCase.

---

## 12. GET /v1/events?since=0 → 200

```
Request:  GET http://127.0.0.1:18081/v1/events?since=0
Response: 200 OK
```

```json
[
    {
        "cursor": 1,
        "eventId": "b9d4b45acd70479b8c9b40c561ec2c68",
        "operationId": "6fb88b5b4ef34c619f2168017392a740",
        "timestamp": "2026-02-13T15:36:44.1896470Z",
        "eventType": "OperationStarted",
        "payload": {
            "operationType": "purchase",
            "startedAt": "2026-02-13T15:36:44.1852280Z"
        }
    },
    {
        "cursor": 2,
        "eventId": "77aeb290c02245ba94e13803d0b9dd49",
        "operationId": "6fb88b5b4ef34c619f2168017392a740",
        "timestamp": "2026-02-13T15:36:45.6507520Z",
        "eventType": "OperationCompleted",
        "payload": {
            "operationType": "purchase",
            "completedAt": "2026-02-13T15:36:45.6507240Z",
            "success": false
        }
    }
]
```

---

## 13. GET /v1/events/stream?since=0 → SSE

```
event: connected
data: {"since":"0"}

id: 1
event: OperationStarted
data: {"cursor":1,"eventId":"...","operationId":"...","timestamp":"...","eventType":"OperationStarted","payload":{"operationType":"purchase","startedAt":"..."}}

id: 2
event: OperationCompleted
data: {"cursor":2,...,"payload":{"operationType":"purchase","completedAt":"...","success":false}}

id: 7
event: OperationStarted
data: {...,"payload":{"operationType":"admin_x-report","startedAt":"..."}}

: keepalive 2026-02-13T15:37:54.0658620Z
: keepalive 2026-02-13T15:37:55.0672410Z
```

**SSE-specific findings:**
- Stream opens with custom `event: connected` + `data: {"since":"0"}` (not in spec)
- Standard SSE format: `id:`, `event:`, `data:` fields
- Keepalive comments sent every ~1 second (format: `: keepalive <ISO timestamp>`)
- Operation types in events: `purchase`, `admin_last-receipt`, `admin_cancel`, `admin_x-report`
- Event types observed: `OperationStarted`, `OperationCompleted`
- No `DisplayText` events observed during these operations

---

## Summary of Divergences from OpenAPI Spec

### 1. Field Casing: camelCase everywhere
All responses use camelCase, not PascalCase.

### 2. New Fields in OperationResponse
| Field | Type | Example |
|-------|------|---------|
| `startedAt` | string (ISO) | "2026-02-13T15:36:44.185228Z" |
| `completedAt` | string (ISO) | "2026-02-13T15:36:45.64783Z" |
| `durationMs` | long | 1462 |
| `methodRejectCode` | int | 0 |
| `methodRejectInfo` | string? | null |
| `resultEventName` | string | "OnLocalMode" |
| `localModeResultData` | string? | "D!000;20260213..." |
| `entryMode` | string? | "CONTACTLESS" |
| `entryModeCode` | string? | "2" |

### 3. New Fields in TerminalStatusResponse
| Field | Type | Example |
|-------|------|---------|
| `connectionState` | string | "Open", "Aborted" |
| `terminalIdentity` | object | `{"terminalID": "42696609"}` |

### 4. SSE Stream Extras
- Initial `connected` event
- Keepalive every ~1 second
- Operation types prefixed with `admin_` for admin operations

### 5. HTTP Status Code Differences
| Endpoint | Expected | Actual | Notes |
|----------|----------|--------|-------|
| Admin operations | 200 | 500 | Even successful admin ops return 500 |
| Purchase (closed terminal) | 503 | 408 | Waits 180s timeout instead of immediate reject |

### 6. Server Requires Content-Length
POST endpoints return `411 Length Required` if no body is sent. Always include `{}` as minimum.

### 7. Success Field Unreliable for Admin
`success: false` returned for admin operations that actually succeeded (e.g. X-report with `localModeResult: 0`). Must check `localModeResult` and/or `printTextRaw` to determine actual outcome.
