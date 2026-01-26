#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

from ehl_protocol import (
    ETX,
    STX_CONTROLLER,
    build_frame,
    describe_frame,
    extract_frames,
    hexdump,
)
from logging_utils import debug, die, info, warn
from serial_linux import Rs485Config, open_serial


CMD_STATE = 0x4B
CMD_ERROR_QUERY = 0x4C
CMD_VOLUME = 0x45
CMD_TANKBIT = 0xC5


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Low-risk EHL probe (read-only-ish poll commands).")
    p.add_argument("--port", required=True, help="Serial device path, e.g. /dev/ttyUSB0 or /dev/serial/by-id/...")
    p.add_argument("--addr", type=int, required=True, help="Dispenser address (1..255)")
    p.add_argument("--baud", type=int, default=9600, help="Baud rate (default: 9600)")
    p.add_argument("--timeout-ms", type=int, default=800, help="Read timeout per command (default: 800ms)")
    p.add_argument("--retries", type=int, default=2, help="Retries per command (default: 2)")
    p.add_argument("--delay-ms", type=int, default=150, help="Delay between commands (default: 150ms)")
    p.add_argument("--debug", action="store_true", help="Verbose debug logs")

    p.add_argument("--rs485", action="store_true", help="Best-effort enable Linux RS-485 ioctl mode")
    p.add_argument("--rts-before-ms", type=int, default=0, help="RS-485 RTS delay before send (ms)")
    p.add_argument("--rts-after-ms", type=int, default=0, help="RS-485 RTS delay after send (ms)")
    return p.parse_args()


def txrx(port, addr: int, cmd: int, *, timeout_ms: int, debug_enabled: bool) -> bool:
    frame = build_frame(addr, cmd, b"", stx=STX_CONTROLLER, etx=ETX)
    info(f"TX {hexdump(frame)}  (ADDR={addr} CMD=0x{cmd:02X})")
    port.write(frame)

    deadline = time.time() + (timeout_ms / 1000.0)
    buf = b""
    got_any = False

    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if chunk:
            got_any = True
            debug(f"RX+ {hexdump(chunk)}", enabled=debug_enabled)
            buf += chunk
            frames, buf = extract_frames(buf)
            for f in frames:
                info(f"RX {describe_frame(f)}")
                return True

    if got_any:
        warn(f"RX <bytes received but no valid frame parsed> remainder={hexdump(buf)}")
    else:
        warn("RX <none>")
    return False


def main() -> int:
    args = parse_args()
    if not (1 <= args.addr <= 255):
        die(f"--addr must be 1..255 (got {args.addr})")

    rs = Rs485Config(
        enabled=bool(args.rs485),
        delay_rts_before_send_ms=int(args.rts_before_ms),
        delay_rts_after_send_ms=int(args.rts_after_ms),
    )

    port, notes = open_serial(args.port, baud=args.baud, rs485=rs if args.rs485 else None)
    try:
        for n in notes:
            warn(n)
        info(f"Opened {args.port} @ {args.baud} 8N1 (non-blocking).")

        commands = [
            ("STATE", CMD_STATE),
            ("ERROR_QUERY", CMD_ERROR_QUERY),
            ("VOLUME", CMD_VOLUME),
            ("TANKBIT", CMD_TANKBIT),
        ]

        ok_total = 0
        for name, cmd in commands:
            info(f"--- Probe: {name} ---")
            ok = False
            for attempt in range(args.retries + 1):
                if attempt:
                    warn(f"Retry {attempt}/{args.retries} for {name}...")
                if txrx(port, args.addr, cmd, timeout_ms=args.timeout_ms, debug_enabled=args.debug):
                    ok = True
                    break
            if ok:
                ok_total += 1
            time.sleep(max(0, args.delay_ms) / 1000.0)

        info(f"Summary: {ok_total}/{len(commands)} commands produced a valid response frame.")
        if ok_total == 0:
            warn("No valid replies. Most common causes: wrong addr, wrong baud, A/B swapped, no bias/termination, port in use.")
            return 2
        return 0
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

