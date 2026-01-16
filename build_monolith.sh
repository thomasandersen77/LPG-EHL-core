#!/bin/bash
# build_monolith.sh - Build LPG-EHL Applications (3 Deployment Modes)
#
# This script creates THREE executable JAR files:
# 1. WEBAPP JAR: lpg-ehl-service + lpg-ehl-webapp (Web UI + REST API)
# 2. HEADLESS JAR: lpg-ehl-service + lpg-ehl-app-headless (Background Service)
# 3. CLI JAR: lpg-ehl-service + lpg-ehl-cli (Command Line Tools)
#
# Output: 
#   - release/lpg-ehl-webapp.jar (Web UI + REST API)
#   - release/lpg-ehl-headless.jar (Headless Background Service)
#   - release/lpg-ehl-cli.jar (CLI Tools)
#
# Usage:
#   ./build_monolith.sh          # Build with tests
#   ./build_monolith.sh --skip-tests  # Build without tests
#
# Requirements:
#   - Maven 3.8+ (for Spring Boot build)
#   - Java 21 (Temurin recommended)

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parse arguments
SKIP_TESTS=false
if [[ "$1" == "--skip-tests" ]]; then
    SKIP_TESTS=true
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Build start time
BUILD_START=$(date +%s)

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  LPG-EHL Multi-Mode Build System${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Build Configuration:${NC}"
echo "  Architecture: Hexagonal (Service-based)"
if [ "$SKIP_TESTS" = true ]; then
    echo "  Tests: SKIPPED"
else
    echo "  Tests: ENABLED"
fi
echo "  Java Version: $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
echo "  Maven Version: $(./mvnw -version | head -1 | cut -d' ' -f3)"
echo ""
echo -e "${YELLOW}Building 3 Deployment Modes:${NC}"
echo "  🖥️  WEBAPP - Full web application with React UI"
echo "  🤖 HEADLESS - Background service (no web server)"
echo "  ⚡ CLI - Command line tools"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Step 1: Clean and Build All Maven Modules
echo -e "${GREEN}[1/5]${NC} Building all Maven modules..."
echo "  📦 Modules: core, emulator, transport, service, webapp, headless, cli"
echo ""

if [ "$SKIP_TESTS" = true ]; then
    echo "  Running: mvn clean install -DskipTests (output suppressed)"
    ./mvnw clean install -DskipTests -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
else
    echo "  Running: mvn clean install (with tests, output suppressed)"
    ./mvnw clean install -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building|Tests run" || true
fi

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo -e "${RED}✗ ERROR: Maven build failed - rerun without -q flag for details${NC}"
    exit 1
fi

echo -e "  ${GREEN}✓${NC} All Maven modules built successfully"
echo ""

# Step 2: Package WebApp JAR
echo -e "${GREEN}[2/5]${NC} Packaging WebApp JAR (with React frontend)..."
cd "$SCRIPT_DIR"
echo "  Module: lpg-ehl-webapp"
echo "  Includes: Service layer + REST API + React UI"
echo ""

if [ "$SKIP_TESTS" = true ]; then
    ./mvnw package -pl lpg-ehl-webapp -am -DskipTests -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
else
    ./mvnw package -pl lpg-ehl-webapp -am -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
fi

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo -e "${RED}✗ ERROR: WebApp packaging failed${NC}"
    exit 1
fi

WEBAPP_JAR=$(find "$SCRIPT_DIR/lpg-ehl-webapp/target" -name "lpg-ehl-webapp-*.jar" -not -name "*-plain.jar" | head -1)
if [ -z "$WEBAPP_JAR" ]; then
    echo -e "${RED}✗ ERROR: WebApp JAR not found${NC}"
    exit 1
fi

WEBAPP_SIZE=$(du -h "$WEBAPP_JAR" | cut -f1)
echo -e "  ${GREEN}✓${NC} WebApp JAR built: $(basename "$WEBAPP_JAR") ($WEBAPP_SIZE)"
echo ""

# Step 3: Package Headless JAR
echo -e "${GREEN}[3/5]${NC} Packaging Headless JAR (background service)..."
cd "$SCRIPT_DIR"
echo "  Module: lpg-ehl-app-headless"
echo "  Includes: Service layer + No web server"
echo "  Perfect for: Production deployment, Docker, Systemd"
echo ""

if [ "$SKIP_TESTS" = true ]; then
    ./mvnw package -pl lpg-ehl-app-headless -am -DskipTests -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
else
    ./mvnw package -pl lpg-ehl-app-headless -am -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
fi

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo -e "${RED}✗ ERROR: Headless packaging failed${NC}"
    exit 1
fi

HEADLESS_JAR=$(find "$SCRIPT_DIR/lpg-ehl-app-headless/target" -name "lpg-ehl-app-headless-*.jar" -not -name "*-plain.jar" | head -1)
if [ -z "$HEADLESS_JAR" ]; then
    echo -e "${RED}✗ ERROR: Headless JAR not found${NC}"
    exit 1
fi

HEADLESS_SIZE=$(du -h "$HEADLESS_JAR" | cut -f1)
echo -e "  ${GREEN}✓${NC} Headless JAR built: $(basename "$HEADLESS_JAR") ($HEADLESS_SIZE)"
echo ""

# Step 4: Package CLI JAR
echo -e "${GREEN}[4/5]${NC} Packaging CLI JAR (command line tools)..."
cd "$SCRIPT_DIR"
echo "  Module: lpg-ehl-cli"
echo "  Includes: Service layer + CLI commands"
echo "  Perfect for: Automation, scripting, diagnostics"
echo ""

if [ "$SKIP_TESTS" = true ]; then
    ./mvnw package -pl lpg-ehl-cli -am -DskipTests -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
else
    ./mvnw package -pl lpg-ehl-cli -am -q 2>&1 | grep -E "BUILD (SUCCESS|FAILURE)|ERROR|Building" || true
fi

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo -e "${RED}✗ ERROR: CLI packaging failed${NC}"
    exit 1
fi

CLI_JAR_SOURCE=$(find "$SCRIPT_DIR/lpg-ehl-cli/target" -name "lpg-ehl-cli-*.jar" -not -name "*-plain.jar" | head -1)
if [ -z "$CLI_JAR_SOURCE" ]; then
    echo -e "${RED}✗ ERROR: CLI JAR not found${NC}"
    exit 1
fi

CLI_SIZE=$(du -h "$CLI_JAR_SOURCE" | cut -f1)
echo -e "  ${GREEN}✓${NC} CLI JAR built: $(basename "$CLI_JAR_SOURCE") ($CLI_SIZE)"
echo ""

# Step 5: Create Release Artifacts
echo -e "${GREEN}[5/5]${NC} Creating release artifacts..."
cd "$SCRIPT_DIR"
echo "  Preparing release/ directory..."
echo ""

RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Copy WebApp JAR
WEBAPP_RELEASE="$RELEASE_DIR/lpg-ehl-webapp.jar"
cp "$WEBAPP_JAR" "$WEBAPP_RELEASE"
chmod +x "$WEBAPP_RELEASE"
WEBAPP_RELEASE_SIZE=$(du -h "$WEBAPP_RELEASE" | cut -f1)
echo -e "  ${GREEN}✓${NC} WebApp JAR: lpg-ehl-webapp.jar ($WEBAPP_RELEASE_SIZE)"

# Copy Headless JAR
HEADLESS_RELEASE="$RELEASE_DIR/lpg-ehl-headless.jar"
cp "$HEADLESS_JAR" "$HEADLESS_RELEASE"
chmod +x "$HEADLESS_RELEASE"
HEADLESS_RELEASE_SIZE=$(du -h "$HEADLESS_RELEASE" | cut -f1)
echo -e "  ${GREEN}✓${NC} Headless JAR: lpg-ehl-headless.jar ($HEADLESS_RELEASE_SIZE)"

# Copy CLI JAR
CLI_RELEASE="$RELEASE_DIR/lpg-ehl-cli.jar"
cp "$CLI_JAR_SOURCE" "$CLI_RELEASE"
chmod +x "$CLI_RELEASE"
CLI_RELEASE_SIZE=$(du -h "$CLI_RELEASE" | cut -f1)
echo -e "  ${GREEN}✓${NC} CLI JAR: lpg-ehl-cli.jar ($CLI_RELEASE_SIZE)"

echo ""
echo -e "  ${GREEN}✓${NC} All release artifacts created in release/"
echo ""

# Build completion time
BUILD_END=$(date +%s)
BUILD_DURATION=$((BUILD_END - BUILD_START))
BUILD_TIME=$(printf "%d:%02d" $((BUILD_DURATION / 60)) $((BUILD_DURATION % 60)))

# Build summary
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ BUILD COMPLETE!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Build Statistics:${NC}"
echo "  Duration: $BUILD_TIME (mm:ss)"
if [ "$SKIP_TESTS" = true ]; then
    echo "  Tests: Skipped"
else
    echo "  Tests: Passed"
fi
echo "  Artifacts: 3 executable JARs"
echo ""
echo -e "${YELLOW}Release Artifacts:${NC}"
echo "  🖥️  WebApp:   release/lpg-ehl-webapp.jar ($WEBAPP_RELEASE_SIZE)"
echo "  🤖 Headless: release/lpg-ehl-headless.jar ($HEADLESS_RELEASE_SIZE)"
echo "  ⚡ CLI:      release/lpg-ehl-cli.jar ($CLI_RELEASE_SIZE)"
echo ""
echo -e "${YELLOW}What's Included in Each JAR:${NC}"
echo ""
echo -e "${BLUE}1. WebApp JAR (lpg-ehl-webapp.jar):${NC}"
echo "  ✓ lpg-ehl-service (Business logic core)"
echo "  ✓ lpg-ehl-core (EHL protocol)"
echo "  ✓ lpg-transport (Serial communication)"
echo "  ✓ lpg-ehl-emulator (LAB mode simulator)"
echo "  ✓ Spring Boot + Tomcat (REST API)"
echo "  ✓ React frontend (Control Panel UI)"
echo "  ✓ WebSocket support (Real-time updates)"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${BLUE}2. Headless JAR (lpg-ehl-headless.jar):${NC}"
echo "  ✓ lpg-ehl-service (Business logic core)"
echo "  ✓ lpg-ehl-core (EHL protocol)"
echo "  ✓ lpg-transport (Serial communication)"
echo "  ✓ Spring Boot (NO WEB SERVER)"
echo "  ✓ Scheduled tasks (@Scheduled)"
echo "  ✓ Database persistence"
echo "  ✓ Azure cloud sync"
echo "  ✓ Hardware watchdog"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${BLUE}3. CLI JAR (lpg-ehl-cli.jar):${NC}"
echo "  ✓ lpg-ehl-service (Business logic core)"
echo "  ✓ lpg-ehl-core (EHL protocol)"
echo "  ✓ lpg-transport (Serial communication)"
echo "  ✓ Spring Shell (CLI framework)"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}HOW TO RUN EACH MODE${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}1. 🖥️  WebApp Mode (Full Web Application):${NC}"
echo ""
echo -e "   ${YELLOW}Development (LAB mode with emulator):${NC}"
echo "     java -jar release/lpg-ehl-webapp.jar"
echo ""
echo "     Then open: http://localhost:8080"
echo "     Swagger:   http://localhost:8080/swagger-ui.html"
echo ""
echo -e "   ${YELLOW}Production (FIELD mode with real hardware):${NC}"
echo "     java -jar release/lpg-ehl-webapp.jar \\"
echo "       --EHL_EMULATOR_ENABLED=false \\"
echo "       --EHL_SERIAL_PORT=/dev/ttyUSB0 \\"
echo "       --DB_HOST=localhost \\"
echo "       --DB_PASSWORD=<secret> \\"
echo "       --AZURE_ENABLED=true"
echo ""
echo -e "   ${YELLOW}Configuration Options:${NC}"
echo "     --EHL_EMULATOR_ENABLED=true/false  # Use emulator or real hardware"
echo "     --EHL_SERIAL_PORT=/dev/ttyUSB0     # Serial port device"
echo "     --DB_HOST=localhost                 # Database host"
echo "     --DB_PASSWORD=secret                # Database password"
echo "     --AZURE_ENABLED=true                # Enable cloud sync"
echo "     --PORT=8080                         # HTTP port"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${GREEN}2. 🤖 Headless Mode (Background Service - NO WEB SERVER):${NC}"
echo ""
echo -e "   ${YELLOW}Development (LAB mode with emulator):${NC}"
echo "     java -jar release/lpg-ehl-headless.jar"
echo ""
echo -e "   ${YELLOW}Production (FIELD mode - typical bensinstasjon deployment):${NC}"
echo "     java -jar release/lpg-ehl-headless.jar \\"
echo "       --EHL_EMULATOR_ENABLED=false \\"
echo "       --EHL_SERIAL_PORT=/dev/ttyUSB0 \\"
echo "       --DB_HOST=localhost \\"
echo "       --DB_PASSWORD=<secret> \\"
echo "       --AZURE_ENABLED=true"
echo ""
echo -e "   ${YELLOW}Systemd Service (Linux production):${NC}"
echo "     # Copy JAR to server"
echo "     scp release/lpg-ehl-headless.jar user@station:/opt/lpg-ehl/"
echo ""
echo "     # Create /etc/systemd/system/lpg-ehl.service:"
echo "     [Unit]"
echo "     Description=LPG EHL Headless Service"
echo "     After=network.target postgresql.service"
echo ""
echo "     [Service]"
echo "     Type=simple"
echo "     User=lpg"
echo "     ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-headless.jar"
echo "     Environment=\"EHL_EMULATOR_ENABLED=false\""
echo "     Environment=\"EHL_SERIAL_PORT=/dev/ttyUSB0\""
echo "     Restart=on-failure"
echo ""
echo "     [Install]"
echo "     WantedBy=multi-user.target"
echo ""
echo "     # Enable and start"
echo "     sudo systemctl enable lpg-ehl"
echo "     sudo systemctl start lpg-ehl"
echo "     sudo systemctl status lpg-ehl"
echo ""
echo -e "   ${YELLOW}Docker Deployment:${NC}"
echo "     docker run -d \\"
echo "       --name lpg-ehl \\"
echo "       --device=/dev/ttyUSB0 \\"
echo "       -e EHL_EMULATOR_ENABLED=false \\"
echo "       -e DB_HOST=postgres \\"
echo "       -e AZURE_ENABLED=true \\"
echo "       lpg-ehl-headless:latest"
echo ""
echo -e "   ${YELLOW}Configuration Options:${NC}"
echo "     --EHL_EMULATOR_ENABLED=false       # FIELD mode by default"
echo "     --EHL_SERIAL_PORT=/dev/ttyUSB0     # Serial port"
echo "     --DB_HOST=localhost                 # Database"
echo "     --AZURE_ENABLED=true                # Cloud sync"
echo "     --LOG_FILE=/var/log/lpg-ehl/headless.log  # Log file"
echo ""
echo -e "   ${YELLOW}What It Does:${NC}"
echo "     ✓ Monitors hardware via serial port"
echo "     ✓ Saves transactions to database"
echo "     ✓ Syncs to Azure automatically"
echo "     ✓ Runs scheduled tasks (@Scheduled)"
echo "     ✓ Hardware watchdog monitoring"
echo "     ✓ NO web server = minimal overhead"
echo ""
echo -e "${BLUE}───────────────────────────────────────────────────────────${NC}"
echo ""
echo -e "${GREEN}3. ⚡ CLI Mode (Command Line Tools):${NC}"
echo ""
echo -e "   ${YELLOW}Interactive Shell:${NC}"
echo "     java -jar release/lpg-ehl-cli.jar"
echo ""
echo -e "   ${YELLOW}Example Commands:${NC}"
echo "     # Migrations"
echo "     java -jar release/lpg-ehl-cli.jar migrate --target-version=1.5.0"
echo ""
echo "     # Export transactions"
echo "     java -jar release/lpg-ehl-cli.jar transactions export \\"
echo "       --from=2026-01-01 --to=2026-01-31 --format=csv"
echo ""
echo "     # Diagnostics"
echo "     java -jar release/lpg-ehl-cli.jar diagnostics \\"
echo "       --check-hardware --check-database"
echo ""
echo "     # Price updates"
echo "     java -jar release/lpg-ehl-cli.jar price set --product=LPG --price=16.50"
echo ""
echo -e "   ${YELLOW}With Real Hardware:${NC}"
echo "     EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyUSB0 \\"
echo "       java -jar release/lpg-ehl-cli.jar [command]"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}DEPLOYMENT RECOMMENDATIONS:${NC}"
echo ""
echo -e "  ${GREEN}Production Bensinstasjon:${NC}"
echo "    • Use HEADLESS mode (lpg-ehl-headless.jar)"
echo "    • Run as systemd service"
echo "    • Set FIELD mode (EHL_EMULATOR_ENABLED=false)"
echo "    • Configure serial port (/dev/ttyUSB0)"
echo "    • Enable Azure sync"
echo ""
echo -e "  ${GREEN}Development & Testing:${NC}"
echo "    • Use WEBAPP mode (lpg-ehl-webapp.jar)"
echo "    • LAB mode with emulator (default)"
echo "    • Access web UI at http://localhost:8080"
echo ""
echo -e "  ${GREEN}Administration & Scripts:${NC}"
echo "    • Use CLI mode (lpg-ehl-cli.jar)"
echo "    • Automate tasks with shell scripts"
echo "    • Database migrations and batch ops"
echo ""
echo -e "${GREEN}✓ Ready for deployment! 🚀${NC}"
echo ""
echo -e "${YELLOW}Documentation:${NC}"
echo "  Architecture: ARCHITECTURE.md"
echo "  Headless:     lpg-ehl-app-headless/README.md"
echo "  WebApp:       lpg-ehl-webapp/README.md"
echo ""
