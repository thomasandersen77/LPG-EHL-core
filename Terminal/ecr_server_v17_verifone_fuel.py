import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

# 4.00 NOK i øre
RESERVATION_AMOUNT = 400 

def create_packet(payload_str):
    """Pakker inn Verifone-kommando med 2-byte lengde-header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("⛽ ECR SERVER V17 - VERIFONE FUEL RESERVATION")
    print("="*70)
    print("Fasit fra logg: Terminalen snakker 'Bracket Protocol' ([...]).")
    print("Strategi:")
    print("  1. Logon ([01])")
    print("  2. Reserver 4 kr ([03])")
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        
        while True:
            print("\n⏳ Venter på terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"📱 TERMINAL KOBLET TIL: {addr}")
                
                logon_sent = False
                reservation_sent = False
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        
                        # 1. LES HEADER
                        header = conn.recv(2)
                        if not header: break
                        msg_len = struct.unpack('!H', header)[0]
                        
                        # 2. HÅNDTER HEARTBEAT (Lengde 0)
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00') # Svar Pong
                            
                            # Hvis vi bare står og pinger, og ikke har sendt Logon ennå... kjør på!
                            if not logon_sent:
                                time.sleep(0.5)
                                print("\n🔑 Sender LOGON ([01;1])...")
                                cmd = '[01;1]'
                                conn.sendall(create_packet(cmd))
                                print(f"🚀 TX: {cmd}")
                                logon_sent = True
                            continue

                        # 3. LES DATA
                        payload = conn.recv(msg_len)
                        text = payload.decode('iso-8859-1', errors='ignore')
                        print(f"\n📥 RX: {text}")

                        # Svar alltid på tekst-ping [00]
                        if '[00]' in text:
                            conn.sendall(create_packet('[00]'))

                        # 4. ANALYSER SVAR
                        # Vi godtar [A...] (Ack) ELLER [00] som tegn på at Logon gikk gjennom
                        # Terminalen kan være litt sær på Logon-svar.
                        
                        if logon_sent and not reservation_sent:
                            # Vent 1 sekund for sikkerhets skyld
                            time.sleep(1.0)
                            print(f"\n⛽ Sender RESERVASJON på {RESERVATION_AMOUNT/100:.2f} kr ([03])...")
                            
                            # Format: [03;Seq;Beløp;Moms;Cashback]
                            cmd = f'[03;2;{RESERVATION_AMOUNT};0;0]'
                            conn.sendall(create_packet(cmd))
                            print(f"🚀 TX: {cmd}")
                            reservation_sent = True

                        if 'A;' in text and reservation_sent:
                            print("\n✅ RESERVASJON MOTTATT!")
                            print("👉 Sjekk skjermen: Ber den om kort?")

                        if 'FEIL' in text:
                            print(f"⚠️ Feilmelding: {text}")
                            # Hvis [03] feiler med "FEIL", betyr det at den ikke støtter 03.
                            # Da er siste utvei [10] (Kjøp) igjen, men nå er vi i hvert fall logget på!

                    except socket.timeout:
                        conn.sendall(b'\x00\x00')
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        break
                
                print("\n🔌 Kobling lukket.")
                time.sleep(1)

if __name__ == "__main__":
    start_server()
