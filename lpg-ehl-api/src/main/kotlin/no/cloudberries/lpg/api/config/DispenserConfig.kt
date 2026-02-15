package no.cloudberries.lpg.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Dispenser address from external configuration only.
 * Must not be part of REST paths or request parameters.
 *
 * Configure in application.yaml:
 *   lpg:
 *     dispenser:
 *       address: 33
 * Or via environment: LPG_DISPENSER_ADDRESS
 */
@Configuration
@ConfigurationProperties(prefix = "lpg.dispenser")
class DispenserConfig {
    /** Single dispenser/pump address for this station (e.g. 33). */
    var address: Int = 1
}
