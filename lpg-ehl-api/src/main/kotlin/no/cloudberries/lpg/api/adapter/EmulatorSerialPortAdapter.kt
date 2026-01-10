package no.cloudberries.lpg.api.adapter

import no.cloudberries.lpg.communication.SerialPortIO
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.emulator.InMemorySerialPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * LAB MODE: Emulator Serial Port Adapter for in-memory testing.
 * 
 * Dette er DEFAULT modus. Når ehl.emulator.enabled=true (eller ikke satt),
 * brukes denne adapteren som kobler EhlCommunicator til EhlDispenserEmulator.
 * 
 * Fordeler:
 * - Ingen fysisk hardware nødvendig
 * - VB6-kompatibel emulator som responderer på protokollkommandoer
 * - Realistisk latency-simulering for timeout-testing
 * - Trygt for utvikling - kan ikke ved et uhell påvirke ekte pumper
 * 
 * Konfigurasjon i application.yaml:
 * ```yaml
 * ehl:
 *   emulator:
 *     enabled: true  # DEFAULT - aktiverer LAB MODE
 *     latency-ms: 20  # Simulert serial port latency
 *     price-per-liter-cents: 1590
 * ```
 */
@Component
@ConditionalOnProperty(
    name = ["ehl.emulator.enabled"],
    havingValue = "true",
    matchIfMissing = true  // DEFAULT: LAB MODE når property ikke er satt
)
class EmulatorSerialPortAdapter(
    @Value("\${ehl.emulator.dispenser-address:1}")
    private val dispenserAddress: Int,
    
    @Value("\${ehl.emulator.price-per-liter-cents:1590}")
    private val pricePerLiterCents: Int,
    
    @Value("\${ehl.emulator.latency-ms:20}")
    private val latencyMs: Long
) : SerialPortIO {
    
    private val logger = LoggerFactory.getLogger(EmulatorSerialPortAdapter::class.java)
    
    // Lazy initialization for emulator og in-memory serial port
    private val _emulator: EhlDispenserEmulator by lazy {
        EhlDispenserEmulator(
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents
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
    
    override fun read(maxBytes: Int): ByteArray {
        return inMemoryPort.read(maxBytes)
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
    fun getEmulator(): EhlDispenserEmulator = _emulator
}
