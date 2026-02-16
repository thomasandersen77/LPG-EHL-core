## Scope reviewed
- Python reference: `test-python/` (scripts + `python-server/server.py`) and `_field-test/PYTHON_KOTLIN_ALIGNMENT_ANALYSIS.md`
- Kotlin modules: `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-service`, `lpg-ehl-webapp`, `lpg-ehl-emulator`, `lpg-web`
- Simulators: `lpg-ehl-serialport-sim`, `lpg-ehl-payment-terminal-sim`

---

### 1) Core protocol alignment (Python vs Kotlin)

#### ✅ Strong alignment points
1. **Frame protocol (Norges Gass variant) is aligned**
    - Python: STX controller/dispenser = `0x10/0x20`, ETX = `0x36`, XOR checksum.
    - Kotlin core: `EhlProtocolConfig` default variant = `NORGES_GASS` (`0x10/0x20/0x36`), codec logic matches XOR framing.

2. **UNBLOCK verification strategy is aligned with Python field behavior**
    - Python `unblock_verified()`: send UNBLOCK, then verify with STATE polling (`open_for_delivery` bit).
    - Kotlin `PumpStateService.unblock()`: `withExclusive { drain(100); send(UNBLOCK); send(STATE)+receiveUntil(...) loop }` until `OPEN_FOR_DELIVERY (0x02)`.
    - This matches the critical field reality where dispenser may return STATE instead of OK.

3. **State bit semantics are aligned**
    - Python: `open_for_delivery=0x02`, `startbutton=0x04`, `automode=0x08`.
    - Kotlin `StatusBitMasks`: same values + error bit `0x80`.

4. **Transport robustness patterns exist in Kotlin**
    - `EhlCommunicator`: `withExclusive`, `drain`, `receiveUntil`, retry in `sendAndReceive`, parse recovery for noise.
    - Tests exist (`EhlCommunicatorReceiveUntilTest`) for interleaved VOLUME, concatenated frames, missing OK, delayed open-bit.

#### ⚠️ Important mismatch to resolve
5. **Serial parity mismatch (likely critical in real hardware mode)**
    - Python field kit configures Linux serial as **8N1** (`_set_raw_8n1`), and field scripts default to 9600 8N1.
    - Kotlin `lpg-transport/SerialPortConfig` defaults to **EVEN parity (8E1)**.
    - Kotlin `RealSerialPortAdapter` hardcodes EVEN parity as well.
    - This can break true Python↔Kotlin hardware parity if the dispenser/adapter actually expects 8N1 (as Python success suggests).

**Recommendation (high priority):**
- Make field parity explicit and environment-driven everywhere (no hardcoded EVEN defaults in adapter layer).
- Set production default parity to match validated field profile (if Python-opened pump used 8N1, default to NONE/8N1 for this station profile).

---

### 2) Evaluation of your "Gemini/ChatGPT refactoring suggestions" text

#### Overall verdict: **Largely well aligned with actual Python behavior and Kotlin needs**

Your critique is mostly correct and nuanced. Specific evaluation:

1. **"Stale data trap" concern** — ✅ valid
    - Kotlin has `drain()` and robust parser, but stale/echo data can still leak across command epochs.
    - Suggestion to add logical TX barriers/epochs is directionally good.

2. **Watchdog based on passive silence is wrong** — ✅ valid and already partially handled
    - `SerialPortManager.checkWatchdog()` is attempt-window based (silence alone is tolerated), which aligns with your point.
    - However `HardwareWatchdogService` logs `timeSinceLastData`, so policy docs should clearly emphasize attempts/failures as primary signal.

3. **Last-seen-state on timeout** — ✅ useful, not fully present in user-facing diagnostics
    - Would significantly improve field triage.

4. **Echo filter byte-for-byte absolute rule is risky** — ✅ your nuance is correct
    - Must be heuristic (timing + direction + context), not global hard drop.

5. **Timestamp nanoTime barrier limits** — ✅ correct nuance
    - Better to do logical command epoching than assume precise physical arrival ordering at packet-level callbacks.

6. **Global ignore of controller STX** — ✅ your caution is correct
    - Contextual filtering during expected response windows is preferable to global drop.

7. **PascalCase strategy** — ✅ your nuance is correct
    - Service side already maps with `@JsonProperty` in DTOs.
    - Simulator side also uses PascalCase model directly (`EventEnvelope` with PascalCase fields), so naming mismatch risk is low for that contract.

---

### 3) Simulator realism vs hardware behavior

## 3A) `lpg-ehl-serialport-sim` (pump/serial simulator)

#### ✅ Good current capabilities
- Field profile knobs already exist: drop response, concat frames, inter-character delay, optional no-ack-on-unblock/block, unsolicited volume bursts.
- EHL frame codec and binary mode implemented.

#### Gaps vs harsh real-world RS-485 behavior
- No explicit **local echo** injection knob (self-hearing TX).
- No explicit **stale previous transaction frame injection** (e.g., stale VOLUME right after UNBLOCK).
- No explicit **fragment response probability** abstraction (although chunking exists, but not tightly modeled per-response fault mode).
- No explicit **random noise burst before frame**.
- CRC/checksum corruption exists via `state.shouldCorruptChecksum()` path, but needs clearer config observability at startup and per-event logging consistency.

**Recommendation:** Implement your proposed Drammen-style knobs with controlled defaults and startup log dump of all active fault knobs.

## 3B) `lpg-ehl-payment-terminal-sim`

#### ✅ Strengths
- Structured Spring app with scenario management, state manager, event store, admin endpoints.
- PascalCase event envelope in simulator output model aligns with terminal contract style.

#### Gaps relative to proposed realistic terminal simulation
- `lpg-ehl-service` `SimulatedTerminalClient` is still thin HTTP wrapper (reserve/capture/reversal), not a full robust transaction-state simulator client contract in itself.
- Terminal sim state manager (`CLOSED/OPEN/READY/BUSY`) is infrastructure-level terminal state, not full per-transaction lifecycle (`WAITING_FOR_CARD`, `AUTHORIZING`, `APPROVED/DECLINED`, `SETTLED`, etc.).
- Missing explicit fault knobs for out-of-order/duplicate events, slow responses, stuck-in-waiting, offline probabilities in a unified model exposed to tests.
- Contract tests against `openapi-payment-terminal.yaml` are not evident in reviewed files.

**Recommendation:** Your prompt for terminal simulator upgrade is well justified and should be treated as a roadmap item.

---

### 4) Cross-module integration observations

1. **`lpg-ehl-webapp` is mostly orchestration/config wrapper**
    - Correctly wires field/lab transport profiles.

2. **Potential config conflict risk**
    - `TransportConfiguration` supports configurable parity string.
    - But `RealSerialPortAdapter` still hardcodes EVEN parity; if this bean path is used anywhere, it can silently diverge from validated Python behavior.

3. **Python open-pump success path is now mirrored in Kotlin service logic**
    - UNBLOCK verification via STATE open bit is in place.

---

### 5) Are you aligned with Python that opened dispenser pump?

## **Short answer: Yes—mostly aligned, with one critical caveat.**

You are aligned on the most important behavioral points:
- UNBLOCK success criteria via STATE open bit,
- robust handling of missing OK/interleaved frames,
- contextual critique of echo/timing assumptions,
- watchdog philosophy for low-traffic installations.

### Critical caveat to fix before claiming full hardware parity:
- **Ensure serial parity/line settings in Kotlin field runtime match the exact Python-tested hardware settings (likely 8N1).**
- Remove/avoid hardcoded 8E1 defaults in paths that may be used in production field mode.

With that addressed, your current direction is strongly consistent with the Python implementation that successfully opened the pump.

---

### 6) Concrete refactoring actions (prioritized)

## P0 (Do now)
1. **Unify serial settings source-of-truth**
    - Eliminate hardcoded EVEN parity in `RealSerialPortAdapter`.
    - Use shared config object everywhere.
    - Add startup log line: `serial profile resolved: baud=X dataBits=Y parity=Z stopBits=W`.

2. **Add field profile presets matching validated Python setup**
    - e.g., `field-profile=norgesgass-python-validated` => 9600/8N1 unless site requires otherwise.

## P1 (Next)
3. **Add command-epoch barrier metadata in transport/service logs**
    - Command id/epoch for send + receiveUntil filtering/diagnostics.

4. **Enhance timeout diagnostics**
    - Return/log last-seen STATE + raw bytes when verify loops timeout.

5. **Serial simulator fault knobs expansion**
    - localEchoProbability, staleInjectionProbability, staleStateProbability,
      fragmentResponseProbability, randomNoiseBurstProbability,
      explicit per-trigger logging.

## P2 (After P1)
6. **Terminal simulator realism uplift**
    - Per-transaction state machine and deterministic dirty-mode knobs.
    - Contract tests vs `openapi-payment-terminal.yaml` examples.

---

### 7) Final verdict

- The **main Kotlin pump-control path is now conceptually aligned** with the proven Python approach that opened the dispenser.
- Your proposed critique/refactoring directions are **substantially correct and pragmatic**.
- To reach practical field parity confidence, prioritize **serial parity/config unification** and then simulator dirty-mode enhancements.
  " > /Users/tandersen/git/NorgesGass/lpg-ehl/docs/PYTHON_KOTLIN_REFACTORING_ASSESSMENT_2026-02-12.md