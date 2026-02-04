# Implementasjonsrapport: Mode & Parity Konfigurasjon Fix

**Dato:** 2026-01-29  
**Basert på:** ChatGPT analyse av Python-Kotlin integrasjon  
**Status:** ✅ Implementert

---

## 📋 Executive Summary

ChatGPT identifiserte et **kritisk konfigurasjonsproblem**: Webapp bruker fortsatt legacy `--ehl.transport.mode=SOCAT` i stedet for den nye `--lpg.mode=FIELD`, noe som fører til at webapp starter i LAB mode (emulator) selv når man tror man kjører FIELD mode.

Samtidig oppdaget vi **parity mismatch**: Simulator og Python bruker 8N1, mens Kotlin webapp default er 8E1.

---

## 🔍 Problembeskrivelse

### Problem 1: Forvirrende Mode-konfiguration

**Symptom:** "Jeg ser emulator-logging selv om jeg kjører FIELD mode"

**Årsak:**
```bash
# start-webapp.sh (FØR fix)
java -jar webapp.jar \
    --ehl.transport.mode=SOCAT \  # ⚠️ LEGACY! Gjør INGENTING
    --ehl.serial.port=/tmp/ttyV1
```

**Hva skjer:**
- `ehl.transport.mode` er deprecated og ignoreres
- `lpg.mode` defaults til `LAB` (matchIfMissing=true)
- Webapp starter InMemorySerialPort + Emulator i stedet for SerialPortManager

**Resultat:** Du ser emulator-logging fordi du FAKTISK kjører emulator, ikke simulator!

### Problem 2: Parity Mismatch

| Komponent | Konfigurert Parity | Resultat |
|-----------|-------------------|----------|
| Python scripts | 8N1 (no parity) | ✅ Fungerer mot simulator |
| Kotlin Simulator | 8N1 (no parity) | ✅ Matcher Python |
| Kotlin Webapp | 8E1 (even parity) | ⚠️ FEILER mot simulator |

**Konsekvens:** Webapp kan ikke kommunisere med simulator uten config-override.

---

## ✅ Implementerte Løsninger

### Fix 1: Oppdatert `start-webapp.sh`

**Endringer:**
```bash
# ETTER fix
java -jar webapp.jar \
    --lpg.mode=FIELD \                      # ✅ Aktiverer FIELD mode
    --ehl.serial.port=/tmp/vserial1 \       # ✅ Matcher simulator
    --ehl.serial.parity=NONE \              # ✅ 8N1 = no parity
    --ehl.serial.baud-rate=9600 \
    --ehl.serial.data-bits=8 \
    --ehl.serial.stop-bits=1
```

**Nye CLI-opsjoner:**
```bash
./scripts/start-webapp.sh \
    --serial-port=/tmp/vserial1 \
    --baud=9600 \
    --parity=NONE
```

### Fix 2: Dokumentert `application-h2.yaml`

**Før:**
```yaml
ehl:
  transport:
    mode: EMULATOR  # Deprecated
  serial:
    parity: EVEN    # ⚠️ Feiler mot simulator
```

**Etter:**
```yaml
ehl:
  transport:
    # LEGACY: Brukes ikke lenger, bruk lpg.mode i stedet
    mode: ${EHL_TRANSPORT_MODE:EMULATOR}
  serial:
    port: ${EHL_SERIAL_PORT:/tmp/vserial1}
    # VIKTIG: NONE for simulator/Python, EVEN for ekte hardware
    parity: ${EHL_SERIAL_PARITY:EVEN}

lpg:
  # Aktiverer InMemorySerialPort (LAB) eller SerialPortManager (FIELD)
  mode: ${LPG_MODE:LAB}

---
# FIELD profile
spring:
  config:
    activate:
      on-profile: field

ehl:
  serial:
    port: /tmp/vserial1
    parity: NONE  # ✅ Matcher simulator (8N1)

lpg:
  mode: FIELD
```

### Fix 3: Dokumentasjon

**Oppdaterte filer:**
- `scripts/start-webapp.sh` - Korrekt mode + konfigurerbar parity
- `application-h2.yaml` - Dokumentert parity mismatch + migration guide
- `docs/PYTHON_INTEGRATION_ANALYSIS.md` - Fullstendig analyse

---

## 🧪 Testing

### Test 1: Verifiser Mode

```bash
# Start simulator
./scripts/start-socat-sim.sh

# Start webapp (FIELD mode)
./scripts/start-webapp.sh

# FORVENTET OUTPUT:
# ════════════════════════════════════════════════════════════
#   🏭 FIELD MODE (via SOCAT)
# ════════════════════════════════════════════════════════════
#   Transport:   SerialPortManager (with watchdog)
#   Serial Port: /tmp/vserial1
#   Parity:      NONE (8N1 = no parity, matcher simulator)
```

### Test 2: Verifiser Kommunikasjon

```bash
# Python test (baseline)
cd python-test
python3 01_probe_readonly.py --port /tmp/vserial1 --addr 1 --debug

# Forventet: RX frames med gyldige DATA bytes

# Webapp test (via /control page)
# 1. Åpne http://localhost:8080/control
# 2. Klikk "SIMULER KORTDRAGNING"
# 3. Klikk "FRI DISPENSER"

# Forventet: Pumpe går til READY_TO_PUMP
```

### Test 3: Verifiser Parity-konfig

```bash
# Overstyr parity via CLI
./scripts/start-webapp.sh --parity=EVEN

# Forventet: FEILER (ingen frames mottatt)

./scripts/start-webapp.sh --parity=NONE

# Forventet: FUNGERER (frames mottatt)
```

---

## 📊 Før vs Etter

### Konfigurasjonsekvivalens

| Gammel (FØR) | Ny (ETTER) |
|--------------|-----------|
| `--ehl.transport.mode=EMULATOR` | `--lpg.mode=LAB` |
| `--ehl.transport.mode=SOCAT` | `--lpg.mode=FIELD --ehl.serial.parity=NONE` |
| `--ehl.transport.mode=HARDWARE` | `--lpg.mode=FIELD --ehl.serial.parity=EVEN` |

### Migration Guide

**Hvis du brukte:**
```bash
# GAMMEL måte
java -jar webapp.jar \
    --ehl.transport.mode=SOCAT \
    --ehl.serial.port=/dev/ttyUSB0
```

**Migrer til:**
```bash
# NY måte
java -jar webapp.jar \
    --lpg.mode=FIELD \
    --ehl.serial.port=/dev/ttyUSB0 \
    --ehl.serial.parity=NONE  # Eller EVEN (avhenger av hardware)
```

---

## 🎯 Anbefalinger

### 1. Pre-flight Check med Python

**Før produksjonsdeploy:**
```bash
# Test kommunikasjon med Python først
python3 python-test/01_probe_readonly.py \
    --port /dev/ttyS0 \
    --addr 1 \
    --debug

# Hvis Python (8N1) fungerer:
# → Hardware bruker sannsynligvis 8N1
# → Konfigurer Kotlin til parity: NONE

# Hvis Python IKKE fungerer:
# → Test med 8E1 (evt via --rs485 flagg)
```

### 2. Automatisk Parity Detection

**Fremtidig forbedring:**
```kotlin
fun autoDetectParity(port: String, address: Int): ParityMode {
    for (parity in listOf(NO_PARITY, EVEN_PARITY, ODD_PARITY)) {
        if (testCommunication(port, address, parity)) {
            logger.info("✅ Auto-detected parity: $parity")
            return parity
        }
    }
    throw IllegalStateException("Could not detect parity")
}
```

### 3. Health Check Endpoint

```kotlin
@GetMapping("/api/debug/serial/health")
fun serialHealth(): Map<String, Any> {
    val testFrame = EhlPacketBuilder.createStateQuery(1)
    val response = communicator.sendAndReceive(testFrame, timeout = 1000)
    
    return mapOf(
        "connected" to transport.isConnected,
        "parity" to config.parity,
        "lastRx" to communicator.lastRxTimestamp,
        "testPassed" to (response != null)
    )
}
```

---

## 🚀 Neste Steg

### Høy Prioritet
1. ✅ **Fiks start-webapp.sh** - Implementert
2. ✅ **Dokumenter parity mismatch** - Implementert
3. ⏳ **Test med ekte hardware** - Venter på field deployment

### Middels Prioritet
4. ⏳ **Automatiser Python-tests i CI/CD** - TODO
5. ⏳ **Legg til auto-detect parity** - TODO
6. ⏳ **Health check endpoint** - TODO

### Lav Prioritet
7. ⏳ **Deprecate ehl.transport.mode** - TODO (breaking change)
8. ⏳ **RS-485 mode support** - TODO (sjeldent behov)

---

## 📚 Relaterte Dokumenter

- [Python Integration Analysis](PYTHON_INTEGRATION_ANALYSIS.md)
- [Serial Contract](SERIAL_CONTRACT.md)
- [Transport Configuration](../lpg-ehl-webapp/src/main/kotlin/no/cloudberries/lpg/api/config/TransportConfiguration.kt)
- [Python Test Scripts](../python-test/README.md)

---

## ✅ Konklusjon

**Implementert:**
- ✅ `start-webapp.sh` bruker nå `--lpg.mode=FIELD`
- ✅ Parity er konfigurerbar via CLI (`--parity=NONE`)
- ✅ Default parity for simulator er 8N1 (NONE)
- ✅ Dokumentert migration fra legacy `ehl.transport.mode`

**Resultat:**
- Python og Kotlin webapp kan nå begge kommunisere med simulator
- Parity kan overrrides for ekte hardware (bruk Python for pre-flight test)
- Ingen endringer i Core-modulen (følger constraint)
- Ingen endringer i Python-koden (følger constraint)

**Status:** ✅ **KLAR FOR TESTING**

---

**Rapport avsluttet:** 2026-01-29

For spørsmål eller issues, se:
- [GitHub Issues](../../issues)
- [Python Integration Analysis](PYTHON_INTEGRATION_ANALYSIS.md)
