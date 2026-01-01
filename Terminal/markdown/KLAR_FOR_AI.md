# ✅ KLAR FOR AI-OPPLASTING

## 📂 Mappe opprettet

**Lokasjon**: `/Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-for-ai`

**Antall filer**: 63 (godt under 1000-grensen)

## 📤 Hva skal lastes opp

### 1. Mappen
Dra hele mappen `lpg-ehl-for-ai` til Gemini eller ChatGPT

### 2. Bilder
Bilder av terminalen fra:
- `/Users/tandersen/Downloads/IMG_1612.HEIC` (eller .png)
- `/Users/tandersen/Downloads/IMG_1613.HEIC`
- `/Users/tandersen/Downloads/IMG_1614.HEIC`
- `/Users/tandersen/Downloads/IMG_1615.HEIC`
- `/Users/tandersen/Downloads/IMG_1616.HEIC`

## 💬 Spørsmål til AI

Kopier og lim inn:

```
Jeg prøver å integrere en betalingsterminal med mitt LPG dispenser-system.

PROBLEM:
Terminalen går inn i "velg vare"-modus når jeg putter kortet inn, 
i stedet for å be om korttapping. Dette betyr at vi sender feil 
ECR-protokoll.

OBSERVASJONER:
- Terminal IP: 192.168.0.4:8009
- Vi kan koble til terminalen (TCP socket fungerer)
- Terminal svarer med "F" (0x46) på våre meldinger
- Når kort puttes inn: "velg vare" vises
- Når kort tas ut: "avbrutt av kunde"

VEDLAGT:
1. lpg-ehl-for-ai/ - Komplett Kotlin-kildekode (63 filer)
2. Bilder av terminalen (for identifikasjon)
3. OPPSUMMERING_FOR_AI.md i mappen (les den først!)

SPØRSMÅL:
1. Hvilken terminal-modell er dette? (se bilder)
2. Hvilken ECR-protokoll bruker den? (ZVT, Nets, OPI, annet?)
3. Hvordan skal betalingskommandoen se ut? (eksakt byte-sekvens)
4. Hvordan initialiserer jeg forbindelsen? (handshake)
5. Hvordan tolker jeg respons? (status-koder)

TEKNISK:
- Kode: Kotlin med TCP sockets
- Port: 8009 (ECR standard)
- Jeg har en lytter som kan fange all kommunikasjon
- Testbeløp: 3 øre (0.03 kr)

Vennligst gi konkrete Kotlin-eksempler for å implementere 
riktig protokoll.
```

## 🎯 Hva AI skal svare

AI bør gi:
1. ✓ Terminal-identifikasjon (merke/modell)
2. ✓ Protokoll-type (ZVT, Nets, etc.)
3. ✓ Konkret Kotlin-kode for betalingskommando
4. ✓ Byte-sekvenser i HEX
5. ✓ Respons-parsing-logikk

## 📝 Etter AI-svar

1. Implementer i `TcpPaymentTerminal.kt`
2. Test med: `./test-ekte-betaling.sh`
3. Hvis det ikke fungerer: Kjør `./lytt-til-terminal.sh` og send logg til AI

## 🔄 Hvis du trenger å oppdatere mappen

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./lag-ai-mappe.sh
```

Dette vil:
- Slette gammel mappe
- Lage ny med oppdatert kode
- Fortsatt kun 63 filer

---

**Alt er klart! Last opp nå! 🚀**
