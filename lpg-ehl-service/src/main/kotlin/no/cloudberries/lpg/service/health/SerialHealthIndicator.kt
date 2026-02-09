package no.cloudberries.lpg.service.health

import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import kotlinx.coroutines.runBlocking

/**
 * HealthIndicator som sjekker om seriell kommunikasjon fungerer.
 * Utfører en STATE-forespørsel til pumpe 1 (linetest).
 */
@Component
class SerialHealthIndicator(
    private val ehlCommunicator: EhlCommunicator
) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun health(): Health {
        return try {
            val linetestPacket = EhlPacket(1, EhlCommand.STATE, byteArrayOf())
            
            val response = runBlocking {
                ehlCommunicator.sendAndReceive(linetestPacket, 2000)
            }
            
            if (response.data.isNotEmpty()) {
                Health.up()
                    .withDetail("serial", "OK")
                    .withDetail("dispenser_1", "CONNECTED")
                    .build()
            } else {
                Health.down()
                    .withDetail("serial", "CONNECTED")
                    .withDetail("dispenser_1", "NO_RESPONSE")
                    .build()
            }
        } catch (e: Exception) {
            logger.warn("Serial health check failed: ${e.message}")
            Health.down()
                .withDetail("serial", "ERROR")
                .withDetail("error", e.message ?: "Unknown error")
                .build()
        }
    }
}
