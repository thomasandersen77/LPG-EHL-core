#!/usr/bin/env python3
"""
ECR Server V10 - PING PONG EDITION
Responds to heartbeat FIRST, then sends purchase command
This is proper etiquette!
"""

import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """Lager pakke med 2-byte lengde-header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("💳 ECR SERVER V10 - PING PONG EDITION")
    print("="*70)
    print()
    print("Strategi: Svar ALLTID på Heartbeat med Heartbeat.")
    print("          Send kjøp etterpå (høflig versjon).")
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
                
                purchase_sent = False
                
                print("👂 Listening for heartbeats (ping-pong protocol)...")
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
                            # 00 00 = Heartbeat fra terminal
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            # 1. SVAR PÅ HEARTBEAT (Ping -> Pong)
                            # Dette er VIKTIG - vi må svare høflig!
                            conn.sendall(b'\x00\x00')
                            
                            if not purchase_sent:
                                # Vent litt for å la terminalen fordøye Pong-en
                                time.sleep(0.5)
                                
                                print("\n\n✅ Link verified (ping-pong OK)")
                                print()
                                print(f"{'─'*70}")
                                print("💰 SENDING PURCHASE COMMAND")
                                print(f"{'─'*70}")
                                print()
                                
                                # 2. SEND KJØP (Verifone Format)
                                # [Kommando;Seq;Beløp;Moms;Cashback]
                                # 10 = Purchase
                                # 1 = Sequence number
                                # 100 = Amount in øre (1.00 NOK)
                                # 0 = VAT
                                # 0 = Cashback
                                cmd_text = '[10;1;100;0;0]'
                                full_packet = create_packet(cmd_text)
                                
                                conn.sendall(full_packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in full_packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Command: {cmd_text}")
                                print(f"   Amount: 1.00 NOK")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should change from 'Vennligst vent'")
                                print("   💡 to 'Beløp: 1.00 kr'")
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
                            
                            # Answer/Receipt
                            if 'A;' in text or text.startswith('A'):
                                print("="*70)
                                print("🎉🎉🎉 KVITTERING MOTTATT! 🎉🎉🎉")
                                print("="*70)
                                print(f"Terminal Response: {text}")
                                print()
                                
                                if 'FEIL' not in text.upper():
                                    print("✅✅✅ SUKSESS! TRANSAKSJONEN ER GODKJENT! ✅✅✅")
                                else:
                                    print("⚠️  Feil format, men vi kommuniserer!")
                                print()
                            
                            # Display meldinger
                            if 'D!' in text:
                                display_msg = text.replace('D!', '').strip()
                                print(f"📺 DISPLAY: {display_msg}")
                                print()
                            
                            # Feilmeldinger
                            if 'FEIL' in text.upper():
                                print("⚠️  Terminal klager på format")
                                print("   Men vi kommuniserer på riktig protokoll!")
                                print()
                            
                            # Bracket kommandoer
                            if text.startswith('['):
                                print(f"ℹ️  Bracket response: {text}")
                                
                                if '[20;' in text:
                                    print("   ✅ Success code!")
                                elif '[99;' in text:
                                    print("   ❌ Error code")
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
