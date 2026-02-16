# Legacy Code Documentation

Denne katalogen inneholder dokumentasjon om legacy-kode og migrering til moderne Kotlin-implementasjon.

## Innhold

- **[LEGACY_ANALYSIS.md](LEGACY_ANALYSIS.md)** - Omfattende analyse av legacy-kode
  - Visual Basic 6 original implementasjon
  - Python re-implementasjon (proof-of-concept)
  - Moderne Kotlin implementasjon
  - Mapping fra legacy til Kotlin
  - Arkitektur sammenligning
  - Testing og kvalitet

- **[ZIP_CONTENTS_MANIFEST.md](ZIP_CONTENTS_MANIFEST.md)** - Arkiv innhold dokumentasjon
  - Oversikt over arkiverte filer
  - Backup struktur

## Legacy Code Generasjoner

### 1. Visual Basic 6 (Original)
**Lokasjon:** `legacy/norgesgass_legacy/`

Original Windows-basert Dispenserkontroll fra ARC-maskiner:
- `pumpekontroll.frm` - Hovedvindu og logikk (~2000 linjer)
- `fra_dispenser.bas` - EHL protokoll-funksjoner (~800 linjer)
- `defs.bas` - Konstanter og typer (~200 linjer)
- `Transaction.cls` - Transaksjonshåndtering (~300 linjer)

**Begrensninger:**
- Single-threaded med DoEvents
- Ingen unit tests
- VB6 IDE krever Windows
- Global state og ingen type-safety

### 2. Python (Eksperimentell Re-implementering)
**Lokasjon:** `legacy/more_legacy/Gammenl kode Python/`

Proof-of-concept for å bevise at protokollen kan re-implementeres:
- `protocol.py` - EHL framing og encoding
- `model.py` - Tilstandsmaskin
- `poller.py` - Polling-loop
- `serial_client.py` - Seriell kommunikasjon

**Forbedringer over VB6:**
- Modularitet
- Cross-platform (Python)
- Separasjon av concerns

**Svakheter:**
- Ingen type safety
- Mangler produksjonskvalitet

### 3. Kotlin (Moderne Produksjonsversjon)
**Lokasjon:** `lpg-ehl-core/`, `lpg-ehl-emulator/`, `lpg-ehl-api/`

Production-ready, type-safe implementasjon:
- **Type Safety**: Compile-time validation
- **Testing**: 61+ unit tests med JUnit 5
- **Immutability**: Thread-safe by design
- **Modular**: Protocol ≠ Communication ≠ State
- **Dependency Inversion**: Interface-basert design

## Viktige Migreringer

### VB6 → Kotlin Mapping

| VB6/Python | Kotlin Module | Tilsvarende Klasse |
|------------|---------------|---------------------|
| `protocol.py:encode_frame()` | `protocol/EhlCodec.kt` | `EhlCodec.encode()` |
| `protocol.py:decode_frame()` | `protocol/EhlCodec.kt` | `EhlCodec.decode()` |
| `model.py:PumpekontrollState` | `protocol/DispenserStatus.kt` | `DispenserStatus` |
| VB6 `comm_out` | `communication/EhlCommunicator.kt` | `send()` |

### Kommando Mapping

| VB6 Hex | Python Const | Kotlin Enum | Beskrivelse |
|---------|--------------|-------------|-------------|
| `&H4B` | `CMD_STATE = 75` | `EhlCommand.STATE` | Query state |
| `&H45` | `CMD_VOLUME = 69` | `EhlCommand.VOLUME` | Query volume |
| `&H77` | `CMD_UNBLOCK = 119` | `EhlCommand.UNBLOCK` | Start delivery |
| `&H69` | `CMD_BLOCK = 105` | `EhlCommand.BLOCK` | Stop dispenser |
| `&H81` | `CMD_RESET_ZER = 129` | `EhlCommand.ZER` | Reset calculator |

## Anbefaling

**Kotlin `lpg-ehl-core` er produksjonsklar** og bør erstatte både VB6 og Python legacy-kode:

✅ Protokoll-kompatibel (100% EHL-standard)  
✅ Omfattende test suite (61+ tests)  
✅ Type-safe (compile-time feilsjekk)  
✅ Vedlikeholdbar (moderne Kotlin idioms)  
✅ Skalerbar (multi-tenant edge-system)  
✅ Dokumentert (README + WARP.md)

## Referanser

- Original VB6 kode: `legacy/norgesgass_legacy/`
- Python PoC: `legacy/more_legacy/Gammenl kode Python/`
- Moderne implementasjon: `lpg-ehl-core/`
- Test rapport: [VB6_COMPATIBILITY_TEST.md](../implementation/VB6_COMPATIBILITY_TEST.md)
