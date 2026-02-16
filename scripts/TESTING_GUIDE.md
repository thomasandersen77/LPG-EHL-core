# Kotlin Scripts for Baxi Terminal Testing

Disse scriptene lar deg teste `baxi-kotlin` biblioteket direkte mot Nets Connect@Cloud uten å måtte bygge hele Spring Boot-applikasjonen.

---

## 🚀 Quick Start

### 1. Installer Kotlin Command Line (hvis ikke allerede installert)

```bash
# Via SDKMAN (anbefalt)
sdk install kotlin

# Via Homebrew
brew install kotlin
```

### 2. Kjør Quick Test

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/scripts
kotlin test-baxi-quick.kts
```

Dette tester bare om du kan koble til terminalen. **Rask iterasjon!**

### 3. Kjør Full Purchase Test

```bash
kotlin test-baxi-terminal.kts
```

Dette tester full betalingsflyt:
- Open terminal
- Wait for ready
- Purchase (10.00 NOK)
- Print receipt
- Close terminal

---

## 📄 Scripts Overview

| Script | Beskrivelse | Brukstid |
|--------|-------------|----------|
| `test-baxi-quick.kts` | Bare connectivity test (open + wait for ready) | ~5 sekunder |
| `test-baxi-terminal.kts` | Full purchase-flyt med kvittering | ~30 sekunder |
| `NETS_CLOUD_CONFIG.md` | Dokumentasjon av Nets-konfigurasjon og credentials | - |

---

## ⚙️ Konfigurasjon

### Standard (Nets Cloud Production)

Scriptene er konfigurert med:
- **Host:** `connectcloud.aws.nets.eu`
- **Port:** `443`
- **Terminal ID:** `42696609`
- **Credentials:** Fra `server.json` (se `NETS_CLOUD_CONFIG.md`)

### Custom Endpoint (f.eks. lokal simulator)

```bash
# Test mot lokal TCP-server på port 7200
NETS_HOST=127.0.0.1 NETS_PORT=7200 kotlin test-baxi-quick.kts
```

### Custom Terminal ID

```bash
TERMINAL_ID=12345678 kotlin test-baxi-terminal.kts
```

---

## 🔍 Forklaring: Hva Scriptene Gjør

### `test-baxi-quick.kts`

```kotlin
1. Importer baxi-kotlin biblioteket
2. Opprett BaxiClient
3. Sett opp event listener (onTerminalReady, onError)
4. Kall client.open(config)
5. Vent på onTerminalReady callback (30s timeout)
6. Print resultat
7. client.close()
```

**Output ved suksess:**
```
✅ Terminal is READY!
✅ SUCCESS! Terminal is ready and connected!
```

**Output ved feil:**
```
❌ Error 401: Unauthorized
❌ Terminal reported error during opening
```

---

### `test-baxi-terminal.kts`

Full flyt med alle steg:

```kotlin
1. Open terminal (venter på onTerminalReady)
2. Initiate purchase via client.transferAmount()
3. Venter på onLocalMode + onLastFinancialResult
4. Parser resultater:
   - LocalMode.result: 1 = success, 2 = rejected
   - LocalMode.responseCode: "00" = approved, "05" = declined, "Z1" = wrong PIN
   - FinancialResult.result: 1 = OK
5. Print kvittering (fra onPrintText events)
6. Close terminal
```

**Output ved suksess:**
```
💳 LocalMode: result=1, responseCode=00
💰 FinancialResult: result=1
✅ Purchase completed!

📄 Receipt:
NETS AS
TRANSAKSJON GODKJENT
...
```

---

## 🧪 Eksempel: Iterativ Testing

### Scenario 1: Test connectivity først

```bash
# Raskt test for å sjekke at du kommer gjennom til Nets
kotlin test-baxi-quick.kts
```

**Hvis dette feiler:**
- Sjekk nettverksforbindelse
- Sjekk at credentials er riktige
- Sjekk at terminalen er online

### Scenario 2: Test purchase

```bash
# Når connectivity virker, test purchase
kotlin test-baxi-terminal.kts
```

**Hvis dette feiler:**
- Sjekk at betalingskortet er aktivert
- Sjekk at beløpet er godkjent av bank
- Sjekk terminal logs for detaljer

### Scenario 3: Test ulike beløp

**Endre beløp i scriptet:**

```kotlin
// I test-baxi-terminal.kts, linje ~164:
val purchaseArgs = TransferAmountArgs(
    operId = "0000",
    type1 = 10,
    amount1 = 5000,  // <-- Endre her (5000 = 50.00 NOK)
    ...
)
```

```bash
kotlin test-baxi-terminal.kts
```

---

## 🐛 Debugging

### Verbose Logging

```bash
export ORG_SLF4J_SIMPLELOGGER_DEFAULTLOGLEVEL=TRACE
kotlin test-baxi-quick.kts
```

### Check Network

```bash
# Test DNS
nslookup connectcloud.aws.nets.eu

# Test TCP connectivity
nc -zv connectcloud.aws.nets.eu 443

# Full HTTPS test
curl -v https://connectcloud.aws.nets.eu/
```

### Common Errors

| Error | Årsak | Løsning |
|-------|-------|---------|
| `callResult != 1` | baxi-kotlin rejected open | Sjekk credentials, terminal ID |
| `401 Unauthorized` | Feil brukernavn/passord | Oppdater credentials fra Nets |
| `Timeout waiting for ready` | Nettverksproblem eller terminal offline | Sjekk connectivity, sjekk terminal status |
| `LocalMode result=2` | Betaling avvist | Sjekk responseCode ("05"=declined, "Z1"=wrong PIN) |

---

## 🔐 Security Note

⚠️ **Credentials i plaintext:**
- Scriptene inneholder hardkodet passord fra `server.json`
- Dette er OK for testing og utvikling
- **Ikke commit scripts med production credentials til public repo!**

For produksjon, bruk miljøvariabler:

```bash
# Legg til i .env eller shell profile
export NETS_CLOUD_USERNAME="cloudberries_shared"
export NETS_CLOUD_PASSWORD="B8PnVjmVq-SMM9QD"

# Oppdater script til å lese fra env
val username = System.getenv("NETS_CLOUD_USERNAME") ?: error("NETS_CLOUD_USERNAME not set")
```

---

## 📚 Further Reading

- `NETS_CLOUD_CONFIG.md` - Komplett dokumentasjon av Nets-konfigurasjon
- `../lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/terminal/BaxiTerminalClient.kt` - Production implementation
- `../lpg-ehl-service/src/test/kotlin/no/cloudberries/lpg/service/terminal/BaxiIntegrationTest.kt` - JUnit test example

---

## ✅ Checklist for Testing

- [ ] Kotlin CLI installert (`kotlin -version`)
- [ ] baxi-kotlin JAR i lokal Maven repo (`~/.m2/repository/no/cloudberries/norgesgass/baxi-kotlin/`)
- [ ] Network connectivity til Nets (`nc -zv connectcloud.aws.nets.eu 443`)
- [ ] Credentials oppdatert (se `NETS_CLOUD_CONFIG.md`)
- [ ] Terminal er online og klar
- [ ] `test-baxi-quick.kts` kjører OK
- [ ] `test-baxi-terminal.kts` kjører OK
- [ ] Du har et test-betalingskort klart

**Når alle er OK:** Du er klar til å integrere i Spring Boot-appen! 🎉
