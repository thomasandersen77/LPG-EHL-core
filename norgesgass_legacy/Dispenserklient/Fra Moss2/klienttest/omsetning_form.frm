VERSION 5.00
Begin VB.Form omsetning_form 
   BorderStyle     =   3  'Fixed Dialog
   Caption         =   "Omsetningsrapport"
   ClientHeight    =   1200
   ClientLeft      =   6750
   ClientTop       =   7020
   ClientWidth     =   4680
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   1200
   ScaleWidth      =   4680
   ShowInTaskbar   =   0   'False
   Begin VB.ComboBox rappaar 
      Height          =   315
      ItemData        =   "omsetning_form.frx":0000
      Left            =   3720
      List            =   "omsetning_form.frx":0025
      TabIndex        =   5
      Text            =   "2010"
      Top             =   210
      Width           =   855
   End
   Begin VB.ComboBox rappmndtil 
      Height          =   315
      ItemData        =   "omsetning_form.frx":006B
      Left            =   2640
      List            =   "omsetning_form.frx":0093
      TabIndex        =   2
      Text            =   "01"
      Top             =   210
      Width           =   735
   End
   Begin VB.ComboBox rappmndfra 
      Height          =   315
      ItemData        =   "omsetning_form.frx":00C7
      Left            =   1560
      List            =   "omsetning_form.frx":00EF
      TabIndex        =   1
      Text            =   "01"
      Top             =   210
      Width           =   735
   End
   Begin VB.CommandButton hentrapport 
      Caption         =   "Hent Rapport"
      Height          =   375
      Left            =   1800
      TabIndex        =   0
      Top             =   720
      Width           =   1455
   End
   Begin VB.Label Label5 
      Caption         =   "-"
      Height          =   255
      Left            =   3480
      TabIndex        =   6
      Top             =   240
      Width           =   135
   End
   Begin VB.Label Label3 
      Caption         =   "Velg Periode :"
      Height          =   255
      Left            =   120
      TabIndex        =   4
      Top             =   240
      Width           =   1335
   End
   Begin VB.Label Label2 
      Caption         =   "-"
      Height          =   255
      Left            =   2400
      TabIndex        =   3
      Top             =   240
      Width           =   135
   End
End
Attribute VB_Name = "omsetning_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Form_Load()
rappmndfra.Text = Month(Now)
rappmndtil.Text = Month(Now)
rappaar.Text = Year(Now)


End Sub

Private Sub Form_QueryUnload(cancel As Integer, UnloadMode As Integer)
Unload Me

End Sub

Private Sub hentrapport_Click()
lpgnorge.Sletttabeller

With lpgnorge.Commands.Item("makebankrapp_presalg")
.CommandText = "SELECT DAY(dato) AS Expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Beløp%', reporttext) + 9, 15)), ',', '.') AS float)) AS sum1 Into presalg From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Beløp%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato)"

End With

lpgnorge.Makebankrapp_presalg

With lpgnorge.Commands.Item("makebankrapp_retur")
.CommandText = "SELECT DAY(dato) AS expr1, SUM(CAST(REPLACE(LTRIM(SUBSTRING(reporttext, PATINDEX('%Retur%', reporttext) + 9, 15)), ',', '.') AS float)) AS retur Into retur From rapporter_bankterminal WHERE (YEAR(dato) = " & rappaar.Text & " AND (MONTH(dato) >=" & rappmndfra.Text & " And month(dato)<=" & rappmndtil.Text & ") AND (type = 'Terminal') AND (reporttext LIKE '%Retur%') AND (reporttext NOT LIKE '%Z-Total%')) GROUP BY DAY(dato) "
End With

lpgnorge.Makebankrapp_retur
If lpgnorge.rsomsetningprdag.State <> 1 Then lpgnorge.rsomsetningprdag.Open

lpgnorge.rsomsetningprdag.Requery

Load Omsetningprdag
Omsetningprdag.Show (1)


End Sub
