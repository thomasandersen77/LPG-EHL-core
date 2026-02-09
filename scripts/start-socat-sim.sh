#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# SOCAT + PLS SIMULATOR
#═══════════════════════════════════════════════════════════════════════
#
# Usage: ./scripts/start-socat-sim.sh [options]
#
# Dispenser Options:
#   --address=<1-8>       Dispenser address (default: 1)
#   --price=<cents>       Price in cents, e.g. 1590 = 15.90 kr/L (default: 1590)
#   --blocked=<bool>      Initial blocked state (default: true)
#   --legacy-address=<bool> Also respond to 32+address (default: true)
#
# Serial Options:
#   --baud=<rate>         Baud rate (default: 9600)
#   --parity=<type>       Parity: NONE, EVEN, ODD (default: NONE)
#   --mode=<mode>         Frame mode: line, stxetx, ehl (default: ehl)
#   --chunk=<bool>        Enable chunked responses (default: false)
#   --latencyMs=<ms>      Add latency jitter to read loop (default: 0)
#   --logHex=<bool>       Log raw bytes as hex (default: true)
#
# Logging Options:
#   --heartbeatIntervalMs=<ms>  Heartbeat log interval (default: 60000)
#
# Fault Injection (for testing error handling):
#   --disconnectAfterSeconds=<sec>  Disconnect after N seconds
#   --badChecksumRate=<rate>       Bad checksum rate 0.0-1.0 (default: 0.0)
#   --powerfaultAfterSeconds=<sec> Power fault after N seconds
#
# Other:
#   --help, -h            Show this help message
#
# Note: DEBUG logging is enabled by default to show RX/TX HEX bytes.
#
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SIM_JAR="$PROJECT_ROOT/release/pls-sim.jar"
BUILD_LOG="$PROJECT_ROOT/.build-sim.log"

# Default configuration
DISPENSER_ADDRESS=1
PRICE_CENTS=1590
BAUD_RATE=9600
PARITY="NONE"
BLOCKED="true"
LEGACY_ADDRESS="true"
MODE="ehl"
CHUNK="false"
LATENCY_MS=0
LOG_HEX="true"
HEARTBEAT_INTERVAL_MS=60000
DISCONNECT_AFTER_SECONDS=""
BAD_CHECKSUM_RATE=""
POWERFAULT_AFTER_SECONDS=""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

# Cleanup
SOCAT_PID=""
SIM_PID=""

show_help() {
    sed -n '2,36p' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --address=*) DISPENSER_ADDRESS="${1#*=}"; shift ;;
        --price=*) PRICE_CENTS="${1#*=}"; shift ;;
        --blocked=*) BLOCKED="${1#*=}"; shift ;;
        --legacy-address=*) LEGACY_ADDRESS="${1#*=}"; shift ;;
        --baud=*) BAUD_RATE="${1#*=}"; shift ;;
        --parity=*) PARITY="${1#*=}"; shift ;;
        --mode=*) MODE="${1#*=}"; shift ;;
        --chunk=*) CHUNK="${1#*=}"; shift ;;
        --latencyMs=*) LATENCY_MS="${1#*=}"; shift ;;
        --logHex=*) LOG_HEX="${1#*=}"; shift ;;
        --heartbeatIntervalMs=*) HEARTBEAT_INTERVAL_MS="${1#*=}"; shift ;;
        --disconnectAfterSeconds=*) DISCONNECT_AFTER_SECONDS="${1#*=}"; shift ;;
        --badChecksumRate=*) BAD_CHECKSUM_RATE="${1#*=}"; shift ;;
        --powerfaultAfterSeconds=*) POWERFAULT_AFTER_SECONDS="${1#*=}"; shift ;;
        --help|-h) show_help ;;
        *) echo "Unknown option: $1"; show_help ;;
    esac
done

cleanup() {
    echo ""
    echo -e "${CYAN}🛑 Stopping services...${NC}"
    
    # Kill simulator by PID first
    if [ -n "${SIM_PID:-}" ] && kill -0 $SIM_PID 2>/dev/null; then
        kill $SIM_PID 2>/dev/null || true
        sleep 0.5
    fi
    
    # Force kill any remaining pls-sim.jar processes
    if pgrep -f "pls-sim.jar" > /dev/null 2>&1; then
        pkill -f "pls-sim.jar" 2>/dev/null || true
        sleep 0.5
    fi
    
    # Verify all pls-sim.jar processes are dead
    if pgrep -f "pls-sim.jar" > /dev/null 2>&1; then
        echo -e "${YELLOW}  ⚠️  Force killing remaining pls-sim.jar processes...${NC}"
        pkill -9 -f "pls-sim.jar" 2>/dev/null || true
    fi
    echo "  ✓ Simulator stopped"
    
    # Kill socat
    if [ -n "${SOCAT_PID:-}" ] && kill -0 $SOCAT_PID 2>/dev/null; then
        kill $SOCAT_PID 2>/dev/null || true
        echo "  ✓ socat stopped"
    fi
    
    # Cleanup virtual serial ports
    rm -f /tmp/vserial0 /tmp/vserial1 /dev/cu.vserial0 /dev/cu.vserial1 "$BUILD_LOG" 2>/dev/null || \
        sudo rm -f /dev/cu.vserial0 /dev/cu.vserial1 2>/dev/null || true
    echo ""
    echo "Cleanup complete. Bye!"
}
trap cleanup EXIT SIGINT SIGTERM

# Check socat
if ! command -v socat &> /dev/null; then
    echo -e "${RED}socat ikke installert. Kjør: brew install socat${NC}"
    exit 1
fi

# Build JAR if missing
if [[ ! -f "$SIM_JAR" ]]; then
    echo -n -e "${GRAY}Bygger simulator...${NC} "
    cd "$PROJECT_ROOT"
    if ! ./mvnw -q -DskipTests package -pl lpg-ehl-serialport-sim -am > "$BUILD_LOG" 2>&1; then
        echo -e "${RED}FEILET${NC}"
        echo ""
        tail -20 "$BUILD_LOG"
        exit 1
    fi
    cp lpg-ehl-serialport-sim/target/lpg-ehl-serialport-sim-*.jar "$SIM_JAR"
    echo -e "${GREEN}✓${NC}"
fi

# Cleanup old PTYs
rm -f /tmp/vserial0 /tmp/vserial1

echo ""
echo -e "${CYAN}[1/2] Starting socat...${NC}"
echo -e "      Port 0: ${BOLD}/tmp/vserial0${NC}  ${GRAY}← Simulator${NC}"
echo -e "      Port 1: ${BOLD}/tmp/vserial1${NC}  ${GRAY}← Python/Webapp${NC}"
echo ""

# Start socat and capture PTY device names
SOCAT_OUTPUT=$(mktemp)
socat -d -d \
    pty,rawer,echo=0,link=/tmp/vserial0 \
    pty,rawer,echo=0,link=/tmp/vserial1 \
    2>"$SOCAT_OUTPUT" &

SOCAT_PID=$!
sleep 1

if [[ ! -e /tmp/vserial0 ]] || [[ ! -e /tmp/vserial1 ]]; then
    echo -e "${RED}Kunne ikke opprette virtuelle serial ports${NC}"
    cat "$SOCAT_OUTPUT"
    rm -f "$SOCAT_OUTPUT"
    exit 1
fi

# Extract actual PTY device paths from socat output
PTY0=$(grep "N PTY is" "$SOCAT_OUTPUT" | head -1 | sed 's/.*PTY is //')
PTY1=$(grep "N PTY is" "$SOCAT_OUTPUT" | tail -1 | sed 's/.*PTY is //')

# Show socat output for debugging
cat "$SOCAT_OUTPUT"
rm -f "$SOCAT_OUTPUT"

if [[ -z "$PTY0" ]] || [[ -z "$PTY1" ]]; then
    echo -e "${RED}Failed to detect PTY device paths${NC}"
    exit 1
fi

# On macOS, jSerialComm can't enumerate PTY devices created by socat,
# but it CAN open them if given the exact path. Just use the PTY directly.
CU_PORT0="$PTY0"
CU_PORT1="$PTY1"

# Fix permissions on PTY devices (jSerialComm needs read access)
if [[ -e "$PTY0" ]]; then
    chmod 666 "$PTY0" 2>/dev/null || echo -e "${YELLOW}Warning: Could not set permissions on $PTY0${NC}"
fi
if [[ -e "$PTY1" ]]; then
    chmod 666 "$PTY1" 2>/dev/null || echo -e "${YELLOW}Warning: Could not set permissions on $PTY1${NC}"
fi

echo -e "${GREEN}      ✓ socat running (PID: $SOCAT_PID)${NC}"
echo -e "${GRAY}      Dev PTYs:       $PTY0 <-> $PTY1${NC}"
echo -e "${GRAY}      Symlinks:       /tmp/vserial0 <-> /tmp/vserial1${NC}"
echo -e "${YELLOW}      Production:     /dev/ttyS3 (physical pump hardware)${NC}"
echo ""
LEGACY_ADDR=$((32 + DISPENSER_ADDRESS))
echo -e "${CYAN}[2/2] Starting PLS Simulator...${NC}"
echo -e "      Address:      ${BOLD}$DISPENSER_ADDRESS${NC} ${GRAY}(legacy: $LEGACY_ADDR = $LEGACY_ADDRESS)${NC}"
echo -e "      Price:        ${BOLD}$(echo "scale=2; $PRICE_CENTS / 100" | bc) kr/L${NC}"
echo -e "      Baud:         ${BOLD}$BAUD_RATE${NC}"
echo -e "      Parity:       ${BOLD}$PARITY${NC}"
echo -e "      Data bits:    ${BOLD}8${NC}"
echo -e "      Stop bits:    ${BOLD}1${NC}"
echo -e "      Blocked:      ${BOLD}$BLOCKED${NC}"
echo -e "      Mode:         ${BOLD}$MODE${NC}"
echo -e "      Chunked:      ${BOLD}$CHUNK${NC}"
echo -e "      Log hex:      ${BOLD}$LOG_HEX${NC}"
if [[ -n "$DISCONNECT_AFTER_SECONDS" ]]; then
    echo -e "      ${YELLOW}⚠️  Disconnect:  ${BOLD}${DISCONNECT_AFTER_SECONDS}s${NC}"
fi
if [[ -n "$BAD_CHECKSUM_RATE" ]]; then
    echo -e "      ${YELLOW}⚠️  Bad checksum: ${BOLD}${BAD_CHECKSUM_RATE}${NC}"
fi
if [[ -n "$POWERFAULT_AFTER_SECONDS" ]]; then
    echo -e "      ${YELLOW}⚠️  Power fault:  ${BOLD}${POWERFAULT_AFTER_SECONDS}s${NC}"
fi
echo ""

# Build simulator command
# Use cu.* port for jSerialComm compatibility on macOS
# Cap heap at 128 MB for ARK-3360 compatibility
SIM_CMD="java -Xmx128m -Dsim.log.level=DEBUG -jar \"$SIM_JAR\""
SIM_CMD+=" --port=$CU_PORT0"
SIM_CMD+=" --address=$DISPENSER_ADDRESS"
SIM_CMD+=" --price=$PRICE_CENTS"
SIM_CMD+=" --baud=$BAUD_RATE"
SIM_CMD+=" --parity=$PARITY"
SIM_CMD+=" --blocked=$BLOCKED"
SIM_CMD+=" --legacy-address=$LEGACY_ADDRESS"
SIM_CMD+=" --mode=$MODE"
SIM_CMD+=" --chunk=$CHUNK"
SIM_CMD+=" --latencyMs=$LATENCY_MS"
SIM_CMD+=" --logHex=$LOG_HEX"
SIM_CMD+=" --heartbeatIntervalMs=$HEARTBEAT_INTERVAL_MS"

# Add fault injection if specified
if [[ -n "$DISCONNECT_AFTER_SECONDS" ]]; then
    SIM_CMD+=" --disconnectAfterSeconds=$DISCONNECT_AFTER_SECONDS"
fi
if [[ -n "$BAD_CHECKSUM_RATE" ]]; then
    SIM_CMD+=" --badChecksumRate=$BAD_CHECKSUM_RATE"
fi
if [[ -n "$POWERFAULT_AFTER_SECONDS" ]]; then
    SIM_CMD+=" --powerfaultAfterSeconds=$POWERFAULT_AFTER_SECONDS"
fi

# Start simulator with full logging to console
eval "$SIM_CMD" &

SIM_PID=$!
sleep 2

if ! kill -0 $SIM_PID 2>/dev/null; then
    echo -e "${RED}Simulator failed to start${NC}"
    exit 1
fi

echo -e "${GREEN}      ✓ Simulator running (PID: $SIM_PID)${NC}"
echo ""

# Ready message
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Klart for testing${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BOLD}Test med Alejandros Python script:${NC}"
echo -e "  cd python-test"
echo -e "  python3 01_probe_readonly.py --port /tmp/vserial1 --addr $DISPENSER_ADDRESS"
echo -e "  ${GRAY}# Eller test legacy adresse:${NC}"
echo -e "  python3 01_probe_readonly.py --port /tmp/vserial1 --addr $LEGACY_ADDR"
echo ""
echo -e "${BOLD}Test med REST API (scan adresser som Alejandros 02_scan_addresses.py):${NC}"
echo -e "  ${GRAY}# Start webapp først, deretter:${NC}"
echo -e "  curl -X POST \"http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=1&end=40\""
echo ""
echo -e "${BOLD}Start webapp (Web UI + REST API):${NC}"
echo -e "  ${GRAY}# Lab-modus (in-memory emulator):${NC}"
echo -e "  java -jar release/lpg-ehl-webapp.jar --spring.profiles.active=lab"
echo -e ""
echo -e "  ${GRAY}# Field-modus (SOCAT virtuell port - development):${NC}"
echo -e "  ./scripts/start-webapp-field.sh --port=/tmp/vserial1"
echo -e "  ${GRAY}# OR manually:${NC}"
echo -e "  java -jar release/lpg-ehl-webapp.jar \\"
echo -e "    --spring.profiles.active=field \\"
echo -e "    --ehl.serial.port=/tmp/vserial1 \\"
echo -e "    --ehl.serial.baud-rate=$BAUD_RATE \\"
echo -e "    --ehl.serial.data-bits=8 \\"
echo -e "    --ehl.serial.parity=$PARITY \\"
echo -e "    --ehl.serial.stop-bits=1"
echo -e ""
echo -e "  ${GRAY}# Field-modus med JVM-tuning:${NC}"
echo -e "  java -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \\"
echo -e "    -jar release/lpg-ehl-webapp.jar \\"
echo -e "    --spring.profiles.active=field \\"
echo -e "    --ehl.serial.port=/tmp/vserial1 \\"
echo -e "    --ehl.serial.baud-rate=$BAUD_RATE \\"
echo -e "    --ehl.serial.parity=$PARITY"
echo -e "  • GUI: ${CYAN}http://localhost:8080${NC}"
echo ""
echo -e "${BOLD}Start headless (Background Service):${NC}"
echo -e "  ${GRAY}# Lab-modus (in-memory emulator):${NC}"
echo -e "  java -jar release/lpg-ehl-headless.jar --spring.profiles.active=lab"
echo -e ""
echo -e "  ${GRAY}# Field-modus (SOCAT virtuell port - development):${NC}"
echo -e "  ./scripts/start-headless-field.sh --port=/tmp/vserial1"
echo -e "  ${GRAY}# OR with debug API:${NC}"
echo -e "  java -jar release/lpg-ehl-headless.jar \\"
echo -e "    --spring.profiles.active=field,debug-api \\"
echo -e "    --ehl.serial.port=/tmp/vserial1 \\"
echo -e "    --ehl.serial.baud-rate=$BAUD_RATE \\"
echo -e "    --ehl.serial.parity=$PARITY"
echo -e ""
echo -e "  ${GRAY}# Field-modus med JVM-tuning:${NC}"
echo -e "  java -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \\"
echo -e "    -XX:+HeapDumpOnOutOfMemoryError \\"
echo -e "    -jar release/lpg-ehl-headless.jar \\"
echo -e "    --spring.profiles.active=field \\"
echo -e "    --ehl.serial.port=/tmp/vserial1 \\"
echo -e "    --ehl.serial.baud-rate=$BAUD_RATE \\"
echo -e "    --ehl.serial.data-bits=8 \\"
echo -e "    --ehl.serial.parity=$PARITY \\"
echo -e "    --ehl.serial.stop-bits=1"
echo ""
echo -e "${YELLOW}──────────────────────────────────────────────────────────────${NC}"
echo -e "${BOLD}For PRODUCTION deployment on ARK-3360 hardware:${NC}"
echo -e "  ${GRAY}# Use production scripts with /dev/ttyS3 defaults:${NC}"
echo -e "  ./scripts/start-webapp-production.sh      ${GRAY}# Webapp with GUI${NC}"
echo -e "  ./scripts/start-headless-production.sh    ${GRAY}# Headless background service${NC}"
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GRAY}Logs will appear below. Press Ctrl+C to stop.${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo ""

# Keep script alive - wait for any background process
# Cleanup will be called automatically by EXIT trap
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$SIM_PID" 2>/dev/null; do
    sleep 1
done
