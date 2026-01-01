import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

# BELØP
RESERVATION_AMOUNT = 400  # 4.00 NOK (Reserveres først)
FINAL_AMOUNT = 200        # 2.00 NOK (Trekkes til slutt)

def create_packet(payload_str):
    """
    Pakker inn Viking-kommando med 2-byte lengde-header.
    Ingenico/Nets bruker ofte Viking-protokollen over TCP med lengde-prefix.
    """
    # Viking bruker ISO-8859-1 (Latin-1)
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    # Pack length as 2 bytes, Big Endian
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("⛽ ECR SERVER V15 - INGENICO FUEL SIMULATOR")
    print("="*70)
    print("VIKTIG: Sørg for at 'ECR/TLS' er satt til 'Nei' på terminalen!")
    print(f"Strategi:")
    print(f"  1. Handshake : Svar på I1 med I2.")
    print(f"  2. Pre-Auth  : Reserver {RESERVATION_AMOUNT/100:.2f} kr.")
    print(f"  3. Pumping   : Vent i 5 sekunder...")
    print(f"  4. Completion: Trekk endelig beløp {FINAL_AMOUNT/100:.2f} kr.")
    
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
                
                # Tilstandsmaskin
                handshake_done = False
                reservation_done = False
                pumping_simulated = False
                completion_sent = False
                
                while True:
                    try:
                        conn.settimeout(15.0)
                        
                        # 1. LES LENGDE (2 bytes)
                        header = conn.recv(2)
                        if not header: break
                        msg_len = struct.unpack('!H', header)[0]
                        
                        if msg_len == 0:
                            # Heartbeat
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00') # Svar Pong
                            continue

                        # 2. LES MELDING
                        payload = conn.recv(msg_len)
                        text = payload.decode('iso-8859-1', errors='ignore')
                        print(f"\n📥 RX: {text}")

                        # --- LOGIKK ---

                        # SCENARIO: HANDSHAKE (Terminal sier "Hei, jeg er I1")
                        if text.startswith('I1'):
                            print("\n🤝 Terminal presenterte seg! Sender I2 (Handshake)...")
                            # I2;KasseID;Versjon;
                            cmd = 'I2;999;1.0;'
                            conn.sendall(create_packet(cmd))
                            print(f"🚀 TX: {cmd}")
                            handshake_done = True
                            
                            # Gå rett til reservasjon etter litt tid
                            time.sleep(1)

                        # SCENARIO: START RESERVASJON (Hvis handshake er ok)
                        if handshake_done and not reservation_done:
                            print(f"\n⛽ Starter RESERVASJON på {RESERVATION_AMOUNT/100:.2f} kr...")
                            print("   (Ber terminalen sjekke kortet)")
                            
                            # P;Seq;Beløp;Moms;Cashback;Op;Flags...
                            # Vi bruker en standard Purchase (P) først for å se om den reagerer.
                            # I Viking kan P også brukes til reservasjon avhengig av oppsett,
                            # men la oss prøve standard P først for å få liv i skjermen.
                            cmd = f'P;1;{RESERVATION_AMOUNT};0;0' 
                            
                            conn.sendall(create_packet(cmd))
                            print(f"🚀 TX: {cmd}")
                            reservation_done = True

                        # SCENARIO: TERMINAL SVARER PÅ RESERVASJON
                        # Viking svarer ofte med 'R' (Resultat) eller 'A' (Admin/Ack)
                        # Her ser vi etter et godkjent svar.
                        if text.startswith('R') and reservation_done and not pumping_simulated:
                            print("\n✅ RESERVASJON GODKJENT/BEHANDLET!")
                            print("   Simulerer pumping i 5 sekunder...")
                            time.sleep(1)
                            print("   ⛽ Pumper...")
                            time.sleep(1)
                            print("   ⛽ Pumper...")
                            time.sleep(1)
                            print("   ⛽ Pumper...")
                            time.sleep(1)
                            print("   ⛽ Ferdig!")
                            pumping_simulated = True
                            
                            print(f"\n💰 Sender ENDELIG KJØP (Completion) på {FINAL_AMOUNT/100:.2f} kr...")
                            # I Viking sender man ofte en ny P (Purchase) eller U (Update).
                            # For testformål sender vi en ny P med det lavere beløpet.
                            # (I en ekte integrasjon bruker man transaksjons-ID for å oppdatere).
                            cmd = f'P;2;{FINAL_AMOUNT};0;0'
                            conn.sendall(create_packet(cmd))
                            print(f"🚀 TX: {cmd}")
                            completion_sent = True

                        # SCENARIO: SLUTT
                        if text.startswith('R') and completion_sent:
                            print("\n🎉🎉🎉 TRANSAKSJON FULLFØRT! 🎉🎉🎉")
                            print(f"Sluttsum trukket: {FINAL_AMOUNT/100:.2f} kr")

                    except socket.timeout:
                        # Send en heartbeat hvis det blir stille
                        conn.sendall(b'\x00\x00')
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        break
                
                time.sleep(1)

if __name__ == "__main__":
    start_server()
