package no.cloudberries.lpg.api.integration

import no.cloudberries.lpg.api.config.NetsCloudConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * REST client for Nets Cloud Connect API
 * 
 * This client communicates with Nets cloud servers instead of directly with payment terminals.
 * The terminal connects to Nets (IP 3.33.230.243:6001), and our application communicates
 * with Nets via REST API.
 * 
 * Flow:
 * 1. Application calls initiateSale() → Nets API
 * 2. Nets API → Terminal (displays payment request)
 * 3. Application polls checkPaymentStatus() until complete
 * 4. Terminal response → Nets API → Application
 */
@Component
@ConditionalOnProperty("nets.cloud-connect.enabled", havingValue = "true")
class NetsCloudClient(
    private val netsConfig: NetsCloudConfig,
    restClientBuilder: RestClient.Builder
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val restClient: RestClient = restClientBuilder
        .baseUrl(netsConfig.baseUrl)
        .defaultHeaders { headers ->
            headers.setBasicAuth(netsConfig.username, netsConfig.password)
            headers.set("Content-Type", "application/json")
        }
        .build()
    
    /**
     * Initiate a payment/sale transaction
     * 
     * @param amountCents Amount in øre/cents (100 = 1.00 NOK)
     * @param reference Unique transaction reference
     * @return Payment response with paymentId for polling
     */
    fun initiateSale(amountCents: Long, reference: String): PaymentResponse {
        logger.info("Initiating sale: amount={} øre, reference={}", amountCents, reference)
        
        val request = SaleRequest(
            terminalId = netsConfig.terminalId,
            merchantId = netsConfig.merchantId,
            amount = amountCents,
            currency = "NOK",
            transactionType = "10", // 10 = Purchase/Sale
            reference = reference,
            operatorId = "1"
        )
        
        return try {
            val response = restClient.post()
                .uri("/sale") // TODO: Verify endpoint from Nets documentation
                .body(request)
                .retrieve()
                .body(PaymentResponse::class.java)
            
            logger.info("Sale initiated successfully: paymentId={}, status={}", 
                response?.paymentId, response?.status)
            response ?: throw NetsApiException("Empty response from Nets API")
        } catch (e: Exception) {
            logger.error("Failed to initiate sale", e)
            throw NetsApiException("Failed to initiate sale: ${e.message}", e)
        }
    }
    
    /**
     * Initiate a refund transaction
     * 
     * @param amountCents Amount to refund in øre/cents
     * @param originalTransactionId Optional reference to original transaction
     * @return Payment response with paymentId
     */
    fun initiateRefund(amountCents: Long, originalTransactionId: String?): PaymentResponse {
        logger.info("Initiating refund: amount={} øre, originalTxId={}", 
            amountCents, originalTransactionId)
        
        val request = SaleRequest(
            terminalId = netsConfig.terminalId,
            merchantId = netsConfig.merchantId,
            amount = amountCents,
            currency = "NOK",
            transactionType = "20", // 20 = Refund
            reference = UUID.randomUUID().toString(),
            operatorId = "1",
            originalTransactionId = originalTransactionId
        )
        
        return try {
            restClient.post()
                .uri("/refund") // TODO: Verify endpoint from Nets documentation
                .body(request)
                .retrieve()
                .body(PaymentResponse::class.java)
                ?: throw NetsApiException("Empty response from Nets API")
        } catch (e: Exception) {
            logger.error("Failed to initiate refund", e)
            throw NetsApiException("Failed to initiate refund: ${e.message}", e)
        }
    }
    
    /**
     * Check payment status
     * 
     * Cloud Connect is asynchronous - the terminal processes payments independently.
     * This method polls the current status of a payment.
     * 
     * @param paymentId The payment ID returned from initiateSale()
     * @return Current payment status
     */
    fun checkPaymentStatus(paymentId: String): PaymentStatusResponse {
        logger.debug("Checking payment status: paymentId={}", paymentId)
        
        return try {
            restClient.get()
                .uri("/payments/{paymentId}", paymentId) // TODO: Verify endpoint
                .retrieve()
                .body(PaymentStatusResponse::class.java)
                ?: throw NetsApiException("Empty status response from Nets API")
        } catch (e: Exception) {
            logger.error("Failed to check payment status: paymentId={}", paymentId, e)
            throw NetsApiException("Failed to check payment status: ${e.message}", e)
        }
    }
    
    /**
     * Cancel an ongoing payment
     * 
     * @param paymentId The payment ID to cancel
     * @return true if cancellation was successful
     */
    fun cancelPayment(paymentId: String): Boolean {
        logger.info("Cancelling payment: paymentId={}", paymentId)
        
        return try {
            restClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .retrieve()
                .body(CancelResponse::class.java)?.success ?: false
        } catch (e: Exception) {
            logger.error("Failed to cancel payment: paymentId={}", paymentId, e)
            false
        }
    }
    
    // ===========================
    // Data Transfer Objects (DTOs)
    // ===========================
    
    /**
     * Request to initiate a sale/refund
     */
    data class SaleRequest(
        val terminalId: String,
        val merchantId: String,
        val amount: Long,
        val currency: String,
        val transactionType: String, // "10" = sale, "20" = refund, "03" = preauth
        val reference: String,
        val operatorId: String,
        val originalTransactionId: String? = null
    )
    
    /**
     * Response from initiating a payment
     */
    data class PaymentResponse(
        val paymentId: String,
        val status: String, // "PENDING", "PROCESSING", "APPROVED", "DECLINED", "CANCELLED"
        val terminalId: String,
        val amount: Long,
        val currency: String,
        val createdAt: Instant = Instant.now()
    )
    
    /**
     * Payment status response
     */
    data class PaymentStatusResponse(
        val paymentId: String,
        val status: String, // "PENDING", "PROCESSING", "APPROVED", "DECLINED", "CANCELLED"
        val terminalId: String,
        val amount: Long,
        val currency: String,
        val authorizationCode: String? = null,
        val transactionId: String? = null,
        val cardType: String? = null,
        val maskedPan: String? = null,
        val errorCode: String? = null,
        val errorMessage: String? = null,
        val completedAt: Instant? = null
    )
    
    /**
     * Cancel response
     */
    data class CancelResponse(
        val success: Boolean,
        val message: String? = null
    )
}

/**
 * Exception thrown when Nets API communication fails
 */
class NetsApiException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
