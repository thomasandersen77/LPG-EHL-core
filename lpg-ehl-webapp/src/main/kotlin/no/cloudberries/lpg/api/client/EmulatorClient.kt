package no.cloudberries.lpg.api.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
@ConditionalOnProperty(
    name = ["ehl.emulator.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
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
            val url = "$baseUrl/api/v1/emulator/settle/$dispenserId?method=$paymentMethod"
            logger.info("🔗 Calling emulator settle endpoint: $url")
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() in 200..299) {
                logger.info("✅ Emulator settled successfully: $dispenserId")
                logger.info("📢 Windows should now be reset to 0.00/0.00")
                logger.debug("Response: ${response.body()}")
                true
            } else {
                logger.error("❌ Emulator settle FAILED: HTTP ${response.statusCode()}")
                logger.error("Response body: ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Exception calling emulator settle endpoint", e)
            logger.error("Make sure emulator is running on $baseUrl")
            false
        }
    }
}
