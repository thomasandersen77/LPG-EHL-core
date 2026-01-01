#!/usr/bin/env python3
"""
ECR Server V12 - LOGIN FIRST EDITION
Verifone protocol with proper login sequence:
1. [00] Ping/Pong
2. [01;1] Logon
3. [10;2;100;0;0] Purchase
Based on A000FEIL I evidence confirming Verifone bracket protocol!
"""

import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """Pakker inn Verifone-kommando med 2-byte lengde-header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("💳 ECR SERVER V12 - LOGIN FIRST")
    print("="*70)
    print()
    print("Strategi: 1. Svar Heartbeat [00]")
    print("          2. Send LOGON ([01;1])")
    print("          3. Send PURCHASE ([10;2;100;0;0])")
    print()
    print("Basert på A000FEIL I som bekrefter Verifone bracket protocol!")
    print()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        print()
        
        while True:
            print("⏳ Waiting for terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"\n{'='*70}")
                print(f"📱 TERMINAL CONNECTED from {addr[0]}:{addr[1]}")
                print(f"{'='*70}")
                print()
                
                logon_sent = False
                purchase_sent = False
                
                print("👂 Listening for Verifone bracket messages...")
                print()
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        header = conn.recv(2)
                        
                        if not header or len(header) < 2:
                            print("\n🔌 Terminal closed connection")
                            break
                        
                        msg_len = struct.unpack('!H', header)[0]
                        
                        if msg_len == 0:
                            # Empty heartbeat - TCP keepalive
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            continue

                        # Vi fikk data!
                        print(f"\n📨 Receiving message (length: {msg_len} bytes)...")
                        
                        payload = conn.recv(msg_len)
                        
                        if len(payload) < msg_len:
                            print(f"⚠️  Incomplete message: got {len(payload)}/{msg_len} bytes")
                            continue
                        
                        text = payload.decode('iso-8859-1', errors='ignore')
                        hex_display = ' '.join(f'{b:02X}' for b in payload)
                        
                        print(f"\n📥 RX: {text}")
                        print(f"   Hex: {hex_display}")
                        print()

                        # 1. SVAR PÅ PING [00]
                        if '[00]' in text:
                            print("❤️  Ping mottatt [00]. Svarer Pong...")
                            pong_packet = create_packet('[00]')
                            conn.sendall(pong_packet)
                            print("📤 TX: [00]")
                            print()
                            
                            # Hvis vi ikke er logget inn, prøv LOGON
                            if not logon_sent:
                                time.sleep(0.5)
                                
                                print(f"{'─'*70}")
                                print("🔑 VERIFONE LOGON SEQUENCE")
                                print(f"{'─'*70}")
                                print("Sender LOGON kommando...")
                                print()
                                
                                # [01;Seq]
                                # 01 = Logon command
                                # 1 = Sequence number
                                cmd = '[01;1]'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: [01;SequenceNo]")
                                print()
                                logon_sent = True
                        
                        # 2. HÅNDTER SVAR (A = Answer)
                        elif text.startswith('[A') or ';A' in text or text.startswith('A'):
                            print("="*70)
                            print("🎉 SVAR MOTTATT FRA TERMINAL!")
                            print("="*70)
                            print(f"Response: {text}")
                            print()
                            
                            # Sjekk for feil
                            if 'FEIL' in text.upper():
                                print("⚠️  Terminal melder FEIL (men vi kommuniserer!)")
                                print("   Dette er fremgang - protokollen er bekreftet!")
                                print()
                                
                                # Hvis Logon feilet, prøv Kjøp direkte (desperat forsøk)
                                if logon_sent and not purchase_sent:
                                    print("🔄 Prøver Purchase direkte likevel...")
                                    time.sleep(0.5)
                                    cmd = '[10;2;100;0;0]'
                                    packet = create_packet(cmd)
                                    conn.sendall(packet)
                                    print(f"🚀 TX: {cmd}")
                                    print()
                                    purchase_sent = True
                            
                            # Er det svar på LOGON?
                            elif logon_sent and not purchase_sent:
                                print("✅ Logon godkjent! Sender PURCHASE...")
                                print()
                                time.sleep(0.5)
                                
                                print(f"{'─'*70}")
                                print("💰 VERIFONE PURCHASE")
                                print(f"{'─'*70}")
                                print()
                                
                                # [10;Seq;Amount;VAT;Cashback]
                                # 10 = Purchase command
                                # 2 = Sequence number (increment from logon)
                                # 100 = Amount in øre (1.00 NOK)
                                # 0 = VAT
                                # 0 = Cashback
                                cmd = '[10;2;100;0;0]'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: [10;Seq;Amount;VAT;Cashback]")
                                print(f"   Amount: 1.00 NOK (100 øre)")
                                print()
                                print("⏳ Waiting for terminal to display amount...")
                                print("   💡 Terminal should wake up NOW!")
                                print()
                                purchase_sent = True
                            
                            # Er det svar på PURCHASE?
                            elif purchase_sent:
                                print("="*70)
                                print("💰💰💰 KJØP BEKREFTET AV TERMINAL! 💰💰💰")
                                print("="*70)
                                print(f"Sluttresultat: {text}")
                                print()
                                
                                if 'FEIL' not in text.upper():
                                    print("✅✅✅ SUKSESS! TRANSAKSJONEN ER GODKJENT! ✅✅✅")
                                print()

                    except socket.timeout:
                        print("\n⏰ Timeout.")
                        if purchase_sent:
                            print("   Purchase already sent, waiting for response...")
                        print()
                        break
                    
                    except struct.error as e:
                        print(f"\n❌ Packet format error: {e}")
                        break
                    
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        import traceback
                        traceback.print_exc()
                        break
                
                print()
                print("="*70)
                print("🔌 Connection ended")
                print("="*70)
                print()
                time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n⚠️  Server stopped by user")
        print("✅ Goodbye!")
