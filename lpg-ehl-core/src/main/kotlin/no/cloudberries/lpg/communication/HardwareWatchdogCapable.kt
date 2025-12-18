package no.cloudberries.lpg.communication

/**
 * Interface for hardware watchdog capability.
 * 
 * This interface allows for dependency inversion, making it possible to:
 * - Use real serial ports with watchdog in production (via SerialPortManager)
 * - Mock the interface for unit testing without JDK bytecode restrictions
 */
interface HardwareWatchdogCapable {
    /**
     * Enable the hardware watchdog monitoring.
     */
    fun enableWatchdog()
    
    /**
     * Disable the hardware watchdog monitoring.
     */
    fun disableWatchdog()
    
    /**
     * Check if the connection is alive (has received data recently).
     * This should be called periodically by the application.
     * 
     * @return true if connection is healthy, false if watchdog timeout exceeded
     */
    fun checkWatchdog(): Boolean
    
    /**
     * Attempt to reconnect to the serial port.
     * Call this when watchdog detects a dead connection.
     * 
     * @return true if reconnect successful, false otherwise
     */
    fun reconnect(): Boolean
    
    /**
     * Get time since last data was received (for monitoring).
     * 
     * @return milliseconds since last data received
     */
    fun getTimeSinceLastData(): Long
}
