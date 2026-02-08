# BAXI-protokollen – Python-implementasjonsrapport (fra dekompilering)

**Kilde:** [instructions/baxi-decompiled.md](../instructions/baxi-decompiled.md)  
**Formål:** Implementasjonsguide for Python mot betalingsterminal basert på dekompilert BAXI .NET DLL  
**Versjon:** 1.0  
**Dato:** Februar 2026  

---

## Innhold

1. [Innledning](#innledning)
2. [Hva vi har fra dekompileringen](#hva-vi-har-fra-dekompileringen)
3. [Hva vi mangler](#hva-vi-mangler)
4. [Python-implementasjonsstrategi](#python-implementasjonsstrategi)
5. [Kodeeksempler (Python)](#kodeeksempler-python)
6. [TCP mot betalingsterminal](#tcp-mot-betalingsterminal)
7. [Oppdagelsesstrategi](#oppdagelsesstrategi)
8. [Oppsummering](#oppsummering)

---

## Innledning

Denne rapporten er basert utelukkende på **dekompileringen** av Nets BAXI .NET DLL (baxi_dotnet 1.3.2.0, BBS.BAXI namespace) beskrevet i [instructions/baxi-decompiled.md](../instructions/baxi-decompiled.md). Formålet er å beskrive hvordan man kan bygge en **Python-implementasjon** mot en betalingsterminal som snakker BAXI-protokollen: hva vi kan utlede fra API-overflaten, hva som mangler (parametre, wire-format, TLD-tagger), og hvordan TCP-laget og event-håndtering bør designes. Python egner seg godt for rask prototyping, REPL-tester og mock-servere for å fange trafikk.

Dekompileringen gir oss **hele API-overflaten** (BaxiCtrl, args, events), men som filen selv konkluderer: *«Vi har ikke parametrene»* – konkrete verdier for TLD-tagger, koder og wire-format må innhentes via trafikk-analyse eller dokumentasjon.

---

## Hva vi har fra dekompileringen

### BaxiCtrl – hovedklasse (mapping til Python-konsepter)

| Kategori | Innhold |
| -------- | ------- |
| **Konfigurasjon** | LogFilePrefix, LogFilePath, HostIpAddress, HostPort, ComPort, BaudRate, DeviceString, SerialDriver, TraceLevel, PrinterWidth, DisplayWidth, CutterSupport, VendorInfoExtended, IndicateEotTransaction, PowerCycleCheck, TidSupervision, AutoGetCustomerInfo, TerminalReady, UseDisplayTextID, UseExtendedLocalMode, UseSplitDisplayText, Use2KBuffer, DisplayTextInLocalMode, LogAutoDeleteDays, MsgRouterOn, MsgRouterIpAddress, MsgRouterPort, SocketListener, SocketListenerAddress, SocketListenerPort |
| **Status (read-only)** | Version, TermType, TerminalID, TerminalSwVersion, TerminalDeviceData_TLD, MethodRejectInfo, MethodRejectCode |
| **Metoder** | Open(), Close(), Dispose(), Administration(AdministrationArgs), TransferAmount(TransferAmountArgs), SendTLD(SendTldArgs), SendJson(SendJsonArgs), TransferCardData(TransferCardDataArgs), BiBAdministration(BiBAdministrationArgs), BiBTransaction(BiBTransactionArgs), GetTLDTag(...) |
| **Events** | OnDisplayText, OnTerminalReady, OnPrintText, OnLocalMode, OnLastFinancialResult, OnError, OnTLDReceived, OnStdRsp, OnJsonReceived |

### Argumenttyper (dataclasses / typing)

- **AdministrationArgs:** oper_id: str, adm_code: int
- **TransferAmountArgs:** oper_id, type1, amount1, type2, amount2, type3, amount3, host_data, article_details, payment_condition_code, auth_code, optional_data
- **LastFinancialResultEventArgs:** result_data, result, accumulator_update, issuer_id, truncated_pan, timestamp, verification_method, session_number, stan_auth, sequence_number, total_amount, rejection_source, rejection_reason, tip_amount, surcharge_amount, terminal_id, acquirer_merchant_id, card_issuer_name, response_code, tcc, aid, tvr, tsi, atc, aed, iac, organisation_number, bank_agent, encrypted_pan, account_type, optional_data
- **LocalModeEventArgs:** Samme som LastFinancialResult + local_mode_result_data
- **DisplayTextEventArgs:** display_text, displaytext_source_id, displaytext_id
- **PrintTextEventArgs:** print_text
- **BaxiErrorEventArgs:** error_code, error_string
- **TLDReceivedArgs / SendTldArgs:** tld_type, tld_data / tld_field (bytes)
- **JsonReceivedArgs / SendJsonArgs:** json_string / json_data
- **BiBAdministrationArgs:** adm_code, oper_id
- **BiBTransactionArgs:** amount, transaction_data
- **TransferCardDataArgs:** track_type (1/2/3), track_data
- **StdRspReceivedArgs:** response
- **TerminalReadyEventArgs:** (tom)

### IBaxiEvents – event-flyt

Alle ni callbacks: OnDisplayText, OnPrintText, OnError, OnLocalMode, OnStdRsp, OnTLDReceived, OnTerminalReady, OnLastFinancialResult, OnJsonReceived.

### Internt (avkuttet i filen)

**QueueType:** DFS13_CONTROLLER, DFS13_LOW_LEVEL, HOST – indikerer interne køer mellom controller, lavnivå og host.

---

## Hva vi mangler

Identisk med Kotlin-rapporten:

1. **Wire-format:** Framing (TLD vs length-prefix vs STX/ETX/LRC), byte-rekkefølge, charset.
2. **TLD:** Tag-verdier, lengdefelt (1/2 byte), nesting.
3. **Koder:** AdmCode, Type1/2/3, Result, ErrorCode – full liste ukjent.
4. **Sekvens:** Nøyaktig rekkefølge Open → TerminalReady → TransferAmount → events → LastFinancialResult; eventuelle handshake-meldinger (f.eks. I1/I2).
5. **TCP vs Serial:** Når brukes HostIpAddress/HostPort vs ComPort/BaudRate.
6. **Proprietære utvidelser:** Nets/Viking kan ha egne utvidelser.

---

## Python-implementasjonsstrategi

### Moduler

- **config:** Dataclass eller dict for host, port, com_port, baud_rate, timeouts.
- **connection:** socket (evt. ssl.wrap_socket) for TCP/SSL; connect, send, receive.
- **protocol:** tld_parser (placeholder), framing (length-prefix, STX/ETX/LRC).
- **events:** Callback-interface (Protocol eller ABC) med on_display_text, on_print_text, on_last_financial_result, on_error, etc.
- **client:** BaxiClient som bruker connection + protocol og kaller event-handlere.

### Fordeler med Python

- REPL for rask testing av bytes og struct.
- `struct.pack` / `struct.unpack` for binær framing.
- `socket` og `ssl` innebygd; enkelt å bygge mock-server som logger alle bytes.
- Eksisterende [ecr_server_v3_handshake.py](../scripts/python/ecr-testing/ecr_server_v3_handshake.py) og [ecr_server_v22_golden_format.py](../scripts/python/ecr-testing/ecr_server_v22_golden_format.py) som referanse for framing og handshake.

---

## Kodeeksempler (Python)

### Dataclasses (args og resultater)

```python
from dataclasses import dataclass
from typing import Optional

@dataclass
class AdministrationArgs:
    adm_code: int
    oper_id: str = "1"

@dataclass
class TransferAmountArgs:
    oper_id: str = "1"
    type1: int = 0       # e.g. 0 = Purchase, 1 = Refund (TBD from capture)
    amount1: int = 0     # øre
    type2: int = 0
    amount2: int = 0
    type3: int = 0
    amount3: int = 0
    host_data: Optional[str] = None
    article_details: Optional[str] = None
    payment_condition_code: Optional[str] = None
    auth_code: Optional[str] = None
    optional_data: Optional[str] = None

@dataclass
class LastFinancialResult:
    result_data: Optional[str]
    result: int           # 0 = approved
    accumulator_update: int
    issuer_id: int
    truncated_pan: Optional[str]
    timestamp: Optional[str]
    verification_method: int
    session_number: Optional[str]
    stan_auth: Optional[str]
    sequence_number: Optional[str]
    total_amount: int
    rejection_source: int
    rejection_reason: Optional[str]
    tip_amount: int
    surcharge_amount: int
    terminal_id: Optional[str]
    acquirer_merchant_id: Optional[str]
    card_issuer_name: Optional[str]
    response_code: Optional[str]
    tcc: Optional[str]
    optional_data: Optional[str]
```

### Event-callback interface (Protocol)

```python
from typing import Protocol

class BaxiEventHandler(Protocol):
    def on_display_text(self, source_id: int, text_id: int, text: str) -> None: ...
    def on_print_text(self, text: str) -> None: ...
    def on_error(self, error_code: int, error_string: str) -> None: ...
    def on_local_mode(self, result: LastFinancialResult, local_mode_result_data: Optional[str]) -> None: ...
    def on_std_rsp(self, response: str) -> None: ...
    def on_tld_received(self, tld_type: str, tld_data: bytes) -> None: ...
    def on_terminal_ready(self) -> None: ...
    def on_last_financial_result(self, result: LastFinancialResult) -> None: ...
    def on_json_received(self, json_string: str) -> None: ...
```

### TCP: send med length-prefix, receive med length-prefix

```python
import socket
import struct

def build_length_prefix_frame(payload: bytes) -> bytes:
    """2-byte big-endian length + payload (as in ecr_server_v3_handshake)."""
    header = struct.pack(">H", len(payload))
    return header + payload

def recv_length_prefix(sock: socket.socket) -> bytes:
    header = sock.recv(2)
    if len(header) < 2:
        raise ConnectionError("Short read on length header")
    (msg_len,) = struct.unpack(">H", header)
    data = b""
    while len(data) < msg_len:
        chunk = sock.recv(msg_len - len(data))
        if not chunk:
            raise ConnectionError("Connection closed during payload")
        data += chunk
    return data

class BaxiTcpConnection:
    def __init__(self, host: str, port: int, connect_timeout: float = 10.0, read_timeout: float = 30.0):
        self.host = host
        self.port = port
        self.connect_timeout = connect_timeout
        self.read_timeout = read_timeout
        self._sock: Optional[socket.socket] = None

    def connect(self) -> None:
        if self._sock is not None:
            return
        self._sock = socket.create_connection((self.host, self.port), timeout=self.connect_timeout)
        self._sock.settimeout(self.read_timeout)

    def send(self, payload: bytes) -> None:
        frame = build_length_prefix_frame(payload)
        self._sock.sendall(frame)

    def receive(self) -> bytes:
        return recv_length_prefix(self._sock)

    def close(self) -> None:
        if self._sock:
            self._sock.close()
            self._sock = None
```

### LRC og STX/ETX/LRC-ramme (som i ecr_server_v3_handshake)

```python
STX = b"\x02"
ETX = b"\x03"

def calculate_lrc(data: bytes) -> int:
    lrc = 0
    for b in data:
        lrc ^= b
    return lrc

def build_stx_etx_lrc_frame(payload: bytes) -> bytes:
    """Serial-style framing: STX + payload + ETX + LRC (LRC over payload+ETX)."""
    lrc_base = payload + ETX
    lrc = calculate_lrc(lrc_base)
    return STX + payload + ETX + bytes([lrc])
```

### BaxiClient (placeholder for TLD/JSON)

```python
class BaxiClient:
    def __init__(self, host: str, port: int, handler: BaxiEventHandler):
        self._conn = BaxiTcpConnection(host, port)
        self._handler = handler

    def open(self) -> None:
        self._conn.connect()
        # TBD: send Open/Session TLD if required; then wait for OnTerminalReady

    def transfer_amount(self, args: TransferAmountArgs) -> int:
        payload = self._build_transfer_amount_payload(args)
        self._conn.send(payload)
        response = self._conn.receive()
        return self._parse_transfer_amount_response(response)

    def administration(self, args: AdministrationArgs) -> int:
        payload = self._build_administration_payload(args)
        self._conn.send(payload)
        response = self._conn.receive()
        return self._parse_administration_response(response)

    def _build_transfer_amount_payload(self, args: TransferAmountArgs) -> bytes:
        # Placeholder: real implementation needs TLD tags from capture
        body = f"P;10;{args.oper_id};{args.amount1};0"
        return body.encode("iso-8859-1")

    def _parse_transfer_amount_response(self, data: bytes) -> int:
        # Placeholder: return result code (0 = approved)
        return 0

    def close(self) -> None:
        self._conn.close()
```

---

## TCP mot betalingsterminal

### Roller

- **App (Python)** er typisk **TCP-klient** som kobler til Nets Cloud (SSL, f.eks. 3.33.230.243:6001) eller terminal direkte (ren TCP, port ofte 3000–3010 eller 8009).

### Anbefalinger

- **Port-scan:** Bruk f.eks. `socket.create_connection((host, port), timeout=2)` i en loop over porter (3000–3010, 6001) for å finne åpen port; eller `concurrent.futures.ThreadPoolExecutor` for parallell skanning.
- **Timeout:** Sett connect- og read-timeout (f.eks. 10 s / 30 s).
- **Framing:** Length-prefix (2 byte big-endian) for TCP; STX/ETX/LRC for serial – se [ecr_server_v3_handshake.py](../scripts/python/ecr-testing/ecr_server_v3_handshake.py) for begge varianter.
- **SSL:** For Nets Cloud bruk `ssl.wrap_socket(sock)` eller `ssl.create_default_context().wrap_socket(sock, server_hostname=host)` etter TCP connect.

### Eksisterende Python-referanser

- [ecr_server_v3_handshake.py](../scripts/python/ecr-testing/ecr_server_v3_handshake.py): LRC, length-framing, handshake (I1/I2), Purchase-kommando.
- [ecr_server_v22_golden_format.py](../scripts/python/ecr-testing/ecr_server_v22_golden_format.py): Alternativ framing/format for testing.

---

## Oppdagelsesstrategi

1. **Wireshark:** Fanger trafikk mellom .NET-app og terminal/Nets Cloud; analyser framing og TLD.
2. **Mock-server:** Python-server (som ecr_server_v3) som logger alle mottatte bytes; kjør .NET-klient mot mock for å matche format.
3. **Fuzzing:** I test-miljø, prøv TLD-tagger og AdmCode/Type-verdier systematisk (lav risiko).

---

## Oppsummering

| Område | Kan bygges i Python i dag | Krever trafikk/dokumentasjon |
| ------ | ------------------------- | ---------------------------- |
| API-lag (args, events) | Ja – dataclasses og BaxiEventHandler | – |
| TCP/SSL connection | Ja – BaxiTcpConnection med length-prefix | Port og framing-validering |
| Framing (length-prefix / STX/ETX/LRC) | Ja – se ecr_server_v3 | Hvilken variant terminal bruker |
| TLD/JSON-innhold | Kun placeholder | TLD-tagger, koder, sekvens |
| transfer_amount / administration | Skjelett med placeholder payload | Reelle payload- og svar-format |

**Anbefaling:** Bygg BaxiClient med konfigurerbar framing og placeholder for TLD/JSON; fyll inn konkrete tag-verdier og koder når trafikk er innsamlet. Bruk ecr_server_v3_handshake.py som referanse for TCP-framing og handshake-mønster.

---

**Full dokumentasjon:** Se også [BAXI_DECOMPILED_SAMLET_RAPPORT.md](BAXI_DECOMPILED_SAMLET_RAPPORT.md) for felles protokollbeskrivelse, gap og TCP-anbefalinger for både Kotlin og Python.
