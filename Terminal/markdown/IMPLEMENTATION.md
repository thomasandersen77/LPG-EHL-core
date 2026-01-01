# LPG-EHL Core Implementation

## Oversikt

Dette er en Kotlin-implementasjon av EHL-protokollen (European Hexadecimal Language) for kommunikasjon med LPG-dispensere via RS-485. Koden er portet fra Visual Basic 6 og modernisert med ryddige, testbare komponenter.

## Implementerte funksjoner

### 1. EHL Protokoll (`no.cloudberries.lpg.protocol`)

#### EHL Commands (`EhlCommands.kt`)
Definerer alle EHL-kommandoer som enum med tilhørende koder:

- **STATE (75)**: Hent/sett dispenser-tilstand
- **UNBLOCK (119)**: Start leveringsmodus
- **BLOCK (105)**: Stopp dispenser
- **VOLUME (69)**: Hent/sett volumdata
- **PRICE (92)**: Hent/sett pris
- **PROG_PRC (169)**: Programmer drivstoffpris
- **PROG_W (117)**: Programmer verdi (preset beløp)
- **PROG_I (112)**: Programmer volum (preset liter)
- **LINETEST (106)**: Test overføring
- **ZER (129)**: Nullstill kalkulator
- **OK (30)**: Kommando-bekreftelse
- **ERROR (37)**: Feilkode
- **STOP (47)**: Stopp dispenser
- **TANK (197)**: Tank-status/kontroll

#### EHL Packet Structure (`EhlPacket.kt`)
Dataklasse for komplett EHL-pakke:

```
STX (0x20) | Length | Address | Command | Data (0-n bytes) | Checksum (XOR) | ETX (0x36)
```

**Eksempel:**
```
0x20 0x07 0x01 0x4B 0x05 0x6F 0x36
  │    │    │    │    │    │    └─ ETX
  │    │    │    │    │    └─ Checksum (XOR av alle bytes)
  │    │    │    │    └─ Data payload
  │    │    │    └─ Kommando (STATE = 0x4B = 75)
  │    │    └─ Dispenser-adresse
  │    └─ Total lengde
  └─ STX (Start of transmission)
```

**Funksjoner:**
- `calculateChecksum()`: Beregn XOR-checksum
- Validering av adresse (1-255)
- Typesikker equals/hashCode

#### Codec (`EhlCodec.kt`)
Enkoder og dekoder for EHL-pakker:

**Encoding:**
```kotlin
val packet = EhlPacket(address = 1, command = EhlCommand.STATE)
val bytes = EhlCodec.encode(packet)
// bytes = [0x20, 0x06, 0x01, 0x4B, 0x6C, 0x36]
```

**Decoding:**
```kotlin
when (val result = EhlCodec.decode(bytes)) {
    is EhlPacketParseResult.Success -> println("OK: ${result.packet}")
    is EhlPacketParseResult.ChecksumError -> println("Checksum feil")
    is EhlPacketParseResult.InvalidFormat -> println("Format feil")
    is EhlPacketParseResult.Incomplete -> println("Ufullstendig pakke")
}
```

**Packet Builders:**
```kotlin
// Ferdiglagde builders for vanlige kommandoer
EhlPacketBuilder.createStateQuery(address = 1)
EhlPacketBuilder.createUnblock(address = 1)
EhlPacketBuilder.createBlock(address = 1)
EhlPacketBuilder.createPriceProgram(address = 1, price = "15.90")
EhlPacketBuilder.createValuePreset(address = 1, amount = 50000) // 500 kr
```

### 2. Transaksjonshåndtering (`no.cloudberries.lpg.transaction`)

#### Transaction State Machine (`Transaction.kt`)

**Tilstander:**
```
NOT_STARTED → READY → ACTIVE → FINISHED → ACCOUNTED
                 ↓                  ↓
              ANNULATED       UNACCOUNTED
```

**TransactionState enum:**
- `NOT_STARTED (0)`: Ikke startet
- `READY (1)`: Klar for levering
- `ACTIVE (2)`: Levering pågår
- `FINISHED (3)`: Levering ferdig
- `UNACCOUNTED (4)`: Uhåndtert (strømbrudd)
- `FINANCIAL_RETURN (5)`: Økonomisk retur
- `FINANCIAL_TECH_RETURN (6)`: Teknisk retur
- `ANNULATED (7)`: Annullert
- `ACCOUNTED (8)`: Regnskapsført

**PaymentType enum:**
- `DEFAULT (0)`: Ikke satt
- `CASH (1)`: Kontant
- `BANK_CARD (2)`: Bankkort
- `STATION_CARD (3)`: Stasjonskort

#### Transaction Data Class
Komplett transaksjonsdata:

```kotlin
data class Transaction(
    val id: String,                      // Unik ID
    val dispenserAddress: Int,           // Dispenser-adresse (1-255)
    var state: TransactionState,         // Nåværende tilstand
    var paymentType: PaymentType,        // Betalingsmetode
    var presetAmount: Int,               // Preset beløp (øre)
    var deliveredVolume: Float,          // Levert volum (liter)
    var deliveredAmount: Int,            // Levert beløp (øre)
    var unitPrice: Float,                // Enhetspris (kr/liter)
    var cashbackAmount: Int,             // Cashback (øre)
    val startTime: Instant,              // Starttid
    var endTime: Instant?                // Sluttid
)
```

**Funksjoner:**
- `transitionTo(newState)`: Validert tilstandsovergang
- `isFinalized()`: Sjekk om ferdig
- `canStart()`: Sjekk om kan startes
- `isActive()`: Sjekk om aktiv

**Eksempel:**
```kotlin
val txn = Transaction(id = "TXN-001", dispenserAddress = 1)
txn.transitionTo(TransactionState.READY)
txn.transitionTo(TransactionState.ACTIVE)

// Simular levering
txn.deliveredVolume = 45.5f
txn.deliveredAmount = 72950  // 729.50 kr
txn.unitPrice = 16.04f
txn.paymentType = PaymentType.BANK_CARD

txn.transitionTo(TransactionState.FINISHED)
```

#### TransactionManager
Administrerer aktive transaksjoner:

```kotlin
val manager = TransactionManager()

// Start transaksjon
val txn = manager.startTransaction(dispenserAddress = 1)

// Hent aktiv transaksjon
val active = manager.getTransaction(1)

// Avslutt transaksjon
val finished = manager.finalizeTransaction(1)

// Hent alle aktive
val all = manager.getAllActiveTransactions()
```

## Testing

### Kjør alle tester
```bash
mvn test
```

### Test coverage

**EHL Protocol Tests (EhlCodecTest.kt):**
- ✓ Encoding av enkle pakker
- ✓ Encoding med data payload
- ✓ Decoding av gyldige pakker
- ✓ Validering av STX/ETX
- ✓ Checksum-validering
- ✓ Håndtering av ufullstendige pakker
- ✓ Round-trip encode/decode
- ✓ Packet builders
- ✓ Prisformatering

**Transaction Tests (TransactionTest.kt):**
- ✓ Initialtilstand
- ✓ Gyldige tilstandsoverganger
- ✓ Ugyldige tilstandsoverganger
- ✓ Finaliserte tilstander
- ✓ Enum-resolusjon
- ✓ Transaksjonsdetaljer

**Transaction Manager Tests:**
- ✓ Start transaksjon
- ✓ Hent aktiv transaksjon
- ✓ Finalisering
- ✓ Flere aktive transaksjoner
- ✓ Validering av adresse

**Test Results:**
```
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
```

## Bruk

### Kjør demo
```bash
mvn -q exec:java
```

Output:
```
=== LPG/EHL Core - Protocol Implementation ===

1. EHL Protocol Packet Demo:
   Creating STATE query for dispenser address 1...
   Encoded packet: 20 06 01 4B 6C 36
   Packet details: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=6C)

2. Decoding EHL Packet:
   ✓ Successfully decoded: EhlPacket(addr=1, cmd=STATE(75), data=[], chksum=6C)

3. Transaction State Machine Demo:
   Transaction created: TXN-1765479472173-2838
   Initial state: Ready
   → Transitioned to: Active
   → Transitioned to: Finished
   Delivered: 45.5 L @ 16.04 kr/L
   Total: 729.5 kr (Bank card)

4. Available EHL Commands:
   - OK           (code  30): Command acknowledgement
   - ERROR        (code  37): Error code data
   - STOP         (code  47): Stop the dispenser
   - STATE        (code  75): Give/take the calculator state
   ...
```

## Kode-kvalitet

### Dokumentasjon
- ✓ KDoc-kommentarer på alle public klasser og funksjoner
- ✓ Eksempler i kode-kommentarer
- ✓ Forklaring av EHL-protokoll-struktur
- ✓ Tydelige enum-beskrivelser

### Design
- ✓ Immutable data structures der mulig
- ✓ Sealed classes for result types
- ✓ Type-safe enums
- ✓ Builder patterns for complex packets
- ✓ Validering av input-data
- ✓ Logging med SLF4J

### Testing
- ✓ Unit tests for alle komponenter
- ✓ Edge case testing
- ✓ Round-trip testing
- ✓ Error path testing
- ✓ Descriptive test names

## Neste steg

For å fullføre porteringen fra VB6-koden, kan følgende komponenter legges til:

1. **RS-485 Communication Handler**
   - Serial port communication
   - Async message handling
   - Timeout management
   - Retry logic

2. **Dispenser State Management**
   - Poll dispenser state
   - Handle state changes
   - Error recovery

3. **Integration med betalingssystem**
   - Bank card handling
   - Station credit cards
   - Cash handling

4. **Database integration**
   - Transaction persistence
   - Reporting
   - Audit logging

5. **REST API / Service layer**
   - Expose functionality over HTTP
   - WebSocket for real-time updates
   - Admin interface

## Avhengigheter

- Kotlin 1.9.23
- Java 21
- SLF4J 2.0.13 (logging)
- Logback 1.5.6 (logging implementation)
- JUnit 5.10.2 (testing)

## Portering fra VB6

Denne implementasjonen erstatter følgende VB6-moduler:

- `defs.bas` → `EhlCommands.kt` + `Transaction.kt`
- `fra_dispenser.bas` → `EhlCodec.kt` + `EhlPacket.kt`
- `Transaction.cls` → `Transaction.kt`

**Forbedringer over VB6-koden:**
- Type-safety (kompilator fanger feil)
- Immutability (færre bugs)
- Testbar arkitektur
- Moderne logging
- Funksjonell error handling
- Ryddig separation of concerns
