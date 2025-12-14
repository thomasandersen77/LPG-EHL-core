Attribute VB_Name = "Module1"

            Case 16
            chksum = 0
            For i = 0 To u - 2
                chksum = chksum Xor x(i)
            Next
            If Check1.Value = 1 Then
            If x(2) <> Int(Ladr.Text) Then Exit Sub
            End If
            
            cmdDISPTX.AddItem "chksum: " & chksum
            
            If chksum = x(u - 1) Then
                        
                        COM_id = COM_id + 1
                        commandtext = commandtext & "-->" & COM_id
                        cmdDISPTX.AddItem Now() & " --" & commandtext
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & Now() & "--" & commandtext
                        commandtext = ""
                    Select Case x(3)
                        Case 105           '(BLOCK) Stop the dispenser
                            
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H7
                                y(3) = Int(x(2))
                                y(4) = &H4B
                                y(5) = Int(givestate.Text)
                                y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
                                y(7) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 37            '(ERROR) Error code data
           ' Label10.Caption = "Error"
                        Case 106           '(LINETEST) Transmission channel test
                         
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H8
                                y(3) = Int(x(2))
                                y(4) = &H6A
                                y(5) = &H55
                                y(6) = &HAA
                                y(7) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6)
                                y(8) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7) & ";" & y(8)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                 If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 30           '(OK) Command acknowledgement
            'Label10.Caption = "OK"
                        Case 92        '(PRICE) Give / take the fuel price
                        If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &HA
                                y(3) = Int(x(2))
                                y(4) = &H5C
                                
                                y(5) = Asc(Mid(DisPris.Text, 1, 1))
                                y(6) = Asc(Mid(DisPris.Text, 2, 1))
                                y(7) = Asc(Mid(DisPris.Text, 4, 1))
                                y(8) = Asc(Mid(DisPris.Text, 5, 1))
                                y(9) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8)
                                y(10) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7) & ";" & y(8) & ";" & y(9) & ";" & y(10)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                                    
                        Case 112            '(Prog_I) Programming fuel amount to delivery
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H6
                                y(3) = Int(x(2))
                                y(4) = &H1E
                                y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                y(6) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                                                Case 117           '(Prog_W) Programming fuel value to delivery
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H6
                                y(3) = Int(x(2))
                                y(4) = &H1E
                                y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                y(6) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                 If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                                                
                        Case 169           '(PROG_PRC) Programming of fuel price
                           If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H6
                                y(3) = Int(x(2))
                                y(4) = &H1E
                                y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                y(6) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
                                commandtext = commandtext & "-->" & COM_id
                                CMDDISPRX.AddItem commandtext
                                DisPris.Text = Chr(x(7)) & Chr(x(6)) & "." & Chr(x(5)) & Chr(x(4))
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 75           '(STATE) Give / take the calculator state
                        If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H7
                                y(3) = Int(x(2))
                                y(4) = &H4B
                                y(5) = Int(givestate.Text)
                                y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
                                y(7) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
                                
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7)
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        
                        Case 47          '(STOP) Stop the dispenser
                        
                        Case 119           '(UNBLOCK) Start delivery mode
                        
                        
                        If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H7
                                y(3) = Int(x(2))
                                y(4) = &H4B
                                y(5) = Int(givestate.Text)
                                y(6) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5)
                                y(7) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7)
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                                         
                        Case 69         '(VOLUME) Give / take the fuel amount
                        
                        Case 121
                        If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &HE
                                y(3) = Int(x(2))
                                y(4) = &H79
                                y(5) = &H31
                                y(6) = &H31
                                y(7) = &H31
                                y(8) = &H31
                                y(9) = &H31
                                y(10) = &H32
                                y(11) = &H32
                                y(12) = &H32
                                y(13) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8) Xor y(9) Xor y(10) Xor y(11) Xor y(12)
                                y(14) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7) & ";" & y(8) & ";" & y(9) & ";" & y(10) & ";" & y(11) & ";" & y(12) & ";" & y(13) & ";" & y(14)
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 133
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H10
                                y(3) = Int(x(2))
                                y(4) = &H85
                                y(5) = Asc("0") 'sw0
                                y(6) = Asc("0") 'sw1
                                y(7) = Asc("0") 'sw2
                                y(8) = Asc("0") 'sw3
                                y(9) = Asc("0") 'sw4
                                y(10) = Asc("0") 'sw5
                                y(11) = Asc("0") 'sw6
                                y(12) = Asc("0") 'Lan_l
                                y(13) = Asc("0") 'Lan_m
                                y(14) = Asc("0") 'Lan_H
                                y(15) = y(1) Xor y(2) Xor y(3) Xor y(4) Xor y(5) Xor y(6) Xor y(7) Xor y(8) Xor y(9) Xor y(10) Xor y(11) Xor y(12) Xor y(13) Xor y(14)
                                y(16) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6)) + Chr(y(7)) + Chr(y(8)) + Chr(y(9)) + Chr(y(10)) + Chr(y(11)) + Chr(y(12)) + Chr(y(13)) + Chr(y(14)) + Chr(y(15)) + Chr(y(16))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6) & ";" & y(7) & ";" & y(8) & ";" & y(9) & ";" & y(10) & ";" & y(11) & ";" & y(12) & ";" & y(13) & ";" & y(14) & ";" & y(15) & ";" & y(16)
                                CMDDISPRX.AddItem commandtext
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 129           '(ZER) Reset the calculator
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H6
                                y(3) = Int(x(2))
                                y(4) = &H1E
                                y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                y(6) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
                                CMDDISPRX.AddItem commandtext
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 195
                         If emulator.Value = 1 Then
                            If ((Int(x(2)) = Int(padr.Text)) Or (Int(x(2)) = Int(ladr2.Text))) Then
                                y(1) = &H20
                                y(2) = &H6
                                y(3) = Int(x(2))
                                y(4) = &H1E
                                y(5) = y(1) Xor y(2) Xor y(3) Xor y(4)
                                y(6) = &H36
                                MSComm1.Output = Chr(y(1)) + Chr(y(2)) + Chr(y(3)) + Chr(y(4)) + Chr(y(5)) + Chr(y(6))
                                commandtext = Now() & "--" & y(1) & ";" & y(2) & ";" & y(3) & ";" & y(4) & ";" & y(5) & ";" & y(6)
                                CMDDISPRX.AddItem commandtext
                                commandtext = commandtext & "-->" & COM_id
                                If logger.Value = 1 Then Print #1, "Fra dispenser(emulator):" & commandtext
                                commandtext = ""
                            End If
                        
                        Else
                        If logger.Value = 1 Then Print #1, "Fra Control/interface:" & commandtext
                        End If
                        
                        Case 197            '(Tank,Btank)
            
                        Case Else
                        If logger.Value = 1 Then Print #1, "Fra control, ikke implementert:" & commandtext

                    End Select      'Select..case..end hva er kommandoen?
            u = -1
            commandtext = ""    'rydder opp etter vellykket endt behandling av commandstring, dog kanskje ikke funnet en funksjon for dette.
            
            Else
            cmdDISPTX.AddItem "CHKSUM ER FEIL!  UGYLDIG KOMMANDO: " & chksum
            u = -1
            commandtext = ""
            
            End If      'If..end ;Er chksum autentisk??
 
