import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """Pakker inn kommando med 2-byte lengde-header (Viking/Verifone Framing)"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def hex_dump(data):
    """Lager en pen hex-dump av dataene"""
    return " ".join("{:02X}".format(b) for b in data)

def start_server():
    print("="*70)
    print("🕵️‍♂️ ECR SERVER V16 - DIAGNOSTIC & PROACTIVE")
    print("="*70)
    print("Strategi:")
    print("  1. Vis ALT som kommer (Raw Hex).")
    print("  2. Hvis terminalen er stille (bare heartbeats), send 'I2' (Handshake).")
    print("  3. Hvis 'I2' feiler, prøv 'P' (Purchase).")
    
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
                
                last_action_time = time.time()
                handshake_sent = False
                purchase_sent = False
                
                while True:
                    try:
                        conn.settimeout(2.0) # Kort timeout for å sjekke om vi skal sende noe
                        
                        # Prøv å lese 2 bytes header
                        try:
                            header = conn.recv(2)
                        except socket.timeout:
                            # Timeout: Terminalen sier ingenting. Skal vi ta initiativet?
                            if time.time() - last_action_time > 3.0:
                                if not handshake_sent:
                                    print("\n💡 Terminal er stille. Vi tar initiativet!")
                                    print("   Sender VIKING HANDSHAKE (I2)...")
                                    # I2;KasseID;Versjon;
                                    cmd = 'I2;999;1.0;'
                                    conn.sendall(create_packet(cmd))
                                    print(f"🚀 TX: {cmd}")
                                    handshake_sent = True
                                    last_action_time = time.time()
                                
                                elif handshake_sent and not purchase_sent:
                                    # Hvis vi har sendt handshake men ikke fått svar på en stund, prøv P
                                    print("\n💡 Ingen reaksjon på I2. Prøver VIKING PURCHASE (P)...")
                                    # Reserver 1 krone
                                    cmd = 'P;1;100;0;0'
                                    conn.sendall(create_packet(cmd))
                                    print(f"🚀 TX: {cmd}")
                                    purchase_sent = True
                                    last_action_time = time.time()
                            continue

                        if not header: break # Kobling brutt
                        
                        msg_len = struct.unpack('!H', header)[0]
                        
                        # --- SCENARIO 1: HEARTBEAT (Lengde 0) ---
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00') # Svar Pong
                            continue

                        # --- SCENARIO 2: DATA MOTTATT ---
                        payload = conn.recv(msg_len)
                        raw_hex = hex_dump(payload)
                        text_safe = payload.decode('iso-8859-1', errors='replace')
                        
                        print(f"\n\n📨 MELDING MOTTATT (Lengde: {msg_len})")
                        print(f"   HEX : {raw_hex}")
                        print(f"   TEKST: {text_safe}")
                        
                        last_action_time = time.time()

                        # --- ANALYSE AV SVAR ---
                        
                        if 'FEIL' in text_safe:
                            print("⚠️  FEILMELDING DETEKTERT!")
                            print("   Dette betyr at protokollen er delvis riktig, men kommandoen feil.")
                        
                        if text_safe.startswith('I1'):
                            print("🤝  Handshake (I1) mottatt fra terminal!")
                            if not handshake_sent:
                                print("   Svarer med I2...")
                                cmd = 'I2;999;1.0;'
                                conn.sendall(create_packet(cmd))
                                handshake_sent = True

                        if text_safe.startswith('R') or text_safe.startswith('A'):
                            print("✅  TRANSAKSJONS-SVAR!")

                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        break
                
                print("\n🔌 Kobling lukket.")
                time.sleep(1)

if __name__ == "__main__":
    start_server()
