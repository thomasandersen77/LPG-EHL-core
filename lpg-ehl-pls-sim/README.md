# LPG-EHL PLS Simulator

Serial port PLS (Pump Level System) simulator for testing EHL protocol without physical hardware.

## Build

```bash
# From project root
mvn -q -DskipTests package

# JAR location
ls -la lpg-ehl-pls-sim/target/pls-sim.jar
```

## Usage

```bash
java -jar lpg-ehl-pls-sim/target/pls-sim.jar --port=<port> [options]
```

### CLI Options

| Option | Default | Description |
|--------|---------|-------------|
| `--port=<port>` | (required) | Serial port device |
| `--baud=<baud>` | 9600 | Baud rate |
| `--mode=<mode>` | line | Frame mode: `line` or `stxetx` |
| `--chunk=<bool>` | false | Enable chunked responses |
| `--latencyMs=<ms>` | 0 | Add random latency jitter (0-N ms) |
| `--logHex=<bool>` | false | Log raw bytes as hex |

### Frame Modes

- **line**: Frames terminated by `\n` (newline)
- **stxetx**: Frames wrapped in STX (0x02) ... ETX (0x03)

### Supported Commands

| Command | Response | Effect |
|---------|----------|--------|
| `FREE` or `UNBLOCK` | `OK` | Unblock dispenser 1 |
| `STOP` or `BLOCK` | `OK` | Block dispenser 1 |
| `STATUS` | `BLOCKED`/`UNBLOCKED` | Query dispenser state |
| Other | `ACK` | Acknowledge unknown command |

---

## macOS Setup (using socat)

Create a virtual serial port pair with `socat`:

```bash
# Terminal 1: Create PTY pair
socat -d -d pty,raw,echo=0 pty,raw,echo=0
# Output shows something like:
#   N PTY is /dev/ttys013
#   N PTY is /dev/ttys014
```

```bash
# Terminal 2: Run simulator on one end
java -jar lpg-ehl-pls-sim/target/pls-sim.jar \
    --port=/dev/ttys013 \
    --baud=9600 \
    --mode=line \
    --chunk=true \
    --logHex=true
```

```bash
# Terminal 3: Test from the other end
# Option A: screen
screen /dev/ttys014 9600

# Option B: minicom
minicom -D /dev/ttys014 -b 9600

# Option C: direct echo
echo "FREE" > /dev/ttys014
cat /dev/ttys014  # See response
```

### macOS One-Liner Test

```bash
# Start socat in background, capture port names
socat -d -d pty,raw,echo=0,link=/tmp/pty1 pty,raw,echo=0,link=/tmp/pty2 &
sleep 1

# Run simulator
java -jar lpg-ehl-pls-sim/target/pls-sim.jar --port=/tmp/pty1 --logHex=true &
sleep 1

# Send test command
echo "FREE" > /tmp/pty2
sleep 0.5
echo "STATUS" > /tmp/pty2
```

---

## Debian/Linux Setup

### Using Real Serial Port

```bash
# Check available ports
ls -la /dev/ttyS* /dev/ttyUSB*

# Run simulator on /dev/ttyS0
sudo java -jar lpg-ehl-pls-sim/target/pls-sim.jar \
    --port=/dev/ttyS0 \
    --baud=9600 \
    --mode=line
```

### Using Virtual Serial Ports

```bash
# Install socat if needed
sudo apt-get install socat

# Create virtual port pair
socat -d -d pty,raw,echo=0,link=/tmp/pty1 pty,raw,echo=0,link=/tmp/pty2 &

# Run simulator
java -jar lpg-ehl-pls-sim/target/pls-sim.jar --port=/tmp/pty1 --logHex=true
```

### Permission Issues

If you get permission errors:

```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Or use udev rules for USB-serial adapters
# /etc/udev/rules.d/99-usb-serial.rules
# SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", MODE="0666"
```

---

## Example Session

```
21:30:15.123 [main] INFO  PlsSimMain - === PLS Simulator Starting ===
21:30:15.145 [main] INFO  PlsSimMain - Configuration:
21:30:15.146 [main] INFO  PlsSimMain -   Port:      /dev/ttys013
21:30:15.146 [main] INFO  PlsSimMain -   Baud:      9600
21:30:15.146 [main] INFO  PlsSimMain -   Mode:      LINE
21:30:15.146 [main] INFO  PlsSimMain -   Chunked:   true
21:30:15.146 [main] INFO  PlsSimMain -   Latency:   0 ms
21:30:15.147 [main] INFO  PlsSimMain -   Log Hex:   true
21:30:15.234 [main] INFO  SerialPortHandler - Serial port opened successfully: ttys013
21:30:15.235 [main] INFO  PlsSimMain - PLS Simulator running. Press Ctrl+C to stop.

21:30:22.456 [pls-sim-reader] INFO  SerialPortHandler - RX: 5 bytes: 46 52 45 45 0A
21:30:22.457 [pls-sim-reader] INFO  SerialPortHandler - Frame received: 'FREE'
21:30:22.458 [pls-sim-reader] INFO  PlsState - Dispenser 1 state changed: UNBLOCKED
21:30:22.461 [pls-sim-reader] DEBUG SerialPortHandler - TX chunk 1: 4F
21:30:22.478 [pls-sim-reader] DEBUG SerialPortHandler - TX chunk 2: 4B 0A
21:30:22.479 [pls-sim-reader] INFO  SerialPortHandler - TX (chunked 2x): 'OK'
```

---

## STX/ETX Mode Example

For binary protocol testing:

```bash
java -jar lpg-ehl-pls-sim/target/pls-sim.jar \
    --port=/dev/ttys013 \
    --mode=stxetx \
    --logHex=true
```

Send STX-wrapped command (hex):
```bash
# STX + "FREE" + ETX = 02 46 52 45 45 03
printf '\x02FREE\x03' > /dev/ttys014
```

Response will be: `02 4F 4B 03` (STX + "OK" + ETX)

---

## Troubleshooting

### Port Not Found

```
RuntimeException: Failed to open serial port: /dev/ttys013
```

- Check if socat is running
- Verify port path with `ls -la /dev/tty*`
- On macOS, ports are `/dev/ttys*`, on Linux `/dev/pts/*` or `/dev/ttyS*`

### No Response

- Ensure correct frame mode (`line` vs `stxetx`)
- Check if newline is sent: `echo -e "FREE\n"` or `echo "FREE"`
- Enable `--logHex=true` to debug byte-level traffic

### Permission Denied

- macOS: Should work without sudo for PTYs
- Linux: Add user to `dialout` group or run with `sudo`
