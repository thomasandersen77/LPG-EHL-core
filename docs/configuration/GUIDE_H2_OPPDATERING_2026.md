# H2 Oppdatering 2026 – Oppdatert vurdering og rettelser

**Dato:** 2026-01-24  
**Formål:** Supplere og korrigere [GUIDE_H2_OG_PROTOKOLL.md](GUIDE_H2_OG_PROTOKOLL.md) slik at beskrivelsen matcher nåværende kode og changelog-oppsett.

---

## 1. Konklusjon

**Ja.** Du kan starte `lpg-ehl-app-headless` med H2 in-memory for felttesting på en fysisk pumpe, forutsatt at du:

- Kjører med `--spring.profiles.active=h2`
- Bygger JAR fra headless-modulen
- Ikke har `DB_*` eller andre miljøvariabler som tvinger Postgres-URL

---

## 2. Hva som stemmer i den opprinnelige guiden

- **H2-avhengighet:** `com.h2database:h2` finnes i `lpg-ehl-app-headless/pom.xml` som runtime. Ingen endring nødvendig.
- **application-h2.yaml:** Finnes i `lpg-ehl-app-headless/src/main/resources/application-h2.yaml` med H2 in-memory, `MODE=PostgreSQL`, og riktig driver.
- **Kjør med H2-profil:** Kommandoen med `--spring.profiles.active=h2` er riktig. JAR-navnet er typisk `lpg-ehl-app-headless-0.0.1-SNAPSHOT.jar` avhengig av Maven-build.
- **Azure deaktivert:** `azure.enabled: false` i H2-profilen; Azure-tjenester er `@ConditionalOnProperty(azure.enabled=true)` og skrus derfor av.
- **Liquibase:** Aktiv changelog er `classpath:db/changelog/db.changelog-master.yaml`. Changelog-filene ligger i service-modulen og pakkes med i headless-JAR.

---

## 3. Rettelser og presiseringer

### 3.1 Changelog: YAML vs XML – 005 kjøres aldri

Den opprinnelige guiden sier at `005-fix-trigger-transaction-id.xml` er Postgres-spesifikk og at Liquibase **hopper over** den for H2.

**Faktisk:** Applikasjonen bruker **kun** `db.changelog-master.yaml`. Den inkluderer:

- `001-initial-schema.yaml`
- `002-fix-daily-summary-view.yaml`
- `006-add-payment-status-column.yaml`
- `007-add-price-history-table.yaml`
- `008-add-road-tax-settings.yaml`
- `009-add-pump-authorization-table.yaml`

**005 er ikke med i YAML-master.** Den ligger i `db.changelog-master.xml`, som **ikke** brukes av headless. 003, 004 og 005 kjører derfor aldri når du starter med H2 (eller Postgres) med nåværende konfigurasjon. At 005 «skippes» for H2 er misvisende – den kjøres ikke i det hele tatt.

### 3.2 JSON/JSONB i 001

Guiden oppgir at skjemaene bruker «standard SQL-typer … UUID og JSON».

I `001-initial-schema.yaml` er `decoded_data` og `payload` satt som **`type: json`** (ikke `jsonb`). Det finnes `modifySql` for H2 som erstatter **`jsonb`** med `clob`. Siden typen i changelog er `json`, genererer Liquibase sannsynligvis `JSON`, så replace treffer kanskje ikke. H2 i `MODE=PostgreSQL` støtter `JSON`; dermed kan det likevel fungere. Beskrivelsen i guiden er unøyaktig, og modifySql er i praksis rettet mot `jsonb` mens changelog bruker `json`. Oppsettet er likevel H2-kompatibelt.

### 3.3 ddl-auto: update vs validate

I `application-h2.yaml` står `spring.jpa.hibernate.ddl-auto: update`. I base `application.yaml` står `validate`.

Guiden nevner ikke dette. Med både Liquibase og `update` kan Hibernate utføre egne schemaendringer i tillegg til Liquibase. Det fungerer ofte, men gir dobbel schema-håndtering og mulig drift. **Anbefaling:** Bruk `validate` dersom du vil styre schema utelukkende via Liquibase.

### 3.4 Changelog 009

`009-add-pump-authorization-table.yaml` er med i YAML-master og kjører ved oppstart. Den bruker kun standardtyper (UUID, INTEGER, VARCHAR, TIMESTAMP, osv.) og ingen Postgres-spesifikke konstruksjoner. Guiden nevner ikke 009; den er H2-kompatibel og påvirker ikke start med H2.

---

## 4. H2-relevante changelogs (YAML-master)

| Fil | H2-relevant |
|-----|-------------|
| 001-initial-schema | `json`-kolonner; modifySql erstatter `jsonb`→clob (kan være no-op). H2 JSON bør fungere. |
| 002-fix-daily-summary-view | `CAST(timestamp AS DATE)` – standard SQL, OK for H2. |
| 006-add-payment-status-column | `varchar`, `clob`, vanlig `UPDATE` – OK. |
| 007-add-price-history-table | Bruker `RANDOM_UUID()` i INSERT – H2-kompatibelt. |
| 008-add-road-tax-settings | `RANDOM_UUID()`, `COMMENT ON COLUMN` kun for `dbms: postgresql` – OK. |
| 009-add-pump-authorization-table | Kun standardtyper – OK. |

---

## 5. Sjekkliste for oppstart med H2

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
   - H2-profilen bruker `jdbc:h2:mem:lpgdb;...` uavhengig av `DB_*`, men base-config kan override ved feil profil.

---

## 6. Kort oppsummert

| Punkt | Opprinnelig guide | Faktisk |
|-------|-------------------|--------|
| H2 + profil | Riktig | Riktig |
| 005 skippes for H2 | Nevnt | 005 kjøres ikke (ikke i YAML-master) |
| Typer i 001 | «UUID og JSON» | `json` brukes; modifySql matcher `jsonb` |
| ddl-auto | Ikke nevnt | H2: `update`; base: `validate` |
| 009 | Ikke nevnt | I bruk, H2-kompatibel |

**Startoppsummering:** Applikasjonen kan startes med H2. Denne filen korrigerer og supplerer guiden slik at den matcher nåværende kode og changelog-oppsett.
