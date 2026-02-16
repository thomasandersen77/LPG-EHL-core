# Kjør Integrert Betalingstest

## Rask Start

### Med SIMULERT terminal (anbefalt først)

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core

java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  no.cloudberries.lpg.payment.IntegratedPaymentDemo
```

### Med EKTE terminal (192.168.0.4:8009)

⚠️ **ADVARSEL**: Dette vil tappe kortet ditt maksimalt 3 øre!

```bash
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  no.cloudberries.lpg.payment.IntegratedPaymentDemo --real
```

## Hva skjer

1. **Dispenser-emulator** starter (1 øre/liter)
2. **Betalingsterminal** kobler til
3. **Fylling** startes (simuleres i 2 sekunder = ~3 liter)
4. **Fylling** stoppes
5. **Volum og pris** leses (max 3 øre)
6. **Betaling** sendes til terminal
7. **Bekreftelse** mottas
8. **Dispenser** resettes

## Forventet Output

```
=== Integrated Dispenser + Payment Demo ===

Price: 1 øre/liter
Max delivery: 3 liters = 3 øre

1. Setting up dispenser emulator...
   ✓ Dispenser ready (1 øre/liter)

2. Setting up payment terminal...
   ✓ Terminal connected

3. Starting delivery...
   ✓ Dispenser unblocked

4. Delivery in progress...

5. Stopping delivery...
   ✓ Delivery stopped
   Volume: ~3 liters
   Amount: 3 øre (0.03 kr)

6. Processing payment...
   Amount to charge: 3 øre

--- Payment Result ---
Status:         APPROVED
Amount:         3 øre (0.03 kr)
Transaction ID: [ID]

✓ PAYMENT APPROVED!

7. Resetting dispenser...
   ✓ Dispenser reset

✓ Demo complete
```

## Neste steg

Når dette fungerer med simulert terminal, test med ekte terminal:

1. Sørg for at terminalen er på (`192.168.0.4`)
2. Kjør med `--real` flagget
3. Hold kort klar for betaling
4. Maksimalt 3 øre vil bli tappet

## Feilsøking

**Problem**: "Failed to connect to terminal"
- Sjekk at terminal er på og koblet til nettverk
- Verifiser IP: `ping 192.168.0.4`
- Sjekk port: `nc -zv 192.168.0.4 8009`

**Problem**: "Unsupported command"
- Dette er normalt for kommandoer emulatoren ikke støtter ennå
- Kjerneflyt vil fortsatt fungere
