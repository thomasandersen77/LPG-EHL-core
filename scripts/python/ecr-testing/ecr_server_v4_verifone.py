#!/usr/bin/env python3
"""
ECR Server V4 - VERIFONE ECR (BRACKETS) EDITION
Handles Verifone bracket protocol: [00], [10;...], etc.
"""

import socket
import time

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def start_server():
    print("="*70)
    print("💳 ECR SERVER V4 - VERIFONE ECR (BRACKETS) EDITION")
    print("="*70)
    print()
    print("Strategi: Snakk 'Klammeparentes-språket' ([00], [10...])")
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
                
                try:
                    conn.settimeout(30)
                    
                    while True:
                        # Les rå data (ingen lengde-header tull)
                        data = conn.recv(1024)
                        if not data: 
                            print("🔌 Terminal disconnected")
                            break
                        
                        text_data = data.decode('iso-8859-1', errors='ignore')
                        hex_data = ' '.join(f'{b:02X}' for b in data)
                        
                        print(f"📥 RX: {text_data}")
                        print(f"   Hex: {hex_data}")
                        print()
                        
                        # SCENARIO 1: PING ([00])
                        # Terminalen sier "Er du der?". Vi MÅ svare "[00]"
                        if '[00]' in text_data:
                            print("❤️  PING MOTTATT ([00]). Svarer PONG...")
                            conn.sendall(b'[00]')
                            print("📤 TX: [00]")
                            print()
                            
                            # Vent litt, så prøver vi å sende kjøp ETTER vi har blitt venner
                            time.sleep(1)
                            
                            print(f"{'─'*70}")
                            print("💰 Sender KJØP: 1.00 NOK (Verifone Format)...")
                            print(f"{'─'*70}")
                            
                            # Verifone Purchase Format: [10;Løpenummer;Beløp;Moms;...]
                            # 10 = Kjøp
                            # 1  = TransaksjonsID
                            # 100 = Beløp (øre)
                            # 0 = Moms (0 øre)
                            # 0 = Ekstra parameter
                            purchase_cmd = b'[10;1;100;0;0]' 
                            conn.sendall(purchase_cmd)
                            print(f"📤 TX: {purchase_cmd.decode()}")
                            print()
                            print("⏳ Waiting for response...")
                            print("   💡 Terminal should light up now and show: 1.00 kr")
                            print()

                        # SCENARIO 2: DISPLAY MELDING (D!...)
                        elif text_data.startswith('D!'):
                            # Terminalen sender tekst som skal vises på kassen
                            display_text = text_data.replace('D!', '').strip()
                            print(f"📺 DISPLAY: {display_text}")
                            print()
                            
                        # SCENARIO 3: KVITTERING / SVAR (A...)
                        elif text_data.startswith('A'):
                            # A = Answer (Svar på kjøp)
                            print(f"✅ KVITTERING MOTTATT: {text_data}")
                            if 'FEIL' not in text_data.upper():
                                print("   🎉 SUKSESS! Kjøp godkjent/behandlet.")
                            print()

                        # SCENARIO 4: ERROR (FEIL)
                        elif 'FEIL' in text_data.upper():
                            print(f"⚠️  Terminal melder FEIL: {text_data}")
                            print("   (Dette kan skje hvis vi sender feil format)")
                            print()
                        
                        # SCENARIO 5: Godkjenning/Status
                        elif text_data.startswith('[') and ';' in text_data:
                            print(f"ℹ️  Status/Response: {text_data}")
                            
                            # Sjekk for suksess-koder
                            if '[20;' in text_data or '[00;0]' in text_data:
                                print("   ✅ Transaksjon godkjent!")
                            print()
                        
                        else:
                            print(f"❓ Ukjent melding: {text_data}")
                            print()

                except socket.timeout:
                    print("⏱️  Timeout - no more data")
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
