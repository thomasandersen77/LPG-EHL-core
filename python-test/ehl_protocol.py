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

