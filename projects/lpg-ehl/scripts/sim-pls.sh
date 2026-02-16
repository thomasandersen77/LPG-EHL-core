#!/usr/bin/env bash
#═══════════════════════════════════════════════════════════════════════
# SIM PLS – Socat + PLS (pumpestyring) simulator
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/release"
PLS_JAR="$RELEASE_DIR/pls-sim.jar"

LOG_DIR="$PROJECT_ROOT/logs"
mkdir -p "$LOG_DIR"

# Defaults
DO_BUILD=false
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
GUI_ENABLED="false"
PROFILE="lab"

# Fault injection (optional)
DISCONNECT_AFTER_SECONDS=""
BAD_CHECKSUM_RATE=""
POWERFAULT_AFTER_SECONDS=""

# Field-mode knobs (optional)
FIELD_ARGS=()

# JVM defaults
SIM_XMS="${SIM_XMS:-64m}"
SIM_XMX="${SIM_XMX:-64m}"
SIM_GC="${SIM_GC:-serial}"         # serial | g1
SIM_JAVA_OPTS="${SIM_JAVA_OPTS:-}" # extra JVM flags

# PIDs
SOCAT_PID=""
SIM_PID=""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

show_help() {
  cat <<'EOF'
SIM PLS – PLS (pumpestyring) simulator + socat

Hva startes:
  1) socat: /tmp/vserial0 <-> /tmp/vserial1 (virtuell seriell link)
  2) PLS-simulator (pls-sim.jar) koblet til /tmp/vserial0 (via socat PTY)

Anbefalt oppstart (lokal dev):
  ./scripts/sim-pls.sh
  # webapp/headless kobler til:
  #   --ehl.serial.port=/tmp/vserial1

Help (starter IKKE simulatorene):
  ./scripts/sim-pls.sh --help
  ./scripts/sim-pls.sh help

Obligatoriske parametre:
  (ingen)

Valgfrie parametre (med defaults):
  --build                          Bygg simulator-JARs dersom de mangler
  --gui                            Start JavaFX GUI (dødmannsknapp)
  --address=<1-8>                  Default: 1
  --price=<cents>                  Default: 1590  (15.90 kr/L)
  --baud=<int>                     Default: 9600
  --parity=<NONE|EVEN|ODD>         Default: NONE
  --blocked=<true|false>           Default: true
  --legacy-address=<true|false>    Default: true
  --profile=<lab|...>              Default: lab
  --mode=<line|stxetx|ehl>         Default: ehl
  --chunk=<true|false>             Default: false
  --latencyMs=<ms>                 Default: 0
  --logHex=<true|false>            Default: true
  --heartbeatIntervalMs=<ms>       Default: 60000

Fault injection (valgfritt):
  --disconnectAfterSeconds=<sec>
  --badChecksumRate=<0.0-1.0>
  --powerfaultAfterSeconds=<sec>

SOCAT logging:
  Socat kjøres med -x -v og skriver trafikk til en loggfil under logs/.

Stop:
  Ctrl+C (scriptet prøver også å drepe hengende pls-sim.jar prosesser)

ARK/Debian notat (hardware):
  På ARK-3360 forventes ofte /dev/ttyS0..3 og evt. /dev/ttyUSB0.. . Hvis de
  "mangler", sjekk f.eks.:
    ls -la /dev/ttyS* /dev/ttyUSB* 2>/dev/null
EOF
  exit 0
}

while [[ $# -gt 0 ]]; do
  case $1 in
    help) show_help ;;
    --help|-h) show_help ;;
    --build) DO_BUILD=true; shift ;;
    --address=*) DISPENSER_ADDRESS="${1#*=}"; shift ;;
    --price=*) PRICE_CENTS="${1#*=}"; shift ;;
    --baud=*) BAUD_RATE="${1#*=}"; shift ;;
    --parity=*) PARITY="${1#*=}"; shift ;;
    --blocked=*) BLOCKED="${1#*=}"; shift ;;
    --legacy-address=*) LEGACY_ADDRESS="${1#*=}"; shift ;;
    --mode=*) MODE="${1#*=}"; shift ;;
    --chunk=*) CHUNK="${1#*=}"; shift ;;
    --latencyMs=*) LATENCY_MS="${1#*=}"; shift ;;
    --logHex=*) LOG_HEX="${1#*=}"; shift ;;
    --heartbeatIntervalMs=*) HEARTBEAT_INTERVAL_MS="${1#*=}"; shift ;;
    --disconnectAfterSeconds=*) DISCONNECT_AFTER_SECONDS="${1#*=}"; shift ;;
    --badChecksumRate=*) BAD_CHECKSUM_RATE="${1#*=}"; shift ;;
    --powerfaultAfterSeconds=*) POWERFAULT_AFTER_SECONDS="${1#*=}"; shift ;;
    --profile=*) PROFILE="${1#*=}"; shift ;;
    --field.*) FIELD_ARGS+=("$1"); shift ;;
    --gui) GUI_ENABLED="true"; shift ;;
    --simXms=*) SIM_XMS="${1#*=}"; shift ;;
    --simXmx=*) SIM_XMX="${1#*=}"; shift ;;
    --simGc=*) SIM_GC="${1#*=}"; shift ;;
    --simJavaOpts=*) SIM_JAVA_OPTS="${1#*=}"; shift ;;
    *) echo "Unknown option: $1"; show_help ;;
  esac
done

cleanup() {
  echo ""
  echo -e "${CYAN}🛑 Stopping PLS simulator...${NC}"

  if [ -n "${SIM_PID:-}" ] && kill -0 "$SIM_PID" 2>/dev/null; then
    kill "$SIM_PID" 2>/dev/null || true
    sleep 0.4
  fi

  # IMPORTANT: kill any leftover instances
  pkill -f "pls-sim.jar" 2>/dev/null || true
  pkill -9 -f "pls-sim.jar" 2>/dev/null || true

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

if [[ ! -f "$PLS_JAR" ]] || [[ "$DO_BUILD" == "true" ]]; then
  echo -e "${YELLOW}Building simulator artifacts...${NC}"
  if [[ -x "$PROJECT_ROOT/scripts/build-simulators.sh" ]]; then
    "$PROJECT_ROOT/scripts/build-simulators.sh" --skip-tests
  else
    echo -e "${YELLOW}scripts/build-simulators.sh not found yet; falling back to build_monolith.sh${NC}"
    "$PROJECT_ROOT/build_monolith.sh" --skip-tests
  fi
fi

if [[ ! -f "$PLS_JAR" ]]; then
  echo -e "${RED}Missing JAR: $PLS_JAR${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ⛽ PLS Simulator (SOCAT)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Start socat (log payload + resolve actual PTY)
rm -f /tmp/vserial0 /tmp/vserial1
SOCAT_LOG_FILE="$LOG_DIR/socat-pls-$(date +"%Y%m%d-%H%M%S").log"

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

# /tmp/vserial0 er vanligvis en symlink til den faktiske PTY-pathen
PTY0=$(readlink /tmp/vserial0 2>/dev/null || true)
if [[ -z "$PTY0" ]]; then
  # fallback hvis readlink ikke ga noe
  PTY0=$(grep "PTY is" "$SOCAT_LOG_FILE" | head -1 | sed 's/.*PTY is //')
fi

if [[ -z "$PTY0" ]] || [[ ! -e "$PTY0" ]]; then
  echo -e "${RED}Fant ikke PTY-path for /tmp/vserial0${NC}"
  echo -e "${GRAY}Se logg: $SOCAT_LOG_FILE${NC}"
  exit 1
fi

chmod 666 "$PTY0" 2>/dev/null || true

echo -e "${GREEN}✓ socat running${NC}  ${GRAY}(/tmp/vserial0 <-> /tmp/vserial1)${NC}"
echo -e "${GRAY}  socat log: $SOCAT_LOG_FILE${NC}"

# JVM flags
JVM_FLAGS=("-Xms${SIM_XMS}" "-Xmx${SIM_XMX}" "-XX:+ExitOnOutOfMemoryError" "-XX:+DisableExplicitGC" "-Djava.security.egd=file:/dev/urandom" "-XX:TieredStopAtLevel=1")
if [[ "$SIM_GC" == "g1" ]]; then
  JVM_FLAGS+=("-XX:+UseG1GC" "-XX:MaxGCPauseMillis=200")
else
  JVM_FLAGS+=("-XX:+UseSerialGC")
fi
read -r -a EXTRA_JVM <<< "${SIM_JAVA_OPTS}"

SIM_CMD=(java "${JVM_FLAGS[@]}" "${EXTRA_JVM[@]}" -Dsim.log.level=DEBUG -jar "$PLS_JAR")
SIM_CMD+=(--port="$PTY0")
SIM_CMD+=(--address="$DISPENSER_ADDRESS")
SIM_CMD+=(--price="$PRICE_CENTS")
SIM_CMD+=(--baud="$BAUD_RATE")
SIM_CMD+=(--parity="$PARITY")
SIM_CMD+=(--blocked="$BLOCKED")
SIM_CMD+=(--legacy-address="$LEGACY_ADDRESS")
SIM_CMD+=(--profile="$PROFILE")
SIM_CMD+=(--mode="$MODE")
SIM_CMD+=(--chunk="$CHUNK")
SIM_CMD+=(--latencyMs="$LATENCY_MS")
SIM_CMD+=(--logHex="$LOG_HEX")
SIM_CMD+=(--heartbeatIntervalMs="$HEARTBEAT_INTERVAL_MS")

if [[ -n "$DISCONNECT_AFTER_SECONDS" ]]; then
  SIM_CMD+=(--disconnectAfterSeconds="$DISCONNECT_AFTER_SECONDS")
fi
if [[ -n "$BAD_CHECKSUM_RATE" ]]; then
  SIM_CMD+=(--badChecksumRate="$BAD_CHECKSUM_RATE")
fi
if [[ -n "$POWERFAULT_AFTER_SECONDS" ]]; then
  SIM_CMD+=(--powerfaultAfterSeconds="$POWERFAULT_AFTER_SECONDS")
fi

SIM_CMD+=("${FIELD_ARGS[@]}")
if [[ "$GUI_ENABLED" == "true" ]]; then
  SIM_CMD+=(--gui)
fi

echo ""
echo -e "${CYAN}Starting PLS...${NC}"
echo -e "  Address:  ${BOLD}$DISPENSER_ADDRESS${NC}"
echo -e "  Price:    ${BOLD}$(awk "BEGIN { printf \"%.2f\", ${PRICE_CENTS}/100 }") kr/L${NC}"
echo -e "  Baud:     ${BOLD}$BAUD_RATE${NC}"
echo -e "  Parity:   ${BOLD}$PARITY${NC}"
echo -e "  Profile:  ${BOLD}$PROFILE${NC}"
echo -e "  GUI:      ${BOLD}$GUI_ENABLED${NC}"
echo -e "  Serial:   ${BOLD}/tmp/vserial1${NC} ${GRAY}(webapp/intellij connects here)${NC}"
echo ""

"${SIM_CMD[@]}" &
SIM_PID=$!
sleep 1

if ! kill -0 "$SIM_PID" 2>/dev/null; then
  echo -e "${RED}PLS Simulator startet ikke${NC}"
  exit 1
fi

echo -e "${GREEN}✓ PLS Simulator running (PID: $SIM_PID)${NC}"
echo -e "${GRAY}Stop alt: Ctrl+C${NC}"

# Keep alive
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$SIM_PID" 2>/dev/null; do
  sleep 1
done
