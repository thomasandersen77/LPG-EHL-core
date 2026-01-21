#!/bin/bash
# build_monolith.sh - Build LPG-EHL Applications
#
# Output: 
#   release/lpg-ehl-webapp.jar    - Web UI + REST API (Undertow)
#   release/lpg-ehl-headless.jar  - Background Service + Debug API
#   release/lpg-ehl-cli.jar       - Command Line Tools
#
# Usage:
#   ./build_monolith.sh              # Build with tests
#   ./build_monolith.sh --skip-tests # Build without tests
#   ./build_monolith.sh --verbose    # Show full Maven output

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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
run_maven "CLI package" package -pl lpg-ehl-cli -am $PKG_ARGS

echo -e "${GREEN}✓${NC}"

# Step 4: Create Release Artifacts
echo -n -e "  ${GRAY}[4/4]${NC} Creating release artifacts... "

RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Find and copy JARs
WEBAPP_JAR=$(find "$SCRIPT_DIR/lpg-ehl-webapp/target" -name "lpg-ehl-webapp-*.jar" -not -name "*-plain.jar" | head -1)
HEADLESS_JAR=$(find "$SCRIPT_DIR/lpg-ehl-app-headless/target" -name "lpg-ehl-app-headless-*.jar" -not -name "*-plain.jar" | head -1)
CLI_JAR=$(find "$SCRIPT_DIR/lpg-ehl-cli/target" -name "lpg-ehl-cli-*.jar" -not -name "*-plain.jar" | head -1)

cp "$WEBAPP_JAR" "$RELEASE_DIR/lpg-ehl-webapp.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-webapp.jar"
cp "$HEADLESS_JAR" "$RELEASE_DIR/lpg-ehl-headless.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-headless.jar"
cp "$CLI_JAR" "$RELEASE_DIR/lpg-ehl-cli.jar" && chmod +x "$RELEASE_DIR/lpg-ehl-cli.jar"

echo -e "${GREEN}✓${NC}"

# Build time
BUILD_END=$(date +%s)
BUILD_DURATION=$((BUILD_END - BUILD_START))
BUILD_TIME=$(printf "%d:%02d" $((BUILD_DURATION / 60)) $((BUILD_DURATION % 60)))

# Sizes
WEBAPP_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-webapp.jar" | cut -f1)
HEADLESS_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-headless.jar" | cut -f1)
CLI_SIZE=$(du -h "$RELEASE_DIR/lpg-ehl-cli.jar" | cut -f1)

# Clean up build log on success
rm -f "$BUILD_LOG"

echo ""
echo -e "${GREEN}✓ BUILD COMPLETE${NC} ${GRAY}($BUILD_TIME)${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════
# ARTIFACT DETAILS
# ═══════════════════════════════════════════════════════════════════════════════

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  📦 RELEASE ARTIFACTS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# WEBAPP
# ─────────────────────────────────────────────────────────────────────────────
echo -e "${YELLOW}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${YELLOW}│${NC} ${BOLD}🖥️  WEBAPP${NC} ${GRAY}($WEBAPP_SIZE)${NC}"
echo -e "${YELLOW}│${NC} ${GRAY}release/lpg-ehl-webapp.jar${NC}"
echo -e "${YELLOW}├─────────────────────────────────────────────────────────────┤${NC}"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC} ${BOLD}Innhold:${NC}"
echo -e "${YELLOW}│${NC}   • React frontend (Control Panel UI)"
echo -e "${YELLOW}│${NC}   • REST API + Swagger UI"
echo -e "${YELLOW}│${NC}   • Undertow webserver (port 8080)"
echo -e "${YELLOW}│${NC}   • WebSocket real-time updates"
echo -e "${YELLOW}│${NC}   • EHL protokoll + emulator"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC} ${BOLD}Start (LAB mode med emulator):${NC}"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC}   java -jar release/lpg-ehl-webapp.jar"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC}   ${GRAY}→ http://localhost:8080${NC}"
echo -e "${YELLOW}│${NC}   ${GRAY}→ http://localhost:8080/swagger-ui.html${NC}"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC} ${BOLD}Start (FIELD mode med fysisk pumpe):${NC}"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC}   java -jar release/lpg-ehl-webapp.jar \\"
echo -e "${YELLOW}│${NC}     --lpg.mode=FIELD \\"
echo -e "${YELLOW}│${NC}     --ehl.serial.port=/dev/ttyS1 \\"
echo -e "${YELLOW}│${NC}     --server.port=8080"
echo -e "${YELLOW}│${NC}"
echo -e "${YELLOW}│${NC} ${BOLD}Miljøvariabler:${NC}"
echo -e "${YELLOW}│${NC}   LPG_MODE=LAB|FIELD         ${GRAY}# LAB=emulator, FIELD=hardware${NC}"
echo -e "${YELLOW}│${NC}   EHL_SERIAL_PORT=/dev/ttyS1 ${GRAY}# Seriell port (FIELD mode)${NC}"
echo -e "${YELLOW}│${NC}   SERVER_PORT=8080           ${GRAY}# HTTP port${NC}"
echo -e "${YELLOW}│${NC}   DB_HOST=localhost          ${GRAY}# PostgreSQL host${NC}"
echo -e "${YELLOW}│${NC}   DB_PASSWORD=secret         ${GRAY}# PostgreSQL passord${NC}"
echo -e "${YELLOW}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# HEADLESS
# ─────────────────────────────────────────────────────────────────────────────
echo -e "${GREEN}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${GREEN}│${NC} ${BOLD}🤖 HEADLESS${NC} ${GRAY}($HEADLESS_SIZE)${NC}"
echo -e "${GREEN}│${NC} ${GRAY}release/lpg-ehl-headless.jar${NC}"
echo -e "${GREEN}├─────────────────────────────────────────────────────────────┤${NC}"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC} ${BOLD}Innhold:${NC}"
echo -e "${GREEN}│${NC}   • Ingen webserver (minimal footprint)"
echo -e "${GREEN}│${NC}   • EHL protokoll + seriell kommunikasjon"
echo -e "${GREEN}│${NC}   • Database-persistering"
echo -e "${GREEN}│${NC}   • Scheduled tasks (polling, watchdog)"
echo -e "${GREEN}│${NC}   • Valgfri Debug API (Undertow)"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC} ${BOLD}Start (LAB mode, ingen webserver):${NC}"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC}   java -jar release/lpg-ehl-headless.jar"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC} ${BOLD}Start (FIELD mode med Debug API for curl-testing):${NC}"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC}   java -jar release/lpg-ehl-headless.jar \\"
echo -e "${GREEN}│${NC}     --spring.profiles.active=debug-api,local \\"
echo -e "${GREEN}│${NC}     --lpg.mode=FIELD \\"
echo -e "${GREEN}│${NC}     --ehl.serial.port=/dev/ttyS1"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}Debug API endepunkter (kun med debug-api profil):${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}  curl http://IP:8080/api/debug/health${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}  curl http://IP:8080/api/debug/state/1${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}  curl -X POST http://IP:8080/api/debug/linetest/1${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}  curl -X POST http://IP:8080/api/debug/unblock/1${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}  curl -X POST http://IP:8080/api/debug/block/1${NC}"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC} ${BOLD}Profiler:${NC}"
echo -e "${GREEN}│${NC}   ${GRAY}(ingen)${NC}      → Headless, ingen webserver"
echo -e "${GREEN}│${NC}   debug-api   → Undertow på port 8080 for curl"
echo -e "${GREEN}│${NC}   local       → Lokal database-config"
echo -e "${GREEN}│${NC}"
echo -e "${GREEN}│${NC} ${BOLD}Miljøvariabler:${NC}"
echo -e "${GREEN}│${NC}   LPG_MODE=LAB|FIELD         ${GRAY}# LAB=emulator, FIELD=hardware${NC}"
echo -e "${GREEN}│${NC}   EHL_SERIAL_PORT=/dev/ttyS1 ${GRAY}# Seriell port (FIELD mode)${NC}"
echo -e "${GREEN}│${NC}   DEBUG_API_PORT=8080        ${GRAY}# Port for debug-api profil${NC}"
echo -e "${GREEN}│${NC}   DB_HOST=localhost          ${GRAY}# PostgreSQL host${NC}"
echo -e "${GREEN}│${NC}   DB_PASSWORD=secret         ${GRAY}# PostgreSQL passord${NC}"
echo -e "${GREEN}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# CLI
# ─────────────────────────────────────────────────────────────────────────────
echo -e "${BLUE}┌─────────────────────────────────────────────────────────────┐${NC}"
echo -e "${BLUE}│${NC} ${BOLD}⚡ CLI${NC} ${GRAY}($CLI_SIZE)${NC}"
echo -e "${BLUE}│${NC} ${GRAY}release/lpg-ehl-cli.jar${NC}"
echo -e "${BLUE}├─────────────────────────────────────────────────────────────┤${NC}"
echo -e "${BLUE}│${NC}"
echo -e "${BLUE}│${NC} ${BOLD}Innhold:${NC}"
echo -e "${BLUE}│${NC}   • Spring Shell interaktiv CLI"
echo -e "${BLUE}│${NC}   • EHL protokoll-kommandoer"
echo -e "${BLUE}│${NC}   • Database-operasjoner"
echo -e "${BLUE}│${NC}"
echo -e "${BLUE}│${NC} ${BOLD}Start (interaktiv shell):${NC}"
echo -e "${BLUE}│${NC}"
echo -e "${BLUE}│${NC}   java -jar release/lpg-ehl-cli.jar"
echo -e "${BLUE}│${NC}"
echo -e "${BLUE}│${NC} ${BOLD}Eksempel-kommandoer:${NC}"
echo -e "${BLUE}│${NC}   java -jar release/lpg-ehl-cli.jar help"
echo -e "${BLUE}│${NC}   java -jar release/lpg-ehl-cli.jar linetest --addr=1"
echo -e "${BLUE}│${NC}"
echo -e "${BLUE}│${NC} ${BOLD}Miljøvariabler:${NC}"
echo -e "${BLUE}│${NC}   LPG_MODE=LAB|FIELD         ${GRAY}# LAB=emulator, FIELD=hardware${NC}"
echo -e "${BLUE}│${NC}   EHL_SERIAL_PORT=/dev/ttyS1 ${GRAY}# Seriell port (FIELD mode)${NC}"
echo -e "${BLUE}└─────────────────────────────────────────────────────────────┘${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════════════════════
# QUICK START
# ═══════════════════════════════════════════════════════════════════════════════

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🚀 QUICK START${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  ${BOLD}Utvikling (LAB mode med emulator):${NC}"
echo -e "    java -jar release/lpg-ehl-webapp.jar"
echo -e "    ${GRAY}→ Åpne http://localhost:8080${NC}"
echo ""
echo -e "  ${BOLD}Felt-test (FIELD mode med Debug API):${NC}"
echo -e "    java -jar release/lpg-ehl-headless.jar \\"
echo -e "      --spring.profiles.active=debug-api \\"
echo -e "      --lpg.mode=FIELD \\"
echo -e "      --ehl.serial.port=/dev/ttyS1"
echo ""
echo -e "    ${GRAY}# Test fra laptop:${NC}"
echo -e "    ${GRAY}curl http://ARK-IP:8080/api/debug/health${NC}"
echo -e "    ${GRAY}curl -X POST http://ARK-IP:8080/api/debug/unblock/1${NC}"
echo ""
echo -e "  ${BOLD}Produksjon (FIELD mode, headless):${NC}"
echo -e "    java -jar release/lpg-ehl-headless.jar \\"
echo -e "      --lpg.mode=FIELD \\"
echo -e "      --ehl.serial.port=/dev/ttyS1"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
