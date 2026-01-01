package no.cloudberries.lpg.communication

/**
 * Interface for serial port I/O operations.
 * 
 * This interface allows for dependency inversion, making it possible to:
 * - Use real serial ports in production (via SerialPortManager)
 * - Use in-memory implementations for testing (via InMemorySerialPort)
 * - Mock the interface for unit testing
 */
interface SerialPortIO {
    /**
     * Check if the serial port is currently connected.
     */
    val isConnected: Boolean

    /**
     * Connect to the serial port.
     * 
     * @return true if connection was successful
     */
    fun connect(): Boolean

    /**
     * Disconnect from the serial port.
     */
    fun disconnect()

    /**
     * Write bytes to the serial port.
     * 
     * @param data Bytes to write
     * @return Number of bytes written
     */
    fun write(data: ByteArray): Int

    /**
     * Read available bytes from the serial port.
     * 
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read (may be empty if no data available)
     */
    fun read(maxBytes: Int = 256): ByteArray

    /**
     * Flush any pending output.
     */
    fun flush()
}
