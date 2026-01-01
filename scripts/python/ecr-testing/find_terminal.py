#!/usr/bin/env python3
"""
Network scanner to find Nets/BAX payment terminal
Scans local network for devices listening on port 8009 (ECR port)
"""

import socket
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

PORT = 8009
TIMEOUT = 0.5  # seconds

def check_host(ip, port):
    """Check if a host is listening on the given port"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(TIMEOUT)
        result = sock.connect_ex((ip, port))
        sock.close()
        return (ip, result == 0)
    except:
        return (ip, False)

def scan_network(network_prefix, port):
    """Scan network range for open ports"""
    print(f"🔍 Scanning {network_prefix}.0/24 for port {port}...")
    print(f"⏳ This may take a minute...\n")
    
    found = []
    
    # Use thread pool for parallel scanning
    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = []
        for i in range(1, 255):
            ip = f"{network_prefix}.{i}"
            futures.append(executor.submit(check_host, ip, port))
        
        completed = 0
        for future in as_completed(futures):
            completed += 1
            if completed % 50 == 0:
                print(f"   Progress: {completed}/254 hosts checked...")
            
            ip, is_open = future.result()
            if is_open:
                found.append(ip)
                print(f"\n✅ Found device listening on {ip}:{port}")
    
    return found

if __name__ == "__main__":
    print("=" * 60)
    print("🔧 TERMINAL NETWORK SCANNER")
    print("=" * 60)
    print()
    
    # Scan 192.168.0.x network
    network = "192.168.0"
    terminals = scan_network(network, PORT)
    
    print()
    print("=" * 60)
    
    if not terminals:
        print("❌ No terminals found on port 8009")
        print()
        print("Possible reasons:")
        print("  - Terminal is on a different network")
        print("  - Terminal ECR mode is not enabled")
        print("  - Terminal is using a different port")
        print("  - Terminal is powered off")
        print()
        print("Next steps:")
        print("  1. Check terminal display for IP address")
        print("  2. Verify ECR is enabled (Menu → ECR → Enable)")
        print("  3. Check ECR port setting (should be 8009)")
    else:
        print(f"✅ Found {len(terminals)} terminal(s):")
        for ip in terminals:
            print(f"   📟 {ip}:{PORT}")
        print()
        print("Next step: Run test with found terminal:")
        print(f"   python3 test_terminal.py")
        print(f"   (or edit test_terminal.py to use: HOST = '{terminals[0]}')")
    
    print("=" * 60)
