#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import socket
import ssl
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
if THIS_DIR not in sys.path:
    sys.path.insert(0, THIS_DIR)

from pt_hex import hexdump, parse_hex  # type: ignore
from pt_logging import debug, die, info, init_logging, warn  # type: ignore


def _safe_int(x: object, default: int) -> int:
    try:
        return int(x)  # type: ignore[arg-type]
    except Exception:
        return default


def _safe_bool(x: object, default: bool) -> bool:
    if isinstance(x, bool):
        return x
    if isinstance(x, str):
        v = x.strip().lower()
        if v in ("1", "true", "yes", "y", "on"):
            return True
        if v in ("0", "false", "no", "n", "off"):
            return False
    return default


def _parse_hex(s: str) -> bytes:
    """
    Accepts:
      - "01 02 0A"
      - "01020A"
      - "0x01,0x02,0x0A"
    """
    try:
        return parse_hex(s)
    except Exception as e:
        raise ValueError(f"invalid hex string: {s!r}") from e


def default_log_path(*, host: str, port: int) -> str:
    log_dir = os.path.join(THIS_DIR, "logs")
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    host_safe = host.replace("/", "_").replace(":", "_")
    return os.path.join(log_dir, f"pt_tcp_{ts}_{host_safe}_{int(port)}.log")


@dataclass(frozen=True)
class TcpTlsConfig:
    enabled: bool
    server_name: str
    insecure_skip_verify: bool


@dataclass(frozen=True)
class TcpConfig:
    host: str
    port: int
    connect_timeout_ms: int
    read_timeout_ms: int
    tls: TcpTlsConfig


@dataclass(frozen=True)
class LoggingConfig:
    console_level: str
    file_level: str


@dataclass(frozen=True)
class Config:
    tcp: TcpConfig
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

    tcp_raw = raw.get("tcp", {}) or {}
    tls_raw = (tcp_raw.get("tls", {}) or {}) if isinstance(tcp_raw.get("tls", {}), dict) else {}
    log_raw = raw.get("logging", {}) or {}

    tls = TcpTlsConfig(
        enabled=_safe_bool(tls_raw.get("enabled", False), False),
        server_name=str(tls_raw.get("server_name", "") or ""),
        insecure_skip_verify=_safe_bool(tls_raw.get("insecure_skip_verify", False), False),
    )
    tcp = TcpConfig(
        host=str(tcp_raw.get("host", "") or "").strip(),
        port=_safe_int(tcp_raw.get("port", 0), 0),
        connect_timeout_ms=_safe_int(tcp_raw.get("connect_timeout_ms", 2500), 2500),
        read_timeout_ms=_safe_int(tcp_raw.get("read_timeout_ms", 3000), 3000),
        tls=tls,
    )
    logging = LoggingConfig(
        console_level=str(log_raw.get("console_level", "INFO") or "INFO").upper(),
        file_level=str(log_raw.get("file_level", "DEBUG") or "DEBUG").upper(),
    )

    if not tcp.host:
        die("config.json: tcp.host is required")
    if not (1 <= tcp.port <= 65535):
        die(f"config.json: tcp.port must be 1..65535 (got {tcp.port})")

    return Config(tcp=tcp, logging=logging)


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Payment terminal TCP capture tool (connect, optionally send bytes, capture reply)."
    )
    p.add_argument("--host", help="Override config tcp.host")
    p.add_argument("--port", type=int, help="Override config tcp.port")
    p.add_argument("--tls", action="store_true", help="Force TLS on (even if config disables)")
    p.add_argument("--no-tls", action="store_true", help="Force TLS off (even if config enables)")
    p.add_argument("--sni", default=None, help="TLS SNI/server_name override (empty disables)")
    p.add_argument("--insecure-skip-verify", action="store_true", help="Disable TLS certificate verification")
    p.add_argument("--connect-timeout-ms", type=int, help="Connect timeout (ms)")
    p.add_argument("--read-timeout-ms", type=int, help="Read timeout per recv (ms)")
    p.add_argument("--read-seconds", type=float, default=3.0, help="How long to read after connect/send (seconds)")
    p.add_argument("--send-hex", default=None, help='Hex bytes to send, e.g. "02 30 31 03"')
    p.add_argument("--send-ascii", default=None, help='ASCII text to send (encoded as UTF-8, no newline added)')
    p.add_argument("--log-file", help="Write logs to this file (also writes to stdout).")
    p.add_argument("--debug", action="store_true", help="More verbose logging")
    return p.parse_args()


def _wrap_tls(sock: socket.socket, cfg: TcpTlsConfig, *, sni_override: Optional[str], insecure: bool) -> socket.socket:
    ctx = ssl.create_default_context()
    if insecure or cfg.insecure_skip_verify:
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

    server_name = cfg.server_name
    if sni_override is not None:
        server_name = sni_override
    server_name = server_name.strip()
    if not server_name:
        server_name = None  # type: ignore[assignment]

    return ctx.wrap_socket(sock, server_hostname=server_name)  # type: ignore[arg-type]


def main() -> int:
    cfg = load_config()
    args = parse_args()

    host = (args.host or cfg.tcp.host).strip()
    port = int(args.port or cfg.tcp.port)
    connect_timeout_ms = int(args.connect_timeout_ms or cfg.tcp.connect_timeout_ms)
    read_timeout_ms = int(args.read_timeout_ms or cfg.tcp.read_timeout_ms)

    tls_enabled = bool(cfg.tcp.tls.enabled)
    if args.tls:
        tls_enabled = True
    if args.no_tls:
        tls_enabled = False

    if not args.log_file:
        args.log_file = default_log_path(host=host, port=port)
    init_logging(args.log_file, console_level=cfg.logging.console_level, file_level=cfg.logging.file_level)

    info(f"Target: {host}:{port} tls={tls_enabled}")
    info(f"Timeouts: connect={connect_timeout_ms}ms read={read_timeout_ms}ms")

    send_payload = b""
    if args.send_hex and args.send_ascii:
        die("Choose only one of --send-hex or --send-ascii")
    if args.send_hex:
        send_payload = _parse_hex(args.send_hex)
    elif args.send_ascii:
        send_payload = args.send_ascii.encode("utf-8")

    sock: Optional[socket.socket] = None
    try:
        sock = socket.create_connection((host, port), timeout=connect_timeout_ms / 1000.0)
        sock.settimeout(read_timeout_ms / 1000.0)
        info("Connected.")

        if tls_enabled:
            sock = _wrap_tls(sock, cfg.tcp.tls, sni_override=args.sni, insecure=bool(args.insecure_skip_verify))
            sock.settimeout(read_timeout_ms / 1000.0)
            if isinstance(sock, ssl.SSLSocket):
                try:
                    info(f"TLS: version={sock.version()} cipher={sock.cipher()}")
                    cert = sock.getpeercert()
                    if cert:
                        debug(f"TLS: peer_cert={cert}", enabled=bool(args.debug))
                    else:
                        warn("TLS: no peer certificate provided (or verification disabled).")
                except Exception as e:
                    warn(f"TLS: could not fetch peer details: {e}")

        if send_payload:
            info(f"TX {hexdump(send_payload)}")
            sock.sendall(send_payload)

        deadline = time.time() + max(0.0, float(args.read_seconds))
        buf_total = b""
        while time.time() < deadline:
            try:
                chunk = sock.recv(4096)
            except socket.timeout:
                continue
            if not chunk:
                warn("RX <socket closed by peer>")
                break
            buf_total += chunk
            info(f"RX+ {hexdump(chunk)}")

        if buf_total:
            debug(f"RX total ({len(buf_total)} bytes): {hexdump(buf_total)}", enabled=True)
        else:
            warn("RX <none>")

        return 0
    except Exception as e:
        die(f"TCP capture failed: {e}", code=1)
    finally:
        try:
            if sock is not None:
                sock.close()
        except Exception:
            pass


if __name__ == "__main__":
    raise SystemExit(main())

