import socket, time

HOST="0.0.0.0"
PORT=8009

def hx(b): 
    return " ".join(f"{x:02X}" for x in b)

def asc(b):
    try:
        s=b.decode("iso-8859-1", errors="replace")
    except:
        return ""
    out=[]
    for ch in s:
        o=ord(ch)
        if 32 <= o <= 126:
            out.append(ch)
        elif ch in "\r\n":
            out.append({"\\r":"␍","\\n":"␊"}[ch])
        else:
            out.append("·")
    return "".join(out)

print("="*70)
print("ECR SNIFF + PINGPONG")
print(" - logs raw bytes")
print(" - if it sees b'[00]' anywhere, it replies b'[00]' immediately")
print(" - does NOT send purchase (yet)")
print("="*70)

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen(5)
    print(f"✅ Listening on {HOST}:{PORT}")

    while True:
        conn, addr = s.accept()
        print(f"\n📱 CONNECTED from {addr[0]}:{addr[1]}")
        conn.settimeout(10)

        with conn:
            buf = b""
            t0 = time.time()
            while True:
                try:
                    data = conn.recv(2048)
                except socket.timeout:
                    print("⏳ timeout waiting for data (still connected)")
                    continue
                if not data:
                    print("🔌 EOF / disconnected")
                    break

                buf += data
                print(f"\n📥 RX {len(data)} bytes")
                print(f"  HEX  : {hx(data)}")
                print(f"  ASCII: {asc(data)}")

                # If bracket ping appears anywhere in stream, respond immediately
                if b"[00]" in buf:
                    print("❤️  Detected [00] in stream -> replying [00]")
                    try:
                        conn.sendall(b"[00]")
                        print("📤 TX: [00]")
                    except Exception as e:
                        print(f"❌ send failed: {e}")
                        break
                    # remove one occurrence to avoid spamming
                    buf = buf.replace(b"[00]", b"", 1)

                # safety: don't keep buffer forever
                if len(buf) > 4096:
                    buf = buf[-1024:]
