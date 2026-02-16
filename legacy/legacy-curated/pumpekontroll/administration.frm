VERSION 5.00
Object = "{FAEEE763-117E-101B-8933-08002B2F4F5A}#1.1#0"; "DBLIST32.OCX"
Begin VB.Form admform 
   Caption         =   "Administrasjon bankterminal"
   ClientHeight    =   5175
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   7650
   DrawMode        =   14  'Copy Pen
   LinkTopic       =   "Form1"
   ScaleHeight     =   5175
   ScaleWidth      =   7650
   StartUpPosition =   3  'Windows Default
   Begin MSDBCtls.DBCombo avstemmingkopi 
      Height          =   315
      Left            =   2400
      TabIndex        =   6
      Top             =   2520
      Width           =   2655
      _ExtentX        =   4683
      _ExtentY        =   556
      _Version        =   393216
      Text            =   ""
   End
   Begin MSDBCtls.DBCombo zrapportkopi 
      Height          =   315
      Left            =   2400
      TabIndex        =   5
      Top             =   1560
      Width           =   2655
      _ExtentX        =   4683
      _ExtentY        =   556
      _Version        =   393216
      Text            =   ""
   End
   Begin MSDBCtls.DBCombo xrapportkopi 
      Height          =   315
      Left            =   2400
      TabIndex        =   4
      Top             =   600
      Width           =   2655
      _ExtentX        =   4683
      _ExtentY        =   556
      _Version        =   393216
      Style           =   2
      Text            =   ""
   End
   Begin VB.CommandButton cmdtomprinterbuffer 
      Caption         =   "Tøm printerbuffer"
      Height          =   735
      Left            =   240
      TabIndex        =   3
      Top             =   3240
      Width           =   1575
   End
   Begin VB.CommandButton Command3 
      Caption         =   "Avstemming"
      Height          =   735
      Left            =   240
      TabIndex        =   2
      Top             =   2280
      Width           =   1575
   End
   Begin VB.CommandButton Command2 
      Caption         =   "Z-Rapport"
      Height          =   735
      Left            =   240
      TabIndex        =   1
      Top             =   1320
      Width           =   1575
   End
   Begin VB.CommandButton Command1 
      Caption         =   "X-Rapport"
      Height          =   855
      Left            =   240
      TabIndex        =   0
      Top             =   240
      Width           =   1575
   End
End
Attribute VB_Name = "admform"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Sub cmdtomprinterbuffer_Click()
admmode = True
rapporttype = "emptyprintbuffer"
Pumpekontroll.baxi.administration &H3131, 0

End Sub

Private Sub Command1_Click()
admmode = True
Pumpekontroll.baxi.administration &H3136, 0
rapporttype = "Xrapport"
DataReport1.Show

End Sub

Private Sub Command2_Click()
admmode = True
Pumpekontroll.baxi.administration &H3137, 0
rapporttype = "Zrapport"
DataReport1.Show

End Sub

Private Sub Command3_Click()
admmode = True
Pumpekontroll.baxi.administration &H3130, 0
rapporttype = "Avstemming"
DataReport1.Show

End Sub

Private Sub Form_Deactivate()
admmode = False

End Sub

Private Sub Form_Load()
Dim rappx, rappz, rappavs As ADODB.Recordset
'rapport_rs.Open "select * from rapporter_bankterminal order by dato desc", sqlconn, adOpenKeyset, adLockOptimistic
Set rappx = rapport_rs.Clone
Set rappz = rapport_rs.Clone
Set rappavs = rapport_rs.Clone
rappx.Filter = "type='Xrapport'"
rappz.Filter = "type='Zrapport'"
rappavs.Filter = "type='Avstemming'"
Set xrapportkopi.DataSource = rappx

xrapportkopi.DataField = rapport_rs.Fields("dato").Name

Set zrapportkopi.DataSource = rappz
zrapportkopi.DataField = rapport_rs.Fields("dato").Name
Set avstemmingkopi.DataSource = rappavs
avstemmingkopi.DataField = rapport_rs.Fields("dato").Name

End Sub

Private Sub Form_QueryUnload(Cancel As Integer, UnloadMode As Integer)
'rapport_rs.Close
'Set rapport_rs = Nothing
admmode = False

End Sub

