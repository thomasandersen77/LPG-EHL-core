# Prompt til Cursor / JetBrains AI: Oppgrader Payment Terminal Simulator med WebSocket `/v1/events/ws` i tillegg til SSE (uten å røre produksjonskode)

## Mål
Jeg vil gjøre `lpg-ehl-payment-terminal-sim` “perfekt” som event-kilde før jeg endrer produksjonskoden (`lpg-ehl-service`).

Simulatoren skal fortsette å støtte **alle eksisterende REST-endepunkter og SSE**, men i tillegg tilby et **WebSocket-endepunkt**:

- `GET /v1/events/stream`  (SSE)  — behold som i dag
- `GET /v1/events?since=...` (REST polling) — behold som i dag
- `WS /v1/events/ws` (ny) — broadcast events i samme format som SSE/poll

GUI (`lpg-ehl-payment-terminal-gui`) skal trigge samme events som før, og disse skal nå dukke opp på:
- SSE stream
- WebSocket stream
- Polling endepunkt

## Repo-kontekst (finn og bruk som fasit)
- Simulator-modul: `lpg-ehl-payment-terminal-sim`
- GUI-modul: `lpg-ehl-payment-terminal-gui`
- OpenAPI-kontrakt: `openapi-payment-terminal.yaml` (REST-kontrakten må forbli kompatibel)
- Event-modell: `EventEnvelope` (PascalCase felt, Cursor, EventType, Payload, osv.)

## Ikke rør
- Ikke endre produksjonskode (`lpg-ehl-service`, webapp, osv.)
- Ikke endre eksisterende REST paths eller payload-format (bakoverkompatibilitet er krav)

## Krav (detaljert)

### 1) WebSocket endpoint: `/v1/events/ws`
Implementer WebSocket i Spring Boot simulatoren.

**Oppførsel:**
- Når en klient kobler seg til, skal den:
  1) få en “hello/connected”-melding (valgfritt, men nyttig)
  2) deretter motta alle nye terminal-events i sanntid

**Message format:**
- Hver WS-message skal være én JSON string som representerer `EventEnvelope` (samme som brukes i REST og SSE `data:`-payload)
- PascalCase beholdes slik simulatoren allerede gjør (ikke endre JSON naming)

**Eksempel WS message:**
```json
{
  "Cursor": 123,
  "EventType": "OperationCompleted",
  "Timestamp": "2026-02-13T18:30:00Z",
  "Payload": { "Success": true, "OperationId": "..." }
}

Reconnect / robustness:
	•	Serveren skal tåle mange reconnects uten memory leak
	•	Client disconnect skal fjernes fra subscriber-lista umiddelbart
	•	Skriv korte logs: WS connected, WS disconnected, WS broadcast event cursor=X

2) “Since” parameter for WS (frivillig men sterkt anbefalt)

For å støtte klienter som reconnecter og vil hente gapet:
	•	Tillat query param: /v1/events/ws?since=<cursor>
	•	Når WS kobler til:
	•	hvis since finnes: send backlog (alle events med cursor > since) først, i korrekt rekkefølge
	•	så live-stream videre

Dette må bruke samme event-store som REST GET /v1/events?since=.

3) Single Source of Truth: EventStore / Publisher

Refaktor internt i simulatoren slik at både:
	•	REST polling (/v1/events)
	•	SSE (/v1/events/stream)
	•	WebSocket (/v1/events/ws)
bruker samme publiseringsmekanisme.

Jeg vil ha et tydelig internt lag, f.eks.:
	•	TerminalEventStore (lagrer events + cursor)
	•	TerminalEventPublisher (publiserer nye events til SSE + WS)
	•	TerminalEventStreamRegistry (holder lister over aktive SSE/WS subscribers)

Viktig: Ikke dupliser event-generering. GUI og REST-handlinger skal kun “append” en event én gang.

4) SSE må fortsatt fungere uendret

SSE-implementasjon skal beholde eksisterende path og format.

Hvis SSE i dag sender:
	•	id: <cursor>
	•	event: terminal-event
	•	data: <EventEnvelope JSON>
så behold det.

5) Endepunktliste må være komplett

Simulatoren skal fortsatt tilby alle REST-endepunkter som eksisterer i dag og er i bruk:
	•	terminal open/close/ready/health (hva dere har)
	•	purchase/reserve/capture/reversal (hva dere har)
	•	/v1/events polling
	•	/v1/events/stream SSE
	•	NY: /v1/events/ws WebSocket

Lag en kort README-seksjon i simulatoren som lister:
	•	alle endpoints
	•	eksempel curl for purchase
	•	eksempel for polling
	•	eksempel for SSE
	•	eksempel WS (f.eks. wscat)

6) GUI-integrasjon: “Trekke kort” → event til WS + SSE + poll

Når jeg trykker “Trekke kort” i GUI (scenario APPROVED/DECLINED):
	•	GUI kaller simulator som før (ingen breaking change)
	•	Simulator genererer samme events som før
	•	Events skal:
	•	dukke opp på /v1/events?since=...
	•	pushes på /v1/events/stream
	•	pushes på /v1/events/ws

7) Dirty-mode (valgfritt, men lag plass)

Ikke implementer alt nå, men legg grunnlag for:
	•	latency injection (delay før broadcast)
	•	duplicate event probability (sende samme cursor to ganger – KUN i “dirty mode”)
	•	out-of-order option (dirty mode)
Disse må være OFF som default.

8) Tester

Legg til tester i simulatoren som verifiserer:
	1.	Når en event blir generert → den dukker opp i event-store
	2.	SSE subscriber får event (kan være unit-test av publisher)
	3.	WS subscriber får event (mock WebSocketSession, eller spring websocket test)
	4.	WS backlog med since fungerer (hvis implementert)

Ikke krev ekte nettverk i test hvis unødvendig – mock sessions.

Akseptanse / “perfekt”-kriterier

Jeg anser simulatoren “perfekt” når:
	•	Alle REST-endepunkter fortsatt fungerer som før
	•	SSE fungerer som før
	•	WS /v1/events/ws fungerer stabilt og sender samme envelope-format som SSE/poll
	•	GUI-trigger gir event på alle tre kanaler
	•	Backlog ved reconnect fungerer (hvis since implementert)
	•	God logging for feilsøking (kort og konkret)

Leveranse

Gi meg en PR-klar endring i lpg-ehl-payment-terminal-sim:
	•	Ny WS endpoint + wiring
	•	Refaktor til SSOT publisher/store
	•	Tester
	•	README / endpoint-dokumentasjon

Tips til implementasjon (ikke bind deg til dette hvis repo allerede har en standard)
	•	Spring Boot WebSocket:
	•	@EnableWebSocket
	•	implementer WebSocketHandler
	•	registrer handler på /v1/events/ws
	•	Bruk Jackson ObjectMapper som allerede er konfigurert for PascalCase DTOer
	•	Bruk thread-safe datastrukturer for subscriber-lista (CopyOnWriteArrayList / ConcurrentHashMap)
