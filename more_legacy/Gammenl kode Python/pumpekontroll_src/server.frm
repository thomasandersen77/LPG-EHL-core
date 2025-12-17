VERSION 5.00
Begin VB.Form serverinnstillinger 
   BorderStyle     =   4  'Fixed ToolWindow
   ClientHeight    =   8025
   ClientLeft      =   2700
   ClientTop       =   3285
   ClientWidth     =   12270
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   8025
   ScaleWidth      =   12270
   ShowInTaskbar   =   0   'False
   Begin VB.TextBox Text8 
      DataField       =   "Pathexportautogas"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   45
      Top             =   5760
      Width           =   2295
   End
   Begin VB.TextBox Text7 
      DataField       =   "possqlserver"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   40
      Top             =   3600
      Width           =   2295
   End
   Begin VB.TextBox Text6 
      DataField       =   "posdb"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   39
      Top             =   4080
      Width           =   2295
   End
   Begin VB.TextBox Text5 
      DataField       =   "posui"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   38
      Top             =   4560
      Width           =   1455
   End
   Begin VB.TextBox Text4 
      DataField       =   "pospw"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      IMEMode         =   3  'DISABLE
      Left            =   1800
      PasswordChar    =   "*"
      TabIndex        =   37
      Top             =   5040
      Width           =   1455
   End
   Begin VB.TextBox Text3 
      DataField       =   "RS485container"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   32
      Text            =   "2"
      Top             =   1200
      Width           =   855
   End
   Begin VB.TextBox Text2 
      DataField       =   "RS485autogas"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   31
      Text            =   "1"
      Top             =   840
      Width           =   855
   End
   Begin VB.TextBox Text1 
      DataField       =   "RFID_comport"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   29
      Text            =   "5"
      Top             =   5160
      Width           =   1455
   End
   Begin VB.TextBox printerfeed 
      DataField       =   "Reciptprinter_printerfeed"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   25
      Text            =   "40"
      Top             =   2640
      Width           =   855
   End
   Begin VB.TextBox bankf1 
      DataField       =   "F1_value"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   23
      Text            =   "010000"
      Top             =   3600
      Width           =   855
   End
   Begin VB.TextBox bankf2 
      DataField       =   "F2_value"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   21
      Text            =   "020000"
      Top             =   3960
      Width           =   855
   End
   Begin VB.TextBox bankf3 
      DataField       =   "F3_value"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   19
      Text            =   "040000"
      Top             =   4320
      Width           =   855
   End
   Begin VB.TextBox bankf4 
      DataField       =   "F4_value"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   17
      Text            =   "060000"
      Top             =   4680
      Width           =   855
   End
   Begin VB.TextBox txtcom_pinpad 
      DataField       =   "Pinpad_comport"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   15
      Text            =   "3"
      Top             =   3120
      Width           =   1455
   End
   Begin VB.TextBox txtcom_printer 
      DataField       =   "Reciptprinter_printerfeed"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   13
      Text            =   "4"
      Top             =   2160
      Width           =   1455
   End
   Begin VB.TextBox txtcom_bank 
      DataField       =   "Paymentpinpad_comport"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   11
      Text            =   "1"
      Top             =   1680
      Width           =   1455
   End
   Begin VB.TextBox txtcom_port 
      DataField       =   "Disp_comport"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   6240
      TabIndex        =   9
      Text            =   "2"
      Top             =   480
      Width           =   1455
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Lagre"
      Height          =   255
      Left            =   5760
      TabIndex        =   8
      Top             =   6360
      Width           =   1095
   End
   Begin VB.TextBox serverpassord 
      DataField       =   "termpw"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      IMEMode         =   3  'DISABLE
      Left            =   1800
      PasswordChar    =   "*"
      TabIndex        =   3
      Top             =   2400
      Width           =   1455
   End
   Begin VB.TextBox serverbrukernavn 
      DataField       =   "termuid"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   2
      Top             =   1920
      Width           =   1455
   End
   Begin VB.TextBox serverdb 
      DataField       =   "termdb"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   1
      Top             =   1440
      Width           =   2295
   End
   Begin VB.TextBox servernavn 
      DataField       =   "termsqlserver"
      DataMember      =   "Settings"
      DataSource      =   "lpgnorge"
      Height          =   285
      Left            =   1800
      TabIndex        =   0
      Top             =   960
      Width           =   2295
   End
   Begin VB.Label Label25 
      Caption         =   "Eksport bane Autogas"
      Height          =   255
      Left            =   120
      TabIndex        =   46
      Top             =   5760
      Width           =   1695
   End
   Begin VB.Label Label24 
      Caption         =   "SQL SERVER :"
      Height          =   255
      Left            =   120
      TabIndex        =   44
      Top             =   3600
      Width           =   1215
   End
   Begin VB.Label Label23 
      Caption         =   "SQL PASSORD :"
      Height          =   255
      Left            =   120
      TabIndex        =   43
      Top             =   5040
      Width           =   1215
   End
   Begin VB.Label Label22 
      Caption         =   "SQL BRUKERNAVN:"
      Height          =   255
      Left            =   120
      TabIndex        =   42
      Top             =   4560
      Width           =   1575
   End
   Begin VB.Label Label21 
      Caption         =   "DATABASE :"
      Height          =   255
      Left            =   120
      TabIndex        =   41
      Top             =   4080
      Width           =   1215
   End
   Begin VB.Label Label20 
      Caption         =   "POS system"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   255
      Left            =   120
      TabIndex        =   36
      Top             =   3120
      Width           =   1455
   End
   Begin VB.Label Label19 
      Caption         =   "Betalingsterminal"
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   255
      Left            =   120
      TabIndex        =   35
      Top             =   480
      Width           =   1455
   End
   Begin VB.Line Line4 
      X1              =   0
      X2              =   4320
      Y1              =   2880
      Y2              =   2880
   End
   Begin VB.Label Label18 
      Caption         =   "RS485 Container"
      Height          =   255
      Left            =   4800
      TabIndex        =   34
      Top             =   1200
      Width           =   1335
   End
   Begin VB.Label Label17 
      Caption         =   "RS485 autogas"
      Height          =   255
      Left            =   4920
      TabIndex        =   33
      Top             =   840
      Width           =   1215
   End
   Begin VB.Label Label16 
      Caption         =   "Comport RFID"
      Height          =   255
      Left            =   4560
      TabIndex        =   30
      Top             =   5160
      Width           =   1215
   End
   Begin VB.Label Label15 
      Caption         =   "Innstillinger perferiutstyr"
      Height          =   255
      Left            =   4560
      TabIndex        =   28
      Top             =   0
      Width           =   2775
   End
   Begin VB.Label Label14 
      Caption         =   "Databaseinnstillinger"
      Height          =   255
      Left            =   120
      TabIndex        =   27
      Top             =   0
      Width           =   2775
   End
   Begin VB.Line Line3 
      X1              =   0
      X2              =   12240
      Y1              =   360
      Y2              =   360
   End
   Begin VB.Label Label13 
      Caption         =   "Printerfeed(mm)"
      Height          =   255
      Left            =   5040
      TabIndex        =   26
      Top             =   2640
      Width           =   1095
   End
   Begin VB.Line Line2 
      X1              =   7920
      X2              =   7920
      Y1              =   360
      Y2              =   7800
   End
   Begin VB.Label Label12 
      Caption         =   "Bank F1"
      Height          =   255
      Left            =   5400
      TabIndex        =   24
      Top             =   3600
      Width           =   735
   End
   Begin VB.Label Label11 
      Caption         =   "Bank F2"
      Height          =   255
      Left            =   5400
      TabIndex        =   22
      Top             =   3960
      Width           =   735
   End
   Begin VB.Label Label10 
      Caption         =   "Bank F3"
      Height          =   255
      Left            =   5400
      TabIndex        =   20
      Top             =   4320
      Width           =   735
   End
   Begin VB.Label Label9 
      Caption         =   "Bank F4"
      Height          =   255
      Left            =   5400
      TabIndex        =   18
      Top             =   4680
      Width           =   735
   End
   Begin VB.Line Line1 
      X1              =   4320
      X2              =   4320
      Y1              =   360
      Y2              =   7800
   End
   Begin VB.Label Label8 
      Caption         =   "Comport Pinpad"
      Height          =   255
      Left            =   4560
      TabIndex        =   16
      Top             =   3120
      Width           =   1215
   End
   Begin VB.Label Label7 
      Caption         =   "Comport printer"
      Height          =   255
      Left            =   4560
      TabIndex        =   14
      Top             =   2160
      Width           =   1215
   End
   Begin VB.Label Label6 
      Caption         =   "Comport bank"
      Height          =   255
      Left            =   4560
      TabIndex        =   12
      Top             =   1680
      Width           =   1215
   End
   Begin VB.Label Label2 
      Caption         =   "Comport dispenser :"
      Height          =   255
      Left            =   4560
      TabIndex        =   10
      Top             =   480
      Width           =   1575
   End
   Begin VB.Label Label5 
      Caption         =   "DATABASE :"
      Height          =   255
      Left            =   120
      TabIndex        =   7
      Top             =   1440
      Width           =   1215
   End
   Begin VB.Label Label4 
      Caption         =   "SQL BRUKERNAVN:"
      Height          =   255
      Left            =   120
      TabIndex        =   6
      Top             =   1920
      Width           =   1575
   End
   Begin VB.Label Label3 
      Caption         =   "SQL PASSORD :"
      Height          =   255
      Left            =   120
      TabIndex        =   5
      Top             =   2400
      Width           =   1215
   End
   Begin VB.Label Label1 
      Caption         =   "SQL SERVER :"
      Height          =   255
      Left            =   120
      TabIndex        =   4
      Top             =   960
      Width           =   1215
   End
End
Attribute VB_Name = "serverinnstillinger"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
