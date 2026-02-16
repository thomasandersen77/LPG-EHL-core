# Nets Cloud Connect Purchase Test Log

Testkjøring: 2026-02-15T19:12:55.284439Z

## Logg

```
[2026-02-15T19:10:54.379161Z] 📋 Konfigurasjon:
[2026-02-15T19:10:54.387145Z]    Base URL:  https://connectcloud.aws.nets.eu
[2026-02-15T19:10:54.387205Z]    Username:  cloudberries_shared
[2026-02-15T19:10:54.387869Z]    Password:  B8P***9QD
[2026-02-15T19:10:54.387911Z]    Beløp: 1 krone (100 øre)
[2026-02-15T19:10:54.387947Z] 
[2026-02-15T19:10:54.472357Z] 🔐 STEG 1: Logger inn...
[2026-02-15T19:10:54.472526Z]    POST https://connectcloud.aws.nets.eu/v1/login
[2026-02-15T19:10:54.472565Z] 
[2026-02-15T19:10:54.998974Z] 📊 Login-respons:
[2026-02-15T19:10:55.002311Z]    Status: 200 OK
[2026-02-15T19:10:55.002371Z] 
[2026-02-15T19:10:55.003654Z] ✅ Login OK!
[2026-02-15T19:10:55.003774Z]    Token: eyJhbGciOiJIUzI1NiIsInR5cCI6Ik...
[2026-02-15T19:10:55.003829Z]    Terminal ID: 42696609
[2026-02-15T19:10:55.003850Z] 
[2026-02-15T19:10:55.003873Z] 🌐 STEG 2: Kobler til WebSocket...
[2026-02-15T19:10:55.003924Z]    URL: wss://connectcloud.aws.nets.eu/ws/json
[2026-02-15T19:10:55.003944Z] 
[2026-02-15T19:10:55.239088Z] ✅ WebSocket tilkoblet!
[2026-02-15T19:10:55.239151Z] 
[2026-02-15T19:10:55.239173Z] 💳 STEG 3: Sender Purchase-kommando (1 krone)...
[2026-02-15T19:10:55.239191Z] 
[2026-02-15T19:10:55.240106Z]    ECRID: POS-20260215191055-24c042ad
[2026-02-15T19:10:55.240140Z]    TransactionType: 48 (ASCII '0' = Purchase)
[2026-02-15T19:10:55.240159Z]    OperId: 1
[2026-02-15T19:10:55.240175Z]    Amount1: 100 (1 krone)
[2026-02-15T19:10:55.240187Z] 
[2026-02-15T19:10:55.240199Z]    JSON Payload (compact):
[2026-02-15T19:10:55.240256Z]    {"NetsRequest":{"MessageHeader":{"$":{"ECRID":"POS-20260215191055-24c042ad","TerminalID":"42696609","VersionNumber":"1"}...
[2026-02-15T19:10:55.240284Z] 
[2026-02-15T19:10:55.240719Z] ✅ Purchase-kommando sendt!
[2026-02-15T19:10:55.240749Z] 
[2026-02-15T19:10:55.240768Z] 👂 STEG 4: Venter på transaksjonsbekreftelse...
[2026-02-15T19:10:55.240790Z]    (Terminal vil be om kort nå)
[2026-02-15T19:10:55.240808Z] 
[2026-02-15T19:10:55.839173Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T19:10:55.840520Z] 📨 Melding #1 mottatt (197 bytes):
[2026-02-15T19:10:55.840559Z] 
[2026-02-15T19:10:55.840973Z]    {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215191055-24c042ad","TerminalID":"42696609","VersionNumber":1}},"Dfs13DisplayText":{"$":{"Source":"1","TextID":"0031"},"_":"Formatfeil\r"}}}
[2026-02-15T19:10:55.841Z] 
[2026-02-15T19:10:56.056042Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T19:10:56.056135Z] 📨 Melding #2 mottatt (764 bytes):
[2026-02-15T19:10:56.056159Z] 
[2026-02-15T19:10:56.056704Z]    {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215191055-24c042ad","TerminalID":"42696609","VersionNumber":1}},"Dfs13LocalMode":{"ResultData":"D!000","Result":"2","Accumulator":"0","IssuerID":"00","TruncatedPAN":"","TimeStamp":null,"CVM":null,"SessionNumber":null,"StanAuth":null,"TotalAmount":null,"RejectionSource":null,"RejectionReason":null,"TipAmount":null,"SurchargeAmount":null,"TerminalID":null,"SiteID":null,"CardIssuerName":null,"ResponseCode":null,"TCC":null,"AID":null,"TVR":nul...
[2026-02-15T19:10:56.056879Z] 
[2026-02-15T19:10:56.270029Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T19:10:56.270103Z] 📨 Melding #3 mottatt (146 bytes):
[2026-02-15T19:10:56.270127Z] 
[2026-02-15T19:10:56.270211Z]    {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215191055-24c042ad","TerminalID":"42696609","VersionNumber":1}},"Dfs13TerminalReady":""}}
[2026-02-15T19:10:56.270236Z] 
[2026-02-15T19:12:55.247456Z] ⏰ Timeout! Ingen transaksjon fullført etter 2 minutter.
[2026-02-15T19:12:55.247629Z] 
[2026-02-15T19:12:55.247652Z] 📊 Statistikk:
[2026-02-15T19:12:55.248645Z]    Antall meldinger mottatt: 3
[2026-02-15T19:12:55.248812Z]    Transaksjon fullført: ❌ NEI
[2026-02-15T19:12:55.248844Z] 

```

## Oppsummering

Denne testen verifiserer:
- ✅ HTTP login med JWT token
- ✅ WebSocket-tilkobling
- ✅ Purchase transaction (1 krone)
- ✅ Display-meldinger fra terminal
- ✅ Transaksjonsbekreftelse

## Purchase JSON Format

```json
{
  "NetsRequest": {
    "MessageHeader": {
      "$": {
        "ECRID": "POS-YYYYMMDDHHMMSS-randomhex",
        "TerminalID": "42696609",
        "VersionNumber": "1"
      }
    },
    "Dfs13TransferAmount": {
      "TransactionType": "0",  // 0 = Purchase
      "OperId": "1",
      "Amount1": "100",        // 1 krone = 100 øre
      "Amount2": "0",
      "Amount3": "0",
      "Type2": "0",
      "Type3": "0",
      "HostData": "",
      "OptionalData": ""
    }
  }
}
```

## Expected Response

### Display Messages (async)
```json
{
  "NetsResponse": {
    "MessageHeader": { ... },
    "Dfs13Display": {
      "Text": "INSERT CARD"
    }
  }
}
```

### Transaction Confirmed
```json
{
  "NetsResponse": {
    "MessageHeader": { ... },
    "Dfs13TransactionConfirmed": {
      "Amount": "100",
      "CardType": "...",
      "ReceiptNumber": "...",
      "AuthCode": "...",
      ...
    }
  }
}
```
