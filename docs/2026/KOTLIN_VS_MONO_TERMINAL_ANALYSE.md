# Analyse: Hvorfor Kotlin-koden ikke starter terminalen

Sammenligning mellom **fungerende** C# MonoServer (`nets-cloud-solution/PaymentTerminalNetsCloudMonoServer/`)
og **ikke-fungerende** Kotlin-modul (`lpg-ehl/lpg-nets-cloud-connect/`).

---

## Hovedkonklusjon

Kotlin-koden har **3 kritiske feil** som gjør at terminalen aldri vil bli klar, pluss **4 manglende funksjoner** som ville blokkert operasjoner selv om tilkoblingen hadde fungert.

---

## 🔴 KRITISK 1: `connect()` blokkerer for alltid – Open-kommando sendes aldri

**Den viktigste feilen.** Ktor sin WebSocket-modell er fundamentalt annerledes enn C# sin `ClientWebSocket`.

### C# (fungerer)
```csharp
// ConnectCloudWebSocketClient.cs linje 60-88
await _ws.ConnectAsync(uri, CancellationToken.None);
// ConnectAsync returnerer umiddelbart. Session-objektet brukes fritt etterpå.
```
C# sin `ClientWebSocket.ConnectAsync()` returnerer straks tilkoblingen er oppe. WebSocket-objektet kan deretter brukes fritt fra hvilken som helst tråd. Receive-loopen kjører uavhengig i `ConnectCloudAdapter.RunReceiveLoopAsync()`.

### Kotlin (blokkerer)
```kotlin
// NetsCloudWebSocketClient.kt linje 40-59
httpClient.webSocket(
    request = { ... }
) {
    session = this
    isConnected = true
    listenerJob = launch { listenForMessages() }
    // ⚠️ Denne loopen kjører FOR ALLTID:
    while (isActive && isConnected) {
        delay(1000)
    }
}
```

Ktor sin `httpClient.webSocket { }` er en **koroutin-scope** som holder WebSocket-sesjonen i live kun så lenge blokken kjører. Koden har en `while`-loop inni som aldri avslutter.

**Konsekvens i `openTerminal()`:**
```kotlin
// NetsCloudConnectTerminalClient.kt linje 40-41
wsClient.connect(loginResponse.token)  // ← Henger her for alltid
// Linje 47-48 nås ALDRI:
val openRequest = messageBuilder.buildOpenRequest()
wsClient.sendMessage(openRequest)
```

**Løsning:** Kjør WebSocket-tilkoblingen i en separat coroutine-scope (f.eks. `CoroutineScope(Dispatchers.IO).launch { ... }`), og bruk en `CompletableDeferred` for å signalisere at sesjonen er klar før `connect()` returnerer.

---

## 🔴 KRITISK 2: Manglende WebSocket sub-protokoll `"json"`

### C# (fungerer)
```csharp
// ConnectCloudWebSocketClient.cs linje 68
_ws.Options.AddSubProtocol("json");
```
Connect@Cloud krever sub-protokollen `json` for å vite at meldinger er JSON-formaterte.

### Kotlin (mangler)
```kotlin
// NetsCloudWebSocketClient.kt linje 20-24
private val httpClient = HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = config.websocket.pingIntervalMs
    }
}
```
Ingen sub-protokoll settes. Connect@Cloud kan avvise tilkoblingen eller sende meldinger i feil format.

**Løsning:** Legg til header i WebSocket-requesten:
```kotlin
headers.append(HttpHeaders.SecWebSocketProtocol, "json")
```

---

## 🔴 KRITISK 3: ECRID genereres ny for hver melding

### C# (fungerer)
```csharp
// NetsMessageHeader.cs linje 25-35
public static string GetOrCreateEcrId(string prefix) {
    lock (_ecrLock) {
        if (_cachedEcrId != null) return _cachedEcrId;  // Returnerer cached
        _cachedEcrId = p + DateTime.UtcNow.ToString(...) + "-" + Guid...;
        return _cachedEcrId;
    }
}
```
ECRID genereres **én gang** og gjenbrukes for alle meldinger i sesjonen. Connect@Cloud bruker ECRID til å identifisere ECR-en (kassaregisteret).

### Kotlin (genererer nytt hver gang)
```kotlin
// NetsMessageBuilder.kt linje 38-44
private fun generateEcrId(): String {
    val timestamp = ...format(Instant.now())
    val suffix = UUID.randomUUID()...
    return "POS-$timestamp-$suffix"  // Ny ID hver gang!
}
```
Brukes i `buildOpenRequest()`, `buildPurchaseRequest()` osv. – dvs. **ny ECRID per melding**. Connect@Cloud kan tolke dette som forskjellige ECR-er og avvise kommandoer.

**Løsning:** Cache ECRID i et `companion object` eller instance-felt, generer kun ved første bruk.

---

## 🟡 MANGLER 1: Ingen priming/fallback ved Open-timeout

### C# (fungerer)
```csharp
// ConnectCloudAdapter.cs linje 349-360
SendOpenRequest();
if (_awaitTerminalReady.Wait(timeout) && ...) return 1;

// Fallback: send "LastResult" admin-kommando for å trigge respons
SendLastResultForPriming();  // admCode 12605
if (_awaitTerminalReady.Wait(TimeSpan.FromSeconds(10)) && ...) return 1;
```
Hvis terminalen ikke svarer med `Dfs13TerminalReady` innen timeout, sender C#-koden en admin-kommando (kode 12605 = "Last Financial Result") som tvinger terminalen til å respondere, og terminalen går til ready-tilstand.

### Kotlin (mangler helt)
Kotlin-koden venter bare på `TerminalReady` og gir opp etter 5 forsøk/timeout. Ingen fallback.

---

## 🟡 MANGLER 2: `MethodRejected(7102)` behandles ikke som "ready"

### C# (fungerer)
```csharp
// ConnectCloudAdapter.cs linje 149-153
else if (code == 7102) {
    _terminalReady = true;
    _awaitTerminalReady.Set();
}
```
Nets Connect@Cloud sender `MethodRejected` med kode 7102 når terminalen allerede er åpen. C#-koden tolker dette korrekt som "terminal er klar".

### Kotlin (mangler)
```kotlin
// NetsResponseParser.kt linje 10-11
fun isTerminalReady(message: String): Boolean =
    message.contains("Dfs13TerminalReady") || message.contains("ALREADY_OPEN")
```
Sjekker for streng-teksten `"ALREADY_OPEN"` (som ikke finnes i Connect@Cloud-protokollen), men sjekker **ikke** for `MethodRejected` med kode 7102.

---

## 🟡 MANGLER 3: Ingen auto-confirm for `Dfs13JsonReceived`

### C# (fungerer)
```csharp
// ConnectCloudAdapter.cs linje 174-209
else if (NetsResponseParser.HasDfs13JsonReceived(netsResponse)) {
    // Automatisk bekreft interaktive prompts (f.eks. PIN bypass)
    var confirmJson = $"{{\"confirm\":{{\"ver\":\"{ver}\",\"id\":{id},\"allow\":1}}}}";
    _wsClient.SendAsync(confirmJson).GetAwaiter().GetResult();
}
```
Under en transaksjon kan terminalen sende interaktive spørsmål (f.eks. "Pin bypass?"). C# bekrefter disse automatisk med `allow=1` for å unngå at transaksjonen henger.

### Kotlin (mangler helt)
Kotlin-koden har ingen håndtering av `Dfs13JsonReceived`. En transaksjon vil henge på ubestemt tid når terminalen sender en interaktiv prompt.

---

## 🟡 MANGLER 4: Ingen håndtering av `Dfs13LastFinancialResult`

### C# (fungerer)
```csharp
// ConnectCloudAdapter.cs linje 216-221
else if (NetsResponseParser.HasDfs13LastFinancialResult(netsResponse)) {
    var data = NetsResponseParser.GetDfs13LastFinancialResult(netsResponse);
    ApplyLocalMode(data);
    _awaitLocalMode.Set();
}
```
`Dfs13LastFinancialResult` er et alternativt sluttsignal for transaksjoner. C#-koden behandler det likt med `Dfs13LocalMode`.

### Kotlin (mangler)
```kotlin
// NetsResponseParser.kt linje 13-14
fun isTransactionComplete(message: String): Boolean =
    message.contains("Dfs13LocalMode")
```
Sjekker kun `Dfs13LocalMode`. Hvis terminalen svarer med `Dfs13LastFinancialResult` i stedet, vil Kotlin-koden aldri oppdage at transaksjonen er ferdig.

---

## Arkitekturforskjell: Meldingsmottak

### C# MonoServer
- **Receive-modell:** Uavhengig, evigvarende loop i `RunReceiveLoopAsync()` som prosesserer alle meldinger og signaliserer via `ManualResetEventSlim`
- **Interleaving:** Håndterer DisplayText, PrintText, JsonReceived, LocalMode osv. uavhengig av rekkefølge
- **TerminalID-filtrering:** Filtrerer meldinger på terminalID i headeren

### Kotlin
- **Receive-modell:** Sekvensiell `receiveMessage()` – én melding om gangen, manuelt kalt fra operasjonsmetodene
- **Interleaving:** Må sjekke hver meldingstype sekvensielt i en loop – risiko for å miste meldinger
- **TerminalID-filtrering:** Ingen filtrering – kan motta meldinger fra andre terminaler i samme konto

---

## Oppsummering: Hva må fikses for at Kotlin-koden skal fungere

### Må fikses (blokkerende):
1. **WebSocket-arkitektur**: Kjør WS-sesjonen i egen coroutine, la `connect()` returnere når sesjonen er klar
2. **Sub-protokoll `"json"`**: Legg til i WebSocket-handshake
3. **Cache ECRID**: Generer én gang per sesjon, gjenbruk for alle meldinger
4. **Priming-fallback**: Send admin 12605 hvis Open-timeout
5. **MethodRejected(7102)**: Behandle som "terminal ready"

### Bør fikses (vil blokkere transaksjoner):
6. **Auto-confirm `Dfs13JsonReceived`**: Svar med `allow=1`
7. **`Dfs13LastFinancialResult`**: Behandle som transaksjon fullført
8. **TerminalID-filtrering**: Ignorer meldinger til andre terminaler

---

## Referanser
- Fungerende C#: `~/git/NorgesGass/BaxiExperiments/nets-cloud-solution/PaymentTerminalNetsCloudMonoServer/`
- Kotlin-modul: `projects/lpg-ehl/lpg-nets-cloud-connect/src/main/kotlin/no/cloudberries/lpg/netscloud/`
- E-posttråd: `projects/lpg-ehl/lpg-nets-cloud-connect/docs/fra_epost_traad.md`
- Kodeanalyse MonoServer: `~/git/NorgesGass/BaxiExperiments/PaymentTerminalNetsCloudMonoServer/KODEANALYSE.md`