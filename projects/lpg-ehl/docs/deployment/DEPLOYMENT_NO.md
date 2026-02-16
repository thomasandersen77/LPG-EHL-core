# Deployment Guide (Norsk)

## 📦 Prosjektstruktur (Monolith SPA)

Din struktur er **perfekt** for produksjon! Her er hvordan det fungerer:

```
lpg-ehl/
├── lpg-web/                    # React frontend (kun for bygging)
│   ├── package.json           # Node dependencies (npm/vite)
│   ├── src/                   # React source code
│   └── dist/                  # Bygget frontend (etter npm run build)
│
└── lpg-ehl-api/               # Spring Boot backend
    ├── src/main/resources/
    │   └── static/            # Frontend kopieres hit
    └── target/
        └── lpg-ehl-api.jar    # Fat JAR (backend + frontend)
```

### Hvordan det fungerer:

1. **Utvikling (IntelliJ)**:
   - Du kjører Spring Boot (port 8080) med `LPG-EHL-API (Local Dev)` config
   - Frontend er allerede bygget og servet fra `static/`
   - PostgreSQL + Azurite kjører i Docker

2. **Bygging (CI/CD eller manuelt)**:
   ```bash
   ./build_monolith.sh
   ```
   - Bygger React frontend med Vite: `lpg-web/dist/`
   - Kopierer til Spring Boot: `lpg-ehl-api/src/main/resources/static/`
   - Bygger Fat JAR med Maven: `lpg-ehl-api.jar`
   - Kopierer til: `release/lpg-ehl-monolith.jar`

3. **Produksjon (Linux)**:
   - En enkelt JAR-fil: `lpg-ehl-monolith.jar`
   - Inneholder ALT: Backend + Frontend + Dependencies
   - Direkte kjørbar: `./lpg-ehl-monolith.jar`

## ✅ Svar på dine spørsmål

### 1. Trenger lpg-web Node backend?

**NEI!** Node.js brukes KUN for å bygge frontend:
- ✅ `npm run build` → Kompilerer React/TypeScript til statiske filer
- ✅ Vite/Tailwind → Optimaliserer CSS og JS
- ❌ Ingen Node backend i produksjon
- ❌ Ingen npm/Node på Linux-serveren

### 2. Kan du fortsette med denne strukturen?

**JA, absolutt!** Strukturen er best practice:
- ✅ Monolith SPA (Spring Boot + React i samme JAR)
- ✅ Separerte mapper for frontend/backend under utvikling
- ✅ Enkel deployment (kun én JAR-fil)
- ✅ Ingen CORS-problemer (alt serveres fra port 8080)

### 3. Holder build-scriptet for Linux?

**JA!** Scriptet er komplett:
- ✅ Bygger React frontend
- ✅ Kopierer til Spring Boot static resources
- ✅ Bygger Fat JAR med alle dependencies
- ✅ `<executable>true</executable>` i pom.xml → Kjørbar på Linux
- ✅ Ingen Docker/Node trengs på Linux-serveren

## 🚀 Deployment til Linux (ARK-maskin)

### Steg 1: Bygg monolith lokalt

```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
./build_monolith.sh
```

**Output:**
```
release/lpg-ehl-monolith.jar  (~80-100 MB)
```

### Steg 2: Test lokalt (valgfritt)

```bash
# Sørg for at PostgreSQL + Azurite kjører
docker-compose -f docker-compose.postgres.yaml up -d

# Test JAR-filen
java -jar release/lpg-ehl-monolith.jar

# Eller direkte
./release/lpg-ehl-monolith.jar
```

Frontend: http://localhost:8080  
API: http://localhost:8080/api/v1/  
Swagger: http://localhost:8080/swagger-ui.html

### Steg 3: Kopier til Linux-server

```bash
# SCP til ARK-maskin
scp release/lpg-ehl-monolith.jar user@ark-machine:/opt/lpg-ehl/
```

### Steg 4: Installer på Linux

```bash
# SSH inn på ARK-maskinen
ssh user@ark-machine

# Stopp gammel versjon
sudo systemctl stop lpg-ehl

# Erstatt JAR
sudo mv /opt/lpg-ehl/lpg-ehl-monolith.jar /opt/lpg-ehl/lpg-ehl.jar

# Start ny versjon
sudo systemctl start lpg-ehl

# Sjekk status
sudo systemctl status lpg-ehl
```

## 📋 Systemd Service (Linux)

Opprett `/etc/systemd/system/lpg-ehl.service`:

```ini
[Unit]
Description=LPG EHL Monolith (Spring Boot + React)
After=network.target postgresql.service

[Service]
Type=simple
User=lpg
WorkingDirectory=/opt/lpg-ehl
ExecStart=/opt/lpg-ehl/lpg-ehl.jar
Restart=on-failure
RestartSec=10

# Environment variables
Environment="SPRING_PROFILES_ACTIVE=production"
Environment="DB_HOST=localhost"
Environment="DB_PORT=5432"
Environment="DB_NAME=lpg_ehl"
Environment="DB_USER=lpg_user"
Environment="DB_PASSWORD=secure_password_here"
Environment="AZURE_CONNECTION_STRING=your_azure_connection_string"
Environment="PORT=8080"

[Install]
WantedBy=multi-user.target
```

**Aktiver service:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable lpg-ehl
sudo systemctl start lpg-ehl
```

## 🔍 Feilsøking

### JAR kjører ikke

```bash
# Sjekk at JAR er executable
ls -la /opt/lpg-ehl/lpg-ehl.jar
# Bør vise: -rwxr-xr-x

# Hvis ikke:
chmod +x /opt/lpg-ehl/lpg-ehl.jar
```

### Database connection errors

```bash
# Sjekk PostgreSQL
sudo systemctl status postgresql
psql -h localhost -U lpg_user -d lpg_ehl

# Verifiser environment variables
sudo systemctl show lpg-ehl --property=Environment
```

### Port 8080 allerede i bruk

```bash
# Finn prosess
sudo lsof -i :8080

# Eller bruk annen port
Environment="PORT=8081"
```

## 📊 Hva inkluderes i JAR-filen?

```
lpg-ehl-monolith.jar (~80-100 MB)
├── BOOT-INF/
│   ├── classes/
│   │   ├── no/cloudberries/lpg/         # Kotlin backend code
│   │   ├── static/                      # React frontend
│   │   │   ├── index.html
│   │   │   ├── assets/
│   │   │   │   ├── index-abc123.js
│   │   │   │   └── index-def456.css
│   │   └── db/changelog/                # Liquibase migrations
│   └── lib/                             # All dependencies
│       ├── spring-boot-*.jar
│       ├── postgresql-*.jar
│       ├── kotlin-stdlib-*.jar
│       └── ... (100+ dependencies)
└── META-INF/
```

## 🎯 Oppsummering

**✅ STRUKTUR:** Perfekt som den er - behold lpg-web som egen modul for utvikling  
**✅ BUILD:** `./build_monolith.sh` er komplett og klar for produksjon  
**✅ DEPLOY:** Én JAR-fil - ingen Docker/Node trengs på Linux  
**✅ EXECUTABLE:** JAR-filen kjører direkte på Linux (`./lpg-ehl-monolith.jar`)

**Din workflow:**
1. Utvikle i IntelliJ (Spring Boot + React hot reload)
2. Bygg med `./build_monolith.sh` når klar
3. Deploy `release/lpg-ehl-monolith.jar` til Linux
4. Profit! 🚀

**Ingen endringer nødvendig - alt er satt opp riktig!**
