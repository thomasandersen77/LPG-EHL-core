import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def read_response(conn, timeout=3):
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
    except:
        return None

def test_formats():
    print("="*70)
    print("🧪 KOMMANDO FORMAT TESTER")
    print("="*70)
    
    # Test kommandoer
    tests = [
        ("LOGON variant 1", "P;01;1"),
        ("LOGON variant 2", "P;01;1;0"),
        ("LOGON variant 3", "P;01;1;0;0"),
        ("LOGON variant 4", "P;01;0"),
        ("LOGON variant 5", "[01;1]"),
        ("LOGON variant 6", "01;1"),
        ("KJØP variant 1", "P;10;1;200"),
        ("KJØP variant 2", "P;10;1;200;0"),
        ("KJØP variant 3", "P;10;200"),
        ("KJØP variant 4", "[10;1;200;0;0]"),
        ("KJØP variant 5", "10;1;200;0;0"),
    ]
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print("\n⏳ Venter på terminal...\n")
        conn, addr = s.accept()
        with conn:
            print(f"✅ KOBLET TIL: {addr}\n")
            
            # Heartbeats
            for _ in range(3):
                try:
                    conn.settimeout(5.0)
                    header = conn.recv(2)
                    if header:
                        msg_len = struct.unpack('!H', header)[0]
                        if msg_len == 0:
                            conn.sendall(b'\x00\x00')
                except:
                    break
            
            print("Tester kommandoer...\n")
            print("-" * 70)
            
            for name, cmd in tests:
                print(f"\n{name}")
                print(f"  Sender: {cmd}")
                conn.sendall(create_packet(cmd))
                
                response = read_response(conn, timeout=2)
                if response:
                    print(f"  ✅ Svar: {response}")
                    if 'FEIL' not in response and response != 'D!000':
                        print(f"  🎯 DETTE KAN VÆRE RIKTIG FORMAT!")
                else:
                    print(f"  ❌ Ingen svar")
                
                time.sleep(0.5)
            
            print("\n" + "="*70)
            print("Test fullført!")

if __name__ == "__main__":
    test_formats()
