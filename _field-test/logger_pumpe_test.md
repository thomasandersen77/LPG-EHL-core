# Logger pumpe test

## Introduksjon – Hva gikk galt, og hvordan fikse det

Denne loggen dokumenterer en felt-test av lpg-ehl i FIELD mode mot fysisk dispenser på /dev/ttyS3.

## Hva som faktisk skjer

1. Applikasjonen starter korrekt
   - Seriell port åpnes
   - Watchdog aktiveres
   - EHL-kommunikator opprettes
   - Pris gjenopprettes fra database
2. Watchdog timeouts oppstår
   - Ingen data mottas fra dispenser
   - Automatisk reconnect fungerer teknisk
   - Men det kommer fortsatt ingen gyldig respons
3. UNBLOCK sendes fra Kotlin
   - Riktig frame sendes: 10 06 21 77 40 36
   - Dispenser svarer
   - Men svaret er STATE – ikke OK
4. Python-testen fungerer
   - Samme UNBLOCK-frame sendes
   - Python følger opp med STATE-query (0x4B)
   - Python tolker STATE=0x5A som:
     - open_for_delivery=True
     - automode=True
   - Python verifiserer dermed at pumpen ER frigitt

---

## Kjerneproblemet

Kotlin-koden forventer:

UNBLOCK → OK

Men dispenseren svarer:

UNBLOCK → STATE (0x4B)

Og STATE=0x5A betyr at pumpen faktisk er:
- Åpen for levering
- I automode
- Klar for fylling

👉 Dispenseren returnerer ikke OK-ack for UNBLOCK.
👉 Den signaliserer status direkte via STATE.

Dette er en klassisk EHL-variant der:

UNBLOCK bekreftes indirekte via STATE, ikke OK.

Python-koden håndterer dette korrekt.
Kotlin-koden avviser det som feil.

---

## Hvordan fikse det (Kotlin)

### 1. Endre UNBLOCK-responslogikk

I stedet for:

```
if (response.command != OK) fail()
```

Implementer:

```
if (response.command == OK) success()
else if (response.command == STATE && state.openForDelivery) success()
else fail()
```

---

### 2. Juster State-bit mapping

Fra Python-loggen:

STATE raw=0x5A
bits=01011010
open_for_delivery=True
automode=True

Kotlin må:
- Tolke 0x5A korrekt
- Matche Python sin bitmapping
- Ikke bruke alternativ bit-definisjon

---

### 3. Ikke clear receive-buffer før STATE tolkes

Loggen viser:

Receive buffer cleared
RETRY attempt 1/3

Dette kan føre til at:
- STATE-responsen mistolkes som “ikke OK”
- Retry trigges unødvendig

Retry bør kun skje hvis:
- Ingen respons
- Checksum-feil
- ERROR-kommando

---

### 4. Autorisasjonsmodell må samsvare med Python

Python gjør:

UNBLOCK
STATE
Verify open_for_delivery == True

Kotlin bør gjøre:

UNBLOCK
→ Hvis STATE mottas
→ Evaluer state bits
→ Hvis open_for_delivery = true → SUCCESS

---

## Foreslått teststrategi

1. Kjør EHL simulator i EHL-mode
2. Simuler STATE=0x5A etter UNBLOCK
3. Verifiser at Kotlin nå returnerer SUCCESS
4. Test på ekte hardware

---

## Taskliste – Justering av Kotlin mot Python

### Protokollnivå
- Tillat STATE som gyldig respons på UNBLOCK
- Implementer STATE-basert suksess-evaluering
- Synkroniser bitmapping med Python-implementasjon
- Fjern unødvendig buffer-clear før respons-evaluering
- Reduser retry-trigger ved gyldig STATE

---

### Service-lag
- Oppdater PumpController til å akseptere STATE som suksess
- Logg state-bits eksplisitt ved UNBLOCK
- Dokumenter EHL-variant (STATE-ack i stedet for OK)

---

### Feltverifisering
- Test UNBLOCK → fysisk pumpe åpner
- Test BLOCK → fysisk pumpe stopper
- Test watchdog under aktiv pumping
- Verifiser at ingen unødvendige reconnect skjer

---

## Avviksanalyse: Python vs Kotlin

### 1. UNBLOCK-respons: Python vs Kotlin

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| **Forventet respons** | OK (0x30) eller STATE | Kun OK (0x1E) |
| **Ved STATE-respons** | Fallback: poll STATE til `open_for_delivery==True` | Feil: "UNBLOCK avvist" |
| **Kilde** | `03_control_unblock_block.py` (await_ack → await_state_effect), `python-server/server.py` (`unblock_verified`) | `PumpStateService.kt` linje 220 |

**Python-flyt** (`server.py` unblock_verified):
1. Send UNBLOCK
2. `exchange(expect_cmd=CMD_UNBLOCK)` – returnerer typisk `rx_matched=None` (dispenser sender STATE, ikke UNBLOCK)
3. `_verify_open_for_delivery(desired=True)` – poller STATE til `open_for_delivery==True`
4. Suksess når STATE viser `open_for_delivery=True`

**Kotlin-flyt** (`PumpStateService.kt` unblock):
1. Send UNBLOCK
2. `ehlCommunicator.sendAndReceive()` – returnerer første gyldige pakke (STATE)
3. `if (unblockResponse.command != EhlCommand.OK)` → FEIL, returnerer `Result.failure`
4. STATE-steg kjøres aldri

---

### 2. STATE-bitmapping: Core vs Service vs Python

| Bit | Python (`ehl_protocol.py`) | Core (`StatusBitMasks`) | Service (`PumpStateService`) |
|-----|---------------------------|-------------------------|------------------------------|
| 0x01 | bit0 | – | STATE_FLAG_NOZZLE_LIFTED |
| 0x02 | open_for_delivery (bit1) | OPEN_FOR_DELIVERY | STATE_FLAG_AUTHORIZED |
| 0x04 | startbutton_pressed (bit2) | START_BUTTON_PRESSED | STATE_FLAG_PUMPING |
| 0x08 | automode (bit3) | AUTOMODE | STATE_FLAG_TRANSACTION_COMPLETE |
| 0x80 | – | ERROR_FLAG | – |

**Avvik:** PumpStateService bruker egne konstanter og semantikk som ikke matcher core/Python:
- `STATE_FLAG_AUTHORIZED=0x02` – i core/Python er 0x02 `OPEN_FOR_DELIVERY`
- `STATE_FLAG_PUMPING=0x04` – i core/Python er 0x04 `START_BUTTON_PRESSED`
- PumpStateService bruker ikke `DispenserStateMapper` eller `StatusBitMasks` fra core

**For UNBLOCK-verifisering:** Python bruker `open_for_delivery` (0x02). Kotlin bør bruke samme bit for å vurdere "pumpe frigitt".

---

### 3. Retry og buffer-håndtering

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| **Buffer ved timeout** | Beholder remainder for neste lesing | `EhlCommunicator`: clearer buffer før retry |
| **Retry-trigger** | Kun ved manglende respons | Timeout → clear buffer → retry |
| **Konsekvens** | STATE som kommer etter timeout kan leses i neste poll | STATE kan bli kastet ved retry før den tolkes |

**Kotlin** (`EhlCommunicator.kt`): Ved timeout cleares buffer (`clearBuffer()`), deretter retry. STATE som allerede ble mottatt (men inntraff etter at Kotlin ga timeout) kan dermed forsvinne eller tolkes som feil.

---

### 4. Python-server: UNBLOCK + STATE-poll

Python-serveren sender:
1. UNBLOCK (0x77)
2. STATE (0x4B) – for å verifisere effekt

Dispenseren svarer på UNBLOCK med STATE (0x4B, data=0x5A). Python ignorerer om UNBLOCK får OK eller STATE og går rett til STATE-poll for verifisering.

---

### 5. Konkrete filer og linjer

| Fil | Avvik/beskrivelse |
|-----|-------------------|
| `PumpStateService.kt:220` | Krever `EhlCommand.OK`; bør tillate STATE med `open_for_delivery` (0x02) |
| `PumpStateService.kt:151-156` | Egne STATE-masker; bør bruke `StatusBitMasks` fra core |
| `EhlCommunicator.kt` | Clear buffer ved retry kan fjerne gyldig STATE |
| `DispenserStateMapper.kt` | Brukes ikke av PumpStateService.unblock() |
| `_field-test/test-python/ehl_protocol.py:178-199` | `interpret_state_byte` – referanse for bitmapping |
| `_field-test/test-python/python-server/server.py:331-363` | `unblock_verified` – UNBLOCK + STATE-verifisering |
| `_field-test/test-python/03_control_unblock_block.py:224-255` | `await_state_effect` – STATE-poll som fallback |

---

### 6. Anbefalt retting (Kotlin)

1. **PumpStateService.unblock()**  
   Tillat STATE som gyldig respons:
   ```kotlin
   if (unblockResponse.command == EhlCommand.OK) {
       // OK – fortsett til STATE-verifisering
   } else if (unblockResponse.command == EhlCommand.STATE && unblockResponse.data.isNotEmpty()) {
       val sb = unblockResponse.data[0].toInt() and 0xFF
       if ((sb and 0x02) != 0) {  // open_for_delivery
           // SUCCESS – hopp til transaksjonsopprettelse
       } else {
           return Result.failure(...)
       }
   } else {
       return Result.failure(...)
   }
   ```

2. **Eliminer duplikat STATE-logikk**  
   Bruk `StatusBitMasks`/`DispenserStateMapper` fra core i stedet for egne konstanter i PumpStateService.

3. **Retry-logikk**  
   Vurder å ikke cleare buffer umiddelbart ved timeout hvis en gyldig STATE allerede er på plass, eller å tolke STATE før retry.

---

## Utvidet avviksanalyse: Kopling (Kotlin) vs Python

### Oversikt – områder som stemmer

| Område | Status | Merknad |
|--------|--------|---------|
| **Frame-format** | OK | Begge: STX + LEN + ADDR + CMD + DATA + CHK + ETX |
| **STX/ETX (Norges Gass)** | OK | Python: 0x10/0x20/0x36. Kotlin `EhlProtocolConfig.NORGES_GASS`: samme |
| **Checksum** | OK | XOR fra STX til siste DATA-byte i begge |
| **VOLUME-parsing** | OK | 5 ASCII-bytes LSB-first, `EhlDataParser.parseVolumeDataVb6` matcher Python `interpret_volume_bytes` |
| **PRICE-parsing** | OK | 4 ASCII-bytes LSB-first, `EhlDataParser.parsePriceData` matcher Python `interpret_price_bytes` |
| **ERROR-parsing** | OK | `EhlErrorMessages.asciiToInt` (0x30–0x39→0–9) matcher Python `_vb_val_chr_byte` |
| **ERROR-tekster** | OK | `EhlErrorMessages` portet fra Python `_VB6_ERR_TEXT_NO` |
| **Adresse** | OK | Python 1–255; Kotlin `EhlPacket` bruker samme |

### Avvik som krever retting

#### 1. UNBLOCK-respons (kritisk)

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| Forventet respons | OK eller STATE | Kun OK |
| Ved STATE | Fallback: poll STATE til `open_for_delivery==True` | Feil: "UNBLOCK avvist" |

**Kilde:** `PumpStateService.kt` linje ~220.

---

#### 2. STATE-bitmapping – PumpStateService bruker feil konstanter

| Bit | Python | Core (`StatusBitMasks`) | PumpStateService |
|-----|--------|-------------------------|------------------|
| 0x02 | open_for_delivery | OPEN_FOR_DELIVERY | STATE_FLAG_AUTHORIZED |
| 0x04 | startbutton_pressed | START_BUTTON_PRESSED | STATE_FLAG_PUMPING |
| 0x08 | automode | AUTOMODE | STATE_FLAG_TRANSACTION_COMPLETE |

**Problem:** PumpStateService bruker egne konstanter og tolker bitene feil. For UNBLOCK må `open_for_delivery` (0x02) brukes.

**Forslag:** Bruk `StatusBitMasks.OPEN_FOR_DELIVERY` og `DispenserStateMapper` fra core.

---

#### 3. Retry og buffer-clear

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| Buffer ved timeout | Beholder remainder | `clearBuffer()` før retry |
| Retry-trigger | Kun ved manglende respons | Timeout → clear → retry |

**Kilde:** `EhlCommunicator.kt` linje 126–128:
```kotlin
clearBuffer()
logger.debug("🔄 RETRY attempt $attempt/...")
```

**Konsekvens:** STATE som er mottatt men ikke tolket ennå kan kastes ved retry.

---

#### 4. Response-filtering – exchange vs sendAndReceive

| Aspekt | Python `exchange()` | Kotlin `sendAndReceive()` |
|--------|---------------------|---------------------------|
| Filtrering | `predicate: addr+expect_cmd` | Ingen filtrering |
| rx_matched | Kun når pred stemmer | Første gyldige pakke |

**Python:** `exchange(expect_cmd=CMD_UNBLOCK)` returnerer `rx_matched=None` når dispenser sender STATE. Python går da til STATE-poll.

**Kotlin:** Tar den første gyldige pakken (STATE). Problemet er ikke filtrering, men at PumpStateService fordømmer STATE som feil.

---

### Avvik som bør verifiseres

#### 5. Timeouts og delays

| Parameter | Python | Kotlin |
|-----------|--------|--------|
| `timeout-ms` / response timeout | 1200 ms (05_unlock), 500 ms (06_test) | 2000 ms (EhlProtocolConfig), 5000 ms (receive default) |
| Inter-command delay | `inter_command_delay_ms` (server) | 100 ms (EhlProtocolConfig) |
| Retries | 3 (05_unlock) | 3 (RetryConfig.DEFAULT) |

**Forslag:** Dokumenter at Kotlin bruker lengre timeout enn Python. Kan være ønskelig for tregere hardware.

---

#### 6. RS-485 RTS/Direksjon

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| RTS før/etter TX | `--rts-before-ms`, `--rts-after-ms` i `serial_linux.py` | Ingen eksplisitt RTS-håndtering i `SerialPortManager` |

**Merknad:** jSerialComm håndterer RS-485 på noen adaptre automatisk. Verifiser at Kotlin fungerer på samme hardware som Python.

---

#### 7. EhlCodec utfører ikke config

`EhlCommunicator.send()` kaller `EhlCodec.encode(packet)` uten `config`. EhlCodec bruker da `EhlProtocolConfig()` = NORGES_GASS. Dette stemmer med Python.

**Verifisering:** Sørg for at transport-laget aldri bruker `STANDARD_EHL` med Norges Gass-utstyr.

---

#### 8. VOLUME – Python vs Kotlin format

Python `interpret_volume_bytes`: `f"{chr(data[4])}{chr(data[3])}{chr(data[2])}.{chr(data[1])}{chr(data[0])}"` → f.eks. "045.50".

Kotlin `parseVolumeDataVb6`: `"$d4$d3$d2$d1$d0"` → "04550", deretter `/100` → 45.50.

**Stemmer:** Samme semantikk, ulik strengrepresentasjon (Python "XX.X" vs Kotlin Double).

---

#### 9. PRODUCT_SELECT/RESET-fallback ved UNBLOCK-feil

| Aspekt | Python | Kotlin |
|--------|--------|--------|
| Ved UNBLOCK uten ACK | Fallback: PRODUCT_SELECT (0xC3) + RESET (0x81) før retry | Ingen fallback |

**Kilde:** `05_unlock_hold_block.py` linje 277–283. Kan deaktiveres med `--disable-product-select-fallback` og `--disable-reset-fallback`.

**Forslag:** Vurder å implementere tilsvarende fallback i Kotlin for dispensere som krever PRODUCT_SELECT eller RESET før UNBLOCK.

---

### Oppsummering – prioriterte rettinger

| Prioritet | Oppgave | Fil |
|-----------|---------|-----|
| 1 | Tillat STATE som gyldig UNBLOCK-respons; evaluer `open_for_delivery` (0x02) | `PumpStateService.kt` |
| 2 | Bruk `StatusBitMasks`/`DispenserStateMapper` i stedet for egne konstanter | `PumpStateService.kt` |
| 3 | Vurder å ikke cleare buffer ved retry før STATE er tolket | `EhlCommunicator.kt` |
| 4 | Dokumenter timeout-forskjell Python vs Kotlin | `EhlProtocolConfig`, README |
| 5 | Test RS-485 RTS oppførsel på felt-hardware | `SerialPortManager.kt` |

---

### Referanse – nøkkelfiler

**Python (referanse for denne analysen):** `_field-test/test-python/`

| Fil | Hensikt |
|-----|---------|
| `_field-test/test-python/ehl_protocol.py` | Protokoll, framing, parsing, `interpret_state_byte` |
| `_field-test/test-python/python-server/server.py` | `unblock_verified`, `exchange`, `poll_state`, `_verify_open_for_delivery` |
| `_field-test/test-python/03_control_unblock_block.py` | `await_ack`, `await_state_effect` |
| `_field-test/test-python/05_unlock_hold_block.py` | `--timeout-ms`, `--retries`, `--verify-ms` |
| `lpg-ehl-core/.../EhlCodec.kt` | encode/decode |
| `lpg-ehl-core/.../EhlDataParser.kt` | VOLUME, PRICE, ERROR |
| `lpg-ehl-core/.../DispenserStatus.kt`, `StatusBitMasks` | STATE-bitmapping |
| `lpg-ehl-service/.../PumpStateService.kt` | unblock(), STATE-logikk |
| `lpg-transport/.../EhlCommunicator.kt` | sendAndReceive, retry, clearBuffer |

---

## Logganalyse – tidslinje fra felt

| Tidspunkt | Hendelse |
|-----------|----------|
| 10:39:22 | Serial port åpnet, watchdog 60s aktiveres |
| 10:39:26–10:39:27 | Oppstart: pris gjenopprettet |
| 10:40:59 | Watchdog timeout #1: 97s uten RX → reconnect |
| 10:42:04 | Watchdog timeout #2: 60s uten RX → reconnect |
| 10:42:47 | UNBLOCK sendes (frame: 10 06 21 77 40 36) |
| 10:42:50 | Timeout 3s – ingen respons på første forsøk |
| 10:42:51 | Buffer cleared, RETRY #1 |
| 10:42:51 | UNBLOCK sendes igjen |
| 10:42:51 | RX: 20 07 21 4B 5A 17 36 (STATE, data=0x5A) |
| 10:42:51 | ❌ UNBLOCK avvist: Forventet OK, fikk STATE |

**Python-server (samme dispenser):** `rx_seen` inneholder STATE med `open_for_delivery=True`, `verify.ok=true`. Pumpen er frigitt.

---

## ChatGPT-analyse – integrasjon og presiseringer

### 1. Plassering av respons-toleranse

**Anbefaling:** Legg fleksibilitet i operasjons-/service-laget (UNBLOCK), ikke i transport.

| Lag | Oppgave |
|-----|---------|
| **Transport/Communicator** | Returner alt som mottas – `rx_seen` + `rx_matched` (Python-modell) |
| **Operasjon (UNBLOCK)** | Eksplisitt policy: aksepter OK, aksepter STATE hvis `OPEN_FOR_DELIVERY`, ellers poll |

**Nåværende Kotlin:** `sendAndReceive()` returnerer kun første gyldige pakke. Det er tilstrekkelig hvis operasjonslaget tolker riktig – problemet er at PumpStateService avviser STATE, ikke at transport mangler rx_seen.

**Minste endring:** Ikke nødvendig med rx_seen i transport for UNBLOCK-fiksen. PumpStateService kan bruke den første pakken (STATE) direkte.

---

### 2. To separate tickets – ikke bland sammen

| Ticket | Tema | Beskrivelse |
|--------|------|-------------|
| **A: UNBLOCK** | Protokoll/semantikk | STATE er gyldig bekreftelse (open_for_delivery). Fikses i PumpStateService. |
| **B: Watchdog** | Drift/overvåkning | "Ingen RX i 60s" kan være normalt i IDLE. Eget driftstema. |

UNBLOCK-feilen kan fikses uten å røre watchdogen. Watchdog-fiksen er uavhengig.

---

### 3. Feil bitmasker = sikkerhetsrisiko

Feil mapping i service er ikke bare tech debt:

- Hvis 0x08 brukes som "authorized/open" i stedet for 0x02, kan pumpen få falsk grønt lys i feil tilstand.
- **Konsekvens:** Eksempel: tro at pumpen er frigitt når den ikke er det.

**Anbefaling:** Fjern de feilaktige maskene i PumpStateService før felt. Bruk `StatusBitMasks` fra core.

---

### 4. Watchdog – ikke aggressiv 5s IDLE-polling

**Diagnose:** Bussen kan være stille i IDLE – det er forventet.

**Løsning:** Unngå fast 5s heartbeat i IDLE. Bruk heller:

- **Adaptiv heartbeat:** Lav frekvens i IDLE (15–30s), høyere frekvens i autorisasjonsvindu/etter UNBLOCK/under fylling.
- **Alternativ:** Watchdog slack/disabled i IDLE; kun aktiv når vi forventer trafikk.

---

## Implementeringsplan – Kotlin mot Python

### Fase 1: UNBLOCK-fiks (Ticket A)

#### 1.1 PumpStateService.unblock() – aksepter STATE som suksess

**Fil:** `lpg-ehl-service/.../PumpStateService.kt`

**Nåværende (linje 222–226):**
```kotlin
if (unblockResponse.command != EhlCommand.OK) {
    return Result.failure(IllegalStateException("UNBLOCK rejected by dispenser: ${unblockResponse.command}"))
}
```

**Ny logikk (Python-paritet):**
```kotlin
when (unblockResponse.command) {
    EhlCommand.OK -> { /* Suksess – fortsett til STATE-verifisering */ }
    EhlCommand.STATE -> {
        if (unblockResponse.data.isNotEmpty()) {
            val sb = unblockResponse.data[0].toInt() and 0xFF
            if ((sb and StatusBitMasks.OPEN_FOR_DELIVERY) != 0) {
                // STATE med open_for_delivery = SUCCESS (som Python)
                // Hopp til transaksjonsopprettelse
            } else {
                return Result.failure(IllegalStateException("UNBLOCK: STATE mangler open_for_delivery"))
            }
        } else {
            return Result.failure(IllegalStateException("UNBLOCK: STATE uten data"))
        }
    }
    else -> return Result.failure(IllegalStateException("UNBLOCK rejected: ${unblockResponse.command}"))
}
```

#### 1.2 Erstatt PumpStateService STATE-masker med StatusBitMasks

**Fjern:** `STATE_FLAG_AUTHORIZED`, `STATE_FLAG_PUMPING`, `STATE_FLAG_TRANSACTION_COMPLETE`, `STATE_FLAG_NOZZLE_LIFTED`

**Bruk:** `StatusBitMasks.OPEN_FOR_DELIVERY`, `StatusBitMasks.START_BUTTON_PRESSED`, `StatusBitMasks.AUTOMODE`, `StatusBitMasks.ERROR_FLAG`

**Fil:** `PumpStateService.kt` linje 151–156 og alle referanser.

#### 1.3 verifyEffectivelyOpen() – Python _verify_open_for_delivery

Hvis UNBLOCK-respons mangler STATE med open_for_delivery, poll STATE til ønsket tilstand (som Python):

```kotlin
private fun verifyOpenForDelivery(address: Int, desired: Boolean, timeoutMs: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val statePacket = EhlPacket(address, EhlCommand.STATE, ByteArray(0))
        val response = runBlocking { ehlCommunicator.sendAndReceive(statePacket, 800) }
        if (response.command == EhlCommand.STATE && response.data.isNotEmpty()) {
            val sb = response.data[0].toInt() and 0xFF
            if ((sb and StatusBitMasks.OPEN_FOR_DELIVERY) != 0 == desired) return true
        }
        Thread.sleep(200)  // verify_interval_ms
    }
    return false
}
```

**Bruk:** Hvis UNBLOCK returnerer OK eller annen uklar respons, kall `verifyOpenForDelivery(address, true, 2500)` før suksess.

---

### Fase 2: Retry og buffer (valgfritt)

**Vurder:** Ikke clear buffer før STATE er tolket. Ved timeout: sjekk om buffer allerede inneholder gyldig STATE før retry.

**Fil:** `EhlCommunicator.kt` linje 126–128. Alternativ: Tolke buffer før `clearBuffer()` og returnere hvis gyldig respons finnes.

---

### Fase 3: Watchdog (Ticket B)

**Separert fra UNBLOCK.** Foreslåtte endringer:

1. **Adaptiv timeout:** Øk watchdog timeout i IDLE (f.eks. 90–120s), eller slå av i ren IDLE.
2. **Health-check:** Ved health-check, send LINETEST/STATE for å generere RX hvis bussen er stille.
3. **Konfigurerbart:** `watchdogTimeoutMs` og "slack in IDLE" som konfig.

---

### Sjekkliste før felt

- [ ] PumpStateService aksepterer STATE med open_for_delivery som UNBLOCK-suksess
- [ ] PumpStateService bruker StatusBitMasks (ingen egne feil masker)
- [ ] verifyOpenForDelivery brukes ved uklar respons
- [ ] EhlPacketFormatter: 0x5A vises som "open_for_delivery, automode" (allerede korrekt)
- [ ] Test mot emulator med STATE=0x5A etter UNBLOCK
- [ ] Test mot fysisk dispenser

---

## Original logg (URORT)

Ingenting under denne linjen er endret.

```
root@debian:/home/thomas# ./start-lpg-ehl.sh

.   ____          _            __ _ _
/\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v3.2.1)

10:37:37.209 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Starting LpgEhlApiApplicationKt using Java 21.0.10 with PID 572 (/home/thomas/release/lpg-ehl-webapp.jar started by root in /home/thomas)
10:37:37.231 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - The following 1 profile is active: "field"
Database is up to date, no changesets to execute
10:38:46.888 WARN  [-] o.h.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
10:39:04.899 INFO  [-] n.c.l.a.c.CommunicationConfig -
10:39:04.905 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
10:39:04.909 INFO  [-] n.c.l.a.c.CommunicationConfig -   EHL KOMMUNIKASJON: 🏭 FIELD MODE
10:39:04.914 INFO  [-] n.c.l.a.c.CommunicationConfig -   Active profiles: [field]
10:39:04.919 INFO  [-] n.c.l.a.c.CommunicationConfig - ═══════════════════════════════════════════════════════════
10:39:04.923 INFO  [-] n.c.l.a.c.CommunicationConfig -
10:39:22.012 INFO  [-] n.c.l.a.c.TransportConfiguration -
10:39:22.014 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
10:39:22.018 INFO  [-] n.c.l.a.c.TransportConfiguration -   🏭 FIELD MODE
10:39:22.020 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
10:39:22.022 INFO  [-] n.c.l.a.c.TransportConfiguration -   Transport:   SerialPortManager (with watchdog)
10:39:22.024 INFO  [-] n.c.l.a.c.TransportConfiguration -   Serial Port: /dev/ttyS3
10:39:22.026 INFO  [-] n.c.l.a.c.TransportConfiguration -   Baud Rate:   9600
10:39:22.029 INFO  [-] n.c.l.a.c.TransportConfiguration -   Data Bits:   8
10:39:22.031 INFO  [-] n.c.l.a.c.TransportConfiguration -   Parity:      NONE
10:39:22.033 INFO  [-] n.c.l.a.c.TransportConfiguration -   Stop Bits:   1
10:39:22.035 INFO  [-] n.c.l.a.c.TransportConfiguration -   Protocol:    EHL over RS-485
10:39:22.037 INFO  [-] n.c.l.a.c.TransportConfiguration -   ──────────────────────────────────────────────────────────
10:39:22.039 INFO  [-] n.c.l.a.c.TransportConfiguration -   ⚠️  Communicating with REAL HARDWARE
10:39:22.041 INFO  [-] n.c.l.a.c.TransportConfiguration - ════════════════════════════════════════════════════════════
10:39:22.043 INFO  [-] n.c.l.a.c.TransportConfiguration -
10:39:22.093 INFO  [-] n.c.l.c.SerialPortManager - Hardware watchdog enabled for /dev/ttyS3 (timeout: 60000ms)
10:39:22.099 INFO  [-] n.c.l.a.c.TransportConfiguration - 🐕 Hardware watchdog enabled
10:39:22.268 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlCommunicator with SerialPortManager
10:39:22.270 INFO  [-] n.c.l.a.c.TransportConfiguration - Raw protocol logging: ENABLED
10:39:22.343 INFO  [-] n.c.l.a.c.TransportConfiguration - 🔄 Retry config: maxRetries=3, initialDelay=100ms, maxDelay=2000ms, backoff=2.0
10:39:22.404 INFO  [-] n.c.l.c.SerialPortManager - Opening serial port: /dev/ttyS3
10:39:22.550 INFO  [-] n.c.l.c.SerialPortManager - Serial port /dev/ttyS3 opened successfully: SerialPortConfig(port=/dev/ttyS3, baud=9600, bits=8, stop=1, parity=0, readTimeout=3000ms, writeTimeout=1000ms)
10:39:22.557 INFO  [-] n.c.l.a.c.TransportConfiguration - ✅ Transport connected successfully
10:39:26.244 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10:39:26.247 INFO  [-] n.c.l.s.p.PumpStateService - 🌟 OPPSTART: Initialiserer pris...
10:39:27.737 INFO  [-] n.c.l.s.p.PumpStateService - 🏷️ STARTUP: Gjenopprettet pris 15.9 kr/L fra database
10:39:27.740 INFO  [-] n.c.l.s.p.PumpStateService -    Satt av: system
10:39:27.741 INFO  [-] n.c.l.s.p.PumpStateService -    Gyldig fra: 2026-02-11T07:49:16.571567
10:39:27.744 INFO  [-] n.c.l.s.p.PumpStateService - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10:39:37.981 INFO  [-] n.c.l.a.c.TransportConfiguration - Creating EhlOperationsService
10:39:40.354 WARN  [-] o.s.b.a.o.j.JpaBaseConfiguration$JpaWebConfiguration - spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
10:39:40.937 WARN  [-] o.s.b.a.s.s.UserDetailsServiceAutoConfiguration -

Using generated security password: bb98d1e7-c02c-4e9d-833d-db7e4f74c3e7

This generated password is for development use only. Your security configuration must be updated before running your application in production.

10:39:57.543 INFO  [-] org.xnio - XNIO version 3.8.8.Final
10:39:57.643 INFO  [-] org.xnio.nio - XNIO NIO Implementation Version 3.8.8.Final
10:39:58.859 INFO  [-] org.jboss.threads - JBoss Threads version 3.5.0.Final
10:39:59.497 INFO  [-] n.c.l.a.LpgEhlApiApplicationKt - Started LpgEhlApiApplicationKt in 152.83 seconds (process running for 161.787)
10:40:01.535 INFO  [-] n.c.l.a.s.WebAppPollingService - 🚀 WebApp polling service started - UI live updates enabled
10:40:59.518 ERROR [-] n.c.l.c.SerialPortManager - ⚠️ WATCHDOG TIMEOUT: No data received from /dev/ttyS3 for 97418ms (threshold: 60000ms). Connection may be dead (USB unplugged/driver hang).
10:40:59.539 ERROR [-] n.c.l.s.s.HardwareWatchdogService - ❌ Watchdog health check failed (consecutive failures: 1). Time since last data: 97444ms
10:40:59.541 WARN  [-] n.c.l.s.s.HardwareWatchdogService - 🔧 Attempting automatic reconnection (attempt #1)...
10:40:59.543 WARN  [-] n.c.l.c.SerialPortManager - 🔄 Attempting reconnect to /dev/ttyS3...
10:40:59.546 INFO  [-] n.c.l.c.SerialPortManager - Closing serial port /dev/ttyS3
10:40:59.550 INFO  [-] n.c.l.c.SerialPortManager - Serial port /dev/ttyS3 closed
10:40:59.553 INFO  [-] n.c.l.c.SerialPortManager - ⏳ Waiting 5000ms for hardware reset...
10:41:04.556 INFO  [-] n.c.l.c.SerialPortManager - 🔌 Reconnecting to /dev/ttyS3...
10:41:04.559 INFO  [-] n.c.l.c.SerialPortManager - Opening serial port: /dev/ttyS3
10:41:04.563 INFO  [-] n.c.l.c.SerialPortManager - Serial port /dev/ttyS3 opened successfully: SerialPortConfig(port=/dev/ttyS3, baud=9600, bits=8, stop=1, parity=0, readTimeout=3000ms, writeTimeout=1000ms)
10:41:04.565 INFO  [-] n.c.l.c.SerialPortManager - ✅ Reconnect successful to /dev/ttyS3
10:41:04.568 INFO  [-] n.c.l.s.s.HardwareWatchdogService - 🎉 Automatic reconnection successful (attempt #1)
10:42:04.573 ERROR [-] n.c.l.c.SerialPortManager - ⚠️ WATCHDOG TIMEOUT: No data received from /dev/ttyS3 for 60005ms (threshold: 60000ms). Connection may be dead (USB unplugged/driver hang).
10:42:04.576 ERROR [-] n.c.l.s.s.HardwareWatchdogService - ❌ Watchdog health check failed (consecutive failures: 1). Time since last data: 60009ms
10:42:04.577 WARN  [-] n.c.l.s.s.HardwareWatchdogService - 🔧 Attempting automatic reconnection (attempt #2)...
10:42:04.579 WARN  [-] n.c.l.c.SerialPortManager - 🔄 Attempting reconnect to /dev/ttyS3...
10:42:04.582 INFO  [-] n.c.l.c.SerialPortManager - Closing serial port /dev/ttyS3
10:42:04.584 INFO  [-] n.c.l.c.SerialPortManager - Serial port /dev/ttyS3 closed
10:42:04.586 INFO  [-] n.c.l.c.SerialPortManager - ⏳ Waiting 5000ms for hardware reset...
10:42:09.589 INFO  [-] n.c.l.c.SerialPortManager - 🔌 Reconnecting to /dev/ttyS3...
10:42:09.591 INFO  [-] n.c.l.c.SerialPortManager - Opening serial port: /dev/ttyS3
10:42:09.593 INFO  [-] n.c.l.c.SerialPortManager - Serial port /dev/ttyS3 opened successfully: SerialPortConfig(port=/dev/ttyS3, baud=9600, bits=8, stop=1, parity=0, readTimeout=3000ms, writeTimeout=1000ms)
10:42:09.595 INFO  [-] n.c.l.c.SerialPortManager - ✅ Reconnect successful to /dev/ttyS3
10:42:09.600 INFO  [-] n.c.l.s.s.HardwareWatchdogService - 🎉 Automatic reconnection successful (attempt #2)
10:42:47.805 INFO  [OPERATOR] n.c.l.a.c.PumpController - 🔓 FRI PUMPE: Unblock request for address 33
10:42:47.817 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10:42:47.821 INFO  [OPERATOR] n.c.lpg.protocol - ⛽ FRI PUMPE - Sender UNBLOCK til dispenser #33
10:42:47.939 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 21 77 40 36
10:42:47.957 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 21 77 40 36] -> UNBLOCK
10:42:47.983 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 21 77 40 36
10:42:48.015 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #33 | UNBLOCK (Start delivery mode) | Bytes: [10 06 21 77 40 36] | Checksum: 0x40 ✓
10:42:48.064 DEBUG [OPERATOR] n.c.l.c.SerialPortManager - Wrote 6 bytes to /dev/ttyS3: 10 06 21 77 40 36
10:42:50.905 WARN  [OPERATOR] n.c.l.c.EhlCommunicator - ⏱️ Timeout on UNBLOCK to addr 33 (attempt 1/4), retrying in 100ms...
10:42:51.010 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - Receive buffer cleared
10:42:51.035 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 🔄 RETRY attempt 1/3 for UNBLOCK to addr 33
10:42:51.041 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 21 77 40 36
10:42:51.043 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 TX HEX: [10 06 21 77 40 36] -> UNBLOCK
10:42:51.045 DEBUG [OPERATOR] n.c.l.p.EhlCodec - Encoded: 10 06 21 77 40 36
10:42:51.052 DEBUG [OPERATOR] n.c.l.c.EhlCommunicator - 📤 SENDING → Dispenser #33 | UNBLOCK (Start delivery mode) | Bytes: [10 06 21 77 40 36] | Checksum: 0x40 ✓
10:42:51.071 DEBUG [OPERATOR] n.c.l.c.SerialPortManager - Wrote 6 bytes to /dev/ttyS3: 10 06 21 77 40 36
10:42:51.088 DEBUG [-] n.c.l.c.SerialPortManager - Read 7 bytes from /dev/ttyS3: 20 07 21 4B 5A 17 36
10:42:51.093 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX HEX: [20 07 21 4B 5A 17 36]
10:42:51.098 DEBUG [-] n.c.l.c.EhlCommunicator - 🟢 Buffer received data | Size: 7 bytes
10:42:51.117 DEBUG [-] n.c.l.p.EhlCodec - Decoded: EhlPacket(addr=33, cmd=STATE(75), data=[5A], chksum=27)
10:42:51.161 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RX PARSED: STATE from addr 33
10:42:51.171 DEBUG [-] n.c.l.p.EhlCodec - Encoded: 10 07 21 4B 5A 27 36
10:42:51.175 DEBUG [-] n.c.l.c.EhlCommunicator - 📥 RECEIVING ← Dispenser #33 | STATE (Give/take the calculator state) | State=0x5A (NOZZLE_LIFTED (Waiting for authorization)) | Bytes: [10 07 21 4B 5A 27 36] | Checksum: 0x27 ✓
10:42:51.187 INFO  [OPERATOR] n.c.lpg.protocol - 📥 UNBLOCK Respons: STATE
10:42:51.191 WARN  [OPERATOR] n.c.lpg.protocol - ⚠️ UNBLOCK avvist: Forventet OK, fikk STATE
10:42:51.194 INFO  [OPERATOR] n.c.lpg.protocol - ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10:42:51.198 WARN  [OPERATOR] n.c.l.a.c.PumpController - ❌ Unblock failed: UNBLOCK rejected by dispenser: STATE

alejandro@debian:~$ curl -sS -X POST http://localhost:8082/unblock
{
"error": "INTERNAL",
"message": "[Errno 13] Permission denied: '/dev/ttyS3'",
"ok": false
}
alejandro@debian:~$ curl -sS -X POST http://localhost:8082/unblock
{
"error": "INTERNAL",
"message": "[Errno 13] Permission denied: '/dev/ttyS3'",
"ok": false
}
alejandro@debian:~$ curl -sS -X POST http://localhost:8082/unblock
{
"send": {
"ok": false,
"rx_matched": null,
"rx_seen": [
{
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"tx": {
"addr": 33,
"cmd": "0x77",
"data_hex": "",
"frame_hex": "10 06 21 77 40 36"
}
},
"verify": {
"desired": true,
"ok": true,
"samples": [
{
"ok": true,
"rx_matched": {
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"rx_seen": [
{
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"state": {
"automode": true,
"bit0": false,
"bit4": true,
"bit5": false,
"bit6": true,
"bit7": false,
"bits": "01011010",
"open_for_delivery": true,
"raw": 90,
"startbutton_pressed": false
},
"tx": {
"addr": 33,
"cmd": "0x4B",
"data_hex": "",
"frame_hex": "10 06 21 4B 7C 36"
}
}
]
}
}
alejandro@debian:~$ curl -sS -X POST http://localhost:8082/unblock
{
"send": {
"ok": false,
"rx_matched": null,
"rx_seen": [
{
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"tx": {
"addr": 33,
"cmd": "0x77",
"data_hex": "",
"frame_hex": "10 06 21 77 40 36"
}
},
"verify": {
"desired": true,
"ok": true,
"samples": [
{
"ok": true,
"rx_matched": {
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"rx_seen": [
{
"addr": 33,
"checksum": "0x17",
"cmd": "0x4B",
"cmd_name": "STATE",
"data_hex": "5A",
"etx": "0x36",
"interpretation": "STATE raw=0x5A bits=01011010 open_for_delivery=True startbutton_pressed=False automode=True",
"len": 7,
"stx": "0x20"
}
],
"state": {
"automode": true,
"bit0": false,
"bit4": true,
"bit5": false,
"bit6": true,
"bit7": false,
"bits": "01011010",
"open_for_delivery": true,
"raw": 90,
"startbutton_pressed": false
},
"tx": {
"addr": 33,
"cmd": "0x4B",
"data_hex": "",
"frame_hex": "10 06 21 4B 7C 36"
}
}
]
}
}
alejandro@debian:~$[4:57 PM]alejandro@debian:~/test-python/python-server$ python3 server.py
2026-02-11 10:54:12 [INFO] server_start
2026-02-11 10:54:12 [INFO] cfg listen=0.0.0.0:8082 serial=/dev/ttyS3 baud=9600 addr=33
2026-02-11 10:54:12 [INFO] cfg defaults product_select_byte=0x30 price=15.90
2026-02-11 10:54:12 [INFO] listening http://0.0.0.0:8082
2026-02-11 10:54:16 [INFO] HTTP POST /unblock from=127.0.0.1 body={}
2026-02-11 10:54:16 [ERROR] HTTP_ERR /unblock: [Errno 13] Permission denied: '/dev/ttyS3'
^C2026-02-11 10:54:35 [INFO] server_stop keyboard_interrupt
alejandro@debian:~/test-python/python-server$ sudo python3 server.py
[sudo] password for alejandro:
2026-02-11 10:54:49 [INFO] server_start
2026-02-11 10:54:49 [INFO] cfg listen=0.0.0.0:8082 serial=/dev/ttyS3 baud=9600 addr=33
2026-02-11 10:54:49 [INFO] cfg defaults product_select_byte=0x30 price=15.90
2026-02-11 10:54:49 [INFO] listening http://0.0.0.0:8082
2026-02-11 10:54:52 [INFO] HTTP POST /unblock from=127.0.0.1 body={}
2026-02-11 10:54:52 [INFO] serial_open: path=/dev/ttyS3 baud=9600 addr=33
2026-02-11 10:54:52 [INFO] HTTP_TX addr=33 cmd=0x77 data= frame=10 06 21 77 40 36
2026-02-11 10:54:54 [INFO] HTTP_TX addr=33 cmd=0x4B data= frame=10 06 21 4B 7C 36
2026-02-11 10:55:18 [INFO] HTTP POST /unblock from=127.0.0.1 body={}
2026-02-11 10:55:19 [INFO] HTTP_TX addr=33 cmd=0x77 data= frame=10 06 21 77 40 36
2026-02-11 10:55:20 [INFO] HTTP_TX addr=33 cmd=0x4B data= frame=10 06 21 4B 7C 36
```
