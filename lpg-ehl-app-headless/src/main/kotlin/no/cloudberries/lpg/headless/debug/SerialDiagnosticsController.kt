package no.cloudberries.lpg.headless.debug

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.service.service.*
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Serial diagnostics controller for headless application.
 * 
 * Same functionality as SerialDebugController in webapp, adapted for headless use.
 * Allows field technicians to diagnose serial port issues via curl when running
 * with the debug-api profile.
 * 
 * Based on Python test scripts by Alejandro:
 * - 00_list_ports.py -> GET /api/debug/serial/ports
 * - 01_probe_readonly.py -> GET /api/debug/serial/health
 * - 02_scan_addresses.py -> POST /api/debug/serial/scan-addresses
 * 
 * Eksempler:
 * ```bash
 * # List available serial ports
 * curl http://localhost:8080/api/debug/serial/ports
 * 
 * # Health check on current serial connection
 * curl "http://localhost:8080/api/debug/serial/health?address=1"
 * 
 * # Smart scan - find working configuration automatically
 * curl -X POST "http://localhost:8080/api/debug/serial/smart-scan?timeoutMs=1000"
 * 
 * # Scan addresses (like Alejandro's 02_scan_addresses.py)
 * curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=40"
 * curl -X POST "http://localhost:8080/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=32&end=40"
 * 
 * # Auto-detect parity mode
 * curl -X POST "http://localhost:8080/api/debug/serial/auto-detect?port=/dev/ttyUSB0&address=33"
 * ```
 */
@RestController
@RequestMapping("/api/debug/serial")
class SerialDiagnosticsController(
    private val serialConfigService: SerialConfigurationService,
    private val parityDetector: SerialParityAutoDetector,
    private val portScanner: SerialPortScanner,
    private val communicator: EhlCommunicator,
    private val transport: SerialTransport
) {
    
    private val logger = LoggerFactory.getLogger(SerialDiagnosticsController::class.java)
    
    init {
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger.info("🔧 SERIAL DIAGNOSTICS API AKTIVERT")
        logger.info("   Endepunkter tilgjengelig på /api/debug/serial/*")
        logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * List all available serial ports on the system.
     * 
     * Like Python script 00_list_ports.py
     * 
     * @return List of available ports with details
     */
    @GetMapping("/ports")
    fun listPorts(): List<AvailablePort> {
        logger.info("Listing available serial ports")
        return portScanner.listAvailablePorts()
    }
    
    /**
     * Health check endpoint for serial communication.
     * 
     * Like Python script 01_probe_readonly.py
     * 
     * @param address Dispenser address to query (default: 1)
     * @return SerialHealthStatus with test results
     */
    @GetMapping("/health")
    fun health(
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<SerialHealthStatus> {
        logger.info("Serial health check requested for address $address")
        
        return try {
            val status = runBlocking {
                serialConfigService.healthCheck(
                    transport = transport,
                    communicator = communicator,
                    address = address,
                    timeoutMs = 2000
                )
            }
            ResponseEntity.ok(status)
        } catch (e: Exception) {
            logger.error("Health check failed: ${e.message}")
            ResponseEntity.ok(SerialHealthStatus(
                connected = false,
                testPassed = false,
                responseTimeMs = 0,
                error = e.message ?: "Unknown error"
            ))
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
     * Smart scan: Automatically find working serial port configuration.
     * 
     * Tests all available ports with common configurations.
     * By default stops on first match (production mode).
     * 
     * @param timeoutMs Timeout per test in milliseconds (default: 1000)
     * @param stopOnFirst If true, returns immediately when first working config is found (default: true)
     * @return List of working configurations sorted by confidence
     */
    @PostMapping("/smart-scan")
    fun smartScan(
        @RequestParam(defaultValue = "1000") timeoutMs: Long,
        @RequestParam(defaultValue = "true") stopOnFirst: Boolean
    ): ResponseEntity<List<WorkingConfiguration>> {
        logger.info("Starting smart serial port scan (timeout=${timeoutMs}ms, stopOnFirst=$stopOnFirst)")
        
        return try {
            val configs = runBlocking {
                portScanner.smartScan(
                    testTimeoutMs = timeoutMs,
                    stopOnFirstMatch = stopOnFirst
                )
            }
            ResponseEntity.ok(configs)
        } catch (e: Exception) {
            logger.error("Smart scan failed: ${e.message}")
            ResponseEntity.ok(emptyList())
        }
    }
    
    /**
     * Scan for responding addresses on a specific port.
     * 
     * Like Python script 02_scan_addresses.py - scans a range of addresses
     * to find which ones respond.
     * 
     * Based on Alejandro's findings:
     * - Real pumps may respond on address 32 + pump_number
     * - E.g., pump 1 = address 33, pump 2 = address 34
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
    fun scanAddresses(
        @RequestParam port: String,
        @RequestParam(defaultValue = "1") start: Int,
        @RequestParam(defaultValue = "64") end: Int,
        @RequestParam(defaultValue = "9600") baud: Int,
        @RequestParam(defaultValue = "NONE") parity: String,
        @RequestParam(defaultValue = "500") timeoutMs: Long
    ): ResponseEntity<AddressScanResult> {
        logger.info("Starting address scan: port=$port, range=$start-$end, baud=$baud, parity=$parity")
        
        return try {
            val result = runBlocking {
                portScanner.scanAddresses(
                    portPath = port,
                    addressRange = start..end,
                    baudRate = baud,
                    parity = parity,
                    testTimeoutMs = timeoutMs
                )
            }
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            logger.error("Address scan failed: ${e.message}")
            ResponseEntity.ok(AddressScanResult(
                portPath = port,
                addressRange = "$start-$end",
                baudRate = baud,
                parity = parity,
                respondingAddresses = emptyList(),
                testedCount = 0
            ))
        }
    }
    
    /**
     * Auto-detect parity mode for a given serial port.
     * 
     * WARNING: This endpoint attempts to open the serial port directly,
     * which may conflict with existing connections.
     * 
     * @param port Serial port device name (e.g., "/dev/ttyUSB0")
     * @param address Dispenser address to test (default: 1)
     * @return ParityDetectionResult with detected parity mode
     */
    @PostMapping("/auto-detect")
    fun autoDetectParity(
        @RequestParam port: String,
        @RequestParam(defaultValue = "1") address: Int
    ): ResponseEntity<ParityDetectionResult> {
        logger.info("Parity auto-detect requested for port $port, address $address")
        
        return try {
            val detectedParity = runBlocking {
                parityDetector.autoDetectParity(
                    portName = port,
                    dispenserAddress = address
                )
            }
            
            val (mode, description) = when (detectedParity) {
                com.fazecast.jSerialComm.SerialPort.NO_PARITY -> "NONE" to "8N1 - No parity (simulator/Python)"
                com.fazecast.jSerialComm.SerialPort.EVEN_PARITY -> "EVEN" to "8E1 - Even parity (standard EHL)"
                com.fazecast.jSerialComm.SerialPort.ODD_PARITY -> "ODD" to "8O1 - Odd parity (rare)"
                else -> "UNKNOWN" to "Unknown parity mode"
            }
            
            ResponseEntity.ok(ParityDetectionResult(
                detected = true,
                parityMode = mode,
                description = description,
                error = null
            ))
        } catch (e: Exception) {
            logger.error("Parity auto-detect failed: ${e.message}", e)
            ResponseEntity.ok(ParityDetectionResult(
                detected = false,
                parityMode = null,
                description = null,
                error = e.message ?: "Unknown error"
            ))
        }
    }
}
