from __future__ import annotations

import datetime as _dt
import os
import sys
from dataclasses import dataclass
from typing import Optional


def ts() -> str:
    return _dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


_LEVELS = {"DEBUG": 10, "INFO": 20, "WARN": 30, "ERROR": 40}


def _level_value(level: str) -> int:
    return _LEVELS.get(level.upper(), 20)


@dataclass
class _LoggerConfig:
    log_file: Optional[str] = None
    also_stdout: bool = True
    console_level: str = "INFO"
    file_level: str = "DEBUG"


_cfg = _LoggerConfig()
_fh: Optional[object] = None  # text file handle


def init_logging(
    log_file: Optional[str],
    *,
    also_stdout: bool = True,
    console_level: str = "INFO",
    file_level: str = "DEBUG",
) -> Optional[str]:
    """
    Minimal logger (no deps).

    - If log_file is None: stdout-only.
    - console_level controls what appears on stdout.
    - file_level controls what is written to the log file.
    """
    global _fh, _cfg
    _cfg = _LoggerConfig(
        log_file=log_file,
        also_stdout=also_stdout,
        console_level=console_level.upper(),
        file_level=file_level.upper(),
    )

    if _fh is not None:
        try:
            _fh.close()
        except Exception:
            pass
        _fh = None

    if not log_file:
        return None

    os.makedirs(os.path.dirname(os.path.abspath(log_file)) or ".", exist_ok=True)
    _fh = open(log_file, "a", encoding="utf-8")
    _fh.write(f"{ts()} [INFO] log_start pid={os.getpid()} argv={' '.join(sys.argv)}\n")
    _fh.flush()
    return log_file


def _emit(level: str, msg: str) -> None:
    line = f"{ts()} [{level}] {msg}\n"
    lvl = _level_value(level)

    # file sink
    if _fh is not None and lvl >= _level_value(_cfg.file_level):
        try:
            _fh.write(line)
            _fh.flush()
        except Exception:
            pass

    # console sink
    if _cfg.also_stdout and lvl >= _level_value(_cfg.console_level):
        sys.stdout.write(line)
        sys.stdout.flush()


def info(msg: str) -> None:
    _emit("INFO", msg)


def warn(msg: str) -> None:
    _emit("WARN", msg)


def error(msg: str) -> None:
    _emit("ERROR", msg)


def debug(msg: str, enabled: bool = True) -> None:
    if enabled:
        _emit("DEBUG", msg)


def die(msg: str, code: int = 2) -> None:
    error(msg)
    raise SystemExit(code)

