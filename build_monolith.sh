#!/bin/bash
# build_monolith.sh - Build LPG-EHL Applications
#
# Output: 
#   release/lpg-ehl-webapp.jar    - Web UI + REST API
#   release/lpg-ehl-headless.jar  - Background Service (+ debug-api profil)
#   release/pls-sim.jar           - PLS Simulator (for socat testing)
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

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BUILD_LOG="$SCRIPT_DIR/.build.log"
BUILD_START=$(date +%s)

# Helper: Run Maven command (quiet unless error or verbose)
run_maven() {
    local description="$1"
    shift
    
    if [ "$VERBOSE" = true ]; then
        ./mvnw "$@"
    else
        if ! ./mvnw "$@" > "$BUILD_LOG" 2>&1; then
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
echo -e "  ${GRAY}Maven:${NC} $(./mvnw -version 2>/dev/null | head -1 | cut -d' ' -f3)"
echo -e "  ${GRAY}Tests:${NC} $([ "$SKIP_TESTS" = true ] && echo 'Skipped' || echo 'Enabled')"
echo ""

# Step 1: Build React Frontend
echo -n -e "  ${GRAY}[1/4]${NC} Building React frontend... "
FRONTEND_DIR="$SCRIPT_DIR/lpg-web"
WEBAPP_STATIC="$SCRIPT_DIR/lpg-ehl-webapp/src/main/resources/static"

if [ -d "$FRONTEND_DIR" ]; then
    cd "$FRONTEND_DIR"
    npm install --silent > /dev/null 2>&1 || npm install > /dev/null 2>&1
    npm run build > /dev/null 2>&1
    
    rm -rf "$WEBAPP_STATIC"/* 2>/dev/null || true
    mkdir -p "$WEBAPP_STATIC"
    cp -r "$FRONTEND_DIR/dist/"* "$WEBAPP_STATIC/"
    STATIC_FILES=$(find "$WEBAPP_STATIC" -type f | wc -l | tr -d ' ')
    echo -e "${GREEN}✓${NC} ${GRAY}($STATIC_FILES files)${NC}"
    cd "$SCRIPT_DIR"
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

echo -e "${GREEN}✓${NC}"

# Step 4: Create Release Artifacts
echo -n -e "  ${GRAY}[4/4]${NC} Creating release artifacts... "

RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Find and copy JARs
WEBAPP_JAR=$(find "$SCRIPT_DIR/lpg-ehl-webapp/target" -name "lpg-ehl-webapp-*.jar" -not -name "*-plain.jar" | head -1)
HEADLESS_JAR=$(find "$SCRIPT_DIR/lpg-ehl-app-headless/target" -name "lpg-ehl-app-headless-*.jar" -not -name "*-plain.jar" | head -1)
PLS_SIM_JAR=$(find "$SCRIPT_DIR/lpg-ehl-serialport-sim/target" -name "lpg-ehl-serialport-sim-*.jar" -not -name "*-plain.jar" | head -1)

cp "$WEBAPP_JAR" "$RELEASE_DIR/lpg-ehl-webapp.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-webapp.jar"
cp "$HEADLESS_JAR" "$RELEASE_DIR/lpg-ehl-headless.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-headless.jar"
cp "$PLS_SIM_JAR" "$RELEASE_DIR/pls-sim.jar" && chmod +x "$RELEASE_DIR/pls-sim.jar"

echo -e "${GREEN}✓${NC}"

# Build time
BUILD_END=$(date +%s)
BUILD_DURATION=$((BUILD_END - BUILD_START))
BUILD_TIME=$(printf "%d:%02d" $((BUILD_DURATION / 60)) $((BUILD_DURATION % 60)))

# Sizes
WEBAPP_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-webapp.jar" | cut -f1)
HEADLESS_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-headless.jar" | cut -f1)
PLS_SIM_SIZE=$(du -h "$RELEASE_DIR/pls-sim.jar" | cut -f1)

# Clean up build log on success
rm -f "$BUILD_LOG"

echo ""
echo -e "${GREEN}✓ BUILD COMPLETE${NC} ${GRAY}($BUILD_TIME)${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  📦 ARTIFACTS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${YELLOW}release/lpg-ehl-webapp.jar${NC}    ${GRAY}($WEBAPP_SIZE)${NC}"
echo -e "  ${GREEN}release/lpg-ehl-headless.jar${NC}  ${GRAY}($HEADLESS_SIZE)${NC}"
echo -e "  ${CYAN}release/pls-sim.jar${NC}           ${GRAY}($PLS_SIM_SIZE)${NC}"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🚀 START APPLIKASJONEN${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
printf "  LAB MODE"
printf "  (In-memory emulator)\n"
echo ""
echo "     ./start-lpg-ehl.sh"
echo "     # eller:"
echo "     java -jar release/lpg-ehl-webapp.jar \\"
echo "       --spring.config.location=file:./application-h2.yaml"
echo ""
echo "     GUI: http://localhost:8080"
echo ""
echo ""
printf "  FIELD MODE"
printf "  (Serial port - hardware eller socat)\n"
echo ""
echo "     ./start-lpg-ehl.sh --field"
echo "     # eller med ekte hardware:"
echo "     java -jar release/lpg-ehl-webapp.jar \\"
echo "       --spring.config.location=file:./application-h2.yaml \\"
echo "       --lpg.mode=FIELD --ehl.serial.port=/dev/ttyUSB0"
echo ""
echo "     GUI: http://localhost:8080"
echo ""
echo ""
printf "  HEADLESS"
printf "  (Background service)\n"
echo ""
echo "     ./start-lpg-ehl.sh --headless"
echo "     # eller:"
echo "     java -jar release/lpg-ehl-headless.jar \\"
echo "       --spring.config.location=file:./application-h2.yaml"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
