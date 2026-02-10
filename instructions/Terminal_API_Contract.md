# Payment Terminal Mono Server API Contract (v1)

## Overview

This document describes the REST API contract for the Payment Terminal Mono Server. All endpoints use JSON for request and response payloads.

**Base URL:** `http://127.0.0.1:18080` (configurable via server.json, production uses 18080)

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
  "VendorDllLoadable": true,
  "TerminalOpen": false,
  "TerminalReady": false,
  "LastError": null,
  "TerminalIdentity": {
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
  "Success": true,
  "Message": "Terminal opened",
  "Error": null
}
```

#### POST /v1/terminal/close

Closes the terminal connection.

**Response:**
```json
{
  "Success": true,
  "Message": "Terminal closed",
  "Error": null
}
```

### Financial Operations

#### POST /v1/payments/purchase

Initiates a purchase transaction.

**Request:**
```json
{
  "AmountMinor": 10000,
  "Currency": "NOK",
  "OperatorId": "0000",
  "OptionalData": "LPG Autogas",
  "PreAvstemming": {
    "Enabled": false,
    "Password": "0000",
    "TimeoutSeconds": 300
  },
  "ClientRequestId": "optional-client-id"
}
```

**Response:**
```json
{
  "Success": true,
  "OperationId": "abc123...",
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
    "MerchantId": "12345678901234"
  },
  "PrintTextRaw": "...",
  "PrintTextSanitized": "...",
  "LastDisplayText": "Insert card",
  "Error": null,
  "ErrorCode": null,
  "DbRowId": 1,
  "ReceiptFileId": "2026-02-09/abc123..."
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
  "PurchaseMinor": 10000,
  "CashbackMinor": 5000,
  "Currency": "NOK",
  "OperatorId": "4321",
  "OptionalData": "",
  "ClientRequestId": "optional-client-id"
}
```

**Response:** Same structure as purchase

### Admin Operations

#### POST /v1/admin/avstemming

Runs avstemming (end-of-day reconciliation).

**Request:**
```json
{
  "Password": "0000"
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
  "AdminCode": "3130",
  "Password": "0000"
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
    "Cursor": 1,
    "EventId": "...",
    "OperationId": "...",
    "Timestamp": "2026-02-09T12:00:00Z",
    "EventType": "OperationStarted",
    "Payload": { ... }
  }
]
```

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `terminal_busy` | 409 | Terminal is busy with another operation |
| `terminal_not_ready` | 503 | Terminal is not ready |
| `operation_timeout` | 408 | Operation timed out |
| `operation_rejected` | 422 | Operation rejected by terminal |
| `vendor_call_failure` | 500 | Vendor DLL call failed |
| `invalid_request` | 400 | Invalid request format |
| `diagnostics_disabled` | 403 | Diagnostics are disabled |

## Error Response Format

All error responses follow this structure:
```json
{
  "Error": "Human-readable error message",
  "ErrorCode": "machine_readable_code",
  "OperationId": "op-uuid",
  "Details": "Additional error details"
}
```

## Idempotency

Operations support idempotency via the `ClientRequestId` field. If a `ClientRequestId` is provided and an operation with that ID already exists, the server returns the existing result instead of executing a new operation.

## Timeouts

- Financial operations: 180 seconds (configurable)
- Admin operations: 300 seconds (configurable)

Timeouts start after the vendor call succeeds (callResult=1).

## Cancellation

Client HTTP disconnect does not cancel terminal activity. The server continues processing, persists results, and clients can retrieve them via events or operation lookup.