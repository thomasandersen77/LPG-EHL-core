#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# START ALL SIMULATORS – Socat + Terminal + PLS
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# JAR paths
RELEASE="$PROJECT_ROOT/release"
PAYMENT_TERMINAL_SIM_JAR="$RELEASE/payment-terminal-sim.jar"
PAYMENT_TERMINAL_GUI_JAR="$RELEASE/payment-terminal-gui.jar"
PLS_SIM_JAR="$RELEASE/pls-sim.jar"

# PIDs
SOCAT_PID=""
TERMINAL_PID=""
PLS_PID=""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

DO_BUILD=false
FIELD_MODE=false
GUI_ENABLED=false
TERMINAL_PORT=18080
TERMINAL_HEADLESS=false

show_help() {
  cat <<'EOF'
START ALL SIMULATORS – Socat + Terminal + PLS

Starter simulatorene for terminal/pumpe-integrasjon.

Default-modus (lokal utvikling):
  1. Socat       – virtuell seriell kobling (/tmp/vserial0 <-> /tmp/vserial1)
  2. PLS         – Pumpe-simulator (/tmp/vserial0)
  3. Terminal    – Payment Terminal Simulator (port 18080)

--field modus (ARK/edge – kun socat + PLS):
  1. Socat       – virtuell seriell kobling (/tmp/vserial0 <-> /tmp/vserial1)
  2. PLS         – Pumpe-simulator (/tmp/vserial0)

  Webapp startes separat:
    java -jar release/lpg-ehl-webapp.jar \
        --spring.profiles.active=field \
        --ehl.serial.port=/tmp/vserial1
  Eller via IntelliJ med "WebApp (FIELD - Auto-detect)" run config.

Port-fordeling:
  /tmp/vserial0  → PLS Simulator (pumpestyring)
  /tmp/vserial1  → Webapp / IntelliJ (FIELD)

Valgfrie parametre:
  --help, -h             Vis denne hjelpen
  --build                Bygg JARs først
  --field                ARK/edge-modus: kun socat + PLS (ingen terminal sim)
  --gui                  Aktiver GUI for PLS simulator
  --terminal-port=PORT   Terminal sim port (default: 18080)
  --terminal-headless    Headless terminal sim (ingen GUI)

Eksempler:
  ./scripts/start-all-simulators.sh                  # Alt (lokal dev)
  ./scripts/start-all-simulators.sh --field           # ARK/edge
  ./scripts/start-all-simulators.sh --field --gui     # ARK/edge med PLS GUI
  ./scripts/start-all-simulators.sh --build           # Bygg + start alt
  ./scripts/start-all-simulators.sh --terminal-headless

Stop: Ctrl+C
EOF
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    help|--help|-h) show_help ;;
    --build) DO_BUILD=true; shift ;;
    --field) FIELD_MODE=true; shift ;;
    --gui) GUI_ENABLED=true; shift ;;
    --terminal-port=*) TERMINAL_PORT="${1#*=}"; shift ;;
    --terminal-headless) TERMINAL_HEADLESS=true; shift ;;
    *) echo -e "${RED}Unknown option: $1${NC}"; show_help ;;
  esac
done

# Step count
TOTAL_STEPS=3
if [[ "$FIELD_MODE" == "true" ]]; then
    TOTAL_STEPS=2
fi

CLEANUP_DONE=false
cleanup() {
  if [[ "$CLEANUP_DONE" == "true" ]]; then
    return
  fi
  CLEANUP_DONE=true

  trap - EXIT INT TERM

  echo ""
  echo -e "${CYAN}🛑 Stopping all services...${NC}"

  # PLS Simulator
  if [ -n "${PLS_PID:-}" ] && kill -0 "$PLS_PID" 2>/dev/null; then
    kill "$PLS_PID" 2>/dev/null || true
    sleep 0.3
    echo -e "  ✓ PLS Simulator stopped"
  fi
  pkill -f "pls-sim\.jar" 2>/dev/null || true
  pkill -9 -f "pls-sim\.jar" 2>/dev/null || true

  # Payment Terminal
  if [ -n "${TERMINAL_PID:-}" ] && kill -0 "$TERMINAL_PID" 2>/dev/null; then
    kill "$TERMINAL_PID" 2>/dev/null || true
    sleep 0.3
    echo -e "  ✓ Payment Terminal Simulator stopped"
  fi
  pkill -f "payment-terminal-sim\.jar" 2>/dev/null || true
  pkill -f "payment-terminal-gui\.jar" 2>/dev/null || true

  # Socat
  if [ -n "${SOCAT_PID:-}" ] && kill -0 "$SOCAT_PID" 2>/dev/null; then
    kill "$SOCAT_PID" 2>/dev/null || true
    echo -e "  ✓ Socat stopped"
  fi

  rm -f /tmp/vserial0 /tmp/vserial1 2>/dev/null || true
  echo ""
  echo "Bye!"
}

on_int() { exit 130; }
on_term() { exit 143; }
trap on_int INT
trap on_term TERM
trap cleanup EXIT

# Check socat (auto-install on Debian)
if ! command -v socat &> /dev/null; then
  if [ -f /etc/debian_version ]; then
    echo -e "${YELLOW}socat ikke installert. Installerer automatisk på Debian...${NC}"
    if command -v apt-get &> /dev/null; then
      sudo apt-get update -qq
      sudo apt-get install -y socat
      echo -e "${GREEN}✓ socat installert${NC}"
    else
      echo -e "${RED}Kunne ikke finne apt-get for å installere socat${NC}"
      exit 1
    fi
  else
    echo -e "${RED}socat ikke installert. Kjør: brew install socat${NC}"
    exit 1
  fi
fi

# Build if requested or JARs missing
build_if_needed() {
  local missing=()
  [[ ! -f "$PLS_SIM_JAR" ]] && missing+=("pls-sim")

  if [[ "$FIELD_MODE" != "true" ]]; then
    if [[ "$TERMINAL_HEADLESS" == "true" ]]; then
      [[ ! -f "$PAYMENT_TERMINAL_SIM_JAR" ]] && missing+=("payment-terminal-sim")
    else
      [[ ! -f "$PAYMENT_TERMINAL_GUI_JAR" ]] && missing+=("payment-terminal-gui")
    fi
  fi

  if [[ ${#missing[@]} -gt 0 ]] || [[ "$DO_BUILD" == "true" ]]; then
    echo -e "${YELLOW}Bygger artifacts: ${missing[*]}${NC}"
    cd "$PROJECT_ROOT"
    if [[ -x "$PROJECT_ROOT/scripts/build-simulators.sh" ]]; then
      "$PROJECT_ROOT/scripts/build-simulators.sh" --skip-tests
    else
      ./build_monolith.sh --skip-tests
    fi
    cd - >/dev/null
  fi
}
build_if_needed

# Verify required jars exist
REQUIRED_JARS=("$PLS_SIM_JAR")
if [[ "$FIELD_MODE" != "true" ]]; then
  if [[ "$TERMINAL_HEADLESS" == "true" ]]; then
    REQUIRED_JARS+=("$PAYMENT_TERMINAL_SIM_JAR")
  else
    REQUIRED_JARS+=("$PAYMENT_TERMINAL_GUI_JAR")
  fi
fi

for j in "${REQUIRED_JARS[@]}"; do
  if [ ! -f "$j" ]; then
    echo -e "${RED}Manglende JAR: $j${NC}"
    echo -e "Kjør: ./scripts/build-simulators.sh"
    exit 1
  fi
done

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
if [[ "$FIELD_MODE" == "true" ]]; then
    echo -e "${BOLD}  🛢️  Start Simulators – FIELD mode (ARK/edge)${NC}"
else
    echo -e "${BOLD}  🛢️  Start All Simulators${NC}"
fi
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""

# 1. Socat
rm -f /tmp/vserial0 /tmp/vserial1
echo -e "${CYAN}[1/$TOTAL_STEPS] Starting socat...${NC}"
echo -e "      ${BOLD}/tmp/vserial0${NC}  ${GRAY}← PLS Simulator${NC}"
echo -e "      ${BOLD}/tmp/vserial1${NC}  ${GRAY}← Webapp${NC}"

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

PTY0=$(grep "N PTY is" "$SOCAT_OUTPUT" | head -1 | sed 's/.*PTY is //')
cat "$SOCAT_OUTPUT"
rm -f "$SOCAT_OUTPUT"

if [[ -e "$PTY0" ]]; then
    chmod 666 "$PTY0" 2>/dev/null || true
fi
echo -e "${GREEN}      ✓ Socat running (PID: $SOCAT_PID)${NC}"
echo ""

# 2. PLS Simulator
echo -e "${CYAN}[2/$TOTAL_STEPS] Starting PLS Simulator (pumpestyring)...${NC}"
echo -e "      Port: ${BOLD}/tmp/vserial0${NC}  (adresse 1)";

PLS_CMD=(java -Xms64m -Xmx64m -XX:+UseSerialGC
    -jar "$PLS_SIM_JAR"
    --port="$PTY0"
    --address=1
    --mode=ehl)
if [[ "$GUI_ENABLED" == "true" ]]; then
    PLS_CMD+=(--gui)
fi
"${PLS_CMD[@]}" &
PLS_PID=$!
sleep 2

if ! kill -0 "$PLS_PID" 2>/dev/null; then
    echo -e "${RED}PLS Simulator startet ikke${NC}"
    exit 1
fi
echo -e "${GREEN}      ✓ PLS Simulator running (PID: $PLS_PID)${NC}"
echo ""

# 3. Payment Terminal Simulator (only in default mode, skipped in --field)
if [[ "$FIELD_MODE" != "true" ]]; then
    terminal_jar="$PAYMENT_TERMINAL_GUI_JAR"
    terminal_mode="GUI"
    if [[ "$TERMINAL_HEADLESS" == "true" ]]; then
      terminal_jar="$PAYMENT_TERMINAL_SIM_JAR"
      terminal_mode="HEADLESS"
    fi

    echo -e "${CYAN}[3/$TOTAL_STEPS] Starting Payment Terminal Simulator (${terminal_mode})...${NC}"
    echo -e "      Port: ${BOLD}${TERMINAL_PORT}${NC}"

    # Ensure port is free
    if command -v lsof &> /dev/null; then
      if lsof -ti:"$TERMINAL_PORT" > /dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Port $TERMINAL_PORT is in use. Killing existing process...${NC}"
        lsof -ti:"$TERMINAL_PORT" | xargs kill -9 2>/dev/null || true
        sleep 1
      fi
    fi

    java -jar "$terminal_jar" --server.port="$TERMINAL_PORT" &
    TERMINAL_PID=$!
    sleep 2
    echo -e "${GREEN}      ✓ Terminal running (PID: $TERMINAL_PID)${NC}"
    echo -e "      ${GRAY}→ http://localhost:${TERMINAL_PORT}${NC}"
    echo ""
fi

# Ready
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  ✅ Klart${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""
if [[ "$FIELD_MODE" == "true" ]]; then
    echo -e "  ${BOLD}Field mode – start webapp separat:${NC}"
    echo -e "  java -jar release/lpg-ehl-webapp.jar \\"
    echo -e "      --spring.profiles.active=field \\"
    echo -e "      --ehl.serial.port=/tmp/vserial1"
    echo ""
    echo -e "  PLS: /tmp/vserial0  →  socat  →  /tmp/vserial1 (webapp)"
else
    echo -e "  ${BOLD}Flyt:${NC}"
    echo -e "  1. Åpne terminal i Payment Terminal Simulator"
    echo -e "  2. Trykk «Trekke kort» (scenario APPROVED)"
    echo -e "  3. PLS får UNBLOCK → trykk START i PLS GUI"
    echo -e "  4. Start webapp separat for å fullføre testen"
fi
echo ""
echo -e "  ${GRAY}Stop alt: Ctrl+C${NC}"
echo ""

# Keep alive
while kill -0 "$SOCAT_PID" 2>/dev/null && kill -0 "$PLS_PID" 2>/dev/null; do
    sleep 1
done
