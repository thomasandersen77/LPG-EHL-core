VERSION 5.00
Begin VB.Form stasjonskreditt_form 
   Caption         =   "Rapporter"
   ClientHeight    =   1605
   ClientLeft      =   8265
   ClientTop       =   5685
   ClientWidth     =   6270
   LinkTopic       =   "Form1"
   ScaleHeight     =   1605
   ScaleWidth      =   6270
   Begin VB.ComboBox rapport 
      Height          =   315
      Left            =   960
      TabIndex        =   8
      Top             =   240
      Width           =   5175
   End
   Begin VB.CommandButton hentrapport 
      Caption         =   "Hent Rapport"
      Height          =   375
      Left            =   4800
      TabIndex        =   5
      Top             =   1200
      Width           =   1335
   End
   Begin VB.ComboBox rappaar 
      Height          =   315
      ItemData        =   "stasjonskreditt_rapport_form.frx":0000
      Left            =   3120
      List            =   "stasjonskreditt_rapport_form.frx":0025
      TabIndex        =   4
      Top             =   930
      Width           =   1095
   End
   Begin VB.ComboBox rappmndtil 
      Height          =   315
      ItemData        =   "stasjonskreditt_rapport_form.frx":006B
      Left            =   2280
      List            =   "stasjonskreditt_rapport_form.frx":0093
      TabIndex        =   3
      Top             =   930
      Width           =   615
   End
   Begin VB.ComboBox rappmndfra 
      Height          =   315
      ItemData        =   "stasjonskreditt_rapport_form.frx":00C7
      Left            =   1440
      List            =   "stasjonskreditt_rapport_form.frx":00EF
      TabIndex        =   2
      Top             =   930
      Width           =   615
   End
   Begin VB.Label Label4 
      Caption         =   "-"
      Height          =   255
      Left            =   3000
      TabIndex        =   7
      Top             =   960
      Width           =   135
   End
   Begin VB.Label Label3 
      Caption         =   "-"
      Height          =   255
      Left            =   2160
      TabIndex        =   6
      Top             =   960
      Width           =   135
   End
   Begin VB.Label Label2 
      Caption         =   "Velg Periode :"
      Height          =   255
      Left            =   120
      TabIndex        =   1
      Top             =   960
      Width           =   1095
   End
   Begin VB.Label Label1 
      Caption         =   "Kunde :"
      Height          =   255
      Left            =   120
      TabIndex        =   0
      Top             =   360
      Width           =   615
   End
End
Attribute VB_Name = "stasjonskreditt_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False

Option Explicit

Dim i() As Integer
Dim x As Integer

Private Sub Form_Activate()
rapport.Clear
If lpgnorge.rskunder.State = 0 Then lpgnorge.rskunder.Open

If lpgnorge.rskunder.RecordCount > 0 Then lpgnorge.rskunder.MoveFirst

x = 1
ReDim i(lpgnorge.rskunder.RecordCount)
While Not lpgnorge.rskunder.EOF
rapport.AddItem lpgnorge.rskunder!Kunde
i(x) = lpgnorge.rskunder!kundeid

lpgnorge.rskunder.MoveNext
x = x + 1

Wend

End Sub

Private Sub Form_Load()

rappmndfra.Text = Month(Now)
rappmndtil.Text = Month(Now)
rappaar.Text = Year(Now)
End Sub

Private Sub Form_QueryUnload(cancel As Integer, UnloadMode As Integer)
Unload Me

End Sub

Private Sub hentrapport_Click()
If lpgnorge.rsstasjontank.State = 1 Then lpgnorge.rsstasjontank.Close

lpgnorge.Commands.Item("Stasjontank").CommandText = "Select datostart,liter,pris,sum,sumekslrab=sum from stasjonskreditt_tankinger where kundeid=" & i(rapport.ListIndex + 1) & " and month(datostart) >=" & rappmndfra.Text & " and month(datostart) <=" & rappmndtil.Text & " and year(datostart)=" & rappaar.Text & " order by datostart"


lpgnorge.rsstasjontank.Open

lpgnorge.rsstasjontank.Requery

Load uttaksrapport

uttaksrapport.Show (1)


End Sub

