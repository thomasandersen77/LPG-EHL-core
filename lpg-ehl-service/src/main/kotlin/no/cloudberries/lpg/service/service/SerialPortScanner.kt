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
    fun listAvailablePorts(includeSocatPorts: Boolean = true, checkAccess: Boolean = true): List<AvailablePort> {
        // Get hardware serial ports from jSerialComm (use systemPortPath for full path)
        val hardwarePorts = SerialPort.getCommPorts().map { port ->
            val portPath = port.systemPortPath ?: port.systemPortName
            val access = if (checkAccess) checkPortAccess(portPath) else "UNKNOWN"
            AvailablePort(
                path = portPath,
                description = port.portDescription ?: "Unknown",
                location = port.portLocation ?: "Unknown",
                vendorId = port.vendorID,
                productId = port.productID,
                accessStatus = access
            )
        }
        
        // Check for socat virtual ports (if they exist)
        val virtualPorts = if (includeSocatPorts) {
            SOCAT_VIRTUAL_PORTS
                .filter { java.io.File(it).exists() }
                .map { path ->
                    val access = if (checkAccess) checkPortAccess(path) else "UNKNOWN"
                    AvailablePort(
                        path = path,
                        description = "Socat Virtual PTY",
                        location = "Virtual",
                        vendorId = 0,
                        productId = 0,
                        accessStatus = access
                    )
                }
        } else {
            emptyList()
        }
        
        // On macOS, scan /dev/cu.* for additional ports not enumerated by jSerialComm
        val macOsPorts = if (includeSocatPorts) {
            scanMacOsSerialPorts(hardwarePorts.map { it.path }.toSet() + virtualPorts.map { it.path }.toSet(), checkAccess)
        } else {
            emptyList()
        }
        
        val allPorts = hardwarePorts + virtualPorts + macOsPorts
        
        logger.info("Found ${allPorts.size} serial ports (${hardwarePorts.size} hardware, ${virtualPorts.size} virtual, ${macOsPorts.size} macOS-detected)")
        allPorts.forEach { port ->
            logger.debug("  - ${port.path}: ${port.description} [${port.accessStatus}]")
        }
        
        return allPorts
    }
    
    /**
     * Check if a serial port can be opened (lightweight access test).
     * @return Access status string: OK, PERMISSION_DENIED, BUSY, NOT_FOUND, UNKNOWN
     */
    private fun checkPortAccess(portPath: String): String {
        return try {
            val file = java.io.File(portPath)
            if (!file.exists()) return "NOT_FOUND"
            if (!file.canRead() || !file.canWrite()) return "PERMISSION_DENIED"
            
            // Try a quick open/close via jSerialComm
            val testPort = SerialPort.getCommPort(portPath)
            testPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 100)
            val opened = testPort.openPort(100)  // 100ms timeout
            if (opened) {
                testPort.closePort()
                "OK"
            } else {
                // Check error details
                when (testPort.lastErrorCode) {
                    0 -> "BUSY"  // Port exists but couldn't be opened (likely in use)
                    else -> "BUSY"
                }
            }
        } catch (e: Exception) {
            logger.debug("Access check failed for $portPath: ${e.message}")
            when {
                e.message?.contains("Permission", ignoreCase = true) == true -> "PERMISSION_DENIED"
                e.message?.contains("busy", ignoreCase = true) == true -> "BUSY"
                else -> "UNKNOWN"
            }
        }
    }
    
    /**
     * Scan macOS /dev/cu.* ports that jSerialComm may not enumerate.
     */
    private fun scanMacOsSerialPorts(alreadyKnown: Set<String>, checkAccess: Boolean): List<AvailablePort> {
        val devDir = java.io.File("/dev")
        if (!devDir.isDirectory) return emptyList()
        
        return devDir.listFiles { _, name ->
            name.startsWith("cu.") && !name.startsWith("cu.Bluetooth") && !name.startsWith("cu.debug")
        }?.mapNotNull { file ->
            val path = file.absolutePath
            if (alreadyKnown.contains(path)) return@mapNotNull null
            val access = if (checkAccess) checkPortAccess(path) else "UNKNOWN"
            AvailablePort(
                path = path,
                description = "macOS serial port",
                location = "System",
                vendorId = 0,
                productId = 0,
                accessStatus = access
            )
        } ?: emptyList()
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
     * Probe a specific serial port with given UART settings.
     * Opens a temporary connection, sends a STATE query, and returns detailed result.
     *
     * @return ProbeResult with detailed diagnostics
     */
    suspend fun probePort(
        portPath: String,
        baudRate: Int = 9600,
        parity: String = "NONE",
        dataBits: Int = 8,
        stopBitsValue: Int = 1,
        address: Int = 1,
        timeoutMs: Long = 2000
    ): ProbeResult {
        val startTime = System.currentTimeMillis()
        val parityValue = when (parity.uppercase()) {
            "NONE" -> SerialPort.NO_PARITY
            "EVEN" -> SerialPort.EVEN_PARITY
            "ODD" -> SerialPort.ODD_PARITY
            else -> SerialPort.NO_PARITY
        }
        val stopBitsMode = when (stopBitsValue) {
            2 -> SerialPort.TWO_STOP_BITS
            else -> SerialPort.ONE_STOP_BIT
        }
        val usedConfig = ProbeConfig(portPath, baudRate, parity.uppercase(), dataBits, stopBitsValue, address)

        var manager: SerialPortManager? = null
        return try {
            // Check file exists first
            if (!java.io.File(portPath).exists()) {
                return ProbeResult(false, false, 0, "NOT_FOUND", "Port $portPath does not exist", usedConfig = usedConfig)
            }

            val config = SerialPortConfig(
                portName = portPath,
                baudRate = baudRate,
                dataBits = dataBits,
                stopBits = stopBitsMode,
                parity = parityValue,
                readTimeout = timeoutMs.toInt(),
                writeTimeout = timeoutMs.toInt()
            )
            manager = SerialPortManager(config)
            
            if (!manager.connect()) {
                val elapsed = System.currentTimeMillis() - startTime
                return ProbeResult(false, false, elapsed, "BUSY", "Failed to open port $portPath (in use or permission denied)", usedConfig = usedConfig)
            }

            val communicator = EhlCommunicator(manager, enableRawLogging = false)
            val testPacket = EhlPacketBuilder.createStateQuery(address)

            val response = withTimeout(timeoutMs) {
                communicator.sendAndReceive(testPacket, timeoutMs)
            }
            val elapsed = System.currentTimeMillis() - startTime
            ProbeResult(
                opened = true,
                testPassed = true,
                responseTimeMs = elapsed,
                errorCategory = null,
                errorMessage = null,
                responseCommand = response.command.name,
                usedConfig = usedConfig
            )
        } catch (e: java.io.IOException) {
            val elapsed = System.currentTimeMillis() - startTime
            val category = when {
                e.message?.contains("Permission", ignoreCase = true) == true -> "PERMISSION"
                e.message?.contains("busy", ignoreCase = true) == true -> "BUSY"
                else -> "UNKNOWN"
            }
            ProbeResult(false, false, elapsed, category, e.message, usedConfig = usedConfig)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            val elapsed = System.currentTimeMillis() - startTime
            ProbeResult(true, false, elapsed, "NO_RESPONSE", "No response within ${timeoutMs}ms from address $address", usedConfig = usedConfig)
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            ProbeResult(false, false, elapsed, "UNKNOWN", e.message, usedConfig = usedConfig)
        } finally {
            try { manager?.disconnect() } catch (_: Exception) {}
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
    val productId: Int,
    val accessStatus: String = "UNKNOWN"  // OK, PERMISSION_DENIED, BUSY, NOT_FOUND, UNKNOWN
)

/**
 * Result of probing a specific serial port configuration.
 */
data class ProbeResult(
    val opened: Boolean,
    val testPassed: Boolean,
    val responseTimeMs: Long,
    val errorCategory: String?,  // BUSY, PERMISSION, NOT_FOUND, NO_RESPONSE, UNKNOWN
    val errorMessage: String?,
    val responseCommand: String? = null,
    val usedConfig: ProbeConfig? = null
)

data class ProbeConfig(
    val port: String,
    val baudRate: Int,
    val parity: String,
    val dataBits: Int,
    val stopBits: Int,
    val address: Int
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
