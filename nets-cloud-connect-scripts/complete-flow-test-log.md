# Complete Flow Test Log
Testkjøring: 2026-02-15T19:22:13.117283Z

## Logg
```
[2026-02-15T19:20:12.195621Z] 📋 Konfigurasjon:
[2026-02-15T19:20:12.197778Z]    ECRID: POS-20260215192012-29bef608 (brukes for hele sesjonen)
[2026-02-15T19:20:12.197955Z]    Beløp: 1 krone (100 øre)
[2026-02-15T19:20:12.197996Z] 
[2026-02-15T19:20:12.289044Z] 🔐 [1/4] Logger inn...
[2026-02-15T19:20:12.806932Z] ✅ Login OK! Terminal: 42696609
[2026-02-15T19:20:12.807007Z] 
[2026-02-15T19:20:12.807056Z] 🌐 [2/4] Kobler til WebSocket...
[2026-02-15T19:20:13.012960Z] ✅ WebSocket tilkoblet!
[2026-02-15T19:20:13.013029Z] 
[2026-02-15T19:20:13.013051Z] 📤 [3/4] Sender Open-kommando...
[2026-02-15T19:20:13.017716Z]    Sendt: {"NetsRequest":{"MessageHeader":{"$":{"ECRID":"POS-20260215192012-29bef608","Ter...
[2026-02-15T19:20:13.069385Z] 📨 Open-svar #1: {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215192012-29bef608","TerminalID":"42696609","VersionNumber":1}},"MethodRejected":{"Code":"7102","Info":"ALREADY_OPEN"}}}...
[2026-02-15T19:20:13.069454Z] ✅ Terminal allerede åpen - klar!
[2026-02-15T19:20:13.069478Z] 
[2026-02-15T19:20:13.069499Z] 💳 [4/4] Sender Purchase (1 krone = 100 øre)...
[2026-02-15T19:20:13.069518Z]    ⚠️  KOLLEGA: Tapp kortet NÅ!
[2026-02-15T19:20:13.069536Z] 
[2026-02-15T19:20:13.069685Z] ✅ Purchase sendt!
[2026-02-15T19:20:13.069715Z] 
[2026-02-15T19:20:14.039294Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T19:20:14.040228Z] 📨 Melding #1:
[2026-02-15T19:20:14.040304Z]    {"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215192012-29bef608","TerminalID":"42696609","VersionNumber":1}},"Dfs13DisplayText":{"$":{"Source":"1","TextID":"1011"},"_":"VENTER PÅ KORTET\r"}}}
[2026-02-15T19:20:14.040341Z] 
[2026-02-15T19:20:14.040508Z] 📺 TERMINAL DISPLAY: VENTER PÅ KORTET
[2026-02-15T19:20:14.040583Z] 
[2026-02-15T19:22:13.076731Z] ⏰ Timeout - ingen transaksjon fullført etter 2 minutter
[2026-02-15T19:22:13.076861Z] ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[2026-02-15T19:22:13.076896Z] 📊 RESULTAT:
[2026-02-15T19:22:13.077028Z]    Meldinger mottatt: 1
[2026-02-15T19:22:13.077072Z]    Transaksjon fullført: NEI
```

## Alle Meldinger (Raw JSON)
### Melding 1
```json
{"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215192012-29bef608","TerminalID":"42696609","VersionNumber":1}},"MethodRejected":{"Code":"7102","Info":"ALREADY_OPEN"}}}
```
### Melding 2
```json
{"NetsResponse":{"MessageHeader":{"$":{"ECRID":"POS-20260215192012-29bef608","TerminalID":"42696609","VersionNumber":1}},"Dfs13DisplayText":{"$":{"Source":"1","TextID":"1011"},"_":"VENTER PÅ KORTET\r"}}}
```
