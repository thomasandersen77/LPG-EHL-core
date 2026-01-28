package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.api.adapter.EmulatorSerialPortAdapter
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.transport.SerialTransport
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Configuration for EHL protocol communication.
 * 
 * ARKITEKTUR: Two-mode system controlled by lpg.mode:
 * 
 * LAB MODE (lpg.mode=LAB, DEFAULT):
 *   - InMemorySerialPort + EhlDispenserEmulator
 *   - No physical hardware required
 *   - Safe for development and testing
 * 
 * FIELD MODE (lpg.mode=FIELD):
 *   - SerialPortManager + real serial port
 *   - Communicates with physical hardware or socat PTY
 *   - For production on ARK-3600 or field testing
 * 
 * KRITISK: EhlCommunicator vet IKKE hvilken modus den kjører i.
 * Den sender bare bytes til SerialTransport-interfacet.
 */
@Configuration
class CommunicationConfig {
    
    private val logger = LoggerFactory.getLogger(CommunicationConfig::class.java)
    
    @Value("\${lpg.mode:LAB}")
    private lateinit var lpgMode: String
    
    @Value("\${ehl.transport.mode:}")
    private var oldTransportMode: String = ""
    
    @PostConstruct
    fun logMode() {
        // Warn if using deprecated parameter
        if (oldTransportMode.isNotBlank()) {
            logger.warn("")
            logger.warn("⚠️  DEPRECATED: --ehl.transport.mode=$oldTransportMode is no longer used")
            logger.warn("⚠️  Please use --lpg.mode=LAB or --lpg.mode=FIELD instead")
            logger.warn("")
        }
        
        val isLabMode = lpgMode.uppercase() == "LAB"
        val modeEmoji = if (isLabMode) "🧪" else "🏭"
        val modeName = if (isLabMode) "LAB MODE" else "FIELD MODE"
        
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("  EHL KOMMUNIKASJON: $modeEmoji $modeName")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
    }
    
    /**
     * Eksponerer EhlDispenserEmulator kun i LAB MODE.
     * Brukes av ProtocolTestController for debugging.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = ["lpg.mode"],
        havingValue = "LAB",
        matchIfMissing = true  // LAB is default
    )
    fun dispenserEmulator(
        @Value("\${ehl.emulator.dispenser-address:1}") dispenserAddress: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int
    ): EhlDispenserEmulator {
        logger.info("🧪 Creating EhlDispenserEmulator (address=$dispenserAddress, price=$pricePerLiterCents)")
        return EhlDispenserEmulator(
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents
        )
    }
    
    // NOTE: Heartbeat disabled due to class conflict between lpg-ehl-core and lpg-ehl-emulator
    // Both have EhlDispenserEmulator in same package. Emulator has good logging already.
}
