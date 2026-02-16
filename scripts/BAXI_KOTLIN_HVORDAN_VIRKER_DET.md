# Hvordan baxi-kotlin Biblioteket Virker

## 🔍 Oversikt

`baxi-kotlin` er et Kotlin-bibliotek som wrapper Nets Connect@Cloud WebSocket-protokollen. Det håndterer **HELE** kommunikasjonen med Nets, inkludert autentisering.

---

## 🔐 Autentisering - VIKTIG!

### Hva du MÅ vite:

**`baxi-kotlin` tar IKKE inn credentials direkte i `BaxiIniConfig`!**

```kotlin
// Dette er FEIL:
val config = BaxiIniConfig(
    hostIpAddress = "connectcloud.aws.nets.eu",
    hostPort = 443,
    username = "cloudberries_shared",  // ❌ Finnes ikke!
    password = "B8PnVjmVq-SMM9QD"     // ❌ Finnes ikke!
)
```

**Riktig konfigurasjon:**
```kotlin
val config = BaxiIniConfig(
    hostIpAddress = "connectcloud.aws.nets.eu",  // ✅ Nets Cloud endpoint
    hostPort = 443,                               // ✅ HTTPS/WSS port
    vendorInfoExtended = "LPG-EHL-SERVICE",      // ✅ App identifier
    socketListenerEnabled = false,                // ✅ Not needed for client
    socketListenerPort = null
)
```

---

## ❓ Så hvor går credentials?

**Svar:** `baxi-kotlin` må ha fått credentials på EN av disse måtene:

### Alternativ 1: Hardkodet i biblioteket
Biblioteket kan ha credentials bakt inn i koden (midlertidig løsning for testing).

### Alternativ 2: Miljøvariabler
Biblioteket kan lese fra environment variables:
```bash
export NETS_CLOUD_USERNAME="cloudberries_shared"
export NETS_CLOUD_PASSWORD="B8PnVjmVq-SMM9QD"
export NETS_TERMINAL_ID="42696609"
```

### Alternativ 3: Config-fil
Biblioteket kan lese fra en `server.json` eller lignende i classpath.

### Alternativ 4: Extended i BaxiIniConfig
Biblioteket kan parse `vendorInfoExtended` eller bruke en annen mekanisme.

---

## 🧪 Hvordan teste hva som faktisk skjer?

### Metode 1: Kjør scriptet og se på loggene

```bash
export ORG_SLF4J_SIMPLELOGGER_DEFAULTLOGLEVEL=DEBUG
kotlin test-baxi-quick.kts
```

**Se etter:**
- "Authenticating with username: ..."
- "Login successful, token: ..."
- "401 Unauthorized" (hvis credentials er feil)

### Metode 2: Sjekk baxi-kotlin kildekoden

```bash
# Finn hvor baxi-kotlin er installert
ls -la ~/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/0.1.0-SNAPSHOT/

# Unpack JAR og se på kildekoden
cd /tmp
jar xf ~/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/0.1.0-SNAPSHOT/baxi-kotlin-0.1.0-SNAPSHOT.jar
grep -r "username" .
grep -r "password" .
```

---

## 📊 Autentiseringsflyt i Nets Connect@Cloud

```
1. Client (baxi-kotlin) → POST https://connectcloud.aws.nets.eu/v1/login
   Body: { "username": "cloudberries_shared", "password": "..." }

2. Nets Cloud → Response: { "token": "eyJhbG..." }

3. Client → Opens WebSocket: wss://connectcloud.aws.nets.eu/ws/json
   Header: Authorization: Bearer eyJhbG...

4. Client → Sends InitRequest with terminal ID

5. Nets Cloud → Responds with InitResponse

6. Client → Fires onTerminalReady() event ✅
```

**Hvis credentials er feil:**
```
1. POST /v1/login → 401 Unauthorized
2. baxi-kotlin fires onError(401, "Unauthorized")
3. Terminal ALDRI blir ready
```

---

## 🔧 Hva du kan kontrollere

### 1. Network Connectivity

```bash
# Test DNS
nslookup connectcloud.aws.nets.eu

# Test HTTPS port
curl -v https://connectcloud.aws.nets.eu/

# Test WebSocket port (hvis annerledes)
nc -zv connectcloud.aws.nets.eu 443
```

### 2. Credentials (manuelt)

```bash
# Test login endpoint direkte
curl -X POST https://connectcloud.aws.nets.eu/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "cloudberries_shared",
    "password": "B8PnVjmVq-SMM9QD"
  }'
```

**Forventet respons:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Hvis feil credentials:**
```json
HTTP/1.1 401 Unauthorized
```

---

## 🎯 Hva scriptet gjør (steg-for-steg)

### test-baxi-quick.kts

```kotlin
1. Import baxi-kotlin library (via @file:DependsOn)

2. Create BaxiClient instance:
   val client = BaxiClientImpl()

3. Set event listener:
   client.setEventListener(object : BaxiEventListener {
       override fun onTerminalReady() { ... }
       override fun onError(...) { ... }
   })

4. Call client.open(config):
   - baxi-kotlin internally:
     a) POST /v1/login (with username/password)
     b) Get token
     c) Open WebSocket with token
     d) Send InitRequest with terminal ID
     e) Wait for InitResponse
     f) Fire onTerminalReady() ✅

5. Wait for onTerminalReady callback (30s timeout)

6. Print result

7. client.close()
```

---

## ⚠️ Mulige Feil og Løsninger

### Feil 1: "401 Unauthorized"
**Årsak:** Credentials er feil eller utløpt  
**Løsning:** 
- Sjekk at username/password er korrekt
- Kontakt Nets for nye credentials hvis nødvendig
- Test manuelt med curl (se over)

### Feil 2: "Timeout waiting for terminal ready"
**Årsak:** Nettverksproblem eller terminal offline  
**Løsning:**
- Sjekk network connectivity (curl/nc)
- Sjekk at terminalen er online (login via Nets portal?)
- Sjekk firewall/proxy settings

### Feil 3: "callResult != 1"
**Årsak:** baxi-kotlin rejected open call  
**Løsning:**
- Sjekk `methodRejectCode` og `methodRejectInfo` i output
- Dette er før autentisering, så det er et config-problem

### Feil 4: Script kompilerer ikke
**Årsak:** baxi-kotlin JAR ikke i Maven repo  
**Løsning:**
```bash
# Sjekk at JAR finnes
ls -la ~/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/0.1.0-SNAPSHOT/

# Hvis mangler, installer fra source:
cd /Users/tandersen/git/NorgesGass/BaxiExperiments/baxi-kotlin
mvn clean install
```

---

## 📝 Testing Tips

### 1. Start med quick test
```bash
kotlin test-baxi-quick.kts
```
Dette tester **bare connectivity og auth**. Ingen purchase, rask feedback!

### 2. Hvis quick test virker, test purchase
```bash
kotlin test-baxi-terminal.kts
```

### 3. Test med verbose logging
```bash
export ORG_SLF4J_SIMPLELOGGER_DEFAULTLOGLEVEL=DEBUG
kotlin test-baxi-quick.kts 2>&1 | tee baxi-test.log
```

### 4. Test credentials manuelt først
```bash
curl -X POST https://connectcloud.aws.nets.eu/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "cloudberries_shared",
    "password": "B8PnVjmVq-SMM9QD"
  }'
```

---

## 🔑 Credentials fra server.json

**File:** `/Users/tandersen/git/NorgesGass/BaxiExperiments/nets-cloud-solution/PaymentTerminalNetsCloudMonoServer/server.json`

```json
{
  "connectCloud": {
    "environment": "PROD",
    "baseUrl": null,
    "username": "cloudberries_shared",
    "password": "B8PnVjmVq-SMM9QD",
    "terminalId": "42696609",
    "ecrIdPrefix": "POS-",
    "operatorIdDefault": "4321",
    "webSocketPath": "/ws/json"
  }
}
```

**baseUrl: null** betyr at Nets Cloud bruker default endpoint:
- **PROD:** `https://connectcloud.aws.nets.eu`
- **QA:** `https://connectcloud-qa.aws.nets.eu` (eller lignende)

---

## ✅ Checklist før du tester

- [ ] `baxi-kotlin` JAR finnes i `~/.m2/repository/`
- [ ] Kotlin CLI installert (`kotlin -version`)
- [ ] Network connectivity OK (`curl https://connectcloud.aws.nets.eu/`)
- [ ] Credentials testet manuelt (curl login)
- [ ] Terminal er online (sjekk med Nets portal)
- [ ] Du har betalingskort klart (for purchase test)

---

## 🚀 Neste Steg

1. **Kjør quick test først:**
   ```bash
   cd /Users/tandersen/git/NorgesGass/lpg-ehl/scripts
   kotlin test-baxi-quick.kts
   ```

2. **Hvis det virker, kjør purchase test:**
   ```bash
   kotlin test-baxi-terminal.kts
   ```

3. **Når begge virker:**
   - Du har bekreftet at `baxi-kotlin` virker!
   - Du kan nå integrere i Spring Boot-appen
   - Sett `payment.terminal.implementation=baxi` i `application.yaml`

**Lykke til! 🎉**
