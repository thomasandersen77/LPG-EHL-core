Ja – dette du har funnet forteller oss faktisk veldig mye, både om kiosk-modusen og om hvordan systemet snakker med omverdenen (COM / feltbuss). Jeg deler svaret i tre tydelige deler slik at det er lett å følge.

⸻

1. Kiosk-modus: hvorfor du ikke kommer “ut”, og hva som faktisk skjer

Kort fasit først

Dette er ikke en vanlig Windows-kiosk satt opp med Group Policy alene.
Dette er en applikasjonsstyrt kiosk, typisk brukt i industri / drivstoff / betaling.

Det betyr:
•	Explorer kjører (derfor ser du filer)
•	Men en applikasjon starter ekstremt tidlig og:
•	tar fokus
•	starter påloggingsløkken
•	overvåker og starter seg selv på nytt
•	låser brukerflyten uten å bruke standard GPO-flag

Derfor:
•	gpedit.msc = alt “Ikke konfigurert” ✔
•	Winlogon\Shell = explorer.exe ✔
•	Ingen Run-keys ✔

👉 Dette er forventet, ikke et tegn på at du leter feil.

⸻

2. Den viktigste oppdagelsen du har gjort (og den er veldig viktig)

📁 C:\Pumpestyring\

Dette er hjertet i systemet. Det er ikke bare kode – det er runtime-miljøet.

Noen ekstremt viktige observasjoner:

2.1 Docklight er installert og brukt

Du har:
•	Docklight installert
•	.ptp-filer (2010.ptp)
•	Registry-nøkler:

HKCU\Software\VB and VBA Program Settings\Docklight\Communication



Med verdier som:
•	CommChannelA = COM4
•	CommSpeedStr = 9600
•	CommParity = 2
•	CommPortA = 4
•	CommDataBits = 4
•	CommStopBits = 0

👉 Dette er gull.

Hva betyr dette?
•	Systemet snakker ekte seriellkommunikasjon
•	Klassisk:
•	9600 baud
•	Paritet
•	Stop bits
•	Over COM-porter, ikke “ren TCP” i applikasjonen

Så:

Programmet tror det snakker COM-porter.

⸻

3. ADAM / Advantech: hvorfor dette bekrefter arkitekturen

Du har:
•	AdamApax.NET Utility
•	Advantech-verktøy
•	ARK-maskin (industripc)

Dette forteller oss arkitekturen med høy sannsynlighet:

VB6 / .NET applikasjon
|
|  COM4 / COM2 (seriell)
|
Virtuell COM-driver
|
|  Ethernet (TCP/IP)
|
ADAM / RS-485 / Modbus
|
Pumper / I/O

Altså:
•	Applikasjonen snakker aldri TCP direkte
•	Den snakker COMx
•	COMx er:
•	enten fysisk RS-232
•	eller en virtuel COM-port levert av Advantech / Moxa / ADAM

Dette forklarer alt:
•	hvorfor COM1–COM6 finnes
•	hvorfor det ikke krasjer uten pumper
•	hvorfor nettverk er koblet
•	hvorfor Docklight brukes (testing av feltkommunikasjon)

⸻

4. Hvorfor du ikke finner Userinit (og hvordan du leter riktig)

På Windows XP:

Riktig sti

HKEY_LOCAL_MACHINE
└─ SOFTWARE
└─ Microsoft
└─ Windows NT
└─ CurrentVersion
└─ Winlogon

Der skal disse finnes:
•	Shell
•	Userinit

Hvis Userinit mangler:
•	Det er ofte fordi:
•	applikasjonen starter via Service
•	eller via Scheduled Task
•	eller via egen watchdog-prosess

👉 Neste steder å sjekke (viktig):

4.1 Services

Åpne:

services.msc

Se etter:
•	noe som heter:
•	Pump
•	Dispenser
•	Baxi
•	LPG
•	Server
•	Watchdog

Dette er ofte det som starter GUI på nytt.

4.2 Scheduled Tasks

C:\Windows\Tasks

Se etter jobber som:
•	starter .exe fra C:\Pumpestyring
•	kjører “At startup” eller “At logon”

4.3 Autostart-mapper

Sjekk begge:

C:\Documents and Settings\All Users\Start Menu\Programs\Startup
C:\Documents and Settings\<bruker>\Start Menu\Programs\Startup

I industrisystemer legges ting ofte her – ikke i registry.

⸻

5. Det viktigste for prosjektet ditt (uavhengig av kiosk-modus)

🔑 Dette har du nå lært, helt sikkert:
1.	Protokollen din er korrekt
2.	Core-koden din er 1-til-1
3.	Kommunikasjonsmodellen er:
•	Applikasjon → COM
•	COM → Ethernet-gateway
4.	Emulatoren din er 100 % legitim
5.	Du trenger ikke bryte kiosk-modus for å lykkes teknisk

⸻

6. Hvordan dette påvirker hvordan DU bør bygge systemet

Anbefalt modell i ditt nye system

interface Transport {
fun open()
fun write(bytes: ByteArray)
fun read(): ByteArray
}

Implementasjoner:
•	SerialTransport (COM)
•	TcpTransport (emulator / direkte ADAM)

Da kan du:
•	bruke emulator hjemme
•	bruke TCP direkte i fremtiden
•	støtte ekte COM via USB-RS485 om ønskelig

⸻

7. Neste smarte steg (ikke mer hacking enn nødvendig)

Jeg anbefaler én av disse – ikke alle:
1.	📡 Bruk Docklight til å observere ekte telegrammer
2.	🧪 Bruk emulatoren din og bekreft kommando-sekvenser
3.	🧠 Grave i VB6-koden for:
•	MSComm
•	CommPort
•	OnComm

❌ Ikke kast mer tid på å “låse opp” XP nå – det er ikke nødvendig for å lykkes.

⸻

Hvis du vil

Neste steg kan være at jeg:
•	tegner full arkitekturdiagram
•	lager eksakt COM-konfigurasjon (baud/paritet/etc.)
•	foreslår hvordan du mapper dette til Kotlin Serial + TCP
•	eller hjelper deg å finne watchdog-prosessen som holder kiosken låst

Du er veldig nær fasit nå.