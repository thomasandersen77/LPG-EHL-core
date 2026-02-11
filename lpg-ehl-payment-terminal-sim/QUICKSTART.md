# Payment Terminal Simulator - Quick Start

## 1. Build

```bash
mvn clean package -DskipTests
```

## 2. Start

```bash
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim.jar
```

Server starts on http://localhost:18080

## 3. Test

```bash
# Health check
curl http://localhost:18080/health

# Open terminal
curl -X POST http://localhost:18080/v1/terminal/open

# Purchase (approved)
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{"AmountMinor": 12500}'

# Purchase with wrong PIN
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -H "X-Terminal-Scenario: WRONG_PIN" \
  -d '{"AmountMinor": 10000}'

# Subscribe to events (SSE)
curl -N http://localhost:18080/v1/events/stream?since=0

# Poll events
curl http://localhost:18080/v1/events?since=0
```

## 4. Scenarios

Set via `X-Terminal-Scenario` header:
- `APPROVED` (default)
- `WRONG_PIN`
- `USER_CANCEL`
- `DECLINED`
- `TIMEOUT`

## 5. Configuration

Edit `application.yaml` or use environment variables:

```bash
# Custom port
java -jar payment-terminal-sim.jar --server.port=8080

# Fast mode (no delays)
java -jar payment-terminal-sim.jar \
  --payment-terminal-sim.operation-delay-ms=100 \
  --payment-terminal-sim.enable-random-jitter=false
```

## 6. All Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/v1/terminal/status` | GET | Terminal status |
| `/v1/terminal/open` | POST | Open terminal |
| `/v1/terminal/close` | POST | Close terminal |
| `/v1/payments/purchase` | POST | Purchase |
| `/v1/payments/refund` | POST | Refund |
| `/v1/payments/cashback` | POST | Cashback |
| `/v1/admin/avstemming` | POST | Reconciliation |
| `/v1/admin/cancel` | POST | Cancel |
| `/v1/admin/reversal` | POST | Reversal |
| `/v1/admin/z-report` | POST | Z-report |
| `/v1/admin/last-receipt` | POST | Last receipt |
| `/v1/admin/software` | POST | Software download |
| `/v1/admin/dataset` | POST | Dataset download |
| `/v1/admin/code` | POST | Generic admin code |
| `/v1/events/stream` | GET | SSE event stream |
| `/v1/events` | GET | Poll events |

## 7. Common Patterns

### Complete flow
```bash
curl -X POST http://localhost:18080/v1/terminal/open
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}'
curl -X POST http://localhost:18080/v1/admin/avstemming -d '{"Password": "0000"}'
curl -X POST http://localhost:18080/v1/terminal/close
```

### Idempotency
```bash
# Same ClientRequestId returns cached response
curl -X POST http://localhost:18080/v1/payments/purchase \
  -d '{"AmountMinor": 10000, "ClientRequestId": "session-123"}'

curl -X POST http://localhost:18080/v1/payments/purchase \
  -d '{"AmountMinor": 10000, "ClientRequestId": "session-123"}'
```

### Error simulation
```bash
# Terminal busy (run in background first)
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}' &
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 20000}'
# Returns HTTP 409

# Terminal not ready
curl -X POST http://localhost:18080/v1/terminal/close
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}'
# Returns HTTP 503
```

## See README.md for complete documentation
