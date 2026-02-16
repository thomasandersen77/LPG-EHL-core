"""Byte-for-byte parser som etterligner mottakslogikken i `pumpekontroll.frm`.

VB6-koden i MSComm1_OnComm gjør (for hver mottatt byte):

- u starter på -1
- Hvis u > 15: reset (u=-1, commandtext_in="")
- u = u + 1
- x(u) = Asc(MSComm1.Input)
- Når x(u)==0x36 (ETX) og x(0)==0x20 og x(1)==(u+1):
    - regn XOR-checksum over x(0..u-2)
    - hvis OK: behandle kommando
    - ellers: logg
    - u=-1, buffer nulles

Viktig: dersom ETX kommer uten at STX/LEN matcher, gjør VB6 i praksis *ingenting* med bufferen.
Den blir først nullstilt ved overflow (u>15) eller når en "formelt komplett" ramme (ETX+STX+LEN) er
mottatt.

Denne parseren følger samme regelsett:

- maks buffer 16 bytes (0..15)
- ramme regnes kun "komplett" når vi ser ETX *og* STX==0x20 *og* LEN==faktisk lengde
- checksumfeil -> drop ramme, reset buffer
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

from .protocol import ETX, STX_DISPENSER, decode_frame, EhlFrame


@dataclass
class ParseStats:
    frames_ok: int = 0
    frames_bad_checksum: int = 0
    frames_bad_len: int = 0
    frames_bad_stx: int = 0
    buffer_overflows: int = 0


class EhlStreamParser:
    def __init__(self) -> None:
        self._buf = bytearray()
        self.stats = ParseStats()

    def reset(self) -> None:
        self._buf.clear()

    def feed_byte(self, b: int) -> Optional[EhlFrame]:
        """Feed én byte (0..255).

        Returnerer EhlFrame når en hel ramme er funnet og validert.
        """

        b &= 0xFF

        # VB6: If u > 15 Then u=-1 (før den tar inn neste byte)
        # u er siste indeks. Når buffer-lengde N => u=N-1.
        # VB6 resetter derfor først når N-1 > 15 => N > 16.
        if len(self._buf) > 16:
            self.stats.buffer_overflows += 1
            self._buf.clear()

        self._buf.append(b)

        # VB6 trigger bare parsing på ETX
        if b != ETX:
            return None

        # VB6: If x(u)=54 And x(0)=32 And x(1)=(u+1) Then
        if len(self._buf) < 2:
            return None

        if self._buf[0] != STX_DISPENSER:
            # VB6: ingen reset her (den lar buffer stå)
            self.stats.frames_bad_stx += 1
            return None

        length = self._buf[1]
        if length != len(self._buf):
            # VB6: ingen reset her (den lar buffer stå)
            self.stats.frames_bad_len += 1
            return None

        # Nå har vi en "formelt komplett" ramme (STX+LEN+ETX), VB6 resetter buffer uansett OK/feil.
        raw = bytes(self._buf)
        self._buf.clear()

        try:
            frame = decode_frame(raw, require_stx=STX_DISPENSER)
            self.stats.frames_ok += 1
            return frame
        except ValueError as e:
            msg = str(e)
            if "Checksum mismatch" in msg:
                self.stats.frames_bad_checksum += 1
            elif "LEN mismatch" in msg:
                self.stats.frames_bad_len += 1
            elif "Unexpected STX" in msg:
                self.stats.frames_bad_stx += 1
            return None
