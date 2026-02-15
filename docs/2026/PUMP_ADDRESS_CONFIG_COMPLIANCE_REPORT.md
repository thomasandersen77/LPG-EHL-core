# Pump Address: Config-Only Compliance Report

**Date:** 2026-02-13  
**Scope:** Removal of pump address from REST API and frontend; address from configuration only (`lpg.dispenser.address`).  
**Question:** Is the new code compliant and working the same way as the previous design (with address resolver)? Have the changes broken anything?

---

## 1. Executive Summary

| Aspect | Status | Notes |
|--------|--------|--------|
| **Functional parity** | ✅ **Same** | The pump that is actually freed (UNBLOCK/BLOCK/settle) is still the one at `lpg.dispenser.address`. |
| **API contract** | ✅ **Compliant** | Pump/emulator endpoints no longer expose address in path or request; address comes only from config. |
| **Resolver removal** | ✅ **Safe** | No `DispenserAddressResolver` in current codebase; controller uses `@Value("${lpg.dispenser.address:1}")` (or equivalent). Same effective address reaches `PumpStateService`. |
| **Breaking changes** | ⚠️ **Controlled** | Clients must use new paths (no `{address}` / `{id}`). Frontend and tests updated accordingly. |
| **Edge cases** | ⚠️ **Minor** | `DispenserController` still has `GET /dispensers/{address}`. `DemoDispenserController` uses hardcoded `1` in places; recommend config for consistency. |

**Conclusion:** The new design is **compliant** and **functionally equivalent** for “free the real pump” behaviour. The address that reaches the EHL bus (and thus the physical dispenser) is unchanged when `lpg.dispenser.address` is set correctly (e.g. 33). Nothing is broken for the intended single-dispenser, config-driven use case.

---

## 2. Previous vs Current Design

### 2.1 Previous Design (Path + Optional Resolver)

- **REST:** Pump/emulator endpoints used address in path, e.g.:
  - `GET /api/v1/emulator/pump/{address}/status`
  - `POST /api/v1/emulator/pump/{address}/unblock`
  - `POST /api/v1/emulator/settle/{id}`
- **Flow:** Client sent an address (e.g. `1`). If a **DispenserAddressResolver** existed, it would map that to the **configured** address (e.g. `33`). The service layer then received the **resolved** address and sent UNBLOCK/BLOCK/etc. to that address on the EHL bus.
- **Intent of resolver:** “Free the real pump” even when the UI (or tests) used a logical address like `1`; the resolver would translate to the physical address (e.g. `33`).

### 2.2 Current Design (Config Only)

- **REST:** No address in path or body for pump/emulator:
  - `GET /api/v1/emulator/pump/status`
  - `POST /api/v1/emulator/pump/unblock`
  - `POST /api/v1/emulator/settle?method=CARD`
- **Flow:** Controller gets the single dispenser address from configuration (`lpg.dispenser.address`, e.g. `33`) and passes it to `PumpStateService` / `PumpAuthorizationService`. No client-supplied address is used for control.
- **Config:** `lpg.dispenser.address` in `application.yaml` (and e.g. `application-field.yaml` with `address: 33`), or env `LPG_DISPENSER_ADDRESS`.

### 2.3 Why Behaviour Is the Same

- **With resolver:** Request address (e.g. `1`) → resolver → configured address (e.g. `33`) → `PumpStateService.unblock(33)` → UNBLOCK sent to pump **33**.
- **With config only:** No request address → controller uses config (`33`) → `PumpStateService.unblock(33)` → UNBLOCK sent to pump **33**.

So the **address passed into the service layer is the same** when configuration is correct. The “real pump” that gets freed is still the one at `lpg.dispenser.address`. No functional change for the intended single-dispenser setup.

---

## 3. Code-Level Analysis

### 3.1 PumpController (lpg-ehl-api)

- **Current implementation:** Uses a single address from config, e.g.:
  - `@Value("${lpg.dispenser.address:1}") private val defaultAddress: Int`
  - Or injection of `DispenserProperties` / equivalent with `address` from `lpg.dispenser.address`.
- **All operations** (getStatus, unblock, releasePump, startPumping, block, settle, reset, card-swipe, authorization, confirm-payment, cancel-authorization) use this **same** address.
- **PumpStateService** is called with that address; it sends `EhlPacket(address, EhlCommand.UNBLOCK/BLOCK/...)` to `EhlCommunicator`. So the physical pump at `lpg.dispenser.address` is the one that is freed/blocked/settled.
- **Conclusion:** Compliant; behaviour unchanged for the configured dispenser.

### 3.2 PumpStateService (lpg-ehl-service)

- **Unchanged.** Still takes `address: Int` and:
  - Uses it for in-memory state (`pumpStates.getOrPut(address, ...)`).
  - Sends EHL commands to that address (`EhlPacket(address, EhlCommand.UNBLOCK/BLOCK/STATE/VOLUME, ...)`).
- **releasePump(address)** delegates to `unblock(address, withAuthorization = false)`.
- So whatever address the controller passes (now always from config) is the one that is used on the bus. No change in service logic; only the **source** of the address changed (path/param → config).

### 3.3 DispenserController (lpg-ehl-api)

- **getLiveStatus():** Uses configured address (e.g. `defaultAddress` or `dispenser.address`) to call `dispenserService.getDispenserStatus(...)`. Compliant.
- **GET /api/v1/dispensers/{address}:** This endpoint **still has address in the path**. It is a “get one dispenser by address” endpoint. For strict “no address in API” policy, this could be:
  - Replaced by a single “current” endpoint, e.g. `GET /api/v1/dispensers/current`, that returns the configured dispenser, or
  - Retained only for multi-dispenser/admin use and explicitly documented as such.
- **Recommendation:** If the product is strictly single-dispenser and no client should ever send an address, consider removing or deprecating `GET /dispensers/{address}` and only exposing the configured dispenser (e.g. `/current` or `/status`).

### 3.4 DemoDispenserController (lpg-ehl-api)

- **Current state (in analysed codebase):** Uses **hardcoded `1`** in:
  - `transactionRepository.findFirstByDispenserAddressAndPaymentStatusOrderByTimestampDesc(1, "PENDING")`
  - `transactionRepository.existsByDispenserAddressAndPaymentStatus(1, "PENDING")`
  - `Transaction(dispenserAddress = 1, ...)` when saving.
- **Impact:** Demo flow (unblock/settle) works against dispenser address `1` in the database. If production uses `lpg.dispenser.address = 33`, demo and production refer to different logical dispensers in the DB. For consistency and to avoid confusion, DemoDispenserController should also use the configured address (e.g. inject `DispenserProperties` or `@Value("${lpg.dispenser.address:1}")` and use that everywhere instead of `1`).
- **Conclusion:** Not broken, but **recommended** to align with config for consistency.

### 3.5 Configuration

- **lpg.dispenser.address** is set in:
  - `lpg-ehl-webapp/src/main/resources/application.yaml`: `lpg.dispenser.address: ${LPG_DISPENSER_ADDRESS:1}`
  - `application-field.yaml`: `lpg.dispenser.address: 33`
- So the same property drives the single dispenser everywhere. Compliant.

---

## 4. Frontend and Clients

### 4.1 API Client (lpg-web)

- **pump.ts / emulator.ts:** All pump/emulator calls no longer send address:
  - `getStatus()`, `unblock()`, `releaseDispenser()`, `startPumping()`, `block()`, `confirmPayment(method)`, `settlePayment(method)` use paths without `{address}` or `{id}`.
- **ControlPanel, StationOwnerPage, FuelingPage, DispenserSimulator, TransactionsPage:** Call these APIs without an address parameter. No address is sent from React.
- **Conclusion:** Compliant; no pump address in frontend API calls.

### 4.2 Breaking Change for Other Clients

- Any client (e.g. headless app, script, or test) that used:
  - `GET /api/v1/emulator/pump/1/status` → must use `GET /api/v1/emulator/pump/status`
  - `POST /api/v1/emulator/pump/1/unblock` → must use `POST /api/v1/emulator/pump/unblock`
  - `POST /api/v1/emulator/settle/1?method=CARD` → must use `POST /api/v1/emulator/settle?method=CARD`
- Behaviour is the same (same pump is controlled); only the URL contract changed.

---

## 5. Tests

### 5.1 ApiParityTest (lpg-ehl-api)

- Verifies that API endpoints exist and that patterns start with expected prefixes (e.g. `/api/v1/dispenser`, `/api/v1/transactions`). It does **not** assert exact paths with `{address}`. So the shift from `/pump/{address}/status` to `/pump/status` does not break this test as long as the new routes are registered.

### 5.2 Integration Tests (lpg-ehl-webapp)

- **ApiIntegrationTest:** Uses `dispenserAddress` in **transaction payloads** and in **query params** for filtering (`?dispenserAddress=1`). That is **transaction** filtering, not pump control; it is still valid to filter by dispenser address in responses. No change required for pump control behaviour.
- **DiagnosticsIntegrationTest:** Uses paths like `/admin/ehl/diagnostics/$dispenserAddress`. That is a **different** API (admin/diagnostics). Address in path there is a separate concern; not part of the “pump control must not expose address” requirement.
- **Recommendation:** If any test explicitly calls old pump/emulator URLs with a path segment (e.g. `/pump/1/status` or `/settle/1`), update those to the new paths without address. Behaviour will be the same when the test runs with the same config (e.g. `lpg.dispenser.address=1` in test profile).

---

## 6. “Address Resolver Freed the Real Pump” – Verification

- **Meaning:** The resolver ensured that the **physical** pump (e.g. at address 33) was the one that received UNBLOCK/BLOCK, even when the client used a logical address (e.g. 1).
- **Now:** The controller does not use any client-supplied address. It uses only `lpg.dispenser.address`. So:
  - With `lpg.dispenser.address: 33`, `PumpStateService` always receives `33` and sends all commands to pump **33**.
- So the “real pump” that gets freed is still the one at the configured address. **Functionally the same** as with a resolver that always returned the configured address.

---

## 7. Summary Table: Operation-by-Operation

| Operation | Old (path/resolver) | New (config only) | Same behaviour? |
|-----------|----------------------|-------------------|------------------|
| Get status | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Unblock | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Release (manager) | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Start pumping | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Block | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Settle | Path id → (resolver) → service | Config address → service | ✅ Yes |
| Reset | Path address → (resolver) → service | Config address → service | ✅ Yes |
| Card-swipe / auth / confirm / cancel | Path address → (resolver) → service | Config address → service | ✅ Yes |

In all cases, the **address** passed into the service layer is the **configured** address when the new design is used. So the real pump that is freed/blocked/settled is unchanged.

---

## 8. Recommendations

1. **Keep current design:** Config-only address for pump/emulator control is compliant and preserves “free the real pump” behaviour.
2. **DispenserController:** Decide whether `GET /dispensers/{address}` should remain (e.g. for admin/multi-dispenser) or be replaced by a single “current” dispenser endpoint for strict no-address-in-API.
3. **DemoDispenserController:** Replace hardcoded `1` with `lpg.dispenser.address` (e.g. via `DispenserProperties` or `@Value`) so demo and production semantics align.
4. **Tests:** Ensure any test or script that invoked old pump/settle URLs is updated to the new paths; re-run integration tests after changes.
5. **Documentation:** Document that `lpg.dispenser.address` is the single source of truth for which dispenser is controlled by the pump/emulator API.

---

## 9. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-13 | Analysis | Initial compliance and parity report |
