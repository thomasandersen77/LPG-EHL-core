#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import socket
import sys
import threading
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
if THIS_DIR not in sys.path:
    sys.path.insert(0, THIS_DIR)

try:
    from pt_hex import hexdump, parse_hex, decode_escapes  # type: ignore
except Exception:
    # Fallback: allow running as a single file copied to a field machine.
    def hexdump(b: bytes) -> str:  # type: ignore[misc]
        return " ".join(f"{x:02X}" for x in b)

    def parse_hex(s: str) -> bytes:  # type: ignore[misc]
        raw = s.strip().replace(",", " ").replace("0x", " ").replace("0X", " ")
        raw = "".join(ch if ch in "0123456789abcdefABCDEF " else " " for ch in raw)
        parts = [p for p in raw.split() if p]
        if len(parts) == 1 and len(parts[0]) % 2 == 0:
            parts = [parts[0][i : i + 2] for i in range(0, len(parts[0]), 2)]
        return bytes(int(p, 16) for p in parts)

    def decode_escapes(s: str) -> bytes:  # type: ignore[misc]
        try:
            return s.encode("utf-8").decode("unicode_escape").encode("latin-1")
        except Exception:
            return s.encode("utf-8")

try:
    from pt_logging import debug, die, info, init_logging, warn  # type: ignore
except Exception:
    # Fallback: minimal stdout logger.
    import datetime as _dt

    def _ts() -> str:
        return _dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def init_logging(  # type: ignore[misc]
        log_file: Optional[str],
        *,
        also_stdout: bool = True,
        console_level: str = "INFO",
        file_level: str = "DEBUG",
    ) -> Optional[str]:
        # no-op in fallback
        return log_file

    def info(msg: str) -> None:  # type: ignore[misc]
        sys.stdout.write(f"{_ts()} [INFO] {msg}\n")
        sys.stdout.flush()

    def warn(msg: str) -> None:  # type: ignore[misc]
        sys.stdout.write(f"{_ts()} [WARN] {msg}\n")
        sys.stdout.flush()

    def debug(msg: str, enabled: bool = True) -> None:  # type: ignore[misc]
        if enabled:
            sys.stdout.write(f"{_ts()} [DEBUG] {msg}\n")
            sys.stdout.flush()

    def die(msg: str, code: int = 2) -> None:  # type: ignore[misc]
        sys.stdout.write(f"{_ts()} [ERROR] {msg}\n")
        sys.stdout.flush()
        raise SystemExit(code)

from baxi_codec import (  # type: ignore
    RS,
    US,
    ascii_preview,
    build_send_data_json,
    split_by_rs,
    try_decode,
)


def _safe_int(x: object, default: int) -> int:
    try:
        return int(x)  # type: ignore[arg-type]
    except Exception:
        return default


def default_log_path(*, port: int) -> str:
    log_dir = os.path.join(THIS_DIR, "logs")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    return os.path.join(log_dir, f"baxi_listener_{ts}_port{int(port)}.log")


@dataclass(frozen=True)
class ListenerConfig:
    bind_host: str
    bind_port: int
    accept_timeout_ms: int


@dataclass(frozen=True)
class LoggingConfig:
    console_level: str
    file_level: str


@dataclass(frozen=True)
class Config:
    listener: ListenerConfig
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

    lst_raw = raw.get("baxi_listener", {}) or {}
    log_raw = raw.get("logging", {}) or {}

    listener = ListenerConfig(
        bind_host=str(lst_raw.get("bind_host", "0.0.0.0") or "0.0.0.0").strip(),
        bind_port=_safe_int(lst_raw.get("bind_port", 6001), 6001),
        accept_timeout_ms=_safe_int(lst_raw.get("accept_timeout_ms", 1000), 1000),
    )
    logging = LoggingConfig(
        console_level=str(log_raw.get("console_level", "INFO") or "INFO").upper(),
        file_level=str(log_raw.get("file_level", "DEBUG") or "DEBUG").upper(),
    )

    if not (1 <= listener.bind_port <= 65535):
        die(f"config.json: baxi_listener.bind_port must be 1..65535 (got {listener.bind_port})")

    return Config(listener=listener, logging=logging)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="BAXI-over-Ethernet: listen for terminal connection (default port 6001) and interact via REPL."
    )
    p.add_argument("--bind-host", help="Override config baxi_listener.bind_host")
    p.add_argument("--bind-port", type=int, help="Override config baxi_listener.bind_port")
    p.add_argument("--log-file", help="Write logs to this file (also writes to stdout).")
    p.add_argument("--debug", action="store_true", help="More verbose logging")
    return p.parse_args()


class _Session:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.conn: Optional[socket.socket] = None
        self.peer: str = ""
        self.alive = True

    def set_conn(self, conn: Optional[socket.socket], peer: str) -> None:
        with self._lock:
            self.conn = conn
            self.peer = peer

    def get_conn(self) -> tuple[Optional[socket.socket], str]:
        with self._lock:
            return self.conn, self.peer


def _rx_thread(sess: _Session, *, debug_enabled: bool) -> None:
    buf = b""
    while sess.alive:
        conn, peer = sess.get_conn()
        if conn is None:
            time.sleep(0.1)
            continue
        try:
            chunk = conn.recv(4096)
        except socket.timeout:
            continue
        except Exception as e:
            warn(f"RX error from {peer}: {e}")
            sess.set_conn(None, "")
            continue
        if not chunk:
            warn(f"RX <peer closed> {peer}")
            sess.set_conn(None, "")
            continue

        debug(f"RX+ raw {hexdump(chunk)}", enabled=debug_enabled)
        buf += chunk

        records, buf = split_by_rs(buf)
        for rec in records:
            info(f"RX {peer} {hexdump(rec)}")
            info(f"RX ascii {ascii_preview(rec)}")
            dec = try_decode(rec)
            if dec is not None:
                info(f"RX decoded kind={dec.kind} {dec.details}")


def _send(conn: socket.socket, payload: bytes) -> None:
    conn.sendall(payload)


def _parse_hex(s: str) -> bytes:
    return parse_hex(s)


def _help() -> None:
    info("Commands:")
    info("  help")
    info("  status               (prints current connection)")
    info("  hex <bytes>          (send raw bytes, e.g. hex 49 32 31 7b 7d 1e)")
    info(r"  ascii <text>         (send UTF-8, supports \xNN, \r, \n, \t)")
    info("  jsonreq <json>       (send SEND DATA JSON request: 0x49 0x32 0x31 + json + RS)")
    info("  jsonresp <json>      (send SEND DATA JSON response: 0x49 0x32 0x32 + json + RS)")
    info("  sendus               (send US byte 0x1F)")
    info("  sendrs               (send RS byte 0x1E)")
    info("  quit")


def _decode_escapes(s: str) -> bytes:
    return decode_escapes(s)


def main() -> int:
    cfg = load_config()
    args = parse_args()

    bind_host = (args.bind_host or cfg.listener.bind_host).strip()
    bind_port = int(args.bind_port or cfg.listener.bind_port)

    if not args.log_file:
        args.log_file = default_log_path(port=bind_port)
    init_logging(args.log_file, console_level=cfg.logging.console_level, file_level=cfg.logging.file_level)

    sess = _Session()
    rx = threading.Thread(target=_rx_thread, args=(sess,), kwargs={"debug_enabled": bool(args.debug)}, daemon=True)
    rx.start()

    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        srv.bind((bind_host, bind_port))
    except Exception as e:
        die(
            f"Bind failed on {bind_host}:{bind_port}: {e}\n"
            "If this is Linux and you're using 6001, ensure firewall allows inbound TCP and terminal is configured with the ARK machine IP."
        )
    srv.listen(1)
    srv.settimeout(max(0.2, cfg.listener.accept_timeout_ms / 1000.0))
    info(f"Listening on {bind_host}:{bind_port} (terminal should connect as client).")
    _help()

    def accept_once() -> None:
        try:
            conn, addr = srv.accept()
        except socket.timeout:
            return
        peer = f"{addr[0]}:{addr[1]}"
        info(f"Accepted connection from {peer}")
        try:
            conn.settimeout(0.5)
            conn.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except Exception:
            pass
        # Replace existing connection
        old, old_peer = sess.get_conn()
        if old is not None:
            warn(f"Closing previous connection {old_peer}")
            try:
                old.close()
            except Exception:
                pass
        sess.set_conn(conn, peer)

    try:
        while True:
            accept_once()

            line = sys.stdin.readline()
            if not line:
                time.sleep(0.1)
                continue
            line = line.strip()
            if not line:
                continue

            cmd, *rest = line.split(" ", 1)
            arg = rest[0] if rest else ""

            if cmd in ("help", "?"):
                _help()
                continue
            if cmd == "quit":
                break
            if cmd == "status":
                c, peer = sess.get_conn()
                info(f"Connected: {bool(c)} peer={peer!r}")
                continue

            conn, peer = sess.get_conn()
            if conn is None:
                warn("No terminal connected yet (waiting on accept).")
                continue

            if cmd == "hex":
                try:
                    payload = _parse_hex(arg)
                except Exception as e:
                    warn(f"Bad hex: {e}")
                    continue
                info(f"TX {peer} {hexdump(payload)}")
                _send(conn, payload)
                continue

            if cmd == "ascii":
                payload = _decode_escapes(arg)
                info(f"TX {peer} {hexdump(payload)}")
                info(f"TX ascii {ascii_preview(payload)}")
                _send(conn, payload)
                continue

            if cmd == "jsonreq":
                try:
                    obj = json.loads(arg)
                except Exception as e:
                    warn(f"Bad JSON: {e}")
                    continue
                payload = build_send_data_json(obj, is_response=False)
                info(f"TX {peer} {hexdump(payload)}")
                _send(conn, payload)
                continue

            if cmd == "jsonresp":
                try:
                    obj = json.loads(arg)
                except Exception as e:
                    warn(f"Bad JSON: {e}")
                    continue
                payload = build_send_data_json(obj, is_response=True)
                info(f"TX {peer} {hexdump(payload)}")
                _send(conn, payload)
                continue

            if cmd == "sendus":
                payload = bytes([US])
                info(f"TX {peer} {hexdump(payload)}")
                _send(conn, payload)
                continue

            if cmd == "sendrs":
                payload = bytes([RS])
                info(f"TX {peer} {hexdump(payload)}")
                _send(conn, payload)
                continue

            warn(f"Unknown command: {cmd!r} (type 'help')")

        return 0
    finally:
        sess.alive = False
        try:
            c, _ = sess.get_conn()
            if c is not None:
                c.close()
        except Exception:
            pass
        try:
            srv.close()
        except Exception:
            pass


if __name__ == "__main__":
    raise SystemExit(main())

