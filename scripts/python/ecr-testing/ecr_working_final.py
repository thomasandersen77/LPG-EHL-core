import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009
AMOUNT = 200  # 2.00 NOK (øre)

def create_packet(payload_str):
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def read_response(conn, timeout=30):
    """Leser EN respons fra terminalen"""
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
    print("⛽ ECR SERVER - WORKING VERSION")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {AMOUNT/100:.2f} kr")
    print("Format: P;10;seq;amount;0 (bekreftet fungerende)")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        session_num = 0
        
        while True:
            session_num += 1
            print(f"\n{'='*70}")
            print(f"SESSION #{session_num}")
            print(f"{'='*70}")
            print("⏳ Venter på terminal...")
            
            conn, addr = s.accept()
            with conn:
                print(f"✅ TERMINAL KOBLET TIL: {addr}\n")
                
                # Vent på stabile heartbeats
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
                
                # KJØP MED FUNGERENDE FORMAT
                cmd = f'P;10;{session_num};{AMOUNT};0'
                print(f"📤 Sender KJØP: {cmd}")
                print(f"💳 SETT KORT I TERMINALEN NÅ!\n")
                
                conn.sendall(create_packet(cmd))
                
                # Vent på svar (lang timeout siden bruker må sette kort)
                print("Venter på terminal", end="")
                response = read_response(conn, timeout=90)
                print()
                
                if response:
                    print(f"\n📥 SVAR: {response}\n")
                    
                    if 'A000' in response and 'FEIL' not in response and 'Timeout' not in response:
                        print("🎉🎉🎉 SUKSESS! BETALING GODKJENT! 🎉🎉🎉")
                        print(f"Respons: {response}")
                    elif 'Timeout' in response:
                        print("⏱️  Terminal timeout - kort ble ikke satt i tide")
                    elif 'FEIL' in response:
                        print(f"❌ FEIL: {response}")
                    elif 'D!' in response:
                        print(f"⏳ Venter: {response}")
                    else:
                        print(f"ℹ️  Ukjent respons: {response}")
                else:
                    print("\n❌ Ingen respons fra terminal\n")
                
                print(f"\n{'='*70}")
                print(f"SESSION #{session_num} AVSLUTTET")
                print(f"{'='*70}\n")
                time.sleep(2)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n🛑 Server stoppet av bruker")
