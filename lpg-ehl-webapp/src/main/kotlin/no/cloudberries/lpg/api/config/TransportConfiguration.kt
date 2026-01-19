package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import no.cloudberries.lpg.pls.RealSerialTransport
import no.cloudberries.lpg.service.operations.EhlOperationsService
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Transport Configuration - Dual-Mode Architecture.
 * 
 * Automatically selects between LAB MODE and FIELD MODE based on
 * `ehl.emulator.enabled` property.
 * 
 * LAB MODE (ehl.emulator.enabled=true):
 *   - Uses InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - Perfect for development and testing
 * 
 * FIELD MODE (ehl.emulator.enabled=false):
 *   - Uses RealSerialTransport + physical RS-485 serial port
 *   - Communicates with real LPG dispenser hardware
 *   - Requires /dev/ttyS0 or similar serial device
 */
@Configuration
class TransportConfiguration {
    
    private val logger = LoggerFactory.getLogger(TransportConfiguration::class.java)
    
    /**
     * LAB MODE: In-memory serial port with emulator.
     * Default configuration - safe for development.
     */
    @Bean
    @ConditionalOnProperty(
        name = ["ehl.emulator.enabled"], 
        havingValue = "true", 
        matchIfMissing = true  // Default to LAB mode
    )
    fun labModeTransport(
        emulator: EhlDispenserEmulator,
        @Value("\${ehl.emulator.latency-ms:20}") latencyMs: Long
    ): SerialTransport {
        logger.info("════════════════════════════════════════════════════════")
        logger.info("🔬 LAB MODE ACTIVATED")
        logger.info("════════════════════════════════════════════════════════")
        logger.info("Transport: InMemorySerialPort")
        logger.info("Backend: EhlDispenserEmulator")
        logger.info("Latency: ${latencyMs}ms (simulated)")
        logger.info("Hardware: NOT REQUIRED")
        logger.info("════════════════════════════════════════════════════════")
        
        return InMemorySerialPort(emulator, latencyMs)
    }
    
    /**
     * FIELD MODE: Real serial port for production hardware.
     * Only activated when ehl.emulator.enabled=false.
     */
    @Bean
    @ConditionalOnProperty(
        name = ["ehl.emulator.enabled"], 
        havingValue = "false"
    )
    fun fieldModeTransport(
        @Value("\${ehl.serial.port}") portName: String,
        @Value("\${ehl.serial.baud-rate}") baudRate: Int
    ): SerialTransport {
        logger.info("════════════════════════════════════════════════════════")
        logger.info("🏭 FIELD MODE ACTIVATED")
        logger.info("════════════════════════════════════════════════════════")
        logger.info("Transport: RealSerialTransport")
        logger.info("Serial Port: $portName")
        logger.info("Baud Rate: $baudRate")
        logger.info("Protocol: EHL over RS-485")
        logger.info("⚠️  WARNING: This will communicate with REAL HARDWARE")
        logger.info("════════════════════════════════════════════════════════")
        
        return RealSerialTransport(portName, baudRate)
    }
    
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
