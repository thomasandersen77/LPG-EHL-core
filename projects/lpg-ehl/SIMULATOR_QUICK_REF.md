# Simulator Quick Reference

## 🚀 Quick Start

### Lokal utvikling (med GUI)
```bash
./scripts/start-all-simulators.sh
```

### Debian server via SSH (headless)
```bash
./scripts/start-all-simulators.sh --headless
```

### Field mode (produksjon)
```bash
./scripts/start-all-simulators.sh --field --headless
```

---

## 📋 Alle Kommandoer

| Kommando | Beskrivelse | Komponenter | GUI |
|----------|-------------|-------------|-----|
| `./scripts/start-all-simulators.sh` | Lokal dev | Socat + PLS + Terminal | ✅ |
| `./scripts/start-all-simulators.sh --headless` | Remote server | Socat + PLS + Terminal | ❌ |
| `./scripts/start-all-simulators.sh --field` | Field (kun PLS) | Socat + PLS | ❌ |
| `./scripts/start-all-simulators.sh --field --with-terminal` | Field + Terminal | Socat + PLS + Terminal | ❌ |
| `./scripts/start-all-simulators.sh --field --headless` | Field headless | Socat + PLS | ❌ |

---

## 🔧 Viktige Flagg

### `--headless`
Tvinger **alle** komponenter til terminal-modus (ingen GUI).  
**Bruk:** Debian server via SSH, CI/CD, Docker.

### `--field`
Field mode: kun socat + PLS.  
**Bruk:** Produksjon (ARK/edge).

### `--with-terminal`
Inkluder terminal-simulatoren i field mode.  
**Krever:** `--field`

### `--gui`
Aktiver GUI for PLS-simulator (dødmannsknapp).  
**Konflikt:** Kan ikke brukes med `--headless`.

### `--build`
Bygg JARs først.

### `--terminal-port=PORT`
Sett port for terminal-simulator (default: 18080).

---

## 🎯 Eksempler

### 1. Lokal test med GUI
```bash
./scripts/start-all-simulators.sh
```

### 2. Headless test (alle komponenter)
```bash
./scripts/start-all-simulators.sh --headless
```

### 3. Field mode på remote server
```bash
# SSH inn
ssh user@debian-server

# Start simulatorene
./scripts/start-all-simulators.sh --field --headless

# Start webapp separat
java -jar release/lpg-ehl-webapp.jar \
    --spring.profiles.active=field \
    --ehl.serial.port=/tmp/vserial1
```

### 4. Field mode med terminal (full stack remote)
```bash
./scripts/start-all-simulators.sh --field --with-terminal --headless
```

### 5. Bygg og start headless
```bash
./scripts/start-all-simulators.sh --build --headless
```

### 6. Custom terminal port
```bash
./scripts/start-all-simulators.sh --headless --terminal-port=8090
```

---

## ⚠️ Viktige Notater

### Headless Mode
- `--headless` deaktiverer **alltid** GUI, uansett andre flagg
- Perfekt for Debian servere uten display
- Alle loggmeldinger går til stdout

### Field Mode
- Default: kun socat + PLS
- Legg til `--with-terminal` for å inkludere terminal-simulator
- Webapp startes separat

### Port Conflicts
Hvis port 18080 er opptatt:
```bash
# Drep prosessen
lsof -ti:18080 | xargs kill -9

# Eller bruk annen port
./scripts/start-all-simulators.sh --headless --terminal-port=8090
```

### Serial Port Permissions (Debian)
```bash
sudo usermod -a -G dialout $USER
# Logg ut og inn igjen
```

---

## 📚 Dokumentasjon

- Full guide: `docs/HEADLESS_DEPLOYMENT.md`
- Arkitektur: `docs/ARKITEKTUR_ANALYSE_EDGE_2026.md`
- Transport modes: `docs/TRANSPORT_MODES.md`

---

## 🛑 Stopp Simulatorene

Trykk `Ctrl+C` i terminalen hvor skriptet kjører.

---

## 💡 Tips

### CI/CD Pipeline
```bash
#!/bin/bash
./scripts/start-all-simulators.sh --headless &
SIM_PID=$!
sleep 5
mvn verify -Pintegration-tests
kill $SIM_PID
```

### Docker
```dockerfile
FROM eclipse-temurin:21-jdk
COPY release/ /app/
WORKDIR /app
CMD ["java", "-jar", "pls-sim.jar", "--port=/dev/ttyUSB0", "--mode=ehl"]
```

### Systemd Service (Debian)
```ini
[Unit]
Description=LPG-EHL PLS Simulator
After=network.target

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl/projects/lpg-ehl
ExecStart=/opt/lpg-ehl/projects/lpg-ehl/scripts/start-all-simulators.sh --field --headless
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

---

## 🐛 Feilsøking

### JavaFX Error
```
UnsupportedOperationException: Unable to open DISPLAY
```
**Løsning:** Bruk `--headless` flagget.

### Port Already in Use
```
java.net.BindException: Address already in use
```
**Løsning:**
```bash
lsof -ti:18080 | xargs kill -9
```

### Socat Not Found
```
socat: command not found
```
**Løsning:**
```bash
# macOS
brew install socat

# Debian/Ubuntu
sudo apt-get install socat
```

---

## ⚙️ Avansert

### JVM Tuning (PLS Simulator)
```bash
./scripts/sim-pls.sh \
    --simXms=128m \
    --simXmx=256m \
    --simGc=g1 \
    --simJavaOpts="-XX:+PrintGCDetails"
```

### Field Mode Knobs
```bash
./scripts/start-all-simulators.sh \
    --field \
    --headless \
    --field.noAckOnUnblock=true \
    --field.mechanicalOpenDelayMs=1200 \
    --field.dropResponseProbability=0.1
```

---

**Oppdatert:** 2026-02-16  
**Se også:** [docs/HEADLESS_DEPLOYMENT.md](docs/HEADLESS_DEPLOYMENT.md)
