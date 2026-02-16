VERSION 5.00
Begin VB.Form displiter 
   Caption         =   "dispenserliter"
   ClientHeight    =   3195
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   4680
   LinkTopic       =   "Form1"
   ScaleHeight     =   3195
   ScaleWidth      =   4680
   StartUpPosition =   3  'Windows Default
   Begin VB.TextBox liter 
      Alignment       =   1  'Right Justify
      BeginProperty DataFormat 
         Type            =   1
         Format          =   "#.##"
         HaveTrueFalseNull=   0
         FirstDayOfWeek  =   0
         FirstWeekOfYear =   0
         LCID            =   1044
         SubFormatType   =   0
      EndProperty
      BeginProperty Font 
         Name            =   "Palatino Linotype"
         Size            =   48
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   1095
      Left            =   360
      TabIndex        =   1
      Text            =   "0.00"
      Top             =   240
      Width           =   3975
   End
   Begin VB.Label Label1 
      BeginProperty Font 
         Name            =   "MS Sans Serif"
         Size            =   12
         Charset         =   0
         Weight          =   700
         Underline       =   0   'False
         Italic          =   0   'False
         Strikethrough   =   0   'False
      EndProperty
      Height          =   495
      Left            =   120
      TabIndex        =   0
      Top             =   1680
      Width           =   4455
   End
End
Attribute VB_Name = "displiter"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Private Sub Form_Load()
liter.SelStart = 0
liter.SelLength = Len(liter)
End Sub


Private Sub liter_KeyPress(KeyAscii As Integer)
If KeyAscii = 13 Then

Dispenserkontroll.tcpclient.SendData "<DISP_LITER>;" & liter.Text & ";<SLUTT>"
displiter.Label1.Caption = "Venter på bekreftelse."

End If

End Sub
