# Terminal- og pumpe-integrasjon

**Dato:** 2026-02-10  
**Status:** Delvis implementert

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

### 3. Service-modul (planlagt)

For full integrasjon trengs:

1. **SimulatedTerminalClient** – HTTP-klient mot terminal-simulator:
   - `reserve(amountMinor)` → `POST /v1/payments/reservation`
   - `capture(operationId, amountMinor)` → `POST /v1/payments/completion`
   - `reversal(operationId)` → `POST /v1/admin/reversal`

2. **TerminalEventPoller** – Poller `/v1/events` eller kobler til SSE:
   - Ved `OperationCompleted` med type `reservation` og `success=true`:
     - Kall `PumpStateService.unblock()` for å frigjøre pumpen

3. **Pump-stopp-deteksjon** – Når pumpe går fra PUMPING til IDLE:
   - Hent volum og beløp
   - Kall `TerminalClient.capture(operationId, amountMinor)`
   - Kall settle for å fullføre transaksjonen

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

### Terminal-simulator med GUI
```bash
cd lpg-ehl-payment-terminal-gui
mvn spring-boot:run
```
Åpnes på http://localhost:18080

### PLS-simulator med socat og GUI
```bash
# Terminal 1: Socat + PLS med GUI
./start-socat-sim.sh
# (Oppdater script for å legge til --gui)

# Eller manuelt:
socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1 &
java -jar lpg-ehl-serialport-sim/target/pls-sim.jar \
  --port=/tmp/ttyV0 --mode=ehl --address=1 --gui
```

### Webapp (field mode mot PLS)
```bash
java -jar lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/ttyV1
```

## Konfigurasjon

For terminal-integrasjon i webapp/headless:
```yaml
payment:
  terminal:
    enabled: true
    base-url: http://localhost:18080
```
