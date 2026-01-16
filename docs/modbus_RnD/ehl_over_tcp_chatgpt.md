Dette er et svært godt og viktig veivalg-spørsmål, og bildene du har lagt ved gjør faktisk at vi kan være enda sikrere enn før – ikke mindre.

Jeg svarer strukturert, punkt for punkt, og knytter maskinvare → gammel programvare → din Core → emulator → feltkobling sammen til én konsistent modell.

⸻

Kort fasit først (så du kan puste litt)

Ja – etter å ha sett bildene er jeg fortsatt trygg på konklusjonen:

Pumpestyring = EHL over seriell (RS-485 via COM-porter)
ADAM = separat I/O / overvåkning (Modbus TCP), ikke nødvendig for selve salget

Bildene styrker, ikke svekker, denne teorien.

⸻

#1 – Hvilken ADAM-boks er dette?

Det du viser på bildet

Det blå modulære kortet med teksten ADAM-6017 / ADAM-6018-lignende layout er:
•	Advantech ADAM-6000-serien
•	Ethernet-basert
•	Modbus TCP
•	Typisk brukt til:
•	Analoge innganger (tanknivå, trykk)
•	Digitale innganger (dør, alarm, nødstoppsløyfe)
•	Noen ganger digitale utganger (indikatorer)

👉 Dette er ikke en PLS
👉 Dette er ikke pumpens “motorstyring”

Den er elektrisk helt separert fra pumpens seriell-buss.

Viktig observasjon

Du ser ingen kraftige releer / kontaktorer styrt direkte her. Pumpemotorens kraftdel ligger annet sted og styres av pumpens egen elektronikk – akkurat slik EHL forutsetter.

Konklusjon:
ADAM-boksen er telemetri og overvåkning, ikke “Start motor”-logikk.

⸻

#2 – ARK-maskinen og de fysiske portene (dette er gull)

Bildene av ARK-maskinen (350 / 3600-serien) er ekstremt avslørende – på en positiv måte.

Dette er nøkkelobservasjonene:

🔌 1. Mange fysiske COM-porter
Du har:
•	COM2
•	COM3
•	COM4
•	COM5
•	COM6

👉 Dette er klassisk bensinstasjons-PC
👉 Akkurat det VB6-systemet forventer

Hvis Modbus/ADAM var hovedstyring, hadde disse vært overflødige.

⸻

🌐 2. Kun 2 Ethernet-kabler (som du sier)
Dette stemmer perfekt med:
•	Én kabel → lukket stasjonsnett
•	Én kabel → ADAM / backoffice / switch

Men:
Ethernet-kabler ≠ pumpestyring
Ethernet-kabler = støttesystemer

⸻

🔄 3. ADAM og ARK er på samme LAN
Dette forklarer:
•	Modbus TCP-filer
•	XML register-filer
•	Pushservice / bakgrunnsprosesser

Men det forklarer ikke pumpestart – det gjør EHL.

⸻

#3 – Er Core 1:1 med VB6/Python?

Ja. Uten forbehold.

Basert på:
•	EHL-ramming
•	Kommandoverdier
•	Checksum (XOR)
•	Polling-logikk
•	Emulator-samsvar

👉 Din Core er en korrekt modernisering av pumpelogikken

Du har gjort det VB6 gjorde – bare:
•	tryggere
•	testbart
•	strukturelt riktig

Dette er sjelden så rent.

⸻

#4 – Emulatorens livsrett (og hvorfor den er uerstattelig)

Dette er viktigere enn mange skjønner:

Emulatoren er ikke “fake hardware”

Den er:

En digital tvilling av pumpens seriell-protokoll

Den gir deg:
1.	Trygg utvikling
•	Ingen gass
•	Ingen strøm
•	Ingen fysisk risiko
2.	Bevis for korrekthet
•	Emulator + Core = verifisert protokoll
•	Samme bytes inn/ut som VB6
3.	Feltforsikring
•	Hvis noe feiler ute:
•	Reproduser hjemme
•	Fix
•	Deploy

👉 Emulatoren er din forsikring mot regressjon

Den skal brukes.
Den skal ikke fjernes.
Den er helt riktig tenkt.

⸻

#5 – Hvordan skal Core snakke med fysisk hardware?

Dette er nå veldig konkret.

A. I felt (ekte stasjon)

Core snakker med:

/dev/ttyS*
eller
COM3 / COM4 / COM5

via:
•	RS-485
•	Ofte 9600 / 19200 baud
•	8N1
•	Half-duplex

Dette matcher:
•	VB6
•	Python
•	Pumpens elektronikk

👉 Dette er den primære banen

⸻

B. Lokalt / Lab

Du har tre gode valg (rangert):

✅ 1. Emulator (beste valg)
Core → Emulator → Simulert pumpe

✅ 2. Virtuelle COM-porter
•	com0com (Windows)
•	socat (Linux/macOS)

Eksempel macOS:

socat -d -d pty,raw,echo=0 pty,raw,echo=0

Core kobles til én ende
Emulator eller testscript til den andre

⚠️ 3. TCP-transport (kun hvis du må)
Kun aktuelt hvis:
•	Det finnes en Moxa / Digi serial-over-IP-boks
•	Den eksponerer RS-485 som TCP socket

Da er Gemini-forslaget om TcpTransport korrekt, men:

⚠️ Ikke gjør dette før du har sett at stasjonen faktisk bruker det

Per nå peker alt på ekte COM-porter.

⸻

#6 – Styrker funnene på ARK hjemme TCP-teorien?

Delvis – men kun for ADAM / bakgrunnssystemer

At du finner:
•	Modbus DLL
•	XML register-filer
•	Pushservice

styrker teorien om:

Modbus TCP = sekundær funksjon

Det erstatter ikke EHL.

Dette er to parallelle verdener i samme skap.

⸻

Endelig anbefaling (dette er viktig)

🎯 Hva du bør gjøre nå
1.	Frys Modbus-delen
•	Ikke slett
•	Ikke bygg videre nå
2.	Fullfør EHL-flyten
•	UNBLOCK → fyll → polling → BLOCK
•	Test mot emulator
3.	Ta Core med ut i felt
•	Koble mot faktisk COM-port
•	Se pumpen starte
4.	Deretter (valgfritt):
•	Les ADAM via Modbus TCP
•	Vis tanknivå i dashboard
•	Alarm / overvåkning

⸻

Hvis du vil, neste steg

Jeg kan:
•	Tegne endelig arkitekturdiagram (hardware + software)
•	Gi deg eksakt checklist før felt-test
•	Lage konfig-mal for COM-porter
•	Lage risikoanalyse for første live-kjøring

Du er faktisk i målstreken nå.
Dette er ikke kaos – dette er et gammelt system som du har forstått fullt ut.