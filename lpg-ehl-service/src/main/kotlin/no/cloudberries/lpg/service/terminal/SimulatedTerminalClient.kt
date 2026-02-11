package no.cloudberries.lpg.service.terminal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * HTTP client for Payment Terminal Simulator.
 * Implements reserve/capture flow for pump-triggered filling.
 */
@Component
@ConditionalOnProperty(name = ["payment.terminal.enabled"], havingValue = "true")
class SimulatedTerminalClient(
    @Value("\${payment.terminal.base-url:http://localhost:18080}") private val baseUrl: String
) : TerminalClient {

    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    private val objectMapper = ObjectMapper()

    override fun reserve(amountMinor: Int): ReservationResponse {
        return try {
            val body = """
                {"AmountMinor": $amountMinor, "Currency": "NOK", "OperatorId": "0000"}
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/payments/reservation"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = json.path("Success").asBoolean(false)
            val operationId = json.path("OperationId").takeIf { !it.isMissingNode }?.asText()
            val error = json.path("Error").takeIf { !it.isMissingNode }?.asText()

            log.info("Reservation: success={}, operationId={}, amount={} kr", success, operationId, amountMinor / 100.0)
            ReservationResponse(success = success, operationId = operationId, error = error)
        } catch (e: Exception) {
            log.error("Reservation failed: {}", e.message)
            ReservationResponse(success = false, error = e.message)
        }
    }

    override fun capture(operationId: String, amountMinor: Int): CaptureResponse {
        return try {
            val body = """
                {"OperationId": "$operationId", "AmountMinor": $amountMinor, "Currency": "NOK", "OperatorId": "0000"}
            """.trimIndent()

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/payments/completion"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = json.path("Success").asBoolean(false)
            val error = json.path("Error").takeIf { !it.isMissingNode }?.asText()

            log.info("Capture: operationId={}, amount={} kr, success={}", operationId, amountMinor / 100.0, success)
            CaptureResponse(success = success, operationId = operationId, error = error)
        } catch (e: Exception) {
            log.error("Capture failed: {}", e.message)
            CaptureResponse(success = false, operationId = operationId, error = e.message)
        }
    }

    override fun reversal(operationId: String?): ReversalResponse {
        return try {
            val body = """{"Password": "0000"}"""

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/v1/admin/reversal"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val json = objectMapper.readTree(response.body())

            val success = json.path("Success").asBoolean(false)
            val opId = json.path("OperationId").takeIf { !it.isMissingNode }?.asText()
            val error = json.path("Error").takeIf { !it.isMissingNode }?.asText()

            log.info("Reversal: operationId={}, success={}", operationId ?: opId, success)
            ReversalResponse(success = success, operationId = opId, error = error)
        } catch (e: Exception) {
            log.error("Reversal failed: {}", e.message)
            ReversalResponse(success = false, operationId = operationId, error = e.message)
        }
    }
}
