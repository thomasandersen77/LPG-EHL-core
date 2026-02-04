package no.cloudberries.lpg.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * LPG EHL Web Application
 * 
 * Spring Boot applikasjon med embedded Tomcat som tilbyr:
 * - REST API for administrasjon av dispensere
 * - React-basert frontend (Control Panel)
 * - WebSocket for real-time oppdateringer
 * - Swagger/OpenAPI dokumentasjon
 * 
 * Komponenter hentes fra flere moduler:
 * - lpg-ehl-service: Business logic (services, repositories, models)
 *   - JPA repositories and entities configured in ServiceConfiguration
 * - lpg-ehl-core: Protocol og kommunikasjon
 * - lpg-ehl-emulator: LAB mode simulator
 * - lpg-transport: Serial port kommunikasjon
 * 
 * NOTE: JPA repository and entity scanning for service module is configured
 * in ServiceConfiguration (lpg-ehl-service module) and automatically picked up.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "no.cloudberries.lpg.api",            // Web controllers og config
        "no.cloudberries.lpg.service",        // Business logic fra service-modulen (includes config)
        "no.cloudberries.lpg.payment",        // Payment controllers
        "no.cloudberries.lpg.credit",         // Credit controllers
        "no.cloudberries.lpg.communication",  // EhlCommunicator fra transport
        "no.cloudberries.lpg.transport"       // Serial port transport
        // NOTE: no.cloudberries.lpg.emulator is NOT scanned here.
        // Emulator beans are conditionally created in CommunicationConfig based on lpg.mode property.
    ]
)
@EnableScheduling
class LpgEhlApiApplication

fun main(args: Array<String>) {
    runApplication<LpgEhlApiApplication>(*args)
}
