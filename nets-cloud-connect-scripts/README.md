# Nets Cloud Connect Testing Scripts

Disse Kotlin-scriptene tester direkte mot Nets Cloud Connect production environment.

## 📋 Oversikt

| Script | Beskrivelse | Status |
|--------|-------------|--------|
| `01-test-login.kts` | Test HTTP login (JWT token) | ✅ Klar |
| `02-test-websocket.kts` | Test WebSocket-tilkobling | 🔜 Kommer |
| `03-test-open-terminal.kts` | Test Open-kommando | 🔜 Kommer |
| `04-test-purchase.kts` | Test kjøp (1 krone) | 🔜 Kommer |
| `05-test-admin-commands.kts` | Test admin-kommandoer | 🔜 Kommer |

---

## 🔐 Credentials (fra Nets e-post)

**Fra:** Jannick (Nets)  
**Dato:** Januar 2026

```
Username: cranberries_shared
Password: Gf&DW*8-IN7Lx6pE
Terminal ID: BAX-1229329 (PROD terminal - EKTE PENGER!)
```

**Endepunkter:**

| Type | Adresse | Port | Bruk |
|------|---------|------|------|
| Login | `https://connectcloud.aws.nets.eu` | 443 | POST /v1/login |
| WebSocket | `wss://connectcloud.aws.nets.eu` | 443 | /ws/json |
| Alternativ (IP) | `3.33.230.243` / `15.197.206.182` | 6001 | TLS-kryptert TCP |

---

## 🚀 Quick Start

### 1. Installer Kotlin CLI (hvis ikke allerede)

```bash
# Via SDKMAN
sdk install kotlin

# Via Homebrew
brew install kotlin
```

### 2. Kjør scriptene

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/nets-cloud-connect-scripts

# Test login
kotlin 01-test-login.kts

# Test WebSocket (når klar)
kotlin 02-test-websocket.kts
```

### 3. Med custom credentials (miljøvariabler)

```bash
export NETS_CLOUD_URL="https://connectcloud.aws.nets.eu"
export NETS_USERNAME="cranberries_shared"
export NETS_PASSWORD="Gf&DW*8-IN7Lx6pE"

kotlin 01-test-login.kts
```

---

## 📊 Testing-strategi

### Fase 1: Connectivity ✅
- [x] `01-test-login.kts` - Verifiser at vi kan logge inn

### Fase 2: WebSocket 🔜
- [ ] `02-test-websocket.kts` - Koble til med JWT token
- [ ] Motta heartbeat/ping
- [ ] Håndtere reconnect

### Fase 3: Terminal Lifecycle 🔜
- [ ] `03-test-open-terminal.kts` - Send `<Open/>` kommando
- [ ] Vente på `<Dfs13TerminalReady/>`
- [ ] Håndtere `<Dfs13DisplayText>`

### Fase 4: Transaksjoner 🔜
- [ ] `04-test-purchase.kts` - Kjøp med 1 krone
- [ ] Parse `<Dfs13LocalMode>` respons
- [ ] Håndtere kvittering (`<Dfs13PrintText>`)

### Fase 5: Admin 🔜
- [ ] `05-test-admin-commands.kts`
- [ ] End of Day (avstemming)
- [ ] X-Report / Z-Report
- [ ] Last Receipt

---

## 📝 Protokoll-referanse

### Login (HTTP POST)

**Request:**
```http
POST /v1/login HTTP/1.1
Host: connectcloud.aws.nets.eu
Content-Type: application/json

{
  "username": "cranberries_shared",
  "password": "Gf&DW*8-IN7Lx6pE"
}
```

**Response (success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### WebSocket (WSS)

**URL:** `wss://connectcloud.aws.nets.eu/ws/json`  
**Header:** `Authorization: bearer <token>`

**Meldingsformat:** XML

### Open Terminal

**Request (XML via WebSocket):**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<NetsRequest>
  <MessageHeader 
    ECRID="LPG-TEST-1" 
    TerminalID="1229329" 
    VersionNumber="1" />
  <Open />
</NetsRequest>
```

**Response:**
```xml
<NetsResponse>
  <MessageHeader TerminalID="1229329" />
  <Dfs13TerminalReady />
</NetsResponse>
```

### Purchase (Kjøp)

**Request:**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<NetsRequest>
  <MessageHeader 
    ECRID="LPG-TEST-1" 
    TerminalID="1229329" 
    VersionNumber="1" />
  <Dfs13TransferAmount>
    <TransactionType>48</TransactionType>
    <OperId>0000</OperId>
    <Amount1>100</Amount1>
    <Amount2>0</Amount2>
    <Amount3>0</Amount3>
    <Type2>48</Type2>
    <Type3>48</Type3>
    <HostData></HostData>
    <OptionalData></OptionalData>
  </Dfs13TransferAmount>
</NetsRequest>
```

**Response:**
```xml
<NetsResponse>
  <MessageHeader TerminalID="1229329" />
  <Dfs13LocalMode>
    <Result>1</Result>
    <TotalAmount>100</TotalAmount>
    <TruncatedPan>************1234</TruncatedPan>
    <CardIssuerName>VISA</CardIssuerName>
    <ResponseCode>00</ResponseCode>
    <StanAuth>123456</StanAuth>
    <!-- ... flere felter ... -->
  </Dfs13LocalMode>
</NetsResponse>
```

---

## ⚠️ VIKTIG SIKKERHET

### 1. PROD Terminal - EKTE PENGER!
Terminal BAX-1229329 er en **produksjonst terminal**. Alle transaksjoner er **ekte** og vil belaste faktiske kort.

**Testing-regler:**
- ✅ Bruk kun TESTBELØP (maks 1 krone)
- ✅ Bruk kun ditt eget testkort
- ❌ ALDRI test med kundens kort
- ❌ ALDRI test med høye beløp

### 2. Credentials
Credentials er sensitive og gir tilgang til production environment.

**Sikkerhet:**
- ✅ Lagret lokalt (ikke committet til Git)
- ✅ Brukes kun for testing
- ❌ Del ALDRI credentials med uautoriserte
- ❌ Commit ALDRI passwords til Git

### 3. Rate Limiting
Nets Cloud Connect har rate limiting.

**Best practices:**
- ✅ Vent mellom kall (minimum 1 sekund)
- ✅ Ikke kjør flere scripts samtidig
- ✅ Ikke loop/retry for aggressivt

---

## 🐛 Troubleshooting

### Problem: "Connection refused"
**Årsak:** Nets Cloud kan være utilgjengelig eller firewall blokkerer  
**Løsning:**
```bash
# Test connectivity
curl -v https://connectcloud.aws.nets.eu/v1/login

# Sjekk DNS
nslookup connectcloud.aws.nets.eu
```

### Problem: "401 Unauthorized"
**Årsak:** Feil credentials  
**Løsning:**
- Verifiser username: `cranberries_shared`
- Verifiser password: `Gf&DW*8-IN7Lx6pE`
- Kontakt Nets hvis credentials er utløpt

### Problem: "Terminal not found"
**Årsak:** Feil Terminal ID  
**Løsning:**
- Bruk Terminal ID: `1229329` (uten "BAX-" prefix)

### Problem: Kotlin script kompilerer ikke
**Årsak:** Dependencies ikke tilgjengelige  
**Løsning:**
```bash
# Sjekk Kotlin versjon
kotlin -version

# Prøv å kjøre med verbose
kotlin -verbose 01-test-login.kts
```

---

## 📚 Dokumentasjon

### Interne docs:
- `NETS_CLOUD_CONNECT_KOMPLETT_GUIDE.md` - Komplett guide (i Google Drive)
- `BAXI_INVESTIGATION_REPORT.md` - Teknisk analyse
- E-post fra Nets (Jannick) - Konfigurasjon og credentials

### Eksterne docs:
- Nets Connect@Cloud Developer Guide v1.2.16
- Baxi DFS13 Protocol Specification

---

## ✅ Testing Checklist

Før du går videre til produksjon:

- [ ] Login fungerer (`01-test-login.kts`)
- [ ] WebSocket-tilkobling fungerer (`02-test-websocket.kts`)
- [ ] Terminal Open fungerer (`03-test-open-terminal.kts`)
- [ ] Kjøp med 1 krone fungerer (`04-test-purchase.kts`)
- [ ] Kvittering mottas og kan parses
- [ ] Admin-kommandoer fungerer (`05-test-admin-commands.kts`)
- [ ] Feilhåndtering er testet (avbrutt betaling, timeout, etc.)
- [ ] Reconnect-logikk fungerer

**Når alle er OK:** Integrer `CloudConnectBaxiClient` i lpg-ehl-service! 🎉

---

**Sist oppdatert:** 2026-02-15  
**Maintainer:** Thomas Andersen  
**Status:** In Progress
