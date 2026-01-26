# Changelog: H2 Database + Field Mode Support

**Dato:** 2026-01-25  
**Branch:** `refactor/serialtransport_master`

## Oversikt

Denne oppdateringen introduserer støtte for:
1. **Ekstern konfigurasjon** via `application-h2.yaml` for testing på ressursbegrensede industrielle maskiner
2. **Field Mode** med ekte seriell transport via SOCAT
3. **Simulator-fix** for real-time GUI-oppdateringer av volum/beløp

---

## 1. Ekstern Konfigurasjon (`application-h2.yaml`)

### Hvorfor ekstern konfigurasjon?

Ved testing på ARM-baserte industrielle maskiner (f.eks. Raspberry Pi) med begrensede ressurser er det kritisk å kunne:
- Endre konfigurasjon uten å bygge JAR på nytt
- Bytte mellom database-backends (H2 for testing, PostgreSQL for produksjon)
- Justere seriellport-parametere under iterativ testing

### Filplassering

```
lpg-ehl/
├── application-h2.yaml          # ← Ekstern konfigurasjon (utenfor JAR)
├── release/
│   ├── lpg-ehl-webapp.jar
│   ├── lpg-ehl-headless.jar
│   └── pls-sim.jar
```

### Bruksmønstre

```bash
# H2 + Emulator (in-memory, ingen hardware)
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2

# H2 + Field Mode (ekte seriell via SOCAT)
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,field
```

### Konfigurerbare parametere

| Parameter | Beskrivelse | Default |
|-----------|-------------|---------|
| `ehl.transport.mode` | EMULATOR, SOCAT, HARDWARE | EMULATOR |
| `ehl.serial.port` | Seriellport-sti | /tmp/ttyV1 |
| `ehl.serial.baud-rate` | Baudrate | 9600 |
| `lpg.mode` | LAB eller FIELD | LAB |
| `security.api-token` | API-autentisering | dev-token-12345 |

---

## 2. Field Mode med SOCAT Transport

### Arkitektur

```
┌─────────────────┐     SOCAT PTY      ┌─────────────────┐
│   PLS Simulator │◄──────────────────►│  Webapp/Headless│
│   (pls-sim.jar) │  /tmp/ttyV0 ↔ V1   │  (lpg-ehl-*.jar)│
└─────────────────┘                    └─────────────────┘
        ↓                                      ↓
   EHL Protokoll                         H2 Database
   0x10 0x06 0x01...                    (in-memory)
```

### Profil-kombinasjoner

| Profiler | Database | Transport | Use Case |
|----------|----------|-----------|----------|
| `h2` | H2 in-memory | Emulator | Utvikling/demo |
| `h2,field` | H2 in-memory | SOCAT/Serial | Integrasjonstest |
| `prod` | PostgreSQL | Hardware | Produksjon |

### Endringer i `application-h2.yaml`

```yaml
---
# FIELD PROFILE - Real hardware/SOCAT transport
spring:
  config:
    activate:
      on-profile: field

ehl:
  transport:
    mode: SOCAT
  serial:
    port: ${EHL_SERIAL_PORT:/tmp/ttyV1}

lpg:
  mode: FIELD
```

---

## 3. Simulator Real-Time Fix

### Problem

Når webapp kjørte med SOCAT + PLS Simulator, vistes ikke volum/beløp i GUI under pumping.

**Rotårsak:**
1. Simulator returnerte `STATE=0x04` (bare DELIVERY_ACTIVE)
2. Webapp forventet `STATE=0x06` (DELIVERY_ACTIVE + NOZZLE_LIFTED)
3. Uten match skjedde ingen auto-deteksjon av pumping
4. `pollVolume()` kjørte aldri → ingen WebSocket-broadcast

### Løsning

**Fil:** `lpg-ehl-serialport-sim/src/main/kotlin/no/cloudberries/lpg/pls/sim/PlsState.kt`

```kotlin
// FØR (feil):
val statusByte: Byte = if (blocked) 0x00 else 0x04

// ETTER (korrekt):
val statusByte: Byte = when {
    blocked -> 0x00                    // IDLE
    volumeMl > 0 -> 0x06               // PUMPING (0x04 + 0x02)
    else -> 0x04                       // READY
}
```

### STATE Bit-maske

| Verdi | Bits | Betydning |
|-------|------|-----------|
| 0x00 | 0000 0000 | IDLE (blokkert) |
| 0x04 | 0000 0100 | READY (frigjort, volum=0) |
| 0x06 | 0000 0110 | PUMPING (frigjort, volum>0) |

---

## 4. SecurityConfig Default Values

### Problem

`SecurityConfig` feilet ved oppstart når `security.api-token` ikke var definert i aktiv profil.

### Løsning

**Fil:** `lpg-ehl-webapp/src/main/kotlin/no/cloudberries/lpg/api/config/SecurityConfig.kt`

```kotlin
// FØR:
@Value("\${security.api-token}") private val apiToken: String

// ETTER:
@Value("\${security.api-token:dev-token-12345}") private val apiToken: String
```

---

## 5. Testprosedyre

### Start SOCAT + Simulator

```bash
# Terminal 1
./scripts/start-sim.sh
```

Output:
```
═══════════════════════════════════════════════
  START SOCAT + SIMULATOR
═══════════════════════════════════════════════
[INFO] ✅ socat kjører (PID: 12345)
[INFO] ✅ Simulator kjører (PID: 12346)

  Simulator:  /tmp/ttyV0
  App-port:   /tmp/ttyV1
```

### Start Webapp med H2 + Field

```bash
# Terminal 2
java -jar release/lpg-ehl-webapp.jar \
  --spring.config.location=file:./application-h2.yaml \
  --spring.profiles.active=h2,field
```

Output:
```
════════════════════════════════════════════════════════════
  🔗 SOCAT MODE (with watchdog)
════════════════════════════════════════════════════════════
  Transport:  SerialPortManager
  Serial Port: /tmp/ttyV1
  Baud Rate:  9600
```

### Verifiser Real-Time Updates

1. Åpne http://localhost:8080
2. Klikk "Simuler Kortdragning"
3. Klikk "FRI DISPENSER"
4. **Forventet:** Volum og beløp telles opp i sanntid
5. Klikk "STOPP" for å avslutte

---

## 6. Filendringer

### Modifiserte filer

| Fil | Endring |
|-----|---------|
| `application-h2.yaml` | Lagt til `field` profil for SOCAT transport |
| `SecurityConfig.kt` | Default values for @Value annotations |
| `PlsState.kt` | STATE-respons inkluderer NOZZLE_LIFTED ved pumping |

### Nye filer

| Fil | Formål |
|-----|--------|
| `scripts/run-h2-webapp.sh` | Launcher for H2 + webapp |
| `scripts/run-h2-headless.sh` | Launcher for H2 + headless |
| `scripts/start-socat.sh` | Standalone SOCAT PTY-pair |

---

## 7. Neste steg

- [ ] Test på Raspberry Pi med ekte RS-485 hardware
- [ ] Legg til PostgreSQL profil for produksjon
- [ ] Dokumenter multi-dispenser støtte
