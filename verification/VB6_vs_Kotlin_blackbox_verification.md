# Verification report: Kotlin/Java vs VB6 (`pumpcontrol`) — black-box equivalence

This report analyzes how the **Kotlin/Java implementation in this repository** replicates the behavior of the **legacy VB6 station controller** (`norgesgass_legacy/pumpcontrol.vbp`) and whether it would behave equivalently if treated as a black box.

Constraints: **code-only**. I did not rely on the repo’s narrative documentation for claims; only what the Kotlin/Java source code demonstrates.

Companion docs:
- `docs/VB6_LEGACY_PUMPEKONTROLL_SPEC.md` (VB6 behavioral spec extracted from code)
- `verification/VB6_vs_Python_blackbox_verification.md` (same analysis style for Python)

---

## Executive conclusion (black box)

**Partially — the Kotlin/Java codebase contains production-grade building blocks for several VB6 subsystems (EHL protocol, serial comm, “BAX/BAX-like” payment protocol), but the full VB6 black-box surface is not reproduced end-to-end in one runnable replacement.**

More specifically:
- ✅ **EHL RS-485 protocol framing/codec and command set** appear implemented in `lpg-ehl-core` (with real serial I/O via `jSerialComm` and higher-level command orchestration via `DispenserConnection`).
- ✅ A **payment-terminal protocol implementation** exists (`NetsBaxProtocol`), plus a real **TLS “cloud connect” client** (`CloudTerminalClient`) speaking BAX frames over SSL/TLS (distinct from VB6’s `baxi.dll` ActiveX integration).
- 🟡 The repo includes a **“legacy text tag” TCP server** in the emulator that looks intentionally compatible with the old VB6 client tags like `<TANK_DISP_UNBLOCK>`/`<TANK_DISP_STOP>`/`<TANK>`/`<STATE_TANK>`/`<RESTART>` — but it is an **emulator** of the dispenser side, not a full station controller replacement.
- ❌ The exact VB6 TCP control server behavior on port **9002** (full command set: `<PRICE>`, `<BANK_CASHBACK>`, `<STATIONSTATE>`, `<GETAUTOGASPRICE>`, etc.) is **not implemented as-is** in the Kotlin “controller” code.
- ❌ VB6’s serial **receipt printer integration**, **RFID station credit over COM**, and **MAPI email** behaviors are not present in the Kotlin runtime code I inspected.

So: **as a black box**, a Kotlin deployment will not look like VB6 to existing integrations unless you run a compatibility layer (and in this repo, only the emulator shows partial compatibility).

---

## Methodology

I treated VB6 as the reference and inspected Kotlin/Java code for the same observable surfaces:

- **Dispenser control**: serial protocol framing, poll loop, commands, state machine, recovery.
- **Payment**: terminal protocol, preauth/capture/refund/reversal semantics, timeouts.
- **External APIs**: “legacy TCP tags” (port 9002 + port 86), REST APIs, etc.
- **Persistence**: transaction recording and status updates.
- **Peripheral I/O**: printer, RFID, pinpad.

---

## Kotlin/Java modules reviewed (main)

### `lpg-ehl-core` (Kotlin): protocol + hardware adapters + payment protocol

Evidence:
- Serial library dependency: `com.fazecast:jSerialComm` in `lpg-ehl-core/pom.xml`.
- Serial manager: `no.cloudberries.lpg.communication.SerialPortManager`
- Protocol codec: `no.cloudberries.lpg.protocol.EhlCodec`, `EhlPacket`, `EhlCommands`
- Command orchestration: `no.cloudberries.lpg.communication.DispenserConnection`
- Payment protocol: `no.cloudberries.lpg.payment.NetsBaxProtocol`
- Cloud payment client: `no.cloudberries.lpg.payment.CloudTerminalClient`

### `lpg-ehl-api` (Kotlin/Spring): REST services + persistence + (some) pump orchestration

Evidence:
- `FuelPumpService`, `DispenserService`, `EhlPacketProcessor`
- `TransactionService` persists transactions and updates payment status
- `SimulatedPaymentGateway` exists for local/dev profiles
- Credit subsystem exists via HTTP (`CreditController`) but not RFID/COM

### `lpg-ehl-emulator` (Kotlin/Spring): dispenser emulator + legacy tag bridge

Evidence:
- `EmulatorService` listens on `emulator.port` (default 9000) and accepts:
  - binary EHL frames
  - legacy text commands starting with `<...>` and responds with `<TANK>`, `<STATE_TANK>`, `<RESTART>`, `<TANK_TERMINAL_MESSAGE>`

---

## Feature-by-feature equivalence matrix

Legend:
- ✅ = implemented and intended to match the VB6 surface
- 🟡 = partial / similar capability but not black-box equivalent
- ❌ = missing (in Kotlin/Java code)

| VB6 surface / subsystem | Kotlin/Java status | Where in code | Notes |
|---|---:|---|---|
| EHL protocol codec (STX/LEN/XOR/ETX) | ✅ | `lpg-ehl-core/.../protocol/EhlCodec.kt` (plus tests) | Core appears designed to be the canonical EHL implementation. |
| Real serial I/O to RS-485 adapter | ✅ | `lpg-ehl-core/.../communication/SerialPortManager.kt` | Uses `jSerialComm`, includes watchdog/reconnect logic. |
| Higher-level command API (STATE/UNBLOCK/BLOCK/PROG_PRC/PROG_AMOUNT) | ✅ | `lpg-ehl-core/.../communication/DispenserConnection.kt` | Has `unblock()`, `block()`, `programPrice()`, `programValue()`. |
| VB6-style periodic polling loop (`state_timer_Timer`) | 🟡 | Not seen as a single loop in core; API/emulator do state handling | Core has building blocks; VB6 behavior is timer-driven and also emits TCP messages. |
| VB6 bank integration: `baxi.dll` object model (TransferAmount_V2 + OnLocalMode callbacks) | ❌ | — | Kotlin does not embed `baxi.dll` (Windows COM). Different integration style. |
| Payment protocol semantics (purchase/preauth/refund/cancel) | ✅/🟡 | `lpg-ehl-core/.../payment/NetsBaxProtocol.kt` | Supports TCP length framing and serial STX/ETX/LRC. Semantics may match “BAX”, but not the same event-driven VB6 control surface. |
| Payment transport to terminal host/cloud | 🟡 | `CloudTerminalClient` (TLS to `3.33.230.243:6001`) | VB6 config uses `91.102.24.142:9670` via `baxi.dll`. Kotlin uses Nets Cloud Connect model. |
| VB6 TCP control server on port 9002 (exact tag grammar) | ❌/🟡 | Emulator has partial legacy tags | No evidence of a Kotlin controller exposing the full `<PRICE>`, `<BANK_CASHBACK>`, `<STATIONSTATE>`, `<GETAUTOGASPRICE>` surface. |
| Status poller port 86 (`<GETAUTOGASPRICE>`) | ❌ | — | I did not find a Kotlin implementation of this port/surface. |
| Receipt printer integration (serial + ESC/status parsing) | ❌ | — | Not present as a hardware adapter like VB6 `com_print_OnComm`. |
| Pinpad COM integration (4 preset buttons) | ❌ | — | Not found in Kotlin. |
| RFID station credit via COM + DB lookups | ❌ | — | Kotlin credit is HTTP-based (`CreditController`), not RFID/COM. |
| POS/UNI export job (orders/orderlines) | ❌ | — | Not found in Kotlin code inspected. |
| Persistence of transactions/reports/logs comparable to VB6 tables | 🟡 | `lpg-ehl-api` persistence | Kotlin persists transactions (and has reporting endpoints), but it’s not the same schema/same side effects as VB6 recordsets. |
| VB6 “timeout rollback” (120s after unblock) | 🟡 | Some timeouts exist (protocol default=120s; socket timeouts) | Exact rollback semantics (reverse/cashback + block) not shown as a unified workflow. |

---

## Detailed comparison by subsystem

### 1) EHL / dispenser control (closest match)

**What Kotlin clearly has**
- A real serial stack with reconnect/watchdog (`SerialPortManager`) and a clean command queue abstraction (`DispenserConnection`).
- A typed EHL command enum and packet model (`EhlCommand`, `EhlPacket`).
- Explicit support for VB6-critical commands:
  - `UNBLOCK (0x77)`
  - `BLOCK (0x69)`
  - price programming (`PROG_PRC`)
  - amount programming (`PROG_AMOUNT`)

**Where equivalence can still break (black box)**
- VB6 behavior is driven by timers + UI state, and it emits legacy TCP tags and printer output as part of the experience. Kotlin core is more “library-like”; equivalence depends on an orchestration layer that ties these together.

### 2) Payment subsystem (capability exists, but integration differs)

**What Kotlin clearly has**
- `NetsBaxProtocol` supports:
  - TCP length-prefixed framing (2-byte header)
  - serial STX/ETX/LRC framing
  - purchase/preauth/refund/cancel/status commands
- `CloudTerminalClient` establishes TLS to a Nets Cloud Connect endpoint (`3.33.230.243:6001`) and reads framed responses.

**Key non-equivalences vs VB6**
- VB6 integrates via **Windows COM ActiveX** (`baxi.dll`) and reacts to **callbacks** like `baxi_OnLocalMode`, `baxi_OnPrinterText`, `baxi_OnDisplayText`, driving dispenser authorization decisions and receipt creation.
- Kotlin payment code is request/response oriented; it does not naturally replicate the VB6 event stream (e.g., forwarding terminal display lines as `<TANK_TERMINAL_MESSAGE>`).

**Black-box implication**
- Even if the terminal ultimately authorizes payments correctly, the station’s externally visible behavior (messages, timeouts, rollback sequencing) will differ unless explicitly implemented.

### 3) Legacy TCP tag protocol (Kotlin emulator is partial compatibility)

In `lpg-ehl-emulator/EmulatorService.kt`, the emulator accepts `<...>` strings and handles at least:
- `<TANK_DISP_UNBLOCK>` → translates to EHL `UNBLOCK` and starts simulation; responds with `<STATE_TANK>...` and streams `<TANK>` updates.
- `<TANK_DISP_STOP>` → translates to `BLOCK`, stops simulation, sends final `<TANK>` and `<STATE_TANK>`.
- On settlement it broadcasts:
  - `<RESTART>;00000000;<SLUTT>`
  - `<TANK>;0;0.00;0.00;price;...`
  - `<STATE_TANK>;00000000`
  - `<TANK_TERMINAL_MESSAGE>;...;<SLUTT>`

**What this means**
- Kotlin *does* contain code that speaks a VB6-like tag protocol, but it is implemented on the emulator side and does not appear to cover the full VB6 command set (`<PRICE>`, `<BANK_CASHBACK>`, `<STATIONSTATE>`, `<GETAUTOGASPRICE>`, etc.).

### 4) Persistence / reporting

Kotlin (API) persists transactions and can update payment status (`TransactionService.updatePaymentStatus`), but the data model is not VB6’s recordset approach (and VB6’s exact side effects like `rapporter_bankterminal`, printer receipts, and technical cashback bookkeeping are not shown here).

### 5) Printer, pinpad, RFID station credit

I did not find Kotlin implementations of:
- serial receipt printer status querying/parsing (VB6 `com_print_OnComm`)
- pinpad COM key mapping to preset amounts
- RFID COM reader handling + station credit DB lookup + POS export loop

Kotlin does have a station-credit *concept* via REST (`CreditController`), but that is not black-box equivalent to “tap RFID tag on COM port to authorize fueling.”

---

## Overall completeness assessment

### If the target is “replace VB6 as the station controller black box”

**Completeness: medium (≈ 40–60%)**, because:
- EHL protocol + hardware serial communication appears implemented and robust.
- Payment protocol support exists (BAX framing), but orchestration with dispenser and legacy outputs is not obviously present as a single cohesive replacement.
- Legacy TCP tag compatibility is partial and located in an emulator component.

### If the target is “modernized replacement (new black box)”

**Completeness: high**, in the sense that Kotlin provides modern building blocks (API, persistence, emulator, cloud payment integration) — but that’s a different question than “is it behaviorally identical to VB6.”

---

## Practical black-box verdicts

### Would a VB6 Dispenserklient (legacy TCP tag client) work unchanged?
- **Only against the Kotlin emulator**, and only for the subset of commands it implements.
- Against the Kotlin API/controller: **no evidence** of a port-9002 tag-compatible server.

### Would station operation (fueling + payment + reporting) look identical to VB6?
- **No**, because printer output, RFID, VB6 UI behaviors, and specific rollback/cashback semantics are not present as the same observable surfaces.

---

## Recommendations to reach VB6-level black-box equivalence

If “drop-in replacement” is the goal, Kotlin needs an explicit compatibility layer that:
- Implements the **exact TCP 9002 protocol** (all `<...>` commands and responses) and port 86 price poller, matching VB6 grammar.
- Bridges those legacy commands into `lpg-ehl-core`:
  - program preset amount → unblock → poll state/volume → tank end → reset
- Integrates payment as a **state machine** with explicit rollback semantics equivalent to VB6:
  - preselect/reserve
  - authorize fueling
  - end-of-tank: compute delta, perform refund/cashback/reversal, persist and emit messages
- Either implements printer output equivalence (at least logical receipts + status surfaces) or replaces it while preserving required side effects for downstream systems.
- Provides an RFID/station-credit adapter if that black-box surface is still required.

---

## Appendix: key evidence pointers (code-level)

- **Serial I/O**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/SerialPortManager.kt`
- **EHL command orchestration**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/DispenserConnection.kt`
- **Payment protocol**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/NetsBaxProtocol.kt`
- **Cloud payment transport**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/CloudTerminalClient.kt`
- **Legacy tag handling (emulator)**: `lpg-ehl-emulator/src/main/kotlin/no/cloudberries/lpg/emulator/EmulatorService.kt`
- **Transaction persistence API**: `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/service/TransactionService.kt`


