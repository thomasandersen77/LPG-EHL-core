# Python vs Kotlin EHL Alignment Analysis

**Dato:** 2026-02-12  
**Referanse:** `logger_pumpe_test.md`, `python-test/`, `lpg-ehl-core`, `lpg-ehl-service`, `lpg-transport`, `lpg-ehl-api`

## 1. Executive Summary

Denne analysen sammenligner Python-serveren i `python-test/python-server/` med Kotlin-implementasjonen for å verifisere at koblingskode, tilstandsmaskin og logikk er 100% på linje. Python-serveren frigir pumpen ved UNBLOCK; den eldre Kotlin-koden feilet fordi den forventet OK-respons, mens dispenseren returnerer STATE.

**Konklusjon:** Den refaktorerte Kotlin-koden er nå **tilnærmet 100% aligned** med Python. Følgende er implementert:

- UNBLOCK aksepterer STATE som respons (ikke OK) og verifiserer via `open_for_delivery` (0x02)
- `StatusBitMasks` fra core brukes konsekvent (0x02, 0x04, 0x08, 0x80)
- `receiveUntil` + `drain` + `withExclusive` håndterer interleaved VOLUME, concatenated frames og manglende OK
- Watchdog er tolerant i IDLE, streng under aktiv operasjon

---

## 2. Python-referanse

### 2.1 Python UNBLOCK-flyt (`server.py`)

```python
def unblock_verified(self) -> Dict[str, Any]:
    r = self.exchange(cmd=CMD_UNBLOCK, data=b"", expect_cmd=CMD_UNBLOCK, debug_frames=False)
    verify = self._verify_open_for_delivery(desired=True)
    return {"send": r, "verify": verify}
```

- **exchange(UNBLOCK, expect_cmd=UNBLOCK):** Sender UNBLOCK, venter på respons som matcher (addr, UNBLOCK).
- **Dispenser svarer med STATE (0x4B), ikke UNBLOCK:** → `rx_matched=None`, `rx_seen=[STATE]`.
- **verify_open_for_delivery(desired=True):** Poler STATE til `open_for_delivery==True`.
- **Suksess:** Når STATE viser `open_for_delivery=True` (0x02).

### 2.2 Python STATE-bitmapping (`ehl_protocol.py`)

```python
def interpret_state_byte(state_b: int) -> Dict[str, object]:
    sb = state_b & 0xFF
    return {
        "open_for_delivery": bool(sb & 0x02),   # bit1
        "startbutton_pressed": bool(sb & 0x04), # bit2
        "automode": bool(sb & 0x08),            # bit3
        ...
    }
```

| Bit | Maske | Python          | Bruk                   |
|-----|-------|-----------------|------------------------|
| 1   | 0x02  | open_for_delivery | Pumpe åpen for levering |
| 2   | 0x04  | startbutton_pressed | Startknapp trykket    |
| 3   | 0x08  | automode        | Automodus              |
| 7   | 0x80  | –               | (error i Kotlin)       |

### 2.3 Python BLOCK-flyt

- `block_verified()`: exchange(BLOCK, expect_cmd=BLOCK), deretter `_verify_open_for_delivery(desired=False)`.

---

## 3. Kotlin-implementasjon

### 3.1 lpg-ehl-core

| Komponent | Status | Beskrivelse |
|-----------|--------|-------------|
| `StatusBitMasks` | ✅ | OPEN_FOR_DELIVERY=0x02, START_BUTTON_PRESSED=0x04, AUTOMODE=0x08, ERROR_FLAG=0x80. Matcher Python. |
| `EhlPacketFormatter` | ✅ | Bruker StatusBitMasks. STATE=0x5A vises som `open_for_delivery=true, automode=true`. |
| `EhlCodec` | ✅ | STX 0x10/0x20, ETX 0x36, XOR-checksum. Matcher Python. |
| `EhlDataParser` | ✅ | VOLUME, PRICE VB6-format. Matcher Python. |

### 3.2 lpg-transport

| Komponent | Status | Beskrivelse |
|-----------|--------|-------------|
| `withExclusive` / `withExclusiveAttempt` | ✅ | RS-485 half-duplex: én kommando-sekvens per adresse. |
| `drain(durationMs)` | ✅ | Leser og kaster innkommende bytes. Tilsvarer Python sin forberedelse. |
| `receiveUntil(timeout, predicate, label)` | ✅ | Mottar pakker til predikat matcher. Ignorerer VOLUME, OK m.m. |
| `EhlCommunicatorReceiveUntilTest` | ✅ | Tester: interleaved VOLUME, concatenated frames, missing OK, delayed open-bit. |

### 3.3 lpg-ehl-service (PumpStateService)

| Komponent | Status | Beskrivelse |
|-----------|--------|-------------|
| UNBLOCK-flyt | ✅ | drain(100) → send(UNBLOCK) → STATE-poll med receiveUntil → sjekk OPEN_FOR_DELIVERY (0x02). |
| StatusBitMasks | ✅ | Bruker `StatusBitMasks.OPEN_FOR_DELIVERY` fra core. Ingen egne STATE_FLAG_*. |
| BLOCK-flyt | ✅ | sendAndReceive(BLOCK), deretter VOLUME. Aksepterer STATE som respons (via sendAndReceive). |
| pollStateForReadyPumps | ✅ | PUMPING = OPEN_FOR_DELIVERY + START_BUTTON_PRESSED. Matcher Python. |
| BusActivityTracker | ✅ | markActivity() ved unblock/block for watchdog-policy. |

### 3.4 lpg-ehl-api (PumpController)

- Eksponerer REST-endepunkter for unblock/block. Ingen protokolllogikk her.

---

## 4. UNBLOCK-flyt: Siden-for-siden

| Steg | Python | Kotlin (ny) |
|------|--------|-------------|
| 1 | exchange(UNBLOCK, expect_cmd=UNBLOCK) | drain(100), send(UNBLOCK) |
| 2 | Dispenser svarer STATE → rx_matched=None | – (venter ikke på OK) |
| 3 | _verify_open_for_delivery(True) | Loop: send(STATE), receiveUntil(800, predicate=STATE) |
| 4 | poll_state() inntil open_for_delivery | Sjekk `(sb and 0x02) != 0` |
| 5 | Suksess når open_for_delivery | Suksess når OPEN_FOR_DELIVERY |
| Timeout | verify_timeout_ms (2500) | 6 s (6000 ms) |
| Intervall | verify_interval_ms (200) | 300 ms STATE-intervall |

**Vurdering:** Kotlin følger samme resultat-orienterte strategi som Python: verifiser via STATE, ikke via OK. Kotlin er mer direkte ved å umiddelbart starte STATE-polling uten å vente på UNBLOCK-ACK.

---

## 5. STATE-bitmapping: Verifisering

| Bit | Python (`ehl_protocol.py`) | Kotlin (`StatusBitMasks`) | PumpStateService |
|-----|----------------------------|---------------------------|------------------|
| 0x02 | open_for_delivery | OPEN_FOR_DELIVERY | ✅ Brukes for UNBLOCK-verifisering og PUMPING-deteksjon |
| 0x04 | startbutton_pressed | START_BUTTON_PRESSED | ✅ Brukes for PUMPING-deteksjon |
| 0x08 | automode | AUTOMODE | ✅ Brukes i EhlPacketFormatter |
| 0x80 | – | ERROR_FLAG | ✅ Brukes i EhlPacketFormatter |

**Eldre avvik (fjernet):** PumpStateService brukte tidligere STATE_FLAG_AUTHORIZED (0x02), STATE_FLAG_PUMPING (0x04) med feil semantikk. Nå brukes StatusBitMasks konsekvent.

---

## 6. Transport og kobling

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| Half-duplex | threading.Lock i EhlBus | txMutex / withExclusive |
| Buffer ved timeout | Beholder remainder | receiveUntil ignorerer ikke-matcher; buffer cleares kun ved drain |
| Støy (VOLUME interleaved) | extract_frames parser alt | receiveUntil med predikat ignorerer VOLUME |
| Retry ved timeout | Ikke beskrevet i server | sendAndReceive har retry; UNBLOCK bruker receiveUntil (uten retry i poll-loop) |

**Kotlin forbedring:** receiveUntil håndterer interleaved VOLUME og concatenated frames uten å kaste gyldig STATE.

---

## 7. Verifikasjon mot logger_pumpe_test.md

### 7.1 Problem fra felt-test

Fra `logger_pumpe_test.md`:
- Kotlin sendte UNBLOCK (10 06 21 77 40 36).
- Dispenser svarte STATE (20 07 21 4B 5A 17 36).
- Kotlin: "❌ UNBLOCK avvist: Forventet OK, fikk STATE".

### 7.2 Ny Kotlin-løsning

1. **PumpStateService.unblock()** bruker ikke lenger sendAndReceive som forventer OK.
2. Kjører `withExclusive { drain(100); send(UNBLOCK); STATE-verify-loop }`.
3. STATE-verify-loop: sender STATE, bruker receiveUntil med predikat for STATE.
4. Aksepterer STATE med `(data[0] & 0x02) != 0` som suksess.

### 7.3 Python-suksess i felt

Fra loggen: Python-server returnerte:
```json
"verify": { "ok": true, "state": { "open_for_delivery": true, "raw": 90 } }
```
STATE raw=0x5A (90) → open_for_delivery=True (0x02 satt). Pumpen frigitt.

### 7.4 Forventet Kotlin-oppførsel nå

Med den nye koden vil Kotlin:
1. Sende UNBLOCK.
2. Motta STATE 0x5A (enten direkte eller etter STATE-poll).
3. Tolke 0x5A: OPEN_FOR_DELIVERY=1.
4. Returnere SUCCESS og opprette transaksjon.

---

## 8. State machine og logikk

### 8.1 Python state flow (implicit)

- UNBLOCK → verify open_for_delivery → pump klar
- BLOCK → verify open_for_delivery=false → pump lukket

### 8.2 Kotlin state flow (PumpStateService)

| Tilstand | Beskrivelse |
|----------|-------------|
| IDLE | Pumpe i ro |
| AUTHORIZED_WAITING | Kort trukket, venter på FRI DISPENSER |
| READY_TO_PUMP | UNBLOCK bekreftet, venter på at kunde starter |
| PUMPING | OPEN_FOR_DELIVERY + START_BUTTON_PRESSED |
| STOPPED | BLOCK sendt, volum hentet |

Kotlin har flere tilstander (kortdragning, timeout) som Python-serveren ikke har. Protokollnivået er likevel alignet.

---

## 9. Gjenstående gap og anbefalinger

| # | Gap | Prioritet | Anbefaling |
|---|-----|-----------|------------|
| 1 | PRODUCT_SELECT/RESET-fallback | Lav | Python 05_unlock har valgfri fallback. Kotlin har ikke dette. Kun relevant for spesifikke dispensere. |
| 2 | pollStateForReadyPumps @Scheduled disabled | Medium | HeadlessPollingService brukes. Verifiser at pumping-deteksjon kjører i produksjon. |
| 3 | RTS RS-485 | Lav | Python har rts-before/after. jSerialComm håndterer på noen adaptre. Verifiser på felt. |

---

## 10. Sjekkliste – klar for felt

- [x] PumpStateService aksepterer STATE med open_for_delivery som UNBLOCK-suksess
- [x] PumpStateService bruker StatusBitMasks (ingen egne feil masker)
- [x] verifyOpenForDelivery via receiveUntil-loop (6 s timeout)
- [x] EhlPacketFormatter: 0x5A vises som open_for_delivery, automode
- [x] EhlCommunicatorReceiveUntilTest: interleaved VOLUME, concatenated frames, missing OK, delayed open-bit
- [x] Watchdog tolerant i IDLE (BusActivityTracker)
- [ ] Felt-test: UNBLOCK → fysisk pumpe åpner (må verifiseres på hardware)
- [ ] Felt-test: BLOCK → fysisk pumpe stopper
- [ ] Felt-test: Watchdog under aktiv pumping

---

## 11. Konklusjon

Den refaktorerte Kotlin-implementasjonen er **aligned med Python** på:

- UNBLOCK: STATE-basert verifisering, open_for_delivery (0x02)
- STATE-bitmapping: StatusBitMasks 0x02, 0x04, 0x08, 0x80
- Transport: drain, receiveUntil, withExclusive for half-duplex
- Støyhåndtering: interleaved VOLUME, concatenated frames, manglende OK

Kotlin vil frigjøre pumpen når dispenseren svarer STATE 0x5A etter UNBLOCK, i tråd med Python-serveren. Anbefalt neste steg: felt-test med samme hardware som logger_pumpe_test.md.

---

*Dokumentet er gjennomgått to ganger for konsistens med python-test, lpg-ehl-core, lpg-ehl-service, lpg-transport og logger_pumpe_test.md.*
