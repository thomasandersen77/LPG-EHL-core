package no.cloudberries.lpg.payment

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

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
    val errorMessage: String? = null
)

/**
 * Payment Terminal Interface
 * 
 * Abstract interface for payment terminal communication.
 */
interface PaymentTerminal {
    /**
     * Connect to the payment terminal
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
     * Request payment
     * 
     * @param amount Amount in øre/cents
     * @return Payment result
     */
    fun requestPayment(amount: Int): PaymentResult
    
    /**
     * Cancel current payment
     */
    fun cancelPayment(): Boolean
}

/**
 * TCP Payment Terminal
 * 
 * Communicates with payment terminal over TCP/IP using ECR protocol.
 * 
 * @property host Terminal IP address
 * @property port Terminal port (typically 8009)
 * @property timeout Socket timeout in milliseconds
 */
class TcpPaymentTerminal(
    private val host: String,
    private val port: Int,
    private val timeout: Int = 30000
) : PaymentTerminal {
    
    private val logger = LoggerFactory.getLogger(TcpPaymentTerminal::class.java)
    private var socket: Socket? = null
    
    override fun connect(): Boolean {
        try {
            logger.info("Connecting to payment terminal at $host:$port")
            socket = Socket(host, port).apply {
                soTimeout = timeout
                tcpNoDelay = true
            }
            logger.info("Connected to payment terminal")
            return true
        } catch (e: IOException) {
            logger.error("Failed to connect to payment terminal: ${e.message}")
            socket = null
            return false
        }
    }
    
    override fun disconnect() {
        try {
            socket?.close()
            logger.info("Disconnected from payment terminal")
        } catch (e: IOException) {
            logger.warn("Error closing socket: ${e.message}")
        } finally {
            socket = null
        }
    }
    
    override fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false
    }
    
    override fun requestPayment(amount: Int): PaymentResult {
        if (!isConnected()) {
            logger.error("Not connected to payment terminal")
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected"
            )
        }
        
        try {
            logger.info("Requesting payment: ${amount / 100.0} kr")
            
            // Send payment request (ECR protocol)
            val request = buildPaymentRequest(amount)
            sendData(request)
            
            // Wait for response
            val response = receiveData()
            val result = parsePaymentResponse(response, amount)
            
            logger.info("Payment result: ${result.status}")
            return result
            
        } catch (e: SocketTimeoutException) {
            logger.error("Payment timeout")
            return PaymentResult(
                status = PaymentStatus.TIMEOUT,
                amount = amount,
                errorMessage = "Timeout"
            )
        } catch (e: IOException) {
            logger.error("Payment I/O error: ${e.message}")
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = e.message
            )
        }
    }
    
    override fun cancelPayment(): Boolean {
        if (!isConnected()) return false
        
        try {
            logger.info("Cancelling payment")
            val request = buildCancelRequest()
            sendData(request)
            return true
        } catch (e: IOException) {
            logger.error("Failed to cancel payment: ${e.message}")
            return false
        }
    }
    
    /**
     * Build ECR payment request
     * 
     * Simple text-based protocol for testing.
     * In production, this would be replaced with actual ECR protocol.
     */
    private fun buildPaymentRequest(amount: Int): ByteArray {
        val amountStr = String.format("%012d", amount)
        val message = "PAY:$amountStr\n"
        return message.toByteArray(StandardCharsets.UTF_8)
    }
    
    /**
     * Build ECR cancel request
     */
    private fun buildCancelRequest(): ByteArray {
        return "CANCEL\n".toByteArray(StandardCharsets.UTF_8)
    }
    
    /**
     * Send data to terminal
     */
    private fun sendData(data: ByteArray) {
        val outputStream = socket?.getOutputStream() 
            ?: throw IOException("Socket not connected")
        
        outputStream.write(data)
        outputStream.flush()
        
        logger.debug("Sent ${data.size} bytes: ${String(data, StandardCharsets.UTF_8).trim()}")
    }
    
    /**
     * Receive data from terminal
     */
    private fun receiveData(): ByteArray {
        val inputStream = socket?.getInputStream() 
            ?: throw IOException("Socket not connected")
        
        val buffer = ByteArray(4096)
        val bytesRead = inputStream.read(buffer)
        
        if (bytesRead <= 0) {
            throw IOException("No data received")
        }
        
        val data = buffer.copyOf(bytesRead)
        logger.debug("Received $bytesRead bytes: ${String(data, StandardCharsets.UTF_8).trim()}")
        
        return data
    }
    
    /**
     * Parse payment response
     */
    private fun parsePaymentResponse(data: ByteArray, amount: Int): PaymentResult {
        val response = String(data, StandardCharsets.UTF_8).trim()
        
        // Simple text-based protocol for testing
        return when {
            response.startsWith("OK:") -> {
                val parts = response.split(":")
                PaymentResult(
                    status = PaymentStatus.APPROVED,
                    amount = amount,
                    transactionId = parts.getOrNull(1),
                    receiptText = "Payment approved"
                )
            }
            response.startsWith("DECLINED") -> {
                PaymentResult(
                    status = PaymentStatus.DECLINED,
                    amount = amount,
                    errorMessage = "Payment declined"
                )
            }
            response.startsWith("ERROR") -> {
                PaymentResult(
                    status = PaymentStatus.ERROR,
                    amount = amount,
                    errorMessage = response.substringAfter(":", "Unknown error")
                )
            }
            else -> {
                PaymentResult(
                    status = PaymentStatus.ERROR,
                    amount = amount,
                    errorMessage = "Unknown response: $response"
                )
            }
        }
    }
}

/**
 * Payment Terminal Simulator
 * 
 * Simulates payment terminal for testing without hardware.
 */
class SimulatedPaymentTerminal(
    private val autoApprove: Boolean = true,
    private val simulatedDelay: Long = 2000
) : PaymentTerminal {
    
    private val logger = LoggerFactory.getLogger(SimulatedPaymentTerminal::class.java)
    private var connected = false
    
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
        if (!connected) {
            return PaymentResult(
                status = PaymentStatus.ERROR,
                amount = amount,
                errorMessage = "Not connected"
            )
        }
        
        logger.info("Simulated payment: ${amount / 100.0} kr")
        
        // Simulate payment processing delay
        Thread.sleep(simulatedDelay)
        
        return if (autoApprove) {
            logger.info("Simulated payment approved")
            PaymentResult(
                status = PaymentStatus.APPROVED,
                amount = amount,
                transactionId = "SIM-${System.currentTimeMillis()}",
                receiptText = "*** SIMULATED PAYMENT ***\nAmount: ${amount / 100.0} kr\nAPPROVED"
            )
        } else {
            logger.info("Simulated payment declined")
            PaymentResult(
                status = PaymentStatus.DECLINED,
                amount = amount,
                errorMessage = "Simulated decline"
            )
        }
    }
    
    override fun cancelPayment(): Boolean {
        logger.info("Simulated payment cancelled")
        return true
    }
}
