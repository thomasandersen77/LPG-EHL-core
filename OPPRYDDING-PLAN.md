# LPG-EHL Repo-opprydding – Plan og instruksjoner

**Dato:** 2026-02-15  
**Status:** Klar for gjennomføring  
**Mål:** Fjerne utdaterte docs, konsolidere duplikater, og rydde rotmappen

---

## 1. Oversikt over kodemoduler (OK – ingen endring nødvendig)

Prosjektet har 10 Maven-moduler som bygges til **3 kjørbare JARs**:

| JAR | Modul | Beskrivelse |
|-----|-------|-------------|
| `lpg-ehl-webapp.jar` | `lpg-ehl-webapp` | Webapp med React + REST API |
| `lpg-ehl-headless.jar` | `lpg-ehl-app-headless` | Headless bakgrunnsservice |
| `pls-sim.jar` | `lpg-ehl-serialport-sim` | PLS-simulator |

**Bibliotekmoduler:** `lpg-ehl-core`, `lpg-transport`, `lpg-ehl-emulator`, `lpg-ehl-service`, `lpg-ehl-api`, `lpg-ehl-payment-terminal-sim`, `lpg-ehl-payment-terminal-gui`

✅ Kodestrukturen stemmer med arkitekturen. Ingen kodeendringer anbefalt.

---

## 2. Filer som skal SLETTES

### 2.1 Root-filer (slett disse)

```bash
# Engangs-rapporter og duplikater
rm ALEJANDRO_ARK_TESTING.md
rm ANALYSE-RAPPORT-CORE-VS-LEGACY.md
rm ANALYSE-RAPPORT-KOTLIN-VS-VB6.md
rm CHANGES_SUMMARY.md
rm CSHARP_TCP_ANALYSIS.md
rm DEPLOYMENT_NO.md               # duplikat av docs/deployment/
rm ECR_INTEGRATION_REPORT.html
rm FIXES_SUMMARY.md
rm GETTING_STARTED.md             # overlapper README
rm IMPLEMENTASJONER-CORE.md
rm IMPLEMENTASJONSPLAN-VB6-COMPLIANCE.md
rm INTELLIJ_FULL_STACK.md         # duplikat av docs/development/
rm INTELLIJ_SETUP.md              # duplikat av docs/development/
rm MONOLITH_DEPLOYMENT.md         # duplikat av docs/deployment/
rm NETS_CLOUD_CONNECT.md          # duplikat av docs/
rm QUICKSTART.md                  # overlapper README
rm RUNNING.md                     # overlapper HEADLESS_USAGE
rm SAMLET-VURDERING-WARP-VS-GEMINI.md
rm SIKKERHETSVURDERING-GEMINI-ANALYSE.md
rm TEGNAL_H2_OPPSTART_OG_ENDRINGER.md
rm TESTING-README.md
rm TEST_GUIDE.md
rm WARP.md.backup
rm WIREMOCK-CAPTURE.md
rm ZIP-FILER-FOR-CHATGPT.md
rm payment-terminal-api-report.md
rm ssh-server-report.md
rm .output.txt

# Loggfiler (bør aldri vært i git)
rm headless.log
rm headless.log.2026-01-22.0.log

# ZIP-filer (store, bør ikke ligge i repo)
rm docs-for-ai.zip
rm lpg-ehl-cli-for-ai.zip
rm lpg-ehl-core-for-ai.zip
rm lpg-ehl-emulator-for-ai.zip
rm legacy-curated.zip
rm more_legacy.zip
rm norgesgass-legacy-for-ai.zip
rm norgesgass_legacy.zip
rm python-legacy-for-ai.zip
rm python-test.zip
rm test-python.zip

# Diverse
rm test_betaling.py.save
rm AUTHORIZED_WAITING
```

### 2.2 docs/ filer (slett disse)

```bash
# AI-verktøy guider (ikke prosjektrelevant)
rm docs/general/CHATGPT_UPLOAD_GUIDE.md
rm docs/general/CHATGPT_VERIFICATION_PACKAGE.md
rm docs/LPG-EHL-CustomGPT-Instructions.md

# Utdaterte planer og engangs-rapporter
rm docs/general/DELIVERY_MANIFEST.md
rm docs/general/IMPLEMENTATION_COMPLETE.md
rm docs/general/IMPLEMENTATION_PLAN_FINAL.md
rm docs/general/IMPLEMENTATION_PLAN_PAYMENT_RESET.md
rm docs/general/IMPLEMENTATION_ROADMAP.md
rm docs/general/IMPLEMENTATION_SUMMARY.md
rm docs/general/PROTOCOL_FIXES_SUMMARY.md
rm docs/general/PROTOCOL_HARDENING_COMPLETE.md
rm docs/general/PARTS_3_4_IMPLEMENTATION.md

# Duplikater
rm docs/general/QUICK_REFERENCE.md
rm docs/general/QUICK_START.md
rm docs/general/QUICKSTART.md
rm docs/general/DOCKER-COMPOSE-README.md
rm docs/general/DEPLOYMENT_QUICKSTART.md
rm docs/implementation/COMPREHENSIVE_IMPLEMENTATION_REPORT.md  # duplikat
rm docs/implementation/VB6_PROTOCOL_IMPLEMENTATION_COMPLETE.md
rm docs/implementation/VB6_COMPATIBILITY_TEST.md

# Utdatert research
rm docs/ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md
rm docs/ARCHITECTURE_ANALYSIS.md     # erstattet av _2026-versjonen
rm docs/AZURE-SYNC-VISUALIZATION.md
rm docs/AZURITE-MONITORING.md
rm docs/COMPLIANCE_VB6_PY_CORE.md
rm docs/CURL_FIELD_DEBUG.md
rm docs/PLAN_LOGBACK_OG_MDC_ACTOR.md
rm docs/PUMP_AUTHORIZATION_RESET.md
rm docs/HARDWARE_MODE_DETECTION.md
rm docs/IMPLEMENTATION_MODE_PARITY_FIX.md
rm docs/refactor_transport_cleanup.md
rm docs/terminal_refactor_results_2026-02-12.md
rm docs/changes/ -rf
rm docs/css/ -rf

# Redundante Baxi-rapporter (behold kun COMPREHENSIVE og KOTLIN)
rm docs/BAXI_DECOMPILED_KOTLIN_RAPPORT.md
rm docs/BAXI_DECOMPILED_PYTHON_RAPPORT.md
rm docs/BAXI_DECOMPILED_SAMLET_RAPPORT.md

# Testing-idénotater (ikke implementert)
rm docs/testing/add_more_functions.md
rm docs/testing/demo-frontend.md
rm docs/testing/product_all_functoins.md
rm docs/testing/vipps_payment.md

# Store filer som bør arkiveres
rm docs/EHL_komplett_tekst_dump.txt    # 300KB textdump
rm docs/miglpg.no.json                # 674KB JSON
```

### 2.3 instructions/ filer (slett disse)

```bash
rm instructions/logg_webapp_errors.txt       # 701KB error-logg
rm instructions/API_TEST_RESULTS.md
rm instructions/new-refactor.md
rm instructions/refactor/refactor_azure_config.md
rm instructions/refactor/refactor_emulator.md
rm instructions/refactor/refactor_emulator_v2.md
rm instructions/refactor/refactor_simulator.md
rm instructions/refactor/refactor_simulator2.md
rm instructions/refactor/refactor_modules_move_logic.md
```

---

## 3. Filer som skal FLYTTES

```bash
# Fra root til docs/
mv ARCHITECTURE.md docs/architecture/
mv GUIDE_H2_OG_PROTOKOLL.md docs/configuration/
mv GUIDE_H2_OPPDATERING_2026.md docs/configuration/
mv HEADLESS_USAGE.md docs/
mv CONFIGURATION_GUIDE.md docs/configuration/
mv EXTERNAL_CONFIG_README.md docs/configuration/
mv FELTESTING.md docs/testing/
mv TROUBLESHOOTING.md docs/

# OpenAPI-specifikasjoner
mv openapi.yaml docs/
mv openapi-payment-terminal.yaml docs/
```

---

## 4. Legg til i .gitignore

```gitignore
# Loggfiler
*.log
*.log.*

# ZIP-arkiver
*.zip

# Python virtualenv
venv/

# Runtime-data
logs/
data/

# IDE backup
*.backup
```

---

## 5. Feil i dokumentasjon som bør fikses

| Fil | Problem | Fix |
|-----|---------|-----|
| `ARCHITECTURE.md` | Refererer til `lpg-ehl-cli` som ikke eksisterer | Fjern alle CLI-referanser |
| `ARCHITECTURE.md` | Sier Java 21 | Endre til Java 17 (per pom.xml) |
| `ARCHITECTURE.md` | Sier Kotlin 1.9+ | Endre til Kotlin 2.1 (per pom.xml) |
| `docs/SERIAL_CONTRACT.md` | Sier Parity=Even (8E1) | Legg til at dette er **konfigurerbart** |
| `docs/general/EHL_PROTOKOLL_SPESIFIKASJON.md` | Sier Parity=None | Legg til at dette er **konfigurerbart** |

---

## 6. Instruksjoner for AI-assistert opprydding

### Alternativ A: Cursor

1. Åpne prosjektet i Cursor
2. Trykk `Cmd+L` for å åpne chat
3. Lim inn følgende prompt:

```
Les filen OPPRYDDING-PLAN.md i prosjektroten.
Kjør alle rm-kommandoene under seksjon 2 (SLETTES) og mv-kommandoene
under seksjon 3 (FLYTTES). Opprett eventuelle manglende mapper først.
Ikke gjør noen kodeendringer, kun slett og flytt markdown-filer.
Oppdater .gitignore med innholdet fra seksjon 4.
```

4. Gjennomgå og godkjenn endringene i Cursor sin diff-visning

### Alternativ B: Antigravity (Gemini)

1. Åpne Antigravity i prosjektet
2. Gi følgende instruksjon:

```
Les OPPRYDDING-PLAN.md og utfør oppryddingen. Kjør alle slettekommandoene
i seksjon 2, flytt filene i seksjon 3, og oppdater .gitignore per seksjon 4.
Etter oppryddingen, fiks dokumentasjonsfeilene i seksjon 5.
Bekreft med en oppsummering av hva som ble gjort.
```

### Alternativ C: Warp Terminal

Warp sin AI-funksjon kan generere og kjøre shell-kommandoer direkte:

1. Åpne Warp Terminal
2. `cd /Users/tandersen/git/NorgesGass/lpg-ehl`
3. Bruk Warp AI (`#`-knappen) og skriv:

```
Slett alle filene listet under seksjon 2 i OPPRYDDING-PLAN.md
```

Eller kjør hele scriptet manuelt – her er alt samlet:

```bash
#!/bin/bash
# LPG-EHL Opprydding – Kjør fra prosjektroten
set -e

echo "🗑️  Sletter root-filer..."
rm -f ALEJANDRO_ARK_TESTING.md ANALYSE-RAPPORT-CORE-VS-LEGACY.md \
  ANALYSE-RAPPORT-KOTLIN-VS-VB6.md CHANGES_SUMMARY.md CSHARP_TCP_ANALYSIS.md \
  DEPLOYMENT_NO.md ECR_INTEGRATION_REPORT.html FIXES_SUMMARY.md \
  GETTING_STARTED.md IMPLEMENTASJONER-CORE.md \
  IMPLEMENTASJONSPLAN-VB6-COMPLIANCE.md INTELLIJ_FULL_STACK.md \
  INTELLIJ_SETUP.md MONOLITH_DEPLOYMENT.md NETS_CLOUD_CONNECT.md \
  QUICKSTART.md RUNNING.md SAMLET-VURDERING-WARP-VS-GEMINI.md \
  SIKKERHETSVURDERING-GEMINI-ANALYSE.md TEGNAL_H2_OPPSTART_OG_ENDRINGER.md \
  TESTING-README.md TEST_GUIDE.md WARP.md.backup WIREMOCK-CAPTURE.md \
  ZIP-FILER-FOR-CHATGPT.md payment-terminal-api-report.md \
  ssh-server-report.md .output.txt headless.log headless.log.*.log \
  test_betaling.py.save AUTHORIZED_WAITING

echo "🗑️  Sletter ZIP-filer..."
rm -f docs-for-ai.zip lpg-ehl-cli-for-ai.zip lpg-ehl-core-for-ai.zip \
  lpg-ehl-emulator-for-ai.zip legacy-curated.zip more_legacy.zip \
  norgesgass-legacy-for-ai.zip norgesgass_legacy.zip \
  python-legacy-for-ai.zip python-test.zip test-python.zip

echo "🗑️  Sletter docs/ utdaterte filer..."
rm -f docs/general/CHATGPT_UPLOAD_GUIDE.md \
  docs/general/CHATGPT_VERIFICATION_PACKAGE.md \
  docs/LPG-EHL-CustomGPT-Instructions.md \
  docs/general/DELIVERY_MANIFEST.md docs/general/IMPLEMENTATION_COMPLETE.md \
  docs/general/IMPLEMENTATION_PLAN_FINAL.md \
  docs/general/IMPLEMENTATION_PLAN_PAYMENT_RESET.md \
  docs/general/IMPLEMENTATION_ROADMAP.md \
  docs/general/IMPLEMENTATION_SUMMARY.md \
  docs/general/PROTOCOL_FIXES_SUMMARY.md \
  docs/general/PROTOCOL_HARDENING_COMPLETE.md \
  docs/general/PARTS_3_4_IMPLEMENTATION.md \
  docs/general/QUICK_REFERENCE.md docs/general/QUICK_START.md \
  docs/general/QUICKSTART.md docs/general/DOCKER-COMPOSE-README.md \
  docs/general/DEPLOYMENT_QUICKSTART.md \
  docs/implementation/COMPREHENSIVE_IMPLEMENTATION_REPORT.md \
  docs/implementation/VB6_PROTOCOL_IMPLEMENTATION_COMPLETE.md \
  docs/implementation/VB6_COMPATIBILITY_TEST.md \
  docs/ANALYSE_PYTHON_TEST_VS_KOTLIN_CORE.md \
  docs/ARCHITECTURE_ANALYSIS.md docs/AZURE-SYNC-VISUALIZATION.md \
  docs/AZURITE-MONITORING.md docs/COMPLIANCE_VB6_PY_CORE.md \
  docs/CURL_FIELD_DEBUG.md docs/PLAN_LOGBACK_OG_MDC_ACTOR.md \
  docs/PUMP_AUTHORIZATION_RESET.md docs/HARDWARE_MODE_DETECTION.md \
  docs/IMPLEMENTATION_MODE_PARITY_FIX.md \
  docs/refactor_transport_cleanup.md \
  docs/terminal_refactor_results_2026-02-12.md \
  docs/BAXI_DECOMPILED_KOTLIN_RAPPORT.md \
  docs/BAXI_DECOMPILED_PYTHON_RAPPORT.md \
  docs/BAXI_DECOMPILED_SAMLET_RAPPORT.md \
  docs/EHL_komplett_tekst_dump.txt docs/miglpg.no.json
rm -rf docs/changes/ docs/css/
rm -f docs/testing/add_more_functions.md docs/testing/demo-frontend.md \
  docs/testing/product_all_functoins.md docs/testing/vipps_payment.md

echo "🗑️  Sletter instructions/ utdaterte filer..."
rm -f instructions/logg_webapp_errors.txt instructions/API_TEST_RESULTS.md \
  instructions/new-refactor.md instructions/refactor/refactor_azure_config.md \
  instructions/refactor/refactor_emulator.md \
  instructions/refactor/refactor_emulator_v2.md \
  instructions/refactor/refactor_simulator.md \
  instructions/refactor/refactor_simulator2.md \
  instructions/refactor/refactor_modules_move_logic.md

echo "📦  Oppretter mapper og flytter filer..."
mkdir -p docs/architecture docs/configuration
mv -f ARCHITECTURE.md docs/architecture/ 2>/dev/null || true
mv -f GUIDE_H2_OG_PROTOKOLL.md docs/configuration/ 2>/dev/null || true
mv -f GUIDE_H2_OPPDATERING_2026.md docs/configuration/ 2>/dev/null || true
mv -f HEADLESS_USAGE.md docs/ 2>/dev/null || true
mv -f CONFIGURATION_GUIDE.md docs/configuration/ 2>/dev/null || true
mv -f EXTERNAL_CONFIG_README.md docs/configuration/ 2>/dev/null || true
mv -f FELTESTING.md docs/testing/ 2>/dev/null || true
mv -f TROUBLESHOOTING.md docs/ 2>/dev/null || true
mv -f openapi.yaml docs/ 2>/dev/null || true
mv -f openapi-payment-terminal.yaml docs/ 2>/dev/null || true

echo "📝  Oppdaterer .gitignore..."
cat >> .gitignore << 'EOF'

# === Opprydding 2026-02-15 ===
*.log
*.log.*
*.zip
venv/
logs/
data/
*.backup
EOF

echo ""
echo "✅  Opprydding ferdig!"
echo "   Kjør 'git status' for å se alle endringer."
echo "   Kjør 'git add -A && git commit -m \"Opprydding: fjernet utdaterte docs\"' for å committe."
```

---

## 7. Etter opprydding – Sjekkliste

- [ ] Kjør `git status` og verifiser at riktige filer er slettet/flyttet
- [ ] Kjør `mvn clean compile -DskipTests` for å verifisere at koden fortsatt bygger
- [ ] Oppdater `docs/README.md` til å reflektere ny struktur
- [ ] Fiks dokumentasjonsfeilene i seksjon 5
- [ ] Commit med beskrivende melding
- [ ] Push til remote

---

## 8. Dokumenter som er BEHOLDT (og hvorfor)

| Dokument | Verdi |
|----------|-------|
| `docs/general/EHL_PROTOKOLL_SPESIFIKASJON.md` | **Gullstandard** – komplett protokollspec |
| `docs/RS485_PUMP_COMMUNICATION_GUIDE.md` | Praktisk wire-level referanse |
| `docs/SERIAL_CONTRACT.md` | Feltdokumentasjon med UART, udev, Docker |
| `docs/BAXI_PROTOCOL_COMPREHENSIVE_ANALYSIS.md` | Research – dekompilert Baxi DLL |
| `docs/BAXI_PROTOCOL_KOTLIN_IMPLEMENTATION.md` | Implementasjonsstrategi |
| `docs/BAXI_PROTOCOL_PYTHON_IMPLEMENTATION.md` | Python-probe strategy |
| `docs/TRANSPORT_MODES.md` | Lab/Field/Socat forklaring |
| `docs/EHL_COMMANDS_GUIDE.md` | Kommandoreferanse |
| `docs/ARCHITECTURE_ANALYSIS_2026.md` | Fersk arkitekturanalyse |
| `docs/ARKITEKTUR_ANALYSE_EDGE_2026.md` | Edge-spesifikk analyse |
| `docs/VB6_LEGACY_PUMPEKONTROLL_SPEC.md` | Legacy-referanse |
| `docs/DATABASE_INTEGRATION_TESTING.md` | Teststrategi |
| `docs/ecr-integration/` | ECR-analyse (hele mappen) |
| `docs/modbus_RnD/` | Modbus-research for fremtid |
| `docs/_research/` | COM-port research |
| `docs/deployment/` | Deployment-guider |
| `docs/development/` | Utvikler-guider |
| `docs/2026/` | Ferske rapporter |
| `instructions/review-2026.md` | Utmerket arkitektur-review |
| `instructions/core/` | Core-instruksjoner |
| `instructions/terminal/` | Terminal-guider |
| `instructions/stationowner/` | Stasjonseier-planer |
