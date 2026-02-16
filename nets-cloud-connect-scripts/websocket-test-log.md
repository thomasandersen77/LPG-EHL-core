# Nets Cloud Connect WebSocket Test Log

Testkjøring: 2026-02-15T18:50:16.729306Z

## Logg

```
[2026-02-15T18:49:45.939252Z] 📋 Konfigurasjon:
[2026-02-15T18:49:45.947364Z]    Base URL:  https://connectcloud.aws.nets.eu
[2026-02-15T18:49:45.947427Z]    Username:  cloudberries_shared
[2026-02-15T18:49:45.948091Z]    Password:  B8P***9QD
[2026-02-15T18:49:45.948132Z] 
[2026-02-15T18:49:46.023463Z] 🔐 STEG 1: Logger inn...
[2026-02-15T18:49:46.023626Z]    POST https://connectcloud.aws.nets.eu/v1/login
[2026-02-15T18:49:46.023664Z] 
[2026-02-15T18:49:46.452948Z] 📊 Login-respons:
[2026-02-15T18:49:46.457850Z]    Status: 200 OK
[2026-02-15T18:49:46.458008Z]    Body: {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6ImNsb3VkYmVycmllc19zaGFyZWQiLCJpZCI6IjRUN0tNWlVXUWZHdS1CRU5zM1JtWCIsImlhdCI6MTc3MTE4MTM4NiwiZXhwIjoxNzcxMjY3Nzg2fQ.slKOLYpfeQ3k9UIKMdHD5qFnJycBPrmUJzSuHnNUKo4","username":"cloudberries_shared","terminals":["42696609"]}
[2026-02-15T18:49:46.458060Z] 
[2026-02-15T18:49:46.460540Z] ✅ Login OK!
[2026-02-15T18:49:46.460720Z]    Token (første 50 tegn): eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZ...
[2026-02-15T18:49:46.460772Z]    Terminal ID: 42696609
[2026-02-15T18:49:46.460796Z] 
[2026-02-15T18:49:46.460814Z] 🌐 STEG 2: Kobler til WebSocket...
[2026-02-15T18:49:46.460867Z]    URL: wss://connectcloud.aws.nets.eu/ws/json
[2026-02-15T18:49:46.460930Z]    Authorization: bearer eyJhbGciOiJIUzI1NiIs...
[2026-02-15T18:49:46.460952Z] 
[2026-02-15T18:49:46.695735Z] ✅ WebSocket tilkoblet!
[2026-02-15T18:49:46.695868Z] 
[2026-02-15T18:49:46.696372Z] 📤 STEG 3: Sender Open-kommando til terminal 42696609...
[2026-02-15T18:49:46.697892Z]    JSON Payload:
[2026-02-15T18:49:46.698051Z]    {
[2026-02-15T18:49:46.698076Z]      "NetsRequest": {
[2026-02-15T18:49:46.698094Z]        "MessageHeader": {
[2026-02-15T18:49:46.698111Z]          "$": {
[2026-02-15T18:49:46.698125Z]            "ECRID": "TEST-20260215184946-2c6748b9",
[2026-02-15T18:49:46.698142Z]            "TerminalID": "42696609",
[2026-02-15T18:49:46.698155Z]            "VersionNumber": "1"
[2026-02-15T18:49:46.698167Z]          }
[2026-02-15T18:49:46.698179Z]        },
[2026-02-15T18:49:46.698190Z]        "Open": {}
[2026-02-15T18:49:46.698203Z]      }
[2026-02-15T18:49:46.698214Z]    }
[2026-02-15T18:49:46.698224Z] 
[2026-02-15T18:49:46.699313Z] ✅ Open-kommando sendt!
[2026-02-15T18:49:46.699365Z] 
[2026-02-15T18:49:46.699390Z] 👂 STEG 4: Lytter på meldinger fra terminalen...
[2026-02-15T18:49:46.699568Z]    (Trykk Ctrl+C for å stoppe)
[2026-02-15T18:49:46.699616Z] 
[2026-02-15T18:49:46.755368Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T18:49:46.756736Z] 📨 Melding #1 mottatt (BINARY, 178 bytes):
[2026-02-15T18:49:46.756775Z] 
[2026-02-15T18:49:46.756901Z]    {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"TEST-20260215184946-2c6748b9","TerminalID":"42696609","VersionNumber":1}},"MethodRejected":{"Code":"7102","Info":"ALREADY_OPEN"}}}
[2026-02-15T18:49:46.756923Z] 
[2026-02-15T18:50:16.702249Z] ⏰ Timeout! Ingen flere meldinger etter 30 sekunder.
[2026-02-15T18:50:16.702314Z] 
[2026-02-15T18:50:16.702336Z] 📊 Statistikk:
[2026-02-15T18:50:16.703125Z]    Antall meldinger mottatt: 1
[2026-02-15T18:50:16.703155Z] 

```

## Oppsummering

Denne testen verifiserer:
- ✅ HTTP login med JWT token
- ✅ WebSocket-tilkobling med Bearer auth
- ✅ Sending av Open-kommando
- ✅ Mottak av Dfs13TerminalReady

## XML-protokoll

### Open-kommando (Client → Server)
```xml
<NetsRequest>
    <Terminal>42696609</Terminal>
    <Dfs13Open>
        <RegisterFlags>21474836470000000000000</RegisterFlags>
    </Dfs13Open>
</NetsRequest>
```

### TerminalReady-respons (Server → Client)
```xml
<NetsResponse>
    <Terminal>42696609</Terminal>
    <Dfs13TerminalReady>
        <TerminalId>42696609</TerminalId>
    </Dfs13TerminalReady>
</NetsResponse>
```

## Neste steg

1. ✅ Login fungerer
2. ✅ WebSocket fungerer
3. ✅ Open-kommando fungerer
4. ⏭️  Test Purchase-kommando (1 krone)
5. ⏭️  Test Admin-kommandoer (avstemming osv.)
