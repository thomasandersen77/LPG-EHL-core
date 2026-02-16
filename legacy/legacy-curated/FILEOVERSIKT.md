# Fileoversikt - Legacy Kuratert Samling

## Statistikk per kategori

### Visual Basic 6 Filer
```
Prosjektfiler (.vbp):      6
Forms (.frm):             43
Moduler (.bas):            7
Classes (.cls):            3
Form binær data (.frx):   ~15
```

### Python Filer
```
Python source (.py):       8
Python cache (__pycache__)
```

### Dokumentasjon og Konfigurasjon
```
README filer:              3
Konfigurasjon (.ini):      1
Versjonsfiler (.ver):      1
RTF-dokumentasjon:         1
Requirements:              1
```

## Detaljert Filliste

### pumpekontroll/ (Hovedapplikasjon)
```
VB6 Prosjekt:
  pumpcontrol.vbp
  
Forms:
  pumpekontroll.frm/.frx
  server.frm/.frx
  dispensere.frm/.frx
  Tankinger_form.frm
  rapporter_form.frm
  ehldebug_form.frm
  dataserveronline.frm
  administration.frm
  Firmainfo.frm
  Stasjonskredittkort_sok.frm
  frmAbout.frm
  frmlogs.frm
  TEXT.frm
  testform.frm
  
Moduler:
  fra_dispenser.bas
  defs.bas
  email.bas
  
Classes:
  Transaction.cls
  mjwPDF.cls
  Class1.cls
```

### EHL4x/ (EHL4x Variant)
```
VB6 Prosjekter:
  EHL4x.vbp
  pumpcontrol.vbp
  
Forms:
  TEXT.frm
  pumpekontroll.frm
  server.frm
  
Moduler:
  fra_dispenser.bas
```

### Dispenserklient/ (Klientapplikasjon)
```
VB6 Prosjekt:
  Dispenserkontroll.vbp
  
Forms:
  Dispenserkontroll.frm
  Kundereg.frm
  Tankinger_form.frm
  Uttaksrapport.frm
  displiter.frm
  innstillinger.frm
  kortscan_form.frm
  kunder.frm
  omsetning_form.frm
  rapporter_form.frm
  stasjonskreditt.frm
  stasjonskreditt_rapport_form.frm
  
Moduler:
  Module1.bas
```

### Report_timer/ (Rapport Tjeneste)
```
VB6 Prosjekt:
  Report_timer.vbp
  
Forms:
  Report_timer.frm
```

### Python/ehl_pumpekontroll_clone/ (Python Implementasjon)
```
Hovedfil:
  pumpekontroll_clone.py
  
EHL Bibliotek (ehl/):
  __init__.py
  protocol.py
  model.py
  stream_parser.py
  poller.py
  serial_client.py
  
Tester (tests/):
  test_protocol.py
  
Konfigurasjon:
  requirements.txt
  README.md
```

### Python/pumpekontroll_src/ (VB6 Referanse)
```
VB6 Prosjekt:
  pumpcontrol.vbp
  pumpcontrol.vbw
  
Forms:
  pumpekontroll.frm/.frx
  server.frm
  dispensere.frm/.frx
  
Moduler:
  defs.bas
  fra_dispenser.bas
  
Dokumentasjon:
  README.txt
```

### Rot-nivå
```
Dokumentasjon:
  README.md (denne samlingen)
  FILEOVERSIKT.md (denne filen)
  de komplementert protokoll.rtf
  
Konfigurasjon:
  server.ini
  version.ver
```

## Nøkkelfiler for Analyse

### EHL Protokoll Implementasjon
1. **VB6:** `pumpekontroll/fra_dispenser.bas`
2. **Python:** `Python/ehl_pumpekontroll_clone/ehl/protocol.py`
3. **Dokumentasjon:** `de komplementert protokoll.rtf`

### Hovedapplikasjoner
1. **Pumpekontroll:** `pumpekontroll/pumpcontrol.vbp`
2. **Dispenserklient:** `Dispenserklient/Dispenserkontroll.vbp`
3. **EHL4x:** `EHL4x/EHL4x.vbp`
4. **Python Clone:** `Python/ehl_pumpekontroll_clone/pumpekontroll_clone.py`

### Forretningslogikk
1. **Definisjoner:** `pumpekontroll/defs.bas`
2. **Transaksjoner:** `pumpekontroll/Transaction.cls`
3. **Serverlogikk:** `pumpekontroll/server.frm`
4. **E-post:** `pumpekontroll/email.bas`
5. **PDF:** `pumpekontroll/mjwPDF.cls`

### Brukergrensesnitt
1. **Dispenserkontroll:** `Dispenserklient/Dispenserkontroll.frm`
2. **Kunderegister:** `Dispenserklient/Kundereg.frm`
3. **Rapporter:** Multiple `rapporter_form.frm`
4. **Tankinger:** Multiple `Tankinger_form.frm`
5. **Stasjonskreditt:** `Dispenserklient/stasjonskreditt.frm`

## Anbefalinger for Videre Arbeid

### Prioritet 1: Protokollforståelse
- Les `de komplementert protokoll.rtf`
- Sammenlign VB6 og Python protokollimplementasjoner
- Dokumenter protokollspesifikasjon

### Prioritet 2: Datamodell
- Analyser `Transaction.cls` og `model.py`
- Kartlegg databasetilgang i VB6-kode
- Identifiser nødvendige entiteter

### Prioritet 3: Forretningslogikk
- Studer forms for forretningsflyt
- Identifiser valideringsregler
- Kartlegg integrasjonspunkter

### Prioritet 4: Moderniseringstiltak
- Vurder gjenbruk av Python-implementasjon
- Plan for ny arkitektur
- Identifiser API-grenser
