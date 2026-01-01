#!/usr/bin/env python3
"""
ECR Server V8 - NEWLINE EDITION (Final attempt)
Adds newline terminator and sends purchase as immediate response to heartbeat
"""

import socket
import time
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def start_server():
    print("="*70)
    print("💳 ECR SERVER V8 - NEWLINE EDITION")
    print("="*70)
    print()
    print("Strategi: Legg til \\n (Enter) bak kommandoen.")
    print("          Bruk kjøpskommandoen som SVAR på ping.")
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
                
                print("👂 Listening for heartbeats...")
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        data = conn.recv(1024)
                        
                        if not data: 
                            print("\n🔌 Terminal closed connection")
                            break
                        
                        # Sjekk om det er en Ping/Heartbeat
                        hex_data = data.hex()
                        text_data = data.decode('iso-8859-1', errors='ignore')
                        
                        # Er det heartbeat? (Bare nuller eller [00])
                        is_heartbeat = hex_data.replace('00', '') == '' or '[00]' in text_data
                        
                        if is_heartbeat:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            if not purchase_sent:
                                print("\n\n⚡ Heartbeat mottatt! Svarer med KJØP (med Newline)...")
                                print()
                                print(f"{'─'*70}")
                                print("💰 SENDING PURCHASE COMMAND WITH NEWLINE")
                                print(f"{'─'*70}")
                                print()
                                
                                # KJØPSKOMMANDO MED NEWLINE (\r\n)
                                # Format: [10;1;100;0;0]\r\n
                                # \r\n = Carriage Return + Line Feed (DOS/Windows style)
                                cmd = b'[10;1;100;0;0]\r\n'
                                
                                conn.sendall(cmd)
                                print(f"🚀 TX: {repr(cmd)}")
                                print(f"   Command: [10;1;100;0;0]")
                                print(f"   Terminator: \\r\\n (CR+LF)")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should react now!")
                                print()
                                purchase_sent = True
                            else:
                                # Vi har allerede sendt kjøp, svar bare med Ping for å holde linja i live
                                conn.sendall(b'[00]')
                        
                        else:
                            # Vi fikk faktisk data (tekst)!
                            hex_display = ' '.join(f'{b:02X}' for b in data)
                            
                            print(f"\n\n📥 RX: {text_data}")
                            print(f"   Hex: {hex_display}")
                            print()
                            
                            # Answer/Receipt
                            if 'A;' in text_data or text_data.startswith('A'):
                                print("="*70)
                                print("🎉🎉🎉 KVITTERING! TERMINALEN SVARTE! 🎉🎉🎉")
                                print("="*70)
                                print(f"SVAR: {text_data}")
                                print()
                                
                                if 'FEIL' not in text_data.upper():
                                    print("✅✅✅ SUKSESS! TRANSAKSJONEN ER GODKJENT! ✅✅✅")
                                else:
                                    print("⚠️  Feil format, men kontakt oppnådd!")
                                    print("   Vi kommuniserer på riktig protokoll!")
                                print()
                            
                            # Feilmeldinger
                            if 'FEIL' in text_data.upper():
                                print("⚠️  Feil format, men kontakt oppnådd!")
                                print("   Dette er fremgang - vi snakker samme protokoll!")
                                print()
                            
                            # Display meldinger
                            if 'D!' in text_data:
                                display_msg = text_data.replace('D!', '').strip()
                                print(f"📺 DISPLAY: {display_msg}")
                                print()
                            
                            # Bracket kommandoer
                            if text_data.startswith('[') and ';' in text_data:
                                print(f"ℹ️  Bracket response: {text_data}")
                                
                                if '[20;' in text_data:
                                    print("   ✅ Success code!")
                                elif '[99;' in text_data:
                                    print("   ❌ Error code")
                                print()

                    except socket.timeout:
                        print("\n⏰ Timeout. Sending probe...")
                        if purchase_sent:
                            print("   (Purchase already sent)")
                        conn.sendall(b'[00]')
                        print()
                    
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
