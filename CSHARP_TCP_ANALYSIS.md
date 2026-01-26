# Grundig Analyse: C# TCP-kommunikasjon vs Kotlin EHL-protokoll

> **Dato:** 2026-01-23  
> **Kilde:** CSharpConverted-V2 (TCP) vs lpg-ehl (Kotlin/EHL)

---

## 1. C# TCP-arkitektur (CSharpConverted-V2)

### `TcpClientWrapper.cs` - Asynkron TCP-klient

**Arkitektur:**
- **Event-basert modell**: Bruker `EventHandler<string> DataReceived` for asynkrone meldinger
- **Kontinuerlig lytteloop**: `ReceiveLoop()` kjører i bakgrunnen og lytter etter data
- **UTF-8 strengbasert**: Alle meldinger er tekststrenger, ikke binære pakker
- **Fire-and-forget pattern**: Sender meldinger og mottar svar asynkront (ikke request/response)

### TCP-kommunikasjonsflyt

```
[C# App] <--TCP/IP--> [Server/Dispenser på 192.168.0.41:9000]
         |
         ├─ SendAsync(msg) → UTF-8 bytes → TCP stream
         └─ ReceiveLoop() ← UTF-8 bytes ← DataReceived event
```

### Kjernekomponenter

#### TcpClientWrapper.cs
```csharp
public class TcpClientWrapper
{
    private readonly string _host;
    private readonly int _port;
    private TcpClient? _client;
    private CancellationTokenSource? _cts;

    public event EventHandler<string>? DataReceived;

    public async Task ConnectAsync()
    {
        _client = new TcpClient();
        _cts = new CancellationTokenSource();
        await _client.ConnectAsync(_host, _port);
        _ = ReceiveLoop(_cts.Token);  // Start bakgrunns-lytting
    }

    public async Task SendAsync(string msg)
    {
        var data = Encoding.UTF8.GetBytes(msg);
        await _client.GetStream().WriteAsync(data, 0, data.Length);
    }

    private async Task ReceiveLoop(CancellationToken ct)
    {
        var buf = new byte[4096];
        var stream = _client!.GetStream();
        while (!ct.IsCancellationRequested)
        {
            var read = await stream.ReadAsync(buf, 0, buf.Length, ct);
            if (read == 0) break;
            var s = Encoding.UTF8.GetString(buf, 0, read);
            DataReceived?.Invoke(this, s);  // Event-trigger
        }
    }
}
```

### Meldingsformat (tekstbasert protokoll)

Meldinger er semikolon-separerte strenger:
- **Format:** `<COMMAND>;param1;param2;...<SLUTT>`
- **Eksempler:**
  - `<TANK_DISP_UNBLOCK>;0;<SLUTT>` - Frigi dispenser
  - `<TANK_DISP_STOP>;<SLUTT>` - Stopp tanking
  - `<NOTAX>1;<SLUTT>` - Avgiftsfri modus på

### Kommandoer mottatt fra server

| Kommando | Funksjon | Parametre |
|----------|----------|-----------|
| `<RESTART>` | Restart-melding med status | status-tekst |
| `<TANK>` | Tankingsstatus | beløp, antall, pris, bank-flag, bank-tekst |
| `<TAX>` | Avgiftsinformasjon | avgiftsverdi |
| `<STATE_TANK>` | Dispenserstatus | statusflagg (pos 4 = frigitt) |
| `<TANK_STOP>` | Tanking avsluttet | - |
| `<PRICE>` | Prisinformasjon | - |
| `<TANK_TERMINAL_MESSAGE>` | Terminal-melding | melding |

### Meldingshåndtering i DispenserkontrollForm.cs

```csharp
public void TcpClient_ProcessMessage(string raw)
{
    var parts = raw.Split(';');
    var cmd = parts[0].Trim();
    
    switch (cmd)
    {
        case "<RESTART>":
            if (parts.Length > 1) UpdateStatus(parts[1]);
            break;
        case "<TANK>":
            if (parts.Length > 1) SetBelop(parts[2]);
            if (parts.Length > 2) SetAntall(parts[3]);
            if (parts.Length > 3) SetPris(parts[4]);
            if (parts.Length > 4) bank.Visible = (parts[5] == "1");
            if (parts.Length > 5) bank.Text = parts[6];
            break;
        case "<STATE_TANK>":
            // Sjekker pos 4 i payload for frigitt-status
            var payload = parts[1];
            if (payload.Length >= 5 && payload[4] == '1')
                cmdStart.Text = "Frigitt";
            else
                cmdStart.Text = "Frigi dispenser";
            break;
        // ... flere kommandoer
    }
}
```

---

## 2. Kotlin EHL-protokoll (lpg-ehl)

### Arkitektur-forskjeller

Kotlin-versjonen bruker en helt annen arkitektur:

**Transport-lag:**
- **RS-485 seriell kommunikasjon** (ikke TCP)
- **Binær protokoll** (ikke tekstbasert)
- **Request/Response pattern** (ikke event-basert)
- **Mutex-beskyttet** for trådsikkerhet

### EhlCommunicator.kt - Synkron request/response

```kotlin
class EhlCommunicator(private val transport: SerialTransport) {
    private val txMutex = Mutex()  // Sikrer kun én request om gangen
    
    suspend fun sendAndReceive(
        packet: EhlPacket, 
        timeoutMs: Long = 2000
    ): EhlPacket {
        return txMutex.withLock {
            withTimeout(timeoutMs) {
                send(packet)
                receive(timeoutMs)
            }
        }
    }
}
```

### EHL Protokoll-spesifikasjon

```
┌─────┬─────────┬─────────┬──────┬──────┐
│ STX │ Address │ Command │ Data │ CHK  │
│ 1B  │   1B    │   1B    │ var  │ 1B   │
└─────┴─────────┴─────────┴──────┴──────┘
```

| Felt | Størrelse | Beskrivelse |
|------|-----------|-------------|
| STX | 1 byte | Start-byte (0x02 fra kontroller, 0x03 fra dispenser) |
| Address | 1 byte | Dispenser-adresse (0-31) |
| Command | 1 byte | EHL-kommando (STATE, UNBLOCK, BLOCK, etc.) |
| Data | variabel | Kommandospesifikk data |
| CHK | 1 byte | Checksum for dataintegritet |

### Bufferhåndtering og feilgjenoppretting

Kotlin-implementasjonen har robust feilhåndtering:

```kotlin
private fun handleCorruptedPacketRecovery() {
    // Søker etter neste STX-byte for å gjenopprette synkronisering
    val remainingBuffer = receiveBuffer.drop(1)
    val nextStxIndex = remainingBuffer.indexOfFirst { 
        it == EhlProtocol.STX_CONTROLLER || it == EhlProtocol.STX_DISPENSER 
    }
    
    if (nextStxIndex >= 0) {
        receiveBuffer.subList(0, nextStxIndex + 1).clear()
    } else {
        receiveBuffer.removeAt(0)  // Minimal datatap
    }
}
```

---

## 3. Nøkkelforskjeller

| Aspekt | C# (TCP) | Kotlin (EHL/RS-485) |
|--------|----------|---------------------|
| **Transport** | TCP/IP socket | RS-485 seriell |
| **Protokoll** | Tekstbasert (`<CMD>;data;`) | Binær pakkeformat |
| **Meldingsmodell** | Asynkron event-basert | Synkron request/response |
| **Feilhåndtering** | Minimal (try/catch ignorerer) | Robust (checksum, buffer-recovery) |
| **Trådsikkerhet** | Ingen eksplisitt | Mutex-beskyttet |
| **Reconnect** | Timer-basert (5 sek intervall) | Manuell/konfigurerbar |
| **Meldingsframing** | Ingen (stoler på TCP) | STX/CHK-basert |
| **Dataintegritet** | Ingen validering | Checksum-verifisering |

---

## 4. C# TCP-kommunikasjonens svakheter

### 4.1 Ingen meldingsframing
TCP er en stream-protokoll uten meldingsgrenser. Koden antar at hele meldingen kommer i én `ReadAsync`:

```csharp
// PROBLEM: Kan motta partial reads
var read = await stream.ReadAsync(buf, 0, buf.Length, ct);
var s = Encoding.UTF8.GetString(buf, 0, read);
DataReceived?.Invoke(this, s);
```

**Risiko:** Hvis en melding er `<TANK>;100;5;20;1;Visa<SLUTT>` og TCP fragmenterer den til `<TANK>;100;5` og `;20;1;Visa<SLUTT>`, vil parsing feile.

### 4.2 Ingen checksum/validering
- Stoler blindt på at data er korrekt
- Ingen verifisering av meldingsintegritet
- Ingen deteksjon av korrupte meldinger

### 4.3 Svak feilhåndtering

```csharp
// Alle feil ignoreres stille
catch { }

// Ingen logging, ingen gjenoppretting
catch (OperationCanceledException) { break; }
catch { break; }
```

### 4.4 Hardkodede verdier

```csharp
public static string ClientsrvLocal = "192.168.0.41"; 
public static int ClientsrvLocalTcpPort = 9000;
```

---

## 5. Meldingsprotokoll-detaljer (C#)

### Utgående meldinger (fra C# til server)

| Melding | Beskrivelse |
|---------|-------------|
| `<TANK_DISP_UNBLOCK>;0;<SLUTT>` | Frigi dispenser (med avgift) |
| `<TANK_DISP_UNBLOCK_NOTAX>;0;<SLUTT>` | Frigi dispenser (avgiftsfri) |
| `<TANK_DISP_STOP>;<SLUTT>` | Stopp tanking |
| `<NOTAX>1;<SLUTT>` | Aktiver avgiftsfri modus |
| `<NOTAX>0;<SLUTT>` | Deaktiver avgiftsfri modus |

### Innkommende meldinger (fra server til C#)

| Melding | Format | Eksempel |
|---------|--------|----------|
| `<TANK>` | `<TANK>;_;beløp;antall;pris;bank_flag;bank_tekst` | `<TANK>;_;150.50;12.5;12.04;1;VISA` |
| `<STATE_TANK>` | `<STATE_TANK>;statusflags` | `<STATE_TANK>;00001` |
| `<TAX>` | `<TAX>;verdi` | `<TAX>;25` |

---

## 6. Anbefalinger for Kotlin-integrasjon

### 6.1 Hvis du skal kommunisere med samme server (TCP)

Implementer en TCP-adapter som wrapper den tekstbaserte protokollen:

```kotlin
interface LegacyProtocolAdapter {
    suspend fun send(command: String)
    fun onMessage(handler: (String) -> Unit)
}

class TcpLegacyAdapter(
    private val host: String, 
    private val port: Int
) : LegacyProtocolAdapter {
    
    private val socket: Socket
    private val buffer = StringBuilder()
    
    suspend fun connect() {
        // Koble til TCP
    }
    
    override suspend fun send(command: String) {
        // Send UTF-8 tekst
    }
    
    private suspend fun receiveLoop() {
        // Les data og buffer til <SLUTT> er funnet
        // Parse og trigger handler for hver komplett melding
    }
}
```

### 6.2 Meldingsframing-løsning

Bruk `<SLUTT>` som meldingsslutt-markør:

```kotlin
private fun processBuffer() {
    val endMarker = "<SLUTT>"
    while (buffer.contains(endMarker)) {
        val endIndex = buffer.indexOf(endMarker)
        val message = buffer.substring(0, endIndex + endMarker.length)
        buffer.delete(0, endIndex + endMarker.length)
        handleMessage(message)
    }
}
```

### 6.3 Meldingsparser

```kotlin
data class LegacyMessage(
    val command: String,
    val params: List<String>
)

fun parseLegacyMessage(raw: String): LegacyMessage {
    val parts = raw.split(";")
    return LegacyMessage(
        command = parts[0].trim(),
        params = parts.drop(1).dropLast(1)  // Fjern <SLUTT>
    )
}
```

---

## 7. Konklusjon

C#-koden representerer en legacy TCP-basert protokoll som er enklere men mindre robust enn Kotlin EHL-implementasjonen. Hovedforskjellene:

1. **C# bruker TCP/IP** med tekstbaserte meldinger
2. **Kotlin bruker RS-485** med binær EHL-protokoll
3. **C# er event-basert**, Kotlin er request/response
4. **Kotlin har mye bedre feilhåndtering** med checksum og buffer-recovery

For å integrere de to systemene, må du enten:
- Implementere en TCP-adapter i Kotlin som snakker det tekstbaserte formatet
- Eller bygge en bro/gateway som oversetter mellom protokollene
