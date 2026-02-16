# Payment Terminal Mono Server API Report

**Date:** 2026-02-10  
**Server:** saklink.tplinkdns.com:2222 (84.214.101.100)  
**Status:** Server taken offline during investigation

---

## Executive Summary

The Payment Terminal Mono Server is running on a Debian server at `saklink.tplinkdns.com` on port 18080. During the initial discovery, the server returned "Bad Request (Invalid host)" errors for all curl requests. This issue has been diagnosed and a configuration fix has been prepared for deployment.

**Root Cause:** Mono's `HttpListener` requires the HTTP `Host` header to match the registered prefix exactly. The original `server.json` bound to `127.0.0.1`, but curl with `http://localhost:18080` sends `Host: localhost`, causing a mismatch.

**Solution:** Update `server.json` with `"bindAddress": "+"` to accept any Host header.

---

## Server Configuration Fix

### Original Configuration
```json
{
  "bindAddress": "127.0.0.1",
  "bindPort": 18080,
  ...
}
```

**Problem:** Only accepts requests with `Host: 127.0.0.1`

### Fixed Configuration
```json
{
  "bindAddress": "+",
  "bindPort": 18080,
  "databasePath": "./data/payment_terminal.db",
  "receiptStoragePath": "./receipts",
  "receiptRetentionDays": 0,
  "receiptRetentionMaxOperations": 0,
  "financialOperationTimeoutSeconds": 180,
  "adminOperationTimeoutSeconds": 300,
  "enableDiagnostics": true,
  "busyRetry": {
    "enabled": true,
    "maxRetrySeconds": 20,
    "retryDelayMs": 1000
  },
  "baxiAssemblyPath": "./baxi_dotnet.dll",
  "vendorInfoExtended": "ECR;;;;",
  "eventLogPath": "./logs/events.tsv"
}
```

**Changes:**
1. ✅ `"bindAddress": "+"` — Accept requests with any Host header (localhost, 127.0.0.1, hostname, etc.)
2. ✅ `"enableDiagnostics": true` — Enable `/v1/diag/*` endpoints for debugging
3. ✅ `"busyRetry.enabled": true` — Auto-retry when terminal is busy
4. ✅ `"eventLogPath"` — Enable TSV event timeline logging

**File location:** `/Users/tandersen/git/NorgesGass/lpg-ehl/server.json`

---

## Payment Terminal Mono Server API Contract

### Base URL
- **Development:** `http://127.0.0.1:18080`
- **Remote Server:** `http://192.168.1.190:18080` (internal network)
- **Public Access:** SSH tunnel via `saklink.tplinkdns.com:2222`

### API Endpoints Overview

#### 1. Health & Status
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Server health check |
| `/v1/terminal/status` | GET | Terminal readiness and identity |

#### 2. Terminal Lifecycle
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/terminal/open` | POST | Open terminal connection |
| `/v1/terminal/close` | POST | Close terminal connection |

#### 3. Financial Operations
| Endpoint | Method | Description | Request Body |
|----------|--------|-------------|--------------|
| `/v1/payments/purchase` | POST | Purchase transaction | `{"amountMinor":10000,"currency":"NOK","operatorId":"4321"}` |
| `/v1/payments/refund` | POST | Refund transaction | Same as purchase |
| `/v1/payments/cashback` | POST | Purchase + cashback | `{"purchaseMinor":10000,"cashbackMinor":5000,"currency":"NOK"}` |

**Notes:**
- `amountMinor` is in øre (cents): 10000 = 100 NOK
- Supports idempotency via `clientRequestId` field
- Timeout: 180 seconds (configurable)

#### 4. Admin Operations
| Endpoint | Method | Description | Request Body |
|----------|--------|-------------|--------------|
| `/v1/admin/avstemming` | POST | End-of-day reconciliation | `{"password":"0000"}` |
| `/v1/admin/cancel` | POST | Cancel current operation | `{"password":"0000"}` |
| `/v1/admin/reversal` | POST | Reverse last transaction | `{"password":"0000"}` |
| `/v1/admin/z-report` | POST | Generate Z-report | `{"password":"0000"}` |
| `/v1/admin/last-receipt` | POST | Retrieve last receipt | `{"password":"0000"}` |
| `/v1/admin/software` | POST | Software download | `{"password":"0000"}` |
| `/v1/admin/dataset` | POST | Dataset download | `{"password":"0000"}` |
| `/v1/admin/code` | POST | Run arbitrary admin code | `{"code":12592,"password":"0000"}` |

**Notes:**
- Default password: `0000`
- Timeout: 300 seconds (configurable)

#### 5. Events & Real-time Monitoring
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/events?since=<cursor>` | GET | Poll for events since cursor |
| `/v1/events/stream` | GET | Server-Sent Events (SSE) stream |

**Cursor formats:**
- Numeric sequence: `since=0` (recommended)
- ISO-8601 timestamp: `since=2026-02-09T12:00:00Z`

#### 6. Diagnostics (requires `enableDiagnostics: true`)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/diag/schema` | GET | Get Baxi control schema |
| `/v1/diag/sendjson` | POST | Send raw JSON to terminal |
| `/v1/diag/sendtld` | POST | Send raw TLD data to terminal |
| `/v1/diag/confirm` | POST | Confirm terminal operation |

---

## Complete API Test Script

Once the server is back online, deploy the fixed `server.json` and run these curl commands:

### Deployment Steps
```bash
# 1. Copy the fixed server.json to the remote server
scp -P 2222 /Users/tandersen/git/NorgesGass/lpg-ehl/server.json thomas@saklink.tplinkdns.com:~/server.json

# 2. SSH in and find the Mono server process
ssh -p 2222 thomas@saklink.tplinkdns.com 'ps aux | grep payment-terminal-mono-server'

# 3. Copy to the correct location and restart (adjust path as needed)
ssh -p 2222 thomas@saklink.tplinkdns.com 'sudo cp ~/server.json /path/to/server.json && sudo systemctl restart payment-terminal-mono-server'
```

### API Test Commands

```bash
# === Health & Status ===
curl -s http://127.0.0.1:18080/health | jq .
# Expected: {"status":"ok","timestamp":"...","configLoaded":true}

curl -s http://127.0.0.1:18080/v1/terminal/status | jq .
# Expected: {"vendorDllLoadable":true,"terminalOpen":false,"terminalReady":false,...}

# === Terminal Lifecycle ===
curl -s -X POST http://127.0.0.1:18080/v1/terminal/open | jq .
# Expected: {"success":true,"message":"Terminal opened"}

# Wait a few seconds for terminal to become ready
sleep 3

curl -s http://127.0.0.1:18080/v1/terminal/status | jq .
# Expected: {"terminalOpen":true,"terminalReady":true,...}

# === Financial Operations ===
# Test purchase (100 NOK)
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "amountMinor": 10000,
    "currency": "NOK",
    "operatorId": "4321",
    "optionalData": "",
    "clientRequestId": "test-purchase-001"
  }' \
  http://127.0.0.1:18080/v1/payments/purchase | jq .

# Expected: OperationResponse with success=true/false, callResult, localModeFields, etc.

# === Admin Operations ===
# Avstemming (reconciliation)
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"password":"0000"}' \
  http://127.0.0.1:18080/v1/admin/avstemming | jq .

# Last receipt
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"password":"0000"}' \
  http://127.0.0.1:18080/v1/admin/last-receipt | jq .

# Z-report
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"password":"0000"}' \
  http://127.0.0.1:18080/v1/admin/z-report | jq .

# === Events ===
curl -s "http://127.0.0.1:18080/v1/events?since=0" | jq .
# Expected: Array of events with eventId, operationId, timestamp, eventType, payload

# === Diagnostics ===
curl -s http://127.0.0.1:18080/v1/diag/schema | jq .
# Expected: Schema of Baxi control interface

# === Terminal Close ===
curl -s -X POST http://127.0.0.1:18080/v1/terminal/close | jq .
# Expected: {"success":true,"message":"Terminal closed"}

curl -s http://127.0.0.1:18080/v1/terminal/status | jq .
# Expected: {"terminalOpen":false,"terminalReady":false,...}
```

---

## Error Codes Reference

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `invalid_request` | Invalid request format |
| 403 | `diagnostics_disabled` | Diagnostics are disabled in config |
| 408 | `operation_timeout` | Operation timed out |
| 409 | `terminal_busy` | Terminal is busy with another operation |
| 422 | `operation_rejected` | Operation rejected by terminal |
| 500 | `vendor_call_failure` | Vendor DLL call failed |
| 503 | `terminal_not_ready` | Terminal is not ready |

---

## Response Structure Examples

### Success Response (Purchase)
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

### Error Response
```json
{
  "error": "Terminal is busy with another operation",
  "errorCode": "terminal_busy"
}
```

### Terminal Status Response
```json
{
  "vendorDllLoadable": true,
  "terminalOpen": true,
  "terminalReady": true,
  "lastError": null,
  "terminalIdentity": {
    "TerminalID": "12345678",
    "MerchantId": "12345678901234"
  }
}
```

---

## Integration with lpg-ehl Project

The Payment Terminal Mono Server wraps the `Baxi.dll` (Nets payment terminal SDK) and provides a RESTful HTTP API. This allows the lpg-ehl LPG dispenser system to integrate card payment functionality.

### Architecture
```
┌─────────────────────────────────────────────────────┐
│ lpg-ehl-webapp (Kotlin/Spring Boot)                 │
│   └─> HTTP Client → Payment Terminal Mono Server   │
└─────────────────────────────────────────────────────┘
                          ↓ HTTP (port 18080)
┌─────────────────────────────────────────────────────┐
│ Payment Terminal Mono Server (C#/Mono)              │
│   └─> BaxiAdapter → baxi_dotnet.dll                 │
└─────────────────────────────────────────────────────┘
                          ↓ TCP (proprietary protocol)
┌─────────────────────────────────────────────────────┐
│ Nets Payment Terminal (Physical Hardware)           │
└─────────────────────────────────────────────────────┘
```

### Key Integration Points

1. **OpenAPI Spec Location:** 
   - Backend: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-webapp/src/main/resources/openapi.yaml`
   - Frontend: `/Users/tandersen/git/cloudberries-candidate-match-web/openapi.yaml`

2. **Payment Flow:**
   - User initiates fuel delivery
   - System calculates amount
   - POST to `/v1/payments/purchase` with amount
   - Poll `/v1/events` or subscribe to `/v1/events/stream` for result
   - System completes or rolls back transaction based on payment result

3. **Admin Operations:**
   - End-of-day reconciliation via `/v1/admin/avstemming`
   - Receipt retrieval via `/v1/admin/last-receipt`

---

## Next Steps

1. ✅ Fixed `server.json` created at `/Users/tandersen/git/NorgesGass/lpg-ehl/server.json`
2. ⏳ **TODO:** Deploy fixed `server.json` to Debian server when it comes back online
3. ⏳ **TODO:** Run complete API test suite and capture responses
4. ⏳ **TODO:** Document actual terminal identity (TerminalID, MerchantId)
5. ⏳ **TODO:** Test full purchase flow with physical terminal
6. ⏳ **TODO:** Integrate endpoints into lpg-ehl-webapp OpenAPI spec
7. ⏳ **TODO:** Create Kotlin HTTP client for payment terminal in lpg-ehl project

---

## Technical Notes

### Mono HttpListener Host Header Strictness
Mono's `HttpListener.Prefixes` requires exact Host header matching:
- Prefix: `http://127.0.0.1:18080/` → Only accepts `Host: 127.0.0.1:18080`
- Prefix: `http://localhost:18080/` → Only accepts `Host: localhost:18080`
- Prefix: `http://+:18080/` → Accepts **any** Host header (wildcard)

This is why using `"bindAddress": "+"` fixes the issue.

### Process Information
From previous SSH session:
- **Process:** `payment-terminal-mono-server.exe ./server.json` (PID: 3305)
- **User:** `alejand+` (UID: 1001)
- **Listening on:** `127.0.0.1:18080` (TCP)

### Network Configuration
- **Internal IP:** 192.168.1.190
- **External Access:** SSH port forwarding via `saklink.tplinkdns.com:2222`
- **Payment Terminal Port:** 6001 (non-HTTP, binary protocol)

---

## References

- **API Documentation:** `/Users/tandersen/git/NorgesGass/BaxiExperiments/PaymentTerminalMonoServer/API_CONTRACT.md`
- **Client Integration Guide:** `/Users/tandersen/git/NorgesGass/BaxiExperiments/PaymentTerminalMonoServer/CLIENT_GUIDE_VB6_REPLACEMENT.md`
- **Deployment Guide:** `/Users/tandersen/git/NorgesGass/BaxiExperiments/PaymentTerminalMonoServer/DEPLOYMENT.md`
- **Original SSH Discovery Report:** `/Users/tandersen/git/NorgesGass/lpg-ehl/ssh-server-report.md`
