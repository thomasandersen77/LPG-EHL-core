# RS-485 Pump/Dispenser Communication Guide (EHL / Norges Gass variant)

This repo controls LPG dispensers (“pump”) over **RS-485 half‑duplex** using a **binary EHL framing** (not Modbus).

This guide is the practical, implementation-aligned description of how we communicate on the wire.

## Scope and terminology

- **Pump / Dispenser**: the LPG dispenser controller on the RS‑485 bus.
- **Controller**: our edge app (webapp/headless) that talks to the dispenser.
- **EHL (Norges Gass variant)**: the byte-oriented protocol variant used in this repo by default.

Authoritative references in repo:
- `docs/_serieal_communication/serial_contract.md` (legacy-verified contract summary)
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/*` (codec + protocol config)
- `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt` (buffering + RX recovery)
- `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/SerialPortManager.kt` (real serial I/O + watchdog)
- `python-test/ehl_protocol.py` (minimal framing + parser used for probes)

## 1) Physical layer (RS‑485)

- **Medium**: RS‑485, 2‑wire differential (A/B), multi-drop bus.
- **Direction**: half‑duplex (only one talker at a time).
- **Termination**: 120Ω at both bus ends (field responsibility).
- **Ground reference**: connect signal ground/shield as per site practice; avoid ground loops.

### Bus discipline (practical)

RS‑485 is shared. In our system we behave as a **single master**:
- We send one request at a time and wait for the response.
- We avoid concurrent requests by design (see “Single‑flight request/response” below).

If you ever put multiple masters on the line, you must add arbitration/backoff; the current implementation assumes **one controller**.

## 2) UART / serial settings

### Production defaults (legacy contract)

The legacy VB6 system used:
- **9600 baud, 8E1** (8 data bits, Even parity, 1 stop bit)
- No flow control

This is reflected in `docs/_serieal_communication/serial_contract.md` and our Java config defaults.

### Linux device paths

Common:
- Onboard UART: `/dev/ttyS0`, `/dev/ttyS1`
- USB RS‑485 adapters: `/dev/ttyUSB0`, `/dev/ttyUSB1`

Permissions: typically require `dialout` group membership.

### RS‑485 driver direction control (DE/RE)

Many adapters auto-switch direction. Some require RS‑485 mode or RTS‑toggling:
- The Python tools support Linux RS‑485 ioctls (`TIOCSRS485`) in `python-test/serial_linux.py`.
- In the Java stack we rely on the adapter/driver behavior (jSerialComm); inter-command delay also helps on slow direction switching.

## 3) Protocol variant and framing

### Variant: Norges Gass

In this repo, the default is the **Norges Gass variant**, where:
- **STX (controller → dispenser)** = `0x10`
- **STX (dispenser → controller)** = `0x20`
- **ETX (both directions)** = `0x36`

See `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlProtocolConfig.kt`.

> Note: Standard EHL exists (legacy/other vendors) with STX=`0x02` and ETX=`0x03`. We keep it as a configurable variant, but hardware in our context uses Norges Gass framing.

### Frame format (on the wire)

All frames are binary and have a self-describing length:

```
STX   LEN   ADDR  CMD   DATA...   CHK   ETX
1     1     1     1     0..N      1     1
```

- **LEN** = total frame length in bytes (including STX and ETX)
- **ADDR** = device address (1..255)
- **CMD** = one byte command code
- **DATA** = optional payload
- **CHK** = XOR checksum (see below)

Minimum frame length is 6 bytes (no DATA):
`STX LEN ADDR CMD CHK ETX`

### Checksum (XOR)

Checksum is **XOR of every byte from STX through the last DATA byte**, i.e. **all bytes except CHK and ETX**.

This is implemented consistently in:
- `python-test/ehl_protocol.py` (`xor_checksum`)
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlPacket.calculateChecksum()`
- `lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/service/WireTraceService.kt` (validation helper)

### Example: UNBLOCK (ADDR=1)

UNBLOCK has no payload and uses CMD `0x77`.

Compute:
- LEN = 6
- CHK = `0x10 XOR 0x06 XOR 0x01 XOR 0x77` = `0x60`

Wire bytes:

```
10 06 01 77 60 36
```

(TX STX=`0x10` because this is controller→dispenser.)

## 4) Addressing

The dispenser is addressed at the protocol layer:
- Typical dispenser address: **0x01**
- Another unit on the same COM/bus may be **0x02**

See `docs/_serieal_communication/serial_contract.md`.

## 5) Request/response sequencing (how we “talk”)

### Single‑flight request/response (no concurrent commands)

The core rule: **we never interleave two outstanding requests** on the same bus.

Implementation: `EhlCommunicator.sendAndReceive()` uses a `Mutex`, so only one coroutine can own the line at a time:
- Send frame
- Then read until the first valid response frame is decoded or timeout occurs

See `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt`.

### Timing defaults (practical)

Protocol config defaults (see `EhlProtocolConfig`):
- **interCommandDelayMs**: 100ms (VB6 had sleeps; also helps RS‑485 direction switching)
- **responseTimeoutMs**: 2000ms
- **maxRetries**: 3

Transport timeouts are configured separately at the serial port layer (read/write timeouts).

### RX buffering and resynchronization

Real RS‑485 lines can be noisy; reads are chunked and frames may arrive split across reads.

We therefore:
- Maintain an **RX byte buffer**
- Search for **STX** bytes (`0x10` or `0x20`) to re-align (“sync”)
- Use **LEN** to determine how many bytes constitute a full frame
- Validate **ETX** and **CHK**

Recovery behavior:
- If checksum/format fails, we try to recover by scanning for the **next STX** and discarding corrupted bytes.
- If we get too many consecutive parse failures, we clear the RX buffer to recover.

See `EhlCommunicator.tryParseBufferWithStatus()` and `handleCorruptedPacketRecovery()`.

### “Clear buffer” usage

At runtime we generally **do not** clear RX before every command (to avoid dropping valid pending bytes), but some diagnostic flows do.

Example: `WireTraceService` clears RX before a compliance test to avoid confusing “old” frames with the test response.

## 6) Command set (what we actually send)

Command codes are one byte. The Python probes list names in `python-test/ehl_protocol.py` and Kotlin defines `EhlCommand`.

High-impact control commands:
- **UNBLOCK** (`0x77`): allow delivery / enable pumping
- **BLOCK** (`0x69`): block/stop delivery
- **STOP** (`0x2F`): stop operation (vendor semantics)
- **RESET/ZER** (`0x81` in Python naming vs Kotlin `ZER`): reset (use carefully)

Read/status commands:
- **STATE** (`0x4B`): query dispenser state (bitfield in VB6 mode)
- **VOLUME** (`0x45`): query volume (VB6-compatible payload is 5 ASCII digits, LSB-first)
- **PRICE** (`0x5C`): query price (4 ASCII digits, LSB-first)
- **ERROR_QUERY** (`0x4C`): query error code(s)
- **LINETEST** (`0x6A`): wire/line test (expects magic bytes in response in VB6 mode)

Programming/preset commands (payloads are “VB6 style”):
- **PROG_PRC** (`0xA9`): program price (4 ASCII digits, LSB-first), typically after **PRODUCT_SELECT** (`0xC3`)
- **PROG_AMOUNT** (`0x75`): amount preset (5 ASCII digits, LSB-first)
- **PROG_VOLUME** (`0x70`): volume preset (6 ASCII digits, LSB-first)

### Payload encoding: “LSB‑first ASCII digits”

A key gotcha: several payloads are **ASCII digits** but sent in **reverse order** (least significant digit first), because that’s what the legacy VB6 did.

Examples (from `EhlPacketBuilder` and `EhlDataParser`):
- Price `"15.90"` → `"1590"` → bytes `['0','9','5','1']`
- Volume response `45.50 L` → `"04550"` → bytes `['0','5','5','4','0']` (then reversed for parsing)

## 7) Typical flows

These are the common “conversations” we run.

### Flow A: Health / connectivity check

- Send **LINETEST** (`0x6A`)
- Expect a valid frame back with correct STX/LEN/CHK/ETX
- In VB6 compliance mode, payload begins with `0x55 0xAA` (see `WireTraceService` validation)

### Flow B: Polling state

Loop:
- Send **STATE** (`0x4B`)
- Parse a 1-byte bitfield
- Decide whether to enable/disable operations based on flags (see `WireTraceService.validateVb6Payload()` for interpretation hints)

### Flow C: Enable delivery (UNBLOCK) / stop (BLOCK)

- Send **UNBLOCK** (`0x77`) to allow delivery
- Confirm by reading **STATE** and/or by observing expected behavior on hardware
- Send **BLOCK** (`0x69`) to stop/block delivery

Safety: the Python tool requires explicit acknowledgement before writing:
`python-test/03_control_unblock_block.py --i-understand-this-can-affect-real-hardware`

### Flow D: Read metering (VOLUME)

- Send **VOLUME** (`0x45`)
- Parse VB6-style 5 ASCII digits (LSB-first) to liters (divide by 100.0)

## 8) Transport-level robustness

### SerialPortManager self-healing

`SerialPortManager` adds production hardening:
- Treats “0 bytes written” as a hard failure and forces disconnect (enables clean reconnect).
- Watchdog: if no data received for **60 seconds**, mark connection dead and reconnect (after a 5s wait).

See `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/SerialPortManager.kt`.

### Logging / wire traces

The stack logs raw hex TX/RX at DEBUG level in `EhlCommunicator`.

For explicit VB6 compliance checks and wire snapshots, use `WireTraceService` (websocket-visible logs):
- It records TX and RX hex, validates STX/ETX/LEN/CHK, and validates some payload formats.

## 9) Lab and integration testing

### SOCAT PTY pair + simulator

Use SOCAT mode for realistic “serial byte stream” behavior without real hardware.

See `docs/TRANSPORT_MODES.md` and scripts like `scripts/start-socat-sim.sh`.

Note on parity in simulation:
- The simulator (`lpg-ehl-serialport-sim/.../SerialPortHandler.kt`) sets `NO_PARITY`.
- PTYs typically don’t enforce parity at the OS level anyway; treat parity as a **hardware/driver concern** and validate parity only against real adapters/hardware.

## 10) Troubleshooting checklist (field)

- **No response**:
  - Wrong port (`/dev/ttyS0` vs `/dev/ttyUSB0`)
  - A/B swapped (try swapping)
  - Missing termination / bad cabling
  - Wrong UART params (must be **9600 8E1** for legacy hardware)
  - Bus collision (another master, or a stuck device driving the line)
- **Checksum errors / “noise”**:
  - Grounding/shielding issues
  - Excessive stub lengths
  - Too high baud for cabling/topology
  - Bad RS‑485 adapter (direction switching, driver bugs)
- **Intermittent timeouts**:
  - Increase inter-command delay (100ms → 200ms)
  - Increase response timeout (2s → 3–5s) for slow devices
  - Verify adapter supports RS‑485 mode correctly

## 11) Where to change behavior in code

- **Wire format / checksum / variant**: `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlCodec.kt`, `EhlProtocolConfig.kt`
- **Command sequencing / RX recovery**: `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt`
- **Serial port settings / watchdog**: `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/SerialPortManager.kt`
- **Field probing tools**: `python-test/*.py`

