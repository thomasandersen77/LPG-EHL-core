Jeg kan samle dette til én kort, “copy/paste”-vennlig Markdown (og dele det i 3 Warp-steg så kontekstvinduet ikke sprenger). Jeg kan ikke lese /Users/... direkte, men jeg kan basere det på MM petro.pdf du lastet opp tidligere + instruksjonene du allerede har skrevet.

Nedenfor er en ferdig fil du kan legge i f.eks. docs/MM-Petro-Implementation.md.

# MM Petro (EHL-x4) – Implementasjonsnotat (Edge / ARK-3600)

## 1) Hvor dette skal bo
**MM Petro / EHL-x4** beskriver selve teller/dispensers “hardware brain”.
Derfor:
- **lpg-ehl-core**: serial/RS-485 config, framing/codec, parsing, state-machine, fault model
- **lpg-ehl-api**: policy/state guardrails, DB-persist, diagnostics/admin-endepunkt, observability
- UI (kiosk/admin): kun via API, aldri direkte serial

## 2) Minimal scope (det du faktisk trenger nå)
- Start/Stop (Authorize/Stop/Reset/Block/Unblock)
- Robust RX/TX (støy, timeouts, re-sync)
- Feilkoder fra EHL-x4 (E-xx) → “ERROR/OUT_OF_ORDER”
- Diagnostics-endepunkt for drift (hva skjedde, når, sist fault)

## 3) Konkrete leveranser (prioritert)
A) Core: SerialConfig + RS-485 hints
B) Core: Fault model (E-01/E-02/E-05/E-06/E-07 + UNKNOWN)
C) Core→API: State transitions (IDLE→AUTHORISED→FUELING→FINISHED + ERROR)
D) API: /admin/ehl/diagnostics
E) Tester: 3 unit-tester (fault parsing + critical->ERROR) + 1 integrasjonstest (emulator injiserer fault)

## 4) Designvalg (regler)
- Når fault er CRITICAL: sett state=ERROR umiddelbart og blokkér nye transaksjoner.
- All serial parsing må tåle:
    - “delvis pakke”
    - “to pakker back-to-back”
    - “korrupt data før STX”
- Ikke finn opp nye bytes: bruk eksisterende codec/packetformat i repo.

## 5) Foreslått pakke/filstruktur
- lpg-ehl-core
    - no.cloudberries.lpg.protocol/
        - EhlFault.kt (eller EhlError.kt)
        - DispenserState.kt (hvis ikke finnes)
        - (ev.) FaultSeverity.kt
- lpg-ehl-api
    - diagnostics/
        - EhlDiagnosticsController.kt
        - EhlDiagnosticsSnapshot.kt

## 6) Diagnostics – minimum payload
- connected: bool (eller “lastSeen < threshold”)
- lastRxAt / lastTxAt
- lastFault: code + reason + recommendedAction
- state: IDLE/AUTHORISED/FUELING/FINISHED/ERROR
- rs485Hints: tekst (terminering A/B, 120Ω, støy)

---

# WARP – Kjør i 3 steg (for lite kontekstvindu)

## STEG 1 (Core): Fault model + unit tests
Mål: Legg til enum + parser + 3 tester.

## STEG 2 (Core/API): Integrer fault i parsing + state reaction
Mål: Når fault kommer inn, publish event/return result, API går til ERROR og blokkerer kommandoer.

## STEG 3 (API): /admin/ehl/diagnostics + 1 integrasjonstest mot emulator
Mål: Eksponer snapshot + test at fault injeksjon -> ERROR + kommando-blokk.

---

# Git/branch (lokalt)
- Ny branch: feature/mm-petro-faults-and-diagnostics
- Commits:
    1) core: add fault model + tests
    2) core/api: integrate fault->state ERROR
    3) api: diagnostics endpoint + integration test

Warp-instruksjoner (3 “lime inn”-blokker)

STEG 1 – Fault model + tester

Du jobber i Kotlin repoet (lpg-ehl-core).
Lag en fault-model basert på EHL-x4 display-feil:
- E-01, E-02, E-05, E-06, E-07 + UNKNOWN
  Krav:
- enum med fields: code, severity (WARNING/CRITICAL), shortReason, recommendedAction
- companion: fromDisplayCode(code: String): EhlFault
- 3 unit tests:
    1) "E-01" -> CRITICAL
    2) " e-05 \r\n" normaliseres -> E-05
    3) ukjent -> UNKNOWN
       Plasser i package: no.cloudberries.lpg.protocol
       Lag små, idiomatiske Kotlin-tester (JUnit5).

STEG 2 – Integrasjon i parsing + state reaction

Integrer fault-detektering i eksisterende serial parsing i lpg-ehl-core (ikke lag ny protokoll).
Når incoming tekst/ramme indikerer "E-xx":
- Parse til EhlFault
- Returner/publiser resultat slik at lpg-ehl-api kan reagere
  I lpg-ehl-api:
- Hvis fault.severity == CRITICAL: sett DispenserState = ERROR (eller OUT_OF_ORDER), stopp autorisering, blokker kommandoer til reset/clear.
  Legg til unit test i API/service-laget som verifiserer at CRITICAL fault -> state ERROR og kommandoer avvises.
  Hold endringene små og koblet til eksisterende DispenserService/state-maskin.

STEG 3 – Diagnostics endpoint + integrasjonstest

Lag /admin/ehl/diagnostics i lpg-ehl-api som returnerer:
- connected (basert på lastRxAt/threshold)
- lastRxAt, lastTxAt
- state
- lastFault (code + reason + recommendedAction)
- rs485Hints (statisk tekst ok)
  Legg til 1 integrasjonstest:
- Start emulator (eller test-double)
- Injiser fault (E-01 eller E-05)
- Verifiser: API går til ERROR og diagnostics viser lastFault.code == "E-01" (eller valgt)

Branch/commit/push (kommandoer du kan kjøre)

git checkout -b feature/mm-petro-faults-and-diagnostics

# etter steg 1
git add .
git commit -m "core: add MM Petro fault model + tests"

