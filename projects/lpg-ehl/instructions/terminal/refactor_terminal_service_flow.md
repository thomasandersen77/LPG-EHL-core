# Mål
Koble LPG service-modulen sin betalingsflyt til **Nets Cloud Connect** slik at:
1) Pumpa kan frigjøres på 2 måter:
   A) Manuell frigiving fra stasjonseier (ingen kort)
   B) Event-basert frigiving når kunde “tapper/insert”x` kort (Nets Cloud Connect events)
2) Når fylling stopper skal transaksjonen alltid fullføres med korrekt betalingssteg:
    - Hvis “kortflyt”: reserver beløp først (preauth), frigjør pumpa, og gjør capture/settlement etter stopp
    - Hvis “manuell frigiving”: transaksjon kan merkes som betalt via valgt metode (CARD/CREDIT) uten terminal-capture
3) Fjern/unngå at gammel terminal-simulator/SSE-logikk påvirker Nets-flyten.

# Viktige observasjoner fra loggene (må brukes i implementasjonen)
- Pumpa går til PAYMENT_PENDING etter stopp og blir i dag “settled” manuelt uten aktiv auth (logg: "Settle pending transaction without active auth") og "Ingen STOPPED autorisasjon å fullføre" — dette skal kun skje i manuell stasjonseier-flyt.
- PaymentTerminalEventConsumer kjører i mode=sse mot http://192.168.0.9:18080 og reconnecter stadig. Nets Cloud Connect bruker egen WS/event-modell; dette må ikke stå og “late som” betaling.
- Dispenser address er 33 og skal komme fra config (ikke REST path).

# Scope / Begrensninger
- Ikke rør produksjonskode som ikke er nødvendig.
- Ikke endre REST paths for pumpeadresse her (det er del 1 dere allerede har gjort).
- Fokus nå: service-modulen (PumpPaymentOrchestrator) + Nets Cloud Connect modulen og integrasjonen.
- Bevar eksisterende manuell flyt fra GUI.

# Oppgaver (implementer i denne rekkefølgen)

## Del 1 — Lag et PaymentTerminal abstraction-lag som støtter Nets Cloud Connect
1) I service-modulen: lag et interface, f.eks. `PaymentTerminalClient` med operasjonene dere trenger:
    - `open()` / `close()` (idempotent)
    - `reserve(amountMinor, correlationId)` (preauth / purchase-start)
    - `capture(amountMinor, correlationId)` ELLER `complete()` avhengig av hva Nets faktisk støtter i deres klient
    - `reversal(correlationId)` ved avbrutt fylling/feil
    - `events(): Flow/Channel` eller callback registration for å motta terminal-events (card tapped, terminal ready, transaction result)
2) Implementer adapter i service-modulen som wrapper `NetsCloudConnectTerminalClient` fra nets-modulen.
    - Mapper domenebegrep: reserve/capture/reversal -> Nets sine meldinger.
    - Sørg for at adapteren kan kjøre uten Spring context (slik manual test gjør), men i produksjon via Spring DI.

**Done criteria**
- Service-modulen kompilerer uten å kjenne til Nets-spesifikke datatyper direkte (kun via adapter/interface).
- Du kan mocke `PaymentTerminalClient` i tester.

## Del 2 — Riktig betalingsflyt i PumpPaymentOrchestrator (to frigivingsmåter)
### A) Kortstyrt flyt (event-basert frigiving)
1) Når terminal-event “CARD_PRESENTED / CARD_TAPPED / INSERTED” kommer:
    - Opprett en ny “Payment Authorization” record koblet til dispenser 33 + en ny transaksjon i status f.eks. `AUTH_PENDING`.
    - Kall `terminal.reserve(150000, correlationId)` (eks 1500 NOK = 150000 øre) og vent på resultat.
2) Ved success reserve:
    - Sett transaksjon i status `AUTHORIZED` og marker “pump can be released”.
    - Kall pumpens release/unblock (via eksisterende service) og sett pumpetilstand `READY_TO_PUMP`.
3) Under fylling:
    - Oppdater UI-lives: liter + kroner konsistent (samme kilde i backend). Ikke gjør terminal-kall her.
4) Når pumping stopper og volum/beløp er kjent:
    - Sett transaksjon `PENDING_CAPTURE`.
    - Kall `terminal.capture(actualAmountMinor, correlationId)` (eller tilsvarende Nets “purchase finalize” hvis det er det dere bruker).
    - Ved success: sett transaksjon `PAID` og pumpa `BLOCKED/IDLE`.
    - Ved failure: forsøk `reversal`/feilhåndtering og sett transaksjon `PAYMENT_FAILED` + tydelig operator-action.

### B) Manuell frigiving (stasjonseier)
1) Når stasjonseier klikker “FRI PUMPE”:
    - Ikke gjør terminal reserve.
    - Frigi pumpa og start fylling direkte.
2) Når pumping stopper:
    - Sett transaksjon i `PAYMENT_PENDING`.
    - GUI lar operator velge “CARD” eller “CREDIT”.
    - Hvis operator velger “CARD” i denne flyten: det er OK at det forblir en manuell markering (ingen terminal-capture) — men logg det tydelig som MANUAL.
    - Hvis “CREDIT”: trigge Azure/kredittlogikk senere.

**Done criteria**
- Logglinjen "Settle pending transaction without active auth" skal kun forekomme i manuell flyt.
- Kortflyt skal alltid ha en aktiv “authorization/correlationId” før pumping frigjøres.

## Del 3 — Terminal-tilstand og timing: unngå “Terminal er opptatt”
Implementer robust state-maskin rundt Nets-terminal:
1) Ikke send `purchase/reserve` i samme millisekund som “TerminalReady”. Legg inn en liten settling-delay (f.eks. 500–1000ms) ELLER vent på en eksplisitt “IDLE/READY display state”.
2) Sørg for at kun én transaksjon kan være “in-flight” per terminal:
    - bruk Mutex/Atomic state i adapteren eller orchestratoren
    - hvis ny reserve kommer mens terminal er busy: kø eller returner “busy” til caller.
3) På “Terminal er opptatt / callResult=2 / cancelled”:
    - gjør retry med backoff (f.eks. 300ms, 700ms, 1500ms, maks 3–5 forsøk)
    - før retry: kall `getStatus()` og bekreft `CONNECTED + READY`.

**Done criteria**
- “Terminal er opptatt” håndteres uten å ødelegge flyten.
- Systemet prøver igjen og ender enten i success eller en tydelig `PAYMENT_FAILED` med operatorbeskjed.

## Del 4 — Avgrens/disable gammel SSE terminal consumer når Nets Cloud Connect er aktiv
1) Legg inn config-flag (Spring) som velger terminal-driver:
    - `payment.terminal.driver = nets-cloud-connect | legacy-sse-sim`
2) Når driver = nets-cloud-connect:
    - Ikke start `PaymentTerminalEventConsumer` (SSE mot 192.168.0.9:18080).
    - Ikke eksponer diag-endepunkter som peker mot gammel terminal-proxy med mindre de er eksplisitt slått på.
3) Når driver = legacy-sse-sim:
    - behold eksisterende SSE consumer.

**Done criteria**
- I field/prod med Nets: ingen reconnect-spam mot 192.168.0.9:18080.
- Terminal-events kommer fra Nets WS, ikke fra SSE.

## Del 5 — Testplan (må leveres sammen med endringen)
Lag tester på 3 nivåer:
1) Unit test av `PumpPaymentOrchestrator` med mock `PaymentTerminalClient`
    - kortflyt: card event -> reserve ok -> release -> pumping stop -> capture ok -> PAID
    - busy: reserve returnerer busy -> retry -> ok
    - failure capture -> reversal -> PAYMENT_FAILED
2) Integrasjonstest mot “fake Nets terminal” (kan være en enkel in-memory stub eller WireMock hvis dere bruker HTTP + WS)
3) Manual test (slik dere allerede har) men:
    - sørg for clean shutdown av WS-listener for å unngå “UncompletedCoroutinesError”.
    - legg inn settling delay mellom open og reserve/purchase.

**Done criteria**
- Testene dokumenterer flyten og hindrer regressjon.
- Manual test kan kjøres uten å henge.

## Del 6 — Logging/observability (for felt)
- Logg correlationId for hver betaling (reserve/capture/reversal) + dispenser address + transactionId.
- Logg eksplisitt hvilken flyt: `FLOW=CARD_EVENT` vs `FLOW=MANUAL_RELEASE`.
- Ved busy/retry: logg attempt count og status snapshot.

# Leveransekrav
- PR med små, fokuserte commits:
    1) PaymentTerminalClient + Nets adapter
    2) Orchestrator-flow (kort + manuell)
    3) Disable gammel SSE consumer når Nets aktiv
    4) Tester + manual-test forbedring
- Ingen breaking changes i REST for frontend i denne PRen.