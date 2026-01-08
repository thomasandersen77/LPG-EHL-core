"""Seriell klient for historisk EHL (pumpekontroll.frm).

Denne klassen gjør:
- åpner serial port (pyserial)
- leser 1 byte av gangen (samme som MSComm1.InputLen = 1)
- bruker EhlStreamParser til å finne rammer
- sender rammer med STX=0x10

Den tar ikke stilling til UI/DB/betaling – kun kommunikasjon.
"""

from __future__ import annotations

import threading
import time
from dataclasses import dataclass
from typing import Callable, Optional

try:
    import serial  # type: ignore
except Exception as e:  # pragma: no cover
    serial = None  # type: ignore

from .protocol import encode_frame, EhlFrame
from .stream_parser import EhlStreamParser


# CMD-koder slik de brukes i pumpekontroll.frm
CMD_STATE = 0x4B
CMD_TANK = 0xC5
CMD_VOLUME = 0x45
CMD_LINETEST = 0x6A
CMD_BLOCK = 0x69
CMD_UNBLOCK = 0x77
CMD_RESET_ZER = 0x81
CMD_PRESTART = 0xC3


@dataclass
class SerialConfig:
    port: str
    baudrate: int = 9600
    bytesize: int = 8
    parity: str = 'N'
    stopbits: int = 1
    timeout_s: float = 0.1


class EhlSerialClient:
    def __init__(
        self,
        cfg: SerialConfig,
        addr: int,
        on_frame: Callable[[EhlFrame], None],
    ) -> None:
        if serial is None:
            raise RuntimeError("pyserial er ikke tilgjengelig. Installer med: pip install pyserial")

        self.cfg = cfg
        self.addr = addr & 0xFF
        self._on_frame = on_frame

        self._ser: Optional[serial.Serial] = None
        self._stop = threading.Event()
        self._thread: Optional[threading.Thread] = None

        self._parser = EhlStreamParser()

        # VB6 har en 'rts' bool som blokkerer sending mens mottak håndteres.
        self._rts = threading.Event()
        self._rts.set()

        self._tx_lock = threading.Lock()

    @property
    def parser_stats(self):
        return self._parser.stats

    def open(self) -> None:
        self._ser = serial.Serial(
            port=self.cfg.port,
            baudrate=self.cfg.baudrate,
            bytesize=self.cfg.bytesize,
            parity=self.cfg.parity,
            stopbits=self.cfg.stopbits,
            timeout=self.cfg.timeout_s,
        )
        self._stop.clear()
        self._thread = threading.Thread(target=self._reader_loop, name="EHL-Reader", daemon=True)
        self._thread.start()

    def close(self) -> None:
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=1.0)
        if self._ser is not None:
            try:
                self._ser.close()
            finally:
                self._ser = None

    def _reader_loop(self) -> None:
        assert self._ser is not None
        while not self._stop.is_set():
            b = self._ser.read(1)
            if not b:
                continue

            # VB6: rts=False ved comEvReceive, og rts=True til slutt
            self._rts.clear()
            try:
                frame = self._parser.feed_byte(b[0])
                if frame is not None:
                    self._on_frame(frame)
            finally:
                self._rts.set()

    def send(self, cmd: int, data: bytes = b"", wait_ms: int = 100) -> bytes:
        """Send en kommando.

        Speiler `comm_out waittime, Chr(...)`-bruken i pumpekontroll.frm:
        - venter til rts=True
        - skriver bytes
        - sover wait_ms

        Returnerer de sendte bytes (for logging/test).
        """

        if self._ser is None:
            raise RuntimeError("Serial port er ikke åpnet")

        # VB6: While Not rts: DoEvents
        self._rts.wait(timeout=2.0)

        frame = encode_frame(addr=self.addr, cmd=cmd, data=data, from_controller=True)

        with self._tx_lock:
            self._ser.write(frame)
            self._ser.flush()

        time.sleep(max(wait_ms, 0) / 1000.0)
        return frame

    # --------- kommandoer som finnes eksplisitt i pumpekontroll.frm ---------

    def poll_state(self) -> bytes:
        return self.send(CMD_STATE, b"", wait_ms=100)

    def poll_tank(self) -> bytes:
        return self.send(CMD_TANK, b"", wait_ms=100)

    def poll_volume(self) -> bytes:
        return self.send(CMD_VOLUME, b"", wait_ms=100)

    def poll_linetest(self) -> bytes:
        return self.send(CMD_LINETEST, b"", wait_ms=100)

    def block(self) -> bytes:
        return self.send(CMD_BLOCK, b"", wait_ms=100)

    def prestart(self) -> bytes:
        # pumpekontroll sender CMD=C3 med 1 databyte 0x30
        return self.send(CMD_PRESTART, bytes([0x30]), wait_ms=50)

    def unblock(self) -> bytes:
        return self.send(CMD_UNBLOCK, b"", wait_ms=100)

    def reset_zer(self) -> bytes:
        return self.send(CMD_RESET_ZER, b"", wait_ms=100)
