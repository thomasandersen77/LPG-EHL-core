VERSION 5.00
Object = "{3B7C8863-D78F-101B-B9B5-04021C009402}#1.2#0"; "richtx32.ocx"
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Object = "{86CF1D34-0C5F-11D2-A9FC-0000F8754DA1}#2.0#0"; "MSCOMCT2.OCX"
Begin VB.Form bankterminal_form 
   Caption         =   "Rapporter"
   ClientHeight    =   8130
   ClientLeft      =   2505
   ClientTop       =   2100
   ClientWidth     =   11970
   LinkTopic       =   "Form1"
   ScaleHeight     =   8130
   ScaleWidth      =   11970
   Begin RichTextLib.RichTextBox TxtComment 
      Bindings        =   "rapporter_form.frx":0000
      DataField       =   "Comment"
      DataMember      =   "kvitteringer"
      DataSource      =   "lpgnorge"
      Height          =   1935
      Left            =   4560
      TabIndex        =   10
      Top             =   5280
      Width           =   4695
      _ExtentX        =   8281
      _ExtentY        =   3413
      _Version        =   393217
      Enabled         =   -1  'True
      ScrollBars      =   3
      TextRTF         =   $"rapporter_form.frx":0017
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Send til tilbakeføring"
      Height          =   615
      Left            =   120
      TabIndex        =   9
      Top             =   7320
      Width           =   975
   End
   Begin VB.CommandButton skrivut 
      Caption         =   "Skriv ut"
      Height          =   375
      Left            =   8400
      TabIndex        =   8
      Top             =   120
      Width           =   855
   End
   Begin VB.CommandButton bankrappsoknullstill 
      Caption         =   "Nullstill"
      Height          =   375
      Left            =   7560
      TabIndex        =   7
      Top             =   120
      Width           =   615
   End
   Begin VB.CommandButton bankrappsokcmd 
      Caption         =   "Søk"
      Height          =   375
      Left            =   6480
      TabIndex        =   6
      Top             =   120
      Width           =   975
   End
   Begin VB.TextBox bankrappsoktekst 
      Height          =   405
      Left            =   4560
      TabIndex        =   5
      Top             =   120
      Width           =   1935
   End
   Begin VB.CommandButton filtrer 
      Caption         =   "Hent transer"
      Height          =   375
      Left            =   3600
      TabIndex        =   2
      Top             =   60
      Width           =   855
   End
   Begin MSComCtl2.DTPicker DTPicker1 
      Height          =   375
      Left            =   1680
      TabIndex        =   0
      Top             =   60
      Width           =   1815
      _ExtentX        =   3201
      _ExtentY        =   661
      _Version        =   393216
      Format          =   16515073
      CurrentDate     =   40612
   End
   Begin RichTextLib.RichTextBox RichTextBox1 
      DataSource      =   "lpgnorge"
      Height          =   4215
      Left            =   4560
      TabIndex        =   3
      Top             =   600
      Width           =   4695
      _ExtentX        =   8281
      _ExtentY        =   7435
      _Version        =   393217
      Enabled         =   -1  'True
      ScrollBars      =   3
      TextRTF         =   $"rapporter_form.frx":0099
   End
   Begin MSDataGridLib.DataGrid DataGrid1 
      Height          =   6615
      Left            =   120
      TabIndex        =   4
      Top             =   600
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   11668
      _Version        =   393216
      AllowUpdate     =   -1  'True
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
      Caption         =   "Hendelser denne dag"
      ColumnCount     =   7
      BeginProperty Column00 
         DataField       =   "dato"
         Caption         =   "Dato"
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
         DataField       =   "type"
         Caption         =   "Hendelsestype"
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
         DataField       =   "reporttext"
         Caption         =   "Tekst"
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
      BeginProperty Column03 
         DataField       =   "dato"
         Caption         =   "dato"
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
      BeginProperty Column04 
         DataField       =   "Comment"
         Caption         =   "Kommentar"
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
      BeginProperty Column05 
         DataField       =   "Commentdate"
         Caption         =   "Commentdate"
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
      BeginProperty Column06 
         DataField       =   "CommentLastedit"
         Caption         =   "CommentLastedit"
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
            Object.Visible         =   -1  'True
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column01 
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column02 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column03 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column04 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column05 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column06 
            Object.Visible         =   0   'False
            ColumnWidth     =   1739,906
         EndProperty
      EndProperty
   End
   Begin VB.Label Label2 
      Caption         =   "Label2"
      Height          =   255
      Left            =   4560
      TabIndex        =   11
      Top             =   5040
      Width           =   975
   End
   Begin VB.Label Label1 
      Caption         =   "Velg rapportdato:"
      Height          =   255
      Left            =   120
      TabIndex        =   1
      Top             =   120
      Width           =   1335
   End
End
Attribute VB_Name = "bankterminal_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False

Private Sub bankrappsokcmd_Click()
On Error GoTo errhandler
    lpgnorge.rskvittering_pr_dag.Filter = "reporttext like '%" & bankrappsoktekst.Text & "%'"
    DataGrid1.Refresh
Exit Sub
errhandler:
Pumpekontroll.errorlist.AddItem "bankrappsokcmd:" & Err.Number & " " & Err.Description

End Sub

Private Sub bankrappsoknullstill_Click()
On Error GoTo errhandler

lpgnorge.rskvittering_pr_dag.Filter = ""
DataGrid1.Refresh
Exit Sub
errhandler:
Pumpekontroll.errorlist.AddItem "bankrappsonullstill:" & Err.Number & " " & Err.Description

End Sub

Private Sub DataGrid1_RowColChange(LastRow As Variant, ByVal LastCol As Integer)
On Error GoTo errhandler

RichTextBox1.Text = DataGrid1.Columns("Tekst").Text
TxtComment.Text = DataGrid1.Columns("Kommentar").Text

Exit Sub
errhandler:
Pumpekontroll.errorlist.AddItem "datagrid1_kvitteringer:" & Err.Number & " " & Err.Description

End Sub

Private Sub filtrer_Click()
On Error GoTo errhandler
100010 If lpgnorge.rskvittering_pr_dag.State = 1 Then lpgnorge.rskvittering_pr_dag.Close
100011 lpgnorge.kvittering_pr_dag Day(DTPicker1.Value), Month(DTPicker1.Value), Year(DTPicker1.Value)
100012 Set DataGrid1.DataSource = lpgnorge.rskvittering_pr_dag

100014 DataGrid1.Refresh
Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "Feil bankterminal_form linje: " & Erl & " " & Err.Number & " " & Err.Description
Resume Next
End Sub

Private Sub Form_Load()
On Error GoTo errhandler
DTPicker1.Value = Now()

10000 If lpgnorge.rskvittering_pr_dag.State = 1 Then lpgnorge.rskvittering_pr_dag.Close

10001 lpgnorge.kvittering_pr_dag Day(DTPicker1.Value), Month(DTPicker1.Value), Year(DTPicker1.Value)
10002 Set DataGrid1.DataSource = lpgnorge.rskvittering_pr_dag

10004 DataGrid1.Refresh
Exit Sub

errhandler:
Pumpekontroll.errorlist.AddItem "Feil bankterminal_form linje: " & Erl & " " & Err.Number & " " & Err.Description
Resume Next
End Sub

Private Sub skrivut_Click()
'Set Kvittering.DataMember = lpgnorge.rskvittering_pr_dag
Dim lbl As RptLabel
Load Kvittering
Set lbl = Kvittering.Sections.Item("Section1").Controls.Item("Label1")
lbl.Caption = bankterminal_form.RichTextBox1.Text
Kvittering.Show (1)


End Sub


