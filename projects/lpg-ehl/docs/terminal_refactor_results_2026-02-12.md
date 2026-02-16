### Payment terminal refactor results (2026-02-12)

#### Scope
- Refactored terminal integration in `lpg-ehl-service` to follow `OPEN TERMINAL -> PURCHASE` flow for station owner use.
- Aligned client payloads with `openapi-payment-terminal.yaml` and captured WireMock responses.
- Relocated WireMock mappings to `lpg-ehl-service/src/test/resources/wiremock` and added integration tests using the standalone WireMock JAR.

#### Key changes
- `TerminalClient` updated to `openTerminal()` and `purchase()` API with new request/response models.
- `SimulatedTerminalClient` now calls `/v1/terminal/open` and `/v1/payments/purchase`, using PascalCase request fields and tolerant response parsing (camelCase/PascalCase).
- `PumpPaymentOrchestrator` updated to perform `openTerminal` then `purchase` and start fueling on approval.
- Reservation/capture flow removed from `TerminalEventHandler` and `TerminalPumpCompletionListener` (now no-op for capture in open->purchase mode).
- `PaymentTerminalDiagnosticsService` now posts `{}` to open/close to satisfy Content-Length requirement.

#### WireMock integration
- WireMock mappings moved to `lpg-ehl-service/src/test/resources/wiremock`.
- Integration tests start the standalone WireMock JAR on port `18080` and use `/__admin` endpoints to register stubs and verify requests.

#### Tests
Run in module `lpg-ehl-service`:
```
mvn -pl lpg-ehl-service test -Dtest=no.cloudberries.lpg.service.terminal.TerminalClientWireMockTest
```

Results:
- `TerminalClientWireMockTest::openTerminalThenPurchaseUsesCorrectFlow` ✅
- `TerminalClientWireMockTest::purchaseReturnsTerminalNotReadyWhenStubbed` ✅

#### Notes
- Standalone WireMock JAR used: `wiremock-standalone-3.3.1.jar` (repo root).
- WireMock root dir: `lpg-ehl-service/src/test/resources/wiremock`.