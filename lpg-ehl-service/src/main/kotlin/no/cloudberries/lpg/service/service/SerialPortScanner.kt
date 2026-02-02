package no.cloudberries.lpg.service.service

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.withTimeout
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.protocol.EhlPacketBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Smart serial port scanner and diagnostics service.
 * 
 * Capabilities:
 * - List all available serial ports on the system
 * - Test communication with each port
 * - Auto-detect working configuration (port + parity + baud)
 * - Scan address ranges (standard 1-8 AND legacy 32+ format)
 * - Recommend best configuration for production
 * 
 * Based on field testing by Alejandro:
 * - Real pumps respond on address 32 + pump_number (e.g., pump 1 = address 33)
 * - STATE, ERROR_QUERY, VOLUME, TANKBIT commands work
 * - UNBLOCK/BLOCK may require specific command sequence
 * 
 * Perfect for:
 * - Field deployment (finding the right port and address)
 * - Development (testing without knowing exact hardware)
 * - Debugging (why isn't communication working?)
 */
@Service
class SerialPortScanner {
    
    private val logger = LoggerFactory.getLogger(SerialPortScanner::class.java)
    
    companion object {
        // Standard address range (most simulators)
        val STANDARD_ADDRESSES = listOf(1, 2, 3, 4)
        
        // Legacy address format: base_address + pump_number (Alejandro's finding)
        // Real hardware often uses 32 + pump_number
        val LEGACY_ADDRESSES = listOf(33, 34, 35, 36)  // 32+1, 32+2, etc.
        
        // Combined: Try standard first, then legacy
        val ALL_ADDRESSES = STANDARD_ADDRESSES + LEGACY_ADDRESSES
        
        // Virtual serial port paths commonly used with socat
        val SOCAT_VIRTUAL_PORTS = listOf(
            "/tmp/vserial0",
            "/tmp/vserial1",
            "/tmp/ttyV0",
            "/tmp/ttyV1"
        )
    }
    
    /**
     * List all available serial ports on the system.
     * Also checks for common socat virtual PTY paths.
     * 
     * @param includeSocatPorts Whether to include socat virtual ports (default: true)
     * @return List of AvailablePort with details
     */
    fun listAvailablePorts(includeSocatPorts: Boolean = true): List<AvailablePort> {
        // Get hardware serial ports from jSerialComm
        val hardwarePorts = SerialPort.getCommPorts().map { port ->
            AvailablePort(
                path = port.systemPortName,
                description = port.portDescription ?: "Unknown",
                location = port.portLocation ?: "Unknown",
                vendorId = port.vendorID,
                productId = port.productID
            )
        }
        
        // Check for socat virtual ports (if they exist)
        val virtualPorts = if (includeSocatPorts) {
            SOCAT_VIRTUAL_PORTS
                .filter { java.io.File(it).exists() }
                .map { path ->
                    AvailablePort(
                        path = path,
                        description = "Socat Virtual PTY",
                        location = "Virtual",
                        vendorId = 0,
                        productId = 0
                    )
                }
        } else {
            emptyList()
        }
        
        val allPorts = hardwarePorts + virtualPorts
        
        logger.info("Found ${allPorts.size} serial ports (${hardwarePorts.size} hardware, ${virtualPorts.size} virtual)")
        allPorts.forEach { port ->
            logger.debug("  - ${port.path}: ${port.description}")
        }
        
        return allPorts
    }
    
    /**
     * Smart scan: Try to find a working serial port automatically.
     * 
     * Tests all available ports with common configurations:
     * - Baud rates: 9600, 19200, 115200
     * - Parity modes: NONE, EVEN, ODD
     * - Addresses: Standard (1-4) AND legacy format (33-36 = 32 + pump_number)
     * 
     * @param testTimeoutMs Timeout per test (default: 1000ms)
     * @param stopOnFirstMatch If true, returns immediately when first working config is found (default: true)
     * @param includeAddresses Custom address list to test (default: standard + legacy)
     * @return List of working configurations, sorted by likelihood
     */
    suspend fun smartScan(
        testTimeoutMs: Long = 1000,
        stopOnFirstMatch: Boolean = true,
        includeAddresses: List<Int> = ALL_ADDRESSES
    ): List<WorkingConfiguration> {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔍 SMART SERIAL PORT SCAN")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Timeout:   ${testTimeoutMs}ms per test")
        logger.info("  Stop mode: ${if (stopOnFirstMatch) "First match" else "Full scan"}")
        logger.info("  Addresses: ${includeAddresses.joinToString(", ")}")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        val availablePorts = listAvailablePorts()
        val workingConfigs = mutableListOf<WorkingConfiguration>()
        
        if (availablePorts.isEmpty()) {
            logger.warn("No serial ports found on system")
            return emptyList()
        }
        
        // Common baud rates (most common first)
        val baudRates = listOf(9600, 19200, 115200)
        
        // Parity modes (most common for EHL first)
        val parityModes = listOf(
            SerialPort.NO_PARITY to "NONE",
            SerialPort.EVEN_PARITY to "EVEN",
            SerialPort.ODD_PARITY to "ODD"
        )
        
        var testCount = 0
        
        // Test each port
        portLoop@ for (port in availablePorts) {
            logger.info("Testing port: ${port.path} (${port.description})")
            
            // Test each baud rate
            for (baud in baudRates) {
                // Test each parity
                for ((parityValue, parityName) in parityModes) {
                    // Test addresses
                    for (address in includeAddresses) {
                        testCount++
                        val config = SerialPortConfig(
                            portName = port.path,
                            baudRate = baud,
                            dataBits = 8,
                            stopBits = SerialPort.ONE_STOP_BIT,
                            parity = parityValue,
                            readTimeout = testTimeoutMs.toInt(),
                            writeTimeout = testTimeoutMs.toInt()
                        )
                        
                        logger.debug("  Test #$testCount: baud=$baud, parity=$parityName, address=$address")
                        
                        if (testConfiguration(config, address, testTimeoutMs)) {
                            val workingConfig = WorkingConfiguration(
                                port = port,
                                baudRate = baud,
                                parity = parityName,
                                dispenserAddress = address,
                                confidence = calculateConfidence(baud, parityName, address)
                            )
                            
                            logger.info("  ✅ FOUND: baud=$baud, parity=$parityName, address=$address")
                            workingConfigs.add(workingConfig)
                            
                            if (stopOnFirstMatch) {
                                logger.info("")
                                logger.info("════════════════════════════════════════════════════════════")
                                logger.info("  ✅ FOUND WORKING CONFIGURATION (stopping scan)")
                                logger.info("════════════════════════════════════════════════════════════")
                                logger.info("  Port:     ${port.path}")
                                logger.info("  Baud:     $baud")
                                logger.info("  Parity:   $parityName")
                                logger.info("  Address:  $address ${formatAddressDescription(address)}")
                                logger.info("  Tests:    $testCount")
                                logger.info("════════════════════════════════════════════════════════════")
                                logger.info("")
                                return listOf(workingConfig)
                            }
                        }
                    }
                }
            }
        }
        
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Found ${workingConfigs.size} working configuration(s)")
        logger.info("  Total tests: $testCount")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        return workingConfigs.sortedByDescending { it.confidence }
    }
    
    /**
     * Scan for responding addresses on a specific port.
     * 
     * Like Python script 02_scan_addresses.py - scans a range of addresses
     * to find which ones respond.
     * 
     * @param portPath Serial port device path
     * @param addressRange Range of addresses to test (default: 1..64)
     * @param baudRate Baud rate (default: 9600)
     * @param parity Parity mode string: NONE, EVEN, ODD (default: NONE)
     * @param testTimeoutMs Timeout per address (default: 500ms - faster for scanning)
     * @return List of responding addresses with response details
     */
    suspend fun scanAddresses(
        portPath: String,
        addressRange: IntRange = 1..64,
        baudRate: Int = 9600,
        parity: String = "NONE",
        testTimeoutMs: Long = 500
    ): AddressScanResult {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔍 ADDRESS SCAN (like 02_scan_addresses.py)")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Port:     $portPath")
        logger.info("  Range:    ${addressRange.first}..${addressRange.last}")
        logger.info("  Baud:     $baudRate")
        logger.info("  Parity:   $parity")
        logger.info("  Timeout:  ${testTimeoutMs}ms per address")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        val parityValue = when (parity.uppercase()) {
            "NONE" -> SerialPort.NO_PARITY
            "EVEN" -> SerialPort.EVEN_PARITY
            "ODD" -> SerialPort.ODD_PARITY
            else -> SerialPort.NO_PARITY
        }
        
        val respondingAddresses = mutableListOf<RespondingAddress>()
        var testedCount = 0
        
        for (address in addressRange) {
            testedCount++
            val config = SerialPortConfig(
                portName = portPath,
                baudRate = baudRate,
                dataBits = 8,
                stopBits = SerialPort.ONE_STOP_BIT,
                parity = parityValue,
                readTimeout = testTimeoutMs.toInt(),
                writeTimeout = testTimeoutMs.toInt()
            )
            
            if (testConfiguration(config, address, testTimeoutMs)) {
                val addrInfo = RespondingAddress(
                    address = address,
                    description = formatAddressDescription(address),
                    responseTimeMs = testTimeoutMs  // Approximation
                )
                respondingAddresses.add(addrInfo)
                logger.info("  ✅ Address $address responds ${addrInfo.description}")
            } else {
                logger.debug("  ❌ Address $address: no response")
            }
        }
        
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        if (respondingAddresses.isEmpty()) {
            logger.warn("  ⚠️  No responding addresses found in range")
        } else {
            logger.info("  Found ${respondingAddresses.size} responding address(es):")
            respondingAddresses.forEach { addr ->
                logger.info("    - Address ${addr.address} ${addr.description}")
            }
        }
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        return AddressScanResult(
            portPath = portPath,
            addressRange = "${addressRange.first}-${addressRange.last}",
            baudRate = baudRate,
            parity = parity,
            respondingAddresses = respondingAddresses,
            testedCount = testedCount
        )
    }
    
    /**
     * Format address with human-readable description.
     */
    private fun formatAddressDescription(address: Int): String {
        return when {
            address in 1..8 -> "(standard pump $address)"
            address in 33..40 -> "(legacy: 32 + pump ${address - 32})"
            address == 32 -> "(legacy base address)"
            else -> ""
        }
    }
    
    /**
     * Test a specific configuration.
     * 
     * @return true if communication successful, false otherwise
     */
    private suspend fun testConfiguration(
        config: SerialPortConfig,
        address: Int,
        timeoutMs: Long
    ): Boolean {
        var manager: SerialPortManager? = null
        
        return try {
            manager = SerialPortManager(config)
            if (!manager.connect()) {
                return false
            }
            
            val communicator = EhlCommunicator(manager, enableRawLogging = false)
            val testPacket = EhlPacketBuilder.createStateQuery(address)
            
            withTimeout(timeoutMs) {
                try {
                    communicator.sendAndReceive(testPacket, timeoutMs)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        } finally {
            try {
                manager?.disconnect()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Calculate confidence score for a configuration.
     * Higher score = more likely to be correct.
     */
    private fun calculateConfidence(baud: Int, parity: String, address: Int): Int {
        var confidence = 100
        
        // Standard EHL baud rate
        if (baud == 9600) confidence += 50
        
        // Common parity modes
        when (parity) {
            "EVEN" -> confidence += 30  // Standard EHL
            "NONE" -> confidence += 25  // Simulator/Python
            "ODD" -> confidence += 10   // Rare
        }
        
        // Address scoring based on Alejandro's field findings
        when {
            address == 1 -> confidence += 20  // Most common standard
            address == 33 -> confidence += 25  // Most common legacy (32+1)
            address in 2..4 -> confidence += 15  // Other standard
            address in 34..36 -> confidence += 20  // Other legacy
        }
        
        return confidence
    }
}

/**
 * Information about an available serial port.
 */
data class AvailablePort(
    val path: String,
    val description: String,
    val location: String,
    val vendorId: Int,
    val productId: Int
)

/**
 * A working serial port configuration.
 */
data class WorkingConfiguration(
    val port: AvailablePort,
    val baudRate: Int,
    val parity: String,
    val dispenserAddress: Int,
    val confidence: Int
) {
    fun toSummary(): String {
        return "Port: ${port.path}, Baud: $baudRate, Parity: $parity, Address: $dispenserAddress (confidence: $confidence%)"
    }
}

/**
 * Result of address scanning.
 */
data class AddressScanResult(
    val portPath: String,
    val addressRange: String,
    val baudRate: Int,
    val parity: String,
    val respondingAddresses: List<RespondingAddress>,
    val testedCount: Int
) {
    fun hasResponses(): Boolean = respondingAddresses.isNotEmpty()
}

/**
 * Information about a responding address.
 */
data class RespondingAddress(
    val address: Int,
    val description: String,
    val responseTimeMs: Long
)
