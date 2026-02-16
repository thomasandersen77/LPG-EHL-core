# VB6 evidence report: why UNBLOCK/BLOCK toggles STATE but doesn’t unlock hardware

## A) Executive summary

- **VB6 does not equate “UNBLOCK sent” with “hardware unlocked.”** VB6 treats “delivery truly started” only when **two separate STATE bits** are asserted: **startbutton pressed** and **open for delivery**.
- **VB6’s “authorized/unlocked” condition is explicit in code**: it starts a new fueling transaction only when `DISP_startbuttonpressed` **and** `DISP_openfordelivery` are both true (derived from the `STATE (0x4B)` byte).
- **UNBLOCK in VB6 is a specific command `0x77`** and VB6 expects a **specific ACK payload** for it: response command `0x77` with `x(4) = 0x30` (“0”). If not, `DispUnblock` is set false.
- **Your field observation (STATE byte toggles by `0x02`) matches VB6’s “open for delivery” bit**, not the “startbutton pressed” bit. That cleanly explains “STATUS toggles, hardware doesn’t release.”
- **VB6’s enable sequence is more than UNBLOCK/BLOCK.** On startup it **selects nozzle/product** and **programs price**, then for prepaid flows it **programs a preset amount** before attempting UNBLOCK.
- **Your Python script currently accepts “any valid response frame”** after UNBLOCK/BLOCK; VB6 does not. This can mask “UNBLOCK rejected” while a concurrent `STATE` poll still returns valid frames.
- **Addressing in VB6 is production-shifted by +32.** Dispenser address is `dispensernr + 32`, which aligns with your responders `0x20/0x21`.

## B) Observations from field test (facts)

Environment:
- ARK-3360 running Debian Bookworm connected to dispenser RS-485.
- Python test kit using Norges Gass EHL framing (STX ctrl->dev 0x10, dev->ctrl 0x20, ETX 0x36, XOR checksum).
- Serial: 9600 baud, 8N1 (as used by python tools), no explicit RS-485 ioctl required in these runs.

Key field results:
1) Correct serial port appears to be /dev/ttyS3. Scanning /dev/ttyS0, /dev/ttyS1, /dev/ttyS2 found no responders.
2) Scanning addresses on /dev/ttyS3 found two responding device addresses: 32 (0x20) and 33 (0x21).
3) Read-only probing on /dev/ttyS3 succeeded 4/4 on BOTH addresses:
   - STATE (0x4B), ERROR_QUERY (0x4C), VOLUME (0x45), TANKBIT (0xC5) all returned valid frames.
4) Control commands were sent and produced valid responses, and the STATE *data byte* changed by one bit:
   - ADDR 32: STATE data D9 -> DB on UNBLOCK, and DB -> D9 on BLOCK
   - ADDR 33: STATE data 58 -> 5A on UNBLOCK, and 5A -> 58 on BLOCK
5) Despite this “state bit toggling,” the dispenser did NOT physically unlock / authorize delivery as expected.

Important: The python control script accepts “any valid response frame” after sending (often a STATE frame). We do NOT yet have proof of a specific ACK/NACK response semantics for UNBLOCK/BLOCK.

## C) What VB6 does on the wire (serial settings + framing + timings)

### Serial port configuration (VB6 evidence)

- **VB6 chooses COM port from `server.ini` and assigns it to `MSComm1.CommPort`.** It does *not* set baud/parity in code; it only sets the port number and opens the port.

Evidence:
- `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/defs.bas` → `Sub Main()` reads `Com_port = Val(cfgline(4))`
- `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Public Function cmdcom_on()` sets `MSComm1.CommPort = Com_port`, then `MSComm1.PortOpen = True`

- **Form-level MSComm properties set DTREnable/NullDiscard/RThreshold but no explicit `.Settings` string appears in the `.frm` file.**
  - **Inference**: baud/parity/stop bits are either MSComm defaults or design-time settings not shown in the extracted `.frm` text we inspected.

### Framing + checksum + “end-of-frame” recognition (VB6 evidence)

From `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Private Sub MSComm1_OnComm()`:

- **Direction bytes**: VB6 expects incoming frames to start with `0x20` and uses `0x10` in all outgoing command builders shown.
- **Length semantics**: VB6 checks `x(1) == (u+1)` where `u` is the last received index; i.e., byte 2 is **total frame length in bytes**.
- **ETX**: VB6 uses decimal `54` (hex `0x36`) as end marker.
- **Checksum**: XOR of bytes `x(0)` .. `x(u-2)` (includes direction + length + addr + cmd + payload), compared to `x(u-1)`.

### Timing / pacing (VB6 evidence)

From `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/defs.bas` → `Sub comm_out(Waittime As Integer, commstr As String)`:

- **VB6 blocks sends behind a software `rts` flag and sleeps after each send.**
  - **Not** evidence of RS-485 DE/RE toggling; this is application-level pacing / mutual exclusion.

## D) VB6 state machine: conditions and transitions leading to “hardware unlock”

### What VB6 considers “delivery actually started” (hard evidence)

VB6 only transitions into “new fueling transaction started” when:

- `DISP_startbuttonpressed` is true (derived from `STATE` bit check), **and**
- `DISP_openfordelivery` is true (derived from `STATE` bit check), **and**
- `new_tank` is false (i.e., not already started)

Evidence:
- `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Private Sub MSComm1_OnComm()` → `Case 75` parses `state_string = decimaltobinn(x(4))`, then:
  - `Mid(state_string, 6, 1)` controls `DISP_startbuttonpressed`
  - `Mid(state_string, 7, 1)` controls `DISP_openfordelivery`
  - it creates a new tanking record only when `Not new_tank And DISP_startbuttonpressed And DISP_openfordelivery`

**Inference (bit mapping, grounded in code + your field bytes):**
- `decimaltobinn()` returns an 8-char bit string and VB6 indexes it with `Mid(..., n, 1)`.
- Your observed toggle `D9 <-> DB` and `58 <-> 5A` is a delta of `0x02`, i.e. one bit.
- That bit aligns with VB6’s `Mid(state_string, 7, 1)` (“open for delivery”), not the `startbuttonpressed` bit.
- Therefore, **UNBLOCK can flip “open for delivery” while “startbuttonpressed/nozzle condition” remains false**, preventing real delivery release.

## E) VB6 command sequences for “enable delivery” and “stop/block”

### Common frame format (VB6 evidence)

Outgoing frames are built as:

`[0x10, LEN, ADDR, CMD, ...payload..., XOR, 0x36]`

Examples in VB6:
- `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `set_preset_amount()`, `disp_unblock()`, `disp_block()`

### “Enable delivery” sequence (prepaid/card path) — ordered steps (VB6 evidence)

From `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Private Sub pre_sum(amount As String)`:

1) Gate: exits unless (among others) `DISP_openfordelivery` is false and `ok_to_opendisp` is true.
2) Program preset amount: loops calling `set_preset_amount amount` until `SetAmount = True`.
3) Send UNBLOCK: loops calling `disp_unblock` until `DispUnblock = True`.

### “Stop/block” sequence (VB6 evidence)

From `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Private Sub disp_block()`:

1) Send `BLOCK` with `CMD = 0x69`
2) Reset internal flags
3) Send `RESET` with `CMD = 0x81` via `Reset_disp(dispnr(0))` (implemented in `defs.bas`)

### What VB6 expects back (ACK semantics) (VB6 evidence)

From `/Users/alejandrosaksida/Alejandro/repos/Cloudberries/NorgesGass/LPG-EHL-core/norgesgass_legacy/pumpekontroll.frm` → `Private Sub MSComm1_OnComm()`:

- **Preset accepted** when response indicates OK:
  - `Case 117`: if `x(4) = 0x30` then `SetAmount = True` else `SetAmount = False`
- **UNBLOCK accepted** when response indicates OK:
  - `Case 119`: if `x(4) = 0x30` then `DispUnblock = True` else `DispUnblock = False`

## F) Gaps between python behavior and VB6 expectations

- **ACK semantics gap (high confidence)**:
  - **VB6**: “UNBLOCK success” == receive a `0x77` response (VB6’s `Case 119`) where the **first payload byte is `0x30`**.
  - **Python**: accepts **any valid frame** (often `STATE`) as “success.”
  - Impact: Python can report “UNBLOCK succeeded” even if the pump rejected `0x77` (or if `0x77` success is necessary-but-not-sufficient for release).

- **Prerequisites gap (high confidence)**:
  - **VB6**: enable flow involves more than UNBLOCK/BLOCK (startup `disp_setprice`, preset programming before unblock in prepay flow, etc.).
  - **Python tests**: did not demonstrate those prerequisite commands prior to UNBLOCK/BLOCK.

- **State-machine gap (very high confidence)**:
  - **VB6** requires BOTH `DISP_openfordelivery` and `DISP_startbuttonpressed` to begin a transaction.
  - Field toggles match only `DISP_openfordelivery`.

- **Addressing gap (moderate confidence)**:
  - VB6 separately handles a `container` module address (`rs485adrcontainer + 32`) in addition to the dispenser address.
  - Your two responders `0x20` and `0x21` may map to **dispenser vs container**, not two equivalent endpoints.

## G) Most likely root causes (ranked)

1) **UNBLOCK toggles “open for delivery” status, but the dispenser still requires “startbutton/nozzle” transition before energizing hardware**
   - Evidence: VB6 only starts a transaction when `DISP_startbuttonpressed AND DISP_openfordelivery`.
   - Field fit: UNBLOCK toggles exactly one bit (`0x02`), which maps to VB6’s `DISP_openfordelivery` (inference grounded in VB6 bit parsing + observed deltas).

2) **Python is treating “any valid response frame” as UNBLOCK acceptance; VB6 requires a specific `0x77` response with `0x30` payload**
   - Evidence: VB6’s `Case 119` gate sets `DispUnblock` only on `x(4)=0x30`.

3) **Missing prerequisite sequence (nozzle/product select, price programming, preset programming) means “open for delivery” doesn’t actually authorize output**
   - Evidence: VB6 programs preset amount before UNBLOCK in `pre_sum()` and programs/queries price and nozzle selection in its tooling/config.

4) **UNBLOCK is being sent to the wrong logical unit (dispenser vs container module)**
   - Evidence: VB6 treats container addressing separately and polls it conditionally.

## H) Recommended next test steps (minimal-risk)

1) **Prove whether the “startbutton/nozzle” bit ever changes**
   - After UNBLOCK, repeatedly read `STATE (0x4B)` and log raw state byte + decoded bits.
   - Try physical actions: lift nozzle, press start (if present), etc.
   - Confirms root cause #1 if `open-for-delivery` toggles but `startbuttonpressed` never becomes 1.

2) **Make Python validate VB6’s ACK semantics**
   - For UNBLOCK/BLOCK, only consider success when the next response frame is the matching command (UNBLOCK/BLOCK) with VB6’s “OK” payload byte (`0x30`).
   - Confirms root cause #2 if UNBLOCK frequently returns non-`0x30` or no `0x77` response while `STATE` frames still arrive.

3) **Replay VB6’s prerequisite sequence**
   - Send the same high-level sequence VB6 relies on:
     - nozzle/product selection
     - preset amount programming
     - UNBLOCK
   - Confirms root cause #3 if adding these steps causes the state machine to progress to actual delivery readiness.

4) **Disambiguate which address is the dispenser vs container/other module**
   - Compare which address reflects nozzle lift / startbutton condition in the state bits and error codes.
   - Confirms root cause #4 if only one address tracks the physical action or reaches VB6’s “startbutton + open” condition.

