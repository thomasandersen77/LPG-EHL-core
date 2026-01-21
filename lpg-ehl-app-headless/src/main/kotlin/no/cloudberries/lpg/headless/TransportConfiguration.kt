package no.cloudberries.lpg.headless

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

/**
 * Transport Configuration for Headless Application - Dual-Mode Architecture.
 * 
 * Automatically selects between LAB MODE and FIELD MODE based on
 * `ehl.emulator.enabled` property.
 * 
 * LAB MODE (ehl.emulator.enabled=true):
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - For testing and development
 * 
 * FIELD MODE (ehl.emulator.enabled=false) - DEFAULT FOR HEADLESS:
 *   - Uses RealSerialTransport + physical RS-485 serial port
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
     * LAB MODE: In-memory serial port with emulator.
     * Used for development and testing when no physical hardware is available.
     * Activated when lpg.mode=LAB
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "lpg",
        name = ["mode"], 
        havingValue = "LAB"
    )
    fun labModeTransport(
        @Value("\${ehl.emulator.dispenser-address:1}") dispenserAddress: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("════════════════════════════════════════════════════════")
        logger.info("🔬 LAB MODE ACTIVATED (HEADLESS)")
        logger.info("════════════════════════════════════════════════════════")
        logger.info("Transport: InMemorySerialPort")
        logger.info("Backend: EhlDispenserEmulator")
        logger.info("Dispenser Address: $dispenserAddress")
        logger.info("Price: ${pricePerLiterCents / 100.0} kr/L")
        logger.info("Latency: ${latencyMs}ms (simulated)")
        logger.info("Hardware: NOT REQUIRED")
        logger.info("════════════════════════════════════════════════════════")
        
        val emulator = EhlDispenserEmulator(
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents
        )
        
        val transport = InMemorySerialPort(emulator, latencyMs)
        transport.connect()
        
        return transport
    }
    
    /**
     * FIELD MODE: Real serial port for production hardware.
     * DEFAULT MODE for headless deployment.
     * Activated when lpg.mode=FIELD (or not specified)
     * 
     * Supports full RS-485/RS-232 configuration:
     * - COM port / device path
     * - Baud rate
     * - Data bits (5, 6, 7, 8)
     * - Parity (NONE, ODD, EVEN, MARK, SPACE)
     * - Stop bits (1, 1.5, 2)
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "lpg",
        name = ["mode"], 
        havingValue = "FIELD",
        matchIfMissing = true  // Default to FIELD mode for headless
    )
    fun fieldModeTransport(
        @Value("\${ehl.serial.port:/dev/ttyS0}") portName: String,
        @Value("\${ehl.serial.baud-rate:9600}") baudRate: Int,
        @Value("\${ehl.serial.data-bits:8}") dataBits: Int,
        @Value("\${ehl.serial.parity:EVEN}") parity: String,
        @Value("\${ehl.serial.stop-bits:1}") stopBits: Int
    ): SerialTransport {
        logger.info("════════════════════════════════════════════════════════")
        logger.info("🏭 FIELD MODE ACTIVATED (HEADLESS)")
        logger.info("════════════════════════════════════════════════════════")
        logger.info("Transport: RealSerialTransport")
        logger.info("Serial Port: $portName")
        logger.info("Baud Rate: $baudRate")
        logger.info("Data Bits: $dataBits")
        logger.info("Parity: $parity")
        logger.info("Stop Bits: $stopBits")
        logger.info("Protocol: EHL over RS-485")
        logger.info("════════════════════════════════════════════════════════")
        logger.info("⚠️  WARNING: This will communicate with REAL HARDWARE")
        logger.info("════════════════════════════════════════════════════════")
        
        // Map parity string to jSerialComm constant
        val parityMode = when (parity.uppercase()) {
            "NONE" -> 0
            "ODD" -> 1
            "EVEN" -> 2
            "MARK" -> 3
            "SPACE" -> 4
            else -> {
                logger.warn("Unknown parity '$parity', defaulting to EVEN")
                2 // EVEN
            }
        }
        
        val transport = RealSerialTransport(portName, baudRate, dataBits, parityMode, stopBits)
        
        // Connect to the serial port
        if (!transport.connect()) {
            logger.error("❌ Failed to connect to serial port $portName")
            logger.warn("⚠️  Hardware communication will fail until port is available")
        }
        
        return transport
    }
    
    /**
     * EHL Communicator - Uses whichever transport is configured.
     * Transport is already connected by the mode-specific bean.
     */
    @Bean
    fun ehlCommunicator(transport: SerialTransport): EhlCommunicator {
        logger.info("Creating EhlCommunicator with ${transport.javaClass.simpleName}")
        return EhlCommunicator(transport)
    }
}
