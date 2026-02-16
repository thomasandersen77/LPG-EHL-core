package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort
import jakarta.annotation.PreDestroy
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages serial port connections for RS-485 communication with LPG dispensers.
 * Handles opening, closing, and configuration of serial ports.
 * 
 * Implements SerialTransport interface for production use with real serial ports.
 * Implements HardwareWatchdogCapable interface for watchdog functionality.
 */
open class  SerialPortManager(private val config: SerialPortConfig) : SerialTransport, HardwareWatchdogCapable, CommandAttemptTracker {
    private val logger = LoggerFactory.getLogger(SerialPortManager::class.java)
    private var serialPort: SerialPort? = null
    private val lock = Any()
    
    // PART 4: HARDWARE WATCHDOG - Self-healing connection monitoring (attempt-based)
    @Volatile
    private var lastDataReceivedTime: Long = System.currentTimeMillis()
    private val reconnectDelayMs: Long = 5_000     // 5 seconds wait before reconnect
    @Volatile
    private var watchdogEnabled: Boolean = false
    private val attemptFailureThreshold = 3
    private val attemptWindowMs: Long = 60_000
    @Volatile
    private var lastAttemptAtMs: Long = 0L
    @Volatile
    private var lastSuccessfulCommandAtMs: Long = 0L
    private val consecutiveFailureCount = AtomicInteger(0)

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

            // Configure parameters (safe to set pre-open; we also re-apply post-open for some drivers).
            port.setComPortParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
            port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                config.readTimeout,
                config.writeTimeout
            )

            if (!port.openPort()) {
                // Provide detailed diagnostics
                val availablePorts = SerialPort.getCommPorts().map { it.systemPortName }
                val errorMsg = buildString {
                    appendLine("Failed to open serial port ${config.portName}")
                    appendLine("Possible causes:")
                    appendLine("  1. Port does not exist or is a dead symlink")
                    appendLine("  2. Port is already in use by another process")
                    appendLine("  3. Insufficient permissions (try: chmod 666 <device>)")
                    appendLine("  4. Device path is incorrect")
                    appendLine()
                    appendLine("Available serial ports detected by jSerialComm:")
                    if (availablePorts.isEmpty()) {
                        appendLine("  (none detected)")
                    } else {
                        availablePorts.forEach { appendLine("  - $it") }
                    }
                    appendLine()
                    appendLine("Tip: On macOS with socat virtual ports, jSerialComm may not")
                    appendLine("enumerate them but can still open them if the underlying")
                    appendLine("device has correct permissions (crw-rw-rw-).")
                }
                logger.error(errorMsg)
                throw IOException("Failed to open serial port ${config.portName}")
            }
            
            // Re-apply after open (some drivers only commit changes post-open).
            port.setComPortParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
            port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)
            port.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING,
                config.readTimeout,
                config.writeTimeout
            )

            // Optional RS-485 driver direction control (RTS toggle) to match Python tool capability.
            if (config.rs485Enabled) {
                val ok = port.setRs485ModeParameters(
                    true,
                    config.rs485RtsHighDuringSend,
                    config.rs485RtsHighAfterSend,
                    config.rs485RxDuringTx,
                    config.rs485DelayRtsBeforeSendMs,
                    config.rs485DelayRtsAfterSendMs
                )
                if (ok) {
                    logger.info(
                        "RS-485 mode enabled (RTS during send={}, after send={}, rxDuringTx={}, rtsBeforeMs={}, rtsAfterMs={})",
                        config.rs485RtsHighDuringSend,
                        config.rs485RtsHighAfterSend,
                        config.rs485RxDuringTx,
                        config.rs485DelayRtsBeforeSendMs,
                        config.rs485DelayRtsAfterSendMs
                    )
                } else {
                    logger.warn("RS-485 mode enable failed (continuing without driver RS-485 control)")
                }
            } else {
                // Best-effort: ensure any RS-485 mode from previous runs is disabled.
                port.disableRs485ModeControl()
            }

            serialPort = port
            logger.info("Serial port ${config.portName} opened successfully: ${config}")
            return true
        }
    }

    /**
     * Close the serial port if it's open.
     */
    @PreDestroy
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
     * Disconnects on read failure to ensure clean reconnect.
     *
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read, or empty array if no data available
     * @throws IOException if not connected or read fails
     */
    override fun readAvailable(maxBytes: Int): ByteArray {
        synchronized(lock) {
            val port = serialPort ?: throw IOException("Serial port not connected")
            
            if (!port.isOpen) {
                throw IOException("Serial port ${config.portName} is not open")
            }

            try {
                val available = port.bytesAvailable()
                if (available <= 0) {
                    return ByteArray(0)
                }

                val buffer = ByteArray(minOf(available, maxBytes))
                val bytesRead = port.readBytes(buffer, buffer.size)
                
                if (bytesRead < 0) {
                    // Read failure - disconnect to force new FD on reconnect
                    logger.error("Read failed from ${config.portName}: bytesRead=$bytesRead")
                    disconnectInternal()
                    throw IOException("Failed to read from serial port ${config.portName}")
                }

                val result = buffer.copyOf(bytesRead)
                
                // WATCHDOG: Update timestamp when we receive valid data
                if (bytesRead > 0) {
                    lastDataReceivedTime = System.currentTimeMillis()
                }
                
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
     * Starts watchdog tracking for attempt-based health checks.
     * Watchdog uses recent command attempts + consecutive failures to decide reconnect.
     */
    override fun enableWatchdog() {
        synchronized(lock) {
            if (watchdogEnabled) {
                logger.warn("Watchdog already enabled for ${config.portName}")
                return
            }
            
            watchdogEnabled = true
            lastDataReceivedTime = System.currentTimeMillis()
            lastAttemptAtMs = 0L
            lastSuccessfulCommandAtMs = 0L
            consecutiveFailureCount.set(0)
            logger.info(
                "Hardware watchdog enabled for ${config.portName} " +
                "(attempt window: ${attemptWindowMs}ms, failure threshold: $attemptFailureThreshold)"
            )
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
     * Check if the connection is alive (attempt-based).
     * Returns false only when recent attempts have repeatedly failed.
     * 
     * @return true if connection is healthy, false if failure threshold exceeded
     */
    override fun checkWatchdog(): Boolean {
        synchronized(lock) {
            if (!watchdogEnabled || !isConnected) {
                return true  // Watchdog disabled or not connected - no check needed
            }

            val lastAttemptAt = lastAttemptAtMs
            if (lastAttemptAt == 0L) {
                return true  // No attempts recorded - silence is OK
            }

            val timeSinceLastAttempt = System.currentTimeMillis() - lastAttemptAt
            if (timeSinceLastAttempt > attemptWindowMs) {
                resetFailures()
                return true  // No recent attempts - silence is OK
            }

            val failures = consecutiveFailureCount.get()
            if (failures >= attemptFailureThreshold) {
                logger.error(
                    "⚠️ WATCHDOG FAILURE: $failures consecutive failures on ${config.portName} " +
                    "within last ${timeSinceLastAttempt}ms (threshold: $attemptFailureThreshold)."
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
                resetFailures()
                lastAttemptAtMs = 0L
                lastSuccessfulCommandAtMs = 0L
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
     * Get time since last data was received (telemetry only).
     */
    override fun getTimeSinceLastData(): Long {
        return System.currentTimeMillis() - lastDataReceivedTime
    }

    override fun recordAttempt() {
        lastAttemptAtMs = System.currentTimeMillis()
    }

    override fun recordSuccess() {
        val now = System.currentTimeMillis()
        lastAttemptAtMs = now
        lastSuccessfulCommandAtMs = now
        consecutiveFailureCount.set(0)
    }

    override fun recordFailure() {
        lastAttemptAtMs = System.currentTimeMillis()
        consecutiveFailureCount.incrementAndGet()
    }

    override fun resetFailures() {
        consecutiveFailureCount.set(0)
    }

    override val lastAttemptAt: Long
        get() = lastAttemptAtMs

    override val lastSuccessfulCommandAt: Long
        get() = lastSuccessfulCommandAtMs

    override val consecutiveFailures: Int
        get() = consecutiveFailureCount.get()

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
