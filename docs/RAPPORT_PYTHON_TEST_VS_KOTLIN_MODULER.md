# Rapport: python-test vs Kotlin-modulene (core, transport, service, webapp, headless)

**Omfang:** Sammenligning av `python-test` med Kotlin i `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-service`, `lpg-ehl-webapp`, `lpg-ehl-app-headless`. Kun research; ingen kodeendringer.

**Referanse:** [ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md](ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md) dekker python-test vs core i mer detalj (protokoll, 8N1/8E1, feltverktøy).

---

## 1. python-test – oversikt

| Fil | Rolle |
|-----|--------|
| `ehl_protocol.py` | EHL-ramming: STX+LEN+ADDR+CMD+DATA+CHK+ETX, XOR-sjekksum, `build_frame` / `parse_one_frame` / `extract_frames`, kommando-navn |
| `serial_linux.py` | Seriell I/O på Linux: 8N1, `open_serial`, valgfri RS-485 (`TIOCSRS485`, RTS-venting), select-basert lesing |
| `logging_utils.py` | Enkel logging til stdout (INFO/WARN/DEBUG/die) |
| `00_list_ports.py` | Lister `/dev/ttyUSB*`, `ttyACM*`, `ttyS*`, `/dev/serial/by-id/*` med exists/access |
| `01_probe_readonly.py` | Sender STATE, ERROR_QUERY, VOLUME, TANKBIT mot én adresse; retries; oppsummerer gyldige svar |
| `02_scan_addresses.py` | Sender STATE til adresseområde (f.eks. 1–32); lister adresser som svarer |
| `03_control_unblock_block.py` | UNBLOCK/BLOCK; krever `--i-understand-this-can-affect-real-hardware`; `--dry-run` |
| `04_listen_only.py` | Passiv sniff: leser seriell linje, parser EHL-rammer, skriver ut RX; ingen TX |

Alle skriptene er standalone (Python 3, ingen pip). Seriell: 9600 8N1.

---

## 2. Kotlin-moduler – roller

### 2.1 lpg-ehl-core

- **Protokoll:** `EhlCodec`, `EhlPacket`, `EhlCommands`, `EhlProtocol`, `EhlProtocolConfig`, `EhlPacketBuilder`, `EhlDataParser`; `EhlDisplayParser`, `DispenserFaultHandler`, `DispenserStateMapper`, `TankStatusMapper`, `LinetestValidator`, `EhlDiagnostics`.
- **Emulator:** `EhlDispenserEmulator`, `InMemorySerialPort`, `DispenserSimulator`.
- **Transaksjon (core):** `Transaction`, `TransactionWatchdog`, `TransactionManager`.
- **Betaling/terminal (core):** `NetsBaxProtocol`, `PaymentGateway`, `CloudTerminalClient`, `TerminalConnection`, `InteractivePaymentDemo`.
- **Transport (core):** `SerialTransport`-interface.
- **Verktøy:** `QuickTest`, `Main` (demo).

Ingen seriell I/O eller REST/CLI her; kun protokoll, emulator og relatert domene.

### 2.2 lpg-transport

- **Seriell:** `SerialPortConfig` (9600 8E1), `SerialPortManager`, `SerialPortIO`, `DispenserConnection`, `HardwareWatchdogCapable`; `RealSerialTransport` (jSerialComm, 8E1).
- **EHL-over-wire:** `EhlCommunicator` (send/receive, buffer, timeout).
- **Demo:** `SerialDemo` – lister porter (`SerialPortManager.listAvailablePorts()`), kobler til valgt port, sender STATE m.m.

Seriell konfigurasjon og faktisk RS-485-tilkobling skjer her. Ingen REST, ingen DB.

### 2.3 lpg-ehl-service

- **Operasjoner:** `EhlOperationsService` – linetest, getState, getVolume, getPrice, unblock, block, getError, getTank, runVb6Sequence; bruker `EhlCommunicator`.
- **Wire trace:** `WireTraceService` – kjører EHL-kommandoer, returnerer TX/RX HEX og validering (VB6-compliance).
- **Pumpe/domene:** `DispenserService`, `PumpStateService`, `PumpAuthorizationService`, `FuelPumpService`; `DispenserStatus`, `PumpAuthorization`; JPA-repos.
- **Transaksjon:** `TransactionService`, `TransactionRepository`, `Transaction` (entity); `TransactionSyncService` (Azure).
- **Pris:** `PriceService`, `PriceHistory`, `PriceHistoryRepository`.
- **System:** `DiagnosticsService`, `HardwareWatchdogService`, `ReportService`.
- **Integrasjon:** `NetsCloudSocketClient`; Azure (`AzureSyncService`, `AzureQueueReaderService`, `AzureSyncQueue`).
- **Annet:** `EhlPacketProcessor`, `MockProtocolService`, credit/customer, road tax, daily summary, m.m.

Service inneholder all applikasjonslogikk rundt EHL, pumpe, transaksjoner og integrasjoner. Ingen REST-controllere; de ligger i webapp/headless.

### 2.4 lpg-ehl-webapp

- **REST API:** Undertow-basert; OpenAPI/Swagger.
- **EHL / protokoll:** `ProtocolTestController` – POST `/api/v1/protocol/test/*` for linetest, state, volume, price, error, tank, block, unblock; bruker `WireTraceService`. `DiagnosticsController` – GET `/admin/ehl/diagnostics` (alle / per adresse). `DispenserController` – GET dispensers, status, live status. `PumpController` (LAB) – status, unblock, block, card-swipe m.m.
- **Transaksjon, pris, betaling, rapporter, config, road tax, credit:** egne controllere.
- **Transport:** `TransportConfiguration` (EMULATOR / SOCAT / HARDWARE), `RealSerialPortAdapter`, `EmulatorSerialPortAdapter`; `CommunicationConfig`.
- **Emulator:** `EmulatorService`, `EmulatorController`; `EmulatorClient`.
- **Annet:** WebSocket (logger), security, SPA-routing, payment config, Azure config, m.m.

Webappen eksponerer EHL-operasjoner og domene-logikk via REST. Protokoll-test bruker `WireTraceService` → `EhlCommunicator` → transport.

### 2.5 lpg-ehl-app-headless

- **Oppstart:** `HeadlessApplication`; `HeadlessStartupRunner`, `HeadlessEventConfiguration`.
- **Transport:** `TransportConfiguration` (HARDWARE/SOCAT/EMULATOR), `SerialPortManager`-beans.
- **Polling:** `HeadlessPollingService` – scheduled; poller STATE/VOLUME, sjekker PENDING-autorisasjoner, sender UNBLOCK, oppdaterer status.
- **Debug API (profil `debug-api`):** `DebugController` – GET `/api/debug/health`, `/api/debug/state/{addr}`, `/api/debug/volume/{addr}`, m.m.; POST `/api/debug/linetest/{addr}`, `/api/debug/unblock/{addr}`, `/api/debug/block/{addr}`; bruker `EhlCommunicator` og `PumpStateService`. Kun aktiv med Undertow når `debug-api` er aktiv.
- **Annet:** `HeartbeatScheduler`, `SecurityConfig`, Azure config.

Headless har ingen vanlig web-UI; kjører polling og (valgfritt) debug-API for testing med f.eks. curl.

---

## 3. Mapping: python-test → Kotlin

| python-test | Kotlin-modul(er) | Hvor / hvordan |
|-------------|-------------------|----------------|
| **ehl_protocol** (ramming, sjekksum, kommandoer) | **core** | `EhlCodec`, `EhlPacket`, `EhlCommands`, `EhlProtocol` tilsvarer. Core har i tillegg parsere, mappere, config. |
| **serial_linux** (8N1, RS-485) | **transport** | `SerialPortConfig`, `SerialPortManager`, `RealSerialTransport` – men **8E1**, ikke 8N1. Ingen `TIOCSRS485`/RTS i transport. |
| **00_list_ports** | **transport** | `SerialPortManager.listAvailablePorts()`, `SerialDemo` bruker det. Ingen dedikert list-ports CLI/REST. |
| **01_probe_readonly** (STATE+ERROR+VOLUME+TANK) | **service**, **webapp**, **headless** | `EhlOperationsService`: getState, getError, getVolume, getTank. REST: ProtocolTestController, DebugController. Ingen identisk «probe batch»-script; vb6-sequence (LINETEST→STATE→VOLUME→PRICE) og smoke (STATE) er nær. |
| **02_scan_addresses** | **service** (CLI) | `SmokeTestCommand` i CLI tester oppgitt adresseliste (`--addresses 1,2`). Ingen «scan range» (f.eks. 1–32) i webapp/headless/core/transport. |
| **03_control_unblock_block** | **service**, **webapp**, **headless** | `EhlOperationsService` unblock/block; `PumpController` (LAB), `ProtocolTestController`, `DebugController`. Ingen safety-flag eller dry-run som i Python. |
| **04_listen_only** | — | Ingen listen-only / passiv sniff i noen av modulene. Kotlin sender og venter svar; sniffer ikke uten å sende. |

---

## 4. Avvik og gap

### 4.1 Seriell: 8N1 vs 8E1

- **python-test:** `serial_linux._set_raw_8n1` → 9600 8N1.
- **Kotlin (transport):** `SerialPortConfig`, `RealSerialTransport` → 9600 **8E1**.

LPG-EHL-fasit er 8E1. Python skiller seg derfra. Se [ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md](ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md) for detaljer.

### 4.2 Kun i python-test (Kotlin har ikke tilsvar)

| Funksjon | python-test | Kotlin |
|----------|-------------|--------|
| **Listen-only (sniff)** | `04_listen_only` | Ingen i core, transport, service, webapp, headless. |
| **Adresse-scan (range)** | `02_scan_addresses` (f.eks. 1–32) | SmokeCommand har fast liste; ingen range-scan. |
| **List ports som CLI** | `00_list_ports` | `listAvailablePorts` + `SerialDemo`; ingen eget list-ports script/API. |
| **Probe-batch** (STATE+ERROR+VOLUME+TANK) | `01_probe_readonly` | Enkelte operasjoner + vb6-seq/smoke; ikke samme batch. |
| **UNBLOCK/BLOCK safety + dry-run** | `03_control_*` | Ublock/block finnes, men uten tilsvarende flag/dry-run. |
| **RS-485 TIOCSRS485 / RTS** | `--rs485`, RTS-venting | Ikke styrt på samme måte i transport. |

### 4.3 Kun i Kotlin (python-test dekker ikke)

- **Protokoll:** Display-parsing, fault/state/tank-mapping, linertest-validering, diagnostikk, protokollvarianter.
- **Emulator:** InMemorySerialPort, EhlDispenserEmulator, DispenserSimulator.
- **Applikasjon:** Transaksjoner, JPA, priser, road tax, daily summary, kunder/kreditt, Azure-sync, Nets Cloud.
- **API:** REST (webapp, headless debug), OpenAPI, WebSocket.
- **Drift:** Scheduled polling (headless), watchdog, heartbeat, kortdragning → UNBLOCK-flyt.
- **CLI:** Spring Shell (linetest, state, volume, price, unblock, block, error, tank, vb6-sequence, smoke).

python-test er kun feltverktøy for ramming, probe, scan, listen og kontroll; den har ingen applikasjonslogikk, DB eller API.

---

## 5. Kort modulvis oversikt

| Modul | Seriell | EHL-protokoll | EHL-operasjoner | REST/API | DB / applikasjon |
|-------|---------|----------------|-----------------|----------|-------------------|
| **core** | Nei | Ja (codec, parsere, mappere) | Nei (kun bygg/parse) | Nei | Nei (transaksjon/betaling i core er domene, ikke DB) |
| **transport** | Ja (8E1) | Nei (bruker core) | Via `EhlCommunicator` | Nei | Nei |
| **service** | Nei | Nei (bruker core) | Ja (`EhlOperationsService`, `WireTraceService`) | Nei | Ja (JPA, Azure, Nets, m.m.) |
| **webapp** | Via config | Via service | Ja (ProtocolTest, Pump, Dispenser, m.m.) | Ja | Via service |
| **headless** | Via config | Via service | Ja (polling, DebugController) | Kun debug-api | Via service |
| **python-test** | Ja (8N1) | Minimal (ramming) | Probe, scan, listen, control | Nei | Nei |

---

## 6. Oppsummert

- **Protokoll og ramming:** python-test og Kotlin (core) bruker samme EHL-format og XOR-sjekksum. Core er fasit og har mye mer (parsing, mapping, diagnostics).
- **Seriell:** python-test bruker 8N1; Kotlin (transport) 8E1. For LPG-EHL er 8E1 fasit.
- **Feltverktøy:** python-test tilbyr listen-only, adresse-scan, list ports, probe-batch og control med safety/dry-run. I Kotlin-stacken finnes ikke listen-only eller scan-range; list ports er kun via `SerialDemo` / `listAvailablePorts`; probe/control finnes som operasjoner og API, men uten de samme verktøyene.
- **Applikasjon:** All business logic, transaksjoner, integrasjoner, REST og polling ligger i Kotlin (service, webapp, headless). python-test har ingen slikt.

Rapporten er basert på gjennomgang av kildekode i `python-test` og i `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-service`, `lpg-ehl-webapp`, `lpg-ehl-app-headless`. Ingen kode er endret.
