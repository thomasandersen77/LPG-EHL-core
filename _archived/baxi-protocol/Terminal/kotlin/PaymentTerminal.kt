package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

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

/**
 * Payment Result
 */
data class PaymentResult(
    val status: PaymentStatus,
    val amount: Int,
    val receiptText: String? = null,
    val transactionId: String? = null,
    val authCode: String? = null,
    val errorMessage: String? = null
)

/**
 * Payment Terminal Interface
 * 
 * Abstract interface for payment terminal communication.
 * Follows Interface Segregation Principle - minimal interface for payment operations.
 */
interface PaymentTerminal : Closeable {
    /**
     * Connect to the payment terminal
     * @return true if connection successful
     */
    fun connect(): Boolean
    
    /**
     * Disconnect from the payment terminal
     */
    fun disconnect()
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
    
    /**
     * Request payment (purchase)
     * 
     * @param amount Amount in øre/cents (100 = 1.00 NOK)
     * @return Payment result
     */
    fun requestPayment(amount: Int): PaymentResult
    
    /**
     * Request pre-authorization (reserve amount)
     * 
     * Used for fuel pumps: reserve max amount before pumping.
     * 
     * @param amount Maximum amount to reserve
     * @return Payment result with transactionId for later capture
     */
    fun requestPreauth(amount: Int): PaymentResult
    
    /**
     * Capture a pre-authorized amount
     * 
     * Called after pumping is complete with actual amount.
     * 
     * @param amount Actual amount to capture
     * @param transactionId Reference from preauth
     * @return Payment result
     */
    fun capturePreauth(amount: Int, transactionId: String): PaymentResult
    
    /**
     * Cancel current payment
     * @return true if cancel successful
     */
    fun cancelPayment(): Boolean
    
    override fun close() = disconnect()
}

/**
 * TCP Payment Terminal
 * 
 * Communicates with payment terminal over TCP/IP using BAX protocol.
 * Uses PaymentTerminalClient for robust TCP handling.
 * 
 * @property host Terminal IP address
 * @property port Terminal port (default 8009 for ECR)
 * @property connectTimeoutMs Connection timeout in milliseconds
 * @property readTimeoutMs Read timeout for responses
 */
class TcpPaymentTerminal(
    private val host: String,
    private val port: Int = 8009,
    private val connectTimeoutMs: Int = 5000,
    private val readTimeoutMs: Int = 30000
) : PaymentTerminal {
    
    private val logger = LoggerFactory.getLogger(TcpPaymentTerminal::class.java)
    private var client: PaymentTerminalClient? = null
    
    override fun connect(): Boolean {
        return try {
            logger.info("Connecting to payment terminal at $host:$port")
            client = PaymentTerminalClient(host, port, connectTimeoutMs, readTimeoutMs)
            client!!.connect()
            logger.info("Connected. Local address: ${client!!.localAddress}")
            true
        } catch (e: IOException) {
            logger.error("Failed to connect to payment terminal: ${e.message}")
            client = null
            false
        }
    }
    
    override fun disconnect() {
        try {
            client?.close()
            logger.info("Disconnected from payment terminal")
        } finally {
            client = null
        }
    }
    
    override fun isConnected(): Boolean = client?.isConnected == true
    
    override fun requestPayment(amount: Int): PaymentResult {
        val connectedClient = client
        if (connectedClient == null || !connectedClient.isConnected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected to terminal"
            )
        }
        
        return try {
            logger.info("Requesting payment: ${amount / 100.0} NOK")
            
            val command = NetsBaxProtocol.createPurchaseCommand(amount)
            val response = connectedClient.sendCommand(command)
            
            parsePaymentResponse(response, amount)
            
        } catch (e: IOException) {
            logger.error("Payment I/O error: ${e.message}")
            PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Communication error: ${e.message}"
            )
        }
    }
    
    override fun requestPreauth(amount: Int): PaymentResult {
        val connectedClient = client
        if (connectedClient == null || !connectedClient.isConnected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected to terminal"
            )
        }
        
        return try {
            logger.info("Requesting preauth: ${amount / 100.0} NOK")
            
            val command = NetsBaxProtocol.createPreauthCommand(amount)
            val response = connectedClient.sendCommand(command)
            
            parsePaymentResponse(response, amount)
            
        } catch (e: IOException) {
            logger.error("Preauth I/O error: ${e.message}")
            PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Communication error: ${e.message}"
            )
        }
    }
    
    override fun capturePreauth(amount: Int, transactionId: String): PaymentResult {
        val connectedClient = client
        if (connectedClient == null || !connectedClient.isConnected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected to terminal"
            )
        }
        
        return try {
            logger.info("Capturing preauth $transactionId: ${amount / 100.0} NOK")
            
            val command = NetsBaxProtocol.createCaptureCommand(amount, transactionId)
            val response = connectedClient.sendCommand(command)
            
            parsePaymentResponse(response, amount)
            
        } catch (e: IOException) {
            logger.error("Capture I/O error: ${e.message}")
            PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Communication error: ${e.message}"
            )
        }
    }
    
    override fun cancelPayment(): Boolean {
        val connectedClient = client ?: return false
        if (!connectedClient.isConnected) return false
        
        return try {
            logger.info("Cancelling payment")
            val command = NetsBaxProtocol.createCancelCommand()
            connectedClient.sendCommand(command)
            true
        } catch (e: IOException) {
            logger.error("Failed to cancel payment: ${e.message}")
            false
        }
    }
    
    /**
     * Parse terminal response into PaymentResult
     */
    private fun parsePaymentResponse(response: TerminalResponse, amount: Int): PaymentResult {
        logger.debug("Parsing response: ${response.toHexString()}")
        
        // Handle empty response (likely ECR whitelist issue)
        if (response.rawData.isEmpty()) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "No response from terminal. Check ECR IP whitelist (${client?.localAddress})"
            )
        }
        
        // Handle timeout without data
        if (!response.hasAck && !response.hasNak && !response.hasCompleteFrame) {
            return PaymentResult(
                status = PaymentStatus.TIMEOUT,
                amount = amount,
                errorMessage = "Terminal timeout - no valid response received"
            )
        }
        
        // Parse the BAX protocol response
        val baxResponse = response.parse()
        
        return when (baxResponse) {
            is BaxResponse.Success -> {
                PaymentResult(
                    status = PaymentStatus.APPROVED,
                    amount = amount,
                    transactionId = baxResponse.transactionId,
                    authCode = baxResponse.authCode,
                    receiptText = "Payment approved\n${baxResponse.payload}"
                )
            }
            
            is BaxResponse.Ack -> {
                // ACK alone means terminal accepted command but no final result yet
                // This typically means we need to poll or wait for user to complete on terminal
                PaymentResult(
                    status = PaymentStatus.PENDING,
                    amount = amount,
                    receiptText = "Terminal acknowledged - check terminal display"
                )
            }
            
            is BaxResponse.Nak -> {
                PaymentResult(
                    status = PaymentStatus.DECLINED,
                    amount = amount,
                    errorMessage = "Terminal rejected command (NAK)"
                )
            }
            
            is BaxResponse.Data -> {
                // Data response - might be status update
                PaymentResult(
                    status = PaymentStatus.PENDING,
                    amount = amount,
                    receiptText = "Terminal data: ${baxResponse.payload}"
                )
            }
            
            is BaxResponse.Error -> {
                PaymentResult(
                    status = PaymentStatus.ERROR,
                    amount = amount,
                    errorMessage = baxResponse.message
                )
            }
            
            is BaxResponse.Incomplete -> {
                PaymentResult(
                    status = PaymentStatus.ERROR,
                    amount = amount,
                    errorMessage = "Incomplete response from terminal"
                )
            }
            
            is BaxResponse.Unknown -> {
                PaymentResult(
                    status = PaymentStatus.ERROR,
                    amount = amount,
                    errorMessage = "Unknown response: $baxResponse"
                )
            }
        }
    }
}

/**
 * Payment Terminal Simulator
 * 
 * Simulates payment terminal for testing without hardware.
 * Useful for development and integration testing.
 */
class SimulatedPaymentTerminal(
    private val autoApprove: Boolean = true,
    private val simulatedDelay: Long = 2000
) : PaymentTerminal {
    
    private val logger = LoggerFactory.getLogger(SimulatedPaymentTerminal::class.java)
    private var connected = false
    private var lastTransactionId: String? = null
    
    override fun connect(): Boolean {
        logger.info("Simulated payment terminal connected")
        connected = true
        return true
    }
    
    override fun disconnect() {
        logger.info("Simulated payment terminal disconnected")
        connected = false
    }
    
    override fun isConnected(): Boolean = connected
    
    override fun requestPayment(amount: Int): PaymentResult {
        return processPayment(amount, "Payment")
    }
    
    override fun requestPreauth(amount: Int): PaymentResult {
        val result = processPayment(amount, "Preauth")
        if (result.status == PaymentStatus.APPROVED) {
            lastTransactionId = result.transactionId
        }
        return result
    }
    
    override fun capturePreauth(amount: Int, transactionId: String): PaymentResult {
        if (!connected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected"
            )
        }
        
        logger.info("Simulated capture for $transactionId: ${amount / 100.0} kr")
        Thread.sleep(simulatedDelay / 2)
        
        return if (autoApprove) {
            logger.info("Simulated capture approved")
            PaymentResult(
                status = PaymentStatus.APPROVED,
                amount = amount,
                transactionId = transactionId,
                authCode = "SIM-AUTH-${System.currentTimeMillis() % 1000}",
                receiptText = "*** SIMULATED CAPTURE ***\nOriginal: $transactionId\nAmount: ${amount / 100.0} kr\nAPPROVED"
            )
        } else {
            PaymentResult(
                status = PaymentStatus.DECLINED,
                amount = amount,
                errorMessage = "Simulated capture decline"
            )
        }
    }
    
    override fun cancelPayment(): Boolean {
        logger.info("Simulated payment cancelled")
        return true
    }
    
    private fun processPayment(amount: Int, type: String): PaymentResult {
        if (!connected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected"
            )
        }
        
        logger.info("Simulated $type: ${amount / 100.0} kr")
        Thread.sleep(simulatedDelay)
        
        return if (autoApprove) {
            val txnId = "SIM-${System.currentTimeMillis()}"
            logger.info("Simulated $type approved: $txnId")
            PaymentResult(
                status = PaymentStatus.APPROVED,
                amount = amount,
                transactionId = txnId,
                authCode = "SIM-AUTH-${System.currentTimeMillis() % 1000}",
                receiptText = "*** SIMULATED $type ***\nAmount: ${amount / 100.0} kr\nAPPROVED"
            )
        } else {
            logger.info("Simulated $type declined")
            PaymentResult(
                status = PaymentStatus.DECLINED,
                amount = amount,
                errorMessage = "Simulated decline"
            )
        }
    }
}
