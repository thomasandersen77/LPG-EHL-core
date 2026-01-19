package no.cloudberries.lpg.headless

import org.slf4j.LoggerFactory
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Headless LPG EHL Application
 * 
 * Dette er en Spring Boot-applikasjon uten web-server (WebApplicationType.NONE).
 * Den kan kjøres på maskiner uten skjerm for å:
 * - Kommunisere med LPG-dispensere via seriell port
 * - Lagre transaksjoner i PostgreSQL database
 * - Synkronisere data til Azure
 * - Kjøre schedulerte oppgaver (polling, watchdog, etc.)
 * 
 * Bruksområder:
 * - Produksjonsmiljø på bensinstasjon (headless server/Raspberry Pi)
 * - Docker containers
 * - Systemd services på Linux
 * 
 * Kjøring:
 * ```
 * java -jar lpg-ehl-app-headless.jar
 * ```
 */
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "no.cloudberries.lpg.headless",      // Headless-spesifikk kode
        "no.cloudberries.lpg.service",        // Business logic fra service-modulen
        "no.cloudberries.lpg.communication",  // EhlCommunicator fra core
        "no.cloudberries.lpg.emulator",       // Emulator (optional, for LAB mode)
        "no.cloudberries.lpg.transport",      // Serial port transport
        "no.cloudberries.lpg.pls"             // RealSerialTransport
    ]
)
@EntityScan(
    basePackages = [
        "no.cloudberries.lpg.service.pump",
        "no.cloudberries.lpg.service.transaction",
        "no.cloudberries.lpg.service.azure",
        "no.cloudberries.lpg.service.price",
        "no.cloudberries.lpg.service.model",
        "no.cloudberries.lpg.service.credit"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "no.cloudberries.lpg.service.pump",
        "no.cloudberries.lpg.service.transaction",
        "no.cloudberries.lpg.service.azure",
        "no.cloudberries.lpg.service.price",
        "no.cloudberries.lpg.service.repository",
        "no.cloudberries.lpg.service.credit"
    ]
)
@EnableScheduling
class HeadlessApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(HeadlessApplication::class.java)
    
    logger.info("═══════════════════════════════════════════════════════════")
    logger.info("   LPG EHL HEADLESS APPLICATION")
    logger.info("   Mode: HEADLESS (No Web Server)")
    logger.info("═══════════════════════════════════════════════════════════")
    
    // Run as headless Spring Boot application (no web server)
    runApplication<HeadlessApplication>(*args) {
        setWebApplicationType(WebApplicationType.NONE)
    }
    
    // Note: HeadlessStartupRunner executes automatically via CommandLineRunner
    // Scheduled tasks (@Scheduled) keep the application alive
}
