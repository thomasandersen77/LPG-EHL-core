Ja – la oss gjøre “gravejobben”. Jeg har gått gjennom det du har lastet opp (VB6-zip, Python-zip, samt ModbusTCP.xml og analysen din).

1) Funn: Jeg fant ikke konkrete Modbus-adresser i VB6-/Python-koden

I de opplastede VB6- og Python-zipene finner jeg ikke kall/konstanter som tydelig viser “coil X = pump enable”, “holding register Y = …” osv. Det kan bety én av disse:
•	Modbus-adressene ligger i et annet prosjekt (f.eks. et separat “ADAM/IO”-program, service, eller en annen VB6-løsning enn den du lastet opp).
•	Adressene ligger i runtime-konfig på XP-maskinen (ini/xml/registry), ikke i kildekoden.
•	VB6/Python i zipene er primært EHL/pumpelogikk, mens ADAM/Modbus-delen var løst via DLL + en annen komponent.

Det jeg kan dokumentere fra filene du lastet opp er at ModbusTCP.dll-APIet (via ModbusTCP.xml) er en Modbus TCP master som støtter standardoperasjoner (coils, discrete inputs, holding/input registers) og bruker typisk TCP port 502.  ￼  ￼

2) Hva vi faktisk kan utlede fra ModbusTCP.xml

Det er ikke et registerkart, men det forteller deg nøyaktig “språket” den gamle løsningen snakket:
•	Den bruker Unit identifier (slave-id) + startAddress + lengde, og kan lese/skriv både coils og registre.  ￼
•	Den støtter blant annet:
•	ReadCoils, ReadDiscreteInputs, ReadHoldingRegister, ReadInputRegister  ￼  ￼
•	WriteSingleCoils, WriteMultipleCoils, WriteSingleRegister, WriteMultipleRegister  ￼  ￼

Dette støtter hypotesen din: ADAM-boksen er sannsynligvis “power/IO”-laget (releer, inputsignaler), mens EHL er “pumpelogikk/data”.

⸻

3) Så hvordan finner vi de riktige adressene i praksis?

Siden registerkartet ikke ligger åpent i koden du lastet opp, er den raskeste og mest robuste veien:

A) Finn registerkartet fra ADAM-modellen (best)

Hvis du får modellnummer (f.eks. ADAM-4050, 4060, 4150, 6050 osv), finnes det nesten alltid et fast Modbus register-/coil-oppsett i manualen. Da slipper du “gjetting”.

B) “Probe”/scan i lab (nest best – men funker)

Bygg et lite verktøy som:
1.	Kobler til ADAM (IP:502)
2.	Leser et fornuftig område med:
•	coils (0xxxx)
•	discrete inputs (1xxxx)
•	input registers (3xxxx)
•	holding registers (4xxxx)
3.	Logger alt og diff’er endringer mens dere:
•	trykker start/stop i UI
•	simulerer “nozzle lifted”
•	simulerer “emergency stop”
•	ser hva som endrer seg

Det du ser etter er:
•	Coil som, når den settes true, fysisk frigir (rele/power enable).
•	Discrete input som gjenspeiler f.eks. “pump ready”, “fault”, “flow switch”.
•	Registre som kan være status/bitmasker.

Viktig: Mange ADAM-moduler bruker coil-adresser 0–N (0-basert) i API, mens dokumentasjon ofte viser 1-basert “00001”. Derfor trenger du et lag som håndterer offset/normalisering.

⸻

4) Første versjon av AdamRegisterMap.yml (basert på “unknown → discovered”)

Dette er en “tom men strukturert” map du kan fylle mens du scanner:

adam:
host: "192.168.0.50"
port: 502
unitId: 1

addressing:
# Mange libs bruker 0-basert addressing internt
base: 0

outputs:
# Disse fylles inn når vi har identifisert riktig coil
pump1_enable:
type: coil
address: null
pump1_block:
type: coil
address: null
pump2_enable:
type: coil
address: null

inputs:
pump1_ready:
type: discrete_input
address: null
pump1_fault:
type: discrete_input
address: null

registers:
# hvis ADAM eksponerer status/bitmasker i holding/input regs
pump1_status_word:
type: input_register
address: null
pump1_fault_word:
type: input_register
address: null


⸻

5) Hva du bør programmere nå (konkret)

Du trenger 2 ting i Kotlin:

(1) “AdamPowerController” (produksjonskode)

Et lite interface i lpg-ehl-core (fordi det er domene-nært: “start/stop/block/unblock” er en del av pumpeflyten), men implementasjonen kan ligge i egen modul.

Interface (core):
•	enablePump(pumpId)
•	disablePump(pumpId)
•	blockPump(pumpId)
•	readInputs() / readStatus()

(2) “modbus-scanner” CLI (lab-verktøy)

En liten CLI som kan:
•	lese ranges (coils/inputs/registers)
•	toggle en coil (med “are you sure?”-flag i CLI)
•	logge til fil for diff

Dette er det som faktisk vil gi dere adressene raskt.

⸻

6) Hvor bør Maven/Gradle-modulen ligge i arkitekturen din?

Slik jeg ville gjort det i ditt repo:
•	lpg-ehl-core
•	Legg kun interface + modeller her (f.eks. AdamPowerController, AdamStatus, RegisterMap).
•	Ny modul: lpg-ehl-modbus
•	Implementasjon + biblioteker (modbus master)
•	Leser AdamRegisterMap.yml
•	Valgfritt: lpg-ehl-modbus-sim
•	Modbus slave simulator (for lab)

Dette matcher samme mønster du allerede har med transport/emulator-konseptet.

⸻

7) Kan du simulere med socat?
   •	For Modbus TCP: ikke nødvendig – det er bare TCP socket (port 502). socat kan fortsatt brukes til å route/redirecte TCP, men det gir deg ikke en Modbus slave.
   •	For Modbus RTU (serial): ja, da kan socat være nyttig for å lage pseudo-tty og teste “serial-lag”.

Siden ModbusTCP.dll eksplisitt er TCP (og nevner port 502), er den mest sannsynlige simuleringen en Modbus TCP slave i en egen prosess/container.  ￼  ￼

⸻

8) Anbefaling: hva jeg ville gjort som “neste commits”
    1.	Lag lpg-ehl-modbus med en enkel ModbusClient (master) som kan:
          •	readCoils(start, len)
          •	writeSingleCoil(address, onOff)
          •	readDiscreteInputs(start, len)
    2.	Lag modbus-scan CLI:
          •	scan --coils 0..64 --di 0..64 --ir 0..64 --hr 0..64
          •	toggle --coil 12 --on (med “–force”)
    3.	Innfør AdamPowerController i core og wire det inn i din eksisterende pump/state-machine slik at:
          •	“Start fueling” → (A) enable relay via ADAM → (B) EHL open/start
          •	“Stop” → (A) EHL stop → (B) disable relay
    4.	I lab: Kjør scanning mens du toggler “start/stop” og se hvilke bits/inputs som endrer seg → fyll inn AdamRegisterMap.yml.

⸻

Hvis du vil, kan jeg i neste steg skrive en helt konkret “terminal-prompt” (bash) som:
•	oppretter lpg-ehl-modbus-modulen,
•	legger inn forslag til pakker/interfaces,
•	lager en CLI modbus-scan,
•	og setter opp en enkel docker-compose med en Modbus TCP slave-simulator (så dere kan teste mot noe før felten).

Bare si om du vil gå for modbus4j-stil (vanlig i Java/Kotlin) eller om du allerede har preferanse på bibliotek.