from __future__ import annotations

import datetime as _dt
import sys
from typing import Any


def ts() -> str:
    return _dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def _log(level: str, msg: str) -> None:
    sys.stdout.write(f"{ts()} [{level}] {msg}\n")
    sys.stdout.flush()


def info(msg: str) -> None:
    _log("INFO", msg)


def warn(msg: str) -> None:
    _log("WARN", msg)


def error(msg: str) -> None:
    _log("ERROR", msg)


def debug(msg: str, enabled: bool) -> None:
    if enabled:
        _log("DEBUG", msg)


def die(msg: str, code: int = 2) -> None:
    error(msg)
    raise SystemExit(code)

