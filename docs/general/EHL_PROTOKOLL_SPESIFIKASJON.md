# EHL-protokoll Teknisk Spesifikasjon

**Formål**: Teknisk dokumentasjon av EHL-protokollen (European Hexadecimal Language) for LPG-dispenserkontroll, basert på analyse av VB6-kildekode og Python-reimplementasjon.

> [!NOTE]
> Python-reimplementasjonen i `ehl_pumpekontroll_clone/` er verifisert å være 100% protokoll-identisk med VB6-koden på wire-nivå.

---

## Innholdsfortegnelse

1. [EHL-protokoll Oversikt](#ehl-protokoll-oversikt)
2. [Rammeformat](#rammeformat)
3. [Checksum-algoritme](#checksum-algoritme)
4. [Kommandokoder](#kommandokoder)
5. [Dataformater](#dataformater)
6. [Seriell Kommunikasjon](#seriell-kommunikasjon)
7. [COM-port til Linux-mapping](#com-port-til-linux-mapping)
8. [Konfigurasjon (application.yaml)](#konfigurasjon-applicationyaml)
9. [VB6 Kildekodereferanser](#vb6-kildekodereferanser)

---

## EHL-protokoll Oversikt

EHL (European Hexadecimal Language) er en binær protokoll for kommunikasjon mellom en kontroller (PC) og LPG-dispensere over RS-485.

### Kommunikasjonsretninger

| Retning | STX-byte | Beskrivelse |
|---------|----------|-------------|
| Controller → Dispenser | `0x10` | Kommandoer og forespørsler |
| Dispenser → Controller | `0x20` | Svar og statusoppdateringer |

### Grunnleggende egenskaper

- **Halv-dupleks** kommunikasjon over RS-485
- **Polling-basert**: Kontrolleren poller dispenseren hvert sekund
- **Adresserbar**: Støtter flere dispensere på samme buss
- **XOR-checksum** for feildeteksjon

---

## Rammeformat

Alle EHL-meldinger følger dette formatet:

```
┌─────┬─────┬──────┬─────┬────────────┬─────┬─────┐
│ STX │ LEN │ ADDR │ CMD │   DATA...  │ CHK │ ETX │
└─────┴─────┴──────┴─────┴────────────┴─────┴─────┘
  1B    1B    1B     1B    0-n bytes    1B    1B
```

### Feltbeskrivelser

| Felt | Størrelse | Beskrivelse |
|------|-----------|-------------|
| **STX** | 1 byte | Start-byte: `0x10` (til dispenser) eller `0x20` (fra dispenser) |
| **LEN** | 1 byte | Total rammelengde inkludert ETX |
| **ADDR** | 1 byte | Dispenser-adresse (beregnet fra dispensernummer) |
| **CMD** | 1 byte | Kommandokode |
| **DATA** | 0-n bytes | Kommandospesifikke data |
| **CHK** | 1 byte | XOR-checksum |
| **ETX** | 1 byte | End-byte: alltid `0x36` |

### Protokollkonstanter

```kotlin
const val STX_CONTROLLER = 0x10  // 16 desimal
const val STX_DISPENSER = 0x20   // 32 desimal  
const val ETX = 0x36             // 54 desimal
```

### Adresseberegning

**Dispenser-adresse = dispensernummer + 32**

Fra VB6 (`pumpekontroll.frm` linje 1982):
```vb
dispnr(0) = lpgnorge.rsdispensere!dispensernr + 32
```

| Dispenser nr. | Adresse (desimal) | Adresse (hex) |
|---------------|-------------------|---------------|
| 1 | 33 | 0x21 |
| 2 | 34 | 0x22 |
| 3 | 35 | 0x23 |

### Beregning av LEN

**LEN = 6 + antall databytes**

Minimumslengde (uten data): 6 bytes (STX + LEN + ADDR + CMD + CHK + ETX)

---

## Checksum-algoritme

### XOR Checksum

Checksum beregnes som XOR over alle bytes **fra STX til og med siste DATA-byte** (ekskluderer CHK og ETX).

#### Algoritme (Kotlin)

```kotlin
fun xorChecksum(bytes: ByteArray): Byte {
    var chk = 0
    for (b in bytes) {
        chk = chk xor (b.toInt() and 0xFF)
    }
    return (chk and 0xFF).toByte()
}
```

#### VB6-implementasjon

Fra `defs.bas` og `pumpekontroll.frm`:

```vb
' Sending (bygger checksum):
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)

' Mottak (validerer checksum):
For i = 0 To u - 2
    chksum = chksum Xor x(i)
Next
If chksum = x(u - 1) Then   ' Gyldig ramme
```

#### Eksempel: STATE-kommando

```
Ramme:   10  06  21  4B  [CHK]  36
         STX LEN ADDR CMD       ETX

CHK = 0x10 XOR 0x06 XOR 0x21 XOR 0x4B = 0x7C
```

Komplett ramme: `10 06 21 4B 7C 36`

---

## Kommandokoder

### Oversikt

| Kommando | Hex | Desimal | Retning | LEN | Beskrivelse |
|----------|-----|---------|---------|-----|-------------|
| STATE | 0x4B | 75 | Begge | 6/7 | Hent kalkulatortilstand |
| VOLUME | 0x45 | 69 | Begge | 6/11 | Hent drivstoffmengde |
| PRICE | 0x5C | 92 | Begge | 6/10 | Hent/sett drivstoffpris |
| LINETEST | 0x6A | 106 | Begge | 6/8 | Test kommunikasjonskanal |
| BLOCK | 0x69 | 105 | C→D | 6 | Stopp/blokker dispenser |
| UNBLOCK | 0x77 | 119 | C→D | 6 | Start leveringsmodus |
| ZER/RESET | 0x81 | 129 | C→D | 6 | Nullstill kalkulator |
| PRESTART | 0xC3 | 195 | C→D | 7 | Forhåndsstart (data: 0x30) |
| TANK | 0xC5 | 197 | Begge | 6/7 | Tank-status |
| PROG_PRC | 0xA9 | 169 | C→D | 10 | Programmer pris |
| OK | 0x1E | 30 | D→C | 6 | Bekreftelse |
| ERROR | 0x25 | 37 | D→C | 7 | Feilkode |

### Kotlin-konstanter

```kotlin
object EhlCommands {
    const val STATE = 0x4B
    const val VOLUME = 0x45
    const val PRICE = 0x5C
    const val LINETEST = 0x6A
    const val BLOCK = 0x69
    const val UNBLOCK = 0x77
    const val RESET_ZER = 0x81
    const val PRESTART = 0xC3
    const val TANK = 0xC5
    const val PROG_PRC = 0xA9
    const val OK = 0x1E
    const val ERROR = 0x25
}
```

### Detaljer per kommando

#### STATE (0x4B)

**Request** (Controller → Dispenser):
```
10 06 [ADDR] 4B [CHK] 36
```

**Response** (Dispenser → Controller):
```
20 07 [ADDR] 4B [STATE_BYTE] [CHK] 36
```

STATE_BYTE bit-tolkning:
| Bit | Maske | Betydning |
|-----|-------|-----------|
| 3 | 0x08 | automode |
| 2 | 0x04 | startbuttonpressed |
| 1 | 0x02 | openfordelivery |

#### LINETEST (0x6A)

Brukes for å teste kommunikasjonskanalen.

**Response** inneholder testpattern `0x55 0xAA`:
```
20 08 [ADDR] 6A 55 AA [CHK] 36
```

#### PRESTART (0xC3)

Sender alltid databyte `0x30`:
```
10 07 [ADDR] C3 30 [CHK] 36
```

---

## Dataformater

### VOLUME (5 bytes, LSB-first ASCII)

Volume-data er 5 ASCII-sifre som representerer liter med 2 desimaler:

```
data[0] = 0.01 L (centi-liter)
data[1] = 0.1 L  (desi-liter)
data[2] = 1 L
data[3] = 10 L
data[4] = 100 L
```

**Eksempel**: `30 35 35 34 30` (ASCII "05540") = 45.50 L

VB6-tolkning (`pumpekontroll.frm`):
```vb
CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
```

### PRICE (4 bytes, LSB-first ASCII)

Pris-data er 4 ASCII-sifre som representerer kr/L med 2 desimaler:

```
data[0] = 0.01 kr
data[1] = 0.1 kr
data[2] = 1 kr
data[3] = 10 kr
```

**Eksempel**: `30 34 36 31` (ASCII "0461") = 16.04 kr/L

VB6-tolkning:
```vb
Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
```

### TANK-status bits

Fra VB6 (`Case 197`):
| Bit | Maske | Betydning |
|-----|-------|-----------|
| 0 | 0x01 | trans_finished_powerfault |
| 3 | 0x08 | trans_unaccounted |

---

## Seriell Kommunikasjon

### Innstillinger

| Parameter | Verdi | VB6-kilde |
|-----------|-------|-----------|
| **Baud Rate** | 9600 | `.ptp`-filer, `serial_client.py` |
| **Data Bits** | 8 | MSComm default |
| **Parity** | None | `ParityReplace = 0` |
| **Stop Bits** | 1 | MSComm default |
| **Flow Control** | None | DTR brukes variabelt |

### MSComm-kontroll egenskaper (VB6)

Fra `pumpekontroll.frm` (MSComm1):

| Egenskap | Verdi | Betydning |
|----------|-------|-----------|
| `InputLen` | 1 | Les 1 byte av gangen |
| `RThreshold` | 1 | Trigger event ved hver byte |
| `NullDiscard` | True | Forkast null-bytes |
| `DTREnable` | True | Data Terminal Ready aktiv |

### Buffer-håndtering

- **Maksimal bufferstørrelse**: 16 bytes
- **Overflow**: Buffer nullstilles ved `u > 15`
- **Ramme-validering**: Kun ved mottak av ETX (0x36)

Fra VB6:
```vb
If u > 15 Then u = -1: commandtext = ""  ' Reset ved overflow
```

### Timing

| Parameter | Verdi | VB6-kilde |
|-----------|-------|-----------|
| Poll-intervall | 1000 ms | `state_timer.Interval` |
| Kommando-ventetid | 100 ms | `comm_out` wait parameter |
| LINETEST ventetid | 500 ms | Eksplisitt i kode |

---

## COM-port til Linux-mapping

### VB6 Standard-porter

| VB6 CommPort | Funksjon | Linux-ekvivalent |
|--------------|----------|------------------|
| COM3 | EHL/Dispenser (MSComm1) | `/dev/ttyUSB0` eller `/dev/ttyS2` |
| COM4 | Bankterminal (com_pinpad) | `/dev/ttyUSB1` eller `/dev/ttyS3` |
| COM5 | Kvitteringsskriver (com_print) | `/dev/ttyUSB2` eller `/dev/ttyS4` |
| COM6 | RFID-leser (RFIDCOM) | `/dev/ttyUSB3` eller `/dev/ttyS5` |

### Navnekonvensjoner i Linux

| Type | Linux-enhet | Beskrivelse |
|------|-------------|-------------|
| Integrert | `/dev/ttyS0`, `/dev/ttyS1`, ... | Innebygde seriellporter |
| USB-adapter | `/dev/ttyUSB0`, `/dev/ttyUSB1`, ... | USB-til-seriell adaptere |
| ACM | `/dev/ttyACM0`, `/dev/ttyACM1`, ... | USB CDC ACM-enheter |

### Stabil enhetsnavn med udev

For å sikre konsistent navngivning ved omstart:

```bash
# /etc/udev/rules.d/99-serial.rules
SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", \
    ATTRS{serial}=="FT123456", SYMLINK+="ehl_dispenser"
```

Bruk deretter `/dev/ehl_dispenser` i konfigurasjon.

---

## Konfigurasjon (application.yaml)

### Eksempel for Kotlin/Spring Boot-applikasjon

```yaml
ehl:
  # Profil: 'lab' for simulert, 'field' for ekte seriellport
  mode: field
  
  serial:
    port: /dev/ttyUSB0        # Linux: /dev/ttyUSB0, Windows: COM3
    baudrate: 9600
    databits: 8
    parity: NONE              # NONE, EVEN, ODD
    stopbits: 1
    timeout-ms: 100
    
  protocol:
    stx-controller: 0x10
    stx-dispenser: 0x20
    etx: 0x36
    max-frame-size: 16
    
  dispenser:
    address-offset: 32        # ADDR = dispensernr + offset
    dispenser-number: 1       # Resulterer i ADDR = 33
    
  timing:
    poll-interval-ms: 1000
    command-wait-ms: 100
    linetest-wait-ms: 500
    
  lab:
    enabled: false            # true for in-memory simulering
    simulator-response-delay-ms: 50
```

### Profil-spesifikk konfigurasjon

**Lab-modus** (`application-lab.yaml`):
```yaml
ehl:
  mode: lab
  lab:
    enabled: true
```

**Felt-modus** (`application-field.yaml`):
```yaml
ehl:
  mode: field
  serial:
    port: /dev/ttyUSB0
```

Kjør med profil:
```bash
java -jar ehl-controller.jar --spring.profiles.active=field
```

---

## VB6 Kildekodereferanser

### Hovedfiler

| Fil | Innhold |
|-----|---------|
| `pumpekontroll.frm` | Hovedskjema, MSComm-kontroller, `MSComm1_OnComm()` mottakslogikk |
| `defs.bas` | Globale variabler, `comm_out()`, konfigurasjonslasting fra `server.ini` |
| `fra_dispenser.bas` | Protokollimplementasjon, kommandohåndtering |
| `dispensere.frm` | Dispenser-konfigurasjon UI |

### Kritiske funksjoner

**Mottakslogikk** (`pumpekontroll.frm` linje ~2511):
```vb
Private Sub MSComm1_OnComm()
    Select Case MSComm1.CommEvent
        Case comEvReceive
            charstr = MSComm1.Input
            ' Byte-for-byte parsing...
            If x(u) = &H36 And x(0) = &H20 And x(1) = (u + 1) Then
                ' Valider checksum og prosesser kommando
            End If
    End Select
End Sub
```

**Sendingslogikk** (`defs.bas` linje ~528):
```vb
Sub comm_out(Waittime As Integer, commstr As String)
    While Not rts
        DoEvents
    Wend
    If Pumpekontroll.MSComm1.PortOpen Then Pumpekontroll.MSComm1.Output = commstr
    commandtext = ""
    Sleep (Waittime)
End Sub
```

### Konfigurasjonsfil (server.ini)

Format: semikolon-separerte verdier på én linje

```
DBserver;DBdb;DBbrukernavn;DBpassord;Com_port;Com_port_bank;...
```

Relevant felt for seriellkommunikasjon:
- Indeks 4: `Com_port` - EHL dispenser COM-port nummer

---

## Oppsummering: Kritiske Verdier

| Kategori | Parameter | Verdi |
|----------|-----------|-------|
| **Seriell** | Baud Rate | 9600 |
| **Seriell** | Data Bits | 8 |
| **Seriell** | Parity | None |
| **Seriell** | Stop Bits | 1 |
| **Protokoll** | STX (til dispenser) | 0x10 |
| **Protokoll** | STX (fra dispenser) | 0x20 |
| **Protokoll** | ETX | 0x36 |
| **Protokoll** | Checksum | XOR (STX..DATA) |
| **Protokoll** | Maks ramme | 16 bytes |
| **Protokoll** | Min ramme | 6 bytes |
| **Adressering** | Offset | dispensernr + 32 |
| **Timing** | Poll-intervall | 1000 ms |
| **Timing** | Kommando-ventetid | 100 ms |
