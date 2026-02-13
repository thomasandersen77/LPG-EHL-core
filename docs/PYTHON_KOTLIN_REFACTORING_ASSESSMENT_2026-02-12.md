# Python vs Kotlin Alignment Review + Refactoring Needs (2026-02-12)

## Scope som er analysert

- Python-referansen som faktisk har åpnet pumpe i felt: `test-python/` + `_field-test/PYTHON_KOTLIN_ALIGNMENT_ANALYSIS.md`
- Kotlin-kjeden: `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-service`, `lpg-ehl-webapp`, `lpg-ehl-emulator`, `lpg-web`
- Simulatorer: `lpg-ehl-serialport-sim`, `lpg-ehl-payment-terminal-sim`, og service-klienten `SimulatedTerminalClient`
- Forslagene i «Gemini/ChatGPT refactoring suggestions» (inkl. echo/stale/watchdog/terminal-state-machine)

---

## 1) Kort konklusjon

### Hovedsvar
Du er **i stor grad aligned** med Python-koden som åpnet pumpe, spesielt på det viktigste punktet:

- Kotlin verifiserer nå UNBLOCK via `STATE` + `open_for_delivery`-bit (`0x02`), ikke via streng forventning om `OK`-ACK.

Dette matcher Python sin feltstrategi og er riktig retning.

### Viktigste gjede gap
Det finnes fortsatt en **seriell-konfigurasjonsrisiko** (særlig parity/defaults/hardcoding på tvers av moduler). Dette bør harmoniseres som “single source of truth” før du kaller løsningen fullstendig feltherdet.

---

## 2) Python-baseline (det som faktisk fungerte)

Fra Python-referansen:

- Framing: NorgesGass-variant (`STX controller=0x10`, `STX dispenser=0x20`, `ETX=0x36`, XOR-checksum)
- UNBLOCK-flyt: send UNBLOCK, verifiser deretter med STATE-poll til `open_for_delivery==true`
- STATE-bits:
  - `0x02` = open_for_delivery
  - `0x04` = startbutton_pressed
  - `0x08` = automode
- Python serial helper kjører rå serialoppsett i stil med `8N1`

---

## 3) Kotlin-status per modul

## 3.1 `lpg-ehl-core` — **God alignment**

- Protokollvariant og frame-parsing er konsistent med Python-flyten
- `StatusBitMasks` er korrekt brukt for `0x02/0x04/0x08`

## 3.2 `lpg-transport` — **Robust transportlogikk**

- `withExclusive()` gir riktig half-duplex-sekvens
- `drain()` brukes for å rydde gammeeiveUntil()` filtrerer bort irrelevante/interleavede frames
- Recovery-mekanismer finnes for parsefeil/støy

### Risiko (P0)
`SerialPortConfig` har default EVEN parity (8E1), mens Python-feltkitet som åpnet pumpe bruker 8N1. Dette kan fungere i noen miljøer, men er et reelt felt-risikopunkt hvis ikke eksplisitt styrt via konfig.

## 3.3 `lpg-ehl-service` (PumpStateService) — **Sterk alignment på unlock**

`unblock()` i Kotlin følger nå resultatorientert feltlogikk:

- `withExclusive { drain(100); send(UNBLOCK); STATE-verify-loop }`
- sjekk av `StatusBitMasks.OPEN_FOR_DELIVERY`
- tolererer at første relevante respons kan være STATE (ikke nødvendigvis OK)

Dette er akkurat den korreksjonen som trengs for Python-paritet.

## 3.4 Watchdog — **Riktig retning, liten nyansering**

- Attempt-basert helsesjekk i `SerialPortManager` er riktig for lavtrafikk-linjer
- Ikke “stillhet = feil” som hovedregel

### Forbedring
`HardwareWatchdogService` bruker fortsatt “time since last data” i telemetri/lås i drift. Behold telemetri, men dokumentér tydelig at reconnect-policy trigges av aktive feil i nylige operasjoner.

## 3.5 `lpg-ehl-webapp` adapterlag — **En konkret inkonsistens**

`RealSerialPortAdapter` hardcoder EVEN parity (`8E1`) i konstruksjon av `SerialPortConfig`.

Dette er en tydelig drift mellom «konfigurerbar serial policy» og faktisk runtime-adapter. Bør fjernes og styres 100% via konfig.

## 3.6 `lpg-ehl-emulator` — **Grei funksjonell baseline**

- Bra for utvikling/intern test
- Ikke ment å være full “dirty bus”-simulator alene

---

## 4) Simulator-vurdering mot hardware-realisme

## 4.1 `lpg-ehl-serialport-sim` — **God base, men ikke komplett feltskitten ennå**

Du har allerede mye nyttig:
- drop response
- concatenated frames
- no ACK på unblock/block
- unsolicited volume
- inter-character delay

Mangler fortsatt som førstegangs-knobs:
- local echo sannsynlighet
- stale frame injection (forrige tx STATE/VOLUME)
- tydelig random noise burst før gyldig frame
- eksplisb (ikke bare generell chunking)
- tydelig startup-dump av aktive fault-knobs + trigger-logging

**Vurdering:** Prompten din for “Drammen-skitten modus” er faglig godt begrunnet og anbefales.

## 4.2 `lpg-ehl-payment-terminal-sim` + `SimulatedTerminalClient` — **Brukbar, men fortsatt enkel**

- Simulatoren har scenario-system, events, reservation/completion, idempotency-cache og delay-variasjon
- Service-klienten (`SimulatedTerminalClient`) er en tynn HTTP-wrapper (reserve/capture/reversal)
- Ikke en full per-transaksjon terminal-state-machine i klientlaget

**Vurdering:** Prompten din om mer realistisk state machine + feilmodi + kontraktstesting er riktig neste 
---

## 5) Vurdering av Gemini-punktene dine

Din kritiske vurdering er i hovedsak riktig:

1. **Stale data trap**: korrekt problemstilling
2. **Watchdog på stillhet er feil i lavtrafikk**: korrekt
3. **Last seen state ved timeout**: høy verdi i felt
4. **Echo-filter byte-for-byte som absolutt regel**: du har rett i at dette må være heuristikk, ikke absolutt sannhet
5. **Timestamp/nanoTime-barriere**: du har rett i at epoch/sekvensbarriere er mer robust enn naiv timestamp-logikk
6. **Global “ignorer STX=controller alltid”**: du har rett i at dette bør være kontekstuelt
7. **PascalCase-naming**: du har rett i nyanseringen (mindre kritisk der `@JsonProperty` allerede er tydelig)

---

## 6) Refactoring som faktisk bør gjøres (prioritert)

## P0 (først)

1. **Serial config SSOT (single source of truth)**
   - Fjern hardcoded parity i adapter(e)
   - Bruk én felles, eksplisitt konfig-kjede
   - Logg effektiv serial-konfig ved startup

2. **Lås feltprofil mot verifisert hardware-oppsett**
   - Hver 9600/8N1, gjør det til tydelig standard i riktig profile

## P1

3. **Bedre timeout-diagnostikk i unblock/state-verifisering**
   - siste observerte state-byte + hvilke frames som ble ignorert

4. **Command-epoch/barriere for stale-debugging**
   - metadata rundt UNBLOCK→STATE verify-loop

5. **Utvid serial-sim fault knobs**
   - echo, stale, noise, fragmentering, trigger-telemetri

## P2

6. **Terminal-sim modenhet**
   - tydelig transaksjons-state-machine
   - dirty-mode knobs (offline/slow/duplicate/out-of-order/stuck)
   - kontraktstester mot `openapi-payment-terminal.yaml`

---

## 7) Direkte svar på spørsmålet ditt

> «Er jeg aligned med Python-koden som testet og åpnet dispenserpumpe?»

**Ja — på den kritiske pumpelogikken er du aligned nå.**

Spesielt: Kotlin følger samme praktiske prinsipp som Python (STATE-verifisering av `open_for_delivery`, ikke streng ACK-avhengighet).

**Men:** Jeg anbefaler at du fullfører serialing (parity/SSOT) og simulator-hardening før du erklærer full feltparitet og produksjonsherding.
