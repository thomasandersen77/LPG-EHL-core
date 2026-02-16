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
    build_frame,
    describe_frame,
    extract_frames,
    hexdump,
    interpret_error_query,
    interpret_frame,
    interpret_state_byte,
)
from logging_utils import debug, die, info, init_logging, warn
from serial_linux import Rs485Config, open_serial


CMD_STATE = 0x4B
CMD_ERROR_QUERY = 0x4C
CMD_VOLUME = 0x45
CMD_TANKBIT = 0xC5

CMD_UNBLOCK = 0x77
CMD_BLOCK = 0x69
CMD_RESET = 0x81
CMD_PRODUCT_SELECT = 0xC3

# VB6 evidence in this repo indicates ACK "OK" is ASCII '0' (0x30) in the first payload byte.
OK_BYTE = 0x30


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description=(
            "Field test: attempt VB6-like UNBLOCK, hold for N seconds, then BLOCK again.\n"
            "Designed for reverse engineering: file log is exhaustive; console is operator-friendly."
        )
    )
    p.add_argument("--port", required=True)
    p.add_argument("--addr", type=int, default=33, help="Target address (default: 33 / 0x21)")
    p.add_argument("--baud", type=int, default=9600)
    p.add_argument("--timeout-ms", type=int, default=1200, help="Timeout waiting for responses/ACKs (default: 1200ms)")
    p.add_argument("--retries", type=int, default=3, help="Retries for UNBLOCK attempts (default: 3)")
    p.add_argument("--hold-seconds", type=int, default=30, help="How long to keep it unblocked (default: 30s)")
    p.add_argument("--poll-ms", type=int, default=400, help="STATE poll interval during hold (default: 400ms)")
    p.add_argument("--verify-ms", type=int, default=4000, help="How long to wait for open bit to clear after BLOCK (default: 4000ms)")

    p.add_argument("--dry-run", action="store_true", help="Do not write to serial; just log what would be sent.")

    p.add_argument("--rs485", action="store_true")
    p.add_argument("--rts-before-ms", type=int, default=0)
    p.add_argument("--rts-after-ms", type=int, default=0)

    p.add_argument(
        "--console-verbosity",
        choices=["important", "chatty"],
        default="important",
        help="Console verbosity (default: important). File logs are always chatty.",
    )

    p.add_argument(
        "--log-file",
        help="Write logs to this file. If omitted, a timestamped file is created under --log-dir.",
    )
    p.add_argument(
        "--log-dir",
        default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs"),
        help="Directory for default log file (default: python-test/logs)",
    )

    p.add_argument(
        "--disable-product-select-fallback",
        action="store_true",
        help="Disable fallback PRODUCT_SELECT attempt if UNBLOCK is not ACKed.",
    )
    p.add_argument(
        "--disable-reset-fallback",
        action="store_true",
        help="Disable fallback RESET attempt if UNBLOCK is not ACKed.",
    )

    p.add_argument(
        "--i-understand-this-can-affect-real-hardware",
        action="store_true",
        help="Required safety acknowledgement to actually send commands.",
    )
    return p.parse_args()


def default_log_path(log_dir: str, *, port: str, addr: int) -> str:
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    port_safe = port.strip("/").replace("/", "_").replace(":", "_")
    return os.path.join(log_dir, f"ehl_unlock_{ts}_{port_safe}_addr{addr}.log")


def _tx(port, *, addr: int, cmd: int, data: bytes = b"", dry_run: bool, important: bool = True) -> bytes:
    frame = build_frame(addr, cmd, data, stx=STX_CONTROLLER, etx=ETX)
    # Always log full bytes to file (DEBUG). Console shows TX only for important steps.
    debug(f"TX CMD=0x{cmd:02X} ADDR={addr} bytes={hexdump(frame)}", enabled=True)
    if important:
        info(f"TX CMD=0x{cmd:02X} ADDR={addr}")
    if not dry_run:
        port.write(frame)
    return frame


def _rx_until(port, *, timeout_ms: int, on_frame, debug_enabled: bool) -> None:
    deadline = time.time() + (timeout_ms / 1000.0)
    buf = b""
    while time.time() < deadline:
        chunk = port.read(timeout_s=0.05)
        if not chunk:
            continue
        debug(f"RX+ {hexdump(chunk)}", enabled=debug_enabled)
        buf += chunk
        frames, buf = extract_frames(buf)
        for f in frames:
            on_frame(f)


def await_cmd_ok(
    port,
    *,
    addr: int,
    cmd: int,
    timeout_ms: int,
    debug_enabled: bool,
) -> bool:
    """
    VB6-style acceptance: receive a frame where CMD matches and first data byte is 0x1E.
    (VB6 code: Case 119/117, If x(4)=30 Then ...).
    """
    ok = False

    def on_frame(f):
        nonlocal ok
        debug(f"RX {describe_frame(f)}", enabled=debug_enabled)
        meaning = interpret_frame(f)
        if meaning:
            debug(f"RX meaning: {meaning}", enabled=debug_enabled)

        # Ignore echoed controller frames if adapter echoes TX.
        if f.stx != STX_DISPENSER:
            return
        if f.addr != (addr & 0xFF):
            return
        if f.cmd != (cmd & 0xFF):
            return
        if len(f.data) >= 1 and f.data[0] == OK_BYTE:
            info(f"ACK OK for CMD=0x{cmd:02X}: {describe_frame(f)}")
            ok = True
        else:
            info(f"ACK REJECT/UNKNOWN for CMD=0x{cmd:02X}: {describe_frame(f)}")

    _rx_until(port, timeout_ms=timeout_ms, on_frame=on_frame, debug_enabled=debug_enabled)
    return ok


def poll_cmd_once(
    port,
    *,
    addr: int,
    cmd: int,
    timeout_ms: int,
    debug_enabled: bool,
):
    """
    Send a poll command and return the first parsed frame matching (addr, cmd), or None.
    Logs all frames to the debug log.
    """
    got = None

    def on_frame(f):
        nonlocal got
        debug(f"RX {describe_frame(f)}", enabled=debug_enabled)
        meaning = interpret_frame(f)
        if meaning:
            debug(f"RX meaning: {meaning}", enabled=debug_enabled)
        # Ignore echoed controller frames if adapter echoes TX.
        if f.stx != STX_DISPENSER:
            return
        if got is None and f.addr == (addr & 0xFF) and f.cmd == (cmd & 0xFF):
            got = f

    _rx_until(port, timeout_ms=timeout_ms, on_frame=on_frame, debug_enabled=debug_enabled)
    return got


def main() -> int:
    args = parse_args()
    if not (1 <= args.addr <= 255):
        die("--addr must be 1..255")

    if not args.log_file:
        args.log_file = default_log_path(args.log_dir, port=args.port, addr=args.addr)

    console_level = "DEBUG" if args.console_verbosity == "chatty" else "INFO"
    init_logging(args.log_file, console_level=console_level, file_level="DEBUG")
    debug_enabled = True  # file is always chatty; console is controlled via console_level

    warn("This can affect real-world hardware state.")
    if args.dry_run:
        warn("Dry-run enabled; not writing to serial.")
    else:
        if not args.i_understand_this_can_affect_real_hardware:
            die("Refusing to send. Pass --i-understand-this-can-affect-real-hardware to proceed.")

    rs = Rs485Config(
        enabled=bool(args.rs485),
        delay_rts_before_send_ms=int(args.rts_before_ms),
        delay_rts_after_send_ms=int(args.rts_after_ms),
    )

    port, notes = open_serial(args.port, baud=args.baud, rs485=rs if args.rs485 else None)
    state_last = None
    err_last = None
    unblocked = False

    try:
        for n in notes:
            warn(n)
        info(f"Opened {args.port} @ {args.baud} 8N1. Target ADDR={args.addr} (0x{args.addr:02X}).")
        info(f"Logging to: {args.log_file}")

        # Baseline snapshot
        info("=== BASELINE (poll STATE/ERROR/VOLUME/TANKBIT) ===")
        for name, cmd in [("STATE", CMD_STATE), ("ERROR_QUERY", CMD_ERROR_QUERY), ("VOLUME", CMD_VOLUME), ("TANKBIT", CMD_TANKBIT)]:
            _tx(port, addr=args.addr, cmd=cmd, dry_run=args.dry_run, important=True)
            f = poll_cmd_once(port, addr=args.addr, cmd=cmd, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)
            if f is None:
                warn(f"{name}: no valid response frame within {args.timeout_ms}ms")
                continue
            if cmd == CMD_STATE and len(f.data) >= 1:
                s = interpret_state_byte(f.data[0])
                info(
                    f"STATE now: raw=0x{s['raw']:02X} bits={s['bits']} "
                    f"open_for_delivery={s['open_for_delivery']} startbutton_pressed={s['startbutton_pressed']} automode={s['automode']}"
                )
                state_last = s["raw"]
            elif cmd == CMD_ERROR_QUERY:
                e = interpret_error_query(f.data)
                if e.get("ok"):
                    msg_no = e.get("message_no")
                    msg_en = e.get("message_en")
                    if msg_no and msg_en and msg_en != msg_no:
                        info(f"ERROR now: main={e['main']} sub={e['sub']} NO='{msg_no}' EN='{msg_en}'")
                    elif msg_no:
                        info(f"ERROR now: main={e['main']} sub={e['sub']} '{msg_no}'")
                    else:
                        info(f"ERROR now: main={e['main']} sub={e['sub']} (unknown)")
                    err_last = (e["main"], e["sub"])
                else:
                    warn(f"ERROR_QUERY: {e}")
            else:
                info(f"{name}: RX {describe_frame(f)}")

        # UNBLOCK attempts
        info("=== UNBLOCK (strict VB6-style ACK required) ===")
        for attempt in range(1, max(1, args.retries) + 1):
            info(f"UNBLOCK attempt {attempt}/{args.retries} ...")
            _tx(port, addr=args.addr, cmd=CMD_UNBLOCK, dry_run=args.dry_run, important=True)
            if args.dry_run:
                unblocked = True
                break
            if await_cmd_ok(port, addr=args.addr, cmd=CMD_UNBLOCK, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled):
                unblocked = True
                break

            warn("UNBLOCK not ACKed per VB6 semantics.")
            if not args.disable_product_select_fallback:
                warn("Fallback: sending PRODUCT_SELECT (0xC3 data=0x30) then retrying UNBLOCK.")
                _tx(port, addr=args.addr, cmd=CMD_PRODUCT_SELECT, data=bytes([0x30]), dry_run=args.dry_run, important=True)
                poll_cmd_once(port, addr=args.addr, cmd=CMD_STATE, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)
            if not args.disable_reset_fallback:
                warn("Fallback: sending RESET (0x81) then retrying UNBLOCK.")
                _tx(port, addr=args.addr, cmd=CMD_RESET, dry_run=args.dry_run, important=True)
                poll_cmd_once(port, addr=args.addr, cmd=CMD_STATE, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)

        if not unblocked:
            warn("UNBLOCK never ACKed. Proceeding to BLOCK anyway (best-effort).")

        # Hold loop: keep it in this mode and watch state/error.
        info(f"=== HOLD {args.hold_seconds}s (poll STATE; log changes) ===")
        hold_deadline = time.time() + max(0, args.hold_seconds)
        next_console_summary = 0.0
        while time.time() < hold_deadline:
            _tx(port, addr=args.addr, cmd=CMD_STATE, dry_run=args.dry_run, important=False)
            f = poll_cmd_once(port, addr=args.addr, cmd=CMD_STATE, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)
            if f is not None and len(f.data) >= 1:
                sb = f.data[0] & 0xFF
                if state_last is None or sb != state_last:
                    s = interpret_state_byte(sb)
                    info(
                        f"STATE change: raw=0x{s['raw']:02X} bits={s['bits']} "
                        f"open_for_delivery={s['open_for_delivery']} startbutton_pressed={s['startbutton_pressed']} automode={s['automode']}"
                    )
                    state_last = sb

            _tx(port, addr=args.addr, cmd=CMD_ERROR_QUERY, dry_run=args.dry_run, important=False)
            eframe = poll_cmd_once(port, addr=args.addr, cmd=CMD_ERROR_QUERY, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)
            if eframe is not None:
                e = interpret_error_query(eframe.data)
                if e.get("ok"):
                    key = (e["main"], e["sub"])
                    if err_last is None or key != err_last:
                        msg_no = e.get("message_no")
                        msg_en = e.get("message_en")
                        if msg_no and msg_en and msg_en != msg_no:
                            info(f"ERROR change: main={e['main']} sub={e['sub']} NO='{msg_no}' EN='{msg_en}'")
                        elif msg_no:
                            info(f"ERROR change: main={e['main']} sub={e['sub']} '{msg_no}'")
                        else:
                            info(f"ERROR change: main={e['main']} sub={e['sub']} (unknown)")
                        err_last = key

            # Photo-friendly summary once per second
            now = time.time()
            if now >= next_console_summary:
                remaining = int(max(0, hold_deadline - now))
                if state_last is not None:
                    s = interpret_state_byte(state_last)
                    err_str = f"{err_last[0]}-{err_last[1]}" if err_last else "n/a"
                    info(
                        f"HOLD t_remaining={remaining}s open_for_delivery={s['open_for_delivery']} "
                        f"startbutton_pressed={s['startbutton_pressed']} automode={s['automode']} err={err_str}"
                    )
                else:
                    info(f"HOLD t_remaining={remaining}s (no STATE yet)")
                next_console_summary = now + 1.0

            time.sleep(max(0, args.poll_ms) / 1000.0)

        # BLOCK
        info("=== BLOCK ===")
        _tx(port, addr=args.addr, cmd=CMD_BLOCK, dry_run=args.dry_run, important=True)
        if not args.dry_run:
            if not await_cmd_ok(port, addr=args.addr, cmd=CMD_BLOCK, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled):
                warn("No VB6-style BLOCK ACK seen (continuing to verify via STATE).")

        # Verify open_for_delivery clears
        info(f"=== VERIFY (wait up to {args.verify_ms}ms for open_for_delivery to clear) ===")
        deadline = time.time() + (max(0, args.verify_ms) / 1000.0)
        cleared = False
        while time.time() < deadline:
            _tx(port, addr=args.addr, cmd=CMD_STATE, dry_run=args.dry_run, important=False)
            f = poll_cmd_once(port, addr=args.addr, cmd=CMD_STATE, timeout_ms=args.timeout_ms, debug_enabled=debug_enabled)
            if f is not None and len(f.data) >= 1:
                s = interpret_state_byte(f.data[0])
                if not s["open_for_delivery"]:
                    info(f"VERIFY OK: open_for_delivery is now false (STATE raw=0x{s['raw']:02X} bits={s['bits']})")
                    cleared = True
                    break
            time.sleep(0.2)

        if not cleared:
            warn("VERIFY WARN: open_for_delivery did not clear within verify window (check log + physical state).")
            return 2

        info("DONE.")
        return 0
    finally:
        port.close()


if __name__ == "__main__":
    raise SystemExit(main())

