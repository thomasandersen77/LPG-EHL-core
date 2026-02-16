# Legacy Kodesamling - Kuratert

Denne mappen inneholder de viktigste Visual Basic 6 og Python-filene fra `norgesgass_legacy` og `more_legacy` mappene.

## Struktur

### `/pumpekontroll`
Hovedapplikasjonen for pumpekontrollsystemet. Inneholder:
- **pumpcontrol.vbp** - Hovedprosjektfilen for VB6
- **pumpekontroll.frm** - Hovedform for pumpekontroll
- **server.frm** - Server-relatert funksjonalitet
- **fra_dispenser.bas** - Moduler for kommunikasjon med dispensere
- **defs.bas** - Definisjoner og konstanter
- **email.bas** - E-postfunksjonalitet
- Ulike forms for:
  - Dispenserstyring (dispensere.frm)
  - Tankinger (Tankinger_form.frm)
  - Rapporter (rapporter_form.frm)
  - EHL debug (ehldebug_form.frm)
  - Dataserver (dataserveronline.frm)
  - Administrasjon (administration.frm)
  - Firmainformasjon (Firmainfo.frm)
  - Stasjonskredittkort (Stasjonskredittkort_sok.frm)
- Class-moduler:
  - Transaction.cls
  - mjwPDF.cls (PDF-generering)

### `/EHL4x`
EHL4x-prosjektet - en variant av pumpekontrollsystemet.
- EHL4x.vbp - Prosjektfil
- pumpcontrol.vbp - Alternativ prosjektfil
- Tilhørende forms og moduler

### `/Dispenserklient`
Klientapplikasjon for dispenserstyring. Inneholder:
- **Dispenserkontroll.vbp** - Hovedprosjekt
- **Dispenserkontroll.frm** - Hovedform
- **Module1.bas** - Hjelpemoduler
- Forms for:
  - Kunderegister (Kundereg.frm, kunder.frm)
  - Tankinger (Tankinger_form.frm)
  - Uttaksrapporter (Uttaksrapport.frm)
  - Dispenserliter (displiter.frm)
  - Innstillinger (innstillinger.frm)
  - Kortskanning (kortscan_form.frm)
  - Omsetning (omsetning_form.frm)
  - Rapporter (rapporter_form.frm)
  - Stasjonskreditt (stasjonskreditt.frm, stasjonskreditt_rapport_form.frm)

### `/Report_timer`
Tjeneste/applikasjon for automatisk rapportgenerering:
- Report_timer.vbp
- Report_timer.frm

### `/Python`
Python-implementasjoner av EHL-protokollen:

#### `/Python/ehl_pumpekontroll_clone`
Python-implementasjon av pumpekontrollsystemet:
- **pumpekontroll_clone.py** - Hovedapplikasjon
- **ehl/** - EHL-protokoll bibliotek:
  - `protocol.py` - Protokollimplementasjon
  - `model.py` - Datamodeller
  - `stream_parser.py` - Parsing av datastrømmer
  - `poller.py` - Polling-logikk
  - `serial_client.py` - Seriell kommunikasjon
- **tests/** - Enhetstester
- **requirements.txt** - Python-avhengigheter
- **README.md** - Dokumentasjon

#### `/Python/pumpekontroll_src`
Original VB6-kildekode som Python-implementasjonen er basert på:
- pumpcontrol.vbp
- pumpekontroll.frm
- server.frm
- dispensere.frm
- defs.bas
- fra_dispenser.bas
- README.txt

## Dokumentasjon
- **de komplementert protokoll.rtf** - Protokolldokumentasjon
- **server.ini** - Serverkonfigurasjon
- **version.ver** - Versjonsinformasjon

## Statistikk
- **Totalt antall filer:** 81
- **VB6 source filer (.frm, .bas, .cls, .vbp):** ~50
- **Python filer (.py):** 8
- **Støttefiler:** Resten

## Kilde
Samlet fra:
- `/Users/tandersen/git/NorgesGass/lpg-ehl/norgesgass_legacy`
- `/Users/tandersen/git/NorgesGass/lpg-ehl/more_legacy`

## Formål
Denne samlingen representerer kjernen i det gamle systemet og er kurert for å:
1. Forstå forretningslogikken i det gamle systemet
2. Identifisere EHL-protokollimplementasjoner
3. Kartlegge funksjoner som må reimplementeres i ny løsning
4. Tjene som referanse under modernisering

## Neste Steg
1. Analysere EHL-protokollimplementasjoner (både VB6 og Python)
2. Dokumentere forretningsregler fra VB6-koden
3. Identifisere gjenbrukbare konsepter for ny implementasjon
4. Kartlegge databaseskjema fra VB6-kode
