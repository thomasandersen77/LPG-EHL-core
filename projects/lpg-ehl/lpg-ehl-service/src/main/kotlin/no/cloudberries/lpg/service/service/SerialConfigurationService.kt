package no.cloudberries.lpg.service.service

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.withTimeout
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.protocol.EhlPacketBuilder
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for serial port configuration and health checking.
 * 
 * Provides high-level operations for:
 * - Auto-detecting parity mode
 * - Validating serial port configuration
 * - Health checking serial communication
 */
@Service
class SerialConfigurationService(
    private val parityDetector: SerialParityAutoDetector
) {
    
    private val logger = LoggerFactory.getLogger(SerialConfigurationService::class.java)
    
    /**
     * Detect and configure serial port settings.
     * 
     * If autoDetect is true, attempts to auto-detect parity mode.
     * Otherwise, uses the provided manual parity setting.
     * 
     * @param portName Serial port device name
     * @param baudRate Baud rate (default: 9600)
     * @param dataBits Data bits (default: 8)
     * @param stopBits Stop bits mode (default: ONE_STOP_BIT)
     * @param autoDetect Whether to auto-detect parity (default: false)
     * @param manualParity Manual parity setting if not auto-detecting (NONE, EVEN, ODD, MARK, SPACE)
     * @param dispenserAddress Dispenser address for auto-detect (default: 1)
     * @param autoDetectTimeoutMs Timeout for auto-detect per attempt (default: 2000ms)
     * @return Configured SerialPortConfig
     */
    suspend fun detectAndConfigureSerial(
        portName: String,
        baudRate: Int = 9600,
        dataBits: Int = 8,
        stopBits: Int = SerialPort.ONE_STOP_BIT,
        autoDetect: Boolean = false,
        manualParity: String? = null,
        dispenserAddress: Int = 1,
        autoDetectTimeoutMs: Long = 2000
    ): SerialPortConfig {
        
        val parityMode = if (autoDetect) {
            logger.info("Auto-detecting parity mode...")
            parityDetector.autoDetectParity(
                portName = portName,
                dispenserAddress = dispenserAddress,
                baudRate = baudRate,
                dataBits = dataBits,
                stopBits = stopBits,
                testTimeoutMs = autoDetectTimeoutMs
            )
        } else {
            logger.info("Using manual parity configuration: $manualParity")
            parseParityMode(manualParity ?: "NONE")
        }
        
        return SerialPortConfig(
            portName = portName,
            baudRate = baudRate,
            dataBits = dataBits,
            stopBits = stopBits,
            parity = parityMode,
            readTimeout = 3000,
            writeTimeout = 1000
        )
    }
    
    /**
     * Parse parity mode from string.
     * 
     * @param parity String representation (NONE, EVEN, ODD, MARK, SPACE)
     * @return Parity mode constant from SerialPort
     */
    private fun parseParityMode(parity: String): Int {
        return when (parity.uppercase()) {
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
    }
    
    /**
     * Perform health check on serial communication.
     * 
     * Sends a state query to the dispenser and measures response time.
     * 
     * @param transport SerialTransport to test
     * @param communicator EhlCommunicator to use
     * @param address Dispenser address to query (default: 1)
     * @param timeoutMs Timeout for health check (default: 2000ms)
     * @return SerialHealthStatus with test results
     */
    suspend fun healthCheck(
        transport: SerialTransport,
        communicator: EhlCommunicator,
        address: Int = 1,
        timeoutMs: Long = 2000
    ): SerialHealthStatus {
        val startTime = System.currentTimeMillis()
        
        return try {
            if (!transport.isConnected) {
                return SerialHealthStatus(
                    connected = false,
                    testPassed = false,
                    responseTimeMs = 0,
                    error = "Transport not connected"
                )
            }
            
            val testPacket = EhlPacketBuilder.createStateQuery(address)
            
            withTimeout(timeoutMs) {
                try {
                    val response = communicator.sendAndReceive(testPacket, timeoutMs)
                    val responseTime = System.currentTimeMillis() - startTime
                    
                    SerialHealthStatus(
                        connected = true,
                        testPassed = true,
                        responseTimeMs = responseTime,
                        error = null,
                        responseCommand = response.command.name
                    )
                } catch (e: Exception) {
                    val responseTime = System.currentTimeMillis() - startTime
                    SerialHealthStatus(
                        connected = true,
                        testPassed = false,
                        responseTimeMs = responseTime,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            SerialHealthStatus(
                connected = transport.isConnected,
                testPassed = false,
                responseTimeMs = responseTime,
                error = e.message ?: "Unknown error"
            )
        }
    }
}

/**
 * Result of serial port health check.
 */
data class SerialHealthStatus(
    val connected: Boolean,
    val testPassed: Boolean,
    val responseTimeMs: Long,
    val error: String?,
    val responseCommand: String? = null
)

/**
 * Result of parity auto-detection.
 */
data class ParityDetectionResult(
    val detected: Boolean,
    val parityMode: String?,
    val description: String?,
    val error: String?
)
