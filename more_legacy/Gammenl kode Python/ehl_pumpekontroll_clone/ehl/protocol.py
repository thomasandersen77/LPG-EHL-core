"""EHL-protokoll (historisk) – utledet direkte fra `pumpekontroll.frm`.

Dette er en 1:1 implementasjon av framing-reglene som VB6-koden bruker:

- PC/Controller -> Dispenser: STX = 0x10
- Dispenser -> PC/Controller: STX = 0x20
- ETX = 0x36
- LEN = total lengde på hele ramma (inkl. ETX)
- CHK = XOR over alle bytes fra STX til siste DATA-byte

Rammeformat:
    STX LEN ADDR CMD DATA... CHK ETX

Denne modulen inneholder bare framing + nyttige encode/decode-hjelpere for
VOLUME og PRICE slik de tolkes i pumpekontroll.frm.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Optional


STX_CONTROLLER = 0x10
STX_DISPENSER = 0x20
ETX = 0x36


@dataclass(frozen=True)
class EhlFrame:
    """Dekodet EHL-ramme."""

    stx: int
    addr: int
    cmd: int
    data: bytes
    checksum: int

    @property
    def length(self) -> int:
        # STX + LEN + ADDR + CMD + DATA + CHK + ETX
        return 6 + len(self.data)


def xor_checksum(payload: Iterable[int]) -> int:
    chk = 0
    for b in payload:
        chk ^= (b & 0xFF)
    return chk & 0xFF


def encode_frame(*, addr: int, cmd: int, data: bytes = b"", from_controller: bool = True) -> bytes:
    """Bygger en EHL-ramme.

    Dette speiler måten `pumpekontroll.frm` bygger `y()` og sender Chr(...) på.
    """

    stx = STX_CONTROLLER if from_controller else STX_DISPENSER
    addr_b = addr & 0xFF
    cmd_b = cmd & 0xFF
    if data is None:
        data = b""
    data_bytes = bytes(data)
    length = 6 + len(data_bytes)
    if not (0 <= length <= 255):
        raise ValueError(f"Frame too long: {length}")

    header = bytes([stx, length & 0xFF, addr_b, cmd_b])
    chk = xor_checksum(header + data_bytes)
    return header + data_bytes + bytes([chk, ETX])


def decode_frame(raw: bytes, *, require_stx: Optional[int] = None) -> EhlFrame:
    """Dekoder en komplett råramme (inkl. ETX). Validerer LEN, CHK og ETX.

    `pumpekontroll.frm` validerer eksplisitt at:
      - raw[-1] == 0x36
      - raw[0] == 0x20 (for innkommende)
      - raw[1] == faktisk lengde
      - XOR checksum stemmer

    Her kan du sette require_stx=0x20 for å matche mottakssiden.
    """

    if len(raw) < 6:
        raise ValueError(f"Frame too short: {len(raw)}")

    stx = raw[0]
    if require_stx is not None and stx != (require_stx & 0xFF):
        raise ValueError(f"Unexpected STX 0x{stx:02X}, expected 0x{require_stx & 0xFF:02X}")

    length = raw[1]
    if length != len(raw):
        raise ValueError(f"LEN mismatch: header={length}, actual={len(raw)}")

    if raw[-1] != ETX:
        raise ValueError(f"Missing ETX 0x36 at end, got 0x{raw[-1]:02X}")

    chk_expected = raw[-2]
    chk_calc = xor_checksum(raw[:-2])
    if chk_expected != chk_calc:
        raise ValueError(
            f"Checksum mismatch: expected 0x{chk_expected:02X}, calculated 0x{chk_calc:02X}"
        )

    addr = raw[2]
    cmd = raw[3]
    data = raw[4:-2]
    return EhlFrame(stx=stx, addr=addr, cmd=cmd, data=data, checksum=chk_expected)


# ---------- DATAFORMAT-HJELPERE (kun det pumpekontroll faktisk tolker) ----------


def decode_price_from_data(data: bytes) -> float:
    """Tolk PRICE-data som `pumpekontroll.frm` gjør.

    Pumpekontroll bygger tekst:
        Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))

    dvs 4 ASCII-siffer i LSB-first rekkefølge:
        data[0]=p0 (0.01), data[1]=p1 (0.1), data[2]=p2 (1), data[3]=p3 (10)

    Returnerer pris i kr/l som float.
    """

    if len(data) != 4:
        raise ValueError(f"PRICE expects 4 data bytes, got {len(data)}")

    p0, p1, p2, p3 = [chr(b) for b in data]
    s = f"{p3}{p2}{p1}{p0}"  # f.eks "1604"
    if not s.isdigit():
        raise ValueError(f"Invalid PRICE digits: {s!r}")
    return int(s) / 100.0


def decode_volume_from_data(data: bytes) -> float:
    """Tolk VOLUME-data som `pumpekontroll.frm` gjør.

    Pumpekontroll gjør:
        CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))

    dvs 5 ASCII-siffer i LSB-first:
        data[0]=d0 (0.01 L), data[1]=d1 (0.1), data[2]=d2 (1), data[3]=d3 (10), data[4]=d4 (100)

    Returnerer liter som float.
    """

    if len(data) != 5:
        raise ValueError(f"VOLUME expects 5 data bytes, got {len(data)}")

    d0, d1, d2, d3, d4 = [chr(b) for b in data]
    s = f"{d4}{d3}{d2}{d1}{d0}"  # f.eks "04550" for 45.50 L
    if not s.isdigit():
        raise ValueError(f"Invalid VOLUME digits: {s!r}")
    return int(s) / 100.0


def state_bits_from_byte(state: int) -> dict[str, bool]:
    """Returnerer de tre bittene pumpekontroll bryr seg om i STATE.

    VB6 leser Mid(state_string, pos, 1) fra en 8-bits streng.
    Med MSB først blir pos 5..8:
      pos5 = bit3 (0x08)
      pos6 = bit2 (0x04)
      pos7 = bit1 (0x02)
      pos8 = bit0 (0x01)

    STATE:
      - automode: pos5 => 0x08
      - startbuttonpressed: pos6 => 0x04
      - openfordelivery: pos7 => 0x02
    """

    b = state & 0xFF
    return {
        "automode": bool(b & 0x08),
        "startbuttonpressed": bool(b & 0x04),
        "openfordelivery": bool(b & 0x02),
    }


def tank_bits_from_byte(tstat: int) -> dict[str, bool]:
    """Returnerer de to bittene pumpekontroll bruker i TANK (CMD=197/0xC5).

    Pumpekontroll.frm (Case 197):
      - Mid(state_string, 8, 1) => trans_finished_powerfault
      - Mid(state_string, 5, 1) => trans_unaccounted

    Med MSB først gir det:
      pos8 => bit0 (0x01)
      pos5 => bit3 (0x08)
    """

    b = tstat & 0xFF
    return {
        "trans_finished_powerfault": bool(b & 0x01),
        "trans_unaccounted": bool(b & 0x08),
    }
