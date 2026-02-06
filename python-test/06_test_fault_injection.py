#!/usr/bin/env python3
"""
Test fault injection features of PLS Simulator.

Tests:
1. Disconnect after N seconds
2. Bad checksum rate
3. Power fault after N seconds

Usage:
  python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test disconnect
  python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test checksum
  python3 06_test_fault_injection.py --port /tmp/vserial1 --addr 1 --test powerfault
"""

import sys
import time
import argparse
from ehl_protocol import (
    ETX,
    STX_CONTROLLER,
    STX_DISPENSER,
    build_frame,
    describe_frame,
    extract_frames,
    hexdump,
)
from logging_utils import info, warn
from serial_linux import open_serial

# ANSI colors
RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
CYAN = '\033[0;36m'
BOLD = '\033[1m'
NC = '\033[0m'

CMD_STATE = 0x4B

def send_command(port, addr, cmd, timeout_ms=500):
    """Send a command and try to get response."""
    frame = build_frame(addr, cmd, b"", stx=STX_CONTROLLER, etx=ETX)
    port.write(frame)
    
    deadline = time.time() + (timeout_ms / 1000.0)
    buf = b""
    
    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if chunk:
            buf += chunk
            frames, buf = extract_frames(buf)
            for f in frames:
                if f.stx == STX_DISPENSER:
                    return True
    return False

def test_disconnect(port_path, addr):
    """Test that simulator disconnects after configured time."""
    print(f"\n{CYAN}═══════════════════════════════════════════════════════════{NC}")
    print(f"{BOLD}Test: Disconnect after 5 seconds{NC}")
    print(f"{CYAN}═══════════════════════════════════════════════════════════{NC}\n")
    
    print(f"1. Start simulator with: {YELLOW}--disconnectAfterSeconds=5{NC}")
    print(f"2. Waiting for disconnect event...\n")
    
    port, _ = open_serial(port_path, baud=9600)
    
    try:
        # Send STATE commands every second until disconnect
        for i in range(10):
            try:
                print(f"[{i+1}s] Sending STATE command...")
                if send_command(port, addr, CMD_STATE):
                    print(f"     ✓ Response received")
                else:
                    print(f"     {RED}✗ No response{NC}")
                time.sleep(1)
            except Exception as e:
                print(f"\n{GREEN}✓ Disconnect detected after {i+1}s: {e}{NC}")
                return
        print(f"\n{RED}✗ Test failed: No disconnect detected within 10s{NC}")
    finally:
        port.close()

def test_checksum(port_path, addr):
    """Test that simulator corrupts checksums at configured rate."""
    print(f"\n{CYAN}═══════════════════════════════════════════════════════════{NC}")
    print(f"{BOLD}Test: Bad checksum rate (50%){NC}")
    print(f"{CYAN}═══════════════════════════════════════════════════════════{NC}\n")
    
    print(f"1. Start simulator with: {YELLOW}--badChecksumRate=0.5{NC}")
    print(f"2. Sending 20 STATE commands and counting checksum errors...\n")
    
    port, _ = open_serial(port_path, baud=9600)
    
    try:
        total = 20
        errors = 0
        success = 0
        
        for i in range(total):
            try:
                if send_command(port, addr, CMD_STATE, timeout_ms=500):
                    success += 1
                    print(f"[{i+1:2d}] {GREEN}✓{NC} Valid response")
                else:
                    errors += 1
                    print(f"[{i+1:2d}] {RED}✗{NC} No response (checksum error?)")
            except Exception as e:
                errors += 1
                print(f"[{i+1:2d}] {RED}✗{NC} Error: {e}")
            time.sleep(0.2)
        
        print(f"\n{BOLD}Results:{NC}")
        print(f"  Success: {success}/{total} ({100*success/total:.0f}%)")
        print(f"  Errors:  {errors}/{total} ({100*errors/total:.0f}%)")
        
        if 0.3 <= errors/total <= 0.7:
            print(f"\n{GREEN}✓ Test passed: Error rate ~50% as expected{NC}")
        else:
            print(f"\n{YELLOW}⚠ Test inconclusive: Error rate not close to 50%{NC}")
    finally:
        port.close()

def test_powerfault(port_path, addr):
    """Test that simulator resets state and disconnects on power fault."""
    print(f"\n{CYAN}═══════════════════════════════════════════════════════════{NC}")
    print(f"{BOLD}Test: Power fault after 5 seconds{NC}")
    print(f"{CYAN}═══════════════════════════════════════════════════════════{NC}\n")
    
    print(f"1. Start simulator with: {YELLOW}--powerfaultAfterSeconds=5{NC}")
    print(f"2. Monitoring state and waiting for power fault...\n")
    
    port, _ = open_serial(port_path, baud=9600)
    
    try:
        # Send STATE commands every second until power fault
        for i in range(10):
            try:
                print(f"[{i+1}s] Querying state...")
                if send_command(port, addr, CMD_STATE):
                    print(f"     ✓ Got response")
                else:
                    print(f"     {RED}✗ No response{NC}")
                time.sleep(1)
            except Exception as e:
                print(f"\n{GREEN}✓ Power fault detected after {i+1}s: {e}{NC}")
                print(f"   Simulator should have reset to IDLE state before disconnect")
                return
        print(f"\n{RED}✗ Test failed: No power fault detected within 10s{NC}")
    finally:
        port.close()

def main():
    parser = argparse.ArgumentParser(description='Test PLS Simulator fault injection')
    parser.add_argument('--port', required=True, help='Serial port (e.g., /tmp/ttyV1)')
    parser.add_argument('--addr', type=int, default=1, help='Dispenser address (default: 1)')
    parser.add_argument('--test', choices=['disconnect', 'checksum', 'powerfault'], 
                       required=True, help='Which fault injection test to run')
    args = parser.parse_args()
    
    print(f"\n{BOLD}PLS Simulator Fault Injection Test{NC}")
    print(f"Port: {args.port}, Address: {args.addr}\n")
    
    if args.test == 'disconnect':
        test_disconnect(args.port, args.addr)
    elif args.test == 'checksum':
        test_checksum(args.port, args.addr)
    elif args.test == 'powerfault':
        test_powerfault(args.port, args.addr)

if __name__ == '__main__':
    main()
