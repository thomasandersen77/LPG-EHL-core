#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

from ehl_protocol import describe_frame, extract_frames, hexdump
from logging_utils import info, warn
from serial_linux import Rs485Config, open_serial


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Listen-only: print any valid EHL frames observed on the serial line.")
    p.add_argument("--port", required=True)
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--duration-s", type=int, default=30, help="How long to listen (default: 30s)")
    p.add_argument("--show-raw", action="store_true", help="Also print raw byte chunks (hexdump)")

    p.add_argument("--rs485", action="store_true")
    p.add_argument("--rts-before-ms", type=int, default=0)
    p.add_argument("--rts-after-ms", type=int, default=0)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    rs = Rs485Config(
        enabled=bool(args.rs485),
        delay_rts_before_send_ms=int(args.rts_before_ms),
        delay_rts_after_send_ms=int(args.rts_after_ms),
    )

    port, notes = open_serial(args.port, baud=args.baud, rs485=rs if args.rs485 else None)
    try:
        for n in notes:
            warn(n)
        info(f"Listening on {args.port} @ {args.baud} for {args.duration_s}s ...")
        deadline = time.time() + max(1, args.duration_s)
        buf = b""
        seen = 0
        while time.time() < deadline:
            chunk = port.read(timeout_s=0.2)
            if not chunk:
                continue
            if args.show_raw:
                info(f"RAW {hexdump(chunk)}")
            buf += chunk
            frames, buf = extract_frames(buf)
            for f in frames:
                seen += 1
                info(f"RX {describe_frame(f)}")
        info(f"Done. Valid frames observed: {seen}")
        return 0
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

