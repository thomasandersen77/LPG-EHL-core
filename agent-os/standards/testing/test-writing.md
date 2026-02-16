## Test coverage best practices

This codebase spans protocol/serial comms, state machines, persistence, and edge integrations (payment/azure). Tests should mirror those boundaries.

### What to test (prioritized)

- **Protocol correctness** (`lpg-ehl-core`): codec framing, checksum, command parsing, noise handling.
- **Transport behavior** (`lpg-transport`): timeouts, reconnect logic, buffering, watchdogs.
- **Business rules/state machines** (`lpg-ehl-service`): pump state transitions, transaction lifecycle, idempotency and conflict handling.
- **Integration boundaries**:
  - Payment terminal HTTP client behavior (busy/not-ready/idempotency) against stubs.
  - Azure queue sync/outbox behavior with retryable failure modes.

### How to test in this repo

- **JUnit 5 + Kotlin test** for unit and component tests.
- **Spring Boot tests** for service-level integration where wiring matters (profiles, persistence).
- **Use the emulator/simulators** for deterministic testing instead of “real serial” whenever possible:
  - In-memory emulator (`lab` mode)
  - serialport simulator (SOCAT / virtual serial where needed)
- **WireMock** for HTTP integration stubs (payment terminal mono server / cloud endpoints when appropriate).

### Pragmatic discipline

- **Add tests when you’ve finished a coherent behavior** (not on every intermediate refactor).
- **Prefer behavior tests** over implementation-detail tests to avoid brittleness.
- **Name tests after the business scenario** (especially around state transitions and failure modes).
