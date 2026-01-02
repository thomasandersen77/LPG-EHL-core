# Start ECR Server

## Quick Start

Terminalen viser "Ikke kontakt med kasse" fordi den prøver å koble til kassesystemet (ECR) men får ikke svar.

**Løsning:** Kjør ECR-serveren som simulerer et kassesystem.

## Kjør serveren

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl/lpg-ehl-core
./run-ecr-server.sh
```

Serveren vil:
1. Starte på port 8009
2. Vente på tilkobling fra terminalen
3. Svare på meldinger fra terminalen
4. Logge all kommunikasjon

## Hva skjer når terminalen kobler til

Serveren vil vise:
```
=== ECR Server for Payment Terminal ===

Starting ECR server on port 8009...
✓ ECR Server is running
Waiting for terminal connection on port 8009...

Payment terminal connected from 192.168.0.4:xxxxx
Terminal session started
Received XX bytes from terminal
  HEX:   [binary data]
Terminal sent [message type]
Sent ACK to terminal
```

## Neste steg

Når terminalen har koblet til:

1. **Se på meldingene** - Serveren logger all kommunikasjon
2. **Observer terminalens respons** - Endrer den status?
3. **Rapporter tilbake** - Si hva terminalen sender, så kan vi implementere riktig protokoll

## Stoppe serveren

Trykk `Ctrl+C` for å stoppe.

## Feilsøking

**Problem:** Port 8009 already in use
**Løsning:** Lukk andre programmer som bruker porten, eller kjør på annen port:
```bash
./run-ecr-server.sh 8010
```

**Problem:** Terminalen kobler ikke til
**Løsning:** 
1. Sjekk at Mac-en din har IP `192.168.0.41`
2. Restart terminalen
3. Sjekk terminalens ECR-konfigurasjon (admin-meny)
