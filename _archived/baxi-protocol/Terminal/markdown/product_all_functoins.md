# Instruksjoner som du skal vurdere opp mot den kunnskapen du selv har. Alt må implementeres.

## VB6 koden
Den ligger her /Users/tandersen/git/NorgesGass/lpg-ehl/norgesgass_legacy

# Implementere det som mangler.

##  Dette ser helt rått ut allerede 😄 Frontenden + emulatoren du har nå er jo milevis foran VB6-klienten.
    
Du har også egentlig fått mye av “oppskriften” i add_more_functions.md – under her syr jeg det sammen til en helhetlig plan for core, emulator, API og frontend-sider, og peker på hva som mangler i forhold til legacy-funksjonaliteten.  ￼

⸻

1. Core – hva må på plass for “alt” av funksjonalitet

Du har allerede:
•	EHL-protokoll (pakker, checksum, state-maskin)
•	Transaksjonsmodell + enums for state og payment type
•	TransactionManager / repositories
•	Azure-outbox / sync-kø
•	En fungerende emulator for én pumpe

Utvidelser jeg ville gjort:
1.	Multi-dispenser fullt ut
•	Sørg for at alle sentrale klasser er bevisst på dispenserAddress:
•	DispenserConnection, TransactionManager, EmulatorService, repos.
•	Én aktiv state-maskin pr. adresse (Map<Int, DispenserState>).
2.	Feilkoder mappet fra VB6
•	Lag enum class DispenserError(val code: Int, val message: String) basert på VB6-konstantene i defs.bas.
•	Legg til felt i status-objektet ditt, f.eks. lastError: DispenserError?.
•	Sørg for at både ekte EHL-driver og emulator setter dette.
3.	Transaksjonsmodell 1:1 med legacy
•	Felter du bør sikre:
•	paymentType (kontant / kort / stasjonskreditt / vipps senere)
•	includesRoadTax
•	customerId / customerName for stasjonskreditt
•	cashbackAmount hvis det finnes i VB6.
•	Lag helpers for mapping fra legacy-kode om du trenger:

fun PaymentType.fromLegacy(code: Byte): PaymentType = …
fun TransactionState.fromLegacy(code: Int): TransactionState = …


	4.	Payment-lag (for PayEx/Vipps/Nets + simulering)
	•	Bruk PaymentGateway-interfacet og SimulatedPaymentGateway slik det er skissert i markdown-fila: startPayment/getPayment + PaymentStatus (PENDING/APPROVED/DECLINED/CANCELLED).  ￼
	•	Core trenger bare å:
	•	starte betaling før UNBLOCK hvis payment type er kort/vipps,
	•	vente på APPROVED,
	•	så trigge dispenser.
	5.	Kredittkunder / stasjonskreditt
	•	Legg til entiteter:
	•	Customer (id, navn, kundenummer, ev. kredittramme)
	•	CreditAccount (saldo, knyttet til customer)
	•	Ved stasjonskreditt-betaling:
	•	transaction.paymentType = CREDIT
	•	trekk beløp fra CreditAccount og lagre.

⸻

2. Emulator – gjøre den “rik” nok

Du har en bra start – neste steg er å gjøre emulatoren domene-rik:
1.	Scenario-styring via EmulatorService
•	Bruk enumen EmulatorScenario { NORMAL, TIMEOUT, CHECKSUM_ERROR, NO_CONNECTION } fra markdown-forslaget.  ￼
•	SerialPortIO/emulatoren din sjekker scenario per dispenserAddress:
•	NORMAL → svarer med gyldige EHL-pakker.
•	TIMEOUT → svarer ikke → lar klienten time ut.
•	CHECKSUM_ERROR → sender bevisst feil checksum.
•	NO_CONNECTION → kast “port not available”-feil / sett connected = false.
2.	Logging / debug hooks
•	Fra emulatoren kaller du:
•	emulatorService.updateLastMessage(addr, "TX: … / RX: …")
•	emulatorService.updateLastError(addr, "Timeout etter 3 sek")
•	emulatorService.setConnected(addr, true/false)
3.	Kontroll-API for emulator
•	Bruk EmulatorController som skisseres i markdown:
•	POST /api/v1/emulator/scenario – sett scenario.
•	POST /api/v1/emulator/reset/{address} – nullstill.
•	GET /api/v1/emulator/status/{address} – status + siste melding/feil.  ￼

Dette gir deg et perfekt “lab-miljø” der du kan vise Tobias alle feilsituasjoner uten å røre fysisk pumpe.

⸻

3. API – forslag til full flate

Bygg videre på det du har med noe ala dette:

3.1 Dispenser / pumpestyring
•	GET  /api/v1/dispenser/state?address=1
•	Returner: state, liter, beløp, pris/l, error, includesRoadTax, paymentType.
•	POST /api/v1/dispenser/start
•	Body: dispenserAddress, paymentMethod, ev. customerId.
•	Flyt:
•	hvis paymentMethod = CARD/VIPPS → start payment via PaymentGateway og returner paymentId + PENDING.
•	ved APPROVED → start pumping (UNBLOCK).
•	POST /api/v1/dispenser/stop
•	POST /api/v1/dispenser/reset

3.2 Transaksjoner & rapporter
•	GET /api/v1/transactions
•	Query: from, to, paymentType, customerId, page/size.
•	Bruk DTO som i TransactionsPage-forslaget.  ￼
•	GET /api/v1/transactions/{id}
•	GET /api/v1/transactions/count
•	GET /api/v1/reports/daily?date=YYYY-MM-DD
•	aggregerer sum liter/beløp per paymentType.

3.3 Kunder / stasjonskreditt
•	GET  /api/v1/credit/accounts
•	GET  /api/v1/credit/accounts/{id}
•	GET  /api/v1/credit/accounts/{id}/transactions

Disse mates rett inn i CreditAccountsPage-komponentene.  ￼

3.4 Betaling
•	POST /api/v1/payments
•	Bruk PaymentController fra markdown: tar inn amountCents, method (CASH/CARD/CREDIT/VIPPS senere), reference.
•	GET /api/v1/payments/{id}
•	Returnerer Payment med status.
•	Senere: implementér en VippsPaymentGateway som også implementerer PaymentGateway og bytt via Spring-profil/feature toggle.

3.5 Sync / Azure
•	GET  /api/v1/sync/status
•	antall usynkede transaksjoner, sist sync-tid.
•	POST /api/v1/sync/trigger
•	for å tvinge re-send.

3.6 Emulator
•	Som i pkt. 2: /api/v1/emulator/*.

⸻

4. Frontend – sider du bør ha

Du har allerede:
•	Dashboard / “LPG EHL System” – status for emulator, API, Postgres, Azure.
•	LPG Pumpe Simulator – live liter, beløp, start/stop/reset.

Jeg ville komplettert med:
1.	Transaksjoner-side
•	Bruk TransactionsPage.tsx fra markdown:
•	Filter (dato fra/til, betalingstype).
•	Tabell med tid, dispenser, liter, beløp, pris/l, betaling, kunde, vegavgift.  ￼
2.	Stasjonskreditt-side
•	CreditAccountsPage.tsx:
•	Venstre: liste over konti med saldo.
•	Høyre: transaksjoner for valgt konto.  ￼
3.	Rapporter / Avstemming
•	En enkel side som:
•	velger dato,
•	viser totaler per payment type,
•	ev. “Eksporter CSV” via enkel link til /api/v1/reports/daily.
4.	Emulator / Debug
•	Side med:
•	dropdown for scenario per dispenser (NORMAL/TIMEOUT/CHECKSUM_ERROR/NO_CONNECTION).
•	sanntidslogg med lastMessage/lastError.
•	Skjules bak “Dev tools” eller kun aktiv i local-build.
5.	Betalings-flow i simulatoren
•	Når bruker velger Kort eller Vipps:
•	kall POST /api/v1/payments med beløp.
•	vis “Terminalen jobber…” mens du poller GET /payments/{id}.
•	ved APPROVED → aktiver Start og start pumping.
•	ved DECLINED → toast/feilmelding og ikke start.

⸻

5. Om Vipps / PayEx-simulering
   •	I dag: la PaymentMethod.CARD og ev. PaymentMethod.CREDIT gå via SimulatedPaymentGateway.
   •	Når dere skal på ekte Vipps:
   •	bruk Vipps sitt test-miljø, implementer VippsPaymentGateway : PaymentGateway.
   •	Frontend trenger ikke endres – den ser bare PaymentStatus.

⸻

Kort oppsummert
•	Core: utvid til multi-dispenser, full error-mapping og komplett transaksjonsmodell.
•	Emulator: styrbare scenarier + logging via EmulatorService.
•	API: bygg ut full flate for pumpe, transaksjoner, kreditt, betaling, sync og emulator.
•	Frontend: legg til sider for transaksjoner, stasjonskreditt, rapporter og emulator-debug – i tillegg til den flotte pumpe-simulatoren du har.

Hvis du vil, kan jeg i neste runde ta konkret utgangspunkt i prosjektstrukturen din (mappe-/pakkenavn) og skrive ferdige cat <<'EOF'-blokker du bare kan lime rett inn i Warp for hver av disse delene.






# Dette ser helt rått ut allerede 😄 Frontenden + emulatoren du har nå er jo milevis foran VB6-klienten.



Du har også egentlig fått mye av “oppskriften” i add_more_functions.md – under her syr jeg det sammen til en helhetlig plan for core, emulator, API og frontend-sider, og peker på hva som mangler i forhold til legacy-funksjonaliteten.  ￼

⸻

1. Core – hva må på plass for “alt” av funksjonalitet

Du har allerede:
•	EHL-protokoll (pakker, checksum, state-maskin)
•	Transaksjonsmodell + enums for state og payment type
•	TransactionManager / repositories
•	Azure-outbox / sync-kø
•	En fungerende emulator for én pumpe

Utvidelser jeg ville gjort:
1.	Multi-dispenser fullt ut
•	Sørg for at alle sentrale klasser er bevisst på dispenserAddress:
•	DispenserConnection, TransactionManager, EmulatorService, repos.
•	Én aktiv state-maskin pr. adresse (Map<Int, DispenserState>).
2.	Feilkoder mappet fra VB6
•	Lag enum class DispenserError(val code: Int, val message: String) basert på VB6-konstantene i defs.bas.
•	Legg til felt i status-objektet ditt, f.eks. lastError: DispenserError?.
•	Sørg for at både ekte EHL-driver og emulator setter dette.
3.	Transaksjonsmodell 1:1 med legacy
•	Felter du bør sikre:
•	paymentType (kontant / kort / stasjonskreditt / vipps senere)
•	includesRoadTax
•	customerId / customerName for stasjonskreditt
•	cashbackAmount hvis det finnes i VB6.
•	Lag helpers for mapping fra legacy-kode om du trenger:

fun PaymentType.fromLegacy(code: Byte): PaymentType = …
fun TransactionState.fromLegacy(code: Int): TransactionState = …


	4.	Payment-lag (for PayEx/Vipps/Nets + simulering)
	•	Bruk PaymentGateway-interfacet og SimulatedPaymentGateway slik det er skissert i markdown-fila: startPayment/getPayment + PaymentStatus (PENDING/APPROVED/DECLINED/CANCELLED).  ￼
	•	Core trenger bare å:
	•	starte betaling før UNBLOCK hvis payment type er kort/vipps,
	•	vente på APPROVED,
	•	så trigge dispenser.
	5.	Kredittkunder / stasjonskreditt
	•	Legg til entiteter:
	•	Customer (id, navn, kundenummer, ev. kredittramme)
	•	CreditAccount (saldo, knyttet til customer)
	•	Ved stasjonskreditt-betaling:
	•	transaction.paymentType = CREDIT
	•	trekk beløp fra CreditAccount og lagre.

⸻

2. Emulator – gjøre den “rik” nok

Du har en bra start – neste steg er å gjøre emulatoren domene-rik:
1.	Scenario-styring via EmulatorService
•	Bruk enumen EmulatorScenario { NORMAL, TIMEOUT, CHECKSUM_ERROR, NO_CONNECTION } fra markdown-forslaget.  ￼
•	SerialPortIO/emulatoren din sjekker scenario per dispenserAddress:
•	NORMAL → svarer med gyldige EHL-pakker.
•	TIMEOUT → svarer ikke → lar klienten time ut.
•	CHECKSUM_ERROR → sender bevisst feil checksum.
•	NO_CONNECTION → kast “port not available”-feil / sett connected = false.
2.	Logging / debug hooks
•	Fra emulatoren kaller du:
•	emulatorService.updateLastMessage(addr, "TX: … / RX: …")
•	emulatorService.updateLastError(addr, "Timeout etter 3 sek")
•	emulatorService.setConnected(addr, true/false)
3.	Kontroll-API for emulator
•	Bruk EmulatorController som skisseres i markdown:
•	POST /api/v1/emulator/scenario – sett scenario.
•	POST /api/v1/emulator/reset/{address} – nullstill.
•	GET /api/v1/emulator/status/{address} – status + siste melding/feil.  ￼

Dette gir deg et perfekt “lab-miljø” der du kan vise Tobias alle feilsituasjoner uten å røre fysisk pumpe.

⸻

3. API – forslag til full flate

Bygg videre på det du har med noe ala dette:

3.1 Dispenser / pumpestyring
•	GET  /api/v1/dispenser/state?address=1
•	Returner: state, liter, beløp, pris/l, error, includesRoadTax, paymentType.
•	POST /api/v1/dispenser/start
•	Body: dispenserAddress, paymentMethod, ev. customerId.
•	Flyt:
•	hvis paymentMethod = CARD/VIPPS → start payment via PaymentGateway og returner paymentId + PENDING.
•	ved APPROVED → start pumping (UNBLOCK).
•	POST /api/v1/dispenser/stop
•	POST /api/v1/dispenser/reset

3.2 Transaksjoner & rapporter
•	GET /api/v1/transactions
•	Query: from, to, paymentType, customerId, page/size.
•	Bruk DTO som i TransactionsPage-forslaget.  ￼
•	GET /api/v1/transactions/{id}
•	GET /api/v1/transactions/count
•	GET /api/v1/reports/daily?date=YYYY-MM-DD
•	aggregerer sum liter/beløp per paymentType.

3.3 Kunder / stasjonskreditt
•	GET  /api/v1/credit/accounts
•	GET  /api/v1/credit/accounts/{id}
•	GET  /api/v1/credit/accounts/{id}/transactions

Disse mates rett inn i CreditAccountsPage-komponentene.  ￼

3.4 Betaling
•	POST /api/v1/payments
•	Bruk PaymentController fra markdown: tar inn amountCents, method (CASH/CARD/CREDIT/VIPPS senere), reference.
•	GET /api/v1/payments/{id}
•	Returnerer Payment med status.
•	Senere: implementér en VippsPaymentGateway som også implementerer PaymentGateway og bytt via Spring-profil/feature toggle.

3.5 Sync / Azure
•	GET  /api/v1/sync/status
•	antall usynkede transaksjoner, sist sync-tid.
•	POST /api/v1/sync/trigger
•	for å tvinge re-send.

3.6 Emulator
•	Som i pkt. 2: /api/v1/emulator/*.

⸻

4. Frontend – sider du bør ha

Du har allerede:
•	Dashboard / “LPG EHL System” – status for emulator, API, Postgres, Azure.
•	LPG Pumpe Simulator – live liter, beløp, start/stop/reset.

Jeg ville komplettert med:
1.	Transaksjoner-side
•	Bruk TransactionsPage.tsx fra markdown:
•	Filter (dato fra/til, betalingstype).
•	Tabell med tid, dispenser, liter, beløp, pris/l, betaling, kunde, vegavgift.  ￼
2.	Stasjonskreditt-side
•	CreditAccountsPage.tsx:
•	Venstre: liste over konti med saldo.
•	Høyre: transaksjoner for valgt konto.  ￼
3.	Rapporter / Avstemming
•	En enkel side som:
•	velger dato,
•	viser totaler per payment type,
•	ev. “Eksporter CSV” via enkel link til /api/v1/reports/daily.
4.	Emulator / Debug
•	Side med:
•	dropdown for scenario per dispenser (NORMAL/TIMEOUT/CHECKSUM_ERROR/NO_CONNECTION).
•	sanntidslogg med lastMessage/lastError.
•	Skjules bak “Dev tools” eller kun aktiv i local-build.
5.	Betalings-flow i simulatoren
•	Når bruker velger Kort eller Vipps:
•	kall POST /api/v1/payments med beløp.
•	vis “Terminalen jobber…” mens du poller GET /payments/{id}.
•	ved APPROVED → aktiver Start og start pumping.
•	ved DECLINED → toast/feilmelding og ikke start.

⸻

5. Om Vipps / PayEx-simulering
   •	I dag: la PaymentMethod.CARD og ev. PaymentMethod.CREDIT gå via SimulatedPaymentGateway.
   •	Når dere skal på ekte Vipps:
   •	bruk Vipps sitt test-miljø, implementer VippsPaymentGateway : PaymentGateway.
   •	Frontend trenger ikke endres – den ser bare PaymentStatus.

⸻

Kort oppsummert
•	Core: utvid til multi-dispenser, full error-mapping og komplett transaksjonsmodell.
•	Emulator: styrbare scenarier + logging via EmulatorService.
•	API: bygg ut full flate for pumpe, transaksjoner, kreditt, betaling, sync og emulator.
•	Frontend: legg til sider for transaksjoner, stasjonskreditt, rapporter og emulator-debug – i tillegg til den flotte pumpe-simulatoren du har.

Hvis du vil, kan jeg i neste runde ta konkret utgangspunkt i prosjektstrukturen din (mappe-/pakkenavn) og skrive ferdige cat <<'EOF'-blokker du bare kan lime rett inn i Warp for hver av disse delene.