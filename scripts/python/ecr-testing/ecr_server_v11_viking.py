#!/usr/bin/env python3
"""
ECR Server V11 - VIKING PROTOCOL EDITION
Uses Nets Viking/Integra protocol based on I1 evidence
Handshake: I2;999;1.0;
Purchase: P;1;100
"""

import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """Pakker inn Viking-kommando med 2-byte lengde-header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("💳 ECR SERVER V11 - VIKING PROTOCOL EDITION")
    print("="*70)
    print()
    print("Strategi: Bytt til Nets Viking kommandoer (I2 og P;...)")
    print("          Format: Lengde + 'P;1;100'")
    print("          Basert på I1-sporet fra tidligere tester!")
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
                
                handshake_sent = False
                purchase_sent = False
                
                print("👂 Listening for heartbeats (Viking protocol)...")
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
                            # Heartbeat (00 00)
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            # 1. SVAR PÅ HEARTBEAT
                            conn.sendall(b'\x00\x00')
                            
                            # Logikk for sekvens: Først I2 (Handshake), så P (Purchase)
                            if not handshake_sent:
                                time.sleep(0.5)
                                
                                print("\n\n🤝 VIKING HANDSHAKE")
                                print(f"{'─'*70}")
                                print("Sender I2 (Kasse-identifikasjon)...")
                                print()
                                
                                # I2;KasseID;Versjon;
                                cmd = 'I2;999;1.0;'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: I2;CashRegisterID;Version;")
                                print()
                                handshake_sent = True
                                
                            elif not purchase_sent:
                                time.sleep(1.0) # Vent litt etter handshake
                                
                                print(f"{'─'*70}")
                                print("💰 VIKING PURCHASE")
                                print(f"{'─'*70}")
                                print("Sender P (Purchase command)...")
                                print()
                                
                                # P;Seq;Beløp(øre);...
                                # Prøver enkelt format først
                                cmd = 'P;1;100'
                                packet = create_packet(cmd)
                                conn.sendall(packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd}")
                                print(f"   Format: P;SequenceNo;AmountInOre")
                                print(f"   Amount: 1.00 NOK (100 øre)")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should wake up now!")
                                print()
                                purchase_sent = True
                                
                        else:
                            # Vi fikk svar!
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
                            
                            # I1 = Terminal presenterer seg
                            if text.startswith('I1'):
                                print("👋 Terminal presenterte seg (I1)")
                                print("   Dette bekrefter Viking-protokollen!")
                                print("   Vi har allerede svart med I2")
                                print()
                            
                            # R = Resultat / Kvittering
                            if text.startswith('R'):
                                print("="*70)
                                print("🎉🎉🎉 KVITTERING MOTTATT! 🎉🎉🎉")
                                print("="*70)
                                print(f"Terminal Response: {text}")
                                print()
                                
                                # Parse resultat
                                parts = text.split(';')
                                if len(parts) > 1:
                                    status = parts[1] if len(parts) > 1 else "unknown"
                                    if status == '0' or status.upper() == 'OK':
                                        print("✅✅✅ SUKSESS! TRANSAKSJONEN ER GODKJENT! ✅✅✅")
                                    else:
                                        print(f"   Status: {status}")
                                print()
                            
                            # A = Answer (alternativt format)
                            if text.startswith('A'):
                                print("="*70)
                                print("🎉🎉🎉 SVAR MOTTATT! 🎉🎉🎉")
                                print("="*70)
                                print(f"Terminal Response: {text}")
                                print()
                            
                            # Feilmeldinger
                            if 'FEIL' in text.upper() or 'ERROR' in text.upper():
                                print("⚠️  Terminal klager på format")
                                print("   Men vi kommuniserer på riktig protokoll!")
                                print()

                    except socket.timeout:
                        print("\n⏰ Timeout.")
                        if purchase_sent:
                            print("   Purchase already sent, no response yet")
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
