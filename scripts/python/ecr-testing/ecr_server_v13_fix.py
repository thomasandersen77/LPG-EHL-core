#!/usr/bin/env python3
"""
ECR Server V13 - RESPONSIVE LOGIN (FIX)
Fixes V12 by responding to ALL heartbeats (00 00) to keep connection alive
Then does proper login + purchase sequence
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
    print("💳 ECR SERVER V13 - RESPONSIVE LOGIN")
    print("="*70)
    print()
    print("Strategi: Svar ALLTID på Heartbeat (00 00) - VIKTIG FIX!")
    print("          Send LOGON ([01;1]) med en gang vi har kontakt.")
    print("          Send KJØP ([10;...]) når vi er logget inn.")
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
                
                print("👂 Responding to heartbeats and sending commands...")
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
                            # 1. HEARTBEAT MOTTATT (00 00)
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            # VIKTIG FIX: SVAR TILBAKE MED HEARTBEAT!
                            # Dette holder forbindelsen i live og ECR grønn
                            conn.sendall(b'\x00\x00')
                            
                            # Hvis vi ikke har logget inn ennå, benytt sjansen nå!
                            if not logon_sent:
                                time.sleep(0.5)
                                
                                print("\n\n✅ Link alive! Sending LOGON...")
                                print(f"{'─'*70}")
                                print("🔑 VERIFONE LOGON")
                                print(f"{'─'*70}")
                                print()
                                
                                cmd = '[01;1]'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: [01;SequenceNo]")
                                print()
                                logon_sent = True
                            
                            continue

                        # Vi fikk data (lengde > 0)!
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

                        # 2. ANALYSER SVAR
                        
                        # Terminalen bekrefter mottak med [00]
                        if '[00]' in text:
                            print("❤️  ACK ([00]) mottatt")
                            print("   Terminal bekrefter mottak av kommando")
                            print()
                            # Terminal har mottatt vår kommando, venter på endelig svar
                        
                        # Terminalen sender Svar (A = Answer)
                        elif 'A;' in text or text.startswith('[A') or text.startswith('A'):
                            print("="*70)
                            print("🎉 SVAR FRA TERMINAL!")
                            print("="*70)
                            print(f"Response: {text}")
                            print()
                            
                            # Sjekk for feil
                            if 'FEIL' in text.upper():
                                print("⚠️  Terminal melder FEIL")
                                print("   Men vi kommuniserer - protokollen er riktig!")
                                print()
                                
                                # Prøv purchase direkte hvis logon feilet
                                if logon_sent and not purchase_sent:
                                    print("🔄 Prøver Purchase direkte...")
                                    time.sleep(0.5)
                                    cmd = '[10;2;100;0;0]'
                                    packet = create_packet(cmd)
                                    conn.sendall(packet)
                                    print(f"🚀 TX: {cmd}")
                                    print()
                                    purchase_sent = True
                            
                            # Logon godkjent - send purchase
                            elif logon_sent and not purchase_sent:
                                print("✅ Logon OK! Sending PURCHASE...")
                                print()
                                time.sleep(0.5)
                                
                                print(f"{'─'*70}")
                                print("💰 VERIFONE PURCHASE")
                                print(f"{'─'*70}")
                                print()
                                
                                # [10;Seq;Amount;VAT;Cashback]
                                cmd = '[10;2;100;0;0]'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: [10;Seq;Amount;VAT;Cashback]")
                                print(f"   Amount: 1.00 NOK (100 øre)")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should wake up NOW!")
                                print()
                                purchase_sent = True
                                
                            # Purchase godkjent
                            elif purchase_sent:
                                print("="*70)
                                print("💰💰💰 KJØP BEKREFTET/BEHANDLET! 💰💰💰")
                                print("="*70)
                                print(f"Result: {text}")
                                print()
                                
                                if 'FEIL' not in text.upper():
                                    print("✅✅✅ SUKSESS! TRANSAKSJONEN ER GODKJENT! ✅✅✅")
                                print()

                    except socket.timeout:
                        print("\n⏰ Timeout.")
                        if purchase_sent:
                            print("   Purchase already sent, waiting for terminal response...")
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
