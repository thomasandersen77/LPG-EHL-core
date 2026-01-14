#!/bin/bash
# Test script for EHL mode

set -e

echo "=== EHL Mode Test Script ==="
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Cleaning up...${NC}"
    pkill -f "pls-sim.jar" || true
    pkill -f "socat.*ttyV" || true
    rm -f /tmp/ttyV0 /tmp/ttyV1
    echo "Cleanup complete"
}

trap cleanup EXIT

echo "Step 1: Creating virtual serial port pair with socat..."
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 &
SOCAT_PID=$!
sleep 2

if [ ! -e /tmp/ttyV0 ] || [ ! -e /tmp/ttyV1 ]; then
    echo -e "${RED}ERROR: Failed to create virtual serial ports${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Virtual serial ports created: /tmp/ttyV0 <-> /tmp/ttyV1${NC}"
echo

echo "Step 2: Starting PLS simulator in EHL mode..."
java -jar target/pls-sim.jar \
    --port=/tmp/ttyV0 \
    --mode=ehl \
    --logHex=true &
SIM_PID=$!
sleep 2

if ! ps -p $SIM_PID > /dev/null; then
    echo -e "${RED}ERROR: Simulator failed to start${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Simulator running (PID: $SIM_PID)${NC}"
echo

echo "Step 3: Sending LINETEST command..."
echo "Frame: 10 06 31 6A 4D 36"
echo "  - STX: 0x10 (Controller)"
echo "  - LEN: 0x06 (6 bytes)"
echo "  - ADDR: 0x31 (dispenser '1')"
echo "  - CMD: 0x6A (LINETEST)"
echo "  - CHK: 0x4D (XOR: 10^06^31^6A)"
echo "  - ETX: 0x36"
echo

# Send LINETEST frame
printf '\x10\x06\x31\x6A\x4D\x36' > /tmp/ttyV1
sleep 1
echo -e "${GREEN}✓ LINETEST frame sent${NC}"
echo

echo "Step 4: Reading response..."
# Read response with timeout
RESPONSE=$(timeout 2 dd if=/tmp/ttyV1 bs=1 count=6 2>/dev/null | xxd -p)

if [ -z "$RESPONSE" ]; then
    echo -e "${RED}ERROR: No response received${NC}"
    exit 1
fi

echo "Received: $RESPONSE"
echo

# Expected response: 20 06 31 1E 09 36
EXPECTED="2006311e0936"
EXPECTED_NORMALIZED=$(echo $EXPECTED | tr -d ' ')
RESPONSE_NORMALIZED=$(echo $RESPONSE | tr -d ' ')

if [ "$RESPONSE_NORMALIZED" = "$EXPECTED_NORMALIZED" ]; then
    echo -e "${GREEN}✓ SUCCESS: Received correct OK response!${NC}"
    echo "Response breakdown:"
    echo "  - STX: 0x20 (Dispenser)"
    echo "  - LEN: 0x06"
    echo "  - ADDR: 0x31"
    echo "  - CMD: 0x1E (OK)"
    echo "  - CHK: 0x09"
    echo "  - ETX: 0x36"
else
    echo -e "${RED}ERROR: Unexpected response${NC}"
    echo "Expected: $EXPECTED"
    echo "Got:      $RESPONSE"
    exit 1
fi

echo
echo "=== Test Complete ==="
