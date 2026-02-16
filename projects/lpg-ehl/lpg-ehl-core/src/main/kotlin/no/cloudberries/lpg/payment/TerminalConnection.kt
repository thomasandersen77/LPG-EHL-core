package no.cloudberries.lpg.payment

import java.io.Closeable

/**
 * Terminal Connection Interface
 * 
 * Clean Architecture abstraction for payment terminal communication.
 * This interface defines the contract for communicating with payment terminals,
 * without exposing implementation details (SSL sockets, serial ports, etc.)
 * 
 * **Implementations:**
 * - `NetsCloudSocketClient` (in lpg-ehl-api) - SSL/TLS connection to Nets Cloud
 * - `DirectTcpTerminalClient` (if needed) - Direct TCP to terminal
 * - `SerialPortTerminalClient` (if needed) - Serial port communication
 * 
 * **Design Principle:**
 * Core business logic should depend on this interface, not on concrete implementations.
 * This allows us to swap communication mechanisms without changing business logic.
 */
interface TerminalConnection : Closeable {
    
    /**
     * Connection state
     */
    val isConnected: Boolean
    
    /**
     * Connect to the payment terminal
     * 
     * @throws TerminalConnectionException if connection fails
     */
    fun connect()
    
    /**
     * Send a command to the terminal and wait for response
     * 
     * @param command Complete protocol frame to send
     * @return Response from terminal
     * @throws TerminalConnectionException if send/receive fails
     */
    fun sendCommand(command: ByteArray): TerminalResponse
    
    /**
     * Send ACK to terminal
     */
    fun sendAck()
    
    /**
     * Send NAK to terminal
     */
    fun sendNak()
}

/**
 * Response from payment terminal
 * 
 * Contains raw data and metadata about the response.
 */
data class TerminalResponse(
    val rawData: ByteArray,
    val hasAck: Boolean,
    val hasNak: Boolean,
    val hasCompleteFrame: Boolean,
    val elapsedMs: Long
) {
    /**
     * Parse the raw data into a BaxResponse
     */
    fun parse(): BaxResponse = NetsBaxProtocol.parseResponse(rawData)
    
    /**
     * Check if response indicates success
     */
    val isSuccess: Boolean
        get() = hasAck && !hasNak && rawData.isNotEmpty()
    
    /**
     * Check if response indicates failure
     */
    val isFailure: Boolean
        get() = hasNak || rawData.isEmpty()
    
    /**
     * Get hex representation of raw data
     */
    fun toHexString(): String = rawData.joinToString(" ") { "%02X".format(it) }
    
    /**
     * Get ASCII representation (printable chars only)
     */
    fun toAsciiString(): String = rawData.map { 
        val c = it.toInt() and 0xFF
        if (c in 32..126) c.toChar() else '.'
    }.joinToString("")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerminalResponse) return false
        return rawData.contentEquals(other.rawData) &&
               hasAck == other.hasAck &&
               hasNak == other.hasNak &&
               hasCompleteFrame == other.hasCompleteFrame
    }
    
    override fun hashCode(): Int {
        var result = rawData.contentHashCode()
        result = 31 * result + hasAck.hashCode()
        result = 31 * result + hasNak.hashCode()
        result = 31 * result + hasCompleteFrame.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "TerminalResponse(bytes=${rawData.size}, ack=$hasAck, nak=$hasNak, frame=$hasCompleteFrame, ms=$elapsedMs)"
    }
}

/**
 * Exception thrown when terminal connection fails
 */
class TerminalConnectionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
