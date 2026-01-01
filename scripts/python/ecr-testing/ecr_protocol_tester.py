import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """Lager pakke med 2-byte lengde header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def test_protocols():
    print("="*70)
    print("🔬 ECR PROTOKOLL TESTER")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print("Tester: Viking (P;...) og Verifone ([...]) format")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        while True:
            print("\n⏳ Venter på terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"✅ KOBLET TIL: {addr}\n")
                
                # Vent på stabil forbindelse (heartbeats)
                print("Venter på heartbeats...")
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
                        else:
                            payload = conn.recv(msg_len)
                            print(f"\n📥 UVENTET DATA: {payload.decode('iso-8859-1', errors='replace')}")
                    except socket.timeout:
                        break
                
                print("\n\n🧪 TEST 1: Viking Protocol (Nets)")
                print("Sender: P;01;1")
                conn.sendall(create_packet('P;01;1'))
                time.sleep(2)
                
                # Les svar
                response = read_response(conn, timeout=3)
                if response:
                    print(f"✅ SVAR: {response}")
                else:
                    print("❌ Ingen svar\n")
                    
                    print("🧪 TEST 2: Verifone Brackets Protocol")
                    print("Sender: [01;1]")
                    conn.sendall(create_packet('[01;1]'))
                    time.sleep(2)
                    
                    response = read_response(conn, timeout=3)
                    if response:
                        print(f"✅ SVAR: {response}")
                    else:
                        print("❌ Ingen svar\n")
                        
                        print("🧪 TEST 3: Viking Purchase (direkte)")
                        print("Sender: P;10;2;200;0;0")
                        conn.sendall(create_packet('P;10;2;200;0;0'))
                        time.sleep(2)
                        
                        response = read_response(conn, timeout=3)
                        if response:
                            print(f"✅ SVAR: {response}")
                        else:
                            print("❌ Ingen svar\n")
                            
                            print("🧪 TEST 4: Verifone Purchase (direkte)")
                            print("Sender: [10;2;200;0;0]")
                            conn.sendall(create_packet('[10;2;200;0;0]'))
                            time.sleep(2)
                            
                            response = read_response(conn, timeout=3)
                            if response:
                                print(f"✅ SVAR: {response}")
                            else:
                                print("❌ Ingen svar på noen av testene")
                
                print("\n" + "="*70)
                print("🔌 Test fullført. Kobling lukkes.")
                break

def read_response(conn, timeout=5):
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
    test_protocols()
