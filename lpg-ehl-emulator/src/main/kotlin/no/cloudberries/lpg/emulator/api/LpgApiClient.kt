package no.cloudberries.lpg.emulator.api

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class LpgApiClient(
    @Value("\${lpg-api.base-url}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(LpgApiClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    
    /**
     * Save a transaction to the API
     */
    fun saveTransaction(transaction: SaveTransactionRequest): Boolean {
        return try {
            val json = """
                {
                    "dispenserAddress": ${transaction.dispenserAddress},
                    "volumeDeciliters": ${transaction.volumeDeciliters},
                    "amountOre": ${transaction.amountOre},
                    "pricePerLiter": ${transaction.pricePerLiter}
                }
            """.trimIndent()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/v1/transactions/demo/save"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() in 200..299) {
                logger.debug("Transaction saved successfully: ${response.body()}")
                true
            } else {
                logger.warn("Failed to save transaction: ${response.statusCode()} - ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error calling API to save transaction", e)
            false
        }
    }
}

data class SaveTransactionRequest(
    val dispenserAddress: Int,
    val volumeDeciliters: Int,
    val amountOre: Int,
    val pricePerLiter: Int
)
