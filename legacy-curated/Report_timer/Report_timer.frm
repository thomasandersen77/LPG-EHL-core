VERSION 5.00
Begin VB.Form Form1 
   Caption         =   "Form1"
   ClientHeight    =   5100
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   7740
   LinkTopic       =   "Form1"
   ScaleHeight     =   5100
   ScaleWidth      =   7740
   StartUpPosition =   3  'Windows Default
End
Attribute VB_Name = "Form1"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Dim SQLconn As New ADODB.Connection
Dim RST As New ADODB.Recordset
Dim cfgline() As String
Dim dbserver, dbdb, textline, dbbrukernavn, dbpassord, sqlconnstr As String


Private Sub Form_Load()

On Error GoTo errhandler

Open "c:\pumpestyring\server.ini" For Input As #1
Line Input #1, textline
cfgline() = Split(textline, ";")
dbserver = cfgline(0)
dbdb = cfgline(1)
dbbrukernavn = cfgline(2)
dbpassord = cfgline(3)
Close #1
sqlconnstr = "Provider=SQLOLEDB;User ID=" & dbbrukernavn & ";Password=" & dbpassord & ";Initial Catalog=" & dbdb & ";Data Source=" & dbserver
SQLconn.Open sqlconnstr
If SQLconn.State = 1 Then
    
    RST.Open "Select * from oppgaver", SQLconn, adOpenKeyset, adLockOptimistic
    If RST.RecordCount <= 0 Then
        RST.AddNew
        RST.Update
        RST.Requery
    End If
    
    If Not RST!zrapport Or IsNull(RST!zrapport) Then
    RST!zrapport = True
    
    End If
    If Not RST!xrapport Or IsNull(RST!xrapport) Then
    RST!xrapport = True
    
    End If
    If Not RST!avstemming Or IsNull(RST!avstemming) Then
    RST!avstemming = True
    End If
    If Not RST!unilink Or IsNull(RST!unilink) Then
    RST!unilink = True
    End If
    
    RST.Update
    Else
'Her må det komme en logging om feil.
End If
RST.Close
SQLconn.Close
Set RST = Nothing
Set SQLconn = Nothing
End
Exit Sub

errhandler:
'Her må det komme en logging om feil
End
End Sub
