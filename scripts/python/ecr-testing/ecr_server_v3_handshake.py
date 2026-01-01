#!/usr/bin/env python3
"""
ECR Server V3 - Handshake Edition
Handles Ingenico/Nets I1 initialization before sending purchase
"""

import socket
import time

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def calculate_lrc(data_bytes):
    lrc = 0
    for b in data_bytes:
        lrc ^= b
    return lrc

def create_baxi_packet(command_str):
    """Lager en Baxi-pakke: STX + Command + ETX + LRC"""
    stx = b'\x02'
    etx = b'\x03'
    payload = command_str.encode('iso-8859-1')
    lrc_base = payload + etx
    lrc = calculate_lrc(lrc_base)
    return stx + payload + etx + bytes([lrc])

def create_framed_packet(payload_bytes):
    """Pakker inn data med 2 bytes lengde-header"""
    length = len(payload_bytes)
    header = length.to_bytes(2, byteorder='big')
    return header + payload_bytes

def hexdump(data):
    """Format bytes as hex string"""
    return ' '.join(f'{b:02X}' for b in data)

def start_server():
    print("="*70)
    print("💳 ECR SERVER V3 - HANDSHAKE EDITION")
    print("="*70)
    print()
    print("Strategi:")
    print("  1. Lytt etter I1 melding fra terminal")
    print("  2. Svar med I2 (kasse-identifikasjon)")
    print("  3. Send Purchase kommando")
    print()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        print("⏳ Waiting for terminal...")
        print()
        
        while True:
            conn, addr = s.accept()
            with conn:
                print(f"{'='*70}")
                print(f"📱 TERMINAL CONNECTED from {addr[0]}:{addr[1]}")
                print(f"{'='*70}")
                print()
                
                # 1. Vent på INIT melding (I1...)
                try:
                    conn.settimeout(10)
                    header = conn.recv(2)
                    if not header: 
                        print("❌ No header received")
                        continue
                    
                    msg_len = int.from_bytes(header, 'big')
                    print(f"📥 Message length: {msg_len} bytes")
                    
                    data = conn.recv(msg_len)
                    print(f"📥 RX (Init): {hexdump(data)}")
                    print(f"   ASCII: {data.decode('iso-8859-1', errors='replace')}")
                    print()
                    
                    # Sjekk om det er en "I1" melding (Identifikasjon)
                    if b'I1;' in data or b'I1' in data:
                        print("👋 Terminal sa HEI (I1). Sender svar (I2)...")
                        
                        # Svar med I2 (Kasse-identifikasjon)
                        # Format: I2;Versjon;KasseID;
                        response_str = "I2;1.0.0;POS1;"
                        response_bytes = response_str.encode('iso-8859-1')
                        response_packet = create_framed_packet(response_bytes)
                        
                        conn.sendall(response_packet)
                        print(f"📤 TX (Handshake): {response_str}")
                        print(f"   Hex: {hexdump(response_packet)}")
                        print()
                        
                        time.sleep(1) # Vent litt før vi sender kjøp
                        
                        # 2. NÅ sender vi Purchase
                        print(f"{'─'*70}")
                        print("💰 Sending PURCHASE: 1.00 NOK")
                        print(f"{'─'*70}")
                        
                        baxi_cmd = create_baxi_packet("P,1,100")
                        purchase_packet = create_framed_packet(baxi_cmd)
                        
                        print(f"📤 TX (Purchase): {hexdump(purchase_packet)}")
                        print(f"   Baxi command: P,1,100")
                        print()
                        
                        conn.sendall(purchase_packet)
                        
                        print("⏳ Waiting for response...")
                        print("   💡 Terminal should light up now!")
                        print()
                        
                        # 3. Lytt etter svar på kjøp
                        conn.settimeout(30)
                        while True:
                            try:
                                h = conn.recv(2)
                                if not h: 
                                    print("🔌 Terminal disconnected")
                                    break
                                
                                l = int.from_bytes(h, 'big')
                                d = conn.recv(l)
                                
                                print(f"📥 RX (Response): {hexdump(d)}")
                                print(f"   ASCII: {d.decode('iso-8859-1', errors='replace')}")
                                
                                if b'\x06' in d:
                                    print("   ✅ ACK RECEIVED!")
                                elif b'APPROVED' in d.upper():
                                    print("   ✅✅✅ PAYMENT APPROVED! ✅✅✅")
                                elif b'DECLINED' in d.upper():
                                    print("   ❌ Payment declined")
                                elif b'CANCEL' in d.upper():
                                    print("   ⚠️  Transaction cancelled")
                                
                                print()
                                
                            except socket.timeout:
                                print("⏱️  Timeout waiting for more data")
                                break
                        
                    else:
                        # Hvis det ikke var I1, prøv å send ACK uansett (fallback)
                        print("⚠️  Ukjent melding (ikke I1). Prøver fallback...")
                        print()
                        
                        # Send ACK
                        conn.sendall(create_framed_packet(b'\x06'))
                        print("📤 TX: ACK")
                        time.sleep(0.5)
                        
                        # Send Purchase
                        baxi_cmd = create_baxi_packet("P,1,100")
                        conn.sendall(create_framed_packet(baxi_cmd))
                        print("📤 TX: Purchase command")
                        print()
                        
                        # Lytt etter svar
                        try:
                            h = conn.recv(2)
                            if h:
                                l = int.from_bytes(h, 'big')
                                d = conn.recv(l)
                                print(f"📥 RX: {hexdump(d)}")
                        except:
                            pass

                except Exception as e:
                    print(f"❌ Error: {e}")
                    import traceback
                    traceback.print_exc()
                
                print()
                print(f"{'='*70}")
                print("🔌 Connection closed")
                print(f"{'='*70}")
                print()

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n⚠️  Server stopped by user")
        print("✅ Goodbye!")
