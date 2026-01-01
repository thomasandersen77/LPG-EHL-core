import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009
AMOUNT = 200  # 2.00 NOK

def create_packet(payload_str):
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def read_response(conn, timeout=10):
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

def start_server():
    print("="*70)
    print("⛽ ECR SERVER - MED LOGON SEKVENS")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {AMOUNT/100:.2f} kr")
    print("Sekvens: P;01;1 (LOGON) → P;10;seq;amount;0 (KJØP)")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        session_num = 0
        
        while True:
            session_num += 1
            print(f"\n⏳ Venter på terminal (SESSION #{session_num})...")
            
            conn, addr = s.accept()
            with conn:
                print(f"✅ KOBLET TIL: {addr}\n")
                
                # Heartbeats
                print("Etablerer", end="")
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
                
                # HOPP OVER LOGON - Det fungerer ikke uansett
                # Gå direkte til KJØP med det eneste formatet som ga "Timeout" (ikke FEIL)
                
                # Fra testen: P;10;1;200;0 ga "A000ECR Timeout"
                cmd = f'P;10;{session_num};{AMOUNT};0'
                print(f"📤 Sender KJØP: {cmd}")
                print(f"💳 SETT KORT NÅ (TAST PIN OM NØDVENDIG)!\n")
                
                conn.sendall(create_packet(cmd))
                
                # Les FLERE svar (terminalen kan sende oppdateringer)
                print("Lytter etter svar...")
                start_time = time.time()
                responses = []
                
                while (time.time() - start_time) < 60:
                    resp = read_response(conn, timeout=3)
                    if resp:
                        responses.append(resp)
                        print(f"📥 {resp}")
                        
                        if 'A000' in resp and 'Timeout' not in resp and 'FEIL' not in resp:
                            print("\n🎉 SUKSESS!\n")
                            break
                        elif any(x in resp for x in ['godkjent', 'approved', 'OK']):
                            print("\n🎉 SUKSESS!\n")
                            break
                    else:
                        if responses:  # Vi fikk minst ett svar
                            break
                
                if not responses:
                    print("❌ Ingen svar\n")
                
                print(f"SESSION #{session_num} FERDIG\n")
                time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n🛑 Stoppet")
