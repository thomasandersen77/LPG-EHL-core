VERSION 5.00
Object = "{248DD890-BB45-11CF-9ABC-0080C7E7B78D}#1.0#0"; "MSWINSCK.OCX"
Object = "{831FDD16-0C5C-11D2-A9FC-0000F8754DA1}#2.0#0"; "MSCOMCTL.OCX"
Begin VB.Form Dispenserkontroll 
   Caption         =   "LPG Dispenserkontroll"
   ClientHeight    =   4350
   ClientLeft      =   1545
   ClientTop       =   2475
   ClientWidth     =   12165
   LinkTopic       =   "Form1"
   ScaleHeight     =   4350
   ScaleWidth      =   12165
   Begin VB.CommandButton bank 
      Height          =   735
      Left            =   7800
      TabIndex        =   20
      Top             =   960
      Visible         =   0   'False
      Width           =   1215
   End
   Begin VB.TextBox txtcashkr 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "0"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   1
      EndProperty
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   420
      Left            =   9840
      MaxLength       =   4
      TabIndex        =   17
      Text            =   "0"
      Top             =   2640
      Width           =   735
   End
   Begin VB.CommandButton cmdcash 
      Caption         =   "Utfør"
      Height          =   375
      Left            =   11280
      TabIndex        =   16
      Top             =   2670
      Width           =   735
   End
   Begin VB.TextBox txtcashore 
      Alignment       =   1  'Right Justify
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   420
      Left            =   10680
      MaxLength       =   2
      TabIndex        =   15
      Text            =   "00"
      Top             =   2640
      Width           =   495
   End
   Begin VB.TextBox txtcashbackore 
      Alignment       =   1  'Right Justify
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   420
      Left            =   10680
      MaxLength       =   2
      TabIndex        =   12
      Text            =   "00"
      Top             =   3210
      Width           =   495
   End
   Begin VB.CommandButton cmdcashback 
      Caption         =   "Utfør"
      Height          =   375
      Left            =   11280
      TabIndex        =   11
      Top             =   3240
      Width           =   735
   End
   Begin VB.TextBox txtcashbackkr 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "0"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   1
      EndProperty
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   420
      Left            =   9840
      MaxLength       =   4
      TabIndex        =   9
      Text            =   "0"
      Top             =   3210
      Width           =   735
   End
   Begin VB.Timer Timer1 
      Interval        =   20000
      Left            =   6840
      Top             =   1800
   End
   Begin MSComctlLib.StatusBar StatusBar1 
      Align           =   2  'Align Bottom
      Height          =   375
      Left            =   0
      TabIndex        =   8
      Top             =   3975
      Width           =   12165
      _ExtentX        =   21458
      _ExtentY        =   661
      _Version        =   393216
      BeginProperty Panels {8E3867A5-8586-11D1-B16A-00C0F0283628} 
         NumPanels       =   2
         BeginProperty Panel1 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   2
         EndProperty
         BeginProperty Panel2 {8E3867AB-8586-11D1-B16A-00C0F0283628} 
            AutoSize        =   1
            Object.Width           =   18389
         EndProperty
      EndProperty
   End
   Begin MSWinsockLib.Winsock tcpclient 
      Left            =   6240
      Top             =   1800
      _ExtentX        =   741
      _ExtentY        =   741
      _Version        =   393216
      RemoteHost      =   "10.87.233.10"
      RemotePort      =   9002
   End
   Begin VB.CommandButton cmdstart 
      BackColor       =   &H0000C000&
      Caption         =   "Frigi dispenser"
      Default         =   -1  'True
      DisabledPicture =   "Dispenserkontroll.frx":0000
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
      Left            =   6120
      MaskColor       =   &H0000C000&
      Style           =   1  'Graphical
      TabIndex        =   1
      TabStop         =   0   'False
      ToolTipText     =   "Trykk for å frigi dispenser."
      Top             =   960
      Width           =   1575
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
      Left            =   6120
      MaskColor       =   &H000000FF&
      Style           =   1  'Graphical
      TabIndex        =   0
      TabStop         =   0   'False
      Top             =   2880
      Width           =   1575
   End
   Begin VB.Label Label9 
      Caption         =   "Label9"
      Height          =   495
      Left            =   8040
      TabIndex        =   19
      Top             =   1080
      Visible         =   0   'False
      Width           =   1935
   End
   Begin VB.Label Label5 
      Caption         =   "Belast kortkunde."
      Height          =   255
      Left            =   8040
      TabIndex        =   18
      Top             =   2730
      Width           =   1695
   End
   Begin VB.Label Label7 
      Caption         =   "Bankterminal status:"
      Height          =   255
      Left            =   8040
      TabIndex        =   14
      Top             =   1800
      Width           =   1815
   End
   Begin VB.Label txtbankdisplay 
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   375
      Left            =   8040
      TabIndex        =   13
      Top             =   2160
      Width           =   4215
   End
   Begin VB.Label Label4 
      Caption         =   "Tilbakefør til kortkunde."
      Height          =   255
      Left            =   8040
      TabIndex        =   10
      Top             =   3300
      Width           =   1815
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
      Left            =   0
      TabIndex        =   7
      Top             =   360
      Width           =   1935
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
      Height          =   375
      Left            =   0
      TabIndex        =   6
      Top             =   1800
      Width           =   1935
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
      Height          =   375
      Left            =   0
      TabIndex        =   5
      Top             =   3120
      Width           =   1935
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
      Height          =   975
      Left            =   2280
      TabIndex        =   4
      Top             =   120
      Width           =   3735
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
      Height          =   975
      Left            =   2280
      TabIndex        =   3
      Top             =   1440
      Width           =   3735
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
      Height          =   975
      Left            =   2280
      TabIndex        =   2
      Top             =   2640
      Width           =   3735
   End
   Begin VB.Menu Rapporter 
      Caption         =   "Databaseregister"
      Index           =   0
      Begin VB.Menu Bankterminal 
         Caption         =   "Bankterminal"
         Index           =   1
      End
      Begin VB.Menu Tankinger 
         Caption         =   "Tankinger"
         Index           =   2
      End
      Begin VB.Menu Stasjonkreditt_tankinger 
         Caption         =   "Stasjonskreditt tankinger"
      End
      Begin VB.Menu Kunder 
         Caption         =   "Kunder"
      End
   End
   Begin VB.Menu Rapporter 
      Caption         =   "Rapporter"
      Index           =   4
      Begin VB.Menu Omsetningsrapport 
         Caption         =   "Omsetningsrapport"
         Index           =   5
      End
      Begin VB.Menu uttaksrapport 
         Caption         =   "Uttaksrapport"
         Index           =   6
      End
      Begin VB.Menu stasjonskreditt_rapport 
         Caption         =   "Stasjonskreditt"
      End
   End
   Begin VB.Menu functiondispliter 
      Caption         =   "Funksjoner"
      Begin VB.Menu frigidispliter 
         Caption         =   "Frigi dispenser(antall liter)"
         Shortcut        =   {F1}
      End
   End
End
Attribute VB_Name = "Dispenserkontroll"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Dim rectext As String
Dim sendtext As String
Dim tcpword() As String
Dim state_string_tank As String


Dim DISP_openfordelivery, trans_unaccounted, trans_finished_powerfault, DISP_startbuttonpressed, DISP_automode As Boolean


Private Sub Bankterminal_Click(Index As Integer)
bankterminal_form.Show (1)

End Sub


Private Sub cmdcash_Click()

Dim cashstring As String

cashstring = txtcashkr.Text & txtcashore.Text
If MsgBox("Er du sikker på at du vil belaste KR " & txtcashkr.Text & "." & txtcashore.Text & " på bankkort.  Pass på at ikke bankterminalen er/skal i bruk før du trykker ok.", vbYesNo) = vbYes Then
    tcpclient.SendData "<BANK_CASH>;" & cashstring
 
End If
txtcashkr.Text = "0"
txtcashore.Text = "00"
End Sub

Private Sub cmdcashback_Click()

Dim cashbackstring As String

cashbackstring = txtcashbackkr.Text & txtcashbackore.Text
If MsgBox("Er du helt sikker på at du vil tilbakeføre KR " & txtcashbackkr.Text & "." & txtcashbackore.Text & " til bankkort.  Pass på at ikke bankterminalen er/skal i bruk før du trykker ok.", vbYesNo) = vbYes Then
    tcpclient.SendData "<BANK_CASHBACK>;" & cashbackstring
 
    
End If
txtcashbackkr.Text = "0"
txtcashbackore.Text = "00"

End Sub

Private Sub cmddisp_stop_Click()
tcpclient.SendData "<TANK_DISP_STOP>"

End Sub

Private Sub cmdstart_Click()

tcpclient.SendData "<TANK_DISP_UNBLOCK>;0"

End Sub

Private Sub Form_Load()
StatusBar1.Panels(1).Text = "Venter 20 sekunder på tilkobling"
If tcpclient.State = 0 Then tcpclient.Connect
'WebBrowser1.Navigate "http:\\10.87.233.251"
End Sub

Private Sub Form_QueryUnload(cancel As Integer, UnloadMode As Integer)
'tcpclient.Close
End Sub


Private Sub frigidispliter_Click()
displiter.Show (1)
End Sub

Private Sub Kunder_Click()
Kundereg.Show (1)
End Sub

Private Sub Omsetningsrapport_Click(Index As Integer)
omsetning_form.Show (1)
End Sub

Private Sub Stasjonkreditt_Click()
stasjonskreditt.Show (1)
End Sub

Private Sub Stasjonkreditt_tankinger_Click()
stasjonskreditt_form.Show (1)
End Sub


Private Sub Tankinger_Click(Index As Integer)
Tankinger_form.Show (1)
End Sub

Private Sub tcpclient_DataArrival(ByVal bytesTotal As Long)

On Error Resume Next

tcpclient.GetData rectext
tcpword = Split(rectext, ";")

Select Case tcpword(0)


Case "<TANK>"
    belop.Caption = tcpword(1)
    antall_liter.Caption = tcpword(2)
    dispris.Caption = tcpword(3)
    If tcpword(4) = "1" Then
        bank.Visible = True
        bank.Caption = tcpword(5)
        End If

Case "<TANK_STOP>"

Case "<STATE_TANK>"

If Len(tcpword(1)) = 8 Then
    state_string_tank = tcpword(1)
    If CInt(Mid(state_string_tank, 8, 1)) = 1 Then trans_finished_powerfault = True Else trans_finished_powerfault = False
    If CInt(Mid(state_string_tank, 5, 1)) = 1 Then trans_unaccounted = True Else trans_unaccounted = False
    If trans_unaccounted Then
        cmdstart.Caption = "Tanking avsluttet"
        If bank.Visible Then bank.Visible = False
        Beep
    End If
End If

Case "<STATE>"
If Len(tcpword(1)) = 8 Then
 If Mid(tcpword(1), 6, 1) = "1" Then DISP_startbuttonpressed = True Else DISP_startbuttonpressed = False
 If Mid(tcpword(1), 7, 1) = "1" Then DISP_openfordelivery = True Else DISP_openfordelivery = False
 If Mid(tcpword(1), 5, 1) = "1" Then DISP_automode = True Else DISP_automode = False
 If DISP_startbuttonpressed And DISP_openfordelivery Then cmdstart.Caption = "Tanker"

Else

    
End If


If DISP_startbuttonpressed And Not DISP_openfordelivery And Not bank.Visible Then
  
    cmdstart.Caption = "Frigi ?"
    Beep
End If

If DISP_openfordelivery Then
 cmdstart.Caption = "Frigitt"
Else
cmdstart.Caption = "Frigi ?"
bank.Visible = False

End If

Case "<DISP_LITER>"
If tcpword(1) = "<DISP_LITER_OK>" And (tcpword(2) = CSng(displiter.liter.Text) / 100) Then
    displiter.Label1.Caption = CSng(tcpword(2)) & " liter bekreftet."
    
   ' displiter.freedisp.SetFocus
    displiter.liter.SelStart = 0
    displiter.liter.SelLength = Len(displiter.liter.Text)


   ' displiter.freedisp.Visible = True
End If



Case "<TANK_TERMINAL_MESSAGE>"
txtbankdisplay.Caption = tcpword(1)


Case "<PRINTERSTATE>"
Label9.Caption = tcpword(1)
End Select
End Sub



Private Sub tcpclient_Error(ByVal Number As Integer, Description As String, ByVal Scode As Long, ByVal Source As String, ByVal HelpFile As String, ByVal HelpContext As Long, CancelDisplay As Boolean)
StatusBar1.Panels(2).Text = "Feilkode :" & Number & " " & Description & " " & Scode
CancelDisplay = True

End Sub

Private Sub Timer1_Timer()
Dim statetext As String

Select Case tcpclient.State

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
statetext = "Tilkoblet til vert."
Case 8
statetext = "Nedkobler."

Case 9
statetext = "Feil ved tilkobling."
End Select

StatusBar1.Panels(1).Text = statetext
StatusBar1.Panels(2).Text = ""

If tcpclient.State > 7 Then
tcpclient.Close

tcpclient.LocalPort = 0

tcpclient.Connect
End If
End Sub





Private Sub txtcashbackkr_Validate(cancel As Boolean)


If IsNumeric(txtcashbackkr.Text) Then
    If CInt(txtcashbackkr.Text) >= 0 Then
    
    Else
        MsgBox "Du må gi skrive inn et gyldig tall i KR feltet. Negative tall er ikke gyldig."
        cancel = True
    End If
Else
    MsgBox "Dette er ikke et gyldig tall i KR feltet."
    cancel = True
End If

End Sub

Private Sub txtcashbackore_Validate(cancel As Boolean)


If IsNumeric(txtcashbackore.Text) Then
    If CInt(txtcashbackore.Text) >= 0 Then
    Else
        MsgBox "Du må gi skrive inn et gyldig tall i øre feltet. Negative tall er ikke gyldig."
        cancel = True
    End If
Else
    MsgBox "Dette er ikke et gyldig tall i øre feltet."
    cancel = True
End If

End Sub

Private Sub txtcashkr_Validate(cancel As Boolean)


If IsNumeric(txtcashkr.Text) Then
    If CInt(txtcashkr.Text) >= 0 Then
    Else
        MsgBox "Du må gi skrive inn et gyldig tall i KR feltet. Negative tall er ikke gyldig."
        cancel = True
    End If
Else
    MsgBox "Dette er ikke et gyldig tall i KR feltet."
    cancel = True
End If

End Sub

Private Sub txtcashore_Validate(cancel As Boolean)


If IsNumeric(txtcashore.Text) Then
    If CInt(txtcashore.Text) >= 0 Then
    Else
        MsgBox "Du må gi skrive inn et gyldig tall i øre feltet. Negative tall er ikke gyldig."
        cancel = True
    End If
Else
    MsgBox "Dette er ikke et gyldig tall i øre feltet."
    cancel = True
End If
End Sub

Private Sub uttaksrapport_Click(Index As Integer)
Uttaksrapport_form.Show (1)
End Sub
