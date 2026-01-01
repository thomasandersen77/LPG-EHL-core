# ECR Testing Scripts

Python-skript for testing og reverse-engineering av Ingenico Self/4000 ECR-terminal.

## Oversikt

Disse skriptene ble utviklet for å teste kommunikasjon med betalingsterminalen over TCP/IP på port 8009. De representerer utviklingsprosessen fra tidlige eksperimenter til den endelige fungerende protokollen.

---

## 📋 Skriptoversikt

### Anbefalte Skript (Bruk disse)

#### `ecr_server_v22_golden_format.py` ⭐ **ANBEFALT**
**Hva:** Det endelige, fungerende scriptet basert på bekreftet protokoll.  
**Bruk:** Testing av betalingstransaksjoner med "Golden Format" kommando.  
**Kjøring:**
```bash
python3 ecr_server_v22_golden_format.py
```
**Funksjonalitet:**
- Lytter på port 8009
- Bruker bekreftet Viking-protokoll: `P;10;1;200;0`
- Håndterer heartbeats automatisk
- Tydelige instruksjoner for korttiming
- 120 sekunders lyttevindu
- Detaljert responanalyse

**Utganger:**
- `A000ECR Timeout` = Format korrekt, kort ikke lest i tide
- `[00]` = Kommando mottatt, men ingen transaksjon startet
- `A000...` (uten FEIL) = Potensiell suksess

---

#### `ecr_format_test.py` 🔬
**Hva:** Tester 11 forskjellige kommandoformat for å finne gyldige varianter.  
**Bruk:** Identifisere hvilke kommandoformat terminalen aksepterer.  
**Kjøring:**
```bash
python3 ecr_format_test.py
```
**Funksjonalitet:**
- Tester Viking og Verifone formater
- LOGON varianter (6 forskjellige)
- KJØP varianter (5 forskjellige)
- Pre-Auth testing
- Markerer gyldige format med ✅

**Resultater:** Se `ECR_INTEGRATION_REPORT.md` seksjon 3.3

---

#### `ecr_protocol_tester.py` 🧪
**Hva:** Multi-protokoll tester for å identifisere korrekt protokoll.  
**Bruk:** Avgjøre om terminal bruker Viking eller Verifone protokoll.  
**Kjøring:**
```bash
python3 ecr_protocol_tester.py
```
**Funksjonalitet:**
- Tester Viking format (`P;...`)
- Tester Verifone format (`[...]`)
- Venter på stabile heartbeats før testing
- Automatisk sekvensiell testing

**Funn:** Terminal er hybrid - aksepterer Viking input, sender Ingenico output

---

### Diagnostikk og Debugging

#### `ecr_passive_listener.py` 👂
**Hva:** Passiv lytter som kun mottar data fra terminal.  
**Bruk:** Undersøke om terminal sender spontane meldinger.  
**Kjøring:**
```bash
python3 ecr_passive_listener.py
```
**Funksjonalitet:**
- Logger all trafikk uten å sende kommandoer
- Viser rådata (hex og tekst)
- Verifiserer heartbeat-mekanisme

**Konklusjon:** Terminal sender kun heartbeats, forventer at kasse initierer.

---

#### `ecr_dialog_handler.py` 💬
**Hva:** Spesialisert håndtering av `D!000` dialog-responser.  
**Bruk:** Debugging av dialogflyt og tilstandsmaskin.  
**Kjøring:**
```bash
python3 ecr_dialog_handler.py
```
**Funksjonalitet:**
- Detaljert logging av alle responser
- Håndterer `D!000` (dialog/venter)
- Debug-output med tidsstempler
- Leser multiple responser kontinuerlig

---

#### `find_terminal.py` 🔍
**Hva:** Scanner nettverket for terminaler på port 8009.  
**Bruk:** Finne terminalens IP-adresse automatisk.  
**Kjøring:**
```bash
python3 find_terminal.py
```
**Funksjonalitet:**
- Skanner 192.168.0.0/24 nettverk
- Prøver TCP-forbindelse på port 8009
- Lister alle responsive enheter

---

#### `test_terminal.py` 🧩
**Hva:** Enkel connectivity-test.  
**Bruk:** Verifisere at terminal er tilgjengelig.  
**Kjøring:**
```bash
python3 test_terminal.py
```

---

### Historiske Versjoner (Utvikling)

Disse skriptene viser utviklingsprosessen og er bevart for dokumentasjon.

#### `ecr_server_v18_final_reservation.py`
- Forsøk på reservasjon med Verifone brackets
- Konklusjon: `A000FEIL I` - ikke støttet

#### `ecr_viking_working.py`
- Viking protokoll implementasjon med LOGON + KJØP
- Første fungerende kommunikasjon

#### `ecr_with_logon.py`
- Test av LOGON sekvens før KJØP
- Konklusjon: LOGON ikke nødvendig

#### `ecr_working_final.py`
- Iterasjon med bekreftet format
- Session tracking

#### `ecr_final.py`
- Forenklet versjon, direkte KJØP

#### `ecr_last_attempt.py`
- Komplett implementasjon før v22
- 120s lyttevindu, detaljert logging

#### `ecr_server.py` til `ecr_server_v13_fix.py`
- Tidlige versjoner (v3-v13)
- Eksperimenter med handshake, framing, ping-pong
- Verifone og Viking testing

#### `ecr_bracket_purchase_try.py`
- Test av bracket-format kjøp

#### `ecr_sniff_pingpong.py`
- Heartbeat sniffing og analyse

---

## 🚀 Kom i Gang (Quick Start)

### 1. Verifiser Terminal
```bash
# Finn terminal på nettverket
python3 find_terminal.py

# Test forbindelse
python3 test_terminal.py
```

### 2. Kjør Hovedskript
```bash
# Kjør det anbefalte scriptet
python3 ecr_server_v22_golden_format.py
```

### 3. Følg Instruksjonene
Scriptet vil gi tydelige instruksjoner for:
- Når du skal trykke ENTER
- Når du skal sette kort i terminal
- Hva de forskjellige responsene betyr

---

## 📊 Protokolldetaljer

### Transportlag
- **Protokoll:** TCP
- **Port:** 8009
- **Framing:** 2-byte big-endian length header + payload
- **Encoding:** ISO-8859-1

### Heartbeat
- **Format:** `0x00 0x00` (lengde = 0)
- **Frekvens:** ~1 sekund
- **Respons:** MÅ svare `0x00 0x00` (PONG)

### Kommandoformat (Viking)
```
P;kommando;sekvensnr;beløp;cashback
```

**Eksempel:**
```
P;10;1;200;0
```
- `P` = Protocol prefix
- `10` = Kjøp (Purchase)
- `1` = Sekvensnummer
- `200` = Beløp i øre (2.00 NOK)
- `0` = Cashback

### Responskoder
| Kode | Betydning |
|------|-----------|
| `[00]` | Kommando mottatt (ACK) |
| `A000ECR Timeout` | Format OK, kort ikke lest |
| `A000FEIL I` | Ugyldig kommando |
| `D!000` | Dialog/venter |
| `` ` `` | Ukjent (backtick) |

---

## 🔧 Avhengigheter

Alle skript bruker kun Python standard library:
```python
import socket
import struct
import sys
import time
from datetime import datetime
```

Ingen eksterne pakker kreves (`pip install` ikke nødvendig).

---

## 📖 Dokumentasjon

For full teknisk rapport, se:
```
../../docs/ecr-integration/ECR_INTEGRATION_REPORT.md
../../docs/ecr-integration/ECR_INTEGRATION_REPORT.pdf
```

---

## ⚠️ Viktige Notater

1. **Terminal Type:** Ingenico Self/4000 (ubetjent terminal)
2. **TLS:** MÅ være satt til "Nei" i terminalinnstillinger
3. **Komm Type:** MÅ være "ECR/Kasse"
4. **Resultat:** Kommunikasjon fungerer, men transaksjoner starter ikke (mangler SDK/provisjonering)

---

## 🆘 Feilsøking

### Problem: "Connection refused"
**Løsning:** 
- Sjekk at terminal er på samme nettverk
- Verifiser at ECR IP PORT er 8009 i terminalinnstillinger
- Kjør `find_terminal.py` for å finne riktig IP

### Problem: Mottar kun `[00]` og ingen transaksjon
**Løsning:**
- Dette er forventet - terminal mangler autentisering/provisjonering
- Kontakt Nets/Bambora for ECR SDK

### Problem: Krypterte/ulesbare responser
**Løsning:**
- Sett ECR/TLS til "Nei" på terminalen
- Restart terminal etter endring

---

## 📞 Support

For hjelp med ECR-integrasjon:
- **Nets Support:** https://www.nets.eu/no/kontakt-oss
- **Bambora Support:** support.merchant@bambora.com

---

**Sist oppdatert:** Januar 2026  
**Forfatter:** LPG-EHL Development Team
