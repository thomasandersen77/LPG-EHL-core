# Terminal Integrasjon Guide - Fra A til Å

Denne guiden tar deg gjennom hele prosessen for å koble til og teste betalingsterminal med LPG-EHL systemet.

## Innholdsfortegnelse
1. [Forutsetninger](#forutsetninger)
2. [Fysisk terminal-sjekk](#1-fysisk-terminal-sjekk)
3. [Nettverksvalidering](#2-nettverksvalidering)
4. [Kjør diagnostikk](#3-kjør-diagnostikk)
5. [Problemløsning](#4-problemløsning)
6. [Integrasjon i produksjon](#5-integrasjon-i-produksjon)

---

## Forutsetninger

### Hardware
- Ingenico/Verifone betalingsterminal (støttet: Self/4000, P400, V400)
- Terminal koblet til samme nettverk som din maskin
- Kjøpmannkort aktivert på terminalen

### Software
- Java 21 (bruk `sdk use java 21.0.7-tem`)
- Maven installert
- Prosjektet klonet og bygget

---

## 1. Fysisk Terminal-Sjekk

### 1.1 Finn terminal-innstillinger
På terminalen, naviger til:
```
Communication > Kasse > ECR
```

### 1.2 Verifiser ECR-konfigurasjon
Sjekk følgende verdier:

| Innstilling | Verdi | Beskrivelse |
|------------|-------|-------------|
| Mode | IP Ethernet | Must be IP-basert |
| ECR Port | 8009 | Standard ECR port |
| ECR IP | 0.0.0.0 eller din IP | Se punkt 1.3 |

### 1.3 Konfigurer ECR IP

**For testing (anbefalt):**
- Sett ECR IP til `0.0.0.0` (tillater alle IP-adresser)

**For produksjon:**
- Sett ECR IP til din maskines IP (f.eks. `192.168.0.10`)
- Finn din IP med: `ipconfig getifaddr en0`

### 1.4 Lagre og bekreft
- Lagre innstillingene
- Terminal bør vise "Venter på kasse" eller lignende

⚠️ **VIKTIG:** Hvis terminalen viser "Ingen kasse registrert", er ECR IP feil konfigurert!

---

## 2. Nettverksvalidering

### 2.1 Finn din lokale IP
```bash
# Mac
ipconfig getifaddr en0

# Alternativt
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### 2.2 Finn terminal IP
Terminalen vil typisk ha en IP fra DHCP. Sjekk i terminalmeny:
```
Communication > Network > IP Address
```

Standard terminal-IP er ofte `192.168.0.4` (kan variere).

### 2.3 Test nettverkstilkobling
```bash
# Test at terminalen svarer
ping -c 3 192.168.0.4

# Test at ECR-porten er åpen
nc -vz 192.168.0.4 8009
```

**Forventet output:**
```
Connection to 192.168.0.4 port 8009 [tcp/*] succeeded!
```

**Feilmeldinger:**
- `Connection refused`: ECR-modus ikke aktiv på terminal
- `Timeout`: Feil IP eller nettverksproblem

---

## 3. Kjør Diagnostikk

### 3.1 Bygg prosjektet
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn clean compile -pl lpg-ehl-core
```

### 3.2 Kjør EcrDiagnosticTool
```bash
# Med standard verdier (192.168.0.4:8009, 1.00 NOK)
mvn exec:java -pl lpg-ehl-core \
  -Dexec.mainClass=no.cloudberries.lpg.payment.EcrDiagnosticTool

# Med egendefinerte verdier
mvn exec:java -pl lpg-ehl-core \
  -Dexec.mainClass=no.cloudberries.lpg.payment.EcrDiagnosticTool \
  -Dexec.args="192.168.0.4 8009 100"
```

### 3.3 Forstå output

#### Steg 1: Lokalt nettverk
```
✅ RESULTAT: Lokalt nettverk OK. Din IP: 192.168.0.10
```
→ Du er på samme subnet som terminalen.

#### Steg 2: Terminal nåbar
```
✅ RESULTAT: Terminal er nåbar på 192.168.0.4
```
→ Terminalen svarer på nettverket.

#### Steg 3: TCP-tilkobling
```
✅ RESULTAT: TCP-tilkobling OK fra 192.168.0.10
```
→ ECR-porten er åpen.

**VIKTIG:** Noter "Local (Din maskin)" IP - denne MÅ være tillatt i terminalens ECR-meny!

#### Steg 4: Test-kommando
```
✅ RESULTAT: ACK mottatt - terminal aksepterte kommandoen.
```
→ Terminalen kommuniserer! Sjekk terminalskjermen for betalingsforespørsel.

---

## 4. Problemløsning

### Problem: "Ingen kasse registrert" på terminal

**Årsak:** ECR IP whitelist matcher ikke din IP.

**Løsning:**
1. Kjør diagnostikk og noter "Local (Din maskin)" IP
2. Gå til terminal: Communication > Kasse > ECR IP
3. Sett ECR IP til den noterte IP-en ELLER `0.0.0.0`
4. Lagre og prøv igjen

### Problem: Timeout - ingen respons

**Årsak 1:** Feil terminal-IP
- Verifiser IP i terminalmenyen

**Årsak 2:** Feil port
- Standard ECR port er 8009, ikke 8008

**Årsak 3:** Brannmur
- Sjekk at port 8009 ikke er blokkert
- Mac: System Preferences > Security & Privacy > Firewall

### Problem: NAK (0x15) mottatt

**Årsak:** Terminalen avviser kommandoen.

**Mulige løsninger:**
1. Terminal er opptatt - vent til forrige transaksjon er ferdig
2. Terminal ikke i riktig modus - restart terminal
3. Ugyldig beløp - sjekk at beløp er > 0

### Problem: Connection refused

**Årsak:** ECR-server ikke aktiv på terminal.

**Løsning:**
1. Verifiser at ECR Mode er "IP Ethernet"
2. Restart terminalen
3. Sjekk at terminal har strøm og nettverkstilkobling

---

## 5. Integrasjon i Produksjon

### 5.1 Betalingsflyt for LPG-stasjoner

```
┌─────────────────────────────────────────────────────────┐
│                    BETALINGSFLYT                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  1. KUNDE STARTER FYLLING                                │
│     │                                                    │
│     ▼                                                    │
│  ┌──────────────────────────────────────┐               │
│  │ requestPreauth(maxAmount: 1500 NOK)  │               │
│  │ → Reserver maksbeløp på kort         │               │
│  └──────────────────────────────────────┘               │
│     │                                                    │
│     ▼                                                    │
│  [PUMPE FRIGJØRES - KUNDE FYLLER]                       │
│     │                                                    │
│     ▼                                                    │
│  2. FYLLING STOPPER                                      │
│     │                                                    │
│     ▼                                                    │
│  ┌──────────────────────────────────────┐               │
│  │ capturePreauth(actualAmount, txnId)  │               │
│  │ → Trekker faktisk beløp fra kort     │               │
│  └──────────────────────────────────────┘               │
│     │                                                    │
│     ▼                                                    │
│  [KVITTERING TIL KUNDE]                                  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Kodeeksempel

```kotlin
val terminal = TcpPaymentTerminal(
    host = "192.168.0.4",
    port = 8009,
    connectTimeoutMs = 5000,
    readTimeoutMs = 30000
)

terminal.use {
    // Koble til
    if (!terminal.connect()) {
        throw RuntimeException("Kunne ikke koble til terminal")
    }
    
    // Pre-auth før fylling (maks 1500 kr)
    val preauth = terminal.requestPreauth(150000)
    if (preauth.status != PaymentStatus.APPROVED) {
        throw RuntimeException("Preauth feilet: ${preauth.errorMessage}")
    }
    val transactionId = preauth.transactionId!!
    
    // ... kunde fyller (pumpe-logikk) ...
    val actualAmountCents = calculateActualAmount()
    
    // Capture med faktisk beløp
    val capture = terminal.capturePreauth(actualAmountCents, transactionId)
    if (capture.status != PaymentStatus.APPROVED) {
        // Håndter feil - logg og varsle
    }
    
    // Ferdig - generer kvittering
    printReceipt(capture)
}
```

### 5.3 Feilhåndtering

```kotlin
when (result.status) {
    PaymentStatus.APPROVED -> {
        // Suksess - fortsett flyten
    }
    PaymentStatus.PENDING -> {
        // Terminal venter på bruker - poll eller vis melding
        logger.info("Venter på bekreftelse på terminal...")
    }
    PaymentStatus.DECLINED -> {
        // Kort avvist - be kunde prøve annet kort
        showErrorToCustomer("Betaling avvist. Prøv et annet kort.")
    }
    PaymentStatus.TIMEOUT -> {
        // Timeout - mulig nettverksproblem
        logger.error("Terminal timeout")
        alertOperator()
    }
    PaymentStatus.ERROR -> {
        // Systemfeil - logg og varsle
        logger.error("Betalingsfeil: ${result.errorMessage}")
        alertOperator()
    }
    PaymentStatus.CANCELLED -> {
        // Bruker avbrøt
        resetPump()
    }
}
```

---

## Vedlegg: Kommandoreferanse

### BAX Protokoll Format
```
[STX] [Payload] [ETX] [LRC]

STX = 0x02 (Start of Text)
ETX = 0x03 (End of Text)
LRC = XOR av alle bytes fra payload gjennom ETX
```

### Støttede kommandoer

| Kommando | Format | Beskrivelse |
|----------|--------|-------------|
| Purchase | P,OpID,Amount | Vanlig kjøp |
| Preauth | A,OpID,Amount | Reservasjon |
| Capture | F,TxnID,Amount | Fullfør reservasjon |
| Cancel | C | Avbryt transaksjon |
| Status | S | Hent status |

### Responskoder

| Kode | Betydning |
|------|-----------|
| ACK (0x06) | Kommando akseptert |
| NAK (0x15) | Kommando avvist |
| 00 | Suksess |
| Andre tall | Feilkode fra terminal |

---

## Sjekkliste før produksjon

- [ ] ECR IP satt til korrekt verdi (ikke 0.0.0.0 i prod!)
- [ ] Timeout-verdier testet og justert
- [ ] Feilhåndtering implementert
- [ ] Logging aktivert
- [ ] Varsling ved feil konfigurert
- [ ] Backup-prosedyre hvis terminal er nede
- [ ] Kvitteringsgenerering fungerer
- [ ] Preauth/Capture flow testet ende-til-ende

---

*Sist oppdatert: 2025-12-29*
*Generert av: LPG-EHL Terminal Integration*
