from __future__ import annotations


def hexdump(b: bytes) -> str:
    return " ".join(f"{x:02X}" for x in b)


def parse_hex(s: str) -> bytes:
    """
    Accepts:
      - "01 02 0A"
      - "01020A"
      - "0x01,0x02,0x0A"
    """
    raw = s.strip().replace(",", " ").replace("0x", " ").replace("0X", " ")
    raw = "".join(ch if ch in "0123456789abcdefABCDEF " else " " for ch in raw)
    parts = [p for p in raw.split() if p]
    if len(parts) == 1 and len(parts[0]) % 2 == 0:
        parts = [parts[0][i : i + 2] for i in range(0, len(parts[0]), 2)]
    return bytes(int(p, 16) for p in parts)


def decode_escapes(s: str) -> bytes:
    """
    For REPL convenience: supports \\xNN, \\r, \\n, \\t.
    """
    try:
        return s.encode("utf-8").decode("unicode_escape").encode("latin-1")
    except Exception:
        return s.encode("utf-8")

