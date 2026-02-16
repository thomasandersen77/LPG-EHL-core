# Lytt til Terminal

## 🎯 Hva dette gjør

Starter en server som **fanger ALL kommunikasjon** fra terminalen.
Dette viser oss nøyaktig hvilken protokoll terminalen bruker.

## 🚀 Kjør

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./lytt-til-terminal.sh
```

## 📊 Hva du ser

```
═══════════════════════════════════════════════
   ECR LISTENER - FANGER TERMINAL-MELDINGER
═══════════════════════════════════════════════

Lytter på port 8009...
⏳ Venter på tilkobling fra terminal...

✅ TILKOBLET!
   Fra: 192.168.0.4:xxxxx

───────────────────────────────────────────────

📥 MELDING #1 (X bytes)

   HEX:   05 01 02 ...
   ASCII: .....
   DEC:   5 1 2 ...
   Type:  ENQ (Enquiry)

   💬 Hva skal vi svare?
   → Sender ACK (0x06)
   ✓ Sendt ACK

📥 MELDING #2 (X bytes)
   ...
```

## 🔍 Hva vi lærer

Fra loggene ser vi:

1. **Første byte** - Kommandotype
   - `0x05` = ENQ (Enquiry)
   - `0x06` = ACK (Acknowledge)
   - `0x02` = STX (Start of Text)
   - `0x10` = DLE (Data Link Escape)

2. **Meldingsformat** - Binær eller tekst

3. **Sekvens** - Hvilken rekkefølge meldinger kommer

4. **Protokoll** - ZVT, Nets, eller proprietær

## ⚡ Neste steg

Når vi vet protokollen, implementerer vi:

1. **Riktig respons** på hver meldingstype
2. **Betalingskommando** i riktig format
3. **Status-polling** for å vente på korttapping
4. **Resultat-parsing** for godkjenning/avvisning

## 💡 Tips

- **La terminalen stå på** - Den sender meldinger automatisk
- **Kopier loggene** - Vi trenger dem for implementering
- **Restart terminal** hvis ingen meldinger kommer
- **Sjekk ECR-modus** i terminalens innstillinger

## ⚠️ Viktig

Dette programmet:
- ✅ Svarer med ACK på alle meldinger
- ✅ Holder forbindelsen oppe
- ❌ Sender IKKE betalingskommando (ennå)
- ❌ Initierer IKKE betaling (ennå)

Vi må først se hva terminalen sender!
