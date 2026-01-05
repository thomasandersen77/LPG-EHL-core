# Test med Ekte Betalingsterminal

## 🎯 Hva dette gjør

Programmet kobler til den EKTE betalingsterminalen og:

1. Kobler til `192.168.0.4:8009`
2. Sender betalingsforespørsel for **3 øre** (0.03 kr)
3. Venter på at du tapper kortet
4. Logger all kommunikasjon
5. Viser resultat

## 🚀 Kjør testen

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./test-ekte-betaling.sh
```

## 📋 Hva du ser

```
══════════════════════════════════════════════════
  INTERAKTIV BETALINGSDEMO MED EKTE TERMINAL
══════════════════════════════════════════════════

Terminal: 192.168.0.4:8009
Beløp: 3 øre (0.03 kr)

⚠️  Du må tappe kortet når terminalen ber om det!

1. Kobler til terminal...
   ✓ Tilkoblet!

2. Venter på terminal...
   Terminal sendte: X bytes
   HEX:   [binary data]
   ASCII: [text hvis leselig]

3. Sender betalingsforespørsel: 3 øre
   → Sendt tekstmelding: PAYMENT:3
   → Sendt binær melding:
      HEX:   [binary data]
      ASCII: [text hvis leselig]

4. Venter på respons fra terminal...

╔═══════════════════════════════════════════════╗
║                                               ║
║   🔔 TAPPE KORTET NÅ!                         ║
║                                               ║
║   Terminalen skal be om betaling             ║
║   Hold kortet mot terminalen                 ║
║                                               ║
║   (Venter i maks 60 sekunder...)             ║
║                                               ║
╚═══════════════════════════════════════════════╝

📥 Mottok X bytes fra terminal:
   HEX:   [response data]
   ASCII: [text hvis leselig]

✅ BETALING GODKJENT!
```

## 🔍 Hva programmet gjør

### 1. Kobler til terminal
Åpner TCP-socket til `192.168.0.4:8009`

### 2. Mottar initial melding
Logger første melding fra terminalen (hvis noen)

### 3. Sender betalingsforespørsel
Prøver to forskjellige formater:
- **Tekstformat**: `"PAYMENT:3\n"`
- **Binært format**: BCD-kodet beløp med kommandokode

### 4. Venter på svar
- Leser all data som kommer fra terminalen
- Viser data i både HEX og ASCII
- Søker etter godkjenning/avvisning
- Timeout etter 60 sekunder

## 📊 Mulige resultater

### ✅ Suksess
```
✅ BETALING GODKJENT!
```
Kortet ble tappet og belastet 3 øre.

### ❌ Avvisning
```
❌ BETALING AVVIST!
```
Terminalen eller kortet avviste transaksjonen.

### ⏱️ Timeout
```
⏱️ Timeout - ingen respons fra terminal

Mulige årsaker:
- Terminalen krever spesiell initialisering
- Feil protokoll-format
- Terminalen venter på annet kommando
```

## 🔧 Hva vi lærer

All kommunikasjon logges i detalj. Dette gir oss:

1. **Initieringssekvens** - Hva terminalen sender først
2. **Protokollformat** - Binær eller tekst
3. **Kommandokoder** - Hvilke bytes betyr hva
4. **Responsformat** - Hvordan terminalen svarer
5. **Feilmeldinger** - Hvis noe går galt

Denne informasjonen brukes til å implementere riktig protokoll.

## ⚠️ Viktig

- **Beløp**: Maksimalt 3 øre (0.03 kr) vil bli trukket
- **Timeout**: Programmet venter maks 60 sekunder
- **Logging**: All kommunikasjon logges for analyse
- **Avbryt**: Trykk Ctrl+C for å avbryte

## 🎓 Neste steg

Basert på loggene kan vi:

1. **Identifisere protokoll** - ZVT, Nets, OPI, eller proprietær
2. **Implementere riktig format** - Basert på responser
3. **Lage full integrasjon** - Med alle kommandoer
4. **Teste betalingsflyt** - Fra fylling til betaling

## 📞 Hjelp

Hvis terminalen ikke responderer:
1. Sjekk at den er på og koblet til nettverket
2. Verifiser IP-adresse: `ping 192.168.0.4`
3. Sjekk port: `nc -zv 192.168.0.4 8009`
4. Prøv å restarte terminalen
5. Sjekk terminalkonfigurasjon (admin-meny)
