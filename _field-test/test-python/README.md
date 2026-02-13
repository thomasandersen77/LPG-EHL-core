# RS-485 Pump (EHL) field test kit (Debian 32-bit)

This folder contains **self-contained Python 3 scripts (no pip dependencies)** to:

- Verify that the **serial/RS-485 link** is alive
- Verify that the device responds with **valid EHL frames** (checksum + framing)
- Optionally (explicitly opt-in), send **control commands** (UNBLOCK/BLOCK)

The scripts implement the same **packet format** used by the VB6 legacy app:

```
STX (1) + LEN (1) + ADDR (1) + CMD (1) + DATA (0..n) + CHK (1) + ETX (1)
```

- Controller→dispenser STX is typically `0x10`
- Dispenser→controller STX is typically `0x20`
- ETX is `0x36`
- CHK is XOR of all bytes from STX up to last DATA byte (i.e. everything except CHK+ETX)

## Quick start

### 0) Identify your serial device

Run:

```bash
python3 00_list_ports.py
```

Typical device paths:

- `/dev/ttyUSB0` (USB RS-485 dongle)
- `/dev/ttyS0` (onboard serial)
- `/dev/serial/by-id/...` (preferred: stable name)

### 1) Run read-only probes (recommended first)

This sends the **lowest-risk** poll commands that the VB6 app itself spams routinely:

- `STATE` (`0x4B`)
- `ERROR_QUERY` (`0x4C`)
- `VOLUME` (`0x45`)
- `TANKBIT` (`0xC5`)

Example:

```bash
python3 01_probe_readonly.py --port /dev/ttyUSB0 --addr 1
```

If you **don’t know the address**, scan a small range first:

```bash
python3 02_scan_addresses.py --port /dev/ttyUSB0 --addr-range 1-32
```

### 2) Listen-only mode (sniff)

To see if the line is chattering (or another device is talking), run:

```bash
python3 04_listen_only.py --port /dev/ttyUSB0
```

### 3) (Optional) Control: UNBLOCK / BLOCK

**This can change the real dispenser state.** Only run if you intend to control the pump.

```bash
python3 03_control_unblock_block.py --port /dev/ttyUSB0 --addr 1 unblock --i-understand-this-can-affect-real-hardware
python3 03_control_unblock_block.py --port /dev/ttyUSB0 --addr 1 block   --i-understand-this-can-affect-real-hardware
```

### 4) (Field test) VB6-style unlock, hold, then block

This script attempts a VB6-like enable sequence with **strict ACK checking**, then holds for 30 seconds (polling STATE/ERROR), then blocks again.

```bash
python3 05_unlock_hold_block.py --port /dev/ttyUSB0 --addr 33 --hold-seconds 30 --i-understand-this-can-affect-real-hardware
```

## Logging to a file

All scripts accept `--log-file` to write the same logs to a `.log` file (in addition to stdout). Example:

```bash
python3 01_probe_readonly.py --port /dev/ttyUSB0 --addr 33 --log-file /tmp/ehl_probe.log
```

## Serial settings

These scripts default to:

- **9600 baud**
- **8 data bits**
- **no parity**
- **1 stop bit** (8N1)
- no flow control

This matches the common EHL setup and what the Kotlin side uses in this repo. The VB6 code does **not** explicitly set `MSComm1.Settings`, so if the field unit uses a different baud, pass `--baud` to these scripts.

## RS-485 driver mode (optional)

Some RS-485 adapters/drivers require enabling RS-485 mode (RTS toggle) via Linux `TIOCSRS485`.
If needed, add `--rs485` (and optionally delays):

```bash
python3 01_probe_readonly.py --port /dev/ttyUSB0 --addr 1 --rs485 --rts-before-ms 2 --rts-after-ms 2
```

If your adapter has automatic direction control (most USB-RS485 dongles do), you should **not** need this.

## What “good” looks like

- You see TX frames logged (hex), then RX frames that:
  - start with `0x20` (dispenser STX)
  - have a reasonable LEN (>= 6)
  - end with `0x36`
  - pass XOR checksum validation

If RX is empty:

- Wrong **port** or wrong **address**
- Wrong **baud**
- RS-485 **A/B swapped**, missing termination/bias, bad ground
- Another process is holding the port open

## Safety notes

- `01_probe_readonly.py`, `02_scan_addresses.py`, `04_listen_only.py` are designed to be **low-risk**.
- `03_control_unblock_block.py` is **high risk** by definition and requires an explicit acknowledgement flag.

