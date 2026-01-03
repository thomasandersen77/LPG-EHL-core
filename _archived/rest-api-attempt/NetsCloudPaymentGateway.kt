package no.cloudberries.lpg.api.payment

import no.cloudberries.lpg.api.config.NetsCloudConfig
import no.cloudberries.lpg.api.integration.NetsApiException
import no.cloudberries.lpg.api.integration.NetsCloudClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Payment gateway implementation using Nets Cloud Connect API
 * 
 * This replaces the old TCP/Baxi protocol implementation with modern REST API calls.
 * 
 * Architecture:
 * - Terminal connects to Nets cloud (3.33.230.243:6001)
 * - Our application calls Nets REST API
 * - Nets relays payment requests/responses between us and the terminal
 * 
 * Benefits:
 * - No TCP socket management
 * - No hex/binary protocol encoding
 * - Nets handles terminal communication complexity
 * - Better reliability and monitoring
 */
@Service
@ConditionalOnProperty("nets.cloud-connect.enabled", havingValue = "true")
class NetsCloudPaymentGateway(
    private val netsClient: NetsCloudClient,
    private val netsConfig: NetsCloudConfig
) : PaymentGateway {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Start a payment request
     * 
     * For CARD payments, this initiates the payment and polls until completion.
     * For CASH payments, returns immediately as APPROVED.
     * 
     * @param request Payment request with amount and method
     * @return Payment with final status
     */
    override fun startPayment(request: PaymentRequest): Payment {
        logger.info("Starting payment: method={}, amount={} øre", 
            request.method, request.amountCents)
        
        return when (request.method) {
            PaymentMethod.CASH -> handleCashPayment(request)
            PaymentMethod.CARD -> handleCardPayment(request)
            PaymentMethod.CREDIT -> handleCreditPayment(request)
            PaymentMethod.VIPPS -> handleVippsPayment(request)
        }
    }
    
    /**
     * Look up a payment by ID
     * 
     * Note: This currently doesn't persist payments. In production, you should
     * store payments in the database and retrieve them here.
     */
    override fun getPayment(id: UUID): Payment? {
        // TODO: Implement database persistence
        logger.warn("getPayment() not yet implemented with persistence")
        return null
    }
    
    /**
     * Handle cash payment - no terminal interaction needed
     */
    private fun handleCashPayment(request: PaymentRequest): Payment {
        logger.info("Cash payment - no terminal interaction")
        return Payment(
            amountCents = request.amountCents,
            method = PaymentMethod.CASH,
            status = PaymentStatus.APPROVED,
            reference = request.reference,
            metadata = request.metadata,
            completedAt = Instant.now()
        )
    }
    
    /**
     * Handle card payment via Nets Cloud Connect
     * 
     * Flow:
     * 1. Initiate payment via API
     * 2. Poll status until terminal completes
     * 3. Return final payment status
     */
    private fun handleCardPayment(request: PaymentRequest): Payment {
        val payment = Payment(
            amountCents = request.amountCents,
            method = PaymentMethod.CARD,
            status = PaymentStatus.PENDING,
            reference = request.reference,
            metadata = request.metadata
        )
        
        return try {
            // Step 1: Initiate payment
            val initiateResponse = netsClient.initiateSale(
                amountCents = request.amountCents,
                reference = request.reference
            )
            
            logger.info("Payment initiated: paymentId={}", initiateResponse.paymentId)
            
            // Step 2: Poll for completion
            val finalStatus = pollPaymentStatus(initiateResponse.paymentId)
            
            // Step 3: Map to our payment status
            payment.copy(
                status = mapNetsStatusToPaymentStatus(finalStatus.status),
                completedAt = finalStatus.completedAt ?: Instant.now(),
                metadata = payment.metadata + mapOf(
                    "nets_payment_id" to initiateResponse.paymentId,
                    "nets_transaction_id" to (finalStatus.transactionId ?: ""),
                    "nets_auth_code" to (finalStatus.authorizationCode ?: ""),
                    "card_type" to (finalStatus.cardType ?: ""),
                    "masked_pan" to (finalStatus.maskedPan ?: "")
                )
            )
        } catch (e: NetsApiException) {
            logger.error("Nets API error", e)
            payment.copy(
                status = PaymentStatus.DECLINED,
                completedAt = Instant.now(),
                metadata = payment.metadata + mapOf("error" to (e.message ?: "Unknown error"))
            )
        } catch (e: PaymentTimeoutException) {
            logger.error("Payment timeout", e)
            payment.copy(
                status = PaymentStatus.CANCELLED,
                completedAt = Instant.now(),
                metadata = payment.metadata + mapOf("error" to "Payment timeout")
            )
        }
    }
    
    /**
     * Poll payment status until completion or timeout
     * 
     * @param paymentId Nets payment ID
     * @return Final payment status response
     * @throws PaymentTimeoutException if max polling attempts exceeded
     */
    private fun pollPaymentStatus(paymentId: String): NetsCloudClient.PaymentStatusResponse {
        var attempts = 0
        
        while (attempts < netsConfig.maxPollAttempts) {
            val status = netsClient.checkPaymentStatus(paymentId)
            
            logger.debug("Poll attempt {}/{}: status={}", 
                attempts + 1, netsConfig.maxPollAttempts, status.status)
            
            when (status.status.uppercase()) {
                "APPROVED", "DECLINED", "CANCELLED", "ERROR" -> {
                    logger.info("Payment terminal: status={}", status.status)
                    return status
                }
                "PENDING", "PROCESSING" -> {
                    // Continue polling
                    Thread.sleep(netsConfig.pollingIntervalMs)
                    attempts++
                }
                else -> {
                    logger.warn("Unknown payment status: {}", status.status)
                    Thread.sleep(netsConfig.pollingIntervalMs)
                    attempts++
                }
            }
        }
        
        // Timeout - attempt to cancel
        logger.warn("Payment polling timeout after {} attempts", attempts)
        netsClient.cancelPayment(paymentId)
        throw PaymentTimeoutException("Payment timeout after ${attempts} polling attempts")
    }
    
    /**
     * Map Nets status codes to our PaymentStatus enum
     */
    private fun mapNetsStatusToPaymentStatus(netsStatus: String): PaymentStatus {
        return when (netsStatus.uppercase()) {
            "APPROVED" -> PaymentStatus.APPROVED
            "DECLINED", "ERROR" -> PaymentStatus.DECLINED
            "CANCELLED" -> PaymentStatus.CANCELLED
            "PENDING", "PROCESSING" -> PaymentStatus.PENDING
            else -> {
                logger.warn("Unknown Nets status: {}, treating as DECLINED", netsStatus)
                PaymentStatus.DECLINED
            }
        }
    }
    
    /**
     * Handle credit/account payment
     * TODO: Implement if station card/credit accounts are supported
     */
    private fun handleCreditPayment(request: PaymentRequest): Payment {
        logger.warn("CREDIT payment not yet implemented")
        return Payment(
            amountCents = request.amountCents,
            method = PaymentMethod.CREDIT,
            status = PaymentStatus.DECLINED,
            reference = request.reference,
            metadata = request.metadata + mapOf("error" to "Credit payment not implemented"),
            completedAt = Instant.now()
        )
    }
    
    /**
     * Handle Vipps payment
     * TODO: Implement if Vipps is supported via terminal or separate integration
     */
    private fun handleVippsPayment(request: PaymentRequest): Payment {
        logger.warn("VIPPS payment not yet implemented")
        return Payment(
            amountCents = request.amountCents,
            method = PaymentMethod.VIPPS,
            status = PaymentStatus.DECLINED,
            reference = request.reference,
            metadata = request.metadata + mapOf("error" to "Vipps payment not implemented"),
            completedAt = Instant.now()
        )
    }
}

/**
 * Exception thrown when payment polling times out
 */
class PaymentTimeoutException(message: String) : RuntimeException(message)
