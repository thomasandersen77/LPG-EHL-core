## Verifisering mot ekte ARK-3600 (kort “første kveld”-oppskrift)

### Mål (30–60 min)
Bekrefte at vi leser/skriver riktig over serial/RS-485 og at vi fanger:
1) **RX/TX flyt** (vi får svar på polling/kommando)
2) **State-endringer** (IDLE → AUTHORISED → FUELING/FINISHED eller ERROR)
3) **Feil/timeout/re-sync** (støy/partial frames håndteres)

---

### Forberedelser
- Koble ARK-3600 + terminal til nett (switch om nødvendig).
- Finn riktig COM-port (Windows) eller `/dev/tty*` (Linux).
- Notér: baud/parity/stopbits hvis kjent. Hvis ikke: la det være konfigurerbart og logg hva som brukes.

---

### Kjøring (anbefalt rekkefølge)
1) **Start core/api med debug-logging**
    - Logg alltid: `lastRxAt`, `lastTxAt`, `state`, `lastFault`, `port`, `serialConfig`.
2) **Kjør kun “safe” kommando først**
    - `LINE_TEST` / `GET_STATUS` / `ERROR_QUERY` (hva dere allerede har i VB6 tester).
3) **Kjør en kontrollert sekvens**
    - Set price → Authorize → (simuler nozzle/fueling hvis mulig) → Stop/Finish → Persist

---

### Det du må logge (minstekrav)
- **Hver pakke**: retning (TX/RX), STX, addr, cmd, length, checksum OK/FAIL, hex-dump ved feil.
- **State transitions**: `oldState -> newState` + “reason” (cmd/status/timeout/fault).
- **Timeouts**: tid siden siste RX, antall retries, re-sync result (f.eks. “found STX at index …”).

---

### 3 “must pass” sjekker
1) **Polling svarer**: minst 10 stabile runder uten checksum/format-feil.
2) **Støy-toleranse**: trekk kabel/lag kort avbrudd → systemet re-synker og fortsetter (eller går til ERROR med tydelig årsak).
3) **Fault-håndtering**: hvis dere kan trigge eller observere `E-xx`, skal:
    - state -> ERROR umiddelbart (CRITICAL)
    - `/admin/ehl/diagnostics` viser `lastFault` + recommendedAction

---

### Praktisk “scope guard”
Hvis punkt (1) er stabilt men (2) feiler:
- Prioritér **codec/buffer re-sync** før UI og businesslogikk.
  Hvis (1) feiler:
- Prioritér **serialConfig/RS-485 wiring/terminering** og logging som viser nøyaktig hva som kommer på linja.

---