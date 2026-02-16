# Payment terminal experiments (Python)

This folder is a **scratchpad / field kit** for experimenting against a payment terminal (network or serial),
without mixing that work into the EHL RS-485 scripts in `projects/python-test/`.

Nothing here is “production”. Keep scripts small, runnable, and log-heavy.

This folder is intended to be **self-contained** so you can copy it to a field machine and run it standalone.

---

## Quick start

### 1) Create a config file

From repo root:

```bash
cp projects/python-test/payment-terminal/config.example.json projects/python-test/payment-terminal/config.json
```

Edit `projects/python-test/payment-terminal/config.json` with the terminal connection details.

### 2) BAXI over Ethernet (recommended starting point)

Per the Baxi.net guide:
- **the terminal initiates the TCP connection** (ECR = server)
- default listener port is **6001**

Run the listener and wait for the terminal to connect:

```bash
cd projects/python-test/payment-terminal
python3 20_baxi_listener_repl.py
```

When connected, the script will dump RX bytes and provide a small REPL to send bytes back.

### 2) TCP capture + optional send

Connects to the terminal (optionally TLS), optionally sends a hex payload, then captures bytes for a few seconds.

```bash
cd projects/python-test/payment-terminal
python3 00_tcp_capture.py --help
python3 00_tcp_capture.py
```

### 3) Serial capture (Linux-only)

Uses the repo’s Linux-only raw serial helper (`projects/python-test/serial_linux.py`).

```bash
cd projects/python-test/payment-terminal
python3 10_serial_capture_linux.py --help
python3 10_serial_capture_linux.py --port /dev/ttyS3 --baud 9600
```

---

## Logging

By default, scripts write logs to `projects/python-test/payment-terminal/logs/` (and also to stdout).
Override with `--log-file` if you want a specific output path.

