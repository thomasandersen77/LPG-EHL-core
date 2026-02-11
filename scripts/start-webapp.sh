#!/bin/bash
#═══════════════════════════════════════════════════════════════════════
# START WEBAPP – LPG-EHL API + Frontend
#═══════════════════════════════════════════════════════════════════════
#
# Starter webappen som kobler til PLS-simulatoren via /tmp/vserial1
#
# Port: 8080 (både API og frontend)
# Serial: /tmp/vserial1 (kobler til PLS via socat)
#
# VIKTIG: Kjør først ./scripts/start-all-simulators.sh
#
#═══════════════════════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# JAR path
WEBAPP_JAR="$PROJECT_ROOT/release/lpg-ehl-webapp.jar"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Check if JAR exists
if [ ! -f "$WEBAPP_JAR" ]; then
    echo -e "${RED}Webapp JAR ikke funnet: $WEBAPP_JAR${NC}"
    echo -e "${YELLOW}Bygg først med: ./build_monolith.sh${NC}"
    exit 1
fi

# Check if vserial1 exists (socat must be running)
if [ ! -e /tmp/vserial1 ]; then
    echo -e "${RED}/tmp/vserial1 eksisterer ikke!${NC}"
    echo -e "${YELLOW}Start simulatorene først: ./scripts/start-all-simulators.sh${NC}"
    exit 1
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  🌐 Start LPG-EHL Webapp${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Starting webapp...${NC}"
echo -e "  Port:        ${BOLD}8080${NC}"
echo -e "  Serial:      ${BOLD}/tmp/vserial1${NC}"
echo -e "  Profile:     ${BOLD}lab,emulator${NC}"
echo ""

# Start webapp
java -jar "$WEBAPP_JAR" \
    --spring.profiles.active=lab,emulator \
    --server.port=8080 \
    --serial.port=/tmp/vserial1 \
    --serial.mode=EMULATOR

echo ""
echo -e "${GREEN}✅ Webapp stopped${NC}"
