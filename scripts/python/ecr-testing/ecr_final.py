import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009
AMOUNT = 200  # 2.00 NOK

def create_packet(payload_str):
    """Lager pakke med 2-byte lengde header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def parse_response(text):
    """Parser ECR response"""
    if text.startswith('[') and text.endswith(']'):
        parts = text[1:-1].split(';')
        if parts:
            return parts[0], text
    
    # Ingenico format
    if text.startswith('A000'):
        if 'FEIL' in text:
            return 'ERROR', text
        return '00', text
    elif text.startswith('D!'):
        return 'WAIT', text
    elif 'FEIL' in text:
        return 'ERROR', text
    
    return 'UNKNOWN', text

def start_server():
    print("="*70)
    print("⛽ ECR SERVER - FINAL VERSION")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {AMOUNT/100:.2f} kr")
    print("Protokoll: Viking (P;...) -> Ingenico respons (A000/D!)")
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
                
                # Vent på heartbeats
                print("Etablerer forbindelse", end="")
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
                
                # LOGON
                print("📤 Sender LOGON (P;01;1)")
                conn.sendall(create_packet('P;01;1'))
                response = read_response(conn, timeout=5)
                
                if response:
                    code, full = parse_response(response)
                    print(f"📥 Svar: {full} (kode: {code})\n")
                
                # KJØP (siden reservasjon ikke støttes)
                print(f"📤 Sender KJØP (P;10;2;{AMOUNT};0;0)")
                print("👉 Sett kort i terminalen nå!\n")
                conn.sendall(create_packet(f'P;10;2;{AMOUNT};0;0'))
                
                # Vent på svar (kan ta tid mens bruker setter kort)
                print("Venter på terminal", end="")
                response = read_response(conn, timeout=60)
                print()
                
                if response:
                    code, full = parse_response(response)
                    print(f"📥 Svar: {full}")
                    
                    if code == '00':
                        print("\n🎉🎉🎉 BETALING GODKJENT! 🎉🎉🎉\n")
                        # Parser detaljer hvis mulig
                        if ';' in full:
                            print(f"Detaljer: {full}")
                    elif code == 'ERROR':
                        print("\n❌ BETALING FEILET\n")
                    else:
                        print(f"\n⚠️  Ukjent status: {code}\n")
                else:
                    print("\n❌ Timeout - ingen svar fra terminal\n")
                
                print("="*70)
                print("🔌 Sesjon avsluttet.\n")
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
            
            if msg_len == 0:
                sys.stdout.write(".")
                sys.stdout.flush()
                conn.sendall(b'\x00\x00')
                continue
            
            payload = conn.recv(msg_len)
            return payload.decode('iso-8859-1', errors='replace')
            
    except socket.timeout:
        return None

if __name__ == "__main__":
    start_server()
