# Baxi Terminal Testing - Komplett Undersøkelsesrapport

**Dato:** 2026-02-15  
**Utført av:** Warp AI + Thomas Andersen  
**Formål:** Undersøke hvordan baxi-kotlin fungerer og teste mot ekte terminal

---

## 📋 Oppsummering (Executive Summary)

### Viktigste Funn:

1. ✅ **BaxiIntegrationTest fungerer perfekt** (mot simulator)
2. ⚠️ **baxi-kotlin != Nets Cloud Client** - Det er en TCP-klient for **fysisk terminal**!
3. 🔑 **Nets Cloud krever separat bibliotek** - Se `PaymentTerminalNetsCloudKotlinServer`
4. 🎯 **To ulike arkitekturer** eksisterer side-om-side

---

## 🔍 Punkt 1: Test av BaxiIntegrationTest

### Kommando:
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn -pl lpg-ehl-service test -Dtest=BaxiIntegrationTest
```

### Resultat:
```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Hva testen gjør:
```kotlin
@Test
fun `should complete purchase against simulator`() {
    // 1. Start BaxiTcpServer (simulator) på port 7202
    // 2. Create BaxiTerminalClient med host=127.0.0.1, port=7202
    // 3. Call openTerminal() -> Wait for onTerminalReady
    // 4. Call purchase(1000 øre)
    // 5. Verify success + responseCode="00"
}
```

**Konklusjon:** `baxi-kotlin` biblioteket fungerer perfekt mot TCP-simulator!

---

## 🔍 Punkt 2: Undersøkelse av baxi-kotlin Source Code

### Lokasjon:
```
/Users/tandersen/git/NorgesGass/BaxiExperiments/
  └── physically-connected-to-ethernet-experiments/
      └── baxi-kotlin/
```

### Arkitektur:

```
BaxiClientImpl
  ├── TcpClientTransport (TCP socket til terminal)
  ├── Dfs13ControllerSm (DFS13 protokoll state machine)
  └── EventDispatcher (event callbacks)
```

### Hva baxi-kotlin IKKE gjør:
- ❌ Snakker IKKE med Nets Connect@Cloud
- ❌ Håndterer IKKE WebSocket/HTTPS
- ❌ Håndterer IKKE autentisering (username/password)
- ❌ Trenger IKKE credentials

### Hva baxi-kotlin GJØR:
- ✅ Snakker **direkte med fysisk terminal** via TCP
- ✅ Implementerer **DFS13 protokoll** (Baxi vendor protocol)
- ✅ Sender TransferAmount, Administration commands
- ✅ Parser DisplayText, PrintText, LocalMode events
- ✅ TCP framing (length-prefixed messages)

### BaxiIniConfig:
```kotlin
data class BaxiIniConfig(
    val hostIpAddress: String,  // IP til terminalen (IKKE Nets Cloud!)
    val hostPort: Int,           // TCP port til terminalen
    val vendorInfoExtended: String?,
    val socketListenerEnabled: Boolean,
    val socketListenerPort: Int?
)
```

**Ingen username/password!** Det er fordi `baxi-kotlin` snakker direkte med terminalen via TCP, ikke med Nets Cloud!

---

## 🔍 Punkt 3: Eksempler fra BaxiExperiments

### Funnet: PaymentTerminalNetsCloudKotlinServer

**Lokasjon:**
```
/Users/tandersen/git/NorgesGass/BaxiExperiments/
  └── nets-cloud-solution/
      └── PaymentTerminalNetsCloudKotlinServer/
```

### Dette er det RIKTIGE biblioteket for Nets Cloud!

#### Moduler:
```
PaymentTerminalNetsCloudKotlinServer/
├── connect-cloud-client/         # WebSocket + Auth til Nets Cloud
│   ├── ConnectCloudAuthClient.kt
│   └── ConnectCloudWebSocketClient.kt
├── payment-terminal-library/     # Terminal orchestration
│   └── TerminalService interface
└── payment-terminal-server/      # HTTP wrapper (port 8080)
```

#### ConnectCloudAuthClient:
```kotlin
suspend fun login(
    baseUrl: String,           // "https://connectcloud.aws.nets.eu"
    username: String,          // "cloudberries_shared"
    password: String,          // "B8PnVjmVq-SMM9QD"
    timeout: Duration
): String {
    // POST /v1/login
    // Returns JWT token
}
```

#### ConnectCloudWebSocketClient:
```kotlin
// Opens WebSocket: wss://connectcloud.aws.nets.eu/ws/json
// Header: Authorization: Bearer <token>
// Sends NetsRequest messages
// Receives NetsResponse messages
```

---

## 📊 To Arkitekturer Side-om-Side

### Arkitektur 1: Fysisk Terminal (baxi-kotlin)

```
┌─────────────────────────────────────────────────────────┐
│              BaxiTerminalClient (Spring)                 │
│           (din lpg-ehl-service/terminal/)               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                 baxi-kotlin Library                      │
│         (BaxiClientImpl + DFS13 protocol)               │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ TCP Socket (port 7200 eller custom)
                     ▼
┌─────────────────────────────────────────────────────────┐
│          Fysisk Betalingsterminal (Baxi)                │
│          (Ethernet-tilkoblet på lokal LAN)              │
└─────────────────────────────────────────────────────────┘
```

**Bruksområde:**
- Terminalen er **fysisk på stedet** (bensinstasjonen)
- Koblet til LAN via Ethernet
- Baxi vendor firmware
- Direct TCP communication

**Config:**
```kotlin
BaxiIniConfig(
    hostIpAddress = "192.168.1.100",  // IP til terminalen på LAN
    hostPort = 7200,                  // Baxi TCP port
    ...
)
```

---

### Arkitektur 2: Nets Connect@Cloud (PaymentTerminalNetsCloudKotlinServer)

```
┌─────────────────────────────────────────────────────────┐
│              Din Service (lpg-ehl-service)              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│       PaymentTerminalNetsCloudKotlinServer               │
│           (payment-terminal-library)                     │
│          TerminalService interface                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           connect-cloud-client                           │
│     (ConnectCloudAuthClient + WebSocket)                │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTPS + WebSocket
                     │ POST /v1/login (username/password)
                     │ WSS /ws/json (Bearer token)
                     ▼
┌─────────────────────────────────────────────────────────┐
│          Nets Connect@Cloud                              │
│      connectcloud.aws.nets.eu:443                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Nets internal routing
                     ▼
┌─────────────────────────────────────────────────────────┐
│      Betalingsterminal (virtuell i Nets Cloud)          │
│          Terminal ID: 42696609                           │
└─────────────────────────────────────────────────────────┘
```

**Bruksområde:**
- Terminalen er **virtuell/cloud-basert**
- Kommunikasjon via Nets Cloud (ikke direkte)
- Krever autentisering (username/password)
- WebSocket for real-time communication

**Credentials (fra server.json):**
```json
{
  "connectCloud": {
    "environment": "PROD",
    "username": "cloudberries_shared",
    "password": "B8PnVjmVq-SMM9QD",
    "terminalId": "42696609"
  }
}
```

---

## 🎯 Hva Betyr Dette For Testing?

### Scenario 1: Test mot Fysisk Terminal (Baxi)

**Hvis terminalen er fysisk på stedet:**

```kotlin
// lpg-ehl-service application.yaml
payment:
  terminal:
    enabled: true
    implementation: baxi
    baxi:
      host: 192.168.1.100  # IP til terminalen
      port: 7200           # Baxi TCP port
```

**Kjør:**
```bash
# Start Spring Boot app
java -jar lpg-ehl-webapp.jar
```

**Krever:**
- ✅ Terminalen må være på og koblet til LAN
- ✅ Nettverksforbindelse til terminalens IP
- ✅ `baxi-kotlin` bibliotek (allerede installert)
- ❌ Ingen credentials nødvendig (TCP direkte)

---

### Scenario 2: Test mot Nets Connect@Cloud

**Hvis terminalen er virtuell i Nets Cloud:**

**Du må ERSTATTE `baxi-kotlin` med `PaymentTerminalNetsCloudKotlinServer`!**

#### Steg 1: Publiser payment-terminal-library til Maven
```bash
cd /Users/tandersen/git/NorgesGass/BaxiExperiments/nets-cloud-solution/PaymentTerminalNetsCloudKotlinServer
./gradlew publishToMavenLocal
```

#### Steg 2: Oppdater lpg-ehl-service/pom.xml
```xml
<dependency>
    <groupId>com.cloudberries.netscloud</groupId>
    <artifactId>payment-terminal-library</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### Steg 3: Implementer NetsCloudTerminalClient
```kotlin
@Component
@ConditionalOnProperty(name = ["payment.terminal.implementation"], havingValue = "netscloud")
class NetsCloudTerminalClient(
    private val terminalService: TerminalService  // Fra payment-terminal-library
) : TerminalClient {
    // Wrapper rundt TerminalService
}
```

#### Steg 4: Config
```yaml
payment:
  terminal:
    enabled: true
    implementation: netscloud
    netscloud:
      username: cloudberries_shared
      password: B8PnVjmVq-SMM9QD
      terminalId: "42696609"
      baseUrl: https://connectcloud.aws.nets.eu
```

---

## ⚠️ Hvorfor Test-Scriptet Feilet

### Problem:
```
🔌 Opening terminal...
   Open() returned: callResult=1
⏳ Waiting for terminal ready (30s timeout)...
⏱️  TIMEOUT - Terminal did not become ready
```

### Årsak:
Vi prøvde å koble til `connectcloud.aws.nets.eu:443` med `baxi-kotlin`, men:

1. **baxi-kotlin** forventer TCP til **fysisk terminal**
2. **Nets Cloud** krever HTTPS + WebSocket med **autentisering**
3. **baxi-kotlin** har INGEN autentisering - det er en rent TCP-protokoll

**Det er som å prøve å bestille pizza via bankkonto-overføring - feil protokoll! 😄**

---

## ✅ Konklusjoner og Anbefalinger

### 1. Hva du har nå:
- ✅ `baxi-kotlin` - For **fysisk terminal** (TCP)
- ✅ `BaxiTerminalClient` - Spring-wrapper rundt `baxi-kotlin`
- ✅ `BaxiIntegrationTest` - Fungerer mot simulator

### 2. Hva du trenger for Nets Cloud:
- 📦 `PaymentTerminalNetsCloudKotlinServer` (payment-terminal-library)
- 🔐 Credentials (username/password/terminalId)
- 🌐 Network connectivity til `connectcloud.aws.nets.eu`

### 3. Testing-strategi:

#### A. Test Lokalt (Simulator)
```bash
# Allerede virker!
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn -pl lpg-ehl-service test -Dtest=BaxiIntegrationTest
```

#### B. Test mot Fysisk Terminal (Hvis tilgjengelig)
```bash
# Sett IP til terminalen
export PAYMENT_TERMINAL_BAXI_HOST=192.168.1.100
export PAYMENT_TERMINAL_BAXI_PORT=7200
export PAYMENT_TERMINAL_IMPLEMENTATION=baxi

# Kjør app
java -jar lpg-ehl-webapp.jar
```

#### C. Test mot Nets Cloud (Krever ny integrasjon)
1. Publiser `payment-terminal-library` til Maven
2. Implementer `NetsCloudTerminalClient`
3. Konfigurer credentials
4. Test via Spring Boot app

### 4. Neste Steg:

**Hvis kollegaen din har fysisk terminal:**
- ✅ Bruk `baxi-kotlin` (allerede implementert!)
- ✅ Finn terminalens IP på LAN
- ✅ Oppdater `application.yaml` med IP
- ✅ Test direkte

**Hvis dere bruker Nets Cloud (virtuell terminal):**
- 📦 Integrer `PaymentTerminalNetsCloudKotlinServer`
- 🔧 Implementer `NetsCloudTerminalClient`
- 🔐 Konfigurer credentials
- 🧪 Test via HTTP API eller direkte

---

## 📝 Ressurser

### Dokumentasjon:
- `BAXI_KOTLIN_HVORDAN_VIRKER_DET.md` - Detaljert forklaring av baxi-kotlin
- `NETS_CLOUD_CONFIG.md` - Nets Cloud credentials og config
- `TESTING_GUIDE.md` - Guide for testing

### Source Code:
- `baxi-kotlin`: `/Users/tandersen/git/NorgesGass/BaxiExperiments/physically-connected-to-ethernet-experiments/baxi-kotlin`
- `PaymentTerminalNetsCloudKotlinServer`: `/Users/tandersen/git/NorgesGass/BaxiExperiments/nets-cloud-solution/PaymentTerminalNetsCloudKotlinServer`

### Implementasjoner:
- `BaxiTerminalClient.kt`: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/BaxiTerminalClient.kt`
- `BaxiIntegrationTest.kt`: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-service/src/test/kotlin/no/cloudberries/lpg/service/terminal/BaxiIntegrationTest.kt`

---

## 🎓 Lærdommer

1. **To ulike løsninger:**
   - `baxi-kotlin` = Fysisk terminal (TCP)
   - `PaymentTerminalNetsCloudKotlinServer` = Nets Cloud (HTTPS/WebSocket)

2. **Credentials-forvirring:**
   - `baxi-kotlin` trenger INGEN credentials (TCP direkte)
   - Nets Cloud trenger username/password (autentisering)

3. **Feil antakelse:**
   - Vi antok at `baxi-kotlin` snakket med Nets Cloud
   - Realiteten: `baxi-kotlin` er en DFS13 TCP-klient

4. **Integrasjonstesten virker:**
   - `BaxiIntegrationTest` beviser at `baxi-kotlin` fungerer
   - Simulatoren (BaxiTcpServer) simulerer fysisk terminal

---

**END OF REPORT**

**Hvis du har spørsmål eller trenger hjelp med neste steg, bare spør! 🚀**
