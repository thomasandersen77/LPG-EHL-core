# Kjøring av LPG-EHL

## Forutsetninger

```bash
# Sett Java 21 med sdkman
sdk use java 21.0.7-tem

# Bygg prosjektet
mvn clean package -DskipTests

# JAR-filer havner i release/
```

---

## IntelliJ Run Configurations

Åpne **Run** → **Edit Configurations** eller bruk dropdown øverst til høyre.

### 🌐 Webapp (med GUI)

| Konfigurasjon | Beskrivelse | URL |
|--------------|-------------|-----|
| **Webapp + Emulator** | Webapp med in-memory emulator (ingen hardware) | http://localhost:8080 |
| **Webapp + Simulator** | Webapp med socat PTY (`/tmp/ttyV1`) | http://localhost:8080 |

### 🖥️ Headless (uten GUI)

| Konfigurasjon | Beskrivelse | Debug API |
|--------------|-------------|-----------|
| **Headless + Emulator** | Headless med in-memory emulator | http://localhost:8081 |
| **Headless + Simulator** | Headless med socat PTY (`/tmp/ttyV1`) | http://localhost:8081 |

### 🔌 Emulator TCP Server

| Konfigurasjon | Beskrivelse | Port |
|--------------|-------------|------|
| **Emulator TCP Server** | TCP server for Windows Dispenserkontroll | 9000 |

### 📦 JAR-kjøring

| Konfigurasjon | Beskrivelse |
|--------------|-------------|
| **JAR: Webapp H2** | Kjører `release/lpg-ehl-webapp.jar` med `application-h2.yaml` |
| **JAR: Headless H2** | Kjører `release/lpg-ehl-headless.jar` med `application-h2.yaml` |

---

## Kjøring fra Terminal

### Webapp med H2

```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml
```

### Headless med H2

```bash
java -jar release/lpg-ehl-headless.jar \
  --spring.config.location=file:./application-h2.yaml
```

### Webapp med Socat Simulator

```bash
# Terminal 1: Start PLS Simulator
java -jar release/pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true

# Terminal 2: Start Webapp
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --ehl.transport.mode=SOCAT \
  --ehl.serial.port=/tmp/ttyV1
```

### Override environment variables

```bash
# Endre port
SERVER_PORT=9090 java -jar release/lpg-ehl-webapp.jar ...

# Endre transport mode
EHL_TRANSPORT_MODE=HARDWARE EHL_SERIAL_PORT=/dev/ttyUSB0 \
  java -jar release/lpg-ehl-webapp.jar ...
```

---

## Transport Modes

| Mode | Beskrivelse | Krever |
|------|-------------|--------|
| `EMULATOR` | In-memory emulator, ingen hardware | Ingenting |
| `SOCAT` | Virtual PTY via socat | PLS Simulator kjørende |
| `HARDWARE` | Ekte seriell port | Fysisk dispenser tilkoblet |

---

## Socat Setup (macOS)

```bash
# Installer socat
brew install socat

# Opprett virtuelt PTY-par (kjør i egen terminal)
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1
```

- `/tmp/ttyV0` → PLS Simulator (dispenser)
- `/tmp/ttyV1` → Webapp/Headless (controller)

---

## Typiske Kombinasjoner

### 1. Rask testing (kun emulator)
- Kjør: **Webapp + Emulator**
- Alt kjører in-memory, ingen oppsett

### 2. Simulator testing (med PLS)
- Terminal 1: Kjør `socat` kommando
- IntelliJ: Kjør **PLS Simulator (EHL)**
- IntelliJ: Kjør **Webapp + Simulator**

### 3. Full test med Windows Dispenserkontroll
- IntelliJ: Kjør **Webapp + Emulator**
- IntelliJ: Kjør **Emulator TCP Server**
- Windows VM: Koble Dispenserkontroll til `[host-ip]:9000`
