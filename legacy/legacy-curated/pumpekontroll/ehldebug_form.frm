VERSION 5.00
Begin VB.Form ehldebug_form 
   Caption         =   "EHL Debug"
   ClientHeight    =   8385
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   12000
   LinkTopic       =   "Form1"
   ScaleHeight     =   8385
   ScaleWidth      =   12000
   StartUpPosition =   3  'Windows Default
   Begin VB.CommandButton Command2 
      Caption         =   "Lukk"
      Height          =   495
      Left            =   9360
      TabIndex        =   7
      Top             =   7800
      Width           =   2535
   End
   Begin VB.CommandButton Command1 
      Caption         =   "Clear"
      Height          =   495
      Left            =   5040
      TabIndex        =   2
      Top             =   7800
      Width           =   3375
   End
   Begin VB.ListBox ehl_rx 
      Height          =   7080
      Left            =   6120
      TabIndex        =   1
      Top             =   480
      Width           =   5655
   End
   Begin VB.ListBox ehl_tx 
      Height          =   7080
      Left            =   240
      TabIndex        =   0
      Top             =   480
      Width           =   5655
   End
   Begin VB.Label Label3 
      Caption         =   "tankstate"
      Height          =   375
      Left            =   2880
      TabIndex        =   6
      Top             =   7680
      Width           =   1455
   End
   Begin VB.Label State 
      Height          =   375
      Left            =   360
      TabIndex        =   5
      Top             =   7680
      Width           =   1575
   End
   Begin VB.Label Label2 
      Caption         =   "RX"
      Height          =   255
      Left            =   6240
      TabIndex        =   4
      Top             =   120
      Width           =   975
   End
   Begin VB.Label Label1 
      Caption         =   "TX"
      Height          =   255
      Left            =   240
      TabIndex        =   3
      Top             =   120
      Width           =   975
   End
End
Attribute VB_Name = "ehldebug_form"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Command1_Click()
ehl_tx.Clear
ehl_rx.Clear

End Sub

Private Sub Command2_Click()
Unload ehldebug_form

End Sub
