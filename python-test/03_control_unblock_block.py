#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import time
from datetime import datetime

from ehl_protocol import (
    ETX,
    STX_CONTROLLER,
    STX_DISPENSER,
    EhlFrame,
    build_frame,
    describe_frame,
    extract_frames,
    hexdump,
    interpret_state_byte,
    interpret_volume_bytes,
)
from logging_utils import debug, die, info, init_logging, warn
from serial_linux import Rs485Config, open_serial


CMD_STATE = 0x4B
CMD_VOLUME = 0x45
CMD_UNBLOCK = 0x77
CMD_BLOCK = 0x69
# VB6 evidence in this repo indicates ACK "OK" is ASCII '0' (0x30) in the first payload byte.
OK_BYTE = 0x30


def default_log_path(*, port: str, addr: int, action: str) -> str:
    script_dir = os.path.dirname(os.path.abspath(__file__))
    log_dir = os.path.join(script_dir, "logs")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    port_safe = port.strip("/").replace("/", "_").replace(":", "_")
    return os.path.join(log_dir, f"ehl_control_{action}_{ts}_{port_safe}_addr{addr}.log")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description=(
            "CONTROL script: UNBLOCK/BLOCK.\n"
            "Some dispensers do not return the expected VB6-style ACK; by default we verify by polling STATE."
        )
    )
    p.add_argument("--port", required=True)
    p.add_argument("--addr", type=int, required=True)
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--timeout-ms", type=int, default=1200)
    p.add_argument(
        "--verify-timeout-ms",
        type=int,
        default=2500,
        help="How long to poll STATE for desired effect (default: 2500ms).",
    )
    p.add_argument(
        "--verify-interval-ms",
        type=int,
        default=200,
        help="Delay between STATE polls during verification (default: 200ms).",
    )
    p.add_argument(
        "--monitor-seconds",
        type=float,
        default=0.0,
        help=(
            "After UNBLOCK: for N seconds, poll STATE+VOLUME to observe behavior (default: 0=disabled). "
            "Useful to see how volume increments during a real delivery."
        ),
    )
    p.add_argument(
        "--monitor-interval-ms",
        type=int,
        default=500,
        help="Delay between STATE+VOLUME poll cycles during monitoring (default: 500ms).",
    )
    p.add_argument(
        "--monitor-debug-frames",
        action="store_true",
        help="During monitoring, also log raw RX frames at DEBUG level.",
    )
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


def read_until_frame(
    port,
    remainder: bytes,
    *,
    addr: int,
    expect_cmd: int,
    timeout_ms: int,
    debug_frames: bool = True,
) -> tuple[EhlFrame | None, bytes]:
    """
    Read from serial until we see a dispenser->controller frame for (addr, expect_cmd),
    or timeout. Returns (frame_or_none, new_remainder_bytes).
    """
    deadline = time.time() + (timeout_ms / 1000.0)
    buf = remainder
    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if chunk:
            buf += chunk
        frames, buf = extract_frames(buf)
        for f in frames:
            debug(f"RX {describe_frame(f)}", enabled=debug_frames)
            # Ignore echoed controller frames if adapter echoes TX.
            if f.stx != STX_DISPENSER:
                continue
            if f.addr != (addr & 0xFF):
                continue
            if f.cmd != (expect_cmd & 0xFF):
                continue
            return f, buf
    return None, buf


def await_ack(
    port,
    remainder: bytes,
    *,
    addr: int,
    cmd: int,
    timeout_ms: int,
    debug_frames: bool = True,
) -> tuple[bool, bytes]:
    deadline = time.time() + (timeout_ms / 1000.0)
    while time.time() < deadline:
        f, remainder = read_until_frame(
            port,
            remainder,
            addr=addr,
            expect_cmd=cmd,
            timeout_ms=50,
            debug_frames=debug_frames,
        )
        if f is None:
            continue
        if len(f.data) >= 1 and f.data[0] == OK_BYTE:
            info(f"ACK OK: RX {describe_frame(f)}")
            return True, remainder
        info(f"ACK REJECT/UNKNOWN: RX {describe_frame(f)}")
    return False, remainder


def poll_state_once(
    port,
    remainder: bytes,
    *,
    addr: int,
    timeout_ms: int,
    debug_frames: bool = True,
) -> tuple[int | None, bytes]:
    """
    Send STATE (0x4B) and return the raw state byte (data[0]) if received, else None.
    """
    frame = build_frame(addr, CMD_STATE, b"", stx=STX_CONTROLLER, etx=ETX)
    port.write(frame)
    f, remainder = read_until_frame(
        port,
        remainder,
        addr=addr,
        expect_cmd=CMD_STATE,
        timeout_ms=timeout_ms,
        debug_frames=debug_frames,
    )
    if f is None or len(f.data) < 1:
        return None, remainder
    return f.data[0] & 0xFF, remainder


def poll_volume_once(
    port,
    remainder: bytes,
    *,
    addr: int,
    timeout_ms: int,
    debug_frames: bool = True,
) -> tuple[float | None, bytes]:
    """
    Send VOLUME (0x45) and return liters as float if received, else None.
    """
    frame = build_frame(addr, CMD_VOLUME, b"", stx=STX_CONTROLLER, etx=ETX)
    port.write(frame)
    f, remainder = read_until_frame(
        port,
        remainder,
        addr=addr,
        expect_cmd=CMD_VOLUME,
        timeout_ms=timeout_ms,
        debug_frames=debug_frames,
    )
    if f is None:
        return None, remainder
    v = interpret_volume_bytes(f.data)
    if not v:
        return None, remainder
    try:
        return float(v), remainder
    except ValueError:
        return None, remainder


def await_state_effect(
    port,
    remainder: bytes,
    *,
    addr: int,
    desired_open_for_delivery: bool,
    verify_timeout_ms: int,
    verify_interval_ms: int,
    per_poll_timeout_ms: int,
    debug_frames: bool = True,
) -> tuple[bool, bytes]:
    deadline = time.time() + (verify_timeout_ms / 1000.0)
    last_sb: int | None = None
    while time.time() < deadline:
        sb, remainder = poll_state_once(
            port,
            remainder,
            addr=addr,
            timeout_ms=per_poll_timeout_ms,
            debug_frames=debug_frames,
        )
        if sb is not None:
            if last_sb is None or sb != last_sb:
                s = interpret_state_byte(sb)
                info(
                    f"STATE now: raw=0x{s['raw']:02X} bits={s['bits']} "
                    f"open_for_delivery={s['open_for_delivery']} startbutton_pressed={s['startbutton_pressed']} automode={s['automode']}"
                )
                last_sb = sb
            if bool(sb & 0x02) == bool(desired_open_for_delivery):
                return True, remainder
        time.sleep(max(0, verify_interval_ms) / 1000.0)
    return False, remainder


def monitor_state_and_volume(
    port,
    remainder: bytes,
    *,
    addr: int,
    seconds: float,
    interval_ms: int,
    per_poll_timeout_ms: int,
    debug_frames: bool,
) -> bytes:
    """
    After UNBLOCK, poll STATE and VOLUME repeatedly to observe state transitions and metering.
    Logs only when values change (plus an initial snapshot).
    """
    if seconds <= 0:
        return remainder
    info(f"Monitoring STATE+VOLUME for {seconds:.1f}s (interval={interval_ms}ms)...")
    deadline = time.time() + seconds
    last_sb: int | None = None
    last_v: float | None = None
    first = True

    while time.time() < deadline:
        sb, remainder = poll_state_once(
            port,
            remainder,
            addr=addr,
            timeout_ms=per_poll_timeout_ms,
            debug_frames=debug_frames,
        )
        v, remainder = poll_volume_once(
            port,
            remainder,
            addr=addr,
            timeout_ms=per_poll_timeout_ms,
            debug_frames=debug_frames,
        )

        if first or (sb is not None and sb != last_sb):
            if sb is None:
                warn("MONITOR: STATE timeout/no response")
            else:
                s = interpret_state_byte(sb)
                info(
                    f"MONITOR STATE: raw=0x{s['raw']:02X} bits={s['bits']} "
                    f"open_for_delivery={s['open_for_delivery']} startbutton_pressed={s['startbutton_pressed']} automode={s['automode']}"
                )
                last_sb = sb

        if first or (v is not None and (last_v is None or v != last_v)):
            if v is None:
                warn("MONITOR: VOLUME timeout/parse failure")
            else:
                info(f"MONITOR VOLUME: {v:.2f} L")
                last_v = v

        first = False
        time.sleep(max(0, interval_ms) / 1000.0)

    return remainder


def main() -> int:
    args = parse_args()
    if not args.log_file:
        args.log_file = default_log_path(port=args.port, addr=int(args.addr), action=str(args.action))
    init_logging(args.log_file, console_level="INFO", file_level="DEBUG")
    if not (1 <= args.addr <= 255):
        die("--addr must be 1..255")

    cmd = CMD_UNBLOCK if args.action == "unblock" else CMD_BLOCK
    desired_open = True if args.action == "unblock" else False
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
        remainder = b""
        port.write(frame)
        info("TX sent. Waiting briefly for VB6-style ACK (optional on some dispensers)...")
        ack_ok, remainder = await_ack(port, remainder, addr=args.addr, cmd=cmd, timeout_ms=args.timeout_ms)
        if ack_ok:
            if args.action == "unblock" and float(args.monitor_seconds) > 0:
                monitor_state_and_volume(
                    port,
                    remainder,
                    addr=args.addr,
                    seconds=float(args.monitor_seconds),
                    interval_ms=int(args.monitor_interval_ms),
                    per_poll_timeout_ms=min(int(args.timeout_ms), 800),
                    debug_frames=bool(args.monitor_debug_frames),
                )
            return 0
        warn("No VB6-style ACK seen. Verifying effect by polling STATE...")
        ok, remainder = await_state_effect(
            port,
            remainder,
            addr=args.addr,
            desired_open_for_delivery=desired_open,
            verify_timeout_ms=args.verify_timeout_ms,
            verify_interval_ms=args.verify_interval_ms,
            per_poll_timeout_ms=min(int(args.timeout_ms), 800),
        )
        if ok:
            info("VERIFY OK: STATE matches requested action.")
            if args.action == "unblock" and float(args.monitor_seconds) > 0:
                monitor_state_and_volume(
                    port,
                    remainder,
                    addr=args.addr,
                    seconds=float(args.monitor_seconds),
                    interval_ms=int(args.monitor_interval_ms),
                    per_poll_timeout_ms=min(int(args.timeout_ms), 800),
                    debug_frames=bool(args.monitor_debug_frames),
                )
            return 0
        warn("VERIFY FAILED: STATE did not reflect requested action within verify window.")
        return 2
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

