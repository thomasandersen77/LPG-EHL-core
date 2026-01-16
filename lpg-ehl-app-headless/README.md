# LPG EHL Headless Application

Bakgrunnsservice for LPG-dispenserstyring **uten web-server eller GUI**. Perfekt for produksjonsmiljøer på bensinstasjoner.

## ✨ Features

- ✅ **NO WEB SERVER** - Ingen HTTP overhead, kun forretningslogikk
- ✅ **Automatic Hardware Communication** - Kobler automatisk til dispensere via serial port
- ✅ **Database Persistence** - Lagrer transaksjoner i PostgreSQL
- ✅ **Azure Cloud Sync** - Automatisk synkronisering til sky
- ✅ **Scheduled Tasks** - Polling, watchdog, og synkronisering kjører automatisk
- ✅ **Systemd Compatible** - Klar for Linux service deployment
- ✅ **Docker Ready** - Fungerer perfekt i containers

## 🚀 Quick Start

### Build

```bash
mvn clean package -pl lpg-ehl-app-headless -am
```

### Run

```bash
# LAB Mode (for testing med emulator)
java -jar target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar

# FIELD Mode (produksjon med ekte hardware)
java -jar target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --EHL_EMULATOR_ENABLED=false \
  --EHL_SERIAL_PORT=/dev/ttyUSB0 \
  --DB_HOST=localhost \
  --AZURE_ENABLED=true
```

## 🐳 Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/lpg-ehl-app-headless-*.jar app.jar

# For serial port access
RUN apk add --no-cache udev

ENV DB_HOST=postgres
ENV EHL_EMULATOR_ENABLED=false

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Run Container

```bash
docker run -d \
  --name lpg-ehl-headless \
  --device=/dev/ttyUSB0 \
  -e DB_HOST=postgres \
  -e DB_PASSWORD=secret \
  -e AZURE_CONNECTION_STRING=$AZURE_CS \
  lpg-ehl-headless:latest
```

## 🐧 Systemd Service

Create `/etc/systemd/system/lpg-ehl.service`:

```ini
[Unit]
Description=LPG EHL Headless Service
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/usr/bin/java -jar /opt/lpg-ehl/lpg-ehl-app-headless.jar
Restart=on-failure
RestartSec=10s

Environment="DB_HOST=localhost"
Environment="EHL_EMULATOR_ENABLED=false"
Environment="EHL_SERIAL_PORT=/dev/ttyUSB0"
Environment="AZURE_ENABLED=true"

[Install]
WantedBy=multi-user.target
```

**Enable and start:**

```bash
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl
sudo systemctl status lpg-ehl
```

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | lpg_ehl | Database name |
| `DB_USER` | lpg_user | Database username |
| `DB_PASSWORD` | - | Database password (**required**) |
| `EHL_EMULATOR_ENABLED` | false | Use emulator (true) or real hardware (false) |
| `EHL_SERIAL_PORT` | /dev/ttyS0 | Serial port device |
| `EHL_BAUD_RATE` | 9600 | Serial baud rate |
| `AZURE_ENABLED` | true | Enable Azure sync |
| `AZURE_CONNECTION_STRING` | - | Azure Storage connection string |
| `LOG_FILE` | /var/log/lpg-ehl/headless.log | Log file path |

### application.yaml

Default configuration i `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: lpg-ehl-headless

ehl:
  emulator:
    enabled: false  # FIELD mode by default for headless
  serial:
    port: /dev/ttyS0
    baud-rate: 9600
    data-bits: 8
    parity: EVEN  # 8E1 format
    stop-bits: 1

logging:
  file:
    name: /var/log/lpg-ehl/headless.log
```

## 📊 Monitoring

### Logs

```bash
# Follow logs
tail -f /var/log/lpg-ehl/headless.log

# Systemd logs
journalctl -u lpg-ehl -f
```

### Expected Startup Output

```
═══════════════════════════════════════════════════════════
   LPG EHL HEADLESS APPLICATION
   Mode: HEADLESS (No Web Server)
═══════════════════════════════════════════════════════════

🚀 HEADLESS STARTUP SEQUENCE

🔌 Initializing hardware communication...
   Mode: FIELD
   🏭 FIELD MODE: Using real hardware
   ✅ Hardware communication initialized

🐕 Starting hardware watchdog service...
   ✅ Watchdog service active

☁️  Starting Azure sync services...
   ✅ Azure queue reader active
   ✅ Transaction sync active

📊 SYSTEM STATUS:
   • Dispensers: Ready for operation
   • Database: Connected
   • Scheduled tasks: Running
   • Pump #1 State: IDLE
   • Current Price: 15.90 kr/L

═══════════════════════════════════════════════════════════
✅ HEADLESS APPLICATION READY
═══════════════════════════════════════════════════════════

💡 System is now running in background mode
💡 Transactions will be saved to database automatically
💡 Azure sync is active (if configured)
💡 Check logs for dispenser activity

Press Ctrl+C to stop
```

## 🎯 Use Cases

### Production Deployment
Kjør headless-appen på en server ved siden av pumpen. Ingen GUI nødvendig - alt skjer automatisk.

### Raspberry Pi
Perfekt for embedded deployment direkte på bensinstasjonen.

### Docker Swarm / Kubernetes
Skalerbar deployment for multi-stasjon setup.

### Testing
Bruk LAB mode med emulator for integrasjonstesting uten hardware.

## 🔧 Troubleshooting

### Serial Port Permission Denied

```bash
# Add user to dialout group
sudo usermod -a -G dialout $USER

# Or change permissions (not recommended)
sudo chmod 666 /dev/ttyUSB0
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Test connection
psql -h localhost -U lpg_user -d lpg_ehl
```

### No Transactions Being Saved

- Check hardware is connected and powered
- Verify serial port configuration
- Check logs for errors
- Ensure database credentials are correct

## 📚 See Also

- [Main Architecture Documentation](../ARCHITECTURE.md)
- [Web Application README](../lpg-ehl-webapp/README.md)
- [Protocol Documentation](../lpg-ehl-core/README.md)

## 🤝 Support

For spørsmål eller problemer, kontakt utviklingsteamet.
