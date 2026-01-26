# Hardware Mode Detection Guide

## Overview

The LPG-EHL system now automatically detects whether it's running in **LAB MODE** (emulated hardware) or **FIELD MODE** (real hardware), and displays appropriate banners in the web UI.

## How It Works

### Backend (Kotlin/Spring Boot)

The backend checks the `ehl.emulator.enabled` configuration property:

- **LAB MODE** (`ehl.emulator.enabled=true`, default):
  - Uses in-memory `EhlDispenserEmulator`
  - No physical hardware required
  - Safe for development

- **FIELD MODE** (`ehl.emulator.enabled=false`):
  - Uses `RealSerialTransport` for actual RS-485 communication
  - Requires `ehl.serial.port` configuration (e.g., `/dev/ttyS0`)
  - ⚠️ **This communicates with real hardware**

### Frontend (React)

The web UI fetches the hardware mode from the new `/api/v1/config/hardware-mode` endpoint and displays:

- **LAB MODE** (yellow banner): 🧪 LAB MODE - SIMULATED HARDWARE
- **FIELD MODE** (red banner): 🏭 PRODUCTION MODE - REAL HARDWARE

The red banner is more prominent to warn operators they're working with real hardware.

## Configuration

### LAB MODE (Default - Safe for Development)

No configuration needed. The system defaults to LAB MODE.

```bash
# Default behavior - uses emulator
java -jar release/lpg-ehl-monolith.jar

# Explicitly set LAB mode
EHL_EMULATOR_ENABLED=true java -jar release/lpg-ehl-monolith.jar
```

### FIELD MODE (Real Hardware via Serial Port)

To run with a real serial port, set `EHL_EMULATOR_ENABLED=false`:

```bash
# With real socat simulator (recommended for testing)
# Terminal 1: Start your socat simulator
socat -v pty,raw,echo=0 pty,raw,echo=0

# Terminal 2: Run the API with real serial port
EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/pts/X EHL_BAUD_RATE=9600 \
  java -jar release/lpg-ehl-monolith.jar
```

Or with environment variables in `.env.local`:

```bash
# .env.local
EHL_EMULATOR_ENABLED=false
EHL_SERIAL_PORT=/dev/ttyS0
EHL_BAUD_RATE=9600
```

Then run:
```bash
set -a && source .env.local && set +a
java -jar release/lpg-ehl-monolith.jar
```

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `ehl.emulator.enabled` | `true` | Set to `false` for real hardware |
| `ehl.serial.port` | `/dev/ttyS0` | Serial port device (FIELD mode only) |
| `ehl.serial.baud-rate` | `9600` | Serial baud rate (FIELD mode only) |

## API Endpoints

### Get Application Mode

```bash
GET /api/v1/config/mode
```

Response:
```json
{
  "mode": "LAB",
  "profiles": ["local"],
  "description": "Simulation mode with emulated hardware"
}
```

### Get Hardware Mode (NEW)

```bash
GET /api/v1/config/hardware-mode
```

**LAB MODE Response:**
```json
{
  "hardwareMode": "LAB",
  "isRealHardware": false,
  "description": "SIMULATED HARDWARE - Using in-memory emulator",
  "serialPort": null,
  "baudRate": null
}
```

**FIELD MODE Response:**
```json
{
  "hardwareMode": "FIELD",
  "isRealHardware": true,
  "description": "REAL HARDWARE - Communicating via serial port",
  "serialPort": "/dev/ttyS0",
  "baudRate": 9600
}
```

## Web UI Changes

### LAB Mode Banner (Yellow)
Appears at the top of the page when using emulated hardware.
```
🧪 LAB MODE - SIMULATED HARDWARE
Simulation mode with emulated hardware
```

### FIELD Mode Banner (Red)
Appears at the top of the page when using real hardware.
```
🏭 PRODUCTION MODE - REAL HARDWARE
REAL HARDWARE - Communicating via serial port (/dev/ttyS0 @ 9600 baud)
```

## Testing with socat

The recommended way to test **FIELD MODE** without real hardware is with `socat`:

### Setup

1. **Install socat** (if not already installed):
   ```bash
   # macOS
   brew install socat
   
   # Ubuntu/Debian
   sudo apt-get install socat
   ```

2. **Create virtual serial port pair with symlinks**:
   ```bash
   socat -d -d -x -v pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1
   ```
   
   This creates two permanent symlinks:
   - `/tmp/ttyV0` - for the PLS simulator
   - `/tmp/ttyV1` - for the API
   
   The `-d -d -x -v` flags provide verbose output for debugging:
   - `-d -d` - Double debug mode (shows all socket operations)
   - `-x` - Hex dump of all data
   - `-v` - Verbose output

3. **On another terminal, run PLS Simulator**:
   ```bash
   java -jar lpg-ehl-pls-sim/target/lpg-ehl-pls-sim.jar \
     --port=/tmp/ttyV0 --baud=9600 --mode=line --chunk=true
   ```

4. **Run the API** on the other port:
   ```bash
   EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/tmp/ttyV1 EHL_BAUD_RATE=9600 \
     java -jar release/lpg-ehl-monolith.jar
   ```

5. **Open the web UI** at `http://localhost:8080`

You should now see the **red FIELD MODE banner** showing:
```
🏭 PRODUCTION MODE - REAL HARDWARE
REAL HARDWARE - Communicating via serial port (/dev/pts/4 @ 9600 baud)
```

## Production Deployment

For production deployment:

1. Set `ehl.emulator.enabled=false`
2. Set `ehl.serial.port` to the actual device (e.g., `/dev/ttyS0`)
3. The red FIELD MODE banner will warn operators they're working with real hardware
4. All API calls will use real RS-485 communication

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   Web UI (React)                            │
│  - Displays LAB or FIELD MODE banner based on API response  │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   │ GET /api/v1/config/hardware-mode
                   │
┌──────────────────▼──────────────────────────────────────────┐
│              ConfigController                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Check: ehl.emulator.enabled property                │   │
│  │ - true  → LAB MODE (emulator)                        │   │
│  │ - false → FIELD MODE (real serial port)              │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────┬───────────────────────────────────────────────┘
               │
         ┌─────┴──────────────────────────────┐
         │                                    │
    ┌────▼─────────┐            ┌────────────▼────────┐
    │  LAB MODE    │            │   FIELD MODE        │
    │ ┌──────────┐ │            │  ┌───────────────┐  │
    │ │Emulator  │ │            │  │Real Serial    │  │
    │ │In-Memory │ │            │  │Port Transport │  │
    │ └──────────┘ │            │  │/dev/ttyS0     │  │
    │              │            │  │@9600 baud     │  │
    │  No physical │            │  │               │  │
    │  hardware    │            │  │  💻 Real LPG  │  │
    │  needed      │            │  │  Dispenser    │  │
    └──────────────┘            │  └───────────────┘  │
                                └────────────────────┘
```

## See Also

- [WARP.md](project-overview/WARP.md) - Complete technical documentation
- [CommunicationConfig.kt](../lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/CommunicationConfig.kt) - Configuration logic
- [TransportConfiguration.kt](../lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/TransportConfiguration.kt) - Transport layer setup
