package no.cloudberries.lpg.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
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
 * - lpg-ehl-core: Protocol og kommunikasjon
 * - lpg-ehl-emulator: LAB mode simulator
 * - lpg-transport: Serial port kommunikasjon
 */
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "no.cloudberries.lpg.api",            // Web controllers og config
        "no.cloudberries.lpg.service",        // Business logic fra service-modulen
        "no.cloudberries.lpg.payment",        // Payment controllers
        "no.cloudberries.lpg.credit",         // Credit controllers
        "no.cloudberries.lpg.communication",  // EhlCommunicator fra core
        "no.cloudberries.lpg.emulator",       // Emulator (LAB mode)
        "no.cloudberries.lpg.transport"       // Serial port transport
    ]
)
@EntityScan(
    basePackages = [
        "no.cloudberries.lpg.service.model",
        "no.cloudberries.lpg.service.credit"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "no.cloudberries.lpg.service.repository",
        "no.cloudberries.lpg.service.credit"
    ]
)
@EnableScheduling
class LpgEhlApiApplication

fun main(args: Array<String>) {
    runApplication<LpgEhlApiApplication>(*args)
}
