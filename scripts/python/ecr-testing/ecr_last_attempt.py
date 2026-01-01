#!/usr/bin/env python3
import socket
import struct
import sys
import time
from datetime import datetime

HOST = '0.0.0.0'
PORT = 8009
AMOUNT = 200  # 2.00 NOK

def create_packet(payload_str):
    payload_bytes = payload_str.encode('iso-8859-1')
    length = len(payload_bytes)
    header = struct.pack('!H', length)
    return header + payload_bytes

def read_response(conn, timeout=5):
    try:
        conn.settimeout(timeout)
        while True:
            header = conn.recv(2)
            if not header:
                return None
            msg_len = struct.unpack('!H', header)[0]
            if msg_len == 0:
                sys.stdout.write(".")
                sys.stdout.flush()
                conn.sendall(b'\x00\x00')
                continue
            payload = conn.recv(msg_len)
            return payload.decode('iso-8859-1', errors='replace')
    except socket.timeout:
        return None
    except Exception as e:
        print(f"\n[ERROR] {e}")
        return None

def start_server():
    print("\n" + "="*70)
    print("🏁 ECR SERVER - SISTE FORSØK")
    print("="*70)
    print(f"Tid: {datetime.now().strftime('%H:%M:%S')}")
    print(f"Lytter på: {HOST}:{PORT}")
    print(f"Beløp: {AMOUNT/100:.2f} kr")
    print("-" * 70)
    
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        
        print("\n⏳ Venter på terminal...")
        conn, addr = s.accept()
        
        with conn:
            print(f"\n✅ TERMINAL KOBLET TIL: {addr}")
            print("Etablerer forbindelse", end="")
            
            # Heartbeats
            for _ in range(3):
                try:
                    conn.settimeout(5.0)
                    header = conn.recv(2)
                    if not header: break
                    msg_len = struct.unpack('!H', header)[0]
                    if msg_len == 0:
                        sys.stdout.write(".")
                        sys.stdout.flush()
                        conn.sendall(b'\x00\x00')
                except:
                    break
            
            print(" OK!\n")
            print("="*70)
            
            # Send KJØP
            cmd = f'P;10;1;{AMOUNT};0'
            print(f"\n📤 SENDER KJØP-KOMMANDO: {cmd}")
            print(f"   (Kjøp, sekvensnr 1, {AMOUNT/100:.2f} kr, ingen cashback)\n")
            
            conn.sendall(create_packet(cmd))
            
            print("="*70)
            print("🚨 NÅ MÅ DU SETTE KORTET I TERMINALEN! 🚨")
            print("="*70)
            print("Instruksjoner:")
            print("1. Sett kortet i terminalen NÅ")
            print("2. Tast PIN hvis terminalen ber om det")
            print("3. Vent til det kommer kvittering eller feilmelding")
            print("="*70)
            print()
            
            # Lytt i 2 MINUTTER for å få ALLE meldinger
            print("🔊 Lytter etter svar fra terminal (120 sekunder)...\n")
            
            start_time = time.time()
            response_count = 0
            last_response_time = start_time
            
            while (time.time() - start_time) < 120:
                resp = read_response(conn, timeout=3)
                
                if resp:
                    response_count += 1
                    last_response_time = time.time()
                    timestamp = datetime.now().strftime('%H:%M:%S')
                    
                    print(f"[{timestamp}] 📥 SVAR #{response_count}: {resp}")
                    print(f"         Type: ", end="")
                    
                    # Analyser svaret
                    if 'A000' in resp:
                        if 'FEIL' in resp:
                            print("❌ FEILMELDING")
                        elif 'Timeout' in resp:
                            print("⏱️  TIMEOUT (ikke satt kort i tide?)")
                        else:
                            print("✅ ACCEPT KODE - KAN VÆRE SUKSESS!")
                    elif 'D!' in resp:
                        print("⏳ DIALOG/VENTER")
                    elif resp.startswith('[00'):
                        print("✅ STATUS OK")
                    elif 'godkjent' in resp.lower() or 'approved' in resp.lower():
                        print("🎉 GODKJENT!")
                    elif 'avvist' in resp.lower() or 'declined' in resp.lower():
                        print("❌ AVVIST")
                    else:
                        print("❓ UKJENT")
                    
                    print()
                    
                    # Hvis vi får et "endelig" svar, vent litt ekstra for å se om det kommer mer
                    if any(x in resp for x in ['A000', 'godkjent', 'avvist', 'approved', 'declined']):
                        if 'FEIL' not in resp and 'Timeout' not in resp:
                            print("⏱️  Venter 5 sek for å se om det kommer flere meldinger...")
                            time.sleep(5)
                else:
                    # Hvis ingen svar på 20 sekunder, anta at vi er ferdige
                    if response_count > 0 and (time.time() - last_response_time) > 20:
                        print("\n⏱️  Ingen flere svar på 20 sekunder. Avslutter.\n")
                        break
            
            print("\n" + "="*70)
            print(f"📊 OPPSUMMERING")
            print("="*70)
            print(f"Totalt {response_count} svar mottatt")
            print(f"Tid brukt: {int(time.time() - start_time)} sekunder")
            
            if response_count == 0:
                print("\n❌ INGEN SVAR FRA TERMINAL")
                print("   Mulige årsaker:")
                print("   - Terminalen er ikke konfigurert for ECR")
                print("   - Feil protokoll")
                print("   - Kommandoformat er feil")
            
            print("="*70)
            print("\n✅ Test fullført!")

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("\n\n🛑 Avbrutt av bruker")
    except Exception as e:
        print(f"\n\n❌ FEIL: {e}")
