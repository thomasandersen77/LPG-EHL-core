# Simulatorfeil: Manglende terminal-GUI og betalingsknapper

**Dato:** 2026-02-14  
**Omfang:** Terminal-/PLS-simulator og oppstartscripter  

## Symptomer
- `./scripts/start-all-simulators.sh --field --gui` viste ikke **terminal-simulator GUI**.
- Webapp-GUI oppdaterte seg ikke når fylling ble stoppet, og **betalingsknapper** kom ikke frem.

## Mest sannsynlige årsaker
1. **`--field`-modus startet ikke betalingsterminal-simulator**
   - Scriptet var eksplisitt satt til å hoppe over terminalsimulator i field-modus.
   - Resultat: Ingen terminal-GUI, og webapp hadde ingen terminal å kontakte.

2. **PLS-simulator kjørte i LAB-profil selv i `--field`-modus**
   - Uten `--profile=field` går PLS ved `STOP/BLOCK` direkte til `IDLE`.
   - Da blir det ikke `PAYMENT_PENDING`-tilstand, og webappen får aldri trigget betalingsflyt/knapper.

3. **Base-URL for terminal i field-profil**
   - `application-field.yaml` peker til `http://192.168.0.9:18080`.
   - Ved lokal simulering må `PAYMENT_TERMINAL_BASE_URL` overstyres til `http://localhost:18080`.

## Implementerte tiltak
- **`start-all-simulators.sh`** starter nå betalingsterminal-simulator også i field-modus når `--gui` brukes.
- **PLS får `--profile=field`** i field-modus, slik at `STOP/BLOCK` gir korrekt `PAYMENT_PENDING`.
- **Instruksjon i scriptet** viser hvordan `PAYMENT_TERMINAL_BASE_URL` bør settes i field-modus når terminalsimulator kjøres lokalt.

## Anbefalt bruk etter endring
```bash
./scripts/start-all-simulators.sh --field --gui

# Start webapp med lokal terminal-sim (field)
PAYMENT_TERMINAL_BASE_URL=http://localhost:18080 \
java -jar release/lpg-ehl-webapp.jar \
  --spring.profiles.active=field \
  --ehl.serial.port=/tmp/vserial1
```

## Forventet effekt
- Terminal-GUI vises i field-modus når `--gui` brukes.
- Ved `STOP/BLOCK` går PLS til `PAYMENT_PENDING`, og webappen viser betalingsknapper.
- Webapp får kontakt med terminalsimulator via korrekt base-URL.