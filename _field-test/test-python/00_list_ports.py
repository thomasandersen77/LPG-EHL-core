#!/usr/bin/env python3
from __future__ import annotations

import argparse
import glob
import os
from pathlib import Path

from logging_utils import info, init_logging


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="List likely serial device paths under /dev.")
    p.add_argument("--log-file", help="Also write logs to this file (in addition to stdout).")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    init_logging(args.log_file, console_level="INFO", file_level="DEBUG")
    info("Listing likely serial device paths...")
    candidates = []

    candidates += sorted(glob.glob("/dev/ttyUSB*"))
    candidates += sorted(glob.glob("/dev/ttyACM*"))
    candidates += sorted(glob.glob("/dev/ttyS*"))

    by_id = Path("/dev/serial/by-id")
    if by_id.exists():
        for p in sorted(by_id.iterdir()):
            try:
                candidates.append(str(p))
            except Exception:
                pass

    if not candidates:
        info("No obvious serial devices found under /dev. Check adapter/drivers and permissions.")
        return 1

    seen = set()
    for p in candidates:
        if p in seen:
            continue
        seen.add(p)
        exists = os.path.exists(p)
        access = "rw" if os.access(p, os.R_OK | os.W_OK) else ("r" if os.access(p, os.R_OK) else "no")
        info(f"- {p} (exists={exists}, access={access})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

