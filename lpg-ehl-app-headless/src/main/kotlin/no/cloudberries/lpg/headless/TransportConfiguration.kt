package no.cloudberries.lpg.headless

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.HardwareWatchdogCapable
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Transport Configuration for Headless Application - Tri-Mode Architecture.
 * 
 * Unified with webapp: Uses `ehl.transport.mode` property.
 * 
 * EMULATOR (ehl.transport.mode=EMULATOR or lpg.mode=LAB):
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - For testing and development
 * 
 * SOCAT (ehl.transport.mode=SOCAT):
 *   - Uses SerialPortManager + socat virtual PTY
 *   - PLS Simulator runs on the other end
 *   - Realistic serial testing without hardware
 * 
 * HARDWARE (ehl.transport.mode=HARDWARE or lpg.mode=FIELD) - DEFAULT FOR HEADLESS:
 *   - Uses SerialPortManager + physical RS-485 serial port
 *   - Communicates with real LPG dispenser hardware
 *   - Production deployment on Raspberry Pi or edge device
 * 
 * Serial Port Configuration (Environment Variables):
 *   EHL_SERIAL_PORT      - Serial port device (default: /dev/ttyS0)
 *   EHL_BAUD_RATE        - Baud rate (default: 9600)
 *   EHL_DATA_BITS        - Data bits (default: 8)
 *   EHL_PARITY           - Parity: NONE, ODD, EVEN, MARK, SPACE (default: EVEN)
 *   EHL_STOP_BITS        - Stop bits: 1, 1.5, 2 (default: 1)
 * 
 * Standard EHL Protocol: 9600 baud, 8E1 (8 data bits, Even parity, 1 stop bit)
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * EMULATOR MODE: In-memory serial port with emulator.
     * Used for development and testing when no physical hardware is available.
     * 
     * Activated when:
     *   - ehl.transport.mode=EMULATOR (unified with webapp)
     *   - lpg.mode=LAB AND ehl.transport.mode is NOT set (legacy support)
     * 
     * NOT activated when ehl.transport.mode=SOCAT or HARDWARE (explicit mode takes precedence)
     */
    @Bean
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('EMULATOR') or " +
        "('\${lpg.mode:}'.equalsIgnoreCase('LAB') and '\${ehl.transport.mode:}'.isEmpty())"
    )
    fun emulatorModeTransport(
        @Value("\${ehl.emulator.dispenser-address:1}") dispenserAddress: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔬 EMULATOR MODE (HEADLESS)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:  InMemorySerialPort")
        logger.info("  Backend:    EhlDispenserEmulator")
        logger.info("  Dispenser:  Address $dispenserAddress")
        logger.info("  Price:      ${pricePerLiterCents / 100.0} kr/L")
        logger.info("  Latency:    ${latencyMs}ms (simulated)")
        logger.info("  Hardware:   NOT REQUIRED")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        val emulator = EhlDispenserEmulator(
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents
        )
        
        val transport = InMemorySerialPort(emulator, latencyMs)
        transport.connect()
        
        return transport
    }
    
    /**
     * SOCAT MODE: SerialPortManager to virtual PTY.
     * For integration testing with PLS Simulator.
     * 
     * Uses SerialPortManager with watchdog for self-healing:
     * - Survives socat/simulator restart without app restart
     * - Automatic reconnect on I/O failure
     * 
     * Setup:
     *   1. Run: socat -d -d pty,raw,echo=0,link=/tmp/ttyV0 pty,raw,echo=0,link=/tmp/ttyV1
     *   2. Start PLS Simulator on /tmp/ttyV0
     *   3. Start this app with --ehl.transport.mode=SOCAT --ehl.serial.port=/tmp/ttyV1
     */
    @Bean("socatSerialPortManager")
    @ConditionalOnProperty(
        name = ["ehl.transport.mode"],
        havingValue = "SOCAT",
        matchIfMissing = false
    )
    fun socatModeSerialPortManager(
        @Value("\${ehl.serial.port:/tmp/ttyV1}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int,
        @Value("\${ehl.serial.read-timeout-ms:3000}") readTimeout: Int,
        @Value("\${ehl.serial.write-timeout-ms:1000}") writeTimeout: Int
    ): SerialPortManager {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔗 SOCAT MODE (HEADLESS)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:   SerialPortManager (with watchdog)")
        logger.info("  Serial Port: $portName")
        logger.info("  Baud Rate:   $baudRate")
        logger.info("  Read Timeout: ${readTimeout}ms")
        logger.info("  Protocol:    EHL over virtual PTY")
        logger.info("  ──────────────────────────────────────────────────────────")
        logger.info("  📋 Setup required:")
        logger.info("     1. socat running with PTY pair")
        logger.info("     2. PLS Simulator on other end of PTY")
        logger.info("  ──────────────────────────────────────────────────────────")
        logger.info("  💡 TIP: Use scripts/start-socat-sim.sh for auto setup")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        val config = SerialPortConfig(
            portName = portName,
            baudRate = baudRate,
            dataBits = 8,
            stopBits = SerialPort.ONE_STOP_BIT,
            parity = SerialPort.EVEN_PARITY,
            readTimeout = readTimeout,
            writeTimeout = writeTimeout
        )
        
        val manager = SerialPortManager(config)
        manager.enableWatchdog()
        logger.info("🐕 Hardware watchdog enabled for SOCAT mode")
        
        return manager
    }
    
    /**
     * Expose SOCAT SerialPortManager as SerialTransport.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = ["ehl.transport.mode"],
        havingValue = "SOCAT",
        matchIfMissing = false
    )
    fun socatModeTransport(@Qualifier("socatSerialPortManager") manager: SerialPortManager): SerialTransport = manager
    
    /**
     * Expose SOCAT SerialPortManager as HardwareWatchdogCapable.
     */
    @Bean
    @ConditionalOnProperty(
        name = ["ehl.transport.mode"],
        havingValue = "SOCAT",
        matchIfMissing = false
    )
    fun socatModeWatchdog(@Qualifier("socatSerialPortManager") manager: SerialPortManager): HardwareWatchdogCapable = manager
    
    /**
     * HARDWARE MODE: Real serial port for production hardware using SerialPortManager.
     * DEFAULT MODE for headless deployment.
     * 
     * Activated when:
     *   - ehl.transport.mode=HARDWARE (unified with webapp)
     *   - lpg.mode=FIELD (legacy support)
     *   - Neither property set (default for headless)
     * 
     * SerialPortManager provides:
     * - Robust write handling with partial write retries
     * - Automatic disconnect on I/O failure (enables clean reconnect)
     * - Hardware watchdog capability for self-healing
     * - Same behavior for PTY (/tmp/ttyV1) and real ports (/dev/ttyS1)
     * 
     * Supports full RS-485/RS-232 configuration:
     * - COM port / device path
     * - Baud rate
     * - Data bits (5, 6, 7, 8)
     * - Parity (NONE, ODD, EVEN, MARK, SPACE)
     * - Stop bits (1, 1.5, 2)
     */
    @Bean("hardwareSerialPortManager")
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "'\${lpg.mode:}'.equalsIgnoreCase('FIELD') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${lpg.mode:}'.isEmpty())"
    )
    fun hardwareModeSerialPortManager(
        @Value("\${ehl.serial.port:/dev/ttyS0}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int,
        @Value("\${ehl.serial.data-bits:8}") dataBits: Int,
        @Value("\${ehl.serial.parity:EVEN}") parity: String,
        @Value("\${ehl.serial.stop-bits:1}") stopBits: Int,
        @Value("\${ehl.serial.read-timeout-ms:3000}") readTimeout: Int,
        @Value("\${ehl.serial.write-timeout-ms:1000}") writeTimeout: Int
    ): SerialPortManager {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🏭 HARDWARE MODE (HEADLESS)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:   SerialPortManager (with watchdog)")
        logger.info("  Serial Port: $portName")
        logger.info("  Baud Rate:   $baudRate")
        logger.info("  Data Bits:   $dataBits")
        logger.info("  Parity:      $parity")
        logger.info("  Stop Bits:   $stopBits")
        logger.info("  Read Timeout: ${readTimeout}ms")
        logger.info("  Write Timeout: ${writeTimeout}ms")
        logger.info("  Protocol:    EHL over RS-485")
        logger.info("  ──────────────────────────────────────────────────────────")
        logger.info("  ⚠️  WARNING: Communicating with REAL HARDWARE")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        // Map parity string to jSerialComm constant
        val parityMode = when (parity.uppercase()) {
            "NONE" -> SerialPort.NO_PARITY
            "ODD" -> SerialPort.ODD_PARITY
            "EVEN" -> SerialPort.EVEN_PARITY
            "MARK" -> SerialPort.MARK_PARITY
            "SPACE" -> SerialPort.SPACE_PARITY
            else -> {
                logger.warn("Unknown parity '$parity', defaulting to EVEN")
                SerialPort.EVEN_PARITY
            }
        }
        
        // Map stop bits string to jSerialComm constant
        val stopBitsMode = when (stopBits) {
            1 -> SerialPort.ONE_STOP_BIT
            2 -> SerialPort.TWO_STOP_BITS
            else -> SerialPort.ONE_STOP_BIT
        }
        
        val config = SerialPortConfig(
            portName = portName,
            baudRate = baudRate,
            dataBits = dataBits,
            stopBits = stopBitsMode,
            parity = parityMode,
            readTimeout = readTimeout,
            writeTimeout = writeTimeout
        )
        
        val manager = SerialPortManager(config)
        
        // Connect to the serial port
        try {
            if (manager.connect()) {
                logger.info("✅ Connected to serial port $portName")
                // Enable watchdog for self-healing
                manager.enableWatchdog()
                logger.info("🐕 Hardware watchdog enabled")
            } else {
                logger.error("❌ Failed to connect to serial port $portName")
                logger.warn("⚠️  Hardware communication will fail until port is available")
            }
        } catch (e: Exception) {
            logger.error("❌ Error connecting to serial port $portName: ${e.message}")
            logger.warn("⚠️  Will retry on first communication attempt")
        }
        
        return manager
    }
    
    /**
     * Expose HARDWARE SerialPortManager as SerialTransport for dependency injection.
     * Only active in HARDWARE mode.
     */
    @Bean
    @Primary
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "'\${lpg.mode:}'.equalsIgnoreCase('FIELD') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${lpg.mode:}'.isEmpty())"
    )
    fun hardwareModeTransport(@Qualifier("hardwareSerialPortManager") manager: SerialPortManager): SerialTransport = manager
    
    /**
     * Expose HARDWARE SerialPortManager as HardwareWatchdogCapable for watchdog monitoring.
     * Only active in HARDWARE mode.
     */
    @Bean
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "'\${lpg.mode:}'.equalsIgnoreCase('FIELD') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${lpg.mode:}'.isEmpty())"
    )
    fun hardwareModeWatchdog(@Qualifier("hardwareSerialPortManager") manager: SerialPortManager): HardwareWatchdogCapable = manager
    
    /**
     * EHL Communicator - Uses whichever transport is configured.
     * Auto-connects on startup.
     */
    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport.javaClass.simpleName}")
        
        val communicator = EhlCommunicator(transport)
        
        // Connect transport if not already connected
        if (!transport.isConnected) {
            val connected = transport.connect()
            if (connected) {
                logger.info("✅ Transport connected successfully")
            } else {
                logger.error("❌ Failed to connect transport")
                throw IllegalStateException("Failed to connect serial transport")
            }
        }
        
        return communicator
    }
}
