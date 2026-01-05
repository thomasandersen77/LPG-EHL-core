package no.cloudberries.lpg.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class EmulatorClient(
    @Value("\${emulator.base-url:http://localhost:8090}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(EmulatorClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    
    /**
     * Notify emulator to settle and broadcast reset to Windows Dispenserkontroll.
     * This ensures Windows displays are cleared after payment.
     * 
     * @param dispenserId Dispenser ID (currently always 1)
     * @param paymentMethod Payment method (CARD, CASH, CREDIT)
     * @return true if successful, false otherwise
     */
    fun settleDispenser(dispenserId: Int, paymentMethod: String): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/v1/emulator/settle/$dispenserId?method=$paymentMethod"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() in 200..299) {
                logger.info("✅ Emulator settled: dispenser=$dispenserId, method=$paymentMethod")
                true
            } else {
                logger.warn("⚠️ Failed to settle emulator: ${response.statusCode()} - ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Error calling emulator to settle dispenser $dispenserId", e)
            false
        }
    }
}
