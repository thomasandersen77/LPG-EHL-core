package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.communication.SerialPortIO
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory serial port implementation for emulator communication.
 * 
 * This implementation allows the EHL emulator to communicate with
 * the EhlCommunicator without physical RS-485 hardware, enabling
 * testing and development of the complete protocol stack.
 * 
 * Thread-safe using concurrent queues for bidirectional communication.
 */
class InMemorySerialPort(
    private val emulator: EhlDispenserEmulator
) : SerialPortIO {
    
    private val toEmulator = ConcurrentLinkedQueue<Byte>()
    private val fromEmulator = ConcurrentLinkedQueue<Byte>()
    private var connected = false
    
    override val isConnected: Boolean
        get() = connected
    
    override fun connect(): Boolean {
        connected = true
        return true
    }
    
    override fun disconnect() {
        connected = false
        toEmulator.clear()
        fromEmulator.clear()
    }
    
    override fun write(data: ByteArray): Int {
        check(connected) { "Port not connected" }
        
        // Queue data to emulator
        data.forEach { toEmulator.add(it) }
        
        // Process all queued bytes
        val inBytes = ByteArray(toEmulator.size) { toEmulator.poll() }
        val responses = emulator.onBytesFromHost(inBytes)
        
        // Queue responses for reading
        responses.forEach { frame ->
            frame.forEach { b -> fromEmulator.add(b) }
        }
        
        return data.size
    }
    
    override fun read(maxBytes: Int): ByteArray {
        check(connected) { "Port not connected" }
        
        if (fromEmulator.isEmpty()) return ByteArray(0)
        
        val result = mutableListOf<Byte>()
        while (result.size < maxBytes && !fromEmulator.isEmpty()) {
            result.add(fromEmulator.poll())
        }
        return result.toByteArray()
    }
    
    override fun flush() {
        // No-op for in-memory implementation
    }
}
