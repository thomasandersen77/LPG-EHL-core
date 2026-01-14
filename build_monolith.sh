#!/bin/bash
# build_monolith.sh - Build LPG-EHL Applications (GUI + CLI)
#
# This script creates TWO executable JAR files:
# 1. GUI JAR: lpg-ehl-core + lpg-ehl-api + lpg-web (React)
# 2. CLI JAR: lpg-ehl-core + lpg-ehl-cli (Spring Shell)
#
# Output: 
#   - release/lpg-ehl-gui.jar (Web UI + REST API)
#   - release/lpg-ehl-cli.jar (Interactive CLI)
#
# Usage:
#   ./build_monolith.sh
#
# Requirements:
#   - Node.js 18+ (for React build)
#   - Maven 3.8+ (for Spring Boot build)
#   - Java 21 (Temurin recommended)

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  LPG-EHL Build System${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Building:"
echo "  • lpg-ehl-gui.jar (Web UI + REST API)"
echo "  • lpg-ehl-cli.jar (Command Line Interface)"
echo ""

# Step 1: Build All Maven Modules First
echo -e "${GREEN}[1/6]${NC} Building all Maven modules (core, emulator, pls, api, cli)..."
echo "  Running Maven reactor build..."
./mvnw clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Maven build failed${NC}"
    exit 1
fi

echo -e "  ${GREEN}✓${NC} All Maven modules built successfully"
echo ""

# Step 2: Build React Frontend
echo -e "${GREEN}[2/6]${NC} Building React frontend (lpg-web)..."
cd lpg-web

if [ ! -d "node_modules" ]; then
    echo "  Installing npm dependencies..."
    npm install
else
    echo "  Dependencies already installed (skipping npm install)"
fi

# Temporarily move .env.local to ensure production build uses window.location.origin
# (The React code has fallbacks that use same-origin when env vars are not set)
if [ -f ".env.local" ]; then
    echo "  Moving .env.local aside for production build..."
    mv .env.local .env.local.bak
    ENV_LOCAL_MOVED=true
fi

echo "  Running production build..."
npm run build

# Restore .env.local
if [ "$ENV_LOCAL_MOVED" = true ]; then
    echo "  Restoring .env.local..."
    mv .env.local.bak .env.local
fi

# Verify build output
if [ ! -d "dist" ]; then
    echo -e "${RED}ERROR: Frontend build failed - dist/ directory not found${NC}"
    exit 1
fi

echo -e "  ${GREEN}✓${NC} Frontend built successfully"
echo ""

# Step 3: Clean old static files
echo -e "${GREEN}[3/6]${NC} Cleaning old static files from Spring Boot..."
cd "$SCRIPT_DIR"
STATIC_DIR="lpg-ehl-api/src/main/resources/static"

if [ -d "$STATIC_DIR" ]; then
    echo "  Removing $STATIC_DIR/*"
    rm -rf "$STATIC_DIR"/*
else
    echo "  Creating $STATIC_DIR"
    mkdir -p "$STATIC_DIR"
fi

echo -e "  ${GREEN}✓${NC} Static directory cleaned"
echo ""

# Step 4: Copy React build to Spring Boot static resources
echo -e "${GREEN}[4/6]${NC} Copying React build to Spring Boot static resources..."
echo "  Source: lpg-web/dist/"
echo "  Target: $STATIC_DIR/"

cp -r lpg-web/dist/* "$STATIC_DIR/"

# Verify copy
FILE_COUNT=$(find "$STATIC_DIR" -type f | wc -l | tr -d ' ')
echo -e "  ${GREEN}✓${NC} Copied $FILE_COUNT files"
echo ""

# Step 5: Package GUI JAR (API + Web)
echo -e "${GREEN}[5/6]${NC} Packaging GUI JAR (API + Web frontend)..."
cd "$SCRIPT_DIR"

echo "  Running Maven package for API module..."
cd lpg-ehl-api
../mvnw package -DskipTests

# Find the built JAR
JAR_FILE=$(find "$SCRIPT_DIR/lpg-ehl-api/target" -name "lpg-ehl-api-*.jar" -not -name "*-plain.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found in target/${NC}"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "  ${GREEN}✓${NC} JAR built successfully: $(basename "$JAR_FILE") ($JAR_SIZE)"
echo ""

# Step 6: Prepare release artifacts
echo -e "${GREEN}[6/6]${NC} Preparing release artifacts..."
cd "$SCRIPT_DIR"

RELEASE_DIR="release"
mkdir -p "$RELEASE_DIR"

# Copy GUI JAR
GUI_JAR="$RELEASE_DIR/lpg-ehl-gui.jar"
cp "$JAR_FILE" "$GUI_JAR"
chmod +x "$GUI_JAR"
GUI_SIZE=$(du -h "$GUI_JAR" | cut -f1)
echo -e "  ${GREEN}✓${NC} GUI JAR created: lpg-ehl-gui.jar ($GUI_SIZE)"

# Copy CLI JAR
CLI_JAR_SOURCE=$(find "$SCRIPT_DIR/lpg-ehl-cli/target" -name "lpg-ehl-cli-*.jar" -not -name "*-plain.jar" | head -1)

if [ -z "$CLI_JAR_SOURCE" ]; then
    echo -e "${RED}ERROR: CLI JAR file not found in lpg-ehl-cli/target/${NC}"
    exit 1
fi

CLI_JAR="$RELEASE_DIR/lpg-ehl-cli.jar"
cp "$CLI_JAR_SOURCE" "$CLI_JAR"
chmod +x "$CLI_JAR"
CLI_SIZE=$(du -h "$CLI_JAR" | cut -f1)
echo -e "  ${GREEN}✓${NC} CLI JAR created: lpg-ehl-cli.jar ($CLI_SIZE)"

# Create convenience symlink for backward compatibility
ln -sf lpg-ehl-gui.jar "$RELEASE_DIR/lpg-ehl-monolith.jar"
echo -e "  ${GREEN}✓${NC} Symlink created: lpg-ehl-monolith.jar -> lpg-ehl-gui.jar"
echo ""

# Build summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Build Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Release Artifacts:${NC}"
echo "  GUI (Web):  release/lpg-ehl-gui.jar ($GUI_SIZE)"
echo "  CLI:        release/lpg-ehl-cli.jar ($CLI_SIZE)"
echo "  Symlink:    release/lpg-ehl-monolith.jar -> lpg-ehl-gui.jar"
echo ""
echo -e "${YELLOW}What's included:${NC}"
echo ""
echo -e "${BLUE}GUI JAR (lpg-ehl-gui.jar):${NC}"
echo "  ✓ lpg-ehl-core (Protocol implementation)"
echo "  ✓ lpg-ehl-emulator (Built-in emulator)"
echo "  ✓ lpg-ehl-pls (Physical layer for real hardware)"
echo "  ✓ lpg-ehl-api (Spring Boot REST API)"
echo "  ✓ lpg-web (React frontend)"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${BLUE}CLI JAR (lpg-ehl-cli.jar):${NC}"
echo "  ✓ lpg-ehl-core (Protocol implementation)"
echo "  ✓ lpg-ehl-emulator (Built-in emulator)"
echo "  ✓ lpg-ehl-pls (Physical layer for real hardware)"
echo "  ✓ Spring Shell CLI"
echo "  ✓ All dependencies (Fat JAR)"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${YELLOW}How to Run${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}1. GUI Mode (Web Interface + REST API):${NC}"
echo "   Local (LAB MODE - with emulator):"
echo "     java -jar release/lpg-ehl-gui.jar"
echo "     # Then open: http://localhost:8080"
echo ""
echo "   With real hardware (FIELD MODE):"
echo "     EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 \\"
echo "       java -jar release/lpg-ehl-gui.jar"
echo ""
echo -e "${GREEN}2. CLI Mode (Terminal Interface):${NC}"
echo "   Interactive shell (LAB MODE - with emulator):"
echo "     java -jar release/lpg-ehl-cli.jar"
echo ""
echo "   Commands available:"
echo "     shell:> linetest 1    # Test connectivity"
echo "     shell:> state 1       # Query pump state"
echo "     shell:> unblock 1     # Allow dispensing"
echo "     shell:> block 1       # Prevent dispensing"
echo "     shell:> volume 1      # Query volume"
echo "     shell:> price 1       # Query price"
echo "     shell:> error 1       # Query errors"
echo "     shell:> tank 1        # Query tank level"
echo "     shell:> vb6 1         # Run VB6 test sequence"
echo "     shell:> help          # Show all commands"
echo "     shell:> exit          # Exit CLI"
echo ""
echo "   With real hardware (FIELD MODE):"
echo "     EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 \\"
echo "       java -jar release/lpg-ehl-cli.jar"
echo ""
echo -e "${GREEN}3. Deploy to Production (ARK machine):${NC}"
echo "   scp release/lpg-ehl-gui.jar user@ark-machine:/opt/lpg-ehl/"
echo "   ssh user@ark-machine"
echo "   sudo systemctl stop lpg-ehl"
echo "   sudo cp /opt/lpg-ehl/lpg-ehl-gui.jar /opt/lpg-ehl/lpg-ehl.jar"
echo "   sudo systemctl start lpg-ehl"
echo ""
echo -e "${GREEN}4. IntelliJ Development:${NC}"
echo "   GUI: Run LpgEhlApiApplication"
echo "        -> http://localhost:8080"
echo "   CLI: Run CliApplicationKt"
echo "        -> Interactive shell in console"
echo ""
echo -e "${YELLOW}Endpoints (GUI mode):${NC}"
echo "  Web UI:     http://localhost:8080"
echo "  API:        http://localhost:8080/api/*"
echo "  Swagger:    http://localhost:8080/swagger-ui.html"
echo "  WebSocket:  ws://localhost:8080/ws"
echo ""
echo -e "${GREEN}Ready for deployment! 🚀${NC}"
