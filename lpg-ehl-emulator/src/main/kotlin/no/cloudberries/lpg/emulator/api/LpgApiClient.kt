package no.cloudberries.lpg.emulator.api

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
    name = ["emulator.standalone.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class LpgApiClient(
    @Value("\${lpg-api.base-url}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(LpgApiClient::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    
    /**
     * Save a transaction to the API and return the database transaction ID
     */
    fun saveTransaction(transaction: SaveTransactionRequest): String? {
        return try {
            val json = """
                {
                    "stationId": "${transaction.stationId}",
                    "edgeId": "${transaction.edgeId}",
                    "dispenserId": "${transaction.dispenserId}",
                    "dispenserAddress": ${transaction.dispenserAddress},
                    "nozzleNumber": ${transaction.nozzleNumber},
                    "volumeDeciliters": ${transaction.volumeDeciliters},
                    "amountOre": ${transaction.amountOre},
                    "pricePerLiter": ${transaction.pricePerLiter},
                    ${if (transaction.paymentType != null) "\"paymentType\": \"${transaction.paymentType}\"," else ""}
                    "productCode": "${transaction.productCode}",
                    "includesRoadTax": ${transaction.includesRoadTax}
                }
            """.trimIndent()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/v1/transactions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() in 200..299) {
                logger.debug("Transaction saved successfully: ${response.body()}")
                // Extract transactionId from JSON response
                val transactionIdRegex = """"transactionId"\s*:\s*"([^"]+)""".toRegex()
                val match = transactionIdRegex.find(response.body())
                match?.groupValues?.get(1)
            } else {
                logger.warn("Failed to save transaction: ${response.statusCode()} - ${response.body()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error calling API to save transaction", e)
            null
        }
    }

    /**
     * Update payment status for a transaction
     */
    fun updatePaymentStatus(transactionId: String, paymentMethod: String): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/v1/transactions/$transactionId/payment?paymentMethod=$paymentMethod&paymentStatus=PAID"))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() in 200..299) {
                logger.debug("Payment status updated successfully for transaction $transactionId")
                true
            } else {
                logger.warn("Failed to update payment status: ${response.statusCode()} - ${response.body()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error updating payment status for transaction $transactionId", e)
            false
        }
    }
}

/**
 * Request to save a transaction to the cloud API.
 * 
 * Multi-Station Support:
 * - stationId: Identifies the physical station (e.g., "S001", "S002")
 * - edgeId: Identifies the edge device running the emulator
 * - dispenserId: Identifies the specific dispenser within the station
 * - dispenserAddress: Legacy EHL address for protocol compatibility
 */
data class SaveTransactionRequest(
    val stationId: String,
    val edgeId: String,
    val dispenserId: String,
    val dispenserAddress: Int,
    val nozzleNumber: Int = 1,
    val volumeDeciliters: Int,
    val amountOre: Int,
    val pricePerLiter: Int,
    val paymentType: String? = null,  // null = PENDING, set on settlement
    val productCode: String = "LPG",
    val includesRoadTax: Boolean = true
)
