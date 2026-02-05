package no.cloudberries.lpg.api.adapter

import no.cloudberries.lpg.transport.SerialTransport
import no.cloudberries.lpg.emulator.IEhlDispenserEmulator
import no.cloudberries.lpg.emulator.IDispenserSimulator
import no.cloudberries.lpg.emulator.impl.InMemorySerialPort
import no.cloudberries.lpg.emulator.impl.EhlDispenserEmulatorImpl
import no.cloudberries.lpg.emulator.impl.DispenserSimulatorImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * LAB MODE: Emulator Serial Port Adapter for in-memory testing.
 * 
 * Aktiv når profile=lab ELLER ingen profile er satt (default).
 * Kobler EhlCommunicator til in-memory EhlDispenserEmulator.
 * 
 * Fordeler:
 * - Ingen fysisk hardware nødvendig
 * - VB6-kompatibel emulator som responderer på protokollkommandoer
 * - Realistisk latency-simulering for timeout-testing
 * - Trygt for utvikling - kan ikke ved et uhell påvirke ekte pumper
 * 
 * Konfigurasjon i application-lab.yaml:
 * ```yaml
 * ehl:
 *   emulator:
 *     latency-ms: 20  # Simulert serial port latency
 *     price-per-liter-cents: 1590
 * ```
 */
@Profile("lab", "default")
class EmulatorSerialPortAdapter(
    @Value("\${ehl.emulator.dispenser-address:1}")
    private val dispenserAddress: Int,
    
    @Value("\${ehl.emulator.price-per-liter-cents:1590}")
    private val pricePerLiterCents: Int,
    
    @Value("\${ehl.emulator.latency-ms:20}")
    private val latencyMs: Long
) : SerialTransport {
    
    private val logger = LoggerFactory.getLogger(EmulatorSerialPortAdapter::class.java)
    
    // Lazy initialization for emulator og in-memory serial port
    private val _emulator: IEhlDispenserEmulator by lazy {
        val simulator = DispenserSimulatorImpl(litresPerSecond = 0.5, pricePerLitreCents = pricePerLiterCents)
        EhlDispenserEmulatorImpl(
            simulator = simulator,
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents,
            litresPerSecond = 0.5
        )
    }
    
    private val inMemoryPort: InMemorySerialPort by lazy {
        InMemorySerialPort(_emulator, latencyMs)
    }
    
    @PostConstruct
    fun init() {
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("🧪 LAB MODE AKTIVERT - Kjører mot emulator")
        logger.info("   Dispenser-adresse: $dispenserAddress")
        logger.info("   Pris: ${pricePerLiterCents / 100.0} kr/L")
        logger.info("   Simulert latency: ${latencyMs}ms")
        logger.info("═══════════════════════════════════════════════════════════")
    }
    
    override val isConnected: Boolean
        get() = inMemoryPort.isConnected
    
    override fun connect(): Boolean {
        logger.info("🔌 Kobler til emulator (LAB MODE)...")
        return inMemoryPort.connect()
    }
    
    override fun disconnect() {
        logger.info("🔌 Kobler fra emulator...")
        inMemoryPort.disconnect()
    }
    
    override fun write(data: ByteArray): Int {
        return inMemoryPort.write(data)
    }
    
    override fun readAvailable(maxBytes: Int): ByteArray {
        return inMemoryPort.readAvailable(maxBytes)
    }
    
    override fun flush() {
        inMemoryPort.flush()
    }
    
    override fun clearBuffer() {
        inMemoryPort.clearBuffer()
    }
    
    /**
     * Hent emulator-instansen for direkte tilgang (f.eks. for testing/debugging).
     */
    fun getEmulator(): IEhlDispenserEmulator = _emulator
}
