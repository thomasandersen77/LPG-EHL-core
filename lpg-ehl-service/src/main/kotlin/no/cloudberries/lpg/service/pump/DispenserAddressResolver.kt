package no.cloudberries.lpg.service.pump

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Resolves which RS-485 dispenser address to use.
 *
 * In FIELD profile we typically run against a single physical dispenser, and the UI may still use
 * logical pump id "1". In that case we must force commands to the configured hardware address
 * (lpg.dispenser.address).
 *
 * In LAB/TEST profiles we keep the requested address to support multiple simulated dispensers.
 */
@Component
class DispenserAddressResolver(
    private val environment: Environment,
    @Value("\${lpg.dispenser.address:}") private val configuredAddressRaw: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val configuredAddress: Int? = configuredAddressRaw
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.toIntOrNull()
        ?.takeIf { it in 1..255 }

    fun resolve(requestedAddress: Int): Int {
        require(requestedAddress in 1..255) { "Invalid dispenser address: $requestedAddress (must be 1-255)" }

        val isField = environment.activeProfiles.any { it.equals("field", ignoreCase = true) }
        if (!isField) return requestedAddress

        val hw = configuredAddress ?: return requestedAddress
        if (hw == requestedAddress) return requestedAddress

        // In field mode we force all pump operations to the configured RS-485 address to avoid
        // accidentally talking to the wrong unit when the UI uses a logical id (e.g. 1).
        logger.warn("FIELD mode: overriding requested dispenser address {} -> {} (lpg.dispenser.address)", requestedAddress, hw)
        return hw
    }
}

