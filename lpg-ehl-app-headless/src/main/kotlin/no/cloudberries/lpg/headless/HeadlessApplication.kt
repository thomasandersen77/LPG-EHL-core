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
        "no.cloudberries.lpg.headless",       // Headless config, startup
        "no.cloudberries.lpg.api.controller", // REST controllers from webapp
        "no.cloudberries.lpg.service",        // Business logic
        "no.cloudberries.lpg.communication",  // EhlCommunicator
        "no.cloudberries.lpg.transport",      // Serial transport
        "no.cloudberries.lpg.pls"             // PLS protocol
    ]
    // Note: no.cloudberries.lpg.api.config excluded - headless has its own config
)
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
