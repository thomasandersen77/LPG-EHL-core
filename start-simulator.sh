#!/bin/bash
# Startup script for PLS Simulator with socat virtual PTY bridge
# This script starts both socat and the simulator together

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== LPG EHL Simulator Startup ===${NC}"

# Check if socat is installed
if ! command -v socat &> /dev/null; then
    echo -e "${RED}Error: socat is not installed${NC}"
    echo "Install with: brew install socat"
    exit 1
fi

# Check if simulator jar exists
if [ ! -f "release/pls-sim.jar" ]; then
    echo -e "${RED}Error: release/pls-sim.jar not found${NC}"
    echo "Build the project first: mvn clean package -DskipTests"
    exit 1
fi

# Cleanup any existing PTYs
rm -f /tmp/ttyV0 /tmp/ttyV1

echo -e "${YELLOW}Starting socat virtual PTY bridge...${NC}"
# Start socat in background to create virtual PTY pair
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 &
SOCAT_PID=$!

# Wait for PTYs to be created
sleep 1

# Check if socat is still running
if ! kill -0 $SOCAT_PID 2>/dev/null; then
    echo -e "${RED}Error: socat failed to start${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Socat bridge established${NC}"
echo -e "  /tmp/ttyV0 ← PLS Simulator (dispenser)"
echo -e "  /tmp/ttyV1 ← Webapp (controller)"
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    kill $SOCAT_PID 2>/dev/null || true
    rm -f /tmp/ttyV0 /tmp/ttyV1
    echo -e "${GREEN}Cleanup complete${NC}"
    exit 0
}

# Trap SIGINT (Ctrl+C) and SIGTERM
trap cleanup SIGINT SIGTERM

echo -e "${YELLOW}Starting PLS Simulator...${NC}"
echo -e "${GREEN}Press Ctrl+C to stop both socat and simulator${NC}"
echo ""

# Start simulator (this runs in foreground)
java -jar release/pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true

# If we get here, simulator exited normally
cleanup
