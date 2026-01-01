# Oppsummering: Betalingsterminal-integrasjon for LPG-EHL

## 🎯 Mål
Integrere en betalingsterminal (IP: 192.168.0.4:8009) med LPG dispenser-emulator for å:
1. Simulere fylling (1 øre/liter, maks 3 liter = 3 øre)
2. Sende betalingsforespørsel til terminal
3. Vente på at bruker tapper kort
4. Motta bekreftelse
5. Fullføre transaksjon

## 🏗️ Prosjektstruktur

### Backend (Kotlin/Maven)
```
/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core/
├── src/main/kotlin/no/cloudberries/lpg/
│   ├── emulator/
│   │   ├── EhlDispenserEmulator.kt    # Dispenser-simulator
│   │   └── InMemorySerialPort.kt      # In-memory kommunikasjon
│   ├── payment/
│   │   ├── PaymentTerminal.kt         # Interface + TCP/Simulert terminal
│   │   ├── PaymentTerminalDemo.kt     # Demo med simulert terminal
│   │   ├── EcrServer.kt               # ECR server (lytter på port 8009)
│   │   ├── InteractivePaymentDemo.kt  # Interaktiv demo med ekte terminal
│   │   └── SimpleEcrListener.kt       # Enkel lytter for protokoll-analyse
│   ├── protocol/
│   │   ├── EhlCodec.kt                # EHL protokoll encoding/decoding
│   │   ├── EhlCommands.kt             # EHL kommandoer
│   │   └── EhlPacket.kt               # Packet struktur
│   └── communication/
│       └── EhlCommunicator.kt         # Kommunikasjon med dispenser
├── run-ecr-server.sh                  # Starter ECR server
├── test-ekte-betaling.sh              # Tester ekte terminal
└── lytt-til-terminal.sh               # Logger terminal-meldinger
```

## ✅ Hva som fungerer

### 1. Dispenser Emulator
- ✓ Simulerer fylling med konfigurerbar pris
- ✓ Håndterer UNBLOCK/BLOCK kommandoer
- ✓ State machine: IDLE → AUTHORIZED → DELIVERING → PAYMENT_PENDING
- ✓ Logger all kommunikasjon

### 2. Payment Terminal Interface
- ✓ Abstrakt interface for alle terminaler
- ✓ `SimulatedPaymentTerminal` - fungerer perfekt for testing
- ✓ `TcpPaymentTerminal` - kobler til ekte terminal

### 3. TCP Kommunikasjon
- ✓ Kan koble til `192.168.0.4:8009`
- ✓ Sender data til terminalen
- ✓ Mottar data fra terminalen

## ⚠️ Problemet

### Symptom
Terminalen går inn i "velg vare"-modus når vi prøver å betale, i stedet for å be om korttapping.

### Observasjoner
1. **Initial test**: Terminal svarte med "F" (0x46)
2. **Ved korttapping**: Terminalen gikk inn i egen flyt ("velg vare")
3. **Ved å ta ut kort**: "Avbrutt av kunde"

### Årsak
Vi sender **feil protokoll-format**. Terminalen forventer spesifikk ECR-protokoll (ZVT, Nets, eller proprietær), men vi sendte:
- Tekstformat: `"PAY:000000000000"`
- Enkel binær: `[0x06, 0x01, ...]`

## 🔧 Implementerte løsninger

### 1. ECR Server (run-ecr-server.sh)
```kotlin
// Lytter på port 8009
// Aksepterer tilkoblinger fra terminal
// Sender ACK på alle meldinger
// Logger all kommunikasjon
```

### 2. Interactive Payment Demo (test-ekte-betaling.sh)
```kotlin
// Kobler til terminal
// Sender betalingsforespørsel (3 øre)
// Viser "TAPPE KORTET NÅ!"
// Venter 60 sekunder
// Logger all respons
```

### 3. Simple ECR Listener (lytt-til-terminal.sh)
```kotlin
// Lytter på port 8009
// Logger ALLE bytes fra terminal
// Viser HEX + ASCII + DEC format
// Identifiserer meldingstype (ENQ, ACK, STX, etc.)
// Svarer med ACK
```

## 📊 Terminal Informasjon

### Nettverkskonfigurasjon
- **Terminal IP**: 192.168.0.4
- **Port**: 8009
- **Mac IP**: 192.168.0.41
- **Protokoll**: Ukjent (må analyseres)

### Wireshark Capture
Fra tidligere analyse:
- Terminal sender kryptert data (TLS/SSL)
- Første respons: "F" (0x46)
- Trolig binær ECR-protokoll

## 🎓 Neste steg

### 1. Identifiser terminal
- **Merke/Modell**: ? (se bilder)
- **Protokoll**: ZVT / Nets / OPI / Proprietær?
- **Dokumentasjon**: Trenger terminal-manual

### 2. Fang protokoll
Kjør: `./lytt-til-terminal.sh`
- Logger alle meldinger fra terminal
- Viser eksakt byte-sekvens
- Identifiser kommandoformat

### 3. Implementer protokoll
Basert på logg:
```kotlin
// Parse terminal meldinger
// Send riktig betalingskommando
// Håndter status-oppdateringer
// Parse resultat (godkjent/avvist)
```

## 💻 Kode-eksempler

### Dispenser + Payment Flow
```kotlin
// Setup
val emulator = EhlDispenserEmulator(
    address = 1,
    pricePerLitreCents = 1  // 1 øre/liter
)
val terminal = TcpPaymentTerminal("192.168.0.4", 8009)

// Flow
emulator.unblock()          // Start fylling
Thread.sleep(2000)          // Simuler fylling (2 sek)
emulator.block()            // Stopp fylling
val amount = emulator.getAmount()  // Les beløp

// Payment
terminal.connect()
val result = terminal.requestPayment(amount)
if (result.status == APPROVED) {
    emulator.reset()
}
```

### Lytter for protokoll-analyse
```kotlin
ServerSocket(8009).use { server ->
    val socket = server.accept()
    val input = socket.getInputStream()
    
    while (true) {
        val data = input.read(buffer)
        println("HEX: ${data.toHex()}")
        println("Type: ${identifyMessageType(data[0])}")
        
        // Send ACK
        socket.getOutputStream().write(0x06)
    }
}
```

## 📝 Spørsmål til AI

1. **Hvilken terminal-modell er dette?** (se bilder)
2. **Hvilken protokoll bruker den?** (ZVT, Nets, OPI, annet)
3. **Hvordan sender vi betalingskommando?** (eksakt byte-sekvens)
4. **Hvordan tolker vi respons?** (status-koder)
5. **Trenger vi spesiell initialisering?** (handshake-sekvens)

## 🔍 Debugging-verktøy

### Kjør lytter
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./lytt-til-terminal.sh
```

### Sjekk tilkobling
```bash
# Test terminal tilgjengelighet
ping 192.168.0.4

# Test port
nc -zv 192.168.0.4 8009

# Se aktive forbindelser
lsof -i :8009
```

### Build og clean
```bash
# Clean backend
mvn clean

# Build
mvn compile

# Run tests
mvn test
```

## 📦 Filer for opplasting

### Inkluder:
- `src/` - All kildekode
- `pom.xml` - Maven konfigurasjon
- `*.md` - Dokumentasjon
- `*.sh` - Kjørbare scripts
- Bilder av terminal

### Ekskluder:
- `target/` - Kompilerte filer
- `.git/` - Git historikk
- `node_modules/` - Frontend dependencies (hvis relevant)

## 🎯 Forventet resultat

Når protokollen er implementert:
1. Terminal viser "SETT INN KORT" (eller lignende)
2. Bruker tapper kort
3. Terminal prosesserer (3 øre)
4. Respons: "GODKJENT" eller "AVVIST"
5. Vårt program mottar resultat
6. Dispenser resettes

## 📞 Teknisk kontekst

- **Språk**: Kotlin 1.9.23
- **JVM**: Java 21
- **Build**: Maven
- **Framework**: Ingen (ren Kotlin/Java)
- **Protokoll**: TCP/IP sockets
- **Testing**: JUnit 5

## ⚡ Rask kommando-referanse

```bash
# Lytt til terminal (ANBEFALT FØRST)
./lytt-til-terminal.sh

# Test med ekte terminal
./test-ekte-betaling.sh

# Kjør ECR server
./run-ecr-server.sh

# Clean og build
mvn clean compile

# Run specific class
java -cp target/classes:$(mvn -q dependency:build-classpath) \
  no.cloudberries.lpg.payment.SimpleEcrListener
```

---

**Vedlagt**: Bilder av betalingsterminal
