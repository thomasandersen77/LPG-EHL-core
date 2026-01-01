import socket, time

HOST="0.0.0.0"
PORT=8009

PURCHASE = b"[10;1;100;0;0]"  # guess. we will adjust once we see terminal replies.

def hx(b): 
    return " ".join(f"{x:02X}" for x in b)

def asc(b):
    return b.decode("iso-8859-1", errors="replace")

print("ECR bracket purchase try (experimental)")

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(5)
    print(f"✅ Listening on {HOST}:{PORT}")

    while True:
        conn, addr = s.accept()
        print(f"\n📱 CONNECTED {addr}")
        conn.settimeout(10)
        with conn:
            sent_purchase = False
            while True:
                try:
                    data = conn.recv(2048)
                except socket.timeout:
                    continue
                if not data:
                    print("🔌 disconnected")
                    break

                print(f"\n📥 RX {len(data)} bytes")
                print(f"  HEX  : {hx(data)}")
                print(f"  ASCII: {asc(data)}")

                if b"[00]" in data:
                    print("❤️  RX [00] -> TX [00]")
                    conn.sendall(b"[00]")

                    if not sent_purchase:
                        time.sleep(0.5)
                        print(f"💰 TX purchase: {PURCHASE!r}")
                        conn.sendall(PURCHASE)
                        sent_purchase = True
