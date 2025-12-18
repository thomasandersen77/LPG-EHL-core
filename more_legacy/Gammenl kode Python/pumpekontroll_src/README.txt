Pumpekontroll (VB6) – utdratt kildekode (fra Pumpestyring.zip)

Dette er en samlet "source bundle" av pumpekontroll-klienten slik den ligger i materialet du lastet opp.

Filer i pakken:
- pumpcontrol.vbp / pumpcontrol.vbw
  VB6-prosjektfil og workspace.

- pumpekontroll.frm / pumpekontroll.frx
  Hovedprogrammet "LPG Pumpekontroll" med MSComm1 (EHL/RS-485) og GUI.

- defs.bas
  Modul "Functions_defs" med hjelpefunksjoner (framing/kommando-ut, binærkonvertering m.m.).

- server.frm
  Dialog som skriver server.ini og setter DB/COM-porter.

- dispensere.frm / dispensere.frx
  Dispenser-/adressekonfig i UI.

- fra_dispenser.bas
  Dispenser-emulator (brukes for lokal testing uten ekte pumpa).

Kompilering / kjøring i VB6:
- Trenger VB6 IDE.
- Eksterne kontroller brukt i prosjektet:
  * MSCOMM32.OCX (Microsoft Comm Control 6.0)
  * MSDATGRD.OCX (DataGrid)
  * MSADODC.OCX (ADO Data Control)
  * evt. andre OCX som prosjektet refererer til
- Hvis du får feilen "Class MSCommLib.MSComm ... was not a loaded control class.",
  må MSCOMM32.OCX være installert og registrert (regsvr32).

NB:
- .frx-filer er binære ressurser som hører til .frm (må ligge ved siden av .frm).
