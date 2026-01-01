#!/usr/bin/env python3
"""
Konverterer HTML til PDF ved å bruke macOS sin innebygde funksjonalitet
"""
import subprocess
import sys
import os

def html_to_pdf_macos(html_file, pdf_file):
    """Konverterer HTML til PDF via macOS"""
    
    if not os.path.exists(html_file):
        print(f"❌ Feil: {html_file} finnes ikke")
        return False
    
    print(f"📄 Konverterer {html_file} → {pdf_file}")
    
    # Metode 1: Via Safari/WebKit headless
    try:
        # Bruk macOS sin /usr/bin/textutil hvis tilgjengelig
        result = subprocess.run([
            '/usr/bin/textutil',
            '-convert', 'html',
            '-output', '/tmp/temp.html',
            html_file
        ], capture_output=True, text=True)
        
        if result.returncode == 0:
            # Så konverter til PDF via cups
            result2 = subprocess.run([
                '/usr/bin/cupsfilter',
                '-m', 'application/pdf',
                '/tmp/temp.html'
            ], capture_output=True)
            
            if result2.returncode == 0:
                with open(pdf_file, 'wb') as f:
                    f.write(result2.stdout)
                print(f"✅ PDF opprettet: {pdf_file}")
                return True
    except Exception as e:
        print(f"Metode 1 feilet: {e}")
    
    # Metode 2: Bruk osascript til å be Safari om å lage PDF
    try:
        apple_script = f'''
        tell application "Safari"
            activate
            open POSIX file "{os.path.abspath(html_file)}"
            delay 2
            tell application "System Events"
                keystroke "p" using command down
                delay 1
                keystroke return
            end tell
        end tell
        '''
        
        print("⚠️  Metode 1 fungerte ikke. Forsøker manuell konvertering...")
        print("   Åpner HTML i Safari - du må selv trykke Cmd+P og lagre som PDF")
        
        subprocess.run(['open', '-a', 'Safari', html_file])
        return None
        
    except Exception as e:
        print(f"Kunne ikke åpne Safari: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Bruk: python3 html_to_pdf.py <input.html> <output.pdf>")
        sys.exit(1)
    
    html_file = sys.argv[1]
    pdf_file = sys.argv[2]
    
    result = html_to_pdf_macos(html_file, pdf_file)
    
    if result is False:
        sys.exit(1)
    elif result is None:
        print("\n💡 Manuell konvertering:")
        print(f"   1. Safari åpner {html_file}")
        print("   2. Trykk Cmd+P (eller File → Print)")
        print("   3. Velg 'Save as PDF' nederst til venstre")
        print(f"   4. Lagre som {pdf_file}")
