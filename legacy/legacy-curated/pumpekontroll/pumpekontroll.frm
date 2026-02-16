VERSION 5.00
Object = "{648A5603-2C6E-101B-82B6-000000000014}#1.1#0"; "MSCOMM32.OCX"
Object = "{831FDD16-0C5C-11D2-A9FC-0000F8754DA1}#2.0#0"; "mscomctl.ocx"
Object = "{3B7C8863-D78F-101B-B9B5-04021C009402}#1.2#0"; "richtx32.ocx"
Object = "{248DD890-BB45-11CF-9ABC-0080C7E7B78D}#1.0#0"; "MSWINSCK.OCX"
Object = "{20C62CAE-15DA-101B-B9A8-444553540000}#1.1#0"; "MSMAPI32.OCX"
Object = "{675FD4BC-A245-4921-8195-2D7401C2BB15}#1.0#0"; "baxi.dll"
Begin VB.Form Pumpekontroll 
   BorderStyle     =   1  'Fixed Single
   Caption         =   "LPG Dispenserkontroll"
   ClientHeight    =   10860
   ClientLeft      =   -30
   ClientTop       =   615
   ClientWidth     =   15570
   LinkTopic       =   "Form2"
   ScaleHeight     =   10860
   ScaleWidth      =   15570
   WindowState     =   2  'Maximized
   Begin VB.CheckBox softrestart 
      Caption         =   "Soft-estart mulig"
      Height          =   195
      Left            =   10320
      TabIndex        =   74
      Top             =   6480
      Value           =   1  'Checked
      Width           =   1455
   End
   Begin VB.CommandButton EHLunblock_cont 
      BackColor       =   &H000000FF&
      Caption         =   "Unblocked"
      Height          =   255
      Left            =   7800
      Style           =   1  'Graphical
      TabIndex        =   73
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton EHLstartbutton_cont 
      BackColor       =   &H000000FF&
      Caption         =   "Startswitch"
      Height          =   255
      Left            =   8880
      Style           =   1  'Graphical
      TabIndex        =   72
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton EHLstate_cont 
      BackColor       =   &H000000FF&
      Caption         =   "state"
      Height          =   255
      Left            =   6720
      Style           =   1  'Graphical
      TabIndex        =   71
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLerror_cont 
      BackColor       =   &H000000FF&
      Caption         =   "Err"
      Height          =   255
      Left            =   7800
      Style           =   1  'Graphical
      TabIndex        =   70
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLvolume_cont 
      BackColor       =   &H000000FF&
      Caption         =   "Vol"
      Height          =   255
      Left            =   8880
      Style           =   1  'Graphical
      TabIndex        =   69
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLtank_cont 
      BackColor       =   &H000000FF&
      Caption         =   "Tank"
      Height          =   255
      Left            =   9960
      Style           =   1  'Graphical
      TabIndex        =   68
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLsetamount 
      BackColor       =   &H000000FF&
      Caption         =   "SetAmount"
      Height          =   255
      Left            =   1920
      Style           =   1  'Graphical
      TabIndex        =   67
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton EHLunblock 
      BackColor       =   &H000000FF&
      Caption         =   "Unblocked"
      Height          =   255
      Left            =   3000
      Style           =   1  'Graphical
      TabIndex        =   66
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton EHLstartbutton 
      BackColor       =   &H000000FF&
      Caption         =   "Startbutton"
      Height          =   255
      Left            =   4080
      Style           =   1  'Graphical
      TabIndex        =   65
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton EHLunaccounted 
      BackColor       =   &H000000FF&
      Caption         =   "Unaccount"
      Height          =   255
      Left            =   5160
      Style           =   1  'Graphical
      TabIndex        =   64
      Top             =   6480
      Width           =   975
   End
   Begin VB.CommandButton Ehltank 
      BackColor       =   &H000000FF&
      Caption         =   "Tank"
      Height          =   255
      Left            =   5160
      Style           =   1  'Graphical
      TabIndex        =   63
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLvol 
      BackColor       =   &H000000FF&
      Caption         =   "Vol"
      Height          =   255
      Left            =   4080
      Style           =   1  'Graphical
      TabIndex        =   62
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLerror 
      BackColor       =   &H000000FF&
      Caption         =   "Err"
      Height          =   255
      Left            =   3000
      Style           =   1  'Graphical
      TabIndex        =   61
      Top             =   6120
      Width           =   975
   End
   Begin VB.CommandButton EHLstate 
      BackColor       =   &H000000FF&
      Caption         =   "state"
      Height          =   255
      Left            =   1920
      Style           =   1  'Graphical
      TabIndex        =   60
      Top             =   6120
      Width           =   975
   End
   Begin VB.Timer timeout_timer 
      Enabled         =   0   'False
      Interval        =   1000
      Left            =   11880
      Top             =   4440
   End
   Begin VB.CheckBox ehldebug 
      Caption         =   "ehldebug"
      Height          =   255
      Left            =   11880
      TabIndex        =   59
      Top             =   6450
      Width           =   1935
   End
   Begin VB.CommandButton datasetdownload 
      Caption         =   "Hent kortavtaler"
      Height          =   495
      Left            =   10920
      TabIndex        =   58
      Top             =   600
      Width           =   975
   End
   Begin VB.CommandButton swupdate 
      Caption         =   "SW Download"
      Height          =   495
      Left            =   10920
      TabIndex        =   57
      Top             =   0
      Width           =   975
   End
   Begin VB.TextBox cashback_ore 
      Alignment       =   1  'Right Justify
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   9.75
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   405
      Left            =   11520
      TabIndex        =   46
      Text            =   "00"
      Top             =   2610
      Width           =   495
   End
   Begin VB.CommandButton Command3 
      Caption         =   "Finish"
      Height          =   495
      Left            =   12120
      TabIndex        =   45
      Top             =   1200
      Width           =   975
   End
   Begin VB.CommandButton Annuler 
      Caption         =   "Annuler siste"
      Height          =   495
      Left            =   12120
      TabIndex        =   44
      Top             =   1800
      Width           =   975
   End
   Begin VB.CommandButton avbryt 
      Caption         =   "Avbryt"
      Height          =   495
      Left            =   12120
      TabIndex        =   43
      Top             =   600
      Width           =   975
   End
   Begin VB.ComboBox Baxilevel 
      Height          =   315
      ItemData        =   "pumpekontroll.frx":0000
      Left            =   10440
      List            =   "pumpekontroll.frx":0013
      TabIndex        =   40
      Text            =   "0 Ingen logging"
      Top             =   3720
      Width           =   2415
   End
   Begin VB.CommandButton baxilog_set 
      Caption         =   "Set"
      Height          =   255
      Left            =   10440
      TabIndex        =   39
      Top             =   4080
      Width           =   375
   End
   Begin VB.CommandButton avstemming 
      Caption         =   "Avstemming"
      Height          =   495
      Left            =   12120
      TabIndex        =   37
      Top             =   0
      Width           =   975
   End
   Begin VB.ListBox List1 
      Height          =   840
      Left            =   3000
      TabIndex        =   36
      Top             =   120
      Width           =   3135
   End
   Begin VB.CheckBox Check2 
      Caption         =   "Virtual release start"
      Height          =   195
      Left            =   8640
      TabIndex        =   35
      Top             =   0
      Width           =   1695
   End
   Begin VB.CheckBox chkdagmodus 
      Caption         =   "Dagmodus"
      Height          =   195
      Left            =   120
      TabIndex        =   33
      Top             =   1200
      Width           =   1335
   End
   Begin MSWinsockLib.Winsock status_poller 
      Left            =   12120
      Top             =   5520
      _ExtentX        =   741
      _ExtentY        =   741
      _Version        =   393216
      LocalPort       =   86
   End
   Begin MSComctlLib.StatusBar StatusBar2 
      Align           =   2  'Align Bottom
      Height          =   375
      Left            =   0
      TabIndex        =   29
      Top             =   10110
      Width           =   15570
      _ExtentX        =   27464
      _ExtentY        =   661
      _Version        =   393216
      BeginProperty Panels {8E3867A5-8586-11D1-B16A-00C0F0283628} 
         NumPanels       =   3
         BeginProperty Panel1 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   9102
            Key             =   "status"
            Object.Tag             =   "status"
         EndProperty
         BeginProperty Panel2 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   9102
         EndProperty
         BeginProperty Panel3 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   9102
         EndProperty
      EndProperty
   End
   Begin VB.CommandButton Command2 
      Caption         =   "Manuall RFID"
      Height          =   315
      Left            =   9240
      TabIndex        =   27
      Top             =   990
      Width           =   1215
   End
   Begin VB.CheckBox Check3 
      Caption         =   "Stasjonskreditt"
      Height          =   255
      Left            =   120
      TabIndex        =   26
      Top             =   2040
      Value           =   1  'Checked
      Width           =   1455
   End
   Begin VB.TextBox rfidtext 
      Height          =   375
      Left            =   6720
      TabIndex        =   25
      Top             =   960
      Width           =   2415
   End
   Begin MSCommLib.MSComm RFIDCOM 
      Left            =   12240
      Top             =   4920
      _ExtentX        =   1005
      _ExtentY        =   1005
      _Version        =   393216
      CommPort        =   6
      DTREnable       =   0   'False
      InputLen        =   16
      NullDiscard     =   -1  'True
      RThreshold      =   16
      SThreshold      =   1
   End
   Begin VB.CheckBox frigi_bank 
      Caption         =   "Frigi bank_virtual"
      Height          =   255
      Left            =   6840
      TabIndex        =   24
      Top             =   0
      Width           =   1575
   End
   Begin MSMAPI.MAPISession MAPISession1 
      Left            =   10440
      Top             =   5520
      _ExtentX        =   1005
      _ExtentY        =   1005
      _Version        =   393216
      DownloadMail    =   0   'False
      LogonUI         =   -1  'True
      NewSession      =   0   'False
   End
   Begin MSMAPI.MAPIMessages MAPIMessages1 
      Left            =   11040
      Top             =   5520
      _ExtentX        =   1005
      _ExtentY        =   1005
      _Version        =   393216
      AddressEditFieldCount=   1
      AddressModifiable=   0   'False
      AddressResolveUI=   0   'False
      FetchSorted     =   0   'False
      FetchUnreadOnly =   0   'False
   End
   Begin VB.ListBox tcplog 
      Height          =   645
      Left            =   240
      TabIndex        =   23
      Top             =   8640
      Width           =   13215
   End
   Begin VB.CheckBox Check1 
      Caption         =   "Kortbetaling aktiv"
      Height          =   255
      Left            =   120
      TabIndex        =   20
      Top             =   1560
      Value           =   1  'Checked
      Width           =   1575
   End
   Begin VB.ListBox errorlist 
      Height          =   1620
      Left            =   240
      TabIndex        =   19
      Top             =   6840
      Width           =   13215
   End
   Begin VB.CommandButton printkvitt 
      Caption         =   "Skriv ut"
      Height          =   255
      Left            =   10560
      TabIndex        =   18
      Top             =   1920
      Width           =   975
   End
   Begin VB.TextBox cashback_kr 
      Alignment       =   1  'Right Justify
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   9.75
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   405
      Left            =   10440
      TabIndex        =   17
      Text            =   "0"
      Top             =   2610
      Width           =   975
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Tilbakefør"
      Height          =   615
      Left            =   12120
      TabIndex        =   16
      Top             =   2400
      Width           =   975
   End
   Begin VB.Timer task_timer 
      Enabled         =   0   'False
      Interval        =   60000
      Left            =   11400
      Top             =   4440
   End
   Begin VB.ListBox container_tank 
      Height          =   645
      Left            =   6720
      TabIndex        =   15
      Top             =   4800
      Width           =   3732
   End
   Begin MSWinsockLib.Winsock tcpserver 
      Left            =   11640
      Top             =   5520
      _ExtentX        =   741
      _ExtentY        =   741
      _Version        =   393216
      LocalPort       =   9002
   End
   Begin MSComctlLib.StatusBar StatusBar1 
      Align           =   2  'Align Bottom
      Height          =   375
      Left            =   0
      TabIndex        =   11
      Top             =   10485
      Width           =   15570
      _ExtentX        =   27464
      _ExtentY        =   661
      _Version        =   393216
      BeginProperty Panels {8E3867A5-8586-11D1-B16A-00C0F0283628} 
         NumPanels       =   5
         BeginProperty Panel1 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   5450
            Text            =   "Status dispenser"
            TextSave        =   "Status dispenser"
            Key             =   "dispstat"
            Object.ToolTipText     =   "Viser status til Dispenser"
         EndProperty
         BeginProperty Panel2 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   5450
            Text            =   "Status printer"
            TextSave        =   "Status printer"
            Key             =   "statprn"
         EndProperty
         BeginProperty Panel3 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   5450
            Text            =   "Status papir"
            TextSave        =   "Status papir"
            Object.ToolTipText     =   "Viser om rullen begynner og nærme seg slutten"
         EndProperty
         BeginProperty Panel4 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   5450
            Text            =   "Status Bet.terminal dispenser"
            TextSave        =   "Status Bet.terminal dispenser"
            Key             =   "statbetdisp"
            Object.ToolTipText     =   "Her vises status for betalingsterminal ute ved dispenser"
         EndProperty
         BeginProperty Panel5 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   5450
            Text            =   "Status klient:"
            TextSave        =   "Status klient:"
            Key             =   "statklient"
            Object.ToolTipText     =   "Viser informasjon om status til kontroll klient."
         EndProperty
      EndProperty
   End
   Begin VB.Timer Printer_state 
      Enabled         =   0   'False
      Interval        =   10000
      Left            =   10920
      Top             =   4440
   End
   Begin RichTextLib.RichTextBox RichTextBox1 
      Height          =   2895
      Left            =   6720
      TabIndex        =   10
      TabStop         =   0   'False
      Top             =   1800
      Width           =   3735
      _ExtentX        =   6588
      _ExtentY        =   5106
      _Version        =   393217
      Enabled         =   -1  'True
      ScrollBars      =   3
      TextRTF         =   $"pumpekontroll.frx":0095
   End
   Begin MSCommLib.MSComm com_pinpad 
      Left            =   11640
      Top             =   4920
      _ExtentX        =   979
      _ExtentY        =   979
      _Version        =   393216
      CommPort        =   4
      DTREnable       =   0   'False
      RThreshold      =   1
      SThreshold      =   1
   End
   Begin MSCommLib.MSComm com_print 
      Left            =   11040
      Top             =   4920
      _ExtentX        =   979
      _ExtentY        =   979
      _Version        =   393216
      CommPort        =   5
      DTREnable       =   0   'False
      InputLen        =   1
      ParityReplace   =   0
      RThreshold      =   1
      SThreshold      =   1
   End
   Begin VB.CommandButton cmdbank_200 
      Caption         =   "400KR"
      Height          =   492
      Left            =   7680
      Style           =   1  'Graphical
      TabIndex        =   8
      TabStop         =   0   'False
      Top             =   360
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_400 
      Caption         =   "600 KR"
      Height          =   492
      Left            =   8640
      Style           =   1  'Graphical
      TabIndex        =   7
      TabStop         =   0   'False
      Top             =   360
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_600 
      Caption         =   "1200Kr"
      Height          =   492
      Left            =   9600
      Style           =   1  'Graphical
      TabIndex        =   6
      TabStop         =   0   'False
      Top             =   360
      Width           =   852
   End
   Begin VB.CommandButton cmdbank_100 
      Caption         =   "200KR"
      Height          =   492
      Left            =   6720
      Style           =   1  'Graphical
      TabIndex        =   5
      TabStop         =   0   'False
      Top             =   360
      Width           =   852
   End
   Begin MSCommLib.MSComm MSComm1 
      Left            =   10440
      Top             =   4920
      _ExtentX        =   979
      _ExtentY        =   979
      _Version        =   393216
      CommPort        =   3
      DTREnable       =   -1  'True
      InputLen        =   1
      NullDiscard     =   -1  'True
      RThreshold      =   1
   End
   Begin VB.Timer state_timer 
      Enabled         =   0   'False
      Interval        =   1000
      Left            =   10440
      Top             =   4440
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
      Height          =   735
      Left            =   0
      MaskColor       =   &H000000FF&
      Style           =   1  'Graphical
      TabIndex        =   1
      TabStop         =   0   'False
      Top             =   6000
      Width           =   1815
   End
   Begin VB.CommandButton cmdstart 
      BackColor       =   &H0000C000&
      Caption         =   "Frigi dispenser"
      DisabledPicture =   "pumpekontroll.frx":0117
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
      TabIndex        =   0
      TabStop         =   0   'False
      ToolTipText     =   "Trykk for å frigi dispenser."
      Top             =   1080
      Width           =   3735
   End
   Begin BAXILibCtl.BaxiCtrl Baxi 
      Left            =   12000
      OleObjectBlob   =   "pumpekontroll.frx":04ED
      Top             =   4080
   End
   Begin VB.Label Label22 
      Caption         =   "Label22"
      Height          =   495
      Left            =   120
      TabIndex        =   56
      Top             =   480
      Width           =   2775
   End
   Begin VB.Label Label21 
      Height          =   375
      Left            =   13200
      TabIndex        =   55
      Top             =   4560
      Width           =   2175
   End
   Begin VB.Label Label20 
      Height          =   375
      Left            =   13200
      TabIndex        =   54
      Top             =   3960
      Width           =   2175
   End
   Begin VB.Label Label19 
      Height          =   375
      Left            =   13080
      TabIndex        =   53
      Top             =   3360
      Width           =   2175
   End
   Begin VB.Label Label18 
      Height          =   375
      Left            =   13200
      TabIndex        =   52
      Top             =   2760
      Width           =   2175
   End
   Begin VB.Label Label17 
      Height          =   255
      Left            =   4560
      TabIndex        =   51
      Top             =   5640
      Width           =   1575
   End
   Begin VB.Label Label16 
      Height          =   255
      Left            =   9240
      TabIndex        =   50
      Top             =   5640
      Width           =   1095
   End
   Begin VB.Label Label15 
      Height          =   255
      Left            =   13200
      TabIndex        =   49
      Top             =   5160
      Width           =   2295
   End
   Begin VB.Label Label14 
      Height          =   375
      Left            =   13200
      TabIndex        =   48
      Top             =   2160
      Width           =   2175
   End
   Begin VB.Label Label13 
      Height          =   375
      Left            =   13200
      TabIndex        =   47
      Top             =   1560
      Width           =   2295
   End
   Begin VB.Label errorcode_container 
      Height          =   255
      Left            =   8160
      TabIndex        =   42
      Top             =   5640
      Width           =   855
   End
   Begin VB.Label errorcode_autogass 
      Height          =   255
      Left            =   3480
      TabIndex        =   41
      Top             =   5640
      Width           =   855
   End
   Begin VB.Label Label9 
      Caption         =   "Baxilog nivå:"
      Height          =   255
      Left            =   10440
      TabIndex        =   38
      Top             =   3360
      Width           =   975
   End
   Begin VB.Label Label8 
      Height          =   255
      Left            =   13200
      TabIndex        =   34
      Top             =   1080
      Width           =   2295
   End
   Begin VB.Label Label6 
      Height          =   255
      Left            =   14520
      TabIndex        =   32
      Top             =   120
      Width           =   1095
   End
   Begin VB.Label Label5 
      Caption         =   "Betalingstype:"
      Height          =   255
      Left            =   13200
      TabIndex        =   31
      Top             =   120
      Width           =   1095
   End
   Begin VB.Label Label4 
      Caption         =   "Label4"
      Height          =   255
      Left            =   120
      TabIndex        =   30
      Top             =   120
      Width           =   2655
   End
   Begin VB.Label Label12 
      Height          =   255
      Left            =   13200
      TabIndex        =   28
      Top             =   600
      Width           =   2295
   End
   Begin VB.Label Label11 
      Height          =   255
      Left            =   2400
      TabIndex        =   22
      Top             =   5640
      Width           =   855
   End
   Begin VB.Label Label10 
      Height          =   255
      Left            =   6840
      TabIndex        =   21
      Top             =   5640
      Width           =   1095
   End
   Begin VB.Label dispris 
      Alignment       =   1  'Right Justify
      BorderStyle     =   1  'Fixed Single
      Caption         =   "00.00"
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "# ##0,00"
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
      Height          =   972
      Left            =   2400
      TabIndex        =   14
      Top             =   4560
      Width           =   3732
   End
   Begin VB.Label antall_liter 
      Alignment       =   1  'Right Justify
      BorderStyle     =   1  'Fixed Single
      Caption         =   "0000.00"
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "# ##0,00"
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
      Height          =   972
      Left            =   2400
      TabIndex        =   13
      Top             =   3360
      Width           =   3732
   End
   Begin VB.Label belop 
      Alignment       =   1  'Right Justify
      BorderStyle     =   1  'Fixed Single
      Caption         =   "0000.00"
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "# ##0,00"
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
      Height          =   972
      Left            =   2400
      TabIndex        =   12
      Top             =   2040
      Width           =   3732
   End
   Begin VB.Label Label7 
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   6720
      TabIndex        =   9
      Top             =   1440
      Width           =   3735
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
      TabIndex        =   4
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
      TabIndex        =   3
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
      Height          =   375
      Left            =   120
      TabIndex        =   2
      Top             =   2400
      Width           =   1935
   End
   Begin VB.Menu Instillinger 
      Caption         =   "&Instillinger"
      Begin VB.Menu Firmainformasjon 
         Caption         =   "Firmainformasjon"
      End
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
   Begin VB.Menu Logs 
      Caption         =   "Logg"
   End
   Begin VB.Menu Kvitteringer 
      Caption         =   "&Kvitteringer"
   End
   Begin VB.Menu tankinger 
      Caption         =   "&Tankinger"
   End
   Begin VB.Menu Stasjonskreditt 
      Caption         =   "&Stasjonskreditt"
      Begin VB.Menu oppdaterstasjonskort 
         Caption         =   "Oppdater stasjonskorttabell"
      End
      Begin VB.Menu ikkeimporterte 
         Caption         =   "Ikke importerte"
      End
      Begin VB.Menu sokkortnummer 
         Caption         =   "&Søk kortnummer"
      End
   End
   Begin VB.Menu testrutiner 
      Caption         =   "Funksjoner"
      Begin VB.Menu restartterminal 
         Caption         =   "Restart betalingsterminal"
      End
   End
End
Attribute VB_Name = "Pumpekontroll"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Sub Annuler_Click()
If MsgBox("Er du helt sikker på at du vil annulere siste korttransaksjon? Dette er ikke reversibelt!", vbCritical + vbYesNo) = vbYes Then
    Baxi_Reversal
End If
End Sub

Private Sub avbryt_Click()
return_amount = False
Baxi.Administration &H3132
End Sub

Private Sub avstemming_Click()
return_amount = False

Baxi.Administration &H3130, 0

End Sub

Private Sub baxi_OnDisplayText()
Label7.Caption = Baxi.DisplayText
tcpsend "<TANK_TERMINAL_MESSAGE>;" & Label7.Caption & ";<SLUTT>"
End Sub

Private Sub baxi_OnError()

Select Case Baxi.LastError
Case 2000
berr = "Serial communication error"
Case 2001
berr = "Port already opened"
Case 2002
berr = "Can 't install timer"
Case 2003
berr = "invalid COM port"
Case 2004
berr = "invalid value of protocol type"
Case 2005
berr = "invalid printer status"
Case 2006
berr = "invalid administration code"
Case 2007
berr = "currently in bank mode"
Case 2008
berr = "application timeout"
Case 2009
berr = "terminal not responding after exhausted retries."
Case 2010
berr = "terminal Not connected Or Not polling"
Case 2011
berr = "error in sending MSG"
Case 2012
berr = "wait event error"
Case 2013
berr = "no EOT after acking msg"
Case 2014
berr = "received LRC Is bad"
Case 2015
berr = "ACK Not received"
Case 2016
berr = "Control not active (unsuccessful Open())"
Case 2017
berr = "sending message with retries but the response is NAK"
Case 2018
berr = "no response from BOPOS server on host message"
Case 2019
berr = "error connecting to host"
Case 2020
berr = "du kjører mot SDI"
Case 2021
berr = "feil i mottatte data, ikke dle+stx ...dle+etx"
Case 2022
berr = "feil ved sending til HOST"
Case 2023
berr = "feilmelding fra BOPOS"
Case 2024
berr = "Invalid ECR message"
Case 2025
berr = "BAXI is waiting for application ack from terminal"
Case 2026
berr = "Closing of the communications thread failed"
Case 2027
berr = "Link layer collision, terminal has discarded the message."

Case 6001
berr = "Baxi allready open"
Case 6003
berr = "Baxi is busy"
Case 6004
berr = "Cancel received when not in Bank Mode"
Case 6005
berr = "Device attribute values are incorrect, this error can occur at Open, if the device attributes are discovered to be invalid."

Case 1000
berr = "  internal Errors"

Case 1001
berr = "invalid comm.Handle"
Case 1002
berr = "getcommask failed"

Case 1003
berr = "setcommstate failed"
Case 1004
berr = "getcommdcb failed"
Case 1005
berr = "getcommstate failed"
Case 1006
berr = "getcommtimeout failed"
Case 1007
berr = "setcommtimeout failed"
Case 1008
berr = "thread creation failed"
Case 1009
berr = "setevvent failed"
Case 1010
berr = "seteventmask failed"
Case 1011
berr = "settimeout failed"
Case 1012
berr = "comm.Handle Not valid"
Case 1013
berr = "parity Error"
Case 1014
berr = "rx overrun"
Case 1015
berr = "waitobject in comm. handler failed"
Case 1016
berr = "no STX"
Case 1017
berr = "no ETX"
Case 1018
berr = "waitobject() failed"
Case 1019
berr = "unable to reset timer"
Case 1020
berr = "case not handled in WaitFor()"
Case 1021
berr = "case not handled in WaitLRC()"
Case 1022
berr = "message received is invalid no stx"
Case 1023
berr = "call to ClearCommError() failed"
Case 1024
berr = "error in waitcommevent()"
Case 1025
berr = "getoverlapped() result failed"
Case 1026
berr = "received Data > buffer"
Case 1027
berr = "wait for single object failed in WaitFor();"
Case 1028
berr = "wait for single object failed in WaitLRC();"
Case 1029
berr = "Read delayed"
Case 1030
berr = "Data Size Is bad"
Case 1031
berr = "No character received in WaitLRC();"
Case 1032
berr = "case not handled in WaitFor()"
Case 1033
berr = "wait for single object failed in WaitFor();"
Case 1034
berr = "comm signal event thread creation failed"
Case 1035
berr = "no more space for error log text"
Case 1036
berr = "sock signal event thread creation failed"
Case 1037
berr = "Out of memory"
Case 1038
berr = "Creation of an event handle failed"
Case 1039
berr = "ERR_WAIT_ABANDON"
Case 1040
berr = "Setting/signalling an event failed"
'
'    Serial communications errors
'Sub errors 2001-2999

'
'Error code  MsgRouter errors
Case 2100
berr = "2100"
Case 2101
berr = "General msgrouter error"
Case 2102
berr = "Msgrouter timeout"
Case 2103
berr = "Socket Error"
Case 2104
berr = "Message length error"
Case 2105
berr = "Sending of message failed"
Case 2106
berr = "Connection Error"
'
'    Syntax errors
Case 3000
berr = "Syntax Error"
Case 3001
berr = "Parameter errors"
Case 3002
berr = "invalid pointer"
 '
'    COM/Interop errors
Case 4001
berr = "Com marshal/unmarshal error"
'
'    Other error type
Case 5001
berr = "method Not implemented"
Case 5002
berr = "internal Not method"
'
'    Client errors
End Select
If Not baxierror Then baxierror = True
StatusBar1.Panels.Item(4) = Baxi.LastError & " " & berr

End Sub
Private Sub restart_baxi()
On Error GoTo errhandler
Baxi.Close
Sleep (5000)
baxierror = False
Baxi.Open
Exit Sub
errhandler:
errorlist.AddItem Now & ", restart_baxi :" & Err.Number & " " & Err.Description
Resume Next
End Sub
Private Sub baxi_OnLocalMode(ByVal result As Integer, ByVal IssuerID As Integer)

On Error GoTo errhandler

manual_bank = False

Select Case result

Case 0
    
    rapport_rs.AddNew
    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
    rapport_rs!dato = Now()
    If Not return_amount Then
        rapport_rs!Type = "Forhåndsvalg"
        rapport_rs!cardnumber = Baxi.CardData
        
        ok_to_opendisp = True
        LogEvent "Cardpayment_ACK", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "TANK"
        PaymentType = 2
        bank_charge = reporttext
    Else
        If InStr(1, UCase(reporttext), "ANNULLERING", vbTextCompare) > 0 Then
            rapport_rs!Type = "Annulering"
            rapport_rs!cardnumber = Baxi.CardData
            'reporttext = Replace(reporttext, "NOK", "Annulert/Retur NOK")
        Else
            rapport_rs!Type = "Tilbakeføring"
            'reporttext = Replace(reporttext, "NOK", "Retur NOK")
            rapport_rs!cardnumber = Baxi.CardData
        End If
        cmdbank_100.BackColor = &H8000000F
        cmdbank_200.BackColor = &H8000000F
        cmdbank_400.BackColor = &H8000000F
        cmdbank_600.BackColor = &H8000000F
        ok_to_opendisp = False
        return_amount = False
        PaymentType = 0
        
    End If
    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
    reporttext = Replace(reporttext, Chr(9), "", 1)
    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
    rapport_rs!reporttext = reporttext
    rapport_rs.Update
    RichTextBox1.Text = reporttext
    


Case 1
    
    Dim cashbackstr_header As String
    Dim cashbackstr_body As String
    Dim cashbackstr_footer As String
    Dim cashback_total As Single
    Dim zrapp_amount As Single
    Dim zrapp_str As String
    
    If reporttext <> "" Then
    
    rapport_rs.AddNew
    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
    rapport_rs!dato = Now()
    rapport_rs!Type = rapporttype
    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
    reporttext = Replace(reporttext, Chr(9), "", 1)
    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
    
    
    If rapporttype = "Zrapport" Then
    
        zrapp_str = Mid(reporttext, InStr(1, reporttext, "Total=", vbTextCompare) + 6, Len(reporttext))
        zrapp_str = Left(zrapp_str, Len(zrapp_str) - 3)
        
        zrapp_amount = CSng(zrapp_str)
        
        If lpgnorge.rscashback.State = 0 Then lpgnorge.rscashback.Open Else lpgnorge.rscashback.Requery
        cashbackstr_header = "Tekniske tilbakeføringer siden forrige Z-rapport:" & Chr(10) + Chr(13)
    
        While Not lpgnorge.rscashback.EOF
            cashbackstr_body = cashbackstr_body & lpgnorge.rscashback!dato & "   " & FormatNumber(lpgnorge.rscashback!belop, 2) & Chr(10) & Chr(13)
            cashback_total = cashback_total + lpgnorge.rscashback!belop
            lpgnorge.rscashback!reported = 1
            lpgnorge.rscashback.MoveNext
        Wend
        cashbackstr_footer = "Tilbakeføringer total :" & FormatNumber(cashback_total, 2) & Chr(10) + Chr(13) & "Beløp til bokføring:" & FormatNumber(zrapp_amount - cashback_total, 2)
        reporttext = reporttext + Chr(10) + Chr(13) & cashbackstr_header & cashbackstr_body & cashbackstr_footer
        With lpgnorge.rssalgstall
        If .State = 0 Then lpgnorge.rssalgstall.Open
        
        .AddNew
        !dato = Now
        !zrapportsum = zrapp_amount
        !tekniskretursum = cashback_total
        If lpgnorge.rsstdagensomsetning.State <> 1 Then lpgnorge.rsstdagensomsetning.Open
        !stasjonskredittsum = lpgnorge.rsstdagensomsetning!dagsomsetning
        lpgnorge.rsstdagensomsetning.Close
        If lpgnorge.rsdbo_oms_manuell.State <> 1 Then lpgnorge.rsdbo_oms_manuell.Open
        !manuellsum = lpgnorge.rsdbo_oms_manuell!dagsomsetning
        lpgnorge.rsdbo_oms_manuell.Close
          
        .Update
        End With
        Eml "Zrapport fra betalingsterminal", reporttext, Trim(lpgnorge.rsfirmainfo!zrapp_reciever1)
    End If
    rapport_rs!reporttext = reporttext
    rapport_rs.Update
    ok_to_opendisp = False
        RichTextBox1.Text = reporttext
    End If
    
Case 2

    rapport_rs.AddNew
    LogEvent "Localmode", CStr(result), rapport_rs!reportid, "rapporter_bankterminal", "BAXI"
    rapport_rs!dato = Now()
    rapport_rs!Type = "Terminal"
    If com_print.PortOpen Then com_print.Output = reporttext & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
    reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
    reporttext = Replace(reporttext, Chr(9), "", 1)
    reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
    bank_charge = Replace(bank_charge, Chr(27) + Chr(78) + Chr(1), "", 1)
    bank_charge = Replace(bank_charge, Chr(9), "", 1)
    bank_charge = Replace(bank_charge, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
    bank_tank = Replace(bank_tank, Chr(27) + Chr(78) + Chr(1), "", 1)
    bank_tank = Replace(bank_tank, Chr(9), "", 1)
    bank_tank = Replace(bank_tank, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
    rapport_rs!reporttext = reporttext
    rapport_rs.Update
    
    If return_amount And Not manual_bank Then
        Technical_Cashback ((bank_sum2 - tank_sum2))
    End If
    ok_to_opendisp = False
    RichTextBox1.Text = reporttext
    cmdbank_100.BackColor = &H8000000F
    cmdbank_200.BackColor = &H8000000F
    cmdbank_400.BackColor = &H8000000F
    cmdbank_600.BackColor = &H8000000F
    PaymentType = 0

End Select
'PaymentType = 0
Bank_answer = True
bank_inprogress = False
manual_bank = False
return_amount = False
reporttext = ""
Exit Sub

errhandler:
errorlist.AddItem "Baxi onlocalmode Linje :" & Erl & " " & Err.Number & " " & Err.Description
'PaymentType = 0
Bank_answer = True
bank_inprogress = False
ok_to_opendisp = False
manual_bank = False
return_amount = False
reporttext = ""

End Sub
Private Sub Technical_Cashback(Cashbackbelop As Single)


On Error GoTo errhandler
        rapport_rs.AddNew
        rapport_rs!dato = Now()
        rapport_rs!Type = "Teknisk tilbakeføring."
        
        rapport_rs!reporttext = bank_charge & Chr(13) & "Teknisk tilbakeføring av NOK :" & FormatNumber(Cashbackbelop, 2) & " er sendt vert."
        rapport_rs.Update
        If lpgnorge.rscashback.State = 0 Then lpgnorge.rscashback.Open
       
        lpgnorge.rscashback.AddNew
        lpgnorge.rscashback!dato = Now
        lpgnorge.rscashback!belop = Cashbackbelop
       
        lpgnorge.rscashback!reported = 0
        lpgnorge.rscashback.Update
       
        Eml "Anmodning om vareretur.", "Nedenfor er en kopi av kundens kvittering." & Chr(10) + Chr(13) & bank_charge & Chr(13) & "Pga brukerfeil på vår ubetjente betalingsautomat, har ikke kunden fått tilbake(vareretur) det beløp han/hun har krav på." & Chr(10) + Chr(13) & "Det anmodes derfor at dere foretar en manuell vareretur for ovenstående kort på NOK : " & FormatNumber(Cashbackbelop, 2) & Chr(10) + Chr(13) & "Spørsmål vedr denne vareretur kan rettes til betaling@lpgnorge.no", Technical_Email
        Eml "Manglende tilbakeføring til kortkunde.", bank_charge & Chr(13) & "-----------Tanket----------" & Chr(13) & bank_tank & "-----------Retur----------" & Chr(13) & reporttext, "betaling@lpgnorge.no"
        bank_sum2 = 0
        tank_sum2 = 0

Exit Sub
errhandler:
errorlist.AddItem "Technical_cashback:" & Erl & " " & Err.Number & " " & Err.Description

End Sub
Private Sub baxi_OnPrinterText()
reporttext = Print_reciept_header & Baxi.PrintText
End Sub

Private Sub Baxi_OnResetTimer(ByVal timeVal As Integer)
errorlist.AddItem Now & "Baxi:Needs more time to complete command.  I ask for :" & timeVal & " seconds."
End Sub

Private Sub baxi_OnSendDataReceived()
On Error Resume Next
Dim s As Integer
Dim st As String
Dim ut As String

st = Baxi.ReceivedSendData
ut = Mid(st, 4, Len(st))
tcplog.AddItem ut
If lpgnorge.rsdispensere!preselectionkeys = 0 Then

If Mid(ut, 1, 12) = "200000001013" Then
    If Mid(ut, 19, 1) = "1" And PaymentType < 2 Then
    Ready_to_presum = True
    
    End If
End If
End If

ut = ""
For s = 1 To Len(st)
ut = ut & CStr(Asc(Mid(st, s, 1))) & ";"

Next

tcplog.AddItem Now & " " & "Onsenddatarecieved :" & st & "-->" & ut
End Sub


Private Sub Baxi_OnStdRsp(ByVal resp As String)
'errorlist.AddItem Now & "Baxi:I'm ready"
End Sub

Private Sub Baxi_OnWarning(ByVal warningCode As Long, ByVal warningData As String, ByVal warnText As String)
'errorlist.AddItem Now & "Baxi warning:" & warningCode & " " & warningData & " " & warnText

End Sub

Private Sub baxilog_set_Click()
On Error GoTo errhandler
Baxi.LogFilePath = "C:\pumpestyring\baxilog\"
Baxi.LogFilePrefix = "Baxilog"
Baxi.TraceLevel = CInt(Left(Baxilevel.Text, 1))
restart_baxi
Exit Sub

errhandler:
errorlist.AddItem Now & " baxilog_set, " & Err.Number & " " & Err.Description

End Sub

Private Sub pre_sum(amount As String)
On Error GoTo errhandler

If PaymentType > 1 Or Check1.Value = 0 Or DISP_openfordelivery Or bank_inprogress Then Exit Sub
SetAmount = False
DispUnblock = False
bank_inprogress = True
bank_sum = CSng(amount) / 100

Bank_answer = False
If frigi_bank.Value = 1 Then
    PaymentType = 2
    GoTo ok_bank
End If
LogEvent "Pre_sum financial start", amount, 0, "", "BAXI"
Baxi.TransferAmount_V2 "0000", &H30, CLng(amount), &H30, 0, &H30, 0, "LPG Autogas", ""
Do Until Bank_answer
DoEvents
Loop
 tanktimeout_count = 0
LogEvent "OK_TO_opendisp", "Status:" & ok_to_opendisp, 0, "", "TANK"
If ok_to_opendisp Then


ok_bank:
    If frigi_bank.Value = 1 Then frigi_bank.Value = 0
    retries = 0
    
    Do Until retries >= 30 Or SetAmount = True
        retries = retries + 1
        set_preset_amount amount
        
        DoEvents

    Loop
 If Not SetAmount Then
 LogEvent "setamount not ok", "Retries:" & CStr(retries) & " PREPAYSUM:" & CStr(bank_sum), 0, "", "Dispenser"
    
 GoTo error_disp_com
 
 End If
 
 
   Do Until DispUnblock Or retries >= 30 '_openfordelivery Or retries >= 30
        retries = retries + 1
        disp_unblock
        
        DoEvents
    Loop
    
    If Not DispUnblock Then
    
    LogEvent "UNBLOCK FOR CARD OK", "Retries:" & CStr(retries) & " PREPAYSUM:" & CStr(bank_sum), 0, "", "Dispenser"
    GoTo error_disp_com
    End If
End If
Exit Sub
error_disp_com:
    LogEvent "ERROR_UNBLOCK", "Retries:" & CStr(retries), 0, "", "Dispenser"
    'return_amount = True
    ok_to_opendisp = False
    
    
    reporttext = Chr(9) & "Det har skjedd en feil med kommunikasjon mot dispenser." & Chr(10) _
                            & Chr(9) & "Ditt forhåndsvalgte beløpe er blitt annulert." & Chr(10) _
                            & Chr(10) _
                            & Chr(9) & "It has been a problem with the dispenser," & Chr(10) _
                            & Chr(9) & "Your prepaid amount has been returned to your card." & Chr(10)

   If com_print.PortOpen Then com_print.Output = reporttext
   Baxi_Reversal
   reporttext = ""
    disp_block
   
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", PRE_SUM " & Err.Number & " " & Err.Description
   Resume Next

End Sub
Private Sub Baxi_Reversal()
On Error GoTo errhandler
bank_inprogress = True
return_amount = True
Baxi.Administration &H3134
Exit Sub
errhandler:
errorlist.AddItem "Error Baxi_reversal:" & Err.Number & " " & Err.Description

End Sub
Private Sub set_preset_amount(amount As String)
        y(1) = &H10
        y(2) = &HB
        y(3) = dispnr(0)
        y(4) = &H75
        y(5) = Asc(Mid(amount, 6, 1)) '&H30
        y(6) = Asc(Mid(amount, 5, 1)) '&H30
        y(7) = Asc(Mid(amount, 4, 1)) '&H30
        y(8) = Asc(Mid(amount, 3, 1)) '&H30
        y(9) = Asc(Mid(amount, 2, 1)) '&H31
        y(10) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8) Xor y(9)
        y(11) = &H36
        comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11))
End Sub

Private Sub cashback_kr_GotFocus()
cashback_kr.SelStart = 0
cashback_kr.SelLength = Len(cashback_kr.Text)


End Sub

Private Sub cashback_ore_GotFocus()
cashback_ore.SelStart = 0
cashback_ore.SelLength = Len(cashback_ore.Text)

End Sub

Private Sub cashback_ore_Validate(Cancel As Boolean)
If Len(cashback_ore.Text) <> 2 Then
MsgBox "Feil i ørebeløp.  Må være to tegn"
Cancel = True
End If

End Sub

Private Sub cmdbank_100_Click()

cmdbank_100.BackColor = vbRed
pre_sum txtbankf1
End Sub

Private Sub cmdbank_200_Click()
cmdbank_200.BackColor = vbRed
pre_sum txtbankf2
End Sub

Private Sub cmdbank_400_Click()
cmdbank_400.BackColor = vbRed
pre_sum txtbankf3
End Sub

Private Sub cmdbank_600_Click()
cmdbank_600.BackColor = vbRed
pre_sum txtbankf4
End Sub

Private Sub cmddisp_stop_Click()

'On Error GoTo errhandler
disp_block
Exit Sub
'errhandler:
'   errorlist.AddItem Now & ", cmddisp_stop: " & Err.Number & " " & Err.Description
'   Resume Next
End Sub

Private Sub disp_block()
On Error GoTo errhandler
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H69
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
DispUnblock = False
SetAmount = False
Disp_was_unblocked = False
tanktimeout_count = 0
PaymentType = 0
tank_end = True
new_tank = False '
Reset_disp (dispnr(0))
checkkreditt = False
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", disp_block: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub disp_unblock(Optional pmtype As Integer, Optional amount As String, Optional volume As String)
On Error GoTo errhandler
 tanktimeout_count = 0
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H77
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", disp_unblock " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub cmdstart_Click()
On Error GoTo errhandler
If PaymentType > 1 Then Exit Sub

PaymentType = 1
disp_unblock
LogEvent "Unblock_LocalGUI", "", 0, "", "DISPENSER"
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", cmdstart_click: " & Err.Number & " " & Err.Description
   Resume Next

End Sub

Private Sub com_pinpad_OnComm()
On Error GoTo errhandler
If PaymentType < 2 Then
    Select Case com_pinpad.CommEvent

        Case comEvReceive
            Select Case Asc(com_pinpad.Input)

            Case 17         '100
            cmdbank_100_Click
            
            
            Case 18         '200
            cmdbank_200_Click
            
            Case 19         '400
            cmdbank_400_Click
            
            Case 20         '600
            cmdbank_600_Click
        End Select
    End Select
End If
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", com_pinpad_oncomm: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub com_print_OnComm()
On Error GoTo errhandler
Select Case com_print.CommEvent

Case comEvReceive
charstrprn = com_print.Input

If prn_paperenq Then
    If Asc(charstrprn) = 0 Then prn_paperlow = False Else prn_paperlow = True
    If prn_paperlow Then StatusBar1.Panels.Item(3).Text = "Lite papir igjen på rull." Else StatusBar1.Panels.Item(3).Text = "Papirmengde ok."
    prn_paperenq = False
    
    'Sett inn lav papir mailprosedyre.
    Exit Sub
End If

If prn_statusenq Then
    If Asc(charstrprn) = 6 Then
    StatusBar1.Panels.Item(2).Text = "Printer OK."
    printer_online = True
    prn_statusenq = False
    
    Exit Sub
End If

If prn_statusenq And Asc(charstrprn) = 21 Then
printer_online = False
nextbyteiserror = True
Exit Sub
End If

If prn_statusenq And nextbyteiserror Then

    Select Case Asc(charstrprn)

        Case 1
        StatusBar1.Panels.Item(2).Text = "Papir fast i presentasjonsmodul. Sjekk nedre del av printer for papirrester"
        Case 2
        StatusBar1.Panels.Item(2).Text = "Papir fast i papirkutter. Sjekk midtre del av printer for papirrester"
        Case 3
        StatusBar1.Panels.Item(2).Text = "Printer tom for papir."
        
        Case 4
        StatusBar1.Panels.Item(2).Text = "Printerhode er løftet."
        Case 5
        StatusBar1.Panels.Item(2).Text = "Papir matefeil.Ikke papir i presentasjonsmodul, likevel printet 10cm."
        
        Case 6
        StatusBar1.Panels.Item(2).Text = "Temperaturfeil, printhode temperatur > 60 Celcius."
        Case 7
        StatusBar1.Panels.Item(2).Text = "Presentasjonsmodul kjører ikke."
        Case 8
        StatusBar1.Panels.Item(2).Text = "Papirkræsj mens tilbakeføring finner sted."
        Case 9
        StatusBar1.Panels.Item(2).Text = "Ikke i bruk."
        Case 16
        StatusBar1.Panels.Item(2).Text = "Tilbakeføring gikk ut på tid..Kunde har ikke tatt kvittering."
        Case 10
        StatusBar1.Panels.Item(2).Text = "Markering ikke funnet."
        Case 11
        StatusBar1.Panels.Item(2).Text = "Markeringskalibrering feilet."
        Case 12
        StatusBar1.Panels.Item(2).Text = "Indeksfeil."
        Case 13
        StatusBar1.Panels.Item(2).Text = "Sjekksum feil."
        Case 14
        StatusBar1.Panels.Item(2).Text = "Feil firmware lastet."
        Case 15
        StatusBar1.Panels.Item(2).Text = "Firmware kan ikke starte pga sjekksum feil eller FW ikke lastet."
       
    End Select
     tcpsend "<PRINTERSTATE>;" & StatusBar1.Panels.Item(2).Text & ";<SLUTT>"
    
    prn_statusenq = False
    nextbyteiserror = False
    
    Exit Sub
End If
End If
End Select
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", com_print_oncomm: " & Err.Number & " " & Err.Description
   Resume Next
End Sub
Private Sub cashback(cashback As Single)
On Error GoTo errhandler
 'bank_sum2 = bank_sum
 return_amount = True
 
 manual_bank = True
 bank_inprogress = True
 cashback = cashback * 100
 Baxi.TransferAmount_V2 "0000", &H31, CLng(cashback), &H30, 0, &H30, 0, "Return LPG Autogas", ""
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", cashback: " & Err.Number & " " & Err.Description
   Resume Next
End Sub
Private Sub cash(cashstr As String)
On Error GoTo errhandler
bank_inprogress = True
manual_bank = True
 Baxi.TransferAmount_V2 "0000", &H30, CLng(cashstr), &H30, 0, &H30, 0, "LPG Autogas", ""
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", cash: " & Err.Number & " " & Err.Description
   Resume Next
End Sub


Private Sub Command1_Click()
On Error GoTo errhandler

Dim manual_returnamount As String
If IsNumeric(cashback_kr.Text) And IsNumeric(cashback_ore.Text) Then
    If MsgBox("Er du helt sikker på at du vil foreta en manuell overføring av kr:" & cashback_kr.Text & "." & cashback_ore.Text & "?", vbInformation + vbYesNo) = vbYes Then
        return_amount = True
        manual_bank = True
        manual_returnamount = cashback_kr.Text & cashback_ore.Text
        cashback (CSng(manual_returnamount) / 100)
    End If
Else
MsgBox "Feil i beløp."

End If
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", command1_click: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub Command2_Click()
On Error GoTo errhandler
checkstatcredit rfidtext.Text
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", command2_click: " & Err.Number & " " & Err.Description
   Resume Next
End Sub


Private Sub Command3_Click()
Baxi.Administration &H313B

End Sub

Private Sub Command4_Click()
If Disp_was_unblocked Then Disp_was_unblocked = False Else Disp_was_unblocked = True

End Sub

Private Sub datasetdownload_Click()
Baxi.Administration &H313F, 0


End Sub

Private Sub Form_Load()
On Error GoTo errhandler
Label4.Caption = "Versjon: " & App.Major & "." & App.Minor & "." & App.Revision
If status_poller.State <> sckConnected Then status_poller.Listen
tcpserver.Listen
tcperror = False
PaymentType = 0
manual_bank = False
tank_end = True
DISP_openfordelivery = False
state_timer.Enabled = False
Bank_answer = False
bank_sum = 0
bank_sum2 = 0
return_amount = False
tank_vol_last = 0
trans_unaccounted = False
i = 0
disptest_interval = 0
cmdbank_100.Caption = CStr(Val(Mid(txtbankf1, 1, Len(txtbankf1) - 2)))
cmdbank_200.Caption = CStr(Val(Mid(txtbankf2, 1, Len(txtbankf2) - 2)))
cmdbank_400.Caption = CStr(Val(Mid(txtbankf3, 1, Len(txtbankf3) - 2)))
cmdbank_600.Caption = CStr(Val(Mid(txtbankf4, 1, Len(txtbankf4) - 2)))
MAPISession1.SignOn
MAPISession1.DownLoadMail = False
dataserveronline.Show
tryme:

servicerunning = False

    Set objSet = GetObject("winmgmts:").ExecQuery("SELECT * FROM Win32_Service")
    For Each objInst In objSet
    
        If (UCase(objInst.Name) = "MSSQL$LPGNORGE" Or UCase(objInst.Name) = "MSSQL$SQLEXPRESS") And UCase(objInst.State) = "RUNNING" Then
        servicerunning = True
        Exit For
        End If
    Next
If servicerunning = False Then GoTo tryme
Unload dataserveronline
Sleep (5000)
If db_ok Then
  
   If cmdcom_on Then
        If prn_comon Then
        
        Else
        errorlist.AddItem "Feil med oppsett av printer komm."
        End If
        
        If pinpad_comon Then
        Else
        errorlist.AddItem "Feil med oppsett av pinpad komm."
        End If
        
        If RFID_comon Then
        Else
        errorlist.AddItem "Feil med opppsett av RFID komm."
        End If
        
        If bank_comon Then
        StatusBar1.Panels(4).Text = "Bankterminal online"
        Else
        errorlist.AddItem "Feil med oppsett av bankterminal."
        End If
        
    rts = True
    rapport_rs.Open "select * from rapporter_bankterminal where datediff(month,dato,getdate())<=1 order by dato", sqlconn, adOpenKeyset, adLockOptimistic
   
    
    lpgnorge.rstasks.Open
    
    lpgnorge.rsdispensere.Open
    If lpgnorge.rskunder.State = 0 Then lpgnorge.rskunder.Open
    lpgnorge.rsfirmainfo.Open
    firmanavn = lpgnorge.rsfirmainfo!firmanavn
    firmaadresse = lpgnorge.rsfirmainfo!firmaadresse
    firmapostnr = lpgnorge.rsfirmainfo!firmapostnr
    firmapoststed = lpgnorge.rsfirmainfo!firmapoststed
    firmaepost = lpgnorge.rsfirmainfo!epost
    firmaorgnr = lpgnorge.rsfirmainfo!orgnr
    firmatelefon = lpgnorge.rsfirmainfo!telefonnummer
    firmaåpningstider = lpgnorge.rsfirmainfo!aapningstider
    Technical_Email = lpgnorge.rsfirmainfo!Technical_Email
    dispnr(0) = lpgnorge.rsdispensere!dispensernr + 32
    dispprice(0) = Format(lpgnorge.rsdispensere!pris, "0.00")
    dispcontainernr = lpgnorge.rsdispensere!rs485adrcontainer
    lpgnorge.rslogs.Open
    LogEvent "Programstart", "", 0, "", "Application"
    disp_setprice dispnr(0), Replace(Format(dispprice(0), "00.00"), ",", "")
    'Reset the dispenser
    check_for_pending_transactions (dispnr(0))
    Reset_disp (dispnr(0))
    state_timer.Enabled = True
    timeout_timer.Enabled = True
    Printer_state.Enabled = True
    task_timer.Enabled = True
    
    Else
        errorlist.AddItem "Kan ikke åpne kommunikasjonsport med dispenser."
    End If
    lpgnorge.rstankinger.Open '
   
Label22.Caption = "ITU SW Ver=" & Baxi.Term_sw_version & Chr(13) & Baxi.Version

Else
MsgBox "Kan ikke åpne database.", vbCritical + vbOKOnly, "db_ok, form_load. Feil med tilkopling."
End
End If
Exit Sub
errhandler:
   errorlist.AddItem Now & ", form_load: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub Form_QueryUnload(Cancel As Integer, UnloadMode As Integer)

On Error Resume Next

rapport_rs.Close
lpgnorge.rstasks.Close

Set RS = Nothing
Set salg_RS = Nothing
Set rapport_rs = Nothing

Printer_state.Enabled = False
state_timer.Enabled = False
timeout_timer.Enabled = False
task_timer.Enabled = False

If tcpserver.State <> sckClosed Then tcpserver.Close
If status_poller.State >= 7 Then status_poller.Close

If Com_port_bank <> 0 Then Baxi.Close
If com_print.PortOpen Then com_print.PortOpen = False
If com_pinpad.PortOpen Then com_pinpad.PortOpen = False
If RFIDCOM.PortOpen Then RFIDCOM.PortOpen = False

If MSComm1.PortOpen Then MSComm1.PortOpen = False
MAPISession1.SignOff

If sqlconn.State = 1 Then
    sqlconn.Close
    Set sqlconn = Nothing
    End If
End
End Sub

Private Sub Kvitteringer_Click()
bankterminal_form.Show

End Sub

Private Sub Logs_Click()
frmlogs.Show
End Sub

Private Sub oppdaterstasjonskort_Click()

On Error Resume Next
If lpgnorge.rsstasjonskred.State <> 1 Then
    lpgnorge.rsstasjonskred.Open
Else
    lpgnorge.rsstasjonskred.Close
    lpgnorge.rsstasjonskred.Open
End If
If Err <> 0 Then
lpgnorge.butikkdata.Close
lpgnorge.butikkdata.Open
End If

If Err = 0 Then
lpgnorge.betterm.Execute "delete from stasjonskort"
Else
errorlist.AddItem Now & " Feil ved oppdatering av stasjonskort. Sjekk nettverk."

Exit Sub
End If
On Error GoTo errhandler


If lpgnorge.rsstasjonskort.State = 0 Then lpgnorge.rsstasjonskort.Open
If lpgnorge.rsstasjonskred.State = 0 Then lpgnorge.rsstasjonskred.Open
With lpgnorge.rsstasjonskred
.Filter = "kortnummer<>''"

If .State <> 1 Then .Open
    .MoveFirst
    While Not .EOF
        lpgnorge.rsstasjonskort.AddNew
        lpgnorge.rsstasjonskort!kortnummer = !kortnummer
        lpgnorge.rsstasjonskort!kundeid = !kontonr
        lpgnorge.rsstasjonskort!kortholderid = !id
        lpgnorge.rsstasjonskort!lastupdated = Now
        lpgnorge.rsstasjonskort!aktiv = 1

        lpgnorge.rsstasjonskort.Update
        DoEvents
        .MoveNext
    Wend
    errorlist.AddItem Now & " Oppdatering av bufrede stasjonskort utført.Antall records:" & lpgnorge.rsstasjonskort.RecordCount
lpgnorge.rsstasjonskort.Close
.Filter = ""

.Close

End With

Exit Sub

errhandler:
errorlist.AddItem "Oppdaterstasjonskort " & Now & " " & Err.Number & " " & Err.Description
If lpgnorge.rsstasjonskort.State = 1 Then lpgnorge.rsstasjonskort.Close
If lpgnorge.rsstasjonskred.State = 1 Then lpgnorge.rsstasjonskred.Close

End Sub

Private Sub Printer_state_Timer()
On Error GoTo errhandler
If Not Baxi.Active = 1 Then
    StatusBar1.Panels.Item(4).Text = "Betalingsterminal ikke aktiv." & Baxi.LastError
    
    restart_baxi
Else
    StatusBar1.Panels.Item(4).Text = "Betalingsterminal OK."
    
End If

    prn_paperenq = True
    com_prn_out Chr(27) & Chr(5) & Chr(2)       'Papir nær slutten
    prn_statusenq = True
    com_prn_out Chr(27) & Chr(5) & Chr(1)       'Status
   
    printer_online = True
    
   StatusBar1.Panels(5).Text = "Status klient:" & gettcpstate
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", printer_stat_timer: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub printkvitt_Click()
If com_print.PortOpen Then com_print.Output = RichTextBox1.Text & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(40)


End Sub

Private Sub Programinnstillinger_Click()
serverinnstillinger.Show (1)

End Sub

Private Sub restartterminal_Click()
Dim result As Double

On Error GoTo errhandler

If PaymentType = 0 And bank_inprogress = False Then
Check1.Enabled = False
Check3.Enabled = False
chkdagmodus.Enabled = False
result = Shell("shutdown -f -r", vbNormalNoFocus)
Else
tcpsend "<RESTART>;RESTART IKKE MULIG PGA TERMINAL OPPTATT;<SLUTT>"
End If

Exit Sub
errhandler:
errorlist.AddItem Now & " Restartterminal_click()" & " " & Err.Number & " " & Err.Description

End Sub

Private Sub RFIDCOM_OnComm()
Dim rfid_string As String
On Error GoTo errhandler

Select Case RFIDCOM.CommEvent

Case comEvReceive

   
  rfid_string = RFIDCOM.Input

If Check3.Value = 1 And Not checkkreditt And PaymentType <= 1 Then
  checkkreditt = True
  rfid_string = Left(rfid_string, 14)
  List1.AddItem Now & " " & rfid_string
  rfidtext.Text = rfid_string
  checkstatcredit rfid_string
Else
'tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt ikke aktiv;<SLUTT>"
End If

Case comEvSend

End Select
Exit Sub

errhandler:
errorlist.AddItem Now & ", RFIDCOM_oncomm :" & Err.Number & " " & Err.Description
checkkreditt = False


End Sub
 Private Sub checkstatcredit(rfidstr As String)
 On Error GoTo errhandler
checkkreditt = True
Select Case POSsystem

Case 1
10000     If lpgnorge.rsstasjonskort.State <> 1 Then lpgnorge.rsstasjonskort.Open Else lpgnorge.rsstasjonskort.Requery
10006     lpgnorge.rsstasjonskort.Filter = "kortnummer='" & rfidstr & "'"
10009     If lpgnorge.rsstasjonskort.RecordCount = 1 Then
10011        stationcredit_custno = lpgnorge.rsstasjonskort!kundeid
10013        stationcredit_contactid = lpgnorge.rsstasjonskort!kortholderid
10008        Label12.Caption = "CC:" & CStr(lpgnorge.rsstasjonskort.RecordCount)
             LogEvent "Stationcredit OK:" & rfidstr & " CC:" & CStr(stationcredit_custno) & ":" & CStr(stationcredit_contactid), "stationcredit", 0, 0, "UNI"
10015        retries = 0
10017        Do Until DispUnblock Or retries > 30
10019           DoEvents
10021           disp_unblock
10023           retries = retries + 1
             Loop
10025        If DispUnblock And retries <= 30 Then
10027           PaymentType = 3
10028           LogEvent "Unblock_stcred OK", "", 0, "", "DISPENSER"

10029           statcred_start = Now()
10033           tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt;<SLUTT>"
10035           Open "c:\pumpestyring\stasjonskreditt\tank" & Day(Now) & Month(Now) & Year(Now) & Hour(Now) & Minute(Now) & ".txt" For Output As #1
10037           Print #1, rfidstr & " " & stationcredit_custno & " " & stationcredit_contactid & " " & PaymentType & " " & DISP_openfordelivery & " " & retries
10039           Close #1
             Else
10040           errorlist.AddItem "RFIDCOM, får ikke frigjort dispenser."
10041          LogEvent "Unblock_stcred FAIL", "", 0, "Retries" & retries, "DISPENSER"

10042           checkkreditt = False
             End If

           Else
10046      tcpsend "<TANK_TERMINAL_MESSAGE>;" & rfidstr & ";<SLUTT>"
10048      PaymentType = 0
10050      checkkreditt = False
           End If
10052      lpgnorge.rsstasjonskort.Filter = ""
10044      retries = 0

Case 0
If lpgnorge.rskortholder.State <> 1 Then lpgnorge.rskortholder.Open Else lpgnorge.rskortholder.Requery
If lpgnorge.rskunder.State <> 1 Then lpgnorge.rskunder.Open Else lpgnorge.rskunder.Requery
lpgnorge.rskortholder.Filter = "kortnummer='" & Trim(rfidstr) & "' and Aktiv=1"
Label12.Caption = lpgnorge.rskortholder.RecordCount
If lpgnorge.rskortholder.RecordCount = 1 Then
    lpgnorge.rskunder.Filter = "kundeid=" & lpgnorge.rskortholder!kundeid & " and Aktiv=1"
    If lpgnorge.rskunder.RecordCount <> 1 Then
       checkkreditt = False
       PaymentType = 0
       lpgnorge.rskortholder.Filter = ""
       lpgnorge.rskunder.Filter = ""
       Exit Sub
     End If
    stationcredit_custno = lpgnorge.rskunder!kundeid
    stationcredit_contactid = lpgnorge.rskortholder!kortholderid
    retries = 0
    LogEvent "Stationcredit OK:" & rfidstr & " CC:" & CStr(stationcredit_custno) & ":" & CStr(stationcredit_contactid), "stationcredit", 0, 0, "UNI"
    Do Until DispUnblock Or retries > 30
        DoEvents
        disp_unblock
        retries = retries + 1
    Loop
    If DispUnblock And retries <= 30 Then
        PaymentType = 3
        LogEvent "Unblock_stcred OK", "", 0, "", "DISPENSER"
        statcred_start = Now()
        tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt;<SLUTT>"
        Open "c:\pumpestyring\stasjonskreditt\tank" & Day(Now) & Month(Now) & Year(Now) & Hour(Now) & Minute(Now) & ".txt" For Output As #1
        Print #1, rfidstr & " " & stationcredit_custno & " " & stationcredit_contactid & " " & PaymentType & " " & DISP_openfordelivery & " " & retries
        Close #1
    Else
        errorlist.AddItem "RFIDCOM, får ikke frigjort dispenser."
        LogEvent "Unblock_stcred FAIL", "", 0, "Retries" & retries, "DISPENSER"
        checkkreditt = False
    End If

Else
    If Len(rfidstr) = 14 Then tcpsend "<TANK_TERMINAL_MESSAGE>;" & rfidstr & ";<SLUTT>"
    PaymentType = 0
    checkkreditt = False
End If
lpgnorge.rskunder.Filter = ""
lpgnorge.rskortholder.Filter = ""
End Select

Exit Sub
errhandler:
errorlist.AddItem Now & " , Checkstasjonskreditt :" & Erl & " " & Err.Number & " " & Err.Description
checkkreditt = False

'Resume Next
End Sub

Private Sub sokkortnummer_Click()
Stasjonskredittkort.Show

End Sub

Private Sub state_timer_Timer()

On Error GoTo errhandler
Select Case PaymentType

Case 1
tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"
Case 2
tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";1;" & bank_sum & ";<SLUTT>"
Case 3
tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"

Case Else

End Select

Ehltank.BackColor = vbRed
EHLstate.BackColor = vbRed

EHLerror.BackColor = vbRed
EHLvol.BackColor = vbRed
EHLtank_cont.BackColor = vbRed
EHLstate_cont.BackColor = vbRed
EHLvolume_cont.BackColor = vbRed
EHLtank_cont.BackColor = vbRed
EHLerror_cont.BackColor = vbRed

tank_ok = False
error_ok = False
volume_ok = False
state_ok = False
state_ok_cont = False
tank_ok_cont = False
volume_ok_cont = False
error_ok_cont = False

If Not MSComm1.PortOpen Then Exit Sub

cmd_retries = 0
While Not state_ok And cmd_retries < 30
EHLstate.Caption = cmd_retries & " State"
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H4B     'status
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
DoEvents
cmd_retries = cmd_retries + 1
Wend
If state_ok Then EHLstate.BackColor = vbGreen

cmd_retries = 0
While Not error_ok And cmd_retries < 30
EHLerror.Caption = cmd_retries & " Err"
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H4C     'error
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
DoEvents
cmd_retries = cmd_retries + 1
Wend
If error_ok Then EHLerror.BackColor = vbGreen

cmd_retries = 0
While Not volume_ok And cmd_retries < 30
EHLvol.Caption = cmd_retries & " Vol"
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &H45     'volume
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
DoEvents
cmd_retries = cmd_retries + 1
If volume_ok Then EHLvol.BackColor = vbGreen
Wend

cmd_retries = 0
While Not tank_ok And cmd_retries < 30
Ehltank.Caption = cmd_retries & " Tank"
y(1) = &H10
y(2) = &H6
y(3) = dispnr(0)
y(4) = &HC5         'tankbit
y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
y(6) = &H36
comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
DoEvents
cmd_retries = cmd_retries + 1

Wend
If Disp_was_unblocked And Not DISP_openfordelivery And Not trans_unaccounted Then
    Disp_was_unblocked = False
    errorlist.AddItem Now & ":Unblock recovery due to startbutton failure."
    LogEvent "Unblock recovery underway", "", 0, "", "EHL"
    errorlist.AddItem Now & ":Unblock recovery underway."
    retries = 0
    DispUnblock = False
    Do Until DispUnblock Or retries >= 30 '_openfordelivery Or retries >= 30
       retries = retries + 1
        
       disp_unblock
       DoEvents
   Loop
   If DispUnblock And retries <= 30 Then
        PaymentType = 2
        LogEvent "Unblock recovery OK", "", 0, "", "EHL"
         errorlist.AddItem Now & ":Unblock recovery OK."
    Else
        LogEvent "Unblock recovery fail,initiated reversal.", "", 0, "", "EHL"
         errorlist.AddItem Now & ":Unblock recovery fail,reversal initiated."
        Baxi_Reversal
    End If
End If

If dispcontainernr > 0 Then     'Hvis dispenser er koblet til container, må vi sjekke her og...
    cmd_retries = 0
    While Not state_ok_cont And cmd_retries < 30
    EHLstate_cont.Caption = cmd_retries & " State"
    y(1) = &H10
    y(2) = &H6
    y(3) = dispcontainernr + 32
    y(4) = &H4B
    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
    y(6) = &H36
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
    DoEvents
    cmd_retries = cmd_retries + 1
    Wend
   cmd_retries = 0
   
   While Not error_ok_cont And cmd_retries < 30
    EHLerror_cont.Caption = cmd_retries & " Err"
    y(1) = &H10
    y(2) = &H6
    y(3) = dispcontainernr + 32
    y(4) = &H4C
    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
    y(6) = &H36
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
    DoEvents
    cmd_retries = cmd_retries + 1
    Wend
    cmd_retries = 0
    If CONT_openfordelivery Or CONT_tank_vol >= CONT_tank_vol_last And CONT_tank_vol <> 0 Then
        While Not volume_ok_cont And cmd_retries < 30
        EHLvolume_cont.Caption = cmd_retries & " Vol"
        y(1) = &H10
        y(2) = &H6
        y(3) = dispcontainernr + 32
        y(4) = &H45
        y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
        y(6) = &H36
        comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
        DoEvents
        cmd_retries = cmd_retries + 1
        Wend
    cmd_retries = 0
    While Not tank_ok_cont And cmd_retries < 30
    EHLtank_cont.Caption = cmd_retries & " Tank"
    y(1) = &H10
    y(2) = &H6
    y(3) = dispcontainernr + 32
    y(4) = &HC5
    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
    y(6) = &H36
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
    DoEvents
    cmd_retries = cmd_retries + 1
    Wend
    End If
   

End If

Exit Sub
errhandler:
StatusBar1.Panels(5).Text = Err.Number & " " & Err.Description

End Sub

Public Function cmdcom_on()

On Error GoTo errhandler

MSComm1.CommPort = Com_port
   
u = -1


If MSComm1.PortOpen = False Then MSComm1.PortOpen = True '
cmdcom_on = True
Exit Function

errhandler:
StatusBar1.Panels(1).Text = "Feil ved kommunikasjon dispenser:" & Err.Number & " " & Err.Description
cmdcom_on = False
End Function

Private Sub MSComm1_OnComm()

On Error GoTo errhandler

Select Case MSComm1.CommEvent

   
   Case comEvReceive   ' Received RThreshold # of
    

110    rts = False
120    If u > 15 Then
130        u = -1
140       commandtext_in = ""
150    End If
160    u = u + 1
170    charstr = MSComm1.Input
180    x(u) = Asc(charstr)
190    commandtext_in = commandtext_in & x(u) & ";"
'Debug.Print commandtext_in
200    If x(u) = 54 And x(0) = 32 And x(1) = (u + 1) Then 'END og Begin har kommet
210        chksum = 0
220        For i = 0 To u - 2
230            chksum = chksum Xor x(i)        'Vi kalkulerer CRC på mottatt string
240        Next
250        If chksum = x(u - 1) Then
260            COM_id = COM_id + 1
270            commandtext_in = ""
280            u = -1
           Select Case x(2)         'Container eller dispenser
            Case dispnr(0)          'Dispenser

290            Select Case x(3)
                 
                   Case 69
                   volume_ok = True
                   
620                tank_vol = CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
                   
630                If ((tank_vol_last = tank_vol) And (trans_unaccounted = True Or Check2.Value = 1)) Then
                        tank_end = True
                        Disp_was_unblocked = False
                        new_tank = False
670                     lpgnorge.rstankinger!Status = 3
680                     lpgnorge.rstankinger!datostopp = Now()
690                     lpgnorge.rstankinger!liter = tank_vol
700                     lpgnorge.rstankinger!Sum = tank_sum
780                     tcpsend "<TANK_STOP>;<SLUTT>"

                        
                        Select Case PaymentType
                        
                            Case 0  'default application start,
                        
                            Case 1  'kontant
                                'tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";0;0;<SLUTT>"
                                
                                Open "c:\deltefiler\tankinger\pump1" & Day(Now) & Month(Now) & Year(Now) & ".txt" For Output As #1
                                Print #1, Replace(tank_vol, ".", ",") & ";" & Replace(tank_unitprice, ".", ",");
                                Close #1
                                PaymentType = 0
                                
                            Case 2  'bank

                                'tcpsend "<TANK>;" & belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption & ";1;" & bank_sum & ";<SLUTT>"
800                             rapport_rs.AddNew
805                             LogEvent "TANK_finished", belop.Caption & ";" & antall_liter.Caption & ";" & dispris.Caption, rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
810                             rapport_rs!dato = Now()
820                             rapport_rs!Type = "Tankkvittering"
830                             reporttext = Print_reciept_header & Chr(9) & "Dato:" & Now() & Chr(10) _
                                & Chr(9) & "LPG Autogas ant.Liter  :" & Chr(9) & tank_vol & Chr(10) _
                                & Chr(9) & "Kr/L inkl MVA:" & Chr(9) & tank_unitprice & Chr(10) _
                                & Chr(9) & "Sum KR:" & Chr(9) & FormatNumber(tank_sum, 2) & Chr(10) _
                                & Chr(9) & "Herav MVA:" & Chr(9) & FormatNumber(tank_sum - (tank_sum / 1.25), 2) & Chr(10) _
                                & Chr(9) & "Innbetalt KR" & Chr(9) & FormatNumber(bank_sum, 2) & Chr(10) _
                                & Chr(9) & "Tilgode KR:" & Chr(9) & FormatNumber((bank_sum - tank_sum), 2) & Chr(10) & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "SETT INN KORTET, EVT TILGODEBELØP" & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "TILBAKEFØRES DITT KORT." & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "DET KAN TA TO VIRKEDAGER FØR " & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "BELØPENE ER SYNLIGE PÅ DIN KONTO." & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
840                             If com_print.PortOpen Then com_print.Output = reporttext
850                             bank_tank = reporttext
                                reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
                                reporttext = Replace(reporttext, Chr(9), "", 1)
                                reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
                                rapport_rs!reporttext = reporttext
                                If ((tank_sum + 0.24) < bank_sum) Then
880
885                                 lpgnorge.rstankinger!Status = 4
890                                 lpgnorge.rstankinger!tilbakesum = (bank_sum - tank_sum)
892
                                    
                                    
900                                 If tank_sum < 0.5 Then
                                        LogEvent "Annul initiated", CStr((bank_sum - tank_sum)), rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
                                        Baxi_Reversal

                                    Else
                                        LogEvent "Cashback initiated", CStr((bank_sum - tank_sum)), rapport_rs!reportid, "Bankterminal_rapporter", "Dispenser"
                                        tank_sum2 = tank_sum
                                        bank_sum2 = bank_sum
                                        cashback ((bank_sum - tank_sum))
910                                 End If

                                Else
920                             return_amount = False
                                bank_sum = 0
                                PaymentType = 0
940                             Bank_answer = False
                                return_amount = False
                                End If
1010                            cmdbank_100.BackColor = &H8000000F
                                cmdbank_200.BackColor = &H8000000F
                                cmdbank_400.BackColor = &H8000000F
                                cmdbank_600.BackColor = &H8000000F




                            Case 3  'stasjonskort
                                Open "c:\pumpestyring\stasjonskreditt\tank" & Day(Now) & Month(Now) & Year(Now) & ".txt" For Output As #1
60000                           Print #1, stationcredit_custno & " " & stationcredit_contactid & " " & Replace(tank_vol, ".", ",") & ";" & Replace(tank_unitprice, ".", ",");
                                Close #1
12000                           rapport_rs.AddNew
12001                           rapport_rs!dato = Now()
12002                           rapport_rs!Type = "Stasjonskreditt"
12003                           reporttext = Print_reciept_header & Chr(9) & "Dato:" & Now() & Chr(10) _
                                & Chr(9) & "LPG Autogas ant.Liter  :" & Chr(9) & FormatNumber(tank_vol, 2) & Chr(10) _
                                & Chr(9) & "Kr/L inkl MVA:" & Chr(9) & FormatNumber(tank_unitprice, 2) & Chr(10) _
                                & Chr(9) & "Sum KR:" & Chr(9) & FormatNumber(tank_sum, 2) & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "Beløpet er belastet ditt kundeforhold knr:" & stationcredit_custno & Chr(10) _
                                & Chr(27) + Chr(78) + Chr(1) & "Velkommen tilbake." & Chr(10) + Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset))
12005                                If com_print.PortOpen Then com_print.Output = reporttext
12006                                reporttext = Replace(reporttext, Chr(27) + Chr(78) + Chr(1), "", 1)
12007                                reporttext = Replace(reporttext, Chr(9), "", 1)
12008                                reporttext = Replace(reporttext, Chr(27) + Chr(30) + Chr(27) + Chr(12) + Chr(CInt(feed_offset)), "", 1)
12009                                rapport_rs!reporttext = reporttext
12010                                rapport_rs.Update
                                Select Case POSsystem
                                
                                Case 1
                                     With lpgnorge.rsstasjonskreditt
                                     .Open
                                     .AddNew
10014                                !datostart = statcred_start
                                     !rabatt = statcred_rabatt
10016                                !unikundeid = stationcredit_custno
                                     !unikontaktid = stationcredit_contactid
12011                                !transferred = False
12012                                !liter = tank_vol
12013                                !pris = tank_unitprice
12014                                !Sum = tank_sum
12015                                !Status = 4
12016                                !datostopp = Now()
12017                                .Update
                                     .Close
                                     End With
                                     checkkreditt = False
                                
                                
                                Case 0
12019                                 With lpgnorge.rsstasjonskreditt
                                        If .State <> 1 Then .Open
                                        .AddNew
                                        !datostart = statcred_start
                                        !unikundeid = stationcredit_custno
                                        !kundeid = stationcredit_custno
                                        !unikontaktid = stationcredit_contactid
                                        !rabatt = lpgnorge.rskunder!rabatt
12020                                   !transferred = False
12021                                   !liter = tank_vol
12022                                   !pris = tank_unitprice
12023                                   !Sum = tank_sum
12024                                   !Status = 4
12025                                   !datostopp = Now()
12026                                   .Update
                                        .Close
                                     End With
12027                                checkkreditt = False
                                
12028
                                End Select
                                 PaymentType = 0
                            Case 4  'manuell banktrans
                                PaymentType = 0
                        
                            Case 5  'virtual bankkort
                                PaymentType = 0
                        
                            Case 6  'virtual stasjonskort
                                PaymentType = 0
                        
                            End Select
                        
980                     tank_vol = 0
1160                    rapport_rs.Update
1165                    lpgnorge.rstankinger.Update
                        DispUnblock = False
                        SetAmount = False
                        PaymentType = 0
                        tank_end = True
                        new_tank = False
                        checkkreditt = False
                        retries = 0
                        rts = True
1050                    Reset_disp (dispnr(0))
1120                    rts = False
                    
                    Else
                         
                         
1170                     tank_vol_last = tank_vol
1180                     tank_unitprice = dispprice(0)
1190                     tank_sum = tank_vol * tank_unitprice
1200                     If tank_vol > 0 Then antall_liter.Caption = Format(tank_vol, "0.00")
1210                     If tank_vol > 0 Then belop.Caption = Format(tank_sum, "0.00")

                    End If
                    beloptext = belop.Caption


                    Case 75
1330                    state_ok = True
1340                        state_string = decimaltobinn(x(4))
1350                        Label11.Caption = state_string
                    
1360                        If state_string <> state_string_old Then
                            LogEvent "STATE", state_string, 0, "", "EHL"
                                If Not checkkreditt Then
                                    tcpsend "<STATE>;" & state_string & ";<SLUTT>"
                                Else
                                    tcpsend "<STATE>;" & Left(state_string, 5) & "0" & Right(state_string, 2)
                                End If
                            End If
                            
1420                        If Mid(state_string, 6, 1) = "1" Then
                                DISP_startbuttonpressed = True
                                EHLstartbutton.BackColor = vbGreen
                            Else
                                DISP_startbuttonpressed = False
                                EHLstartbutton.BackColor = vbRed
                            End If
1430                        If Mid(state_string, 7, 1) = "1" Then
                                DISP_openfordelivery = True
                                If PaymentType = 2 Then Disp_was_unblocked = True
                                EHLunblock.BackColor = vbGreen
                                
                            Else
                                DISP_openfordelivery = False
                                EHLunblock.BackColor = vbRed
                                
                            End If
1440                        If Mid(state_string, 5, 1) = "1" Then disp_automode = True Else disp_automode = False
                            If Not new_tank And DISP_startbuttonpressed And DISP_openfordelivery Then
1470                                new_tank = True
1480                                tank_end = False
660                                 tank_vol = 0
2100                                tank_vol_last = 0
2110                                tank_unitprice = 0
2120                                tank_sum = 0
                                    lpgnorge.rstankinger.AddNew
                                    lpgnorge.rstankinger!datostart = Now()
2040                                tanknr = lpgnorge.rstankinger!tankid
                                    Select Case PaymentType
                                
                                        Case 0
                                    
                                        Case 1
                                            lpgnorge.rstankinger!betalingstype = 1
                                        Case 2
                                            lpgnorge.rstankinger!betalingstype = 2
                                            lpgnorge.rstankinger!presalg = bank_sum
                                        Case 3
                                            lpgnorge.rstankinger!betalingstype = 3
                                          
                                        Case 4
                                            lpgnorge.rstankinger!betalingstype = 4
                                        Case 5
                                            lpgnorge.rstankinger!betalingstype = 5
                                        Case 6
                                            lpgnorge.rstankinger!betalingstype = 6
                                    End Select
                            End If
                        DISP_errorstatestring = Mid(state_string, 2, 2)
                        state_string_old = state_string

                        Case 76
                            error_ok = True
                    
                            If Chr(x(4)) & "-" & Chr(x(5)) <> errorcode_autogass.Caption Then
                                 errorcode_autogass.Caption = Chr(x(4)) & "-" & Chr(x(5))
                                 logdisp_err "Autogass:", Val(Chr(x(4))), Val(Chr(x(5)))
                            End If
                       
2400                    Case 92        '(PRICE) Give / take the fuel price

                         dispris.Caption = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
                       
                       
                       Case 117
                        
                        'setamount_ok = True
                        If x(4) = 30 Then
                        SetAmount = True
                        LogEvent "setamount:true", "retries:" & CStr(retries), 0, 0, "EHL"
                        
                        EHLsetamount.BackColor = vbGreen
                        
                        Else
                        SetAmount = False
                        LogEvent "setamount:false", "retries:" & CStr(retries), 0, 0, "EHL"
                        
                        EHLsetamount.BackColor = vbRed
                        End If
                        
                        Case 119
                        dispunblock_ok = True
                        
                        If x(4) = 30 Then
                        DispUnblock = True
                        tanktimeout_count = 0
                        
                        
                        Else
                        DispUnblock = False ' = False
                        'Command_disp = "amount"
                        
                        End If
                                                      
                        Case 129
                        If x(4) = 30 Then
                        EHLunaccounted.BackColor = vbRed
                        EHLsetamount.BackColor = vbRed
                        
                        Else
                        
                        End If
                        
2520                    Case 133           '(SUM) Give / take total sum of delivered fuel and number of transactions from switching on the supply
                    
2530                    Case 197            '(Tank)
                        tank_ok = True
                        Ehltank.BackColor = vbGreen
2550                        state_string_Tank = decimaltobinn_tank(x(4))
2560                        If state_string_Tank <> state_string_tank_old Then
                                tcpsend "<STATE_TANK>;" & state_string_Tank & ";<SLUTT>"
                                LogEvent "STATE_tank", state_string_Tank, 0, "", "EHL"
                                 
                            End If
2570                        If CInt(Mid(state_string_Tank, 8, 1)) = 1 Then
                                trans_finished_powerfault = True
                                rapport_rs.MoveLast
                                Select Case rapport_rs!Type
                                    Case "Forhåndsvalg"
                                        errorlist.AddItem Now & ":Forhåndsvalg må tilbakeføres etter strømbrudd.reportid=" & rapport_rs!reportid
                                        LogEvent "Pwr_fault", "Annulering etter pwr", 0, "Bankterminal_kvitteringer", "Dispenser"
                                        Baxi_Reversal
                                        
                                    Case "Tankkvittering"
                                        errorlist.AddItem Now & ":Strømbrudd under tanking, retur av restbeløp må gjennomføres. reportid:" & rapport_rs!reportid
                                        LogEvent "Pwr_fault", "Tilbakeføring etter pwr må utføres", 0, "Bankterminal_kvitteringer", "Dispenser"
                                    
                                    Case "Tilbakeføring"
                                        errorlist.AddItem Now & ":Strømbrudd etter tilbakeføring, ingen handling nødvendig."
                                        LogEvent "Pwr_fault", "Tilbakeføring etter pwr ingen reaksjon nødvendig", 0, "Bankterminal_kvitteringer", "Dispenser"
                                    
                                    Case "Terminal"
                                        errorlist.AddItem Now & ":Sjekk siste transaksjon, reportid:" & rapport_rs!reportid
                                        LogEvent "Pwr_fault", "Sjekk trans", 0, "Bankterminal_kvitteringer", "Dispenser"
                                    
                                End Select
                                
                            
                            Else
                                trans_finished_powerfault = False
                            End If
2580                        If CInt(Mid(state_string_Tank, 5, 1)) = 1 Then
                            trans_unaccounted = True
                            EHLsetamount.BackColor = vbGreen
                            'rts = True
                            'Reset_disp (dispnr(0))
                            'rts = False
                    
                            Else
                            trans_unaccounted = False

                            End If
2590                        state_string_tank_old = state_string_Tank
                            Label17.Caption = state_string_Tank

                End Select   'Select..Case..end hvilken kommando er det
                
                Case dispcontainernr + 32       'Container
            
                    Select Case x(3)
                        
                        Case 69
                        volume_ok_cont = True
                        EHLvolume_cont.BackColor = vbGreen
                        
                        CONT_tank_vol = CSng(Chr(x(8)) & Chr(x(7)) & Chr(x(6)) & "," & Chr(x(5)) & Chr(x(4)))
                   
                        If ((CONT_tank_vol_last = CONT_tank_vol) And CONT_trans_unaccounted) Then

                            CONT_tank_end = True
                            CONT_new_tank = False
                            If lpgnorge.rsfyllemaskin.State <> 1 Then lpgnorge.rsfyllemaskin.Open
                            
                            lpgnorge.rsfyllemaskin!Status = 3
                            lpgnorge.rsfyllemaskin!datostopp = Now()
                            lpgnorge.rsfyllemaskin!liter = CONT_tank_vol
                            CONT_tank_vol = 0
                            lpgnorge.rsfyllemaskin.Update
                            rts = True
                            Reset_disp dispcontainernr + 32
                            rts = False
                    
                        Else            'Flaskefylling pågår
                            CONT_tank_vol_last = CONT_tank_vol
                        End If
                        
                        Case 75
                           state_ok_cont = True
                           EHLstate_cont.BackColor = vbGreen
                           CONT_state_string = decimaltobinn(x(4))
                            Label10.Caption = CONT_state_string
                    
                            If CONT_state_string <> CONT_state_string_old Then

                              If Mid(CONT_state_string, 6, 1) = "1" Then
                                CONT_startbuttonpressed = True
                                EHLstartbutton_cont.BackColor = vbGreen
                              Else
                                CONT_startbuttonpressed = False
                                EHLstartbutton_cont.BackColor = vbRed
                                
                                
                              End If
                              If Mid(CONT_state_string, 7, 1) = "1" Then
                                CONT_openfordelivery = True
                                EHLunblock_cont.BackColor = vbGreen
                              Else
                                CONT_openfordelivery = False
                                EHLunblock_cont.BackColor = vbRed
                              End If
                              If Mid(CONT_state_string, 5, 1) = "1" Then CONT_automode = True Else CONT_automode = False
                              If Not CONT_new_tank And CONT_startbuttonpressed Then
                                    rts = True
                                    y(1) = &H10
                                    y(2) = &H6
                                    y(3) = dispcontainernr + 32
                                    y(4) = &H77
                                    y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                    y(6) = &H36
                                    CONT_new_tank = True
                                    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                    container_tank.AddItem Now() & "--> Container frigitt."
                                    CONT_new_tank = True
                                    CONT_tank_end = False
                                    CONT_tank_vol = 0
                                    CONT_tank_vol_last = 0
                                    If Not lpgnorge.rsfyllemaskin.State = 1 Then lpgnorge.rsfyllemaskin.Open
                                    
                                    lpgnorge.rsfyllemaskin.AddNew
                                    lpgnorge.rsfyllemaskin!datostart = Now()
                            End If
                            CONT_errorstatestring = Mid(CONT_state_string, 2, 2)
                            CONT_state_string_old = CONT_state_string
                           End If
                        Case 76
                        error_ok_cont = True
                        EHLerror_cont.BackColor = vbGreen
                        If Chr(x(4)) & "-" & Chr(x(5)) <> errorcode_container.Caption Then
                            errorcode_container.Caption = Chr(x(4)) & "-" & Chr(x(5))
                            logdisp_err "Container", Val(Chr(x(4))), Val(Chr(x(5)))
                        End If
                        
                        Case 92
                        
                        Case 106
                        
                        Case 133
                        
                        Case 197
                        tank_ok_cont = True
                        EHLtank_cont.BackColor = vbGreen
                        
                        CONT_state_string_Tank = decimaltobinn_tank(x(4))
                        If CInt(Mid(CONT_state_string_Tank, 8, 1)) = 1 Then CONT_trans_finished_powerfault = True Else CONT_trans_finished_powerfault = False
                        If CInt(Mid(CONT_state_string_Tank, 5, 1)) = 1 Then CONT_trans_unaccounted = True Else CONT_trans_unaccounted = False
                        CONT_state_string_tank_old = CONT_state_string_Tank
                        Label16.Caption = CONT_state_string
                        
                    End Select
                End Select
                
            End If
    
2640    For u = 0 To 15     'vi nullstiller x() etter godkjent kommando
2650        x(u) = 0
2660    Next
2670    u = -1
2680    commandtext_in = ""
2690    Else
2700    End If      'Har END kommet?
 
2710    End Select      'Select..END Comm_event
2720    rts = True
2730    Exit Sub

errhandler:
errorlist.AddItem Now & ", mscomm1_oncomm: Linje :" & Erl & " " & Err.Number & " " & Err.Description & " " & Err.Source
Resume Next
End Sub


Private Sub status_poller_ConnectionRequest(ByVal requestID As Long)
'errorlist.AddItem Now & " conn from:" & requestID

End Sub

Private Sub status_poller_DataArrival(ByVal bytesTotal As Long)

On Error Resume Next

Dim recstr As String

status_poller.GetData recstr
If recstr = "<GETAUTOGASPRICE>" Then
If status_poller.State = 7 Then status_poller.SendData "<02;" & lpgnorge.rsdispensere!pris & ">"

End If
End Sub

Private Sub status_poller_Error(ByVal Number As Integer, Description As String, ByVal Scode As Long, ByVal Source As String, ByVal HelpFile As String, ByVal HelpContext As Long, CancelDisplay As Boolean)
CancelDisplay = True
errorlist.AddItem Now & "status_poller: " & Number & " " & Description
status_poller.Close

End Sub



Private Sub swupdate_Click()
Baxi.Administration &H313E

End Sub

Private Sub tankinger_Click()
Tankinger_form.Show

End Sub

Private Sub task_timer_Timer()

On Error GoTo last

'Check_DBconn

Dim bank_retries As Date
task_timer.Enabled = False
lpgnorge.rstasks.Requery
If lpgnorge.rstasks!unilink = True And POSsystem = 1 Then
    oppdaterstasjonskort_Click
    lpgnorge.rstasks!unilink = False
    lpgnorge.rstasks.Update
End If

If lpgnorge.rstasks!zrapport = True Then
    admmode = True
    bank_inprogress = True
    Baxi.Administration &H3137, 0
    rapporttype = "Zrapport"
    lpgnorge.rstasks!zrapport = False
    lpgnorge.rstasks.Update
End If
bank_retries = Now()
Do Until Baxi.LocalMode = 1 Or Now() >= DateAdd("n", 5, bank_retries)
    If Now() >= DateAdd("n", 5, bank_retries) Then GoTo last
    DoEvents
    Loop
If lpgnorge.rstasks!avstemming = True Then
    admmode = True
    bank_inprogress = True
    Baxi.Administration &H3130, 0
    rapporttype = "Avstemming"
    lpgnorge.rstasks!avstemming = False
    lpgnorge.rstasks.Update
End If
    

If POSsystem = 1 Then Check_and_import_order

Select Case status_poller.State

Case 0
statetext = "Ikke Tilkoblet til vert."
Case 1
statetext = "Kommunikasjonskanal åpen."
Case 2
statetext = "Lytter."
Case 3
statetext = "Tilkobling venter."
Case 4
statetext = "Prøver og finne vert."

Case 5
statetext = "Vert funnet."

Case 6
statetext = "Venter på tilkobling."


Case 7
statetext = "Tilkoblet til poller vert."
'Check3.Value = 1
Case 8
statetext = "Nedkobler."

Case 9
statetext = "Feil ved tilkobling."
End Select

StatusBar2.Panels(2).Text = statetext


If status_poller.State = 0 Or status_poller.State > 7 Then
    'Check3.Value = 0
    status_poller.Close
    status_poller.LocalPort = 86
    status_poller.Listen
End If

task_timer.Enabled = True
Exit Sub
last:
If Err <> 0 Then Pumpekontroll.errorlist.AddItem Now & ", Tasktimer: " & Erl & " " & Err.Number & " " & Err.Description  ' Err.Clear          'Her trenger vi logging
task_timer.Enabled = True

End Sub

Private Sub Check_and_import_order()
On Error GoTo errhandler
Dim ordrestring As String, ordrestring1 As String
Dim ordrelinjestr As String, ordrelinjestr1 As String
Dim omva As Single, omvagr As Single, onettosum As Single, ototalsum As Single, ogrlhoy As Single, omomshoy As Single
Dim onr As Long
Dim rsordrelinjeid As Long
'Exit Sub
If lpgnorge.rsstasjonskreditt.State = 0 Then lpgnorge.rsstasjonskreditt.Open

lpgnorge.rsstasjonskreditt.Filter = "transferred=0"
While Not lpgnorge.rsstasjonskreditt.EOF
       If IsNull(lpgnorge.rsstasjonskreditt!liter) Or lpgnorge.rsstasjonskreditt!liter <= 0 Then GoTo next_stationcredit_rec
40010       omva = lpgnorge.rsstasjonskreditt!Sum - (lpgnorge.rsstasjonskreditt!Sum / (MVA / 20))
40020       omvagr = lpgnorge.rsstasjonskreditt!Sum / (MVA / 20)
40030       onettosum = lpgnorge.rsstasjonskreditt!Sum / (MVA / 20)
40040       ogrlhoy = omvagr
40050       omomshoy = omva
40060       ototalsum = lpgnorge.rsstasjonskreditt!Sum
40070       If lpgnorge.rsordre.State <> 1 Then lpgnorge.rsordre.Open
40080       lpgnorge.rsordre.Requery
40090       onr = lpgnorge.rsordre!ordrenr + 1
40100       If lpgnorge.rsNavnaddress.State <> 1 Then lpgnorge.rsNavnaddress.Open
40110       lpgnorge.rsNavnaddress.Filter = "kontonr=" & lpgnorge.rsstasjonskreditt!unikundeid
40120       If lpgnorge.rsNavnaddress.RecordCount <> 1 Then errorlist.AddItem "Import ordre, kan ikke avgjøre kundeadresse."

            With lpgnorge.rsordre
40140          .AddNew
40150          !ordrenr = onr
40160          !kundenr = lpgnorge.rsstasjonskreditt!unikundeid
40170          !kundenavn = lpgnorge.rsNavnaddress!Name
40180          !adresse = lpgnorge.rsNavnaddress!address
40190          !adresse2 = lpgnorge.rsNavnaddress!address2
40200          !postnr = lpgnorge.rsNavnaddress!postal_code
40210          !poststed = lpgnorge.rsNavnaddress!city
40220          Set RS = lpgnorge.butikkdata.Execute("Select Name from c_contacts where id=" & lpgnorge.rsstasjonskreditt!unikontaktid)
40230          If RS.RecordCount = 1 Then !deres_ref = Left(RS!Name, 20) Else errorlist.AddItem "Fant ikke kontaktpersondata"
40240          RS.Close
40260          !ordredato = CDate(Now)
40270          !leveringsdato = CDate(Now)
40280          !avgfritt_grunnlag = 0
40290          !behandlingsregel = 0
40300          !bet_maate = 0
40400          !moms = omva
40410          !momsprosent = MVA
40420          !mvagrunnlag = omvagr
40430          !nettosum = onettosum
40440          !registreringsdato = CDate(Now)
40450          !totalsum = ototalsum
40460          !Status = "110202"
40470          !grlhoy = ogrlhoy
40480          !momshoy = omomshoy
40490          !lastedit = CDate(Now)

40500
               
            End With
40510       If lpgnorge.rsordrelinje.State <> 1 Then lpgnorge.rsordrelinje.Open
40505            If lpgnorge.rsvarer.State = 0 Then lpgnorge.rsvarer.Open

40520       lpgnorge.rsordrelinje.Requery
            With lpgnorge.rsordrelinje
40530            rsordrelinjeid = lpgnorge.rsordrelinje!id
40540            .AddNew
40550            !id = rsordrelinjeid + 1
40560            !ordrenr = onr
40570            !kundenr = lpgnorge.rsstasjonskreditt!unikundeid
40580            !varenr = lpgnorge.rsdispensere!autogasvarenr
40585            lpgnorge.rsvarer.Filter = "Varenr=" & !varenr
40586            If lpgnorge.rsvarer.RecordCount = 1 Then
40587               !innpris = lpgnorge.rsvarer!innpris_selvkost
                 Else
                 errorlist.AddItem Now & "ordre:" & onr & " Import ordre, kan ikke avgjøre vare, setter innpris til 0"
40588            !innpris = 0
                 End If
                 
40590            !Varetekst = "Autogass " & lpgnorge.rsstasjonskreditt!datostart
40600            !antall = lpgnorge.rsstasjonskreditt!liter
40610            !antall_lev = !antall
40620            !enhet = "LTR"
40630            !kontonr = 3000
40640            !momskode = 0                      '1 for inkl 25% 0 for eksl 25%
40650            !pris = lpgnorge.rsstasjonskreditt!pris / (MVA / 20)
40652            lpgnorge.rsordre!dekningsbidrag = lpgnorge.rsordre!nettosum - (!innpris * !antall)
40655            !db = (lpgnorge.rsordre!dekningsbidrag * 100) / lpgnorge.rsordre!nettosum
40660            !rabatt = 0
40670            !lsum = lpgnorge.rsstasjonskreditt!Sum
40680            !MVA = lpgnorge.rsstasjonskreditt!Sum - (lpgnorge.rsstasjonskreditt!Sum / (MVA / 20))
40690            !netto = lpgnorge.rsstasjonskreditt!Sum - !MVA
40700            !Type = 0
40710            !ab_id = 0
40720            !ab_avregnid = 0
40730            !momsprosent = MVA
40740
            End With
40742       lpgnorge.rsordre.Update
40744       lpgnorge.rsordrelinje.Update
            
next_stationcredit_rec:
            DoEvents
40750       lpgnorge.rsstasjonskreditt!transferred = True
40760       lpgnorge.rsstasjonskreditt!transferdato = Now
40770       lpgnorge.rsstasjonskreditt.MoveNext
            Wend
            'lpgnorge.rstasks!unilink = False
            'lpgnorge.rstasks.Update
            lpgnorge.rsstasjonskreditt.Close
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", check and import_order: " & Erl & " " & Err.Number & " " & Err.Description
  If lpgnorge.rsstasjonskreditt.State = 1 Then lpgnorge.rsstasjonskreditt.Close
  
End Sub

Private Sub tcpserver_ConnectionRequest(ByVal requestID As Long)
On Error GoTo errhandler
If tcpserver.State <> sckClosed Then tcpserver.Close
tcpserver.Accept requestID
   Exit Sub
errhandler:
   errorlist.AddItem Now & ", tcpserver_connectionrequest: " & Err.Number & " " & Err.Description
   Resume Next
End Sub

Private Sub tcpserver_DataArrival(ByVal bytesTotal As Long)
On Error GoTo errhandler

tcpserver.GetData gettcpdatastr
tcpword = Split(gettcpdatastr, ";")

Select Case tcpword(0)

Case "<RESTART>"
If tcpword(1) = "<SLUTT>" Then
    errorlist.AddItem Now & " Klient ønsker å restarte betalingsterminal"
    If softrestart.Value = 1 Then
        restartterminal_Click
         tcpsend "<RESTART>;RESTART IVERKSETTES OM<30sek;<SLUTT>"
    Else
     tcpsend "<RESTART>;RESTART IKKE MULIG PGA ADMIN;<SLUTT>"
    End If
    
Else
End If

Case "<STASJONSKORT>"

Case "<STATIONSTATE>"
If tcpword(3) = "<SLUTT>" Then

Select Case tcpword(1)


Case 1      'Dagmodus
    Select Case tcpword(2)

    Case 0
    chkdagmodus.Value = 0

    Case 1
    chkdagmodus.Value = 1

    End Select


Case 2      'Kortbetaling
    Select Case tcpword(2)

    Case 0
        Check1.Value = 0
    Case 1
        Check1.Value = 1
    End Select

Case 3      'Stasjonskreditt
    Select Case tcpword(2)

    Case 0
        Check3.Value = 0
    
    Case 1
        Check3.Value = 1

    End Select

End Select


Else
tcpsend "<STATIONSTATE>;?;<SLUTT>"

End If

Case "<DISP_LITER>"
If tcpword(2) = "<SLUTT>" Then
'literantall = "000000"
'    literantall = literantall & tcpword(1)
'    literantall = Right(literantall, 6)
'    disp_unblock_liter literantall
'    tcpsend "<DISP_LITER>;<DISP_LITER_OK>;" & CSng(literantall) / 100
Else

End If

Case "<TANK_DISP_UNBLOCK>"
If tcpword(2) = "<SLUTT>" And Not checkkreditt Then

tcpsend "<TANK_DISP_UNBLOCK>;OK;<SLUTT>"
    If PaymentType > 1 Then
    
        tcpsend "<STATUS>;Dispenser opptatt;<SLUTT>"
    Else
        PaymentType = 1
        LogEvent "Manual unblock", "", "", "", "Client"
        
        If tcpword(1) <> "" Then disp_unblock (tcpword(1)) Else disp_unblock
    End If
Else
tcpsend "<TANK_DISP_UNBLOCK>;?;<SLUTT>"
End If


Case "<TANK_DISP_STOP>"
If tcpword(1) = "<SLUTT>" Then
  
    tcpsend "<TANK_DISP_STOP>;OK;<SLUTT>"
   disp_block
Else
    tcpsend "<TANK_DISP_STOP>;?;<SLUTT>"
End If

Case "<BANK_CASH>"

If tcpword(2) = "<SLUTT>" Then
   
    tcpsend "<BANK_CASH>;OK;<SLUTT>"
    manual_bank = True
    cash (tcpword(1))
Else
    tcpsend "<BANK_CASH>;?;<SLUTT>"
End If

Case "<BANK_CASHBACK>"

If tcpword(2) = "<SLUTT>" Then

    tcpsend "<BANK_CASHBACK>;OK;<SLUTT>"
    bank_inprogress = True
    manual_bank = True
    cashback (CSng(tcpword(1)) / 100)
Else
    tcpsend "<BANK_CASHBACK>;?;<SLUTT>"
End If
Case "<PRICE>"

If tcpword(2) = "<SLUTT>" Then

    On Error Resume Next
    lpgnorge.rsdispensere.MoveFirst
    lpgnorge.rsdispensere!pris = CSng(tcpword(1))
    If lpgnorge.rsdispensere!pris > 1 Then
        lpgnorge.rsdispensere.Update
        lpgnorge.rsdispensere.MoveFirst
        dispprice(0) = Format(lpgnorge.rsdispensere!pris, "0.00")
        disp_setprice dispnr(0), Replace(Format(dispprice(0), "00.00"), ",", "")
    Else
    tcpsend "<PRICE>;ERROR;Pris for liten;<SLUTT>"
    errorlist.AddItem Now & "Pris er lavere enn 1 kr, avbrutt:"
    End If
    
    If Err = 0 Then
        tcpsend "<PRICE>;OK;<SLUTT>"
    Else
        errorlist.AddItem "Feil ved prisendring:" & Now & " " & Err.Number & " " & Err.Description
    End If
    On Error GoTo errhandler
Else
    tcpsend "<PRICE>;?;<SLUTT>"
End If

End Select

Exit Sub

errhandler:
errorlist.AddItem Now & "tcpserver_dataarrival :" & Erl & " " & Err.Number & " " & Err.Description
Resume Next

End Sub
Private Sub tcpserver_Error(ByVal Number As Integer, Description As String, ByVal Scode As Long, ByVal Source As String, ByVal HelpFile As String, ByVal HelpContext As Long, CancelDisplay As Boolean)

On Error GoTo errhandler
CancelDisplay = True
tcperror = True
errorlist.AddItem Number & " - " & Description & " :" & Scode & " " & Source
tcpserver.Close

Exit Sub
errhandler:
errorlist.AddItem Err.Number & " " & Err.Description

End Sub
Private Sub disp_unblock_liter(volume As String)
On Error GoTo errhandler

    y(1) = &H10
    y(2) = &HC
    y(3) = dispnr(0)
    y(4) = &H70
    y(5) = Asc(Mid(volume, 6, 1))
    y(6) = Asc(Mid(volume, 5, 1))
    y(7) = Asc(Mid(volume, 4, 1))
    y(8) = Asc(Mid(volume, 3, 1))
    y(9) = Asc(Mid(volume, 2, 1))
    y(10) = Asc(Mid(volume, 1, 1))
    y(11) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8) Xor y(9) Xor y(10)
    y(12) = &H36
    
    comm_out 100, Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12))
disp_unblock
Exit Sub
errhandler:
errorlist.AddItem Err.Number & " " & Err.Description

End Sub



Private Sub Timeout_timer_Timer()

On Error GoTo errhandler

If Ready_to_presum Then
    Ready_to_presum = False
    pre_sum lpgnorge.rsdispensere!preselectionamount
Exit Sub
End If


If RFIDCOM.PortOpen Then RFIDCOM.Output = "c"
If tcperror = True Then
    tcperror = False
    If tcpserver.State <> 0 Then tcpserver.Close
    tcpserver.Listen
End If

    If tank_vol = 0 And PaymentType > 0 And DISP_openfordelivery Then
        tanktimeout_count = tanktimeout_count + 1
        Label15.Caption = "tanktimeout:" & tanktimeout_count
        If tanktimeout_count >= tank_timeout Then
            tanktimeout_count = 0
            errorlist.AddItem Now & ":Tank_timeout :transaksjon rollback from paymenttype:" & PaymentType
            LogEvent "Trans_rollback", "Paymenttype:" & CStr(PaymentType), 0, "", "Dispenser"
            Select Case PaymentType
                Case 1
                tcpsend "<TANK_TERMINAL_MESSAGE>;Dispenser blokkert pga timeout(2min);<SLUTT>"
                
                Case 2
                Baxi_Reversal ' Technical_Cashback (bank_sum)   'sjekk bank_sum
                tcpsend "<TANK_TERMINAL_MESSAGE>;Banktransaksjon annulert pga timeout(2min);<SLUTT>"
            
                Case 3
                checkkreditt = False
                tcpsend "<TANK_TERMINAL_MESSAGE>;Stasjonskreditt blokkert pga timeout(2min);<SLUTT>"
            End Select
        disp_block
        End If
        
    End If
Exit Sub
errhandler:
errorlist.AddItem Now & " " & Err.Number & Err.Description
Resume Next
End Sub

