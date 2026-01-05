package no.cloudberries.lpg.payment

/**
 * Payment Gateway Interface (Port)
 * 
 * Clean Architecture "Port" for payment processing.
 * Implementations (Adapters) can be:
 * - NetsCloudPaymentGateway (Production - real Nets Cloud Connect)
 * - SimulatedPaymentGateway (Lab - fake approvals for testing)
 * 
 * This interface allows swapping payment providers without changing business logic.
 */
interface PaymentGateway {
    
    /**
     * Initiate a payment
     * 
     * @param request Payment details
     * @return Payment result with status and transaction info
     */
    fun initiatePayment(request: PaymentRequest): PaymentResult
    
    /**
     * Cancel an ongoing payment
     * 
     * @param transactionId Transaction to cancel
     * @return Cancellation result
     */
    fun cancelPayment(transactionId: String): PaymentResult
    
    /**
     * Check payment status
     * 
     * @param transactionId Transaction to check
     * @return Current payment status
     */
    fun checkStatus(transactionId: String): PaymentStatus
    
    /**
     * Get gateway type (for logging/monitoring)
     */
    fun getGatewayType(): String
}

/**
 * Payment Request
 */
data class PaymentRequest(
    val amountCents: Int,
    val method: PaymentMethod,
    val reference: String
)

/**
 * Payment Method
 */
enum class PaymentMethod {
    CARD,
    CASH,
    STATION_CARD
}

/**
 * Payment Result
 */
data class PaymentResult(
    val success: Boolean,
    val transactionId: String?,
    val authCode: String? = null,
    val receiptText: String? = null,
    val errorMessage: String? = null,
    val status: PaymentStatus
)

/**
 * Payment Status
 */
enum class PaymentStatus {
    PENDING,
    APPROVED,
    DECLINED,
    CANCELLED,
    ERROR,
    TIMEOUT
}
