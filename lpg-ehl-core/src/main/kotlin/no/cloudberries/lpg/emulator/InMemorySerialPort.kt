package no.cloudberries.lpg.emulator

import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory serial port implementation for emulator communication.
 * 
 * LAB MODE: Denne implementasjonen kobler EhlCommunicator til EhlDispenserEmulator
 * uten fysisk RS-485 hardware. Brukes for utvikling og testing.
 * 
 * Funksjoner:
 * - Thread-safe kommunikasjon via concurrent queues
 * - Realistisk latency-simulering (20ms) for å teste timeouts
 * - VB6-kompatibel emulator som backend
 * 
 * @param emulator Dispenser-emulatoren som håndterer protokollkommandoer
 * @param simulatedLatencyMs Simulert ventetid for å etterligne ekte serial port (default: 20ms)
 */
class InMemorySerialPort(
    private val emulator: EhlDispenserEmulator,
    private val simulatedLatencyMs: Long = 20
) : SerialTransport {
    
    private val logger = LoggerFactory.getLogger(InMemorySerialPort::class.java)
    private val toEmulator = ConcurrentLinkedQueue<Byte>()
    private val fromEmulator = ConcurrentLinkedQueue<Byte>()
    @Volatile
    private var connected = false
    
    override val isConnected: Boolean
        get() = connected
    
    override fun connect(): Boolean {
        logger.info("🔌 InMemorySerialPort: Kobler til emulator (LAB MODE)")
        connected = true
        return true
    }
    
    override fun disconnect() {
        logger.info("🔌 InMemorySerialPort: Kobler fra emulator")
        connected = false
        toEmulator.clear()
        fromEmulator.clear()
    }
    
    override fun write(data: ByteArray): Int {
        check(connected) { "Port ikke tilkoblet" }
        
        // Simuler realistisk serial port latency
        if (simulatedLatencyMs > 0) {
            try {
                Thread.sleep(simulatedLatencyMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        
        // Queue data til emulator
        data.forEach { toEmulator.add(it) }
        
        // Prosesser alle køede bytes
        val inBytes = ByteArray(toEmulator.size) { toEmulator.poll() }
        val responses = emulator.onBytesFromHost(inBytes)
        
        // Kø responser for lesing
        responses.forEach { frame ->
            frame.forEach { b -> fromEmulator.add(b) }
        }
        
        logger.debug("📤 Sendt ${data.size} bytes til emulator, mottok ${responses.sumOf { it.size }} bytes respons")
        return data.size
    }
    
    override fun readAvailable(maxBytes: Int): ByteArray {
        check(connected) { "Port ikke tilkoblet" }
        
        if (fromEmulator.isEmpty()) return ByteArray(0)
        
        val result = mutableListOf<Byte>()
        while (result.size < maxBytes && !fromEmulator.isEmpty()) {
            result.add(fromEmulator.poll())
        }
        return result.toByteArray()
    }
    
    override fun flush() {
        // No-op for in-memory implementasjon
    }
    
    override fun clearBuffer() {
        toEmulator.clear()
        fromEmulator.clear()
        logger.debug("🧹 Buffere tømt")
    }
}
