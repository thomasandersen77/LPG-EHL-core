#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import sys
import threading
import time
import traceback
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Callable, Dict, List, Optional, Tuple
from urllib.parse import urlparse

# Allow importing helpers from python-test/ (parent folder)
THIS_DIR = os.path.dirname(os.path.abspath(__file__))
PYTHON_TEST_DIR = os.path.abspath(os.path.join(THIS_DIR, ".."))
if PYTHON_TEST_DIR not in sys.path:
    sys.path.insert(0, PYTHON_TEST_DIR)

from ehl_protocol import (  # type: ignore
    ETX,
    STX_CONTROLLER,
    STX_DISPENSER,
    EhlFrame,
    build_frame,
    describe_frame,
    extract_frames,
    hexdump,
    interpret_error_query,
    interpret_frame,
    interpret_price_bytes,
    interpret_state_byte,
    interpret_volume_bytes,
)
from logging_utils import debug, die, error, info, init_logging, warn  # type: ignore
from serial_linux import Rs485Config, open_serial, sleep_ms  # type: ignore


CMD_LINETEST = 0x6A
CMD_STATE = 0x4B
CMD_VOLUME = 0x45
CMD_PRICE = 0x5C
CMD_ERROR_QUERY = 0x4C
CMD_TANK = 0xC5

CMD_PRODUCT_SELECT = 0xC3
CMD_PROG_PRC = 0xA9
CMD_UNBLOCK = 0x77
CMD_BLOCK = 0x69
CMD_RESET = 0x81


def _now_iso() -> str:
    # log timestamps are handled by logging_utils; this is for JSON responses
    return time.strftime("%Y-%m-%dT%H:%M:%S%z")


def _json_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, indent=2, sort_keys=True) + "\n").encode("utf-8")


def _safe_int(x: Any, default: int) -> int:
    try:
        return int(x)
    except Exception:
        return default


def _encode_price_payload(price: str) -> bytes:
    """
    VB6-compatible PROG_PRC payload.
    price: "XX.XX" -> "XXXX" digits -> LSB-first ASCII bytes.
    Example: "15.90" -> "1590" -> bytes ['0','9','5','1'].
    """
    p = price.strip()
    if len(p) != 5 or p[2] != ".":
        raise ValueError("price must be in format XX.XX (e.g. 15.90)")
    digits = p.replace(".", "")
    if len(digits) != 4 or not digits.isdigit():
        raise ValueError("price must be in format XX.XX (digits only)")
    return bytes([ord(digits[3]), ord(digits[2]), ord(digits[1]), ord(digits[0])])


@dataclass(frozen=True)
class Config:
    listen_host: str
    listen_port: int
    serial_port: str
    baud: int
    addr: int
    timeout_ms: int
    inter_command_delay_ms: int
    rs485: Rs485Config
    product_select_byte: int
    default_price: str
    verify_timeout_ms: int
    verify_interval_ms: int
    console_level: str
    file_level: str


def load_config() -> Config:
    path = os.path.join(THIS_DIR, "config.json")
    with open(path, "r", encoding="utf-8") as f:
        raw = json.load(f)

    server = raw.get("server", {}) or {}
    serial = raw.get("serial", {}) or {}
    defaults = raw.get("defaults", {}) or {}
    logging = raw.get("logging", {}) or {}
    rs485_raw = (serial.get("rs485", {}) or {}) if isinstance(serial.get("rs485", {}), dict) else {}

    rs485 = Rs485Config(
        enabled=bool(rs485_raw.get("enabled", False)),
        delay_rts_before_send_ms=_safe_int(rs485_raw.get("rts_before_ms", 0), 0),
        delay_rts_after_send_ms=_safe_int(rs485_raw.get("rts_after_ms", 0), 0),
    )

    return Config(
        listen_host=str(server.get("listen_host", "0.0.0.0")),
        listen_port=_safe_int(server.get("listen_port", 8080), 8080),
        serial_port=str(serial.get("port", "/dev/ttyS3")),
        baud=_safe_int(serial.get("baud", 9600), 9600),
        addr=_safe_int(serial.get("addr", 33), 33),
        timeout_ms=_safe_int(serial.get("timeout_ms", 1200), 1200),
        inter_command_delay_ms=_safe_int(serial.get("inter_command_delay_ms", 120), 120),
        rs485=rs485,
        product_select_byte=_safe_int(defaults.get("product_select_byte", 0x30), 0x30) & 0xFF,
        default_price=str(defaults.get("price", "15.90")),
        verify_timeout_ms=_safe_int(defaults.get("verify_timeout_ms", 2500), 2500),
        verify_interval_ms=_safe_int(defaults.get("verify_interval_ms", 200), 200),
        console_level=str(logging.get("console_level", "INFO")),
        file_level=str(logging.get("file_level", "DEBUG")),
    )


class EhlBus:
    """
    Single shared RS-485 bus session:
    - single-flight access enforced by a lock
    - maintains a remainder buffer across calls
    """

    def __init__(self, cfg: Config):
        self.cfg = cfg
        self._lock = threading.Lock()
        self._port = None
        self._remainder = b""
        self._notes: List[str] = []

    def open(self) -> None:
        if self._port is not None:
            return
        port, notes = open_serial(self.cfg.serial_port, baud=self.cfg.baud, rs485=self.cfg.rs485)
        self._port = port
        self._notes = list(notes)
        for n in self._notes:
            info(f"serial_note: {n}")
        info(f"serial_open: path={self.cfg.serial_port} baud={self.cfg.baud} addr={self.cfg.addr}")

    def close(self) -> None:
        if self._port is not None:
            try:
                self._port.close()
            finally:
                self._port = None

    def _read_frames_until(
        self,
        *,
        deadline_s: float,
        predicate: Callable[[EhlFrame], bool],
        debug_frames: bool,
    ) -> Tuple[Optional[EhlFrame], List[EhlFrame]]:
        """
        Read chunks until predicate(frame) is True, or timeout.
        Returns (matched_frame_or_none, all_frames_seen).
        """
        assert self._port is not None
        buf = self._remainder
        seen: List[EhlFrame] = []

        while time.time() < deadline_s:
            chunk = self._port.read(timeout_s=0.05)
            if chunk:
                debug(f"RX+ {hexdump(chunk)}", enabled=debug_frames)
                buf += chunk

            frames, buf = extract_frames(buf)
            for f in frames:
                # Ignore TX echo or non-dispenser frames for server semantics.
                if f.stx != STX_DISPENSER:
                    debug(f"RX <ignored non-dispenser frame> {describe_frame(f)}", enabled=debug_frames)
                    continue
                seen.append(f)
                debug(f"RX {describe_frame(f)}", enabled=debug_frames)
                if predicate(f):
                    self._remainder = buf
                    return f, seen

        self._remainder = buf
        return None, seen

    def exchange(
        self,
        *,
        cmd: int,
        data: bytes = b"",
        expect_cmd: Optional[int] = None,
        timeout_ms: Optional[int] = None,
        debug_frames: bool = True,
    ) -> Dict[str, Any]:
        """
        Send a command and wait for a response frame.
        We prefer a response matching (addr, expect_cmd), but will also return any frames seen.
        """
        with self._lock:
            self.open()
            assert self._port is not None

            sleep_ms(self.cfg.inter_command_delay_ms)

            addr = self.cfg.addr & 0xFF
            tx = build_frame(addr, cmd & 0xFF, data, stx=STX_CONTROLLER, etx=ETX)
            info(f"HTTP_TX addr={addr} cmd=0x{cmd & 0xFF:02X} data={hexdump(data)} frame={hexdump(tx)}")
            self._port.write(tx)

            want_cmd = (expect_cmd if expect_cmd is not None else cmd) & 0xFF
            deadline_s = time.time() + ((timeout_ms or self.cfg.timeout_ms) / 1000.0)

            def pred(f: EhlFrame) -> bool:
                return f.addr == addr and f.cmd == want_cmd

            matched, seen = self._read_frames_until(deadline_s=deadline_s, predicate=pred, debug_frames=debug_frames)

            def frame_obj(f: EhlFrame) -> Dict[str, Any]:
                interp = interpret_frame(f)
                return {
                    "stx": f"0x{f.stx:02X}",
                    "len": f.length,
                    "addr": f.addr,
                    "cmd": f"0x{f.cmd:02X}",
                    "cmd_name": f.cmd_name,
                    "data_hex": hexdump(f.data) if f.data else "",
                    "checksum": f"0x{f.checksum:02X}",
                    "etx": f"0x{f.etx:02X}",
                    "interpretation": interp,
                }

            return {
                "ok": matched is not None,
                "tx": {
                    "addr": addr,
                    "cmd": f"0x{cmd & 0xFF:02X}",
                    "data_hex": hexdump(data) if data else "",
                    "frame_hex": hexdump(tx),
                },
                "rx_matched": frame_obj(matched) if matched is not None else None,
                "rx_seen": [frame_obj(f) for f in seen],
            }

    def poll_state(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_STATE, expect_cmd=CMD_STATE, debug_frames=False)
        f = r.get("rx_matched")
        if f and f.get("interpretation") is None:
            # fall through; python helper already interprets in interpret_frame if possible
            pass
        if r.get("ok") and f and f.get("data_hex"):
            # decode state byte for structured response
            try:
                raw = int(f["data_hex"].split()[0], 16)
                s = interpret_state_byte(raw)
                r["state"] = s
            except Exception:
                pass
        return r

    def poll_volume(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_VOLUME, expect_cmd=CMD_VOLUME, debug_frames=False)
        f = r.get("rx_matched")
        if r.get("ok") and f:
            # ehl_protocol.interpret_volume_bytes expects bytes; reconstruct from hex
            try:
                data_hex = f.get("data_hex", "")
                data = bytes(int(x, 16) for x in data_hex.split()) if data_hex else b""
                r["volume_text"] = interpret_volume_bytes(data)
            except Exception:
                pass
        return r

    def poll_price(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_PRICE, expect_cmd=CMD_PRICE, debug_frames=False)
        f = r.get("rx_matched")
        if r.get("ok") and f:
            try:
                data_hex = f.get("data_hex", "")
                data = bytes(int(x, 16) for x in data_hex.split()) if data_hex else b""
                r["price_text"] = interpret_price_bytes(data)
            except Exception:
                pass
        return r

    def poll_error(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_ERROR_QUERY, expect_cmd=CMD_ERROR_QUERY, debug_frames=False)
        f = r.get("rx_matched")
        if r.get("ok") and f:
            try:
                data_hex = f.get("data_hex", "")
                data = bytes(int(x, 16) for x in data_hex.split()) if data_hex else b""
                r["error"] = interpret_error_query(data)
            except Exception:
                pass
        return r

    def linetest(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_LINETEST, expect_cmd=CMD_LINETEST, debug_frames=False)
        f = r.get("rx_matched")
        if r.get("ok") and f:
            try:
                data_hex = f.get("data_hex", "")
                data = bytes(int(x, 16) for x in data_hex.split()) if data_hex else b""
                ok = len(data) >= 2 and data[0] == 0x55 and data[1] == 0xAA
                r["linetest_ok"] = ok
                r["linetest_expected"] = "55 AA"
            except Exception:
                pass
        return r

    def product_select_default(self) -> Dict[str, Any]:
        b = bytes([self.cfg.product_select_byte & 0xFF])
        return self.exchange(cmd=CMD_PRODUCT_SELECT, data=b, expect_cmd=CMD_PRODUCT_SELECT, debug_frames=False)

    def program_price(self, price: str) -> Dict[str, Any]:
        payload = _encode_price_payload(price)
        return self.exchange(cmd=CMD_PROG_PRC, data=payload, expect_cmd=CMD_PROG_PRC, debug_frames=False)

    def unblock_verified(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_UNBLOCK, data=b"", expect_cmd=CMD_UNBLOCK, debug_frames=False)
        # Verify by polling STATE (open_for_delivery should become True).
        verify = self._verify_open_for_delivery(desired=True)
        return {"send": r, "verify": verify}

    def block_verified(self) -> Dict[str, Any]:
        r = self.exchange(cmd=CMD_BLOCK, data=b"", expect_cmd=CMD_BLOCK, debug_frames=False)
        verify = self._verify_open_for_delivery(desired=False)
        return {"send": r, "verify": verify}

    def reset(self) -> Dict[str, Any]:
        # No strict semantics assumed here; return raw exchange.
        return self.exchange(cmd=CMD_RESET, data=b"", expect_cmd=CMD_RESET, debug_frames=False)

    def _verify_open_for_delivery(self, *, desired: bool) -> Dict[str, Any]:
        deadline = time.time() + (self.cfg.verify_timeout_ms / 1000.0)
        samples: List[Dict[str, Any]] = []
        while time.time() < deadline:
            s = self.poll_state()
            samples.append(s)
            st = s.get("state")
            if isinstance(st, dict) and bool(st.get("open_for_delivery")) == desired:
                return {"ok": True, "desired": desired, "samples": samples[-5:]}
            sleep_ms(self.cfg.verify_interval_ms)
        return {"ok": False, "desired": desired, "samples": samples[-5:]}


class Api:
    def __init__(self, cfg: Config, bus: EhlBus):
        self.cfg = cfg
        self.bus = bus

    def health(self) -> Dict[str, Any]:
        return {
            "ok": True,
            "time": _now_iso(),
            "serial_port": self.cfg.serial_port,
            "baud": self.cfg.baud,
            "addr": self.cfg.addr,
        }

    def state(self) -> Dict[str, Any]:
        return self.bus.poll_state()

    def volume(self) -> Dict[str, Any]:
        return self.bus.poll_volume()

    def price_get(self) -> Dict[str, Any]:
        return self.bus.poll_price()

    def error_get(self) -> Dict[str, Any]:
        return self.bus.poll_error()

    def linetest(self) -> Dict[str, Any]:
        return self.bus.linetest()

    def product_select(self) -> Dict[str, Any]:
        return self.bus.product_select_default()

    def price_set(self, body: Dict[str, Any]) -> Dict[str, Any]:
        price = str(body.get("price") or self.cfg.default_price)
        # VB6-style: product select before pricing operations
        ps = self.bus.product_select_default()
        pp = self.bus.program_price(price)
        rd = self.bus.poll_price()
        return {"product_select": ps, "program_price": pp, "readback": rd}

    def unblock(self) -> Dict[str, Any]:
        return self.bus.unblock_verified()

    def block(self) -> Dict[str, Any]:
        return self.bus.block_verified()

    def reset(self) -> Dict[str, Any]:
        return self.bus.reset()

    def flow_start(self, body: Dict[str, Any]) -> Dict[str, Any]:
        price = str(body.get("price") or self.cfg.default_price)
        ps = self.bus.product_select_default()
        pp = self.bus.program_price(price)
        ub = self.bus.unblock_verified()
        st = self.bus.poll_state()
        return {"product_select": ps, "program_price": pp, "unblock": ub, "state": st}

    def flow_stop(self) -> Dict[str, Any]:
        bl = self.bus.block_verified()
        st = self.bus.poll_state()
        return {"block": bl, "state": st}


class Handler(BaseHTTPRequestHandler):
    server_version = "EHL-PythonServer/0.1"

    def _read_json_body(self) -> Dict[str, Any]:
        n = int(self.headers.get("content-length") or "0")
        if n <= 0:
            return {}
        raw = self.rfile.read(n)
        try:
            return json.loads(raw.decode("utf-8"))
        except Exception:
            return {}

    def _send(self, status: int, obj: Any) -> None:
        body = _json_bytes(obj)
        self.send_response(status)
        self.send_header("content-type", "application/json; charset=utf-8")
        self.send_header("content-length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args: object) -> None:
        # suppress default stderr logging; we log explicitly
        return

    def do_GET(self) -> None:
        api: Api = self.server.api  # type: ignore[attr-defined]
        p = urlparse(self.path)
        path = p.path.rstrip("/") or "/"
        info(f"HTTP {self.command} {path} from={self.client_address[0]}")
        try:
            if path == "/health":
                return self._send(200, api.health())
            if path == "/state":
                return self._send(200, api.state())
            if path == "/volume":
                return self._send(200, api.volume())
            if path == "/price":
                return self._send(200, api.price_get())
            if path == "/error":
                return self._send(200, api.error_get())
            if path == "/linetest":
                return self._send(200, api.linetest())
            return self._send(404, {"ok": False, "error": "NOT_FOUND", "path": path})
        except Exception as e:
            error(f"HTTP_ERR {path}: {e}")
            debug(traceback.format_exc(), enabled=True)
            return self._send(500, {"ok": False, "error": "INTERNAL", "message": str(e)})

    def do_POST(self) -> None:
        api: Api = self.server.api  # type: ignore[attr-defined]
        p = urlparse(self.path)
        path = p.path.rstrip("/") or "/"
        body = self._read_json_body()
        info(f"HTTP {self.command} {path} from={self.client_address[0]} body={body}")
        try:
            if path == "/unblock":
                return self._send(200, api.unblock())
            if path == "/block":
                return self._send(200, api.block())
            if path == "/reset":
                return self._send(200, api.reset())
            if path == "/product-select":
                return self._send(200, api.product_select())
            if path == "/price":
                return self._send(200, api.price_set(body))
            if path == "/flow/start":
                return self._send(200, api.flow_start(body))
            if path == "/flow/stop":
                return self._send(200, api.flow_stop())
            return self._send(404, {"ok": False, "error": "NOT_FOUND", "path": path})
        except Exception as e:
            error(f"HTTP_ERR {path}: {e}")
            debug(traceback.format_exc(), enabled=True)
            return self._send(500, {"ok": False, "error": "INTERNAL", "message": str(e)})


def main() -> None:
    cfg = load_config()

    log_dir = os.path.join(THIS_DIR, "logs")
    os.makedirs(log_dir, exist_ok=True)
    log_file = os.path.join(log_dir, f"python_server_{time.strftime('%Y%m%d_%H%M%S')}.log")
    init_logging(log_file, console_level=cfg.console_level, file_level=cfg.file_level)

    info("server_start")
    info(f"cfg listen={cfg.listen_host}:{cfg.listen_port} serial={cfg.serial_port} baud={cfg.baud} addr={cfg.addr}")
    info(f"cfg defaults product_select_byte=0x{cfg.product_select_byte:02X} price={cfg.default_price}")

    bus = EhlBus(cfg)
    api = Api(cfg, bus)

    httpd = ThreadingHTTPServer((cfg.listen_host, cfg.listen_port), Handler)
    httpd.api = api  # type: ignore[attr-defined]

    try:
        info(f"listening http://{cfg.listen_host}:{cfg.listen_port}")
        httpd.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        info("server_stop keyboard_interrupt")
    finally:
        try:
            bus.close()
        except Exception:
            pass


if __name__ == "__main__":
    main()

