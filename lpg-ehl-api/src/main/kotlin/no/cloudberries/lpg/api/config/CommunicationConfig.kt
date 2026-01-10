package no.cloudberries.lpg.api.config

import no.cloudberries.lpg.api.adapter.EmulatorSerialPortAdapter
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.SerialPortIO
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Configuration for EHL protocol communication.
 * 
 * ARKITEKTUR: Dual-mode system styrt av ehl.emulator.enabled:
 * 
 * LAB MODE (ehl.emulator.enabled=true, DEFAULT):
 *   - EmulatorSerialPortAdapter laster automatisk
 *   - Kommuniserer med in-memory EhlDispenserEmulator
 *   - Trygt for utvikling - ingen fysisk hardware påvirkes
 * 
 * FIELD MODE (ehl.emulator.enabled=false):
 *   - RealSerialPortAdapter laster automatisk
 *   - Kommuniserer med fysisk RS-485 serial port
 *   - For produksjon på ARK-3600 eller lignende
 * 
 * KRITISK: EhlCommunicator vet IKKE hvilken modus den kjører i.
 * Den sender bare bytes til SerialPortIO-interfacet.
 */
@Configuration
class CommunicationConfig {
    
    private val logger = LoggerFactory.getLogger(CommunicationConfig::class.java)
    
    @Value("\${ehl.emulator.enabled:true}")
    private var emulatorEnabled: Boolean = true
    
    @PostConstruct
    fun logMode() {
        val mode = if (emulatorEnabled) "🧪 LAB MODE" else "🏭 FIELD MODE"
        logger.info("")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("  EHL KOMMUNIKASJON: $mode")
        logger.info("═══════════════════════════════════════════════════════════")
        logger.info("")
    }
    
    /**
     * EhlCommunicator - hovedprotokoll-laget.
     * 
     * Spring injiserer automatisk riktig SerialPortIO-implementasjon:
     * - EmulatorSerialPortAdapter (LAB MODE) eller
     * - RealSerialPortAdapter (FIELD MODE)
     * 
     * basert på ehl.emulator.enabled property.
     */
    @Bean
    fun ehlCommunicator(serialPortIO: SerialPortIO): EhlCommunicator {
        logger.info("🔧 Oppretter EhlCommunicator med ${serialPortIO::class.simpleName}")
        
        // Koble til serial port
        if (!serialPortIO.isConnected) {
            serialPortIO.connect()
        }
        
        return EhlCommunicator(serialPortIO)
    }
    
    /**
     * Eksponerer EhlDispenserEmulator kun i LAB MODE.
     * Brukes av ProtocolTestController for debugging.
     */
    @Bean
    fun dispenserEmulator(serialPortIO: SerialPortIO): EhlDispenserEmulator? {
        return if (serialPortIO is EmulatorSerialPortAdapter) {
            serialPortIO.getEmulator()
        } else {
            null  // Ingen emulator i FIELD MODE
        }
    }
}
