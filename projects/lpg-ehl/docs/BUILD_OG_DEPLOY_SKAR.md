# Bygg- og deploy-skår: React eksternalisert, én Java-prosess på edge

**Kort:** Frontend bygges med Node (Build Mona Lisa), kopieres til webapp `src/main/resources` som statisk innhold. Edge-enheten kjører kun én Kotlin/JVM-prosess som serverer både API og den ferdigbyggede frontenden fra JAR — uten Node på arkmaskinen.

---

## 1. Overblikk

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  BYGG (CI / utvikler-PC)                                                     │
│  Node-miljø tilgjengelig                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │  Build Mona Lisa
         │  • npm install / npm run build (React/Vite)
         │  • Kopier dist/ → lpg-ehl-webapp/src/main/resources/static
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  lpg-ehl-webapp                                                              │
│  src/main/resources/static/   ← statiske filer (index.html, JS, CSS)        │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │  mvn package
         │  • Kotlin-kompilering, Spring Boot repackage
         │  • Statiske filer pakkes inn i JAR
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  lpg-ehl-webapp-*.jar (fat JAR)                                              │
│  • Én JVM-prosess                                                            │
│  • REST API + statisk fil-servering fra classpath/resources                  │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         │  deploy til edge (systemd / Docker / manuelt)
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ARKMASKIN (edge)                                                            │
│  • Kun Java/Kotlin-runtime                                                   │
│  • Ingen Node, ingen npm                                                     │
│  • Én prosess: API + frontend ut av samme JAR                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Build Mona Lisa (detaljert skår)

| Steg | Hva skjer | Hvor |
|------|-----------|------|
| 1 | Node kjører (npm install, npm run build) | Bygg-miljø / CI / utvikler-PC |
| 2 | React/Vite produserer `lpg-web/dist/` | Samme |
| 3 | Skript kopierer innhold til `lpg-ehl-webapp/src/main/resources/static/` | Samme |
| 4 | Maven bygger webapp-modulen; `static/` havner i JAR under `BOOT-INF/classes/static/` | Bygg-miljø |
| 5 | Ferdig JAR deployes til edge | Edge-enhet |

**Viktig:** Alt som har med Node og React-bygg skjer **før** JAR-en lages. Edge ser bare JAR + JVM.

---

## 3. Hvorfor denne skåren?

- **Edge spares for Node:** Ingen Node-runtime, ingen npm, ingen ekstra prosesser på arkmaskinen.
- **Én prosess:** Én Kotlin/Spring Boot-prosess serverer både API og ferdigbygget frontend (statisk fra `resources`).
- **Klar separasjon:** React-koden lever i eget repo/modul (`lpg-web`); kun bygg-**resultatet** havner i webapp som statisk innhold. Kildekoden til frontend er dermed eksternalisert, mens **kjøringen** skjer inne i JAR-en på edge.

---

## 4. Ark-enhet (edge) – faktisk tilstand

| Komponent | På edge? |
|-----------|----------|
| JVM (Java/Kotlin) | ✅ Én prosess |
| Spring Boot (Undertow) | ✅ Inne i JAR |
| REST API | ✅ Samme JAR |
| Frontend (HTML/JS/CSS) | ✅ Servert som statisk fra JAR |
| Node / npm | ❌ Ikke installert |
| React kildekode / Vite | ❌ Ikke tilstede |

---

## 5. Oppsummering

**Build Mona Lisa** sikrer at React-frontenden bygges med Node og kopieres til Web App sin **Source Main Resources** (statisk). Derfra pakkes den inn i webapp-JAR. På arkmaskinen kjører **bare én Java-prosess** skrevet i Kotlin; den serverer både API og den ferdigbyggede frontenden fra JAR-filen. Frontenden er dermed eksternalisert i **bygg**, men **kjøres** inne i webapp-JAR for å spare edge-enheten for Node-miljøet.
