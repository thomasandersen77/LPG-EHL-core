VERSION 5.00
Begin VB.Form Uttaksrapport_form 
   Caption         =   "Uttaksrapport"
   ClientHeight    =   1050
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   4680
   LinkTopic       =   "Form1"
   ScaleHeight     =   1050
   ScaleWidth      =   4680
   StartUpPosition =   3  'Windows Default
   Begin VB.CommandButton hentrapport 
      Caption         =   "Hent Rapport"
      Height          =   375
      Left            =   1680
      TabIndex        =   3
      Top             =   510
      Width           =   1455
   End
   Begin VB.ComboBox rappmndfra 
      Height          =   315
      ItemData        =   "Uttaksrapport.frx":0000
      Left            =   1440
      List            =   "Uttaksrapport.frx":0028
      TabIndex        =   2
      Text            =   "01"
      Top             =   0
      Width           =   735
   End
   Begin VB.ComboBox rappmndtil 
      Height          =   315
      ItemData        =   "Uttaksrapport.frx":005C
      Left            =   2520
      List            =   "Uttaksrapport.frx":0084
      TabIndex        =   1
      Text            =   "01"
      Top             =   0
      Width           =   735
   End
   Begin VB.ComboBox rappaar 
      Height          =   315
      ItemData        =   "Uttaksrapport.frx":00B8
      Left            =   3600
      List            =   "Uttaksrapport.frx":00DD
      TabIndex        =   0
      Text            =   "2010"
      Top             =   0
      Width           =   855
   End
   Begin VB.Label Label2 
      Caption         =   "-"
      Height          =   255
      Left            =   2280
      TabIndex        =   6
      Top             =   30
      Width           =   135
   End
   Begin VB.Label Label3 
      Caption         =   "Velg Periode :"
      Height          =   255
      Left            =   0
      TabIndex        =   5
      Top             =   30
      Width           =   1335
   End
   Begin VB.Label Label5 
      Caption         =   "-"
      Height          =   255
      Left            =   3360
      TabIndex        =   4
      Top             =   30
      Width           =   135
   End
End
Attribute VB_Name = "Uttaksrapport_form"
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

With lpgnorge.Commands.Item("Uttakliter")


.CommandText = "select dag=day(datostart), isnull(sum(liter),0) as liter from tankinger where month(datostart)>=" & Uttaksrapport_form.rappmndfra.Text & " and month(datostart)<=" & Uttaksrapport_form.rappmndtil.Text & " and year(datostart)=" & Uttaksrapport_form.rappaar & " group by day(datostart) order by day(datostart)"
End With

If lpgnorge.rsUttakliter.State <> 1 Then lpgnorge.rsUttakliter.Open

lpgnorge.rsUttakliter.Requery

Load uttak
uttak.Show (1)


End Sub

