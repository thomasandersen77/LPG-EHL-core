package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Manages serial port connections for RS-485 communication with LPG dispensers.
 * Handles opening, closing, and configuration of serial ports.
 * 
 * Implements SerialPortIO interface for production use with real serial ports.
 */
class SerialPortManager(private val config: SerialPortConfig) : SerialPortIO {
    private val logger = LoggerFactory.getLogger(SerialPortManager::class.java)
    private var serialPort: SerialPort? = null
    private val lock = Any()

    /**
     * Check if the serial port is currently open and connected.
     */
    override val isConnected: Boolean
        get() = synchronized(lock) { serialPort?.isOpen == true }

    /**
     * Open the serial port with the configured settings.
     *
     * @return true if successful, false otherwise
     * @throws IOException if unable to open the port
     */
    override fun connect(): Boolean {
        synchronized(lock) {
            if (isConnected) {
                logger.warn("Serial port ${config.portName} is already connected")
                return true
            }

            logger.info("Opening serial port: ${config.portName}")
            
            val port = SerialPort.getCommPort(config.portName)
            
            // Configure port settings
            port.baudRate = config.baudRate
            port.numDataBits = config.dataBits
            port.numStopBits = config.stopBits
            port.parity = config.parity
            
            // Set timeouts
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                config.readTimeout,
                config.writeTimeout
            )

            // Open the port
            if (!port.openPort()) {
                val error = "Failed to open serial port ${config.portName}"
                logger.error(error)
                throw IOException(error)
            }

            serialPort = port
            logger.info("Serial port ${config.portName} opened successfully: ${config}")
            return true
        }
    }

    /**
     * Close the serial port if it's open.
     */
    override fun disconnect() {
        synchronized(lock) {
            serialPort?.let { port ->
                if (port.isOpen) {
                    logger.info("Closing serial port ${config.portName}")
                    port.closePort()
                    logger.info("Serial port ${config.portName} closed")
                }
            }
            serialPort = null
        }
    }

    /**
     * Write raw bytes to the serial port.
     *
     * @param data Bytes to write
     * @return Number of bytes written
     * @throws IOException if not connected or write fails
     */
    override fun write(data: ByteArray): Int {
        synchronized(lock) {
            val port = serialPort ?: throw IOException("Serial port not connected")
            
            if (!port.isOpen) {
                throw IOException("Serial port ${config.portName} is not open")
            }

            val bytesWritten = port.writeBytes(data, data.size)
            
            if (bytesWritten < 0) {
                throw IOException("Failed to write to serial port ${config.portName}")
            }

            logger.debug("Wrote $bytesWritten bytes to ${config.portName}: ${data.toHexString()}")
            return bytesWritten
        }
    }

    /**
     * Read available bytes from the serial port.
     *
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read, or empty array if no data available
     * @throws IOException if not connected or read fails
     */
    override fun read(maxBytes: Int): ByteArray {
        synchronized(lock) {
            val port = serialPort ?: throw IOException("Serial port not connected")
            
            if (!port.isOpen) {
                throw IOException("Serial port ${config.portName} is not open")
            }

            val available = port.bytesAvailable()
            if (available <= 0) {
                return ByteArray(0)
            }

            val buffer = ByteArray(minOf(available, maxBytes))
            val bytesRead = port.readBytes(buffer, buffer.size)
            
            if (bytesRead < 0) {
                throw IOException("Failed to read from serial port ${config.portName}")
            }

            val result = buffer.copyOf(bytesRead)
            logger.debug("Read $bytesRead bytes from ${config.portName}: ${result.toHexString()}")
            return result
        }
    }

    /**
     * Get the number of bytes available to read.
     */
    fun bytesAvailable(): Int {
        synchronized(lock) {
            return serialPort?.bytesAvailable() ?: 0
        }
    }

    /**
     * Flush any pending output.
     */
    override fun flush() {
        synchronized(lock) {
            serialPort?.flushIOBuffers()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SerialPortManager::class.java)

        /**
         * List all available serial ports on the system.
         *
         * @return List of port names
         */
        fun listAvailablePorts(): List<String> {
            val ports = SerialPort.getCommPorts()
            val portNames = ports.map { it.systemPortName }
            logger.info("Available serial ports: $portNames")
            return portNames
        }

        /**
         * Check if a specific serial port exists.
         *
         * @param portName Port name to check
         * @return true if port exists
         */
        fun portExists(portName: String): Boolean {
            return listAvailablePorts().contains(portName)
        }
    }
}

/**
 * Extension function to convert ByteArray to hex string for logging.
 */
private fun ByteArray.toHexString(): String {
    return joinToString(" ") { "%02X".format(it) }
}
