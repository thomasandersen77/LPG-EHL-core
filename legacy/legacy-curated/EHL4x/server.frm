VERSION 5.00
Begin VB.Form serverinnstillinger 
   BorderStyle     =   4  'Fixed ToolWindow
   Caption         =   "Server innstillinger"
   ClientHeight    =   3384
   ClientLeft      =   6600
   ClientTop       =   4584
   ClientWidth     =   4260
   LinkTopic       =   "Form1"
   MaxButton       =   0   'False
   MinButton       =   0   'False
   ScaleHeight     =   3384
   ScaleWidth      =   4260
   ShowInTaskbar   =   0   'False
   Begin VB.TextBox txtcom_bank 
      Height          =   285
      Left            =   1800
      TabIndex        =   11
      Text            =   "0"
      Top             =   2520
      Width           =   1455
   End
   Begin VB.TextBox txtcom_port 
      Height          =   285
      Left            =   1800
      TabIndex        =   9
      Top             =   2040
      Width           =   1455
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Lagre"
      Height          =   255
      Left            =   1440
      TabIndex        =   8
      Top             =   3000
      Width           =   1095
   End
   Begin VB.TextBox serverpassord 
      Height          =   285
      Left            =   1800
      TabIndex        =   3
      Top             =   1560
      Width           =   1455
   End
   Begin VB.TextBox serverbrukernavn 
      Height          =   285
      Left            =   1800
      TabIndex        =   2
      Top             =   1080
      Width           =   1455
   End
   Begin VB.TextBox serverdb 
      Height          =   285
      Left            =   1800
      TabIndex        =   1
      Top             =   600
      Width           =   2295
   End
   Begin VB.TextBox servernavn 
      Height          =   285
      Left            =   1800
      TabIndex        =   0
      Top             =   120
      Width           =   2295
   End
   Begin VB.Label Label6 
      Caption         =   "Comport bank"
      Height          =   252
      Left            =   120
      TabIndex        =   12
      Top             =   2520
      Width           =   1212
   End
   Begin VB.Label Label2 
      Caption         =   "Comport dispenser :"
      Height          =   252
      Left            =   120
      TabIndex        =   10
      Top             =   2040
      Width           =   1572
   End
   Begin VB.Label Label5 
      Caption         =   "DATABASE :"
      Height          =   255
      Left            =   120
      TabIndex        =   7
      Top             =   600
      Width           =   1215
   End
   Begin VB.Label Label4 
      Caption         =   "SQL BRUKERNAVN:"
      Height          =   255
      Left            =   120
      TabIndex        =   6
      Top             =   1080
      Width           =   1575
   End
   Begin VB.Label Label3 
      Caption         =   "SQL PASSORD :"
      Height          =   255
      Left            =   120
      TabIndex        =   5
      Top             =   1560
      Width           =   1215
   End
   Begin VB.Label Label1 
      Caption         =   "SQL SERVER :"
      Height          =   255
      Left            =   120
      TabIndex        =   4
      Top             =   120
      Width           =   1215
   End
End
Attribute VB_Name = "serverinnstillinger"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit


Private Sub Command1_Click()
Open App.Path & "\server.ini" For Output As #1
Print #1, Me.servernavn & ";" & Me.serverdb & ";" & Me.serverbrukernavn & ";" & Me.serverpassord & ";" & Me.txtcom_port & ";" & Me.txtcom_bank
DBserver = Me.servernavn
DBdb = Me.serverdb
DBbrukernavn = Me.serverbrukernavn
DBpassord = Me.serverpassord
Com_port = Me.txtcom_port
Com_port_bank = Me.txtcom_bank
Close #1
Unload Me


End Sub

Private Sub Form_Load()
Dim txtline()

Open App.Path & "\server.ini" For Output As #1
txtline = Split()



End Sub
