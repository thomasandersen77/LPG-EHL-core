# DispenserAddressResolver: Fra Runtime-Mapping til Direkte Konfigurasjon

**Dato:** 2026-02-15
**Analyse av:** Refactoring fra commit f3ccf54 → 600ed83
**Moduler:** `lpg-ehl-service`, `lpg-ehl-api`

---

## Executive Summary

`DispenserAddressResolver` ble introdusert i commit **f3ccf54** (13. feb 2026) og fjernet i commit **600ed83** (15. feb 2026). Den levde i bare 2 dager før den ble erstattet med en enklere, direkte konfigurasjonsstrategi.

**Konklusjon:** Refactoringen var **riktig valg** - den nye løsningen er enklere, tryggere, og bedre tilpasset faktisk deployment-modell (én app per stasjon, én pumpe per stasjon).

---

## Den Gamle Løsningen: DispenserAddressResolver

### Implementasjon (lpg-ehl-service/src/main/kotlin/no/cloudberries/lpg/service/pump/DispenserAddressResolver.kt)

```kotlin
@Component
class DispenserAddressResolver(
    private val environment: Environment,
    @Value("\${lpg.dispenser.address:}") private val configuredAddressRaw: String
) {
    fun resolve(requestedAddress: Int): Int {
        require(requestedAddress in 1..255) { "Invalid dispenser address: $requestedAddress (must be 1-255)" }

        val isField = environment.activeProfiles.any { it.equals("field", ignoreCase = true) }
        if (!isField) return requestedAddress

        val hw = configuredAddress ?: return requestedAddress
        if (hw == requestedAddress) return requestedAddress

        // In field mode we force all pump operations to the configured RS-485 address
        logger.warn("FIELD mode: overriding requested dispenser address {} -> {} (lpg.dispenser.address)",
                    requestedAddress, hw)
        return hw
    }
}
```

### Bruksmønster i PumpController (Gammel)

```kotlin
@RestController
@RequestMapping("/api/v1/emulator")
class PumpController(
    private val pumpStateService: PumpStateService,
    private val authorizationService: PumpAuthorizationService,
    private val dispenserAddressResolver: DispenserAddressResolver  // ← Injected
) {
    private fun resolveAddress(requestedAddress: Int): Int =
        dispenserAddressResolver.resolve(requestedAddress)

    @GetMapping("/pump/{address}/status")  // ← Address in path
    fun getPumpStatus(@PathVariable address: Int): ResponseEntity<Map<String, Any>> {
        val resolved = resolveAddress(address)  // ← Runtime mapping
        val status = pumpStateService.getStatus(resolved)
        // ...
    }

    @PostMapping("/pump/{address}/unblock")
    fun unblockPump(@PathVariable address: Int): ResponseEntity<...> {
        val resolved = resolveAddress(address)
        val result = pumpStateService.unblock(resolved, withAuthorization = true)
        // ...
    }
}
```

### Oppførsel

| **Scenario** | **Request Address** | **Configured Address** | **Spring Profile** | **Resultat** |
|--------------|---------------------|------------------------|-------------------|--------------|
| LAB mode | 1 | (ikke satt) | `lab` | 1 (passthrough) |
| LAB mode | 5 | 33 | `lab` | 5 (passthrough) |
| FIELD mode | 1 | 33 | `field` | **33** (overstyrt) |
| FIELD mode | 1 | (ikke satt) | `field` | 1 (ingen config) |

**Nøkkellogikk:**
- **LAB/TEST:** Request address brukes som-den-er → støtter multi-dispenser GUI
- **FIELD:** Tvinger alltid til `lpg.dispenser.address` → sikrer riktig fysisk enhet

---

## Den Nye Løsningen: Direkte Konfigurasjon

### Implementasjon (lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/controller/PumpController.kt)

```kotlin
@RestController
@RequestMapping("/api/v1/emulator")
class PumpController(
    private val pumpStateService: PumpStateService,
    private val authorizationService: PumpAuthorizationService,
    @Value("\${lpg.dispenser.address:1}") private val defaultAddress: Int  // ← Direct injection
) {
    @GetMapping("/pump/status")  // ← No address in path
    fun getPumpStatus(): ResponseEntity<Map<String, Any>> {
        val status = pumpStateService.getStatus(defaultAddress)  // ← Direct usage
        return ResponseEntity.ok(mapOf(
            "state" to status.state,
            "address" to status.address,
            // ...
        ))
    }

    @PostMapping("/pump/unblock")
    fun unblockPump(): ResponseEntity<...> {
        logger.info("🔓 FRI PUMPE: Unblock request for address $defaultAddress")
        val result = pumpStateService.unblock(defaultAddress, withAuthorization = true)
        // ...
    }
}
```

### URL-struktur Endringer

| **Endpoint** | **Gammel (med resolver)** | **Ny (direkte config)** |
|--------------|---------------------------|-------------------------|
| Status | `GET /pump/{address}/status` | `GET /pump/status` |
| Unblock | `POST /pump/{address}/unblock` | `POST /pump/unblock` |
| Release | `POST /pump/{address}/release` | `POST /pump/release` |
| Start pumping | `POST /pump/{address}/start-pumping` | `POST /pump/start-pumping` |
| Block | `POST /pump/{address}/block` | `POST /pump/block` |
| Settle | `POST /settle/{id}` | `POST /settle` |
| Reset | `POST /pump/{address}/reset` | `POST /pump/reset` |
| Card swipe | `POST /pump/{address}/card-swipe` | `POST /pump/card-swipe` |
| Authorization | `GET /pump/{address}/authorization` | `GET /pump/authorization` |
| Confirm payment | `POST /pump/{address}/confirm-payment` | `POST /pump/confirm-payment` |
| Cancel auth | `POST /pump/{address}/cancel-authorization` | `POST /pump/cancel-authorization` |

**Alle 11 endpoints** ble forenklet - `{address}` parameter fjernet fra URL.

---

## Sammenligning: Teknisk Analyse

### Kompleksitet

| **Aspekt** | **DispenserAddressResolver** | **Direkte konfig** |
|------------|------------------------------|---------------------|
| **Lines of code** | ~45 linjer (egen klasse) | 1 linje (`@Value`) |
| **Dependencies** | `Environment`, `@Value`, Logger | Bare `@Value` |
| **Runtime overhead** | `resolve()` per request + profil-sjekk | Ingen (fast value) |
| **Spring profiles** | Oppførsel endres med `field` profil | Profil-uavhengig |
| **Testbarhet** | Mock Environment + resolver | Bare mock @Value |

### Sikkerhet & Feilhåndtering

| **Scenario** | **Gammel** | **Ny** |
|--------------|-----------|---------|
| **GUI sender feil address** | Overstyres i FIELD mode (logger warning) | Kan ikke skje (ingen parameter) |
| **Ugyldig address (0, 256)** | `require()` kaster exception | Spring validering ved oppstart |
| **Mangler config** | Fallback til requested address | Default `1` |
| **Multi-dispenser forsøk** | Fungerer i LAB, feiler i FIELD | Ikke støttet i noen modus |

### API-design

**DispenserAddressResolver-tilnærming:**
```
POST /api/v1/emulator/pump/1/unblock    ← "Unblock pumpe #1"
POST /api/v1/emulator/pump/5/unblock    ← "Unblock pumpe #5"
```
💭 Impliserer at systemet kan håndtere flere pumper samtidig (men kan ikke det i FIELD mode)

**Direkte konfig-tilnærming:**
```
POST /api/v1/emulator/pump/unblock      ← "Unblock DEN pumpen"
```
✅ Klarere kontrakt: Én app = én pumpe (som er faktisk deployment-modell)

---

## Hvorfor Ble Den Fjernet?

### 1. **Overdesign for Faktisk Use Case**

**Realitet i produksjon:**
- Hver stasjon har **én** fysisk pumpe
- Én Spring Boot-instans per stasjon
- GUI sender alltid logisk `address=1`
- Fysisk RS-485-adresse (f.eks. 33) konfigureres i `application.yml`

**Konklusjon:** Multi-address logikk var unødvendig - én property holder.

### 2. **Forenklet API-kontrakt**

REST-prinsipp: **Ressurser skal være entydige**

❌ **Problem med gammel API:**
```
GET /pump/1/status    ← Hva betyr "1"? Logisk ID? Fysisk adresse?
GET /pump/33/status   ← Kan jeg kalle både 1 og 33? Får jeg forskjellige data?
```

✅ **Løsning i ny API:**
```
GET /pump/status      ← "Den ene pumpen denne instansen kontrollerer"
```

### 3. **Redusert Kognitiv Belastning**

**Gammel:** Utvikler må forstå:
- Spring profiles (`field` vs. `lab`)
- Runtime address resolution
- Hvorfor samme app oppfører seg forskjellig i dev vs. prod

**Ny:** Utvikler må forstå:
- `lpg.dispenser.address` property
- (Det er alt.)

### 4. **Eliminert Runtime-feil**

**Scenario som kunne skjedd:**
```kotlin
// GUI-bug sender feil address
POST /pump/99/unblock

// I LAB mode: Sender UNBLOCK til address 99 (kanskje ikke-eksisterende emulator)
// I FIELD mode: Logger warning, sender til address 33 (korrekt, men forvirrende)
```

**Ny løsning:** Ingen måte å sende feil address - parameter eksisterer ikke.

---

## Trade-offs: Hva Mistet Vi?

### ❌ **LAB Multi-Dispenser Testing**

**Gammel:**
```
POST /pump/1/unblock    ← Kontroller emulator #1
POST /pump/2/unblock    ← Kontroller emulator #2
```
Kunne teste flere virtuelle pumper fra samme GUI.

**Ny:**
Må kjøre separate instanser med forskjellige ports:
```bash
# Terminal 1
java -jar app.jar --lpg.dispenser.address=1 --server.port=8080

# Terminal 2
java -jar app.jar --lpg.dispenser.address=2 --server.port=8081
```

**Vurdering:** 🤷 Dette scenarioet har aldri vært reelt behov - LAB mode bruker én emulator av gangen.

### ❌ **Dynamisk Address Routing**

Hypotetisk fremtidig scenario: Én server styrer 10 pumper.

**Gammel:** Kunne støttet dette med endringer i service-laget.
**Ny:** Krever arkitektur-redesign (connection pool per address, etc.)

**Vurdering:** 🚫 Ikke en del av produkt-roadmap. Hver stasjon får egen app-instans.

---

## Når Burde DispenserAddressResolver Beholdes?

Hvis noen av disse var sanne:

1. ✅ **Multi-dispenser stasjoner:** Én stasjon med 5 pumper, én app-instans styrer alle
2. ✅ **Dynamisk routing:** Cloud-basert tjeneste som router til forskjellige stasjoner
3. ✅ **Test-framework:** Automatiserte tester som trenger å skifte mellom målenheter
4. ✅ **Address-translation kompleksitet:** Logiske adresser 1-10 mapper til fysiske 100-110

**Faktisk situasjon:** Ingen av disse gjelder. ✅ **Riktig å fjerne.**

---

## Konklusjon

| **Kriterie** | **Vurdering** | **Kommentar** |
|--------------|---------------|---------------|
| **Kode-kvalitet** | ✅ **Forbedret** | 45 linjer → 1 linje, lettere å forstå |
| **Sikkerhet** | ✅ **Forbedret** | Eliminert feil address-vector |
| **Performance** | ✅ **Forbedret** | Ingen runtime overhead |
| **API-design** | ✅ **Forbedret** | Klarere REST-kontrakt |
| **Fleksibilitet** | ⚠️ **Redusert** | Mistet multi-dispenser støtte (ikke behov) |
| **Testbarhet** | ✅ **Forbedret** | Færre mocks, enklere setup |

### Anbefaling

✅ **Behold den nye løsningen.**

DispenserAddressResolver var en god abstraksjon for et problem vi ikke har. Direkte konfigurasjon er:
- **Enklere** å forstå og vedlikeholde
- **Tryggere** (ingen runtime edge cases)
- **Bedre aligned** med faktisk deployment-modell

Hvis multi-dispenser support trengs i fremtiden, kan det implementeres som en **separat service** med proper connection pooling - ikke som address-rewriting i controlleren.

---

## Appendiks: Migreringsguide (Hvis Noen Brukte Gammel API)

### Frontend-endringer

```typescript
// GAMMEL
async function unblockPump(address: number) {
    await fetch(`/api/v1/emulator/pump/${address}/unblock`, { method: 'POST' });
}

// NY
async function unblockPump() {
    await fetch('/api/v1/emulator/pump/unblock', { method: 'POST' });
}
```

### Konfigurasjon

```yaml
# application-field.yml
lpg:
  dispenser:
    address: 33  # ← Fysisk RS-485-adresse på stasjonen

spring:
  profiles:
    active: field  # ← Ikke lenger brukt for address-logikk, men fortsatt relevant for andre features
```

### Backend Service-kall

```kotlin
// GAMMEL
val resolved = dispenserAddressResolver.resolve(requestAddress)
pumpStateService.unblock(resolved)

// NY
pumpStateService.unblock(defaultAddress)
```

---

**Dokumentert av:** Claude (Anthropic)
**Referanse commits:**
- f3ccf54 - "Unlock pump works now" (introduserte DispenserAddressResolver)
- 600ed83 - "feat: comprehensive payment terminal integration..." (fjernet DispenserAddressResolver)
