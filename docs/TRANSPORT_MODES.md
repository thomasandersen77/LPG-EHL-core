# LPG-EHL Transport Modes

This document describes the tri-mode transport architecture for EHL serial communication.

## Overview

The LPG-EHL system supports three transport modes for communicating with LPG dispensers:

| Mode | Use Case | Transport | Backend |
|------|----------|-----------|---------|
| **EMULATOR** | Development | InMemorySerialPort | EhlDispenserEmulator |
| **SOCAT** | Integration Testing | RealSerialTransport | PLS Simulator via PTY |
| **HARDWARE** | Production | RealSerialTransport | Physical RS-485 |

## Configuration

Set the transport mode using the `ehl.transport.mode` property:

```bash
# Environment variable
export EHL_TRANSPORT_MODE=EMULATOR  # or SOCAT, HARDWARE

# Command line
java -jar lpg-ehl-webapp.jar --ehl.transport.mode=SOCAT

# application.yaml
ehl:
  transport:
    mode: EMULATOR
```

### Legacy Support

The old `ehl.emulator.enabled` property still works for backwards compatibility:

```yaml
ehl:
  emulator:
    enabled: true   # Maps to EMULATOR mode
    enabled: false  # Maps to HARDWARE mode
```

---

## Mode 1: EMULATOR (Default for Webapp)

The emulator mode uses an in-memory transport that communicates directly with the `EhlDispenserEmulator` class. No physical hardware or external processes are required.

### When to Use
- Local development
- Unit testing
- Quick prototyping
- CI/CD pipelines

### Configuration

```yaml
ehl:
  transport:
    mode: EMULATOR
  emulator:
    dispenser-address: 1
    price-per-liter-cents: 1590
    latency-ms: 20
```

### Starting

```bash
# Webapp (EMULATOR is default)
java -jar lpg-ehl-webapp/target/lpg-ehl-webapp.jar

# Explicit
java -jar lpg-ehl-webapp.jar --ehl.transport.mode=EMULATOR
```

### Characteristics
- ✅ Zero setup required
- ✅ Fast startup
- ✅ Deterministic behavior
- ✅ Perfect for automated testing
- ❌ No real serial communication
- ❌ Different timing characteristics than hardware

---

## Mode 2: SOCAT (Integration Testing)

The SOCAT mode uses real serial port communication over a virtual PTY (pseudo-terminal) pair created by `socat`. The PLS Simulator runs on one end, and the application connects to the other end.

### When to Use
- Integration testing with realistic serial communication
- Testing protocol edge cases
- Validating wire-level protocol compliance
- Development without physical hardware

### Architecture

```
┌─────────────────┐     ┌─────────┐     ┌─────────────────┐
│  Application    │◄───►│  socat  │◄───►│  PLS Simulator  │
│  (webapp/       │     │  PTY    │     │  (pls-sim.jar)  │
│   headless)     │     │  pair   │     │                 │
│                 │     │         │     │                 │
│  /tmp/ttyV1     │     │         │     │  /tmp/ttyV0     │
└─────────────────┘     └─────────┘     └─────────────────┘
```

### Quick Start

The easiest way to start SOCAT mode is using the provided script:

```bash
# Build JARs first
mvn -q -DskipTests package

# Start everything (webapp + socat + simulator)
./scripts/start-socat-sim.sh

# Or just simulator (for manual testing)
./scripts/start-socat-sim.sh --sim-only

# Headless with debug API
./scripts/start-socat-sim.sh --headless --debug-api
```

### Manual Setup

If you prefer manual control:

**Terminal 1: Start socat**
```bash
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1
```

**Terminal 2: Start PLS Simulator**
```bash
java -jar lpg-ehl-serialport-sim/target/pls-sim.jar \
    --port=/tmp/ttyV0 \
    --mode=ehl \
    --logHex=true \
    --address=1 \
    --price=1590 \
    --blocked=true
```

**Terminal 3: Start Application**
```bash
# Webapp
java -jar lpg-ehl-webapp/target/lpg-ehl-webapp.jar \
    --ehl.transport.mode=SOCAT \
    --ehl.serial.port=/tmp/ttyV1

# OR Headless with debug API
java -jar lpg-ehl-app-headless/target/lpg-ehl-headless.jar \
    --ehl.transport.mode=SOCAT \
    --ehl.serial.port=/tmp/ttyV1 \
    --spring.profiles.active=debug-api
```

### PLS Simulator Options

```
Usage: java -jar pls-sim.jar --port=<port> [options]

Required:
  --port=<port>       Serial port device (e.g., /tmp/ttyV0)

Serial Options:
  --baud=<baud>       Baud rate (default: 9600)
  --mode=<mode>       Frame mode: 'line', 'stxetx', or 'ehl' (default: line)
  --chunk=<bool>      Enable chunked responses (default: false)
  --latencyMs=<ms>    Add latency jitter (default: 0)
  --logHex=<bool>     Log raw bytes as hex (default: false)

Dispenser Options:
  --address=<addr>    Dispenser address 1-8 (default: 1)
  --price=<cents>     Price per liter in cents (default: 1590)
  --blocked=<bool>    Initial blocked state (default: true)
```

### Testing with curl (Headless + debug-api)

When running headless with the `debug-api` profile:

```bash
# Check health
curl http://localhost:8080/api/debug/health

# Get dispenser state
curl http://localhost:8080/api/debug/state/1

# Unblock dispenser (enable pumping)
curl -X POST http://localhost:8080/api/debug/unblock/1

# Block dispenser
curl -X POST http://localhost:8080/api/debug/block/1

# Get volume
curl http://localhost:8080/api/debug/volume/1

# Line test
curl -X POST http://localhost:8080/api/debug/linetest/1
```

### Characteristics
- ✅ Real serial communication
- ✅ Realistic timing
- ✅ Can test edge cases (noise, timeouts)
- ✅ No physical hardware needed
- ❌ Requires socat installed
- ❌ More complex setup

---

## Mode 3: HARDWARE (Production)

The hardware mode connects to a physical RS-485 serial port for communication with real LPG dispenser hardware.

### When to Use
- Production deployment
- Testing with real dispensers
- Field service diagnostics

### Prerequisites

- RS-485 serial adapter (USB or built-in)
- Physical LPG dispenser connected
- Correct wiring (A/B, GND)

### Configuration

```yaml
ehl:
  transport:
    mode: HARDWARE
  serial:
    port: /dev/ttyS0        # Linux
    # port: /dev/ttyUSB0    # USB adapter
    # port: COM3            # Windows
    baud-rate: 9600
    data-bits: 8
    parity: EVEN
    stop-bits: 1
```

### Starting

```bash
# On production hardware (ARK-3600)
java -jar lpg-ehl-headless.jar --ehl.transport.mode=HARDWARE

# Explicit serial port
java -jar lpg-ehl-headless.jar \
    --ehl.transport.mode=HARDWARE \
    --ehl.serial.port=/dev/ttyS0
```

### Serial Port Examples

| Platform | Port |
|----------|------|
| Linux (built-in) | `/dev/ttyS0`, `/dev/ttyS1` |
| Linux (USB) | `/dev/ttyUSB0`, `/dev/ttyACM0` |
| Raspberry Pi | `/dev/ttyAMA0` |
| macOS | `/dev/cu.usbserial-*` |
| Windows | `COM1`, `COM3`, etc. |

### Characteristics
- ✅ Real hardware communication
- ✅ Production-ready
- ⚠️ Requires physical hardware
- ⚠️ Wrong commands can affect real equipment

---

## Startup Script Reference

The `scripts/start-socat-sim.sh` script automates SOCAT mode setup:

```
Usage: ./scripts/start-socat-sim.sh [options]

Options:
  --webapp          Start lpg-ehl-webapp after simulator (default)
  --headless        Start lpg-ehl-headless instead of webapp
  --sim-only        Only start socat + simulator, no app
  --address=<1-8>   Dispenser address (default: 1)
  --price=<cents>   Price in cents (default: 1590)
  --blocked=<bool>  Initial blocked state (default: true)
  --debug-api       Enable debug-api profile for headless
  --port=<port>     Web server port (default: 8080)
  --help            Show help message

Examples:
  ./scripts/start-socat-sim.sh                         # Webapp mode
  ./scripts/start-socat-sim.sh --sim-only              # Just simulator
  ./scripts/start-socat-sim.sh --headless --debug-api  # Headless with API
  ./scripts/start-socat-sim.sh --price=2100            # Custom price
```

---

## Default Modes by Application

| Application | Default Mode | Reason |
|-------------|--------------|--------|
| lpg-ehl-webapp | EMULATOR | Safe for development |
| lpg-ehl-headless | HARDWARE | Production deployment |

To override:

```bash
# Run headless in SOCAT mode
java -jar lpg-ehl-headless.jar --ehl.transport.mode=SOCAT

# Run webapp in HARDWARE mode (caution!)
java -jar lpg-ehl-webapp.jar --ehl.transport.mode=HARDWARE
```

---

## Troubleshooting

### SOCAT Mode Issues

**Error: "socat is not installed"**
```bash
# macOS
brew install socat

# Debian/Ubuntu
sudo apt-get install socat
```

**Error: "Failed to open serial port"**
- Check that socat is running
- Verify PTY symlinks exist: `ls -la /tmp/ttyV*`

**No response from simulator**
- Check simulator logs for received commands
- Verify `--mode=ehl` is set for binary protocol
- Try `--logHex=true` to see raw bytes

### HARDWARE Mode Issues

**Permission denied on serial port**
```bash
# Add user to dialout group (Linux)
sudo usermod -a -G dialout $USER
# Log out and back in

# Or run with sudo (not recommended)
sudo java -jar ...
```

**No response from dispenser**
- Check wiring (A/B, GND)
- Verify baud rate: 9600
- Verify parity: EVEN
- Check termination resistors
