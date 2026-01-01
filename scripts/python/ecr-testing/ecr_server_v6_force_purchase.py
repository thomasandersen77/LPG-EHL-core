#!/usr/bin/env python3
"""
ECR Server V6 - FORCE PURCHASE
No more Mr. Nice Guy - sends purchase command immediately
"""

import socket
import time
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def start_server():
    print("="*70)
    print("💳 ECR SERVER V6 - FORCE PURCHASE")
    print("="*70)
    print()
    print("Strategi: Ignorer '00 00'. Send '[10;1;100;0;0]' (Kjøp 1 kr) rett i fletta.")
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
                
                # Vent bittelitt for å la tilkoblingen sette seg
                time.sleep(0.5)

                # STEG 1: SEND KJØP UANSETT HVA
                # Format: [Kommando;Seq;Beløp;Moms;Cashback]
                # 10 = Purchase (Kjøp)
                # 1 = Sekvensnummer
                # 100 = 1.00 NOK
                cmd = b'[10;1;100;0;0]'
                
                print(f"🚀 Sender KJØP NÅ: {cmd.decode()}")
                print(f"   Format: [Command;SeqNo;Amount;VAT;Cashback]")
                print(f"   Command: 10 (Purchase)")
                print(f"   Amount: 100 øre (1.00 NOK)")
                print()
                
                try:
                    conn.sendall(cmd)
                    print("📤 TX: Sendt!")
                    print()
                except Exception as e:
                    print(f"❌ Send feilet: {e}")
                    print()
                    continue

                # STEG 2: LES SVAR (og filtrer støy)
                print("👂 Lytter etter svar...")
                print("   (Filtrerer vekk 00 00 heartbeats...)")
                print()
                
                buffer = b""
                start_time = time.time()
                
                while time.time() - start_time < 30: # 30 sekunder timeout
                    try:
                        conn.settimeout(1.0) # Ikke blokker for lenge
                        data = conn.recv(1024)
                        if not data: 
                            print("\n🔌 Terminal disconnected")
                            break
                        
                        # Filtrer vekk 00 00 heartbeats for å se skogen for bare trær
                        clean_data = data.replace(b'\x00', b'')
                        
                        if len(clean_data) > 0:
                            text_data = clean_data.decode('iso-8859-1', errors='ignore')
                            hex_data = ' '.join(f'{b:02X}' for b in clean_data)
                            
                            print(f"\n📥 RX: {text_data}")
                            print(f"   Hex: {hex_data}")
                            print()
                            
                            # JACKPOT: Answer fra terminal
                            if 'A;' in text_data or text_data.startswith('A'):
                                print("="*70)
                                print("🎉🎉🎉 SVAR! VI HAR SVAR! 🎉🎉🎉")
                                print("="*70)
                                print(f"Kvittering: {text_data}")
                                print()
                                
                                if 'FEIL' in text_data.upper():
                                    print("⚠️  Terminalen klager på formatet.")
                                    print("   Men vi er på riktig protokoll!")
                                    print("   Vi må bare finjustere kommandoen.")
                                else:
                                    print("✅✅✅ KJØP GODKJENT! ✅✅✅")
                                print()
                                break # Vi er ferdige med denne testen
                            
                            # Display melding
                            if 'D!' in text_data:
                                display_msg = text_data.replace('D!', '').strip()
                                print(f"📺 Display: {display_msg}")
                                print()
                            
                            # Bracket kommandoer
                            if text_data.startswith('['):
                                print(f"ℹ️  Bracket response: {text_data}")
                                
                                # Sjekk for kjente responser
                                if '[20;' in text_data:
                                    print("   ✅ Suksess-kode mottatt!")
                                elif '[99;' in text_data:
                                    print("   ❌ Feil-kode mottatt")
                                print()
                            
                            # Feilmeldinger
                            if 'FEIL' in text_data.upper() and not text_data.startswith('A'):
                                print(f"⚠️  Feilmelding: {text_data}")
                                print()

                        else:
                            # Bare null-bytes, vis en prikk
                            sys.stdout.write(".")
                            sys.stdout.flush()

                    except socket.timeout:
                        # Timeout er OK, vi bare lytter videre
                        continue
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        import traceback
                        traceback.print_exc()
                        break
                
                print()
                print("="*70)
                print("🔌 Connection closed")
                print("="*70)
                print()
                time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n⚠️  Server stopped by user")
        print("✅ Goodbye!")
