package no.cloudberries.lpg.pls

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Real serial port transport using jSerialComm.
 * 
 * For produksjon på Debian-baserte edge devices (ARK-3600 eller lignende).
 * Kommuniserer med fysisk RS-485 dispenser via /dev/ttyS0 eller lignende.
 * 
 * KONFIGURASJON:
 * - ehl.serial.port (default: /dev/ttyS0)
 * - ehl.serial.baud-rate (default: 9600)
 * - ehl.serial.data-bits (default: 8)
 * - ehl.serial.parity (default: EVEN)
 * - ehl.serial.stop-bits (default: 1)
 * 
 * Serial parametere: 9600 baud, 8E1 (8 data bits, Even parity, 1 stop bit)
 */
class RealSerialTransport(
    private val portName: String,
    private val baudRate: Int = 9600,
    private val dataBits: Int = 8,
    private val parity: Int = SerialPort.EVEN_PARITY,
    private val stopBits: Int = SerialPort.ONE_STOP_BIT
) : SerialTransport {
    
    private val logger = LoggerFactory.getLogger(RealSerialTransport::class.java)
    private var serialPort: SerialPort? = null
    
    override val isConnected: Boolean
        get() = serialPort?.isOpen == true
    
    override fun connect(): Boolean {
        try {
            // Check if port is in enumerated list (informational only)
            val availablePorts = SerialPort.getCommPorts()
            val portExists = availablePorts.any { it.systemPortName == portName || it.systemPortPath == portName }
            
            if (!portExists) {
                logger.warn("Port $portName not in enumerated list. Available: ${availablePorts.joinToString { it.systemPortPath }}")
                logger.warn("Attempting connection anyway (may be virtual/PTY device)...")
            }
            
            // Attempt to open the serial port (works for both real and virtual ports)
            val port = SerialPort.getCommPort(portName)
            
            // Configure serial parameters (default: 9600 8E1)
            port.baudRate = baudRate
            port.numDataBits = dataBits
            port.numStopBits = stopBits
            port.parity = parity
            
            // Open port with timeout (3000ms for compatibility with socat)
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                3000,  // Read timeout 3000ms (socat + simulator needs more time)
                0      // No write timeout
            )
            
            if (!port.openPort()) {
                logger.error("Failed to open serial port $portName")
                return false
            }
            
            serialPort = port
            logger.info("🔌 FIELD MODE: Connected to serial port $portName at $baudRate baud")
            return true
            
        } catch (e: Exception) {
            logger.error("Error connecting to serial port $portName: ${e.message}", e)
            return false
        }
    }
    
    override fun disconnect() {
        try {
            serialPort?.closePort()
            serialPort = null
            logger.info("🔌 Disconnected from serial port $portName")
        } catch (e: Exception) {
            logger.error("Error disconnecting from serial port: ${e.message}", e)
        }
    }
    
    override fun write(data: ByteArray): Int {
        val port = serialPort ?: throw IllegalStateException("Serial port not connected")
        
        try {
            var totalWritten = 0
            var remaining = data
            var retries = 0
            val maxRetries = 3
            
            while (remaining.isNotEmpty() && retries < maxRetries) {
                val bytesWritten = port.writeBytes(remaining, remaining.size)
                
                if (bytesWritten <= 0) {
                    // Complete write failure - throw to trigger watchdog reconnect
                    throw IOException("Write failed: 0 bytes written (retry $retries)")
                }
                
                totalWritten += bytesWritten
                
                if (bytesWritten < remaining.size) {
                    // Partial write - retry with remaining bytes
                    logger.warn("Partial write: $bytesWritten of ${remaining.size} bytes, retrying...")
                    remaining = remaining.copyOfRange(bytesWritten, remaining.size)
                    retries++
                    Thread.sleep(10)  // Small delay before retry
                } else {
                    // All bytes written successfully
                    remaining = ByteArray(0)
                }
            }
            
            if (remaining.isNotEmpty()) {
                throw IOException("Failed to write all bytes after $maxRetries retries: wrote $totalWritten of ${data.size}")
            }
            
            return totalWritten
        } catch (e: IOException) {
            logger.error("Serial port write failed: ${e.message}")
            throw e
        } catch (e: Exception) {
            logger.error("Error writing to serial port: ${e.message}", e)
            throw IOException("Serial port write error: ${e.message}", e)
        }
    }
    
    override fun readAvailable(maxBytes: Int): ByteArray {
        val port = serialPort ?: throw IllegalStateException("Serial port not connected")
        
        try {
            val available = port.bytesAvailable()
            if (available <= 0) {
                return ByteArray(0)
            }
            
            val bytesToRead = minOf(available, maxBytes)
            val buffer = ByteArray(bytesToRead)
            val bytesRead = port.readBytes(buffer, bytesToRead)
            
            return if (bytesRead == bytesToRead) {
                buffer
            } else {
                buffer.copyOf(bytesRead)
            }
        } catch (e: Exception) {
            logger.error("Error reading from serial port: ${e.message}", e)
            return ByteArray(0)
        }
    }
    
    override fun flush() {
        try {
            serialPort?.flushIOBuffers()
        } catch (e: Exception) {
            logger.warn("Error flushing serial port: ${e.message}")
        }
    }
    
    override fun clearBuffer() {
        try {
            // Read and discard all available data
            val port = serialPort ?: return
            while (port.bytesAvailable() > 0) {
                val buffer = ByteArray(256)
                port.readBytes(buffer, buffer.size)
            }
            logger.debug("🧹 Serial port buffer cleared")
        } catch (e: Exception) {
            logger.warn("Error clearing buffer: ${e.message}")
        }
    }
}
