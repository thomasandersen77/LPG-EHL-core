# fra kunde

Vi er i løpende utvikling av grensesnittet, og status per i dag er at deler av arkitekturen for MinLPG nå er etablert. Nedenfor beskrives hvilke komponenter som allerede er operative, samt hva som er klart for integrasjon mot deres API-lag og MQTT-klient for dispenserinstallasjonene. Gi gjerne innspill til løsningene og eventuelle forbedringsforslag.



Arkitektur – oversikt
WordPress fungerer som frontend for både administrasjon og kundegrensesnitt. Backend-plattformen er etablert i Render med PostgreSQL som database. MQTT-infrastrukturen er satt opp i HiveMQ Cloud. Hele løsningen er strukturert slik at alle lokale funksjoner i WordPress kan flyttes over til API-basert databehandling uten endringer i UI-laget.



WordPress (UI-laget)
Frontend er ferdig implementert med rollemodell, klippekort, kredittmodul, stasjonsinnstillinger, priskontroll, transaksjonsvisning og PDF-generering. All funksjonalitet er modulbasert og forberedt for overgang fra interne PHP-kall til full API-integrasjon. Når deres REST-endepunkter er tilgjengelige, erstattes dagens lokale datalagring sømløst med API-kommunikasjon.



Backend – Render
Backend-miljøet er klargjort for deres implementasjon og består av: Web Service (Node runtime). Managed PostgreSQL som lagringsmotor for stasjonsdata, transaksjoner og enhetsinformasjon.
Miljøvariabler som allerede er konfigurert:
DATABASE_URL
JWT_SECRET
MQTT_BROKER_URL



API-lag, autentisering og DB-modell implementeres av dere direkte i GitHub-repoet som er koblet mot Render.



Er dette sammenfallene med deres estimat ? Videre trenger vi at betalingsløsningen håndteres. Vi har tilgengelig testterminal. Trenger også tilbakemelding på når dere ser for dere at produktet kan igangsattes.  Ser dere for dere at vi kan bruke ARK-3600 med ny Linux programvare som vil virke på dagens kontaktløse betalingsterminal ?



# Mitt arkitektur forslag 

Hei Alejandro,

Jeg trenger din frontend- og arkitekturekspertise litt raskt. Vi står i en diskusjon med kunden (Norgesgass) om teknologivalg for deres nye styringssystem for gass-stasjoner (LPG).

Kort om prosjektet: Vi skal levere et komplett system for styring av gassdispensere, betaling (Vipps/terminal) og administrasjon.

Hardware: ARK-3600 Industri-PC ute på stasjonene.

Kritikalitet: Dette er IoT og finansielle transaksjoner. Nedetid = tapte penger og sinte kunder.

Fase 1 (Det vi har signert på): Få opp en MVP på én stasjon.

Sette opp Linux/Docker på ARK-3600.

Backend-logikk som snakker med pumpa (EHL-protokoll) – Dette har jeg allerede kodet i Kotlin/Spring Boot.

Et lokalt GUI på touch-skjermen ved pumpa (Kiosk-mode).

Konflikten / Utfordringen: Kunden (som er prisbevisst og har bygget litt selv) foreslår følgende arkitektur for hele systemet:

Frontend/App: WordPress (PHP) for både admin og kiosk-skjermen (!).

Backend: En tynn Node.js service på Render + Managed Postgres.

Kommunikasjon: MQTT (HiveMQ).

Min vurdering (og der jeg trenger din støtte): Jeg mener det er uansvarlig å bygge et transaksjonskritisk IoT-system på toppen av WordPress/PHP. Det er risiko for "spaghetti-kode", sikkerhetshull via plugins, og dårlig stabilitet mot hardware.

Mitt forslag til "Robust Arkitektur" (The Cloudberries Way): Vi møter dem på halvveien ved å beholde billig infrastruktur (Render/HiveMQ), men bytter ut "motoren":

Edge & Backend: Kotlin/Spring Boot (Kjører i Docker både på stasjon og i skyen). Gir oss typesikkerhet, transaksjonskontroll og stabilitet.

Frontend (Ditt domene): React (Vite/Next.js). Vi bruker designet de har laget i WordPress som mal, men bygger det som en skikkelig SPA som snakker med API-et mitt.

Infrastruktur: Vi bruker Render (som de ønsker) og HiveMQ/Postgres, men vi eier koden som kjører på den.

Oppgaven til deg: Jeg har gir deg tilgang til repoet (lpg-ehl-core) så du kan se hva jeg har gjort på backend. Kan du innen kl 14:00 i morgen:

Se kjapt på e-posten fra Tobias (kunden) nedenfor.

Gi meg 3-4 kulepunkter fra et Frontend/Arkitekt-perspektiv på hvorfor React + API er bedre enn WordPress/PHP for en betalingskiosk?

Bekrefte at vi kan "kopiere" utseendet fra WordPress over i React relativt raskt, slik at de ikke føler at designjobben deres er bortkastet?

Vi må overbevise dem om at investeringen i en skikkelig stack nå sparer dem for enorme problemer om 6 måneder.


# mer info



Her er et forslag til intern e-post du kan sende til Per Christian (og evt. Øyvind). Den er bevisst intern-ærlig, men formulert så han enkelt kan plukke ut argumenter og spørsmål til dialogen med Tobias.

⸻

Emne: Vurdering av MinLPG-forslaget (WordPress/Node vs Kotlin/Docker-plattformen

Hei Per Christian,

Jeg har lest gjennom forslaget Tobias sendte om MinLPG-arkitekturen, og vil gi en faglig vurdering sett opp mot den Kotlin/Docker-løsningen vi har jobbet frem rundt ARK-3600.

1. Kort oppsummert innhold i forslaget
   •	Frontend: WordPress (PHP) som både administrasjons- og kundegrensesnitt
   •	Backend: Node runtime på Render som “web service”
   •	Database: Managed PostgreSQL på Render
   •	Meldingstrafikk: MQTT via HiveMQ Cloud
   •	Plan: Alt som i dag lagres lokalt i WordPress skal etter hvert flyttes over til et API-lag som de ber oss implementere (REST-endepunkter, autentisering, datamodell m.m.).

Med andre ord: vi overtar ansvaret for kjernelogikken og API-laget, mens WordPress og Node/Render blir rammen rundt.

2. Det som er positivt

Det er tydelig at de har lagt ned arbeid i:
•	å kartlegge funksjonelle behov i UI (klippekort, kreditt, stasjonsinnstillinger, priser, transaksjoner, PDF-rapporter osv.)
•	å få opp en teknisk infrastruktur (Render, PostgreSQL, HiveMQ)
•	å strukturere frontend slik at den kan snakke med et eksternt API senere.

Det er bra – vi slipper å begynne på “blankt ark” når det gjelder hvilke skjermbilder og moduler de ønsker seg.

3. Hovedinnvendinger mot teknologivalget

Jeg er likevel ganske skeptisk til å bruke WordPress/PHP + Node på Render som fundament for et forretningskritisk system som skal:
•	styre stasjoner
•	håndtere kreditt, betaling og avgifter
•	leve i mange år fremover.

De viktigste punktene:
1.	WordPress/PHP som kjernesystem
•	WordPress er fantastisk som CMS/nettside, men her snakker vi om et driftskritisk domene-system (pumper, kreditt, transaksjoner).
•	PHP-kode som blandes inn i WordPress-miljøet gir:
•	svakere type-sikkerhet og mindre hjelp fra verktøyene
•	større angrepsflate (plugins/temaer)
•	vanskeligere testbarhet og release-kontroll
•	Det er også vanskeligere å finne seniorutviklere som både kan PHP/WordPress godt og vil jobbe med denne typen domene på sikt.
2.	Node-backend på Render er i praksis et tomt skall
•	Slik jeg leser det, er Node-servicen i Render mest et “entry point” hvor de forventer at vi skal:
•	designe og implementere API-laget
•	håndtere autentisering, autorisasjon, logging, overvåkning
•	modellere og implementere all domenelogikk.
•	Da får vi en hybrid: WordPress-frontend + Node-API + vår Kotlin-logikk på stasjonene. Det øker kompleksiteten, uten at det gir oss særlig gevinst.
3.	Robusthet, observability og vedlikehold
•	For et system som dette vil jeg mye heller ha:
•	Kotlin/Spring Boot som backend (både sentralt og på edge):
•	statisk typet språk
•	moden økosystem for logging, tracing, metrics, health checks osv.
•	veldig god støtte for database-tilkoblinger, transaksjoner og feil­håndtering.
•	Node kan fint brukes, men erfaringen min (bl.a. fra LSK Historic/Next.js) er at det blir tungt å holde ryddig og robust når domenereglene og integrasjonene vokser.
4.	Ansvarsdeling og risiko
•	Slik det er formulert nå, ender vi med en arkitektur der:
•	vi har ansvar for kjerne-API og domene
•	de har ansvar for WordPress-delen
•	Node/Render ligger imellom.
•	Jeg er redd dette gir en del gråsoner (“hvor ligger feilen nå – WP, Node, API eller edge?”) og mer drift/feilsøking enn nødvendig.

4. Sammenligning med Kotlin/Docker-løsningen vi har skisset

Det vi har jobbet frem de siste dagene er i praksis:
•	Edge-lag på ARK-3600:
•	Kotlin/Spring Boot
•	EHL-protokoll implementert og testet
•	kjører i Docker på Linux
•	lokal Postgres for transaksjoner/status.
•	Admin/portal-lag:
•	React-basert UI (pumpesimulator, transaksjonsliste, kreditt, rapporter, emulator-debug osv.)
•	Ren REST-integrasjon mot API-et.

Samtidig er dette bare en emulator og demo jeg har skrudd sammen på en helg – og jeg er ikke frontend-spesialist. Poenget mitt er:

Hvis dette er mulig å sette opp på et par dager, med begrenset frontend-kompetanse, så kan et lite team i Cloudberries bygge en langt mer gjennomarbeidet og robust admin-løsning på samme plattform.

Fordelene vi får da:
•	én konsistent backend-stack (Kotlin/Spring/Postgres)
•	samme teknologi på edge og sentralt
•	enklere logging, overvåkning og instrumentering
•	enklere å onboarde andre Cloudberries-konsulenter på sikt.

5. Mulige spørsmål du kan ta med til Tobias

Hvis du vil utfordre forslaget litt (på en konstruktiv måte), kunne du f.eks. spørre:
1.	Langsiktig vedlikehold:
Hvordan ser de for seg vedlikehold og videreutvikling av PHP/WordPress-delen over 5–10 år? Hvem skal eie denne koden og ansvaret?
2.	Sikkerhet og oppdateringer:
Hvilken plan har de for sikkerhetsoppdateringer av WordPress/plugins/temaer, og hvordan unngår man at dette påvirker driftskritisk funksjonalitet?
3.	Rollefordeling:
Hvor går grensen mellom hva MinLPG-teamet skal gjøre, og hva Cloudberries skal ta ansvar for?
Er målet på sikt at all domene-logikk (kreditt, avgift, transaksjoner) flyttes ut i vårt API-lag, og WordPress kun blir et “tynt” UI?
4.	Alternativ arkitektur:
Ville de vært åpne for en modell der:
•	Kotlin/Spring/Postgres utgjør kjernesystemet (både sentralt og på edge), og
•	frontend/UI bygges på toppen av dette som en ren klient (React/Next eller annen moderne webstack)?
WordPress kunne eventuelt brukes kun som CMS/innhold hvis de ønsker det.

⸻

Jeg mener ikke at arbeidet de har gjort er bortkastet – spesielt ikke på funksjonell designsiden. Men rent teknisk synes jeg ikke en kombinasjon av WordPress/PHP + Node på Render er en ansvarlig plattform for et system som skal styre pumpeanlegg, kreditt og betaling i mange år fremover.

Jeg tror vi bør posisjonere oss tydelig på at:
•	vi tar ansvar for kjerneplattformen (Kotlin/Docker/Postgres + integrasjon mot ARK-3600 og betaling),
•	og at eventuelt videre frontend/portal-arbeid bør bygges på toppen av denne plattformen, ikke rundt WordPress.

