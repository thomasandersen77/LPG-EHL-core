# TEGNAL: H2 oppstart og endringer

**Mottaker:** Tegnal (AI-assistent)  
**Formål:** Verifisere oppstart av lpg-ehl-headless med H2-profil og utføre navngitte endringer.  
**Referanser:** Warp-oppsummering (oppstartsplan), [GUIDE_H2_OPPDATERING_2026.md](GUIDE_H2_OPPDATERING_2026.md).

---

## 1. Oppstartsplan (referanse)

Bruk dette som **sannhetskilde** for hvordan headless med H2-profil skal oppføre seg. Ingen oppgaver her, kun referanse.

### Applikasjonstype

- Spring Boot headless (ingen web-server: `web-application-type: none`)
- Profil: `h2`

### Database

- H2 in-memory med PostgreSQL-kompatibilitetsmodus
- URL: `jdbc:h2:mem:lpgdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
- Liquibase migrasjoner aktivert

### Transportlag

- Mode: **HARDWARE** (ekte seriell port)
- Port: `/dev/ttyS0`
- EHL-protokoll: 9600 baud, 8E1 (8 databits, Even paritet, 1 stoppbit)

### LPG-konfigurasjon

- Mode: **FIELD** (produksjonsmodus)
- Dispenser-adresse: 1
- Polling-intervall: 2000 ms
- Pris: 15.90 kr/liter
- Stasjon: STATION-001

### Deaktiverte tjenester

- Azure Storage Queue (`azure.enabled: false`)
- Nets Cloud Connect (peker til localhost)

### Logging

- Loggfil: `/tmp/lpg-ehl/headless.log`
- Nivå: INFO for applikasjon, WARN for Hibernate/Spring/Liquibase

### Oppstartssekvens

1. Spring Boot initialiserer med h2-profil
2. H2 in-memory database opprettes
3. Liquibase kjører migrasjoner
4. SerialPortManager kobler til /dev/ttyS0
5. HeadlessPollingService starter polling av dispenser på adresse 1 hver 2. sekund

---

## 2. Rettelser og tillegg (fra GUIDE_H2_OPPDATERING_2026)

- **Changelog:** Kun **YAML**-changelog brukes (`db.changelog-master.yaml`). XML-master og 003/004/005 kjører ikke. 005 «skippes» ikke for H2 – den kjøres aldri.
- **001-initial-schema:** Bruker `json` (ikke `jsonb`) for `decoded_data` og `payload`. H2-modifySql kan være no-op; oppsettet er likevel H2-kompatibelt.
- **009:** Med i YAML-master, H2-kompatibel.
- **ddl-auto:** H2-profilen bruker `spring.jpa.hibernate.ddl-auto: update`. Anbefaling: bruk `validate` dersom schema skal styres utelukkende via Liquibase.

---

## 3. Oppgaver til tegnal

### 3.1 Verifisere at oppstart stemmer med oppstartsplanen

- Bygg headless-JAR: `mvn -pl lpg-ehl-app-headless -am package -DskipTests`
- Kjør med `--spring.profiles.active=h2`
- Kontroller at ingen `DB_*` tvinger Postgres-URL
- Verifiser at logg/oppførsel matcher oppstartsplanen (H2, Liquibase, SerialPortManager, HeadlessPollingService, osv.)

### 3.2 (Valgfri) Endre ddl-auto i application-h2

- **Fil:** `lpg-ehl-app-headless/src/main/resources/application-h2.yaml`
- **Endring:** Sett `spring.jpa.hibernate.ddl-auto` fra `update` til `validate`
- **Begrunnelse:** Schema styres kun via Liquibase; unngå dobbel schema-håndtering med Hibernate `update`
- **Merk:** Kun utfør hvis bruker bekrefter at schema skal styres utelukkende av Liquibase

### 3.3 Dokumentere utførte steg

- Kort rapportere hva som ble verifisert og hvilke filendringer som ble gjort (om noen)

---

## 4. Sjekkliste

1. **Bygg headless-JAR**
   ```bash
   mvn -pl lpg-ehl-app-headless -am package -DskipTests
   ```

2. **Kjør med H2-profil**
   ```bash
   java -jar lpg-ehl-app-headless/target/lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar --spring.profiles.active=h2
   ```
   (Juster sti og JAR-navn dersom du bygger annerledes.)

3. **Kontroller miljøvariabler**
   - Ingen `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` som tvinger Postgres-URL.
