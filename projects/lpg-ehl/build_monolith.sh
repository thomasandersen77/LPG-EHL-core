#!/bin/bash
# build_monolith.sh - Build LPG-EHL Applications
#
# Output: 
#   release/lpg-ehl-webapp.jar           - Web UI + REST API
#   release/lpg-ehl-headless.jar         - Background Service (+ debug-api profil)
#   release/pls-sim.jar                  - PLS Simulator (for socat testing)
#   release/payment-terminal-sim.jar     - Payment Terminal HTTP Simulator
#   release/payment-terminal-gui.jar     - Payment Terminal Visual Simulator GUI
#
# Usage:
#   ./build_monolith.sh              # Build all
#   ./build_monolith.sh --skip-tests # Skip tests
#   ./build_monolith.sh --verbose    # Show Maven output

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

# Parse arguments
SKIP_TESTS=false
VERBOSE=false
for arg in "$@"; do
    case $arg in
        --skip-tests) SKIP_TESTS=true ;;
        --verbose) VERBOSE=true ;;
    esac
done

# Paths
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # projects/lpg-ehl
REPO_ROOT="$PROJECT_DIR"

MVNW="$PROJECT_DIR/mvnw"
if [ ! -x "$MVNW" ]; then
    echo -e "${RED}✗ BUILD FAILED:${NC} Could not find executable mvnw at $MVNW"
    exit 1
fi

cd "$PROJECT_DIR"

BUILD_LOG="$PROJECT_DIR/.build.log"
BUILD_START=$(date +%s)

# Helper: Run Maven command (quiet unless error or verbose)
run_maven() {
    local description="$1"
    shift
    
    if [ "$VERBOSE" = true ]; then
        "$MVNW" "$@"
    else
        if ! "$MVNW" "$@" > "$BUILD_LOG" 2>&1; then
            echo ""
            echo -e "${RED}✗ BUILD FAILED: $description${NC}"
            echo ""
            echo -e "${YELLOW}Last 30 lines of output:${NC}"
            tail -30 "$BUILD_LOG"
            echo ""
            echo -e "${GRAY}Full log: $BUILD_LOG${NC}"
            exit 1
        fi
    fi
}

# Header
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  🛢️  LPG-EHL Build System${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${GRAY}Java:${NC}  $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
echo -e "  ${GRAY}Maven:${NC} $("$MVNW" -version 2>/dev/null | head -1 | cut -d' ' -f3)"
echo -e "  ${GRAY}Tests:${NC} $([ "$SKIP_TESTS" = true ] && echo 'Skipped' || echo 'Enabled')"
echo ""

# Step 1: Build React Frontend
echo -n -e "  ${GRAY}[1/4]${NC} Building React frontend... "
FRONTEND_DIR="$PROJECT_DIR/lpg-web"
WEBAPP_STATIC="$PROJECT_DIR/lpg-ehl-webapp/src/main/resources/static"

if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    npm install --silent > /dev/null 2>&1 || npm install > /dev/null 2>&1
    npm run build > /dev/null 2>&1
    
    rm -rf "$WEBAPP_STATIC"/* 2>/dev/null || true
    mkdir -p "$WEBAPP_STATIC"
    cp -r "$FRONTEND_DIR/dist/"* "$WEBAPP_STATIC/"
    STATIC_FILES=$(find "$WEBAPP_STATIC" -type f | wc -l | tr -d ' ')
    echo -e "${GREEN}✓${NC} ${GRAY}($STATIC_FILES files)${NC}"
    cd "$PROJECT_DIR"
else
    echo -e "${YELLOW}⚠ Skipped${NC}"
fi

# Step 2: Maven Build
echo -n -e "  ${GRAY}[2/4]${NC} Compiling all modules... "
MVN_ARGS="clean install"
[ "$SKIP_TESTS" = true ] && MVN_ARGS="$MVN_ARGS -DskipTests"

run_maven "Maven compile" $MVN_ARGS -q
echo -e "${GREEN}✓${NC}"

# Step 3: Package JARs
echo -n -e "  ${GRAY}[3/4]${NC} Packaging JAR files... "

PKG_ARGS="-DskipTests -q"

run_maven "WebApp package" package -pl lpg-ehl-webapp -am $PKG_ARGS
run_maven "Headless package" package -pl lpg-ehl-app-headless -am $PKG_ARGS
run_maven "PLS Sim package" package -pl lpg-ehl-serialport-sim -am $PKG_ARGS
run_maven "Payment Terminal Sim package" package -pl lpg-ehl-payment-terminal-sim -am $PKG_ARGS
run_maven "Payment Terminal GUI package" package -pl lpg-ehl-payment-terminal-gui -am $PKG_ARGS

echo -e "${GREEN}✓${NC}"

# Step 4: Create Release Artifacts
echo -n -e "  ${GRAY}[4/4]${NC} Creating release artifacts... "

RELEASE_DIR="$PROJECT_DIR/release"
mkdir -p "$RELEASE_DIR"

# Find and copy JARs
WEBAPP_JAR=$(find "$PROJECT_DIR/lpg-ehl-webapp/target" -name "lpg-ehl-webapp-*.jar" -not -name "*-plain.jar" | head -1)
HEADLESS_JAR=$(find "$PROJECT_DIR/lpg-ehl-app-headless/target" -name "lpg-ehl-app-headless-*.jar" -not -name "*-plain.jar" | head -1)
PLS_SIM_JAR=$(find "$PROJECT_DIR/lpg-ehl-serialport-sim/target" -name "pls-sim.jar" | head -1)
if [ -z "$PLS_SIM_JAR" ]; then
    # Fallback if naming changes in the future
    PLS_SIM_JAR=$(find "$PROJECT_DIR/lpg-ehl-serialport-sim/target" -name "lpg-ehl-serialport-sim-*.jar" -not -name "*-plain.jar" | head -1)
fi

PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_DIR/lpg-ehl-payment-terminal-sim/target" -name "payment-terminal-sim-exec.jar" | head -1)
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ]; then
    PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_DIR/lpg-ehl-payment-terminal-sim/target" -name "payment-terminal-sim.jar" | head -1)
fi
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ]; then
    PAYMENT_TERMINAL_SIM_JAR=$(find "$PROJECT_DIR/lpg-ehl-payment-terminal-sim/target" -name "lpg-ehl-payment-terminal-sim-*.jar" -not -name "*-plain.jar" | head -1)
fi

PAYMENT_TERMINAL_GUI_JAR=$(find "$PROJECT_DIR/lpg-ehl-payment-terminal-gui/target" -name "payment-terminal-gui.jar" | head -1)
if [ -z "$PAYMENT_TERMINAL_GUI_JAR" ]; then
    PAYMENT_TERMINAL_GUI_JAR=$(find "$PROJECT_DIR/lpg-ehl-payment-terminal-gui/target" -name "lpg-ehl-payment-terminal-gui-*.jar" -not -name "*-plain.jar" | head -1)
fi

# Require main artifacts
if [ -z "$WEBAPP_JAR" ] || [ ! -f "$WEBAPP_JAR" ]; then
    echo ""
    echo -e "${RED}✗ BUILD FAILED:${NC} Could not find WebApp JAR in lpg-ehl-webapp/target"
    exit 1
fi

if [ -z "$HEADLESS_JAR" ] || [ ! -f "$HEADLESS_JAR" ]; then
    echo ""
    echo -e "${RED}✗ BUILD FAILED:${NC} Could not find Headless JAR in lpg-ehl-app-headless/target"
    exit 1
fi

PLS_SIM_AVAILABLE=true
if [ -z "$PLS_SIM_JAR" ] || [ ! -f "$PLS_SIM_JAR" ]; then
    PLS_SIM_AVAILABLE=false
fi

PAYMENT_TERMINAL_SIM_AVAILABLE=true
if [ -z "$PAYMENT_TERMINAL_SIM_JAR" ] || [ ! -f "$PAYMENT_TERMINAL_SIM_JAR" ]; then
    PAYMENT_TERMINAL_SIM_AVAILABLE=false
fi

PAYMENT_TERMINAL_GUI_AVAILABLE=true
if [ -z "$PAYMENT_TERMINAL_GUI_JAR" ] || [ ! -f "$PAYMENT_TERMINAL_GUI_JAR" ]; then
    PAYMENT_TERMINAL_GUI_AVAILABLE=false
fi

cp "$WEBAPP_JAR" "$RELEASE_DIR/lpg-ehl-webapp.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-webapp.jar"
cp "$HEADLESS_JAR" "$RELEASE_DIR/lpg-ehl-headless.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-headless.jar"
if [ "$PLS_SIM_AVAILABLE" = true ]; then
    cp "$PLS_SIM_JAR" "$RELEASE_DIR/pls-sim.jar" && chmod +x "$RELEASE_DIR/pls-sim.jar"
fi
if [ "$PAYMENT_TERMINAL_SIM_AVAILABLE" = true ]; then
    cp "$PAYMENT_TERMINAL_SIM_JAR" "$RELEASE_DIR/payment-terminal-sim.jar" && chmod +x "$RELEASE_DIR/payment-terminal-sim.jar"
fi
if [ "$PAYMENT_TERMINAL_GUI_AVAILABLE" = true ]; then
    cp "$PAYMENT_TERMINAL_GUI_JAR" "$RELEASE_DIR/payment-terminal-gui.jar" && chmod +x "$RELEASE_DIR/payment-terminal-gui.jar"
fi

echo -e "${GREEN}✓${NC}"

# Build time
BUILD_END=$(date +%s)
BUILD_DURATION=$((BUILD_END - BUILD_START))
BUILD_TIME=$(printf "%d:%02d" $((BUILD_DURATION / 60)) $((BUILD_DURATION % 60)))

# Sizes
WEBAPP_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-webapp.jar" | cut -f1)
HEADLESS_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-headless.jar" | cut -f1)
if [ "$PLS_SIM_AVAILABLE" = true ]; then
    PLS_SIM_SIZE=$(du -h "$RELEASE_DIR/pls-sim.jar" | cut -f1)
else
    PLS_SIM_SIZE="(not built)"
fi
if [ "$PAYMENT_TERMINAL_SIM_AVAILABLE" = true ]; then
    PAYMENT_TERMINAL_SIM_SIZE=$(du -h "$RELEASE_DIR/payment-terminal-sim.jar" | cut -f1)
else
    PAYMENT_TERMINAL_SIM_SIZE="(not built)"
fi
if [ "$PAYMENT_TERMINAL_GUI_AVAILABLE" = true ]; then
    PAYMENT_TERMINAL_GUI_SIZE=$(du -h "$RELEASE_DIR/payment-terminal-gui.jar" | cut -f1)
else
    PAYMENT_TERMINAL_GUI_SIZE="(not built)"
fi

# Clean up build log on success
rm -f "$BUILD_LOG"

echo ""
echo -e "${GREEN}✓ BUILD COMPLETE${NC} ${GRAY}($BUILD_TIME)${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  📦 ARTIFACTS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${YELLOW}release/lpg-ehl-webapp.jar${NC}           ${GRAY}($WEBAPP_SIZE)${NC}"
echo -e "  ${GREEN}release/lpg-ehl-headless.jar${NC}         ${GRAY}($HEADLESS_SIZE)${NC}"
if [ "$PLS_SIM_AVAILABLE" = true ]; then
    echo -e "  ${CYAN}release/pls-sim.jar${NC}                  ${GRAY}($PLS_SIM_SIZE)${NC}"
else
    echo -e "  ${CYAN}release/pls-sim.jar${NC}                  ${GRAY}$PLS_SIM_SIZE${NC}"
fi
if [ "$PAYMENT_TERMINAL_SIM_AVAILABLE" = true ]; then
    echo -e "  ${CYAN}release/payment-terminal-sim.jar${NC}     ${GRAY}($PAYMENT_TERMINAL_SIM_SIZE)${NC}"
else
    echo -e "  ${CYAN}release/payment-terminal-sim.jar${NC}     ${GRAY}$PAYMENT_TERMINAL_SIM_SIZE${NC}"
fi
if [ "$PAYMENT_TERMINAL_GUI_AVAILABLE" = true ]; then
    echo -e "  ${CYAN}release/payment-terminal-gui.jar${NC}     ${GRAY}($PAYMENT_TERMINAL_GUI_SIZE)${NC}"
else
    echo -e "  ${CYAN}release/payment-terminal-gui.jar${NC}     ${GRAY}$PAYMENT_TERMINAL_GUI_SIZE${NC}"
fi
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🚀 BRUK${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BOLD}1. Start socat + simulator:${NC}"
echo -e "     ./scripts/start-socat-sim.sh"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo -e "${BOLD}  2. WEBAPP (Web UI + REST API)${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "  ${CYAN}Lab-modus (in-memory emulator):${NC}"
echo -e "    java -jar release/lpg-ehl-webapp.jar --spring.profiles.active=lab"
echo ""
echo -e "  ${CYAN}Field-modus (serialport):${NC}"
echo -e "    java -jar release/lpg-ehl-webapp.jar \\"
echo -e "      --spring.profiles.active=field \\"
echo -e "      --ehl.serial.port=/dev/ttyUSB0 \\"
echo -e "      --ehl.serial.baud-rate=9600 \\"
echo -e "      --ehl.serial.data-bits=8 \\"
echo -e "      --ehl.serial.parity=EVEN \\"
echo -e "      --ehl.serial.stop-bits=1"
echo ""
echo -e "  ${CYAN}Field-modus (SOCAT virtuell port):${NC}"
echo -e "    ./scripts/start-socat-sim.sh    ${GRAY}# Start først${NC}"
echo -e "    java -jar release/lpg-ehl-webapp.jar \\"
echo -e "      --spring.profiles.active=field \\"
echo -e "      --ehl.serial.port=/tmp/vserial1 \\"
echo -e "      --ehl.serial.parity=NONE"
echo ""
echo -e "  ${CYAN}Field-modus (produksjon med JVM-tuning):${NC}"
echo -e "    java -Xms256m -Xmx512m \\"
echo -e "      -XX:+UseG1GC -XX:MaxGCPauseMillis=100 \\"
echo -e "      -jar release/lpg-ehl-webapp.jar \\"
echo -e "      --spring.profiles.active=field \\"
echo -e "      --ehl.serial.port=/dev/ttyUSB0 \\"
echo -e "      --ehl.serial.baud-rate=9600 \\"
echo -e "      --ehl.serial.data-bits=8 \\"
echo -e "      --ehl.serial.parity=EVEN \\"
echo -e "      --ehl.serial.stop-bits=1"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo -e "${BOLD}  3. HEADLESS (Background Service)${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "  ${GREEN}Lab-modus (in-memory emulator):${NC}"
echo -e "    java -jar release/lpg-ehl-headless.jar --spring.profiles.active=lab"
echo ""
echo -e "  ${GREEN}Field-modus (serialport):${NC}"
echo -e "    java -jar release/lpg-ehl-headless.jar \\"
echo -e "      --spring.profiles.active=field \\"
echo -e "      --ehl.serial.port=/dev/ttyUSB0 \\"
echo -e "      --ehl.serial.baud-rate=9600 \\"
echo -e "      --ehl.serial.data-bits=8 \\"
echo -e "      --ehl.serial.parity=EVEN \\"
echo -e "      --ehl.serial.stop-bits=1"
echo ""
echo -e "  ${GREEN}Field-modus med debug API:${NC}"
echo -e "    java -jar release/lpg-ehl-headless.jar \\"
echo -e "      --spring.profiles.active=field,debug-api \\"
echo -e "      --ehl.serial.port=/dev/ttyUSB0 \\"
echo -e "      --ehl.serial.parity=EVEN"
echo ""
echo -e "  ${GREEN}Field-modus (produksjon med JVM-tuning):${NC}"
echo -e "    java -Xms128m -Xmx256m \\"
echo -e "      -XX:+UseG1GC -XX:MaxGCPauseMillis=50 \\"
echo -e "      -XX:+HeapDumpOnOutOfMemoryError \\"
echo -e "      -jar release/lpg-ehl-headless.jar \\"
echo -e "      --spring.profiles.active=field \\"
echo -e "      --ehl.serial.port=/dev/ttyUSB0 \\"
echo -e "      --ehl.serial.baud-rate=9600 \\"
echo -e "      --ehl.serial.data-bits=8 \\"
echo -e "      --ehl.serial.parity=EVEN \\"
echo -e "      --ehl.serial.stop-bits=1"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo -e "${BOLD}  📋 KONFIGURASJON${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "  ${BOLD}Transport-innstillinger:${NC}"
echo -e "    --ehl.serial.port=<device>       ${GRAY}# /dev/ttyUSB0, /tmp/vserial1, COM1${NC}"
echo -e "    --ehl.serial.baud-rate=<rate>    ${GRAY}# 9600 (default), 19200, 38400${NC}"
echo -e "    --ehl.serial.data-bits=<bits>    ${GRAY}# 8 (default), 7${NC}"
echo -e "    --ehl.serial.parity=<mode>       ${GRAY}# NONE, EVEN, ODD, MARK, SPACE${NC}"
echo -e "    --ehl.serial.stop-bits=<bits>    ${GRAY}# 1 (default), 2${NC}"
echo -e "    --ehl.serial.parity-auto-detect=true  ${GRAY}# Auto-detect parity${NC}"
echo ""
echo -e "  ${BOLD}Standard protokoll-innstillinger:${NC}"
echo -e "    EHL/PLS hardware:  9600 baud, 8E1 (8 data, EVEN parity, 1 stop)"
echo -e "    PLS Simulator:     9600 baud, 8N1 (8 data, NONE parity, 1 stop)"
echo ""
echo -e "  ${BOLD}JVM-innstillinger (field-modus):${NC}"
echo -e "    -Xms<size>                       ${GRAY}# Initial heap (f.eks. 128m, 256m)${NC}"
echo -e "    -Xmx<size>                       ${GRAY}# Max heap (f.eks. 256m, 512m)${NC}"
echo -e "    -XX:+UseG1GC                     ${GRAY}# G1 garbage collector (anbefalt)${NC}"
echo -e "    -XX:+UseZGC                      ${GRAY}# ZGC for lav latens${NC}"
echo -e "    -XX:MaxGCPauseMillis=<ms>        ${GRAY}# Maks GC-pause (f.eks. 50, 100)${NC}"
echo -e "    -XX:+HeapDumpOnOutOfMemoryError  ${GRAY}# Dump ved OOM${NC}"
echo ""
echo -e "  ${BOLD}Profiler:${NC}"
echo -e "    lab         ${GRAY}# In-memory emulator (utvikling)${NC}"
echo -e "    field       ${GRAY}# Ekte serialport (produksjon)${NC}"
echo -e "    debug-api   ${GRAY}# Aktiver debug-endepunkter${NC}"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo -e "${BOLD}  4. PAYMENT TERMINAL SIMULATOR (anbefalt: GUI)${NC}"
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "  ${CYAN}Start med GUI (anbefalt):${NC}"
echo -e "    java -jar release/payment-terminal-gui.jar"
echo ""
echo -e "  ${CYAN}Start GUI med custom port:${NC}"
echo -e "    java -jar release/payment-terminal-gui.jar --server.port=8080"
echo ""
echo -e "  ${CYAN}Start GUI med spesifikt scenario:${NC}"
echo -e "    java -jar release/payment-terminal-gui.jar \\"
echo -e "      --payment-terminal-sim.default-scenario=WRONG_PIN"
echo ""
echo -e "  ${CYAN}Start uten GUI (kun HTTP API):${NC}"
echo -e "    java -jar release/payment-terminal-gui.jar --terminal.gui.enabled=false"
echo -e "    ${GRAY}# eller bruk headless-varianten:${NC}"
echo -e "    java -jar release/payment-terminal-sim.jar"
echo ""
echo -e "  ${CYAN}Tilgjengelige scenarier:${NC}"
echo -e "    APPROVED, DECLINED, WRONG_PIN, USER_CANCEL, TIMEOUT, BUSY, NOT_READY"
echo ""
echo -e "  ${CYAN}Verifiser simulator:${NC}"
echo -e "    curl http://localhost:18080/health"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
