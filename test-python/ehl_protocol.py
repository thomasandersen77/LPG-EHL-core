"""
Minimal EHL (legacy dispenser) framing helpers.

Frame format (as used by the VB6 app in this repo):
  STX (1) + LEN (1) + ADDR (1) + CMD (1) + DATA (0..n) + CHK (1) + ETX (1)

Checksum is XOR of all bytes from STX up to last DATA byte
(i.e., everything except CHK and ETX).
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple


STX_CONTROLLER = 0x10
STX_DISPENSER = 0x20
ETX = 0x36


CMD_NAMES: Dict[int, str] = {
    0x1E: "OK",
    0x25: "ERROR_DATA",
    0x2F: "STOP",
    0x45: "VOLUME",
    0x4B: "STATE",
    0x4C: "ERROR_QUERY",
    0x5C: "PRICE",
    0x69: "BLOCK",
    0x6A: "LINETEST",
    0x70: "PROG_VOLUME",
    0x75: "PROG_AMOUNT",
    0x77: "UNBLOCK",
    0x81: "RESET",
    0xA9: "PROG_PRC",
    0xC3: "PRODUCT_SELECT",
    0xC5: "TANKBIT",
}


def cmd_name(cmd: int) -> str:
    return CMD_NAMES.get(cmd & 0xFF, f"UNKNOWN_0x{cmd & 0xFF:02X}")


def hexdump(b: bytes) -> str:
    return " ".join(f"{x:02X}" for x in b)


def xor_checksum(frame_wo_chk_etx: bytes) -> int:
    x = 0
    for bb in frame_wo_chk_etx:
        x ^= bb
    return x & 0xFF


def build_frame(
    addr: int,
    cmd: int,
    data: bytes = b"",
    *,
    stx: int = STX_CONTROLLER,
    etx: int = ETX,
) -> bytes:
    if not (1 <= addr <= 255):
        raise ValueError(f"addr must be 1..255, got {addr}")
    cmd_b = cmd & 0xFF
    length = 6 + len(data)
    if length > 255:
        raise ValueError(f"frame too long: {length}")

    header = bytes([stx & 0xFF, length & 0xFF, addr & 0xFF, cmd_b])
    body = header + data
    chk = xor_checksum(body)
    return body + bytes([chk, etx & 0xFF])


@dataclass(frozen=True)
class EhlFrame:
    stx: int
    length: int
    addr: int
    cmd: int
    data: bytes
    checksum: int
    etx: int

    @property
    def cmd_name(self) -> str:
        return cmd_name(self.cmd)

    def to_bytes(self) -> bytes:
        data = bytes(self.data)
        return build_frame(self.addr, self.cmd, data, stx=self.stx, etx=self.etx)


def parse_one_frame(buf: bytes) -> Optional[Tuple[EhlFrame, int]]:
    """
    Try to parse a single frame at buf[0]. Returns (frame, consumed_bytes) or None if incomplete/invalid.
    This expects buf[0] is already aligned to an STX.
    """
    if len(buf) < 6:
        return None

    stx = buf[0]
    if stx not in (STX_CONTROLLER, STX_DISPENSER):
        return None

    length = buf[1]
    if length < 6 or length > 255:
        return None
    if len(buf) < length:
        return None
    if buf[length - 1] != ETX:
        return None

    addr = buf[2]
    cmd = buf[3]
    data_len = length - 6
    data = buf[4 : 4 + data_len]
    checksum = buf[length - 2]

    chk_calc = xor_checksum(buf[: length - 2])
    if chk_calc != checksum:
        return None

    frame = EhlFrame(
        stx=stx,
        length=length,
        addr=addr,
        cmd=cmd,
        data=data,
        checksum=checksum,
        etx=ETX,
    )
    return frame, length


def extract_frames(stream_bytes: bytes) -> Tuple[List[EhlFrame], bytes]:
    """
    Extract as many valid frames as possible from a byte stream.
    Returns (frames, remainder_bytes).
    """
    frames: List[EhlFrame] = []
    i = 0
    n = len(stream_bytes)

    while i < n:
        # Find next STX
        while i < n and stream_bytes[i] not in (STX_CONTROLLER, STX_DISPENSER):
            i += 1
        if i >= n:
            return frames, b""

        parsed = parse_one_frame(stream_bytes[i:])
        if parsed is None:
            # Could be incomplete; keep remainder from i
            return frames, stream_bytes[i:]
        frame, consumed = parsed
        frames.append(frame)
        i += consumed

    return frames, b""


def describe_frame(f: EhlFrame) -> str:
    data_hex = hexdump(f.data) if f.data else "<none>"
    return (
        f"STX=0x{f.stx:02X} LEN={f.length} ADDR={f.addr} "
        f"CMD=0x{f.cmd:02X}({f.cmd_name}) DATA={data_hex} "
        f"CHK=0x{f.checksum:02X} ETX=0x{f.etx:02X}"
    )


def bits8(x: int) -> str:
    return f"{x & 0xFF:08b}"


def interpret_state_byte(state_b: int) -> Dict[str, object]:
    """
    Interpret STATE (0x4B) using VB6 bit checks:
      - Mid(state_string, 7, 1) => open for delivery  => bit1 (0x02)
      - Mid(state_string, 6, 1) => start button       => bit2 (0x04)
      - Mid(state_string, 5, 1) => automode           => bit3 (0x08)

    VB6 builds state_string using decimaltobinn(x(4)) (8-bit string, MSB first).
    """
    sb = state_b & 0xFF
    return {
        "raw": sb,
        "bits": bits8(sb),
        "open_for_delivery": bool(sb & 0x02),
        "startbutton_pressed": bool(sb & 0x04),
        "automode": bool(sb & 0x08),
        # Remaining bits are not labeled in VB6 in this repo
        "bit0": bool(sb & 0x01),
        "bit4": bool(sb & 0x10),
        "bit5": bool(sb & 0x20),
        "bit6": bool(sb & 0x40),
        "bit7": bool(sb & 0x80),
    }


def _vb_val_chr_byte(b: int) -> int:
    """
    VB6 uses Val(Chr(x(n))) for error levels. If the device sends ASCII digits,
    Val('3') -> 3. If it sends raw bytes, this is ambiguous; we fall back to numeric.
    """
    bb = b & 0xFF
    if 0x30 <= bb <= 0x39:
        return bb - 0x30
    return bb


_VB6_ERR_TEXT_NO: Dict[tuple[int, int], str] = {
    # Extracted from norgesgass_legacy/defs.bas -> logdisp_err()
    (1, 1): "Ingen kommunikasjon Display<-->CPU",
    (1, 2): "For mange kommunikasjonsfeil Display<-->CPU",
    (1, 3): "Intern feil Display",
    (2, 1): "Pulser ikke tilkoblet",
    (2, 2): "Feil rotasjon på pulser",
    (2, 3): "En pulserkanal mangler",
    (2, 4): "Feil serie på pulser",
    (2, 5): "Pulser buffer overflow",
    (2, 6): "LPG flow for høy",
    (3, 1): "Output overload(Para 10)",
    (3, 2): "Output control failure",
    (3, 3): "Startknapp aktivert under oppstart",
    (3, 4): "No load detected",
    (3, 5): "Termisk pumpebeskyttelse aktivert",
    (4, 1): "Minnefeil system",
    (4, 2): "Reset aktivert på hovedkort",
    (4, 3): "Strømbrudd",
    (4, 4): "Intern kommunikasjon CPU<-->Mainstream",
    (4, 5): "Calculations owerflow",
    (4, 7): "Brownout reset- for lite strøm til prosessor",
    (4, 8): "Ingen svar fra CPU",
    (5, 1): "Ingen Rs485 kommunikasjon",
    (6, 1): "Fylling har pågått for lenge (Para 22)",
    (6, 2): "For lang tid uten pulser ( Para 24)",
    (6, 3): "Flow for høy(Para 45)",
    (6, 4): "Maksimal grense for beløp nådd",
    (6, 6): "Pris er satt til 0.00",
    (6, 7): "Flow for liten (Para 48)",
    (6, 8): "Feil transaksjon state",
}


def interpret_error_query(data: bytes) -> Dict[str, object]:
    """
    Interpret ERROR_QUERY (0x4C) reply using VB6 'logdisp_err' mapping.
    VB6 treats main/sub levels as Val(Chr(x(4))), Val(Chr(x(5))).
    """
    if len(data) < 2:
        return {"ok": False, "reason": "missing bytes", "raw": hexdump(data)}
    main = _vb_val_chr_byte(data[0])
    sub = _vb_val_chr_byte(data[1])
    msg_no = _VB6_ERR_TEXT_NO.get((main, sub))
    # Best-effort English: translation of the VB6 Norwegian text (not new protocol info).
    msg_en = None
    if msg_no:
        msg_en = msg_no  # default to NO if we don't translate explicitly
        if msg_no == "Pris er satt til 0.00":
            msg_en = "Price is set to 0.00"
        elif msg_no == "Ingen Rs485 kommunikasjon":
            msg_en = "No RS-485 communication"
        elif msg_no == "Startknapp aktivert under oppstart":
            msg_en = "Start button active during startup"
        elif msg_no == "Strømbrudd":
            msg_en = "Power failure"
        elif msg_no == "Termisk pumpebeskyttelse aktivert":
            msg_en = "Thermal pump protection active"
    return {
        "ok": True,
        "main": main,
        "sub": sub,
        "message_no": msg_no,
        "message_en": msg_en,
        "raw": hexdump(data[:2]),
    }


def interpret_price_bytes(data: bytes) -> Optional[str]:
    # VB6: Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
    if len(data) < 4:
        return None
    try:
        return f"{chr(data[3])}{chr(data[2])}.{chr(data[1])}{chr(data[0])}"
    except Exception:
        return None


def interpret_volume_bytes(data: bytes) -> Optional[str]:
    # VB6: Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & \",\" & Chr(x(5)) & Chr(x(4))
    if len(data) < 5:
        return None
    try:
        return f"{chr(data[4])}{chr(data[3])}{chr(data[2])}.{chr(data[1])}{chr(data[0])}"
    except Exception:
        return None


def interpret_frame(f: EhlFrame) -> Optional[str]:
    """
    Return a best-effort human interpretation (based on VB6 code in this repo).
    Intended for field logs; never used for protocol validation.
    """
    if f.cmd == 0x4B and len(f.data) >= 1:
        s = interpret_state_byte(f.data[0])
        return (
            f"STATE raw=0x{s['raw']:02X} bits={s['bits']} "
            f"open_for_delivery={s['open_for_delivery']} "
            f"startbutton_pressed={s['startbutton_pressed']} "
            f"automode={s['automode']}"
        )
    if f.cmd == 0x4C:
        e = interpret_error_query(f.data)
        if not e.get("ok"):
            return f"ERROR_QUERY {e}"
        msg_no = e.get("message_no")
        msg_en = e.get("message_en")
        if msg_no and msg_en and msg_en != msg_no:
            return f"ERROR main={e['main']} sub={e['sub']} NO='{msg_no}' EN='{msg_en}'"
        if msg_no:
            return f"ERROR main={e['main']} sub={e['sub']} '{msg_no}'"
        return f"ERROR main={e['main']} sub={e['sub']} (unknown)"
    if f.cmd == 0x5C:
        p = interpret_price_bytes(f.data)
        if p:
            return f"PRICE {p}"
    if f.cmd == 0x45:
        v = interpret_volume_bytes(f.data)
        if v:
            return f"VOLUME {v}"
    return None

