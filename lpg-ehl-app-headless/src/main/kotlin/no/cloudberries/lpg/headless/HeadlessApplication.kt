package no.cloudberries.lpg.headless

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
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
        "no.cloudberries.lpg.headless",      // Headless-spesifikk kode (config, service, startup)
        "no.cloudberries.lpg.service",        // Business logic fra service-modulen
        "no.cloudberries.lpg.communication",  // EhlCommunicator fra transport
        "no.cloudberries.lpg.transport",      // Serial port transport
        "no.cloudberries.lpg.pls"             // RealSerialTransport
        // Note: no.cloudberries.lpg.emulator is NOT scanned - has web dependencies
        // For LAB mode, emulator is created directly in TransportConfiguration
    ]
)
// EntityScan and EnableJpaRepositories are defined in ServiceConfiguration
@EnableScheduling
class HeadlessApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(HeadlessApplication::class.java)
    
    logger.info("═══════════════════════════════════════════════════════════")
    logger.info("   LPG EHL HEADLESS APPLICATION")
    logger.info("═══════════════════════════════════════════════════════════")
    
    // Run Spring Boot application
    // Web application type is controlled by spring.main.web-application-type in yaml:
    //   - Default (application.yaml): NONE (headless mode)
    //   - debug-api profile: SERVLET (Undertow web server for curl testing)
    runApplication<HeadlessApplication>(*args)
    
    // Note: HeadlessStartupRunner executes automatically via CommandLineRunner
    // Scheduled tasks (@Scheduled) keep the application alive
}
