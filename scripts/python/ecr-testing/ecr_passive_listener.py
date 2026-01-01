import socket
import struct
import sys
import time

HOST = '0.0.0.0'
PORT = 8009

def start_passive_listener():
    print("="*70)
    print("🎧 PASSIV ECR LYTTER - Reverse Engineering Mode")
    print("="*70)
    print(f"Lytter på: {HOST}:{PORT}")
    print("Instruksjon: Trykk på terminalen (Kasse-knapp, Kjøp, etc.)")
    print("             Vi logger ALT terminalen sender!")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        while True:
            print("\n⏳ Venter på terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"✅ KOBLET TIL: {addr}")
                print("📡 Logger all trafikk...\n")
                
                while True:
                    try:
                        conn.settimeout(60.0)
                        
                        # Les 2-byte header
                        header = conn.recv(2)
                        if not header:
                            break
                        
                        msg_len = struct.unpack('!H', header)[0]
                        
                        # Heartbeat (lengde 0)
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            conn.sendall(b'\x00\x00')  # Svar PONG
                            continue
                        
                        # DATA MOTTATT!
                        payload = conn.recv(msg_len)
                        
                        print(f"\n📥 RAW HEX: {payload.hex()}")
                        print(f"📥 RAW BYTES: {payload}")
                        
                        try:
                            text = payload.decode('iso-8859-1')
                            print(f"📥 TEXT: {text}")
                        except:
                            print("📥 TEXT: [ikke dekoderbar]")
                        
                        print(f"📏 Lengde: {msg_len} bytes")
                        print("-" * 70)
                        
                        # ECHO tilbake (for å holde forbindelsen åpen)
                        conn.sendall(header + payload)
                        
                    except socket.timeout:
                        print("\n⏰ 60 sek timeout. Sender keepalive...")
                        conn.sendall(b'\x00\x00')
                    except Exception as e:
                        print(f"\n❌ Feil: {e}")
                        break
                
                print("\n🔌 Kobling lukket.")

if __name__ == "__main__":
    start_passive_listener()
