# Pumpekontroll-klone (EHL) – 1:1 protokoll mot `pumpekontroll.frm`

Dette prosjektet er en **kjørbar** referanse-implementasjon av EHL-wire-protokollen
slik den er implementert i VB6-fila `pumpekontroll.frm`.

Målet er **100 % protokollidentisk** på wire-nivå:

- STX = `0x10` fra controller til dispenser
- STX = `0x20` fra dispenser til controller
- ETX = `0x36`
- LEN = total ramme-lengde (inkl. ETX)
- CHK = XOR av bytes `STX..DATA`
- Polling-sekvens identisk med `state_timer_Timer`
- Parsing/tilstand identisk med `MSComm1_OnComm` (STATE/VOLUME/PRICE/LINETEST/TANK)

Prosjektet inkluderer **ikke**:
- database, rapporter, printer
- bankterminal (baxi.dll)

Det er med vilje – det som leveres her er en ren, testbar og portabel `EHL`-kjerne.

## Installering

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# Linux/Mac: source .venv/bin/activate
pip install -r requirements.txt
```

## Kjøring

### Standard (historisk adresse = dispensernr + 32)

```bash
python pumpekontroll_clone.py --port COM5 --dispenser-nr 1 --unit-price 16.04
```

Dette bruker ADDR = 33, som tilsvarer `dispnr(0) = dispensernr + 32` i VB6.

### Overstyr adresse

```bash
python pumpekontroll_clone.py --port /dev/ttyUSB0 --addr 33
```

## Kommandoer

Når programmet kjører:

- `start`  -> sender `PRESTART (C3 30)` og deretter `UNBLOCK (77)`
- `stop`   -> sender `BLOCK (69)`
- `reset`  -> sender `ZER/RESET (81)`
- `day`, `night`, `closed` -> setter station-mode (påvirker bare automode-logg)
- `status` -> skriver status
- `quit`   -> avslutter

## Arkitektur

- `ehl/protocol.py` – framing + checksum + decode av VOLUME/PRICE og bit-tolkning
- `ehl/stream_parser.py` – byte-for-byte parser (som VB6 MSComm1.InputLen=1)
- `ehl/serial_client.py` – serial IO + kommandoer som finnes i pumpekontroll.frm
- `ehl/model.py` – tilstandsoppdatering som speiler VB6-logikken
- `ehl/poller.py` – poll-loop som speiler `state_timer_Timer`
- `pumpekontroll_clone.py` – CLI-programmet

## Notater om kompatibilitet

Denne koden følger pumpekontroll.frm sin mottaksregel: innkommende rammer må starte med
`STX=0x20`. Hvis din hardware sender med annet STX må du først gjøre den kompatibel.
