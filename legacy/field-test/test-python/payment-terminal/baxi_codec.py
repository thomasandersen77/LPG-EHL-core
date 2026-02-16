from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, Optional

US = 0x1F  # Unit Separator
RS = 0x1E  # Record Separator (often used as message terminator in examples)

# From the guide (PIN bypass example):
#   H49   = command code SEND DATA
#   H3231 = JSON format Request
#   H3232 = JSON format Response
CMD_SEND_DATA = 0x49  # 'I'
FMT_JSON_REQUEST = b"\x32\x31"  # "21"
FMT_JSON_RESPONSE = b"\x32\x32"  # "22"


def split_by_rs(stream: bytes) -> tuple[list[bytes], bytes]:
    """
    Split a TCP byte stream into records terminated by RS (0x1E).
    Returns (records_including_rs, remainder).

    This is a heuristic (but matches multiple examples in the guide).
    """
    out: list[bytes] = []
    start = 0
    while True:
        i = stream.find(bytes([RS]), start)
        if i < 0:
            break
        out.append(stream[: i + 1])
        stream = stream[i + 1 :]
        start = 0
    return out, stream


def ascii_preview(b: bytes, *, max_len: int = 200) -> str:
    b = b[:max_len]
    s = []
    for ch in b:
        if 32 <= ch <= 126:
            s.append(chr(ch))
        elif ch == US:
            s.append("<US>")
        elif ch == RS:
            s.append("<RS>")
        elif ch == 10:
            s.append("<LF>")
        elif ch == 13:
            s.append("<CR>")
        else:
            s.append(f"\\x{ch:02x}")
    return "".join(s)


def build_send_data_json(payload: Any, *, is_response: bool) -> bytes:
    """
    Build the wire payload for the documented SEND DATA JSON message pattern.

    NOTE: The guide provides the header bytes, but not the outer framing/terminator.
    We append RS (0x1E) because many examples show RS used as record terminator.
    """
    body = json.dumps(payload, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    fmt = FMT_JSON_RESPONSE if is_response else FMT_JSON_REQUEST
    return bytes([CMD_SEND_DATA]) + fmt + body + bytes([RS])


@dataclass(frozen=True)
class Decoded:
    kind: str
    details: str
    json_obj: Optional[Any] = None


def try_decode(record_including_rs: bytes) -> Optional[Decoded]:
    """
    Best-effort decoding of a single RS-terminated record.
    Returns None if nothing recognized.
    """
    if not record_including_rs:
        return None

    rec = record_including_rs
    if rec and rec[-1] == RS:
        rec = rec[:-1]

    # SEND DATA JSON (documented header)
    if len(rec) >= 3 and rec[0] == CMD_SEND_DATA and rec[1:3] in (FMT_JSON_REQUEST, FMT_JSON_RESPONSE):
        fmt = "json_request" if rec[1:3] == FMT_JSON_REQUEST else "json_response"
        raw = rec[3:]
        try:
            s = raw.decode("utf-8", errors="strict")
            obj = json.loads(s)
            return Decoded(kind=fmt, details=s, json_obj=obj)
        except Exception:
            return Decoded(kind=fmt, details=f"<non-json payload> {ascii_preview(raw)}", json_obj=None)

    # TLD-ish / separator-based (guide uses US/RS delimiters a lot)
    if bytes([US]) in rec:
        parts = rec.split(bytes([US]))
        preview = " | ".join(ascii_preview(p, max_len=120) for p in parts[:10])
        more = "" if len(parts) <= 10 else f" | ... ({len(parts)} fields)"
        return Decoded(kind="us_delimited", details=preview + more)

    return None

