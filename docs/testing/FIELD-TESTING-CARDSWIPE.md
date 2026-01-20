# Felt-testing: Kortdragning-simulering

Denne dokumentasjonen beskriver hvordan du simulerer kortdragning for felt-testing av LPG-dispensere.

## Oversikt

Kortdragning-simuleringen gir deg mulighet til å teste hele pumpe-flyten uten faktisk betalingsterminal:

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Webapp/curl    │────▶│    PostgreSQL    │◀────│  Headless App   │
│ (card-swipe)    │     │ pump_authorization│     │ (polling)       │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
                                                 ┌─────────────────┐
                                                 │   Dispenser     │
                                                 │   (UNBLOCK)     │
                                                 └─────────────────┘
```

## Flyt

1. **Simuler kortdragning** - Webapp/curl setter inn PENDING autorisasjon i database
2. **Headless oppdager** - HeadlessPollingService finner PENDING autorisasjon
3. **UNBLOCK sendes** - Headless sender UNBLOCK til pumpe via RS-232
4. **Pumping starter** - Autorisasjon oppdateres til AUTHORIZED → PUMPING
5. **Kunde tar LPG** - Volum/beløp oppdateres kontinuerlig
6. **Stopp pumping** - Når ferdig, settes status til STOPPED
7. **Bekreft betaling** - Webapp/curl markerer betaling som fullført → COMPLETED

## Bruk fra Webapp

### 1. Åpne kontrollsiden
Gå til webapp: `http://localhost:8080/control`

### 2. Simuler kortdragning
Klikk "Simuler kortdragning" knappen (💳).

Du kan også bruke API direkte:
```bash
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/card-swipe \
  -H "Content-Type: application/json" \
  -d '{"maxAmountKr": 2000, "paymentMethod": "SIMULATION"}'
```

### 3. Vent på UNBLOCK
Headless-appen (som kjører på en annen maskin) vil oppdage autorisasjonen og sende UNBLOCK.

Sjekk status:
```bash
curl http://localhost:8080/api/v1/emulator/pump/1/authorization
```

### 4. Bekreft betaling (når ferdig)
```bash
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/confirm-payment \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod": "CARD"}'
```

## Bruk med Headless på Linux-maskin i felt

### Forutsetninger
- Headless-appen kjører på Linux-maskin ved LPG-stasjonen
- RS-232 tilkobling til dispenser
- Nettverkstilgang til PostgreSQL database
- Webapp tilgjengelig via nettverket

### Steg-for-steg

1. **Start headless-appen på Linux-maskin:**
```bash
cd lpg-ehl-app-headless
java -jar target/lpg-ehl-app-headless.jar \
  --lpg.serial.port=/dev/ttyUSB0 \
  --spring.datasource.url=jdbc:postgresql://db-server:5432/lpg_ehl
```

2. **Fra en annen maskin (laptop/nettbrett), simuler kortdragning:**
```bash
# Erstatt <webapp-ip> med IP-adressen til webapp-serveren
curl -X POST http://<webapp-ip>:8080/api/v1/emulator/pump/1/card-swipe
```

3. **Headless-appen logger:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💳 PENDING autorisasjon funnet: abc123...
   Dispenser: 1
   Maks beløp: 2000 kr
   Trigget av: WEBAPP_SIMULATION
   Prosesserer...
✅ UNBLOCK vellykket - Pumpe 1 frigjort
   Auth status: PENDING → AUTHORIZED → PUMPING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

4. **Kunden tar LPG fra pumpen**

5. **Når kunden er ferdig, bekreft betaling:**
```bash
curl -X POST http://<webapp-ip>:8080/api/v1/emulator/pump/1/confirm-payment
```

## API-endepunkter

| Metode | Endpoint | Beskrivelse |
|--------|----------|-------------|
| POST | `/api/v1/emulator/pump/{address}/card-swipe` | Simuler kortdragning |
| GET | `/api/v1/emulator/pump/{address}/authorization` | Hent aktiv autorisasjon |
| POST | `/api/v1/emulator/pump/{address}/confirm-payment` | Bekreft betaling |
| POST | `/api/v1/emulator/pump/{address}/cancel-authorization` | Kanseller autorisasjon |

## Autorisasjons-statuser

| Status | Beskrivelse |
|--------|-------------|
| `PENDING` | Kortdragning simulert, venter på UNBLOCK |
| `AUTHORIZED` | UNBLOCK sendt, pumpe frigjort |
| `PUMPING` | Aktivt uttak pågår |
| `STOPPED` | Pumping stoppet, venter på betaling |
| `COMPLETED` | Betaling bekreftet, transaksjon avsluttet |
| `CANCELLED` | Autorisasjon kansellert |
| `EXPIRED` | Autorisasjon utløpt (ikke implementert enda) |

## Database-tabell

Autorisasjoner lagres i `pump_authorization`:

```sql
SELECT authorization_id, dispenser_address, status, 
       max_amount_kr, actual_volume_liters, actual_amount_kr,
       triggered_by, created_at
FROM pump_authorization
ORDER BY created_at DESC;
```

## Feilsøking

### "Det finnes allerede en aktiv autorisasjon"
Kanseller den eksisterende først:
```bash
curl -X POST http://localhost:8080/api/v1/emulator/pump/1/cancel-authorization
```

### Headless finner ikke autorisasjoner
Sjekk at:
1. Headless-appen er startet og poller
2. Database-tilkobling fungerer
3. `lpg.dispenser.address` matcher dispenser-adressen

### UNBLOCK feiler
Sjekk:
1. RS-232 kabel er tilkoblet
2. Riktig serial port konfigurert
3. Dispenser er på og responderer

## Konfigurasjon

### application.yaml (headless)
```yaml
lpg:
  dispenser:
    address: 1
  polling:
    interval-ms: 2000  # Hvor ofte sjekke for autorisasjoner
  serial:
    port: /dev/ttyUSB0
```

### Environment variabler
```bash
export LPG_DISPENSER_ADDRESS=1
export LPG_POLLING_INTERVAL_MS=2000
export LPG_SERIAL_PORT=/dev/ttyUSB0
```
