# LPG EHL Developer Guide

Komplett guide for lokal utvikling og testing av LPG EHL systemet.

## 📋 Innholdsfortegnelse

1. [Arkitektur-oversikt](#arkitektur-oversikt)
2. [Kjøre med Docker Compose](#kjøre-med-docker-compose)
3. [Kjøre i IntelliJ IDEA](#kjøre-i-intellij-idea)
4. [Hvordan modulene kommuniserer](#hvordan-modulene-kommuniserer)
5. [Testing og debugging](#testing-og-debugging)

---

## 🏗️ Arkitektur-oversikt

### Modulstruktur

```
lpg-ehl/
├── lpg-ehl-core/          # Protokoll-implementasjon (bibliotek)
│   ├── protocol/          # EHL packet encoding/decoding
│   ├── communication/     # Serial port abstraksjon
│   └── transaction/       # Transaction state machine
│
├── lpg-ehl-emulator/      # TCP-server som simulerer pumpe
│   └── EhlDispenserEmulator.kt
│
├── lpg-ehl-api/           # Spring Boot REST API
│   ├── controller/        # REST endpoints
│   ├── service/           # Business logic
│   └── model/             # Database entities
│
└── lpg-web/               # React frontend
    ├── components/        # React komponenter
    └── api/               # API klient
```

### Dataflyt

```
┌─────────────┐         ┌──────────────┐         ┌────────────┐
│   Frontend  │◄───────►│     API      │◄───────►│ PostgreSQL │
│  (React)    │  HTTP   │ (Spring Boot)│  JDBC   │  Database  │
└─────────────┘         └──────┬───────┘         └────────────┘
                               │
                               │ TCP Socket
                               │ (EHL Protocol)
                               ▼
                        ┌──────────────┐
                        │   Emulator   │
                        │ eller        │
                        │ Ekte Pumpe   │
                        └──────────────┘
```

---

## 🐳 Kjøre med Docker Compose

### Alternativ 1: Hele stacken i Docker

Dette er den enkleste måten å få hele systemet opp og kjøre.

```bash
# Gå til prosjektmappen
cd /Users/tandersen/git/NorgesGass/lpg-ehl

# Start alle tjenester
docker-compose -f docker-compose-local.yaml up

# Eller i bakgrunnen:
docker-compose -f docker-compose-local.yaml up -d

# Se logger
docker-compose -f docker-compose-local.yaml logs -f

# Stopp alle
docker-compose -f docker-compose-local.yaml down
```

**Tjenester som startes:**

| Tjeneste | Port | Beskrivelse |
|----------|------|-------------|
| **postgres** | 5432 | PostgreSQL database |
| **azurite** | 10001 | Azure Storage emulator |
| **emulator** | 9000 | TCP-server som simulerer pumpe |
| **api** | 8080 | Spring Boot REST API |
| **frontend** | 3000 | React web-app |
| **wiremock** | 8081 | Mock-server for testing |

**Tilgang:**
- Frontend: http://localhost:3000
- API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui.html
- Database: `localhost:5432` (user: `lpg_user`, password: `lpg_dev_password`)

### Alternativ 2: Kun infrastruktur i Docker

Kjør bare database og emulator i Docker, mens du utvikler API og frontend lokalt.

```bash
# Start kun postgres, azurite og emulator
docker-compose -f docker-compose-local.yaml up postgres azurite emulator

# Eller lag en egen profil:
docker-compose -f docker-compose-local.yaml up postgres azurite emulator -d
```

Deretter kjør API og frontend i IntelliJ/terminal (se neste seksjon).

---

## 💻 Kjøre i IntelliJ IDEA

### Forutsetninger

1. **Java 21** installert via SDKMAN:
   ```bash
   sdk install java 21.0.7-tem
   sdk use java 21.0.7-tem
   ```

2. **Maven** installert:
   ```bash
   sdk install maven
   ```

3. **Node.js 18+** for frontend:
   ```bash
   brew install node
   # eller
   nvm install 18
   ```

4. **Database kjører** (via Docker):
   ```bash
   docker-compose -f docker-compose-local.yaml up postgres azurite -d
   ```

### Importere prosjektet

1. Åpne IntelliJ IDEA
2. **File → Open** → Velg `/Users/tandersen/git/NorgesGass/lpg-ehl`
3. IntelliJ vil automatisk oppdage Maven-strukturen
4. Vent til Maven har lastet ned dependencies (se nederst til høyre)

### Konfigurere Run Configurations

#### 1. Kjøre Emulator

**Run → Edit Configurations → + → Application**

```
Name: Emulator
Module: lpg-ehl-emulator
Main class: no.cloudberries.lpg.emulator.EhlEmulatorServerKt
VM options: -Demulator.port=9000 -Demulator.address=1
Working directory: $MODULE_WORKING_DIR$
```

**Eller via terminal:**
```bash
cd lpg-ehl-emulator
mvn exec:java -Dexec.mainClass="no.cloudberries.lpg.emulator.EhlEmulatorServerKt"
```

#### 2. Kjøre API

**Run → Edit Configurations → + → Spring Boot**

```
Name: API
Module: lpg-ehl-api
Main class: no.cloudberries.lpg.api.LpgEhlApiApplicationKt
Active profiles: local
Environment variables:
  DB_HOST=localhost
  DB_PORT=5432
  DB_NAME=lpg_ehl
  DB_USER=lpg_user
  DB_PASSWORD=lpg_dev_password
  EMULATOR_HOST=localhost
  EMULATOR_PORT=9000
  AZURE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;QueueEndpoint=http://localhost:10001/devstoreaccount1;
```

**Eller via terminal:**
```bash
cd lpg-ehl-api
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**API vil være tilgjengelig på:**
- REST API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

#### 3. Kjøre Frontend

**Run → Edit Configurations → + → npm**

```
Name: Frontend
Package.json: lpg-web/package.json
Command: run
Scripts: dev
```

**Eller via terminal:**
```bash
cd lpg-web
npm install  # Første gang
npm run dev
```

**Frontend vil være tilgjengelig på:**
- http://localhost:3000

### Anbefalt oppstart-rekkefølge

1. **Start infrastruktur** (database + emulator):
   ```bash
   docker-compose -f docker-compose-local.yaml up postgres azurite emulator -d
   ```

2. **Start Emulator** (hvis ikke i Docker):
   - Kjør "Emulator" run configuration i IntelliJ
   - Eller: `cd lpg-ehl-emulator && mvn exec:java`

3. **Start API**:
   - Kjør "API" run configuration i IntelliJ
   - Eller: `cd lpg-ehl-api && mvn spring-boot:run`

4. **Start Frontend**:
   - Kjør "Frontend" run configuration i IntelliJ
   - Eller: `cd lpg-web && npm run dev`

---

## 🔌 Hvordan modulene kommuniserer

### 1. Core ↔ Emulator/Pumpe (EHL Protocol)

**Core** er et bibliotek som brukes av både **API** og **Emulator**.

```kotlin
// Core definerer interface
interface SerialPortIO {
    fun send(data: ByteArray)
    fun receive(): ByteArray
}

// API bruker TCP-socket implementasjon
class TcpSerialPort(host: String, port: Int) : SerialPortIO {
    // Snakker med emulator via TCP
}

// Produksjon bruker ekte serial port
class RealSerialPort(device: String) : SerialPortIO {
    // Snakker med ekte pumpe via RS-485
}
```

**Dataflyt:**

```
API Service
    ↓
EhlCommunicator (fra core)
    ↓
TcpSerialPort (i api)
    ↓ TCP Socket (port 9000)
Emulator (lytter på port 9000)
```

### 2. API ↔ Database

API bruker Spring Data JPA for å lagre transaksjoner:

```kotlin
@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val communicator: EhlCommunicator
) {
    fun startDelivery(dispenserAddress: Int) {
        // 1. Send UNBLOCK til pumpe via core
        communicator.send(EhlPacket(dispenserAddress, EhlCommand.UNBLOCK))
        
        // 2. Motta respons
        val response = communicator.receive()
        
        // 3. Lagre transaksjon i database
        transactionRepository.save(Transaction(...))
    }
}
```

### 3. Frontend ↔ API

Frontend bruker Axios for HTTP-kall:

```typescript
// Frontend sender HTTP request
const response = await axios.post('/api/v1/dispenser/unblock');

// API mottar request
@PostMapping("/unblock")
fun unblock(): DispenserStateDto {
    // Snakker med emulator via EHL protocol
    return service.unblock()
}
```

### 4. API ↔ Azure Storage

API synkroniserer transaksjoner til Azure Queue:

```kotlin
@Scheduled(fixedDelay = 30000) // Hver 30. sekund
fun syncToAzure() {
    val unsynced = transactionRepository.findUnsynced()
    
    unsynced.forEach { transaction ->
        queueClient.sendMessage(transaction.toJson())
        transaction.syncedAt = now()
        transactionRepository.save(transaction)
    }
}
```

---

## 🧪 Testing og debugging

### Teste hele flyten

1. **Start systemet** (Docker eller IntelliJ)

2. **Åpne frontend**: http://localhost:3000

3. **Klikk på "⛽ Pumpe Simulator"**

4. **Test pumpekontroller:**
   - Klikk **"▶ Start"** → Emulator starter levering
   - Observe live-oppdatering av liter og beløp
   - Klikk **"■ Stopp"** → Emulator stopper
   - Klikk **"↻ Reset"** → Nullstill

5. **Sjekk database:**
   ```bash
   docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl
   
   SELECT * FROM transactions;
   SELECT * FROM dispenser_status;
   ```

6. **Sjekk API logs** (i IntelliJ Console eller Docker logs)

### Debug i IntelliJ

1. **Sett breakpoints** i API-koden (f.eks. `DemoDispenserController`)

2. **Start API i debug mode**: 
   - Klikk på "Debug" ikonet ved siden av run configuration

3. **Trigger request fra frontend**

4. **Inspiser variabler** når breakpoint treffer

### Teste EHL Protocol direkte

Du kan sende raw EHL-kommandoer til emulator via `nc`:

```bash
# Start emulator
docker-compose -f docker-compose-local.yaml up emulator

# Koble til med netcat
nc localhost 9000

# Send STATE command (heksadesimalt)
# STX(20) LEN(04) ADDR(01) CMD(75) CHK(50) ETX(36)
echo -ne '\x20\x04\x01\x75\x50\x36' | nc localhost 9000 | xxd
```

### Common Issues

#### 1. "Connection refused" til database

```bash
# Sjekk at postgres kjører
docker ps | grep postgres

# Start postgres hvis den ikke kjører
docker-compose -f docker-compose-local.yaml up postgres -d
```

#### 2. "Port 8080 already in use"

```bash
# Finn prosess som bruker port 8080
lsof -i :8080

# Drep prosessen
kill -9 <PID>
```

#### 3. Frontend kan ikke koble til API

Sjekk at CORS er konfigurert i API:

```kotlin
// lpg-ehl-api/src/main/kotlin/config/SecurityConfig.kt
@Bean
fun corsConfigurer() = object : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:5173")
    }
}
```

#### 4. Emulator svarer ikke

```bash
# Test at emulator lytter
nc -zv localhost 9000

# Se emulator logs
docker-compose -f docker-compose-local.yaml logs emulator
```

---

## 📚 Nyttige kommandoer

### Maven

```bash
# Bygg alle moduler
mvn clean install

# Bygg uten tester
mvn clean install -DskipTests

# Kjør kun tester
mvn test

# Kjør spesifikk test
mvn test -Dtest=TransactionTest
```

### Docker

```bash
# Se kjørende containere
docker ps

# Se alle containere (inkl. stoppede)
docker ps -a

# Se logs for spesifikk container
docker logs -f lpg-api

# Gå inn i container
docker exec -it lpg-api bash

# Restart en tjeneste
docker-compose -f docker-compose-local.yaml restart api

# Rebuild og start
docker-compose -f docker-compose-local.yaml up --build
```

### Database

```bash
# Koble til database
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl

# Kjør SQL-fil
docker exec -i lpg-postgres psql -U lpg_user -d lpg_ehl < init-db.sql

# Backup database
docker exec lpg-postgres pg_dump -U lpg_user lpg_ehl > backup.sql

# Se alle tabeller
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl -c "\dt"
```

---

## 🎯 Quick Reference

### Anbefalt utvikling-setup

**For backend-utvikling:**
```bash
# Terminal 1: Infrastruktur
docker-compose -f docker-compose-local.yaml up postgres azurite emulator -d

# Terminal 2: API (IntelliJ eller terminal)
cd lpg-ehl-api && mvn spring-boot:run

# Terminal 3: Frontend
cd lpg-web && npm run dev
```

**For frontend-utvikling:**
```bash
# Terminal 1: Alt i Docker unntatt frontend
docker-compose -f docker-compose-local.yaml up postgres azurite emulator api -d

# Terminal 2: Frontend lokalt
cd lpg-web && npm run dev
```

### Ports oversikt

| Port | Tjeneste | URL |
|------|----------|-----|
| 3000 | Frontend | http://localhost:3000 |
| 5432 | PostgreSQL | jdbc:postgresql://localhost:5432/lpg_ehl |
| 8080 | API | http://localhost:8080 |
| 8081 | WireMock | http://localhost:8081 |
| 9000 | Emulator | tcp://localhost:9000 |
| 10001 | Azurite | http://localhost:10001 |

---

**Lykke til med utviklingen! 🚀**
