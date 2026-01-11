# Verification report: Python vs VB6 (`pumpcontrol`) — black-box equivalence

This report analyzes how the **Python code in this repository** implements (or does not implement) the functionality of the **legacy VB6 controller** (`norgesgass_legacy/pumpcontrol.vbp`), and whether the Python implementation would behave equivalently if we treat both systems as black boxes.

Companion docs:
- `docs/VB6_LEGACY_PUMPEKONTROLL_SPEC.md` (behavioral spec extracted from VB6)
- `docs/VB6_LEGACY_PUMPEKONTROLL_ARCHITECTURE.md` (Mermaid/UML diagrams)

---

## Executive conclusion (black box)

**No — the Python codebase, as it exists in this repo, is not behaviorally equivalent to the VB6 system as a black box.**

What *is* close:
- The Python “clone” under `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/` **faithfully implements the EHL serial framing + polling loop + a subset of the dispenser state machine**.

What is *not* close (major gaps vs VB6):
- **Payment terminal integration** (VB6 uses **BAXI ActiveX `baxi.dll`**, host-linked; Python has *experiments* for ECR protocols but not the same integration and not tied to dispenser workflow).
- **TCP control API on port 9002** (VB6 has it; Python clone does not).
- **SQL persistence and recordset-based side effects** (VB6 writes/reads multiple tables; Python clone does not touch DB).
- **Receipt printing and printer status logic** (VB6 has serial printer integration; Python clone does not).
- **RFID station-credit workflow + POS export (UNI)** (VB6 has it; Python clone does not).
- **Operational timers** (VB6 has rollback timeout behavior, periodic tasks like Z-report/settlement; Python clone does not).

Therefore, if you put the Python code in place of VB6 at a station and treat both as black boxes, **you will not get equal behavior** except for a narrow slice: “send/poll EHL frames and observe state/volume.”

---

## Methodology

I treated the VB6 code as the reference implementation (spec’d in the companion docs) and compared it to the Python code present in this repo:

### Python sources reviewed (main)

**EHL / pumpcontrol clone (explicitly intended to mirror VB6 EHL logic)**
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/pumpekontroll_clone.py`
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/ehl/protocol.py`
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/ehl/stream_parser.py`
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/ehl/serial_client.py`
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/ehl/poller.py`
- `more_legacy/Gammenl kode Python/ehl_pumpekontroll_clone/ehl/model.py`

**Payment / ECR experiments (terminal protocol exploration)**
- `scripts/python/ecr-testing/ecr_server_v9_framed.py`
- `scripts/python/ecr-testing/ecr_server_v3_handshake.py`
- `scripts/python/ecr-testing/ecr_dialog_handler.py`
- `scripts/python/ecr-testing/ecr_protocol_tester.py`
- (and many other variants under `scripts/python/ecr-testing/`)

---

## What VB6 actually is (reference)

VB6 (`pumpcontrol`) is a **station controller** that composes several subsystems into one operational state machine:

- **EHL dispenser serial control** (RS-485 framing, polling, unblock/block, preset amount, set price, reset, etc.).
- **Payment terminal integration** via **BAXI (`baxi.dll`)**: preselect charge, reversal, cashback/return, Z-report/settlement, terminal status, display text.
- **Receipt printing** via serial (ESC/status queries + parsing of printer status bytes).
- **RFID station credit** with DB lookups and (optionally) POS/UNI integration, including export to POS order tables.
- **TCP control server** on port `9002` with commands like `<PRICE>`, `<TANK_DISP_UNBLOCK>`, `<BANK_CASHBACK>`, etc.
- **Status poller** on port `86` responding to `<GETAUTOGASPRICE>`.
- **Persistence in SQL Server** for transactions, reports, logs, tasks, etc.
- **Timers** driving polling + rollback timeout + background tasks.

The black-box “observable surface” of the VB6 system includes:
- Dispenser behavior (hardware side effects)
- Payment-terminal behavior (card/customer interactions)
- TCP protocol responses on port 9002
- Database side effects (for reporting/accounting)
- Printer output/status

---

## Python implementation inventory (what exists)

### A) `ehl_pumpekontroll_clone` (Python)

This folder explicitly states it is a “clone” of parts of VB6:

In `pumpekontroll_clone.py` it says:
- It implements *EHL framing + checksum* (STX/ETX/LEN/XOR),
- The *same polling loop* as VB6 `state_timer_Timer`,
- The *same interpretation* of STATE/VOLUME/PRICE/LINETEST/TANK as VB6 `MSComm1_OnComm`,
- Start sequence: *PRESTART + UNBLOCK*,
- Stop: *BLOCK*,
- Reset: *ZER/RESET*,

And also explicitly:
- **“Det inkluderer ikke DB, bank (baxi.dll) eller printerlogikk.”**

So this Python component aims to replicate the **EHL dispenser protocol subset**, not the full station controller.

### B) `scripts/python/ecr-testing` (Python)

This is a collection of iterative experiments around **ECR/payment-terminal network protocols**, often:
- Listening on `PORT = 8009`,
- Speaking **length-prefixed framed messages** (2-byte length header),
- Emitting commands like `[10;1;100;0;0]` (Verifone-style bracket protocol), or `P;10;...` (Nets/Viking/Ingenico-style),
- Handling “heartbeats” (`0x0000`) and dialog messages (`D!`).

These scripts are **not integrated with the EHL dispenser flow** nor do they replicate VB6’s BAXI ActiveX integration semantics (callbacks like `baxi_OnLocalMode`, `TransferAmount_V2`, or the host link configured in VB6).

---

## Feature-by-feature equivalence matrix

Legend:
- ✅ = implemented and intended to match VB6 behavior
- 🟡 = partial / similar concept but not equivalent
- ❌ = missing

| VB6 surface / subsystem | Python `ehl_pumpekontroll_clone` | Python `ecr-testing` scripts | Notes |
|---|---:|---:|---|
| EHL serial frame codec (STX/LEN/XOR/ETX) | ✅ | ❌ | Clone implements `STX_CONTROLLER=0x10`, `STX_DISPENSER=0x20`, `ETX=0x36` + XOR. |
| MSComm “byte-by-byte” receive model | ✅ | ❌ | Clone reads 1 byte at a time; stream parser mimics VB6’s “parse only on ETX” logic. |
| Polling loop (`state_timer_Timer`) | ✅ | ❌ | Clone polls STATE, TANK, conditional VOLUME, periodic LINETEST. |
| Dispenser commands: UNBLOCK/BLOCK/RESET/PRESTART | ✅ | ❌ | Clone has `prestart()`, `unblock()`, `block()`, `reset_zer()`. |
| Dispenser commands: set price, set preset amount, error query | ❌ | ❌ | Clone does not implement VB6 `disp_setprice` or `set_preset_amount`; no error poll. |
| Container controller (secondary address) | ❌ | ❌ | VB6 polls container if configured; clone only supports a single addr. |
| VB6 “tank end” detection | 🟡 | ❌ | Clone ends when `vol stable AND trans_unaccounted=True`; VB6 also has other conditions (e.g., UI flag `Check2`), DB statuses, and more rollback behavior. |
| Bank payment integration (BAXI ActiveX) | ❌ | ❌/🟡 | ECR scripts explore other protocols; not BAXI ActiveX + host configuration + callbacks. |
| Rollback semantics (timeout=120s, reversal/cashback decisions) | ❌ | ❌ | Clone does not implement bank timeout logic or reversals; ECR scripts are standalone. |
| TCP control server (port 9002) & messages | ❌ | ❌ | Clone is CLI-only; no `<PRICE>`, `<TANK_DISP_UNBLOCK>`, etc. |
| Status poller (port 86, `<GETAUTOGASPRICE>`) | ❌ | ❌ | Not present in clone. |
| Printer serial integration + status parsing | ❌ | ❌ | Not present. |
| RFID station credit authorization | ❌ | ❌ | Not present. |
| POS/UNI export (orders/order lines) | ❌ | ❌ | Not present. |
| SQL persistence/logging/reports | ❌ | ❌ | Clone keeps in-memory state only. |
| Z-report/settlement tasks + emailing | ❌ | ❌ | Not present. |

**Summary:** the Python code covers **a subset of EHL dispenser I/O**, and not the station controller’s operational surface.

---

## Detailed comparison: EHL dispenser subsystem

### What matches well

**Framing and checksum**
- Python `ehl/protocol.py` encodes/decodes `STX LEN ADDR CMD DATA... CHK ETX` with XOR checksum and validates length + checksum — matching VB6’s `MSComm1_OnComm` rules.

**Stream parsing model**
- Python `ehl/stream_parser.py` only attempts to parse when it sees `ETX=0x36`, and validates `STX=0x20` and `LEN == actual length` before checksum validation, then clears buffer — same high-level idea as VB6.

**Polling order**
- Python `ehl/poller.py` follows VB6’s periodic sequence:
  - STATE (0x4B)
  - TANK (0xC5)
  - conditional VOLUME (0x45)
  - LINETEST (0x6A) every 10th tick

**Key state extraction**
- Python `ehl/model.py` extracts the same three state bits that VB6 uses (`automode`, `startbuttonpressed`, `openfordelivery`) and the two tank bits (`trans_unaccounted`, `trans_finished_powerfault`).

### Important differences (behavioral)

**1) “Tank end” determination differs**
- VB6 ends tanking when volume is stable under certain conditions **and then performs a complex finalize path** (DB writes, receipt print, bank cashback/reversal decisions, resetting many flags).
- Python clone ends tanking only on:
  - `tank_vol_last == tank_vol AND trans_unaccounted == True`
  - and then sends a `RESET/ZER (0x81)` immediately.

This means **black-box end-of-transaction behavior will differ**, even if the dispenser protocol layer is correct.

**2) Missing EHL commands that VB6 uses in production**
VB6 uses additional commands at runtime that the clone does not implement, notably:
- set price programming (VB6 `disp_setprice(...)`)
- program preset amount (VB6 `set_preset_amount(...)`, critical for card preselect)
- explicit error query + error mapping (`0x4C` + mapping in VB6)
- “check for pending transactions” and reset sequences beyond the simplistic flow

**3) Container polling is absent**
VB6 can poll a container controller (`rs485adrcontainer`) as a second address; clone has no equivalent.

**4) Timing/flow control**
VB6 uses a global `rts` gate and many `Sleep(wait_ms)`/`DoEvents` patterns. Python implements an `Event`-based `rts` gate and sleeps in `send()`, but the **overall concurrency/latency profile** is different (threaded reader, independent poll thread), which may matter with marginal serial timing.

---

## Detailed comparison: Payment subsystem

### VB6 payment: BAXI ActiveX + host-linked behavior

VB6 uses `baxi.dll` (ActiveX control `BAXILibCtl.BaxiCtrl`) with:
- `.CommPort` + `.BaudRate` (serial)
- `.HostIpAddress = "91.102.24.142"` and `.HostPort = "9670"`
- async events:
  - `baxi_OnLocalMode` drives state transitions
  - `baxi_OnPrinterText` builds receipt text
  - `baxi_OnDisplayText` sends messages to TCP clients
- methods:
  - `TransferAmount_V2` for purchase and cashback
  - `Administration` for reversal, settlement, Z-report, etc.

### Python payment: ECR protocol experiments (not equivalent)

The Python `scripts/python/ecr-testing/` code:
- listens on a TCP port (usually 8009)
- sends/receives framed messages with 2-byte length prefix
- uses either “bracket protocol” (`[10;..]`) or `P;..` formats
- handles terminal “heartbeats” and dialog messages

Even where a file uses “baxi” in a helper name (e.g. `create_baxi_packet`), it’s implementing:
- STX=0x02 / ETX=0x03 / LRC — which is **not the VB6 EHL protocol** and does not correspond to VB6’s BAXI ActiveX object model.

**Most importantly:** none of the Python payment scripts are integrated with the dispenser state machine (preselect → program preset amount → unblock → tank → cashback/reversal) that VB6 implements.

**Black-box verdict for payment:** not equivalent.

---

## TCP control protocol (port 9002): missing in Python

VB6 exposes an operator/client control surface over TCP port `9002`:
- price updates
- dispenser unblock/block
- manual bank actions
- station state toggles
- forwarding of display/printer state

The Python clone exposes only an interactive CLI (`stdin`) and provides **no network API**. So any system integrating via the VB6 TCP protocol would break.

**Black-box verdict for TCP control:** not equivalent.

---

## Persistence, reporting, and operational tasks: missing in Python

VB6 persists and operationalizes the station:
- writes `tankinger`, `rapporter_bankterminal`, `stasjonskreditt`, `cashback`, etc.
- runs periodic tasks to export station-credit to POS orders
- triggers Z-report and emails
- maintains event logs (`LogEvent`)

Python clone does not write DB, does not generate receipts, and does not export to POS.

**Black-box verdict for accounting/reporting:** not equivalent.

---

## Overall completeness assessment

### If the goal is “clone VB6 as a station controller”

**Completeness: low (≈ 10–20%)** — because the bulk of VB6’s business-critical behavior is outside the dispenser protocol codec.

### If the goal is “extract and validate the EHL serial protocol + polling”

**Completeness: medium-to-high (≈ 60–80%)** for that *subsystem*, because:
- framing, parsing, polling order, and bit decoding are implemented intentionally to match VB6,
- but important production commands and edge behaviors still appear missing (set price, preset amount programming, error querying, container support, full end-of-transaction logic).

---

## Recommendation: how to get black-box equivalence

If you want “swap VB6 for Python without changing external systems,” Python must implement at least:
- **TCP server on port 9002** with the same message grammar and responses
- **BAXI-equivalent payment integration** *or* a replacement that still exposes identical semantics and timing (preselect, onLocalMode-like state transitions, receipts, reversal/cashback)
- **SQL persistence** mirroring the required side effects (or a compatibility layer)
- **RFID station credit** authorization and POS export job
- **Printer driver** and printer-state reporting
- **Timeout/rollback** behavior (120s) and recovery semantics

Without these, you can still use the Python code as a **hardware-protocol test harness** for EHL, but not as a station controller replacement.


