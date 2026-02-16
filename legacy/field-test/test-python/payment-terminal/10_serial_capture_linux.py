#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
if THIS_DIR not in sys.path:
    sys.path.insert(0, THIS_DIR)

from pt_hex import hexdump  # type: ignore
from pt_logging import debug, die, info, init_logging, warn  # type: ignore
from serial_linux import open_serial  # type: ignore


def _safe_int(x: object, default: int) -> int:
    try:
        return int(x)  # type: ignore[arg-type]
    except Exception:
        return default


def default_log_path(*, port: str) -> str:
    log_dir = os.path.join(THIS_DIR, "logs")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    port_safe = port.strip("/").replace("/", "_").replace(":", "_")
    return os.path.join(log_dir, f"pt_serial_{ts}_{port_safe}.log")


@dataclass(frozen=True)
class SerialConfig:
    port: str
    baud: int


@dataclass(frozen=True)
class LoggingConfig:
    console_level: str
    file_level: str


@dataclass(frozen=True)
class Config:
    serial: SerialConfig
    logging: LoggingConfig


def load_config() -> Config:
    path = os.path.join(THIS_DIR, "config.json")
    if not os.path.exists(path):
        die(
            "Missing config.json. Create it from config.example.json:\n"
            "  cp python-test/payment-terminal/config.example.json python-test/payment-terminal/config.json"
        )
    with open(path, "r", encoding="utf-8") as f:
        raw = json.load(f)

    serial_raw = raw.get("serial_linux", {}) or {}
    log_raw = raw.get("logging", {}) or {}

    serial = SerialConfig(
        port=str(serial_raw.get("port", "") or "").strip(),
        baud=_safe_int(serial_raw.get("baud", 9600), 9600),
    )
    logging = LoggingConfig(
        console_level=str(log_raw.get("console_level", "INFO") or "INFO").upper(),
        file_level=str(log_raw.get("file_level", "DEBUG") or "DEBUG").upper(),
    )

    if not serial.port:
        die("config.json: serial_linux.port is required")
    return Config(serial=serial, logging=logging)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Linux-only serial capture (read bytes, log hex).")
    p.add_argument("--port", help="Override config serial_linux.port (e.g. /dev/ttyS3)")
    p.add_argument("--baud", type=int, help="Override config serial_linux.baud")
    p.add_argument("--duration-s", type=float, default=30.0, help="How long to capture (seconds)")
    p.add_argument("--log-file", help="Write logs to this file (also writes to stdout).")
    p.add_argument("--debug", action="store_true", help="More verbose logging")
    return p.parse_args()


def main() -> int:
    if sys.platform != "linux":
        die(f"This script is Linux-only (sys.platform={sys.platform!r}). Use 00_tcp_capture.py for network tests.")

    cfg = load_config()
    args = parse_args()

    port_path = (args.port or cfg.serial.port).strip()
    baud = int(args.baud or cfg.serial.baud)

    if not args.log_file:
        args.log_file = default_log_path(port=port_path)
    init_logging(args.log_file, console_level=cfg.logging.console_level, file_level=cfg.logging.file_level)

    info(f"Opening {port_path} @ {baud} 8N1 (raw, non-blocking).")
    port, notes = open_serial(port_path, baud=baud, rs485=None)
    try:
        for n in notes:
            warn(n)

        deadline = time.time() + max(0.0, float(args.duration_s))
        buf_total = b""
        while time.time() < deadline:
            chunk = port.read(timeout_s=0.2)
            if not chunk:
                continue
            buf_total += chunk
            info(f"RX+ {hexdump(chunk)}")

        if buf_total:
            debug(f"RX total ({len(buf_total)} bytes): {hexdump(buf_total)}", enabled=True)
        else:
            warn("RX <none>")
        return 0
    finally:
        try:
            port.close()
        except Exception:
            pass


if __name__ == "__main__":
    raise SystemExit(main())

