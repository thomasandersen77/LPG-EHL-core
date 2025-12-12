package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.communication.SerialPortIO
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory implementation of SerialPortIO for testing with EHL emulator.
 * 
 * This class provides a virtual serial port that communicates with an EhlDispenserEmulator
 * instead of a real hardware serial port. This allows testing the full EHL protocol stack
 * without physical hardware.
 * 
 * @property emulator The dispenser emulator to communicate with
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

        // Add data to queue for emulator
        data.forEach { toEmulator.add(it) }

        // Let emulator process the incoming bytes
        val inBytes = ByteArray(toEmulator.size) { toEmulator.poll() }
        val responses = emulator.onBytesFromHost(inBytes)

        // Add emulator responses to the read queue
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
