# Scripts

`scripts/` er ryddet for å fokusere på det som brukes i praksis: starte simulatorene og bygge artifacts.

## 🚀 Simulator-start (3 scripts)

### 1) PLS (pumpestyring) + socat

```bash
./scripts/sim-pls.sh
# GUI (dødmannsknapp):
./scripts/sim-pls.sh --gui
```

Dette lager virtuelle porter:
- `/tmp/vserial0` ↔ `/tmp/vserial1`

**Viktig for IntelliJ / field-mode:** Webapp/headless skal koble til PLS via:
- `--ehl.serial.port=/tmp/vserial1`

### 2) Payment terminal

```bash
./scripts/sim-terminal.sh
# GUI:
./scripts/sim-terminal.sh --gui
```

Default port: `18080` (overstyr med `--port=...`).

### 3) Alt-i-ett (terminal + PLS)

```bash
./scripts/sim-all.sh
# GUI for begge:
./scripts/sim-all.sh --gui
```

## 🔨 Build

### Bygg webapp (React + webapp-jar)

```bash
./scripts/build-webapp.sh
```

Output:
- `release/lpg-ehl-webapp.jar`

### Bygg simulatorer (PLS + terminal)

```bash
./scripts/build-simulators.sh
```

Output:
- `release/pls-sim.jar`
- `release/payment-terminal-sim.jar`
- `release/payment-terminal-gui.jar`

## 🧰 Tools

Nyttige hjelpere ligger i `scripts/tools/` (ikke en del av normal oppstart). Eksempel:
- `scripts/tools/full-pump-test.sh`
- `scripts/tools/cleanup-stuck-auth.sh`
- `scripts/tools/h2-console.sh`
