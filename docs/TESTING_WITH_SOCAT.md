# Testing with Socat Virtual Serial Ports

This guide explains how to test the EHL protocol simulator using virtual serial ports created with `socat`, allowing you to run Alejandro's Python test scripts without physical hardware.

## Overview

**socat** creates a pair of linked pseudo-terminals (PTYs) that behave like a null-modem cable:
- One end: Simulator listens for EHL commands
- Other end: Test client (Python script, webapp, etc.) connects

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   socat      │        │  Standalone  │        │  Python      │
│              │◄──────►│  Simulator   │        │  Test Script │
│ vserial0 ◄──►│        │              │        │              │
│ vserial1     │◄───────┼──────────────┼───────►│              │
└──────────────┘        └──────────────┘        └──────────────┘
```

## Prerequisites

### Install socat

**macOS:**
```bash
brew install socat
```

**Ubuntu/Debian:**
```bash
sudo apt-get install socat
```

**Verify installation:**
```bash
socat -V
```

## Quick Start (3 Terminals)

### Terminal 1: Start Virtual Serial Ports

```bash
./start-virtual-serial.sh
```

This creates:
- `/tmp/vserial0` ← Simulator listens here
- `/tmp/vserial1` ← Clients connect here

**Output:**
```
════════════════════════════════════════════════════════════
  🔌 Virtual Serial Port Pair Creator
════════════════════════════════════════════════════════════

Creating paired virtual serial ports using socat...

  Port 0: /tmp/vserial0  ← Simulator listens here
  Port 1: /tmp/vserial1  ← Clients connect here

Press Ctrl+C to stop
```

Leave this terminal running.

### Terminal 2: Start Standalone Simulator

```bash
# Build first (if not already done)
./build_monolith.sh --skip-tests

# Run simulator
java -jar release/pls-sim.jar --port /tmp/vserial0
```

**Options:**
```bash
java -jar release/pls-sim.jar --port /tmp/vserial0 \
  --address 1 \
  --price 15.90 \
  --baud 9600 \
  --flow-rate 0.5
```

**Output:**
```
┌────────────────────────────────────────────────────────────┐
│ 🛢️  PLS Simulator - Standalone EHL Dispenser Emulator      │
└────────────────────────────────────────────────────────────┘

Configuration:
  Serial Port:  /tmp/vserial0
  Address:      1
  Price:        15.90 kr/L
  Baud Rate:    9600
  Flow Rate:    0.5 L/s

Connecting to /tmp/vserial0...

✅ Simulator ready!
   Waiting for EHL commands on /tmp/vserial0...

Test with Alejandro's Python script:
  python3 python-test/01_probe_readonly.py --port /tmp/vserial1 --addr 1

Press Ctrl+C to stop
```

### Terminal 3: Run Python Tests

```bash
cd python-test

# Basic read-only test (STATE, ERROR_QUERY, VOLUME, TANKBIT)
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1
```

**Expected output:**
```
INFO --- Probe: STATE ---
TX 10 06 01 4B 6C 36 -> UNBLOCK (0x4B) to address #1
RX STX=0x20 LEN=7 ADDR=1 CMD=0x4B(STATE) DATA=00 CHK=0x6F ETX=0x36

INFO --- Probe: ERROR_QUERY ---
...

Summary: 4/4 commands produced a valid response frame.
```

## Testing Scenarios

### 1. Address Scanning

Find which addresses respond:

```bash
python3 02_scan_addresses.py --port /tmp/vserial1 --addr-range 1-32
```

**Output:**
```
Scanning addresses 1..32 on /tmp/vserial1 @ 9600 using STATE (0x4B)
ADDR 1: RX STX=0x20 LEN=7 ADDR=1 CMD=0x4B(STATE) ...
Found responding addresses: 1
```

### 2. Control Commands (UNBLOCK/BLOCK)

**⚠️ Warning:** These change simulator state!

```bash
python3 03_control_unblock_block.py \
  --port /tmp/vserial1 \
  --addr 1 \
  unblock \
  --i-understand-this-can-affect-real-hardware
```

### 3. Listen-Only Mode (Passive Sniffing)

```bash
python3 04_listen_only.py --port /tmp/vserial1 --duration-s 30
```

## Troubleshooting

### Error: "Failed to open serial port"

**Cause:** Port already in use or doesn't exist.

**Solution:**
1. Check if socat is running:
   ```bash
   ps aux | grep socat
   ```

2. Verify ports exist:
   ```bash
   ls -la /tmp/vserial*
   ```

3. Restart socat (Terminal 1)

### Error: "Permission denied"

**Cause:** Insufficient permissions for serial port access.

**Solution (Linux):**
```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Log out and log back in, then verify:
groups
```

**Solution (macOS):**
Socat virtual ports in `/tmp/` should work without special permissions.

### Simulator not responding

**Check logs in Terminal 2:**
- Look for `📥 RX:` messages when you send commands
- If no RX messages appear, check socat connection

**Verify serial link:**
```bash
# Terminal 4: Send test byte to vserial1
echo -n '\x10' > /tmp/vserial1

# Check Terminal 2 for incoming data log
```

### Python script shows "No valid replies"

Common causes:
- **Wrong address**: Simulator is address 1 by default
  ```bash
  python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1
  ```

- **Wrong baud rate**: Both must match (default 9600)
  ```bash
  # Simulator
  java -jar pls-sim.jar --port /tmp/vserial0 --baud 9600
  
  # Python
  python3 01_probe_readonly.py --port /tmp/vserial1 --baud 9600
  ```

- **Port swapped**: Use vserial0 for simulator, vserial1 for client

## Advanced Usage

### Custom Price Testing

```bash
# Start simulator with 18.50 kr/L
java -jar release/pls-sim.jar --port /tmp/vserial0 --price 18.50

# Query price via Python
python3 -c "
from ehl_protocol import *
from serial_linux import open_serial

port, _ = open_serial('/tmp/vserial1', baud=9600)
frame = build_frame(1, 0x5C, b'', stx=STX_CONTROLLER)  # PRICE query
port.write(frame)
# ... read response
"
```

### Multiple Dispensers

Run multiple simulators on different addresses:

```bash
# Terminal 2a: Dispenser 1 on port pair 0/1
./start-virtual-serial.sh  # Creates vserial0/1
java -jar pls-sim.jar --port /tmp/vserial0 --address 1

# Terminal 2b: Dispenser 2 on port pair 2/3
# (Modify start-virtual-serial.sh to create vserial2/3)
java -jar pls-sim.jar --port /tmp/vserial2 --address 2
```

### Logging All Traffic

Enable debug logging to see all hex bytes:

```bash
# Simulator: Set log level in logback.xml
# Python: Use --debug flag
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1 --debug
```

## Integration with lpg-ehl-webapp

Instead of standalone simulator, you can run webapp in FIELD mode pointing to socat:

```bash
# Terminal 1: socat (as before)
./start-virtual-serial.sh

# Terminal 2: webapp in FIELD mode (NOT standalone sim)
./start-lpg-ehl.sh --field --serial-port /tmp/vserial0

# Terminal 3: Python tests
python3 python-test/01_probe_readonly.py --port /tmp/vserial1 --addr 1
```

**Note:** This uses the full webapp stack (DB, REST API, etc.) instead of just the simulator.

## Cleanup

When done testing:

1. **Stop Python script** (Ctrl+C in Terminal 3)
2. **Stop simulator** (Ctrl+C in Terminal 2)
3. **Stop socat** (Ctrl+C in Terminal 1)

Virtual ports will be automatically cleaned up.

## See Also

- `python-test/README.md` - Python test script documentation
- `QUICK_START.md` - Quick start guide for lpg-ehl project
- Alejandro's Slack thread on kortoppdatering testing
