import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009
RESERVATION_AMOUNT = 200  # 2.00 NOK

def create_packet(payload_str):
    """Lager pakke med 2-byte lengde header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def parse_response(text):
    """Parser ECR response (både bracket og A000/D! format)"""
    # Verifone bracket format: [00] eller [A000...]
    if text.startswith('[') and text.endswith(']'):
        parts = text[1:-1].split(';')
        if parts:
            return parts[0], text
    
    # Ingenico format: A000..., D!000, etc.
    # A000 = Accept/OK, D! = Dialog/Wait
    if text.startswith('A000'):
        return '00', text  # Behandle som suksess
    elif text.startswith('D!'):
        return 'WAIT', text  # Venter på mer input
    elif 'FEIL' in text:
        return 'ERROR', text
    
    return None, text

def start_server():
    print("="*70)
    print("⛽ ECR SERVER - VIKING PROTOCOL (WORKING)")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {RESERVATION_AMOUNT/100:.2f} kr")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        while True:
            print("\n⏳ Venter på terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"✅ TERMINAL KOBLET TIL: {addr}\n")
                
                # Vent på stabile heartbeats
                print("Etablerer forbindelse", end="")
                heartbeat_count = 0
                while heartbeat_count < 3:
                    try:
                        conn.settimeout(5.0)
                        header = conn.recv(2)
                        if not header: break
                        
                        msg_len = struct.unpack('!H', header)[0]
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00')
                            heartbeat_count += 1
                    except socket.timeout:
                        break
                
                print(" OK!\n")
                
                # STEG 1: LOGON
                print("📤 STEG 1: Sender LOGON (P;01;1)")
                conn.sendall(create_packet('P;01;1'))
                
                response = read_response(conn, timeout=5)
                if not response:
                    print("❌ Ingen svar på LOGON. Avbryter.")
                    continue
                
                code, full = parse_response(response)
                print(f"📥 Svar: {full}")
                
                if code == 'ERROR':
                    print(f"⚠️  LOGON feilet: {full}")
                    continue
                elif code == 'WAIT':
                    print("⏳ Terminal venter... Ignorer og fortsett")
                    # D!000 betyr terminalen venter på handling, ikke feil
                elif code == '00':
                    print("✅ LOGON OK!")
                else:
                    print(f"⚠️  Ukjent svar: {full}")
                    continue
                
                print()
                time.sleep(1)
                
                # STEG 2: RESERVASJON
                print(f"📤 STEG 2: Sender RESERVASJON (P;03;2;{RESERVATION_AMOUNT};0;0)")
                # Format: P;03;Seq;Beløp;Moms;Cashback
                conn.sendall(create_packet(f'P;03;2;{RESERVATION_AMOUNT};0;0'))
                
                response = read_response(conn, timeout=30)
                if not response:
                    print("❌ Ingen svar på RESERVASJON (timeout 30s)")
                    continue
                
                code, full = parse_response(response)
                print(f"📥 Svar: {full}")
                
                if code == '00':
                    print("\n🎉🎉🎉 RESERVASJON GODKJENT! 🎉🎉🎉")
                    print("👉 SJEKK SKJERMEN PÅ TERMINALEN!")
                elif 'FEIL' in full or code != '00':
                    print(f"\n⚠️  Reservasjon feilet. Prøver vanlig KJØP...\n")
                    time.sleep(1)
                    
                    # FALLBACK: KJØP
                    print(f"📤 STEG 3: Sender KJØP (P;10;3;{RESERVATION_AMOUNT};0;0)")
                    conn.sendall(create_packet(f'P;10;3;{RESERVATION_AMOUNT};0;0'))
                    
                    response = read_response(conn, timeout=30)
                    if response:
                        code, full = parse_response(response)
                        print(f"📥 Svar: {full}")
                        
                        if code == '00':
                            print("\n🎉 KJØP GODKJENT!")
                        else:
                            print(f"\n❌ KJØP FEILET: {full}")
                
                print("\n" + "="*70)
                print("🔌 Sesjon avsluttet.")
                time.sleep(2)

def read_response(conn, timeout=10):
    """Leser ett svar fra terminalen"""
    try:
        conn.settimeout(timeout)
        
        while True:
            header = conn.recv(2)
            if not header:
                return None
            
            msg_len = struct.unpack('!H', header)[0]
            
            # Skip heartbeats
            if msg_len == 0:
                sys.stdout.write(".")
                sys.stdout.flush()
                conn.sendall(b'\x00\x00')
                continue
            
            # DATA!
            payload = conn.recv(msg_len)
            text = payload.decode('iso-8859-1', errors='replace')
            return text
            
    except socket.timeout:
        return None

if __name__ == "__main__":
    start_server()
