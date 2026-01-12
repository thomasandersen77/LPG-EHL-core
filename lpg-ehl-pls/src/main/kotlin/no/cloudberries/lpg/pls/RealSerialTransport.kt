package no.cloudberries.lpg.pls

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory

/**
 * Real serial port transport using jSerialComm.
 * 
 * For produksjon på Debian-baserte edge devices (ARK-3600 eller lignende).
 * Kommuniserer med fysisk RS-485 dispenser via /dev/ttyS0 eller lignende.
 * 
 * KONFIGURASJON:
 * - ehl.serial.port (default: /dev/ttyS0)
 * - ehl.serial.baud-rate (default: 9600)
 * 
 * Serial parametere: 9600 baud, 8N1, no parity
 */
class RealSerialTransport(
    private val portName: String,
    private val baudRate: Int = 9600
) : SerialTransport {
    
    private val logger = LoggerFactory.getLogger(RealSerialTransport::class.java)
    private var serialPort: SerialPort? = null
    
    override val isConnected: Boolean
        get() = serialPort?.isOpen == true
    
    override fun connect(): Boolean {
        try {
            // Find the serial port
            val port = SerialPort.getCommPort(portName)
            if (port == null) {
                logger.error("Serial port $portName not found")
                return false
            }
            
            // Configure serial parameters: 9600 8N1
            port.baudRate = baudRate
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = SerialPort.NO_PARITY
            
            // Open port with timeout
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                100,  // Read timeout 100ms
                0     // No write timeout
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
            val bytesWritten = port.writeBytes(data, data.size)
            if (bytesWritten != data.size) {
                logger.warn("Only wrote $bytesWritten of ${data.size} bytes")
            }
            return bytesWritten
        } catch (e: Exception) {
            logger.error("Error writing to serial port: ${e.message}", e)
            throw e
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
