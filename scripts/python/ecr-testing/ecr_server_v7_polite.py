#!/usr/bin/env python3
"""
ECR Server V7 - THE POLITE SERVER
Responds to heartbeats, establishes trust, then sends purchase
"""

import socket
import time
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def start_server():
    print("="*70)
    print("💳 ECR SERVER V7 - THE POLITE SERVER")
    print("="*70)
    print()
    print("Strategi: Svar på Heartbeat. Etabler tillit. SÅ send kjøp.")
    print()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        print()
        
        while True:
            print("⏳ Waiting for terminal (Green blinking)...")
            conn, addr = s.accept()
            with conn:
                print(f"\n{'='*70}")
                print(f"📱 TERMINAL CONNECTED from {addr[0]}:{addr[1]}")
                print(f"   (Client port {addr[1]} is normal - ephemeral port)")
                print(f"{'='*70}")
                print()
                
                # Variabler for tilstand
                connection_start = time.time()
                last_heartbeat = time.time()
                purchase_sent = False
                heartbeat_count = 0
                
                print("👂 Listening for heartbeats...")
                print()
                
                while True:
                    try:
                        conn.settimeout(5.0) # 5 sekunder timeout
                        data = conn.recv(1024)
                        
                        if not data: 
                            print("\n🔌 Terminal closed connection")
                            break
                        
                        hex_data = data.hex()
                        
                        # SCENARIO 1: BINÆR HEARTBEAT (00 00)
                        # Dette er det vi så i loggen din (....)
                        if hex_data.replace('00', '') == '':
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            heartbeat_count += 1
                            last_heartbeat = time.time()
                            
                            # Svar med samme mynt for å holde linja åpen
                            conn.sendall(data) 
                            
                            # Hvis vi har hatt stabil kontakt i 2 sekunder, prøv å sende kjøp
                            connection_duration = time.time() - connection_start
                            if not purchase_sent and connection_duration > 2.0:
                                print(f"\n\n✅ Connection stable ({heartbeat_count} heartbeats over {connection_duration:.1f}s)")
                                print()
                                print(f"{'─'*70}")
                                print("💰 Sending PURCHASE command...")
                                print(f"{'─'*70}")
                                print()
                                
                                # Format: [10;1;100;0;0]
                                # 10 = Purchase
                                # 1 = Sequence number
                                # 100 = Amount in øre (1.00 NOK)
                                # 0 = VAT
                                # 0 = Cashback
                                cmd = b'[10;1;100;0;0]'
                                conn.sendall(cmd)
                                print(f"🚀 TX: {cmd.decode()}")
                                print(f"   Amount: 1.00 NOK")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should light up now!")
                                print()
                                purchase_sent = True
                                
                            continue

                        # SCENARIO 2: TEKST DATA (ikke bare null-bytes)
                        text = data.decode('iso-8859-1', errors='ignore')
                        hex_display = ' '.join(f'{b:02X}' for b in data)
                        
                        print(f"\n\n📥 RX: {text}")
                        print(f"   Hex: {hex_display}")
                        print()
                        
                        # Text ping [00]
                        if '[00]' in text:
                            print("❤️  Text Ping received. Replying [00]...")
                            conn.sendall(b'[00]')
                            print("📤 TX: [00]")
                            print()
                        
                        # Answer/Receipt fra terminal
                        if 'A;' in text or text.startswith('A'):
                            print("="*70)
                            print("🎉🎉🎉 KVITTERING MOTTATT! 🎉🎉🎉")
                            print("="*70)
                            print(f"Terminal svarte: {text}")
                            print()
                            
                            if 'FEIL' not in text.upper():
                                print("✅✅✅ SUKSESS! ✅✅✅")
                            else:
                                print("⚠️  Inneholder FEIL, men vi kommuniserer!")
                            print()
                            
                        # Feilmeldinger
                        if 'FEIL' in text.upper():
                            print("⚠️  Terminalen klager på formatet, men vi kommuniserer!")
                            print("   Dette er fremgang - vi snakker samme protokoll!")
                            print()
                        
                        # Display meldinger
                        if 'D!' in text:
                            display_msg = text.replace('D!', '').strip()
                            print(f"📺 DISPLAY: {display_msg}")
                            print()
                        
                        # Bracket kommandoer
                        if text.startswith('[') and ';' in text:
                            print(f"ℹ️  Bracket command: {text}")
                            
                            if '[20;' in text:
                                print("   ✅ Success code!")
                            elif '[99;' in text:
                                print("   ❌ Error code")
                            print()

                    except socket.timeout:
                        elapsed = time.time() - last_heartbeat
                        print(f"\n⏰ Timeout ({elapsed:.1f}s since last heartbeat)")
                        
                        if purchase_sent:
                            print("   Purchase already sent, waiting for response...")
                        else:
                            print("   Sending probe [00]...")
                            try:
                                conn.sendall(b'[00]')
                            except:
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
