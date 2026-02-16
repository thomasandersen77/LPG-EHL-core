# LPG-EHL Physical Layer Support (PLS)

Modul for ekte RS-485 serial kommunikasjon med LPG dispensere.

## Formål

Denne modulen inneholder `RealSerialTransport` - implementasjonen av `SerialTransport` interfacet som bruker jSerialComm for å kommunisere med fysisk hardware.

## Bruk

### I lpg-ehl-api (Spring Boot)

Modulen lastes automatisk når `ehl.emulator.enabled=false` i `application.yaml`.

### I lpg-ehl-cli

CLI bruker samme konfigurasjon for å velge transport.

## Konfigurasjon

```yaml
ehl:
  emulator:
    enabled: false  # FIELD MODE - bruk ekte serial port
  serial:
    port: /dev/ttyS0  # Serial port device
    baud-rate: 9600   # Baud rate (9600 for EHL protocol)
    data-bits: 8
    parity: EVEN      # Even parity (8E1 format)
    stop-bits: 1
```

## Serial Parametere (9600 8E1)

- **Baud rate:** 9600
- **Data bits:** 8
- **Parity:** Even ✅
- **Stop bits:** 1

> **VIKTIG:** Alle parametere er nå konfigurerbare via environment variables eller application.yaml.
> Se [SERIAL_CONTRACT.md](../docs/SERIAL_CONTRACT.md) for full dokumentasjon.

## Plattformer

- **Debian/Ubuntu Linux:** `/dev/ttyS0`, `/dev/ttyUSB0`
- **macOS:** `/dev/cu.usbserial-*`
- **Windows:** `COM1`, `COM2`, etc.

## Testing

For å teste uten fysisk hardware, bruk emulator mode:

```yaml
ehl:
  emulator:
    enabled: true  # LAB MODE
```

## Arkitektur

```
RealSerialTransport
    └── jSerialComm (com.fazecast:jSerialComm:2.10.4)
        └── Direkte kommunikasjon med OS serial port driver
            ├── Linux: /dev/ttyS0, /dev/ttyUSB0
            ├── macOS: /dev/cu.usbserial-*
            └── Windows: COM1, COM2, etc.
```

## Avhengigheter

- **lpg-ehl-core:** SerialTransport interface
- **jSerialComm:** Cross-platform serial port library
- **SLF4J:** Logging

## Kompilering

```bash
cd lpg-ehl-pls
mvn clean compile
```

## Se også

- [IMPLEMENTATION_PLAN_PLS_CLI.md](../docs/IMPLEMENTATION_PLAN_PLS_CLI.md) - Full implementasjonsplan
- [SerialTransport.kt](../lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/transport/SerialTransport.kt) - Transport interface
