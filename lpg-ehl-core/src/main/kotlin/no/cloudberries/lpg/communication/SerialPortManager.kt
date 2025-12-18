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
open class SerialPortManager(private val config: SerialPortConfig) : SerialPortIO {
    private val logger = LoggerFactory.getLogger(SerialPortManager::class.java)
    private var serialPort: SerialPort? = null
    private val lock = Any()
    
    // PART 4: HARDWARE WATCHDOG - Self-healing connection monitoring
    @Volatile
    private var lastDataReceivedTime: Long = System.currentTimeMillis()
    private val watchdogTimeoutMs: Long = 60_000  // 60 seconds without data = dead connection
    private val reconnectDelayMs: Long = 5_000     // 5 seconds wait before reconnect
    @Volatile
    private var watchdogEnabled: Boolean = false

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
            
            // WATCHDOG: Update timestamp when we receive valid data
            if (bytesRead > 0) {
                lastDataReceivedTime = System.currentTimeMillis()
            }
            
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

    /**
     * PART 4: HARDWARE WATCHDOG - Enable connection monitoring.
     * Starts a background watchdog that checks if data is being received.
     * If no data is received for `watchdogTimeoutMs`, triggers auto-reconnect.
     */
    fun enableWatchdog() {
        synchronized(lock) {
            if (watchdogEnabled) {
                logger.warn("Watchdog already enabled for ${config.portName}")
                return
            }
            
            watchdogEnabled = true
            lastDataReceivedTime = System.currentTimeMillis()
            logger.info("Hardware watchdog enabled for ${config.portName} (timeout: ${watchdogTimeoutMs}ms)")
        }
    }
    
    /**
     * Disable the hardware watchdog.
     */
    fun disableWatchdog() {
        synchronized(lock) {
            watchdogEnabled = false
            logger.info("Hardware watchdog disabled for ${config.portName}")
        }
    }
    
    /**
     * Check if the connection is alive (has received data recently).
     * This should be called periodically by the application.
     * 
     * @return true if connection is healthy, false if watchdog timeout exceeded
     */
    fun checkWatchdog(): Boolean {
        synchronized(lock) {
            if (!watchdogEnabled || !isConnected) {
                return true  // Watchdog disabled or not connected - no check needed
            }
            
            val timeSinceLastData = System.currentTimeMillis() - lastDataReceivedTime
            
            if (timeSinceLastData > watchdogTimeoutMs) {
                logger.error(
                    "⚠️ WATCHDOG TIMEOUT: No data received from ${config.portName} " +
                    "for ${timeSinceLastData}ms (threshold: ${watchdogTimeoutMs}ms). " +
                    "Connection may be dead (USB unplugged/driver hang)."
                )
                return false
            }
            
            return true
        }
    }
    
    /**
     * SELF-HEALING: Attempt to reconnect to the serial port.
     * Call this when watchdog detects a dead connection.
     * 
     * Sequence:
     * 1. Close current port
     * 2. Wait 5 seconds for hardware/driver to reset
     * 3. Open port again
     * 
     * @return true if reconnect successful, false otherwise
     */
    fun reconnect(): Boolean {
        logger.warn("🔄 Attempting reconnect to ${config.portName}...")
        
        try {
            // Step 1: Close existing connection
            disconnect()
            
            // Step 2: Wait for hardware/driver to reset
            logger.info("⏳ Waiting ${reconnectDelayMs}ms for hardware reset...")
            Thread.sleep(reconnectDelayMs)
            
            // Step 3: Reconnect
            logger.info("🔌 Reconnecting to ${config.portName}...")
            val success = connect()
            
            if (success) {
                logger.info("✅ Reconnect successful to ${config.portName}")
                lastDataReceivedTime = System.currentTimeMillis()  // Reset watchdog timer
            } else {
                logger.error("❌ Reconnect failed to ${config.portName}")
            }
            
            return success
            
        } catch (e: Exception) {
            logger.error("Reconnect failed with exception: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Get time since last data was received (for monitoring).
     */
    fun getTimeSinceLastData(): Long {
        return System.currentTimeMillis() - lastDataReceivedTime
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
