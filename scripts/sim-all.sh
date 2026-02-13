#!/usr/bin/env bash
#═══════════════════════════════════════════════════════════════════════
# SIM ALL – Socat + Terminal + PLS
#═══════════════════════════════════════════════════════════════════════
#
# Starter simulatorene for terminal/pumpe-integrasjon:
#   1. Socat       – virtuell seriell kobling (vserial0 <-> vserial1)
#   2. Terminal    – Payment Terminal (headless default, GUI med --gui)
#   3. PLS         – Pumpe-simulator (GUI med --gui)
#
# Port-fordeling:
#   - /tmp/vserial0  → PLS Simulator (pumpestyring)
#   - /tmp/vserial1  → Webapp / IntelliJ (FIELD) kobler her
#   - Terminal HTTP  → 18080 (default)
#
# Usage:
#   ./scripts/sim-all.sh
#   ./scripts/sim-all.sh --gui
#   ./scripts/sim-all.sh --build
#   ./scripts/sim-all.sh --terminal-port=18080
#
# Stop:
#   Ctrl+C (dreper også eventuelle hengende pls-sim.jar prosesser)
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/release"

PLS_JAR="$RELEASE_DIR/pls-sim.jar"
TERMINAL_SIM_JAR="$RELEASE_DIR/payment-terminal-sim.jar"
TERMINAL_GUI_JAR="$RELEASE_DIR/payment-terminal-gui.jar"

DO_BUILD=false
GUI_ENABLED=false
TERMINAL_PORT=18080

# PLS defaults
DISPENSER_ADDRESS=1
PRICE_CENTS=1590
BAUD_RATE=9600
PARITY="NONE"
MODE="ehl"
LEGACY_ADDRESS="true"
BLOCKED="true"
CHUNK="false"
LATENCY_MS=0
LOG_HEX="true"
HEARTBEAT_INTERVAL_MS=60000
PROFILE="lab"

# Fault injection (optional)
DISCONNECT_AFTER_SECONDS=""
BAD_CHECKSUM_RATE=""
POWERFAULT_AFTER_SECONDS=""

FIELD_ARGS=()

SOCAT_PID=""
TERMINAL_PID=""
PLS_PID=""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

show_help() {
  cat <<'EOF'
SIM ALL – PLS + Payment Terminal + socat

Hva startes:
  1) socat: /tmp/vserial0 <-> /tmp/vserial1 (virtuell seriell link)
  2) Payment Terminal simulator (headless default, GUI med --gui)
  3) PLS simulator koblet til /tmp/vserial0 (via socat PTY)

Anbefalt oppstart (lokal dev):
  ./scripts/sim-all.sh
  # webapp/headless kobler til PLS via:
  #   --ehl.serial.port=/tmp/vserial1

Help (starter IKKE simulatorene):
  ./scripts/sim-all.sh --help
  ./scripts/sim-all.sh help

Obligatoriske parametre:
  (ingen)

Valgfrie parametre (utvalg, med defaults):
  --build                       Bygg simulator-JARs dersom de mangler
  --gui                         GUI for både terminal og PLS
  --terminal-port=<port>        Default: 18080

PLS options (videresendes til pls-sim.jar):
  --address=<1-8>               Default: 1
  --price=<cents>               Default: 1590
  --baud=<int>                  Default: 9600
  --parity=<NONE|EVEN|ODD>      Default: NONE
  --blocked=<true|false>        Default: true
  --legacy-address=<true|false> Default: true
  --profile=<lab|...>           Default: lab
  --mode=<line|stxetx|ehl>      Default: ehl
  --chunk=<true|false>          Default: false
  --latencyMs=<ms>              Default: 0
  --logHex=<true|false>         Default: true
  --heartbeatIntervalMs=<ms>    Default: 60000

Fault injection (valgfritt):
  --disconnectAfterSeconds=<sec>
  --badChecksumRate=<0.0-1.0>
  --powerfaultAfterSeconds=<sec>

Field-mode passthrough (valgfritt):
  --field.*   Alle args som starter med --field. sendes videre.

SOCAT logging:
  Socat kjøres med -x -v og skriver trafikk til en loggfil under logs/.

Stop:
  Ctrl+C (dreper også eventuelle hengende simulator-prosesser)
EOF
  exit 0
}

while [[ $# -gt 0 ]]; do
  case $1 in
    help) show_help ;;
    --help|-h) show_help ;;
    --build) DO_BUILD=true; shift ;;
    --gui) GUI_ENABLED=true; shift ;;
    --terminal-port=*) TERMINAL_PORT="${1#*=}"; shift ;;
    --address=*) DISPENSER_ADDRESS="${1#*=}"; shift ;;
    --price=*) PRICE_CENTS="${1#*=}"; shift ;;
    --baud=*) BAUD_RATE="${1#*=}"; shift ;;
    --parity=*) PARITY="${1#*=}"; shift ;;
    --blocked=*) BLOCKED="${1#*=}"; shift ;;
    --legacy-address=*) LEGACY_ADDRESS="${1#*=}"; shift ;;
    --profile=*) PROFILE="${1#*=}"; shift ;;
    --field.*) FIELD_ARGS+=("$1"); shift ;;
    --mode=*) MODE="${1#*=}"; shift ;;
    --chunk=*) CHUNK="${1#*=}"; shift ;;
    --latencyMs=*) LATENCY_MS="${1#*=}"; shift ;;
    --logHex=*) LOG_HEX="${1#*=}"; shift ;;
    --heartbeatIntervalMs=*) HEARTBEAT_INTERVAL_MS="${1#*=}"; shift ;;
    --disconnectAfterSeconds=*) DISCONNECT_AFTER_SECONDS="${1#*=}"; shift ;;
    --badChecksumRate=*) BAD_CHECKSUM_RATE="${1#*=}"; shift ;;
    --powerfaultAfterSeconds=*) POWERFAULT_AFTER_SECONDS="${1#*=}"; shift ;;
    *) echo "Unknown option: $1"; show_help ;;
  esac
done

cleanup() {
  echo ""
  echo -e "${CYAN}🛑 Stopping all simulators...${NC}"

  if [ -n "${PLS_PID:-}" ] && kill -0 "$PLS_PID" 2>/dev/null; then
    kill "$PLS_PID" 2>/dev/null || true
    sleep 0.3
  fi
  pkill -f "pls-sim\.jar" 2>/dev/null || true
  pkill -9 -f "pls-sim\.jar" 2>/dev/null || true

  if [ -n "${TERMINAL_PID:-}" ] && kill -0 "$TERMINAL_PID" 2>/dev/null; then
    kill "$TERMINAL_PID" 2>/dev/null || true
    sleep 0.3
  fi
  pkill -f "payment-terminal-sim\.jar" 2>/dev/null || true
  pkill -f "payment-terminal-gui\.jar" 2>/dev/null || true

  if [ -n "${SOCAT_PID:-}" ] && kill -0 "$SOCAT_PID" 2>/dev/null; then
    kill "$SOCAT_PID" 2>/dev/null || true
  fi

  rm -f /tmp/vserial0 /tmp/vserial1 2>/dev/null || true

  echo -e "${GRAY}Cleanup complete.${NC}"
}
trap cleanup EXIT SIGINT SIGTERM

if ! command -v socat &> /dev/null; then
  echo -e "${RED}socat ikke installert. Kjør: brew install socat${NC}"
  exit 1
fi

if [[ "$DO_BUILD" == "true" ]]; then
  echo -e "${YELLOW}Building simulator artifacts...${NC}"
  if [[ -x "$PROJECT_ROOT/scripts/build-simulators.sh" ]]; then
    "$PROJECT_ROOT/scripts/build-simulators.sh" --skip-tests
  else
    "$PROJECT_ROOT/build_monolith.sh" --skip-tests
  fi
fi

# Verify required jars
if [[ ! -f "$PLS_JAR" ]]; then
  echo -e "${RED}Missing JAR: $PLS_JAR${NC}"
  echo -e "${YELLOW}Build with: ./scripts/build-simulators.sh${NC}"
  exit 1
fi

TERMINAL_JAR="$TERMINAL_SIM_JAR"
if [[ "$GUI_ENABLED" == "true" ]]; then
  TERMINAL_JAR="$TERMINAL_GUI_JAR"
fi

if [[ ! -f "$TERMINAL_JAR" ]]; then
  echo -e "${RED}Missing JAR: $TERMINAL_JAR${NC}"
  echo -e "${YELLOW}Build with: ./scripts/build-simulators.sh${NC}"
  exit 1
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🛢️  Simulator stack${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""

# 1. socat
rm -f /tmp/vserial0 /tmp/vserial1
SOCAT_LOG_FILE="$PROJECT_ROOT/logs/socat-all-$(date +"%Y%m%d-%H%M%S").log"
mkdir -p "$PROJECT_ROOT/logs"

# -x -v: log TX/RX bytes (hexdump) on stderr
socat -d -d -x -v \
  pty,rawer,echo=0,link=/tmp/vserial0 \
  pty,rawer,echo=0,link=/tmp/vserial1 \
  2>>"$SOCAT_LOG_FILE" &
SOCAT_PID=$!
sleep 1

if [[ ! -e /tmp/vserial0 ]] || [[ ! -e /tmp/vserial1 ]]; then
  echo -e "${RED}Kunne ikke opprette virtuelle serial ports${NC}"
  echo -e "${GRAY}Se logg: $SOCAT_LOG_FILE${NC}"
  exit 1
fi

PTY0=$(readlink /tmp/vserial0 2>/dev/null || true)
if [[ -z "$PTY0" ]]; then
  PTY0=$(grep "PTY is" "$SOCAT_LOG_FILE" | head -1 | sed 's/.*PTY is //')
fi

if [[ -z "$PTY0" ]] || [[ ! -e "$PTY0" ]]; then
  echo -e "${RED}Fant ikke PTY-path for /tmp/vserial0${NC}"
  echo -e "${GRAY}Se logg: $SOCAT_LOG_FILE${NC}"
  exit 1
fi

chmod 666 "$PTY0" 2>/dev/null || true

echo -e "${GREEN}[1/3]✓${NC} socat running ${GRAY}(/tmp/vserial0 <-> /tmp/vserial1)${NC}"
echo -e "      ${GRAY}socat log: $SOCAT_LOG_FILE${NC}"

# 2. terminal
if lsof -ti:"$TERMINAL_PORT" > /dev/null 2>&1; then
  echo -e "${YELLOW}⚠️  Port $TERMINAL_PORT is in use. Killing existing process...${NC}"
  lsof -ti:"$TERMINAL_PORT" | xargs kill -9 2>/dev/null || true
  sleep 1
fi

echo -e "${CYAN}[2/3] Starting Payment Terminal ($([ "$GUI_ENABLED" = true ] && echo GUI || echo HEADLESS))...${NC}"
java -jar "$TERMINAL_JAR" --server.port="$TERMINAL_PORT" &
TERMINAL_PID=$!
sleep 1
if ! kill -0 "$TERMINAL_PID" 2>/dev/null; then
  echo -e "${RED}Terminal startet ikke${NC}"
  exit 1
fi

echo -e "${GREEN}      ✓ Terminal running (PID: $TERMINAL_PID)${NC}"
echo -e "      ${GRAY}→ http://localhost:$TERMINAL_PORT${NC}"

# 3. PLS
echo -e "${CYAN}[3/3] Starting PLS Simulator...${NC}"
PLS_CMD=(java -Xms64m -Xmx64m -XX:+UseSerialGC -Dsim.log.level=DEBUG -jar "$PLS_JAR")
PLS_CMD+=(--port="$PTY0")
PLS_CMD+=(--address="$DISPENSER_ADDRESS")
PLS_CMD+=(--mode="$MODE")
PLS_CMD+=(--price="$PRICE_CENTS")
PLS_CMD+=(--baud="$BAUD_RATE")
PLS_CMD+=(--parity="$PARITY")
PLS_CMD+=(--blocked="$BLOCKED")
PLS_CMD+=(--legacy-address="$LEGACY_ADDRESS")
PLS_CMD+=(--profile="$PROFILE")
PLS_CMD+=(--chunk="$CHUNK")
PLS_CMD+=(--latencyMs="$LATENCY_MS")
PLS_CMD+=(--logHex="$LOG_HEX")
PLS_CMD+=(--heartbeatIntervalMs="$HEARTBEAT_INTERVAL_MS")

if [[ -n "$DISCONNECT_AFTER_SECONDS" ]]; then
  PLS_CMD+=(--disconnectAfterSeconds="$DISCONNECT_AFTER_SECONDS")
fi
if [[ -n "$BAD_CHECKSUM_RATE" ]]; then
  PLS_CMD+=(--badChecksumRate="$BAD_CHECKSUM_RATE")
fi
if [[ -n "$POWERFAULT_AFTER_SECONDS" ]]; then
  PLS_CMD+=(--powerfaultAfterSeconds="$POWERFAULT_AFTER_SECONDS")
fi

PLS_CMD+=("${FIELD_ARGS[@]}")
if [[ "$GUI_ENABLED" == "true" ]]; then
  PLS_CMD+=(--gui)
fi
"${PLS_CMD[@]}" &
PLS_PID=$!
sleep 1
if ! kill -0 "$PLS_PID" 2>/dev/null; then
  echo -e "${RED}PLS Simulator startet ikke${NC}"
  exit 1
fi

echo -e "${GREEN}      ✓ PLS Simulator running (PID: $PLS_PID)${NC}"

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Klar${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Webapp/IntelliJ (FIELD): ${BOLD}--ehl.serial.port=/tmp/vserial1${NC}"
echo -e "  Stop alt: ${GRAY}Ctrl+C${NC}"

echo ""
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$PLS_PID" 2>/dev/null && kill -0 "$TERMINAL_PID" 2>/dev/null; do
  sleep 1
done
