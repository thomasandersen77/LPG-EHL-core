package no.cloudberries.lpg.service.service

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.protocol.EhlPacketBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Auto-detects the correct parity mode for serial communication.
 * 
 * Tests communication with different parity modes (NONE, EVEN, ODD) and returns
 * the first mode that successfully communicates with the dispenser.
 * 
 * This is useful when deploying to hardware with unknown parity configuration,
 * as simulators typically use 8N1 (no parity) while real hardware may use 8E1 (even parity).
 */
@Service
class SerialParityAutoDetector {
    
    private val logger = LoggerFactory.getLogger(SerialParityAutoDetector::class.java)
    
    data class ParityMode(
        val value: Int,
        val name: String,
        val description: String
    )
    
    companion object {
        private val PARITY_MODES = listOf(
            ParityMode(
                SerialPort.NO_PARITY,
                "NONE",
                "8N1 - No parity (simulator/Python)"
            ),
            ParityMode(
                SerialPort.EVEN_PARITY,
                "EVEN",
                "8E1 - Even parity (standard EHL)"
            ),
            ParityMode(
                SerialPort.ODD_PARITY,
                "ODD",
                "8O1 - Odd parity (rare)"
            )
        )
    }
    
    /**
     * Auto-detect parity by attempting communication with each mode.
     * Returns the first working parity mode.
     * 
     * @param portName Serial port device name (e.g., "/dev/ttyS0", "/tmp/vserial1")
     * @param dispenserAddress Dispenser address to test (default: 1)
     * @param baudRate Baud rate (default: 9600)
     * @param dataBits Data bits (default: 8)
     * @param stopBits Stop bits mode (default: ONE_STOP_BIT)
     * @param testTimeoutMs Timeout for each test attempt (default: 2000ms)
     * @return Detected parity mode (SerialPort.NO_PARITY, EVEN_PARITY, or ODD_PARITY)
     * @throws IllegalStateException if no parity mode works
     */
    suspend fun autoDetectParity(
        portName: String,
        dispenserAddress: Int = 1,
        baudRate: Int = 9600,
        dataBits: Int = 8,
        stopBits: Int = SerialPort.ONE_STOP_BIT,
        testTimeoutMs: Long = 2000
    ): Int {
        logger.info("")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  🔍 AUTO-DETECTING PARITY MODE")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("  Port:       $portName")
        logger.info("  Address:    $dispenserAddress")
        logger.info("  Baud Rate:  $baudRate")
        logger.info("  Timeout:    ${testTimeoutMs}ms per test")
        logger.info("════════════════════════════════════════════════════════════")
        logger.info("")
        
        for (parityMode in PARITY_MODES) {
            logger.info("Testing parity: ${parityMode.name} - ${parityMode.description}")
            
            val result = testCommunication(
                portName = portName,
                address = dispenserAddress,
                baudRate = baudRate,
                dataBits = dataBits,
                stopBits = stopBits,
                parity = parityMode.value,
                timeoutMs = testTimeoutMs
            )
            
            if (result) {
                logger.info("")
                logger.info("════════════════════════════════════════════════════════════")
                logger.info("  ✅ AUTO-DETECTED PARITY: ${parityMode.name}")
                logger.info("════════════════════════════════════════════════════════════")
                logger.info("  Description: ${parityMode.description}")
                logger.info("════════════════════════════════════════════════════════════")
                logger.info("")
                return parityMode.value
            } else {
                logger.debug("❌ Parity ${parityMode.name} failed")
            }
        }
        
        val error = "Could not auto-detect parity - no mode worked. " +
                "Ensure the dispenser is powered on and connected to $portName"
        logger.error(error)
        throw IllegalStateException(error)
    }
    
    /**
     * Test communication with specific parity setting.
     * 
     * @return true if communication successful, false otherwise
     */
    private suspend fun testCommunication(
        portName: String,
        address: Int,
        baudRate: Int,
        dataBits: Int,
        stopBits: Int,
        parity: Int,
        timeoutMs: Long
    ): Boolean {
        var manager: SerialPortManager? = null
        
        return try {
            val config = SerialPortConfig(
                portName = portName,
                baudRate = baudRate,
                dataBits = dataBits,
                stopBits = stopBits,
                parity = parity,
                readTimeout = timeoutMs.toInt(),
                writeTimeout = 1000
            )
            
            manager = SerialPortManager(config)
            if (!manager.connect()) {
                logger.debug("Failed to connect with parity mode $parity")
                return false
            }
            
            // Create communicator with raw logging disabled for cleaner auto-detect output
            val communicator = EhlCommunicator(manager, enableRawLogging = false)
            
            // Send state query and wait for response
            val testPacket = EhlPacketBuilder.createStateQuery(address)
            
            withTimeout(timeoutMs) {
                try {
                    val response = communicator.sendAndReceive(testPacket, timeoutMs)
                    logger.debug("Received valid response: ${response.command}")
                    true
                } catch (e: TimeoutCancellationException) {
                    logger.debug("Timeout waiting for response")
                    false
                } catch (e: Exception) {
                    logger.debug("Communication error: ${e.message}")
                    false
                }
            }
        } catch (e: TimeoutCancellationException) {
            logger.debug("Test timed out for parity $parity")
            false
        } catch (e: Exception) {
            logger.debug("Test failed for parity $parity: ${e.message}")
            false
        } finally {
            try {
                manager?.disconnect()
            } catch (e: Exception) {
                logger.debug("Error disconnecting: ${e.message}")
            }
        }
    }
}
