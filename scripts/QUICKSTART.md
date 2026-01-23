# QUICKSTART: Simulator + Headless App

Denne guiden forklarer hvordan du kjører PLS-simulatoren og headless-appen.

---

## 🚀 Rask Start (Kommandolinje)

### Steg 1: Start Simulator (Terminal 1)

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
./scripts/start-sim.sh
```

Dette starter:
- **socat**: Virtuelt seriellport-par (`/tmp/ttyV0` ↔ `/tmp/ttyV1`)
- **PLS Simulator**: Kobler til `/tmp/ttyV0`

La denne kjøre.

### Steg 2: Start App (Terminal 2)

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl

# Full webapp med GUI og WebSocket-logging:
./scripts/start-webapp.sh

# Eller headless (uten GUI):
./scripts/start-headless.sh

# Headless med debug API (for curl):
./scripts/start-headless.sh --debug-api
```

Webapp GUI:
- Dashboard: http://localhost:8080
- Kontrollpanel: http://localhost:8080/control (med live logging)

---

## 📋 Opsjoner

### start-sim.sh

| Opsjon | Default | Beskrivelse |
|--------|---------|-------------|
| `--address=N` | 1 | Dispenser-adresse (1-8) |
| `--price=N` | 1590 | Pris i øre (1590 = 15.90 kr/L) |
| `--blocked=bool` | true | Start blokkert |

**Eksempel:**
```bash
./scripts/start-sim.sh --price=2100 --blocked=false
```

### start-headless.sh

| Opsjon | Default | Beskrivelse |
|--------|---------|-------------|
| `--debug-api` | false | Aktiver REST API |
| `--port=N` | 8080 | Web-port (hvis debug-api) |

---

## 🔧 Manuell Kjøring (uten scripts)

### JAR-filer

Alle JAR-filer ligger i `release/`:
- `pls-sim.jar` - PLS Simulator
- `lpg-ehl-headless.jar` - Headless app
- `lpg-ehl-webapp.jar` - Full webapp med GUI

### Start Simulator Manuelt

```bash
# Terminal 1: Start socat
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1

# Terminal 2: Start simulator
java -jar release/pls-sim.jar \
  --port=/tmp/ttyV0 \
  --mode=ehl \
  --logHex=true \
  --address=1 \
  --price=1590 \
  --blocked=true
```

### Start Headless App Manuelt

```bash
# Uten web-server:
java -jar release/lpg-ehl-headless.jar \
  --ehl.transport.mode=SOCAT \
  --ehl.serial.port=/tmp/ttyV1 \
  --logging.file.name=./headless.log

# Med debug API:
java -jar release/lpg-ehl-headless.jar \
  --spring.profiles.active=debug-api \
  --ehl.transport.mode=SOCAT \
  --ehl.serial.port=/tmp/ttyV1 \
  --logging.file.name=./headless.log \
  --server.port=8080
```

---

## 💻 IntelliJ IDEA

### Run Configuration for Simulator

1. **Run > Edit Configurations > + > Application**
2. **Name:** `PLS Simulator (SOCAT)`
3. **Main class:** `no.cloudberries.lpg.sim.PumpSimulatorAppKt`
4. **Module:** `lpg-ehl-serialport-sim`
5. **Program arguments:**
   ```
   --port=/tmp/ttyV0 --mode=ehl --logHex=true --address=1 --price=1590 --blocked=true
   ```
6. **Before launch:** Legg til "Run External Tool" for socat:
   - Program: `/opt/homebrew/bin/socat` (eller `which socat`)
   - Arguments: `-d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1`

**Alternativt:** Start socat manuelt i terminal først, deretter kjør simulator fra IntelliJ.

### Run Configuration for Headless App

1. **Run > Edit Configurations > + > Application**
2. **Name:** `Headless App (SOCAT)`
3. **Main class:** `no.cloudberries.lpg.headless.HeadlessApplicationKt`
4. **Module:** `lpg-ehl-app-headless`
5. **Program arguments:**
   ```
   --ehl.transport.mode=SOCAT --ehl.serial.port=/tmp/ttyV1 --logging.file.name=./headless.log
   ```
6. **For debug API, legg til:**
   ```
   --spring.profiles.active=debug-api --server.port=8080
   ```

### Compound Configuration

For å starte begge samtidig:

1. **Run > Edit Configurations > + > Compound**
2. **Name:** `Simulator + Headless`
3. Legg til: `PLS Simulator (SOCAT)` og `Headless App (SOCAT)`

**NB:** Start socat manuelt først, eller bruk et shell-script som "Before launch".

---

## 🧪 Test med curl (debug-api)

Når headless kjører med `--debug-api`:

```bash
# Helsesjekk
curl http://localhost:8080/api/debug/health

# Les pumpetilstand
curl http://localhost:8080/api/debug/state/1

# Frigi pumpe
curl -X POST http://localhost:8080/api/debug/unblock/1

# Blokker pumpe
curl -X POST http://localhost:8080/api/debug/block/1

# Linetest
curl -X POST http://localhost:8080/api/debug/linetest/1
```

---

## 📁 Filstruktur

```
lpg-ehl/
├── release/
│   ├── pls-sim.jar              # PLS Simulator
│   ├── lpg-ehl-headless.jar     # Headless app
│   └── lpg-ehl-webapp.jar       # Full webapp
│
├── scripts/
│   ├── start-sim.sh             # ⭐ Start socat + simulator
│   ├── start-webapp.sh          # ⭐ Start webapp med GUI
│   ├── start-headless.sh        # Start headless app
│   ├── start-socat-sim.sh       # Gammel: Alt-i-ett
│   └── QUICKSTART.md            # Denne filen
│
└── /tmp/
    ├── ttyV0                    # Simulator PTY
    └── ttyV1                    # App PTY
```

---

## ❓ Feilsøking

### "socat er ikke installert"
```bash
brew install socat
```

### "JAR ikke funnet"
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn -q -DskipTests package
```

### "PTY ikke funnet"
Start `start-sim.sh` først, deretter `start-headless.sh`.

### "Permission denied" på serial port
```bash
# macOS: Ikke nødvendig for /tmp/ttyV*
# Linux: sudo usermod -a -G dialout $USER
```

### Database-feil
Start PostgreSQL:
```bash
docker-compose -f docker-compose-local.yaml up -d
```

---

## 📚 Mer Info

- [README.md](./README.md) - Full scripts-dokumentasjon
- [README_HEADLESS.md](../lpg-ehl-app-headless/README_HEADLESS.md) - Headless app docs
- [WARP.md](../lpg-ehl-core/WARP.md) - Prosjektoversikt
