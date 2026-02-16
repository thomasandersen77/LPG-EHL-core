# Payment Terminal Simulator

HTTP REST API simulator for Baxi/NETS payment terminal testing without physical hardware.

This Spring Boot application simulates the PaymentTerminalMonoServer API, providing full endpoint coverage for development and testing.

## Features

- ✅ **Full API Coverage**: All 17 REST endpoints from OpenAPI spec
- ✅ **SSE Event Stream**: Real-time events via `/v1/events/stream`
- ✅ **WebSocket Event Stream**: Real-time events via `/v1/events/ws` (supports `?since=` backlog)
- ✅ **REST Polling Events**: Cursor/timestamp polling via `/v1/events?since=...`
- ✅ **Scenario Testing**: Simulate APPROVED, DECLINED, WRONG_PIN, USER_CANCEL, TIMEOUT, BUSY, NOT_READY
- ✅ **Idempotency**: `ClientRequestId` support with response caching
- ✅ **Terminal State Machine**: CLOSED → OPEN → READY → BUSY lifecycle
- ✅ **Configurable Delays**: Realistic operation delays with random jitter
- ✅ **PascalCase Responses**: OpenAPI-compliant JSON (except `/health`)

## Quick Start

### Build

```bash
# From project root
mvn clean package

# Or build only this module
mvn -pl lpg-ehl-payment-terminal-sim clean package
```

### Run

```bash
# Default port 18080
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim.jar

# Custom port via Spring Boot property
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim.jar --server.port=8080

# Or via Maven
mvn -pl lpg-ehl-payment-terminal-sim spring-boot:run
```

### Verify

```bash
curl http://localhost:18080/health
# {"status":"ok","timestamp":"2026-02-10T...","configLoaded":true}
```

## Scenario Configuration

Scenarios can be selected in three ways (in priority order):

1. **Per request** with the `X-Terminal-Scenario` header (requires `payment-terminal-sim.allow-scenario-header=true`).
2. **Default scenario** in `application.yaml` (`payment-terminal-sim.default-scenario`).
3. **Fallback** to `APPROVED` if the name is unknown.

YAML scenario files are supported when `payment-terminal-sim.scenarios-enabled=true`.
The simulator looks for `<SCENARIO>.yml` in:

- `classpath:scenarios/` (built-in files under `src/main/resources/scenarios`)
- `payment-terminal-sim.scenarios-path` (external folder, if configured)

If a YAML file is found, it overrides the built-in enum behavior for that scenario.

### Available Scenarios (minimum pack)

- `APPROVED`
- `DECLINED`
- `WRONG_PIN`
- `USER_CANCEL`
- `TIMEOUT`
- `BUSY`
- `NOT_READY`

### Start simulator with a default scenario

Edit `src/main/resources/application.yaml`:

```yaml
payment-terminal-sim:
  default-scenario: WRONG_PIN
  scenarios-enabled: true
```

Then start the simulator as usual:

```bash
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim.jar
```

You can also start directly with `java -jar` and override the default scenario inline:

```bash
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim.jar \
  --payment-terminal-sim.default-scenario=WRONG_PIN
```

### Start with scenario per request (header override)

```bash
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -H "X-Terminal-Scenario: DECLINED" \
  -d '{
    "AmountMinor": 12500,
    "Currency": "NOK",
    "OperatorId": "0000",
    "ClientRequestId": "req-1"
  }'
```

### Use external scenario files (run from JAR)

1. Put your scenario files in a folder, for example `/opt/payment-terminal-sim/scenarios`.
2. Create an external `application.yaml` with the scenario path:

```yaml
payment-terminal-sim:
  default-scenario: APPROVED
  scenarios-enabled: true
  scenarios-path: /opt/payment-terminal-sim/scenarios
```

3. Start the JAR and point Spring Boot to the external config:

```bash
java -jar payment-terminal-sim.jar \
  --spring.config.additional-location=file:/opt/payment-terminal-sim/
```

### Can multiple scenarios be loaded at once?

Yes. All scenario YAML files in the configured `scenarios` folder can be present.
The simulator selects **one scenario per request** based on the header or default configuration.

## API Endpoints

### Health & Terminal Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check (lowercase keys!) |
| GET | `/v1/terminal/status` | Terminal readiness |
| POST | `/v1/terminal/open` | Open terminal |
| POST | `/v1/terminal/close` | Close terminal |

### Financial Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/payments/purchase` | Purchase (card payment) |
| POST | `/v1/payments/refund` | Refund |
| POST | `/v1/payments/cashback` | Purchase + cashback |

### Administrative Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/admin/avstemming` | Reconciliation |
| POST | `/v1/admin/cancel` | Cancel operation |
| POST | `/v1/admin/reversal` | Reverse last transaction |
| POST | `/v1/admin/z-report` | Z-report (end-of-day) |
| POST | `/v1/admin/last-receipt` | Last receipt |
| POST | `/v1/admin/software` | Software download |
| POST | `/v1/admin/dataset` | Dataset download |
| POST | `/v1/admin/code` | Generic admin code |

### Events

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/events/stream` | SSE event stream |
| GET | `/v1/events?since=X` | Poll events (cursor or timestamp) |

### WebSocket

| Method | Endpoint | Description |
|--------|----------|-------------|
| WS | `/v1/events/ws` | WebSocket event stream (same EventEnvelope JSON as SSE/poll) |

## Usage Examples

### 1. Terminal Lifecycle

```bash
# Check status
curl http://localhost:18080/v1/terminal/status

# Open terminal
curl -X POST http://localhost:18080/v1/terminal/open

# Verify ready
curl http://localhost:18080/v1/terminal/status
```

### 2. Purchase (Approved)

```bash
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{
    "AmountMinor": 12500,
    "Currency": "NOK",
    "OperatorId": "0000",
    "OptionalData": "LPG Autogas",
    "ClientRequestId": "test-123"
  }'
```

Response (PascalCase):
```json
{
  "Success": true,
  "OperationId": "abc-123-...",
  "StartedAt": "2026-02-10T...",
  "CompletedAt": "2026-02-10T...",
  "DurationMs": 2000,
  "CallResult": 1,
  "LocalModeResult": 0,
  "ResponseCode": "00",
  "LastDisplayText": "GODKJENT",
  "PrintTextRaw": "NETS AS\nTRANSAKSJON GODKJENT\n...",
  "ReceiptFileId": "..."
}
```

### 3. Scenario Testing

Simulate different outcomes via `X-Terminal-Scenario` header:

```bash
# Wrong PIN
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -H "X-Terminal-Scenario: WRONG_PIN" \
  -d '{"AmountMinor": 10000}'

# User cancel
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "X-Terminal-Scenario: USER_CANCEL" \
  -d '{"AmountMinor": 10000}'

# Declined
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "X-Terminal-Scenario: DECLINED" \
  -d '{"AmountMinor": 10000}'
```

### 4. SSE Event Stream

```bash
# Subscribe to event stream
curl -N http://localhost:18080/v1/events/stream?since=0
```

Output:
```
event: OperationStarted
id: 1
data: {"Cursor":1,"EventId":"...","OperationId":"...","Timestamp":"...","EventType":"OperationStarted","Payload":{...}}

event: DisplayText
id: 2
data: {"Cursor":2,...}

event: OperationCompleted
id: 3
data: {"Cursor":3,...}
```

### 5. Poll Events

```bash
# Get all buffered events
curl http://localhost:18080/v1/events?since=0

# Get events since cursor 5
curl http://localhost:18080/v1/events?since=5

# Get events since timestamp
curl http://localhost:18080/v1/events?since=2026-02-10T12:00:00Z
```

### 6. WebSocket Event Stream

```bash
# with wscat (npm i -g wscat)
wscat -c ws://localhost:18080/v1/events/ws

# backlog + live stream from cursor 10
wscat -c "ws://localhost:18080/v1/events/ws?since=10"
```

Example message payload (same EventEnvelope as polling/SSE data):
```json
{
  "Cursor": 123,
  "EventType": "OperationCompleted",
  "Timestamp": "2026-02-13T18:30:00Z",
  "Payload": {
    "Success": true,
    "OperationId": "..."
  }
}
```

### 7. Admin Operations

```bash
# Avstemming (reconciliation)
curl -X POST http://localhost:18080/v1/admin/avstemming \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'

# Reversal (annullering)
curl -X POST http://localhost:18080/v1/admin/reversal \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'

# Z-report
curl -X POST http://localhost:18080/v1/admin/z-report \
  -H "Content-Type: application/json" \
  -d '{"Password": "0000"}'
```

## Configuration

Edit `src/main/resources/application.yaml`:

```yaml
server:
  port: 18080

payment-terminal-sim:
  # Default scenario (APPROVED, WRONG_PIN, USER_CANCEL, DECLINED)
  default-scenario: APPROVED

  # Operation delay (milliseconds)
  operation-delay-ms: 2000

  # Enable random jitter (delay will be between 50% and 100% of operation-delay-ms)
  enable-random-jitter: true

  # Terminal identity
  terminal-id: "12345678"
  merchant-id: "12345678901234"

  # Allow scenario override via X-Terminal-Scenario header
  allow-scenario-header: true

  # Event buffer size (for SSE)
  event-buffer-size: 1000

  # SSE heartbeat cadence
  sse-heartbeat-ms: 5000

  # Simulator profile: lab | field
  profile: lab

  # Field mode knobs (used when profile=field)
  field:
    operation-delay-min-ms: 2000
    operation-delay-max-ms: 10000
    not-ready-probability: 0.05
    rejection-probability: 0.1
    rejection-wrong-pin-probability: 0.5

  # Dirty mode scaffolding (OFF by default)
  dirty:
    enabled: false
    latency-ms: 0
    duplicate-event-probability: 0.0
    out-of-order: false
```

## Scenarios

| Scenario | HTTP | Success | LocalModeResult | ResponseCode | Description |
|----------|------|---------|-----------------|--------------|-------------|
| `APPROVED` | 200 | true | 0 | "00" | Purchase approved |
| `WRONG_PIN` | 422 | false | 2 | "Z1" | Wrong PIN entered |
| `USER_CANCEL` | 422 | false | 2 | "" | User cancelled |
| `DECLINED` | 422 | false | 2 | "05" | Card declined |
| `TIMEOUT` | 408 | - | - | - | Operation timeout |
| `BUSY` | 409 | - | - | - | Terminal busy (automatic) |
| `NOT_READY` | 503 | - | - | - | Terminal not ready (automatic) |

## Terminal State Machine

```
CLOSED ──(POST /v1/terminal/open)──> OPEN ──(auto)──> READY
READY ──(operation start)──> BUSY ──(operation end)──> READY
READY ──(POST /v1/terminal/close)──> CLOSED
```

**Important:**
- Operations require terminal to be in READY state
- Only one operation at a time (BUSY state returns HTTP 409)
- State transitions are automatic

## Idempotency

Send `ClientRequestId` in request body to enable idempotent retries:

```bash
curl -X POST http://localhost:18080/v1/payments/purchase \
  -d '{"AmountMinor": 10000, "ClientRequestId": "session-abc-123"}'

# Second call with same ClientRequestId returns cached response
curl -X POST http://localhost:18080/v1/payments/purchase \
  -d '{"AmountMinor": 10000, "ClientRequestId": "session-abc-123"}'
```

## Error Handling

| HTTP | ErrorCode | Description |
|------|-----------|-------------|
| 400 | `invalid_request` | Invalid request body |
| 408 | `operation_timeout` | Operation timeout |
| 409 | `terminal_busy` | Terminal busy with another operation |
| 422 | `operation_rejected` | Terminal rejected operation |
| 503 | `terminal_not_ready` | Terminal not ready |
| 500 | `vendor_call_failure` | Internal error |

Error response format (busy/not ready/timeout):
```json
{
  "Error": "Terminal is busy with another operation",
  "ErrorCode": "terminal_busy",
  "Details": "Current operation: abc-123"
}
```

Rejected response format (422, OperationResponse):
```json
{
  "Success": false,
  "OperationId": "op-123",
  "CallResult": 1,
  "ResponseCode": "Z1",
  "RejectionReason": "3:2:Z1",
  "LastDisplayText": "AVVIST",
  "ErrorCode": "operation_rejected",
  "Error": "Terminal rejected the operation"
}
```

## Integration with lpg-ehl-service

Configure your application to use the simulator:

```yaml
# application.yaml
payment-terminal:
  base-url: http://localhost:18080
  timeout-seconds: 180
```

## Testing Tips

### 1. Test Happy Path

```bash
curl -X POST http://localhost:18080/v1/terminal/open
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}'
```

### 2. Test Busy Scenario

```bash
# Start long operation in background
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}' &

# Immediately try another (should get 409)
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 20000}'
```

### 3. Test Not Ready

```bash
curl -X POST http://localhost:18080/v1/terminal/close
curl -X POST http://localhost:18080/v1/payments/purchase -d '{"AmountMinor": 10000}'
# Should return 503 Service Unavailable
```

### 4. Test Timeout

```bash
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "X-Terminal-Scenario: TIMEOUT" \
  -d '{"AmountMinor": 10000}'
```

### 5. Test Event Stream with WebSocat

```bash
# Install websocat: brew install websocat
websocat ws://localhost:18080/v1/events/ws?since=0
```

## Logging

Logs are written to stdout with configurable levels:

```yaml
logging:
  level:
    root: INFO
    no.cloudberries.lpg.payment.terminal.sim: DEBUG
```

Sample log output:
```
12:34:56.789 [http-nio-18080-exec-1] INFO  PaymentController - Purchase request: amount=10000, operatorId=0000, clientRequestId=test-123
12:34:56.790 [http-nio-18080-exec-1] DEBUG ScenarioManager - Scenario selected from header: APPROVED
12:34:56.791 [http-nio-18080-exec-1] DEBUG TerminalStateManager - Operation started: abc-123
12:34:58.800 [http-nio-18080-exec-1] INFO  PaymentController - Purchase completed: operationId=abc-123, success=true
```

## Architecture

```
Controller Layer
├── HealthController        (GET /health)
├── TerminalController      (Terminal lifecycle)
├── PaymentController       (Financial operations)
├── AdminController         (Admin operations)
└── EventController         (SSE + polling)

WebSocket Layer
└── TerminalEventsWebSocketHandler (WS /v1/events/ws)

Service Layer
├── TerminalStateManager    (State machine: CLOSED/OPEN/READY/BUSY)
├── ScenarioManager         (Scenario selection + delays)
├── ReceiptGenerator        (Mock receipt text)
├── TerminalEventStore      (SSOT in-memory event buffer)
├── TerminalEventPublisher  (Broadcast to SSE + WS)
└── TerminalEventStreamRegistry (Active SSE/WS subscribers)

Configuration
├── SimulatorConfig         (@ConfigurationProperties)
├── JacksonConfig           (PascalCase serialization)
└── CorsConfig              (CORS for browser testing)

Exception Handling
├── TerminalExceptionHandler (Global @RestControllerAdvice)
└── TerminalExceptions      (Custom exceptions for each error code)
```

## Differences from Production MonoServer

| Feature | Simulator | Production MonoServer |
|---------|-----------|----------------------|
| Vendor DLL | ❌ Not used | ✅ Required (baxi_dotnet.dll) |
| Physical Terminal | ❌ Not required | ✅ Required |
| Database | ❌ In-memory only | ✅ SQLite persistence |
| Receipt Storage | ❌ In-memory | ✅ Filesystem |
| Mono Dependency | ❌ None | ✅ Required on Linux |
| Platform | ✅ Any JVM (cross-platform) | ⚠️ Windows/.NET or Linux/Mono |

## Troubleshooting

### Port already in use

```bash
# Check what's using port 18080
lsof -i :18080

# Use different port
java -jar payment-terminal-sim.jar --server.port=8080
```

### Terminal stuck in BUSY state

The simulator automatically transitions BUSY → READY after operation completes. If stuck:

```bash
# Restart simulator
# State is in-memory, restart clears everything
```

### Events not appearing in SSE stream

Events are buffered (default 1000 events). Old events are discarded when buffer is full.

```yaml
# Increase buffer size
payment-terminal-sim:
  event-buffer-size: 5000
```

## Related Documentation

- [MASTER_PAYMENT_TERMINAL_GUIDE.md](../instructions/terminal/MASTER_PAYMENT_TERMINAL_GUIDE.md) - Complete payment terminal documentation
- [openapi-payment-terminal.yaml](../openapi-payment-terminal.yaml) - OpenAPI specification
- [Terminal_API_Contract.md](../instructions/Terminal_API_Contract.md) - API contract details

## License

Proprietary - Cloudberries AS
