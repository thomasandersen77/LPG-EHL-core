VERSION 5.00
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Begin VB.Form kortscan_form 
   BorderStyle     =   1  'Fixed Single
   Caption         =   "Kundekort som er skannet."
   ClientHeight    =   3645
   ClientLeft      =   6075
   ClientTop       =   3780
   ClientWidth     =   4680
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   3645
   ScaleWidth      =   4680
   Begin MSDataGridLib.DataGrid kortliste 
      Bindings        =   "kortscan_form.frx":0000
      Height          =   2775
      Left            =   120
      TabIndex        =   0
      Top             =   120
      Width           =   4455
      _ExtentX        =   7858
      _ExtentY        =   4895
      _Version        =   393216
      AllowUpdate     =   -1  'True
      HeadLines       =   1
      RowHeight       =   15
      FormatLocked    =   -1  'True
      AllowDelete     =   -1  'True
      BeginProperty HeadFont {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      DataMember      =   "kortscan"
      Caption         =   "Kundekort som er skannet,men ikke tilordnet"
      ColumnCount     =   3
      BeginProperty Column00 
         DataField       =   "kortid"
         Caption         =   "kortid"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column01 
         DataField       =   "kortnummer"
         Caption         =   "kortnummer"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column02 
         DataField       =   "scandato"
         Caption         =   "scandato"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   ""
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      SplitCount      =   1
      BeginProperty Split0 
         MarqueeStyle    =   4
         BeginProperty Column00 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column01 
            ColumnWidth     =   2250,142
         EndProperty
         BeginProperty Column02 
            ColumnWidth     =   1739,906
         EndProperty
      EndProperty
   End
End
Attribute VB_Name = "kortscan_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()
If MsgBox("Er du helt sikker på at du vil slette den gjeldende tilknytning mellom kort og kunde?", vbYesNo, "Slette korttilknytning?") = vbYes Then
lpgnorge.Butikkdata.Execute "delete from lpgstasjonskort where kortnummer='" & Trim(Kundereg.kundeliste.Columns(4).Text) & "'"

Else
End If


lpgnorge.rskortscan.Requery
End Sub

Private Sub Form_Load()
lpgnorge.rskortscan.Requery

End Sub

Private Sub kortliste_DblClick()
    If lpgnorge.rskortscan.RecordCount > 0 Then
    If MsgBox("Vil du knytte dette kortnummeret opp mot kunde du nå redigerer/legger til?", vbYesNo) = vbYes Then
    'If Not IsEmpty(kortliste.Columns(1).Value) Then
        Kundereg.kontaktpersoner.Columns(22) = Me.kortliste.Columns(1).Text
        
        lpgnorge.rskortscan!used = True
        lpgnorge.rskortscan.Update
        lpgnorge.rskortscan.Requery
        kortliste.Refresh
        
        
    'End If
    Unload Me
    
    
    End If
Else
MsgBox ("Ingen kort er skannet og klar til å knyttes sammen med kortbruker")

End If
End Sub
