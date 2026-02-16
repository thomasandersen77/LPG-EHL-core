Attribute VB_Name = "Functions_defs"
Option Explicit

Type trans
PaymentType As Integer
Presum As Single
TankSum As Single
TankVol As Single
TankPrice As Single
cashbacksum As Single
Status As Integer '0 =Not started 1=Ready 2=Active 3=Finished 4=Unaccounted 5=Financial Return 6=Financial Tech.return 7=Annulated 8=Accounted 9=Finished

End Type
Public Transaction As trans

'const
Public Const tank_timeout = 120 'timeout i sekunder før dispenser går tilbake i unblock modus, og evt tilbakefører bank_beløp
Public Const MVA = 25

''APIS

Public Declare Sub Sleep Lib "kernel32" (ByVal dwMilliseconds As Long)
Public Declare Function sndPlaySound Lib "winmm.dll" Alias "sndPlaySoundA" (ByVal lpszSoundName As String, ByVal uFlags As Long) As Long

''Strings

Public DBserver As String, DBserver_POS As String, DBdb As String, DBdb_POS As String, DBbrukernavn As String, DBbrukernavn_POS As String, DBpassord As String, DBpassord_POS As String, SQLconnstr As String, sqlconnstr_epos1 As String
Public beloptext As String, system_state As String, state_string_Tank As String, state_string_tank_old As String, Command_disp As String, state_string As String, state_string_old As String, charstr As String, charstrprn As String, commandtext As String, commandtext_in As String, DISP_errorstatestring As String, textline As String
Public CONT_state_string_Tank As String, CONT_state_string_tank_old As String, CONT_state_string As String, CONT_state_string_old As String, CONT_errorstatestring As String
Public tcpword() As String, tcpword_kreditt() As String, tcpword_status() As String
Public berr As String, rapporttype  As String, txtbankf1  As String, txtbankf2  As String, txtbankf3 As String, txtbankf4 As String, feed_offset As String, reporttext As String
Public statetext As String, cfgline() As String
Public firmanavn As String, firmaadresse As String, firmapostnr As String, firmapoststed As String, firmaorgnr As String, firmatelefon As String, firmaåpningstider As String
Public firmaepost As String, bank_tank As String
Public gettcpdatastr As String, gettcpdatastr_kreditt As String, gettcpdatastr_status As String
Public bank_charge As String, Technical_Email As String


''Integers
Public Com_port As Integer, Com_port_bank As Integer, com_port_print As Integer, com_port_pinpad As Integer, com_port_stcredit As Integer
Public retries_kreditt As Integer, dispcontainernr As Integer, i As Integer, u As Integer, w As Integer, v As Integer, disptest_interval As Integer, retries As Integer
Public tanktimeout_count As Integer, POSsystem As Integer
Public statcred_rabatt As Integer
Public cmd_retries As Integer
''Long
Public COM_id As Long, valresult As Long, com_port_bank_baud As Long
Public stationcredit_custno As Long
Public stationcredit_contactid As Long
Public tanknr As Long

''Byte
Public y(16) As Byte, x(16) As Byte, chksum As Byte, TCPsendMessage(15) As String
Public PaymentType As Byte '0=default 1=kontant 2=bankkort 3=stasjonskort

''Single
Public CONT_tank_vol As Single, CONT_tank_vol_last As Single, tank_vol As Single, tank_vol_last As Single, tank_unitprice As Single, tank_sum As Single, tank_sum2 As Single, bank_sum As Single, bank_sum2 As Single

Public statcred_start As Date

''Booleans
Public ok_to_opendisp As Boolean, Disp_was_unblocked As Boolean, disp_command As Boolean, Bank_answer As Boolean, bank_inprogress As Boolean
Public prn_statusenq As Boolean, prn_paperenq As Boolean, prn_paperlow As Boolean, nextbyteiserror As Boolean
Public printer_online As Boolean, bank_online As Boolean, manual_bank As Boolean, admmode As Boolean, baxierror As Boolean, bank_localmode As Boolean
Public NEW_command As Boolean, disp_init As Boolean, rts As Boolean, DISP_startbuttonpressed As Boolean, DISP_openfordelivery As Boolean, disp_automode As Boolean, station_day As Boolean, trans_finished_powerfault As Boolean, trans_unaccounted As Boolean, new_tank As Boolean, tank_end As Boolean
Public CONT_startbuttonpressed As Boolean, CONT_openfordelivery As Boolean, CONT_automode As Boolean, CONT_trans_finished_powerfault As Boolean, CONT_trans_unaccounted As Boolean, CONT_new_tank As Boolean, CONT_tank_end As Boolean
Public checkkreditt As Boolean
Public return_amount As Boolean
Public tcperror As Boolean
Public SetAmount As Boolean
Public DispUnblock As Boolean
Public Ready_to_presum As Boolean

Public tank_ok As Boolean, dispunblock_ok As Boolean, setamount_ok As Boolean, error_ok As Boolean
Public volume_ok As Boolean, state_ok As Boolean
Public tank_ok_cont As Boolean, dispunblock_ok_cont As Boolean, error_ok_cont As Boolean
Public volume_ok_cont As Boolean, state_ok_cont As Boolean
Public servicerunning As Boolean

''Variants
Public Com_print_input
Public dispprice(2), dispnr(2)
Public objInst, objSet

''Recordsets
Public sqlconn As New ADODB.Connection
Public salg_RS As New ADODB.Recordset

Public rapport_rs As New ADODB.Recordset
Public RS As New ADODB.Recordset

Sub Main()
On Error GoTo errhandler
If Dir(App.Path & "\server.ini", vbNormal) = "" Then
    serverinnstillinger.Show (1)
Else
    Open App.Path & "\server.ini" For Input As #1
    Input #1, textline
    cfgline() = Split(textline, ";")
    DBserver = cfgline(0)
    DBdb = cfgline(1)
    DBbrukernavn = cfgline(2)
    DBpassord = cfgline(3)
    Com_port = Val(cfgline(4))
    Com_port_bank = Val(cfgline(5))
    com_port_print = Val(cfgline(6))
    com_port_pinpad = Val(cfgline(7))
    com_port_stcredit = Val(cfgline(8))
    com_port_bank_baud = Val(cfgline(9))
    txtbankf1 = cfgline(10)
    txtbankf2 = cfgline(11)
    txtbankf3 = cfgline(12)
    txtbankf4 = cfgline(13)
    feed_offset = cfgline(14)
    POSsystem = cfgline(15)
    DBserver_POS = cfgline(16)
    DBdb_POS = cfgline(17)
    DBbrukernavn_POS = cfgline(18)
    DBpassord_POS = cfgline(19)
    Close #1
End If
lpgnorge.betterm.ConnectionString = "Provider=SQLOLEDB.1;Password=" & DBpassord & ";Persist Security Info=True;User ID=" & DBbrukernavn & ";Initial Catalog=" & DBdb & ";Data Source=" & DBserver 'lpgmoss_bet\sqlexpress"
lpgnorge.butikkdata.ConnectionString = "Provider=SQLOLEDB.1;Password=" & DBpassord_POS & ";Persist Security Info=True;User ID=" & DBbrukernavn_POS & ";Initial Catalog=" & DBdb_POS & ";Data Source=" & DBserver_POS ' LPGMOSS_BUTIKK\UNI
Pumpekontroll.Show

   Exit Sub
errhandler:
   Pumpekontroll.errorlist.AddItem Now & ", Main: " & Err.Number & " " & Err.Description
   Resume Next

End Sub
Public Sub logdisp_err(produkt As String, ERRmainlevel As Byte, ERRsublevel As Byte)
Dim errtext As String

Select Case ERRmainlevel

Case 1
    Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Ingen kommunikasjon Display<-->CPU"
    Case 2
     errtext = Now & " " & "For mange kommunikasjonsfeil Display<-->CPU"
    Case 3
    errtext = Now & " " & "Intern feil Display"
    End Select
    
Case 2
Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Pulser ikke tilkoblet"
    Case 2
    errtext = Now & " " & "Feil rotasjon på pulser"
    Case 3
    errtext = Now & " " & "En pulserkanal mangler"
    Case 4
    errtext = Now & " " & "Feil serie på pulser"
    Case 5
    errtext = Now & " " & "Pulser buffer overflow"
    Case 6
    errtext = Now & " " & "LPG flow for høy"
    End Select
Case 3
Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Output overload(Para 10)"
    Case 2
    errtext = Now & " " & "Output control failure"
    Case 3
    errtext = Now & " " & "Startknapp aktivert under oppstart"
    Case 4
    errtext = Now & " " & "No load detected"
    Case 5
    errtext = Now & " " & "Termisk pumpebeskyttelse aktivert"
    End Select
Case 4
Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Minnefeil system"
    Case 8
    errtext = Now & " " & "CPU"
    Case 3
    errtext = Now & " " & "Strømbrudd"
    Case 4
    errtext = Now & " " & "Intern kommunikasjon CPU<-->Mainstream"
    Case 5
    errtext = Now & " " & "Calculations owerflow"
    Case 2
    errtext = Now & " " & "Reset aktivert på hovedkort"
    Case 7
    errtext = Now & " " & "Brownout reset- for lite strøm til prosessor"
    Case 8
    errtext = Now & " " & "Ingen svar fra CPU"
    End Select
Case 5
Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Ingen Rs485 kommunikasjon"
    End Select
Case 6
Select Case ERRsublevel
    Case 1
    errtext = Now & " " & "Fylling har pågått for lenge (Para 22)"
    Case 2
    errtext = Now & " " & "For lang tid uten pulser ( Para 24)"
    Case 3
    errtext = Now & " " & "Flow for høy(Para 45)"
    Case 4
    errtext = Now & " " & "Maksimal grense for beløp nådd"
    Case 6
    errtext = Now & " " & "Pris er satt til 0.00"
    Case 7
    errtext = Now & " " & "Flow for liten (Para 48)"
    Case 8
    errtext = Now & " " & "Feil transaksjon state"
    Case 5
    errtext = Now & " " & ""
    End Select
Case 7
Select Case ERRsublevel
    Case 1
    Case 2
    Case 3
    End Select
Case 8
Select Case ERRsublevel
    Case 1
    Case 2
    Case 3
    End Select
Case 9
Select Case ERRsublevel
    Case 1
    Case 2
    Case 3
    End Select
End Select

Pumpekontroll.errorlist.AddItem produkt & " " & errtext

End Sub
Sub tcpsend(tcpmelding As String)
On Error GoTo errhandler

 If Pumpekontroll.tcpserver.State = 7 Then Pumpekontroll.tcpserver.SendData tcpmelding

Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "TCP_send:" & Err.Number & " " & Err.Description
Resume Next
End Sub

Function gettcpstate() As String
On Error GoTo errhandler
Select Case Pumpekontroll.tcpserver.State
Case 0
gettcpstate = "Ingen kontakt"


Case 1
gettcpstate = "Åpen"
Case 2
gettcpstate = "Lytter"

Case 3
gettcpstate = "Forbindelse foregår"

Case 4
gettcpstate = "Slår opp navn"

Case 5
gettcpstate = "Navn funnet"

Case 6
gettcpstate = "Kobler til..."

Case 7
gettcpstate = "Tilkoblet"

Case 8
gettcpstate = "Pir stenger forbindelse."
tcperror = True 'Pumpekontroll.tcpserver.Close

Case 9
gettcpstate = "Feil."
tcperror = True 'Pumpekontroll.tcpserver.Listen

End Select

Exit Function

errhandler:
Pumpekontroll.errorlist.AddItem "Gettcpstate Linje :" & Erl & " " & Err.Number & " " & Err.Description
Resume Next
End Function

Function RFID_comon() As Boolean
On Error GoTo errhandler
If com_port_stcredit > 0 Then
    Pumpekontroll.RFIDCOM.CommPort = com_port_stcredit
    Pumpekontroll.RFIDCOM.PortOpen = True
    'Pumpekontroll.RFIDCOM.Output = "c"
    
    RFID_comon = True
Else
RFID_comon = False
End If
Exit Function

errhandler:
RFID_comon = False
Pumpekontroll.errorlist.AddItem "Kan ikke åpne kommunikasjon til RFID. Stasjonskreditt stengt."
Pumpekontroll.Check3.Value = 0
Resume Next
End Function

Function pinpad_comon() As Boolean
On Error GoTo errhandler

If com_port_pinpad > 0 Then
    Pumpekontroll.com_pinpad.CommPort = com_port_pinpad                     'Disse setningene må flyttes.
    Pumpekontroll.com_pinpad.PortOpen = True  '
    pinpad_comon = True
    
Else
pinpad_comon = False
End If

Exit Function

errhandler:
pinpad_comon = False
Pumpekontroll.errorlist.AddItem "Kan ikke åpne kommunikasjon til Pinpad." & Err.Number & " " & Err.Description
Resume Next

End Function


Function prn_comon() As Boolean


On Error GoTo errhandler

If com_port_print > 0 Then
    Pumpekontroll.com_print.CommPort = com_port_print
    Pumpekontroll.com_print.PortOpen = True  '
    prn_comon = True
   
    
Else
prn_comon = True
End If
printer_online = False

Exit Function

errhandler:
printer_online = False
prn_comon = False
Pumpekontroll.errorlist.AddItem "Kan ikke åpne kommunikasjonsport til kvitteringskriver." & Err.Number & " " & Err.Description
Resume Next
End Function

Public Function bank_comon() As Boolean
If Com_port_bank > 0 Then
    With Pumpekontroll.Baxi
        '.AutoGetCustomerInfo = 1
        
        .CommPort = Com_port_bank
        .BaudRate = com_port_bank_baud
        '.CutterSupport = False
        '.AutoGetCustomerInfo = 1
        .PrinterWidth = 24
        .DisplayWidth = 24
        .HostIpAddress = "91.102.24.142"
        .HostPort = "9670"
        
        .Open
        If Pumpekontroll.Baxi.Active = 1 Then bank_comon = True Else bank_comon = False
        baxierror = False
        
    End With
Else
bank_comon = False

End If

End Function
Public Function db_ok() As Boolean

On Error GoTo errhandler
Dim retrycount

retrycount = 0
retry_open:
retrycount = retrycount + 1
SQLconnstr = "Provider=SQLOLEDB;User ID=" & DBbrukernavn & ";Password=" & DBpassord & ";Initial Catalog=" & DBdb & ";Data Source=" & DBserver

sqlconn.Open SQLconnstr

If sqlconn.State = 1 Then db_ok = True

Exit Function

errhandler:
If retrycount >= 10 Then
Pumpekontroll.errorlist.AddItem "Feil: " & Err.Number & " " & Err.Description
db_ok = False
Else
Err.Clear
GoTo retry_open
End If
End Function
Public Sub Check_DBconn()

On Error Resume Next

Dim TestRS As ADODB.Recordset
Dim TestConn As ADODB.Connection

Set TestConn = lpgnorge.Connections.Item("betterm")
If TestConn.State = 0 Then TestConn.Open

    TestRS.Open "Select top 1 * from dispensere", TestConn, adOpenDynamic, adLockOptimistic
    If Err <> 0 Then
        Pumpekontroll.errorlist.AddItem "Check_dbconn_dispensere: " & Err.Number & " " & Err.Description
    Else

End If
If TestRS.State = 1 Then TestRS.Close
If TestConn.State = 1 Then TestConn.Close

If POSsystem = 1 Then
    Set TestConn = lpgnorge.Connections.Item("Butikkdata")
    If TestConn.State = 0 Then TestConn.Open
    TestRS.Open "Select top 1 * from stasjonskred", TestConn, adOpenDynamic, adLockOptimistic
    If Err <> 0 Then
        Pumpekontroll.errorlist.AddItem "Check_dbconn_butikkdata: " & Err.Number & " " & Err.Description
    Else
    End If
    
    If TestRS.State = 1 Then TestRS.Close
    If TestConn.State = 1 Then TestConn.Close
    
End If
End Sub
Public Sub LogEvent(Ev As String, RawData As String, REFtableid As Long, REFtablename As String, Evtype As String)
On Error GoTo errhandler

If lpgnorge.rslogs.State = 1 Then
lpgnorge.rslogs.AddNew
lpgnorge.rslogs!dato = Now
lpgnorge.rslogs!event = Ev
lpgnorge.rslogs!ref_tableid = REFtableid
lpgnorge.rslogs!ref_tablename = REFtablename
lpgnorge.rslogs!RawData = RawData
lpgnorge.rslogs!Type = Evtype
lpgnorge.rslogs.Update

Else
Pumpekontroll.errorlist.AddItem Now & " Får ikke logget event til DB:" & Ev & ", " & RawData & ", " & Evtype
End If
Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem Now & ": Logevent, " & Err.Number & " " & Err.Description
Resume Next

End Sub
Public Sub Reset_disp(dispn As Integer)
y(1) = &H10
y(2) = &H6
y(3) = dispn
y(4) = &H81
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
End Sub


Public Sub check_for_pending_transactions(dispn As Integer)
'sjekk om dispenser nødstopp er aktivert etter forhåndsvalg

y(1) = &H10
y(2) = &H6
y(3) = dispn
y(4) = &HC5
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))


End Sub

Public Function disp_setprice(dispn, price As String) As Boolean

y(1) = &H10
y(2) = &H7
y(3) = dispn
y(4) = &HC3
y(5) = &H30
y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
y(7) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
y(1) = &H10
y(2) = &HA
y(3) = dispn
y(4) = &HA9
y(5) = Asc(Mid(price, 4, 1))
y(6) = Asc(Mid(price, 3, 1))
y(7) = Asc(Mid(price, 2, 1))
y(8) = Asc(Mid(price, 1, 1))
y(9) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8)
y(10) = &H36

comm_out 1000, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10))

y(1) = &H10
y(2) = &H6
y(3) = dispn
y(4) = &H5C
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
End Function

Sub comm_out(Waittime As Integer, commstr As String)

While Not rts

DoEvents
Wend
'If Pumpekontroll.ehldebug.Value = 1 Then
'End If

If Pumpekontroll.MSComm1.PortOpen Then Pumpekontroll.MSComm1.Output = commstr
commandtext = ""
Sleep (Waittime)
End Sub

Sub com_prn_out(prnstr As String)

If Pumpekontroll.com_print.PortOpen Then Pumpekontroll.com_print.Output = prnstr
End Sub

Public Function Print_reciept_header() As String

On Error GoTo errhandler
Dim header As String
15    tanknr = lpgnorge.rstankinger!tankid
header = Chr(27) + Chr(78) + Chr(1) & firmanavn & Chr(10) & Chr(27) + Chr(78) + Chr(1) & firmaadresse & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & firmapostnr & " " & firmapoststed & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & "Orgnr: " & firmaorgnr & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & "Telefon:" & firmatelefon & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & "Epost:" & firmaepost & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & "Åpningstider" & Chr(10) _
                & Chr(27) + Chr(78) + Chr(1) & firmaåpningstider & Chr(10) _
                & Chr(10) & Chr(10) & "Tankingsnummer:" & tanknr & Chr(10) & Chr(10)
                
Print_reciept_header = header
   Exit Function
errhandler:
   Pumpekontroll.errorlist.AddItem Now & ", print_reciept_header: " & Err.Number & " " & Err.Description
   Resume Next
End Function

Public Function decimaltobinn(desimal As Byte) As String

Dim hex_string As String
Dim digit_num As Integer
Dim digit_value As Integer
Dim nibble_string As String
Dim result_string As String
Dim factor As Integer
Dim bit As Integer

    hex_string = Hex$(desimal)

    hex_string = Right$(String$(8, "0") & hex_string, 8)
    For digit_num = 8 To 1 Step -1
        digit_value = CLng("&H" & Mid$(hex_string, _
            digit_num, 1))
       factor = 1
        nibble_string = ""
        For bit = 3 To 0 Step -1
            If digit_value And factor Then
                nibble_string = "1" & nibble_string
            Else
                nibble_string = "0" & nibble_string
            End If
            factor = factor * 2
        Next bit
     result_string = nibble_string & result_string
    Next digit_num

    decimaltobinn = Right(result_string, 8)
End Function


Public Function decimaltobinn_tank(desimal As Byte) As String

Dim hex_string As String
Dim digit_num As Integer
Dim digit_value As Integer
Dim nibble_string As String
Dim result_string As String
Dim factor As Integer
Dim bit As Integer

    hex_string = Hex$(desimal)

    hex_string = Right$(String$(8, "0") & hex_string, 8)
    For digit_num = 8 To 1 Step -1
        digit_value = CLng("&H" & Mid$(hex_string, _
            digit_num, 1))
       factor = 1
        nibble_string = ""
        For bit = 3 To 0 Step -1
            If digit_value And factor Then
                nibble_string = "1" & nibble_string
            Else
                nibble_string = "0" & nibble_string
            End If
            factor = factor * 2
        Next bit
     result_string = nibble_string & result_string
    Next digit_num

    decimaltobinn_tank = Right(result_string, 8)
End Function

