# LPG EHL - Quick Start Guide

Start hele systemet på 2 minutter! ⚡

## 🚀 Raskeste måte (Docker Compose)

```bash
# 1. Gå til prosjektmappen
cd /Users/tandersen/git/NorgesGass/lpg-ehl

# 2. Start alle tjenester
docker-compose -f docker-compose-local.yaml up

# 3. Åpne nettleseren
# Frontend: http://localhost:3000
```

**Det er alt!** 🎉

---

## 📱 Bruke systemet

### Steg 1: Åpne Frontend
Åpne nettleseren på: **http://localhost:3000**

Du vil se landingssiden med oversikt over alle tjenester.

### Steg 2: Start Pumpe-simulator
Klikk på den store grønne knappen: **"⛽ Åpne Pumpe Simulator"**

### Steg 3: Test pumpefunksjonalitet

Nå kan du simulere en komplett pumpe-transaksjon:

1. **▶ Start** - Begynn drivstoffleveringen
   - Pumpen starter å levere 0.5 L/s
   - Liter og beløp oppdateres live (hver 0.5 sekund)

2. **■ Stopp** - Avslutt leveringen
   - Pumpen stopper
   - Viser totalt beløp og liter

3. **↻ Reset** - Nullstill pumpen
   - Tilbake til IDLE-tilstand
   - Klar for ny transaksjon

### Steg 4: Se resultater i database

```bash
# Koble til database
docker exec -it lpg-postgres psql -U lpg_user -d lpg_ehl

# Se transaksjoner
SELECT * FROM transactions;

# Se pumpestatus
SELECT * FROM dispenser_status;
```

---

## 🛑 Stoppe systemet

```bash
# Stopp alle tjenester
docker-compose -f docker-compose-local.yaml down

# Eller bare Ctrl+C i terminalen hvor de kjører
```

---

## 🔗 Nyttige lenker

Når systemet kjører, har du tilgang til:

| Tjeneste | URL | Beskrivelse |
|----------|-----|-------------|
| **Frontend** | http://localhost:3000 | Web-grensesnitt |
| **API** | http://localhost:8080 | REST API |
| **Swagger** | http://localhost:8080/swagger-ui.html | API-dokumentasjon |
| **Health Check** | http://localhost:8080/actuator/health | API-status |
| **Database** | `localhost:5432` | PostgreSQL (user: lpg_user, pass: lpg_dev_password) |

---

## 💡 Tips

### Se logger
```bash
# Alle tjenester
docker-compose -f docker-compose-local.yaml logs -f

# Kun API
docker-compose -f docker-compose-local.yaml logs -f api

# Kun emulator
docker-compose -f docker-compose-local.yaml logs -f emulator
```

### Rebuild etter kodeendringer
```bash
docker-compose -f docker-compose-local.yaml up --build
```

### Start kun noen tjenester
```bash
# Kun database og emulator
docker-compose -f docker-compose-local.yaml up postgres azurite emulator
```

---

## 🆘 Problemer?

### "Port already in use"
```bash
# Finn og drep prosessen
lsof -i :8080  # eller :3000, :5432, etc.
kill -9 <PID>
```

### "Connection refused"
Vent 30 sekunder etter oppstart - tjenestene trenger tid til å starte.

### Database-feil
```bash
# Slett volumes og start på nytt
docker-compose -f docker-compose-local.yaml down -v
docker-compose -f docker-compose-local.yaml up
```

---

## 📚 Mer dokumentasjon

- [Full Developer Guide](DEVELOPER_GUIDE.md) - Kjøre i IntelliJ, debug, etc.
- [README](README.md) - Komplett prosjektdokumentasjon
- [OpenAPI Spec](openapi.yaml) - API-referanse

---

**Lykke til! 🚀**
