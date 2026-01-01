#!/usr/bin/env bash
set -euo pipefail

HOST="${1:-192.168.0.41}"
PORT="${2:-9000}"

echo "EHL STATE command -> $HOST:$PORT"
python3 - <<'PY' "$HOST" "$PORT"
import socket, sys
host=sys.argv[1]; port=int(sys.argv[2])

# Build EHL STATE packet with correct format:
# STX_CONTROLLER(0x10) + LENGTH + ADDRESS + COMMAND + CHECKSUM + ETX(0x03)
STX_CONTROLLER = 0x10
ETX = 0x03
address = 1
command = 75  # STATE command (0x4B)

# Calculate packet length (STX + LENGTH + ADDRESS + COMMAND + CHECKSUM + ETX)
packet_length = 6  # No data payload for STATE command

# Build payload for checksum: LENGTH + ADDRESS + COMMAND
payload = bytes([packet_length, address, command])

# Calculate checksum (XOR of LENGTH + ADDRESS + COMMAND)
checksum = 0
for b in payload:
    checksum ^= b

# Build complete frame
frame = bytes([STX_CONTROLLER]) + payload + bytes([checksum & 0xFF, ETX])

print(f"TX: {' '.join(f'{b:02X}' for b in frame)}")

s=socket.socket()
s.settimeout(1.5)
s.connect((host, port))
s.sendall(frame)
try:
    data=s.recv(256)
except Exception:
    data=b""
print("RX:", " ".join(f"{b:02X}" for b in data) if data else "<ingenting>")
s.close()
PY
