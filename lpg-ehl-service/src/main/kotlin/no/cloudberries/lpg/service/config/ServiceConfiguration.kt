package no.cloudberries.lpg.service.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Service Module Configuration
 * 
 * This configuration is automatically picked up by all entry points (webapp, headless, CLI)
 * that depend on lpg-ehl-service module. It ensures that all JPA repositories and entities
 * in the service module are properly scanned and registered with Spring.
 * 
 * Packages covered:
 * - no.cloudberries.lpg.service.repository (repositories for transactions, daily summary, etc.)
 * - no.cloudberries.lpg.service.credit (credit accounts and customers)
 * - no.cloudberries.lpg.service.azure (Azure sync queue)
 * - no.cloudberries.lpg.service.transaction (transaction entities and watchdog)
 */
@Configuration
@EnableJpaRepositories(
    basePackages = [
        "no.cloudberries.lpg.service.repository",
        "no.cloudberries.lpg.service.credit",
        "no.cloudberries.lpg.service.azure",
        "no.cloudberries.lpg.service.transaction"
    ]
)
@EntityScan(
    basePackages = [
        "no.cloudberries.lpg.service.model",
        "no.cloudberries.lpg.service.credit",
        "no.cloudberries.lpg.service.azure",
        "no.cloudberries.lpg.service.transaction"
    ]
)
class ServiceConfiguration
