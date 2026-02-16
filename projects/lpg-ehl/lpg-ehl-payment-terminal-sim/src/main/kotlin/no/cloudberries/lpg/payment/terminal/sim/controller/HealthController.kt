package no.cloudberries.lpg.payment.terminal.sim.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Health check endpoint.
 *
 * NOTE: This endpoint returns lowercase keys (exception to PascalCase rule).
 * We build the JSON manually to ensure correct casing.
 */
@RestController
class HealthController(
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/health", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun health(): ResponseEntity<String> {
        // Build JSON manually to ensure lowercase keys
        val healthMap = mapOf(
            "status" to "ok",
            "timestamp" to Instant.now().toString(),
            "configLoaded" to true
        )

        val json = objectMapper.writeValueAsString(healthMap)
        return ResponseEntity.ok(json)
    }
}
