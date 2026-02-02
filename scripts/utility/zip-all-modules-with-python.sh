#!/bin/bash
# zip-all-modules-with-python.sh - Export all modules + python-test for AI analysis
# 
# Usage: ./scripts/zip-all-modules-with-python.sh
# Output: 
#   - Separate zip files for each module
#   - Combined AI-ANALYSIS-CONTEXT.zip with README and all sources
#
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIR="$PROJECT_ROOT/ai-exports"

cd "$PROJECT_ROOT"

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "🗜️  Creating comprehensive AI-friendly archives..."
echo "📦 Timestamp: $TIMESTAMP"
echo "📁 Output: $OUTPUT_DIR"
echo ""

# Function to zip a module
zip_module() {
    local module_dir=$1
    local module_name=$(basename "$module_dir")
    local output_file="$OUTPUT_DIR/${module_name}-${TIMESTAMP}.zip"
    
    if [ ! -d "$module_dir" ]; then
        echo "   ⚠️  Skipping $module_name (not found)"
        return
    fi
    
    echo "📦 Zipping $module_name..."
    
    cd "$module_dir"
    zip -q -r "$output_file" . \
        -x "*.git/*" \
        -x "*target/*" \
        -x "*node_modules/*" \
        -x "*dist/*" \
        -x "*build/*" \
        -x "*.idea/*" \
        -x "*.vscode/*" \
        -x "*.DS_Store" \
        -x "*.class" \
        -x "*.jar" \
        -x "*.war" \
        -x "*.log" \
        -x "*.tmp" \
        -x "*~" \
        -x "*.swp" \
        -x "*.iml" \
        -x "*.pyc" \
        -x "*__pycache__/*"
    
    cd "$PROJECT_ROOT"
    
    local size=$(du -h "$output_file" | cut -f1)
    echo "   ✅ $(basename "$output_file") ($size)"
}

# Zip all Maven modules (Kotlin/Java)
echo "=== Maven Modules (Kotlin/Java) ==="
zip_module "lpg-ehl-core"
zip_module "lpg-ehl-service"
zip_module "lpg-transport"
zip_module "lpg-ehl-emulator"
zip_module "lpg-ehl-serialport-sim"
zip_module "lpg-ehl-webapp"
zip_module "lpg-ehl-app-headless"
zip_module "lpg-ehl-cli"

echo ""
echo "=== React Frontend (TypeScript) ==="
zip_module "lpg-web"

echo ""
echo "=== Python Test Scripts ==="
zip_module "python-test"

# Create comprehensive README for AI
echo ""
echo "📝 Creating AI analysis context README..."

cat > "$OUTPUT_DIR/AI-ANALYSIS-README-${TIMESTAMP}.md" << 'EOF'
# LPG-EHL System - Complete Source Code Export for AI Analysis

**Export Date:** $(date +"%Y-%m-%d %H:%M:%S")
**Purpose:** Complete source code archive for AI analysis, including Python test scripts

---

## 🏗️ System Architecture Overview

This is a **Spring Boot monorepo** for controlling LPG dispensers via RS-485/EHL protocol.

### Deployment Modes
- **LAB Mode**: In-memory emulator (no hardware), for development
- **FIELD Mode**: Real RS-485 hardware communication, for production

---

## 📦 Module Structure

### Core Protocol Layer (NO Spring dependencies)
```
lpg-ehl-core/
├── protocol/          # EHL protocol codec (framing, checksum, commands)
├── transaction/       # Transaction state machine
└── emulator/          # In-memory dispenser emulator (LAB mode)
```

**Key Files:**
- `EhlCodec.kt` - EHL packet encoding/decoding
- `EhlProtocolConfig.kt` - Protocol variant configuration
- `TransactionManager.kt` - Transaction lifecycle
- `EhlDispenserEmulator.kt` - VB6-compatible emulator

---

### Transport Layer
```
lpg-transport/
├── communication/     # SerialPortManager (watchdog, reconnect)
└── pls/              # PlsState, RealSerialTransport (deprecated)
```

**Key Files:**
- `SerialPortManager.kt` - Robust serial I/O with hardware watchdog
- `SerialTransport.kt` - Interface for transport abstraction

**Serial Config:**
- Baud: 9600
- Parity: EVEN (8E1)
- jSerialComm library for cross-platform support

---

### Service Layer (Spring + JPA)
```
lpg-ehl-service/
├── pump/             # PumpStateService, PumpAuthorizationService
├── azure/            # Azure Storage Queue sync
└── dto/              # API response DTOs
```

**Key Files:**
- `PumpStateService.kt` - Pump state management (IDLE → PUMPING → PAYMENT_PENDING)
- `PumpAuthorizationService.kt` - Payment authorization workflow
- `AzureSyncService.kt` - Background sync to Azure (optional)

**Database:**
- H2 (in-memory) for testing
- PostgreSQL for production
- Liquibase migrations in `src/main/resources/db/changelog/`

---

### Web Application
```
lpg-ehl-webapp/
├── api/controller/   # REST API endpoints
├── config/           # Spring configuration (LAB vs FIELD mode)
└── resources/static/ # React frontend (built from lpg-web/)
```

**Key Endpoints:**
- `/api/v1/emulator/pump/{address}/*` - Pump control (now works in FIELD mode)
- `/api/v1/transactions` - Transaction history
- `/api/v1/sync/status` - Azure sync status (stub when disabled)
- `/ws/logs` - WebSocket for real-time protocol logs

**Configuration:**
- `lpg.mode=LAB|FIELD` - Controls transport layer
- `ehl.serial.port=/dev/ttys004` - Serial port path
- `azure.enabled=true|false` - Azure sync toggle

---

### Headless Application
```
lpg-ehl-app-headless/
└── PlsHeadlessApplication.kt
```

**Purpose:** Production deployment without web server
- Monitors pump state
- Sends EHL commands
- No GUI, designed for edge devices (ARK-3600, Raspberry Pi)

---

### React Frontend
```
lpg-web/
├── src/
│   ├── components/   # ControlPanel, DispenserSimulator
│   ├── pages/        # HomePage, TransactionsPage, etc.
│   └── contexts/     # AppModeContext (LAB/FIELD detection)
└── openapi.yaml      # API contract
```

**Tech Stack:**
- Vite + React 18 + TypeScript
- TanStack Query for data fetching
- Tailwind CSS for styling

**Key Features:**
- `/control` - Manual pump control ("Fri Pumpe")
- `/transactions` - Transaction history
- `/reports` - Daily summaries

---

### Python Test Scripts
```
python-test/
├── serial_linux.py        # Raw serial port (termios + select)
├── ehl_protocol.py        # EHL framing and checksum
├── 01_probe_readonly.py   # Read-only probe (STATE, VOLUME, etc.)
└── logging_utils.py       # Console logging
```

**Purpose:** 
- Field testing with **real hardware** (not emulator)
- Minimal dependencies (no pyserial)
- Validates Kotlin implementation correctness

**Protocol:**
- Frame: `STX + LEN + ADDR + CMD + DATA + CHK + ETX`
- Checksum: XOR of all bytes from STX to end of DATA
- Serial: 8N1 (8 data bits, no parity, 1 stop bit)

**Key Differences from Kotlin:**
- **Python**: Uses native Linux `termios` + `select()` for non-blocking I/O
- **Kotlin**: Uses `jSerialComm` library with blocking reads + timeout
- **Both**: Identical EHL protocol implementation

---

## 🔄 How Components Interact

### LAB Mode (Development)
```
React Frontend (lpg-web)
    ↓ HTTP/WS
lpg-ehl-webapp (Spring Boot)
    ↓ SerialTransport interface
InMemorySerialPort
    ↓ Concurrent queues
EhlDispenserEmulator (VB6-compatible)
```

### FIELD Mode (Production with Hardware)
```
React Frontend OR Headless App
    ↓ HTTP/WS or direct service calls
lpg-ehl-webapp OR lpg-ehl-app-headless
    ↓ SerialTransport interface
SerialPortManager (hardware watchdog)
    ↓ jSerialComm (8E1, 9600 baud)
REAL RS-485 DISPENSER
```

### FIELD Mode (Testing with Simulator)
```
Python Test Script OR Kotlin Webapp
    ↓ /tmp/vserial1       ↓ /tmp/vserial0
         ↓                     ↓
       socat (virtual serial ports)
              ↓
    lpg-ehl-serialport-sim (PlsSimulator.kt)
    - Emulates hardware dispenser
    - Binary EHL protocol over serial
    - 8N1, 9600 baud
```

---

## 🐍 Python Test Scripts Analysis

### Core Files

#### 1. `serial_linux.py` - Low-level Serial I/O
```python
# Linux-only, uses termios + select()
class SerialPort:
    def write(data: bytes) -> None
    def read(max_bytes: int, timeout_s: float) -> bytes
```

**Key Differences from Kotlin:**
- No jSerialComm dependency
- Uses `os.open()` with `O_NONBLOCK`
- `select.select()` for non-blocking reads
- `termios.tcdrain()` for write completion

#### 2. `ehl_protocol.py` - EHL Framing
```python
STX_CONTROLLER = 0x10
STX_DISPENSER = 0x20
ETX = 0x36

def build_frame(addr, cmd, data) -> bytes
def extract_frames(stream_bytes) -> tuple[list[EhlFrame], bytes]
```

**Matches Kotlin:**
- ✅ Same STX/ETX values
- ✅ Same checksum algorithm (XOR)
- ✅ Same frame structure

#### 3. `01_probe_readonly.py` - Test Script
```python
# Sends read-only commands to real dispenser:
commands = [
    ("STATE", 0x4B),
    ("ERROR_QUERY", 0x4C),
    ("VOLUME", 0x45),
    ("TANKBIT", 0xC5)
]
```

**Purpose:**
- Validate communication with real hardware
- Compare responses with Kotlin implementation
- Debug parity/framing issues

---

## 🔍 Python vs Kotlin Transport Comparison

| Aspect | Python (serial_linux.py) | Kotlin (SerialPortManager.kt) |
|--------|--------------------------|-------------------------------|
| **Parity** | 8N1 (No parity) | 8E1 (Even parity) |
| **Read Strategy** | `select()` + non-blocking | Blocking with timeout |
| **Write Strategy** | `write()` + `tcdrain()` | Retry logic (up to 3x) |
| **Timeout** | 800ms (default) | 3000ms read, 1000ms write |
| **RS-485 Mode** | Optional Linux ioctl | Not explicitly set |

**Critical Finding:**
- Python uses **8N1** (no parity)
- Kotlin webapp uses **8E1** (even parity)
- **Simulator uses 8N1** (SerialPortHandler.kt line 47)

This means:
- ✅ Python ↔ Simulator: Compatible (both 8N1)
- ⚠️ Kotlin Webapp ↔ Simulator: **PARITY MISMATCH** if using default config

---

## 🧪 Testing Workflow

### With Python + Simulator
```bash
# Terminal 1: Start simulator + socat
./scripts/start-socat-sim.sh

# Terminal 2: Run Python test
cd python-test
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1 --debug
```

**Expected Result:**
```
TX 10 06 01 4B 5C 36  (ADDR=1 CMD=0x4B)
⬅️  RX EHL: addr=1 cmd=STATE (0x4B) dataLen=0
➡️  TX EHL: addr=1 cmd=STATE (0x4B) dataLen=4
RX STX=0x20 LEN=10 ADDR=1 CMD=0x4B(STATE) DATA=...
```

### With Kotlin Webapp + Simulator
```bash
# Terminal 1: Start simulator + socat
./scripts/start-socat-sim.sh

# Terminal 2: Run webapp
java -jar release/lpg-ehl-webapp.jar \
    --spring.config.location=file:./application-h2.yaml \
    --lpg.mode=FIELD \
    --ehl.serial.port=/tmp/vserial1

# Browser: http://localhost:8080/control
```

---

## 🔧 Key Configuration Files

### Backend (Kotlin)
- `application.yaml` - Main config (default = LAB mode)
- `application-h2.yaml` - H2 database + FIELD mode testing
- `application-field.yaml` - Production config (PostgreSQL)

### Frontend (React)
- `.env.local` - Local API endpoint override
- `vite.config.ts` - Dev server proxy config

### Python
- No config files - all CLI arguments

---

## 📊 Database Schema (Liquibase)

Key Tables:
- `transactions` - Completed fuel deliveries
- `dispenser_status` - Real-time pump state
- `pump_authorization` - Payment authorization workflow
- `price_history` - Price changes over time
- `azure_sync_queue` - Pending sync items

Migrations: `lpg-ehl-service/src/main/resources/db/changelog/changes/`

---

## 🚀 Build & Deploy

### Build Everything
```bash
# Backend (creates release/*.jar)
./mvnw clean package -DskipTests

# Frontend (creates lpg-web/dist/)
cd lpg-web && npm run build

# Copy frontend to webapp
cp -r lpg-web/dist/* lpg-ehl-webapp/src/main/resources/static/
```

### Run Locally
```bash
# LAB mode (in-memory emulator)
java -jar release/lpg-ehl-webapp.jar

# FIELD mode (with simulator)
./scripts/start-socat-sim.sh  # Terminal 1
java -jar release/lpg-ehl-webapp.jar --lpg.mode=FIELD --ehl.serial.port=/tmp/vserial1
```

---

## 🎯 Analysis Focus Areas

### For AI Analysis:
1. **Protocol Correctness**: Compare Python vs Kotlin EHL implementation
2. **Transport Layer**: Identify parity mismatch and propose fix
3. **State Machine**: Review transaction lifecycle (PumpStateService)
4. **Error Handling**: Watchdog, reconnect, checksum failures
5. **Testing Strategy**: How Python scripts validate Kotlin code
6. **Architecture**: Monorepo structure, module dependencies
7. **Production Readiness**: Headless vs webapp deployment

### Questions to Answer:
- Why does Python work with simulator but Kotlin might not?
- Should we standardize on 8N1 or 8E1 parity?
- Can we auto-detect parity from hardware responses?
- Should Python tests be automated in CI/CD?

---

## 📝 Recent Changes (from chat history)

**2026-01-29**: Fixed `/control` page in FIELD mode
- Removed `@ConditionalOnProperty` from `PumpController` 
- Made `SyncController` dependencies optional
- Added null-safety to React frontend (`?? 0`)
- Now all API endpoints work in both LAB and FIELD modes

---

## 🔗 Key Documentation Files

Inside this archive:
- `README.md` - Project overview
- `docs/ARCHITECTURE.md` - System design
- `docs/SERIAL_CONTRACT.md` - Transport layer spec
- `lpg-ehl-core/WARP.md` - Core module rules for AI
- `python-test/*.py` - Python test scripts

---

**End of AI Analysis Context**

For detailed analysis, cross-reference:
1. Python serial implementation (`serial_linux.py`) with Kotlin `SerialPortManager.kt`
2. Python EHL protocol (`ehl_protocol.py`) with Kotlin `EhlCodec.kt`
3. Test script behavior (`01_probe_readonly.py`) with webapp control panel
4. Simulator configuration (`SerialPortHandler.kt`) with transport config

Happy analyzing! 🚀
EOF

# Replace timestamp in README
sed -i '' "s/\$(date +\"%Y-%m-%d %H:%M:%S\")/$TIMESTAMP/" "$OUTPUT_DIR/AI-ANALYSIS-README-${TIMESTAMP}.md"

# Create combined archive with README
echo ""
echo "📦 Creating combined AI analysis archive..."
COMBINED_FILE="$OUTPUT_DIR/AI-ANALYSIS-COMPLETE-${TIMESTAMP}.zip"

cd "$PROJECT_ROOT"
zip -q -r "$COMBINED_FILE" \
    "$OUTPUT_DIR/AI-ANALYSIS-README-${TIMESTAMP}.md" \
    "lpg-ehl-core/src/" \
    "lpg-ehl-core/pom.xml" \
    "lpg-ehl-core/WARP.md" \
    "lpg-ehl-service/src/" \
    "lpg-ehl-service/pom.xml" \
    "lpg-transport/src/" \
    "lpg-transport/pom.xml" \
    "lpg-ehl-webapp/src/" \
    "lpg-ehl-webapp/pom.xml" \
    "lpg-ehl-app-headless/src/" \
    "lpg-ehl-app-headless/pom.xml" \
    "lpg-ehl-serialport-sim/src/" \
    "lpg-ehl-serialport-sim/pom.xml" \
    "lpg-web/src/" \
    "lpg-web/package.json" \
    "lpg-web/tsconfig.json" \
    "python-test/*.py" \
    "python-test/README.md" \
    "README.md" \
    "pom.xml" \
    "docs/*.md" \
    "scripts/*.sh" \
    -x "*.git/*" \
    -x "*target/*" \
    -x "*node_modules/*" \
    -x "*dist/*" \
    -x "*build/*" \
    -x "*.class" \
    -x "*.pyc" \
    -x "*__pycache__/*"

COMBINED_SIZE=$(du -h "$COMBINED_FILE" | cut -f1)
echo "   ✅ AI-ANALYSIS-COMPLETE-${TIMESTAMP}.zip ($COMBINED_SIZE)"

echo ""
echo "📋 Created files:"
ls -lh "$OUTPUT_DIR"/*-${TIMESTAMP}.* 2>/dev/null | awk '{print "   " $9 " (" $5 ")"}'

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✅ DONE! Archives created:"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📦 Individual Modules:"
echo "   • lpg-ehl-core-${TIMESTAMP}.zip"
echo "   • lpg-ehl-service-${TIMESTAMP}.zip"
echo "   • lpg-transport-${TIMESTAMP}.zip"
echo "   • lpg-ehl-emulator-${TIMESTAMP}.zip"
echo "   • lpg-ehl-serialport-sim-${TIMESTAMP}.zip"
echo "   • lpg-ehl-webapp-${TIMESTAMP}.zip"
echo "   • lpg-ehl-app-headless-${TIMESTAMP}.zip"
echo "   • lpg-ehl-cli-${TIMESTAMP}.zip"
echo "   • lpg-web-${TIMESTAMP}.zip"
echo "   • python-test-${TIMESTAMP}.zip"
echo ""
echo "📦 Combined Archive (recommended for AI upload):"
echo "   ✨ AI-ANALYSIS-COMPLETE-${TIMESTAMP}.zip ($COMBINED_SIZE)"
echo "      Contains: README + all source code + Python tests"
echo ""
echo "📝 Documentation:"
echo "   • AI-ANALYSIS-README-${TIMESTAMP}.md"
echo "      Comprehensive guide for AI analysis"
echo ""
echo "🎯 Next Steps:"
echo "   1. Upload AI-ANALYSIS-COMPLETE-${TIMESTAMP}.zip to ChatGPT"
echo "   2. Reference AI-ANALYSIS-README-${TIMESTAMP}.md for context"
echo "   3. Ask for analysis of Python ↔ Kotlin protocol integration"
echo ""
echo "📂 Location: $OUTPUT_DIR"
echo "═══════════════════════════════════════════════════════════"
