# Payment Terminal Simulator Upgrade Report (SSE + WebSocket + SSOT)

## Scope

This report documents the requested simulator upgrade in:

- `lpg-ehl-payment-terminal-sim`
- `lpg-ehl-payment-terminal-gui`
- `scripts/sim-all.sh`

Main goal delivered: keep all existing REST + SSE behavior intact and add WebSocket event streaming on `/v1/events/ws`, backed by the same event source as polling/SSE.

---

## What was implemented

## 1) New WebSocket endpoint `/v1/events/ws`

Implemented in simulator with Spring WebSocket:

- Added `WebSocketConfig` with endpoint registration:
  - `WS /v1/events/ws`
- Added `TerminalEventsWebSocketHandler`:
  - Sends initial hello message on connect
  - Supports `?since=<cursor-or-timestamp>` backlog replay
  - Streams live events after backlog
  - Removes disconnected/error sessions from registry
  - Logs:
    - `WS connected ...`
    - `WS disconnected ...`
    - `WS broadcast event cursor=...`

---

## 2) SSOT refactor for events (polling + SSE + WS)

Replaced the previous event handling with explicit single-source components:

- `TerminalEventStore`
  - Append events once
  - Cursor/timestamp based retrieval (`resolveSince`)
- `TerminalEventStreamRegistry`
  - Tracks active SSE emitters + WS subscribers
- `TerminalEventPublisher`
  - Unified publish path:
    - append to store
    - broadcast to SSE
    - broadcast to WS
  - Unified backlog replay for both SSE and WS
- `SseEventSender` + `JacksonSseEventSender`
  - Isolated SSE envelope sending

Result: GUI/actions publish once; event appears consistently on:

- `GET /v1/events?since=...`
- `GET /v1/events/stream`
- `WS /v1/events/ws`

---

## 3) SSE compatibility preserved

SSE endpoint remains:

- `GET /v1/events/stream`

Behavior remains compatible:

- `id: <cursor>`
- `event: <EventType>`
- `data: <EventEnvelope JSON>`

Heartbeat support is configurable via:

- `payment-terminal-sim.sse-heartbeat-ms`

---

## 4) Dirty-mode scaffolding added (OFF by default)

Added configuration and hooks (no destructive defaults):

- `payment-terminal-sim.dirty.enabled`
- `payment-terminal-sim.dirty.latency-ms`
- `payment-terminal-sim.dirty.duplicate-event-probability`
- `payment-terminal-sim.dirty.out-of-order`

Default in `application.yaml` is OFF/0.

---

## 5) GUI wiring compatibility updates

Updated GUI module imports/types to use renamed store class:

- `EventStore` -> `TerminalEventStore`

No breaking GUI API behavior introduced.

---

## 6) Simulator documentation updates

Updated `lpg-ehl-payment-terminal-sim/README.md` with:

- Full endpoint overview including WS
- Examples for:
  - purchase
  - polling
  - SSE
  - WebSocket (`wscat`)
- Event architecture section updated to SSOT components
- New config options documented (`sse-heartbeat-ms`, dirty mode)

---

## 7) `scripts/sim-all.sh` improvements

Improved robustness and usability:

- Safer shell mode:
  - `set -euo pipefail`
  - strict `IFS`
- Added terminal startup health check:
  - waits for `http://localhost:<port>/health`
  - configurable timeout with `--terminal-wait-seconds=<sec>`
  - optional skip with `--skip-healthcheck`
- Added numeric validation for terminal port and wait timeout
- Added quick usage hints in ready output for:
  - polling
  - SSE
  - WS

---

## Tests added

Added simulator-focused tests:

- `TerminalEventStoreTest`
  - verifies generated event is stored and retrievable
- `TerminalEventPublisherTest`
  - verifies SSE subscriber receive path
  - verifies WS backlog replay via `since`
  - verifies WS registration failure handling on send failure
- `TerminalEventsWebSocketHandlerTest`
  - verifies WS connection receives hello + backlog (`since`) over mocked session

---

## Validation results

Executed:

```bash
mvn -pl lpg-ehl-payment-terminal-sim test -DskipITs
```

Result:

- **BUILD SUCCESS**
- Tests run: **6**
- Failures: **0**
- Errors: **0**

---

## How to run simulator

## Option A: Build simulator artifacts and run full stack script

```bash
./scripts/build-simulators.sh --with-tests
./scripts/sim-all.sh --terminal-port=18080
```

Optional GUI mode:

```bash
./scripts/sim-all.sh --gui
```

---

## Option B: Run only payment terminal simulator module

```bash
mvn -pl lpg-ehl-payment-terminal-sim spring-boot:run
```

or

```bash
mvn -pl lpg-ehl-payment-terminal-sim clean package
java -jar lpg-ehl-payment-terminal-sim/target/payment-terminal-sim-exec.jar
```

---

## Quick verification commands

### Health

```bash
curl http://localhost:18080/health
```

### Trigger payment event

```bash
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{"AmountMinor":12500,"Currency":"NOK","OperatorId":"0000"}'
```

### Polling

```bash
curl "http://localhost:18080/v1/events?since=0"
```

### SSE

```bash
curl -N "http://localhost:18080/v1/events/stream?since=0"
```

### WebSocket

```bash
wscat -c ws://localhost:18080/v1/events/ws
# or with backlog
wscat -c "ws://localhost:18080/v1/events/ws?since=10"
```

---

## Notes

- Production modules (`lpg-ehl-service`, webapp, etc.) were not modified as part of this simulator upgrade.
- Existing REST endpoints and payload compatibility were preserved.