#!/usr/bin/env bash
#═══════════════════════════════════════════════════════════════════════
# BUILD SIMULATORS
#═══════════════════════════════════════════════════════════════════════
#
# Builds ONLY the simulator artifacts and copies them to ./release:
#   - release/pls-sim.jar
#   - release/payment-terminal-sim.jar
#   - release/payment-terminal-gui.jar
#
# Usage:
#   ./scripts/build-simulators.sh
#   ./scripts/build-simulators.sh --skip-tests
#   ./scripts/build-simulators.sh --verbose
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

SKIP_TESTS=true
VERBOSE=false

for arg in "$@"; do
  case $arg in
    --skip-tests) SKIP_TESTS=true ;;
    --with-tests) SKIP_TESTS=false ;;
    --verbose) VERBOSE=true ;;
  esac
done

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

MVN_ARGS=(clean package -pl projects/lpg-ehl/lpg-ehl-serialport-sim,projects/lpg-ehl/lpg-ehl-payment-terminal-sim,projects/lpg-ehl/lpg-ehl-payment-terminal-gui -am)
if [[ "$SKIP_TESTS" == "true" ]]; then
  MVN_ARGS+=(-DskipTests)
fi
if [[ "$VERBOSE" == "false" ]]; then
  MVN_ARGS+=(-q)
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🔨 Build simulators${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "  ${GRAY}Tests:${NC} $([ "$SKIP_TESTS" = true ] && echo Skipped || echo Enabled)"
echo ""

./mvnw "${MVN_ARGS[@]}"

echo ""
echo -e "${GRAY}Collecting artifacts...${NC}"

RELEASE_DIR="$PROJECT_ROOT/release"
mkdir -p "$RELEASE_DIR"

# PLS
PLS_SIM_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-serialport-sim/target" -name "pls-sim.jar" | head -1)
if [ -z "$PLS_SIM_JAR" ]; then
  PLS_SIM_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-serialport-sim/target" -name "lpg-ehl-serialport-sim-*.jar" -not -name "*-plain.jar" | head -1)
fi

# Terminal sim
PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-payment-terminal-sim/target" -name "payment-terminal-sim-exec.jar" | head -1)
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ]; then
  PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-payment-terminal-sim/target" -name "payment-terminal-sim.jar" | head -1)
fi
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ]; then
  PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-payment-terminal-sim/target" -name "lpg-ehl-payment-terminal-sim-*.jar" -not -name "*-plain.jar" | head -1)
fi

# Terminal GUI
PAYMENT_TERMINAL_GUI_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-payment-terminal-gui/target" -name "payment-terminal-gui.jar" | head -1)
if [ -z "$PAYMENT_TERMINAL_GUI_JAR" ]; then
  PAYMENT_TERMINAL_GUI_JAR=$(find "$PROJECT_ROOT/projects/lpg-ehl/lpg-ehl-payment-terminal-gui/target" -name "lpg-ehl-payment-terminal-gui-*.jar" -not -name "*-plain.jar" | head -1)
fi

if [ -z "$PLS_SIM_JAR" ] || [ ! -f "$PLS_SIM_JAR" ]; then
  echo -e "${RED}✗ Could not find PLS simulator jar in projects/lpg-ehl/lpg-ehl-serialport-sim/target${NC}"
  exit 1
fi
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ] || [ ! -f "$PAYMENT_TERMINAL_SIM_JAR" ]; then
  echo -e "${RED}✗ Could not find payment terminal sim jar in projects/lpg-ehl/lpg-ehl-payment-terminal-sim/target${NC}"
  exit 1
fi
if [ -z "$PAYMENT_TERMINAL_GUI_JAR" ] || [ ! -f "$PAYMENT_TERMINAL_GUI_JAR" ]; then
  echo -e "${RED}✗ Could not find payment terminal gui jar in projects/lpg-ehl/lpg-ehl-payment-terminal-gui/target${NC}"
  exit 1
fi

cp "$PLS_SIM_JAR" "$RELEASE_DIR/pls-sim.jar" && chmod +x "$RELEASE_DIR/pls-sim.jar"
cp "$PAYMENT_TERMINAL_SIM_JAR" "$RELEASE_DIR/payment-terminal-sim.jar" && chmod +x "$RELEASE_DIR/payment-terminal-sim.jar"
cp "$PAYMENT_TERMINAL_GUI_JAR" "$RELEASE_DIR/payment-terminal-gui.jar" && chmod +x "$RELEASE_DIR/payment-terminal-gui.jar"

echo ""
echo -e "${GREEN}✓ Done${NC}"
ls -lh "$RELEASE_DIR/pls-sim.jar" "$RELEASE_DIR/payment-terminal-sim.jar" "$RELEASE_DIR/payment-terminal-gui.jar" | awk '{print "  " $9 " (" $5 ")"}'
