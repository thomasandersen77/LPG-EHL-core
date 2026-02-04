# Scripts Guide

## 🚀 Hovedskripter

### 1. `start-socat-sim.sh` - Start Simulator

Starter både SOCAT og PLS simulator i én kommando.

**Grunnleggende bruk:**
```bash
./scripts/start-socat-sim.sh
```

**Avansert konfigurasjon:**
```bash
./scripts/start-socat-sim.sh \
  --address=1 \
  --price=1590 \
  --baud=9600 \
  --parity=NONE \
  --blocked=true
```

### 2. `start-webapp-field.sh` - Start Webapp

**Grunnleggende bruk (anbefalt):**
```bash
./scripts/start-webapp-field.sh --auto-detect
```

### 3. `start-headless-field.sh` - Start Headless App

**Grunnleggende bruk:**
```bash
./scripts/start-headless-field.sh --auto-detect
```

---

## 📋 Typisk Workflow

```bash
# Terminal 1: Start simulator
./scripts/start-socat-sim.sh

# Terminal 2: Start webapp
./scripts/start-webapp-field.sh --auto-detect

# Browser: http://localhost:8080
```

---

**Se [../GETTING_STARTED.md](../GETTING_STARTED.md) for full dokumentasjon.**
