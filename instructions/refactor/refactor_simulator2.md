# Oppgave: Fiks PLS-simulator slik at den matcher Python-fasit for UNBLOCK/STATE, uten å endre produksjonskode

## Bakgrunn (observasjon fra logger)
Webapp (FIELD) sender UNBLOCK og poller STATE til `open_for_delivery`-bit `0x02` observeres innen 6s.
PLS-simulator kjører Profile=LAB og svarer STATE med data `0x01` etter UNBLOCK, så `0x02` kommer aldri og UNBLOCK feiler.

## Mål
1) PLS-simulator (serialport-sim) skal kunne brukes som erstatning for fysisk dispenser, og MUST være kompatibel med Python-fasit:
  - Etter UNBLOCK skal STATE-poll returnere statusbyte som inkluderer `open_for_delivery` bit `0x02` (så lenge ikke fault injection sier noe annet).
2) Dette må fungere både når simulator kjører LAB og FIELD profil (eller via et nytt knob som tvinger "python-aligned semantics").
3) Ikke endre produksjonskode (service/webapp/core/transport). Kun simulatorene.

## Scope
- Endre kun `lpg-ehl-serialport-sim` (PLS simulator).
- I tillegg: hvis det finnes en egen LAB-emulator i `lpg-ehl-emulator` som brukes i lab-mode, sørg for at dens LAB-strategi også bruker samme statusbit-semantikk (slik at LAB ikke avviker fra felt-fasit på open_for_delivery).

## Krav: Statusbit-semantikk må alignes med Kotlin core / Python
Bruk følgende bitmasker (single source of truth i simulatoren):
- `OPEN_FOR_DELIVERY = 0x02`
- `STARTBUTTON = 0x04` (settes når GUI "Start"/rød knapp trykkes)
- `AUTOMODE = 0x08` (hvis brukt; hvis ikke, la den være 0 i enkel modus)
- `ERROR = 0x80` (om simulator har error states)

## Konkret endring i PLS simulator (viktigst)
Finn hvor simulatoren bygger STATE statusbyte (f.eks. `buildStatusByte()` eller tilsvarende).
- I state `AUTHORIZED`:
  - SETT alltid `OPEN_FOR_DELIVERY (0x02)` etter mottatt UNBLOCK (uavhengig av LAB/FIELD profil),
  - IKKE sett `STARTBUTTON (0x04)` før GUI-knappen trykkes.
- I state `IDLE`:
  - `OPEN_FOR_DELIVERY` skal normalt være 0.
- I state `PUMPING`:
  - `OPEN_FOR_DELIVERY` kan fortsatt være satt (dispenser er åpen), og `STARTBUTTON` kan være 0/1 avhengig av hvordan feltet faktisk gjør det.
  - Viktigst: ikke bryt UNBLOCK-verifikasjonen (0x02 må være true før pumping).

## Profil-policy (LAB vs FIELD)
- Behold LAB-profilen som "snill" når det gjelder delays/chunking/noise,
  men den må IKKE ha annen semantikk for statusbits enn FIELD.
- Hvis dere vil beholde gammel LAB-bits for interne tester: legg det bak et eksplisitt flagg, default = python-aligned.
  Eksempel:
  - `sim.protocolSemantics=PYTHON_ALIGNED` (default)
  - alternativ `LEGACY_LAB_BITS` kun hvis noen eksplisitt trenger det.

## Logging / verifikasjon
Legg til en tydelig logglinje ved UNBLOCK:
- "UNBLOCK received -> entering AUTHORIZED, stateByte=0x?? (bits ...)"
  Og ved hver STATE response:
- "STATE reply: 0x?? open_for_delivery=... startbutton=... automode=..."

## Tester
Oppdater/legg til en test som simulerer:
- send UNBLOCK
- send STATE x N
- assert at minst én STATE innen 6s inneholder `0x02`

## Ikke gjør
- Ikke rør Kotlin produksjonskode i service/webapp/transport/core.
- Ikke endre REST API.
- Ikke endre webapp GUI.

## Leveranse
- Commit i simulator-modulen som gjør at webapp sin UNBLOCK ikke feiler når simulatoren kjører i LAB-profil.
- Kort notat i README: "LAB profile uses python-aligned status semantics; differences are noise/delay only."