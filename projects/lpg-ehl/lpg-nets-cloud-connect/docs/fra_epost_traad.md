Her er en oppsummering av e-postkorrespondansen, inkludert tekniske og kommersielle detaljer.

### Oppsummering av e-posttråden

E-posttråden dreier seg om Cloudberries AS sitt behov for å sette opp en **SELF/4000 CL betalingsterminal** for deres kunde NorgesGass, og å velge mellom to integrasjonsløsninger fra Nets/Nexi Group: **Nets Cloud/Cloud Connect** eller **direkte integrasjon via ECR/BAXI-protokoll**.

**Opprinnelige spørsmål og tekniske detaljer:**

  * **Integrasjonsveier:**
    1.  **Nets Cloud/Cloud Connect:** Forespørsel om kostnad for 50+ terminaler og om hvert terminal krever eget abonnement. Trenger hjelp med protokoll og integrasjon.
    2.  **Direkte integrasjon (ECR/BAXI):** Ønsket om å integrere direkte i **Java 21**. Deres nåværende system er en **ARK 3360** som kjører **Debian Bookworm** (64-bit Linux på Intel Atom), hvor de har eksperimentert med `BAXI_dotnet.dll` i .NET 4.8.
  * **Kommersielt/Prosessuelt:** Cloudberries trenger å estimere kostnadene for 50+ terminaler for å ta en beslutning og uttrykte frustrasjon over lang ventetid for dokumentasjon og testmiljø for Cloud Connect. Tilbudet gjelder **unattended/selvbetjente terminaler**.

**Svar og avklaringer fra Nets/Nexi Group (Jannick Schønecker og Steinar Sjølie):**

  * **Dokumentasjon:** Nets sendte lenker til dokumentasjon for Cloud Connect med Self4000:
      * [Lenke 1](https://slack-files.com/T0139QBJ4BC-F08E0QT5F8Q-66e6a49d5f)
      * [Lenke 2](https://slack-files.com/T0139QBJ4BC-F07JZ66FCQ5-7282f1ce3b)
  * **Kostnad og abonnement:**
      * Selve "Cloud" har **ingen kostnad**.
      * Det påløper en månedlig faktura for **programvare og serviceavgift på ca. 15–20 euro per terminal**, som gjelder for alle terminaler levert av Nets.
      * Det er bekreftet at de **kan ha én Cloud-konto** og ett tilgangspunkt for alle terminalene, som settes opp etter at integrasjonen er fullført.
  * **Testmiljø:**
      * Joakim Augestad oppga BAX-nummeret/Terminal ID for deres **Self4000**: **1229329**.
      * Nets bekreftet at dette er en **PROD-terminal** og at testing vil involvere **ekte penger**. Steinar Sjølie ble nevnt som kontakt for å skaffe en dedikert testterminal.

**Avtalte neste skritt og teknisk oppsett:**

Etter å ha fått avklart kostnadsspørsmålet, bekreftet Alejandro Saksida at de ønsker å **starte testing mot PROD-terminalen** (BAX 1229329) og ba om hjelp til å konfigurere den for Nets Cloud.

Nets sendte følgende tilgangsinformasjon og konfigurasjonsdetaljer:

  * **Cloud Connect-pålogging:**
      * **Bruker:** `cranberries_shared`
      * **Passord:** `Gf&DW*8-IN7Lx6pE`
  * **Terminalinnstillinger (Under PARAMETERS):**
      * **Meny \[2\] Communication/Comms:**
          * Comm. type = "Ethernet" eller "Wi-Fi"
          * Host IP addr. = `91.102.24.142` (PROD)
          * Host IP: `9670`
      * **Meny \[3\] Kasse/ECR:**
          * ECR = Yes
          * Comm. type = "IP Wi-Fi" eller "IP Ethernet"
          * ECR/TLS = Yes
          * ECR IP Addr. = `3.33.230.243, 15.197.206.182` (PROD)
          * ECR IP port = `6001`
      * **Annet:** De må også hente **kortavtalene** (`cardsheme`/`kortaftaler`) fra terminalen.

**Siste status:**

Alejandro har konfigurert terminalen som foreslått, men har fulgt opp med spørsmål om når han kan forvente den resterende informasjonen og om kortavtalene er nødvendig for videre oppsett, samt bemerket en indikasjon på at terminalen ikke koblet seg til skyen.

Vennlig hilsen,
Thomas
