#!/usr/bin/env python3
import socket
import time
import struct
import sys
from datetime import datetime

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

# DET GYLNE FORMATET FRA RAPPORTEN
# P;Kommando;Sekvens;Beløp;Cashback
# P;10;1;200;0  <- Dette ga "A000ECR Timeout" (GYLDIG FORMAT!)
CMD_PURCHASE = 'P;10;1;200;0'

def create_packet(payload_str):
    """Lager pakke med 2-byte lengde-header (Big Endian)"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("🏆 ECR SERVER V22 - THE GOLDEN FORMAT")
    print("="*70)
    print(f"Terminal: Ingenico Self/4000 (Ubetjent terminal)")
    print(f"Format som ga 'ECR Timeout': {CMD_PURCHASE}")
    print("'Timeout' = GYLDIG FORMAT (ikke 'FEIL I')")
    print("-" * 70)
    print()
    print("⚠️  VIKTIG - INGENICO SELF/4000 SPESIFIKK INSTRUKSJON:")
    print()
    print("   Ubetjente terminaler (Self/4000) fungerer annerledes enn")
    print("   butikkterminaler. Prøv EN av disse metodene:")
    print()
    print("   METODE A: Sett chip-kort i leseren NÅ (før tilkobling)")
    print("             og la det stå der hele tiden")
    print()
    print("   METODE B: Hold kontaktløst kort ved leseren NØYAKTIG")
    print("             når du ser '🚀 SENDER GOLDEN KJØP'")
    print()
    print("   METODE C: Prøv å vente med kort til terminalen viser")
    print("             noe på skjermen (hvis den gjør det)")
    print()
    print("-" * 70)
    input("\n👉 Trykk ENTER når du er klar (kort i posisjon for Metode A)...")
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"\n✅ Lytter på {HOST}:{PORT}")
        
        print("\n⏳ Venter på terminal...")
        conn, addr = s.accept()
        
        with conn:
            print(f"📱 TERMINAL KOBLET TIL: {addr}")
            print("\nEtablerer forbindelse", end="")
            
            purchase_sent = False
            response_count = 0
            start_time = time.time()
            last_response_time = start_time
            
            # Heartbeat etablering
            for _ in range(3):
                try:
                    conn.settimeout(5.0)
                    header = conn.recv(2)
                    if not header: break
                    msg_len = struct.unpack('!H', header)[0]
                    if msg_len == 0:
                        sys.stdout.write(".")
                        sys.stdout.flush()
                        conn.sendall(b'\x00\x00')
                except:
                    break
            
            print(" OK!\n")
            print("="*70)
            
            # SEND GOLDEN COMMAND
            print(f"\n🚀 SENDER 'GOLDEN' KJØP: {CMD_PURCHASE}")
            print(f"   Tid: {datetime.now().strftime('%H:%M:%S')}")
            print("\n👉 HVIS METODE B: HOLD KORT VED LESER NÅ!")
            print()
            
            conn.sendall(create_packet(CMD_PURCHASE))
            
            # LYTT I 120 SEKUNDER
            print("🔊 Lytter etter svar (120 sekunder)...")
            print("   (Hvis intet skjer, kan du prøve å holde/sette kortet nå)")
            print()
            
            while (time.time() - start_time) < 120:
                try:
                    conn.settimeout(3.0)
                    
                    header = conn.recv(2)
                    if not header:
                        break
                    
                    msg_len = struct.unpack('!H', header)[0]
                    
                    # Heartbeat
                    if msg_len == 0:
                        sys.stdout.write(".")
                        sys.stdout.flush()
                        conn.sendall(b'\x00\x00')
                        continue
                    
                    # DATA!
                    payload = conn.recv(msg_len)
                    text = payload.decode('iso-8859-1', errors='replace')
                    
                    response_count += 1
                    last_response_time = time.time()
                    timestamp = datetime.now().strftime('%H:%M:%S')
                    
                    print(f"\n[{timestamp}] 📥 SVAR #{response_count}: {text}")
                    
                    # ANALYSE
                    if 'A000ECR Timeout' in text:
                        print("         ⏱️  ECR TIMEOUT")
                        print("         ✅ Format er RIKTIG!")
                        print("         ❌ Men kort ble ikke lest i tide")
                        print("         💡 Prøv igjen med raskere kort-timing")
                    
                    elif 'A000' in text and 'FEIL' not in text and 'Timeout' not in text:
                        print("         🎉🎉🎉 POTENSIELT SUKSESS! 🎉🎉🎉")
                        print(f"         Full respons: {text}")
                        # Vent på mer data
                        time.sleep(5)
                    
                    elif 'FEIL' in text:
                        print("         ❌ FEILMELDING")
                        print(f"         Detaljer: {text}")
                    
                    elif '[00]' in text:
                        print("         ✅ Kommando mottatt (ACK)")
                    
                    elif 'D!' in text:
                        print("         ⏳ Dialog/Venter status")
                    
                    elif text == '`':
                        print("         ❓ Ukjent tegn (backtick)")
                    
                    else:
                        print(f"         ❓ Ukjent respons")
                    
                except socket.timeout:
                    # Sjekk om vi har vært stille lenge
                    if response_count > 0 and (time.time() - last_response_time) > 20:
                        print("\n\n⏱️  Ingen aktivitet på 20 sekunder.")
                        break
                    continue
                
                except Exception as e:
                    print(f"\n❌ Feil: {e}")
                    break
            
            print("\n" + "="*70)
            print("📊 OPPSUMMERING")
            print("="*70)
            print(f"Totalt {response_count} svar mottatt")
            print(f"Tid brukt: {int(time.time() - start_time)} sekunder")
            print()
            
            if response_count == 0:
                print("❌ INGEN SVAR")
                print("   Mulig årsak: Terminal ikke i ECR-modus")
            elif response_count <= 2:
                print("⚠️  FÅ SVAR (typisk [00] + backtick)")
                print("   Terminal mottar kommando men starter ikke transaksjons-UI")
                print("   Sannsynlig: Mangler SDK/autentisering fra Nets/Bambora")
            
            print("="*70)
            print("\n✅ Test fullført")
            
            print("\n💡 NESTE STEG:")
            print("   1. Hvis du fikk 'ECR Timeout': Prøv igjen med raskere timing")
            print("   2. Hvis du kun fikk [00]: Terminal trenger SDK/provisjonering")
            print("   3. Kontakt Nets/Bambora med ECR_INTEGRATION_REPORT.md")

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n🛑 Avbrutt av bruker")
    except Exception as e:
        print(f"\n\n❌ KRITISK FEIL: {e}")
        import traceback
        traceback.print_exc()
