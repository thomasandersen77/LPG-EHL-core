#!/usr/bin/env python3
from __future__ import annotations

import argparse
import time

from ehl_protocol import ETX, STX_CONTROLLER, build_frame, describe_frame, extract_frames, hexdump
from logging_utils import debug, die, info, init_logging, warn
from serial_linux import Rs485Config, open_serial


CMD_UNBLOCK = 0x77
CMD_BLOCK = 0x69
OK_BYTE = 0x1E  # VB6 checks x(4)=30 (decimal) for OK


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="CONTROL script: UNBLOCK/BLOCK. Requires VB6-style acknowledgement (cmd echo + OK byte)."
    )
    p.add_argument("--port", required=True)
    p.add_argument("--addr", type=int, required=True)
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--timeout-ms", type=int, default=1200)
    p.add_argument("--dry-run", action="store_true", help="Do not write to serial; just print what would be sent.")
    p.add_argument("--log-file", help="Also write logs to this file (in addition to stdout).")

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


def await_ack(port, *, addr: int, cmd: int, timeout_ms: int, debug_frames: bool = True) -> bool:
    deadline = time.time() + (timeout_ms / 1000.0)
    buf = b""
    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if not chunk:
            continue
        buf += chunk
        frames, buf = extract_frames(buf)
        for f in frames:
            debug(f"RX {describe_frame(f)}", enabled=debug_frames)
            if f.addr != (addr & 0xFF):
                continue
            if f.cmd != (cmd & 0xFF):
                continue
            if len(f.data) >= 1 and f.data[0] == OK_BYTE:
                info(f"ACK OK: RX {describe_frame(f)}")
                return True
            info(f"ACK REJECT/UNKNOWN: RX {describe_frame(f)}")
    return False


def main() -> int:
    args = parse_args()
    init_logging(args.log_file, console_level="INFO", file_level="DEBUG")
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
        info("TX sent. Waiting for VB6-style ACK (cmd echo + data[0]==0x1E)...")
        if await_ack(port, addr=args.addr, cmd=cmd, timeout_ms=args.timeout_ms):
            return 0
        warn("No VB6-style ACK seen (device may still have acted; verify with STATE via 01_probe_readonly.py).")
        return 2
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

