# Last opp til Gemini/ChatGPT

## 🚀 Rask start

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./pakk-for-ai.sh
```

Dette lager en ZIP-fil med kun kildekode og konfigurasjon.

## 📦 Hva skal lastes opp

### 1. Koden (ZIP-fil)
- Kjør `./pakk-for-ai.sh`
- Finner filen i parent-directory
- Navn: `lpg-ehl-core-YYYYMMDD-HHMMSS.zip`

### 2. Oppsummering
- Fil: `OPPSUMMERING_FOR_AI.md`
- Inneholder: Komplett teknisk beskrivelse

### 3. Terminal-bilder
- Fra `/Users/tandersen/Downloads/IMG_161*.HEIC`
- Konvertert til PNG hvis nødvendig

## 💬 Spørsmål til AI

### Til Gemini/ChatGPT:

```
Jeg prøver å integrere en betalingsterminal med mitt LPG dispenser-system.

PROBLEM:
Terminalen går inn i "velg vare"-modus i stedet for å be om korttapping.
Dette betyr at vi sender feil ECR-protokoll.

VEDLAGT:
1. lpg-ehl-core-*.zip - Komplett kildekode
2. OPPSUMMERING_FOR_AI.md - Detaljert beskrivelse
3. Terminal-bilder - For identifikasjon

SPØRSMÅL:
1. Hvilken terminal-modell er dette? (se bilder)
2. Hvilken ECR-protokoll bruker den? (ZVT, Nets, OPI?)
3. Hvordan sender jeg riktig betalingskommando?
4. Hvilken byte-sekvens forventer terminalen?
5. Hvordan tolker jeg respons fra terminalen?

Koden er i Kotlin og bruker TCP sockets (192.168.0.4:8009).
Jeg har implementert en lytter som kan fange all kommunikasjon.
```

## 🎯 Forventet svar

AI bør kunne:
1. ✓ Identifisere terminal-type fra bilder
2. ✓ Foreslå korrekt ECR-protokoll
3. ✓ Gi konkret Kotlin-kode for betalingskommando
4. ✓ Forklare handshake-sekvens
5. ✓ Vise hvordan parse respons

## 📊 Etter AI-svar

Implementer protokollen:
1. Oppdater `TcpPaymentTerminal.kt`
2. Legg til riktig protokoll-encoding
3. Test med `./test-ekte-betaling.sh`
4. Verifiser at terminal ber om korttapping

## 💡 Tips

**Hvis AI trenger mer info:**
- Kjør `./lytt-til-terminal.sh`
- Kopier ALL output
- Send tilbake til AI

**Hvis AI foreslår bibliotek:**
- Legg til i `pom.xml`
- Kjør `mvn dependency:copy-dependencies`
- Importer i koden

## ⚡ Rask referanse

```bash
# Pakk for AI
./pakk-for-ai.sh

# Lytt til terminal
./lytt-til-terminal.sh

# Test implementasjon
./test-ekte-betaling.sh

# Build etter endringer
mvn clean compile
```

## 📝 Viktig informasjon

**Nettverkskonfig:**
- Terminal: 192.168.0.4:8009
- Mac: 192.168.0.41
- Vi kan koble til, men sender feil protokoll

**Testbeløp:**
- Maks 3 øre (0.03 kr)
- Trygt for testing

**Dokumentasjon:**
- All kode er dokumentert med KDoc
- README.md inneholder overview
- WARP.md inneholder utvikler-guide
