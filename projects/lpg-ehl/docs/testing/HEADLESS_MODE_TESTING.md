# Headless Mode Testing - Kortdragning og Pumpe-frigjøring

Denne guiden beskriver hvordan du tester headless-modus hvor systemet automatisk oppdager kortdragning-autorisasjoner og frigjør pumpen uten manuell interaksjon via GUI.

## 📋 Oversikt

**Headless-modus** er designet for produksjonsmiljøer hvor:
- Kortterminalen oppretter en autorisasjon i databasen når et kort dras
- Headless-appen poller databasen og finner PENDING-autorisasjoner
- Headless sender UNBLOCK-kommando til pumpen automatisk
- Pumping starter uten GUI-interaksjon

**GUI-modus** (til sammenligning):
- GUI sender UNBLOCK direkte når du trykker "SIMULER KORTDRAGNING"
- Ingen avhengighet til headless-polling
- Ideell for lab-testing og debugging

## 🎯 Arkitektur

```
┌─────────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│  Kortterminal /     │  INSERT │   PostgreSQL     │  POLL   │   Headless      │
│  GUI Simulator      ├────────►│ pump_auth table  │◄────────┤   Poller        │
│                     │         │  (PENDING)       │         │                 │
└─────────────────────┘         └──────────────────┘         └────────┬────────┘
                                                                      │
                                                                      │ UNBLOCK
                                                                      ▼
                                                             ┌─────────────────┐
                                                             │   EHL Dispenser │
                                                             │   (Pumpe #1)    │
                                                             └─────────────────┘
```

## 🛠️ Testoppsett

### Forutsetninger

1. **PostgreSQL kjører** på port 5432
2. **lpg-ehl-webapp** kjører på port 8080 (GUI + API)
3. **lpg-ehl-app-headless** kjører (polling-agent)

### Start headless-appen

```bash
cd lpg-ehl-app-headless
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Headless-appen vil:
- Koble til database
- Starte polling hver 2. sekund
- Logge: `🔍 Polling for PENDING authorizations...`

## 📝 Test-scenarioer

### Scenario 1: Simuler kortdragning via GUI (for å teste headless)

1. **Åpne Control Panel**
   ```
   http://localhost:8080/control
   ```

2. **Send kortdragning med `immediate=false`** (via API)
   
   **VIKTIG**: Ikke bruk GUI-knappen! Den sender alltid `immediate=true`.
   
   Bruk cURL eller Postman:
   
   ```bash
   curl -X POST http://localhost:8080/api/v1/emulator/pump/1/card-swipe \
     -H "Content-Type: application/json" \
     -d '{
       "maxAmountKr": 2000,
       "triggeredBy": "HEADLESS_TEST",
       "paymentMethod": "CARD",
       "immediate": false
     }'
   ```
   
   **Respons:**
   ```json
   {
     "success": true,
     "message": "Kortdragning simulert - venter på UNBLOCK fra headless",
     "mode": "HEADLESS",
     "authorization": {
       "authorizationId": "123e4567-e89b-12d3-a456-426614174000",
       "dispenserAddress": 1,
       "status": "PENDING",
       "maxAmountKr": 2000,
       "pricePerLiterKr": 15.90
     }
   }
   ```

3. **Observer headless-loggene**
   
   Innen 2 sekunder skal headless finne autorisasjonen:
   
   ```
   🔍 Polling for PENDING authorizations...
   💳 Found PENDING authorization: 123e4567-e89b-12d3-a456-426614174000
   🔓 Sending UNBLOCK to dispenser #1...
   ✅ Pump unblocked - authorization marked as AUTHORIZED
   ```

4. **Verifiser i GUI**
   
   Control Panel skal vise:
   - Status: "Pumpe frigjort!" eller "Klar til fylling"
   - Autorisasjon: AUTHORIZED (ikke PENDING)

5. **Start pumping**
   
   Løft dysen (simulator) eller trykk "START PUMPING" i GUI.

6. **Stopp pumping**
   
   Trykk "STOPP PUMPING" når du har fylt tilstrekkelig volum.

7. **Bekreft betaling**
   
   Trykk "BEKREFT BETALING" for å fullføre transaksjonen.

8. **Verifiser i database**
   
   ```sql
   SELECT * FROM pump_authorization 
   WHERE authorization_id = '123e4567-e89b-12d3-a456-426614174000';
   ```
   
   Status-forløp skal være:
   - PENDING → AUTHORIZED → PUMPING → STOPPED → COMPLETED

---

### Scenario 2: Simuler ekte kortterminal-integrasjon

For å simulere en **ekte kortterminal** som oppretter autorisasjoner direkte i databasen:

1. **Opprett PENDING autorisasjon manuelt**
   
   ```sql
   INSERT INTO pump_authorization (
     authorization_id,
     dispenser_address,
     status,
     max_amount_kr,
     price_per_liter_kr,
     triggered_by,
     payment_method,
     created_at
   ) VALUES (
     gen_random_uuid(),
     1,
     'PENDING',
     2000.00,
     15.90,
     'TERMINAL_SIMULATOR',
     'CARD',
     NOW()
   );
   ```

2. **Observer headless**
   
   Headless vil oppdage denne autorisasjonen innen 2 sekunder og sende UNBLOCK.

3. **Fortsett som normalt**
   
   Pumping → Stopp → Betalingsbekreftelse.

---

### Scenario 3: Test polling-intervall og feilhåndtering

1. **Stopp headless-appen**
2. **Opprett flere PENDING autorisasjoner** (via API eller SQL)
3. **Start headless-appen igjen**
4. **Observer at headless prosesserer alle pending autorisasjoner i rekkefølge**

Headless skal:
- Prosessere eldste autorisasjon først (`ORDER BY created_at ASC`)
- Håndtere feil gracefully (logge og fortsette til neste)
- Ikke duplikat-prosessere (status endres fra PENDING til AUTHORIZED)

---

## 🔍 Debugging

### Sjekk aktive autorisasjoner

```sql
SELECT 
  authorization_id,
  dispenser_address,
  status,
  max_amount_kr,
  triggered_by,
  created_at,
  authorized_at
FROM pump_authorization
WHERE status IN ('PENDING', 'AUTHORIZED', 'PUMPING', 'STOPPED')
ORDER BY created_at DESC;
```

### Nullstill autorisasjon (for re-test)

```sql
DELETE FROM pump_authorization WHERE authorization_id = '<UUID>';
```

### Sjekk headless-polling

Logg-utskrifter du skal se:

```
🔍 Polling for PENDING authorizations...
💳 Found PENDING authorization: <UUID>
📤 TX: 20 05 01 55 60 36  (UNBLOCK kommando)
📥 RX: 20 04 01 6B 50 36  (ACK)
✅ Pump unblocked - authorization marked as AUTHORIZED
```

---

## ⚙️ Konfigurasjon

### Polling-intervall

Standard er **2 sekunder**. For å endre:

```yaml
# application-local.yaml
headless:
  polling:
    interval-ms: 2000  # 2 sekunder
```

### Database-tilkobling

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lpg_ehl
    username: postgres
    password: postgres
```

---

## 🚨 Vanlige feil

### "No PENDING authorizations found" - men GUI viser PENDING

**Problem**: Headless og GUI bruker forskjellige databaser.

**Løsning**: 
- Sjekk `application-local.yaml` i begge moduler
- Verifiser at begge peker til samme database
- Restart begge applikasjoner

### Headless sender UNBLOCK, men pumpe ikke frigjort

**Problem**: EHL-kommunikasjon feilet.

**Løsning**:
- Sjekk at emulator kjører på riktig port (9001)
- Verifiser at EhlCommunicator er konfigurert korrekt
- Se på Protocol-loggene i GUI for TX/RX HEX

### Autorisasjon blir sittende i PENDING

**Problem**: Headless-appen kjører ikke.

**Løsning**:
- Start `lpg-ehl-app-headless`
- Sjekk loggene for feilmeldinger
- Verifiser at Spring Boot startet uten feil

---

## ✅ Success Criteria

En vellykket headless-test skal:

1. ✅ Opprette PENDING autorisasjon (via API eller SQL)
2. ✅ Headless oppdager autorisasjonen innen 2 sekunder
3. ✅ UNBLOCK sendes til pumpe (se HEX i logs)
4. ✅ Autorisasjon oppdateres til AUTHORIZED
5. ✅ Pumping kan starte uten GUI-interaksjon
6. ✅ Transaksjon persisteres til database ved fullføring
7. ✅ Status-forløp: PENDING → AUTHORIZED → PUMPING → STOPPED → COMPLETED

---

## 📚 Relaterte dokumenter

- [Control Panel Testing](FIELD-TESTING-CARDSWIPE.md) - GUI-modus testing
- [Transaction Flow](../general/TRANSACTION_FLOW.md) - Full transaksjonssyklus
- [Database Schema](../schema/PUMP_AUTHORIZATION.md) - Autorisasjonstabeller
- [Headless Architecture](../architecture/HEADLESS_POLLING.md) - Teknisk design

---

**Sist oppdatert:** 2026-01-19  
**Testet på:** lpg-ehl v0.1.0-SNAPSHOT
