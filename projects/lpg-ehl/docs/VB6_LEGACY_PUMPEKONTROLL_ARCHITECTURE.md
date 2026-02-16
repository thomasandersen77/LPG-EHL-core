# VB6 Legacy — Architecture (Mermaid/UML)

This document is a visual companion to `docs/VB6_LEGACY_PUMPEKONTROLL_SPEC.md`.

It describes the VB6 `pumpcontrol` system as a set of **components**, **hardware/software boundaries**, and the **two core workflows** (bank preselect + station credit) using Mermaid diagrams.

---

## Big picture (components + boundaries)

```mermaid
flowchart LR
  %% External actors
  Operator[Operator / Technician]
  Customer[Customer]
  RemoteClient[Control client\n(VB6 Dispenserklient or other)]
  Poller[Status poller]

  %% Main app
  subgraph Host["Windows host running VB6 pumpcontrol.exe"]
    UI["Pumpekontroll (VB6 Form)\n- timers\n- event handlers\n- global state machine"]
    DispenserDrv["EHL Dispenser driver\n(MSComm1 + frame codec)"]
    PrinterDrv["Receipt printer driver\n(com_print + ESC/status)"]
    PinpadDrv["Pinpad driver\n(com_pinpad)"]
    RFIDDrv["RFID driver\n(RFIDCOM)"]
    BaxiDrv["BAXI terminal adapter\n(baxi.dll ActiveX)"]
    TcpServer["TCP server\nWinsock tcpserver:9002"]
    StatusPoller["Status poller\nWinsock status_poller:86"]
    Email["Email adapter\nMAPI (MSMAPI)"]
    DBAccess["ADO/Recordsets\n(DataEnvironment lpgnorge)"]
  end

  %% Local infrastructure
  subgraph SQL["SQL Server"]
    Betterm[(Controller DB\n\"betterm\")]
    POS[(POS/UNI DB\n\"butikkdata\")]
  end

  %% Hardware
  Dispenser["LPG dispenser\n(EHL over RS-485/serial)"]
  Container["Optional container controller\n(EHL addr +32)"]
  Printer["Receipt printer\n(serial)"]
  Pinpad["Local pinpad\n(4 buttons)"]
  RFID["RFID reader\n(serial)"]
  CardTerminal["Card terminal\n(BAXI)"]

  %% Connections
  Customer --> CardTerminal
  Customer --> Dispenser
  Operator --> Host
  RemoteClient <--> TcpServer
  Poller <--> StatusPoller

  UI <--> DispenserDrv
  UI <--> PrinterDrv
  UI <--> PinpadDrv
  UI <--> RFIDDrv
  UI <--> BaxiDrv
  UI <--> DBAccess
  UI <--> TcpServer
  UI <--> StatusPoller
  UI <--> Email

  DispenserDrv <--> Dispenser
  DispenserDrv <--> Container
  PrinterDrv <--> Printer
  PinpadDrv <--> Pinpad
  RFIDDrv <--> RFID
  BaxiDrv <--> CardTerminal

  DBAccess <--> Betterm
  DBAccess <--> POS
```

---

## Deployment view (ports + physical IO)

```mermaid
flowchart TB
  subgraph Host["Windows host: pumpcontrol.exe"]
    subgraph Ports["Network ports"]
      P9002["TCP 9002\ncontrol protocol"]
      P86["TCP 86\nprice poller"]
    end
    subgraph Serial["Serial/COM ports"]
      COMdisp["COM: dispenser (MSComm1)\nEHL frames"]
      COMbaxi["COM: card terminal (Baxi)\nserial + host IP 91.102.24.142:9670"]
      COMprn["COM: receipt printer (com_print)"]
      COMpin["COM: pinpad (com_pinpad)"]
      COMrfid["COM: RFID (RFIDCOM)"]
    end
    subgraph FS["Local files"]
      F1["C:\\deltefiler\\tankinger\\pump1<date>.txt\n(cash transactions)"]
      F2["C:\\pumpestyring\\stasjonskreditt\\tank<date>.txt\n(station credit trace)"]
      F3["C:\\pumpestyring\\baxilog\\\n(BAXI logs)"]
    end
    DB1[(SQL: betterm)]
    DB2[(SQL: butikkdata/POS)]
  end

  Client["Control client\n(VB6 Dispenserklient / others)"] <--> P9002
  Poller["Poller"] <--> P86

  COMdisp <--> Disp["EHL dispenser"]
  COMprn <--> Prn["Receipt printer"]
  COMpin <--> Pin["Pinpad"]
  COMrfid <--> Rfid["RFID reader"]
  COMbaxi <--> Baxi["BAXI terminal"]

  Host <--> DB1
  Host <--> DB2
```

---

## Core domain objects (UML-ish class view)

This is not the full DB schema; it’s the conceptual model the VB6 code operates on.

```mermaid
classDiagram
  class Tanking {
    +tankid
    +datostart
    +datostopp
    +liter
    +sum
    +betalingstype
    +presalg(bank_preselect)
    +tilbakesum
    +status
  }

  class BankReport {
    +reportid
    +dato
    +type
    +reporttext
    +cardnumber
  }

  class StationCreditTxn {
    +datostart
    +datostopp
    +unikundeid
    +unikontaktid
    +liter
    +pris
    +sum
    +rabatt
    +transferred
    +transferdato
  }

  class DispenserConfig {
    +dispensernr
    +pris
    +rs485adrcontainer?
    +autogasvarenr (POS export)
    +preselectionkeys (affects BAXI Ready_to_presum)
  }

  class TaskFlags {
    +zrapport:boolean
    +avstemming:boolean
    +unilink:boolean
  }

  class POSOrder {
    +ordrenr
    +kundenr
    +moms/mvagrunnlag/nettosum/totalsum
    +status
  }

  class POSOrderLine {
    +ordrenr
    +varenr
    +antall(liter)
    +enhet = LTR
    +pris
    +mva
  }

  Tanking "1" --> "0..*" BankReport : creates receipt/report
  DispenserConfig --> Tanking : pricing + addressing
  TaskFlags --> BankReport : triggers Z/settlement reports
  StationCreditTxn --> POSOrder : exported to POS as orders
  POSOrder "1" --> "1..*" POSOrderLine : contains
```

---

## State machine (PaymentType + key guards)

This is the main controller “mode switch”. It’s global state in VB6, but should become an explicit state machine in a reimplementation.

```mermaid
stateDiagram-v2
  [*] --> Idle

  Idle --> CashEnabled: TCP <TANK_DISP_UNBLOCK>\nor GUI cmdstart
  Idle --> BankPreselecting: pre_sum(amount)\n(pinpad buttons 100/200/400/600)
  Idle --> StationCreditChecking: RFID read (14 chars)\nif station credit enabled

  CashEnabled --> CashEnabled: Poll dispenser state/volume
  CashEnabled --> Finalizing: Tank end detected
  CashEnabled --> Idle: disp_block / timeout rollback

  BankPreselecting --> BankAwaitingTerminal: Baxi.TransferAmount_V2
  BankAwaitingTerminal --> BankProgrammingDispenser: baxi_OnLocalMode(result=0)\n(ok_to_opendisp=true)
  BankAwaitingTerminal --> Idle: baxi_OnLocalMode failure/return\n(or abort)

  BankProgrammingDispenser --> BankEnabled: set_preset_amount ok\n+ disp_unblock ok
  BankProgrammingDispenser --> BankRollback: dispenser comm failure
  BankRollback --> Idle: print error + Baxi_Reversal + disp_block

  BankEnabled --> BankEnabled: Poll dispenser state/volume
  BankEnabled --> Finalizing: Tank end detected
  BankEnabled --> TimeoutRollback: 120s no fueling\nwhile open for delivery
  TimeoutRollback --> Idle: Baxi_Reversal + disp_block

  StationCreditChecking --> StationCreditEnabled: DB match + disp_unblock ok
  StationCreditChecking --> Idle: no match or unblock fail
  StationCreditEnabled --> StationCreditEnabled: Poll dispenser state/volume
  StationCreditEnabled --> Finalizing: Tank end detected
  StationCreditEnabled --> Idle: timeout rollback + disp_block

  Finalizing --> Idle: write receipts/DB + reset flags
```

---

## Sequence: Bank preselect -> fueling -> completion -> cashback/reversal decision

This diagram is the “money path” you likely care most about re-implementing correctly.

```mermaid
sequenceDiagram
  autonumber
  actor Customer
  participant Pinpad as Pinpad (COM)
  participant App as VB6 Controller (Pumpekontroll)
  participant Baxi as BAXI Terminal (baxi.dll)
  participant Disp as EHL Dispenser (serial)
  participant Prn as Printer (serial)
  participant DB as SQL (betterm)

  Customer->>Pinpad: presses preset button (100/200/400/600)
  Pinpad->>App: com_pinpad_OnComm (key code)
  App->>App: pre_sum(amountDigits)\n(guards: PaymentType<=1, card enabled, not open, not inprogress)
  App->>Baxi: TransferAmount_V2(amountDigits)
  Baxi-->>App: baxi_OnPrinterText (receipt text)
  Baxi-->>App: baxi_OnDisplayText (UI + TCP message)
  Baxi-->>App: baxi_OnLocalMode(result=0)\ncard payment OK
  App->>DB: insert rapporter_bankterminal(Type=Forhåndsvalg)
  App->>Prn: print receipt (header + Baxi.PrintText)
  App->>Disp: set_preset_amount(amountDigits)\nretry <=30
  Disp-->>App: SetAmount ack (via MSComm1_OnComm)
  App->>Disp: disp_unblock (opcode 0x77)\nretry <=30
  Disp-->>App: DispUnblock observed in state polling

  Customer->>Disp: starts fueling
  loop Every state_timer tick
    App->>Disp: query state/error/volume/tank bits
    Disp-->>App: state + volume frames
    App->>DB: update rstankinger (in-flight)
  end

  Note over App,Disp: Tank end detected when volume stops changing under conditions

  App->>DB: finalize rstankinger (status, liters, sum)
  App->>Prn: print tank receipt\n(includes MVA and prepaid amount)
  alt bank_sum - tank_sum > ~0.24
    alt tank_sum < 0.5
      App->>Baxi: Administration 0x3134 (Reversal)
    else tank_sum >= 0.5
      App->>Baxi: TransferAmount_V2(type=0x31, cashback=delta*100)
    end
  else no cashback needed
    App->>App: clear bank state (PaymentType=0)
  end
```

---

## Sequence: Station credit (RFID) -> fueling -> POS export

```mermaid
sequenceDiagram
  autonumber
  actor Customer
  participant RFID as RFID Reader (COM)
  participant App as VB6 Controller (Pumpekontroll)
  participant DB as SQL (betterm)
  participant POS as POS/UNI DB (butikkdata)
  participant Disp as EHL Dispenser (serial)
  participant Prn as Printer (serial)

  Customer->>RFID: taps station card
  RFID->>App: RFIDCOM_OnComm (rfidstr[14])
  App->>App: checkstatcredit(rfidstr)\n(guards: enabled, PaymentType<=1)

  alt POSsystem=1
    App->>DB: query rsstasjonskort by kortnummer=rfidstr
  else POSsystem=0
    App->>DB: query rskortholder+kunder (active)
  end

  alt card authorized
    App->>Disp: disp_unblock retry<=30
    Disp-->>App: DispUnblock observed
    App->>App: PaymentType=3\nstatcred_start=Now
    Customer->>Disp: fuels
    loop Poll state/volume
      App->>Disp: query state/volume
      Disp-->>App: state + volume
    end
    App->>Prn: print station credit receipt
    App->>DB: insert rsstasjonskreditt (transferred=false)
  else not authorized
    App->>App: reset flags + remain idle
  end

  opt POS export job (every minute if POSsystem=1)
    App->>DB: filter rsstasjonskreditt where transferred=0
    App->>POS: insert rsordre + rsordrelinje\n(using autogasvarenr)
    App->>DB: mark transferred=true + transferdato=Now
  end
```

---

## Dispenser serial message framing (UML-ish)

This is the “codec” the VB6 app implements for EHL messages.

```mermaid
flowchart LR
  subgraph RX["Dispenser -> Controller (RX)"]
    A["Byte0: 0x20 (start)"] --> B["Byte1: length = N"]
    B --> C["Byte2: address (dispenser or container)"]
    C --> D["Byte3: message type (e.g., 69 vol, 75 state, 76 error)"]
    D --> E["Byte4..Byte(N-3): payload"]
    E --> F["Byte(N-2): XOR checksum of bytes 0..N-3"]
    F --> G["Byte(N-1): 0x36 (end)"]
  end
```

---

## Notes for re-implementation (what the diagrams imply)

- The VB6 app uses **event-driven callbacks** (serial receive, Winsock receive, BAXI events) plus **periodic polling** (timers). In a rewrite, keep the same concurrency model but make it explicit (actor/event loop).
- The “bank preselect” flow is a **distributed transaction** across BAXI + dispenser + DB; the rollback paths (`Baxi_Reversal` + `disp_block`) are critical and must be **idempotent**.
- The `PaymentType` global is effectively a coarse lock; model it as a **state machine** with explicit transitions and guards.


