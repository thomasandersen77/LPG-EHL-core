#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

from ehl_protocol import ETX, STX_CONTROLLER, build_frame, describe_frame, extract_frames, hexdump
from logging_utils import die, info, init_logging, warn
from serial_linux import Rs485Config, open_serial


CMD_STATE = 0x4B


def parse_range(s: str) -> tuple[int, int]:
    if "-" not in s:
        v = int(s)
        return v, v
    a, b = s.split("-", 1)
    return int(a), int(b)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Scan for responding dispenser addresses using STATE (0x4B).")
    p.add_argument("--port", required=True)
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--addr-range", default="1-32", help="Address range like 1-32 (default) or a single number")
    p.add_argument("--timeout-ms", type=int, default=250, help="Per-address read window (default: 250ms)")
    p.add_argument("--delay-ms", type=int, default=20, help="Delay between addresses (default: 20ms)")
    p.add_argument("--log-file", help="Also write logs to this file (in addition to stdout).")

    p.add_argument("--rs485", action="store_true")
    p.add_argument("--rts-before-ms", type=int, default=0)
    p.add_argument("--rts-after-ms", type=int, default=0)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    init_logging(args.log_file, console_level="INFO", file_level="DEBUG")
    a0, a1 = parse_range(args.addr_range)
    if a0 > a1:
        a0, a1 = a1, a0
    if a0 < 1 or a1 > 255:
        die("--addr-range must be within 1..255")

    rs = Rs485Config(
        enabled=bool(args.rs485),
        delay_rts_before_send_ms=int(args.rts_before_ms),
        delay_rts_after_send_ms=int(args.rts_after_ms),
    )

    port, notes = open_serial(args.port, baud=args.baud, rs485=rs if args.rs485 else None)
    try:
        for n in notes:
            warn(n)
        info(f"Scanning addresses {a0}..{a1} on {args.port} @ {args.baud} using STATE (0x4B)")
        found: list[int] = []
        buf = b""

        for addr in range(a0, a1 + 1):
            frame = build_frame(addr, CMD_STATE, b"", stx=STX_CONTROLLER, etx=ETX)
            port.write(frame)

            deadline = time.time() + (args.timeout_ms / 1000.0)
            got = False
            while time.time() < deadline:
                chunk = port.read(timeout_s=0.02)
                if not chunk:
                    continue
                buf += chunk
                frames, buf = extract_frames(buf)
                for f in frames:
                    if f.addr == addr:
                        info(f"ADDR {addr}: RX {describe_frame(f)}")
                        found.append(addr)
                        got = True
                        break
                if got:
                    break

            time.sleep(max(0, args.delay_ms) / 1000.0)

        if not found:
            warn("No responding addresses found in range.")
            return 2
        info(f"Found responding addresses: {', '.join(str(x) for x in found)}")
        return 0
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

