# Oppgave: Fiks PLS-simulatoren slik at UNBLOCK→STATE oppfører seg identisk med Python-fasit

## Bakgrunn (observasjon fra logger)
Webapp verifiserer UNBLOCK ved å poll'e STATE til `open_for_delivery`-bit (0x02) blir observert.
Vi ser to ulike simulator-oppførsler:
- Feil: Etter UNBLOCK svarer simulator STATE=0x01 (open_for_delivery=false) gjentatte ganger → UNBLOCK timeout.
- Riktig: Etter UNBLOCK svarer simulator STATE=0x03 (bits=00000011) og webapp logger "UNBLOCK verified" og fortsetter.

**Fasit-krav:** Etter UNBLOCK skal simulatoren innen kort tid (typisk umiddelbart) returnere en STATE-byte som inkluderer bit 0x02 (open_for_delivery). Minimum er at `STATE & 0x02 != 0`.

## Omfang
- Endre kun PLS-simulatoren (lpg-ehl-serialport-sim).
- I tillegg: sørg for at LAB-mode-strategien også bruker samme STATE-bit-encoding som PLS-simulatoren.
  (LAB-mode ligger i emulator-modulen; IKKE endre produksjonskode.)

## Krav 1: Korrekt STATE-bit encoding etter UNBLOCK
Finn hvor simulatoren bygger STATE-response (cmd=0x4B) og hvor den setter intern tilstand ved UNBLOCK (cmd=0x77).

Implementer en entydig mapping:
- Når intern state er "AUTHORIZED" / "READY_TO_PUMP" / "OPEN_FOR_DELIVERY"-ekvivalent:
    - STATE byte MÅ ha `open_for_delivery` (0x02) satt.
    - For dagens semantikk betyr dette typisk STATE=0x03 (0x01 + 0x02), men det viktigste er bit 0x02.
- Når pumpen er IDLE uten autorisasjon:
    - STATE byte skal IKKE ha 0x02 satt.

Dette må gjelde uansett GUI (GUI kan fortsatt styre startbutton/pumping, men UNBLOCK-verifisering skal ikke avhenge av GUI-klikk).

## Krav 2: Startbutton / pumping (GUI)
Behold GUI-mekanismen:
- Når GUI "Start" trykkes kan simulatoren sette startbutton-bit (0x04) i STATE (evt. kortvarig), og gå videre til pumping.
- Men: UNBLOCK->open_for_delivery skal komme før GUI-start, slik at webappen kommer til "klar til fylling".

## Krav 3: Konsistens (ingen tilfeldig mismatch)
Fjern/endre logikk som kan gjøre at simulatoren fortsatt returnerer 0x01 etter UNBLOCK.
Hvis dere har tidsvinduer, race, eller "venter på GUI før open_for_delivery": slå dette av eller gjør det konfigurerbart, default = Python-fasit.

## Krav 4: Synlig logging i simulator
Legg til tydelig logg ved STATE-response:
- Intern state
- Byte som sendes (hex)
- Hvilke bits som er satt (open_for_delivery/startbutton/automode/error)

Legg også til logg ved UNBLOCK som viser:
- Før/etter internal state
- Hvilke flags som nå er aktive (inkl. open_for_delivery=true)

## Krav 5: LAB-mode / emulator
Finn tilsvarende STATE-encoding i emulator-modulen (LAB-mode). Sørg for at samme encoding-regler gjelder der:
- UNBLOCK skal føre til STATE med 0x02 bit.
- Ikke la LAB-mode kreve GUI-klikk for at open_for_delivery settes.

## Akseptansekriterier (må kunne verifiseres i logg)
1) Når webapp sender UNBLOCK → første eller tidligste STATE etterpå skal ha data byte hvor `(byte & 0x02) != 0`.
2) Webapp skal logge "UNBLOCK verified" og gå videre til "klar til fylling" uten timeout.
3) Dette skal være stabilt over mange kjøringer (ingen flakiness).

## Tips til hvor buggen sannsynligvis er
Det ser ut som simulatoren logger "AUTHORIZED" internt, men STATE-response bygges likevel som IDLE (0x01).
Sjekk at STATE-response bruker nåværende state/flags og ikke et annet felt (f.eks. "displayState" vs "protocolState"), eller at state-byten ikke blir clobber'et av en default.