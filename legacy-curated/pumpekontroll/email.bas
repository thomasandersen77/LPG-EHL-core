Attribute VB_Name = "email"
Option Explicit

Dim objemail


Public Sub Eml(emlsub As String, emltext As String, emlto As String, Optional emlattachment As String)
    
 On Error GoTo errhandler
Dim s1 As String
Dim b1 As Boolean
b1 = False

If emlattachment <> "" Then
b1 = True
End If
With Pumpekontroll.MAPIMessages1
            .SessionID = Pumpekontroll.MAPISession1.SessionID
            .Compose
            .RecipAddress = emlto
            .AddressResolveUI = False
            .ResolveName
            .MsgSubject = emlsub
             If b1 = True Then
            .AttachmentPathName = emlattachment
             End If
          
            .MsgNoteText = emltext
            .Send False
End With
Exit Sub
errhandler:
Pumpekontroll.errorlist.AddItem Err.Number & " " & Err.Description

End Sub



