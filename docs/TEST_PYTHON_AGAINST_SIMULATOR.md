# Python test scripts vs PLS simulator via socat

This document describes running the **`projects/python-test/`** scripts against the **PLS serial-port simulator** (`release/pls-sim.jar`) using **virtual serial ports** created by **socat**.

It covers:
- how `./scripts/sim-pls.sh` wires socat + the simulator together (especially on macOS)
- what each Python script tested
- the observed results
- why two of the fault-injection Python tests *reported* failure even though the simulator did inject the faults

## 1) Overview

### Goal
Validate end-to-end EHL framing interoperability:

- **Python client** opens a serial device (`/tmp/vserial1`) and sends EHL frames.
- **PLS simulator** opens the peer serial device and responds with valid dispenser frames.

This verifies the same “serial line contract” that the Kotlin/Java stack uses, but without physical RS-485 hardware.

### Components
- **socat**: creates a linked PTY pair (virtual serial cable)
- **`release/pls-sim.jar`**: standalone simulator that responds to EHL frames
- **Python scripts in `projects/python-test/`**: send/parse/verify EHL frames and state changes

## 2) How the socat + simulator setup works

### 2.1 Virtual serial ports
`./scripts/sim-pls.sh` starts socat and creates two symlinks:

- `/tmp/vserial0` — intended as the **simulator side**
- `/tmp/vserial1` — intended as the **client side** (Python/Webapp)

Under the hood, socat also creates the *real* PTY device nodes (e.g. `/dev/ttys008`, `/dev/ttys009`).

### 2.2 Why the script detects PTY paths on macOS
On macOS, Java serial libraries may not reliably enumerate socat-created PTYs, but they can usually **open them if given the exact PTY device path**.

So `sim-pls.sh`:
1) captures socat output
2) extracts the actual PTY paths (`N PTY is /dev/ttysXYZ`)
3) starts the simulator with `--port=<actual PTY path>`

The Python scripts still use `/tmp/vserial1` (the symlink) on their side.

### 2.3 Simulator startup and configuration
`./scripts/sim-pls.sh` will build the simulator artifacts if they’re missing (via `./scripts/build-simulators.sh`).

The simulator is then started with a command conceptually like:

- `java -Dsim.log.level=DEBUG -jar release/pls-sim.jar --port=<PTY0> ...`

Key config values used in this test run:
- **Address**: `1`
- **Legacy address**: enabled (simulator also responds to `32 + address`, i.e. `33`)
- **Baud**: `9600`
- **Parity**: `NONE`
- **Mode**: `ehl`
- **logHex**: `true` (logs raw TX/RX bytes)

Relevant simulator implementation areas:
- CLI parsing: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/CliArgs.kt`
- Main entrypoint: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/PlsSimMain.kt`
- State machine + fault injection: `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/PlsState.kt`

### 2.4 EHL framing (what is exchanged)
The Python scripts implement the same binary packet structure used by the legacy VB6 app:

- `STX + LEN + ADDR + CMD + DATA + CHK + ETX`

Common constants used by the scripts:
- Controller → Dispenser STX: typically `0x10`
- Dispenser → Controller STX: typically `0x20`
- ETX: `0x36`
- CHK: XOR of bytes from STX through last DATA byte

## 3) What was executed (Python scripts)

All scripts were run against:
- `--port /tmp/vserial1`
- address `--addr 1` (except address scan which uses a range)

### Normal (non-fault) scripts
1) `00_list_ports.py`
- **Not run as part of the automated suite**, because it lists `/dev/tty*`-style devices and is mainly for real hardware.
- Included here for completeness: it helps identify real RS-485 adapters and stable `/dev/serial/by-id/...` paths.

2) `02_scan_addresses.py`
- **Purpose**: discover which address responds by polling `STATE (0x4B)`.
- **What it validates**:
  - responses are parseable EHL frames
  - responses come from a dispenser frame (STX `0x20`)
  - matching address logic

3) `01_probe_readonly.py`
- **Purpose**: low-risk read-only-ish probes.
- **Commands tested**:
  - `STATE (0x4B)`
  - `ERROR_QUERY (0x4C)`
  - `VOLUME (0x45)`
  - `TANKBIT (0xC5)`
- **What it validates**:
  - framing and checksum parsing
  - basic command/response compatibility
  - that the simulator returns sensible frames for each poll

4) `03_control_unblock_block.py`
- **Purpose**: send control commands that change state.
- **Commands**:
  - `UNBLOCK (0x77)`
  - `BLOCK (0x69)`
- **Safety**: requires `--i-understand-this-can-affect-real-hardware` before it will actually write.
- **What it validates**:
  - command TX is accepted
  - the effect is verified by polling `STATE` (rather than relying solely on an ACK payload)

5) `05_unlock_hold_block.py`
- **Purpose**: VB6-style enable sequence:
  - attempt UNBLOCK (with retries)
  - hold for N seconds and poll
  - BLOCK again
- **What it validates**:
  - that the simulator behaves consistently across repeated polls during the “hold”
  - that BLOCK returns to a closed/blocked state

6) `04_listen_only.py`
- **Purpose**: passive sniffing; prints any valid frames observed.
- **Important note**: it does not transmit. If nothing else is chattering while it listens, it may observe zero frames.

### Fault injection scripts
7) `06_test_fault_injection.py`
- **Purpose**: validate the simulator’s error injection modes.
- **Tests**:
  - `disconnect` (sim closes the serial port after N seconds)
  - `checksum` (sim corrupts checksums at configured probability)
  - `powerfault` (sim resets state to IDLE and disconnects after N seconds)

## 4) Observed results

### 4.1 Per-script logs (Python)
The scripts write timestamped logs under `projects/python-test/logs/`.

Latest logs from the run:
- `projects/python-test/logs/ehl_scan_20260209_004630_tmp_vserial1_range1_8.log`
- `projects/python-test/logs/ehl_probe_20260209_004633_tmp_vserial1_addr1.log`
- `projects/python-test/logs/ehl_control_unblock_20260209_004633_tmp_vserial1_addr1.log`
- `projects/python-test/logs/ehl_control_block_20260209_004635_tmp_vserial1_addr1.log`
- `projects/python-test/logs/ehl_unlock_20260209_004636_tmp_vserial1_addr1.log`
- `projects/python-test/logs/ehl_listen_20260209_004700_tmp_vserial1.log`

### 4.2 Normal behavior tests: PASS
From the run:
- Address scan found the simulator at address 1 in the `1..8` range.
- Read-only probes successfully parsed valid response frames.
- UNBLOCK and BLOCK succeeded and were verified via subsequent STATE polling.
- The unlock/hold/block sequence executed successfully against the simulator.

### 4.3 Fault injection tests: mixed

#### Checksum corruption (badChecksumRate=0.5): PASS
The checksum fault test behaved as expected (roughly half the responses fail checksum validation).

#### Disconnect-after-N and Powerfault-after-N: simulator injected faults, but the Python test *reported* failure
The simulator **did** inject faults, but the Python test’s detection method does not work reliably with macOS PTYs.

From the simulator wrapper log (`/tmp/socat-sim.log`) during the powerfault scenario:

- `⚡ FAULT INJECTION: POWER FAULT after 5.0s`
- `⚡ Resetting state to IDLE and disconnecting...`
- `🔌 FAULT INJECTION: Force disconnect triggered`

Why the Python test reports failure:
- `06_test_fault_injection.py` treats a “disconnect detected” event as an **exception thrown from serial read/write**.
- On macOS PTYs, a “disconnect” often shows up as:
  - reads returning no bytes / timeouts
  - no more parseable frames
  - *without necessarily raising an exception*

So the simulator is behaving correctly (closing/disconnecting), but the client-side test logic needs to accept “no responses after previously receiving responses” as a valid disconnect signal.

## 5) How to reproduce

### 5.1 Start simulator via socat
From repo root:

```bash
./scripts/sim-pls.sh --address=1 --logHex=true
```

This should show:
- `/tmp/vserial0` (simulator)
- `/tmp/vserial1` (python/webapp)

### 5.2 Run the normal python scripts

```bash
cd projects/python-test

python3 02_scan_addresses.py --port /tmp/vserial1 --addr-range 1-8
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1

python3 03_control_unblock_block.py --port /tmp/vserial1 --addr 1 \
  --i-understand-this-can-affect-real-hardware unblock

python3 03_control_unblock_block.py --port /tmp/vserial1 --addr 1 \
  --i-understand-this-can-affect-real-hardware block

python3 05_unlock_hold_block.py --port /tmp/vserial1 --addr 1 --hold-seconds 30 \
  --i-understand-this-can-affect-real-hardware

python3 04_listen_only.py --port /tmp/vserial1 --duration-s 10
```

### 5.3 Fault injection reproduction
Restart the simulator with one mode at a time.

Disconnect after 5s:
```bash
./scripts/sim-pls.sh --address=1 --logHex=true --disconnectAfterSeconds=5
```

Bad checksum rate 50%:
```bash
./scripts/sim-pls.sh --address=1 --logHex=true --badChecksumRate=0.5
```

Power fault after 5s:
```bash
./scripts/sim-pls.sh --address=1 --logHex=true --powerfaultAfterSeconds=5
```

Then run:
```bash
cd projects/python-test
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test disconnect
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test checksum
python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test powerfault
```

## 6) Recommended improvement (optional)

Update `projects/python-test/06_test_fault_injection.py` so that disconnect/powerfault tests pass when:
1) it successfully receives responses at the start, and then
2) after the configured fault time, it observes a sustained period of no valid responses

Additionally, it should exit non-zero when a test fails (right now it can print “failed” but still exit with success depending on the path).
