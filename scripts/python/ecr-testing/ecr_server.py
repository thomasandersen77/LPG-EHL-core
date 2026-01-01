#!/usr/bin/env python3
"""
ECR Server - Acts as Electronic Cash Register
Listens on port 8009 for payment terminal connections
"""

import socket
import threading
import time
import sys

# Protocol constants
STX = 0x02
ETX = 0x03
ACK = 0x06
NAK = 0x15
ENQ = 0x05

PORT = 8009

def calculate_lrc(data):
    lrc = 0
    for byte in data:
        lrc ^= byte
    return lrc & 0xFF

def build_frame(payload):
    payload_bytes = payload.encode('iso-8859-1')
    frame = bytearray([STX])
    frame.extend(payload_bytes)
    frame.append(ETX)
    lrc = calculate_lrc(frame[1:])
    frame.append(lrc)
    return bytes(frame)

def hexdump(data):
    return ' '.join(f'{b:02X}' for b in data)

def parse_response(data):
    if not data:
        return None
    if len(data) == 1:
        if data[0] == ACK:
            return "ACK"
        elif data[0] == NAK:
            return "NAK"
        elif data[0] == ENQ:
            return "ENQ"
    try:
        if STX in data:
            stx_idx = data.index(STX)
            etx_idx = data.index(ETX)
            if stx_idx >= 0 and etx_idx > stx_idx:
                payload = data[stx_idx+1:etx_idx]
                return payload.decode('iso-8859-1', errors='replace')
    except (ValueError, UnicodeDecodeError):
        pass
    return None

def handle_terminal(client_socket, client_addr):
    """Handle communication with connected terminal"""
    print(f"\n{'='*70}")
    print(f"📱 TERMINAL CONNECTED from {client_addr[0]}:{client_addr[1]}")
    print(f"{'='*70}\n")
    
    try:
        client_socket.settimeout(30)
        
        # Wait a moment for terminal to send initial handshake
        time.sleep(0.5)
        
        # Check if terminal sent anything
        try:
            client_socket.settimeout(2)
            initial = client_socket.recv(1024)
            if initial:
                print(f"📥 Terminal sent initial message: {hexdump(initial)}")
                parsed = parse_response(initial)
                if parsed:
                    print(f"   Parsed: {parsed}")
                
                # Respond with ACK
                print(f"📤 Sending ACK response")
                client_socket.send(bytes([ACK]))
                time.sleep(0.3)
        except socket.timeout:
            print("   (No initial message from terminal)")
        
        # Now send Purchase command
        print(f"\n{'─'*70}")
        print("💰 Sending PURCHASE command: 1.00 NOK")
        print(f"{'─'*70}\n")
        
        purchase_frame = build_frame("P,1,100")
        print(f"📤 TX: {hexdump(purchase_frame)}")
        print(f"   Command: P,1,100")
        print()
        
        client_socket.send(purchase_frame)
        
        print("⏳ Waiting for terminal response...")
        print("   💡 Terminal should light up now and show: 'Beløp: 1.00 kr'")
        print()
        
        # Wait for response
        client_socket.settimeout(20)
        response = bytearray()
        start = time.time()
        
        while time.time() - start < 20:
            try:
                chunk = client_socket.recv(1024)
                if not chunk:
                    break
                
                response.extend(chunk)
                print(f"📥 RX: {hexdump(chunk)}")
                
                # Check if we have complete response
                if response:
                    if response[0] == ACK:
                        print("   ✅ ACK received")
                        # Transaction in progress, wait for more
                        continue
                    elif response[0] == NAK:
                        print("   ❌ NAK received")
                        break
                    elif STX in response and ETX in response:
                        etx_idx = response.index(ETX)
                        if etx_idx + 1 < len(response):
                            break
            except socket.timeout:
                if response:
                    break
                print("   ⏱️  Still waiting...")
        
        print()
        print(f"{'─'*70}")
        print("📋 FINAL RESPONSE:")
        print(f"{'─'*70}")
        
        if response:
            print(f"   Raw ({len(response)} bytes): {hexdump(response)}")
            
            parsed = parse_response(response)
            if parsed:
                print(f"   Parsed: {parsed}")
                print()
                
                if "APPROVED" in parsed.upper():
                    print("   ✅✅✅ PAYMENT APPROVED! ✅✅✅")
                elif "DECLINED" in parsed.upper():
                    print("   ❌ Payment declined")
                elif "CANCEL" in parsed.upper():
                    print("   ⚠️  Transaction cancelled")
                elif parsed == "ACK":
                    print("   ✅ Transaction acknowledged")
                elif parsed == "NAK":
                    print("   ❌ Transaction rejected")
                else:
                    print(f"   ℹ️  Terminal response: {parsed}")
        else:
            print("   ❌ No response received")
        
        print()
        print("🔌 Keeping connection open for 10 more seconds...")
        client_socket.settimeout(10)
        
        # Listen for any additional messages
        try:
            while True:
                more = client_socket.recv(1024)
                if not more:
                    break
                print(f"📥 Additional message: {hexdump(more)}")
                parsed = parse_response(more)
                if parsed:
                    print(f"   {parsed}")
        except socket.timeout:
            pass
        
    except Exception as e:
        print(f"❌ Error handling terminal: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        client_socket.close()
        print(f"\n{'='*70}")
        print(f"📱 TERMINAL DISCONNECTED")
        print(f"{'='*70}\n")

def main():
    print("=" * 70)
    print("💳 ECR SERVER - Electronic Cash Register")
    print("=" * 70)
    print()
    print(f"Listening on port {PORT}...")
    print()
    print("🔧 INSTRUCTIONS:")
    print("   1. This server is now running and waiting for the terminal")
    print("   2. On the payment terminal, go to: Menu → ECR → Register")
    print("   3. Terminal should connect to this server automatically")
    print("   4. Server will send a 1.00 NOK purchase command")
    print()
    print("Press Ctrl+C to stop")
    print("=" * 70)
    print()
    
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    try:
        server_socket.bind(('0.0.0.0', PORT))
        server_socket.listen(1)
        
        print(f"✅ Server listening on 0.0.0.0:{PORT}")
        print()
        print("⏳ Waiting for terminal to connect...")
        print()
        
        while True:
            client_socket, client_addr = server_socket.accept()
            
            # Handle each connection in a new thread
            thread = threading.Thread(
                target=handle_terminal,
                args=(client_socket, client_addr)
            )
            thread.start()
    
    except KeyboardInterrupt:
        print("\n\n⚠️  Shutting down server...")
    except Exception as e:
        print(f"❌ Server error: {e}")
    finally:
        server_socket.close()
        print("✅ Server stopped")
        print()

if __name__ == "__main__":
    main()
