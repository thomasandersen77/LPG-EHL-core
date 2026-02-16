# Analyse: python-test vs lpg-ehl-core (Kotlin fasit)

**Formål:** Sammenligne `python-test` og `lpg-ehl-core` med Kotlin som fasit for LPG-EHL, og kartlegge hva Python gjør som Kotlin ikke gjør.

---

## 1. Kort oversikt

| | python-test | lpg-ehl-core (Kotlin) |
|--|-------------|------------------------|
| **Rolle** | Feltverktøy: probe, scan, listen, control | Protokoll + codec, emulator, tester |
| **Format** | Standalone Python 3-skript (uten pip) | Kotlin/JVM, Maven |
| **EHL-ramming** | `ehl_protocol.py`: STX+LEN+ADDR+CMD+DATA+CHK+ETX, XOR-sjekksum | `EhlCodec`, `EhlPacket`, `EhlCommands` |
| **Seriell** | 8N1 (ingen paritet) | 8E1 (even parity) – **avvik** |

---

## 2. Protokoll og ramming – likt

- **Rammeformat:** `STX (1) + LEN (1) + ADDR (1) + CMD (1) + DATA (0..n) + CHK (1) + ETX (1)`. Fellest for begge.
- **Sjekksum:** XOR av alle byte fra STX til og med siste DATA-byte (alt unntatt CHK og ETX). Samme i Python og Kotlin.
- **STX:** Controller `0x10`, dispenser `0x20`. **ETX:** `0x36`. Samme.
- **Kommandoer:** STATE (`0x4B`), ERROR_QUERY (`0x4C`), VOLUME (`0x45`), TANK/TANKBIT (`0xC5`), UNBLOCK (`0x77`), BLOCK (`0x69`), LINETEST (`0x6A`), PROG_PRC (`0xA9`), osv. – tilsvarer hverandre.

Kotlin-core har i tillegg full protokoll-støtte (EhlDisplayParser, DispenserFaultHandler, TankStatusMapper, PROG_VOLUME/PROG_AMOUNT/PRODUCT_SELECT, osv.). Python har kun et minimalt sett i `ehl_protocol.py` og scriptene.

---

## 3. Viktig avvik: 8N1 vs 8E1 (LPG-EHL)

| | python-test | lpg-ehl-core / lpg-transport |
|--|-------------|------------------------------|
| **Paritet** | Ingen (`_set_raw_8n1`, `PARENB` fjernes) | **EVEN** (`SerialPort.EVEN_PARITY`) |
| **Format** | 9600 8N1 | 9600 8E1 |

- **Kotlin (fasit):** `SerialPortConfig`, `RealSerialTransport` og dokumentasjon bruker **8E1** for EHL/LPG-EHL.
- **Python:** `serial_linux.py` setter eksplisitt **8N1** (ingen paritet).

`python-test/README.md` hevder at serial-innstillingene «matches … what the Kotlin side uses» – det stemmer **ikke**; Kotlin bruker 8E1.

**Konsekvens:** Hvis dispenseren forventer 8E1 (typisk for LPG-EHL), vil Python med 8N1 kunne gi feil eller ustabil kommunikasjon. **Python skiller seg fra Kotlin-fasiten på seriell konfigurasjon.**

---

## 4. Hva python-test gjør som Kotlin ikke gjør (LPG-EHL)

Kotlin har verken i core eller CLI tilsvarende **standalone**-verktøy for disse bruksområdene:

### 4.1 Listen-only (sniff) – `04_listen_only.py`

- **Python:** Lytt på seriell linje uten å sende. Les rå bytes, parse EHL-rammer, skriv ut gyldige RX-rammer i en konfigurerbar periode (`--duration-s`). Kan vise rå hex (`--show-raw`).
- **Kotlin:** Ingen tilsvarende listen-only CLI eller verktøy. Headless/CLI sender kommandoer og venter svar; de sniffer ikke passivt.

### 4.2 Adresse-scan – `02_scan_addresses.py`

- **Python:** Sender STATE (`0x4B`) til et intervall (f.eks. 1–32), identifiserer hvilke adresser som svarer, lister dem.
- **Kotlin:** `SmokeTestCommand` tester en **oppgitt** adresseliste (`--addresses 1,2`). Ingen «scan range»-funksjon. Core har ikke scan-verktøy.

### 4.3 List ports – `00_list_ports.py`

- **Python:** Lister relevante serielle enheter (`/dev/ttyUSB*`, `ttyACM*`, `ttyS*`, `/dev/serial/by-id/*`), med exists/access-info.
- **Kotlin:** `SerialPortManager.listAvailablePorts()` og jSerialComm `SerialPort.getCommPorts()` finnes, men det er **ingen** dedikert list-ports CLI i core eller CLI-modulen.

### 4.4 Probe read-only (batch) – `01_probe_readonly.py`

- **Python:** Sender i rekkefølge STATE, ERROR_QUERY, VOLUME, TANKBIT mot én adresse; retries og timeout; oppsummerer hvor mange kommandoer som ga gyldig svar.
- **Kotlin:** CLI har enkelte kommandoer (`state`, `error`, `volume`, `tank`) og `run-vb6-sequence` (LINETEST→STATE→VOLUME→PRICE). Ingen eksakt «probe batch» (STATE+ERROR_QUERY+VOLUME+TANKBIT) som ett script. `SmokeTestCommand` bruker kun STATE.

### 4.5 Control med sikkerhetsflagg – `03_control_unblock_block.py`

- **Python:** UNBLOCK/BLOCK kun ved eksplisitt `--i-understand-this-can-affect-real-hardware`. `--dry-run` for å vise planlagte rammer uten å sende.
- **Kotlin:** CLI `unblock`/`block` uten tilsvarende «safety flag» eller dry-run. Kontroll-logikk finnes, men ikke samme tydelige feltverktøy-grep.

### 4.6 RS-485 (Linux)

- **Python:** Valgfri `--rs485` med `TIOCSRS485`, RTS før/etter send (`--rts-before-ms`, `--rts-after-ms`). Linux-spesifikt.
- **Kotlin:** jSerialComm; RS-485 / RTS-venting er ikke styrt på samme måte i koden som er gjennomgått.

---

## 5. Resten av Kotlin-core – ikke dekket av python-test

Kotlin-core inneholder mye som python-test **ikke** implementerer:

- **Protokoll:** `EhlDisplayParser`, `DispenserFaultHandler`, `DispenserStateMapper`, `TankStatusMapper`, `EhlDiagnostics`, `LinetestValidator`, konfigurerbare varianter (`EhlProtocolConfig`).
- **Emulator:** `EhlDispenserEmulator`, `InMemorySerialPort`, `DispenserSimulator`.
- **Transaksjon:** `Transaction`, `TransactionWatchdog`.
- **Betaling/terminal:** `NetsBaxProtocol`, `PaymentGateway`, `CloudTerminalClient`, osv. (i core-modulen).
- **Tester:** Enhetstester for codec, parser, state/fault-mapping, VB6-compliance, osv.

Python er kun feltverktøy rundt minimal EHL-ramming og noen kommandoer.

---

## 6. Oppsummert

| Tema | python-test | Kotlin (fasit) |
|-------|-------------|----------------|
| Rammeformat / sjekksum / kommandoer | Samme som Kotlin | Fasit |
| Seriell 8N1 vs 8E1 | **8N1** – avvik fra fasit | **8E1** |
| Listen-only (sniff) | Ja | Nei |
| Adresse-scan (range) | Ja | Nei |
| List ports CLI | Ja | Nei |
| Probe batch (STATE+ERROR+VOLUME+TANK) | Ja | Delvis (smoke/vb6-seq, ikke identisk) |
| UNBLOCK/BLOCK med safety flag + dry-run | Ja | Nei (CLI uten tilsvarende) |
| RS-485 TIOCSRS485 / RTS | Ja (Linux) | Ikke på samme måte |
| Full protokoll, emulator, transaksjon, betaling | Nei | Ja |

**Konklusjon:**  
- **Protokoll og ramming** er tilnærmet like; Kotlin er fasit.  
- **Seriell:** Python bruker 8N1, Kotlin 8E1. For LPG-EHL er 8E1 fasit; Python skiller seg der.  
- **Feltverktøy:** Python tilbyr listen-only, adresse-scan, list ports, probe-batch og control med safety/dry-run som **egen** funksjonalitet. Kotlin har ingen direkte motsvar til listen-only og scan, og ikke samme list-ports / safety-/dry-run-verktøy.

**Anbefaling:**  
- Dersom python-test skal brukes mot ekte LPG-EHL-hardware: vurder å **endre serial til 8E1** (ev. med `--parity`/`--8e1`) slik at det matcher Kotlin-fasiten.  
- Oppdatér `python-test/README.md` slik at det ikke hevder at serial-innstillingene matcher Kotlin; de gjør det ikke (8N1 vs 8E1).
