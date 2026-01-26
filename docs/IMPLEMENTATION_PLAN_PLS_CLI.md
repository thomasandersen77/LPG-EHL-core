# Implementasjonsplan: PLS og CLI Moduler

**Dato:** 2025-01-12  
**Versjon:** 1.0  
**Formål:** Implementere Physical Layer Support (PLS) for ekte serial hardware og Command Line Interface (CLI) modul med Spring Shell

---

## Oversikt

Denne planen beskriver implementeringen av to nye moduler i LPG-EHL systemet:

1. **lpg-ehl-pls** - Physical Layer Support for ekte RS-485 serial kommunikasjon med dispensere
2. **lpg-ehl-cli** - Command Line Interface for testing og drift av pumper via terminal

**Arkitektonisk Mål:**
- Introdusere `SerialTransport` interface for abstrahering av transport-lag
- Støtte både emulator (eksisterende InMemorySerialPort) og ekte hardware (ny RealSerialTransport)
- Dele samme forretningslogikk (DispenserService, PumpStateService) mellom API/GUI og CLI
- Bruke Spring Boot konfigurasjon for å bytte mellom emulator og ekte hardware
- Forbedre EhlCommunicator for robust multi-packet håndtering

---

## Del 1: SerialTransport Interface (lpg-ehl-core)

### 1.1 Opprett SerialTransport Interface

**Fil:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/transport/SerialTransport.kt`

**Formål:** Abstrakt interface for alle transportlag-implementasjoner (emulator, ekte serial port, mock for tester)

```kotlin
package no.cloudberries.lpg.transport

/**
 * Abstraction for serial transport layer.
 * 
 * Dette interfacet tillater at EhlCommunicator kan jobbe mot både:
 * - InMemorySerialPort (emulator for testing/utvikling)
 * - RealSerialTransport (ekte RS-485 hardware for produksjon)
 * - MockSerialTransport (for unit testing)
 * 
 * Bytt implementasjon via Spring configuration property: ehl.emulator.enabled
 */
interface SerialTransport {
    /**
     * Check if transport is connected and ready.
     */
    val isConnected: Boolean
    
    /**
     * Open/connect the transport.
     * @return true if successful
     */
    fun connect(): Boolean
    
    /**
     * Close/disconnect the transport.
     */
    fun disconnect()
    
    /**
     * Write bytes to transport.
     * @param data Bytes to write
     * @return Number of bytes written
     */
    fun write(data: ByteArray): Int
    
    /**
     * Read available bytes from transport (non-blocking).
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read (may be empty if no data available)
     */
    fun readAvailable(maxBytes: Int = 256): ByteArray
    
    /**
     * Flush any pending output.
     */
    fun flush()
    
    /**
     * Clear all pending data in receive buffer.
     */
    fun clearBuffer() {}
}
```

### 1.2 Refaktorer SerialPortIO til SerialTransport

**Mål:** Erstatt `SerialPortIO` interface med `SerialTransport` i hele codebasen.

**Filer som må endres:**
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt`
  - Endre `private val serialPort: SerialPortIO` til `private val transport: SerialTransport`
  - Endre alle `serialPort.read()` til `transport.readAvailable()`
  - Endre alle `serialPort.write()` til `transport.write()`
  
- `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/emulator/InMemorySerialPort.kt`
  - Endre `class InMemorySerialPort : SerialPortIO` til `class InMemorySerialPort : SerialTransport`
  - Rename `read()` metode til `readAvailable()`

**VIKTIG:** Denne refaktoreringen må kompilere 100% før du går videre!

---

## Del 2: PLS Modul (lpg-ehl-pls)

### 2.1 Oppdater pom.xml

**Fil:** `lpg-ehl-pls/pom.xml`

**Innhold:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>no.cloudberries.lpg</groupId>
        <artifactId>lpg-ehl-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>lpg-ehl-pls</artifactId>
    <name>LPG-EHL Physical Layer Support</name>
    <description>Real serial port implementation for RS-485 hardware communication</description>

    <dependencies>
        <!-- Depend on Core for SerialTransport interface -->
        <dependency>
            <groupId>no.cloudberries.lpg</groupId>
            <artifactId>lpg-ehl-core</artifactId>
        </dependency>

        <!-- jSerialComm for real serial port communication -->
        <dependency>
            <groupId>com.fazecast</groupId>
            <artifactId>jSerialComm</artifactId>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>

        <!-- Kotlin -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>src/main/kotlin</sourceDirectory>
        <testSourceDirectory>src/test/kotlin</testSourceDirectory>

        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 Implementer RealSerialTransport

**Fil:** `lpg-ehl-pls/src/main/kotlin/no/cloudberries/lpg/pls/RealSerialTransport.kt`

**Formål:** Adapter som wrapper jSerialComm for ekte RS-485 kommunikasjon

**Implementasjon:**
```kotlin
package no.cloudberries.lpg.pls

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory

/**
 * Real serial port transport using jSerialComm.
 * 
 * For produksjon på Debian-baserte edge devices (ARK-3600 eller lignende).
 * Kommuniserer med fysisk RS-485 dispenser via /dev/ttyS0 eller lignende.
 * 
 * KONFIGURASJON:
 * - ehl.serial.port (default: /dev/ttyS0)
 * - ehl.serial.baud-rate (default: 9600)
 * 
 * Serial parametere: 9600 baud, 8E1, even parity
 */
class RealSerialTransport(
    private val portName: String,
    private val baudRate: Int = 9600
) : SerialTransport {
    
    private val logger = LoggerFactory.getLogger(RealSerialTransport::class.java)
    private var serialPort: SerialPort? = null
    
    override val isConnected: Boolean
        get() = serialPort?.isOpen == true
    
    override fun connect(): Boolean {
        try {
            // Find the serial port
            val port = SerialPort.getCommPort(portName)
            if (port == null) {
                logger.error("Serial port $portName not found")
                return false
            }
            
            // Configure serial parameters: 9600 8E1
            port.baudRate = baudRate
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = SerialPort.EVEN_PARITY
            
            // Open port with timeout
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                100,  // Read timeout 100ms
                0     // No write timeout
            )
            
            if (!port.openPort()) {
                logger.error("Failed to open serial port $portName")
                return false
            }
            
            serialPort = port
            logger.info("🔌 FIELD MODE: Connected to serial port $portName at $baudRate baud")
            return true
            
        } catch (e: Exception) {
            logger.error("Error connecting to serial port $portName: ${e.message}", e)
            return false
        }
    }
    
    override fun disconnect() {
        try {
            serialPort?.closePort()
            serialPort = null
            logger.info("🔌 Disconnected from serial port $portName")
        } catch (e: Exception) {
            logger.error("Error disconnecting from serial port: ${e.message}", e)
        }
    }
    
    override fun write(data: ByteArray): Int {
        val port = serialPort ?: throw IllegalStateException("Serial port not connected")
        
        try {
            val bytesWritten = port.writeBytes(data, data.size.toLong())
            if (bytesWritten != data.size) {
                logger.warn("Only wrote $bytesWritten of ${data.size} bytes")
            }
            return bytesWritten
        } catch (e: Exception) {
            logger.error("Error writing to serial port: ${e.message}", e)
            throw e
        }
    }
    
    override fun readAvailable(maxBytes: Int): ByteArray {
        val port = serialPort ?: throw IllegalStateException("Serial port not connected")
        
        try {
            val available = port.bytesAvailable()
            if (available <= 0) {
                return ByteArray(0)
            }
            
            val bytesToRead = minOf(available, maxBytes)
            val buffer = ByteArray(bytesToRead)
            val bytesRead = port.readBytes(buffer, bytesToRead.toLong())
            
            return if (bytesRead == bytesToRead) {
                buffer
            } else {
                buffer.copyOf(bytesRead)
            }
        } catch (e: Exception) {
            logger.error("Error reading from serial port: ${e.message}", e)
            return ByteArray(0)
        }
    }
    
    override fun flush() {
        try {
            serialPort?.flushIOBuffers()
        } catch (e: Exception) {
            logger.warn("Error flushing serial port: ${e.message}")
        }
    }
    
    override fun clearBuffer() {
        try {
            // Read and discard all available data
            val port = serialPort ?: return
            while (port.bytesAvailable() > 0) {
                val buffer = ByteArray(256)
                port.readBytes(buffer, buffer.size.toLong())
            }
            logger.debug("🧹 Serial port buffer cleared")
        } catch (e: Exception) {
            logger.warn("Error clearing buffer: ${e.message}")
        }
    }
}
```

### 2.3 Lag README for PLS

**Fil:** `lpg-ehl-pls/README.md`

```markdown
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
```

## Serial Parametere

- **Baud rate:** 9600
- **Data bits:** 8
- **Stop bits:** 1
- **Parity:** Even (8E1)

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
```

---

## Del 3: Spring Configuration for Transport Switching

### 3.1 Opprett TransportConfiguration

**Fil:** `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/TransportConfiguration.kt`

**Formål:** Spring Boot konfigurasjon som velger riktig transport basert på `ehl.emulator.enabled` property

```kotlin
package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import no.cloudberries.lpg.pls.RealSerialTransport
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring Configuration for SerialTransport selection.
 * 
 * Velger transport basert på ehl.emulator.enabled:
 * - true  -> InMemorySerialPort (LAB MODE)
 * - false -> RealSerialTransport (FIELD MODE)
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * LAB MODE: InMemorySerialPort med emulator
     * Laster når ehl.emulator.enabled=true (default)
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "ehl.emulator",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true  // Default til LAB MODE hvis property mangler
    )
    fun inMemoryTransport(
        @Value("\${ehl.emulator.dispenser-address:1}") address: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("🧪 LAB MODE: Initializing InMemorySerialPort with emulator")
        
        val emulator = EhlDispenserEmulator(
            dispenserAddress = address,
            pricePerLiterCents = pricePerLiterCents
        )
        
        return InMemorySerialPort(emulator, latencyMs)
    }
    
    /**
     * FIELD MODE: RealSerialTransport med jSerialComm
     * Laster når ehl.emulator.enabled=false
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "ehl.emulator",
        name = ["enabled"],
        havingValue = "false"
    )
    fun realSerialTransport(
        @Value("\${ehl.serial.port:/dev/ttyS0}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int
    ): SerialTransport {
        logger.info("🏭 FIELD MODE: Initializing RealSerialTransport on $portName at $baudRate baud")
        return RealSerialTransport(portName, baudRate)
    }
}
```

### 3.2 Oppdater CommunicationConfig

**Fil:** `lpg-ehl-api/src/main/kotlin/no/cloudberries/lpg/api/config/CommunicationConfig.kt`

**Endringer:**
- Fjern direkte instansiering av emulator
- Injiser `SerialTransport` i stedet for `SerialPortIO`
- Forenkle `ehlCommunicator()` bean

```kotlin
@Bean
fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
    logger.info("🔧 Creating EhlCommunicator with ${transport::class.simpleName}")
    
    // Connect transport
    if (!transport.isConnected) {
        transport.connect()
    }
    
    return EhlCommunicator(transport)
}
```

### 3.3 Legg til PLS dependency i API pom.xml

**Fil:** `lpg-ehl-api/pom.xml`

**Legg til:**
```xml
<dependency>
    <groupId>no.cloudberries.lpg</groupId>
    <artifactId>lpg-ehl-pls</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## Del 4: Forbedre EhlCommunicator for Multi-Packet Handling

### 4.1 Problem Statement

**Nåværende begrensning:**
- `EhlCommunicator.receive()` leser én enkelt pakke
- Hvis flere pakker kommer i samme RX chunk (f.eks. `OK + STATE + VOLUME`), vil bare den første bli håndtert
- Unsolicited packets (STATE, VOLUME) vil blokkere når man venter på OK ack

**Løsning:**
- Background frame decoder som parser alt som kommer inn
- Dispatch unsolicited packets til handler/state cache
- `awaitResponse(predicate, timeout)` som venter på matching pakke i cache

### 4.2 Implementer Packet Dispatcher

**Fil:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/PacketDispatcher.kt`

```kotlin
package no.cloudberries.lpg.communication

import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlCommand
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Dispatcher for incoming EHL packets.
 * 
 * Håndterer unsolicited packets (STATE, VOLUME) og cacher response packets
 * for awaitResponse() matching.
 */
class PacketDispatcher {
    
    private val logger = LoggerFactory.getLogger(PacketDispatcher::class.java)
    
    // Queue of received packets waiting for matching response
    private val receivedPackets = ConcurrentLinkedQueue<EhlPacket>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    
    // Handlers for unsolicited packets
    private val unsolicitedHandlers = mutableMapOf<EhlCommand, (EhlPacket) -> Unit>()
    
    /**
     * Register handler for unsolicited packets of a specific command type.
     */
    fun registerUnsolicitedHandler(command: EhlCommand, handler: (EhlPacket) -> Unit) {
        unsolicitedHandlers[command] = handler
        logger.debug("Registered unsolicited handler for ${command.name}")
    }
    
    /**
     * Dispatch an incoming packet.
     * - If it matches a registered unsolicited handler, invoke handler
     * - Otherwise, queue it for awaitResponse matching
     */
    fun dispatch(packet: EhlPacket) {
        // Check if this is an unsolicited packet with handler
        val handler = unsolicitedHandlers[packet.command]
        if (handler != null) {
            try {
                handler(packet)
            } catch (e: Exception) {
                logger.error("Error in unsolicited handler for ${packet.command}: ${e.message}", e)
            }
            return
        }
        
        // Queue for awaitResponse matching
        lock.withLock {
            receivedPackets.add(packet)
            condition.signalAll()
        }
    }
    
    /**
     * Wait for a packet matching the predicate.
     * 
     * @param predicate Function to test if packet matches
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return Matching packet or null if timeout
     */
    fun awaitResponse(predicate: (EhlPacket) -> Boolean, timeoutMs: Long): EhlPacket? {
        val deadline = System.currentTimeMillis() + timeoutMs
        
        lock.withLock {
            while (true) {
                // Check existing packets for match
                val iterator = receivedPackets.iterator()
                while (iterator.hasNext()) {
                    val packet = iterator.next()
                    if (predicate(packet)) {
                        iterator.remove()
                        return packet
                    }
                }
                
                // Check timeout
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) {
                    return null
                }
                
                // Wait for new packets
                condition.await(remaining, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }
    
    /**
     * Clear all queued packets.
     */
    fun clear() {
        lock.withLock {
            receivedPackets.clear()
        }
    }
}
```

### 4.3 Integrer PacketDispatcher i EhlCommunicator

**Fil:** `lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/communication/EhlCommunicator.kt`

**Endringer:**

1. Legg til PacketDispatcher instans:
```kotlin
class EhlCommunicator(private val transport: SerialTransport) {
    private val logger = LoggerFactory.getLogger(EhlCommunicator::class.java)
    private val receiveBuffer = mutableListOf<Byte>()
    private val bufferLock = Any()
    private val dispatcher = PacketDispatcher()
    
    // Background reader scope
    private val readerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
```

2. Start background reader i init block:
```kotlin
init {
    startBackgroundReader()
}

private fun startBackgroundReader() {
    readerScope.launch {
        while (isActive) {
            try {
                val packet = readNextPacket()
                if (packet != null) {
                    dispatcher.dispatch(packet)
                } else {
                    delay(10)  // No packet ready, wait a bit
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    logger.error("Error in background reader: ${e.message}", e)
                    delay(100)  // Back off on errors
                }
            }
        }
    }
}
```

3. Endre `sendAndReceive()` til å bruke `awaitResponse()`:
```kotlin
suspend fun sendAndReceive(packet: EhlPacket, timeoutMs: Long = 2000): EhlPacket {
    // Send packet
    send(packet)
    
    // Wait for matching response via dispatcher
    return withContext(Dispatchers.IO) {
        dispatcher.awaitResponse(
            predicate = { it.address == packet.address && it.command == packet.command },
            timeoutMs = timeoutMs
        ) ?: throw TimeoutException("No response for ${packet.command} within ${timeoutMs}ms")
    }
}
```

4. Legg til metode for å registrere unsolicited handlers:
```kotlin
/**
 * Register handler for unsolicited packets (STATE, VOLUME, etc.)
 */
fun registerUnsolicitedHandler(command: EhlCommand, handler: (EhlPacket) -> Unit) {
    dispatcher.registerUnsolicitedHandler(command, handler)
}
```

5. Legg til shutdown metode:
```kotlin
fun shutdown() {
    readerScope.cancel()
    transport.disconnect()
}
```

**VIKTIG:** Denne refaktoreringen krever grundig testing med emulator!

---

## Del 5: CLI Modul (lpg-ehl-cli)

### 5.1 Oppdater pom.xml

**Fil:** `lpg-ehl-cli/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>no.cloudberries.lpg</groupId>
        <artifactId>lpg-ehl-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>lpg-ehl-cli</artifactId>
    <name>LPG-EHL Command Line Interface</name>
    <description>CLI for testing and operating LPG dispensers</description>

    <dependencies>
        <!-- Core module -->
        <dependency>
            <groupId>no.cloudberries.lpg</groupId>
            <artifactId>lpg-ehl-core</artifactId>
        </dependency>
        
        <!-- PLS module for real serial port support -->
        <dependency>
            <groupId>no.cloudberries.lpg</groupId>
            <artifactId>lpg-ehl-pls</artifactId>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring Shell -->
        <dependency>
            <groupId>org.springframework.shell</groupId>
            <artifactId>spring-shell-starter</artifactId>
            <version>3.2.0</version>
        </dependency>

        <!-- Kotlin -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
        </dependency>

        <!-- Coroutines -->
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-core</artifactId>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>src/main/kotlin</sourceDirectory>
        
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
            </plugin>
            
            <!-- Spring Boot executable JAR -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>no.cloudberries.lpg.cli.CliApplicationKt</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 5.2 Opprett Spring Boot Application

**Fil:** `lpg-ehl-cli/src/main/kotlin/no/cloudberries/lpg/cli/CliApplication.kt`

```kotlin
package no.cloudberries.lpg.cli

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * LPG-EHL Command Line Interface Application.
 * 
 * Bruker Spring Shell for interaktiv terminal-basert kommandolinje.
 * Støtter både LAB MODE (emulator) og FIELD MODE (ekte serial port).
 */
@SpringBootApplication
class CliApplication

fun main(args: Array<String>) {
    runApplication<CliApplication>(*args)
}
```

### 5.3 Opprett Transport Configuration for CLI

**Fil:** `lpg-ehl-cli/src/main/kotlin/no/cloudberries/lpg/cli/config/CliTransportConfiguration.kt`

```kotlin
package no.cloudberries.lpg.cli.config

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import no.cloudberries.lpg.pls.RealSerialTransport
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CliTransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(CliTransportConfiguration::class.java)
    
    /**
     * LAB MODE transport
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "ehl.emulator",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun inMemoryTransport(
        @Value("\${ehl.emulator.dispenser-address:1}") address: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int
    ): SerialTransport {
        logger.info("🧪 CLI LAB MODE: Using emulator")
        val emulator = EhlDispenserEmulator(address, pricePerLiterCents)
        return InMemorySerialPort(emulator, 20)
    }
    
    /**
     * FIELD MODE transport
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "ehl.emulator",
        name = ["enabled"],
        havingValue = "false"
    )
    fun realSerialTransport(
        @Value("\${ehl.serial.port:/dev/ttyS0}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int
    ): SerialTransport {
        logger.info("🏭 CLI FIELD MODE: Using real serial port $portName")
        return RealSerialTransport(portName, baudRate)
    }
    
    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport::class.simpleName}")
        if (!transport.isConnected) {
            transport.connect()
        }
        return EhlCommunicator(transport)
    }
}
```

### 5.4 Implementer CLI Commands

**Fil:** `lpg-ehl-cli/src/main/kotlin/no/cloudberries/lpg/cli/commands/PumpCommands.kt`

```kotlin
package no.cloudberries.lpg.cli.commands

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.*
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

/**
 * Spring Shell commands for pump control.
 */
@ShellComponent
class PumpCommands(
    private val communicator: EhlCommunicator
) {
    
    @ShellMethod("Unblock a pump (allow dispensing)")
    fun unblock(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.UNBLOCK,
            data = byteArrayOf()
        )
        
        println("📤 TX: UNBLOCK to pump $address")
        printHex("TX", EhlCodec.encode(packet))
        
        val response = communicator.sendAndReceive(packet, 2000)
        printHex("RX", EhlCodec.encode(response))
        
        if (response.command == EhlCommand.OK) {
            "✅ Pump $address unblocked successfully"
        } else {
            "❌ Unexpected response: ${response.command}"
        }
    }
    
    @ShellMethod("Block a pump (prevent dispensing)")
    fun block(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.BLOCK,
            data = byteArrayOf()
        )
        
        println("📤 TX: BLOCK to pump $address")
        printHex("TX", EhlCodec.encode(packet))
        
        val response = communicator.sendAndReceive(packet, 2000)
        printHex("RX", EhlCodec.encode(response))
        
        if (response.command == EhlCommand.OK) {
            "✅ Pump $address blocked successfully"
        } else {
            "❌ Unexpected response: ${response.command}"
        }
    }
    
    @ShellMethod("Query pump state")
    fun state(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.STATE,
            data = byteArrayOf()
        )
        
        println("📤 TX: STATE query to pump $address")
        printHex("TX", EhlCodec.encode(packet))
        
        val response = communicator.sendAndReceive(packet, 2000)
        printHex("RX", EhlCodec.encode(response))
        
        if (response.command == EhlCommand.STATE && response.data.isNotEmpty()) {
            val statusByte = EhlDataParser.parseStateData(response.data)
            val status = DispenserStateMapper.mapFromStatusByte(statusByte)
            "📊 Pump $address status: $status (raw: 0x${statusByte.toString(16).uppercase()})"
        } else {
            "❌ Invalid response"
        }
    }
    
    @ShellMethod("Query pump volume")
    fun volume(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.VOLUME,
            data = byteArrayOf()
        )
        
        println("📤 TX: VOLUME query to pump $address")
        printHex("TX", EhlCodec.encode(packet))
        
        val response = communicator.sendAndReceive(packet, 2000)
        printHex("RX", EhlCodec.encode(response))
        
        if (response.command == EhlCommand.VOLUME && response.data.size >= 4) {
            val (liters, cents) = EhlDataParser.parseVolumeData(response.data)
            val nok = cents / 100.0
            "📊 Pump $address: ${liters}L, ${nok} NOK"
        } else {
            "❌ Invalid response"
        }
    }
    
    @ShellMethod("Run line test (check connectivity)")
    fun linetest(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.LINETEST,
            data = byteArrayOf()
        )
        
        println("📤 TX: LINETEST to pump $address")
        printHex("TX", EhlCodec.encode(packet))
        
        val response = communicator.sendAndReceive(packet, 2000)
        printHex("RX", EhlCodec.encode(response))
        
        if (response.command == EhlCommand.OK) {
            "✅ Pump $address connectivity OK"
        } else {
            "❌ Connectivity test failed"
        }
    }
    
    private fun printHex(label: String, bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        println("   $label HEX: [$hex]")
    }
}
```

**Fil:** `lpg-ehl-cli/src/main/kotlin/no/cloudberries/lpg/cli/commands/DiagnosticCommands.kt`

```kotlin
package no.cloudberries.lpg.cli.commands

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.*
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
class DiagnosticCommands(
    private val communicator: EhlCommunicator
) {
    
    @ShellMethod("Query tank error status")
    fun error(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.TANK_ERROR,
            data = byteArrayOf()
        )
        
        val response = communicator.sendAndReceive(packet, 2000)
        
        if (response.command == EhlCommand.TANK_ERROR && response.data.isNotEmpty()) {
            val errorByte = response.data[0].toInt() and 0xFF
            if (errorByte == 0) {
                "✅ Pump $address: No errors"
            } else {
                "⚠️ Pump $address error status: 0x${errorByte.toString(16).uppercase()}"
            }
        } else {
            "❌ Invalid response"
        }
    }
    
    @ShellMethod("Query tank levels")
    fun tank(
        @ShellOption(defaultValue = "1") address: Int
    ): String = runBlocking {
        val packet = EhlPacket(
            address = address,
            command = EhlCommand.TANK,
            data = byteArrayOf()
        )
        
        val response = communicator.sendAndReceive(packet, 2000)
        
        if (response.command == EhlCommand.TANK && response.data.size >= 4) {
            // Parse tank data (format TBD based on VB6 specs)
            "📊 Pump $address tank data: ${response.data.size} bytes"
        } else {
            "❌ Invalid response"
        }
    }
}
```

### 5.5 Opprett application.yaml for CLI

**Fil:** `lpg-ehl-cli/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: lpg-ehl-cli
  main:
    banner-mode: off  # Disable Spring Boot banner for cleaner CLI
  shell:
    interactive:
      enabled: true
    history:
      enabled: true

# EHL Configuration (same as API)
ehl:
  emulator:
    enabled: ${EHL_EMULATOR_ENABLED:true}  # Default: LAB MODE
    dispenser-address: ${EHL_DISPENSER_ADDRESS:1}
    price-per-liter-cents: ${EHL_PRICE_CENTS:1590}
  serial:
    port: ${EHL_SERIAL_PORT:/dev/ttyS0}
    baud-rate: ${EHL_BAUD_RATE:9600}

# Logging
logging:
  level:
    root: INFO
    no.cloudberries.lpg: INFO
    org.springframework: WARN
```

### 5.6 Lag README for CLI

**Fil:** `lpg-ehl-cli/README.md`

```markdown
# LPG-EHL CLI

Command Line Interface for testing and operating LPG dispensers.

## Bygg

```bash
cd lpg-ehl-cli
mvn clean package
```

## Kjør

### LAB MODE (med emulator)

```bash
java -jar target/lpg-ehl-cli-0.0.1-SNAPSHOT.jar
```

### FIELD MODE (ekte serial port)

```bash
EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 \
java -jar target/lpg-ehl-cli-0.0.1-SNAPSHOT.jar
```

## Kommandoer

### Pump Control

```shell
shell:> unblock 1           # Allow pump 1 to dispense
shell:> block 1             # Prevent pump 1 from dispensing
```

### Diagnostics

```shell
shell:> state 1             # Query pump state
shell:> volume 1            # Query current volume
shell:> linetest 1          # Test connectivity
shell:> error 1             # Query error status
shell:> tank 1              # Query tank levels
```

### Shell Commands

```shell
shell:> help                # Show all commands
shell:> exit                # Exit CLI
```

## Output Format

All commands print:
- 📤 TX HEX: Bytes sent to pump
- 📥 RX HEX: Bytes received from pump
- High-level result message

Example:
```
shell:> unblock 1
📤 TX: UNBLOCK to pump 1
   TX HEX: [10 01 04 00 15 36]
📥 RX HEX: [20 01 05 00 24 36]
   RX parsed: OK
✅ Pump 1 unblocked successfully
```
```

---

## Del 6: Testing Strategy

### 6.1 Unit Tests for RealSerialTransport

**Fil:** `lpg-ehl-pls/src/test/kotlin/no/cloudberries/lpg/pls/RealSerialTransportTest.kt`

```kotlin
package no.cloudberries.lpg.pls

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Integration tests for RealSerialTransport.
 * 
 * Disse testene kjører kun hvis SERIAL_PORT_TEST=true environment variable er satt,
 * siden de krever ekte serial port hardware eller serial port emulator (socat).
 */
class RealSerialTransportTest {
    
    @Test
    @EnabledIfEnvironmentVariable(named = "SERIAL_PORT_TEST", matches = "true")
    fun testConnectAndDisconnect() {
        val portName = System.getenv("SERIAL_PORT_NAME") ?: "/dev/ttyS0"
        val transport = RealSerialTransport(portName, 9600)
        
        val connected = transport.connect()
        assert(connected) { "Failed to connect to $portName" }
        assert(transport.isConnected) { "Transport should be connected" }
        
        transport.disconnect()
        assert(!transport.isConnected) { "Transport should be disconnected" }
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "SERIAL_PORT_TEST", matches = "true")
    fun testWriteAndRead() {
        val portName = System.getenv("SERIAL_PORT_NAME") ?: "/dev/ttyS0"
        val transport = RealSerialTransport(portName, 9600)
        
        transport.connect()
        
        val testData = byteArrayOf(0x10, 0x01, 0x05, 0x00, 0x14, 0x36)
        val written = transport.write(testData)
        assert(written == testData.size) { "Should write all bytes" }
        
        // Wait for response (assumes loopback or connected device)
        Thread.sleep(100)
        
        val received = transport.readAvailable(256)
        assert(received.isNotEmpty()) { "Should receive data" }
        
        transport.disconnect()
    }
}
```

### 6.2 Integration Test Plan

**Testmiljø:**
1. **Emulator test (CI/CD safe):**
   - Start API med `ehl.emulator.enabled=true`
   - Start CLI med samme setting
   - Kjør CLI kommandoer og verifiser output

2. **Serial loopback test (requires hardware):**
   - Koble TX og RX sammen på serial port (loopback)
   - Start CLI med `ehl.emulator.enabled=false`
   - Kjør LINETEST og verifiser at sendte bytes kommer tilbake

3. **Emulator TCP test (optional):**
   - Start lpg-ehl-emulator på TCP port 9000
   - Bruk socat eller nc for å teste kommandoer

### 6.3 Manual Test Checklist

**LAB MODE Test:**
```bash
# Terminal 1: Start API
cd lpg-ehl-api
mvn spring-boot:run

# Terminal 2: Start CLI
cd lpg-ehl-cli
mvn spring-boot:run

# In CLI:
shell:> linetest 1
shell:> state 1
shell:> unblock 1
shell:> volume 1
```

**FIELD MODE Test (on Debian with /dev/ttyS0):**
```bash
# Set FIELD MODE
export EHL_EMULATOR_ENABLED=false
export EHL_SERIAL_PORT=/dev/ttyS0

# Start CLI
cd lpg-ehl-cli
mvn spring-boot:run

# In CLI:
shell:> linetest 1     # Should communicate with real pump
shell:> state 1
```

---

## Del 7: Documentation Updates

### 7.1 Oppdater WARP.md

**Fil:** `/Users/tandersen/git/NorgesGass/lpg-ehl/WARP.md`

**Legg til seksjon:**
```markdown
## CLI Module (lpg-ehl-cli)

Spring Shell-basert kommandolinje for testing og drift av pumper.

### Kommandoer

- **pump**: unblock, block
- **diag**: state, volume, linetest, error, tank

### Bruk

```bash
# LAB MODE (emulator)
cd lpg-ehl-cli
mvn spring-boot:run

# FIELD MODE (ekte serial port)
EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 mvn spring-boot:run
```

## PLS Module (lpg-ehl-pls)

Physical Layer Support for ekte RS-485 serial kommunikasjon.

### RealSerialTransport

Implementasjon av `SerialTransport` interface med jSerialComm.

**Konfigurasjon:**
- `ehl.serial.port`: Serial port device (default: /dev/ttyS0)
- `ehl.serial.baud-rate`: Baud rate (default: 9600)

**Serial parametere:** 9600 8E1, even parity
```

### 7.2 Lag ny ARCHITECTURE.md

**Fil:** `/Users/tandersen/git/NorgesGass/lpg-ehl/docs/ARCHITECTURE.md`

```markdown
# LPG-EHL Architecture

## Module Structure

```
lpg-ehl/
├── lpg-ehl-core/          # Protocol implementation (transport-agnostic)
│   ├── protocol/          # EhlCodec, EhlPacket, EhlCommand
│   ├── communication/     # EhlCommunicator, PacketDispatcher
│   └── transport/         # SerialTransport interface
│
├── lpg-ehl-pls/           # Physical Layer Support (real serial port)
│   └── RealSerialTransport.kt
│
├── lpg-ehl-emulator/      # Dispenser emulator (for testing)
│   ├── EhlDispenserEmulator.kt
│   └── InMemorySerialPort.kt
│
├── lpg-ehl-api/           # Spring Boot REST API + WebSocket GUI
│   ├── service/           # DispenserService, PumpStateService
│   └── controller/        # REST endpoints
│
└── lpg-ehl-cli/           # Spring Shell CLI
    └── commands/          # PumpCommands, DiagnosticCommands
```

## Transport Layer Abstraction

### SerialTransport Interface

Common interface for all transport implementations:

```
SerialTransport
    ├── InMemorySerialPort (LAB MODE)
    └── RealSerialTransport (FIELD MODE)
```

**Selection via Spring Configuration:**

```yaml
ehl:
  emulator:
    enabled: true   # LAB MODE -> InMemorySerialPort
    enabled: false  # FIELD MODE -> RealSerialTransport
```

### Packet Dispatcher Architecture

```
EhlCommunicator
    ├── Background Reader (coroutine)
    │   └── Continuously reads from transport
    │       └── Parses packets
    │           ├── Dispatches unsolicited packets (STATE, VOLUME) to handlers
    │           └── Queues response packets for awaitResponse()
    │
    └── PacketDispatcher
        ├── unsolicitedHandlers map
        └── receivedPackets queue
```

**Benefits:**
- Handles back-to-back packets (OK + STATE + VOLUME in same RX chunk)
- Non-blocking reads
- Unsolicited packet handling (STATE updates from pump without query)
- Clean timeout handling

## Shared Service Layer

Both API and CLI use the same business logic:

```
DispenserService
    ├── handlePacket()        # State machine
    ├── queuePriceUpdate()    # Safe price updates
    └── State tracking        # IDLE, STARTED, FILLING, FINISHED

PumpStateService (future)
    ├── Polling logic
    └── State callbacks
```

## Configuration Examples

### LAB MODE (API + CLI)

```yaml
ehl:
  emulator:
    enabled: true
    dispenser-address: 1
    price-per-liter-cents: 1590
```

### FIELD MODE (Debian edge device)

```yaml
ehl:
  emulator:
    enabled: false
  serial:
    port: /dev/ttyS0
    baud-rate: 9600
```
```

---

## Del 8: Implementation Roadmap

### Fase 1: Core Refactoring (Priority: HIGH)

1. ✅ Opprett `SerialTransport` interface
2. ✅ Refaktorer `InMemorySerialPort` til `SerialTransport`
3. ✅ Refaktorer `EhlCommunicator` til å bruke `SerialTransport`
4. ✅ Verifiser at alle tester passerer

**Estimert tid:** 1-2 timer  
**Risk:** Medium (store endringer i core)

### Fase 2: PLS Module (Priority: HIGH)

1. ✅ Oppdater `lpg-ehl-pls/pom.xml`
2. ✅ Implementer `RealSerialTransport`
3. ✅ Skriv README for PLS
4. ✅ Skriv unit tests (optional, kun med hardware)

**Estimert tid:** 2-3 timer  
**Risk:** Low (ny modul, ingen eksisterende kode påvirkes)

### Fase 3: Transport Configuration (Priority: HIGH)

1. ✅ Opprett `TransportConfiguration` i API
2. ✅ Refaktorer `CommunicationConfig`
3. ✅ Legg til PLS dependency i API pom.xml
4. ✅ Test switching mellom LAB og FIELD mode

**Estimert tid:** 1-2 timer  
**Risk:** Low (Spring configuration)

### Fase 4: PacketDispatcher (Priority: MEDIUM)

1. ⏳ Implementer `PacketDispatcher`
2. ⏳ Integrer i `EhlCommunicator`
3. ⏳ Test multi-packet scenarios
4. ⏳ Test unsolicited packet handling

**Estimert tid:** 3-4 timer  
**Risk:** High (kompleks concurrency logic)

**NOTE:** Denne fasen kan utsettes hvis single-packet håndtering er tilstrekkelig for første release.

### Fase 5: CLI Module (Priority: MEDIUM)

1. ✅ Oppdater `lpg-ehl-cli/pom.xml`
2. ✅ Opprett Spring Boot application
3. ✅ Opprett `CliTransportConfiguration`
4. ✅ Implementer `PumpCommands`
5. ✅ Implementer `DiagnosticCommands`
6. ✅ Opprett `application.yaml`
7. ✅ Skriv README for CLI

**Estimert tid:** 3-4 timer  
**Risk:** Low (standalone modul)

### Fase 6: Testing & Documentation (Priority: LOW)

1. ⏳ Manual testing av LAB MODE (CLI + API)
2. ⏳ Manual testing av FIELD MODE (requires hardware)
3. ⏳ Oppdater WARP.md
4. ⏳ Skriv ARCHITECTURE.md
5. ⏳ Integration test plan

**Estimert tid:** 2-3 timer  
**Risk:** Low

---

## Del 9: Success Criteria

### Minimum Viable Implementation (MVI)

1. ✅ `SerialTransport` interface definert i lpg-ehl-core
2. ✅ `RealSerialTransport` implementert i lpg-ehl-pls
3. ✅ Spring Configuration switching mellom LAB og FIELD mode
4. ✅ CLI modul kompilerer og kjører i LAB MODE
5. ✅ CLI kommandoer fungerer med emulator

### Full Implementation

1. ✅ PacketDispatcher håndterer multi-packet scenarios
2. ✅ CLI fungerer i FIELD MODE med ekte serial port
3. ✅ DispenserService delt mellom API og CLI
4. ✅ Robust error handling og recovery
5. ✅ Comprehensive documentation

---

## Del 10: Known Issues & Future Work

### Known Issues

1. **PacketDispatcher concurrency:** Background reader og awaitResponse må være thread-safe
2. **Serial port permissions:** På Linux krever /dev/ttyS0 ofte sudo eller dialout group membership
3. **Unsolicited packets:** Må ha mekanisme for å dispatche STATE/VOLUME updates til riktig handler

### Future Work

1. **Shared service layer:** Refaktorer DispenserService til egen modul som deles av API og CLI
2. **Connection pooling:** Support for multiple serial ports (multi-pump stations)
3. **Async command queue:** Support for queuing commands til busy pumps
4. **Metrics & monitoring:** Expose serial port statistics via CLI kommandoer
5. **Config profiles:** Spring profiles for ulike stasjoner (station-001.yaml, etc.)

---

## Appendix A: File Structure After Implementation

```
lpg-ehl/
├── lpg-ehl-core/
│   └── src/main/kotlin/no/cloudberries/lpg/
│       ├── transport/
│       │   └── SerialTransport.kt                    [NEW]
│       ├── communication/
│       │   ├── EhlCommunicator.kt                    [MODIFIED]
│       │   └── PacketDispatcher.kt                   [NEW]
│       └── emulator/
│           └── InMemorySerialPort.kt                 [MODIFIED]
│
├── lpg-ehl-pls/
│   ├── pom.xml                                       [MODIFIED]
│   ├── README.md                                     [NEW]
│   └── src/main/kotlin/no/cloudberries/lpg/pls/
│       └── RealSerialTransport.kt                    [NEW]
│
├── lpg-ehl-api/
│   ├── pom.xml                                       [MODIFIED - add PLS dependency]
│   └── src/main/kotlin/no/cloudberries/lpg/api/config/
│       ├── TransportConfiguration.kt                 [NEW]
│       └── CommunicationConfig.kt                    [MODIFIED]
│
└── lpg-ehl-cli/
    ├── pom.xml                                       [MODIFIED]
    ├── README.md                                     [NEW]
    └── src/main/kotlin/no/cloudberries/lpg/cli/
        ├── CliApplication.kt                         [NEW]
        ├── config/
        │   └── CliTransportConfiguration.kt          [NEW]
        ├── commands/
        │   ├── PumpCommands.kt                       [NEW]
        │   └── DiagnosticCommands.kt                 [NEW]
        └── resources/
            └── application.yaml                      [NEW]
```

---

## Appendix B: ChatGPT Prompt for Each Phase

### Prompt for Fase 1 (Core Refactoring)

```
Implementer Fase 1 fra IMPLEMENTATION_PLAN_PLS_CLI.md:

1. Opprett SerialTransport interface i lpg-ehl-core/src/main/kotlin/no/cloudberries/lpg/transport/SerialTransport.kt
2. Refaktorer InMemorySerialPort til å implementere SerialTransport (rename read() til readAvailable())
3. Refaktorer EhlCommunicator til å bruke SerialTransport i stedet for SerialPortIO
4. Kjør alle tester og verifiser at alt kompilerer

Ikke gå videre til Fase 2 før alt kompilerer 100%.
```

### Prompt for Fase 2 (PLS Module)

```
Implementer Fase 2 fra IMPLEMENTATION_PLAN_PLS_CLI.md:

1. Oppdater lpg-ehl-pls/pom.xml med dependencies (se plan)
2. Implementer RealSerialTransport.kt (se kode i plan)
3. Lag README.md for PLS modul (se plan)

Test at modulen kompilerer med: cd lpg-ehl-pls && mvn clean compile
```

### Prompt for Fase 3 (Transport Configuration)

```
Implementer Fase 3 fra IMPLEMENTATION_PLAN_PLS_CLI.md:

1. Opprett TransportConfiguration.kt i lpg-ehl-api
2. Refaktorer CommunicationConfig.kt til å bruke SerialTransport
3. Legg til PLS dependency i lpg-ehl-api/pom.xml
4. Test at API starter i LAB MODE: mvn spring-boot:run

Verifiser at log viser "🧪 LAB MODE: Using emulator".
```

### Prompt for Fase 4 (PacketDispatcher)

```
Implementer Fase 4 fra IMPLEMENTATION_PLAN_PLS_CLI.md:

1. Opprett PacketDispatcher.kt (se kode i plan)
2. Integrer PacketDispatcher i EhlCommunicator
3. Refaktorer sendAndReceive() til å bruke awaitResponse()
4. Test med emulator

VIKTIG: Dette er en kompleks endring. Test grundig!
```

### Prompt for Fase 5 (CLI Module)

```
Implementer Fase 5 fra IMPLEMENTATION_PLAN_PLS_CLI.md:

1. Oppdater lpg-ehl-cli/pom.xml (se plan)
2. Opprett CliApplication.kt, CliTransportConfiguration.kt
3. Implementer PumpCommands.kt og DiagnosticCommands.kt
4. Opprett application.yaml for CLI
5. Lag README.md

Test CLI: cd lpg-ehl-cli && mvn spring-boot:run
Prøv kommandoer: linetest 1, state 1, unblock 1
```

---

## Appendix C: Testing Commands

### Kompiler alle moduler
```bash
cd /Users/tandersen/git/NorgesGass/lpg-ehl
mvn clean install
```

### Test API (LAB MODE)
```bash
cd lpg-ehl-api
mvn spring-boot:run
# Verifiser log: "🧪 LAB MODE"
```

### Test API (FIELD MODE)
```bash
cd lpg-ehl-api
EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 mvn spring-boot:run
# Verifiser log: "🏭 FIELD MODE"
```

### Test CLI (LAB MODE)
```bash
cd lpg-ehl-cli
mvn spring-boot:run
# In shell:
shell:> linetest 1
shell:> state 1
shell:> unblock 1
```

### Test CLI (FIELD MODE)
```bash
cd lpg-ehl-cli
EHL_EMULATOR_ENABLED=false EHL_SERIAL_PORT=/dev/ttyS0 mvn spring-boot:run
# In shell:
shell:> linetest 1
```

---

## End of Implementation Plan

**Versjon:** 1.0  
**Dato:** 2025-01-12  
**Forfatter:** Warp AI Agent (Claude 4.5 Sonnet)

For spørsmål eller issues, se WARP.md eller kontakt prosjekteier.
