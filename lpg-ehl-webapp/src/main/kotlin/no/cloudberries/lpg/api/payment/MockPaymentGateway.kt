package no.cloudberries.lpg.api.payment

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Mock Payment Gateway for LAB mode
 * 
 * Simulates payment processing without real terminal hardware.
 */
@Component
@Profile("lab", "local", "default")
@Primary
class MockPaymentGateway : PaymentGateway {
    
    private val logger = LoggerFactory.getLogger(MockPaymentGateway::class.java)
    private val payments = ConcurrentHashMap<UUID, Payment>()
    private val random = Random.Default
    
    override fun startPayment(request: PaymentRequest): Payment {
        logger.info("💳 [MOCK] Starting payment: ${request.amountCents / 100.0} NOK via ${request.method}")
        
        // Simulate processing delay
        Thread.sleep(random.nextLong(500, 1500))
        
        // 90% success rate
        val approved = random.nextInt(100) < 90
        
        val payment = Payment(
            id = UUID.randomUUID(),
            requestedAt = Instant.now(),
            completedAt = Instant.now(),
            amountCents = request.amountCents,
            method = request.method,
            status = if (approved) PaymentStatus.APPROVED else PaymentStatus.DECLINED,
            reference = request.reference,
            metadata = request.metadata
        )
        
        payments[payment.id] = payment
        
        if (approved) {
            logger.info("✅ [MOCK] Payment APPROVED: ${payment.id}")
        } else {
            logger.warn("❌ [MOCK] Payment DECLINED: ${payment.id}")
        }
        
        return payment
    }
    
    override fun getPayment(id: UUID): Payment? {
        return payments[id]
    }
}
