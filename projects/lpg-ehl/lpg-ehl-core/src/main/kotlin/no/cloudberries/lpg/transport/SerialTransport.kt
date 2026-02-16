package no.cloudberries.lpg.transport

/**
 * Abstraction for serial transport layer.
 * 
 * Dette interfacet tillater at EhlCommunicator kan jobbe mot både:
 * - InMemorySerialPort (emulator for testing/utvikling)
 * - RealSerialTransport (ekte RS-485 hardware for produksjon)
 * - MockSerialTransport (for unit testing)
 * 
 * Bytt implementasjon via Spring configuration property: ehl.emulator.enabled
 */
interface SerialTransport {
    /**
     * Check if transport is connected and ready.
     */
    val isConnected: Boolean
    
    /**
     * Open/connect the transport.
     * @return true if successful
     */
    fun connect(): Boolean
    
    /**
     * Close/disconnect the transport.
     */
    fun disconnect()
    
    /**
     * Write bytes to transport.
     * @param data Bytes to write
     * @return Number of bytes written
     */
    fun write(data: ByteArray): Int
    
    /**
     * Read available bytes from transport (non-blocking).
     * @param maxBytes Maximum number of bytes to read
     * @return Bytes read (may be empty if no data available)
     */
    fun readAvailable(maxBytes: Int = 256): ByteArray
    
    /**
     * Flush any pending output.
     */
    fun flush()
    
    /**
     * Clear all pending data in receive buffer.
     */
    fun clearBuffer() {}
}
