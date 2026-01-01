# Kjør Test med Ekte Terminal

## ⚡ Rask Start

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./test-ekte-betaling.sh
```

## 🎯 Hva skjer

1. Programmet kobler til terminalen (`192.168.0.4:8009`)
2. Sender betalingsforespørsel for **3 øre**
3. Ber deg tappe kortet
4. Venter på bekreftelse (maks 60 sek)
5. Viser resultat

## 📊 Output

Programmet viser:
- ✅ All kommunikasjon i HEX og ASCII
- 📥 Data fra terminalen
- 🔔 Når du skal tappe kortet
- ✓ Resultat (godkjent/avvist)

## ⚠️ Viktig

- Beløp: Maks 3 øre (0.03 kr)
- Hold kortet klart
- Programmet venter i 60 sekunder

## 💡 Tips

**Første gang**: Se på outputen for å forstå protokollen
**Deretter**: Vi kan implementere riktig ECR-protokoll basert på responser
