package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.api.adapter.EmulatorSerialPortAdapter
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.transport.SerialTransport
import no.cloudberries.lpg.emulator.IEhlDispenserEmulator
import no.cloudberries.lpg.emulator.impl.EhlDispenserEmulatorImpl
import no.cloudberries.lpg.emulator.impl.DispenserSimulatorImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import jakarta.annotation.PostConstruct

/**
 * Configuration for EHL protocol communication.
 * 
 * ARKITEKTUR: Dual-mode system styrt av Spring Profiles:
 * 
 * LAB MODE (profile=lab eller ingen profile):
 *   - EmulatorSerialPortAdapter laster automatisk
 *   - Kommuniserer med in-memory EhlDispenserEmulator
 *   - Trygt for utvikling - ingen fysisk hardware påvirkes
 * 
 * FIELD MODE (profile=field):
 *   - RealSerialPortAdapter laster automatisk
 *   - Kommuniserer med fysisk RS-485 serial port eller socat PTY
 *   - For produksjon på ARK-3600 eller socat testing
 * 
 * KRITISK: EhlCommunicator vet IKKE hvilken modus den kjører i.
 * Den sender bare bytes til SerialTransport-interfacet.
 */
@Configuration
class CommunicationConfig(
    private val environment: Environment
) {
    
    private val logger = LoggerFactory.getLogger(CommunicationConfig::class.java)
    
    @PostConstruct
    fun logMode() {
        val activeProfiles = environment.activeProfiles.toList()
        val isLabMode = activeProfiles.isEmpty() || 
                        activeProfiles.contains("lab") || 
                        activeProfiles.contains("default")
        val mode = if (isLabMode) "🧪 LAB MODE" else "🏭 FIELD MODE"
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("  EHL KOMMUNIKASJON: $mode")
        logger.info("  Active profiles: ${activeProfiles.ifEmpty { listOf("default") }}")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
    }
    
    /**
     * Eksponerer EhlDispenserEmulator kun i LAB MODE.
     * Brukes av ProtocolTestController for debugging.
     */
    @Bean
    @Profile("lab", "default")
    fun dispenserEmulator(
        @Value("\${ehl.emulator.dispenser-address:1}") dispenserAddress: Int,
        @Value("\${ehl.emulator.price-per-liter-cents:1590}") pricePerLiterCents: Int
    ): IEhlDispenserEmulator {
        logger.info("🧪 Creating EhlDispenserEmulator (address=$dispenserAddress, price=$pricePerLiterCents)")
        val simulator = DispenserSimulatorImpl(litresPerSecond = 0.5, pricePerLitreCents = pricePerLiterCents)
        return EhlDispenserEmulatorImpl(
            simulator = simulator,
            address = dispenserAddress,
            pricePerLitreCents = pricePerLiterCents,
            litresPerSecond = 0.5
        )
    }
    
    // NOTE: Heartbeat disabled due to class conflict between lpg-ehl-core and lpg-ehl-emulator
    // Both have EhlDispenserEmulator in same package. Emulator has good logging already.
}
