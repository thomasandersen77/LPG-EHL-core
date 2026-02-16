package no.cloudberries.lpg.api.config

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.HardwareWatchdogCapable
import no.cloudberries.lpg.communication.RetryConfig
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.emulator.IEhlDispenserEmulator
import no.cloudberries.lpg.emulator.impl.InMemorySerialPort
import no.cloudberries.lpg.service.operations.EhlOperationsService
import no.cloudberries.lpg.service.service.SerialConfigurationService
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * Transport Configuration - Two Mode Architecture using Spring Profiles.
 * 
 * PROFILES:
 * 
 * lab (default when no profile specified):
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - For development and testing
 * 
 * field:
 *   - Uses SerialPortManager + real serial port
 *   - Works with both physical hardware AND socat virtual PTY
 *   - For production deployment or socat testing
 *   - Use ./scripts/start-socat-sim.sh to create virtual PTY pair
 * 
 * Serial Port Configuration (field profile):
 *   ehl.serial.port                - Serial port device (default: /tmp/vserial1 for socat)
 *   ehl.serial.baud-rate           - Baud rate (default: 9600)
 *   ehl.serial.data-bits           - Data bits (default: 8)
 *   ehl.serial.parity              - Parity: NONE, ODD, EVEN (default: NONE for simulator)
 *   ehl.serial.parity-auto-detect  - Auto-detect parity mode (default: false)
 *   ehl.serial.stop-bits           - Stop bits: 1 or 2 (default: 1)
 * 
 * Standard EHL Protocol: 9600 baud, 8E1 (8 data bits, Even parity, 1 stop bit)
 * PLS Simulator:         9600 baud, 8N1 (8 data bits, No parity, 1 stop bit)
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * LAB MODE: In-memory serial port with emulator.
     * Active when profile=lab, test, OR when no profile is specified (default).
     */
    @Bean
    @Primary
    @Profile("lab", "test", "default")
    fun labModeTransport(
        emulator: IEhlDispenserEmulator,
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
     * FIELD MODE: Real serial port for production or socat testing.
     * Active only when profile=field.
     * 
     * Works with:
     * - Real hardware: ehl.serial.port=/dev/ttyS0 (or /dev/ttyUSB0, COM1, etc.)
     * - Socat PTY:     ehl.serial.port=/tmp/vserial1 (run ./scripts/start-socat-sim.sh first)
     */
    @Bean
    @Primary
    @Profile("field")
    fun fieldModeTransport(
        serialConfigService: SerialConfigurationService,
        @Value("\${ehl.serial.port:/tmp/vserial1}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int,
        @Value("\${ehl.serial.data-bits:8}") dataBits: Int,
        @Value("\${ehl.serial.parity:NONE}") parity: String,
        @Value("\${ehl.serial.parity-auto-detect:false}") autoDetect: Boolean,
        // Used for parity auto-detect probe (must match actual hardware address in FIELD mode).
        @Value("\${lpg.dispenser.address:1}") dispenserAddress: Int,
        // Optional RS-485 driver direction control (RTS toggle), like Python's `--rs485`.
        @Value("\${ehl.serial.rs485.enabled:false}") rs485Enabled: Boolean,
        @Value("\${ehl.serial.rs485.rts-high-during-send:true}") rs485RtsHighDuringSend: Boolean,
        @Value("\${ehl.serial.rs485.rts-high-after-send:false}") rs485RtsHighAfterSend: Boolean,
        @Value("\${ehl.serial.rs485.rx-during-tx:false}") rs485RxDuringTx: Boolean,
        @Value("\${ehl.serial.rs485.rts-before-ms:0}") rs485RtsBeforeMs: Int,
        @Value("\${ehl.serial.rs485.rts-after-ms:0}") rs485RtsAfterMs: Int,
        @Value("\${ehl.serial.stop-bits:1}") stopBits: Int,
        @Value("\${ehl.serial.read-timeout-ms:3000}") readTimeout: Int,
        @Value("\${ehl.serial.write-timeout-ms:1000}") writeTimeout: Int
    ): SerialTransport = runBlocking {
        val isSocat = portName.contains("ttyV") || portName.contains("pty")
        
        // Auto-detect parity if enabled, otherwise use manual configuration
        val stopBitsMode = when (stopBits) {
            1 -> SerialPort.ONE_STOP_BIT
            2 -> SerialPort.TWO_STOP_BITS
            else -> SerialPort.ONE_STOP_BIT
        }
        
        val baseConfig = if (autoDetect) {
            logger.info("")
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("  🏭 FIELD MODE - AUTO-DETECT" + if (isSocat) " (via SOCAT)" else "")
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("  Serial Port: $portName")
            logger.info("  Auto-detect: ENABLED")
            logger.info("  Probe addr:  $dispenserAddress (lpg.dispenser.address)")
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("")

            // IMPORTANT: auto-detect is best-effort. If it fails, we still start the server
            // so you can use /api/debug/serial/* endpoints to iterate safely.
            runCatching {
                serialConfigService.detectAndConfigureSerial(
                    portName = portName,
                    baudRate = baudRate,
                    dataBits = dataBits,
                    stopBits = stopBitsMode,
                    autoDetect = true,
                    manualParity = null,
                    dispenserAddress = dispenserAddress,
                    autoDetectTimeoutMs = minOf(2000L, readTimeout.toLong())
                )
            }.getOrElse { e ->
                logger.error("Parity auto-detect failed, falling back to manual parity='$parity' (server will still start). Cause: ${e.message}")
                val parityMode = when (parity.uppercase()) {
                    "NONE" -> SerialPort.NO_PARITY
                    "ODD" -> SerialPort.ODD_PARITY
                    "EVEN" -> SerialPort.EVEN_PARITY
                    "MARK" -> SerialPort.MARK_PARITY
                    "SPACE" -> SerialPort.SPACE_PARITY
                    else -> {
                        logger.warn("Unknown parity '$parity', defaulting to NONE")
                        SerialPort.NO_PARITY
                    }
                }
                SerialPortConfig(
                    portName = portName,
                    baudRate = baudRate,
                    dataBits = dataBits,
                    stopBits = stopBitsMode,
                    parity = parityMode,
                    readTimeout = readTimeout,
                    writeTimeout = writeTimeout
                )
            }
        } else {
            logger.info("")
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("  🏭 FIELD MODE" + if (isSocat) " (via SOCAT)" else "")
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("  Transport:   SerialPortManager (with watchdog)")
            logger.info("  Serial Port: $portName")
            logger.info("  Baud Rate:   $baudRate")
            logger.info("  Data Bits:   $dataBits")
            logger.info("  Parity:      $parity")
            logger.info("  Stop Bits:   $stopBits")
            logger.info("  Protocol:    EHL over " + if (isSocat) "virtual PTY" else "RS-485")
            if (isSocat) {
                logger.info("  ──────────────────────────────────────────────────────────")
                logger.info("  💡 TIP: Run ./scripts/start-socat.sh in another terminal")
            } else {
                logger.info("  ──────────────────────────────────────────────────────────")
                logger.info("  ⚠️  Communicating with REAL HARDWARE")
                logger.info("  💡 If comm fails but Python works: set ehl.serial.parity=NONE or enable parity auto-detect")
            }
            logger.info("════════════════════════════════════════════════════════════")
            logger.info("")
            
            val parityMode = when (parity.uppercase()) {
                "NONE" -> SerialPort.NO_PARITY
                "ODD" -> SerialPort.ODD_PARITY
                "EVEN" -> SerialPort.EVEN_PARITY
                "MARK" -> SerialPort.MARK_PARITY
                "SPACE" -> SerialPort.SPACE_PARITY
                else -> {
                    logger.warn("Unknown parity '$parity', defaulting to NONE")
                    SerialPort.NO_PARITY
                }
            }
            
            SerialPortConfig(
                portName = portName,
                baudRate = baudRate,
                dataBits = dataBits,
                stopBits = stopBitsMode,
                parity = parityMode,
                rs485Enabled = rs485Enabled,
                rs485RtsHighDuringSend = rs485RtsHighDuringSend,
                rs485RtsHighAfterSend = rs485RtsHighAfterSend,
                rs485RxDuringTx = rs485RxDuringTx,
                rs485DelayRtsBeforeSendMs = rs485RtsBeforeMs,
                rs485DelayRtsAfterSendMs = rs485RtsAfterMs,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout
            )
        }

        // Apply RS-485 driver direction settings also for the auto-detect path (if enabled).
        val config = baseConfig.copy(
            rs485Enabled = rs485Enabled,
            rs485RtsHighDuringSend = rs485RtsHighDuringSend,
            rs485RtsHighAfterSend = rs485RtsHighAfterSend,
            rs485RxDuringTx = rs485RxDuringTx,
            rs485DelayRtsBeforeSendMs = rs485RtsBeforeMs,
            rs485DelayRtsAfterSendMs = rs485RtsAfterMs
        )
        
        // Keep transport simple: connect/disconnect + explicit manual reconnect when needed.
        // Avoid enabling scheduled watchdog loops that can mask protocol-level issues.
        SerialPortManager(config)
    }
    
    /**
     * Expose SerialPortManager as HardwareWatchdogCapable for FIELD mode.
     */
    @Bean
    @Profile("field")
    fun fieldModeWatchdog(
        transport: SerialTransport
    ): HardwareWatchdogCapable? {
        return transport as? HardwareWatchdogCapable
    }
    
    /**
     * EHL Communicator - Uses whichever transport is configured.
     * 
     * Retry Configuration (via ehl.retry.*):
     *   ehl.retry.max-retries      - Maximum retry attempts (default: 3)
     *   ehl.retry.initial-delay-ms - Initial delay before first retry (default: 100ms)
     *   ehl.retry.max-delay-ms     - Maximum delay between retries (default: 2000ms)
     *   ehl.retry.backoff-multiplier - Exponential backoff multiplier (default: 2.0)
     */
    @Bean
    fun ehlCommunicator(
        transport: SerialTransport,
        @Value("\${lpg.protocol.logging.enabled:true}") rawLoggingEnabled: Boolean,
        @Value("\${ehl.retry.max-retries:3}") maxRetries: Int,
        @Value("\${ehl.retry.initial-delay-ms:100}") initialDelayMs: Long,
        @Value("\${ehl.retry.max-delay-ms:2000}") maxDelayMs: Long,
        @Value("\${ehl.retry.backoff-multiplier:2.0}") backoffMultiplier: Double
    ): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport.javaClass.simpleName}")
        logger.info("Raw protocol logging: ${if (rawLoggingEnabled) "ENABLED" else "DISABLED"}")
        
        val retryConfig = RetryConfig(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            backoffMultiplier = backoffMultiplier
        )
        
        logger.info("🔄 Retry config: maxRetries=$maxRetries, initialDelay=${initialDelayMs}ms, " +
                "maxDelay=${maxDelayMs}ms, backoff=$backoffMultiplier")
        
        val communicator = EhlCommunicator(
            transport = transport,
            enableRawLogging = rawLoggingEnabled,
            retryConfig = retryConfig
        )
        
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
