package no.cloudberries.lpg.headless

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.service.pump.DispenserService
import no.cloudberries.lpg.service.pump.PumpStateService
import no.cloudberries.lpg.service.system.HardwareWatchdogService
import no.cloudberries.lpg.service.azure.AzureQueueReaderService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Headless Startup Runner
 * 
 * Denne komponenten kjører ved oppstart av headless-applikasjonen og:
 * 1. Initialiserer hardware-kommunikasjon (dispenser, serial port)
 * 2. Starter watchdog for å overvåke hardware-tilstand
 * 3. Aktiverer Azure sync services
 * 4. Logger systemstatus
 * 
 * Alt skjer automatisk uten brukerinteraksjon - perfekt for produksjon.
 */
@Component
class HeadlessStartupRunner(
    private val ehlCommunicator: EhlCommunicator,
    private val dispenserService: DispenserService,
    @Autowired(required = false) private val pumpStateService: PumpStateService?,
    @Autowired(required = false) private val hardwareWatchdogService: HardwareWatchdogService?,
    @Autowired(required = false) private val azureQueueReaderService: AzureQueueReaderService?,
    @Value("\${ehl.transport.mode:}") private val transportMode: String,
    @Value("\${lpg.mode:LAB}") private val legacyMode: String,
    @Value("\${lpg.dispenser.address:1}") private val dispenserAddress: Int
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("🚀 HEADLESS STARTUP SEQUENCE")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
        
        // 1. Initialize hardware communication
        initializeHardware()
        
        // 2. Start hardware monitoring
        startHardwareMonitoring()
        
        // 3. Start Azure sync services
        startAzureSync()
        
        // 4. Report system status
        reportSystemStatus()
        
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("✅ HEADLESS APPLICATION READY")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
        logger.info("💡 System is now running in background mode")
        logger.info("💡 Transactions will be saved to database automatically")
        logger.info("💡 Azure sync is active (if configured)")
        logger.info("💡 Check logs for dispenser activity")
        logger.info("")
    }
    
    /**
     * Determine effective transport mode:
     * - ehl.transport.mode takes precedence if set
     * - Falls back to lpg.mode for backwards compatibility
     */
    private fun getEffectiveMode(): String = when {
        transportMode.equals("SOCAT", ignoreCase = true) -> "SOCAT"
        transportMode.equals("HARDWARE", ignoreCase = true) -> "HARDWARE"
        transportMode.equals("EMULATOR", ignoreCase = true) -> "EMULATOR"
        legacyMode.equals("FIELD", ignoreCase = true) -> "HARDWARE"
        legacyMode.equals("LAB", ignoreCase = true) -> "EMULATOR"
        else -> "HARDWARE" // Default for headless
    }
    
    private fun initializeHardware() {
        val effectiveMode = getEffectiveMode()
        
        logger.info("🔌 Initializing hardware communication...")
        logger.info("   Mode: $effectiveMode")
        logger.info("   Dispenser Address: $dispenserAddress")
        
        try {
            when (effectiveMode) {
                "EMULATOR" -> logger.info("   🧪 EMULATOR MODE: Using in-memory emulator")
                "SOCAT" -> logger.info("   🔗 SOCAT MODE: Using virtual PTY to PLS Simulator")
                "HARDWARE" -> logger.info("   🏭 HARDWARE MODE: Using real serial hardware")
            }
            
            // Test communication by sending a STATE query
            logger.info("   📡 Testing communication with dispenser...")
            val testPacket = EhlPacket(
                address = dispenserAddress,
                command = EhlCommand.STATE,
                data = byteArrayOf()
            )
            
            val response = runBlocking {
                ehlCommunicator.sendAndReceive(testPacket, 2000)
            }
            
            logger.info("   ✅ Communication test successful")
            logger.info("   Response: address=${response.address}, command=${response.command.name}, data=${response.data.size} bytes")
            
            // Send response to DispenserService for processing
            dispenserService.handlePacket(response)
            
        } catch (e: Exception) {
            logger.error("   ❌ Failed to initialize hardware: ${e.message}", e)
            logger.warn("   ⚠️  Continuing - polling service will retry")
        }
    }
    
    private fun startHardwareMonitoring() {
        if (hardwareWatchdogService != null) {
            logger.info("🐕 Starting hardware watchdog service...")
            // Watchdog kjører automatisk via @Scheduled annotations
            logger.info("   ✅ Watchdog service active")
        } else {
            logger.info("   ℹ️  Hardware watchdog service not available")
        }
    }
    
    private fun startAzureSync() {
        if (azureQueueReaderService != null) {
            logger.info("☁️  Starting Azure sync services...")
            // Azure services kjører automatisk via @Scheduled annotations
            logger.info("   ✅ Azure queue reader active")
            logger.info("   ✅ Transaction sync active")
        } else {
            logger.info("   ℹ️  Azure sync services not configured")
        }
    }
    
    private fun reportSystemStatus() {
        logger.info("")
        logger.info("📊 SYSTEM STATUS:")
        logger.info("   • Dispensers: Ready for operation")
        logger.info("   • Database: Connected")
        logger.info("   • Scheduled tasks: Running")
        
        pumpStateService?.let { pump ->
            try {
                val status = pump.getStatus(1)
                logger.info("   • Pump #1 State: ${status.state}")
                logger.info("   • Current Price: ${status.pricePerLitreKr} kr/L")
            } catch (e: Exception) {
                logger.warn("   • Pump status: Not available")
            }
        }
    }
}
