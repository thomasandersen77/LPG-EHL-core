VERSION 5.00
Object = "{67397AA1-7FB1-11D0-B148-00A0C922E820}#6.0#0"; "MSADODC.OCX"
Object = "{3B7C8863-D78F-101B-B9B5-04021C009402}#1.2#0"; "richtx32.ocx"
Begin VB.Form Firmainfo 
   Caption         =   "Firmainformasjon"
   ClientHeight    =   4725
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   4830
   LinkTopic       =   "Form1"
   ScaleHeight     =   4725
   ScaleWidth      =   4830
   StartUpPosition =   3  'Windows Default
   Begin VB.CommandButton Command1 
      Caption         =   "Lagre"
      Height          =   735
      Left            =   840
      TabIndex        =   8
      Top             =   3960
      Width           =   3375
   End
   Begin RichTextLib.RichTextBox RichTextBox1 
      DataField       =   "Firmanavn"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   0
      Top             =   120
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":0000
   End
   Begin MSAdodcLib.Adodc firmainf 
      Height          =   330
      Left            =   1440
      Top             =   3600
      Visible         =   0   'False
      Width           =   1920
      _ExtentX        =   3387
      _ExtentY        =   582
      ConnectMode     =   0
      CursorLocation  =   3
      IsolationLevel  =   -1
      ConnectionTimeout=   15
      CommandTimeout  =   30
      CursorType      =   1
      LockType        =   3
      CommandType     =   2
      CursorOptions   =   0
      CacheSize       =   50
      MaxRecords      =   0
      BOFAction       =   0
      EOFAction       =   0
      ConnectStringType=   1
      Appearance      =   1
      BackColor       =   -2147483643
      ForeColor       =   -2147483640
      Orientation     =   0
      Enabled         =   -1
      Connect         =   $"Firmainfo.frx":008B
      OLEDBString     =   $"Firmainfo.frx":0113
      OLEDBFile       =   ""
      DataSourceName  =   ""
      OtherAttributes =   ""
      UserName        =   "sa"
      Password        =   "lpg01"
      RecordSource    =   "Firmainfo"
      Caption         =   "Adodc1"
      BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
         Name            =   "MS Sans Serif"
         Size            =   8.25
         Charset         =   0
         Weight          =   400
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      _Version        =   393216
   End
   Begin RichTextLib.RichTextBox RichTextBox2 
      DataField       =   "Firmapoststed"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   1080
      TabIndex        =   1
      Top             =   1080
      Width           =   3375
      _ExtentX        =   5953
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":019B
   End
   Begin RichTextLib.RichTextBox RichTextBox3 
      DataField       =   "Firmapostnr"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   2
      Top             =   1080
      Width           =   855
      _ExtentX        =   1508
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":0226
   End
   Begin RichTextLib.RichTextBox RichTextBox4 
      DataField       =   "Firmaadresse"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   3
      Top             =   600
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":02B1
   End
   Begin RichTextLib.RichTextBox RichTextBox6 
      DataField       =   "aapningstider"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   4
      Top             =   3000
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":033C
   End
   Begin RichTextLib.RichTextBox RichTextBox7 
      DataField       =   "Orgnr"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   5
      Top             =   2520
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":03C7
   End
   Begin RichTextLib.RichTextBox RichTextBox8 
      DataField       =   "Epost"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   6
      Top             =   2040
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":0452
   End
   Begin RichTextLib.RichTextBox RichTextBox9 
      DataField       =   "Telefonnummer"
      DataSource      =   "firmainf"
      Height          =   375
      Left            =   120
      TabIndex        =   7
      Top             =   1560
      Width           =   4335
      _ExtentX        =   7646
      _ExtentY        =   661
      _Version        =   393217
      Enabled         =   -1  'True
      TextRTF         =   $"Firmainfo.frx":04DD
   End
End
Attribute VB_Name = "Firmainfo"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()
firmainf.Recordset.Update
MsgBox "Informasjon oppdatert..Programmet må startes på nytt."
Unload Me


End Sub

Private Sub Form_Initialize()
firmainf.ConnectionString = "Provider=SQLOLEDB.1;Integrated Security=SSPI;Persist Security Info=False;Initial Catalog=" & DBdb & ";Data Source=" & DBserver
End Sub

