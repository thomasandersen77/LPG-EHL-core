#!/usr/bin/env python3
"""
ECR SERVER V14 - LOW AMOUNT EDITION
====================================
Strategi:
  1. Keep-Alive: Svar alltid på '00 00' med '00 00' (Grønn ECR)
  2. Handshake : Send LOGON ([01]) først
  3. Action    : Send RESERVASJON ([03]) på 2.00 NOK (200 øre)

Teori: Terminalen kan være konfigurert for "Pay at Pump" (AFD) og forventer
       Pre-auth (Reservasjon) i stedet for direkte Kjøp.
       
Testing med kun 2 kroner er trygt for lommeboka! 💰
"""

import socket
import time
import struct
import sys

# KONFIGURASJON
HOST = '0.0.0.0'
PORT = 8009

# BELØP Å RESERVERE (I ØRE)
# 200 øre = 2.00 Kroner
RESERVATION_AMOUNT = 200 

def create_packet(payload_str):
    """Pakker inn Verifone-kommando med 2-byte lengde-header"""
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    # Pack length as 2 bytes, Big Endian (Network Order)
    header = struct.pack('!H', length) 
    return header + payload_bytes

def start_server():
    print("="*70)
    print("⛽ ECR SERVER V14 - LOW AMOUNT EDITION")
    print("="*70)
    print("Strategi:")
    print("  1. Keep-Alive: Svar alltid på '00 00' med '00 00'.")
    print("  2. Handshake : Send LOGON ([01]) først.")
    print(f"  3. Action    : Send RESERVASJON ([03]) på {RESERVATION_AMOUNT/100:.2f} NOK.")
    print("="*70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print(f"✅ Listening on {HOST}:{PORT}")
        
        while True:
            print("\n⏳ Waiting for terminal...")
            conn, addr = s.accept()
            with conn:
                print(f"📱 TERMINAL CONNECTED from {addr}")
                
                # Tilstander
                logon_sent = False
                reservation_sent = False
                
                while True:
                    try:
                        conn.settimeout(10.0)
                        
                        # LES HEADER (2 bytes)
                        header = conn.recv(2)
                        if not header: break
                        
                        msg_len = struct.unpack('!H', header)[0]
                        
                        # --- HÅNDTER HEARTBEAT (Lengde 0) ---
                        if msg_len == 0:
                            sys.stdout.write(".")
                            sys.stdout.flush()
                            
                            # SVAR PÅ PING! (Viktig for Grønn ECR)
                            conn.sendall(b'\x00\x00')
                            
                            # LOGIKK: Hva er neste steg?
                            
                            # Steg 1: Vi må logge på
                            if not logon_sent:
                                time.sleep(0.5)
                                print("\n🔑 Sender LOGON ([01;1])...")
                                cmd = '[01;1]'
                                conn.sendall(create_packet(cmd))
                                print(f"🚀 TX: {cmd}")
                                logon_sent = True
                            
                            # Steg 2: Hvis vi har logget på, prøv Reservasjon
                            elif logon_sent and not reservation_sent:
                                # Vent litt lenger (2 sek) for å la Logon synke inn
                                time.sleep(2.0)
                                print(f"\n⛽ Sender RESERVASJON på {RESERVATION_AMOUNT/100:.2f} NOK ([03])...")
                                
                                # Format: [03;Seq;Beløp;Moms;Cashback]
                                # Beløp er i øre
                                cmd = f'[03;2;{RESERVATION_AMOUNT};0;0]'
                                
                                conn.sendall(create_packet(cmd))
                                print(f"🚀 TX: {cmd}")
                                reservation_sent = True
                                
                            continue

                        # --- HÅNDTER DATA (Lengde > 0) ---
                        payload = conn.recv(msg_len)
                        text = payload.decode('iso-8859-1', errors='ignore')
                        print(f"\n📥 RX: {text}")

                        # Svar på Tekst-Ping [00] også (bare for sikkerhets skyld)
                        if '[00]' in text:
                            conn.sendall(create_packet('[00]'))

                        # Sjekk respons
                        if 'A;' in text or text.startswith('[A'):
                            print(f"🎉 Svar mottatt: {text}")
                            
                            if reservation_sent:
                                print("\n✅ TERMINAL HAR SVART PÅ RESERVASJON!")
                                print("👉 Sjekk skjermen nå! Ber den om kort?")
                                print("👉 Beløp skal være 2,00 kr.")

                        if 'FEIL' in text:
                            print(f"⚠️ Terminal melder feil: {text}")
                            
                    except socket.timeout:
                        print("\n⏰ Timeout. Sender Heartbeat probe...")
                        conn.sendall(b'\x00\x00')
                    except Exception as e:
                        print(f"\n❌ Error: {e}")
                        break
                
                print("\n🔌 Connection closed")
                time.sleep(1)

if __name__ == "__main__":
    start_server()
