package no.cloudberries.lpg.api.controller

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.service.service.*
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

/**
 * Debug controller for serial port diagnostics.
 * 
 * Provides endpoints for:
 * - Health checking serial communication
 * - Manual parity detection
 * - Serial port diagnostics
 */
@RestController
@RequestMapping("/api/debug/serial")
class SerialDebugController(
    private val serialConfigService: SerialConfigurationService,
    private val parityDetector: SerialParityAutoDetector,
    private val portScanner: SerialPortScanner,
    private val communicator: EhlCommunicator,
    private val transport: SerialTransport
) {
    
    private val logger = LoggerFactory.getLogger(SerialDebugController::class.java)
    
    /**
     * Health check endpoint for serial communication.
     * 
     * Sends a state query to the dispenser and measures response time.
     * 
     * @param address Dispenser address to query (default: 1)
     * @return SerialHealthStatus with test results
     */
    @GetMapping("/health")
    suspend fun health(
        @RequestParam(defaultValue = "1") address: Int
    ): SerialHealthStatus {
        logger.info("Serial health check requested for address $address")
        
        return serialConfigService.healthCheck(
            transport = transport,
            communicator = communicator,
            address = address,
            timeoutMs = 2000
        )
    }
    
    /**
     * Auto-detect parity mode for a given serial port.
     * 
     * WARNING: This endpoint attempts to open the serial port directly,
     * which may conflict with existing connections.
     * 
     * @param port Serial port device name (e.g., "/dev/ttyS0")
     * @param address Dispenser address to test (default: 1)
     * @return ParityDetectionResult with detected parity mode
     */
    @PostMapping("/auto-detect")
    suspend fun autoDetectParity(
        @RequestParam port: String,
        @RequestParam(defaultValue = "1") address: Int
    ): ParityDetectionResult {
        logger.info("Parity auto-detect requested for port $port, address $address")
        
        return try {
            val detectedParity = parityDetector.autoDetectParity(
                portName = port,
                dispenserAddress = address
            )
            
            val (mode, description) = when (detectedParity) {
                com.fazecast.jSerialComm.SerialPort.NO_PARITY -> "NONE" to "8N1 - No parity (simulator/Python)"
                com.fazecast.jSerialComm.SerialPort.EVEN_PARITY -> "EVEN" to "8E1 - Even parity (standard EHL)"
                com.fazecast.jSerialComm.SerialPort.ODD_PARITY -> "ODD" to "8O1 - Odd parity (rare)"
                else -> "UNKNOWN" to "Unknown parity mode"
            }
            
            ParityDetectionResult(
                detected = true,
                parityMode = mode,
                description = description,
                error = null
            )
        } catch (e: Exception) {
            logger.error("Parity auto-detect failed: ${e.message}", e)
            ParityDetectionResult(
                detected = false,
                parityMode = null,
                description = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Get current serial port connection status.
     * 
     * @return Map with connection details
     */
    @GetMapping("/status")
    fun status(): Map<String, Any> {
        return mapOf(
            "connected" to transport.isConnected,
            "transportType" to transport.javaClass.simpleName
        )
    }
    
    /**
     * List all available serial ports on the system.
     * 
     * @return List of available ports with details
     */
    @GetMapping("/ports")
    fun listPorts(): List<AvailablePort> {
        logger.info("Listing available serial ports")
        return portScanner.listAvailablePorts()
    }
    
    /**
     * Smart scan: Automatically find working serial port configuration.
     * 
     * WARNING: This will test all available ports and configurations.
     * May take 10-30 seconds depending on number of ports.
     * 
     * @param timeoutMs Timeout per test in milliseconds (default: 1000)
     * @param stopOnFirst If true, returns immediately when first working config is found (default: true)
     * @return List of working configurations sorted by confidence
     */
    @PostMapping("/smart-scan")
    suspend fun smartScan(
        @RequestParam(defaultValue = "1000") timeoutMs: Long,
        @RequestParam(defaultValue = "true") stopOnFirst: Boolean
    ): List<WorkingConfiguration> {
        logger.info("Starting smart serial port scan (timeout=${timeoutMs}ms, stopOnFirst=$stopOnFirst)")
        return portScanner.smartScan(
            testTimeoutMs = timeoutMs,
            stopOnFirstMatch = stopOnFirst
        )
    }
    
    /**
     * Scan for responding addresses on a specific port.
     * 
     * Like Python script 02_scan_addresses.py - scans a range of addresses
     * to find which ones respond.
     * 
     * Examples:
     *   curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=40"
     *   curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/tmp/vserial1&start=32&end=40"
     * 
     * @param port Serial port device path
     * @param start Start of address range (default: 1)
     * @param end End of address range (default: 64)
     * @param baud Baud rate (default: 9600)
     * @param parity Parity mode: NONE, EVEN, ODD (default: NONE)
     * @param timeoutMs Timeout per address (default: 500ms)
     * @return AddressScanResult with list of responding addresses
     */
    @PostMapping("/scan-addresses")
    suspend fun scanAddresses(
        @RequestParam port: String,
        @RequestParam(defaultValue = "1") start: Int,
        @RequestParam(defaultValue = "64") end: Int,
        @RequestParam(defaultValue = "9600") baud: Int,
        @RequestParam(defaultValue = "NONE") parity: String,
        @RequestParam(defaultValue = "500") timeoutMs: Long
    ): AddressScanResult {
        logger.info("Starting address scan: port=$port, range=$start-$end, baud=$baud, parity=$parity")
        return portScanner.scanAddresses(
            portPath = port,
            addressRange = start..end,
            baudRate = baud,
            parity = parity,
            testTimeoutMs = timeoutMs
        )
    }
}
