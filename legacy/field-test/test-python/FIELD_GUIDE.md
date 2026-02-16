# EHL RS-485 Python field guide (ARK 3360 / Debian)

This folder contains **self-contained Python 3 scripts (no pip dependencies)** for on-site verification and control of the EHL dispenser over RS-485.

This guide is optimized for **SSH-only** operation and for producing **actionable logs** you can send back after a field run.

---

## Safety and operator notes

- **Read-only first**: run scan + probe before any control commands.
- **Control is real**: `UNBLOCK/BLOCK` changes the dispenser state. The scripts require an explicit acknowledgement flag.
- **ACK behavior varies**: some dispensers do not return a VB6-style ACK for `UNBLOCK/BLOCK`. The updated `03_control_unblock_block.py` verifies control by polling `STATE (0x4B)` (`open_for_delivery` bit) after sending.

---

## 0) Go to the scripts directory

```bash
cd /home/cloudberries/LPG-EHL-core/python-test
```

If your repo path differs, `cd` to the repo’s `python-test/` directory.

---

## 1) Choose port/baud/address (recommended: environment variables)

Prefer stable device paths under `/dev/serial/by-id/` when available.

```bash
export PORT="/dev/ttyS3"   # example: onboard serial
export BAUD="9600"         # default for these scripts
export ADDR="33"           # dispenser address (scan if unknown)
```

---

## 2) Create a capture folder (everything in one place)

```bash
export RUN_DIR="$HOME/ehl_field_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RUN_DIR"
echo "RUN_DIR=$RUN_DIR"
```

---

## 3) Snapshot the system + serial device state

This is the fastest way to debug “it worked on site but not later”.

```bash
{
  date -Is
  hostname
  uname -a
  cat /etc/os-release
  python3 --version
  id
  groups
  echo "--- /dev/serial ---"
  ls -l /dev/serial/by-id 2>/dev/null || true
  ls -l /dev/serial/by-path 2>/dev/null || true
  echo "--- target port ---"
  ls -l "$PORT"
  stty -F "$PORT" -a 2>/dev/null || true
  echo "--- usb ---"
  lsusb 2>/dev/null || true
  lsusb -t 2>/dev/null || true
  echo "--- who has the port open ---"
  sudo lsof "$PORT" 2>/dev/null || true
} > "$RUN_DIR/system_snapshot.txt"

sudo dmesg -T > "$RUN_DIR/dmesg_full.txt"
sudo journalctl -k --no-pager > "$RUN_DIR/journal_kernel_full.txt"
```

Optional (recommended while testing, in a second terminal):

```bash
sudo journalctl -kf --no-pager > "$RUN_DIR/journal_kernel_follow.txt"
```

---

## 4) Identify likely serial ports

```bash
python3 00_list_ports.py --log-file "$RUN_DIR/00_list_ports.log"
```

Pick the best candidate and set `PORT` accordingly.

---

## 5) Read-only verification (safe-first flow)

### 5.1 Scan addresses (if unknown / to confirm responders)

```bash
python3 02_scan_addresses.py \
  --port "$PORT" --baud "$BAUD" --addr-range 1-64 \
  --log-file "$RUN_DIR/02_scan_addresses.log"
```

If it finds responders, set:

```bash
export ADDR="33"   # example
```

### 5.2 Probe the device with standard poll commands

This sends:
- `STATE (0x4B)`
- `ERROR_QUERY (0x4C)`
- `VOLUME (0x45)`
- `TANKBIT (0xC5)`

```bash
python3 01_probe_readonly.py \
  --port "$PORT" --baud "$BAUD" --addr "$ADDR" \
  --log-file "$RUN_DIR/01_probe_readonly.log"
```

If `01_probe_readonly.py` is **4/4**, the transport is healthy (port/baud/framing).

### 5.3 Listen-only sniff (optional)

Good to confirm line chatter / framing integrity without transmitting.

```bash
python3 04_listen_only.py \
  --port "$PORT" --baud "$BAUD" --duration-s 60 --show-raw \
  --log-file "$RUN_DIR/04_listen_only.log"
```

---

## 6) Control (explicit opt-in)

### 6.1 Explicit BLOCK

```bash
python3 03_control_unblock_block.py \
  --port "$PORT" --baud "$BAUD" --addr "$ADDR" \
  --log-file "$RUN_DIR/03_block.log" \
  --i-understand-this-can-affect-real-hardware \
  block
```

### 6.2 Explicit UNBLOCK

```bash
python3 03_control_unblock_block.py \
  --port "$PORT" --baud "$BAUD" --addr "$ADDR" \
  --log-file "$RUN_DIR/03_unblock.log" \
  --i-understand-this-can-affect-real-hardware \
  unblock
```

### 6.3 Full “unlock → hold → block” run (recommended field control run)

This is operator-friendly and logs state/error changes during the hold.

```bash
python3 05_unlock_hold_block.py \
  --port "$PORT" --baud "$BAUD" --addr "$ADDR" \
  --hold-seconds 30 --console-verbosity chatty \
  --log-file "$RUN_DIR/05_unlock_hold_block.log" \
  --i-understand-this-can-affect-real-hardware
```

---

## 7) RS-485 ioctl mode (only if needed)

Most USB-RS485 adapters do automatic direction control. Some setups need Linux RS-485 mode (`TIOCSRS485`).

Add `--rs485` (and optional RTS delays) to the script you’re running:

```bash
python3 01_probe_readonly.py \
  --port "$PORT" --baud "$BAUD" --addr "$ADDR" \
  --rs485 --rts-before-ms 2 --rts-after-ms 2 \
  --log-file "$RUN_DIR/01_probe_readonly_rs485.log"
```

---

## 8) Quick troubleshooting

- **Read-only probe works, but control says “no ACK”**:
  - This can be normal. `03_control_unblock_block.py` now verifies by polling `STATE` (`open_for_delivery` bit).
- **No responses at all**:
  - wrong `PORT` or wrong `ADDR`
  - wrong baud (try `--baud 4800`, `9600`, `19200`)
  - RS-485 A/B swapped, missing termination/bias, ground/reference issue
  - another process holds the port (`sudo lsof "$PORT"`)
- **Permission denied on `/dev/tty*`**:

```bash
sudo usermod -a -G dialout "$USER"
# logout/login required
```

As a temporary workaround (not recommended long-term):

```bash
sudo chmod 666 "$PORT"
```

---

## 9) Pack up results

```bash
tar -C "$RUN_DIR" -czf "$RUN_DIR.tgz" .
ls -lh "$RUN_DIR.tgz"
```

