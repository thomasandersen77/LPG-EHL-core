# Verification report (follow-up): Kotlin/Java vs VB6 (`pumpcontrol`) — latest code snapshot

This is an **updated follow-up** to `verification/VB6_vs_Kotlin_blackbox_verification.md`, re-checking the **current Kotlin/Java code in this repo** for VB6 (`norgesgass_legacy/pumpcontrol.vbp`) **black-box equivalence**.

Constraints: **code-only** (no narrative docs as evidence).

Date: 2026-01-11

---

## Executive conclusion (black box, latest code)

**Still partial.** The Kotlin/Java codebase continues to provide strong “building blocks” (EHL codec + serial stack + payment protocol pieces), but it does **not** demonstrate a single, cohesive replacement that reproduces the full **VB6 external surfaces**, especially the legacy TCP control plane.

Key points (updated/confirmed):
- ✅ **EHL protocol + serial I/O** remain clearly implemented in Kotlin modules (`lpg-ehl-core`, `lpg-ehl-for-ai`).
- 🟡 **Legacy text-tag TCP support exists only in the emulator**, and can be configured to listen on **any port (including 9002)** via `emulator.port`.
- ❌ There is still **no evidence** of a Kotlin “station controller” exposing the **full VB6 TCP 9002 command set** (`<PRICE>`, `<BANK_CASHBACK>`, `<STATIONSTATE>`, etc.).
- ❌ There is still **no evidence** of the **port 86** poller responding to `<GETAUTOGASPRICE>`.

---

## What changed vs the previous report

From re-scanning the current repo state:

- **No new Kotlin implementation of VB6’s 9002 command grammar** was found (searches for `9002`, `<PRICE>`, `<BANK_CASHBACK>`, `<STATIONSTATE>`, `<GETAUTOGASPRICE>` in Kotlin source returned none).
- **Clarification (important nuance)**: `lpg-ehl-emulator`’s `EmulatorService` is a real TCP server (via `ServerSocket`) and is configurable with `@Value("\${emulator.port:9000}")`. If you run it with `emulator.port=9002`, a legacy client could connect to port 9002 — but it would be talking to the **emulator’s subset protocol**, not a full VB6 controller.
- `lpg-ehl-api` contains a different `EmulatorService` class (`no.cloudberries.lpg.emulator.EmulatorService`) but it is **not a TCP server**; it’s an in-memory scenario/config service behind `@Profile("local","dev")`.

Net: the prior report’s core conclusion remains accurate, but the “port 9002” topic should be read as:
> **No evidence of a full VB6 port-9002 tag-compatible controller server**; only an emulator that can optionally be bound to that port and implements a limited subset.

---

## Updated feature-by-feature equivalence matrix (latest code)

Legend:
- ✅ = implemented and intended to match VB6’s observable surface
- 🟡 = partial / similar capability but not black-box equivalent
- ❌ = missing (in Kotlin/Java code)

| VB6 surface / subsystem | Kotlin/Java status | Where in code | Notes |
|---|---:|---|---|
| EHL protocol codec (STX/LEN/XOR/ETX) | ✅ | `lpg-ehl-core/.../protocol/EhlCodec.kt`, `lpg-ehl-for-ai/.../protocol/EhlCodec.kt` | Codec/packet model exists and is test-covered. |
| Real serial I/O to RS-485 adapter | ✅ | `lpg-ehl-core/.../communication/SerialPortManager.kt`, `lpg-ehl-for-ai/.../communication/SerialPortManager.kt` | Uses `jSerialComm` (core) and real IO adapters. |
| Higher-level command API (STATE/UNBLOCK/BLOCK/PROG_PRC/PROG_AMOUNT) | ✅ | `.../communication/DispenserConnection.kt` | Matches VB6-critical commands. |
| VB6-style periodic polling loop + legacy emissions | 🟡 | present as building blocks | Kotlin has the components, but VB6 “timers + side effects” aren’t shown as one controller. |
| VB6 bank integration via `baxi.dll` COM callbacks | ❌ | — | Kotlin cannot embed VB6 COM control. Integration is different. |
| Payment protocol framing/commands (BAX/BAX-like) | ✅/🟡 | `.../payment/NetsBaxProtocol.kt` | Command construction/parsing exists; equivalence depends on orchestration. |
| Payment transport to terminal host/cloud | 🟡 | `.../payment/CloudTerminalClient.kt` (+ API client analog) | Transport differs from VB6 config/host and callback-driven behavior. |
| VB6 TCP control server on port 9002 (full tag grammar) | ❌/🟡 | Emulator only: `lpg-ehl-emulator/.../EmulatorService.kt` | Emulator can bind to 9002 via config, but does **not** implement VB6’s full `<PRICE>/<BANK_CASHBACK>/<STATIONSTATE>` surface. |
| Status poller port 86 (`<GETAUTOGASPRICE>`) | ❌ | — | No Kotlin code found implementing this listener/surface. |
| Receipt printer integration (serial) | ❌ | — | Not present as a hardware adapter equivalent to VB6 printing behavior. |
| RFID station credit via COM + DB lookup | ❌ | — | Not present as COM/RFID; Kotlin credit appears to be API/HTTP-oriented. |
| Persistence of transactions/reports/logs comparable to VB6 tables | 🟡 | `lpg-ehl-api` services + emulator persistence sink | Kotlin persists transactions, but not proven equivalent schema/side effects. |

---

## Concrete evidence: what the emulator actually supports (latest code)

In `lpg-ehl-emulator`:
- It accepts **legacy text commands** when the received payload starts with `<`.
- It explicitly handles:
  - `TANK_DISP_UNBLOCK` → translates to EHL `UNBLOCK`, starts simulation, emits `<STATE_TANK>` and streaming `<TANK>`.
  - `TANK_DISP_STOP` → translates to EHL `BLOCK`, stops simulation, emits final `<TANK>` and `<STATE_TANK>`.
- It broadcasts reset signals including:
  - `<RESTART>;00000000;<SLUTT>`
  - `<TANK>;0;0.00;0.00;<price>;...`
  - `<STATE_TANK>;00000000`
  - `<TANK_TERMINAL_MESSAGE>;...;<SLUTT>`

Notably absent (in emulator code inspected):
- `<PRICE>` handler
- `<BANK_CASHBACK>` handler
- `<STATIONSTATE>` handler
- `<GETAUTOGASPRICE>` on port 86

---

## Updated black-box implications (latest code)

If you “replace VB6 with Kotlin”:

- **Legacy Dispenserklient compatibility**:
  - **Works only if you run the emulator and bind it to the expected port** (e.g. `emulator.port=9002`), and only for the subset of commands it implements.
  - It will **not** behave like VB6 for the broader 9002 control surface because those tags are not implemented in Kotlin as a controller.

- **Modernized operation**:
  - Kotlin can be perfectly viable as a **new black box** (REST + new UI + modern integration), but that’s not “drop-in legacy compatibility”.

---

## Next steps (if you want full legacy compatibility)

To reach VB6-level black-box equivalence, the Kotlin side still needs an explicit **compatibility-layer service** that:
- Implements the **full** VB6 TCP 9002 protocol (all tags VB6 supports, exact request/response grammar and timing expectations).
- Implements the **port 86** price poller (`<GETAUTOGASPRICE>`).
- Bridges those commands into the actual Kotlin dispenser/payment orchestration (not into the emulator simulation).

---

## Appendix: key evidence pointers (latest code)

- Legacy tag TCP server (emulator): `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`
- Emulator config-only service (API, local/dev): `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`
- EHL comms: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/*`
- EHL protocol: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/*`
- Payment protocol + transport: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/*`


