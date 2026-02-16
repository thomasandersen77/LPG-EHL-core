Dette er **svært gode nyheter**. At du har en fysisk kopi av maskinen (ARK-3360F) som kjører softwaren hjemme, gir deg en enorm fordel. Det er i praksis en "Digital Tvilling" av stasjonen i Drammen.

Ja, funnene dine **styrker teorien om TCP/Ethernet** betraktelig. Her er analysen av hvorfor, og nøyaktig hva du bør gjøre på lab-maskinen din for å hente ut "Fasiten".

### 1. Hvorfor funnene dine styrker TCP-teorien 🔌

1. **Ingen seriekabler = Ingen krasj?**
   Hvis programvaren starter opp på lab-maskinen din *uten* at du har koblet til fysiske pumper via seriekabel, betyr det at programmet ikke prøver å åpne en fysisk `COM1` som mangler (da ville VB6 ofte krasjet med "Run-time error 8002: Invalid Port Number").
* I stedet prøver den sannsynligvis å åpne en **Virtuell COM-port** (som driveren mapper til en IP-adresse) eller en **TCP Socket**. Siden nettverkskortet er der, "feiler" den bare stille ved å få timeout, i stedet for å krasje.


2. **Modbus-filer funnet:**
   Som vi konkluderte med: Disse filene (`ModbusTCP.dll` osv.) bekrefter at systemet er bygget for å snakke over nettverk (TCP), mest sannsynlig mot ADAM-boksen (som du mangler hjemme, men som softwaren *leter* etter).
3. **ARK-maskinen:**
   Advantech ARK-serien (både 3360 og 3600) er vifteløse industriservere som ofte brukes nettopp som **Gateways**. De har ofte doble nettverksporter nettopp for å skille WAN (Internett/Betaling) fra LAN (Pumper/ADAM).

---

### 2. Detektivarbeid: Hva du skal lete etter i Windows XP (Fasiten) 🕵️‍♂️

Siden du kommer deg inn i Explorer, kan du finne **IP-adressene og portene** systemet prøver å koble seg til. Da slipper du å gjette når du skal kode `TcpTransport`!

Gjør dette på Lab-maskinen din:

#### A. Sjekk etter "Virtual COM" drivere (GULLGRUVE) 🏆

Dette er det viktigste punktet. Hvis pumpene går over Ethernet, er det nesten garantert installert en driver som heter noe sånt som:

* **Moxa NPort Administrator**
* **NPort Windows Driver Manager**
* **Digi RealPort**
* **Advantech Serial Device Server**

**Gjør dette:**

1. Gå til **Control Panel** -> **Add/Remove Programs**. Ser du noe fra Moxa, Digi eller Advantech der?
2. Gå til **Device Manager** (Enhetsbehandling).
* Se under **Ports (COM & LPT)**.
* Heter portene bare "Communications Port"? Eller heter de "Moxa Port (COM3)"?
* Hvis du finner en slik driver: **Åpne den!** Inne i innstillingene der står IP-adressen til boksen i Drammen hardkodet.



#### B. Sjekk Registry (Registeret) ®️

Gamle VB6-programmer (og drivere) lagrer ofte oppsettet sitt her.

1. Start -> Run -> `regedit`
2. Naviger til: `HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services`
* Se etter mapper som `NPort`, `Moxa`, `Serial`.


3. Naviger til: `HKEY_CURRENT_USER\Software\VB and VBA Program Settings`
* Ligger det noe som heter `Pumpekontroll`, `EHL` eller `Cloudberries` (eller gammelt firmanavn) her?



#### C. Sjekk "hosts"-filen 📝

Noen ganger mapper utviklere navn til IP-er her.

* Åpne i Notepad: `C:\Windows\System32\drivers\etc\hosts`
* Ser du linjer som: `192.168.1.50 pumpe1`?

---

### 3. Gode råd til deg: Hvordan bruke Lab-oppsettet ditt 🧪

Du har Mac-en din og ARK-maskinen koblet i samme switch. Dette er perfekt for "Man-in-the-Middle" analyse.

**Steg 1: "Lytt" på nettverket (Sniffing)**
Selv om du ikke har pumper, *roper* sannsynligvis ARK-maskinen etter dem.

1. **Installer Wireshark på ARK-maskinen:**
* Last ned en gammel versjon som støtter Windows XP (Legacy release 1.10 eller 1.12).
* Legg den på minnepenn og installer.


2. **Start opptak:**
* Start Wireshark.
* Start Pumpe-softwaren.


3. **Se etter TCP-pakker:**
* Du vil se pakker merket med **SYN** (røde/svarte linjer) som prøver å koble til en IP (f.eks. `192.168.1.55` på port `4001`).
* **BINGO!** Da har du IP-en og porten du skal bruke i din Kotlin-kode.



**Steg 2: Lur ARK-maskinen (Simulering)**
Når du har funnet IP-en ARK-maskinen prøver å snakke med (la oss si `192.168.1.55`):

1. Gi Mac-en din denne IP-adressen manuelt på nettverkskortet (Ethernet-adapteret).
2. Kjør `ncat` (eller din `lpg-ehl-emulator`) på Mac-en som lytter på porten (f.eks. 4001).
3. Nå vil det gamle Windows-programmet på ARK-maskinen **koble seg til Mac-en din**.
4. Du vil se EHL-hex-kodene (`10 ...`) tikke inn på Mac-skjermen din.

**Konklusjon:**
Du trenger ikke dra til Drammen enda.

1. Bruk ARK-maskinen til å finne **Måladressen** (IP:Port) via Wireshark eller Device Manager.
2. Bruk Mac-en til å **være pumpene** og verifiser at du forstår EHL-dialogen.

Dette oppsettet ditt er gull verdt. Utnytt det! 🚀****