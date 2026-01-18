# LPG-EHL Serial Contract (RS-485)

## 1. Physical Layer

- **Medium:** RS-485 (2-wire differential signaling)
- **Topology:** Multi-drop bus (shared pair), device addressing at protocol layer
- **Flow control:** None (half-duplex communication)
- **Cable:** Twisted pair, 120Ω termination at both ends recommended
- **Max distance:** Up to 1200m (depending on cable quality and baud rate)

## 2. Link / UART Parameters (Default Legacy)

| Parameter | Value | Notes |
|-----------|-------|-------|
| **Baud rate** | 9600 | Must be configurable at runtime |
| **Data bits** | 8 | Standard |
| **Parity** | Even | Critical for error detection |
| **Stop bits** | 1 | Standard |
| **Shorthand** | **9600 8E1** | Industry standard notation |

> **NOTE:** All parameters MUST be configurable at runtime (environment variables or application config), but defaults should match legacy hardware configuration (9600 8E1).

## 3. Configuration

### Environment Variables

```bash
# Serial port device path
EHL_SERIAL_PORT=/dev/ttyS0

# UART parameters (all optional, defaults to 9600 8E1)
EHL_SERIAL_BAUD_RATE=9600
EHL_SERIAL_DATA_BITS=8
EHL_SERIAL_PARITY=EVEN
EHL_SERIAL_STOP_BITS=1

# Timeouts (milliseconds)
EHL_SERIAL_READ_TIMEOUT=1000
EHL_SERIAL_WRITE_TIMEOUT=1000
```

### Spring Boot Configuration (application.yaml)

```yaml
ehl:
  serial:
    port: /dev/ttyS0
    baud-rate: 9600
    data-bits: 8
    parity: EVEN  # 8E1 format
    stop-bits: 1
    read-timeout: 1000
    write-timeout: 1000
```

## 4. Linux Device Naming

Common device paths by platform:

| Platform | Device Path | Notes |
|----------|-------------|-------|
| **Onboard serial** | `/dev/ttyS0`, `/dev/ttyS1` | Built-in COM ports |
| **USB serial adapters** | `/dev/ttyUSB0`, `/dev/ttyUSB1` | Most common for RS-485 converters |
| **FTDI devices** | `/dev/ttyUSB*` | USB-to-serial chips |
| **Prolific devices** | `/dev/ttyUSB*` | USB-to-serial chips |
| **CP2102 devices** | `/dev/ttyUSB*` | Silicon Labs USB-to-serial |

### Device Permissions

Serial ports typically require `dialout` group membership:

```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Check device permissions
ls -l /dev/ttyUSB0
# Expected: crw-rw---- 1 root dialout

# Check group membership
groups
```

## 5. Transport Implementations

### RealSerialTransport (FIELD Mode)
- Opens Linux device via jSerialComm
- Applies UART parameters (9600 8E1)
- Handles read/write operations
- Implements watchdog for auto-reconnect
- Location: `lpg-transport` module

### EmulatorTransport (LAB Mode)
- In-memory emulator for CI/CD and local development
- Virtual serial port simulation
- TCP-backed for multi-process testing
- Location: `lpg-ehl-emulator` module

### TcpSerialTransport (Optional Future)
- Serial-over-IP gateways (e.g., Moxa NPort)
- Remote hardware access
- Not yet implemented

## 6. Timeouts and Robustness

### Read Operations
- **Non-blocking or short blocking reads** with periodic polling
- **Default timeout:** 1000ms
- **Retry strategy:** Application layer responsibility
- **Buffer management:** Circular buffer with overflow protection

### Write Operations
- **Best-effort delivery**
- **Partial write logging:** Warning if not all bytes written
- **Default timeout:** 1000ms
- **Error handling:** Exception propagation to service layer

### Retry Policy
- **Command-level retries:** Owned by service layer, not transport
- **Typical retry count:** 3 attempts
- **Backoff strategy:** Exponential (100ms, 200ms, 400ms)

### Connection Management
- **Watchdog:** Automatic reconnection on communication failure
- **Health check interval:** 30 seconds
- **Reconnection attempts:** Unlimited with exponential backoff

## 7. Logging and Debugging

### Protocol Logging (HEX Dump)

Enable detailed protocol logging for troubleshooting:

```yaml
logging:
  level:
    no.cloudberries.lpg.communication: DEBUG
    no.cloudberries.lpg.protocol: DEBUG
```

Expected log output:
```
[PROTOCOL] TX -> 01 03 00 00 00 01 84 0A
[PROTOCOL] RX <- 01 03 02 00 00 B8 44
```

### Log Correlation
- Each transaction has unique correlation ID
- Request/response pairing via correlation
- WebSocket streaming for real-time monitoring

### Debug Tools
```bash
# Monitor serial port traffic (requires root)
sudo cat /dev/ttyUSB0 | hexdump -C

# Check port status
sudo lsof /dev/ttyUSB0

# List all serial ports
ls -la /dev/tty{S,USB}*
```

## 8. Lab Simulation

### Recommended: socat PTY Pairs

Create virtual serial port pairs for local development:

```bash
# Create PTY pair
socat -d -d pty,raw,echo=0 pty,raw,echo=0

# Example output:
# 2024/01/16 20:00:00 socat[1234] N PTY is /dev/pts/5
# 2024/01/16 20:00:00 socat[1234] N PTY is /dev/pts/6
```

**Terminal 1 - Run application:**
```bash
export EHL_EMULATOR_ENABLED=false
export EHL_SERIAL_PORT=/dev/pts/5
java -jar lpg-ehl-webapp.jar
```

**Terminal 2 - Run emulator:**
```bash
# If emulator supports serial device path:
export EMULATOR_SERIAL_PORT=/dev/pts/6
java -jar lpg-ehl-emulator.jar

# Or bridge TCP to PTY:
socat TCP-LISTEN:9000,reuseaddr,fork FILE:/dev/pts/6,raw,echo=0
```

### Docker Testing

```bash
# Create named volume for device access
docker run -d \
  --name lpg-ehl \
  --device=/dev/ttyUSB0:/dev/ttyUSB0 \
  -e EHL_SERIAL_PORT=/dev/ttyUSB0 \
  lpg-ehl-headless:latest
```

## 9. Troubleshooting

### Common Issues

| Problem | Cause | Solution |
|---------|-------|----------|
| Permission denied | Not in dialout group | `sudo usermod -a -G dialout $USER` |
| Device not found | Wrong path or unplugged | Check `ls -la /dev/tty*` |
| Garbled data | Wrong baud rate | Verify 9600 8E1 on both sides |
| Timeout errors | Cable issue or wrong device | Check connections, try loopback test |
| Parity errors | 8N1 vs 8E1 mismatch | Ensure EVEN parity on both sides |

### Loopback Test

Test serial port hardware by connecting TX to RX:

```bash
# Connect pin 2 (TX) to pin 3 (RX) on DB9 connector
# Then run:
echo "TEST" > /dev/ttyUSB0 &
cat /dev/ttyUSB0
# Should echo back "TEST"
```

### Serial Port Diagnostics

```bash
# Check port configuration
stty -F /dev/ttyUSB0 -a

# Set port to 9600 8E1
stty -F /dev/ttyUSB0 9600 cs8 parenb -parodd -cstopb

# Monitor with screen (Ctrl-A K to exit)
screen /dev/ttyUSB0 9600,cs8,parenb,-parodd
```

## 10. Hardware Compatibility

### Tested RS-485 Adapters

| Manufacturer | Model | Chipset | Status |
|--------------|-------|---------|--------|
| FTDI | USB-RS485-WE-1800-BT | FT232RL | ✅ Verified |
| Prolific | PL2303 | PL2303 | ✅ Verified |
| WaveShare | USB TO RS485 (B) | CH340 | ✅ Verified |
| Moxa | UPort 1150 | Proprietary | ⚠️ Needs testing |

### Industrial PCs

| Model | Port | Notes |
|-------|------|-------|
| ARK-3600 Series | `/dev/ttyS0` | Onboard RS-485 |
| Raspberry Pi + HAT | `/dev/ttyAMA0` | With RS-485 HAT |
| ASUS Tinker Board | `/dev/ttyS1` | With RS-485 HAT |

## 11. Protocol Reference

See related documentation:
- [EHL Protocol Specification](EHL_PROTOCOL.md)
- [Implementation Plan](IMPLEMENTATION_PLAN_PLS_CLI.md)
- [Architecture Overview](../ARCHITECTURE.md)

## 12. Testing Checklist

- [ ] Serial port opens successfully
- [ ] Baud rate configured correctly (9600)
- [ ] Parity set to EVEN (8E1)
- [ ] Can write bytes without errors
- [ ] Can read responses within timeout
- [ ] Watchdog reconnects on failure
- [ ] Permissions work for non-root user
- [ ] Works in Docker container
- [ ] PTY simulation works for CI/CD
- [ ] Logging captures TX/RX in HEX format
