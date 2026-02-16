# VB6 Legacy Spec — `pumpcontrol` (Dispenserkontroll)

This document specifies the behavior implemented by the VB6 code under `norgesgass_legacy/` (primary project: `pumpcontrol.vbp`). It is written as a re-implementation spec for engineers.

**Primary artifacts analyzed**
- `norgesgass_legacy/pumpcontrol.vbp` (startup = `Sub Main`)
- `norgesgass_legacy/defs.bas` (`Sub Main`, comm helpers, DB, logging, printer header, error mapping)
- `norgesgass_legacy/pumpekontroll.frm` (main runtime logic: EHL dispenser, BAXI terminal, TCP server, timers, station credit)
- `norgesgass_legacy/email.bas` (MAPI email sending)

---

## Scope and purpose

The VB6 application is an **unattended LPG dispenser controller** that integrates:
- **Dispenser control (EHL)** over **serial/RS-485** via `MSComm`.
- **Card payment terminal (BAXI)** over serial via `baxi.dll` (including pre-authorization/preselection, reversal, cashback/return).
- **Receipt printing** over serial (ESC/POS-like control bytes).
- **Station credit (RFID)** over serial and DB lookups (local vs POS/UNI mode).
- **Local TCP control API** (client can set price, unblock/block, initiate manual bank ops, etc.).
- **Periodic tasks** (Z-report/settlement, sync station cards from POS system, export station credit to POS order tables).

In modern terms: a kiosk controller with hardware drivers + business workflow state machine.

---

## Runtime entrypoint and startup

### Entrypoint
- VB project `pumpcontrol.vbp` sets `Startup="Sub Main"`.
- `Sub Main` is in `norgesgass_legacy/defs.bas` (module `Functions_defs`).

### Configuration file: `server.ini`

At startup, `Sub Main` loads `server.ini` from `App.Path` (working directory).
- If missing: opens `serverinnstillinger` form (interactive configuration).
- If present: reads a single line and splits on `;` into `cfgline()`.

**`server.ini` fields (by index)**
0. `DBserver`
1. `DBdb`
2. `DBbrukernavn`
3. `DBpassord`
4. `Com_port` (dispenser `MSComm1`)
5. `Com_port_bank` (BAXI)
6. `com_port_print` (receipt printer)
7. `com_port_pinpad` (local pinpad with 4 fixed buttons)
8. `com_port_stcredit` (RFID reader)
9. `com_port_bank_baud` (BAXI serial baud)
10. `txtbankf1` (bank preselect amount #1, **string representing øre/cents**, used as raw digits)
11. `txtbankf2` (amount #2)
12. `txtbankf3` (amount #3)
13. `txtbankf4` (amount #4)
14. `feed_offset` (printer feed/cut parameter; used in ESC sequences)
15. `POSsystem` (0 = local station-credit DB, 1 = POS/UNI integration mode)
16. `DBserver_POS`
17. `DBdb_POS`
18. `DBbrukernavn_POS`
19. `DBpassord_POS`

### DB wiring (two logical DBs)

The app uses ADO and a “Data Environment” named `lpgnorge` (DataReport/DataEnvironment objects).

At the end of `Sub Main`:
- `lpgnorge.betterm.ConnectionString` points to `DBserver/DBdb`
- `lpgnorge.butikkdata.ConnectionString` points to `DBserver_POS/DBdb_POS`
- UI form `Pumpekontroll` is shown.

### Service dependency: SQL Server service loop

In `Pumpekontroll.Form_Load`:
- It loops via WMI until either `MSSQL$LPGNORGE` or `MSSQL$SQLEXPRESS` is in `RUNNING` state.
- After running, it sleeps 5 seconds before proceeding.

### Network listeners

On load:
- `tcpserver.Listen` (Winsock server) on **local port 9002**
- `status_poller.Listen` (Winsock) on **local port 86**

### Hardware initialization order (high level)

After DB is reachable (`db_ok()`):
- Open dispenser serial port (`cmdcom_on()`)
- Open printer serial port (`prn_comon()`)
- Open pinpad serial port (`pinpad_comon()`)
- Open RFID serial port (`RFID_comon()`)
- Open BAXI (`bank_comon()`)

Then:
- Open key recordsets (`rsdispensere`, `rsfirmainfo`, `rstasks`, `rstankinger`, etc.)
- Read company info (firmainfo) into globals for receipts.
- Load dispenser config from DB: `dispensernr`, `pris`, and optional `rs485adrcontainer`.
- Set dispenser price on hardware (`disp_setprice`).
- Send initial “pending transactions check” and reset.
- Start periodic timers: `state_timer`, `timeout_timer`, `Printer_state`, `task_timer`.

---

## Core state model (application-level)

### `PaymentType` (global)

The app’s workflow is gated by a global `PaymentType`:

- `0`: Idle / no active payment workflow
- `1`: Cash/manual release (unblock without bank)
- `2`: Bank card workflow (BAXI preselect + unblock)
- `3`: Station credit (RFID validated) workflow
- `4`: Manual bank transaction (set in some flows; effectively returns to 0)
- `5`: Virtual bank card (unused in main logic; returns to 0)
- `6`: Virtual station card (unused in main logic; returns to 0)

Constraints enforced in several places:
- If `PaymentType > 1`, manual unblock paths are blocked.
- RFID check only allowed if `PaymentType <= 1`.
- Preselect bank (`pre_sum`) only allowed if `PaymentType <= 1`, card payment enabled, dispenser not already open for delivery, and bank not already in progress.

### Tanking lifecycle flags

Key globals:
- `DISP_startbuttonpressed` (derived from dispenser state bits)
- `DISP_openfordelivery` (derived from dispenser state bits)
- `new_tank` (when a new delivery session has started)
- `tank_end` (set true when volume stops changing under end conditions)
- `Disp_was_unblocked` (used to recover from “unblock but start never happened”)
- `tanktimeout_count` (counts seconds from unblock if user doesn’t start)

---

## Interfaces and protocols

### 1) EHL dispenser protocol over serial (RS-485)

#### Frame format (as implemented)

Two distinct “start bytes” are used:
- Controller -> dispenser: start byte **0x10** (`&H10`)
- Dispenser -> controller: start byte **0x20** (`32`)

The VB6 receive loop (`MSComm1_OnComm`) accepts a message when:
- `x(0) == 0x20`
- `x(1) == (u + 1)` where `x(1)` is a length byte and `u` is the last index
- `x(u) == 0x36` (end byte)

Checksum:
- `checksum = XOR(all bytes from x(0) .. x(u-2))`
- Valid when `checksum == x(u-1)`

Addressing:
- Dispenser address used is `dispnr(0) = (db.dispensernr + 32)`
- Optional container address used is `(db.rs485adrcontainer + 32)`

#### Commands sent by controller

All commands end with `0x36`.

Common controller-to-dispenser commands (from `defs.bas` and `pumpekontroll.frm`):
- **Reset**: opcode `0x81` (frame length 0x06)
- **Check pending transactions**: opcode `0xC5` (frame length 0x06)
- **Set price (init step)**: opcode `0xC3` then a second frame with opcode `0xA9` carrying ASCII digits of price
- **Query state**: opcode `0x4B`
- **Query error**: opcode `0x4C`
- **Query volume**: opcode `0x45`
- **Query tank bits**: opcode `0xC5` (same opcode as “pending” check; behavior differs by device response type code)
- **Block dispenser (stop)**: opcode `0x69`
- **Unblock dispenser (start delivery mode)**: opcode `0x77`
- **Program preset amount**: opcode `0x75` with ASCII digits of the amount (øre)
- **(Optional) Unblock with liter limit**: opcode `0x70` with 6 ASCII digits (currently disabled in TCP handler)

#### Responses processed by controller

The response “type” is in `x(3)` (for frames addressed to a specific device).

Implemented response types of interest:
- `69` (volume): updates `tank_vol` from digits `x(4..8)` interpreted as a decimal string; used to compute `tank_sum = tank_vol * tank_unitprice`.
- `75` (state): updates an 8-bit state string derived from `decimaltobinn(x(4))`.
  - Bit usage as coded:
    - `Mid(state_string, 6, 1) == "1"` => start button pressed
    - `Mid(state_string, 7, 1) == "1"` => open for delivery
    - `Mid(state_string, 5, 1) == "1"` => automode
- `76` (error): maps errors and logs (and for container, logs via `logdisp_err`).
- `197` (tank bits): used to detect “unaccounted” and “finished due powerfault” states for container; the dispenser variant also exists but is not fully documented here.

**Important derived behaviors**
- **New tank start detection**: when state indicates start pressed + open for delivery, and `new_tank == False`, the controller:
  - creates a new `rstankinger` record with `datostart = Now()`
  - stores payment type + bank preselection amount (if bank)
  - resets volume/sum accumulators
- **Tank end detection (volume-based)**: when a new volume frame is identical to the last one and either:
  - a transaction is marked “unaccounted”, OR
  - “virtual release start” UI flag is enabled (`Check2.Value = 1`)
  
  …then the controller treats the delivery as finished and finalizes the transaction.

---

### 2) BAXI payment terminal integration (`baxi.dll`)

#### Connectivity/config

From `bank_comon()`:
- Configures `Baxi.CommPort = Com_port_bank`, `Baxi.BaudRate = com_port_bank_baud`
- `Baxi.HostIpAddress = "91.102.24.142"`
- `Baxi.HostPort = "9670"`
- Sets display and printer widths to 24
- Calls `Baxi.Open`

Logging:
- `Baxi.LogFilePath = "C:\pumpestyring\baxilog\"`
- `Baxi.LogFilePrefix = "Baxilog"`
- `Baxi.TraceLevel = <selected>`

#### Core API calls used

- **Preselect / sale initiation**:
  - `Baxi.TransferAmount_V2 "0000", &H30, CLng(amount), &H30, 0, &H30, 0, "LPG Autogas", ""`
  - `amount` is a string of digits representing **øre/cents**; UI shows `bank_sum = amount/100`.

- **Cashback / return**:
  - `Baxi.TransferAmount_V2 "0000", &H31, CLng(cashback_ore), &H30, 0, &H30, 0, "Return LPG Autogas", ""`
  - In VB: `cashback` is computed in NOK then multiplied by 100 before sending.

- **Reversal / annulment**:
  - `Baxi.Administration &H3134`

- **Settlement / reconciliation**:
  - `Baxi.Administration &H3130, 0` (avstemming)
  - `Baxi.Administration &H3137, 0` (Z-rapport)

Other:
- `Baxi.Administration &H3132` (cancel/abort current?)
- `Baxi.Administration &H313E` (software update)
- `Baxi.Administration &H313F, 0` (download “kortavtaler” dataset)

#### Event handling semantics (must be replicated)

`baxi_OnPrinterText`:
- Updates `reporttext = Print_reciept_header & Baxi.PrintText`

`baxi_OnDisplayText`:
- Pushes terminal display line to TCP clients via `<TANK_TERMINAL_MESSAGE>;...;<SLUTT>`

`baxi_OnLocalMode(result, IssuerID)` is the **primary state transition**:

- `result == 0`: “financial completed” (success)
  - If `return_amount == False` (normal preselect):
    - Create a `rapporter_bankterminal` record of type `"Forhåndsvalg"`
    - `rapport_rs.cardnumber = Baxi.CardData`
    - Set `ok_to_opendisp = True`
    - Set `PaymentType = 2`
  - Else (return workflow):
    - Record type is `"Annulering"` if receipt contains “ANNULLERING”; else `"Tilbakeføring"`
    - Reset UI colors; `ok_to_opendisp = False`, `return_amount = False`, `PaymentType = 0`
  - Always prints the receipt and stores a sanitized `reporttext` into DB.

- `result == 1`: “print/report available” (used for Z-rapport, avstemming)
  - Stores report into `rapporter_bankterminal`.
  - For Z-rapport:
    - Reads all unreported “technical cashback” rows (`rscashback`) and appends a section to the Z-report.
    - Marks those cashback rows as reported.
    - Stores daily totals in `rssalgstall` and emails the Z-report.

- `result == 2`: “terminal” (post-transaction)
  - Writes terminal report to `rapporter_bankterminal`.
  - If `return_amount == True` AND `manual_bank == False`, it triggers `Technical_Cashback(bank_sum2 - tank_sum2)`.
  - Resets `PaymentType = 0`, clears flags, clears UI preselect button colors.

`baxi_OnError`:
- Maps numeric error codes to human strings and marks `baxierror = True`.
- A periodic `Printer_state_Timer` checks `Baxi.Active`; if not active it calls `restart_baxi()` (Close -> sleep 5s -> Open).

#### Bank preselect -> unblock workflow (`pre_sum`)

Inputs:
- `amount`: digit string representing øre/cents (ex: `"000100"` for NOK 1.00, exact formatting depends on configuration).

Guards:
- Exit if `PaymentType > 1` OR card payment disabled OR dispenser already open OR bank already in progress.

Process:
1. Set `bank_inprogress = True`, `bank_sum = amount/100`, `Bank_answer = False`.
2. If “virtual release” (`frigi_bank`) is enabled: skip financial request, set `PaymentType = 2` and proceed.
3. Otherwise send `TransferAmount_V2(..., amount, ...)` to start bank flow.
4. Wait in a tight loop until `Bank_answer` is set (by `baxi_OnLocalMode`).
5. If `ok_to_opendisp == True`:
   - Try up to 30 times to program preset amount to dispenser (`set_preset_amount amount`), waiting for `SetAmount == True`.
   - Then try up to 30 times to `disp_unblock`, waiting for `DispUnblock == True`.
6. If any dispenser step fails:
   - Print an error receipt informing customer the prepayment is annulled.
   - Trigger `Baxi_Reversal()`.
   - Block and reset dispenser.

---

### 3) Station credit (RFID) workflow

RFID input:
- Read from `RFIDCOM` serial port; expected string length **14**, truncated via `Left(rfid_string, 14)`.

Eligibility:
- Only if station credit enabled (`Check3.Value == 1`)
- Not already checking (`checkkreditt == False`)
- `PaymentType <= 1` (i.e., not in bank workflow)

Authorization (`checkstatcredit(rfidstr)`):

Two modes:

#### POS mode (`POSsystem == 1`)
Data source:
- `rsstasjonskort` (a local buffer table in the “betterm” DB) is filtered by `kortnummer == rfidstr`.

If exactly one record:
- Extract:
  - `stationcredit_custno = kundeid`
  - `stationcredit_contactid = kortholderid`
- Attempt to `disp_unblock` up to 30 times until `DispUnblock == True`.
- On success:
  - `PaymentType = 3`
  - `statcred_start = Now()`
  - Send TCP message `<TANK_TERMINAL_MESSAGE>;Stasjonskreditt;<SLUTT>`
  - Write a debug trace file:
    - `C:\pumpestyring\stasjonskreditt\tank<dd><mm><yyyy><HH><MM>.txt`
    - Content includes rfid, customer/contact ids, paymentType, openfordelivery flag, retries.
- On failure: log, set `checkkreditt = False`.

If not found:
- Send TCP message containing the scanned RFID.
- Reset `PaymentType = 0`, `checkkreditt = False`.

#### Local mode (`POSsystem == 0`)
Data source:
- `rskortholder` filtered by `kortnummer == Trim(rfidstr)` and `Aktiv=1`
- Then `rskunder` filtered by `kundeid` and `Aktiv=1`

If exactly one active match:
- Same unblock loop and success/failure behavior as POS mode.
Else:
- Emit `<TANK_TERMINAL_MESSAGE>` with RFID (if length==14), reset flags.

#### Station credit settlement (on tank end)

When a tank ends under `PaymentType == 3`:
- Prints a receipt and inserts a `rapporter_bankterminal` record with Type `"Stasjonskreditt"`.
- Inserts a row into `rsstasjonskreditt` table with:
  - `datostart = statcred_start`
  - `unikundeid = stationcredit_custno`
  - `unikontaktid = stationcredit_contactid`
  - `liter`, `pris`, `sum`
  - `Status = 4`
  - `datostopp = Now()`
  - `transferred = False`
  - `rabatt`: either `statcred_rabatt` (POS mode) or `rskunder.rabatt` (local mode)

Additionally, it writes a daily file:
- `C:\pumpestyring\stasjonskreditt\tank<dd><mm><yyyy>.txt`
- Content includes station credit ids and amount data.

#### POS export job (every minute)

`task_timer_Timer` calls `Check_and_import_order` when `POSsystem == 1`.

`Check_and_import_order`:
- Filters `rsstasjonskreditt` rows where `transferred=0`.
- For each row with `liter > 0`, it creates:
  - an order record in `rsordre` (POS DB) with customer address lookup via `rsNavnaddress` (kontonr = unikundeid) and contact name via `butikkdata.Execute("Select Name from c_contacts where id=unikontaktid")`
  - an order line in `rsordrelinje` with:
    - `varenr = rsdispensere.autogasvarenr`
    - `antall = liter`, `enhet = "LTR"`
    - revenue/tax fields computed from `MVA` constant
    - `innpris` looked up in `rsvarer` (if missing: set innpris=0 and log)
- Marks each station-credit row:
  - `transferred = True`
  - `transferdato = Now`

---

### 4) Receipt printer protocol

Printer I/O is via `com_print` serial port.

The system prints:
- Bank receipts (`reporttext`) with a header from `Print_reciept_header`.
- Tanking receipts for bank and station credit (includes MVA breakdown).
- Status queries:
  - paper low query: `ESC 0x05 0x02`
  - status query: `ESC 0x05 0x01`

`com_print_OnComm` parses printer responses to set:
- printer OK vs error
- paper low state
- specific printer error messages (presenter module jam, cutter jam, out of paper, etc.)

Printer status is also forwarded over TCP:
- `<PRINTERSTATE>;<message>;<SLUTT>`

---

### 5) Local TCP control protocol (client <-> controller)

Server:
- `tcpserver` listens on **port 9002**.
- All messages are delimited by semicolons; many end with sentinel `<SLUTT>`.

**Inbound commands implemented**

- **Restart controller host (Windows restart)**:
  - Request: `<RESTART>;<SLUTT>`
  - Only allowed if `PaymentType == 0` and `bank_inprogress == False`
  - Response:
    - success: `<RESTART>;RESTART IVERKSETTES OM<30sek;<SLUTT>`
    - blocked: `<RESTART>;RESTART IKKE MULIG PGA TERMINAL OPPTATT;<SLUTT>`

- **Set station runtime toggles**:
  - Request: `<STATIONSTATE>;<which>;<value>;<SLUTT>`
  - `which`:
    - `1` => day mode (`chkdagmodus`)
    - `2` => card payment enabled (`Check1`)
    - `3` => station credit enabled (`Check3`)

- **Manual dispenser unblock (cash mode)**:
  - Request: `<TANK_DISP_UNBLOCK>;<optional amount>;<SLUTT>`
  - If `PaymentType > 1`: responds `<STATUS>;Dispenser opptatt;<SLUTT>`
  - Else:
    - sets `PaymentType = 1`
    - calls `disp_unblock` (optionally with parameter; currently unused)
    - responds `<TANK_DISP_UNBLOCK>;OK;<SLUTT>`

- **Manual dispenser stop/block**:
  - Request: `<TANK_DISP_STOP>;<SLUTT>`
  - Calls `disp_block`, responds `<TANK_DISP_STOP>;OK;<SLUTT>`

- **Manual bank sale**:
  - Request: `<BANK_CASH>;<amountDigits>;<SLUTT>`
  - Calls `cash(amountDigits)` and sets `manual_bank = True`.

- **Manual bank cashback/return**:
  - Request: `<BANK_CASHBACK>;<amountInOreDigits>;<SLUTT>`
  - Calls `cashback(amount/100)` and sets `manual_bank=True`, `bank_inprogress=True`.

- **Update dispenser price**:
  - Request: `<PRICE>;<priceFloat>;<SLUTT>`
  - Writes price to `rsdispensere.pris` if > 1, updates hardware price (`disp_setprice`).
  - Response: `<PRICE>;OK;<SLUTT>` or `<PRICE>;ERROR;Pris for liten;<SLUTT>`

**Outbound messages**
- `<TANK_TERMINAL_MESSAGE>;...;<SLUTT>` (BAXI display text; station credit messages; timeout messages)
- `<STATE>;<8bit_state_string>;<SLUTT>`
- `<TANK_STOP>;<SLUTT>`
- `<TANK>;<belop>;<liter>;<unitprice>;{isBank};{bankSum};<SLUTT>` (sent periodically from `state_timer`)
- `<PRINTERSTATE>;<message>;<SLUTT>`

Client project:
- There is a separate VB6 client (`norgesgass_legacy/Dispenserklient/Dispenserkontroll.frm`) that connects and consumes the above messages.

---

### 6) Status poller (port 86)

The controller also listens on port **86** and responds to a single request:
- Request: `<GETAUTOGASPRICE>`
- Response: `<02;<price>>` if connected

This appears intended for a simple external polling host.

---

## Transaction completion rules

### Cash (PaymentType=1)
On tank end:
- Writes a line file: `c:\deltefiler\tankinger\pump1<dd><mm><yyyy>.txt` containing `<liter>;<unitprice>`
- Resets `PaymentType = 0`.

### Bank (PaymentType=2)
On tank end:
- Creates a tank receipt (“Tankkvittering”) in `rapporter_bankterminal` with:
  - liters, unit price, sum, MVA breakdown
  - bank preselected amount
  - amount due back = `bank_sum - tank_sum`
- If `bank_sum` exceeds `tank_sum` by more than ~NOK 0.24:
  - Marks tank as “needs return” (DB status set to 4) and sets `tilbakesum`.
  - If `tank_sum < 0.5`: triggers full reversal.
  - Else: triggers cashback (return) for the delta.
- If not: clears bank state, returns to idle.

If dispenser communication fails after preselect:
- Prints error message and triggers `Baxi_Reversal`, then blocks dispenser.

### Station credit (PaymentType=3)
On tank end:
- Prints station credit receipt.
- Writes/records station-credit sale into DB (`rsstasjonskreditt`) for later export.
- Returns to idle.

---

## Timeout and rollback behavior

Timer: `timeout_timer` fires every 1 second.

If:
- `tank_vol == 0` AND `PaymentType > 0` AND `DISP_openfordelivery == True`

…then the controller increments `tanktimeout_count`.

When `tanktimeout_count >= tank_timeout` (120 seconds):
- For cash: sends client message “dispenser blocked due timeout”
- For bank: triggers `Baxi_Reversal` and sends “bank transaction annulled due timeout”
- For station credit: resets check flags and sends “station credit blocked due timeout”
- In all cases: blocks and resets dispenser (`disp_block`)

This is the “user didn’t start fueling after being enabled” safety.

---

## Data persistence: DB tables used (observed)

This is not an exhaustive schema, but these tables are semantically required:

**Controller DB (“betterm”)**
- `dispensere` (dispensernr, pris, rs485adrcontainer, autogasvarenr, preselectionkeys, preselectionamount)
- `tankinger` (tankid, datostart, datostopp, liter, sum, presalg, betalingstype, status, tilbakesum)
- `rapporter_bankterminal` (dato, type, reporttext, cardnumber, reportid/identity)
- `firmainfo` (company identity + email recipients + Technical_Email)
- `tasks` (zrapport boolean, avstemming boolean, unilink boolean)
- `logs` (event stream written by `LogEvent`)
- `stasjonskort` (local cache of card-number -> customer/contact mapping)
- `stasjonskreditt` (station credit transactions, with transferred flags)
- `cashback` (technical cashback bookkeeping; reported flag)
- `salgstall` / `stdagensomsetning` / `dbo_oms_manuell` (daily totals)
- `kunder`, `kortholder` (local station credit mode)

**POS DB (“butikkdata” / UNI)**
- `stasjonskred` (source for station card sync; read by `oppdaterstasjonskort_Click`)
- `c_contacts` (contact name lookup)
- `ordre`, `ordrelinje`, `Navnaddress`, `varer` (order export targets)

---

## Re-implementation guidance (non-functional)

To re-implement safely:
- Treat each hardware interface as its own adapter with clear timeouts and retries.
- Replace VB6 busy-wait loops (`DoEvents`) with bounded async waits and explicit state transitions.
- Persist critical state to DB (or durable log) so that process restarts do not lose:
  - “bank preselect started” and “dispenser unblocked” situations
  - pending cashback/reversal requirements
- Make rollback semantics explicit and idempotent:
  - “block dispenser + reset” is safe to re-run
  - “reversal” vs “cashback” must be deduplicated (transaction IDs)

---

## Out-of-scope / unknowns

Some behaviors are present but not fully specified here because they depend on:
- The exact EHL protocol documentation (meaning of each bit/byte beyond what the code uses).
- Full DB schema definitions (VB6 uses DataEnvironment recordsets, not explicit DDL in this repo).
- `baxi.dll` contract and exact meanings of `LocalMode` result codes beyond observed usage.

If you want, I can extend this spec by:
- Extracting a full message map of all `x(3)` response codes implemented in `MSComm1_OnComm`.
- Writing a formal state machine diagram (Mermaid) for the bank + dispenser combined workflow.


