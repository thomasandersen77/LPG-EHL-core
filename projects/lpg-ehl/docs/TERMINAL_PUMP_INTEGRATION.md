# Terminal- og pumpe-integrasjon

**Dato:** 2026-02-10  
**Status:** Implementert

## Implementert

### 1. Payment Terminal Simulator

**Nye API-endepunkter:**
- `POST /v1/payments/reservation` – Reserver beløp på kort (pre-auth)
  - Request: `{"AmountMinor": 150000, "Currency": "NOK"}` (1500 kr)
  - Ved godkjenning lagres reservasjonen; pumpen kan frigjøres

- `POST /v1/payments/completion` – Fullfør reservasjon med faktisk beløp
  - Request: `{"OperationId": "...", "AmountMinor": 84700}` (847 kr)
  - Beløpet må være ≤ reservert beløp

**GUI-forbedringer (Ingenico-stil):**
- Lyseblå LCD-skjerm (#003C5C)
- Cyan/grønn tekst
- "Trekke kort"-knapp som kaller reservation
- Reservasjonsbeløp-inndata (f.eks. 1000 eller 1500 kr)
- Mørk bakgrunn og tydelige knapper

### 2. PLS Simulator

**JavaFX GUI med `--gui`-flagget:**
```bash
java -jar pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --address=1 --gui
```

- Svart bakgrunn
- Rød "START"-knapp – klikk for å starte fylling (etter UNBLOCK)
- Grønn "STOPP"-knapp – klikk for å stoppe
- Viser liter og beløp i sanntid
- Med `--gui`: UNBLOCK setter AUTHORIZED; knappen styrer dyse (start/stopp)

### 3. Service-modul (implementert)

1. **SimulatedTerminalClient** – HTTP-klient mot terminal-simulator:
   - `reserve(amountMinor)` → `POST /v1/payments/reservation`
   - `capture(operationId, amountMinor)` → `POST /v1/payments/completion`
   - `reversal(operationId)` → `POST /v1/admin/reversal`

2. **TerminalEventPoller** – Poller `/v1/events` hvert sekund:
   - Ved `OperationCompleted` med type `reservation` og `success=true`:
     - Kall `PumpPaymentOrchestrator.unblockPumpAfterReservation()` → UNBLOCK via PumpStateService

3. **TerminalPumpCompletionListener** – Lytter på `PumpStoppedEvent`:
   - Når pumpe stopper med volum > 0 og det finnes session i TerminalPumpSession:
     - Kall `TerminalClient.capture(operationId, amountMinor)`
     - Kall `PumpStateService.settle(pumpId, "CARD")`
     - Fjern session fra TerminalPumpSession

4. **Pump-stopp-deteksjon** – To steder:
   - **Webapp "Stopp fylling":** `block()` sender BLOCK til PLS, henter volum, publiserer PumpStoppedEvent
   - **PLS GUI "STOPP":** `pollVolume()` oppdager STATE=0x00 (IDLE) → `handleHardwareStop()` → PumpStoppedEvent

## Brukerflyt

1. **Terminal:** Bruker velger reservasjonsbeløp (f.eks. 1500 kr) og trykker "Trekke kort"
2. **Terminal:** Reserverer beløpet, returnerer OperationId
3. **Service:** Mottar event, sender UNBLOCK til pumpe
4. **PLS:** Får UNBLOCK, går til AUTHORIZED
5. **PLS GUI:** Bruker klikker "START" → fylling starter
6. **Webapp:** Viser liter og beløp i sanntid (eksisterende polling)
7. **PLS GUI:** Bruker klikker "STOPP" → fylling stopper
8. **Service:** Oppdager stopp, henter volum, kaller terminal completion med faktisk beløp
9. **Terminal:** Vises "GODKJENT" med riktig beløp

## Kjøring

### Alt-i-ett: sim-all.sh
```bash
# Headless (default): terminal-sim + PLS uten GUI
./scripts/sim-all.sh

# GUI for begge simulatorene (terminal GUI + PLS JavaFX dødmannsknapp)
./scripts/sim-all.sh --gui

# Bygg artifacts først (hvis release/ mangler)
./scripts/sim-all.sh --build
```
Starter: Socat, Payment Terminal (headless eller GUI), PLS Simulator (med eller uten GUI).  
Webapp startes separat (typisk i IntelliJ) og må bruke `/tmp/vserial1`.  
Stop med Ctrl+C.

### Manuell kjøring

### Terminal-simulator (headless eller GUI)
```bash
# Headless API-sim (default)
./scripts/sim-terminal.sh

# GUI
./scripts/sim-terminal.sh --gui
```
Åpnes på http://localhost:18080 (default).

### PLS-simulator med socat (med eller uten GUI)
```bash
# Headless (default): auto-start etter UNBLOCK
./scripts/sim-pls.sh

# GUI (dødmannsknapp): UNBLOCK -> AUTHORIZED, og START/STOPP styrer dyse
./scripts/sim-pls.sh --gui
```

### Webapp (field mode mot PLS + terminal-sim)
Webapp startes typisk i IntelliJ. Viktigst er at field-modus bruker socat-porten:
- `--ehl.serial.port=/tmp/vserial1`

Hvis du vil starte fra JAR:
```bash
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field,terminal-sim \
  --ehl.serial.port=/tmp/vserial1
```
(Profilen `terminal-sim` aktiverer payment.terminal.enabled og base-url mot localhost:18080)

## Konfigurasjon

For terminal-integrasjon i webapp/headless:
```yaml
payment:
  terminal:
    enabled: true
    base-url: http://localhost:18080
```
