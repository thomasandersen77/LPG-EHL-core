package no.cloudberries.lpg.api.config

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.HardwareWatchdogCapable
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import no.cloudberries.lpg.service.operations.EhlOperationsService
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Transport Configuration - Two Mode Architecture.
 * 
 * Uses `lpg.mode` property:
 * 
 * LAB (lpg.mode=LAB) - DEFAULT:
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - For development and testing
 * 
 * FIELD (lpg.mode=FIELD):
 *   - Uses SerialPortManager + real serial port
 *   - Works with both physical hardware AND socat virtual PTY
 *   - For production deployment or socat testing
 *   - Use ./scripts/start-socat.sh to create virtual PTY pair
 * 
 * Serial Port Configuration:
 *   ehl.serial.port       - Serial port device (default: /tmp/ttyV1 for socat)
 *   ehl.serial.baud-rate  - Baud rate (default: 9600)
 *   ehl.serial.data-bits  - Data bits (default: 8)
 *   ehl.serial.parity     - Parity: NONE, ODD, EVEN (default: EVEN)
 *   ehl.serial.stop-bits  - Stop bits: 1 or 2 (default: 1)
 * 
 * Standard EHL Protocol: 9600 baud, 8E1 (8 data bits, Even parity, 1 stop bit)
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * LAB MODE: In-memory serial port with emulator.
     * Default mode - safe for development and testing.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = ["lpg.mode"],
        havingValue = "LAB",
        matchIfMissing = true  // LAB is the default
    )
    fun labModeTransport(
        emulator: EhlDispenserEmulator,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔬 LAB MODE")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:  InMemorySerialPort + Emulator")
        logger.info("  Latency:    ${latencyMs}ms (simulated)")
        logger.info("  Hardware:   NOT REQUIRED")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        return InMemorySerialPort(emulator, latencyMs)
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
        logger.info("  🔗 SOCAT MODE (with watchdog)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:  SerialPortManager")
        logger.info("  Serial Port: $portName")
        logger.info("  Baud Rate:  $baudRate")
        logger.info("  Read Timeout: ${readTimeout}ms")
        logger.info("  Protocol:   EHL over virtual PTY")
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
     * HARDWARE MODE: SerialPortManager for production hardware.
     * 
     * Uses SerialPortManager with watchdog for self-healing:
     * - Survives cable disconnect/reconnect
     * - Automatic reconnect on I/O failure
     * 
     * Activated when:
     *   - ehl.transport.mode=HARDWARE (explicit)
     *   - ehl.transport.mode is not set AND ehl.emulator.enabled=false (legacy)
     */
    @Bean("hardwareSerialPortManager")
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${ehl.emulator.enabled:true}'.equalsIgnoreCase('false'))"
    )
    fun hardwareModeSerialPortManager(
        @Value("\${ehl.serial.port}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int,
        @Value("\${ehl.serial.read-timeout-ms:3000}") readTimeout: Int,
        @Value("\${ehl.serial.write-timeout-ms:1000}") writeTimeout: Int
    ): SerialPortManager {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🏭 HARDWARE MODE (with watchdog)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:  SerialPortManager")
        logger.info("  Serial Port: $portName")
        logger.info("  Baud Rate:  $baudRate")
        logger.info("  Read Timeout: ${readTimeout}ms")
        logger.info("  Protocol:   EHL over RS-485")
        logger.info("  ──────────────────────────────────────────────────────────")
        logger.info("  ⚠️  WARNING: Communicating with REAL HARDWARE")
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
        logger.info("🐕 Hardware watchdog enabled for HARDWARE mode")
        
        return manager
    }
    
    /**
     * Expose HARDWARE SerialPortManager as SerialTransport.
     */
    @Bean
    @Primary
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${ehl.emulator.enabled:true}'.equalsIgnoreCase('false'))"
    )
    fun hardwareModeTransport(@Qualifier("hardwareSerialPortManager") manager: SerialPortManager): SerialTransport = manager
    
    /**
     * Expose HARDWARE SerialPortManager as HardwareWatchdogCapable.
     */
    @Bean
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('HARDWARE') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${ehl.emulator.enabled:true}'.equalsIgnoreCase('false'))"
    )
    fun hardwareModeWatchdog(@Qualifier("hardwareSerialPortManager") manager: SerialPortManager): HardwareWatchdogCapable = manager
    
    /**
     * EHL Communicator - Uses whichever transport is configured.
     */
    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport.javaClass.simpleName}")
        
        val communicator = EhlCommunicator(transport)
        
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
    
    /**
     * High-level operations service.
     */
    @Bean
    fun ehlOperationsService(communicator: EhlCommunicator): EhlOperationsService {
        logger.info("Creating EhlOperationsService")
        return EhlOperationsService(communicator)
    }
}
