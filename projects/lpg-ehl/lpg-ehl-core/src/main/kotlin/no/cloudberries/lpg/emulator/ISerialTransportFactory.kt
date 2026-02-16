package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.transport.SerialTransport

/**
 * Factory interface for creating SerialTransport implementations for testing.
 * 
 * This allows the emulator module to provide in-memory serial port implementations
 * without the core module depending on concrete implementations.
 */
interface ISerialTransportFactory {
    /**
     * Create an in-memory serial port connected to the given emulator.
     * 
     * @param emulator The emulator to connect to
     * @param simulatedLatencyMs Simulated latency in milliseconds (default: 20ms)
     * @return SerialTransport implementation for testing
     */
    fun createInMemoryPort(
        emulator: IEhlDispenserEmulator,
        simulatedLatencyMs: Long = 20
    ): SerialTransport
}
