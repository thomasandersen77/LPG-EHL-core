VERSION 5.00
Begin {78E93846-85FD-11D0-8487-00A0C90DC8A9} Datareport1 
   Bindings        =   "DataReport1.dsx":0000
   Caption         =   "Bankreport"
   ClientHeight    =   7830
   ClientLeft      =   60
   ClientTop       =   345
   ClientWidth     =   7620
   StartUpPosition =   2  'CenterScreen
   _ExtentX        =   13441
   _ExtentY        =   13811
   _Version        =   393216
   _DesignerVersion=   100688210
   BeginProperty Font {0BE35203-8F91-11CE-9DE3-00AA004BB851} 
      Name            =   "Arial"
      Size            =   8.25
      Charset         =   0
      Weight          =   400
      Underline       =   0   'False
      Italic          =   0   'False
      Strikethrough   =   0   'False
   EndProperty
   GridX           =   1
   GridY           =   1
   LeftMargin      =   1440
   RightMargin     =   1440
   TopMargin       =   1440
   BottomMargin    =   1440
   DataMember      =   "salgstall"
   NumSections     =   5
   SectionCode0    =   1
   BeginProperty Section0 {1C13A8E0-A0B6-11D0-848E-00A0C90DC8A9} 
      _Version        =   393216
      Name            =   "ReportHeader"
      Object.Height          =   360
      NumControls     =   0
   EndProperty
   SectionCode1    =   2
   BeginProperty Section1 {1C13A8E0-A0B6-11D0-848E-00A0C90DC8A9} 
      _Version        =   393216
      Name            =   "PageHeader"
      Object.Height          =   360
      NumControls     =   0
   EndProperty
   SectionCode2    =   4
   BeginProperty Section2 {1C13A8E0-A0B6-11D0-848E-00A0C90DC8A9} 
      _Version        =   393216
      Name            =   "salgstall_Detail"
      Object.Height          =   1440
      NumControls     =   0
   EndProperty
   SectionCode3    =   7
   BeginProperty Section3 {1C13A8E0-A0B6-11D0-848E-00A0C90DC8A9} 
      _Version        =   393216
      Name            =   "PageFooter"
      Object.Height          =   360
      NumControls     =   0
   EndProperty
   SectionCode4    =   8
   BeginProperty Section4 {1C13A8E0-A0B6-11D0-848E-00A0C90DC8A9} 
      _Version        =   393216
      Name            =   "ReportFooter"
      Object.Height          =   360
      NumControls     =   0
   EndProperty
End
Attribute VB_Name = "Datareport1"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = False
Attribute VB_PredeclaredId = True
Attribute VB_Exposed = False
Option Explicit

Private Sub DataReport_Initialize()

Set Datareport1.DataSource = rapport_rs
 Datareport1.Sections("Section1").Controls.Item("txtrapport").DataField = rapport_rs.Fields("reporttext").Name

End Sub
