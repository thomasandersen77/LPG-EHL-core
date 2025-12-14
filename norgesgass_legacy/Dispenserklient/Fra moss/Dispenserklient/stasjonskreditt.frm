VERSION 5.00
Object = "{86CF1D34-0C5F-11D2-A9FC-0000F8754DA1}#2.0#0"; "MSCOMCT2.OCX"
Object = "{CDE57A40-8B86-11D0-B3C6-00A0C90AEA82}#1.0#0"; "MSDATGRD.OCX"
Begin VB.Form stasjonskreditt 
   Caption         =   "v"
   ClientHeight    =   8040
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   13050
   LinkTopic       =   "Form1"
   ScaleHeight     =   8040
   ScaleWidth      =   13050
   StartUpPosition =   3  'Windows Default
   Begin MSComCtl2.DTPicker datofra 
      Height          =   375
      Left            =   1800
      TabIndex        =   5
      Top             =   120
      Width           =   1335
      _ExtentX        =   2355
      _ExtentY        =   661
      _Version        =   393216
      Format          =   16449537
      CurrentDate     =   40616
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Hent transer"
      Height          =   375
      Left            =   5040
      TabIndex        =   4
      Top             =   120
      Width           =   1095
   End
   Begin MSDataGridLib.DataGrid DataGrid1 
      Bindings        =   "stasjonskreditt.frx":0000
      Height          =   7215
      Left            =   120
      TabIndex        =   0
      Top             =   600
      Width           =   12855
      _ExtentX        =   22675
      _ExtentY        =   12726
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
      DataMember      =   "stasjonskreditt"
      Caption         =   "Tankinger gjort på stasjonskreditt"
      ColumnCount     =   7
      BeginProperty Column00 
         DataField       =   "Datostart"
         Caption         =   "Dato"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   0
            Format          =   "dd.MM.yyyy"
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   0
         EndProperty
      EndProperty
      BeginProperty Column01 
         DataField       =   "Liter"
         Caption         =   "Liter"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   1
            Format          =   "0,00"
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   1
         EndProperty
      EndProperty
      BeginProperty Column02 
         DataField       =   "Pris"
         Caption         =   "Pris"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   1
            Format          =   "0,00"
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   1
         EndProperty
      EndProperty
      BeginProperty Column03 
         DataField       =   "sum"
         Caption         =   "sum"
         BeginProperty DataFormat {6D835690-900B-11D0-9484-00A0C91110ED} 
            Type            =   1
            Format          =   "0,00"
            HaveTrueFalseNull=   0
            FirstDayOfWeek  =   0
            FirstWeekOfYear =   0
            LCID            =   1044
            SubFormatType   =   1
         EndProperty
      EndProperty
      BeginProperty Column04 
         DataField       =   "rabatt"
         Caption         =   "rabatt"
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
         DataField       =   "Kundenavn"
         Caption         =   "Kundenavn"
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
         DataField       =   "Korteier"
         Caption         =   "Korteier"
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
         BeginProperty Column00 
            ColumnWidth     =   1739,906
         EndProperty
         BeginProperty Column01 
            Alignment       =   1
            ColumnWidth     =   1065,26
         EndProperty
         BeginProperty Column02 
            Alignment       =   1
            ColumnWidth     =   1065,26
         EndProperty
         BeginProperty Column03 
            Alignment       =   1
            ColumnWidth     =   1065,26
         EndProperty
         BeginProperty Column04 
            Object.Visible         =   0   'False
            ColumnWidth     =   1065,26
         EndProperty
         BeginProperty Column05 
            ColumnWidth     =   3585,26
         EndProperty
         BeginProperty Column06 
            ColumnWidth     =   3674,835
         EndProperty
      EndProperty
   End
   Begin MSComCtl2.DTPicker datotil 
      Height          =   375
      Left            =   3480
      TabIndex        =   6
      Top             =   120
      Width           =   1335
      _ExtentX        =   2355
      _ExtentY        =   661
      _Version        =   393216
      Format          =   16449537
      CurrentDate     =   40616
   End
   Begin VB.Label Label1 
      Caption         =   "Vis tankinger i tidsrom:"
      Height          =   255
      Left            =   0
      TabIndex        =   3
      Top             =   150
      Width           =   1695
   End
   Begin VB.Label Label2 
      Caption         =   "-"
      Height          =   255
      Left            =   3000
      TabIndex        =   2
      Top             =   150
      Width           =   135
   End
   Begin VB.Label Label5 
      Caption         =   "-"
      Height          =   255
      Left            =   3240
      TabIndex        =   1
      Top             =   240
      Width           =   135
   End
End
Attribute VB_Name = "stasjonskreditt"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()
lpgnorge.rsstasjonskreditt.Filter = "datostart>=" & datofra.Value & " and datostart <=" & datotil.Value


End Sub

Private Sub Form_Load()
datofra.Value = Date
datotil.Value = Date
End Sub
