VERSION 5.00
Begin VB.Form innstillinger 
   Caption         =   "Innstillinger"
   ClientHeight    =   4560
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   4680
   LinkTopic       =   "Form3"
   ScaleHeight     =   4560
   ScaleWidth      =   4680
   StartUpPosition =   2  'CenterScreen
   Begin VB.CommandButton lagrelukk 
      Caption         =   "Lagre/Lukk"
      Height          =   375
      Left            =   1560
      TabIndex        =   16
      Top             =   3960
      Width           =   1695
   End
   Begin VB.TextBox txtpassordbutikkdata 
      Height          =   375
      Left            =   2760
      TabIndex        =   14
      Top             =   3480
      Width           =   1575
   End
   Begin VB.TextBox txtdbbutikkdata 
      Height          =   375
      Left            =   2760
      TabIndex        =   12
      Top             =   3000
      Width           =   1575
   End
   Begin VB.TextBox txtdbserverbutikkdata 
      Height          =   375
      Left            =   2760
      TabIndex        =   10
      Top             =   2520
      Width           =   1575
   End
   Begin VB.TextBox txtpassorddbbetterm 
      Height          =   375
      IMEMode         =   3  'DISABLE
      Left            =   2760
      PasswordChar    =   "*"
      TabIndex        =   8
      Top             =   2040
      Width           =   1575
   End
   Begin VB.TextBox txtdbbetterm 
      Height          =   375
      Left            =   2760
      TabIndex        =   6
      Top             =   1560
      Width           =   1575
   End
   Begin VB.TextBox txtdbserverbetterm 
      Height          =   375
      Left            =   2760
      TabIndex        =   4
      Top             =   1080
      Width           =   1575
   End
   Begin VB.TextBox txtipadressekamera 
      Height          =   375
      Left            =   2760
      TabIndex        =   2
      Top             =   600
      Width           =   1575
   End
   Begin VB.TextBox txtipadresseklient 
      Height          =   375
      Left            =   2760
      TabIndex        =   0
      Top             =   120
      Width           =   1575
   End
   Begin VB.Label Label8 
      Caption         =   "Passord databaseserver butikkdata"
      Height          =   255
      Left            =   120
      TabIndex        =   15
      Top             =   3600
      Width           =   2655
   End
   Begin VB.Label Label7 
      Caption         =   "Database butikkdata:"
      Height          =   255
      Left            =   120
      TabIndex        =   13
      Top             =   3120
      Width           =   2655
   End
   Begin VB.Label Label6 
      Caption         =   "Databaseserver butikkdata :"
      Height          =   255
      Left            =   120
      TabIndex        =   11
      Top             =   2640
      Width           =   2655
   End
   Begin VB.Label Label5 
      Caption         =   "Passord databaseserver betterm"
      Height          =   255
      Left            =   120
      TabIndex        =   9
      Top             =   2160
      Width           =   2655
   End
   Begin VB.Label Label4 
      Caption         =   "Database betterm :"
      Height          =   255
      Left            =   120
      TabIndex        =   7
      Top             =   1680
      Width           =   2655
   End
   Begin VB.Label Label3 
      Caption         =   "Databaseserver betterm :"
      Height          =   255
      Left            =   120
      TabIndex        =   5
      Top             =   1200
      Width           =   2655
   End
   Begin VB.Label Label2 
      Caption         =   "IPadresse kamera :"
      Height          =   255
      Left            =   120
      TabIndex        =   3
      Top             =   720
      Width           =   2655
   End
   Begin VB.Label Label1 
      Caption         =   "IPadresse betalingsterminalterminal :"
      Height          =   255
      Left            =   120
      TabIndex        =   1
      Top             =   240
      Width           =   2655
   End
End
Attribute VB_Name = "innstillinger"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Form_Load()
If Dir(App.Path & "\settings.ini") <> "" Then
Open App.Path & "\settings.ini" For Input As #1
Input #1, textline
     cfgline() = Split(textline, ";")
     Me.txtipadresseklient.Text = cfgline(0)
     Me.txtipadressekamera.Text = cfgline(1)
     Me.txtdbserverbetterm.Text = cfgline(2)
     Me.txtdbbetterm.Text = cfgline(3)
     Me.txtpassorddbbetterm.Text = cfgline(4)
     Me.txtdbserverbutikkdata.Text = cfgline(5)
     Me.txtdbbutikkdata.Text = cfgline(6)
     Me.txtpassordbutikkdata.Text = cfgline(7)
    Close #1
Else
End If

End Sub


Private Sub lagrelukk_Click()
Open App.Path & "\settings.ini" For Output As #1
Print #1, Me.txtipadresseklient.Text & ";" & Me.txtipadressekamera.Text & ";" & Me.txtdbserverbetterm.Text & ";" & Me.txtdbbetterm.Text & ";" & Me.txtpassorddbbetterm.Text & ";" & Me.txtdbserverbutikkdata.Text & ";" & Me.txtdbbutikkdata.Text & ";" & Me.txtpassordbutikkdata.Text
    Close #1
    MsgBox " Programmet må restartes før endringene vil ha virkning. Avslutter nå.", vbOKOnly, "Dispenserklient"
    End
End Sub
