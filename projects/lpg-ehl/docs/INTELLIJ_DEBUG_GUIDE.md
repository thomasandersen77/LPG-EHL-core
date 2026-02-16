# IntelliJ Debug Guide

## Overview
Dette prosjektet inkluderer ferdiglagde IntelliJ run configurations som gj\u00f8r det enkelt \u00e5 debugge webapp, headless, og simulator direkte fra IntelliJ IDEA.

## Importere Prosjektet
1. \u00c5pne IntelliJ IDEA
2. Velg **File > Open**
3. Naviger til `/path/to/lpg-ehl` og velg `pom.xml`
4. Velg **Open as Project**
5. Vent p\u00e5 at Maven importerer alle moduler

## Tilgjengelige Run Configurations
Run configurations ligger i `.run/` directory og laster automatisk i IntelliJ:

### Webapp
- **WebApp (LAB)**: Starter webapp i LAB mode med emulator (ingen hardware n\u00f8dvendig)
- **WebApp (FIELD - Auto-detect)**: Starter webapp i FIELD mode med auto-detect parity

### Headless
- **HeadlessApp (LAB)**: Starter headless app i LAB mode med emulator

### Simulator
- **PLS Simulator**: Starter PLS simulator p\u00e5 `/tmp/vserial1` med EHL protocol og 8N1 parity

## Typisk Workflow for Debugging

### 1. Debugging med Simulator (SOCAT)

#### Steg 1: Start SOCAT (Terminal)
```bash
./scripts/start-socat-sim.sh
```

Dette oppretter virtual serial port pair: `/tmp/vserial0` <-> `/tmp/vserial1`

#### Steg 2: Start PLS Simulator (IntelliJ)
1. Velg run configuration **"PLS Simulator"** fra dropdown
2. Klikk p\u00e5 debug-knappen (\ud83d\udc1e)
3. Simulatoren starter og lytter p\u00e5 `/tmp/vserial1`

#### Steg 3: Start Webapp (IntelliJ)
1. Velg run configuration **"WebApp (FIELD - Auto-detect)"**
2. Klikk p\u00e5 debug-knappen
3. Webapp starter og kobler til `/tmp/vserial1`
4. Auto-detect vil finne NONE parity (8N1)

#### Steg 4: Debugging
- Sett breakpoints i koden (f.eks. i `EhlCommunicator.kt`)
- \u00c5pne http://localhost:8080/control i nettleseren
- Utf\u00f8r operasjoner (f.eks. "Simuler kortdragning")
- Breakpoints vil treffe og du kan inspisere variabler

### 2. Debugging uten SOCAT (LAB Mode)

For rask testing uten simulator:

1. Velg **"WebApp (LAB)"** eller **"HeadlessApp (LAB)"**
2. Klikk debug
3. Alt kj\u00f8rer in-memory med emulator

Fordeler:
- \u2705 Ingen eksterne avhengigheter
- \u2705 Raskere oppstart
- \u2705 Perfekt for unit testing av business logic

Ulemper:
- \u274c Tester ikke ekte serial port kommunikasjon
- \u274c Ingen test av parity/timing issues

## Debugging Tips

### Breakpoints
Gode steder \u00e5 sette breakpoints:

**Protocol Level:**
- `EhlCommunicator.sendAndReceive()` - Se alle packets inn/ut
- `EhlCodec.encode()` / `decode()` - Inspiser byte-encoding

**Transport Level:**
- `SerialPortManager.connect()` - Se serial port oppstart
- `SerialParityAutoDetector.autoDetectParity()` - Debug auto-detect

**Business Logic:**
- `TransportConfiguration.fieldModeTransport()` - Se konfigurasjon
- Service layer-metoder

### Logging
Juster log-nivå i IntelliJ:

1. Edit run configuration
2. Environment variables:
   - `LOGGING_LEVEL_NO_CLOUDBERRIES_LPG=DEBUG` - Verbose logging
   - `LOGGING_LEVEL_ROOT=INFO` - Standard logging

### Multiple Instances
Du kan kj\u00f8re flere instanser samtidig:
- \u00c9n simulator
- \u00c9n webapp ELLER headless (ikke begge samtidig p\u00e5 samme port)

## Auto-detect Parity Testing

Test auto-detect med forskjellige parity modes:

1. Start simulator med `--parity=NONE`:
```bash
java -jar pls-sim.jar --port=/tmp/vserial1 --parity=NONE
```

2. Start webapp med auto-detect:
```bash
# Via IntelliJ: WebApp (FIELD - Auto-detect)
# Eller via kommandolinje:
java -jar lpg-ehl-webapp.jar --spring.profiles.active=field --ehl.serial.parity-auto-detect=true
```

3. Sjekk logs for auto-detected parity:
```
\ud83d\udd0d AUTO-DETECTING PARITY MODE
Testing parity: NONE - 8N1 - No parity (simulator/Python)
\u2705 AUTO-DETECTED PARITY: NONE
```

## Health Check Endpoints

Test serial kommunikasjon via REST API:

```bash
# Health check
curl http://localhost:8080/api/debug/serial/health

# Connection status  
curl http://localhost:8080/api/debug/serial/status

# Manual parity detection (ADVARSEL: St\u00f8tter med aktiv connection)
curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/tmp/vserial1"
```

## Troubleshooting

### Problem: "Address already in use"
**L\u00f8sning:** En annen instans kj\u00f8rer allerede. Stopp eksisterende prosesser.

### Problem: "Failed to open serial port"
**Sjekk:**
1. Er SOCAT kj\u00f8rende? (`ps aux | grep socat`)
2. Er simulator kj\u00f8rende p\u00e5 samme port?
3. Har du riktig port-navn? (`/tmp/vserial1` for socat)

### Problem: "Could not auto-detect parity"
**Sjekk:**
1. Er simulator kj\u00f8rende og tilkoblet?
2. Er simulator konfigurert med riktig address? (default: 1)
3. Test manuelt med Python-script f\u00f8rst

### Problem: Maven import feiler
**L\u00f8sning:**
1. **File > Invalidate Caches / Restart**
2. H\u00f8yreklikk p\u00e5 `pom.xml` > **Maven > Reload Project**
3. Sjekk at Java SDK er satt til 21

## Anbefalte IntelliJ Plugins
- **Kotlin** - For Kotlin support
- **Spring Boot** - For Spring Boot support  
- **Maven** - For Maven support (built-in)

## Ekstra Ressurser
- [IMPLEMENTATION_MODE_PARITY_FIX.md](IMPLEMENTATION_MODE_PARITY_FIX.md) - Parity konfigurasjonsguide
- [PYTHON_INTEGRATION_ANALYSIS.md](PYTHON_INTEGRATION_ANALYSIS.md) - Python/Kotlin integrasjon
- [COMPLETE_ARCHITECTURE_GUIDE.md](COMPLETE_ARCHITECTURE_GUIDE.md) - Fullstendig arkitekturguide
