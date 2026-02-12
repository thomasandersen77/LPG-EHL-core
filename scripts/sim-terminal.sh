#!/usr/bin/env bash
#═══════════════════════════════════════════════════════════════════════
# SIM TERMINAL – Payment Terminal Simulator
#═══════════════════════════════════════════════════════════════════════
#
# Starter betalingsterminalen enten:
#   - headless HTTP sim (default): release/payment-terminal-sim.jar
#   - GUI (web + visual):          release/payment-terminal-gui.jar  (flag: --gui)
#
# Usage:
#   ./scripts/sim-terminal.sh
#   ./scripts/sim-terminal.sh --gui
#   ./scripts/sim-terminal.sh --port=18080
#   ./scripts/sim-terminal.sh --build
#
# Stop:
#   Ctrl+C
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_ROOT/release"

# Defaults
HTTP_PORT=18080
GUI_ENABLED=false
DO_BUILD=false

SIM_JAR="$RELEASE_DIR/payment-terminal-sim.jar"
GUI_JAR="$RELEASE_DIR/payment-terminal-gui.jar"

SIM_PID=""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

show_help() {
  cat <<'EOF'
SIM TERMINAL – Payment Terminal Simulator

Hva startes:
  Payment terminal simulator (headless) eller GUI-variant.

Anbefalt oppstart (lokal dev):
  ./scripts/sim-terminal.sh
  # GUI:
  ./scripts/sim-terminal.sh --gui

Help (starter IKKE simulatoren):
  ./scripts/sim-terminal.sh --help
  ./scripts/sim-terminal.sh help

Obligatoriske parametre:
  (ingen)

Valgfrie parametre (med defaults):
  --port=<port>     Default: 18080
  --gui             Start GUI-variant (JavaFX)
  --build           Bygg simulator-JARs dersom de mangler

Stop:
  Ctrl+C
EOF
  exit 0
}

while [[ $# -gt 0 ]]; do
  case $1 in
    help) show_help ;;
    --help|-h) show_help ;;
    --port=*) HTTP_PORT="${1#*=}"; shift ;;
    --gui) GUI_ENABLED=true; shift ;;
    --build) DO_BUILD=true; shift ;;
    *) echo "Unknown option: $1"; show_help ;;
  esac
done

cleanup() {
  echo ""
  echo -e "${CYAN}🛑 Stopping terminal...${NC}"

  if [ -n "${SIM_PID:-}" ] && kill -0 "$SIM_PID" 2>/dev/null; then
    kill "$SIM_PID" 2>/dev/null || true
    sleep 0.4
  fi

  # fallback: kill any remaining jars
  pkill -f "payment-terminal-sim\.jar" 2>/dev/null || true
  pkill -f "payment-terminal-gui\.jar" 2>/dev/null || true

  # also kill any process occupying the port
  if lsof -ti:"$HTTP_PORT" > /dev/null 2>&1; then
    lsof -ti:"$HTTP_PORT" | xargs kill -9 2>/dev/null || true
  fi

  echo -e "${GRAY}Cleanup complete.${NC}"
}
trap cleanup EXIT SIGINT SIGTERM

if [[ "$DO_BUILD" == "true" ]]; then
  echo -e "${YELLOW}Building simulator artifacts...${NC}"
  if [[ -x "$PROJECT_ROOT/scripts/build-simulators.sh" ]]; then
    "$PROJECT_ROOT/scripts/build-simulators.sh" --skip-tests
  else
    "$PROJECT_ROOT/build_monolith.sh" --skip-tests
  fi
fi

JAR_TO_RUN="$SIM_JAR"
if [[ "$GUI_ENABLED" == "true" ]]; then
  JAR_TO_RUN="$GUI_JAR"
fi

if [[ ! -f "$JAR_TO_RUN" ]]; then
  echo -e "${RED}Missing JAR: $JAR_TO_RUN${NC}"
  echo -e "${YELLOW}Build with: ./scripts/build-simulators.sh${NC}"
  exit 1
fi

# Ensure port is free
if lsof -ti:"$HTTP_PORT" > /dev/null 2>&1; then
  echo -e "${YELLOW}⚠️  Port $HTTP_PORT is in use. Killing existing process...${NC}"
  lsof -ti:"$HTTP_PORT" | xargs kill -9 2>/dev/null || true
  sleep 1
fi

echo ""
echo -e "${CYAN}Starting Payment Terminal...${NC}"
echo -e "  Mode: ${BOLD}$([ "$GUI_ENABLED" = true ] && echo GUI || echo HEADLESS)${NC}"
echo -e "  Port: ${BOLD}$HTTP_PORT${NC}"
echo -e "  JAR:  ${GRAY}$JAR_TO_RUN${NC}"
echo ""

java -jar "$JAR_TO_RUN" --server.port="$HTTP_PORT" &
SIM_PID=$!
sleep 1

if ! kill -0 "$SIM_PID" 2>/dev/null; then
  echo -e "${RED}Terminal failed to start${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Terminal running (PID: $SIM_PID)${NC}"
echo -e "  ${GRAY}http://localhost:$HTTP_PORT${NC}"
echo -e "${GRAY}Stop: Ctrl+C${NC}"

wait $SIM_PID
