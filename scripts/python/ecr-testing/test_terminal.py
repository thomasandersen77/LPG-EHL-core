#!/usr/bin/env python3
"""
Nets/BAX Terminal Test Script
Sends a 1.00 NOK purchase request to the physical terminal
"""

import socket
import sys
import time

# Protocol constants
STX = 0x02
ETX = 0x03
ACK = 0x06
NAK = 0x15

# Terminal configuration
HOST = "192.168.0.41"
PORT = 8009
TIMEOUT = 15  # seconds

def calculate_lrc(data):
    """Calculate LRC (XOR checksum)"""
    lrc = 0
    for byte in data:
        lrc ^= byte
    return lrc & 0xFF

def build_frame(payload):
    """Build BAX protocol frame: STX + payload + ETX + LRC"""
    payload_bytes = payload.encode('iso-8859-1')
    
    # Build frame without LRC first
    frame = bytearray([STX])
    frame.extend(payload_bytes)
    frame.append(ETX)
    
    # Calculate LRC over payload + ETX (everything after STX)
    lrc = calculate_lrc(frame[1:])
    frame.append(lrc)
    
    return bytes(frame)

def hexdump(data):
    """Format bytes as hex string"""
    return ' '.join(f'{b:02X}' for b in data)

def parse_response(data):
    """Parse response from terminal"""
    if not data:
        return None
    
    # Single byte responses
    if len(data) == 1:
        if data[0] == ACK:
            return "ACK"
        elif data[0] == NAK:
            return "NAK"
    
    # Frame response
    try:
        stx_idx = data.index(STX)
        etx_idx = data.index(ETX)
        if stx_idx >= 0 and etx_idx > stx_idx:
            payload = data[stx_idx+1:etx_idx]
            return payload.decode('iso-8859-1')
    except (ValueError, UnicodeDecodeError):
        pass
    
    return None

print("=" * 60)
print("🔧 NETS/BAX TERMINAL TEST")
print("=" * 60)
print(f"Terminal: {HOST}:{PORT}")
print("Amount: 1.00 NOK (100 øre)")
print()

try:
    # Connect to terminal
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(5)
    
    print(f"⏳ Connecting to {HOST}:{PORT}...")
    sock.connect((HOST, PORT))
    print("✅ Connected to terminal")
    
    # Build and send purchase command
    # Format: P,<OperatorID>,<AmountCents>
    purchase_cmd = "P,1,100"  # 1 NOK = 100 øre
    frame = build_frame(purchase_cmd)
    
    print()
    print("📤 Sending Purchase command:")
    print(f"   Command: {purchase_cmd}")
    print(f"   Frame: {hexdump(frame)}")
    print()
    
    sock.sendall(frame)
    
    print(f"⏳ Waiting for terminal response (timeout: {TIMEOUT}s)...")
    print("   (Terminal should now light up and display: 'Beløp: 1.00 kr')")
    print("   (Insert test card or cancel to see response)")
    print()
    
    # Read response with longer timeout for user interaction
    sock.settimeout(TIMEOUT)
    response = bytearray()
    start_time = time.time()
    
    while time.time() - start_time < TIMEOUT:
        try:
            chunk = sock.recv(1024)
            if not chunk:
                break
            response.extend(chunk)
            
            # Check if we have complete frame
            if STX in response and ETX in response:
                etx_idx = response.index(ETX)
                if etx_idx + 1 < len(response):  # Have LRC byte
                    break
        except socket.timeout:
            if response:
                break
            continue
    
    if not response:
        print("❌ No response from terminal")
        print()
        print("Possible issues:")
        print("  - Terminal not powered on")
        print("  - ECR mode not enabled")
        print("  - Wrong IP address or port")
        print("  - Firewall blocking connection")
        print()
        print("To check:")
        print(f"  1. Verify terminal settings (Menu → ECR → IP: {HOST})")
        print(f"  2. Verify ECR port is {PORT}")
        print("  3. Ensure ECR mode is enabled")
        print(f"  4. Test connectivity: ping {HOST}")
    else:
        print(f"📥 Received response ({len(response)} bytes):")
        print(f"   Hex: {hexdump(response)}")
        print()
        
        parsed = parse_response(response)
        if parsed:
            print(f"   📄 Parsed: {parsed}")
            print()
            
            if parsed == "ACK":
                print("   ✅ Terminal acknowledged command")
            elif parsed == "NAK":
                print("   ❌ Terminal rejected (NAK)")
                print("      Possible reasons:")
                print("      - ECR not registered (check terminal menu)")
                print("      - Terminal busy or in wrong mode")
            elif "APPROVED" in parsed.upper():
                print("   ✅ PAYMENT APPROVED!")
            elif "DECLINED" in parsed.upper():
                print("   ❌ Payment declined")
            elif "NO ECR" in parsed.upper():
                print("   ⚠️  ECR not registered in terminal")
                print("      Go to: Menu → ECR → Register")
            elif "CANCEL" in parsed.upper():
                print("   ⚠️  Transaction cancelled by user")
            else:
                print(f"   ℹ️  Terminal response: {parsed}")
    
    sock.close()
    
except ConnectionRefusedError:
    print("❌ Connection refused!")
    print()
    print("Terminal is not accepting connections on port 8009.")
    print()
    print("Troubleshooting steps:")
    print(f"  1. Verify terminal IP: ping {HOST}")
    print(f"  2. Check if something is listening: nc -zv {HOST} {PORT}")
    print("  3. Check terminal settings:")
    print("     - Menu → Network → IP address")
    print("     - Menu → ECR → Port (should be 8009)")
    print("     - Menu → ECR → Enable ECR")
    
except socket.timeout:
    print("⏱️  Timeout waiting for response")
    print()
    print("Terminal connected but no response received.")
    print("This might mean:")
    print("  - Terminal waiting for card insertion")
    print("  - Transaction in progress")
    print("  - Terminal not in ECR mode")
    
except Exception as e:
    print(f"❌ Error: {e}")
    print()
    print("Troubleshooting:")
    print(f"  - Verify IP: {HOST}")
    print(f"  - Verify port: {PORT}")
    print(f"  - Test connectivity: ping {HOST}")

print()
print("=" * 60)
