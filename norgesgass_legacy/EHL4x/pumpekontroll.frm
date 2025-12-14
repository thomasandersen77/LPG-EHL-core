VERSION 5.00
Object = "{648A5603-2C6E-101B-82B6-000000000014}#1.1#0"; "mscomm32.ocx"
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Object = "{67397AA1-7FB1-11D0-B148-00A0C922E820}#6.0#0"; "MSADODC.OCX"
Object = "{675FD4BC-A245-4921-8195-2D7401C2BB15}#1.0#0"; "baxi.dll"
Begin VB.Form Pumpekontroll 
   BorderStyle     =   1  'Fixed Single
   Caption         =   "LPG Pumpekontroll"
   ClientHeight    =   7950
   ClientLeft      =   1320
   ClientTop       =   0
   ClientWidth     =   14820
   LinkTopic       =   "Form2"
   ScaleHeight     =   7950
   ScaleWidth      =   14820
   WindowState     =   2  'Maximized
   Begin MSCommLib.MSComm com_print 
      Left            =   480
      Top             =   360
      _ExtentX        =   979
      _ExtentY        =   979
      _Version        =   393216
      CommPort        =   11
      DTREnable       =   0   'False
      InputLen        =   1
      ParityReplace   =   0
      RThreshold      =   1
      SThreshold      =   1
   End
   Begin VB.TextBox bankkvittering 
      Height          =   735
      Left            =   6360
      MultiLine       =   -1  'True
      ScrollBars      =   3  'Both
      TabIndex        =   21
      Top             =   1440
      Width           =   8175
   End
   Begin VB.CommandButton cmdbank_200 
      Caption         =   "200KR"
      Height          =   492
      Left            =   7680
      TabIndex        =   17
      Top             =   840
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_400 
      Caption         =   "400 KR"
      Height          =   492
      Left            =   8640
      TabIndex        =   16
      Top             =   840
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_600 
      Caption         =   "600KR"
      Height          =   492
      Left            =   9600
      TabIndex        =   15
      Top             =   840
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_100 
      Caption         =   "100KR"
      Height          =   492
      Left            =   6720
      TabIndex        =   14
      Top             =   840
      Width           =   852
   End
   Begin MSDataGridLib.DataGrid vistankinger 
      Bindings        =   "pumpekontroll.frx":0000
      Height          =   1815
      Left            =   120
      TabIndex        =   13
      Top             =   6120
      Width           =   14415
      _ExtentX        =   25426
      _ExtentY        =   3201
      _Version        =   393216
      AllowUpdate     =   0   'False
      AllowArrows     =   -1  'True
      HeadLines       =   1
      RowHeight       =   15
      TabAction       =   1
      FormatLocked    =   -1  'True
      BeginProperty HeadFont {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Caption         =   "Viser de siste tankinger"
      ColumnCount     =   7
      BeginProperty Column00 
         DataField       =   "Datostart"
         Caption         =   "Datostart"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column01 
         DataField       =   "Liter"
         Caption         =   "Liter"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column02 
         DataField       =   "Pris"
         Caption         =   "Pris"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column03 
         DataField       =   "sum"
         Caption         =   "sum"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column04 
         DataField       =   "betalingstype"
         Caption         =   "betalingstype"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column05 
         DataField       =   "Status"
         Caption         =   "Status"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column06 
         DataField       =   "Datostopp"
         Caption         =   "Datostopp"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      SplitCount      =   1
      BeginProperty Split0 
         MarqueeStyle    =   4
         BeginProperty Column00 
            ColumnWidth     =   1530,142
         EndProperty
         BeginProperty Column01 
            Alignment       =   1
            ColumnWidth     =   1184,882
         EndProperty
         BeginProperty Column02 
            Alignment       =   1
            ColumnWidth     =   1184,882
         EndProperty
         BeginProperty Column03 
            Alignment       =   1
            ColumnWidth     =   1184,882
         EndProperty
         BeginProperty Column04 
            ColumnWidth     =   1035,213
         EndProperty
         BeginProperty Column05 
            ColumnWidth     =   645,165
         EndProperty
         BeginProperty Column06 
            Alignment       =   1
            ColumnWidth     =   1769,953
         EndProperty
      EndProperty
   End
   Begin MSAdodcLib.Adodc tankinger 
      Height          =   312
      Left            =   4320
      Top             =   7200
      Width           =   1572
      _ExtentX        =   2778
      _ExtentY        =   582
      ConnectMode     =   0
      CursorLocation  =   3
      IsolationLevel  =   -1
      ConnectionTimeout=   15
      CommandTimeout  =   30
      CursorType      =   1
      LockType        =   3
      CommandType     =   2
      CursorOptions   =   0
      CacheSize       =   50
      MaxRecords      =   0
      BOFAction       =   0
      EOFAction       =   0
      ConnectStringType=   1
      Appearance      =   1
      BackColor       =   -2147483643
      ForeColor       =   -2147483640
      Orientation     =   0
      Enabled         =   -1
      Connect         =   "Provider=SQLOLEDB.1;Password=lpg01;Persist Security Info=True;User ID=sa;Initial Catalog=LPGNORGE;Data Source=lpgsrv01"
      OLEDBString     =   "Provider=SQLOLEDB.1;Password=lpg01;Persist Security Info=True;User ID=sa;Initial Catalog=LPGNORGE;Data Source=lpgsrv01"
      OLEDBFile       =   ""
      DataSourceName  =   ""
      OtherAttributes =   ""
      UserName        =   ""
      Password        =   ""
      RecordSource    =   "Tankinger"
      Caption         =   "Adodc1"
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      _Version        =   393216
   End
   Begin VB.TextBox prg_amount 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   " # ##0,00"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   0
      EndProperty
      BeginProperty Font 
         Name            =   "Arial"
         Size            =   9.75
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   360
      Left            =   1320
      TabIndex        =   10
      TabStop         =   0   'False
      Text            =   "0"
      Top             =   1326
      Width           =   612
   End
   Begin MSCommLib.MSComm MSComm1 
      Left            =   600
      Top             =   2760
      _ExtentX        =   979
      _ExtentY        =   979
      _Version        =   393216
      DTREnable       =   -1  'True
      InputLen        =   1
      NullDiscard     =   -1  'True
      RThreshold      =   1
   End
   Begin VB.Timer state_timer 
      Interval        =   1000
      Left            =   120
      Top             =   2040
   End
   Begin VB.ListBox cmdDISPTX 
      Height          =   1620
      Left            =   6360
      TabIndex        =   9
      TabStop         =   0   'False
      Top             =   3960
      Visible         =   0   'False
      Width           =   8175
   End
   Begin VB.ListBox CMDDISPRX 
      Height          =   1620
      Left            =   6360
      TabIndex        =   8
      TabStop         =   0   'False
      Top             =   2280
      Visible         =   0   'False
      Width           =   8175
   End
   Begin VB.CommandButton cmddisp_stop 
      BackColor       =   &H000000FF&
      Caption         =   "Stopp dispenser"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   13.5
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   732
      Left            =   4680
      MaskColor       =   &H000000FF&
      Style           =   1  'Graphical
      TabIndex        =   4
      Top             =   1080
      Width           =   1452
   End
   Begin VB.CommandButton cmdstart 
      BackColor       =   &H0000C000&
      Caption         =   "Frigi dispenser"
      DisabledPicture =   "pumpekontroll.frx":0018
      Enabled         =   0   'False
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   9.75
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   735
      Left            =   2400
      MaskColor       =   &H0000C000&
      Style           =   1  'Graphical
      TabIndex        =   3
      TabStop         =   0   'False
      ToolTipText     =   "Trykk for å frigi dispenser."
      Top             =   1080
      Width           =   2172
   End
   Begin VB.TextBox antall_liter 
      Alignment       =   1  'Right Justify
      BeginProperty Font 
         Name            =   "Arial"
         Size            =   48
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   1215
      Left            =   2400
      TabIndex        =   2
      TabStop         =   0   'False
      Text            =   "0000.00"
      Top             =   3240
      Width           =   3735
   End
   Begin VB.TextBox DisPris 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   " # ##0,00"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   0
      EndProperty
      BeginProperty Font 
         Name            =   "Arial"
         Size            =   48
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   1215
      Left            =   2400
      TabIndex        =   1
      TabStop         =   0   'False
      Text            =   "00.00"
      Top             =   4560
      Width           =   3735
   End
   Begin VB.TextBox belop 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   " # ##0,00"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   0
      EndProperty
      BeginProperty Font 
         Name            =   "Arial"
         Size            =   48
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   1215
      Left            =   2400
      TabIndex        =   0
      TabStop         =   0   'False
      Text            =   "0000.00"
      Top             =   1920
      Width           =   3735
   End
   Begin VB.Label Label7 
      Height          =   255
      Left            =   2280
      TabIndex        =   20
      Top             =   360
      Width           =   3975
   End
   Begin VB.Label Label5 
      Caption         =   "Label5"
      Height          =   255
      Left            =   10320
      TabIndex        =   19
      Top             =   360
      Width           =   1335
   End
   Begin VB.Label Label4 
      Height          =   255
      Left            =   6840
      TabIndex        =   18
      Top             =   360
      Width           =   3135
   End
   Begin BAXILibCtl.BaxiCtrl baxi 
      Left            =   720
      OleObjectBlob   =   "pumpekontroll.frx":03EE
      Top             =   6240
   End
   Begin VB.Label Label8 
      Caption         =   "KR"
      Height          =   252
      Left            =   1920
      TabIndex        =   12
      Top             =   1380
      Width           =   372
   End
   Begin VB.Label Label6 
      Caption         =   "Stopp ved:"
      Height          =   252
      Left            =   360
      TabIndex        =   11
      Top             =   1380
      Width           =   852
   End
   Begin VB.Label Label3 
      Caption         =   "Pris kr/l"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   13.5
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   372
      Left            =   120
      TabIndex        =   7
      Top             =   5040
      Width           =   1932
   End
   Begin VB.Label Label2 
      Caption         =   "Antall liter"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   13.5
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   372
      Left            =   120
      TabIndex        =   6
      Top             =   3720
      Width           =   1932
   End
   Begin VB.Label Label1 
      Caption         =   "Beløp å betale"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   13.5
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   372
      Left            =   120
      TabIndex        =   5
      Top             =   2280
      Width           =   1932
   End
   Begin VB.Menu Instillinger 
      Caption         =   "&Instillinger"
      Begin VB.Menu Brukere 
         Caption         =   "Brukere"
      End
      Begin VB.Menu dispensere 
         Caption         =   "Dispensere"
      End
      Begin VB.Menu Programinnstillinger 
         Caption         =   "Program"
      End
   End
   Begin VB.Menu Stasjonmodus 
      Caption         =   "&Stasjonmodus"
      Begin VB.Menu Nattstilling 
         Caption         =   "Nattstilling"
      End
      Begin VB.Menu Dagstilling 
         Caption         =   "Dagstilling"
      End
      Begin VB.Menu Stengt 
         Caption         =   "Stengt"
      End
   End
End
Attribute VB_Name = "Pumpekontroll"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Sub baxi_OnDisplayText()
Label7.Caption = baxi.DisplayText
End Sub

Private Sub baxi_OnError()
Label5.Caption = baxi.LastError

End Sub

Private Sub baxi_OnLocalMode(ByVal Result As Integer, ByVal IssuerID As Integer)
Label4.Caption = Result
Select Case Result

Case 0
ok_to_opendisp = True
Case 1
ok_to_opendisp = False
Case 2
ok_to_opendisp = False

Case Else
ok_to_opendisp = False

End Select
Bank_answer = True

End Sub

Private Sub baxi_OnPrinterText()
com_print.Output = baxi.PrintText
com_print.Output = Chr(27) & Chr(112)
End Sub

Private Sub cmdbank_100_Click()
Bank_answer = False

baxi.TransferAmount_V2 "0000", &H30, 300, &H30, 0, &H30, 0, "LPG Autogas", ""
Do Until Bank_answer
DoEvents
Loop
If ok_to_opendisp Then

Else
MsgBox "IKKE OK Dispenser"
End If

End Sub

Private Sub cmdbank_200_Click()
Bank_answer = False

baxi.TransferAmount_V2 "0000", &H30, 200, &H30, 0, &H30, 0, "LPG Autogas", ""
Do Until Bank_answer
DoEvents
Loop
If ok_to_opendisp Then
MsgBox "OK dispenser"
Else
MsgBox "IKKE OK Dispenser"
End If
End Sub

Private Sub cmddisp_stop_Click()
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H69
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
commandtext = y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
cmdDISPTX.AddItem commandtext
commandtext = ""
End Sub

Private Sub cmdstart_Click()

Dim prg_amount_litre As Integer
Dim prg_amount_kr As Integer
If IsNumeric(prg_amount.Text) Then
    prg_amount_kr = Int(prg_amount.Text)
    y(1) = &H10
    y(2) = &H7
    y(3) = dispnr(0)
    y(4) = &HC3
    y(5) = &H30
    y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
    y(7) = &H36
    comm_out 50, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
    commandtext = Now() & "-->" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7)
    cmdDISPTX.AddItem commandtext
    
    'y(1) = &H10
    'y(2) = &HB
    'y(3) = dispnr(0)
    'y(4) = &H75
    'y(5) = &H30
    'y(6) = &H31
    'y(7) = &H30
    'y(8) = &H30
    'y(9) = &H30
    'y(10) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8) Xor y(9)
    'y(11) = &H36
    'comm_out 50, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11))
    'commandtext = Now() & "-->" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7) & ";" & y(8) & ";" & y(9) & ";" & y(10) & ";" & y(11)
    'cmdDISPTX.AddItem commandtext
    'commandtext = ""
End If

y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H77
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
commandtext = y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
cmdDISPTX.AddItem commandtext
commandtext = ""
End Sub

Private Sub Command7_Click()
y(1) = &H10
y(2) = &H7
y(3) = dispnr(0)
y(4) = &HC3
y(5) = &H30
y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
y(7) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7)
'cmdDISPTX.AddItem commandtext

End Sub



Private Sub com_print_OnComm()

Dim inputstring(15)


Select Case com_print.CommEvent

Case comEvReceive
Com_print_input = com_print.Input
bankkvittering.Text = bankkvittering.Text & " " & Hex(Asc(Com_print_input))


End Select

End Sub

Private Sub Dagstilling_Click()
station_day = True
station_night = False
station_closed = False
Stengt.Checked = False
Nattstilling.Checked = False
Dagstilling.Checked = True
cmdstart.Enabled = True

End Sub

Private Sub dispensere_Click()
dispenser.Show (1)


End Sub

Private Sub Form_Load()

station_closed = True
cmdstart.Enabled = False
Stengt.Checked = True
tank_end = True
DISP_openfordelivery = False


i = 0
disptest_interval = 0
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
    Com_port = cfgline(4)
    Com_port_bank = cfgline(5)
    com_port_print = cfgline(6)
    
    
    Close #1
End If
If db_ok Then
   If cmdcom_on Then
    rts = True
    RST_disp.Open "Select * from dispensere", sqlconn, adOpenKeyset, adLockOptimistic
        dispnr(0) = RST_disp!dispensernr + 32
        i = 1
        disp_init = False
        Do Until disp_init = True Or i = 21
        check_disp_com (dispnr(0))
        DoEvents
        i = i + 1
        
        Loop
        If Not disp_init Then
        MsgBox "Fikk ingen kommunikasjon med dispenser."
        End
        
        End If
        
        dispprice(0) = Format(RST_disp!pris, "0.00")
        disp_setprice dispnr(0), Replace(Format(dispprice(0), "00.00"), ",", "")
    RST_disp.Close
    Else
      MsgBox "Kan ikke åpne kommunikasjon med dispensere.", vbOKOnly + vbCritical, "Kommunikasjonsfeil:"
    End If
    com_print.CommPort = com_port_print
    com_print.PortOpen = True
    
    If bank_online And printer_online Then
        cmdbank_100.Enabled = True
        cmdbank_200.Enabled = True
        cmdbank_400.Enabled = True
        cmdbank_600.Enabled = True
    Else
        cmdbank_100.Enabled = False
        cmdbank_200.Enabled = False
        cmdbank_400.Enabled = False
        cmdbank_600.Enabled = False
    End If
    
Else
MsgBox "Kan ikke åpne database.", vbCritical + vbOKOnly, "Feil med tilkopling."
End
End If

End Sub

Private Sub Form_QueryUnload(Cancel As Integer, UnloadMode As Integer)
If Com_port_bank <> 0 Then baxi.Close
If com_print.PortOpen Then com_print.PortOpen = False

If cmdcom_off Then

Else
    MsgBox "Feil ved lukking av kommunikasjonsport.", vbOKOnly, "Kommunikasjonsfeil"

End If
If sqlconn.State = 1 Then
    sqlconn.Close
    Set sqlconn = Nothing
    End If
End Sub


Private Sub Nattstilling_Click()
station_day = False
station_night = True
station_closed = False
Stengt.Checked = False
Nattstilling.Checked = True
Dagstilling.Checked = False
cmdstart.Enabled = False

End Sub

Private Sub prg_amount_Validate(Cancel As Boolean)
If IsNumeric(prg_amount.Text) Then
    If Not Int(prg_amount.Text) > 999 And Not Int(prg_amount.Text) < 10 Then

    Else
        MsgBox "Ikke gyldig beløp.Må bestå av tall og imellom 10 og 999.", vbCritical + vbOKOnly
        Cancel = True
    End If
End If
End Sub

Private Sub Programinnstillinger_Click()
serverinnstillinger.Show (1)

End Sub

Private Sub state_timer_Timer()

If MSComm1.PortOpen = False Then Exit Sub
If cmdstart.FontBold = True Then cmdstart.FontBold = False

'If Not rts Then Exit Sub

y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H4B
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &HC5
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
If DISP_openfordelivery Or tank_vol >= tank_vol_last Then
    y(1) = &H10
    y(2) = &H6
    y(3) = dispnr(0)
    y(4) = &H45
    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
    y(6) = &H36
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
End If

If disptest_interval = 10 Or disptest_interval = 0 Then
    y(1) = &H10
    y(2) = &H6
    y(3) = dispnr(0)
    y(4) = &H6A
    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
    y(6) = &H36
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
    disptest_interval = 1
End If
disptest_interval = disptest_interval + 1
End Sub

Public Function cmdcom_on()


On Error GoTo errhandler

MSComm1.CommPort = Com_port
   
u = -1


If MSComm1.PortOpen = False Then MSComm1.PortOpen = True '
cmdcom_on = True
Exit Function

errhandler:
MsgBox "Feil ved kommunikasjon:" & Err.Number & " " & Err.Description
cmdcom_on = False
End Function

Public Function cmdcom_off()


On Error GoTo errhandler

If MSComm1.PortOpen = True Then MSComm1.PortOpen = False
'Set MSComm1 = Nothing

cmdcom_off = True
Exit Function

errhandler:
cmdcom_off = False
MsgBox "Feil ved lukking av comport, restart av maskinen ved å gjøre den strømløs er anbefalt:" & Err.Number & " " & Err.Description

End Function

Private Sub MSComm1_OnComm()
'On Error Resume Next
Select Case MSComm1.CommEvent

   
    Case comEvReceive   ' Received RThreshold # of
    rts = False
    If u > 15 Then
        u = -1
        commandtext_in = ""
    End If
    u = u + 1
    charstr = MSComm1.Input
    
    x(u) = Asc(charstr)
    
    commandtext_in = commandtext_in & x(u) & ";"
    If x(u) = 54 And x(0) = 32 And x(1) = (u + 1) Then 'END og Begin har kommet
        chksum = 0
        For i = 0 To u - 2
            chksum = chksum Xor x(i)        'Vi kalkulerer CRC på mottatt string
        Next
        If chksum = x(u - 1) Then
            COM_id = COM_id + 1
            CMDDISPRX.AddItem COM_id & "-_" & Now() & " -- chksum :" & chksum & " <-- " & commandtext_in
            commandtext_in = ""
            u = -1
            Select Case x(3)
               Case 30           '(OK) Command acknowledgement
                    
               Case 37            '(ERROR) Error code data
               
               Case 69
                tank_vol = CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
                If ((tank_vol_last = tank_vol) And trans_unaccounted = True) Then
                SaveSetting "LPGTank", "Lasttank", "Tank_status", 4
                tankinger.Recordset.AddNew
                tankinger.Recordset!datostart = GetSetting("LPGTank", "Lasttank", "Tank_Start")
                tankinger.Recordset!betalingstype = GetSetting("LPGTank", "Lasttank", "Tank_betalingstype")
                tankinger.Recordset!Status = GetSetting("LPGTank", "Lasttank", "Tank_status")
                tankinger.Recordset!datostopp = Now()
                tankinger.Recordset!liter = tank_vol
                tankinger.Recordset!Sum = tank_sum
                tankinger.Recordset!pris = tank_unitprice
                tankinger.Recordset.Update
                tank_vol = 0
                com_print.Output = "LPG Autogas ant.Liter  :" & tankvol & Chr(27) & Chr(10)
                com_print.Output = "          Kr/L inkl VAT:" & tank_unitprice & Chr(27) & Chr(10)
                com_print.Output = "                    Sum:" & tank_sum & Chr(27) & Chr(10)
                com_print.Output = "Innbetalt 100kr - " & tank_sum & Chr(27) & Chr(10)
                com_print.Output = "Tilgode kr:????.??."
                com_print.Output = " Sett inn kort, beløp tilbakeføres." & Chr(27) & Chr(10)
                com_print.Output = Chr(27) & Chr(112) & Chr(30)
                
                
                If Err = 0 Then
                    rts = True
                    y(1) = &H10
                    y(2) = &H6
                    y(3) = dispnr(0)
                    y(4) = &H81
                    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                    y(6) = &H36
                    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                    rts = False
                Else
                MsgBox "error:" & Err.Number & " " & Err.Description
                    End If
                Else
                    tank_vol_last = tank_vol
                    tank_unitprice = dispprice(0)
                    tank_sum = tank_vol * tank_unitprice
                    antall_liter.Text = Format(tank_vol, "0.00") 'Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
                    belop.Text = Format(tank_sum, "0.00") 'Format(((Int(Replace(antall_liter.Text, ".", "", 1, -1, vbTextCompare)) / 100) * (Int(Replace(DisPris.Text, ".", "", 1, -1, vbTextCompare)) / 100)), "0000.00")
                    SaveSetting "LPGTank", "Lasttank", "Tank_vol", tank_vol
                    SaveSetting "LPGTank", "Lasttank", "Tank_unitprice", tank_unitprice
                    SaveSetting "LPGTank", "Lasttank", "tank_sum", tank_sum
                    SaveSetting "LPGTank", "Lasttank", "Tank_status", 3
                End If
             
                    Case 75
                    state_string = decimaltobinn(x(4))
                    If Mid(state_string, 6, 1) = "1" Then DISP_startbuttonpressed = True Else DISP_startbuttonpressed = False
                    If Mid(state_string, 7, 1) = "1" Then DISP_openfordelivery = True Else DISP_openfordelivery = False
                    If Mid(state_string, 5, 1) = "1" Then disp_automode = True Else disp_automode = False
                    If DISP_startbuttonpressed Then
                        If new_tank = False Then
                            new_tank = True
                            tank_end = False
                            SaveSetting "LPGTank", "Lasttank", "Tank_Start", Now()
                            SaveSetting "LPGTank", "Lasttank", "Tank_status", 1
                            SaveSetting "LPGTank", "Lasttank", "Tank_betalingstype", 1
                        End If
                    If DISP_openfordelivery Then
                            tank_vol = 0
                            tank_vol_last = 0
                            tank_unitprice = 0
                            tank_sum = 0
                            SaveSetting "LPGTank", "Lasttank", "Tank_status", 2
                    End If
                    
                    Else
                    new_tank = False
                    End If
                    DISP_errorstatestring = Mid(state_string, 2, 2)
                    If DISP_startbuttonpressed And Not DISP_openfordelivery And Not station_closed Then
                        If disp_automode Then
                            valresult = sndPlaySound("c:\tada.wav", &H1)
                            cmdstart.FontBold = True
                        End If
                    End If
                                        
                    Case 92        '(PRICE) Give / take the fuel price
                     DisPris.Text = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
        
                     Case 106           '(LINETEST) Transmission channel test
                        If x(4) = 85 And x(5) = 170 Then
                        disp_init = True
                        Else
                        disp_init = False
                        
                        End If
                      
                     
                    Case 133           '(SUM) Give / take total sum of delivered fuel and number of transactions from switching on the supply
                    
                    Case 197            '(Tank)
                    state_string = decimaltobinn(x(4))
                    If Mid(state_string, 8, 1) = "1" Then trans_finished_powerfault = True Else trans_finished_powerfault = False
                    If Mid(state_string, 5, 1) = "1" Then trans_unaccounted = True Else trans_unaccounted = False
                    'Text1.Text = state_string
                   
            End Select   'Select..Case..end hvilken kommando er det
            Else
                CMDDISPRX.AddItem COM_id & "-_" & Now() & " -- chksum :" & chksum & " <-- " & commandtext_in  'Kalkulert chksum er ikke lik med oppgitt chksum i string
            End If
    
    For u = 0 To 15     'vi nullstiller x() etter godkjent kommando
        x(u) = 0
    Next
    u = -1
    commandtext_in = ""
    Else
    
    End If      'Har END kommet?
 
End Select      'Select..END Comm_event
rts = True

End Sub

Private Sub Stengt_Click()
station_day = False
station_night = False
station_closed = True
Stengt.Checked = True
Nattstilling.Checked = False
Dagstilling.Checked = False
cmdstart.Enabled = False

End Sub

Private Sub vistankinger_AfterInsert()
tankinger.Recordset.Sort = "datostart desc"

End Sub

