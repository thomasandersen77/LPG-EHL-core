# LAB Testing Guide - EHL Protocol

## Overview

This guide covers testing the EHL protocol in LAB MODE with the emulator.

## State Machine Flow

The pump state machine is now **hardware-driven**:

```
IDLE → (UNBLOCK) → READY_TO_PUMP → (auto-detect 0x06/0x07) → PUMPING → (BLOCK) → PAYMENT_PENDING → (SETTLE) → IDLE
                         ↓
                   (60s timeout)
                         ↓
                       IDLE + Transaction CANCELLED
```

**Key change**: Pumping is no longer triggered via API. The system polls the dispenser STATE
and automatically transitions to PUMPING when raw state 0x06/0x07 (DELIVERY_ACTIVE + NOZZLE_LIFTED) is detected.

## Debug Endpoints

Available at `/api/debug/*` (requires `--spring.profiles.active=debug-api`):

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/debug/health` | System health check |
| GET | `/api/debug/state/{addr}` | Get pump state (business logic) |
| GET | `/api/debug/raw-state/{addr}` | Get raw dispenser STATE |
| GET | `/api/debug/volume/{addr}` | Get current volume |
| POST | `/api/debug/linetest/{addr}` | Test communication |
| POST | `/api/debug/unblock/{addr}` | Unblock pump (start 60s timeout) |
| POST | `/api/debug/block/{addr}` | Block pump (stop delivery) |
| POST | `/api/debug/reset/{addr}` | Reset pump to IDLE |
| POST | `/api/debug/settle/{addr}` | Settle transaction (LAB/DEBUG only) |

**Removed from debug-api**: `POST /api/debug/start-pumping/{addr}` - Pumping is hardware-triggered in FIELD/SOCAT mode.

**Still available in webapp**: `POST /api/v1/emulator/pump/{address}/start-pumping` - For /control GUI in LAB MODE.

## Testing with SOCAT Mode

### 1. Start socat PTY pair
```bash
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1
```

### 2. Start PLS Simulator on /tmp/ttyV0
(Simulator should respond to EHL commands and transition to PUMPING state when nozzle is lifted)

### 3. Start headless app
```bash
java -jar release/lpg-ehl-webapp.jar \
  --ehl.transport.mode=SOCAT \
  --ehl.serial.port=/tmp/ttyV1 \
  --spring.profiles.active=local,debug-api
```

### 4. Test flow
```bash
# Test communication
curl -X POST http://localhost:8080/api/debug/linetest/1

# Unblock pump (starts 60s timeout)
curl -X POST http://localhost:8080/api/debug/unblock/1

# Check state (should be READY_TO_PUMP)
curl http://localhost:8080/api/debug/state/1

# Simulator: lift nozzle to start pumping
# System auto-detects pumping from raw state 0x06/0x07

# Check state (should be PUMPING)
curl http://localhost:8080/api/debug/state/1

# Check volume during pumping
curl http://localhost:8080/api/debug/volume/1

# Block pump (stop delivery)
curl -X POST http://localhost:8080/api/debug/block/1

# Check state (should be PAYMENT_PENDING)
curl http://localhost:8080/api/debug/state/1

# Settle (simulate payment)
curl -X POST http://localhost:8080/api/debug/settle/1

# Verify start-pumping endpoint is gone (should return 404)
curl -X POST http://localhost:8080/api/debug/start-pumping/1
# Expected: 404 Not Found
```

## Logging

### INFO Level
- State transitions (IDLE → READY_TO_PUMP → PUMPING → PAYMENT_PENDING → IDLE)
- UNBLOCK/BLOCK commands sent
- 60s timeout start/cancel/expire
- Transaction create/update/cancel

### DEBUG Level  
- TX/RX HEX protocol bytes
- State poll results

### Emulator Logging
The emulator logs each command received with input/output hex.

### File Logging
Headless app writes logs to:
- `logs/headless.log` - Main application log (rolling, 30 days)
- `logs/protocol.log` - EHL TX/RX protocol traffic (rolling, 7 days)

Configure log path:
```bash
java -jar headless.jar -Dlogging.file.name=/var/log/lpg/headless.log
```

### Heartbeat
Heartbeat logs every 30 seconds showing:
- Pump state and last transition
- Volume, amount, price
- Nozzle position

During active pumping, status logs every 3 seconds.

## 60s Timeout Behavior

When UNBLOCK is sent:
1. Pump transitions to READY_TO_PUMP
2. Transaction created with status STARTED
3. 60s timeout starts
4. System polls dispenser STATE every 500ms

If customer starts pumping (raw state 0x06/0x07 detected):
- Timeout cancelled
- Pump transitions to PUMPING

If 60s expires without pumping:
- BLOCK command sent automatically
- Transaction marked as CANCELLED
- Pump returns to IDLE
