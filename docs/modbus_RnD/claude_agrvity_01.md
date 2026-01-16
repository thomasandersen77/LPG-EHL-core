Analyse: Pumpekontroll og Kommunikasjon med Fysiske Koblinger
🎯 Hovedfunn
CAUTION

VB6-kildekoden bruker IKKE Modbus/ADAM for pumpekontroll.

Pumpene styres via EHL4x-protokollen over RS485 seriell kommunikasjon.

📋 Hvordan Pumper Frigis (Authorize/Open)
Kommando: UNBLOCK (0x77)
Private Sub disp_unblock(...)
y(1) = &H10        ' STX (Start of Transmission)
y(2) = &H6         ' LEN (Total frame length = 6 bytes)
y(3) = dispnr(0)   ' ADR (Dispenser address = dispensernr + 32)
y(4) = &H77        ' CMD (UNBLOCK = 119 decimal)
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)  ' CHK (XOR checksum)
y(6) = &H36        ' ETX (End of Transmission)
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
End Sub
Full sekvens for å frigi pumpe:

PRESTART (0xC3 0x30) - Forbered dispenser
UNBLOCK (0x77) - Frigjør for levering
🛑 Hvordan Pumper Stoppes (Block)
Kommando: BLOCK (0x69)
Private Sub disp_block()
y(1) = &H10        ' STX
y(2) = &H6         ' LEN
y(3) = dispnr(0)   ' ADR
y(4) = &H69        ' CMD (BLOCK = 105 decimal)
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)  ' CHK
y(6) = &H36        ' ETX
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))

' Reset state variabler
DispUnblock = False
Disp_was_unblocked = False
Reset_disp (dispnr(0))
End Sub
📍 Adresseformat
Enhet	Adresse Formel	Eksempel
Dispenser	dispensernr + 32	Dispenser 1 → Addr 33
Container	rs485adrcontainer + 32	Container 2 → Addr 34
Kilde fra VB6:

dispnr(0) = lpgnorge.rsdispensere!dispensernr + 32
🔌 Kommunikasjonskanal
Parameter	Verdi	Kilde
Protokoll	EHL4x (Custom)	Ikke Modbus
Transport	RS485 Serial	MSComm1 i VB6
Baudrate	9600 (default)
pumpekontroll_clone.py
STX (fra PC)	0x10	STX_CONTROLLER
STX (fra Disp)	0x20	STX_DISPENSER
ETX	0x36	End of Transmission
Checksum	XOR	Alle bytes STX..DATA
📊 Komplett Kommandoliste (EHL Protokoll)
Cmd (Hex)	Decimal	Navn	Beskrivelse
0x69	105	BLOCK	Stopp/blokker dispenser
0x77	119	UNBLOCK	Start/frigjør dispenser
0x81	129	ZER/RESET	Nullstill kalkulator
0xC3	195	PRESTART	Forbered for levering
0x4B	75	STATE	Hent/sett dispenser-tilstand
0x4C	76	ERROR	Hent feilkode
0x45	69	VOLUME	Hent levert volum
0xC5	197	TANK	Hent tank-status bits
0x5C	92	PRICE	Sett/hent pris
0x75	117	PROG_W	Programmer beløp
0x6A	106	LINETEST	Test kommunikasjon
❓ Hvor er ADAM/Modbus?
Funn
ADAM-modulen ble referert kun i en separat .NET-applikasjon:

Fil: 04-Payment-Terminal-Baxi/Pushservice/

ADAM: Port:0 Location: 55 Timeout:500 ms
Husk å sette timeout i ADAM til 1860sec
Pushservice.exe (C#/.NET) inneholder:

ModbusTCP.dll - Modbus TCP klientbibliotek
Database-lesing av modbusaddress og modbusport fra Settings-tabellen
Konklusjon
IMPORTANT

ADAM-modulen brukes av Pushservice, IKKE av VB6 pumpekontroll.

Pushservice ser ut til å lese priser/tanknivåer og pushe til ekstern server, ikke styre selve pumpene.

🎯 Anbefaling for Kotlin-implementasjon
For din nye Kotlin-applikasjon:

1. EHL Protokoll (Pumpekontroll)
   Bruk EHL-protokollen som beskrevet ovenfor:

object EhlCommands {
const val STX_CONTROLLER: Byte = 0x10.toByte()
const val ETX: Byte = 0x36.toByte()

    const val CMD_BLOCK: Byte = 0x69.toByte()      // Stopp pumpe
    const val CMD_UNBLOCK: Byte = 0x77.toByte()    // Frigjør pumpe
    const val CMD_RESET: Byte = 0x81.toByte()      // Nullstill
    const val CMD_PRESTART: Byte = 0xC3.toByte()   // Forbered
}
fun buildFrame(addr: Int, cmd: Byte, data: ByteArray = byteArrayOf()): ByteArray {
val len = 6 + data.size
val header = byteArrayOf(0x10.toByte(), len.toByte(), addr.toByte(), cmd)
val chk = (header + data).fold(0) { acc, b -> acc xor (b.toInt() and 0xFF) }.toByte()
return header + data + byteArrayOf(chk, 0x36.toByte())
}
2. Hvis du trenger ADAM/Modbus
   Du må finne kildekoden til Pushservice eller:

Sjekk database-tabellen Settings for modbusaddress og modbusport
ADAM-6060 moduler bruker typisk coil-adresser 17-24 for 8 reléer
📁 Relevante Kildefiler
Fil	Beskrivelse
pumpekontroll.frm
VB6 hovedlogikk
defs.bas
VB6 konstanter og variabler
protocol.py
Python EHL protokoll
pumpekontroll_clone.py
Python klon av VB6-logikk
EHL_MODBUS_PROTOCOL_ANALYSIS.md
Full protokollanalyse
