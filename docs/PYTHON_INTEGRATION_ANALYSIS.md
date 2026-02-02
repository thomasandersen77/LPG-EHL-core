# Python Test Scripts - Fullstendig Integrasjonsanalyse

**Dato:** 2026-01-29  
**Analysert av:** Warp AI Agent  
**Formål:** Grundig analyse av hvordan Python test-scripts passer sammen med Kotlin Core-modul, Transport-lag, Webapp og Headless-app

---

## 📋 Innholdsfortegnelse

1. [Executive Summary](#executive-summary)
2. [Python-moduler Oversikt](#python-moduler-oversikt)
3. [Protokoll-kompatibilitet](#protokoll-kompatibilitet)
4. [Transport-lag Sammenligning](#transport-lag-sammenligning)
5. [Integrasjonspunkter](#integrasjonspunkter)
6. [Testing-strategi](#testing-strategi)
7. [Funn og Anbefalinger](#funn-og-anbefalinger)
8. [Konklusjon](#konklusjon)

---

## 1. Executive Summary

### 🎯 Hovedfunn

**Formål med Python-scripts:**
- ✅ Felt-testing av EKTE hardware (ikke emulator)
- ✅ Validering av at Kotlin-implementasjonen fungerer korrekt
- ✅ Minimale avhengigheter (ingen `pyserial`, kun stdlib)
- ✅ Debian 32-bit kompatibel for edge-enheter

**Status:**
- ✅ **Protokoll**: Python og Kotlin er 100% kompatible (EHL framing, checksum, STX/ETX)
- ⚠️ **Transport**: PARITY MISMATCH oppdaget (8N1 vs 8E1)
- ✅ **Testing**: Python validerer at simulatoren fungerer korrekt
- ⚠️ **Produksjon**: Kotlin webapp kan ha problemer med ekte hardware pga. parity

### 🔍 Kritisk Oppdagelse

| Komponent | Parity Setting | Kommentar |
|-----------|---------------|-----------|
| **Python Scripts** | 8N1 (No parity) | ✅ Matcher simulator |
| **Kotlin Simulator** | 8N1 (No parity) | ✅ SerialPortHandler.kt linje 47 |
| **Kotlin Webapp** | 8E1 (Even parity) | ⚠️ RealSerialTransport.kt linje 39 |
| **Ekte Hardware** | Ukjent (sannsynligvis 8N1) | ❓ Må verifiseres i felt |

**Konsekvens:**
- Python ↔ Simulator: **Fungerer perfekt** ✅
- Kotlin Webapp ↔ Simulator: **Vil FEILE** ved default config ⚠️
- Kotlin Webapp ↔ Ekte Hardware: **Avhenger av hardware-konfig** ❓

---

## 2. Python-moduler Oversikt

### 📂 Filstruktur

```
python-test/
├── README.md                    # Brukerdokumentasjon
├── logging_utils.py             # Enkel logging (stdout)
├── serial_linux.py              # Low-level serial I/O (termios)
├── ehl_protocol.py              # EHL framing og checksum
├── 01_probe_readonly.py         # Read-only kommandoer
├── 02_scan_addresses.py         # Scan adresser 1-32
├── 03_control_unblock_block.py  # FARLIG: Send UNBLOCK/BLOCK
└── 04_listen_only.py            # Passive sniffer
```

### 🎯 Bruksscenarier

#### Scenario 1: Initial Verifikasjon
```bash
# Finn serial port
python3 00_list_ports.py

# Test kommunikasjon
python3 01_probe_readonly.py --port /dev/ttyUSB0 --addr 1 --debug
```

**Formål:** Verifiser at hardware svarer med gyldige EHL-pakker

#### Scenario 2: Adresse-scanning
```bash
python3 02_scan_addresses.py --port /dev/ttyUSB0 --addr-range 1-32
```

**Formål:** Finn ukjent dispenser-adresse

#### Scenario 3: Passiv Lytting
```bash
python3 04_listen_only.py --port /dev/ttyUSB0
```

**Formål:** Debugg eksisterende kommunikasjon (VB6-app eller annen kontroller)

#### Scenario 4: Aktiv Kontroll (FARLIG)
```bash
python3 03_control_unblock_block.py --port /dev/ttyUSB0 --addr 1 unblock \
    --i-understand-this-can-affect-real-hardware
```

**Formål:** Test UNBLOCK/BLOCK kommandoer (kan starte pumping!)

---

## 3. Protokoll-kompatibilitet

### 🔬 EHL Frame Format

#### Python Implementation (`ehl_protocol.py`)

```python
STX_CONTROLLER = 0x10  # Controller → Dispenser
STX_DISPENSER = 0x20   # Dispenser → Controller
ETX = 0x36

Frame: STX + LEN + ADDR + CMD + DATA + CHK + ETX
Checksum: XOR(STX + LEN + ADDR + CMD + DATA)
```

#### Kotlin Implementation (`EhlCodec.kt`)

```kotlin
// EhlProtocolConfig (default)
stxController = 0x10.toByte()
stxDispenser = 0x20.toByte()
etx = 0x36.toByte()

// EhlCodec.encode() / decode()
Frame: STX + LEN + ADDR + CMD + DATA + CHK + ETX
Checksum: XOR(STX + LEN + ADDR + CMD + DATA)
```

### ✅ Kompatibilitet: 100%

**Testet med:**
- STATE (0x4B)
- ERROR_QUERY (0x4C)
- VOLUME (0x45)
- TANKBIT (0xC5)
- UNBLOCK (0x77)
- BLOCK (0x69)

**Resultat:** Python og Kotlin bygger IDENTISKE pakker.

### 📊 Eksempel: STATE Command

**Python generert:**
```
10 06 01 4B 5C 36
```

**Kotlin generert:**
```
10 06 01 4B 5C 36
```

**Forklaring:**
- `10` = STX_CONTROLLER
- `06` = LEN (6 bytes totalt)
- `01` = ADDR (adresse 1)
- `4B` = CMD (STATE)
- `5C` = CHK (0x10 ^ 0x06 ^ 0x01 ^ 0x4B)
- `36` = ETX

---

## 4. Transport-lag Sammenligning

### 🔌 Python: `serial_linux.py`

**Teknologi:**
- Native Linux `termios` API
- `os.open()` med `O_RDWR | O_NOCTTY | O_NONBLOCK`
- `select.select()` for non-blocking reads
- `termios.tcdrain()` for write completion

**Konfigurasjon:**
```python
# serial_linux.py linje 96-137
def _set_raw_8n1(fd: int, baud: int):
    attrs = termios.tcgetattr(fd)
    
    # 8N1: 8 data bits, NO parity, 1 stop bit
    attrs[2] &= ~(termios.CSIZE | termios.PARENB | termios.CSTOPB)
    attrs[2] |= termios.CS8 | termios.CREAD | termios.CLOCAL
    
    # Non-blocking
    attrs[6][termios.VMIN] = 0
    attrs[6][termios.VTIME] = 0
```

**Nøkkelpunkter:**
- ✅ 8N1 (no parity)
- ✅ Non-blocking reads
- ✅ `select()` med timeout
- ✅ Ingen eksterne avhengigheter

### 🔌 Kotlin: `SerialPortManager.kt`

**Teknologi:**
- `jSerialComm` library (cross-platform)
- Blocking reads med timeout
- Retry-logikk ved partial writes
- Hardware watchdog for reconnect

**Konfigurasjon:**
```kotlin
// SerialPortConfig (default for EHL)
port.baudRate = 9600
port.numDataBits = 8
port.numStopBits = SerialPort.ONE_STOP_BIT
port.parity = SerialPort.EVEN_PARITY  // ⚠️ 8E1!
```

**Nøkkelpunkter:**
- ⚠️ 8E1 (even parity) - DEFAULT
- ✅ Blocking reads (3000ms timeout)
- ✅ Write retry (up to 3x)
- ✅ Hardware watchdog

### 🔌 Kotlin Simulator: `SerialPortHandler.kt`

**Konfigurasjon:**
```kotlin
// SerialPortHandler.kt linje 43-47
port.baudRate = baud
port.numDataBits = 8
port.numStopBits = SerialPort.ONE_STOP_BIT
port.parity = SerialPort.NO_PARITY  // ✅ 8N1!
```

**Nøkkelpunkter:**
- ✅ 8N1 (no parity) - Matcher Python!
- ✅ 100ms read timeout
- ✅ Binary EHL protocol

### 📊 Sammenligning

| Aspekt | Python | Kotlin Webapp | Kotlin Simulator |
|--------|--------|---------------|------------------|
| **Parity** | 8N1 ✅ | 8E1 ⚠️ | 8N1 ✅ |
| **Baud** | 9600 | 9600 | 9600 |
| **Read Strategy** | select() + non-blocking | Blocking (3000ms) | Blocking (100ms) |
| **Write Strategy** | write() + tcdrain() | Retry (3x) | Simple write |
| **Timeout** | 800ms (default) | 3000ms read, 1000ms write | 100ms |
| **Reconnect** | Nei | Ja (watchdog) | Nei |

---

## 5. Integrasjonspunkter

### 🔄 Hvordan Python brukes sammen med Kotlin-systemet

#### Integrasjon 1: Validering av Simulator

```
┌─────────────────┐         ┌──────────────────┐
│  Python Script  │ ◄─────► │ socat (virtual)  │
│  (8N1)          │  vser1  │  /tmp/vserial0/1 │
└─────────────────┘         └──────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ Kotlin Simulator │
                            │ (8N1)            │
                            │ PlsSimulator.kt  │
                            └──────────────────┘
```

**Status:** ✅ **FUNGERER PERFEKT**

**Test:**
```bash
# Terminal 1
./scripts/start-socat-sim.sh

# Terminal 2
python3 python-test/01_probe_readonly.py --port /tmp/vserial1 --addr 1 --debug
```

**Forventet Resultat:**
```
TX 10 06 01 4B 5C 36
⬅️  RX EHL: addr=1 cmd=STATE (0x4B) dataLen=4
RX STX=0x20 LEN=10 ADDR=1 CMD=0x4B(STATE) DATA=...
```

#### Integrasjon 2: Validering av Webapp (med Simulator)

```
┌─────────────────┐         ┌──────────────────┐
│ Kotlin Webapp   │ ◄─────► │ socat (virtual)  │
│ (8E1) ⚠️       │  vser1  │  /tmp/vserial0/1 │
└─────────────────┘         └──────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ Kotlin Simulator │
                            │ (8N1)            │
                            └──────────────────┘
```

**Status:** ⚠️ **PARITY MISMATCH**

**Problem:** Webapp sender med even parity (8E1), men simulator forventer no parity (8N1).

**Løsning:** Overstyr webapp config:
```yaml
# application-h2.yaml
ehl:
  serial:
    port: /tmp/vserial1
    parity: NO_PARITY  # ← Overstyr default (EVEN_PARITY)
```

#### Integrasjon 3: Felt-testing med Ekte Hardware

```
┌─────────────────┐         ┌──────────────────┐
│  Python Script  │ ◄─────► │ RS-485 Adapter   │
│  (8N1)          │ USB     │  /dev/ttyUSB0    │
└─────────────────┘         └──────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ Ekte Dispenser   │
                            │ (8N1 eller 8E1?) │
                            └──────────────────┘
```

**Status:** ❓ **UKJENT** (avhenger av hardware-konfig)

**Testing:**
1. Test først med Python (8N1)
2. Hvis Python fungerer: Hardware bruker sannsynligvis 8N1
3. Konfigurer Kotlin webapp til 8N1
4. Hvis Python IKKE fungerer: Test 8E1 via `--rs485` flagg

#### Integrasjon 4: Headless App i Felt

```
┌─────────────────┐         ┌──────────────────┐
│ Headless App    │ ◄─────► │ RS-485 Adapter   │
│ (8E1 default)   │         │  /dev/ttyS0      │
└─────────────────┘         └──────────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ Ekte Dispenser   │
                            │ (VB6-kompatibel) │
                            └──────────────────┘
```

**Bruk Python for Pre-flight Check:**
```bash
# Før du starter headless app
python3 01_probe_readonly.py --port /dev/ttyS0 --addr 1

# Hvis Python fungerer:
# → Hardware bruker sannsynligvis 8N1
# → Konfigurer headless til NO_PARITY
```

---

## 6. Testing-strategi

### 🧪 Testing Workflow

#### Fase 1: Simulator Validering (LAB)

**Mål:** Verifiser at simulator oppfører seg som ekte hardware

```bash
# 1. Start simulator
./scripts/start-socat-sim.sh

# 2. Test med Python
cd python-test
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1 --debug

# 3. Test med Webapp
java -jar release/lpg-ehl-webapp.jar \
    --lpg.mode=FIELD \
    --ehl.serial.port=/tmp/vserial1 \
    --ehl.serial.parity=NO_PARITY
```

**Forventet Resultat:**
- ✅ Python får respons fra simulator
- ✅ Webapp får respons fra simulator
- ✅ Begge ser identiske DATA-bytes

#### Fase 2: Hardware Pre-flight (FIELD)

**Mål:** Verifiser kommunikasjon før produksjon

```bash
# 1. Finn serial port
python3 00_list_ports.py

# 2. Test read-only kommandoer
python3 01_probe_readonly.py --port /dev/ttyUSB0 --addr 1 --debug

# 3. Hvis ukjent adresse
python3 02_scan_addresses.py --port /dev/ttyUSB0 --addr-range 1-32
```

**Forventet Resultat:**
- ✅ Python får gyldige responser
- ✅ Checksum validerer OK
- ✅ STATE viser BLOCKED (eller IDLE)

#### Fase 3: Produksjon Deploy (FIELD)

**Mål:** Start headless app med korrekt konfig

```bash
# 1. Basert på Python-testing, konfigurer parity
# Hvis Python (8N1) fungerte:
cat > application-field.yaml << EOF
ehl:
  serial:
    port: /dev/ttyS0
    parity: NO_PARITY
EOF

# 2. Start headless
java -jar lpg-ehl-app-headless.jar \
    --spring.config.location=file:./application-field.yaml
```

#### Fase 4: Kontinuerlig Validering

**Mål:** Periodisk test av kommunikasjon

```bash
# Cron job (hver time)
*/60 * * * * python3 /opt/lpg/python-test/01_probe_readonly.py \
    --port /dev/ttyS0 --addr 1 >> /var/log/lpg-health.log 2>&1
```

### 📊 Test Matrices

#### Test Case 1: Python vs Kotlin Protocol

| Test | Python | Kotlin | Status |
|------|--------|--------|--------|
| Build STATE frame | `10 06 01 4B 5C 36` | `10 06 01 4B 5C 36` | ✅ Identical |
| Parse STATE response | 4 data bytes | 4 data bytes | ✅ Identical |
| Checksum validation | XOR algorithm | XOR algorithm | ✅ Identical |
| Frame extraction | Handles partial frames | Handles partial frames | ✅ Compatible |

#### Test Case 2: Transport Compatibility

| Configuration | Python → Simulator | Webapp → Simulator | Webapp → Hardware |
|---------------|--------------------|--------------------|-------------------|
| Python 8N1 + Sim 8N1 | ✅ Works | N/A | N/A |
| Webapp 8E1 + Sim 8N1 | N/A | ⚠️ Fails | ❓ Unknown |
| Webapp 8N1 + Sim 8N1 | N/A | ✅ Works (with config) | ❓ Depends |

---

## 7. Funn og Anbefalinger

### 🔍 Kritiske Funn

#### Funn 1: Parity Mismatch ⚠️

**Problem:**
- Python scripts bruker 8N1 (no parity)
- Kotlin simulator bruker 8N1 (no parity)
- Kotlin webapp bruker 8E1 (even parity) som DEFAULT

**Konsekvens:**
```kotlin
// RealSerialTransport.kt linje 39
private val parity: Int = SerialPort.EVEN_PARITY
```

**Dette gjør at:**
- ✅ Python ↔ Simulator: Fungerer
- ⚠️ Webapp ↔ Simulator: FEILER ved default konfig
- ❓ Webapp ↔ Hardware: Ukjent (avhenger av hardware)

**Anbefaling:**
1. **Kort sikt:** Dokumenter at parity MÅ konfigureres
2. **Mellomlang sikt:** Auto-detect parity ved oppstart
3. **Lang sikt:** Standardiser på 8N1 (hvis hardware tillater)

#### Funn 2: Timeout Forskjeller

**Python:** 800ms default, konfigurerbar via `--timeout-ms`
**Kotlin:** 3000ms read timeout (hardkodet)

**Observasjon:** Kotlin er mer tolerant for trege dispensere.

**Anbefaling:** OK som-er. Python er for testing, ikke produksjon.

#### Funn 3: Ingen Automated Tests

**Problem:** Python scripts kjøres manuelt, ikke i CI/CD.

**Anbefaling:**
```yaml
# .github/workflows/integration-test.yml
- name: Test Python ↔ Simulator
  run: |
    ./scripts/start-socat-sim.sh &
    sleep 2
    python3 python-test/01_probe_readonly.py \
      --port /tmp/vserial1 --addr 1 --timeout-ms 2000
```

#### Funn 4: RS-485 Mode Ikke Håndtert

**Python:** Støtter `--rs485` flagg for Linux ioctl
**Kotlin:** Ingen eksplisitt RS-485 mode konfigurasjon

**Anbefaling:** Legg til RS-485 støtte i `SerialPortManager`:
```kotlin
if (config.rs485Mode) {
    // Bruk JNA eller ioctl wrapper for TIOCSRS485
    enableRs485Mode(serialPort, config.rtsDelayMs)
}
```

### ✅ Positive Funn

1. **Protokoll 100% Kompatibel** - Python og Kotlin genererer identiske pakker
2. **Minimal Dependencies** - Python krever kun stdlib (perfekt for edge)
3. **Debugging Tools** - Listen-only mode er uvurderlig for troubleshooting
4. **Safety First** - Control commands krever eksplisitt ack-flagg

---

## 8. Konklusjon

### 📝 Oppsummering

**Python test-scripts er:**
- ✅ Essensielle for felt-validering før produksjon
- ✅ 100% protokoll-kompatible med Kotlin
- ⚠️ Avslører parity mismatch mellom webapp og simulator
- ✅ Minimale avhengigheter (perfekt for Debian 32-bit edge)

### 🎯 Bruksområder

| Scenario | Tool | Formål |
|----------|------|--------|
| **Initial Setup** | Python `00_list_ports.py` | Finn serial port |
| **Pre-flight Check** | Python `01_probe_readonly.py` | Verifiser hardware |
| **Address Discovery** | Python `02_scan_addresses.py` | Finn ukjent adresse |
| **Debugging** | Python `04_listen_only.py` | Sniff VB6-kommunikasjon |
| **Development** | Kotlin Webapp + Simulator | LAB mode testing |
| **Production** | Kotlin Headless + Hardware | FIELD mode |

### 🔧 Action Items

**Høy Prioritet:**
1. ⚠️ Fiks parity mismatch: Enten standardiser på 8N1, eller auto-detect
2. 📝 Dokumenter at webapp må konfigureres med `parity: NO_PARITY` for simulator
3. 🧪 Automatiser Python-tests i CI/CD

**Middels Prioritet:**
4. 🔌 Legg til RS-485 mode i `SerialPortManager`
5. 📊 Lag metrics for Python test-resultater (Prometheus?)
6. 🐛 Legg til retry-logikk i Python scripts (match Kotlin)

**Lav Prioritet:**
7. 🐍 Lag Python wrapper for `jSerialComm` (unify transport layer)
8. 📦 Pakk Python scripts som `.deb` for enkel deploy
9. 🎯 Lag GUI for Python scripts (Tk eller curses)

---

## 📚 Referanser

### Filer Analysert

**Python:**
- `python-test/serial_linux.py` - 198 linjer
- `python-test/ehl_protocol.py` - 174 linjer
- `python-test/01_probe_readonly.py` - 117 linjer
- `python-test/logging_utils.py` - 38 linjer

**Kotlin:**
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/protocol/EhlCodec.kt`
- `lpg-transport/src/main/kotlin/no/cloudberries/lpg/communication/SerialPortManager.kt`
- `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/SerialPortHandler.kt`

### Relaterte Dokumenter

- `README.md` - Prosjekt oversikt
- `docs/ARCHITECTURE.md` - System design
- `docs/SERIAL_CONTRACT.md` - Transport layer spec
- `python-test/README.md` - Python brukerdokumentasjon

---

**Rapport Avsluttet: 2026-01-29**

For spørsmål eller oppfølging, se:
- [Python Test Scripts README](../python-test/README.md)
- [Serial Contract](SERIAL_CONTRACT.md)
- [Architecture Overview](ARCHITECTURE.md)
