package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Manages serial port connections for RS-485 communication with LPG dispensers.
 * Handles opening, closing, and configuration of serial ports.
 * 
 * Implements SerialTransport interface for production use with real serial ports.
 * Implements HardwareWatchdogCapable interface for watchdog functionality.
 */
open class  SerialPortManager(private val config: SerialPortConfig) : SerialTransport, HardwareWatchdogCapable {
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
     * Handles partial writes with retries and treats 0 bytes written as hard failure.
     *
     * @param data Bytes to write
     * @return Number of bytes written
     * @throws IOException if not connected, write fails, or port becomes dead
     */
    override fun write(data: ByteArray): Int {
        synchronized(lock) {
            val port = serialPort ?: throw IOException("Serial port not connected")
            
            if (!port.isOpen) {
                throw IOException("Serial port ${config.portName} is not open")
            }

            var totalWritten = 0
            var remaining = data
            var retries = 0
            val maxRetries = 3
            
            try {
                while (remaining.isNotEmpty() && retries < maxRetries) {
                    val bytesWritten = port.writeBytes(remaining, remaining.size)
                    
                    if (bytesWritten <= 0) {
                        // Hard failure - port is dead (socat/simulator died, cable unplugged)
                        logger.error("Write failed: 0 bytes written to ${config.portName} (retry $retries)")
                        // Disconnect to force new FD on reconnect
                        disconnectInternal()
                        throw IOException("Write failed: 0 bytes written - port dead")
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
                    // Exhausted retries
                    disconnectInternal()
                    throw IOException("Failed to write all bytes after $maxRetries retries: wrote $totalWritten of ${data.size}")
                }
                
                logger.debug("Wrote $totalWritten bytes to ${config.portName}: ${data.toHexString()}")
                return totalWritten
                
            } catch (e: IOException) {
                throw e  // Already handled above
            } catch (e: Exception) {
                logger.error("Unexpected error writing to ${config.portName}: ${e.message}", e)
                disconnectInternal()
                throw IOException("Serial port write error: ${e.message}", e)
            }
        }
    }
    
    /**
     * Internal disconnect without logging (called from error handlers while holding lock).
     */
    private fun disconnectInternal() {
        serialPort?.let { port ->
            try {
                if (port.isOpen) {
                    port.closePort()
                }
            } catch (e: Exception) {
                // Ignore close errors during error recovery
            }
        }
        serialPort = null
    }

    /**
     * Read available bytes from the serial port.
     * BLOCKS until at least 1 byte is available or timeout is reached.
     * This relies on TIMEOUT_READ_SEMI_BLOCKING mode configured in connect().
     * 
     * Disconnects on read failure to ensure clean reconnect.
     *
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read, or empty array if timeout with no data
     * @throws IOException if not connected or read fails
     */
    override fun readAvailable(maxBytes: Int): ByteArray {
        synchronized(lock) {
            val port = serialPort ?: throw IOException("Serial port not connected")
            
            if (!port.isOpen) {
                throw IOException("Serial port ${config.portName} is not open")
            }

            try {
                // With TIMEOUT_READ_SEMI_BLOCKING, readBytes will block until:
                // 1. At least 1 byte is read, OR
                // 2. Timeout is reached (config.readTimeout ms)
                // 
                // DO NOT check bytesAvailable() first - that defeats the purpose of blocking read!
                val buffer = ByteArray(maxBytes)
                val bytesRead = port.readBytes(buffer, buffer.size)
                
                if (bytesRead < 0) {
                    // Read failure - disconnect to force new FD on reconnect
                    logger.error("Read failed from ${config.portName}: bytesRead=$bytesRead")
                    disconnectInternal()
                    throw IOException("Failed to read from serial port ${config.portName}")
                }
                
                if (bytesRead == 0) {
                    // Timeout with no data - this is normal, return empty array
                    return ByteArray(0)
                }

                val result = buffer.copyOf(bytesRead)
                
                // WATCHDOG: Update timestamp when we receive valid data
                lastDataReceivedTime = System.currentTimeMillis()
                
                logger.debug("Read $bytesRead bytes from ${config.portName}: ${result.toHexString()}")
                return result
                
            } catch (e: IOException) {
                throw e  // Already handled above
            } catch (e: Exception) {
                logger.error("Unexpected error reading from ${config.portName}: ${e.message}", e)
                disconnectInternal()
                throw IOException("Serial port read error: ${e.message}", e)
            }
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
     * Clear the receive buffer by reading and discarding all available data.
     */
    override fun clearBuffer() {
        synchronized(lock) {
            try {
                val port = serialPort ?: return
                var cleared = 0
                while (port.bytesAvailable() > 0) {
                    val buffer = ByteArray(256)
                    val read = port.readBytes(buffer, buffer.size)
                    if (read <= 0) break
                    cleared += read
                }
                if (cleared > 0) {
                    logger.debug("🧹 Serial port buffer cleared: $cleared bytes discarded")
                }
            } catch (e: Exception) {
                logger.warn("Error clearing buffer: ${e.message}")
            }
        }
    }

    /**
     * PART 4: HARDWARE WATCHDOG - Enable connection monitoring.
     * Starts a background watchdog that checks if data is being received.
     * If no data is received for `watchdogTimeoutMs`, triggers auto-reconnect.
     */
    override fun enableWatchdog() {
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
    override fun disableWatchdog() {
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
    override fun checkWatchdog(): Boolean {
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
    override fun reconnect(): Boolean {
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
    override fun getTimeSinceLastData(): Long {
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
