import socket
import time
import struct
import sys

# DIN LOKALE IP (BEKREFTET)
HOST = '0.0.0.0' # Lytter på alle interfaces, inkludert 192.168.0.41
PORT = 8009

# BELØP (Øre)
RESERVATION_AMOUNT = 200 # 2.00 NOK

def create_packet(payload_str):
    """Lager Verifone-pakke: [Lengde][Payload]"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("⛽ ECR SERVER V18 - DEN ENDELIGE LØSNINGEN")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print("Forutsetning: ECR/TLS MÅ være 'Nei' på terminalen.")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        while True:
            print("\n⏳ Venter på terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"✅ TERMINAL KOBLET TIL: {addr}")
                
                logon_sent = False
                reservation_sent = False
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        
                        # 1. LES HEADER (2 bytes lengde)
                        try:
                            header = conn.recv(2)
                        except socket.timeout:
                            # Hvis terminalen er stille, send en probe for å vekke den
                            print("⏰ Timeout. Sender Heartbeat-probe...")
                            conn.sendall(b'\x00\x00')
                            continue

                        if not header: break
                        
                        msg_len = struct.unpack('!H', header)[0]
                        
                        # --- HÅNDTER HEARTBEAT (Lengde 0) ---
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00') # MÅ SVARE PONG!
                            
                            # START AUTOMATISK SEKVENS NÅR VI HAR KONTAKT
                            if not logon_sent:
                                time.sleep(0.5)
                                print("\n🚀 Starter prosess... Sender LOGON ([01])...")
                                conn.sendall(create_packet('[01;1]'))
                                logon_sent = True
                            
                            continue

                        # --- HÅNDTER DATA (Lengde > 0) ---
                        payload = conn.recv(msg_len)
                        text = payload.decode('iso-8859-1', errors='replace')
                        print(f"\n📥 MOTTATT: {text}")

                        # SCENARIO: Vi får svar på LOGON (enten [00] eller [A...])
                        if logon_sent and not reservation_sent:
                            print("✅ Logon sendt. Går videre til RESERVASJON...")
                            time.sleep(1.0)
                            
                            print(f"⛽ Sender RESERVASJON på {RESERVATION_AMOUNT/100:.2f} kr ([03])...")
                            # Kommando 03 = Pre-Auth / Reservasjon
                            # Format: [03;Seq;Beløp;Moms;Cashback]
                            cmd = f'[03;2;{RESERVATION_AMOUNT};0;0]'
                            conn.sendall(create_packet(cmd))
                            reservation_sent = True

                        # SCENARIO: Suksess?
                        if 'A;' in text and reservation_sent:
                            print("\n🎉🎉🎉 SUKSESS! RESERVASJON MOTTATT! 🎉🎉🎉")
                            print("👉 SJEKK SKJERMEN NÅ! Ber den om kort?")

                        # SCENARIO: Feilmelding?
                        if 'FEIL' in text:
                            print(f"⚠️  Terminal melder feil: {text}")
                            if reservation_sent:
                                print("   Kommando [03] feilet. Terminalen støtter kanskje ikke reservasjon?")
                                print("   Prøver Kjøp [10] som fallback...")
                                time.sleep(1)
                                conn.sendall(create_packet(f'[10;3;{RESERVATION_AMOUNT};0;0]'))

                    except Exception as e:
                        print(f"\n❌ Feil: {e}")
                        break
                
                print("\n🔌 Kobling lukket.")
                time.sleep(1)

if __name__ == "__main__":
    start_server()
