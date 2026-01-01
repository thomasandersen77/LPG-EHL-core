#!/usr/bin/env python3
"""
ECR Server V5 - MASTER EDITION
Takes initiative immediately - sends [00] ping first without waiting
"""

import socket
import time

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def start_server():
    print("="*70)
    print("💳 ECR SERVER V5 - MASTER EDITION")
    print("="*70)
    print()
    print("Strategi: Vi sender '[00]' FØRST. Vi venter ikke.")
    print()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        print()
        
        while True:
            print("⏳ Waiting for connection...")
            conn, addr = s.accept()
            with conn:
                print(f"\n{'='*70}")
                print(f"📱 TERMINAL CONNECTED from {addr[0]}:{addr[1]}")
                print(f"{'='*70}")
                print()
                
                # STEG 1: VI TAR INITIATIVET
                print("⚡ Sender PING ([00]) umiddelbart...")
                try:
                    conn.sendall(b'[00]')
                    print("📤 TX: [00]")
                    print()
                except Exception as e:
                    print(f"❌ Kunne ikke sende: {e}")
                    continue

                # Variabel for å holde styr på om vi har sendt kjøp
                purchase_sent = False

                try:
                    conn.settimeout(30)
                    
                    while True:
                        try:
                            data = conn.recv(1024)
                            if not data: 
                                print("\n🔌 Terminal disconnected")
                                break
                            
                            hex_data = data.hex()
                            text_data = data.decode('iso-8859-1', errors='ignore')
                            
                            # Filtrer bort 00 00 støy i loggen
                            if hex_data == "0000":
                                # Ignorer heartbeat
                                print(".", end="", flush=True)
                                continue
                            
                            print(f"\n📥 RX: {text_data}")
                            print(f"   Hex: {hex_data}")
                            print()

                            # STEG 2: HÅNDTER SVAR
                            
                            # Hvis vi får [00] tilbake, eller terminalen våkner
                            if '[00]' in text_data and not purchase_sent:
                                print("✅ PONG Mottatt! Forbindelse etablert.")
                                print()
                                time.sleep(1)
                                
                                print(f"{'─'*70}")
                                print("💰 Sender KJØP: 1.00 NOK...")
                                print(f"{'─'*70}")
                                print()
                                
                                # Format: [10;Løpenr;Beløp;Moms;Cashback]
                                # 10 = Purchase command
                                # 1 = Transaction ID
                                # 100 = Amount in øre (1.00 NOK)
                                # 0 = VAT
                                # 0 = Cashback
                                cmd = b'[10;1;100;0;0]'
                                conn.sendall(cmd)
                                print(f"📤 TX: {cmd.decode()}")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should light up now!")
                                print()
                                purchase_sent = True

                            # Hvis vi får et Svar (A = Answer)
                            elif text_data.startswith('A') or 'A;' in text_data:
                                print("🎉 TRANSAKSJON BEHANDLET!")
                                print(f"📄 Resultat: {text_data}")
                                print()
                                
                                # Sjekk for godkjenning
                                if 'GODKJENT' in text_data.upper() or 'APPROVED' in text_data.upper():
                                    print("   ✅✅✅ PAYMENT APPROVED! ✅✅✅")
                                elif 'AVVIST' in text_data.upper() or 'DECLINED' in text_data.upper():
                                    print("   ❌ Payment declined")
                                else:
                                    print("   ℹ️  Transaction completed")
                                print()
                            
                            # Display melding
                            elif text_data.startswith('D!'):
                                display_text = text_data.replace('D!', '').strip()
                                print(f"📺 DISPLAY: {display_text}")
                                print()
                            
                            # Feilhåndtering - hvis den klager igjen, prøv på nytt
                            elif 'FEIL' in text_data.upper():
                                print(f"⚠️  Terminal melder feil: {text_data}")
                                print("   Prøver ping igjen...")
                                print()
                                time.sleep(1)
                                conn.sendall(b'[00]')
                                print("📤 TX: [00]")
                                print()
                            
                            # Status eller andre bracket-meldinger
                            elif text_data.startswith('['):
                                print(f"ℹ️  Status/Response: {text_data}")
                                
                                # Sjekk for suksess
                                if '[20;' in text_data:
                                    print("   ✅ Transaksjon godkjent!")
                                print()
                            
                            else:
                                print(f"❓ Ukjent melding: {text_data}")
                                print()

                        except socket.timeout:
                            print("\n⏱️  Timeout - no more data")
                            break
                
                except Exception as e:
                    print(f"\n❌ Error: {e}")
                    import traceback
                    traceback.print_exc()
                
                print()
                print(f"{'='*70}")
                print("🔌 Connection closed")
                print(f"{'='*70}")
                print()
                time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n⚠️  Server stopped by user")
        print("✅ Goodbye!")
