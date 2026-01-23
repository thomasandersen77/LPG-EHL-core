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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Transport Configuration - Tri-Mode Architecture.
 * 
 * Selects transport based on `ehl.transport.mode` property:
 * 
 * EMULATOR (Default for webapp):
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - Perfect for development and testing
 * 
 * SOCAT (Integration testing):
 *   - Uses RealSerialTransport + socat virtual PTY
 *   - PLS Simulator runs on the other end
 *   - Realistic serial testing without hardware
 * 
 * HARDWARE (Production):
 *   - Uses RealSerialTransport + physical RS-485
 *   - Communicates with real LPG dispenser
 *   - Requires /dev/ttyS0 or similar serial device
 * 
 * Legacy support: `ehl.emulator.enabled` is still honored for backwards compatibility.
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * EMULATOR MODE: In-memory serial port with emulator.
     * Default configuration - safe for development.
     * 
     * Activated when:
     *   - ehl.transport.mode=EMULATOR (explicit)
     *   - ehl.transport.mode is not set AND ehl.emulator.enabled=true (legacy)
     *   - Both properties are missing (default)
     */
    @Bean
    @ConditionalOnExpression(
        "'\${ehl.transport.mode:}'.equalsIgnoreCase('EMULATOR') or " +
        "('\${ehl.transport.mode:}'.isEmpty() and '\${ehl.emulator.enabled:true}'.equalsIgnoreCase('true'))"
    )
    fun emulatorModeTransport(
        emulator: EhlDispenserEmulator,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔬 EMULATOR MODE")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Transport:  InMemorySerialPort")
        logger.info("  Backend:    EhlDispenserEmulator")
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
     * Auto-connects on startup.
     */
    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport.javaClass.simpleName}")
        
        val communicator = EhlCommunicator(transport)
        
        // Connect transport
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
     * High-level operations service - Shared by API and CLI.
     */
    @Bean
    fun ehlOperationsService(communicator: EhlCommunicator): EhlOperationsService {
        logger.info("Creating EhlOperationsService")
        return EhlOperationsService(communicator)
    }
}
