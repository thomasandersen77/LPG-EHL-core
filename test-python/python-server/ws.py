from __future__ import annotations

"""
Tiny RFC6455 WebSocket helper (no external deps).

This is intentionally minimal:
- supports server-side handshake
- supports receiving masked text frames (client->server)
- supports sending unmasked text frames (server->client)

Good enough for the React app's `/ws/logs` subscription + JSON messages.
"""

import base64
import hashlib
import os
import socket
import struct
from dataclasses import dataclass
from typing import Optional, Tuple


_WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"


def make_accept_key(sec_websocket_key: str) -> str:
    h = hashlib.sha1((sec_websocket_key.strip() + _WS_GUID).encode("utf-8")).digest()
    return base64.b64encode(h).decode("ascii")


def _recv_exact(conn: socket.socket, n: int) -> bytes:
    buf = b""
    while len(buf) < n:
        chunk = conn.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("socket closed")
        buf += chunk
    return buf


@dataclass
class WsFrame:
    fin: bool
    opcode: int
    payload: bytes


def recv_frame(conn: socket.socket) -> WsFrame:
    """
    Receive a single WebSocket frame. Supports masked frames (expected from browsers).
    """
    b1, b2 = _recv_exact(conn, 2)
    fin = bool(b1 & 0x80)
    opcode = b1 & 0x0F
    masked = bool(b2 & 0x80)
    ln = b2 & 0x7F

    if ln == 126:
        (ln,) = struct.unpack("!H", _recv_exact(conn, 2))
    elif ln == 127:
        (ln,) = struct.unpack("!Q", _recv_exact(conn, 8))

    mask_key = _recv_exact(conn, 4) if masked else b""
    payload = _recv_exact(conn, ln) if ln else b""

    if masked and payload:
        payload = bytes(payload[i] ^ mask_key[i % 4] for i in range(len(payload)))

    return WsFrame(fin=fin, opcode=opcode, payload=payload)


def send_text(conn: socket.socket, text: str) -> None:
    payload = text.encode("utf-8")
    _send_frame(conn, opcode=0x1, payload=payload)


def send_close(conn: socket.socket, code: int = 1000, reason: str = "") -> None:
    payload = struct.pack("!H", int(code)) + reason.encode("utf-8")
    _send_frame(conn, opcode=0x8, payload=payload)


def _send_frame(conn: socket.socket, *, opcode: int, payload: bytes) -> None:
    fin_opcode = 0x80 | (opcode & 0x0F)
    ln = len(payload)

    if ln < 126:
        header = struct.pack("!BB", fin_opcode, ln)
    elif ln <= 0xFFFF:
        header = struct.pack("!BBH", fin_opcode, 126, ln)
    else:
        header = struct.pack("!BBQ", fin_opcode, 127, ln)

    conn.sendall(header + payload)

