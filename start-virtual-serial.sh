#!/usr/bin/env bash
#
# Start virtual serial port pair using socat.
# This creates two linked serial ports that act like a null-modem cable.
#
# Usage:
#   ./start-virtual-serial.sh
#
# The simulator should connect to /tmp/vserial0
# Test clients (Python scripts, etc.) connect to /tmp/vserial1

set -euo pipefail

echo "════════════════════════════════════════════════════════════"
echo "  🔌 Virtual Serial Port Pair Creator"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Creating paired virtual serial ports using socat..."
echo ""
echo "  Port 0: /tmp/vserial0  ← Simulator listens here"
echo "  Port 1: /tmp/vserial1  ← Clients connect here"
echo ""
echo "────────────────────────────────────────────────────────────"
echo ""
echo "Example usage:"
echo ""
echo "  Terminal 1 (this one):"
echo "    ./start-virtual-serial.sh"
echo ""
echo "  Terminal 2 (run simulator):"
echo "    java -jar release/pls-sim.jar --port /tmp/vserial0"
echo ""
echo "  Terminal 3 (test with Python):"
echo "    python3 python-test/01_probe_readonly.py --port /tmp/vserial1 --addr 1"
echo ""
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Check if socat is installed
if ! command -v socat &> /dev/null; then
    echo "❌ Error: socat is not installed"
    echo ""
    echo "Install socat:"
    echo "  macOS:  brew install socat"
    echo "  Ubuntu: sudo apt-get install socat"
    echo "  Debian: sudo apt-get install socat"
    exit 1
fi

# Clean up old symlinks if they exist
rm -f /tmp/vserial0 /tmp/vserial1

# Start socat with virtual serial pair
# Options:
#   -d -d         : Double verbose (show connection info)
#   pty,rawer     : Create pseudo-terminal in raw mode
#   echo=0        : Disable echo
#   link=...      : Create stable symlink at specified path
socat -d -d \
    pty,rawer,echo=0,link=/tmp/vserial0 \
    pty,rawer,echo=0,link=/tmp/vserial1
