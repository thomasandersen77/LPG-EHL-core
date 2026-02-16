
# 1) Finn alt som lukter seriell
## rg -n "MSComm|CommPort|Settings\s*=|OnComm|InputLen|RThreshold|SThreshold|PortOpen"

decompiled/Pushservice.decompiled.cs
261:		internal static MySettings Settings => MySettings.Default;

CSharpConverted-V2/omsetning_form.cs
296:                PrinterSettings = new PrinterSettings()
340:                    PrinterSettings = ps,

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/pumpekontroll.log
1:Line 15: Class MSCommLib.MSComm of control MSComm1 was not a loaded control class.

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/fra_dispenser.bas
33:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
59:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8))
86:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10))
107:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
127:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
148:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
171:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
198:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
228:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14))
258:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14)) + Chr(y(15)) + Chr(y(16))
279:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
300:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/pumpekontroll.frm
17:   Begin MSCommLib.MSComm com_print
23:      CommPort        =   11
25:      InputLen        =   1
27:      RThreshold      =   1
28:      SThreshold      =   1
302:   Begin MSCommLib.MSComm MSComm1
309:      InputLen        =   1
311:      RThreshold      =   1
718:Private Sub com_print_OnComm()
808:    com_print.CommPort = com_port_print
809:    com_print.PortOpen = True
832:If com_print.PortOpen Then com_print.PortOpen = False
876:If MSComm1.PortOpen = False Then Exit Sub
923:MSComm1.CommPort = Com_port
928:If MSComm1.PortOpen = False Then MSComm1.PortOpen = True '
942:If MSComm1.PortOpen = True Then MSComm1.PortOpen = False
943:'Set MSComm1 = Nothing
954:Private Sub MSComm1_OnComm()
956:Select Case MSComm1.CommEvent
959:    Case comEvReceive   ' Received RThreshold # of
966:    charstr = MSComm1.Input

Pumpestyring 2/Pumpestyring/Pumpestyring/fra_dispenser.bas
33:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
59:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8))
86:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10))
107:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
127:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
148:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
171:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
198:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
228:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14))
258:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14)) + Chr(y(15)) + Chr(y(16))
279:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
300:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/uttaksrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Stasjonskredittrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Omsetningprdag.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/uttaksrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Stasjonskredittrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Omsetningprdag.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/uttaksrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/pumpekontroll.log
1:Line 15: Class MSCommLib.MSComm of control MSComm1 was not a loaded control class.

Pumpestyring 2/Pumpestyring/Pumpestyring/defs.bas
299:    Pumpekontroll.RFIDCOM.CommPort = com_port_stcredit
300:    Pumpekontroll.RFIDCOM.PortOpen = True
320:    Pumpekontroll.com_pinpad.CommPort = com_port_pinpad                     'Disse setningene m�flyttes.
321:    Pumpekontroll.com_pinpad.PortOpen = True  '
344:    Pumpekontroll.com_print.CommPort = com_port_print
345:    Pumpekontroll.com_print.PortOpen = True  '
368:        .CommPort = Com_port_bank
535:If Pumpekontroll.MSComm1.PortOpen Then Pumpekontroll.MSComm1.Output = commstr
542:If Pumpekontroll.com_print.PortOpen Then Pumpekontroll.com_print.Output = prnstr

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/fra_dispenser.bas
33:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
59:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8))
86:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10))
107:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
127:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
148:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
171:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
198:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
228:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14))
258:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14)) + Chr(y(15)) + Chr(y(16))
279:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
300:                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/pumpekontroll.frm
17:   Begin MSCommLib.MSComm com_print
23:      CommPort        =   11
25:      InputLen        =   1
27:      RThreshold      =   1
28:      SThreshold      =   1
302:   Begin MSCommLib.MSComm MSComm1
309:      InputLen        =   1
311:      RThreshold      =   1
718:Private Sub com_print_OnComm()
808:    com_print.CommPort = com_port_print
809:    com_print.PortOpen = True
832:If com_print.PortOpen Then com_print.PortOpen = False
876:If MSComm1.PortOpen = False Then Exit Sub
923:MSComm1.CommPort = Com_port
928:If MSComm1.PortOpen = False Then MSComm1.PortOpen = True '
942:If MSComm1.PortOpen = True Then MSComm1.PortOpen = False
943:'Set MSComm1 = Nothing
954:Private Sub MSComm1_OnComm()
956:Select Case MSComm1.CommEvent
959:    Case comEvReceive   ' Received RThreshold # of
966:    charstr = MSComm1.Input

Pumpestyring 2/Pumpestyring/Pumpestyring/Class1.cls
17:Public WithEvents MScomm1 As MSComm

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Stasjonskredittrapport.Dsr
30:   _Settings       =   7

Pumpestyring 2/Pumpestyring/Pumpestyring/pumpekontroll.frm
349:   Begin MSCommLib.MSComm RFIDCOM
355:      CommPort        =   6
357:      InputLen        =   16
359:      RThreshold      =   16
360:      SThreshold      =   1
542:   Begin MSCommLib.MSComm com_pinpad
548:      CommPort        =   4
550:      RThreshold      =   1
551:      SThreshold      =   1
553:   Begin MSCommLib.MSComm com_print
559:      CommPort        =   5
561:      InputLen        =   1
563:      RThreshold      =   1
564:      SThreshold      =   1
606:   Begin MSCommLib.MSComm MSComm1
612:      CommPort        =   3
614:      InputLen        =   1
616:      RThreshold      =   1
1290:    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
1368:    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
1569:   If com_print.PortOpen Then com_print.Output = reporttext
1711:Private Sub com_pinpad_OnComm()
1740:Private Sub com_print_OnComm()
2033:If com_print.PortOpen Then com_print.PortOpen = False
2034:If com_pinpad.PortOpen Then com_pinpad.PortOpen = False
2035:If RFIDCOM.PortOpen Then RFIDCOM.PortOpen = False
2037:If MSComm1.PortOpen Then MSComm1.PortOpen = False
2142:If com_print.PortOpen Then com_print.Output = RichTextBox1.Text & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(40)
2172:Private Sub RFIDCOM_OnComm()
2342:If Not MSComm1.PortOpen Then Exit Sub
2497:MSComm1.CommPort = Com_port
2502:If MSComm1.PortOpen = False Then MSComm1.PortOpen = True '
2511:Private Sub MSComm1_OnComm()
2515:Select Case MSComm1.CommEvent
2518:   Case comEvReceive   ' Received RThreshold # of
2527:170    charstr = MSComm1.Input
2591:840                             If com_print.PortOpen Then com_print.Output = reporttext
2643:12005                                If com_print.PortOpen Then com_print.Output = reporttext
3491:If RFIDCOM.PortOpen Then RFIDCOM.Output = "c"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Omsetningprdag.Dsr
30:   _Settings       =   7


# 2) Finn alt som lukter socket/TCP (winsock)
## rg -n "Winsock|Socket|connect|recv|send|TCP|IP|port|NPort|RealPort|Moxa|Digi" 

rg -n "Winsock|Socket|connect|recv|send|TCP|IP|port|NPort|RealPort|Moxa|Digi"
Dispenserkontroll_Ready/ProtocolSpec_NO.md
3:**Transport:** TCP-klient (UTF‑8) til `clientsrv_local:clientsrv_localtcpport`  
7:## Klient → Server (sendt fra programmet)
10:  *TAG14* = første 14 tegn fra RFID-inndata via serieport.
23:  Koden sender `"<PRICE>;<{pris}>; <SLUTT>"` (merk den noe uvanlige formateringen i kilden). Praktisk anbefaling er å tolke dette som `"<PRICE>{pris};<SLUTT>"` server‑side.
47:## Serieport (RFID)
48:- Portnavn lastes fra `settings.ini` (`client_rfidcomport=COM5`). Hastighet 9600 bps.  
49:- Inndata strippes og avkortes til 14 tegn før videresending.
55:3. `connection.txt` i programkatalogen
58:I tillegg forsøker programmet å lese `clientsrv_local` og `clientsrv_localtcpport` fra databasen (`Settings`‑tabell) før det faller tilbake til `settings.ini`.

Dispenserkontroll_Ready/README_QuickStart_NO.md
8:- Valgfritt: SQL Server (hvis rapporter/datavisning skal fungere)
11:1. Kopiér `appsettings.example.json` til `appsettings.json` og sett korrekt forbindelse til databasen (eller la stå tom hvis du kun vil teste TCP‑protokollen).
13:   - `clientsrv_local` og `clientsrv_localtcpport`
14:   - `client_rfidcomport` hvis du har RFID‑leser
18:1. Kjør referanseserveren: `python reference_server.py` (krever Python 3.9+). Den lytter på port 5000 og viser meldinger den mottar fra klienten.
19:2. Sett `clientsrv_local=127.0.0.1` og `clientsrv_localtcpport=5000` i `settings.ini`.
23:- **Ikke** sjekk inn ekte passord i `appsettings.json`. Bruk miljøvariabelen `DB_CONNECTION` eller `connection.txt` (med korrekte tilgangsrettigheter).
24:- TCP‑trafikken er i klartekst – vurder VLAN/VPN/segmentering i produksjon.
27:- Programmet forsøker å hente `clientsrv_local`/`clientsrv_localtcpport` fra tabellen `Settings` i databasen hvis tilgjengelig; ellers fra `settings.ini`.
28:- Enkel reconnection‑mekanisme er aktiv – ved nettbrudd prøver programmet å koble til igjen etter en kort stund.

Dispenserkontroll_Ready/reference_server.py
2:import socket, threading
10:        # Eksempel: send restart/velkomst
12:        conn.sendall(msg.encode("utf-8"))
15:            data = conn.recv(4096)
33:                    conn.sendall(f"<TANK>;0.00;0.00;0.00;0;Vennligst start;<SLUTT>".encode("utf-8"))
39:                    conn.sendall(b"<STATE_TANK>;1;<SLUTT>")
40:                    conn.sendall(b"<TANK>;123.45;6.78;18.22;1;Betaling OK;<SLUTT>")
43:                    conn.sendall(b"<TANK_STOP>;<SLUTT>")
46:                    conn.sendall(b"<PRICE>;OK;<SLUTT>")

decompiled/Pushservice.decompiled.cs
24:using ModbusTCP;
374:		private short Modbus_port;
557:						val.CommandText = "Select * from information_schema.columns where table_name='Settings' and column_name='Modbusport'";
560:						writelog("Sjekket om modbusport eksisterer");
569:							val.CommandText = "Alter table settings add modbusport int NULL DEFAULT 502";
571:							writelog("Opprettet modbusport");
678:					myCmd.CommandText = "Select modbusaddress,modbusport,locationid,volpush,pricepush from settings";
705:					if (Information.IsDBNull(RuntimeHelpers.GetObjectValue(myReader["modbusport"])))
707:						Modbus_port = 0;
711:						Modbus_port = Conversions.ToShort(myReader["Modbusport"]);
721:					writelog("ADAM:" + Modbus_address + " Port:" + Conversions.ToString((int)Modbus_port) + " Location: " + Conversions.ToString((int)Station_Locationid) + " Timeout:" + Conversions.ToString((uint)mb.timeout) + " ms");
744:				if ((Operators.CompareString(Modbus_address, "", false) != 0) & (Modbus_port > 0) & (volpush == 1))
891:					writelog("Modbus: exception not connected");
894:					writelog("Modbus: exception connection lost");
903:					writelog("Modbus: exception send failt");
977:					mb.disconnect();
983:		private void aTimer_Elapsed(object sender, ElapsedEventArgs e)
989:				if (mb.connected)
991:					writelog("modbus connectstate:connected");
995:					writelog("modbus connectstate:disconnected");
996:					mb.disconnect();
997:					mb.connect(Modbus_address, checked((ushort)Modbus_port));
999:				while (!(mb.connected | mb_error))
1114:		private void pTimer_Elapsed(object sender, ElapsedEventArgs e)
1173:		private void utimer_Elapsed(object sender, ElapsedEventArgs e)

Moss/Dispenserkontroll.runtimeconfig.json
15:      "System.Reflection.Metadata.MetadataUpdater.IsSupported": false,

Dispenserkontroll_Ready/settings.ini.example
2:# Nettverksserver som Dispenserkontroll skal koble til (TCP)
4:clientsrv_localtcpport=5000
6:# Serieport (RFID-leser). Bruk din faktiske COM-port (f.eks. COM3)
7:client_rfidcomport=COM5

Dispenserkontroll_Ready/bin/Dispenserkontroll.deps.json
296:      "path": "runtime.linux-arm.runtime.native.system.io.ports/6.0.0",
297:      "hashPath": "runtime.linux-arm.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
303:      "path": "runtime.linux-arm64.runtime.native.system.io.ports/6.0.0",
304:      "hashPath": "runtime.linux-arm64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
310:      "path": "runtime.linux-x64.runtime.native.system.io.ports/6.0.0",
311:      "hashPath": "runtime.linux-x64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
317:      "path": "runtime.native.system.io.ports/6.0.0",
318:      "hashPath": "runtime.native.system.io.ports.6.0.0.nupkg.sha512"
324:      "path": "runtime.osx-arm64.runtime.native.system.io.ports/6.0.0",
325:      "hashPath": "runtime.osx-arm64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
331:      "path": "runtime.osx-x64.runtime.native.system.io.ports/6.0.0",
332:      "hashPath": "runtime.osx-x64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
345:      "path": "system.io.ports/6.0.0",
346:      "hashPath": "system.io.ports.6.0.0.nupkg.sha512"

Pumpestyring 2/Pumpestyring/Pumpestyring/SQLBASE_SCRIPT/LPGNORGE ALL SCRIPTS 160311.sql
67:/****** Object:  Table [dbo].[rapporter_bankterminal]    Script Date: 03/16/2011 09:24:29 ******/
74:CREATE TABLE [dbo].[rapporter_bankterminal](
75:	[reportid] [numeric](18, 0) IDENTITY(1,1) NOT NULL,
76:	[reporttext] [text] NULL,
89:	[zrapport] [bit] NULL,
90:	[xrapport] [bit] NULL,
202:SELECT * from rapporter_bankterminal where day(dato)=@dag and month(dato)=@mnd and year(dato)=@aar order by dato

Moss/Dispenserkontroll.deps.json
344:      "path": "runtime.linux-arm.runtime.native.system.io.ports/6.0.0",
345:      "hashPath": "runtime.linux-arm.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
351:      "path": "runtime.linux-arm64.runtime.native.system.io.ports/6.0.0",
352:      "hashPath": "runtime.linux-arm64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
358:      "path": "runtime.linux-x64.runtime.native.system.io.ports/6.0.0",
359:      "hashPath": "runtime.linux-x64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
365:      "path": "runtime.native.system.io.ports/6.0.0",
366:      "hashPath": "runtime.native.system.io.ports.6.0.0.nupkg.sha512"
372:      "path": "runtime.osx-arm64.runtime.native.system.io.ports/6.0.0",
373:      "hashPath": "runtime.osx-arm64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
379:      "path": "runtime.osx-x64.runtime.native.system.io.ports/6.0.0",
380:      "hashPath": "runtime.osx-x64.runtime.native.system.io.ports.6.0.0.nupkg.sha512"
421:      "path": "system.io.ports/6.0.0",
422:      "hashPath": "system.io.ports.6.0.0.nupkg.sha512"

CSharpConverted-V2/TcpClientWrapper.cs
2:using System.Net.Sockets;
12:        private readonly int _port;
18:        public TcpClientWrapper(string host, int port)
21:            _port = port;
31:            await _client.ConnectAsync(_host, _port);

Pumpestyring 2/Pumpestyring/Pumpestyring/administration.frm
25:   Begin MSDBCtls.DBCombo zrapportkopi
36:   Begin MSDBCtls.DBCombo xrapportkopi
65:      Caption         =   "Z-Rapport"
73:      Caption         =   "X-Rapport"
90:rapporttype = "emptyprintbuffer"
98:rapporttype = "Xrapport"
99:DataReport1.Show
106:rapporttype = "Zrapport"
107:DataReport1.Show
114:rapporttype = "Avstemming"
115:DataReport1.Show
126:'rapport_rs.Open "select * from rapporter_bankterminal order by dato desc", sqlconn, adOpenKeyset, adLockOptimistic
127:Set rappx = rapport_rs.Clone
128:Set rappz = rapport_rs.Clone
129:Set rappavs = rapport_rs.Clone
130:rappx.Filter = "type='Xrapport'"
131:rappz.Filter = "type='Zrapport'"
133:Set xrapportkopi.DataSource = rappx
135:xrapportkopi.DataField = rapport_rs.Fields("dato").Name
137:Set zrapportkopi.DataSource = rappz
138:zrapportkopi.DataField = rapport_rs.Fields("dato").Name
140:avstemmingkopi.DataField = rapport_rs.Fields("dato").Name
145:'rapport_rs.Close
146:'Set rapport_rs = Nothing

CSharpConverted-V2/Tankinger_form.Designer.cs
24:            ((System.ComponentModel.ISupportInitialize)(this.dataGridViewResults)).BeginInit();
62:            this.label1.Text = "Velg rapportdato:";
74:            this.Text = "Oppslag rapporter";
76:            ((System.ComponentModel.ISupportInitialize)(this.dataGridViewResults)).EndInit();

Pumpestyring 2/Pumpestyring/Pumpestyring/mjwPDF.cls
467:                   "Orientation set to portrait.", vbCritical, "Error in orientation - " & mjwPDFVersion
936:    strTyLink = "ELLIPSE"
937:    PDFSetLink URLLink, "ELLIPSE", Int((x - rx / 2)), Int((y + ry / 2 - ry / 2 * 11 / 20))
1298:                Case "ELLIPSE"
1803:            Case "ELLIPSE"
1831:        MsgBox "Image format not supported." & _
1833:                "Only JPEG images are supported." & _
1862:        MsgBox "Image format not supported." & _
1864:                "Only JPEG images are supported." & _
1894:        MsgBox "Image format not supported." & _
1896:                "Only JPEG images are supported." & _

Common%20Files/Common Files/System/Ole DB/MSOrclOLEDBreadme.txt
13:1. PRODUCT DESCRIPTION
22:1. PRODUCT DESCRIPTION
30:The Microsoft OLE DB Provider for Oracle provides access to Oracle databases (version 7.3 or version 8) using the Oracle Client Software, version 7.3.3.4.0 or greater, or version 8.0.4.1.1c. The client software must be installed on the client machine. This provider has been tested primarily with Oracle Client Software version 7.3.3.4.0 and SQL*Net version 2.3.3.0.4. When upgrading an Oracle client, it is important both to install the client upgrade and to apply the "Required Supporting Files" that ships with the Server patch.
37:a) The Microsoft OLE DB Provider for Oracle data sources does not directly support scrolling, nor does it support updating through the rowset using IRowsetChange. If the provider is instantiated using IDataInitialize or invoked through ADO, Service Components will provide updating and scrolling functionality if requested by the application. In addition, the provider does support executing SQL update commands.
43:d) The Transaction Unit Of Work as reported by GetTransaction will always be NULL. For example, get a transaction from a session, and call GetTransaction on it. The XACTUOW from the XACTTRANSINFO will always be NULL.
48:a) This provider doesn't support the {resultset} escape implemented by the Microsoft ODBC Driver for Oracle.
50:b) OLE DB 2.0 includes a new flag in IConvertType::CanConvert, which is DBCONVERTFLAGS_FROMVARIANT. This flag is supported by the provider; however, it has no effect on the return value from CanConvert at this time.  
52:c) In Oracle version 8, the maximum size of a VARCHAR column has increased from 2000 to 4000 bytes. The Oracle 7.3.x client software has no way to bind a parameter value larger 2000 bytes. Therefore, if you create a table with a VARCHAR column of larger than 2000 bytes, you will be unable to perform parameterized inserts, updates, deletes, and queries against it with data that exceeds the 2000-byte limit of the client software. Because both the ODBC Driver for Oracle and the OLE DB Provider for Oracle use parameterized inserts, updates, deletes, and queries, they will report ORA-01026 errors in this case. Data that is within the limits enforced by the Oracle client software will work.

CSharpConverted-V2/omsetning_form.Designer.cs
20:            hentrapport = new System.Windows.Forms.Button();
29:            // hentrapport
31:            hentrapport.Location = new System.Drawing.Point(240, 111);
32:            hentrapport.Margin = new System.Windows.Forms.Padding(4, 5, 4, 5);
33:            hentrapport.Name = "hentrapport";
34:            hentrapport.Size = new System.Drawing.Size(193, 57);
35:            hentrapport.TabIndex = 0;
36:            hentrapport.Text = "Hent Rapport";
37:            hentrapport.UseVisualStyleBackColor = true;
38:            hentrapport.Click += hentrapport_Click;
112:            Controls.Add(hentrapport);
115:            Text = "Omsetningsrapport";
124:        private System.Windows.Forms.Button hentrapport;

Pumpestyring 2/Pumpestyring/Pumpestyring/SQLBASE_SCRIPT/oppgaver.sql
9:	[zrapport] [bit] NULL,
10:	[xrapport] [bit] NULL,

Pumpestyring 2/Pumpestyring/Pumpestyring/DataReport1.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} Datareport1
3:   Bindings        =   "DataReport1.dsx":0000
4:   Caption         =   "Bankreport"
34:      Name            =   "ReportHeader"
62:      Name            =   "ReportFooter"
67:Attribute VB_Name = "Datareport1"
74:Private Sub DataReport_Initialize()
76:Set Datareport1.DataSource = rapport_rs
77: Datareport1.Sections("Section1").Controls.Item("txtrapport").DataField = rapport_rs.Fields("reporttext").Name

Pumpestyring 2/Pumpestyring/Pumpestyring/pumpcontrol.vbw
4:Datareport1 = 220, 220, 869, 666, , 0, 0, 516, 549, CZ

CSharpConverted-V2/omsetning_form.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

Pumpestyring 2/Pumpestyring/Pumpestyring/pumpcontrol.vbp
6:Reference=*\G{642AC760-AAB4-11D0-8494-00A0C90DC8A9}#1.0#0#..\WINDOWS\system32\MSDBRPTR.DLL#Microsoft Data Report Designer 6.0 (SP4)
17:Designer=DataReport1.Dsr
27:Form=rapporter_form.frm
43:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/SQLBASE_SCRIPT/rapporter_bankterminal.sql
3:/****** Object:  Table [dbo].[rapporter_bankterminal]    Script Date: 09/10/2010 14:10:43 ******/
10:CREATE TABLE [dbo].[rapporter_bankterminal](
11:	[reportid] [numeric](18, 0) IDENTITY(1,1) NOT NULL,
12:	[reporttext] [text] COLLATE SQL_Latin1_General_CP1_CI_AS NULL,

CSharpConverted-V2/DispenserkontrollForm.Designer.cs
45:            menuReports = new System.Windows.Forms.ToolStripMenuItem();
46:            miOmsetningsrapport = new System.Windows.Forms.ToolStripMenuItem();
47:            miUttaksrapport = new System.Windows.Forms.ToolStripMenuItem();
240:            menuStripMain.Items.AddRange(new System.Windows.Forms.ToolStripItem[] { menuTool, menuReports, menuTools });
303:            // menuReports
305:            menuReports.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] { miOmsetningsrapport, miUttaksrapport, miStasjonskreditt });
306:            menuReports.Name = "menuReports";
307:            menuReports.Size = new System.Drawing.Size(90, 24);
308:            menuReports.Text = "Rapporter";
309:            menuReports.Click += menuReports_Click;
311:            // miOmsetningsrapport
313:            miOmsetningsrapport.Name = "miOmsetningsrapport";
314:            miOmsetningsrapport.Size = new System.Drawing.Size(220, 26);
315:            miOmsetningsrapport.Text = "Omsetningsrapport";
316:            miOmsetningsrapport.Click += OmsetningsrapportMenu_Click;
318:            // miUttaksrapport
320:            miUttaksrapport.Name = "miUttaksrapport";
321:            miUttaksrapport.Size = new System.Drawing.Size(220, 26);
322:            miUttaksrapport.Text = "Uttaksrapport";
323:            miUttaksrapport.Click += UttaksrapportMenu_Click;
401:    private System.Windows.Forms.ToolStripMenuItem menuReports;
410:    private System.Windows.Forms.ToolStripMenuItem miOmsetningsrapport;
411:    private System.Windows.Forms.ToolStripMenuItem miUttaksrapport;

Pumpestyring 2/Pumpestyring/Pumpestyring/Tankinger_form.frm
5:   Caption         =   "Oppslag rapporter"
232:      Caption         =   "Velg rapportdato:"

CSharpConverted-V2/Avgiftsrapport_form.cs
8:    public partial class Avgiftsrapport_form : Form
10:        public Avgiftsrapport_form()
15:        private void Form_Load(object sender, EventArgs e)
23:        private void hentrapport_Click(object sender, EventArgs e)
41:                    var preview = new Form() { Width = 600, Height = 400, Text = "Avgiftsrapport - preview" };
48:                    MessageBox.Show("Feil ved henting av rapport: " + (err ?? "Ukjent feil"));

CSharpConverted-V2/Kortscan_form.cs
14:        private void Kortscan_form_Load(object sender, EventArgs e)

CSharpConverted-V2/Avgiftsrapport_form.Designer.cs
6:    partial class Avgiftsrapport_form
9:        private Button hentrapport;
28:            this.hentrapport = new System.Windows.Forms.Button();
63:            // hentrapport
65:            this.hentrapport.Location = new System.Drawing.Point(120, 60);
66:            this.hentrapport.Name = "hentrapport";
67:            this.hentrapport.Size = new System.Drawing.Size(200, 30);
68:            this.hentrapport.TabIndex = 3;
69:            this.hentrapport.Text = "Hent Rapport";
70:            this.hentrapport.UseVisualStyleBackColor = true;
71:            this.hentrapport.Click += new System.EventHandler(this.hentrapport_Click);
85:            // Avgiftsrapport_form
91:            this.Controls.Add(this.hentrapport);
95:            this.Name = "Avgiftsrapport_form";

CSharpConverted-V2/bankterminal_form.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

Pumpestyring 2/Pumpestyring/Pumpestyring/SQLBASE_SCRIPT/Tables/oppgaver.sql
9:	[zrapport] [bit] NULL,
10:	[xrapport] [bit] NULL,

CSharpConverted-V2/DbHelper.cs
52:                        // If the file contains many parts and one of them looks like a connection string, return it.
63:            // 3) connection.txt (legacy helper we used earlier)
64:            var connectionTxt = Path.Combine(baseDir, "connection.txt");
65:            if (File.Exists(connectionTxt))
67:                try { return File.ReadAllText(connectionTxt).Trim(); } catch { }
80:            if (string.IsNullOrEmpty(connStr)) throw new InvalidOperationException("No DB connection string found (DbHelper.GetConnectionString()).");
90:        // Parameterized query support
94:            if (string.IsNullOrEmpty(connStr)) throw new InvalidOperationException("No DB connection string found (DbHelper.GetConnectionString()).");
143:            if (string.IsNullOrEmpty(connStr)) throw new InvalidOperationException("No DB connection string found (DbHelper.GetConnectionString()).");
154:            if (string.IsNullOrEmpty(connStr)) throw new InvalidOperationException("No DB connection string found (DbHelper.GetConnectionString()).");

Pumpestyring 2/Pumpestyring/Pumpestyring/SQLBASE_SCRIPT/Tables/rapporter_bankterminal.sql
3:/****** Object:  Table [dbo].[rapporter_bankterminal]    Script Date: 04/28/2010 12:55:30 ******/
10:CREATE TABLE [dbo].[rapporter_bankterminal](
11:	[reportid] [numeric](18, 0) IDENTITY(1,1) NOT NULL,
12:	[reporttext] [text] COLLATE SQL_Latin1_General_CP1_CI_AS NULL,

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/server.frm
23:   Begin VB.TextBox txtcom_port
67:      Caption         =   "Comport bank"
75:      Caption         =   "Comport dispenser :"
125:Print #1, Me.servernavn & ";" & Me.serverdb & ";" & Me.serverbrukernavn & ";" & Me.serverpassord & ";" & Me.txtcom_port & ";" & Me.txtcom_bank
130:Com_port = Me.txtcom_port
131:Com_port_bank = Me.txtcom_bank

Common%20Files/Common Files/System/Ole DB/oledbjvs.inc
56:var DB_E_MULTIPLESTATEMENTS          	= 0x80040E2E;
77:var DB_E_BOOKMARKSKIPPED             	= 0x80040E43;
104:var DB_E_MULTIPLESTORAGE             	= 0x80040E5E;
145:var DB_S_BOOKMARKSKIPPED             	= 0x00040EC3;
168:var DB_S_MULTIPLECHANGES             	= 0x00040EDC;

Common%20Files/Common Files/System/Ole DB/JoltReadme.txt
13:1. PRODUCT DESCRIPTION
22:1. PRODUCT DESCRIPTION
29:With this release of the Microsoft OLE DB Provider for Jet, you can use ANSI standard syntax with respect to parameter markers in queries. Previous versions of this provider supported only the Microsoft Jet-specific parameter marker syntax consisting of a parameter name enclosed in square brackets, where the parameter name is optional.
33:This version of the provider also supports the ANSI syntax, where a question mark, "?", is the parameter marker -- e.g., "...WHERE col1 = ?".
44:To connect to a Microsoft Access database, you previously had to create and reference a DSN in your scripts.  For example, the following script establishes a connection with a Microsoft Access database:
49:However, with the Microsoft OLE DB Provider for Jet, you can directly access your Microsoft Access database files.  The following opens a database connection without referencing a DSN:
57:Your applications that use the Microsoft Access ODBC Driver will continue to be supported. However, you may choose to convert your existing applications from the Microsoft Access ODBC Driver to the Microsoft OLE DB Provider for Jet.   
59:If you need to access secure databases through the Microsoft OLE DB Provider for Jet, you may have to set additional information to what is listed above.  To specify a database password (as opposed to a user password), you need to set the property "Jet OLEDB:Database Password" on the ADO connection object (as above).  Furthermore, if you need to specify a specific system database, you should use the property "Jet OLEDB:System database".
66:If you choose to convert your applications, you must be aware of the functionality differences between the two providers' access methods.  Specifically, for this release, the Microsoft OLE DB Provider for Jet does not support the following:
72:Installable ISAM Support
74:There is currently no support for accessing data other than native Jet data when using the Microsoft OLE DB Provider for Jet directly.  If it is imperative that you access external data from your application, you can -- and should -- continue to use the Microsoft OLE DB Provider for ODBC data until the next release of the Microsoft OLE DB Provider for Jet data.  This provider will be available with the MDAC 2.1 release.
76:Stored Procedure Support
85:IRowsetUpdate::GetOriginalData will fail on a newly inserted row while the pending change is outstanding. The Jet 3.5 Engine itself will not handle the case where you try to retrieve the original values for newly inserted columns, hence the OLE DB provider cannot support this functionality.

CSharpConverted-V2/Alertform.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

CSharpConverted-V2/Kundereg.Designer.cs
29:            ((System.ComponentModel.ISupportInitialize)(this.kortholdereGrid)).BeginInit();
137:            ((System.ComponentModel.ISupportInitialize)(this.kortholdereGrid)).EndInit();

CSharpConverted-V2/OtherForms.cs
6:    // Avgiftsrapport_form implemented in its own files

Common%20Files/Common Files/System/Ole DB/MSDASC.TXT
13:1. PRODUCT DESCRIPTION
22:1. PRODUCT DESCRIPTION
24:Microsoft Data Link API provides a common user interface for defining and managing connections to OLE DB data sources. This user interface can also be called using an application programming interface, the data link API.
26:You can save the connection information to a data link file (.udl).  Then you can modify these files through the Data Links Property page, and applications can use them in creating connections to various OLE DB data stores. The Data Link API provides applications the ability to select, load, or save .udl files.
28:The same user interface used to manage connection information in .udl files can be used by applications to gather connection information from users when doing ad-hoc connections to OLE DB data stores. The data links API allows applications to obtain a string version of the connection information from an existing OLE DB datasource object, create a datasource object from an existing connection string, or use the Data Links dialog to edit the connection properties of an uninitialized OLE DB data source object.
47:The file format of the data link file has been changed in the final release to support unicode. This change means that your existing pre-release data link files will not work with the final build. You will need to rebuild your data link files.  
50:The pre-release version of the IDBPromptInitialize interface supported by the data link component did not include a pointer to a controlling unknown as the first argument to PromptDataSource. Applications that call the IDBPromptInitialize::PromptInitialize method in the pre-release version of Data Link API will have to add this argument and recompile in order to work with the release version.
53:IDataInitialize::GetInitString returns a connection string containing the initialization properties set on the data source object. This method includes an argument, fIncludePassword, for specifying whether or not the password is returned as part of that initialization information. Note that, if DBPROP_AUTH_PERSISTSENSITIVEAUTHINFO is set to VARIANT_FALSE, the password is not returned as part of GetInitString, even if fIncludePassword is true. In order to include the password as part of the information returned from GetInitString, consumers should be sure that DBPROP_AUTH_PERSISTSENSITIVEAUTHINFO is set to VARIANT_TRUE.
56:IDBPromptInitialize::PromptDataSource allows an application to prompt the user for connection information. The application can pass a default data source into PromptDataSource and can specify DBPROMPTOPTIONS_DISABLE_PROVIDER_SELECTION in order to prevent the user from changing the specified data source. This flag must be combined with DBPROMPTOPTIONS_WIZARDSHEET or DBPROMPTOPTIONS_PROPERTYSHEET; it is not valid to set only this flag. Setting this flag without specifying a valid data source in *ppDataSource on input returns an error, E_INVALIDARG.
84:* Doc Bug: The description of IDBPromptInitialize::PromptDatasource states that the method returns a connection string. In fact, the method returns a datasource object with the specified properties set.
88:* Doc Bug: The description of IDBPromptInitialize::PromptFileName suggests that the filename can be passed to IDataInitialze in order to get a data source object based on the connection string. This is not supported. In order to load a data source from a .udl file, the application must call IDataInitialize::LoadStringFromStorage to obtain the connection string from the file and then call IDataInitialize::GetDatasource with that string in order to obtain the data source object based on the connection string.
95:2.0 Limitation: Note that the 2.0 release of Data Link API does not support creating remote providers. Calling IDataInitialize::CreateDBInstanceEx for anything other than a local provider will fail.

Pumpestyring 2/Pumpestyring/Pumpestyring/Fonts/Symbol.afm
138:C 181 ; WX 713 ; N proportional ; B 27 123 639 404 ;

CSharpConverted-V2/Properties/Resources.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/pumpekontroll.frm
776:    Com_port = cfgline(4)
777:    Com_port_bank = cfgline(5)
778:    com_port_print = cfgline(6)
808:    com_print.CommPort = com_port_print
831:If Com_port_bank <> 0 Then baxi.Close
837:    MsgBox "Feil ved lukking av kommunikasjonsport.", vbOKOnly, "Kommunikasjonsfeil"
923:MSComm1.CommPort = Com_port
950:MsgBox "Feil ved lukking av comport, restart av maskinen ved �gjre den strmls er anbefalt:" & Err.Number & " " & Err.Description

CSharpConverted-V2/MainPorted.cs
12:        // Example ADO.NET connection loader if the original DB is accessible via OLE DB
13:        public static void LoadFromDatabase(string connectionString)
17:                using var conn = new OleDbConnection(connectionString);
20:                cmd.CommandText = "SELECT clientsrv_local, clientsrv_localtcpport FROM settings_table"; // adjust table

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/EHL4x.vbp
25:ServerSupportFiles=0

CSharpConverted-V2/Innstillinger.cs
16:        private void Form_Load(object sender, EventArgs e)
50:        private void Command1_Click(object sender, EventArgs e)

Common%20Files/Common Files/System/Ole DB/oledbvbs.inc
56:Const DB_E_MULTIPLESTATEMENTS          	= &H80040E2E
77:Const DB_E_BOOKMARKSKIPPED             	= &H80040E43
104:Const DB_E_MULTIPLESTORAGE             	= &H80040E5E
145:Const DB_S_BOOKMARKSKIPPED             	= &H00040EC3
168:Const DB_S_MULTIPLECHANGES             	= &H00040EDC

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/pumpcontrol.vbp
27:ServerSupportFiles=0

CSharpConverted-V2/README.md
10:- TcpClientWrapper.cs (async TCP wrapper that raises DataReceived events)
13:- This is a partial automated port focusing on TCP message handling and main form core actions.
14:- Database/Recordset interactions were not ported; settings are read from `settings.ini` similarly to the VB code.
18:- Build and run in Visual Studio to test connectivity to your dispenser.

CSharpConverted-V2/Stasjonskreditt.cs
14:        private void Stasjonskreditt_Load(object sender, EventArgs e)
21:        private void Command1_Click(object sender, EventArgs e)

CSharpConverted-V2/Uttaksrapport_form.Designer.cs
3:    partial class Uttaksrapport_form
27:            this.hentrapport = new System.Windows.Forms.Button();
34:            ((System.ComponentModel.ISupportInitialize)(this.dataGridViewResults)).BeginInit();
37:            // hentrapport
39:            this.hentrapport.Location = new System.Drawing.Point(1680, 510);
40:            this.hentrapport.Name = "hentrapport";
41:            this.hentrapport.Size = new System.Drawing.Size(145, 37);
42:            this.hentrapport.TabIndex = 3;
43:            this.hentrapport.Text = "Hent Rapport";
44:            this.hentrapport.UseVisualStyleBackColor = true;
45:            this.hentrapport.Click += new System.EventHandler(this.hentrapport_Click);
103:            // Uttaksrapport_form
111:            this.Controls.Add(this.hentrapport);
115:            this.Name = "Uttaksrapport_form";
116:            this.Text = "Uttaksrapport";
117:            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.Uttaksrapport_form_FormClosing);
118:            this.Load += new System.EventHandler(this.Uttaksrapport_form_Load);
119:            ((System.ComponentModel.ISupportInitialize)(this.dataGridViewResults)).EndInit();
126:        private System.Windows.Forms.Button hentrapport;

Common%20Files/Common Files/System/Ole DB/MSDASQLreadme.txt
14:1. PRODUCT DESCRIPTION
21:1. PRODUCT DESCRIPTION
34:A new property, DBPROP_SERVER_NAME, has been added. This property is a datasource information property, not an initialization property. It returns, upon initialization, the name of the server to which you are connected. In many cases, this will be the same as the DBPROP_INIT_DATASOURCE property. For example, when connecting to an ODBC datasource you might specify a DSN (friendly name), and the server name would tell you the name of the actual server to which you were connected.
40:The OLE DB Specification calls for Booleans converted to string data types to appear as the strings "True" or "False". Using the ODBC provider, if the underlying ODBC driver does not support SQLDescribeParam and the OLE DB consumer does not specify the type of the parameter, the ODBC provider will convert Boolean parameter values to "-1" and "0" when the data type of the parameter is a string. In order to ensure proper conversion against ODBC drivers that do not support describing parameters, the OLE DB consumer should always call SetParameterInfo to specify the types of parameters.

CSharpConverted-V2/bankterminal_form.cs
14:        private void Form_Load(object sender, EventArgs e)
20:        private void filtrer_Click(object sender, EventArgs e)
34:                    // If there's a textual report field, concatenate into the rich text box
35:                    if (results.Columns.Contains("reporttext"))
40:                            sb.AppendLine(r["reporttext"]?.ToString());
56:        private void DataGrid1_CellClick(object sender, DataGridViewCellEventArgs e)
65:                    if (row.Table.Columns.Contains("reporttext"))
67:                    RichTextBox1.Text = row["reporttext"]?.ToString();
91:        private void utskrift_Click(object sender, EventArgs e)
98:                MessageBox.Show("Ingen rapport å skrive ut.");
104:            MessageBox.Show("Rapporten er kopiert til utklippstavlen (bruk en teksteditor for å se).\nImplementer riktig utskrift ved behov.");

CSharpConverted-V2/Tankinger_form.cs
16:        private void Tankinger_form_Load(object sender, EventArgs e)
22:        private void filtrer_Click(object sender, EventArgs e)

CSharpConverted-V2/Kortscan_form.Designer.cs
19:            ((System.ComponentModel.ISupportInitialize)(this.kortlisteGrid)).BeginInit();
42:            ((System.ComponentModel.ISupportInitialize)(this.kortlisteGrid)).EndInit();

CSharpConverted-V2/Dispenserkontroll.csproj.user
8:    <Compile Update="Avgiftsrapport_form.cs">
41:    <Compile Update="Uttaksrapport_form.cs">

CSharpConverted-V2/omsetning_form.cs
19:        // ───────── Rapport/print state ─────────
20:        private DataTable _reportTable;
21:        private string _reportTitle;                 // "Omsetningsrapport"
22:        private string _reportPeriod;                // "Periode :MM - MM År: YYYY. Alle beløp inkl mva"
23:        private string _reportDateTime;              // "dd.MM.yyyy / HH:mm"
30:        private void omsetning_form_Load(object sender, EventArgs e)
50:            try { hentrapport.Click -= hentrapport_Click; hentrapport.Click += hentrapport_Click; } catch { }
53:        private void omsetning_form_FormClosing(object sender, FormClosingEventArgs e) => this.Dispose();
55:        private void hentrapport_Click(object sender, EventArgs e)
71:                " isnull(zrapportsum,0) as zrapportsum," +
98:            BuildReportTable(raw);
100:            _reportTitle = "Omsetningsrapport";
101:            _reportPeriod = $"Periode :{mfrom} - {mto} År: {yearInt}. Alle beløp inkl mva";
102:            _reportDateTime = $"{DateTime.Now:dd.MM.yyyy} / {DateTime.Now:HH:mm}";
108:        private void BuildReportTable(DataTable raw)
110:            _reportTable = new DataTable();
111:            _reportTable.Columns.Add("Dato", typeof(DateTime));
112:            _reportTable.Columns.Add("Manuell", typeof(decimal));
113:            _reportTable.Columns.Add("Stasjonskr", typeof(decimal));
114:            _reportTable.Columns.Add("Zrapp. beløp", typeof(decimal));
115:            _reportTable.Columns.Add("Teknisk", typeof(decimal));
116:            _reportTable.Columns.Add("Sum kort", typeof(decimal));
117:            _reportTable.Columns.Add("Totalsum", typeof(decimal));
126:                decimal zrp = ToDec(r["zrapportsum"]);
131:                _reportTable.Rows.Add(dato, man, sta, zrp, tek, kort, tot);
180:                Text = _reportTitle
187:                Text = _reportPeriod
193:                Text = _reportDateTime,
219:                DataSource = _reportTable,
295:                DocumentName = "Omsetningsrapport",
320:                    Title = "Lagre omsetningsrapport som PDF",
325:                    FileName = $"Omsetningsrapport_{DateTime.Now:yyyy-MM-dd}.pdf",
365:        // ───────── Utskrift: tegn rapporten så papir/PDF matcher skjerm ─────────
366:        private void PrintDoc_PrintPage(object sender, PrintPageEventArgs e)
383:            g.DrawString(_reportTitle, fHeader, Brushes.Black, x, y);
386:            g.DrawString(_reportPeriod, fBold, Brushes.Black, x, y);
387:            var rightText = _reportDateTime;
409:            while (_printRowIndex < _reportTable.Rows.Count)
413:                var r = _reportTable.Rows[_printRowIndex];
440:            if (_printRowIndex >= _reportTable.Rows.Count)
468:        private void rappmndfra_SelectedIndexChanged(object sender, EventArgs e) { }

CSharpConverted-V2/Uttaksrapport_form.cs
9:    public partial class Uttaksrapport_form : Form
11:        public Uttaksrapport_form()
16:        private void Uttaksrapport_form_Load(object sender, EventArgs e)
39:        private void Uttaksrapport_form_FormClosing(object sender, FormClosingEventArgs e)
45:        private void hentrapport_Click(object sender, EventArgs e)
70:                MessageBox.Show("Ingen DB-tilkobling eller feil ved spørring:\n" + (err ?? "Ukjent feil") + "\nSQL:\n" + sql, "Uttaksrapport");

CSharpConverted-V2/Omsetningprdag.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

CSharpConverted-V2/frmSettnypris.cs
15:        private void Form_Load(object sender, EventArgs e)
22:        private async void settprisok_Click(object sender, EventArgs e)
32:            // Normalize decimal separator to '.' for parsing, but keep original formatting for sending
55:                DispenserkontrollForm.Instance.UpdateStatus(sent ? "Venter på bekreftelse." : "Feil ved sending av pris.");
59:                MessageBox.Show("Hovedprogrammet er ikke tilgjengelig. Kan ikke sende pris.");
70:                MessageBox.Show("Feil: klarte ikke sende pris til dispenserkontrollen.");
74:        private void Form_QueryUnload(object sender, FormClosingEventArgs e)

CSharpConverted-V2/bankterminal_form.Designer.cs
32:            ((System.ComponentModel.ISupportInitialize)(this.DataGrid1)).BeginInit();
86:            this.Label1.Text = "Velg rapportdato:";
98:            this.Text = "Rapporter";
100:            ((System.ComponentModel.ISupportInitialize)(this.DataGrid1)).EndInit();

CSharpConverted-V2/Avgiftsrapport_form.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

CSharpConverted-V2/Kundereg.cs
18:        private void Kundereg_Load(object sender, EventArgs e)
23:        private void cmdsok_Click(object sender, EventArgs e)
53:        private void cmdnullstill_Click(object sender, EventArgs e)
61:        private void Command10_Click(object sender, EventArgs e)
168:        private void Command6_Click(object sender, EventArgs e)
177:        private void Command7_Click(object sender, EventArgs e)
185:        private void Command8_Click(object sender, EventArgs e)
193:        private void Command5_Click(object sender, EventArgs e)
200:        private void Command3_Click(object sender, EventArgs e)
206:        private void Command9_Click(object sender, EventArgs e)
212:        private void Combo1_LostFocus(object sender, EventArgs e)
217:        private void Text1_Change(object sender, EventArgs e)

CSharpConverted-V2/Kundereg.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

CSharpConverted-V2/DispenserkontrollForm.resx
34:    type or mimetype. Type corresponds to a .NET class that support
36:    Classes that don't support this are serialized and stored with the
63:    <xsd:import namespace="http://www.w3.org/XML/1998/namespace" />

CSharpConverted-V2/DispenserkontrollForm.cs
4:using System.Net.Sockets;
23:        private System.Windows.Forms.Timer? reconnectTimer;
79:                            // also support semicolon separated lines seen earlier
115:            // VB6 Main: read DB connection info from settings.ini to open the database,
116:            // then read the "settings" table for clientsrv_local and clientsrv_localtcpport.
117:            // Our DbHelper already discovers a connection string (appsettings, settings.ini, env),
122:                var sql = "SELECT clientsrv_local, clientsrv_localtcpport FROM settings";
146:                    // DbHelper reported a failure; log the error for diagnostics
190:                // start timers after initial connect attempt
199:        // Menu click handlers to show ported forms
200:        private void BankterminalMenu_Click(object sender, EventArgs e)
205:        private void TankingerMenu_Click(object sender, EventArgs e)
210:        private void KunderegisterMenu_Click(object sender, EventArgs e)
215:        private void InnstillingerMenu_Click(object sender, EventArgs e)
220:        private void SettNyPrisMenu_Click(object sender, EventArgs e)
225:        private void KortscanMenu_Click(object sender, EventArgs e)
230:        private void OmsetningsrapportMenu_Click(object sender, EventArgs e)
235:        private void UttaksrapportMenu_Click(object sender, EventArgs e)
237:            try { new Uttaksrapport_form().Show(); } catch { }
240:        private void StasjonskredittMenu_Click(object sender, EventArgs e)
245:        private void ExitMenu_Click(object sender, EventArgs e)
260:                        var connected = tcpclient != null && tcpclient.IsConnected;
261:                        UpdateStatus(connected ? "Tilkoblet til dispenser." : "Ikke tilkoblet til dispenser.");
268:            if (reconnectTimer == null)
270:                reconnectTimer = new System.Windows.Forms.Timer();
271:                reconnectTimer.Interval = 30000; // 30s
272:                reconnectTimer.Tick += async (s, e) =>
290:                reconnectTimer.Start();
327:                                // our TcpClientWrapper doesn't expose VB6 numeric states; approximate with connected status
368:                // try to read configured port name from settings.ini (client_rfidcomport)
369:                string portName = null;
376:                        if (l.IndexOf("client_rfidcomport", StringComparison.OrdinalIgnoreCase) >= 0)
379:                            if (parts.Length > 1) portName = parts[1].Trim();
385:                if (string.IsNullOrEmpty(portName)) return;
386:                rfidPort = new System.IO.Ports.SerialPort(portName, 9600);
390:            catch { /* swallow errors - RF port optional */ }
393:        private void RfidPort_DataReceived(object? sender, System.IO.Ports.SerialDataReceivedEventArgs e)
397:                var sp = sender as System.IO.Ports.SerialPort;
425:                // forward to tcp server if connected
453:        // Simplified message processor (port of TcpClient_ProcessMessage)
522:                        // VB6 sends OK or non-OK responses; if parts[1]=="OK" we can set an internal flag
542:        // Allow other forms to send messages through the main tcp client
603:        private async void cmdStart_Click(object sender, EventArgs e)
606:            // Equivalent of sending <TANK_DISP_UNBLOCK> or <TANK_DISP_UNBLOCK_NOTAX>
619:        private async void cmdStop_Click(object sender, EventArgs e)
632:        private async void chkNotax_CheckedChanged(object sender, EventArgs e)
639:        // Menu / helper actions to open other forms (porting simple Show() calls from VB6)
640:        public void OpenUttaksrapport()
642:            var f = new Uttaksrapport_form();
646:        public void OpenAvgiftsrapport()
648:            var f = new Avgiftsrapport_form();
658:        public void OpenOmsetningsrapport()
676:        private void lblBelop_Click(object sender, EventArgs e)
681:        private void menuReports_Click(object sender, EventArgs e)

CSharpConverted-V2/Dispenserkontroll.sln.DotSettings.user
2:	<s:String x:Key="/Default/CodeInspection/ExcludedFiles/FilesAndFoldersToSkip2/=7020124F_002D9FFC_002D4AC3_002D8F3D_002DAAB8E0240759_002Ff_003ATCPClient_002Ecs_002Fl_003A_002E_002E_003F_002E_002E_003F_002E_002E_003F_002E_002E_003F_002E_002E_003F_002E_002E_003FApplication_0020Support_003FJetBrains_003FRider2025_002E3_003Fresharper_002Dhost_003FSourcesCache_003F81ab20aa8158456caedab6b45cf1a3b2f23e334f83b6b65dbe16e9aa2a24b_003FTCPClient_002Ecs/@EntryIndexedValue">ForceIncluded</s:String>

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/Pushservice/pushservice.txt
5:13.08.2017 07:27:59 Sjekket om modbusport eksisterer
20:13.08.2017 17:49:27 Sjekket om modbusport eksisterer
34:17.08.2017 18:21:14 Sjekket om modbusport eksisterer
87:09.09.2017 20:19:39 Sjekket om modbusport eksisterer
109:13.09.2017 13:35:11 Sjekket om modbusport eksisterer
180:05.11.2017 18:17:09 Sjekket om modbusport eksisterer
195:10.11.2017 13:47:20 Sjekket om modbusport eksisterer
353:16.06.2018 14:53:45 Sjekket om modbusport eksisterer
463:14.07.2018 17:49:30 Sjekket om modbusport eksisterer
477:15.07.2018 13:44:51 Sjekket om modbusport eksisterer
499:15.07.2018 13:57:28 Sjekket om modbusport eksisterer
553:29.07.2018 09:55:52 Sjekket om modbusport eksisterer
591:30.07.2018 11:16:16 Sjekket om modbusport eksisterer
613:05.08.2018 17:42:22 Sjekket om modbusport eksisterer
651:11.08.2018 07:37:44 Sjekket om modbusport eksisterer
674:11.08.2018 13:25:03 Sjekket om modbusport eksisterer
792:14.10.2018 00:52:14 Sjekket om modbusport eksisterer
822:21.12.2018 22:14:13 Sjekket om modbusport eksisterer
836:21.12.2018 22:26:22 Sjekket om modbusport eksisterer
861:25.01.2019 12:12:33 Sjekket om modbusport eksisterer
972:11.03.2019 17:16:39 Sjekket om modbusport eksisterer
1560:20.04.2019 17:58:24 Sjekket om modbusport eksisterer
1582:22.04.2019 11:46:51 Sjekket om modbusport eksisterer
1598:05.05.2019 12:01:15 Sjekket om modbusport eksisterer
1612:05.05.2019 12:20:59 Sjekket om modbusport eksisterer
1636:11.05.2019 12:48:50 Sjekket om modbusport eksisterer
1659:16.05.2019 14:05:50 Sjekket om modbusport eksisterer
1722:06.07.2019 11:33:30 Sjekket om modbusport eksisterer
1736:06.07.2019 12:01:01 Sjekket om modbusport eksisterer
1750:10.07.2019 17:57:57 Sjekket om modbusport eksisterer
1764:11.07.2019 17:20:11 Sjekket om modbusport eksisterer
1778:12.07.2019 16:57:24 Sjekket om modbusport eksisterer
1793:22.07.2019 12:07:49 Sjekket om modbusport eksisterer
1807:22.07.2019 13:10:16 Sjekket om modbusport eksisterer
1837:30.07.2019 05:44:28 Sjekket om modbusport eksisterer
1859:31.07.2019 17:09:11 Sjekket om modbusport eksisterer
1874:01.08.2019 16:21:08 Sjekket om modbusport eksisterer
1888:02.08.2019 09:35:45 Sjekket om modbusport eksisterer
1918:12.08.2019 10:50:49 Sjekket om modbusport eksisterer
1948:20.08.2019 13:22:45 Sjekket om modbusport eksisterer
1962:21.08.2019 11:58:23 Sjekket om modbusport eksisterer
1976:27.08.2019 06:36:45 Sjekket om modbusport eksisterer
2005:08.09.2019 12:54:59 Sjekket om modbusport eksisterer
2019:12.09.2019 06:51:18 Sjekket om modbusport eksisterer
2057:08.10.2019 12:25:31 Sjekket om modbusport eksisterer
2111:03.01.2020 15:59:10 Sjekket om modbusport eksisterer
2125:03.01.2020 16:12:48 Sjekket om modbusport eksisterer
2172:29.02.2020 15:24:27 Sjekket om modbusport eksisterer
4059:03.04.2020 11:30:13 Sjekket om modbusport eksisterer
4074:22.04.2020 14:05:51 Sjekket om modbusport eksisterer
4093:07.05.2020 09:33:32 Sjekket om modbusport eksisterer
4112:21.05.2020 11:41:31 Sjekket om modbusport eksisterer
4150:01.07.2020 10:52:37 Sjekket om modbusport eksisterer
4164:01.07.2020 10:55:46 Sjekket om modbusport eksisterer
4183:01.07.2020 11:14:32 Sjekket om modbusport eksisterer
4826:10.07.2020 15:10:26 Sjekket om modbusport eksisterer
4840:13.07.2020 17:52:41 Sjekket om modbusport eksisterer
4854:13.07.2020 17:58:08 Sjekket om modbusport eksisterer
4876:13.07.2020 18:26:16 Sjekket om modbusport eksisterer
4890:13.07.2020 18:27:57 Sjekket om modbusport eksisterer
4912:13.07.2020 18:31:33 Sjekket om modbusport eksisterer
5285:14.07.2020 12:48:29 Sjekket om modbusport eksisterer
6330:18.07.2020 13:27:54 Sjekket om modbusport eksisterer
6418:18.07.2020 17:16:44 Sjekket om modbusport eksisterer
6898:19.07.2020 17:02:20 Sjekket om modbusport eksisterer
8721:23.07.2020 13:04:24 Sjekket om modbusport eksisterer
8899:28.07.2020 15:39:28 Sjekket om modbusport eksisterer
10071:31.07.2020 02:39:02 Sjekket om modbusport eksisterer
11341:02.08.2020 18:14:15 Sjekket om modbusport eksisterer
11376:03.08.2020 06:16:48 Sjekket om modbusport eksisterer
11648:12.08.2020 11:40:57 Sjekket om modbusport eksisterer
11683:27.08.2020 16:35:45 Sjekket om modbusport eksisterer
11778:11.06.2021 04:21:48 Sjekket om modbusport eksisterer
11801:09.07.2021 16:58:59 Sjekket om modbusport eksisterer
11823:09.07.2021 17:07:53 Sjekket om modbusport eksisterer
11853:19.07.2021 07:08:33 Sjekket om modbusport eksisterer
11867:19.07.2021 07:17:28 Sjekket om modbusport eksisterer
11882:24.07.2021 14:17:07 Sjekket om modbusport eksisterer
11896:25.07.2021 19:40:18 Sjekket om modbusport eksisterer
11910:01.08.2021 13:35:35 Sjekket om modbusport eksisterer
11940:02.08.2021 15:18:19 Sjekket om modbusport eksisterer
11954:03.08.2021 15:19:24 Sjekket om modbusport eksisterer
11968:03.08.2021 15:21:16 Sjekket om modbusport eksisterer
11990:04.08.2021 11:41:07 Sjekket om modbusport eksisterer
12036:13.08.2021 17:26:14 Sjekket om modbusport eksisterer
12051:19.08.2021 11:27:33 Sjekket om modbusport eksisterer
12073:20.08.2021 18:16:18 Sjekket om modbusport eksisterer
12087:21.08.2021 09:20:49 Sjekket om modbusport eksisterer
12504:25.08.2021 11:08:01 Sjekket om modbusport eksisterer
12526:31.08.2021 14:17:41 Sjekket om modbusport eksisterer
12548:11.09.2021 06:22:23 Sjekket om modbusport eksisterer
12570:15.09.2021 06:37:23 Sjekket om modbusport eksisterer
12592:01.12.2021 08:23:54 Sjekket om modbusport eksisterer
64037:24.04.2022 00:58:08 Sjekket om modbusport eksisterer
93820:25.06.2022 21:48:37 Sjekket om modbusport eksisterer
96643:10.07.2022 17:18:18 Sjekket om modbusport eksisterer
110421:08.08.2022 19:03:22 Sjekket om modbusport eksisterer
120873:22.03.2023 11:39:43 Sjekket om modbusport eksisterer
130251:11.04.2023 07:24:44 Sjekket om modbusport eksisterer
130274:21.11.2025 11:30:45 Sjekket om modbusport eksisterer
130288:21.11.2025 11:33:14 Sjekket om modbusport eksisterer
130310:21.11.2025 11:41:58 Sjekket om modbusport eksisterer
130324:21.11.2025 11:43:03 Sjekket om modbusport eksisterer
130343:21.11.2025 12:00:55 Sjekket om modbusport eksisterer

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/Pushservice/Pushservice.exe.manifest
45:    <dependentAssembly dependencyType="install" allowDelayedBinding="true" codebase="ModbusTCP.dll" size="11776">
46:      <assemblyIdentity name="ModbusTCP" version="3.3.0.0" language="neutral" processorArchitecture="msil" />

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x.vbp
25:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/uttaksrapport.Dsr
3:   Bindings        =   "uttaksrapport.dsx":0000
4:   Caption         =   "Uttaksrapport"
14:   ReportWidth     =   8397
410:Private Sub DataReport_Initialize()
411:Me.Sections("Section4").Controls("Label10").Caption = "Periode :" & Uttaksrapport_form.rappmndfra & " - " & Uttaksrapport_form.rappmndtil & " �: " & Uttaksrapport_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/defs.bas
31:Public berr As String, rapporttype  As String, txtbankf1  As String, txtbankf2  As String, txtbankf3 As String, txtbankf4 As String, feed_offset As String, reporttext As String
40:Public Com_port As Integer, Com_port_bank As Integer, com_port_print As Integer, com_port_pinpad As Integer, com_port_stcredit As Integer
46:Public COM_id As Long, valresult As Long, com_port_bank_baud As Long
52:Public y(16) As Byte, x(16) As Byte, chksum As Byte, TCPsendMessage(15) As String
88:Public rapport_rs As New ADODB.Recordset
103:    Com_port = Val(cfgline(4))
104:    Com_port_bank = Val(cfgline(5))
105:    com_port_print = Val(cfgline(6))
106:    com_port_pinpad = Val(cfgline(7))
107:    com_port_stcredit = Val(cfgline(8))
108:    com_port_bank_baud = Val(cfgline(9))
240:Sub tcpsend(tcpmelding As String)
248:Pumpekontroll.errorlist.AddItem "TCP_send:" & Err.Number & " " & Err.Description
298:If com_port_stcredit > 0 Then
299:    Pumpekontroll.RFIDCOM.CommPort = com_port_stcredit
319:If com_port_pinpad > 0 Then
320:    Pumpekontroll.com_pinpad.CommPort = com_port_pinpad                     'Disse setningene m�flyttes.
343:If com_port_print > 0 Then
344:    Pumpekontroll.com_print.CommPort = com_port_print
359:Pumpekontroll.errorlist.AddItem "Kan ikke �ne kommunikasjonsport til kvitteringskriver." & Err.Number & " " & Err.Description
364:If Com_port_bank > 0 Then
368:        .CommPort = Com_port_bank
369:        .BaudRate = com_port_bank_baud
370:        '.CutterSupport = False

Common%20Files/Common Files/System/ado/adovbs.inc
249:Const adPropNotSupported = &H0000
312:Const adErrPropNotSupported = &Hea2
316:Const adErrDenyNotSupported = &Hea6
317:Const adErrDenyTypeNotSupported = &Hea7

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/omsetning_form.frm
4:   Caption         =   "Omsetningsrapport"
45:   Begin VB.CommandButton hentrapport
46:      Caption         =   "Hent Rapport"
96:Private Sub hentrapport_Click()
100:.CommandText = "SELECT DAY(dato) AS Expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Belp%', reporttext) + 9, 15)), ',', '.') AS float)) AS sum1 Into presalg From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Belp%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato)"
107:.CommandText = "SELECT DAY(dato) AS expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Retur%', reporttext) + 9, 15)), ',', '.') AS float)) AS retur Into retur From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Retur%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato) "

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/Pushservice/Pushservice.exe.config
23:<startup><supportedRuntime version="v2.0.50727"/></startup></configuration>

Pumpestyring 2/Pumpestyring/Pumpestyring/Report_timer/Report_timer.vbp
4:Form=Report_timer.frm
6:ExeName32="Report_timer.exe"
8:Name="Reporttimer"
15:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/Pushservice/Pushservice.vshost.exe.config
23:<startup><supportedRuntime version="v2.0.50727"/></startup></configuration>

Common%20Files/Common Files/System/ado/ADOreadme.txt
13:1. PRODUCT DESCRIPTION
22:2.7 ADO Support for Visual Analyzer (Microsoft Visual Studio(TM), Enterprise Edition Only)
34:1. PRODUCT DESCRIPTION
41:Microsoft Remote Data Service (RDS) is a component of ADO that provides fast and efficient data connectivity and the data publishing framework for applications hosted in Microsoft Internet Explorer. It is based on a client/server, distributed technology that works over HTTP, HTTPS (HTTP over Secure Sockets layer), and DCOM application protocols. Using data-aware ActiveX controls, RDS provides data access programming in the style of Microsoft Visual Basic(R) to Web developers who need to build distributed, data-intensive applications for use over corporate intranets and the Internet.
51:As did RDO 2.0, ADO now supports asynchronous operations. Asynchronous operations allow you to cancel out of an extended operation or to continue processing while waiting for the connection to complete. Events notify you when an asynchronous operation has been completed. Asynchronous fetching is a feature specific to the client cursor (CursorLocation = adUseClient), which returns the first rows from a query result and then continues fetching in the background while you manipulate the rows that have already been fetched.
59:You can now save a Recordset object right to your local hard drive and load it later (when working with client cursors). This allows you to connect to the server, execute a query, call rst.Save("myfilename"), shut down the computer, and later call rs.Open("myfilename",,,adCmdFile) and modify the data.
73:2.7 ADO Support for Visual Analyzer (Microsoft Visual Studio, Enterprise Edition Only)
79:Provides enhanced functionality for Recordset objects built with client-side cursors in two-tier scenarios. New functions, like Resync and Update, with conflict resolution are now supported on client cursors.
129:i) All two-tier and DCOM scenarios on the RDS.DataControl object. This means that you cannot open database connections on your local machine or from servers to which you connect using the DCOM protocol.
137:i) Making any connection where provider is not MS Remote. So the connection string must start with "Provider=MS Remote". The "Remote Server" tag in the connection string must also be the same name as the server from which the page has been downloaded. Local two-tier and DCOM connections are not allowed.
149:By changing the security level, you can change the behavior of disconnected ADO Recordset objects running in the browser. If you want to enable unsafe operations and do not want to be prompted every time such an operation is attempted, then you must explicitly set the value for the above option to "Enable." This is done by customizing the security settings, as described below. Please also note that if you attempt an unsafe operation (such as saving it to a file in the local filesystem) on an ADO Recordset obtained from the RDS DataControl, then you must set the value for the above option to "Enable." The setting of "Prompt" acts like "Disable" for such Recordset objects (obtained from the RDS Datacontrol).
165:Now ADO/RDS objects will behave in specified custom mode. These settings affect the following behavior of ADO/RDS objects (as described in 3.2.3) in the specified security zone -- opening local two-tier connections; working over DCOM; connecting to a server other than the one from which the page was originally downloaded; saving and opening a recordset to/from files on the local machine.
175:4.1 Client impersonation in RDS is not currently supported due to missing support from the operating system.
184:4.4 When using the Recordset.Save method, for best results use CursorLocation=adUseClient. Some OLE DB providers do not support all of the functionality necessary to support the saving of recordsets, and the client cursor can be used in order to supply that functionality.
195:4.5.2 ConnectComplete and Disconnect
199:The description for the adStatus parameter also states to "set this parameter to adStatusUnwantedEvent to prevent subsequent notifications."  However, closing and reopening a connection causes and events that have been "turned off" in this manner to start firing again.
213:The description for the pConnection parameter states that this connection object reference is to "the connection on which the command executed."  Warnings can also occur on other types of operations, such as opening a connection.
216:In the Remarks section, the following Recordset operations can also cause these events to be fired: Filter, AbsolutePage, AbsolutePosition.  It will also fire if the child recordset has recordset events connected and the parent recordset moves. Also, Delete will NOT fire these events.
223:4.7 Asynchronous Fetching is available in ADO 2.0 when using CursorLocation=adUseClient.  There are two ways to turn this on -- one via the Options parameter to Recordset.Open, and another via the Recordset Properties Collection "Asynchronous Rowset Processing" property.  For best results, always use the Recordset.Open parameter.  Not using the parameter can cause the loss of ADO background fetch related events.  Additionally, background fetching using Provider="MS Remote" is not supported through the properties collection -- only via the Recordset.Open parameter.
259:4.11 When using Events in ADO against a provider which does not support bookmarks, the user will receive a RecordsetChanged notification each time ADO is required to fetch new rows from the OLE DB provider. The frequency with which this occurs is directly dependent on the Recordset.CacheSize property.
279:4.15 In ADO, the RecordCount property of the Recordset object may not always be supported by the provider or specific cursor type being used. In those cases in which the provider or cursor type doesn't support RecordCount, -1 will be returned as the value.
293:4.19 In the documentation for the topic "Step4: Manipulate the data (ADO Tutorial)," the sample code refers to the Optimize property on the Field object. This is slightly incorrect -- the Optimize property is found in the Properties collection of the Field object when using CursorLocation=adUseClient or a disconnected Recordset object. Sample usage is as follows:

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_09.log
18:12:53:15.3593750  :  CutterSupport          = 0
29:12:53:15.3593750  :  TCPIPSERVER-----------------------------------------
30:12:53:15.3593750  :  SocketListener         = 0
31:12:53:15.3593750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/rapporter_form.frm
6:   Caption         =   "Rapporter"
44:      TextRTF         =   $"rapporter_form.frx":0000
47:      Bindings        =   "rapporter_form.frx":0082
107:         DataField       =   "reporttext"
108:         Caption         =   "reporttext"
120:         DataField       =   "reportid"
121:         Caption         =   "Reportid"
154:      Caption         =   "Velg rapportdato:"
169:RichTextBox1.Text = DataGrid1.Columns("reporttext").Text

Pumpestyring 2/Pumpestyring/Pumpestyring/Report_timer/Report_timer.frm
49:    If Not RST!zrapport Or IsNull(RST!zrapport) Then
50:    RST!zrapport = True
53:    If Not RST!xrapport Or IsNull(RST!xrapport) Then
54:    RST!xrapport = True

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_27.log
18:12:31:36.4531250  :  CutterSupport          = 0
29:12:31:36.4531250  :  TCPIPSERVER-----------------------------------------
30:12:31:36.4531250  :  SocketListener         = 0
31:12:31:36.4531250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_02.log
18:02:06:22.1250000  :  CutterSupport          = 0
29:02:06:22.1250000  :  TCPIPSERVER-----------------------------------------
30:02:06:22.1250000  :  SocketListener         = 0
31:02:06:22.1250000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Tankinger_form.frm
5:   Caption         =   "Oppslag rapporter"
233:      Caption         =   "Velg rapportdato:"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_25.log
18:23:58:03.9218750  :  CutterSupport          = 0
29:23:58:03.9218750  :  TCPIPSERVER-----------------------------------------
30:23:58:03.9375000  :  SocketListener         = 0
31:23:58:03.9375000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_21.log
18:10:40:22.7812500  :  CutterSupport          = 0
29:10:40:22.7812500  :  TCPIPSERVER-----------------------------------------
30:10:40:22.7812500  :  SocketListener         = 0
31:10:40:22.7812500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_29.log
18:23:58:40.0156250  :  CutterSupport          = 0
29:23:58:40.0156250  :  TCPIPSERVER-----------------------------------------
30:23:58:40.0156250  :  SocketListener         = 0
31:23:58:40.0156250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_06.log
18:09:22:06.5156250  :  CutterSupport          = 0
29:09:22:06.5156250  :  TCPIPSERVER-----------------------------------------
30:09:22:06.5156250  :  SocketListener         = 0
31:09:22:06.5156250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_12.log
18:14:38:38.1807500  :  CutterSupport          = 0
29:14:38:38.1807500  :  TCPIPSERVER-----------------------------------------
30:14:38:38.1807500  :  SocketListener         = 0
31:14:38:38.1807500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_15.log
18:23:58:20.5000000  :  CutterSupport          = 0
29:23:58:20.5000000  :  TCPIPSERVER-----------------------------------------
30:23:58:20.5000000  :  SocketListener         = 0
31:23:58:20.5000000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_01.log
18:10:36:17.1093750  :  CutterSupport          = 0
29:10:36:17.1093750  :  TCPIPSERVER-----------------------------------------
30:10:36:17.1093750  :  SocketListener         = 0
31:10:36:17.1093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_19.log
18:20:12:31.8867500  :  CutterSupport          = 0
29:20:12:31.8867500  :  TCPIPSERVER-----------------------------------------
30:20:12:31.8867500  :  SocketListener         = 0
31:20:12:31.8867500  :  SocketListenerPort     = 6001
180:20:17:52.6718750  :  CutterSupport          = 0
191:20:17:52.6718750  :  TCPIPSERVER-----------------------------------------
192:20:17:52.6718750  :  SocketListener         = 0
193:20:17:52.6718750  :  SocketListenerPort     = 6001

Common%20Files/Common Files/System/ado/adojavas.inc
249:var adPropNotSupported = 0x0000;
312:var adErrPropNotSupported = 0xea2;
316:var adErrDenyNotSupported = 0xea6;
317:var adErrDenyTypeNotSupported = 0xea7;

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_01.log
18:23:58:04.3281250  :  CutterSupport          = 0
29:23:58:04.3281250  :  TCPIPSERVER-----------------------------------------
30:23:58:04.3281250  :  SocketListener         = 0
31:23:58:04.3281250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_15.log
18:21:51:09.7901250  :  CutterSupport          = 0
29:21:51:09.7901250  :  TCPIPSERVER-----------------------------------------
30:21:51:09.7901250  :  SocketListener         = 0
31:21:51:09.7901250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_18.log
18:08:30:45.0713750  :  CutterSupport          = 0
29:08:30:45.0713750  :  TCPIPSERVER-----------------------------------------
30:08:30:45.0713750  :  SocketListener         = 0
31:08:30:45.0713750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_12.log
18:14:38:37.3838750  :  CutterSupport          = 0
29:14:38:37.3995000  :  TCPIPSERVER-----------------------------------------
30:14:38:37.3995000  :  SocketListener         = 0
31:14:38:37.3995000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_03.log
18:12:11:18.3750000  :  CutterSupport          = 0
29:12:11:18.3750000  :  TCPIPSERVER-----------------------------------------
30:12:11:18.3750000  :  SocketListener         = 0
31:12:11:18.3750000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/Pushservice/ModbusTCP.xml
4:        <name>ModbusTCP</name>
7:        <member name="T:ModbusTCP.Master">
9:            Modbus TCP common driver class. This class implements a modbus TCP master driver.
10:            It supports the following commands:
30:        <member name="F:ModbusTCP.Master.excIllegalFunction">
33:        <member name="F:ModbusTCP.Master.excIllegalDataAdr">
36:        <member name="F:ModbusTCP.Master.excIllegalDataVal">
39:        <member name="F:ModbusTCP.Master.excSlaveDeviceFailure">
42:        <member name="F:ModbusTCP.Master.excAck">
45:        <member name="F:ModbusTCP.Master.excSlaveIsBusy">
48:        <member name="F:ModbusTCP.Master.excGatePathUnavailable">
51:        <member name="F:ModbusTCP.Master.excExceptionNotConnected">
52:            <summary>Constant for exception not connected.</summary>
54:        <member name="F:ModbusTCP.Master.excExceptionConnectionLost">
55:            <summary>Constant for exception connection lost.</summary>
57:        <member name="F:ModbusTCP.Master.excExceptionTimeout">
60:        <member name="F:ModbusTCP.Master.excExceptionOffset">
63:        <member name="F:ModbusTCP.Master.excSendFailt">
64:            <summary>Constant for exception send failt.</summary>
66:        <member name="T:ModbusTCP.Master.ResponseData">
69:        <member name="E:ModbusTCP.Master.OnResponseData">
72:        <member name="T:ModbusTCP.Master.ExceptionData">
75:        <member name="E:ModbusTCP.Master.OnException">
78:        <member name="P:ModbusTCP.Master.timeout">
82:        <member name="P:ModbusTCP.Master.refresh">
86:        <member name="P:ModbusTCP.Master.connected">
87:            <summary>Shows if a connection is active.</summary>
89:        <member name="M:ModbusTCP.Master.#ctor">
92:        <member name="M:ModbusTCP.Master.#ctor(System.String,System.UInt16)">
94:            <param name="ip">IP adress of modbus slave.</param>
95:            <param name="port">Port number of modbus slave. Usually port 502 is used.</param>
97:        <member name="M:ModbusTCP.Master.connect(System.String,System.UInt16)">
98:            <summary>Start connection to slave.</summary>
99:            <param name="ip">IP adress of modbus slave.</param>
100:            <param name="port">Port number of modbus slave. Usually port 502 is used.</param>
102:        <member name="M:ModbusTCP.Master.disconnect">
103:            <summary>Stop connection to slave.</summary>
105:        <member name="M:ModbusTCP.Master.Finalize">
108:        <member name="M:ModbusTCP.Master.Dispose">
111:        <member name="M:ModbusTCP.Master.ReadCoils(System.UInt16,System.Byte,System.UInt16,System.UInt16)">
118:        <member name="M:ModbusTCP.Master.ReadCoils(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[]@)">
126:        <member name="M:ModbusTCP.Master.ReadDiscreteInputs(System.UInt16,System.Byte,System.UInt16,System.UInt16)">
133:        <member name="M:ModbusTCP.Master.ReadDiscreteInputs(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[]@)">
141:        <member name="M:ModbusTCP.Master.ReadHoldingRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16)">
148:        <member name="M:ModbusTCP.Master.ReadHoldingRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[]@)">
156:        <member name="M:ModbusTCP.Master.ReadInputRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16)">
163:        <member name="M:ModbusTCP.Master.ReadInputRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[]@)">
171:        <member name="M:ModbusTCP.Master.WriteSingleCoils(System.UInt16,System.Byte,System.UInt16,System.Boolean)">
178:        <member name="M:ModbusTCP.Master.WriteSingleCoils(System.UInt16,System.Byte,System.UInt16,System.Boolean,System.Byte[]@)">
186:        <member name="M:ModbusTCP.Master.WriteMultipleCoils(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[])">
194:        <member name="M:ModbusTCP.Master.WriteMultipleCoils(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.Byte[],System.Byte[]@)">
203:        <member name="M:ModbusTCP.Master.WriteSingleRegister(System.UInt16,System.Byte,System.UInt16,System.Byte[])">
210:        <member name="M:ModbusTCP.Master.WriteSingleRegister(System.UInt16,System.Byte,System.UInt16,System.Byte[],System.Byte[]@)">
218:        <member name="M:ModbusTCP.Master.WriteMultipleRegister(System.UInt16,System.Byte,System.UInt16,System.Byte[])">
225:        <member name="M:ModbusTCP.Master.WriteMultipleRegister(System.UInt16,System.Byte,System.UInt16,System.Byte[],System.Byte[]@)">
233:        <member name="M:ModbusTCP.Master.ReadWriteMultipleRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.UInt16,System.Byte[])">
242:        <member name="M:ModbusTCP.Master.ReadWriteMultipleRegister(System.UInt16,System.Byte,System.UInt16,System.UInt16,System.UInt16,System.Byte[],System.Byte[]@)">

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_17.log
18:13:54:20.5088750  :  CutterSupport          = 0
29:13:54:20.5088750  :  TCPIPSERVER-----------------------------------------
30:13:54:20.5088750  :  SocketListener         = 0
31:13:54:20.5088750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_28.log
18:14:35:04.6718750  :  CutterSupport          = 0
29:14:35:04.6718750  :  TCPIPSERVER-----------------------------------------
30:14:35:04.6718750  :  SocketListener         = 0
31:14:35:04.6718750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_27.log
18:12:31:36.1093750  :  CutterSupport          = 0
29:12:31:36.1093750  :  TCPIPSERVER-----------------------------------------
30:12:31:36.1093750  :  SocketListener         = 0
31:12:31:36.1093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_06.log
18:09:22:06  :  CutterSupport          = 0
29:09:22:06  :  TCPIPSERVER-----------------------------------------
30:09:22:06  :  SocketListener         = 0
31:09:22:06  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_24.log
18:14:38:20.5625000  :  CutterSupport          = 0
29:14:38:20.5625000  :  TCPIPSERVER-----------------------------------------
30:14:38:20.5625000  :  SocketListener         = 0
31:14:38:20.5625000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_14.log
18:10:03:12.2031250  :  CutterSupport          = 0
29:10:03:12.2031250  :  TCPIPSERVER-----------------------------------------
30:10:03:12.2031250  :  SocketListener         = 0
31:10:03:12.2031250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_14.log
18:11:27:14.8526250  :  CutterSupport          = 0
29:11:27:14.8526250  :  TCPIPSERVER-----------------------------------------
30:11:27:14.8526250  :  SocketListener         = 0
31:11:27:14.8526250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_23.log
18:13:37:42.4426250  :  CutterSupport          = 0
29:13:37:42.4426250  :  TCPIPSERVER-----------------------------------------
30:13:37:42.4426250  :  SocketListener         = 0
31:13:37:42.4426250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/omsetning_form.log
6:Line 52: Property List in rapporttype could not be set.
7:Line 52: Property ItemData in rapporttype could not be set.

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_08.log
18:01:05:43.6093750  :  CutterSupport          = 0
29:01:05:43.6093750  :  TCPIPSERVER-----------------------------------------
30:01:05:43.6093750  :  SocketListener         = 0
31:01:05:43.6093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_13.log
18:05:23:10.5245000  :  CutterSupport          = 0
29:05:23:10.5245000  :  TCPIPSERVER-----------------------------------------
30:05:23:10.5245000  :  SocketListener         = 0
31:05:23:10.5245000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_22.log
18:23:58:41.4113750  :  CutterSupport          = 0
29:23:58:41.4113750  :  TCPIPSERVER-----------------------------------------
30:23:58:41.4113750  :  SocketListener         = 0
31:23:58:41.4113750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_20.log
18:12:01:57.6562500  :  CutterSupport          = 0
29:12:01:57.6562500  :  TCPIPSERVER-----------------------------------------
30:12:01:57.6562500  :  SocketListener         = 0
31:12:01:57.6562500  :  SocketListenerPort     = 6001
508:15:05:46.4375000  :  CutterSupport          = 0
519:15:05:46.4375000  :  TCPIPSERVER-----------------------------------------
520:15:05:46.4375000  :  SocketListener         = 0
521:15:05:46.4375000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Stasjonskredittrapport.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} uttaksrapport
3:   Bindings        =   "Stasjonskredittrapport.dsx":0000
4:   Caption         =   "Uttaksrapport pr kunde"
14:   ReportWidth     =   8715
36:      Name            =   "ReportHeader"
56:         Object.Caption         =   "Uttaksrapport "
377:      Name            =   "ReportFooter"
480:Attribute VB_Name = "uttaksrapport"
486:Private Sub DataReport_Initialize()
487:Me.Sections("Reportheader").Controls("Label8").Caption = "Periode :" & stasjonskreditt_form.rappmndfra.Text & " - " & stasjonskreditt_form.rappmndtil.Text & " �: " & stasjonskreditt_form.rappaar.Text & " for " & stasjonskreditt_form.rapport.Text

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_07.log
18:06:09:37.2812500  :  CutterSupport          = 0
29:06:09:37.2812500  :  TCPIPSERVER-----------------------------------------
30:06:09:37.2812500  :  SocketListener         = 0
31:06:09:37.2812500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_16.log
18:08:55:45.7432500  :  CutterSupport          = 0
29:08:55:45.7432500  :  TCPIPSERVER-----------------------------------------
30:08:55:45.7432500  :  SocketListener         = 0
31:08:55:45.7432500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/baxilog/baxiHost_2025_11_21.log
18:11:30:52.9687500  :  CutterSupport          = 0
29:11:30:52.9687500  :  TCPIPSERVER-----------------------------------------
30:11:30:52.9687500  :  SocketListener         = 0
31:11:30:52.9687500  :  SocketListenerPort     = 6001
50:11:33:20.5000000  :  CutterSupport          = 0
61:11:33:20.5000000  :  TCPIPSERVER-----------------------------------------
62:11:33:20.5000000  :  SocketListener         = 0
63:11:33:20.5000000  :  SocketListenerPort     = 6001
85:11:37:00.1562500  :  CutterSupport          = 0
96:11:37:00.1562500  :  TCPIPSERVER-----------------------------------------
97:11:37:00.1562500  :  SocketListener         = 0
98:11:37:00.1562500  :  SocketListenerPort     = 6001
120:11:43:08.4531250  :  CutterSupport          = 0
131:11:43:08.4531250  :  TCPIPSERVER-----------------------------------------
132:11:43:08.4531250  :  SocketListener         = 0
133:11:43:08.4531250  :  SocketListenerPort     = 6001
155:12:01:02.1093750  :  CutterSupport          = 0
166:12:01:02.1093750  :  TCPIPSERVER-----------------------------------------
167:12:01:02.1093750  :  SocketListener         = 0
168:12:01:02.1093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_21.log
18:10:40:22.4062500  :  CutterSupport          = 0
29:10:40:22.4062500  :  TCPIPSERVER-----------------------------------------
30:10:40:22.4062500  :  SocketListener         = 0
31:10:40:22.4062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_26.log
18:04:49:32.1406250  :  CutterSupport          = 0
29:04:49:32.1406250  :  TCPIPSERVER-----------------------------------------
30:04:49:32.1406250  :  SocketListener         = 0
31:04:49:32.1406250  :  SocketListenerPort     = 6001
56:04:51:26.2031250  :  CutterSupport          = 0
67:04:51:26.2031250  :  TCPIPSERVER-----------------------------------------
68:04:51:26.2031250  :  SocketListener         = 0
69:04:51:26.2031250  :  SocketListenerPort     = 6001
126:04:51:46.5781250  :  CutterSupport          = 0
137:04:51:46.5781250  :  TCPIPSERVER-----------------------------------------
138:04:51:46.5781250  :  SocketListener         = 0
139:04:51:46.5781250  :  SocketListenerPort     = 6001
196:05:10:03.9218750  :  CutterSupport          = 0
207:05:10:03.9218750  :  TCPIPSERVER-----------------------------------------
208:05:10:03.9218750  :  SocketListener         = 0
209:05:10:03.9218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_20.log
18:23:58:26.7082500  :  CutterSupport          = 0
29:23:58:26.7082500  :  TCPIPSERVER-----------------------------------------
30:23:58:26.7082500  :  SocketListener         = 0
31:23:58:26.7082500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_09.log
18:12:34:03.3281250  :  CutterSupport          = 0
29:12:34:03.3281250  :  TCPIPSERVER-----------------------------------------
30:12:34:03.3281250  :  SocketListener         = 0
31:12:34:03.3281250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_06.log
18:09:22:05.9843750  :  CutterSupport          = 0
29:09:22:05.9843750  :  TCPIPSERVER-----------------------------------------
30:09:22:05.9843750  :  SocketListener         = 0
31:09:22:05.9843750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_08.log
18:20:03:38.6093750  :  CutterSupport          = 0
29:20:03:38.6093750  :  TCPIPSERVER-----------------------------------------
30:20:03:38.6093750  :  SocketListener         = 0
31:20:03:38.6093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_09.log
18:12:53:15.1718750  :  CutterSupport          = 0
29:12:53:15.1718750  :  TCPIPSERVER-----------------------------------------
30:12:53:15.1718750  :  SocketListener         = 0
31:12:53:15.1718750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_28.log
18:00:24:11.9218750  :  CutterSupport          = 0
29:00:24:11.9218750  :  TCPIPSERVER-----------------------------------------
30:00:24:11.9218750  :  SocketListener         = 0
31:00:24:11.9218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_29.log
18:17:30:26.5937500  :  CutterSupport          = 0
29:17:30:26.5937500  :  TCPIPSERVER-----------------------------------------
30:17:30:26.5937500  :  SocketListener         = 0
31:17:30:26.5937500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_26.log
18:18:55:02.8217500  :  CutterSupport          = 0
29:18:55:02.8217500  :  TCPIPSERVER-----------------------------------------
30:18:55:02.8217500  :  SocketListener         = 0
31:18:55:02.8217500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_12.log
18:14:38:37.3682500  :  CutterSupport          = 0
29:14:38:37.3682500  :  TCPIPSERVER-----------------------------------------
30:14:38:37.3682500  :  SocketListener         = 0
31:14:38:37.3682500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Dispenserkontroll.vbw
7:Uttaksrapport_form = 154, 154, 585, 614, C, 132, 132, 563, 592, C
10:uttaksrapport = 66, 66, 841, 471, C, 88, 88, 838, 778, C

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_21.log
18:07:15:51.3020000  :  CutterSupport          = 0
29:07:15:51.3020000  :  TCPIPSERVER-----------------------------------------
30:07:15:51.3020000  :  SocketListener         = 0
31:07:15:51.3020000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_01.log
18:23:58:04.2968750  :  CutterSupport          = 0
29:23:58:04.2968750  :  TCPIPSERVER-----------------------------------------
30:23:58:04.2968750  :  SocketListener         = 0
31:23:58:04.2968750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Dispenserkontroll.vbp
8:Form=rapporter_form.frm
12:Reference=*\G{642AC760-AAB4-11D0-8494-00A0C90DC8A9}#1.0#0#..\WINDOWS\system32\MSDBRPTR.DLL#Microsoft Data Report Designer 6.0 (SP4)
19:Form=Uttaksrapport.frm
20:Designer=uttaksrapport.Dsr
22:Designer=Stasjonskredittrapport.Dsr
23:Form=stasjonskreditt_rapport_form.frm
39:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_15.log
18:23:58:20.4687500  :  CutterSupport          = 0
29:23:58:20.4687500  :  TCPIPSERVER-----------------------------------------
30:23:58:20.4687500  :  SocketListener         = 0
31:23:58:20.4687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_11.log
18:23:58:52.7752520  :  CutterSupport          = 0
29:23:58:52.7752520  :  TCPIPSERVER-----------------------------------------
30:23:58:52.7752520  :  SocketListener         = 0
31:23:58:52.7752520  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_29.log
18:23:58:40  :  CutterSupport          = 0
29:23:58:40  :  TCPIPSERVER-----------------------------------------
30:23:58:40  :  SocketListener         = 0
31:23:58:40  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_09.log
18:12:34:03.8750000  :  CutterSupport          = 0
29:12:34:03.8750000  :  TCPIPSERVER-----------------------------------------
30:12:34:03.8750000  :  SocketListener         = 0
31:12:34:03.8750000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_28.log
18:00:24:12.0937500  :  CutterSupport          = 0
29:00:24:12.0937500  :  TCPIPSERVER-----------------------------------------
30:00:24:12.0937500  :  SocketListener         = 0
31:00:24:12.0937500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_13.log
18:05:23:10.6182500  :  CutterSupport          = 0
29:05:23:10.6182500  :  TCPIPSERVER-----------------------------------------
30:05:23:10.6182500  :  SocketListener         = 0
31:05:23:10.6182500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_07.log
18:06:09:37.2968750  :  CutterSupport          = 0
29:06:09:37.2968750  :  TCPIPSERVER-----------------------------------------
30:06:09:37.2968750  :  SocketListener         = 0
31:06:09:37.2968750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/lpgnorge.Dsr
16:      ConnectionName  =   "Rapporter"
41:      ActiveConnectionName=   "Rapporter"
52:         Name            =   "reportid"
53:         Caption         =   "reportid"
60:         Name            =   "reporttext"
61:         Caption         =   "reporttext"
133:      ActiveConnectionName=   "Rapporter"
147:      ActiveConnectionName=   "Rapporter"
160:      ActiveConnectionName=   "Rapporter"
174:      ActiveConnectionName=   "Rapporter"
187:      ActiveConnectionName=   "Rapporter"
217:      ActiveConnectionName=   "Rapporter"
351:      ActiveConnectionName=   "Rapporter"
479:      ActiveConnectionName=   "Rapporter"
549:      ActiveConnectionName=   "Rapporter"
582:      ActiveConnectionName=   "Rapporter"
668:      ActiveConnectionName=   "Rapporter"
739:      ActiveConnectionName=   "Rapporter"
990:         Name            =   "Sist_eksportert"
991:         Caption         =   "Sist_eksportert"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_21.log
18:07:15:52.2863750  :  CutterSupport          = 0
29:07:15:52.2863750  :  TCPIPSERVER-----------------------------------------
30:07:15:52.2863750  :  SocketListener         = 0
31:07:15:52.2863750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_07.log
18:06:09:37.9218750  :  CutterSupport          = 0
29:06:09:37.9218750  :  TCPIPSERVER-----------------------------------------
30:06:09:37.9218750  :  SocketListener         = 0
31:06:09:37.9218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/pumpekontroll.frm
289:   Begin MSWinsockLib.Winsock status_poller
462:   Begin MSWinsockLib.Winsock tcpserver
997:      Begin VB.Menu ikkeimporterte
998:         Caption         =   "Ikke importerte"
1038:tcpsend "<TANK_TERMINAL_MESSAGE>;" & Label7.Caption & ";<SLUTT>"
1051:berr = "invalid COM port"
1065:berr = "terminal Not connected Or Not polling"
1067:berr = "error in sending MSG"
1079:berr = "sending message with retries but the response is NAK"
1083:berr = "error connecting to host"
1089:berr = "feil ved sending til HOST"
1207:berr = "Socket Error"
1260:    rapport_rs.AddNew
1261:    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
1262:    rapport_rs!dato = Now()
1264:        rapport_rs!Type = "Forh�dsvalg"
1265:        rapport_rs!cardnumber = Baxi.CardData
1268:        LogEvent "Cardpayment_ACK", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "TANK"
1270:        bank_charge = reporttext
1272:        If InStr(1, UCase(reporttext), "ANNULLERING", vbTextCompare) > 0 Then
1273:            rapport_rs!Type = "Annulering"
1274:            rapport_rs!cardnumber = Baxi.CardData
1275:            'reporttext = Replace(reporttext, "NOK", "Annulert/Retur NOK")
1277:            rapport_rs!Type = "Tilbakefring"
1278:            'reporttext = Replace(reporttext, "NOK", "Retur NOK")
1279:            rapport_rs!cardnumber = Baxi.CardData
1290:    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
1291:    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
1292:    reporttext = Replace(reporttext, Chr(9), "", 1)
1293:    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
1294:    rapport_rs!reporttext = reporttext
1295:    rapport_rs.Update
1296:    RichTextBox1.Text = reporttext
1309:    If reporttext <> "" Then
1311:    rapport_rs.AddNew
1312:    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
1313:    rapport_rs!dato = Now()
1314:    rapport_rs!Type = rapporttype
1315:    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
1316:    reporttext = Replace(reporttext, Chr(9), "", 1)
1317:    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
1320:    If rapporttype = "Zrapport" Then
1322:        zrapp_str = Mid(reporttext, InStr(1, reporttext, "Total=", vbTextCompare) + 6, Len(reporttext))
1328:        cashbackstr_header = "Tekniske tilbakefringer siden forrige Z-rapport:" & Chr(10) + Chr(13)
1333:            lpgnorge.rscashback!reported = 1
1337:        reporttext = reporttext + Chr(10) + Chr(13) & cashbackstr_header & cashbackstr_body & cashbackstr_footer
1343:        !zrapportsum = zrapp_amount
1354:        Eml "Zrapport fra betalingsterminal", reporttext, Trim(lpgnorge.rsfirmainfo!zrapp_reciever1)
1356:    rapport_rs!reporttext = reporttext
1357:    rapport_rs.Update
1359:        RichTextBox1.Text = reporttext
1364:    rapport_rs.AddNew
1365:    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
1366:    rapport_rs!dato = Now()
1367:    rapport_rs!Type = "Terminal"
1368:    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
1369:    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
1370:    reporttext = Replace(reporttext, Chr(9), "", 1)
1371:    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
1378:    rapport_rs!reporttext = reporttext
1379:    rapport_rs.Update
1385:    RichTextBox1.Text = reporttext
1398:reporttext = ""
1409:reporttext = ""
1416:        rapport_rs.AddNew
1417:        rapport_rs!dato = Now()
1418:        rapport_rs!Type = "Teknisk tilbakefring."
1420:        rapport_rs!reporttext = bank_charge & Chr(13) & "Teknisk tilbakefring av NOK :" & FormatNumber(Cashbackbelop, 2) & " er sendt vert."
1421:        rapport_rs.Update
1428:        lpgnorge.rscashback!reported = 0
1432:        Eml "Manglende tilbakefring til kortkunde.", bank_charge & Chr(13) & "-----------Tanket----------" & Chr(13) & bank_tank & "-----------Retur----------" & Chr(13) & reporttext, "betaling@lpgnorge.no"
1442:reporttext = Print_reciept_header & Baxi.PrintText
1474:tcplog.AddItem Now & " " & "Onsenddatarecieved :" & st & "-->" & ut
1563:    reporttext = Chr(9) & "Det har skjedd en feil med kommunikasjon mot dispenser." & Chr(10) _
1569:   If com_print.PortOpen Then com_print.Output = reporttext
1571:   reporttext = ""
1811:     tcpsend "<PRINTERSTATE>;" & StatusBar1.Panels.Item(2).Text & ";<SLUTT>"
1965:    rapport_rs.Open "select * from rapporter_bankterminal where datediff(month,dato,getdate())<=1 order by dato", sqlconn, adOpenKeyset, adLockOptimistic
1997:        errorlist.AddItem "Kan ikke �ne kommunikasjonsport med dispenser."
2017:rapport_rs.Close
2022:Set rapport_rs = Nothing
2032:If Com_port_bank <> 0 Then Baxi.Close
2163:tcpsend "<RESTART>;RESTART IKKE MULIG PGA TERMINAL OPPTATT;<SLUTT>"
2190:'tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt ikke aktiv;<SLUTT>"
2228:10033           tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt;<SLUTT>"
2240:10046      tcpsend "<TANK_TERMINAL_MESSAGE>;" & rfidstr & ";<SLUTT>"
2274:        tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt;<SLUTT>"
2285:    If Len(rfidstr) = 14 Then tcpsend "<TANK_TERMINAL_MESSAGE>;" & rfidstr & ";<SLUTT>"
2312:tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"
2314:tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";1;" & bank_sum & ";<SLUTT>"
2316:tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"
2497:MSComm1.CommPort = Com_port
2558:780                     tcpsend "<TANK_STOP>;<SLUTT>"
2566:                                'tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"
2575:                                'tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";1;" & bank_sum & ";<SLUTT>"
2576:800                             rapport_rs.AddNew
2577:805                             LogEvent "TANK_finished", belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption, rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
2578:810                             rapport_rs!dato = Now()
2579:820                             rapport_rs!Type = "Tankkvittering"
2580:830                             reporttext = Print_reciept_header & Chr(9) & "Dato:" & Now() & Chr(10) _
2591:840                             If com_print.PortOpen Then com_print.Output = reporttext
2592:850                             bank_tank = reporttext
2593:                                reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
2594:                                reporttext = Replace(reporttext, Chr(9), "", 1)
2595:                                reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
2596:                                rapport_rs!reporttext = reporttext
2605:                                        LogEvent "Annul initiated", CStr((bank_sum - tank_sum)), rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
2609:                                        LogEvent "Cashback initiated", CStr((bank_sum - tank_sum)), rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
2634:12000                           rapport_rs.AddNew
2635:12001                           rapport_rs!dato = Now()
2636:12002                           rapport_rs!Type = "Stasjonskreditt"
2637:12003                           reporttext = Print_reciept_header & Chr(9) & "Dato:" & Now() & Chr(10) _
2643:12005                                If com_print.PortOpen Then com_print.Output = reporttext
2644:12006                                reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
2645:12007                                reporttext = Replace(reporttext, Chr(9), "", 1)
2646:12008                                reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
2647:12009                                rapport_rs!reporttext = reporttext
2648:12010                                rapport_rs.Update
2706:1160                    rapport_rs.Update
2740:                                    tcpsend "<STATE>;" & state_string & ";<SLUTT>"
2742:                                    tcpsend "<STATE>;" & Left(state_string, 5) & "0" & Right(state_string, 2)
2856:                                tcpsend "<STATE_TANK>;" & state_string_Tank & ";<SLUTT>"
2862:                                rapport_rs.MoveLast
2863:                                Select Case rapport_rs!Type
2865:                                        errorlist.AddItem Now & ":Forh�dsvalg m�tilbakefres etter strmbrudd.reportid=" & rapport_rs!reportid
2870:                                        errorlist.AddItem Now & ":Strmbrudd under tanking, retur av restbelp m�gjennomfres. reportid:" & rapport_rs!reportid
2878:                                        errorlist.AddItem Now & ":Sjekk siste transaksjon, reportid:" & rapport_rs!reportid
3079:If lpgnorge.rstasks!zrapport = True Then
3083:    rapporttype = "Zrapport"
3084:    lpgnorge.rstasks!zrapport = False
3096:    rapporttype = "Avstemming"
3102:If POSsystem = 1 Then Check_and_import_order
3152:Private Sub Check_and_import_order()
3176:40120       If lpgnorge.rsNavnaddress.RecordCount <> 1 Then errorlist.AddItem "Import ordre, kan ikke avgjre kundeadresse."
3224:                 errorlist.AddItem Now & "ordre:" & onr & " Import ordre, kan ikke avgjre vare, setter innpris til 0"
3261:   errorlist.AddItem Now & ", check and import_order: " & Erl & " " & Err.Number & " " & Err.Description
3272:   errorlist.AddItem Now & ", tcpserver_connectionrequest: " & Err.Number & " " & Err.Description
3289:         tcpsend "<RESTART>;RESTART IVERKSETTES OM<30sek;<SLUTT>"
3291:     tcpsend "<RESTART>;RESTART IKKE MULIG PGA ADMIN;<SLUTT>"
3341:tcpsend "<STATIONSTATE>;?;<SLUTT>"
3351:'    tcpsend "<DISP_LITER>;<DISP_LITER_OK>;" & CSng(literantall) / 100
3359:tcpsend "<TANK_DISP_UNBLOCK>;OK;<SLUTT>"
3362:        tcpsend "<STATUS>;Dispenser opptatt;<SLUTT>"
3370:tcpsend "<TANK_DISP_UNBLOCK>;?;<SLUTT>"
3377:    tcpsend "<TANK_DISP_STOP>;OK;<SLUTT>"
3380:    tcpsend "<TANK_DISP_STOP>;?;<SLUTT>"
3387:    tcpsend "<BANK_CASH>;OK;<SLUTT>"
3391:    tcpsend "<BANK_CASH>;?;<SLUTT>"
3398:    tcpsend "<BANK_CASHBACK>;OK;<SLUTT>"
3403:    tcpsend "<BANK_CASHBACK>;?;<SLUTT>"
3418:    tcpsend "<PRICE>;ERROR;Pris for liten;<SLUTT>"
3423:        tcpsend "<PRICE>;OK;<SLUTT>"
3425:        errorlist.AddItem "Feil ved prisendring:" & Now & " " & Err.Number & " " & Err.Description
3429:    tcpsend "<PRICE>;?;<SLUTT>"
3507:                tcpsend "<TANK_TERMINAL_MESSAGE>;Dispenser blokkert pga timeout(2min);<SLUTT>"
3511:                tcpsend "<TANK_TERMINAL_MESSAGE>;Banktransaksjon annulert pga timeout(2min);<SLUTT>"
3515:                tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt blokkert pga timeout(2min);<SLUTT>"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_13.log
18:05:23:10.5557500  :  CutterSupport          = 0
29:05:23:10.5557500  :  TCPIPSERVER-----------------------------------------
30:05:23:10.5557500  :  SocketListener         = 0
31:05:23:10.5557500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_07.log
18:23:58:44.1406250  :  CutterSupport          = 0
29:23:58:44.1406250  :  TCPIPSERVER-----------------------------------------
30:23:58:44.1406250  :  SocketListener         = 0
31:23:58:44.1406250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_11.log
18:23:58:52.8377520  :  CutterSupport          = 0
29:23:58:52.8377520  :  TCPIPSERVER-----------------------------------------
30:23:58:52.8377520  :  SocketListener         = 0
31:23:58:52.8377520  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_24.log
18:15:14:47.9401489  :  CutterSupport          = 0
29:15:14:47.9401489  :  TCPIPSERVER-----------------------------------------
30:15:14:47.9401489  :  SocketListener         = 0
31:15:14:47.9401489  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Kundereg.frm
309:         DataField       =   "Sist_eksportert"
310:         Caption         =   "Sist_eksportert"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_20.log
18:12:01:58.7187500  :  CutterSupport          = 0
29:12:01:58.7187500  :  TCPIPSERVER-----------------------------------------
30:12:01:58.7187500  :  SocketListener         = 0
31:12:01:58.7187500  :  SocketListenerPort     = 6001
89:15:05:46.5000000  :  CutterSupport          = 0
100:15:05:46.5000000  :  TCPIPSERVER-----------------------------------------
101:15:05:46.5000000  :  SocketListener         = 0
102:15:05:46.5000000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_30.log
18:13:05:46.5000000  :  CutterSupport          = 0
29:13:05:46.5000000  :  TCPIPSERVER-----------------------------------------
30:13:05:46.5000000  :  SocketListener         = 0
31:13:05:46.5000000  :  SocketListenerPort     = 6001
100:16:40:30.9062500  :  CutterSupport          = 0
111:16:40:30.9062500  :  TCPIPSERVER-----------------------------------------
112:16:40:30.9062500  :  SocketListener         = 0
113:16:40:30.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_08.log
18:01:05:44.4687500  :  CutterSupport          = 0
29:01:05:44.4687500  :  TCPIPSERVER-----------------------------------------
30:01:05:44.4687500  :  SocketListener         = 0
31:01:05:44.4687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/innstillinger.frm
127:      Caption         =   "IPadresse kamera :"
135:      Caption         =   "IPadresse betalingsterminalterminal :"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_14.log
18:10:03:12.1875000  :  CutterSupport          = 0
29:10:03:12.1875000  :  TCPIPSERVER-----------------------------------------
30:10:03:12.1875000  :  SocketListener         = 0
31:10:03:12.1875000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_18.log
18:12:25:59.2707500  :  CutterSupport          = 0
29:12:25:59.2707500  :  TCPIPSERVER-----------------------------------------
30:12:25:59.2707500  :  SocketListener         = 0
31:12:25:59.2707500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_05.log
18:07:43:30.6562500  :  CutterSupport          = 0
29:07:43:30.6562500  :  TCPIPSERVER-----------------------------------------
30:07:43:30.6562500  :  SocketListener         = 0
31:07:43:30.6562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_13.log
18:18:11:10.0312500  :  CutterSupport          = 0
29:18:11:10.0312500  :  TCPIPSERVER-----------------------------------------
30:18:11:10.0312500  :  SocketListener         = 0
31:18:11:10.0312500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_22.log
18:11:24:20.3125000  :  CutterSupport          = 0
29:11:24:20.3125000  :  TCPIPSERVER-----------------------------------------
30:11:24:20.3125000  :  SocketListener         = 0
31:11:24:20.3125000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_02.log
18:23:58:12.0312500  :  CutterSupport          = 0
29:23:58:12.0312500  :  TCPIPSERVER-----------------------------------------
30:23:58:12.0312500  :  SocketListener         = 0
31:23:58:12.0312500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_28.log
18:14:35:04.6562500  :  CutterSupport          = 0
29:14:35:04.6562500  :  TCPIPSERVER-----------------------------------------
30:14:35:04.6562500  :  SocketListener         = 0
31:14:35:04.6562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Uttaksrapport.frm
2:Begin VB.Form Uttaksrapport_form
3:   Caption         =   "Uttaksrapport"
12:   Begin VB.CommandButton hentrapport
13:      Caption         =   "Hent Rapport"
22:      ItemData        =   "Uttaksrapport.frx":0000
24:      List            =   "Uttaksrapport.frx":0028
32:      ItemData        =   "Uttaksrapport.frx":005C
34:      List            =   "Uttaksrapport.frx":0084
42:      ItemData        =   "Uttaksrapport.frx":00B8
44:      List            =   "Uttaksrapport.frx":00DD
75:Attribute VB_Name = "Uttaksrapport_form"
93:Private Sub hentrapport_Click()
98:.CommandText = "select dag=day(datostart), isnull(sum(liter),0) as liter from tankinger where month(datostart)>=" & Uttaksrapport_form.rappmndfra.Text & " and month(datostart)<=" & Uttaksrapport_form.rappmndtil.Text & " and year(datostart)=" & Uttaksrapport_form.rappaar & " group by day(datostart) order by day(datostart)"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_26.log
18:18:55:03.6030000  :  CutterSupport          = 0
29:18:55:03.6186250  :  TCPIPSERVER-----------------------------------------
30:18:55:03.6186250  :  SocketListener         = 0
31:18:55:03.6186250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_14.log
18:11:27:14.1651250  :  CutterSupport          = 0
29:11:27:14.1651250  :  TCPIPSERVER-----------------------------------------
30:11:27:14.1651250  :  SocketListener         = 0
31:11:27:14.1651250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_16.log
18:04:16:56.5156250  :  CutterSupport          = 0
29:04:16:56.5156250  :  TCPIPSERVER-----------------------------------------
30:04:16:56.5156250  :  SocketListener         = 0
31:04:16:56.5156250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/stasjonskreditt_rapport_form.frm
3:   Caption         =   "Rapporter"
11:   Begin VB.ComboBox rapport
18:   Begin VB.CommandButton hentrapport
19:      Caption         =   "Hent Rapport"
28:      ItemData        =   "stasjonskreditt_rapport_form.frx":0000
30:      List            =   "stasjonskreditt_rapport_form.frx":0025
37:      ItemData        =   "stasjonskreditt_rapport_form.frx":006B
39:      List            =   "stasjonskreditt_rapport_form.frx":0093
46:      ItemData        =   "stasjonskreditt_rapport_form.frx":00C7
48:      List            =   "stasjonskreditt_rapport_form.frx":00EF
98:rapport.Clear
106:rapport.AddItem lpgnorge.rskunder!Kunde
128:Private Sub hentrapport_Click()
131:lpgnorge.Commands.Item("Stasjontank").CommandText = "Select datostart,liter,pris,sum,sumekslrab=sum from stasjonskreditt_tankinger where kundeid=" & i(rapport.ListIndex + 1) & " and month(datostart) >=" & rappmndfra.Text & " and month(datostart) <=" & rappmndtil.Text & " and year(datostart)=" & rappaar.Text & " order by datostart"
138:Load uttaksrapport
140:uttaksrapport.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/server.frm
15:      DataField       =   "Pathexportautogas"
89:      DataField       =   "RFID_comport"
155:      DataField       =   "Pinpad_comport"
177:      DataField       =   "Paymentpinpad_comport"
187:   Begin VB.TextBox txtcom_port
188:      DataField       =   "Disp_comport"
249:      Caption         =   "Eksport bane Autogas"
345:      Caption         =   "Comport RFID"
427:      Caption         =   "Comport Pinpad"
435:      Caption         =   "Comport printer"
443:      Caption         =   "Comport bank"
451:      Caption         =   "Comport dispenser :"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_03.log
18:17:31:50.7968750  :  CutterSupport          = 0
29:17:31:50.7968750  :  TCPIPSERVER-----------------------------------------
30:17:31:50.7968750  :  SocketListener         = 0
31:17:31:50.7968750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/ftpcmd_til.txt
4:put \\lpgromerike_pos\deltefiler\cardexport.txt /synch/ebgass/Fra/Cards.txt

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_17.log
18:23:58:03.3651345  :  CutterSupport          = 0
29:23:58:03.3651345  :  TCPIPSERVER-----------------------------------------
30:23:58:03.3651345  :  SocketListener         = 0
31:23:58:03.3651345  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_10.log
18:10:03:23.8906250  :  CutterSupport          = 0
29:10:03:23.8906250  :  TCPIPSERVER-----------------------------------------
30:10:03:23.8906250  :  SocketListener         = 0
31:10:03:23.8906250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_30.log
18:13:05:45.9062500  :  CutterSupport          = 0
29:13:05:45.9062500  :  TCPIPSERVER-----------------------------------------
30:13:05:45.9062500  :  SocketListener         = 0
31:13:05:45.9062500  :  SocketListenerPort     = 6001
455:16:40:30.6718750  :  CutterSupport          = 0
466:16:40:30.6718750  :  TCPIPSERVER-----------------------------------------
467:16:40:30.6718750  :  SocketListener         = 0
468:16:40:30.6718750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Omsetningprdag.Dsr
14:   ReportWidth     =   8382
37:      Name            =   "ReportHeader"
277:      Name            =   "ReportFooter"
430:Private Sub DataReport_Initialize()
431:Me.Sections("Reportheader").Controls("Label10").Caption = "Periode :" & omsetning_form.rappmndfra & " - " & omsetning_form.rappmndtil & " �: " & omsetning_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_05.log
18:07:43:30.4687500  :  CutterSupport          = 0
29:07:43:30.4687500  :  TCPIPSERVER-----------------------------------------
30:07:43:30.4687500  :  SocketListener         = 0
31:07:43:30.4687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_04.log
18:11:05:44.0156250  :  CutterSupport          = 0
29:11:05:44.0156250  :  TCPIPSERVER-----------------------------------------
30:11:05:44.0156250  :  SocketListener         = 0
31:11:05:44.0156250  :  SocketListenerPort     = 6001
277:14:37:17.8281250  :  CutterSupport          = 0
288:14:37:17.8281250  :  TCPIPSERVER-----------------------------------------
289:14:37:17.8281250  :  SocketListener         = 0
290:14:37:17.8281250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_04.log
18:11:05:44  :  CutterSupport          = 0
29:11:05:44  :  TCPIPSERVER-----------------------------------------
30:11:05:44  :  SocketListener         = 0
31:11:05:44  :  SocketListenerPort     = 6001
376:14:37:18  :  CutterSupport          = 0
387:14:37:18  :  TCPIPSERVER-----------------------------------------
388:14:37:18  :  SocketListener         = 0
389:14:37:18  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/DataReport1.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} DataReport1
3:   Caption         =   "DataReport1"
65:Attribute VB_Name = "DataReport1"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_25.log
18:09:02:44.0561250  :  CutterSupport          = 0
29:09:02:44.0561250  :  TCPIPSERVER-----------------------------------------
30:09:02:44.0561250  :  SocketListener         = 0
31:09:02:44.0561250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_17.log
18:23:58:03.3963845  :  CutterSupport          = 0
29:23:58:03.3963845  :  TCPIPSERVER-----------------------------------------
30:23:58:03.3963845  :  SocketListener         = 0
31:23:58:03.3963845  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_24.log
18:15:14:47.7214115  :  CutterSupport          = 0
29:15:14:47.7214115  :  TCPIPSERVER-----------------------------------------
30:15:14:47.7214115  :  SocketListener         = 0
31:15:14:47.7214115  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_18.log
18:12:25:58.5051250  :  CutterSupport          = 0
29:12:25:58.5051250  :  TCPIPSERVER-----------------------------------------
30:12:25:58.5051250  :  SocketListener         = 0
31:12:25:58.5051250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/Dispenserkontroll.frm
157:   Begin MSWinsockLib.Winsock tcpclient
404:   Begin VB.Menu Rapporter
422:   Begin VB.Menu Rapporter
423:      Caption         =   "Rapporter"
425:      Begin VB.Menu Omsetningsrapport
426:         Caption         =   "Omsetningsrapport"
429:      Begin VB.Menu uttaksrapport
430:         Caption         =   "Uttaksrapport"
433:      Begin VB.Menu stasjonskreditt_rapport
461:Dim sendtext As String
540:Private Sub Omsetningsrapport_Click(Index As Integer)
767:Private Sub uttaksrapport_Click(Index As Integer)
768:Uttaksrapport_form.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_10.log
18:10:03:23.8750000  :  CutterSupport          = 0
29:10:03:23.8750000  :  TCPIPSERVER-----------------------------------------
30:10:03:23.8750000  :  SocketListener         = 0
31:10:03:23.8750000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_03.log
18:17:31:50.7812500  :  CutterSupport          = 0
29:17:31:50.7812500  :  TCPIPSERVER-----------------------------------------
30:17:31:50.7812500  :  SocketListener         = 0
31:17:31:50.7812500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/ADMINI~1.log
2:Line 25: Class MSDBCtls.DBCombo of control zrapportkopi was not a loaded control class.
3:Line 36: Class MSDBCtls.DBCombo of control xrapportkopi was not a loaded control class.

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_11.log
18:23:58:52.8065020  :  CutterSupport          = 0
29:23:58:52.8065020  :  TCPIPSERVER-----------------------------------------
30:23:58:52.8065020  :  SocketListener         = 0
31:23:58:52.8065020  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_23.log
18:09:26:13.1875000  :  CutterSupport          = 0
29:09:26:13.1875000  :  TCPIPSERVER-----------------------------------------
30:09:26:13.1875000  :  SocketListener         = 0
31:09:26:13.1875000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_06.log
18:13:44:34.4375000  :  CutterSupport          = 0
29:13:44:34.4375000  :  TCPIPSERVER-----------------------------------------
30:13:44:34.4375000  :  SocketListener         = 0
31:13:44:34.4375000  :  SocketListenerPort     = 6001
76:17:25:32.1562500  :  CutterSupport          = 0
87:17:25:32.1562500  :  TCPIPSERVER-----------------------------------------
88:17:25:32.1562500  :  SocketListener         = 0
89:17:25:32.1562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_19.log
18:10:41:10.4426250  :  CutterSupport          = 0
29:10:41:10.4426250  :  TCPIPSERVER-----------------------------------------
30:10:41:10.4426250  :  SocketListener         = 0
31:10:41:10.4426250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/stasjonskreditt_rapport_form.log
6:Line 94: Property List in rapporttype could not be set.
7:Line 94: Property ItemData in rapporttype could not be set.

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_31.log
18:23:58:55.6562500  :  CutterSupport          = 0
29:23:58:55.6562500  :  TCPIPSERVER-----------------------------------------
30:23:58:55.6562500  :  SocketListener         = 0
31:23:58:55.6562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_05.log
18:07:43:30.4375000  :  CutterSupport          = 0
29:07:43:30.4375000  :  TCPIPSERVER-----------------------------------------
30:07:43:30.4375000  :  SocketListener         = 0
31:07:43:30.4375000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_27.log
18:15:11:37.4062500  :  CutterSupport          = 0
29:15:11:37.4062500  :  TCPIPSERVER-----------------------------------------
30:15:11:37.4062500  :  SocketListener         = 0
31:15:11:37.4062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_11.log
18:23:58:17.9687500  :  CutterSupport          = 0
29:23:58:17.9687500  :  TCPIPSERVER-----------------------------------------
30:23:58:17.9687500  :  SocketListener         = 0
31:23:58:17.9687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_16.log
18:04:16:56.5312500  :  CutterSupport          = 0
29:04:16:56.5312500  :  TCPIPSERVER-----------------------------------------
30:04:16:56.5312500  :  SocketListener         = 0
31:04:16:56.5312500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_22.log
18:11:24:19.6562500  :  CutterSupport          = 0
29:11:24:19.6562500  :  TCPIPSERVER-----------------------------------------
30:11:24:19.6562500  :  SocketListener         = 0
31:11:24:19.6562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_23.log
18:13:37:43.0676250  :  CutterSupport          = 0
29:13:37:43.0676250  :  TCPIPSERVER-----------------------------------------
30:13:37:43.0676250  :  SocketListener         = 0
31:13:37:43.0676250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra Moss2/klienttest/innstillinger.frm
127:      Caption         =   "IPadresse kamera :"
135:      Caption         =   "IPadresse betalingsterminalterminal :"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_17.log
18:13:54:20.4776250  :  CutterSupport          = 0
29:13:54:20.4776250  :  TCPIPSERVER-----------------------------------------
30:13:54:20.4776250  :  SocketListener         = 0
31:13:54:20.4776250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_02.log
18:23:58:12  :  CutterSupport          = 0
29:23:58:12  :  TCPIPSERVER-----------------------------------------
30:23:58:12  :  SocketListener         = 0
31:23:58:12  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_08.log
18:20:03:38.5781250  :  CutterSupport          = 0
29:20:03:38.5781250  :  TCPIPSERVER-----------------------------------------
30:20:03:38.5781250  :  SocketListener         = 0
31:20:03:38.5781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_19.log
18:10:41:10.0988750  :  CutterSupport          = 0
29:10:41:10.0988750  :  TCPIPSERVER-----------------------------------------
30:10:41:10.0988750  :  SocketListener         = 0
31:10:41:10.0988750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_25.log
18:09:02:43.7592500  :  CutterSupport          = 0
29:09:02:43.7592500  :  TCPIPSERVER-----------------------------------------
30:09:02:43.7592500  :  SocketListener         = 0
31:09:02:43.7592500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_03.log
18:12:11:17.6250000  :  CutterSupport          = 0
29:12:11:17.6250000  :  TCPIPSERVER-----------------------------------------
30:12:11:17.6250000  :  SocketListener         = 0
31:12:11:17.6250000  :  SocketListenerPort     = 6001
83:12:11:32.6875000  :  Fire_OnLocalMode     :: D  03410651******8027;20160503121125;0;028;104402000704;;;;;;;14139877;533955;Visa SpareBank 1;00;IB1;A0000000031010;0000008000;F800;009B;140101;0000000000;;;;Inactive;{"od":{"ver":"1.01","preauth":{"ver":"1.0","auth":{"ver":"1.0","token":{"ver":"1.0","t":"RIofzToOT7apuaaV4UpI6LtIPWI=","e":"160602"}},"data":{"ver":"1.0","id":3,"tpan":"410651******8027","ref":"104402 000704","TCC":"IB1","resp":"00"},"receipt":{"ver":"1.0","cdt":{"ver":"1.0","prnstr":["Visa SpareBank 1 ","************8027-2","AID: A0000000031010","TVR: 0000008000","TSI: F800"]}}}}}

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_29.log
18:17:30:26.5781250  :  CutterSupport          = 0
29:17:30:26.5781250  :  TCPIPSERVER-----------------------------------------
30:17:30:26.5781250  :  SocketListener         = 0
31:17:30:26.5781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_23.log
18:09:26:13.7031250  :  CutterSupport          = 0
29:09:26:13.7031250  :  TCPIPSERVER-----------------------------------------
30:09:26:13.7031250  :  SocketListener         = 0
31:09:26:13.7031250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_20.log
18:23:58:26.7238750  :  CutterSupport          = 0
29:23:58:26.7238750  :  TCPIPSERVER-----------------------------------------
30:23:58:26.7238750  :  SocketListener         = 0
31:23:58:26.7238750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_15.log
18:21:51:09.1495000  :  CutterSupport          = 0
29:21:51:09.1495000  :  TCPIPSERVER-----------------------------------------
30:21:51:09.1495000  :  SocketListener         = 0
31:21:51:09.1495000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_22.log
18:23:58:41.4738750  :  CutterSupport          = 0
29:23:58:41.4738750  :  TCPIPSERVER-----------------------------------------
30:23:58:41.4738750  :  SocketListener         = 0
31:23:58:41.4738750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_02.log
18:02:06:21.3593750  :  CutterSupport          = 0
29:02:06:21.3593750  :  TCPIPSERVER-----------------------------------------
30:02:06:21.3593750  :  SocketListener         = 0
31:02:06:21.3593750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_01.log
18:10:36:16.7812500  :  CutterSupport          = 0
29:10:36:16.7812500  :  TCPIPSERVER-----------------------------------------
30:10:36:16.7812500  :  SocketListener         = 0
31:10:36:16.7812500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_16.log
18:08:55:45.3370000  :  CutterSupport          = 0
29:08:55:45.3370000  :  TCPIPSERVER-----------------------------------------
30:08:55:45.3370000  :  SocketListener         = 0
31:08:55:45.3370000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_10.log
18:11:05:21.3906250  :  CutterSupport          = 0
29:11:05:21.3906250  :  TCPIPSERVER-----------------------------------------
30:11:05:21.3906250  :  SocketListener         = 0
31:11:05:21.3906250  :  SocketListenerPort     = 6001
69:11:08:14.8437500  :  CutterSupport          = 0
80:11:08:14.8437500  :  TCPIPSERVER-----------------------------------------
81:11:08:14.8437500  :  SocketListener         = 0
82:11:08:14.8437500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_06.log
18:13:44:34.2500000  :  CutterSupport          = 0
29:13:44:34.2500000  :  TCPIPSERVER-----------------------------------------
30:13:44:34.2500000  :  SocketListener         = 0
31:13:44:34.2500000  :  SocketListenerPort     = 6001
233:17:25:32.0781250  :  CutterSupport          = 0
244:17:25:32.0781250  :  TCPIPSERVER-----------------------------------------
245:17:25:32.0781250  :  SocketListener         = 0
246:17:25:32.0781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/uttaksrapport.Dsr
3:   Bindings        =   "uttaksrapport.dsx":0000
4:   Caption         =   "Uttaksrapport"
14:   ReportWidth     =   8397
410:Private Sub DataReport_Initialize()
411:Me.Sections("Section4").Controls("Label10").Caption = "Periode :" & Uttaksrapport_form.rappmndfra & " - " & Uttaksrapport_form.rappmndtil & " �: " & Uttaksrapport_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_04.log
18:11:05:44.4062500  :  CutterSupport          = 0
29:11:05:44.4062500  :  TCPIPSERVER-----------------------------------------
30:11:05:44.4062500  :  SocketListener         = 0
31:11:05:44.4062500  :  SocketListenerPort     = 6001
84:14:37:18.0781250  :  CutterSupport          = 0
95:14:37:18.0781250  :  TCPIPSERVER-----------------------------------------
96:14:37:18.0781250  :  SocketListener         = 0
97:14:37:18.0781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_27.log
18:15:11:36.9062500  :  CutterSupport          = 0
29:15:11:36.9062500  :  TCPIPSERVER-----------------------------------------
30:15:11:36.9062500  :  SocketListener         = 0
31:15:11:36.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_30.log
18:23:58:45.4687500  :  CutterSupport          = 0
29:23:58:45.4687500  :  TCPIPSERVER-----------------------------------------
30:23:58:45.4687500  :  SocketListener         = 0
31:23:58:45.4687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_12.log
18:23:58:18.7500000  :  CutterSupport          = 0
29:23:58:18.7500000  :  TCPIPSERVER-----------------------------------------
30:23:58:18.7500000  :  SocketListener         = 0
31:23:58:18.7500000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_10.log
18:10:03:24.9062500  :  CutterSupport          = 0
29:10:03:24.9062500  :  TCPIPSERVER-----------------------------------------
30:10:03:24.9062500  :  SocketListener         = 0
31:10:03:24.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_12.log
18:23:58:18.7812500  :  CutterSupport          = 0
29:23:58:18.7812500  :  TCPIPSERVER-----------------------------------------
30:23:58:18.7812500  :  SocketListener         = 0
31:23:58:18.7812500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_27.log
18:15:11:36.9218750  :  CutterSupport          = 0
29:15:11:36.9218750  :  TCPIPSERVER-----------------------------------------
30:15:11:36.9218750  :  SocketListener         = 0
31:15:11:36.9218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/server.frm
23:   Begin VB.TextBox txtcom_port
67:      Caption         =   "Comport bank"
75:      Caption         =   "Comport dispenser :"
125:Print #1, Me.servernavn & ";" & Me.serverdb & ";" & Me.serverbrukernavn & ";" & Me.serverpassord & ";" & Me.txtcom_port & ";" & Me.txtcom_bank
130:Com_port = Me.txtcom_port
131:Com_port_bank = Me.txtcom_bank

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_06.log
18:13:44:34.2343750  :  CutterSupport          = 0
29:13:44:34.2343750  :  TCPIPSERVER-----------------------------------------
30:13:44:34.2343750  :  SocketListener         = 0
31:13:44:34.2343750  :  SocketListenerPort     = 6001
305:17:25:32.0937500  :  CutterSupport          = 0
316:17:25:32.0937500  :  TCPIPSERVER-----------------------------------------
317:17:25:32.0937500  :  SocketListener         = 0
318:17:25:32.0937500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_05.log
18:08:08:22.6093750  :  CutterSupport          = 0
29:08:08:22.6093750  :  TCPIPSERVER-----------------------------------------
30:08:08:22.6093750  :  SocketListener         = 0
31:08:08:22.6093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_04.log
18:07:09:53.6093750  :  CutterSupport          = 0
29:07:09:53.6093750  :  TCPIPSERVER-----------------------------------------
30:07:09:53.6093750  :  SocketListener         = 0
31:07:09:53.6093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_29.log
18:17:30:27.3437500  :  CutterSupport          = 0
29:17:30:27.3437500  :  TCPIPSERVER-----------------------------------------
30:17:30:27.3437500  :  SocketListener         = 0
31:17:30:27.3437500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_30.log
18:23:58:45.5000000  :  CutterSupport          = 0
29:23:58:45.5000000  :  TCPIPSERVER-----------------------------------------
30:23:58:45.5000000  :  SocketListener         = 0
31:23:58:45.5000000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_01.log
18:10:36:16.7656250  :  CutterSupport          = 0
29:10:36:16.7656250  :  TCPIPSERVER-----------------------------------------
30:10:36:16.7656250  :  SocketListener         = 0
31:10:36:16.7656250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/omsetning_form.frm
4:   Caption         =   "Omsetningsrapport"
45:   Begin VB.CommandButton hentrapport
46:      Caption         =   "Hent Rapport"
96:Private Sub hentrapport_Click()
100:.CommandText = "SELECT DAY(dato) AS Expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Belp%', reporttext) + 9, 15)), ',', '.') AS float)) AS sum1 Into presalg From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Belp%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato)"
107:.CommandText = "SELECT DAY(dato) AS expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Retur%', reporttext) + 9, 15)), ',', '.') AS float)) AS retur Into retur From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Retur%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato) "

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_20.log
18:23:58:26.7707500  :  CutterSupport          = 0
29:23:58:26.7707500  :  TCPIPSERVER-----------------------------------------
30:23:58:26.7707500  :  SocketListener         = 0
31:23:58:26.7707500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_09.log
18:12:53:15.1875000  :  CutterSupport          = 0
29:12:53:15.1875000  :  TCPIPSERVER-----------------------------------------
30:12:53:15.1875000  :  SocketListener         = 0
31:12:53:15.1875000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_23.log
18:13:37:42.4113750  :  CutterSupport          = 0
29:13:37:42.4113750  :  TCPIPSERVER-----------------------------------------
30:13:37:42.4113750  :  SocketListener         = 0
31:13:37:42.4113750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_15.log
18:21:51:09.1338750  :  CutterSupport          = 0
29:21:51:09.1338750  :  TCPIPSERVER-----------------------------------------
30:21:51:09.1338750  :  SocketListener         = 0
31:21:51:09.1338750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_08.log
18:20:03:39.1562500  :  CutterSupport          = 0
29:20:03:39.1562500  :  TCPIPSERVER-----------------------------------------
30:20:03:39.1562500  :  SocketListener         = 0
31:20:03:39.1562500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_24.log
18:14:38:20.5781250  :  CutterSupport          = 0
29:14:38:20.5781250  :  TCPIPSERVER-----------------------------------------
30:14:38:20.5781250  :  SocketListener         = 0
31:14:38:20.5781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_21.log
18:10:40:22.4218750  :  CutterSupport          = 0
29:10:40:22.4218750  :  TCPIPSERVER-----------------------------------------
30:10:40:22.4218750  :  SocketListener         = 0
31:10:40:22.4218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/rapporter_form.frm
6:   Caption         =   "Rapporter"
43:      TextRTF         =   $"rapporter_form.frx":0000
46:      Bindings        =   "rapporter_form.frx":0082
106:         DataField       =   "reporttext"
107:         Caption         =   "reporttext"
119:         DataField       =   "reportid"
120:         Caption         =   "Reportid"
153:      Caption         =   "Velg rapportdato:"
168:RichTextBox1.Text = DataGrid1.Columns("reporttext").Text

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/pumpekontroll.frm
776:    Com_port = cfgline(4)
777:    Com_port_bank = cfgline(5)
778:    com_port_print = cfgline(6)
808:    com_print.CommPort = com_port_print
831:If Com_port_bank <> 0 Then baxi.Close
837:    MsgBox "Feil ved lukking av kommunikasjonsport.", vbOKOnly, "Kommunikasjonsfeil"
923:MSComm1.CommPort = Com_port
950:MsgBox "Feil ved lukking av comport, restart av maskinen ved �gjre den strmls er anbefalt:" & Err.Number & " " & Err.Description

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_14.log
18:11:27:14.1338750  :  CutterSupport          = 0
29:11:27:14.1338750  :  TCPIPSERVER-----------------------------------------
30:11:27:14.1338750  :  SocketListener         = 0
31:11:27:14.1338750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/rapporter_form.frm
6:   Caption         =   "Rapporter"
15:      Bindings        =   "rapporter_form.frx":0000
29:      TextRTF         =   $"rapporter_form.frx":0017
102:      TextRTF         =   $"rapporter_form.frx":0099
164:         DataField       =   "reporttext"
269:      Caption         =   "Velg rapportdato:"
285:    lpgnorge.rskvittering_pr_dag.Filter = "reporttext like '%" & bankrappsoktekst.Text & "%'"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Tankinger_form.frm
5:   Caption         =   "Oppslag rapporter"
233:      Caption         =   "Velg rapportdato:"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_18.log
18:08:30:45.1026250  :  CutterSupport          = 0
29:08:30:45.1026250  :  TCPIPSERVER-----------------------------------------
30:08:30:45.1026250  :  SocketListener         = 0
31:08:30:45.1026250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_19.log
18:20:12:31.9180000  :  CutterSupport          = 0
29:20:12:31.9180000  :  TCPIPSERVER-----------------------------------------
30:20:12:31.9180000  :  SocketListener         = 0
31:20:12:31.9180000  :  SocketListenerPort     = 6001
143:20:17:52.5000000  :  CutterSupport          = 0
154:20:17:52.5000000  :  TCPIPSERVER-----------------------------------------
155:20:17:52.5000000  :  SocketListener         = 0
156:20:17:52.5000000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/EHL4x.vbp
25:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Dispenserkontroll.vbp
10:Form=rapporter_form.frm
15:Reference=*\G{642AC760-AAB4-11D0-8494-00A0C90DC8A9}#1.0#0#..\..\WINDOWS\system32\MSDBRPTR.DLL#Microsoft Data Report Designer 6.0 (SP4)
22:Form=Uttaksrapport.frm
23:Designer=uttaksrapport.Dsr
25:Designer=Stasjonskredittrapport.Dsr
26:Form=stasjonskreditt_rapport_form.frm
43:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_26.log
18:04:49:32.1093750  :  CutterSupport          = 0
29:04:49:32.1093750  :  TCPIPSERVER-----------------------------------------
30:04:49:32.1093750  :  SocketListener         = 0
31:04:49:32.1093750  :  SocketListenerPort     = 6001
52:04:51:26.1718750  :  CutterSupport          = 0
63:04:51:26.1718750  :  TCPIPSERVER-----------------------------------------
64:04:51:26.1718750  :  SocketListener         = 0
65:04:51:26.1718750  :  SocketListenerPort     = 6001
97:04:51:46.5625000  :  CutterSupport          = 0
108:04:51:46.5625000  :  TCPIPSERVER-----------------------------------------
109:04:51:46.5625000  :  SocketListener         = 0
110:04:51:46.5625000  :  SocketListenerPort     = 6001
142:05:10:03.9062500  :  CutterSupport          = 0
153:05:10:03.9062500  :  TCPIPSERVER-----------------------------------------
154:05:10:03.9062500  :  SocketListener         = 0
155:05:10:03.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_25.log
18:23:58:03.9062500  :  CutterSupport          = 0
29:23:58:03.9062500  :  TCPIPSERVER-----------------------------------------
30:23:58:03.9062500  :  SocketListener         = 0
31:23:58:03.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_13.log
18:18:11:10.0468750  :  CutterSupport          = 0
29:18:11:10.0468750  :  TCPIPSERVER-----------------------------------------
30:18:11:10.0468750  :  SocketListener         = 0
31:18:11:10.0468750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_22.log
18:23:58:41.4426250  :  CutterSupport          = 0
29:23:58:41.4426250  :  TCPIPSERVER-----------------------------------------
30:23:58:41.4426250  :  SocketListener         = 0
31:23:58:41.4426250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/pumpcontrol.vbp
27:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_07.log
18:23:58:44.1093750  :  CutterSupport          = 0
29:23:58:44.1093750  :  TCPIPSERVER-----------------------------------------
30:23:58:44.1093750  :  SocketListener         = 0
31:23:58:44.1093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_28.log
18:00:24:11.9062500  :  CutterSupport          = 0
29:00:24:11.9062500  :  TCPIPSERVER-----------------------------------------
30:00:24:11.9062500  :  SocketListener         = 0
31:00:24:11.9062500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_31.log
18:23:58:55.6875000  :  CutterSupport          = 0
29:23:58:55.6875000  :  TCPIPSERVER-----------------------------------------
30:23:58:55.6875000  :  SocketListener         = 0
31:23:58:55.6875000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_26.log
18:04:49:32.1718750  :  CutterSupport          = 0
29:04:49:32.1718750  :  TCPIPSERVER-----------------------------------------
30:04:49:32.1718750  :  SocketListener         = 0
31:04:49:32.1718750  :  SocketListenerPort     = 6001
52:04:51:26.2656250  :  CutterSupport          = 0
63:04:51:26.2656250  :  TCPIPSERVER-----------------------------------------
64:04:51:26.2656250  :  SocketListener         = 0
65:04:51:26.2656250  :  SocketListenerPort     = 6001
87:04:51:46.6562500  :  CutterSupport          = 0
98:04:51:46.6562500  :  TCPIPSERVER-----------------------------------------
99:04:51:46.6562500  :  SocketListener         = 0
100:04:51:46.6562500  :  SocketListenerPort     = 6001
122:05:10:03.9843750  :  CutterSupport          = 0
133:05:10:03.9843750  :  TCPIPSERVER-----------------------------------------
134:05:10:03.9843750  :  SocketListener         = 0
135:05:10:03.9843750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_21.log
18:07:15:51.2863750  :  CutterSupport          = 0
29:07:15:51.2863750  :  TCPIPSERVER-----------------------------------------
30:07:15:51.2863750  :  SocketListener         = 0
31:07:15:51.2863750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_10.log
18:11:05:21.8750000  :  CutterSupport          = 0
29:11:05:21.8750000  :  TCPIPSERVER-----------------------------------------
30:11:05:21.8750000  :  SocketListener         = 0
31:11:05:21.8750000  :  SocketListenerPort     = 6001
52:11:08:14.9218750  :  CutterSupport          = 0
63:11:08:14.9218750  :  TCPIPSERVER-----------------------------------------
64:11:08:14.9218750  :  SocketListener         = 0
65:11:08:14.9218750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_24.log
18:14:38:21.5468750  :  CutterSupport          = 0
29:14:38:21.5468750  :  TCPIPSERVER-----------------------------------------
30:14:38:21.5468750  :  SocketListener         = 0
31:14:38:21.5468750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/lpgnorge.Dsr
16:      ConnectionName  =   "Rapporter"
31:      ActiveConnectionName=   "Rapporter"
41:         Name            =   "reportid"
42:         Caption         =   "reportid"
49:         Name            =   "reporttext"
50:         Caption         =   "reporttext"
122:      ActiveConnectionName=   "Rapporter"
136:      ActiveConnectionName=   "Rapporter"
149:      ActiveConnectionName=   "Rapporter"
163:      ActiveConnectionName=   "Rapporter"
176:      ActiveConnectionName=   "Rapporter"
206:      ActiveConnectionName=   "Rapporter"
340:      ActiveConnectionName=   "Rapporter"
468:      ActiveConnectionName=   "Rapporter"
538:      ActiveConnectionName=   "Rapporter"
570:      ActiveConnectionName=   "Rapporter"
656:      ActiveConnectionName=   "Rapporter"
728:      ActiveConnectionName=   "Rapporter"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_04.log
18:07:09:54.1093750  :  CutterSupport          = 0
29:07:09:54.1093750  :  TCPIPSERVER-----------------------------------------
30:07:09:54.1093750  :  SocketListener         = 0
31:07:09:54.1093750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_18.log
18:08:30:45.7432500  :  CutterSupport          = 0
29:08:30:45.7432500  :  TCPIPSERVER-----------------------------------------
30:08:30:45.7432500  :  SocketListener         = 0
31:08:30:45.7432500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_09.log
18:12:34:03.2968750  :  CutterSupport          = 0
29:12:34:03.2968750  :  TCPIPSERVER-----------------------------------------
30:12:34:03.2968750  :  SocketListener         = 0
31:12:34:03.2968750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/lpgnorge.Dsr
427:         Name            =   "Transportmaate"
428:         Caption         =   "Transportmaate"
435:         Name            =   "Transport"
436:         Caption         =   "Transport"
443:         Name            =   "Transportoer"
444:         Caption         =   "Transportoer"
619:         Name            =   "ExportHandTerm"
620:         Caption         =   "ExportHandTerm"
1428:      CommandText     =   "Select * from cashback where reported=0"
1471:         Name            =   "Reported"
1472:         Caption         =   "Reported"
1581:         Name            =   "reportid"
1582:         Caption         =   "reportid"
1589:         Name            =   "reporttext"
1590:         Caption         =   "reporttext"
1734:         Name            =   "Pathexportautogas"
1735:         Caption         =   "Pathexportautogas"
1742:         Name            =   "Disp_comport"
1743:         Caption         =   "Disp_comport"
1766:         Name            =   "Paymentpinpad_comport"
1767:         Caption         =   "Paymentpinpad_comport"
1774:         Name            =   "Reciptprinter_comport"
1775:         Caption         =   "Reciptprinter_comport"
1790:         Name            =   "Pinpad_comport"
1791:         Caption         =   "Pinpad_comport"
1830:         Name            =   "RFID_comport"
1831:         Caption         =   "RFID_comport"
2936:      CommandText     =   "dbo.rapporter_bankterminal"
2948:         Name            =   "reportid"
2949:         Caption         =   "reportid"
2956:         Name            =   "reporttext"
2957:         Caption         =   "reporttext"
3116:         Name            =   "zrapport"
3117:         Caption         =   "zrapport"
3124:         Name            =   "xrapport"
3125:         Caption         =   "xrapport"
3575:         Name            =   "Zrapportsum"
3576:         Caption         =   "Zrapportsum"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_07.log
18:23:58:44.1718750  :  CutterSupport          = 0
29:23:58:44.1718750  :  TCPIPSERVER-----------------------------------------
30:23:58:44.1718750  :  SocketListener         = 0
31:23:58:44.1718750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_12.log
18:23:58:18.8281250  :  CutterSupport          = 0
29:23:58:18.8281250  :  TCPIPSERVER-----------------------------------------
30:23:58:18.8281250  :  SocketListener         = 0
31:23:58:18.8281250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_13.log
18:18:11:11.0625000  :  CutterSupport          = 0
29:18:11:11.0625000  :  TCPIPSERVER-----------------------------------------
30:18:11:11.0625000  :  SocketListener         = 0
31:18:11:11.0625000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/omsetning_form.log
6:Line 52: Property List in rapporttype could not be set.
7:Line 52: Property ItemData in rapporttype could not be set.

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_11.log
18:23:58:17.9375000  :  CutterSupport          = 0
29:23:58:17.9375000  :  TCPIPSERVER-----------------------------------------
30:23:58:17.9375000  :  SocketListener         = 0
31:23:58:17.9375000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Uttaksrapport.frm
2:Begin VB.Form Uttaksrapport_form
3:   Caption         =   "Uttaksrapport"
12:   Begin VB.CommandButton hentrapport
13:      Caption         =   "Hent Rapport"
22:      ItemData        =   "Uttaksrapport.frx":0000
24:      List            =   "Uttaksrapport.frx":0028
32:      ItemData        =   "Uttaksrapport.frx":005C
34:      List            =   "Uttaksrapport.frx":0084
42:      ItemData        =   "Uttaksrapport.frx":00B8
44:      List            =   "Uttaksrapport.frx":00DD
75:Attribute VB_Name = "Uttaksrapport_form"
93:Private Sub hentrapport_Click()
98:.CommandText = "select dag=day(datostart), isnull(sum(liter),0) as liter from tankinger where month(datostart)>=" & Uttaksrapport_form.rappmndfra.Text & " and month(datostart)<=" & Uttaksrapport_form.rappmndtil.Text & " and year(datostart)=" & Uttaksrapport_form.rappaar & " group by day(datostart) order by day(datostart)"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_11.log
18:23:58:18.0156250  :  CutterSupport          = 0
29:23:58:18.0156250  :  TCPIPSERVER-----------------------------------------
30:23:58:18.0156250  :  SocketListener         = 0
31:23:58:18.0156250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_05.log
18:08:08:23.3125000  :  CutterSupport          = 0
29:08:08:23.3125000  :  TCPIPSERVER-----------------------------------------
30:08:08:23.3125000  :  SocketListener         = 0
31:08:08:23.3125000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/stasjonskreditt_rapport_form.frm
3:   Caption         =   "Rapporter"
11:   Begin VB.ComboBox rapport
19:   Begin VB.CommandButton hentrapport
20:      Caption         =   "Hent Rapport"
29:      ItemData        =   "stasjonskreditt_rapport_form.frx":0000
31:      List            =   "stasjonskreditt_rapport_form.frx":0025
38:      ItemData        =   "stasjonskreditt_rapport_form.frx":006B
40:      List            =   "stasjonskreditt_rapport_form.frx":0093
47:      ItemData        =   "stasjonskreditt_rapport_form.frx":00C7
49:      List            =   "stasjonskreditt_rapport_form.frx":00EF
99:rapport.Clear
107:rapport.AddItem lpgnorge.rskunder!Kunde
129:Private Sub hentrapport_Click()
132:lpgnorge.Commands.Item("Stasjontank").CommandText = "Select datostart,liter,pris,sum,sumekslrab=sum from stasjonskreditt_tankinger where kundeid=" & i(rapport.ListIndex + 1) & " and month(datostart) >=" & rappmndfra.Text & " and month(datostart) <=" & rappmndtil.Text & " and year(datostart)=" & rappaar.Text & " order by datostart"
139:Load uttaksrapport
141:uttaksrapport.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_05.log
18:08:08:22.6250000  :  CutterSupport          = 0
29:08:08:22.6250000  :  TCPIPSERVER-----------------------------------------
30:08:08:22.6250000  :  SocketListener         = 0
31:08:08:22.6250000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_30.log
18:23:58:45.4375000  :  CutterSupport          = 0
29:23:58:45.4375000  :  TCPIPSERVER-----------------------------------------
30:23:58:45.4375000  :  SocketListener         = 0
31:23:58:45.4375000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_19.log
18:20:12:32.4648750  :  CutterSupport          = 0
29:20:12:32.4648750  :  TCPIPSERVER-----------------------------------------
30:20:12:32.4648750  :  SocketListener         = 0
31:20:12:32.4648750  :  SocketListenerPort     = 6001
63:20:17:52.7500000  :  CutterSupport          = 0
74:20:17:52.7500000  :  TCPIPSERVER-----------------------------------------
75:20:17:52.7500000  :  SocketListener         = 0
76:20:17:52.7500000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_31.log
18:23:58:55.6250000  :  CutterSupport          = 0
29:23:58:55.6250000  :  TCPIPSERVER-----------------------------------------
30:23:58:55.6250000  :  SocketListener         = 0
31:23:58:55.6250000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Stasjonskredittrapport.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} uttaksrapport
3:   Bindings        =   "Stasjonskredittrapport.dsx":0000
4:   Caption         =   "Uttaksrapport pr kunde"
14:   ReportWidth     =   8715
36:      Name            =   "ReportHeader"
56:         Object.Caption         =   "Uttaksrapport "
377:      Name            =   "ReportFooter"
480:Attribute VB_Name = "uttaksrapport"
486:Private Sub DataReport_Initialize()
487:Me.Sections("Reportheader").Controls("Label8").Caption = "Periode :" & stasjonskreditt_form.rappmndfra.Text & " - " & stasjonskreditt_form.rappmndtil.Text & " �: " & stasjonskreditt_form.rappaar.Text & " for " & stasjonskreditt_form.rapport.Text

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_25.log
18:23:58:03.9687500  :  CutterSupport          = 0
29:23:58:03.9687500  :  TCPIPSERVER-----------------------------------------
30:23:58:03.9687500  :  SocketListener         = 0
31:23:58:03.9687500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_17.log
18:23:58:03.4276345  :  CutterSupport          = 0
29:23:58:03.4276345  :  TCPIPSERVER-----------------------------------------
30:23:58:03.4276345  :  SocketListener         = 0
31:23:58:03.4276345  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_16.log
18:08:55:45.3213750  :  CutterSupport          = 0
29:08:55:45.3213750  :  TCPIPSERVER-----------------------------------------
30:08:55:45.3213750  :  SocketListener         = 0
31:08:55:45.3213750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Omsetningprdag.Dsr
14:   ReportWidth     =   8382
37:      Name            =   "ReportHeader"
277:      Name            =   "ReportFooter"
430:Private Sub DataReport_Initialize()
431:Me.Sections("Reportheader").Controls("Label10").Caption = "Periode :" & omsetning_form.rappmndfra & " - " & omsetning_form.rappmndtil & " �: " & omsetning_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_03.log
18:17:31:51.2500000  :  CutterSupport          = 0
29:17:31:51.2500000  :  TCPIPSERVER-----------------------------------------
30:17:31:51.2500000  :  SocketListener         = 0
31:17:31:51.2500000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_23.log
18:09:26:13.2031250  :  CutterSupport          = 0
29:09:26:13.2031250  :  TCPIPSERVER-----------------------------------------
30:09:26:13.2031250  :  SocketListener         = 0
31:09:26:13.2031250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_04.log
18:07:09:53.6250000  :  CutterSupport          = 0
29:07:09:53.6250000  :  TCPIPSERVER-----------------------------------------
30:07:09:53.6250000  :  SocketListener         = 0
31:07:09:53.6250000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/DataReport1.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} DataReport1
3:   Caption         =   "DataReport1"
65:Attribute VB_Name = "DataReport1"

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_02.log
18:02:06:21.3437500  :  CutterSupport          = 0
29:02:06:21.3437500  :  TCPIPSERVER-----------------------------------------
30:02:06:21.3437500  :  SocketListener         = 0
31:02:06:21.3437500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Dispenserkontroll.vbw
7:Uttaksrapport_form = 154, 154, 585, 614, C, 132, 132, 563, 592, C
10:uttaksrapport = 66, 66, 841, 471, C, 88, 88, 838, 778, CZ

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_10.log
18:11:05:21.4218750  :  CutterSupport          = 0
29:11:05:21.4218750  :  TCPIPSERVER-----------------------------------------
30:11:05:21.4218750  :  SocketListener         = 0
31:11:05:21.4218750  :  SocketListenerPort     = 6001
67:11:08:14.7187500  :  CutterSupport          = 0
78:11:08:14.7187500  :  TCPIPSERVER-----------------------------------------
79:11:08:14.7187500  :  SocketListener         = 0
80:11:08:14.7187500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_25.log
18:09:02:43.7436250  :  CutterSupport          = 0
29:09:02:43.7436250  :  TCPIPSERVER-----------------------------------------
30:09:02:43.7436250  :  SocketListener         = 0
31:09:02:43.7436250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_18.log
18:12:25:58.4895000  :  CutterSupport          = 0
29:12:25:58.4895000  :  TCPIPSERVER-----------------------------------------
30:12:25:58.4895000  :  SocketListener         = 0
31:12:25:58.4895000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_17.log
18:13:54:20.4463750  :  CutterSupport          = 0
29:13:54:20.4463750  :  TCPIPSERVER-----------------------------------------
30:13:54:20.4463750  :  SocketListener         = 0
31:13:54:20.4463750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_03.log
18:12:11:17.6093750  :  CutterSupport          = 0
29:12:11:17.6093750  :  TCPIPSERVER-----------------------------------------
30:12:11:17.6093750  :  SocketListener         = 0
31:12:11:17.6093750  :  SocketListenerPort     = 6001
98:12:11:32.6718750  :     ResultData = D  03410651******8027;20160503121125;0;028;104402000704;;;;;;;14139877;533955;Visa SpareBank 1;00;IB1;A0000000031010;0000008000;F800;009B;140101;0000000000;;;;Inactive;{"od":{"ver":"1.01","preauth":{"ver":"1.0","auth":{"ver":"1.0","token":{"ver":"1.0","t":"RIofzToOT7apuaaV4UpI6LtIPWI=","e":"160602"}},"data":{"ver":"1.0","id":3,"tpan":"410651******8027","ref":"104402 000704","TCC":"IB1","resp":"00"},"receipt":{"ver":"1.0","cdt":{"ver":"1.0","prnstr":["Visa SpareBank 1 ","************8027-2","AID: A0000000031010","TVR: 0000008000","TSI: F800"]}}}}}

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_19.log
18:10:41:10.0832500  :  CutterSupport          = 0
29:10:41:10.0832500  :  TCPIPSERVER-----------------------------------------
30:10:41:10.0832500  :  SocketListener         = 0
31:10:41:10.0832500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_24.log
18:15:14:47.7057874  :  CutterSupport          = 0
29:15:14:47.7057874  :  TCPIPSERVER-----------------------------------------
30:15:14:47.7057874  :  SocketListener         = 0
31:15:14:47.7057874  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_22.log
18:11:24:19.6718750  :  CutterSupport          = 0
29:11:24:19.6718750  :  TCPIPSERVER-----------------------------------------
30:11:24:19.6718750  :  SocketListener         = 0
31:11:24:19.6718750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_20.log
18:12:01:57.6718750  :  CutterSupport          = 0
29:12:01:57.6718750  :  TCPIPSERVER-----------------------------------------
30:12:01:57.6718750  :  SocketListener         = 0
31:12:01:57.6718750  :  SocketListenerPort     = 6001
401:15:05:44.7187500  :  CutterSupport          = 0
412:15:05:44.7187500  :  TCPIPSERVER-----------------------------------------
413:15:05:44.7187500  :  SocketListener         = 0
414:15:05:44.7187500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_30.log
18:13:05:45.8750000  :  CutterSupport          = 0
29:13:05:45.8750000  :  TCPIPSERVER-----------------------------------------
30:13:05:45.8750000  :  SocketListener         = 0
31:13:05:45.8750000  :  SocketListenerPort     = 6001
599:16:40:30.8437500  :  CutterSupport          = 0
610:16:40:30.8437500  :  TCPIPSERVER-----------------------------------------
611:16:40:30.8437500  :  SocketListener         = 0
612:16:40:30.8437500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_02.log
18:23:58:12.0625000  :  CutterSupport          = 0
29:23:58:12.0625000  :  TCPIPSERVER-----------------------------------------
30:23:58:12.0625000  :  SocketListener         = 0
31:23:58:12.0625000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_08.log
18:01:05:43.6250000  :  CutterSupport          = 0
29:01:05:43.6250000  :  TCPIPSERVER-----------------------------------------
30:01:05:43.6250000  :  SocketListener         = 0
31:01:05:43.6250000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_16.log
18:04:16:57.0781250  :  CutterSupport          = 0
29:04:16:57.0781250  :  TCPIPSERVER-----------------------------------------
30:04:16:57.0781250  :  SocketListener         = 0
31:04:16:57.0781250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_27.log
18:12:31:36.3906250  :  CutterSupport          = 0
29:12:31:36.3906250  :  TCPIPSERVER-----------------------------------------
30:12:31:36.3906250  :  SocketListener         = 0
31:12:31:36.3906250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_15.log
18:23:58:20.5312500  :  CutterSupport          = 0
29:23:58:20.5312500  :  TCPIPSERVER-----------------------------------------
30:23:58:20.5312500  :  SocketListener         = 0
31:23:58:20.5312500  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_28.log
18:14:35:05.3593750  :  CutterSupport          = 0
29:14:35:05.3593750  :  TCPIPSERVER-----------------------------------------
30:14:35:05.3593750  :  SocketListener         = 0
31:14:35:05.3593750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_01.log
18:23:58:04.3593750  :  CutterSupport          = 0
29:23:58:04.3593750  :  TCPIPSERVER-----------------------------------------
30:23:58:04.3593750  :  SocketListener         = 0
31:23:58:04.3593750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_29.log
18:23:58:40.0468750  :  CutterSupport          = 0
29:23:58:40.0468750  :  TCPIPSERVER-----------------------------------------
30:23:58:40.0468750  :  SocketListener         = 0
31:23:58:40.0468750  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_14.log
18:10:03:12.6875000  :  CutterSupport          = 0
29:10:03:12.6875000  :  TCPIPSERVER-----------------------------------------
30:10:03:12.6875000  :  SocketListener         = 0
31:10:03:12.6875000  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/uttaksrapport.Dsr
3:   Bindings        =   "uttaksrapport.dsx":0000
4:   Caption         =   "Uttaksrapport"
14:   ReportWidth     =   8397
410:Private Sub DataReport_Initialize()
411:Me.Sections("Section4").Controls("Label10").Caption = "Periode :" & Uttaksrapport_form.rappmndfra & " - " & Uttaksrapport_form.rappmndtil & " �: " & Uttaksrapport_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_26.log
18:18:55:02.7905000  :  CutterSupport          = 0
29:18:55:02.8061250  :  TCPIPSERVER-----------------------------------------
30:18:55:02.8061250  :  SocketListener         = 0
31:18:55:02.8061250  :  SocketListenerPort     = 6001

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/omsetning_form.frm
4:   Caption         =   "Omsetningsrapport"
45:   Begin VB.CommandButton hentrapport
46:      Caption         =   "Hent Rapport"
96:Private Sub hentrapport_Click()
100:.CommandText = "SELECT DAY(dato) AS Expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Belp%', reporttext) + 9, 15)), ',', '.') AS float)) AS sum1 Into presalg From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Belp%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato)"
107:.CommandText = "SELECT DAY(dato) AS expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Retur%', reporttext) + 9, 15)), ',', '.') AS float)) AS retur Into retur From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Retur%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato) "

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/rapporter_form.frm
6:   Caption         =   "Rapporter"
43:      TextRTF         =   $"rapporter_form.frx":0000
46:      Bindings        =   "rapporter_form.frx":0082
106:         DataField       =   "reporttext"
107:         Caption         =   "reporttext"
119:         DataField       =   "reportid"
120:         Caption         =   "Reportid"
153:      Caption         =   "Velg rapportdato:"
168:RichTextBox1.Text = DataGrid1.Columns("reporttext").Text

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Tankinger_form.frm
5:   Caption         =   "Oppslag rapporter"
233:      Caption         =   "Velg rapportdato:"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/omsetning_form.log
6:Line 52: Property List in rapporttype could not be set.
7:Line 52: Property ItemData in rapporttype could not be set.

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Stasjonskredittrapport.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} uttaksrapport
3:   Bindings        =   "Stasjonskredittrapport.dsx":0000
4:   Caption         =   "Uttaksrapport pr kunde"
14:   ReportWidth     =   8715
36:      Name            =   "ReportHeader"
56:         Object.Caption         =   "Uttaksrapport "
377:      Name            =   "ReportFooter"
480:Attribute VB_Name = "uttaksrapport"
486:Private Sub DataReport_Initialize()
487:Me.Sections("Reportheader").Controls("Label8").Caption = "Periode :" & stasjonskreditt_form.rappmndfra.Text & " - " & stasjonskreditt_form.rappmndtil.Text & " �: " & stasjonskreditt_form.rappaar.Text & " for " & stasjonskreditt_form.rapport.Text

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Dispenserkontroll.vbw
7:Uttaksrapport_form = 154, 154, 585, 614, C, 132, 132, 563, 592, C
10:uttaksrapport = 66, 66, 841, 471, , 88, 88, 838, 778, C

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Dispenserkontroll.vbp
10:Form=rapporter_form.frm
15:Reference=*\G{642AC760-AAB4-11D0-8494-00A0C90DC8A9}#1.0#0#..\..\WINDOWS\system32\MSDBRPTR.DLL#Microsoft Data Report Designer 6.0 (SP4)
22:Form=Uttaksrapport.frm
23:Designer=uttaksrapport.Dsr
25:Designer=Stasjonskredittrapport.Dsr
26:Form=stasjonskreditt_rapport_form.frm
40:ServerSupportFiles=0

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/lpgnorge.Dsr
16:      ConnectionName  =   "Rapporter"
31:      ActiveConnectionName=   "Rapporter"
41:         Name            =   "reportid"
42:         Caption         =   "reportid"
49:         Name            =   "reporttext"
50:         Caption         =   "reporttext"
122:      ActiveConnectionName=   "Rapporter"
136:      ActiveConnectionName=   "Rapporter"
149:      ActiveConnectionName=   "Rapporter"
163:      ActiveConnectionName=   "Rapporter"
176:      ActiveConnectionName=   "Rapporter"
206:      ActiveConnectionName=   "Rapporter"
340:      ActiveConnectionName=   "Rapporter"
410:      ActiveConnectionName=   "Rapporter"
442:      ActiveConnectionName=   "Rapporter"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Uttaksrapport.frm
2:Begin VB.Form Uttaksrapport_form
3:   Caption         =   "Uttaksrapport"
12:   Begin VB.CommandButton hentrapport
13:      Caption         =   "Hent Rapport"
22:      ItemData        =   "Uttaksrapport.frx":0000
24:      List            =   "Uttaksrapport.frx":0028
32:      ItemData        =   "Uttaksrapport.frx":005C
34:      List            =   "Uttaksrapport.frx":0084
42:      ItemData        =   "Uttaksrapport.frx":00B8
44:      List            =   "Uttaksrapport.frx":00DD
75:Attribute VB_Name = "Uttaksrapport_form"
93:Private Sub hentrapport_Click()
98:.CommandText = "select dag=day(datostart), isnull(sum(liter),0) as liter from tankinger where month(datostart)>=" & Uttaksrapport_form.rappmndfra.Text & " and month(datostart)<=" & Uttaksrapport_form.rappmndtil.Text & " and year(datostart)=" & Uttaksrapport_form.rappaar & " group by day(datostart) order by day(datostart)"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/stasjonskreditt_rapport_form.frm
3:   Caption         =   "Rapporter"
11:   Begin VB.ComboBox rapport
19:   Begin VB.CommandButton hentrapport
20:      Caption         =   "Hent Rapport"
29:      ItemData        =   "stasjonskreditt_rapport_form.frx":0000
31:      List            =   "stasjonskreditt_rapport_form.frx":0025
38:      ItemData        =   "stasjonskreditt_rapport_form.frx":006B
40:      List            =   "stasjonskreditt_rapport_form.frx":0093
47:      ItemData        =   "stasjonskreditt_rapport_form.frx":00C7
49:      List            =   "stasjonskreditt_rapport_form.frx":00EF
99:rapport.Clear
107:rapport.AddItem lpgnorge.rskunder!Kunde
129:Private Sub hentrapport_Click()
132:lpgnorge.Commands.Item("Stasjontank").CommandText = "Select datostart,liter,pris,sum,sumekslrab=sum from stasjonskreditt_tankinger where kundeid=" & i(rapport.ListIndex + 1) & " and month(datostart) >=" & rappmndfra.Text & " and month(datostart) <=" & rappmndtil.Text & " and year(datostart)=" & rappaar.Text & " order by datostart"
139:Load uttaksrapport
141:uttaksrapport.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Omsetningprdag.Dsr
14:   ReportWidth     =   8382
37:      Name            =   "ReportHeader"
277:      Name            =   "ReportFooter"
430:Private Sub DataReport_Initialize()
431:Me.Sections("Reportheader").Controls("Label10").Caption = "Periode :" & omsetning_form.rappmndfra & " - " & omsetning_form.rappmndtil & " �: " & omsetning_form.rappaar

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/DataReport1.Dsr
2:Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} DataReport1
3:   Caption         =   "DataReport1"
65:Attribute VB_Name = "DataReport1"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/Dispenserkontroll.frm
163:   Begin VB.TextBox txtsend
181:   Begin MSWinsockLib.Winsock tcpclient
473:   Begin VB.Menu Rapporter
488:   Begin VB.Menu Rapporter
489:      Caption         =   "Rapporter"
491:      Begin VB.Menu Omsetningsrapport
492:         Caption         =   "Omsetningsrapport"
495:      Begin VB.Menu uttaksrapport
496:         Caption         =   "Uttaksrapport"
499:      Begin VB.Menu stasjonskreditt_rapport
512:Dim sendtext As String
570:    txtsend.Text = "<TANK_DISP_UNBLOCK>;" & prg_amount_kr
573:txtsend.Text = "<TANK_DISP_UNBLOCK>;" & prg_amount_kr
589:Private Sub Omsetningsrapport_Click(Index As Integer)
598:Private Sub stasjonskreditt_rapport_Click()
678:Private Sub tcpsend_Click()
680:tcpclient.SendData txtsend.Text
730:' 0    sckClosed    connection closed
732:'  2    sckListening    listening for incoming connections
733:'  3    sckConnectionPending    connection pending
736:'  6    sckConnecting    connecting to remote host
737:'  7    sckConnected    connected to remote host
820:Private Sub uttaksrapport_Click(Index As Integer)
821:Uttaksrapport_form.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/stasjonskreditt_rapport_form.log
6:Line 94: Property List in rapporttype could not be set.
7:Line 94: Property ItemData in rapporttype could not be set.

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Fra moss/Dispenserklient/innstillinger.frm
127:      Caption         =   "IPadresse kamera :"
135:      Caption         =   "IPadresse betalingsterminalterminal :"

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/Dispenserkontroll.frm
157:   Begin MSWinsockLib.Winsock tcpclient
395:   Begin VB.Menu Rapporter
413:   Begin VB.Menu Rapporter
414:      Caption         =   "Rapporter"
416:      Begin VB.Menu Omsetningsrapport
417:         Caption         =   "Omsetningsrapport"
420:      Begin VB.Menu uttaksrapport
421:         Caption         =   "Uttaksrapport"
424:      Begin VB.Menu stasjonskreditt_rapport
444:Dim sendtext As String
516:Private Sub Omsetningsrapport_Click(Index As Integer)
735:Private Sub uttaksrapport_Click(Index As Integer)
736:Uttaksrapport_form.Show (1)

Pumpestyring 2/Pumpestyring/Pumpestyring/Dispenserklient/stasjonskreditt_rapport_form.log
6:Line 94: Property List in rapporttype could not be set.
7:Line 94: Property ItemData in rapporttype could not be set.

# 3) Finn alle COM-referanser direkte
##  rg -n "COM[0-9]+"

rg -n "COM[0-9]+"
Dispenserkontroll_Ready/ProtocolSpec_NO.md
48:- Portnavn lastes fra `settings.ini` (`client_rfidcomport=COM5`). Hastighet 9600 bps.

Dispenserkontroll_Ready/settings.ini.example
6:# Serieport (RFID-leser). Bruk din faktiske COM-port (f.eks. COM3)
7:client_rfidcomport=COM5

Pumpestyring 2/Pumpestyring/Pumpestyring/RFID.ptp
6:COM5
7:COM2

Pumpestyring 2/Pumpestyring/Pumpestyring/2010.ptp
6:COM2
7:COM10

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_09.log
7:12:53:15.3593750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_21.log
7:10:40:22.7812500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_28.log
7:14:35:04.6718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_14.log
7:10:03:12.2031250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_13.log
7:05:23:10.5245000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_07.log
7:06:09:37.2812500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_06.log
7:09:22:05.9843750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_12.log
7:14:38:37.3682500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_01.log
7:23:58:04.2968750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/dotnet/baxilog/baxiHost_2025_11_21.log
7:11:30:52.9687500  :  DeviceString           = COM1
39:11:33:20.5000000  :  DeviceString           = COM1
74:11:37:00.1562500  :  DeviceString           = COM1
109:11:43:08.4531250  :  DeviceString           = COM1
144:12:01:02.1093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_15.log
7:23:58:20.4687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_29.log
7:23:58:40  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_20.log
7:12:01:58.7187500  :  DeviceString           = COM1
78:15:05:46.5000000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_08.log
7:01:05:44.4687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_22.log
7:11:24:20.3125000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_03.log
7:17:31:50.7968750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_17.log
7:23:58:03.3651345  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_04.log
7:11:05:44  :  DeviceString           = COM1
365:14:37:18  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_10.log
7:10:03:23.8750000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_11.log
7:23:58:52.8065020  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_05.log
7:07:43:30.4375000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_16.log
7:04:16:56.5312500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_02.log
7:23:58:12  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_23.log
7:09:26:13.7031250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_06.log
7:13:44:34.2500000  :  DeviceString           = COM1
222:17:25:32.0781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_12.log
7:23:58:18.7500000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_27.log
7:15:11:36.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_01.log
7:10:36:16.7656250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_15.log
7:21:51:09.1338750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_14.log
7:11:27:14.1338750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_13.log
7:18:11:10.0468750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_07.log
7:23:58:44.1093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_26.log
7:04:49:32.1718750  :  DeviceString           = COM1
41:04:51:26.2656250  :  DeviceString           = COM1
76:04:51:46.6562500  :  DeviceString           = COM1
111:05:10:03.9843750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_24.log
7:14:38:21.5468750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_18.log
7:08:30:45.7432500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/ehl.ptp
6:COM3
7:COM10

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_11.log
7:23:58:17.9375000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_05.log
7:08:08:22.6250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_30.log
7:23:58:45.4375000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_16.log
7:08:55:45.3213750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_02.log
7:02:06:21.3437500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_03.log
7:12:11:17.6093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_17.log
7:13:54:20.4463750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_31.log
7:23:58:55.6250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_04.log
7:07:09:53.6250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_10.log
7:11:05:21.4218750  :  DeviceString           = COM1
56:11:08:14.7187500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_19.log
7:20:12:32.4648750  :  DeviceString           = COM1
52:20:17:52.7500000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_25.log
7:23:58:03.9687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_23.log
7:09:26:13.2031250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_18.log
7:12:25:58.4895000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_24.log
7:15:14:47.7057874  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_30.log
7:13:05:45.8750000  :  DeviceString           = COM1
588:16:40:30.8437500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_02.log
7:23:58:12.0625000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_16.log
7:04:16:57.0781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_12.log
7:14:38:37.3838750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_24.log
7:15:14:47.7214115  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_04.log
7:11:05:44.4062500  :  DeviceString           = COM1
73:14:37:18.0781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_06.log
7:09:22:06  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_27.log
7:12:31:36.1093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_18.log
7:12:25:58.5051250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_10.log
7:10:03:24.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_14.log
7:11:27:14.8526250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_17.log
7:23:58:03.4276345  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_08.log
7:01:05:43.6093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_03.log
7:17:31:51.2500000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_23.log
7:09:26:13.1875000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_22.log
7:23:58:41.4113750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_25.log
7:23:58:03.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_25.log
7:09:02:43.7436250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_22.log
7:11:24:19.6562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_25.log
7:09:02:43.7592500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_19.log
7:10:41:10.0988750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_19.log
7:20:12:31.8867500  :  DeviceString           = COM1
169:20:17:52.6718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_19.log
7:10:41:10.0832500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_13.log
7:05:23:10.6182500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_09.log
7:12:34:03.3281250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_20.log
7:12:01:57.6562500  :  DeviceString           = COM1
497:15:05:46.4375000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_07.log
7:06:09:37.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_11.log
7:23:58:52.8377520  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_26.log
7:18:55:02.8217500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_09.log
7:12:53:15.1718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_18.log
7:08:30:45.0713750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_16.log
7:08:55:45.7432500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_28.log
7:00:24:11.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_12.log
7:14:38:38.1807500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_06.log
7:09:22:06.5156250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_02.log
7:02:06:22.1250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_05.log
7:07:43:30.6562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_30.log
7:13:05:45.9062500  :  DeviceString           = COM1
444:16:40:30.6718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_21.log
7:10:40:22.4062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_23.log
7:13:37:42.4426250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_22.log
7:11:24:19.6718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_03.log
7:12:11:18.3750000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_01.log
7:10:36:17.1093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_17.log
7:13:54:20.5088750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_05_15.log
7:21:51:09.7901250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_21.log
7:07:15:51.3020000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_24.log
7:14:38:20.5625000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_08.log
7:20:03:38.6093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_07.log
7:23:58:44.1406250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_18.log
7:12:25:59.2707500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_25.log
7:09:02:44.0561250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_20.log
7:23:58:26.7238750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_03_29.log
7:17:30:26.5937500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_04_20.log
7:23:58:26.7082500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_02.log
7:23:58:12.0312500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_05_26.log
7:04:49:32.1250000  :  DeviceString           = COM1
45:04:51:26.2031250  :  DeviceString           = COM1
115:04:51:46.5781250  :  DeviceString           = COM1
185:05:10:03.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_28.log
7:14:35:04.6562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_29.log
7:17:30:27.3437500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_02.log
7:02:06:21.3593750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_24.log
7:15:14:47.9245248  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_01.log
7:10:36:16.7812500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_20.log
7:12:01:57.6718750  :  DeviceString           = COM1
390:15:05:44.7187500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_30.log
7:13:05:46.5000000  :  DeviceString           = COM1
89:16:40:30.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_13.log
7:18:11:10.0312500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_20.log
7:23:58:26.7707500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_26.log
7:18:55:03.6030000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_16.log
7:04:16:56.5156250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_17.log
7:23:58:03.3963845  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_16.log
7:08:55:45.3370000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_08.log
7:20:03:39.1562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_19.log
7:10:41:10.4426250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_14.log
7:11:27:14.1651250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_30.log
7:23:58:45.4687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_27.log
7:15:11:36.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_08.log
7:01:05:43.6250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_09.log
7:12:34:03.8750000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_05.log
7:07:43:30.4687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_12.log
7:23:58:18.7812500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_03.log
7:17:31:50.7812500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_28.log
7:00:24:12.0937500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_04.log
7:11:05:44.0156250  :  DeviceString           = COM1
266:14:37:17.8281250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_11.log
7:23:58:52.7752520  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_27.log
7:12:31:36.3906250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_10.log
7:10:03:23.8906250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_27.log
7:12:31:36.4531250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_05.log
7:08:08:22.6093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_17.log
7:13:54:20.4776250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_21.log
7:07:15:52.2863750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_31.log
7:23:58:55.6562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_15.log
7:23:58:20.5312500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_03.log
7:12:11:17.6250000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_10.log
7:11:05:21.3906250  :  DeviceString           = COM1
58:11:08:14.8437500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_15.log
7:21:51:09.1495000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_06.log
7:13:44:34.2343750  :  DeviceString           = COM1
294:17:25:32.0937500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_07.log
7:06:09:37.2968750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_01.log
7:23:58:04.3593750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_22.log
7:23:58:41.4738750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_23.log
7:13:37:43.0676250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_29.log
7:23:58:40.0156250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_11.log
7:23:58:17.9687500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_13.log
7:18:11:11.0625000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_29.log
7:23:58:40.0468750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_12.log
7:23:58:18.8281250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_13.log
7:05:23:10.5557500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_04.log
7:07:09:53.6093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_15.log
7:23:58:20.5000000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_19.log
7:20:12:31.9180000  :  DeviceString           = COM1
132:20:17:52.5000000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_05.log
7:08:08:23.3125000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_28.log
7:14:35:05.3593750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_18.log
7:08:30:45.1026250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_06.log
7:13:44:34.4375000  :  DeviceString           = COM1
65:17:25:32.1562500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_01.log
7:23:58:04.3281250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_25.log
7:23:58:03.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_27.log
7:15:11:37.4062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_11.log
7:23:58:18.0156250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_14.log
7:10:03:12.6875000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_26.log
7:04:49:32.1093750  :  DeviceString           = COM1
41:04:51:26.1718750  :  DeviceString           = COM1
86:04:51:46.5625000  :  DeviceString           = COM1
131:05:10:03.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_22.log
7:23:58:41.4426250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_21.log
7:07:15:51.2863750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_14.log
7:10:03:12.1875000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_30.log
7:23:58:45.5000000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_29.log
7:17:30:26.5781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_03_31.log
7:23:58:55.6875000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_08.log
7:20:03:38.5781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_07.log
7:23:58:44.1718750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_26.log
7:18:55:02.7905000  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_23.log
7:13:37:42.4113750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_04.log
7:07:09:54.1093750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_04_09.log
7:12:34:03.2968750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiAppl_2016_03_28.log
7:00:24:11.9062500  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_24.log
7:14:38:20.5781250  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiHost_2016_04_10.log
7:11:05:21.8750000  :  DeviceString           = COM1
41:11:08:14.9218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_21.log
7:10:40:22.4218750  :  DeviceString           = COM1

Pumpestyring 2/Pumpestyring/Pumpestyring/baxilog/baxiBax_2016_05_09.log
7:12:53:15.1875000  :  DeviceString           = COM1

# 4) Finn protokoll/telegram (STX/ETX etc)
## rg -n "0x10|&H10|STX|ETX|XOR|checksum|CRC" 

rg -n "0x10|&H10|STX|ETX|XOR|checksum|CRC"
Pumpestyring 2/Pumpestyring/Pumpestyring/frmAbout.frm
115:Const KEY_NOTIFY = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/mjwPDF.cls
45:    Private Const WM_CLOSE = &H10
211:Private CRCounter           As Long
2126:    CRCounter = 0
2674:    CRCounter = CRCounter + 2
2688:    CRCounter = 0

Pumpestyring 2/Pumpestyring/Pumpestyring/fra_dispenser.bas
243:                                y(2) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/pumpekontroll.frm
644:y(1) = &H10
662:    y(1) = &H10
673:    'y(1) = &H10
690:y(1) = &H10
703:y(1) = &H10
881:y(1) = &H10
888:y(1) = &H10
896:    y(1) = &H10
906:    y(1) = &H10
974:            chksum = chksum Xor x(i)        'Vi kalkulerer CRC p�mottatt string
1011:                    y(1) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x/fra_dispenser.bas
243:                                y(2) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/pumpekontroll.frm
1145:berr = "no STX"
1147:berr = "no ETX"
1591:        y(1) = &H10
1659:y(1) = &H10
1684:y(1) = &H10
2347:y(1) = &H10
2362:y(1) = &H10
2377:y(1) = &H10
2392:y(1) = &H10
2431:    y(1) = &H10
2445:    y(1) = &H10
2459:        y(1) = &H10
2472:    y(1) = &H10
2534:230            chksum = chksum Xor x(i)        'Vi kalkulerer CRC p�mottatt string
2959:                                    y(1) = &H10
3457:    y(1) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/defs.bas
470:y(1) = &H10
483:y(1) = &H10
496:y(1) = &H10
504:y(1) = &H10
517:y(1) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/pumpekontroll.frm
644:y(1) = &H10
662:    y(1) = &H10
673:    'y(1) = &H10
690:y(1) = &H10
703:y(1) = &H10
881:y(1) = &H10
888:y(1) = &H10
896:    y(1) = &H10
906:    y(1) = &H10
974:            chksum = chksum Xor x(i)        'Vi kalkulerer CRC p�mottatt string
1011:                    y(1) = &H10

Pumpestyring 2/Pumpestyring/Pumpestyring/EHL4x 2/fra_dispenser.bas
243:                                y(2) = &H10

Common%20Files/Common Files/System/ado/adovbs.inc
214:Const adModeShareDenyNone = &H10
438:Const adFieldPendingInsert = &H10000
442:Const adFieldPendingUnknownDelete = &H100000
449:Const adSeekBeforeEQ = &H10

Common%20Files/Common Files/System/ado/adojavas.inc
214:var adModeShareDenyNone = 0x10;
438:var adFieldPendingInsert = 0x10000;
442:var adFieldPendingUnknownDelete = 0x100000;
449:var adSeekBeforeEQ = 0x10;

