# Nets Cloud Connect - Komplett Teknisk Guide

**Versjon:** 1.0  
**Dato:** 31. januar 2026  
**Forfatter:** Thomas Andersen (Arkitekt)

---

## Innholdsfortegnelse

1. [Hva er Nets Cloud Connect?](#1-hva-er-nets-cloud-connect)
2. [Arkitekturoversikt](#2-arkitekturoversikt)
3. [Protokollspesifikasjon](#3-protokollspesifikasjon)
4. [Meldingstyper og Format](#4-meldingstyper-og-format)
5. [Transaksjonstyper](#5-transaksjonstyper)
6. [Administrasjonskommandoer](#6-administrasjonskommandoer)
7. [Responstyper og Håndtering](#7-responstyper-og-håndtering)
8. [Feilkoder](#8-feilkoder)
9. [LPG-spesifikk Betalingsflyt](#9-lpg-spesifikk-betalingsflyt)
10. [Kotlin-implementasjon](#10-kotlin-implementasjon)
11. [Testing og Simulering](#11-testing-og-simulering)
12. [Kommunikasjon til Teamet](#12-kommunikasjon-til-teamet)

---

## 1. Hva er Nets Cloud Connect?

### 1.1 Definisjon

Nets Cloud Connect er en **skybasert betalingsgateway** som tillater kassesystemer (POS/ECR) å kommunisere med betalingsterminaler via Nets' infrastruktur i skyen.

### 1.2 Hva Nets Håndterer

- ✅ All kommunikasjon med banken (acquirer)
- ✅ Kortvalidering og autorisasjon
- ✅ EMV-chipbehandling
- ✅ PCI DSS-compliance
- ✅ Sikker datahåndtering
- ✅ Kvitteringsgenerering
- ✅ Oppgjør og reconciliation

### 1.3 Hva Vi Må Håndtere

- 📤 Sende betalingsforespørsler (beløp, type)
- 📥 Motta og parse responser
- 🖥️ Vise status til brukeren
- 🔄 Håndtere feil og retry-logikk
- 📊 Lagre transaksjonsdata lokalt

### 1.4 Fordeler med Cloud Connect

| Aspekt | Lokal Terminal | Cloud Connect |
|--------|----------------|---------------|
| Installasjon | Kabling, drivere | Kun nettverkstilgang |
| Vedlikehold | Manuell oppdatering | Automatisk via sky |
| Skalerbarhet | Én terminal per tilkobling | Flere terminaler via én tilkobling |
| PCI Scope | Stort (lokal kortdata) | Minimalt (Nets håndterer) |

---

## 2. Arkitekturoversikt

### 2.1 Systemarkitektur

```
┌─────────────────┐     TCP/WebSocket      ┌─────────────────┐
│                 │◄─────────────────────►│                 │
│   Vår App       │     XML-meldinger      │  Nets Cloud     │
│   (Kotlin)      │                        │  Connect        │
│                 │                        │                 │
└─────────────────┘                        └────────┬────────┘
                                                    │
                                                    │ Intern
                                                    │ protokoll
                                                    ▼
                                           ┌─────────────────┐
                                           │  Betalings-     │
                                           │  terminal       │
                                           │  (Nets/Verifone)│
                                           └─────────────────┘
```

### 2.2 Tilkoblingsalternativer

| Metode | Port | Fordeler | Ulemper |
|--------|------|----------|---------|
| **TCP Socket** | 6000 | Enkel, rask | Manuell framing |
| **WebSocket** | 443 | Brannmurvennlig, auto-framing | Litt mer overhead |

**Anbefaling for Kotlin:** WebSocket med Ktor Client (mer idiomatisk, bedre feilhåndtering)

### 2.3 Tilkoblingsparametre

```
Testmiljø:
  Host: 193.122.56.145 (eller testapi.cloudconnect.ml)
  Port: 6000 (TCP) / 443 (WSS)
  
Produksjon:
  Host: Leveres av Nets
  Port: 6000 / 443
```

---

## 3. Protokollspesifikasjon

### 3.1 Meldingsformat (TCP Socket)

Hver melding består av:

```
┌──────────────┬──────────────────────────┐
│ 4 bytes      │ N bytes                  │
│ Length       │ XML Payload              │
│ (Big-endian) │ (UTF-8 encoded)          │
└──────────────┴──────────────────────────┘
```

**Eksempel i Kotlin:**
```kotlin
// Sende melding
val xmlBytes = xmlMessage.toByteArray(Charsets.UTF_8)
val lengthBytes = ByteBuffer.allocate(4).putInt(xmlBytes.size).array()
outputStream.write(lengthBytes)
outputStream.write(xmlBytes)

// Motta melding
val lengthBytes = ByteArray(4)
inputStream.read(lengthBytes)
val length = ByteBuffer.wrap(lengthBytes).int
val xmlBytes = ByteArray(length)
inputStream.read(xmlBytes)
val xmlMessage = String(xmlBytes, Charsets.UTF_8)
```

### 3.2 Meldingsformat (WebSocket)

WebSocket håndterer framing automatisk - send XML direkte som tekstmelding.

### 3.3 XML-struktur

**Request (vi sender):**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<NetsRequest>
  <MessageHeader 
    ECRID="LPG-STASJON-1" 
    TerminalID="72199301" 
    VersionNumber="1" />
  <!-- Kommando her -->
</NetsRequest>
```

**Response (vi mottar):**
```xml
<NetsResponse>
  <MessageHeader TerminalID="72199301" />
  <!-- Respons her -->
</NetsResponse>
```

### 3.4 MessageHeader-attributter

| Attributt | Beskrivelse | Eksempel |
|-----------|-------------|----------|
| `ECRID` | Vår kasse-ID | "LPG-STASJON-1" |
| `TerminalID` | 8-sifret terminal-ID | "72199301" |
| `VersionNumber` | Protokollversjon | "1" |

---

## 4. Meldingstyper og Format

### 4.1 Dfs13TransferAmount (Betalingsforespørsel)

**Request:**
```xml
<Dfs13TransferAmount>
  <TransactionType>48</TransactionType>
  <OperId>0000</OperId>
  <Amount1>10000</Amount1>
  <Amount2>0</Amount2>
  <Amount3>0</Amount3>
  <Type2>48</Type2>
  <Type3>48</Type3>
  <HostData></HostData>
  <OptionalData></OptionalData>
</Dfs13TransferAmount>
```

**Felter:**

| Felt | Type | Beskrivelse |
|------|------|-------------|
| `TransactionType` | int | Se transaksjonstyper |
| `OperId` | string | Operatør-ID (4 siffer) |
| `Amount1` | int | Hovedbeløp i øre |
| `Amount2` | int | Sekundærbeløp (cashback) i øre |
| `Amount3` | int | Tertiærbeløp i øre |
| `Type2/Type3` | int | Type for Amount2/Amount3 |
| `HostData` | string | Ekstra data til host |
| `OptionalData` | string | JSON-formatert tilleggsdata |

### 4.2 Dfs13Administration (Administrasjonskommando)

**Request:**
```xml
<Dfs13Administration>
  <OperId>0000</OperId>
  <AdmCode>12592</AdmCode>
  <OptionalData></OptionalData>
</Dfs13Administration>
```

### 4.3 Dfs13SendJson (JSON-kommando)

**Request:**
```xml
<Dfs13SendJson>
  <Data>{"cardinfo": {"ver": "1.01", "alltags": "?"}}</Data>
</Dfs13SendJson>
```

### 4.4 Open (Åpne tilkobling)

**Request:**
```xml
<Open />
```

---

## 5. Transaksjonstyper

### 5.1 TransactionType-verdier

| Kode | Hex | Navn | Beskrivelse |
|------|-----|------|-------------|
| 48 | 0x30 | **Purchase** | Vanlig kjøp |
| 49 | 0x31 | **Refund** | Tilbakebetaling |
| 50 | 0x32 | **Reversal** | Annullering |
| 51 | 0x33 | **Cashback** | Kjøp med kontantuttak |
| **52** | 0x34 | **PreAuth** | **Reservering (viktig for LPG!)** |
| 54 | 0x36 | **Balance Inquiry** | Saldoforespørsel |
| 56 | 0x38 | **Deposit** | Innskudd |
| 62 | 0x3E | **Bonus** | Bonusbelastning |
| 64 | 0x40 | **Offline** | Offline-transaksjon |

### 5.2 For LPG-betaling

**Vi trenger primært:**
1. **PreAuth (52)** - Reservere maksbeløp før fylling
2. **Purchase (48)** - Endelig belastning etter fylling
3. **Reversal (50)** - Kansellere reservering ved feil

---

## 6. Administrasjonskommandoer

### 6.1 AdmCode-verdier

| Desimal | Hex | Navn | Beskrivelse |
|---------|-----|------|-------------|
| 12592 | 0x3130 | **End of Day** | Dagsoppgjør/reconciliation |
| 12594 | 0x3132 | **Cancel** | Avbryt pågående operasjon |
| 12598 | 0x3136 | **X-Report** | Midlertidig rapport |
| 12599 | 0x3137 | **Z-Report** | Sluttrapport |
| 12603 | 0x313B | **Silent Cancel** | Avbryt uten dialog |
| 12604 | 0x313C | **Last Receipt** | Hent siste kvittering |
| 12605 | 0x313D | **Last Result** | Hent siste transaksjonsresultat |
| 12606 | 0x313E | **Software Download** | Last ned programvare |
| 12607 | 0x313F | **Dataset Download** | Last ned datasett |

---

## 7. Responstyper og Håndtering

### 7.1 Dfs13LocalMode (Transaksjonsresultat)

**Response:**
```xml
<Dfs13LocalMode>
  <Result>1</Result>
  <AccumulatorUpdate>0x01</AccumulatorUpdate>
  <IssuerId>01</IssuerId>
  <TruncatedPan>************1234</TruncatedPan>
  <Timestamp>20260131143052</Timestamp>
  <VerificationMethod>PIN</VerificationMethod>
  <SessionNumber>000001</SessionNumber>
  <StanAuth>123456</StanAuth>
  <SequenceNumber>000001</SequenceNumber>
  <TotalAmount>10000</TotalAmount>
  <RejectionSource>0</RejectionSource>
  <RejectionReason>0</RejectionReason>
  <TipAmount>0</TipAmount>
  <SurchargeAmount>0</SurchargeAmount>
  <TerminalID>72199301</TerminalID>
  <AcquirerMerchantID>1234567890</AcquirerMerchantID>
  <CardIssuerName>VISA</CardIssuerName>
  <ResponseCode>00</ResponseCode>
  <!-- ... flere felter ... -->
</Dfs13LocalMode>
```

**Viktige felter:**

| Felt | Beskrivelse | Verdier |
|------|-------------|---------|
| `Result` | Transaksjonsresultat | 1=Godkjent, 0=Avslått |
| `TruncatedPan` | Maskert kortnummer | ************1234 |
| `TotalAmount` | Belastet beløp i øre | 10000 = 100,00 kr |
| `CardIssuerName` | Korttype | VISA, MASTERCARD, etc |
| `ResponseCode` | Banksvar | 00=OK, andre=feil |
| `RejectionSource` | Avslagskilde | 0=Ingen, 1=Terminal, 2=Bank |
| `RejectionReason` | Avslagsårsak | Se dokumentasjon |

### 7.2 Dfs13TerminalReady

```xml
<Dfs13TerminalReady />
```

Indikerer at terminalen er klar for ny transaksjon.

### 7.3 Dfs13DisplayText

```xml
<Dfs13DisplayText>
  <Text Source="1" TextID="1011">Vennligst vent...</Text>
</Dfs13DisplayText>
```

Tekst som skal vises til operatør/kunde.

### 7.4 Dfs13PrintText

```xml
<Dfs13PrintText>
  <Text>
    KVITTERING
    ---------
    Beløp: 100,00 NOK
    Kort: VISA ****1234
  </Text>
</Dfs13PrintText>
```

Kvitteringstekst for utskrift.

### 7.5 Dfs13Error

```xml
<Dfs13Error>
  <ErrorCode>8013</ErrorCode>
  <ErrorInfo>Terminal not found</ErrorInfo>
</Dfs13Error>
```

### 7.6 MethodRejected

```xml
<MethodRejected>
  <RejectCode>7102</RejectCode>
  <RejectInfo>ALREADY_OPEN</RejectInfo>
</MethodRejected>
```

---

## 8. Feilkoder

### 8.1 Nets Interne Feilkoder

| Kode | Beskrivelse | Handling |
|------|-------------|----------|
| 7102 | ALREADY_OPEN | Ignorer, fortsett |
| 8013 | Terminal not found | Sjekk TerminalID |
| 9000 | Connection lost | Reconnect |
| 9100 | Unauthorized/Invalid login | Sjekk credentials |

### 8.2 Banksvar (ResponseCode)

| Kode | Betydning |
|------|-----------|
| 00 | Godkjent |
| 05 | Avslått av bank |
| 12 | Ugyldig transaksjon |
| 14 | Ugyldig kortnummer |
| 51 | Ikke dekning |
| 54 | Kortet utløpt |
| 55 | Feil PIN |
| 61 | Beløpsgrense overskredet |
| 91 | Bank utilgjengelig |

---

## 9. LPG-spesifikk Betalingsflyt

### 9.1 Pre-Auth/Capture Flow

Dette er flyten for drivstoffbetaling der kunden fyller først og betaler etterpå:

```
┌──────────────┐                              ┌──────────────┐
│   LPG App    │                              │ Nets Cloud   │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │  1. TransferAmount(type=52, amount=150000)  │
       │────────────────────────────────────────────►│
       │         "Reserver 1500 kr"                  │
       │                                             │
       │  2. Dfs13LocalMode(result=1)                │
       │◄────────────────────────────────────────────│
       │         "Reservasjon OK"                    │
       │                                             │
       │  ══════ KUNDE FYLLER DRIVSTOFF ══════       │
       │         (Faktisk beløp: 847,50 kr)          │
       │                                             │
       │  3. TransferAmount(type=48, amount=84750)   │
       │────────────────────────────────────────────►│
       │         "Trekk 847,50 kr"                   │
       │                                             │
       │  4. Dfs13LocalMode(result=1)                │
       │◄────────────────────────────────────────────│
       │         "Betaling OK"                       │
       │                                             │
       ▼                                             ▼
```

### 9.2 Meldingseksempler for LPG

**Steg 1: Reservering (Pre-Auth)**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<NetsRequest>
  <MessageHeader ECRID="LPG-PUMP-1" TerminalID="72199301" VersionNumber="1" />
  <Dfs13TransferAmount>
    <TransactionType>52</TransactionType>
    <OperId>0000</OperId>
    <Amount1>150000</Amount1>
    <Amount2>0</Amount2>
    <Amount3>0</Amount3>
    <Type2>48</Type2>
    <Type3>48</Type3>
  </Dfs13TransferAmount>
</NetsRequest>
```

**Steg 2: Respons på reservering**
```xml
<NetsResponse>
  <MessageHeader TerminalID="72199301" />
  <Dfs13LocalMode>
    <Result>1</Result>
    <TotalAmount>150000</TotalAmount>
    <TruncatedPan>************1234</TruncatedPan>
    <CardIssuerName>VISA</CardIssuerName>
    <StanAuth>123456</StanAuth>
    <!-- Lagre StanAuth for capture -->
  </Dfs13LocalMode>
</NetsResponse>
```

**Steg 3: Fullføring (Capture)**
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<NetsRequest>
  <MessageHeader ECRID="LPG-PUMP-1" TerminalID="72199301" VersionNumber="1" />
  <Dfs13TransferAmount>
    <TransactionType>48</TransactionType>
    <OperId>0000</OperId>
    <Amount1>84750</Amount1>
    <Amount2>0</Amount2>
    <Amount3>0</Amount3>
    <Type2>48</Type2>
    <Type3>48</Type3>
    <HostData>StanAuth:123456</HostData>
  </Dfs13TransferAmount>
</NetsRequest>
```

### 9.3 Feilhåndtering for LPG

| Scenario | Handling |
|----------|----------|
| Reservering avslått | Vis feilmelding, ikke start fylling |
| Nettverksfeil under fylling | Lagre lokal, retry capture |
| Capture feiler | Retry med eksponentiell backoff |
| Kunde forlater uten å fylle | Send Reversal (type=50) |

---

## 10. Kotlin-implementasjon

### 10.1 Prosjektstruktur

```
src/main/kotlin/
├── no/cloudberries/lpg/payment/
│   ├── NetsCloudConnectClient.kt    # Socket/WebSocket klient
│   ├── NetsRequestBuilder.kt        # XML-bygging
│   ├── NetsResponseParser.kt        # XML-parsing
│   ├── NetsPaymentService.kt        # Høynivå API
│   └── model/
│       ├── TransactionType.kt       # Enum for transaksjonstyper
│       ├── NetsRequest.kt           # Request-modeller
│       └── NetsResponse.kt          # Response sealed classes
```

### 10.2 TransactionType Enum

```kotlin
enum class TransactionType(val code: Int) {
    PURCHASE(48),
    REFUND(49),
    REVERSAL(50),
    CASHBACK(51),
    PRE_AUTH(52),
    BALANCE_INQUIRY(54),
    DEPOSIT(56),
    BONUS(62),
    OFFLINE(64);
    
    companion object {
        fun fromCode(code: Int): TransactionType? = 
            entries.find { it.code == code }
    }
}
```

### 10.3 Response Sealed Classes

```kotlin
sealed class NetsResponse {
    abstract val terminalId: String
    
    data class LocalMode(
        override val terminalId: String,
        val result: Int,
        val totalAmount: Long,
        val truncatedPan: String,
        val cardIssuerName: String,
        val responseCode: String,
        val stanAuth: String,
        val timestamp: String,
        val rejectionSource: Int,
        val rejectionReason: Int
    ) : NetsResponse() {
        val isApproved: Boolean get() = result == 1
    }
    
    data class TerminalReady(
        override val terminalId: String
    ) : NetsResponse()
    
    data class DisplayText(
        override val terminalId: String,
        val text: String,
        val source: Int,
        val textId: Int
    ) : NetsResponse()
    
    data class PrintText(
        override val terminalId: String,
        val text: String
    ) : NetsResponse()
    
    data class Error(
        override val terminalId: String,
        val errorCode: Int,
        val errorInfo: String
    ) : NetsResponse()
    
    data class MethodRejected(
        override val terminalId: String,
        val rejectCode: Int,
        val rejectInfo: String
    ) : NetsResponse()
}
```

### 10.4 WebSocket Client med Ktor

```kotlin
class NetsCloudConnectClient(
    private val config: NetsConfig
) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 30_000
        }
    }
    
    private var session: DefaultClientWebSocketSession? = null
    private val _responses = MutableSharedFlow<NetsResponse>()
    val responses: SharedFlow<NetsResponse> = _responses.asSharedFlow()
    
    suspend fun connect() {
        client.webSocket(
            method = HttpMethod.Get,
            host = config.host,
            port = config.port,
            path = config.path
        ) {
            session = this
            
            // Motta meldinger
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val xml = frame.readText()
                        val response = NetsResponseParser.parse(xml)
                        _responses.emit(response)
                    }
                    else -> {}
                }
            }
        }
    }
    
    suspend fun send(request: String) {
        session?.send(Frame.Text(request))
    }
    
    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}
```

### 10.5 Request Builder

```kotlin
object NetsRequestBuilder {
    
    fun transferAmount(
        terminalId: String,
        ecrId: String,
        type: TransactionType,
        amountOre: Long,
        amount2Ore: Long = 0,
        hostData: String = "",
        optionalData: String = ""
    ): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" ?>""")
        append("<NetsRequest>")
        append("""<MessageHeader ECRID="$ecrId" TerminalID="$terminalId" VersionNumber="1" />""")
        append("<Dfs13TransferAmount>")
        append("<TransactionType>${type.code}</TransactionType>")
        append("<OperId>0000</OperId>")
        append("<Amount1>$amountOre</Amount1>")
        append("<Amount2>$amount2Ore</Amount2>")
        append("<Amount3>0</Amount3>")
        append("<Type2>48</Type2>")
        append("<Type3>48</Type3>")
        if (hostData.isNotEmpty()) {
            append("<HostData>$hostData</HostData>")
        }
        append("<OptionalData>$optionalData</OptionalData>")
        append("</Dfs13TransferAmount>")
        append("</NetsRequest>")
    }
    
    fun administration(
        terminalId: String,
        ecrId: String,
        admCode: Int
    ): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" ?>""")
        append("<NetsRequest>")
        append("""<MessageHeader ECRID="$ecrId" TerminalID="$terminalId" VersionNumber="1" />""")
        append("<Dfs13Administration>")
        append("<OperId>0000</OperId>")
        append("<AdmCode>$admCode</AdmCode>")
        append("<OptionalData></OptionalData>")
        append("</Dfs13Administration>")
        append("</NetsRequest>")
    }
    
    fun cancel(terminalId: String, ecrId: String) = 
        administration(terminalId, ecrId, 12594)
    
    fun endOfDay(terminalId: String, ecrId: String) = 
        administration(terminalId, ecrId, 12592)
}
```

### 10.6 Response Parser

```kotlin
object NetsResponseParser {
    
    fun parse(xml: String): NetsResponse {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
        
        val root = doc.documentElement
        if (root.nodeName != "NetsResponse") {
            throw IllegalArgumentException("Invalid response: ${root.nodeName}")
        }
        
        val header = root.getElementsByTagName("MessageHeader").item(0) as Element
        val terminalId = header.getAttribute("TerminalID")
        
        // Finn action-elementet (andre barn av NetsResponse)
        val actionNode = root.childNodes.let { nodes ->
            (0 until nodes.length)
                .map { nodes.item(it) }
                .filterIsInstance<Element>()
                .firstOrNull { it.nodeName != "MessageHeader" }
        } ?: throw IllegalArgumentException("No action in response")
        
        return when (actionNode.nodeName) {
            "Dfs13LocalMode" -> parseLocalMode(terminalId, actionNode)
            "Dfs13TerminalReady" -> NetsResponse.TerminalReady(terminalId)
            "Dfs13DisplayText" -> parseDisplayText(terminalId, actionNode)
            "Dfs13PrintText" -> parsePrintText(terminalId, actionNode)
            "Dfs13Error" -> parseError(terminalId, actionNode)
            "MethodRejected" -> parseMethodRejected(terminalId, actionNode)
            else -> throw IllegalArgumentException("Unknown action: ${actionNode.nodeName}")
        }
    }
    
    private fun parseLocalMode(terminalId: String, node: Element): NetsResponse.LocalMode {
        return NetsResponse.LocalMode(
            terminalId = terminalId,
            result = node.getTextContent("Result")?.toIntOrNull() ?: 0,
            totalAmount = node.getTextContent("TotalAmount")?.toLongOrNull() ?: 0,
            truncatedPan = node.getTextContent("TruncatedPan") ?: "",
            cardIssuerName = node.getTextContent("CardIssuerName") ?: "",
            responseCode = node.getTextContent("ResponseCode") ?: "",
            stanAuth = node.getTextContent("StanAuth") ?: "",
            timestamp = node.getTextContent("Timestamp") ?: "",
            rejectionSource = node.getTextContent("RejectionSource")?.toIntOrNull() ?: 0,
            rejectionReason = node.getTextContent("RejectionReason")?.toIntOrNull() ?: 0
        )
    }
    
    private fun Element.getTextContent(tagName: String): String? =
        getElementsByTagName(tagName).item(0)?.textContent
}
```

### 10.7 Høynivå Payment Service

```kotlin
@Service
class NetsPaymentService(
    private val config: NetsConfig,
    private val transactionRepository: TransactionRepository
) {
    private val client = NetsCloudConnectClient(config)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _paymentEvents = MutableSharedFlow<PaymentEvent>()
    val paymentEvents: SharedFlow<PaymentEvent> = _paymentEvents.asSharedFlow()
    
    init {
        scope.launch {
            client.responses.collect { response ->
                handleResponse(response)
            }
        }
    }
    
    suspend fun preAuthorize(pumpId: String, maxAmountOre: Long): Result<PreAuthResult> {
        val request = NetsRequestBuilder.transferAmount(
            terminalId = config.terminalId,
            ecrId = "LPG-$pumpId",
            type = TransactionType.PRE_AUTH,
            amountOre = maxAmountOre
        )
        
        client.send(request)
        
        // Vent på svar med timeout
        return withTimeoutOrNull(60_000) {
            client.responses
                .filterIsInstance<NetsResponse.LocalMode>()
                .first()
                .let { response ->
                    if (response.isApproved) {
                        Result.success(PreAuthResult(
                            stanAuth = response.stanAuth,
                            truncatedPan = response.truncatedPan,
                            cardType = response.cardIssuerName
                        ))
                    } else {
                        Result.failure(PaymentDeclinedException(
                            response.rejectionReason,
                            response.responseCode
                        ))
                    }
                }
        } ?: Result.failure(TimeoutException("Pre-auth timeout"))
    }
    
    suspend fun capture(pumpId: String, actualAmountOre: Long, stanAuth: String): Result<CaptureResult> {
        val request = NetsRequestBuilder.transferAmount(
            terminalId = config.terminalId,
            ecrId = "LPG-$pumpId",
            type = TransactionType.PURCHASE,
            amountOre = actualAmountOre,
            hostData = "StanAuth:$stanAuth"
        )
        
        client.send(request)
        
        return withTimeoutOrNull(60_000) {
            client.responses
                .filterIsInstance<NetsResponse.LocalMode>()
                .first()
                .let { response ->
                    if (response.isApproved) {
                        Result.success(CaptureResult(
                            amount = actualAmountOre,
                            timestamp = response.timestamp
                        ))
                    } else {
                        Result.failure(PaymentDeclinedException(
                            response.rejectionReason,
                            response.responseCode
                        ))
                    }
                }
        } ?: Result.failure(TimeoutException("Capture timeout"))
    }
    
    suspend fun cancelReservation(pumpId: String): Result<Unit> {
        val request = NetsRequestBuilder.transferAmount(
            terminalId = config.terminalId,
            ecrId = "LPG-$pumpId",
            type = TransactionType.REVERSAL,
            amountOre = 0
        )
        client.send(request)
        return Result.success(Unit)
    }
}
```

### 10.8 Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Ktor WebSocket Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // XML parsing (standard JDK)
    // Ingen ekstra avhengighet nødvendig
}
```

---

## 11. Testing og Simulering

### 11.1 Nets Simulator

Vi kan lage en enkel simulator som svarer på forespørsler:

```kotlin
class NetsSimulator {
    
    fun handleRequest(xml: String): String {
        // Parse request og generer passende respons
        return when {
            xml.contains("Dfs13TransferAmount") -> simulateTransferAmount(xml)
            xml.contains("Dfs13Administration") -> simulateAdministration(xml)
            else -> simulateError()
        }
    }
    
    private fun simulateTransferAmount(xml: String): String {
        // Alltid godkjenn i simulator
        return """
            <NetsResponse>
              <MessageHeader TerminalID="72199301" />
              <Dfs13LocalMode>
                <Result>1</Result>
                <TotalAmount>10000</TotalAmount>
                <TruncatedPan>************1234</TruncatedPan>
                <CardIssuerName>VISA</CardIssuerName>
                <ResponseCode>00</ResponseCode>
                <StanAuth>${System.currentTimeMillis()}</StanAuth>
                <Timestamp>${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}</Timestamp>
              </Dfs13LocalMode>
            </NetsResponse>
        """.trimIndent()
    }
}
```

### 11.2 Integrasjon med Eksisterende Emulator

Vi kan koble simulatoren til lpg-ehl-emulatorens WebSocket:

```kotlin
// I lpg-ehl-api
@Configuration
class NetsSimulatorConfig {
    
    @Bean
    fun netsSimulator() = NetsSimulator()
    
    @Bean  
    fun netsSimulatorEndpoint(simulator: NetsSimulator) = 
        NetsSimulatorWebSocketHandler(simulator)
}
```

---

## 12. Kommunikasjon til Teamet

### 12.1 For Alejandro (Teknisk)

**Emne: Nets Cloud Connect - Teknisk Implementasjon**

Hei Alejandro,

Her er det tekniske oppsettet for Nets Cloud Connect-integrasjonen:

**Protokoll:**
- WebSocket til Nets-server (alternativt TCP port 6000)
- XML-meldinger (NetsRequest/NetsResponse)
- Enkelt request/response-mønster

**Hovedkomponenter vi må lage:**
1. `NetsCloudConnectClient` - WebSocket-klient med Ktor
2. `NetsRequestBuilder` - Bygger XML-forespørsler
3. `NetsResponseParser` - Parser XML-svar til sealed classes
4. `NetsPaymentService` - Høynivå API for betalinger

**For LPG trenger vi:**
- `preAuthorize(amount)` → Reserverer beløp før fylling
- `capture(amount, stanAuth)` → Trekker faktisk beløp etter fylling
- `cancelReservation()` → Kansellerer hvis kunden ikke fyller

**Estimert arbeid:**
- Grunnleggende implementasjon: 1-2 dager
- Testing og feilhåndtering: 1 dag
- Integrasjon med eksisterende kode: 1 dag

**Kompleksitetsvurdering:** LAV
- Det er ren XML over socket
- Ingen kompleks autentisering
- Veldokumentert protokoll

Se vedlagt dokumentasjon for alle detaljer.

Mvh,
Thomas

---

### 12.2 For Tobias (Forretning)

**Emne: Nets Cloud Connect - Status og Forventninger**

Hei Tobias,

Her er en oppsummering av Nets Cloud Connect-integrasjonen:

**Hva er det?**
Nets Cloud Connect er tjenesten som lar vårt system snakke med betalingsterminaler via Nets' sky. Når en kunde drar kortet på LPG-stasjonen, går kommunikasjonen gjennom denne tjenesten.

**Hva håndterer Nets?**
- All sikker kortbehandling
- Kommunikasjon med bankene
- PCI-compliance (sikkerhetskrav)
- Kvitteringer

**Hva må vi gjøre?**
1. Sende forespørsel om reservering når kunde setter inn kort
2. Motta bekreftelse
3. La kunden fylle
4. Sende endelig beløp når fylling er ferdig

**Tidsestimat:**
- Implementasjon: 3-4 dager
- Testing: 1-2 dager

**Risiko:** LAV
- Veletablert og dokumentert API
- Vi har fungerende eksempelkode å bygge på
- Ingen komplekse integrasjoner

**Neste steg:**
1. Få tilgang til testmiljø fra Nets (Terminal-ID og server-adresse)
2. Implementere grunnleggende flyt
3. Teste mot Nets' testsystem

La meg vite hvis du har spørsmål!

Mvh,
Thomas

---

## Vedlegg

### A. Referansedokumentasjon fra Nets

- `Connect@Cloud_frontendAPI_documentation_v1.2.16.pdf` - Hovedspesifikasjon
- `Baxi.Agent_1.9.0.0_XMLInterface.pdf` - XML-protokoll
- `Baxi.NetProgrammersGuide_v1.13.5.0.pdf` - Programmererguide
- `LocalMode-TrxResultHandling_v1.2.pdf` - Responshåndtering
- `Baxi.Agent_Errorcodes.pdf` - Feilkoder

### B. Python-kildekode (referanse)

Se `/Nets Cloud Connect/Nets Cloud Connect/cc-python_ws-sample_20220113.zip`

### C. C# Baxi.NET (referanse - lokal terminal)

Se `/Nets Cloud Connect/Baxi NET/BaxiNetSampleApp_1.10.3.0/`

---

*Dokumentet oppdatert: 31. januar 2026*
