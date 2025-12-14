VERSION 5.00
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Begin VB.Form Stasjonskredittkort 
   BorderStyle     =   3  'Fixed Dialog
   Caption         =   "Finn stasjonskort"
   ClientHeight    =   5445
   ClientLeft      =   45
   ClientTop       =   330
   ClientWidth     =   5310
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   5445
   ScaleWidth      =   5310
   ShowInTaskbar   =   0   'False
   StartUpPosition =   3  'Windows Default
   Begin VB.CommandButton Command1 
      Caption         =   "Nullstill"
      Height          =   285
      Left            =   3840
      TabIndex        =   3
      Top             =   240
      Width           =   855
   End
   Begin VB.CommandButton sokkort 
      Caption         =   "Søk kort"
      Height          =   285
      Left            =   2640
      TabIndex        =   1
      Top             =   240
      Width           =   975
   End
   Begin VB.TextBox stasjonskort 
      Height          =   285
      Left            =   600
      TabIndex        =   0
      Top             =   240
      Width           =   1935
   End
   Begin MSDataGridLib.DataGrid stasjonskortgrid 
      Bindings        =   "Stasjonskredittkort_sok.frx":0000
      Height          =   4335
      Left            =   240
      TabIndex        =   2
      Top             =   720
      Width           =   4575
      _ExtentX        =   8070
      _ExtentY        =   7646
      _Version        =   393216
      AllowUpdate     =   0   'False
      HeadLines       =   1
      RowHeight       =   15
      FormatLocked    =   -1  'True
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
      DataMember      =   "stasjonskred"
      Caption         =   "Fant følgende stasjonskort:"
      ColumnCount     =   3
      BeginProperty Column00 
         DataField       =   "kontonr"
         Caption         =   "Kundenummer"
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
         DataField       =   "id"
         Caption         =   "KontaktID"
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
         DataField       =   "kortnummer"
         Caption         =   "Kortnummer"
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
            ColumnWidth     =   1200,189
         EndProperty
         BeginProperty Column01 
            ColumnWidth     =   915,024
         EndProperty
         BeginProperty Column02 
            ColumnWidth     =   1739,906
         EndProperty
      EndProperty
   End
End
Attribute VB_Name = "Stasjonskredittkort"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Sub Command1_Click()
On Error GoTo errhandler

lpgnorge.rsstasjonskred.Filter = ""

Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "Sok_stasjonskort:" & Err.Number & " " & Err.Description

End Sub

Private Sub Form_Load()
On Error GoTo errhandler

lpgnorge.rsstasjonskred.Filter = ""
Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "Sok_stasjonskort:" & Err.Number & " " & Err.Description

End Sub

Private Sub Form_QueryUnload(Cancel As Integer, UnloadMode As Integer)
On Error GoTo errhandler

lpgnorge.rsstasjonskred.Filter = ""
Exit Sub


errhandler:
Pumpekontroll.errorlist.AddItem "Sok_stasjonskort:" & Err.Number & " " & Err.Description
End Sub

Private Sub sokkort_Click()
On Error GoTo errhandler

lpgnorge.rsstasjonskred.Filter = "kortnummer ='" & Trim(stasjonskort.Text) & "'"
Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "Sok_stasjonskort:" & Err.Number & " " & Err.Description

End Sub
