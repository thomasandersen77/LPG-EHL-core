# Scripts Directory

Samling av hjelpeskript for LPG-EHL Edge system.

---

## 📁 Struktur

```
scripts/
├── python/
│   ├── ecr-testing/          # ECR terminal testing (Python)
│   │   ├── README.md         # Detaljert dokumentasjon
│   │   ├── ecr_server_v22_golden_format.py ⭐ ANBEFALT
│   │   ├── ecr_format_test.py
│   │   ├── ecr_protocol_tester.py
│   │   └── ... (25+ skript)
│   │
│   └── utilities/            # Generelle verktøy
│       └── html_to_pdf.py    # Markdown til PDF konvertering
│
└── shell/                    # Bash scripts
    ├── README.md             # Detaljert dokumentasjon  
    ├── start-local.sh        # Start lokal utvikling
    ├── start-system.sh       # Start produksjon
    ├── enq_ping.sh          # Dispenser connectivity test
    └── test_vb6_protocols.sh # VB6 protokoll testing
```

---

## 🚀 Quick Start Guide

### ECR Terminal Testing
```bash
cd python/ecr-testing
python3 ecr_server_v22_golden_format.py
```
👉 **Full dokumentasjon:** [python/ecr-testing/README.md](python/ecr-testing/README.md)

### System Oppstart
```bash
cd shell
./start-local.sh    # Lokal utvikling
# eller
./start-system.sh   # Produksjon
```
👉 **Full dokumentasjon:** [shell/README.md](shell/README.md)

### Dispenser Testing
```bash
cd shell
./enq_ping.sh 192.168.1.100 9000
```

---

## 📚 Dokumentasjon per Kategori

### 🐍 Python Scripts

#### ECR Testing ([python/ecr-testing/](python/ecr-testing/))
**Formål:** Reverse-engineering og testing av Ingenico Self/4000 betalingsterminal.

**Hovedskript:**
- `ecr_server_v22_golden_format.py` - Produksjonsklart testskript
- `ecr_format_test.py` - Kommandoformat testing
- `ecr_protocol_tester.py` - Protokoll-identifikasjon

**Status:** Kommunikasjon fungerer ✅, transaksjoner krever SDK fra Nets/Bambora ⚠️

**Les mer:** [python/ecr-testing/README.md](python/ecr-testing/README.md)

---

#### Utilities ([python/utilities/](python/utilities/))
**Formål:** Generelle hjelpeverktøy.

**Skript:**
- `html_to_pdf.py` - Konverterer HTML/Markdown til PDF (bruker macOS funksjoner)

---

### 🔧 Shell Scripts ([shell/](shell/))

**Formål:** System administrasjon og testing.

**Hovedskript:**
- `start-local.sh` - Lokal utviklingsoppstart
- `start-system.sh` - Produksjonsoppstart
- `enq_ping.sh` - Dispenser health check
- `test_vb6_protocols.sh` - Full protokolltest

**Les mer:** [shell/README.md](shell/README.md)

---

## 🎯 Bruksområder

### For Utviklere
```bash
# Start lokal utvikling
cd shell && ./start-local.sh

# Test ECR kommunikasjon
cd python/ecr-testing && python3 ecr_server_v22_golden_format.py
```

### For Testing
```bash
# Test dispenser forbindelse
cd shell && ./enq_ping.sh <DISPENSER_IP> 9000

# Full VB6 protokoll test
cd shell && ./test_vb6_protocols.sh <DISPENSER_IP> 9000

# Test ECR terminal
cd python/ecr-testing && python3 find_terminal.py
```

### For Produksjon
```bash
# Start komplett system
cd shell && ./start-system.sh
```

---

## 🔧 Avhengigheter

### Python Scripts
- **Python 3.8+** (system Python på macOS)
- **Ingen eksterne pakker** (bruker kun standard library)

### Shell Scripts  
- **Bash 4.0+** (inkludert i macOS/Linux)
- **netcat** (for dispenser testing)
- **Docker** (for produksjon)
- **Java 21** (for Edge applikasjon)

---

## 📖 Relatert Dokumentasjon

### Tekniske Rapporter
- **ECR Integration:** [../docs/ecr-integration/ECR_INTEGRATION_REPORT.pdf](../docs/ecr-integration/ECR_INTEGRATION_REPORT.pdf)
- **Protokoll Analyse:** [../docs/ecr-integration/FINAL_PROTOCOL_ANALYSIS.md](../docs/ecr-integration/FINAL_PROTOCOL_ANALYSIS.md)

### Generell Dokumentasjon
- **Developer Guide:** [../docs/general/DEVELOPER_GUIDE.md](../docs/general/DEVELOPER_GUIDE.md)
- **Deployment Guide:** [../docs/general/DEPLOYMENT_QUICKSTART.md](../docs/general/DEPLOYMENT_QUICKSTART.md)
- **Multi-Station Setup:** [../docs/general/MULTI-STATION-SETUP.md](../docs/general/MULTI-STATION-SETUP.md)

---

## 🆘 Feilsøking

### Python Scripts Kjører Ikke
```bash
# Sjekk Python versjon
python3 --version  # Skal være 3.8+

# Prøv direkte
python3 scripts/python/ecr-testing/ecr_server_v22_golden_format.py
```

### Shell Scripts Ikke Kjørbare
```bash
# Gi kjørerettigheter
chmod +x scripts/shell/*.sh

# Kjør med bash eksplisitt
bash scripts/shell/start-local.sh
```

### Port Allerede i Bruk
```bash
# Finn prosess
lsof -i :8080

# Drep prosess
kill -9 <PID>
```

---

## 📝 Bidra

### Legge til Nytt Skript

1. **Velg riktig kategori:**
   - Python → `python/`
   - Shell → `shell/`
   - Utility → `python/utilities/`

2. **Opprett fil:**
   ```bash
   touch scripts/python/my_script.py
   chmod +x scripts/python/my_script.py  # hvis executable
   ```

3. **Dokumenter:**
   - Legg til i relevant README
   - Inkluder brukseksempel
   - Beskriv formål og avhengigheter

4. **Test:**
   ```bash
   # Test skriptet grundig
   python3 scripts/python/my_script.py
   ```

---

## 🔐 Sikkerhet

⚠️ **Viktig sikkerhetshensyn:**

- **Aldri hardkod secrets** - bruk miljøvariabler
- **Valider all input** - spesielt i shell scripts
- **Begrenset tilgang** - kjør med minste nødvendige rettigheter
- **Logg sensitiv aktivitet** - for audit trail
- **Test i isolert miljø** først

---

## 📊 Status Oversikt

| Kategori | Antall Skript | Status | Dokumentasjon |
|----------|---------------|--------|---------------|
| ECR Testing (Python) | 27 | ✅ Komplett | [README](python/ecr-testing/README.md) |
| Utilities (Python) | 1 | ✅ Komplett | Inline |
| Shell Scripts | 4 | ✅ Komplett | [README](shell/README.md) |

---

## 🎓 Læringsressurser

### For Nye Utviklere

1. **Start her:** [../docs/general/DEVELOPER_GUIDE.md](../docs/general/DEVELOPER_GUIDE.md)
2. **Forstå ECR:** [python/ecr-testing/README.md](python/ecr-testing/README.md)
3. **System oppstart:** [shell/README.md](shell/README.md)

### Avanserte Topics

- **Protokoll Analyse:** [../docs/ecr-integration/ECR_INTEGRATION_REPORT.pdf](../docs/ecr-integration/ECR_INTEGRATION_REPORT.pdf)
- **Multi-Station:** [../docs/general/MULTI-STATION-SETUP.md](../docs/general/MULTI-STATION-SETUP.md)

---

## 📞 Support

**For hjelp:**
- 📖 Les relevant README i underkategori
- 🐛 Sjekk feilsøkingsseksjon
- 💬 Kontakt LPG-EHL Development Team

**Rapporter problemer:**
- GitHub Issues (hvis tilgjengelig)
- Intern support-kanal

---

**Sist oppdatert:** Januar 2026  
**Vedlikeholdt av:** LPG-EHL Development Team
