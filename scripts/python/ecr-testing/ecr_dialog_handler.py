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
    """Parser ECR response med detaljert logging"""
    print(f"   [DEBUG] Rå respons: '{text}' (len={len(text)})")
    
    if not text or text.strip() == '':
        return 'EMPTY', text
    
    # Bracket format
    if text.startswith('[') and text.endswith(']'):
        parts = text[1:-1].split(';')
        if parts:
            return parts[0], text
    
    # Ingenico format
    if text.startswith('A000'):
        if 'FEIL' in text:
            return 'CMD_ERROR', text
        return '00', text
    elif text.startswith('D!'):
        return 'DIALOG', text
    elif 'FEIL' in text:
        return 'ERROR', text
    
    return 'UNKNOWN', text

def read_all_responses(conn, timeout=5):
    """Leser ALLE ventende svar (kan være flere meldinger)"""
    responses = []
    conn.settimeout(timeout)
    
    while True:
        try:
            header = conn.recv(2)
            if not header:
                break
            
            msg_len = struct.unpack('!H', header)[0]
            
            if msg_len == 0:
                sys.stdout.write(".")
                sys.stdout.flush()
                conn.sendall(b'\x00\x00')
                continue
            
            payload = conn.recv(msg_len)
            text = payload.decode('iso-8859-1', errors='replace')
            responses.append(text)
            
        except socket.timeout:
            break
        except:
            break
    
    return responses

def start_server():
    print("="*70)
    print("⛽ ECR SERVER - DIALOG HANDLER")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {AMOUNT/100:.2f} kr")
    print("Håndterer: D! dialog flow")
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
                print("📤 STEG 1: LOGON (P;01;1)")
                conn.sendall(create_packet('P;01;1'))
                
                responses = read_all_responses(conn, timeout=5)
                for resp in responses:
                    code, full = parse_response(resp)
                    print(f"📥 {full} → {code}")
                
                time.sleep(0.5)
                
                # KJØP
                print(f"\n📤 STEG 2: KJØP (P;10;2;{AMOUNT};0;0)")
                print("👉 Hold kort klart!\n")
                conn.sendall(create_packet(f'P;10;2;{AMOUNT};0;0'))
                
                # Les kontinuerlig i 60 sekunder
                print("Venter på respons (60s timeout)...")
                start_time = time.time()
                last_response_code = None
                
                while (time.time() - start_time) < 60:
                    responses = read_all_responses(conn, timeout=2)
                    
                    for resp in responses:
                        code, full = parse_response(resp)
                        print(f"📥 {full} → {code}")
                        last_response_code = code
                        
                        # DIALOG FLOW HANDLING
                        if code == 'DIALOG':
                            # D!000 = Venter på handling
                            print("   ℹ️  Terminal venter på input...")
                            # Kanskje vi må sende en ACK eller fortsettelseskommando?
                            # Test: Send en tom ACK
                            time.sleep(0.5)
                            # Prøv å sende en "fortsett" kommando
                            # conn.sendall(create_packet('P;00;0'))  # Generic ACK
                        
                        elif code == '00':
                            print("\n🎉🎉🎉 SUKSESS! 🎉🎉🎉\n")
                            break
                        
                        elif code == 'ERROR' or code == 'CMD_ERROR':
                            print(f"\n❌ FEIL: {full}\n")
                            break
                    
                    if last_response_code in ['00', 'ERROR', 'CMD_ERROR']:
                        break
                    
                    time.sleep(0.5)
                
                if not responses:
                    print("\n❌ Timeout - ingen svar\n")
                
                print("="*70)
                print("🔌 Sesjon avsluttet\n")
                time.sleep(2)

if __name__ == "__main__":
    start_server()
