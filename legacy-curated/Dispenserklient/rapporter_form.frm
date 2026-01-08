VERSION 5.00
Object = "{3B7C8863-D78F-101B-B9B5-04021C009402}#1.2#0"; "richtx32.ocx"
Object = "{86CF1D34-0C5F-11D2-A9FC-0000F8754DA1}#2.0#0"; "MSCOMCT2.OCX"
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Begin VB.Form bankterminal_form 
   Caption         =   "Rapporter"
   ClientHeight    =   6915
   ClientLeft      =   2505
   ClientTop       =   2100
   ClientWidth     =   8430
   LinkTopic       =   "Form1"
   ScaleHeight     =   6915
   ScaleWidth      =   8430
   Begin VB.CommandButton filtrer 
      Caption         =   "Hent transer"
      Height          =   375
      Left            =   3960
      TabIndex        =   2
      Top             =   60
      Width           =   855
   End
   Begin MSComCtl2.DTPicker DTPicker1 
      Height          =   375
      Left            =   2040
      TabIndex        =   0
      Top             =   60
      Width           =   1815
      _ExtentX        =   3201
      _ExtentY        =   661
      _Version        =   393216
      Format          =   16449537
      CurrentDate     =   40612
   End
   Begin RichTextLib.RichTextBox RichTextBox1 
      Height          =   6135
      Left            =   4320
      TabIndex        =   3
      Top             =   600
      Width           =   3975
      _ExtentX        =   7011
      _ExtentY        =   10821
      _Version        =   393217
      TextRTF         =   $"rapporter_form.frx":0000
   End
   Begin MSDataGridLib.DataGrid DataGrid1 
      Bindings        =   "rapporter_form.frx":0082
      Height          =   6135
      Left            =   120
      TabIndex        =   4
      Top             =   600
      Width           =   3975
      _ExtentX        =   7011
      _ExtentY        =   10821
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
      Caption         =   "Hendelser denne dag"
      ColumnCount     =   4
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
         Caption         =   "Hendelsetype"
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
         Caption         =   "reporttext"
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
         DataField       =   "reportid"
         Caption         =   "Reportid"
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
            Object.Visible         =   -1  'True
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
      EndProperty
   End
   Begin VB.Label Label1 
      Caption         =   "Velg rapportdato:"
      Height          =   255
      Left            =   120
      TabIndex        =   1
      Top             =   120
      Width           =   1815
   End
End
Attribute VB_Name = "bankterminal_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False

Private Sub DataGrid1_RowColChange(LastRow As Variant, ByVal LastCol As Integer)
RichTextBox1.Text = DataGrid1.Columns("reporttext").Text
End Sub

Private Sub filtrer_Click()
On Error Resume Next
If lpgnorge.rsdbo_kvittering_pr_dag.State = 1 Then lpgnorge.rsdbo_kvittering_pr_dag.Close

lpgnorge.dbo_kvittering_pr_dag Day(DTPicker1.Value), Month(DTPicker1.Value), Year(DTPicker1.Value)
Set DataGrid1.DataSource = lpgnorge.rsdbo_kvittering_pr_dag
'lpgnorge.rsdbo_kvittering_pr_dag.Requery
DataGrid1.Refresh

End Sub

Private Sub Form_Load()
On Error Resume Next
DTPicker1.Value = Now()

If lpgnorge.rsdbo_kvittering_pr_dag.State = 1 Then lpgnorge.rsdbo_kvittering_pr_dag.Close

lpgnorge.dbo_kvittering_pr_dag Day(DTPicker1.Value), Month(DTPicker1.Value), Year(DTPicker1.Value)
Set DataGrid1.DataSource = lpgnorge.rsdbo_kvittering_pr_dag
'lpgnorge.rsdbo_kvittering_pr_dag.Requery
DataGrid1.Refresh


End Sub

