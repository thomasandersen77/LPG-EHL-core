#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

from ehl_protocol import ETX, STX_CONTROLLER, build_frame, describe_frame, extract_frames, hexdump
from logging_utils import die, info, warn
from serial_linux import Rs485Config, open_serial


CMD_UNBLOCK = 0x77
CMD_BLOCK = 0x69


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="CONTROL script: UNBLOCK/BLOCK. Requires explicit acknowledgement.")
    p.add_argument("--port", required=True)
    p.add_argument("--addr", type=int, required=True)
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--timeout-ms", type=int, default=1200)
    p.add_argument("--dry-run", action="store_true", help="Do not write to serial; just print what would be sent.")

    p.add_argument("--rs485", action="store_true")
    p.add_argument("--rts-before-ms", type=int, default=0)
    p.add_argument("--rts-after-ms", type=int, default=0)

    p.add_argument(
        "--i-understand-this-can-affect-real-hardware",
        action="store_true",
        help="Required safety acknowledgement to actually send commands.",
    )

    sub = p.add_subparsers(dest="action", required=True)
    sub.add_parser("unblock", help="Send UNBLOCK (0x77) - enables delivery mode.")
    sub.add_parser("block", help="Send BLOCK (0x69) - stops/blocks delivery mode.")
    return p.parse_args()


def await_any_frame(port, timeout_ms: int) -> bool:
    deadline = time.time() + (timeout_ms / 1000.0)
    buf = b""
    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if not chunk:
            continue
        buf += chunk
        frames, buf = extract_frames(buf)
        for f in frames:
            info(f"RX {describe_frame(f)}")
            return True
    return False


def main() -> int:
    args = parse_args()
    if not (1 <= args.addr <= 255):
        die("--addr must be 1..255")

    cmd = CMD_UNBLOCK if args.action == "unblock" else CMD_BLOCK
    frame = build_frame(args.addr, cmd, b"", stx=STX_CONTROLLER, etx=ETX)

    info(f"Prepared {args.action.upper()} frame for ADDR={args.addr}: {hexdump(frame)}")
    warn("This can affect real-world hardware state.")

    if args.dry_run:
        info("Dry-run enabled; not writing to serial.")
        return 0

    if not args.i_understand_this_can_affect_real_hardware:
        die("Refusing to send. Pass --i-understand-this-can-affect-real-hardware to proceed.")

    rs = Rs485Config(
        enabled=bool(args.rs485),
        delay_rts_before_send_ms=int(args.rts_before_ms),
        delay_rts_after_send_ms=int(args.rts_after_ms),
    )

    port, notes = open_serial(args.port, baud=args.baud, rs485=rs if args.rs485 else None)
    try:
        for n in notes:
            warn(n)
        info(f"Opened {args.port} @ {args.baud}. Sending now...")
        port.write(frame)
        info("TX sent. Waiting for any valid response frame...")
        if await_any_frame(port, args.timeout_ms):
            info("Got a valid response frame.")
            return 0
        warn("No valid response frame seen (device may still have acted; check STATE with 01_probe_readonly.py).")
        return 2
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

