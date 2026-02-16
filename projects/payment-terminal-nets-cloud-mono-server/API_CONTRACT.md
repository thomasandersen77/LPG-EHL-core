# Payment Terminal Mono Server API Contract (v1)

**Connect@Cloud variant:** This server uses Nets Connect@Cloud instead of BAXI DLL. The API contract is identical for compatibility. The `vendorDllLoadable` field is preserved; it indicates the adapter (Connect@Cloud) is available.

## Overview

This document describes the REST API contract for the Payment Terminal Mono Server. All endpoints use JSON for request and response payloads.

**Base URL:** `http://127.0.0.1:8080` (configurable via `server.json`)

## Configuration (`server.json`)

Selected fields:

- `bindAddress` / `bindPort`: HTTP bind settings
- `connectCloud`: Connect@Cloud settings (environment, baseUrl, username, password, terminalId, etc.)
- `eventLogPath` (optional): write a TSV timeline of Connect@Cloud messages + events

## Endpoints

### Health & Status

#### GET /health

Returns server health status.

**Response:**
```json
{
  "status": "ok",
  "timestamp": "2026-02-09T12:00:00Z",
  "configLoaded": true
}
```

#### GET /v1/terminal/status

Returns terminal status and readiness.

**Response:**
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": false,
  "terminalReady": false,
  "lastError": null,
  "terminalIdentity": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234"
  }
}
```

### Terminal Lifecycle

#### POST /v1/terminal/open

Opens the terminal connection and waits for TerminalReady.

**Response:**
```json
{
  "success": true,
  "message": "Terminal opened"
}
```

#### POST /v1/terminal/close

Closes the terminal connection.

**Response:**
```json
{
  "success": true,
  "message": "Terminal closed"
}
```

### Financial Operations

#### POST /v1/payments/purchase

Initiates a purchase transaction.

**Request:**
```json
{
  "amountMinor": 10000,
  "currency": "NOK",
  "operatorId": "4321",
  "optionalData": "",
  "preAvstemming": {
    "enabled": false,
    "password": "0000",
    "timeoutSeconds": 300
  },
  "clientRequestId": "optional-client-id"
}
```

**Response:**
```json
{
  "success": true,
  "operationId": "abc123...",
  "startedAt": "2026-02-09T12:00:00Z",
  "completedAt": "2026-02-09T12:00:05Z",
  "durationMs": 5000,
  "callResult": 1,
  "methodRejectCode": 0,
  "methodRejectInfo": null,
  "resultEventName": "OnLocalMode",
  "localModeResult": 0,
  "responseCode": "00",
  "rejectionSource": "0",
  "rejectionReason": null,
  "localModeResultData": "D  ...;...;2;...",
  "localModeFields": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234"
  },
  "printTextRaw": "...",
  "printTextSanitized": "...",
  "lastDisplayText": "Insert card",
  "entryMode": "CONTACTLESS",
  "entryModeCode": "2",
  "error": null,
  "errorCode": null,
  "dbRowId": 1,
  "receiptFileId": "2026-02-09/abc123..."
}
```

#### POST /v1/payments/refund

Initiates a refund transaction.

**Request:** Same as purchase

**Response:** Same structure as purchase

#### POST /v1/payments/cashback

Initiates a cashback transaction (purchase + cashback).

**Request:**
```json
{
  "purchaseMinor": 10000,
  "cashbackMinor": 5000,
  "currency": "NOK",
  "operatorId": "4321",
  "optionalData": "",
  "clientRequestId": "optional-client-id"
}
```

**Response:** Same structure as purchase

### Admin Operations

#### POST /v1/admin/avstemming

Runs avstemming (end-of-day reconciliation).

**Request:**
```json
{
  "password": "0000"
}
```

**Response:** OperationResponse with `reportFields` populated

#### POST /v1/admin/cancel

Cancels the current operation.

**Request:** Same as avstemming

**Response:** OperationResponse

#### POST /v1/admin/reversal

Reverses the last transaction.

**Request:** Same as avstemming

**Response:** OperationResponse

#### POST /v1/admin/z-report

Generates a Z-report.

**Request:** Same as avstemming

**Response:** OperationResponse with `reportFields` populated

#### POST /v1/admin/last-receipt

Retrieves the last receipt.

**Request:** Same as avstemming

**Response:** OperationResponse

#### POST /v1/admin/software

Initiates software download.

**Request:** Same as avstemming

**Response:** OperationResponse

#### POST /v1/admin/dataset

Initiates dataset download.

**Request:** Same as avstemming

**Response:** OperationResponse

#### POST /v1/admin/code

Runs an arbitrary admin code.

**Request:**
```json
{
  "code": 12592,
  "password": "0000"
}
```

**Response:** OperationResponse

### Events

#### GET /v1/events/stream

Server-Sent Events stream for real-time event notifications.

**Response:** `text/event-stream` format

#### GET /v1/events?since=<timestamp>

Polling endpoint for events since a cursor.

Cursor formats supported:
- **Recommended**: sequence cursor (numeric), e.g. `since=0`
- **Legacy**: ISO-8601 timestamp, e.g. `since=2026-02-09T12:00:00Z`

**Response:**
```json
[
  {
    "eventId": "...",
    "operationId": "...",
    "timestamp": "2026-02-09T12:00:00Z",
    "eventType": "OperationStarted",
    "payload": { ... }
  }
]
```

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `terminal_busy` | 409 | Terminal is busy with another operation; retry later |
| `terminal_not_ready` | 503 | Terminal not ready; call Open, wait for TerminalReady |
| `operation_timeout` | 408 | Operation timed out waiting for completion event |
| `operation_rejected` | 422 | Terminal rejected (wrong PIN, cancel, invalid args, etc.) |
| `vendor_call_failure` | 500 | Connect@Cloud call failed or rejected |
| `invalid_request` | 400 | Malformed request or invalid parameters |
| `diagnostics_disabled` | 403 | Diagnostics endpoint disabled |

### MethodRejectCode and Connect@Cloud Error Mapping

The server maps Connect@Cloud MethodRejected and Dfs13Error codes to the above. See TROUBLESHOOTING.md for 7104, 7103, 8013, 9100, 9000, etc.

## Idempotency

Operations support idempotency via the `clientRequestId` field. If a `clientRequestId` is provided and an operation with that ID already exists, the server returns the existing result instead of executing a new operation.

## Timeouts

- Financial operations: 180 seconds (configurable)
- Admin operations: 300 seconds (configurable)

Timeouts start after the vendor call succeeds (callResult=1).

## Cancellation

Client HTTP disconnect does **not** cancel terminal activity. The server continues processing, persists results, and clients can retrieve them via events or operation lookup. There is no explicit abort API for in-flight operations.
