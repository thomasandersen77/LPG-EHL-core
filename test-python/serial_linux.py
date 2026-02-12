"""
Linux-only minimal serial port helper (no external deps).

Designed for Debian 32-bit field use.
Provides:
  - open serial port in raw mode (8N1)
  - read/write with select-based timeout
  - optional RS-485 ioctl enabling (best-effort)
"""

from __future__ import annotations

import errno
import fcntl
import os
import select
import struct
import termios
import time
from dataclasses import dataclass
from typing import Optional


BAUD_MAP = {
    1200: termios.B1200,
    2400: termios.B2400,
    4800: termios.B4800,
    9600: termios.B9600,
    19200: termios.B19200,
    38400: termios.B38400,
    57600: termios.B57600,
    115200: termios.B115200,
}


# Linux RS485 ioctls (won't exist on macOS; this module is for Debian/Linux)
TIOCGRS485 = 0x542E
TIOCSRS485 = 0x542F

SER_RS485_ENABLED = 1 << 0
SER_RS485_RTS_ON_SEND = 1 << 1
SER_RS485_RTS_AFTER_SEND = 1 << 2
SER_RS485_RX_DURING_TX = 1 << 4


@dataclass
class Rs485Config:
    enabled: bool = False
    rts_on_send: bool = True
    rts_after_send: bool = False
    rx_during_tx: bool = False
    delay_rts_before_send_ms: int = 0
    delay_rts_after_send_ms: int = 0


class SerialPort:
    def __init__(self, fd: int, path: str):
        self.fd = fd
        self.path = path

    def close(self) -> None:
        try:
            os.close(self.fd)
        finally:
            self.fd = -1

    def write(self, data: bytes) -> None:
        view = memoryview(data)
        while view:
            try:
                n = os.write(self.fd, view)
                view = view[n:]
            except InterruptedError:
                continue

        try:
            termios.tcdrain(self.fd)
        except Exception:
            # Some USB adapters may not support tcdrain well; ignore.
            pass

    def read(self, max_bytes: int = 4096, timeout_s: float = 0.5) -> bytes:
        if timeout_s < 0:
            timeout_s = 0
        r, _, _ = select.select([self.fd], [], [], timeout_s)
        if not r:
            return b""
        try:
            return os.read(self.fd, max_bytes)
        except OSError as e:
            if e.errno in (errno.EAGAIN, errno.EWOULDBLOCK):
                return b""
            raise


def _set_raw_8n1(fd: int, baud: int) -> None:
    if baud not in BAUD_MAP:
        raise ValueError(f"Unsupported baud {baud}. Supported: {sorted(BAUD_MAP.keys())}")

    attrs = termios.tcgetattr(fd)

    # iflag
    attrs[0] &= ~(
        termios.IGNBRK
        | termios.BRKINT
        | termios.PARMRK
        | termios.ISTRIP
        | termios.INLCR
        | termios.IGNCR
        | termios.ICRNL
        | termios.IXON
        | getattr(termios, "IXOFF", 0)
        | getattr(termios, "IXANY", 0)
    )

    # oflag
    attrs[1] &= ~termios.OPOST

    # cflag: 8N1, local, read-enable, no flow control
    attrs[2] &= ~(termios.CSIZE | termios.PARENB | termios.CSTOPB)
    attrs[2] |= termios.CS8 | termios.CREAD | termios.CLOCAL
    if hasattr(termios, "CRTSCTS"):
        attrs[2] &= ~termios.CRTSCTS

    # lflag
    attrs[3] &= ~(termios.ECHO | termios.ECHONL | termios.ICANON | termios.ISIG | termios.IEXTEN)

    # cc: non-blocking reads driven by select()
    attrs[6][termios.VMIN] = 0
    attrs[6][termios.VTIME] = 0

    # set baud
    speed = BAUD_MAP[baud]
    attrs[4] = speed  # ispeed
    attrs[5] = speed  # ospeed

    termios.tcsetattr(fd, termios.TCSANOW, attrs)


def _maybe_enable_rs485(fd: int, rs485: Rs485Config) -> Optional[str]:
    if not rs485.enabled:
        return None

    flags = SER_RS485_ENABLED
    if rs485.rts_on_send:
        flags |= SER_RS485_RTS_ON_SEND
    if rs485.rts_after_send:
        flags |= SER_RS485_RTS_AFTER_SEND
    if rs485.rx_during_tx:
        flags |= SER_RS485_RX_DURING_TX

    # struct serial_rs485 (linux/serial.h):
    #   __u32 flags;
    #   __u32 delay_rts_before_send;
    #   __u32 delay_rts_after_send;
    #   __u32 padding[5];
    buf = struct.pack(
        "IIIIIIII",
        flags,
        int(rs485.delay_rts_before_send_ms),
        int(rs485.delay_rts_after_send_ms),
        0,
        0,
        0,
        0,
        0,
    )

    try:
        fcntl.ioctl(fd, TIOCSRS485, buf)
        return "RS-485 ioctl enabled"
    except OSError as e:
        return f"RS-485 ioctl not supported (ignored): {e}"


def open_serial(
    path: str,
    *,
    baud: int = 9600,
    rs485: Optional[Rs485Config] = None,
) -> tuple[SerialPort, list[str]]:
    notes: list[str] = []
    fd = os.open(path, os.O_RDWR | os.O_NOCTTY | os.O_NONBLOCK)
    try:
        _set_raw_8n1(fd, baud)
        if rs485 is not None:
            msg = _maybe_enable_rs485(fd, rs485)
            if msg:
                notes.append(msg)
        return SerialPort(fd, path), notes
    except Exception:
        os.close(fd)
        raise


def sleep_ms(ms: int) -> None:
    time.sleep(max(0, ms) / 1000.0)

