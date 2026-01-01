#!/usr/bin/env python3
"""
ECR Server V9 - FRAMED EDITION
Uses proper length-prefixed packet format (2-byte header + payload)
This is THE missing piece!
"""

import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

def create_packet(payload_str):
    """
    Pakker inn meldingen med en 2-byte header som sier hvor lang den er.
    Dette er nøkkelen!
    Format: [Length (2 bytes)][Payload]
    """
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    # Pack length as 2 bytes, Big Endian (Network Order)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("💳 ECR SERVER V9 - FRAMED EDITION")
    print("="*70)
    print()
    print("Strategi: Send 'Lengde' (2 bytes) + 'Kommando'.")
    print("          Dette er lengde-prefikset pakkeformat!")
    print()
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        print()
        
        while True:
            print("⏳ Waiting for terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"\n{'='*70}")
                print(f"📱 TERMINAL CONNECTED from {addr[0]}:{addr[1]}")
                print(f"{'='*70}")
                print()
                
                purchase_sent = False
                
                print("👂 Reading length-prefixed packets...")
                print()
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        
                        # Les først 2 bytes (Header = Length)
                        header = conn.recv(2)
                        
                        if not header or len(header) < 2: 
                            print("\n🔌 Terminal closed connection")
                            break
                        
                        # Dekod lengden (Big Endian unsigned short)
                        msg_len = struct.unpack('!H', header)[0]
                        
                        if msg_len == 0:
                            # 00 00 = Heartbeat (Empty message)
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            # Svar med heartbeat tilbake (viktig!)
                            conn.sendall(b'\x00\x00')
                            
                            if not purchase_sent:
                                print("\n\n⚡ Heartbeat mottatt! Sender FRAMED PURCHASE...")
                                print()
                                print(f"{'─'*70}")
                                print("💰 SENDING LENGTH-PREFIXED PURCHASE COMMAND")
                                print(f"{'─'*70}")
                                print()
                                
                                # Kommandoen inni klammene (Verifone ECR format)
                                cmd_text = '[10;1;100;0;0]'
                                
                                # Pakk den inn med lengde-header
                                full_packet = create_packet(cmd_text)
                                
                                conn.sendall(full_packet)
                                
                                hex_display = ' '.join(f'{b:02X}' for b in full_packet)
                                print(f"🚀 TX (Hex): {hex_display}")
                                print(f"   Header: {full_packet[:2].hex()} (Length = {len(cmd_text)})")
                                print(f"   Payload: {cmd_text}")
                                print()
                                print("⏳ Waiting for terminal response...")
                                print("   💡 Terminal should light up NOW!")
                                print()
                                purchase_sent = True
                                
                        else:
                            # Vi fikk faktisk data! Les resten basert på lengden
                            print(f"\n📨 Receiving message (length: {msg_len} bytes)...")
                            
                            payload = conn.recv(msg_len)
                            
                            if len(payload) < msg_len:
                                print(f"⚠️  Incomplete message: got {len(payload)}/{msg_len} bytes")
                                continue
                            
                            text = payload.decode('iso-8859-1', errors='ignore')
                            hex_display = ' '.join(f'{b:02X}' for b in payload)
                            
                            print(f"\n📥 RX: {text}")
                            print(f"   Hex: {hex_display}")
                            print()
                            
                            # Answer/Receipt
                            if 'A;' in text or text.startswith('A'):
                                print("="*70)
                                print("🎉🎉🎉 SVAR! TERMINALEN SVARTE! 🎉🎉🎉")
                                print("="*70)
                                print(f"Terminal Response: {text}")
                                print()
                                
                                if 'FEIL' not in text.upper():
                                    print("✅✅✅ SUKSESS! KJØPET ER GODKJENT! ✅✅✅")
                                else:
                                    print("⚠️  Feil format, men vi kommuniserer!")
                                print()
                            
                            # Display meldinger
                            if 'D!' in text:
                                display_msg = text.replace('D!', '').strip()
                                print(f"📺 DISPLAY: {display_msg}")
                                print()
                            
                            # Feilmeldinger
                            if 'FEIL' in text.upper():
                                print("⚠️  Terminal klager på format, men vi kommuniserer!")
                                print("   Dette betyr vi er på riktig protokoll!")
                                print()
                            
                            # Bracket kommandoer
                            if text.startswith('['):
                                print(f"ℹ️  Bracket command: {text}")
                                
                                if '[20;' in text:
                                    print("   ✅ Success code!")
                                elif '[99;' in text:
                                    print("   ❌ Error code")
                                print()

                    except socket.timeout:
                        print("\n⏰ Timeout.")
                        if purchase_sent:
                            print("   Purchase already sent, no response yet")
                        print()
                        break
                    
                    except struct.error as e:
                        print(f"\n❌ Packet format error: {e}")
                        break
                    
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        import traceback
                        traceback.print_exc()
                        break
                
                print()
                print("="*70)
                print("🔌 Connection ended")
                print("="*70)
                print()
                time.sleep(1)

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n⚠️  Server stopped by user")
        print("✅ Goodbye!")
