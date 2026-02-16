# Test-guide: Payment Terminal Integrasjon

**Dato:** 2026-02-10  
**Status:** Klar for testing

---

## ✅ Hva er implementert

### Prioritet 1: Service-integrasjon (KOMPLETT)

1. **✅ OperationResponse oppdatert** med entry mode fields:
   - `LocalModeResultData`
   - `EntryMode` (CONTACTLESS/CHIP)
   - `EntryModeCode` ("2"/"0")

2. **✅ TerminalClient** - HTTP-kommunikasjon:
   - `checkStatus()` - Sjekk terminal status
   - `purchase(amountMinor, clientRequestId)` - Initier betaling
   - `reversal()` - Reverser siste transaksjon
   - `avstemming()` - Avstemming/reconciliation

3. **✅ TerminalEventListener** - SSE event stream:
   - Kobler til `/v1/events/stream` ved oppstart
   - Håndterer `OperationStarted`, `OperationCompleted`, `OperationTimeout`
   - Automatisk reconnect ved connection drop
   - Cursor-basert gjenopptagelse

4. **✅ TerminalEventHandler** - Business logic:
   - Parse event payloads
   - Trigge `PumpPaymentOrchestrator` ved godkjent betaling
   - Logger alle events

5. **✅ PumpPaymentOrchestrator** - Kobler betaling og pumping:
   - Oppretter transaksjon ved godkjent betaling
   - Starter fueling via EHL-protokoll (PRODUCT_SELECT, PROG_PRC, UNBLOCK)
   - Reversal ved feil
   - Omfattende logging

6. **✅ Konfigurasjon**:
   - `application.yaml` oppdatert med terminal-settings
   - Profil `terminal-local` for testing
   - WebFlux dependency lagt til

### Prioritet 2: GUI-forbedringer (KOMPLETT)

7. **✅ Farger oppdatert** til å matche ekte Ingenico terminal:
   - Svart chassis (`#0A0A0A`)
   - Blå/cyan LCD-display (`#003C5C` bakgrunn, `#00D9FF` tekst)
   - NFC-ikon i gull/beige (`#D4AF37`)
   - Røde, gule, grønne knapper som i ekte terminal

8. **✅ LED-stripe** (grønn, som i bildene):
   - Ny komponent: `LedStripe.kt`
   - Pulserende animasjon når aktiv
   - Gradient-effekt med blur
   - Plassert nederst i terminalen

9. **✅ Display styling forbedret**:
   - Større høyde (90dp)
   - Bedre padding
   - Mørk blå ramme
   - Cyan tekst (LCD-stil)

---

## 🚀 Testing - Steg for steg

### Steg 1: Start simulator

```bash
cd /Users/tandersen/.cursor/worktrees/lpg-ehl/isy/lpg-ehl-payment-terminal-sim
mvn spring-boot:run
```

**Forventet resultat:**
- Simulator starter på port 18080
- GUI åpnes automatisk
- Display viser "KLAR" med blå/cyan bakgrunn
- LED-stripe nederst er idle (mørk grønn)
- Ingen feil i logs

**Test GUI:**
1. Velg scenario: **APPROVED** (dropdown øverst)
2. Tast beløp: `12500` (125 kr)
3. Trykk **OK** (grønn knapp)
4. Se:
   - Display: "VENTER PÅ KORT..."
   - LED-stripe: **pulserende grønn** (animasjon)
   - Etter 2-5s: "GODKJENT ✓"
   - Terminal resetter

### Steg 2: Verifiser terminal API

```bash
# Terminal status
curl http://localhost:18080/v1/terminal/status

# Forventet:
# {
#   "TerminalReady": true,
#   "TerminalOpen": true,
#   ...
# }

# Test purchase via REST
curl -X POST http://localhost:18080/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -H "X-Terminal-Scenario: APPROVED" \
  -d '{"AmountMinor": 50000}'

# Forventet:
# {
#   "Success": true,
#   "EntryMode": "CONTACTLESS",
#   "EntryModeCode": "2",
#   "LocalModeResultData": "D  ;12345678;2;00;87654321",
#   ...
# }
```

### Steg 3: Start service med terminal enabled

```bash
cd /Users/tandersen/.cursor/worktrees/lpg-ehl/isy

# Bygg først (hvis ikke allerede gjort)
mvn clean package -pl lpg-ehl-app-headless -am -DskipTests

# Start med H2 + terminal-local profil
java -jar lpg-ehl-app-headless/target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=terminal-local
```

**Forventet i logs:**
```
Starting terminal event listener (http://localhost:18080/v1/events/stream)
Terminal SSE connection established
```

**Hvis feil:**
- Sjekk at simulator kjører på port 18080
- Sjekk firewall/nettverksinnstillinger

### Steg 4: Test full integrasjon

**Scenario: Betaling via GUI → Pump authorization**

1. **I simulator GUI:**
   - Velg scenario: APPROVED
   - Tast beløp: `50000` (500 kr)
   - Trykk OK

2. **Forventet i simulator logs:**
   ```
   POST /v1/payments/purchase (AmountMinor=50000)
   Publishing SSE: OperationStarted
   Publishing SSE: OperationCompleted (Success=true)
   ```

3. **Forventet i service logs:**
   ```
   🟢 Terminal operation completed: OperationId=..., Success=true, ResponseCode=00
   ✅ Payment approved - triggering pump authorization
   ⛽ Starting pumping for pump 1 after approved payment
   📝 Created transaction: ID=..., dispenser=1, price=15.90 kr/L
   Sending PRODUCT_SELECT for pump 1, product 1
   Sending PROG_PRC for pump 1 with price 15.90 kr/L
   Sending UNBLOCK for pump 1
   ✅ Pump 1 authorized for fueling
   🟢 Customer can now lift nozzle and start fueling
   ```

4. **Hvis du har fysisk pumpe/emulator:**
   - Pumpen skal få grønt lys
   - Kunde kan løfte pistol og tanke

---

## 🔍 Feilsøking

### Problem: Simulator starter ikke

**Sjekk:**
```bash
# Er port 18080 allerede i bruk?
lsof -i :18080

# Hvis ja, stop prosessen eller bruk annen port
```

**Løsning:**
Endre port i `application.yaml` (simulator):
```yaml
server:
  port: 19080  # Bruk annen port
```

### Problem: Service kobler ikke til SSE stream

**Sjekk logs:**
```
SSE connection error, reconnecting in 5s...
```

**Løsning:**
- Verifiser at simulator kjører: `curl http://localhost:18080/health`
- Sjekk `payment.terminal.base-url` i service config

### Problem: Pumpe ikke frigjort etter betaling

**Mulige årsaker:**
1. EHL-emulator ikke kjørende
2. Serial port ikke tilgjengelig (`/dev/ttyS3`)
3. Pumpe ikke i IDLE-state

**Sjekk logs:**
```
❌ Pump 1 is not idle (current status: PUMPING)
```

**Løsning:**
- Sjekk EHL-emulator status
- Verifiser at pumpe er i IDLE før betaling

### Problem: Reversal feiler

**Logs:**
```
❌ Reversal failed! Manual intervention required.
```

**Løsning:**
- Terminal kan ikke reversere hvis beløp allerede sendt til bank
- Kontakt support for manuell reversal

---

## 📊 Verifisering - Sjekkliste

### Simulator GUI

- [ ] Blå/cyan display (som LCD)
- [ ] Grønn LED-stripe nederst
- [ ] LED pulserer når "VENTER PÅ KORT..."
- [ ] "GODKJENT ✓" vises ved approved
- [ ] "AVVIST" vises ved declined
- [ ] Terminal resetter etter 2s

### Service integrasjon

- [ ] SSE connection etablert ved oppstart
- [ ] OperationCompleted event mottas
- [ ] PumpPaymentOrchestrator trigges
- [ ] Transaksjon opprettes i database
- [ ] PRODUCT_SELECT sendes til pumpe
- [ ] PROG_PRC sendes med pris
- [ ] UNBLOCK sendes til pumpe
- [ ] Pumpe går til AUTHORIZED

### Reversal-håndtering

- [ ] Reversal sendes ved EHL-feil
- [ ] Reversal response logges
- [ ] Feil håndteres gracefully

---

## 🎯 Testing i morgen (med kunde)

### Pre-test (10 min før)

1. **Start simulator:**
   ```bash
   cd lpg-ehl-payment-terminal-sim
   mvn spring-boot:run &
   ```

2. **Verifiser GUI:**
   - Blå display
   - Grønn LED
   - Test APPROVED scenario manuelt

3. **Start service:**
   ```bash
   java -jar lpg-ehl-app-headless.jar --spring.profiles.active=terminal-local
   ```

4. **Verifiser logs:**
   - "Terminal SSE connection established"
   - Ingen errors

### Live demo

**Scenario 1: Normal betaling**
1. Kunde "trekker kort" i GUI (50000 øre = 500 kr)
2. Venter på "GODKJENT ✓"
3. Pumpe frigjøres
4. Kunde tanker
5. Verifiser transaksjon i database

**Scenario 2: Avvist betaling**
1. Bytt scenario til DECLINED
2. Kunde "trekker kort"
3. Venter på "AVVIST"
4. Pumpe forblir blokkert

**Scenario 3: Feil PIN**
1. Bytt scenario til WRONG_PIN
2. Kunde "trekker kort"
3. Venter på "AVVIST"
4. Pumpe forblir blokkert

---

## 📝 Notater fra testing

### 2026-02-10 18:00

**Kompilering:**
- ✅ Simulator kompilerer (warning om deprecated URL-constructor)
- ✅ Service kompilerer med WebFlux og terminal-kode

**Neste:**
- Test runtime med simulator + service sammen
- Verifiser SSE event flow
- Test pumpe-frigjøring

---

**Vellykket implementering!** 🎉

Alle Prioritet 1 og 2 oppgaver er fullført. Systemet er klart for testing.
