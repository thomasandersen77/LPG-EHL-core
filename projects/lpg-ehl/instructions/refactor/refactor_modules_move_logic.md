# Refactoring-plan: rene moduler med tydelig ansvarsfordeling

## Hensikt
Målet med denne refactoringen er å gjøre modulene **strengt rene** og med **tydelige ansvar**, slik at:
- `lpg-ehl-core` er **100% protokoll/codec** (ingen serial, ingen Spring, ingen IO)
- `lpg-transport` er **100% IO/transport** (serial/TCP-serial/PTTY + jSerialComm, osv.)
- `lpg-ehl-service` er **100% forretningslogikk/use-cases** (orkestrering, transaksjoner, polling, integrasjoner)
- `lpg-ehl-webapp`, `lpg-ehl-app-headless`, `lpg-ehl-cli` er **tynne entrypoints** som bare “wire’r” ting og eksponerer UI/REST/CLI.

Resultatet er at du kan bygge/kjøre:
- GUI (webapp) med React + controllers
- headless “drift” uten web
- CLI for smoke tests / felt-debug  
  …men med **samme Service** og **samme Transport** under.

---

# Steg 0 — Sikkerhet / forberedelser (baseline)

**Prompt til terminal/agent:**
1) Sjekk status, kjør full build og test før flytting (baseline).
2) Lag en “refactor checklist” som du oppdaterer i repo (f.eks. `docs/refactor_transport_cleanup.md`).

```bash
git status
mvn -q -DskipTests=false test


⸻

Steg 1 — Kartlegg “lekkasjer” (hva ligger feil sted)

Mål: Finn alt i lpg-ehl-core som ikke er “ren protokoll”, og alt som er transport/serial i webapp/core.

Prompt til terminal/agent:
Kjør søk og lag en kort rapport (fil/linjer) over:
	•	hvor jSerialComm / SerialPort brukes
	•	hvor SerialPortManager og communication-pakker ligger
	•	hvor EhlOperationsService og “use-case”-aktige tjenester ligger

# Finn serial-avhengigheter
rg -n "jSerialComm|com\.fazecast\.jSerialComm|SerialPort\b" .

# Finn SerialPortManager og communication-pakker
rg -n "SerialPortManager|package .*communication" .

# Finn EhlOperationsService og lignende “use-case” tjenester
rg -n "EhlOperationsService|OperationsService|Orchestrator|UseCase" .

Output: Agenten/terminalen skal nå ha en liste over filstier som skal flyttes i neste steg.

⸻

Steg 2 — Gjør lpg-transport til “sannheten” for transport

Mål: Alt som er serial/TCP-serial/PTTY/IO skal bo i lpg-transport.

Prompt til terminal/agent:
	1.	Opprett pakker i lpg-transport som passer prosjektet (eksempel):
	•	...transport.serial
	•	...transport.tcp
	•	...transport.pty (om du har PTY/socat/simulator)
	2.	Flytt (med git mv) transport-relaterte klasser fra lpg-ehl-core og lpg-ehl-webapp inn i lpg-transport.

Viktig: bruk git mv så historikk bevares.

# Eksempel (du må justere sti etter hva rg fant)
# Flytt communication/serial manager fra core:
git mv lpg-ehl-core/src/main/kotlin/**/communication lpg-transport/src/main/kotlin/**/communication || true
git mv lpg-ehl-core/src/main/kotlin/**/SerialPortManager.kt lpg-transport/src/main/kotlin/**/SerialPortManager.kt || true

# Flytt RealSerial* / adaptere fra webapp:
git mv lpg-ehl-webapp/src/main/kotlin/**/RealSerial* lpg-transport/src/main/kotlin/**/ || true
git mv lpg-ehl-webapp/src/main/kotlin/**/Serial*Adapter* lpg-transport/src/main/kotlin/**/ || true

	3.	Oppdater imports etter flytting.

mvn -q -DskipTests=true test


⸻

Steg 3 — Stram lpg-ehl-core til “ren EHL”

Mål: Core skal kun inneholde:
	•	framing (STX/LEN/ADR/CMD/DATA/XOR/ETX)
	•	encoder/decoder/parsing
	•	checksums (XOR)
	•	enums/konstanter/kommandoer
	•	evt. protokoll-nær state machine (uten IO og uten Spring)

Prompt til terminal/agent:
	1.	Fjern alle avhengigheter fra core som ikke er rene Kotlin (f.eks. spring-*, jSerialComm).
	2.	Sørg for at core ikke importerer noe fra webapp/service/transport.

# Vis core sine imports som lukter feil
rg -n "import org\.springframework|import com\.fazecast|import javax\.persistence|@Service|@Component" lpg-ehl-core/src/main/kotlin || true

Krav: Etter dette skal lpg-ehl-core bygge uten å dra inn serial/Spring.

⸻

Steg 4 — Flytt “use-cases” til lpg-ehl-service

Mål: Ting som ligner orkestrering og operasjoner (f.eks. EhlOperationsService) skal ut av core.

Prompt til terminal/agent:
	1.	Flytt EhlOperationsService (og tilsvarende) fra core → service.
	2.	Juster slik at service bruker core for codec/protokoll, og service bruker transport for IO.

# Eksempel (juster etter din faktiske sti)
git mv lpg-ehl-core/src/main/kotlin/**/EhlOperationsService.kt lpg-ehl-service/src/main/kotlin/**/EhlOperationsService.kt || true

	3.	Sørg for at CLI/Webapp/Headless kaller service for use-cases (ikke core).

mvn -q -DskipTests=false test


⸻

Steg 5 — POM-opprydding (riktig avhengighetsretning)

Mål: Avhengigheter blir slik:
	•	lpg-ehl-core: (ingen)
	•	lpg-transport: → lpg-ehl-core (+ serial libs)
	•	lpg-ehl-service: → lpg-ehl-core, lpg-transport (+ Spring Data/JPA osv.)
	•	entrypoints:
	•	webapp: → service
	•	headless: → service (uten spring-boot-starter-web)
	•	cli: → service

Prompt til terminal/agent:
Oppdater POM-ene til å speile dette og fjern unødvendige deps som ligger feil.

# Sjekk hele dependency-treet for kjappe røde flagg
mvn -q -DskipTests dependency:tree > target/deptree.txt
rg -n "lpg-ehl-core.*spring|lpg-ehl-core.*jSerialComm" target/deptree.txt || true


⸻

Steg 6 — “Entry points” skal være tynne

Mål: Entry-moduler skal bare:
	•	starte app
	•	velge transport-impl (real serial vs tcp vs sim)
	•	eksponere REST/UI/CLI
	•	sette profiler/config

Prompt til terminal/agent:
	1.	Søk etter @Service, @Repository, @Entity i webapp/headless/cli og flytt “business” tilbake til service.

rg -n "@Service|@Repository|@Entity" lpg-ehl-webapp/src/main/kotlin lpg-ehl-app-headless/src/main/kotlin lpg-ehl-cli/src/main/kotlin || true

	2.	La webapp bli igjen med controllers/websocket/security/static react.

⸻

Steg 7 — Verifikasjon: bygg alle jar-varianter

Prompt til terminal/agent:
Bygg alt, og kjør de tre entrypoints sine grunnleggende tester.

# Full build
mvn -q -DskipTests=false test

# (valgfritt) bygg jar
mvn -q -DskipTests package

Suksesskriterier:
	•	lpg-ehl-core har ingen Spring/Serial-deps
	•	lpg-transport er eneste sted som kjenner serial/COM/parity/baud
	•	lpg-ehl-service er eneste sted med use-cases/orkestrering
	•	webapp/headless/cli bygger og bruker service + transport

⸻

Steg 8 — (Valgfritt) Dependency-guard som hindrer regress

Prompt til terminal/agent:
Legg inn en enkel “guard” i CI eller i en Maven profile:
	•	fail hvis lpg-ehl-core importerer org.springframework eller com.fazecast
	•	fail hvis webapp inneholder @Entity/@Repository (hvis du ønsker det strengt)

En enkel start uten ArchUnit:
	•	en script-assert i CI:

# Core må ikke ha Spring eller serial imports
! rg -n "import org\.springframework|import com\.fazecast" lpg-ehl-core/src/main/kotlin

# (valgfritt) Webapp bør ikke ha entities/repositories
! rg -n "@Entity|@Repository" lpg-ehl-webapp/src/main/kotlin


⸻

Steg 9 — Commit-strategi (anbefalt)

Prompt til terminal/agent:
Lag små commits i denne rekkefølgen:
	1.	baseline + kartlegging (rapport i docs)
	2.	flytt transport til lpg-transport
	3.	core cleanup (deps + imports)
	4.	flytt use-cases til service
	5.	entrypoint-opprydding
	6.	dependency guard

git add -A
git commit -m "refactor: baseline + map serial/service leaks"

# ... osv per steg

Hvis du limer dette inn i terminal-agenten din, vil den “grave” frem filene med `rg`, flytte med `git mv`, og så iterere på kompilering (`mvn test`) til alt er rent.

Når du har kjørt Steg 1 og får listen med treff (filstier), kan du poste den listen her – så kan jeg gi deg en helt konkret “disse 12 filene flyttes hit” versjon, uten gjetting.