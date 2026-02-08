# BAXI-protokollen – Kotlin-implementasjonsrapport (fra dekompilering)

**Kilde:** [instructions/baxi-decompiled.md](../instructions/baxi-decompiled.md)  
**Formål:** Implementasjonsguide for Kotlin mot betalingsterminal basert på dekompilert BAXI .NET DLL  
**Versjon:** 1.0  
**Dato:** Februar 2026  

---

## Innhold

1. [Innledning](#innledning)
2. [Hva vi har fra dekompileringen](#hva-vi-har-fra-dekompileringen)
3. [Hva vi mangler](#hva-vi-mangler)
4. [Kotlin-implementasjonsstrategi](#kotlin-implementasjonsstrategi)
5. [Kodeeksempler (Kotlin)](#kodeeksempler-kotlin)
6. [TCP mot betalingsterminal](#tcp-mot-betalingsterminal)
7. [Oppdagelsesstrategi](#oppdagelsesstrategi)
8. [Oppsummering](#oppsummering)

---

## Innledning

Denne rapporten er basert utelukkende på **dekompileringen** av Nets BAXI .NET DLL (baxi_dotnet 1.3.2.0, BBS.BAXI namespace) beskrevet i [instructions/baxi-decompiled.md](../instructions/baxi-decompiled.md). Formålet er å beskrive hvordan man kan bygge en **Kotlin-implementasjon** mot en betalingsterminal som snakker BAXI-protokollen: hva vi kan utlede fra API-overflaten, hva som mangler (parametre, wire-format, TLD-tagger), og hvordan TCP-laget og event-håndtering bør designes.

Dekompileringen gir oss **hele API-overflaten** (BaxiCtrl, args, events), men som filen selv konkluderer: *«Vi har ikke parametrene»* – dvs. konkrete verdier for TLD-tagger, koder og wire-format må innhentes via trafikk-analyse eller dokumentasjon.

---

## Hva vi har fra dekompileringen

### BaxiCtrl – hovedklasse

| Kategori | Innhold |
| -------- | ------- |
| **Konfigurasjon** | LogFilePrefix, LogFilePath, HostIpAddress, HostPort, ComPort, BaudRate, DeviceString, SerialDriver, TraceLevel, PrinterWidth, DisplayWidth, CutterSupport, VendorInfoExtended, IndicateEotTransaction, PowerCycleCheck, TidSupervision, AutoGetCustomerInfo, TerminalReady, UseDisplayTextID, UseExtendedLocalMode, UseSplitDisplayText, Use2KBuffer, DisplayTextInLocalMode, LogAutoDeleteDays, MsgRouterOn, MsgRouterIpAddress, MsgRouterPort, SocketListener, SocketListenerAddress, SocketListenerPort |
| **Status (read-only)** | Version, TermType, TerminalID, TerminalSwVersion, TerminalDeviceData_TLD, MethodRejectInfo, MethodRejectCode |
| **Metoder** | Open(), Close(), Dispose(), Administration(AdministrationArgs), TransferAmount(TransferAmountArgs), SendTLD(SendTldArgs), SendJson(SendJsonArgs), TransferCardData(TransferCardDataArgs), BiBAdministration(BiBAdministrationArgs), BiBTransaction(BiBTransactionArgs), GetTLDTag(byte[], byte[], out byte[]) |
| **Events** | OnDisplayText, OnTerminalReady, OnPrintText, OnLocalMode, OnLastFinancialResult, OnError, OnTLDReceived, OnStdRsp, OnJsonReceived |

### Argumenttyper med felt

- **AdministrationArgs:** OperID (String), AdmCode (Int)
- **TransferAmountArgs:** OperID, Type1, Amount1, Type2, Amount2, Type3, Amount3, HostData, ArticleDetails, PaymentConditionCode, AuthCode, OptionalData
- **LastFinancialResultEventArgs:** ResultData, Result, AccumulatorUpdate, IssuerId, TruncatedPan, Timestamp, VerificationMethod, SessionNumber, StanAuth, SequenceNumber, TotalAmount, RejectionSource, RejectionReason, TipAmount, SurchargeAmount, TerminalID, AcquirerMerchantID, CardIssuerName, ResponseCode, TCC, AID, TVR, TSI, ATC, AED, IAC, OrganisationNumber, BankAgent, EncryptedPAN, AccountType, OptionalData
- **LocalModeEventArgs:** Samme som LastFinancialResultEventArgs + LocalModeResultData
- **DisplayTextEventArgs:** DisplayText, DisplaytextSourceID, DisplaytextID
- **PrintTextEventArgs:** PrintText
- **BaxiErrorEventArgs:** ErrorCode, ErrorString
- **TLDReceivedArgs / SendTldArgs:** TldType, TldData / TldField (ByteArray)
- **JsonReceivedArgs / SendJsonArgs:** JsonString / JsonData
- **BiBAdministrationArgs:** AdmCode, OperId
- **BiBTransactionArgs:** Amount, TransactionData
- **TransferCardDataArgs:** TrackType (1/2/3), TrackData
- **StdRspReceivedArgs:** Response
- **TerminalReadyEventArgs:** (tom)

### IBaxiEvents – event-flyt

Alle ni callbacks: OnDisplayText, OnPrintText, OnError, OnLocalMode, OnStdRsp, OnTLDReceived, OnTerminalReady, OnLastFinancialResult, OnJsonReceived.

### Internt (avkuttet i filen)

**QueueType:** DFS13_CONTROLLER, DFS13_LOW_LEVEL, HOST (resten av enum ikke synlig). Indikerer interne køer mellom controller, lavnivå og host.

---

## Hva vi mangler

Følgende kan **ikke** utledes fra dekompileringen alene og må innhentes via trafikk-analyse (f.eks. Wireshark), mock-server som logger bytes, eller Nets/Viking-dokumentasjon:

1. **Wire-format**
   - Framing: TLD vs length-prefix (2 byte) vs STX/ETX/LRC; byte-rekkefølge (big-/little-endian).
   - Charset for tekstfelter (ISO-8859-1, UTF-8, etc.).

2. **TLD (Tag-Length-Data)**
   - Konkrete tag-verdier (hex/byte) for kommandoer og svar.
   - Lengdefelt: 1 byte eller 2 byte; nesting av TLD.

3. **Koder**
   - AdmCode: 1=EOD, 2=X-Report, 3=Z-Report, 10=Reconciliation er nevnt som typiske; full liste ukjent.
   - Type1/Type2/Type3: Purchase=0, Refund=1 er typiske; andre verdier ukjente.
   - Result: 0=approved; andre verdier for avslag/feil.
   - ErrorCode: ingen verditabell i dekompileringen.

4. **Sekvens**
   - Nøyaktig rekkefølge: Open → (TerminalReady?) → TransferAmount/Administration → events → LastFinancialResult.
   - Eventuelle handshake-meldinger (f.eks. I1/I2 som i noen ECR-implementasjoner).

5. **TCP vs Serial**
   - Når brukes HostIpAddress/HostPort vs ComPort/BaudRate; om samme protokoll brukes over begge.

6. **Proprietære utvidelser**
   - Filen nevner at Nets/Viking kan gjøre noe proprietært utover standard BAXI.

---

## Kotlin-implementasjonsstrategi

### Mapping fra .NET til Kotlin

- **BaxiCtrl** → `BaxiClient` (eller `BaxiController`): hovedklasse som holder config, connection og event-handlere.
- **Args** → Kotlin `data class` med samme felter; EventArgs → data classes for resultater.
- **Events** → én `BaxiEventHandler`-interface med metoder for hver event; eller sealed interface + implementasjoner.

### Lagdeling

- **Config:** Data class for HostIpAddress, HostPort, ComPort, BaudRate, timeouts, etc.
- **Connection:** TCP (eller SSL) socket-lag med connect/send/receive; evt. Serial ved behov.
- **Protocol:** TLD-encoding/decoding og framing (length-prefix eller STX/ETX/LRC) – placeholder-tagger til reelle verdier er kjent.
- **TransactionManager:** Høynivå: open(), transferAmount(args), administration(args); koordinering av kommandoer og event-callbacks.

---

## Kodeeksempler (Kotlin)

### Data classes (args og resultater)

```kotlin
// AdministrationArgs – fra dekompilering
data class AdministrationArgs(
    val admCode: Int,
    val operID: String = "1"
)

// TransferAmountArgs – fra dekompilering
data class TransferAmountArgs(
    val operID: String = "1",
    val type1: Int,           // e.g. 0 = Purchase, 1 = Refund (TBD from capture)
    val amount1: Int,        // øre
    val type2: Int = 0,
    val amount2: Int = 0,
    val type3: Int = 0,
    val amount3: Int = 0,
    val hostData: String? = null,
    val articleDetails: String? = null,
    val paymentConditionCode: String? = null,
    val authCode: String? = null,
    val optionalData: String? = null
)

// LastFinancialResult – fra LastFinancialResultEventArgs
data class LastFinancialResult(
    val resultData: String?,
    val result: Int,              // 0 = approved
    val accumulatorUpdate: Int,
    val issuerId: Int,
    val truncatedPan: String?,
    val timestamp: String?,
    val verificationMethod: Int,
    val sessionNumber: String?,
    val stanAuth: String?,
    val sequenceNumber: String?,
    val totalAmount: Int,
    val rejectionSource: Int,
    val rejectionReason: String?,
    val tipAmount: Int,
    val surchargeAmount: Int,
    val terminalID: String?,
    val acquirerMerchantID: String?,
    val cardIssuerName: String?,
    val responseCode: String?,
    val tcc: String?,
    val optionalData: String?
)
```

### Event-handler interface

```kotlin
interface BaxiEventHandler {
    fun onDisplayText(sourceId: Int, textId: Int, text: String) {}
    fun onPrintText(text: String) {}
    fun onError(errorCode: Int, errorString: String) {}
    fun onLocalMode(result: LastFinancialResult, localModeResultData: String?) {}
    fun onStdRsp(response: String) {}
    fun onTLDReceived(tldType: String, tldData: ByteArray) {}
    fun onTerminalReady() {}
    fun onLastFinancialResult(result: LastFinancialResult) {}
    fun onJsonReceived(jsonString: String) {}
}
```

### TCP-lag med connect / send / receive

```kotlin
class BaxiTcpConnection(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 30000
) {
    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null

    fun connect() {
        if (socket?.isConnected == true) return
        socket = Socket()
        socket!!.soTimeout = readTimeoutMs
        socket!!.connect(InetSocketAddress(host, port), connectTimeoutMs)
        output = socket!!.getOutputStream()
        input = socket!!.getInputStream()
    }

    fun send(payload: ByteArray) {
        // Length-prefix framing (2 bytes big-endian) – typical for TCP BAXI
        val header = byteArrayOf(
            (payload.size shr 8).toByte(),
            (payload.size and 0xFF).toByte()
        )
        output!!.write(header)
        output!!.write(payload)
        output!!.flush()
    }

    fun receive(): ByteArray {
        val header = ByteArray(2)
        input!!.readFully(header)
        val len = ((header[0].toInt() and 0xFF) shl 8) or (header[1].toInt() and 0xFF)
        val payload = ByteArray(len)
        input!!.readFully(payload)
        return payload
    }

    fun close() {
        socket?.close()
        socket = null
        input = null
        output = null
    }
}
```

### Framing: length-prefix vs STX/ETX/LRC

```kotlin
// Length-prefix (2 byte big-endian) – anbefalt for TCP
fun buildLengthPrefixFrame(payload: ByteArray): ByteArray {
    val header = byteArrayOf(
        (payload.size shr 8).toByte(),
        (payload.size and 0xFF).toByte()
    )
    return header + payload
}

// STX/ETX/LRC – for serial (referanse fra eksisterende NetsBaxProtocol)
const val STX: Byte = 0x02
const val ETX: Byte = 0x03

fun buildStxEtxLrcFrame(payload: ByteArray): ByteArray {
    var lrc = 0
    for (b in payload) lrc = lrc xor (b.toInt() and 0xFF)
    lrc = lrc xor (ETX.toInt() and 0xFF)
    return byteArrayOf(STX) + payload + ETX + lrc.toByte()
}
```

### Høynivå BaxiClient (placeholder for TLD/JSON)

```kotlin
class BaxiClient(
    private val host: String,
    private val port: Int,
    private val handler: BaxiEventHandler
) {
    private val connection = BaxiTcpConnection(host, port)

    fun open() {
        connection.connect()
        // TBD: send Open/Session TLD if required by protocol; then wait for OnTerminalReady
    }

    fun transferAmount(args: TransferAmountArgs): Int {
        // TBD: build TLD or JSON from args; tag values from wire capture
        val payload = buildTransferAmountPayload(args) // placeholder
        connection.send(payload)
        val response = connection.receive()
        return parseTransferAmountResponse(response)
    }

    fun administration(args: AdministrationArgs): Int {
        val payload = buildAdministrationPayload(args) // placeholder
        connection.send(payload)
        val response = connection.receive()
        return parseAdministrationResponse(response)
    }

    private fun buildTransferAmountPayload(args: TransferAmountArgs): ByteArray {
        // Placeholder: real implementation needs TLD tags from capture
        val body = "P;10;${args.operID};${args.amount1};0" // TCP-style string as in NetsBaxProtocol
        return buildLengthPrefixFrame(body.toByteArray(Charsets.ISO_8859_1))
    }

    fun close() = connection.close()
}
```

---

## TCP mot betalingsterminal

### Roller

- **App (Kotlin)** er typisk **TCP-klient** som kobler til enten:
  - **Nets Cloud Connect** (SSL, f.eks. 3.33.230.243:6001) – anbefalt i produksjon, eller
  - **Terminal direkte** (ren TCP, port ofte 3000–3010 eller 8009).

### Anbefalinger

- **Port-scan:** Skann porter (f.eks. 3000–3010, 6001 for Cloud) for å bekrefte åpen port før integrasjon.
- **Timeout:** Sett connectTimeout (f.eks. 10 s) og readTimeout (f.eks. 30 s for betaling).
- **Framing:** Bruk length-prefix (2 byte big-endian) for TCP når terminal/Nets forventer det; STX/ETX/LRC for serial (se [TCP_ETHERNET_FRAMING.md](../_archived/baxi-protocol/TCP_ETHERNET_FRAMING.md)).
- **SSL:** For Nets Cloud bruk eksisterende [CloudTerminalClient](../lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/CloudTerminalClient.kt) (TLS 1.2/1.3 mot 3.33.230.243:6001).

### Eksisterende kode i prosjektet

- [NetsBaxProtocol.kt](../lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/NetsBaxProtocol.kt): TCP_ETHERNET vs SERIAL, `P;10;...`-kommandoer, `BaxResponse`.
- [CloudTerminalClient.kt](../lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/payment/CloudTerminalClient.kt): SSL-mot Nets Cloud, send/receive med samme framing som NetsBaxProtocol.

---

## Oppdagelsesstrategi

1. **Wireshark:** Fanger trafikk mellom .NET-app og terminal (eller Nets Cloud). Analyser første bytes: length-prefix vs STX; deretter TLD-tagger og lengder.
2. **Mock-server:** Python eller Kotlin server som logger alle mottatte bytes; kjør .NET-klient mot mock for å matche format.
3. **Fuzzing:** I test-miljø, prøv systematisk TLD-tagger og AdmCode/Type-verdier for å kartlegge gyldige verdier (lav risiko).

---

## Oppsummering

| Område | Kan bygges i Kotlin i dag | Krever trafikk/dokumentasjon |
| ------ | ------------------------- | --------------------------- |
| API-lag (args, events) | Ja – data classes og BaxiEventHandler | – |
| TCP/SSL connection | Ja – BaxiTcpConnection / CloudTerminalClient | Port og framing-validering |
| Framing (length-prefix / STX/ETX/LRC) | Ja – konfigurerbar | Hvilken variant terminal bruker |
| TLD/JSON-innhold | Kun placeholder | TLD-tagger, koder, sekvens |
| TransferAmount / Administration | Skjelett med placeholder payload | Reelle payload- og svar-format |

**Anbefaling:** Bygg BaxiClient med konfigurerbar framing og placeholder for TLD/JSON; fyll inn konkrete tag-verdier og koder når trafikk er innsamlet eller dokumentasjon tilgjengelig. Bruk eksisterende NetsBaxProtocol og CloudTerminalClient der det passer (TCP/SSL mot Nets Cloud).

---

**Full dokumentasjon:** Se også [BAXI_DECOMPILED_SAMLET_RAPPORT.md](BAXI_DECOMPILED_SAMLET_RAPPORT.md) for felles protokollbeskrivelse, gap og TCP-anbefalinger for både Kotlin og Python.
