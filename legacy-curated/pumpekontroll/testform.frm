VERSION 5.00
Begin VB.Form testform 
   Caption         =   "Test form"
   ClientHeight    =   3195
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   4680
   LinkTopic       =   "Form1"
   ScaleHeight     =   3195
   ScaleWidth      =   4680
   StartUpPosition =   3  'Windows Default
   Begin VB.CommandButton Command1 
      Caption         =   "Test zrappDBWrite"
      Height          =   495
      Left            =   1080
      TabIndex        =   0
      Top             =   240
      Width           =   2175
   End
End
Attribute VB_Name = "testform"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()

On Error GoTo errhandler


If lpgnorge.rsdbo_oms_manuell.State <> 1 Then lpgnorge.rsdbo_oms_manuell.Open

        MsgBox lpgnorge.rsdbo_oms_manuell!dagsomsetning
   lpgnorge.rsdbo_oms_manuell.Close
   
Exit Sub
errhandler:
Pumpekontroll.errorlist.AddItem Now & " Testform linje:" & Erl & " " & Err.Number & " " & Err.Description

End Sub
