package no.cloudberries.lpg.api.adapter

import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortIO
import no.cloudberries.lpg.communication.SerialPortManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * FIELD MODE: Real Serial Port Adapter for fysisk RS-485 kommunikasjon.
 * 
 * Denne adapteren brukes kun i produksjon når ehl.emulator.enabled=false.
 * Den kobler til ekte hardware via jSerialComm-biblioteket.
 * 
 * Konfigurasjon i application.yaml:
 * ```yaml
 * ehl:
 *   emulator:
 *     enabled: false  # Aktiverer denne adapteren
 *   serial:
 *     port: "/dev/ttyS0"
 *     baud-rate: 9600
 * ```
 */
@Component
@ConditionalOnProperty(
    name = ["ehl.emulator.enabled"],
    havingValue = "false"
)
class RealSerialPortAdapter(
    @Value("\${ehl.serial.port:/dev/ttyS0}")
    private val portName: String,
    
    @Value("\${ehl.serial.baud-rate:9600}")
    private val baudRate: Int
) : SerialPortIO {
    
    private val logger = LoggerFactory.getLogger(RealSerialPortAdapter::class.java)
    
    private val serialPortManager: SerialPortManager by lazy {
        val config = SerialPortConfig(
            portName = portName,
            baudRate = baudRate,
            dataBits = 8,
            stopBits = 1,
            parity = 0,  // No parity
            readTimeout = 1000,
            writeTimeout = 1000
        )
        SerialPortManager(config)
    }
    
    @PostConstruct
    fun init() {
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("🔌 FIELD MODE AKTIVERT - Kobler til ekte hardware")
        logger.info("   Serial port: $portName")
        logger.info("   Baud rate: $baudRate")
        logger.info("═══════════════════════════════════════════════════════════")
    }
    
    @PreDestroy
    fun shutdown() {
        if (isConnected) {
            logger.info("🔌 Kobler fra serial port $portName...")
            disconnect()
        }
    }
    
    override val isConnected: Boolean
        get() = serialPortManager.isConnected
    
    override fun connect(): Boolean {
        logger.info("🔌 Kobler til serial port $portName...")
        return try {
            val result = serialPortManager.connect()
            if (result) {
                logger.info("✅ Tilkoblet til $portName (FIELD MODE)")
                // Enable hardware watchdog for self-healing
                serialPortManager.enableWatchdog()
            } else {
                logger.error("❌ Kunne ikke koble til $portName")
            }
            result
        } catch (e: Exception) {
            logger.error("❌ Feil ved tilkobling til $portName: ${e.message}", e)
            false
        }
    }
    
    override fun disconnect() {
        serialPortManager.disableWatchdog()
        serialPortManager.disconnect()
        logger.info("🔌 Frakoblet fra $portName")
    }
    
    override fun write(data: ByteArray): Int {
        return serialPortManager.write(data)
    }
    
    override fun read(maxBytes: Int): ByteArray {
        return serialPortManager.read(maxBytes)
    }
    
    override fun flush() {
        serialPortManager.flush()
    }
    
    override fun clearBuffer() {
        // For real serial port, we flush and discard any pending data
        flush()
        // Read and discard any pending data
        var discarded = 0
        while (true) {
            val data = read(256)
            if (data.isEmpty()) break
            discarded += data.size
        }
        if (discarded > 0) {
            logger.debug("🧹 Forkastet $discarded bytes fra buffer")
        }
    }
    
    /**
     * Check watchdog and reconnect if needed (for scheduled health checks)
     */
    fun checkHealth(): Boolean {
        if (!serialPortManager.checkWatchdog()) {
            logger.warn("⚠️ Watchdog timeout - forsøker reconnect...")
            return serialPortManager.reconnect()
        }
        return true
    }
}
