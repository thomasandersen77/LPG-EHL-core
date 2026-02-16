package no.cloudberries.lpg.api.payment

import no.cloudberries.lpg.api.config.NetsCloudConfig
import no.cloudberries.lpg.api.integration.NetsApiException
import no.cloudberries.lpg.api.integration.NetsCloudClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for NetsCloudPaymentGateway
 * 
 * Tests polling logic, timeout handling, and status mapping
 * using mocked NetsCloudClient.
 */
class NetsCloudPaymentGatewayTest {
    
    private fun createConfig(): NetsCloudConfig {
        return NetsCloudConfig(
            baseUrl = "https://test.api.nets.eu",
            username = "test_user",
            password = "test_password",
            terminalId = "42696609",
            merchantId = "TEST_MERCHANT",
            timeoutSeconds = 30,
            pollingIntervalMs = 10, // Fast polling for tests
            maxPollAttempts = 5,
            enabled = true
        )
    }
    
    @Test
    fun `startPayment for CASH should return APPROVED immediately`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CASH,
            reference = "TXN-001"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(PaymentMethod.CASH, payment.method)
        assertNotNull(payment.completedAt)
        verifyNoInteractions(client) // No API calls for cash
    }
    
    @Test
    fun `startPayment for CARD should poll until APPROVED`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        // Mock initiate sale
        whenever(client.initiateSale(any(), any())).thenReturn(
            NetsCloudClient.PaymentResponse(
                paymentId = "PAY-123",
                status = "PENDING",
                terminalId = "42696609",
                amount = 10000,
                currency = "NOK"
            )
        )
        
        // Mock status polling: PENDING → PROCESSING → APPROVED
        whenever(client.checkPaymentStatus("PAY-123"))
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-123",
                    status = "PENDING",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK"
                )
            )
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-123",
                    status = "PROCESSING",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK"
                )
            )
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-123",
                    status = "APPROVED",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK",
                    authorizationCode = "AUTH-789",
                    transactionId = "TXN-NETS-001"
                )
            )
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-001"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(PaymentMethod.CARD, payment.method)
        assertEquals("PAY-123", payment.metadata["nets_payment_id"])
        assertEquals("AUTH-789", payment.metadata["nets_auth_code"])
        assertEquals("TXN-NETS-001", payment.metadata["nets_transaction_id"])
        assertNotNull(payment.completedAt)
        
        // Verify polling happened
        verify(client).initiateSale(10000, "TXN-001")
        verify(client, times(3)).checkPaymentStatus("PAY-123")
    }
    
    @Test
    fun `startPayment for CARD should return DECLINED on terminal decline`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        whenever(client.initiateSale(any(), any())).thenReturn(
            NetsCloudClient.PaymentResponse(
                paymentId = "PAY-456",
                status = "PENDING",
                terminalId = "42696609",
                amount = 10000,
                currency = "NOK"
            )
        )
        
        // Mock immediate decline
        whenever(client.checkPaymentStatus("PAY-456"))
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-456",
                    status = "DECLINED",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK",
                    errorCode = "51",
                    errorMessage = "Insufficient funds"
                )
            )
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-002"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertNotNull(payment.completedAt)
        
        verify(client).checkPaymentStatus("PAY-456")
    }
    
    @Test
    fun `startPayment should handle timeout and cancel payment`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        whenever(client.initiateSale(any(), any())).thenReturn(
            NetsCloudClient.PaymentResponse(
                paymentId = "PAY-TIMEOUT",
                status = "PENDING",
                terminalId = "42696609",
                amount = 10000,
                currency = "NOK"
            )
        )
        
        // Mock status always returning PENDING (simulating timeout)
        whenever(client.checkPaymentStatus("PAY-TIMEOUT"))
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-TIMEOUT",
                    status = "PENDING",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK"
                )
            )
        
        whenever(client.cancelPayment(any())).thenReturn(true)
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-TIMEOUT"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.CANCELLED, payment.status)
        assertEquals("Payment timeout", payment.metadata["error"])
        assertNotNull(payment.completedAt)
        
        // Verify timeout behavior
        verify(client).initiateSale(10000, "TXN-TIMEOUT")
        verify(client, times(config.maxPollAttempts)).checkPaymentStatus("PAY-TIMEOUT")
        verify(client).cancelPayment("PAY-TIMEOUT")
    }
    
    @Test
    fun `startPayment should handle API error gracefully`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        // Mock API error
        whenever(client.initiateSale(any(), any()))
            .thenThrow(NetsApiException("Network error"))
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-ERROR"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertEquals("Failed to initiate sale: Network error", payment.metadata["error"])
        assertNotNull(payment.completedAt)
    }
    
    @Test
    fun `startPayment should map CANCELLED status correctly`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        whenever(client.initiateSale(any(), any())).thenReturn(
            NetsCloudClient.PaymentResponse(
                paymentId = "PAY-CANCEL",
                status = "PENDING",
                terminalId = "42696609",
                amount = 10000,
                currency = "NOK"
            )
        )
        
        // User cancels on terminal
        whenever(client.checkPaymentStatus("PAY-CANCEL"))
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-CANCEL",
                    status = "CANCELLED",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK"
                )
            )
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-CANCEL"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.CANCELLED, payment.status)
        assertNotNull(payment.completedAt)
    }
    
    @Test
    fun `startPayment should stop polling on first terminal response`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        whenever(client.initiateSale(any(), any())).thenReturn(
            NetsCloudClient.PaymentResponse(
                paymentId = "PAY-FAST",
                status = "PENDING",
                terminalId = "42696609",
                amount = 10000,
                currency = "NOK"
            )
        )
        
        // Immediate approval on first poll
        whenever(client.checkPaymentStatus("PAY-FAST"))
            .thenReturn(
                NetsCloudClient.PaymentStatusResponse(
                    paymentId = "PAY-FAST",
                    status = "APPROVED",
                    terminalId = "42696609",
                    amount = 10000,
                    currency = "NOK",
                    authorizationCode = "AUTH-FAST"
                )
            )
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CARD,
            reference = "TXN-FAST"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.APPROVED, payment.status)
        
        // Should only poll once (no unnecessary polling)
        verify(client, times(1)).checkPaymentStatus("PAY-FAST")
    }
    
    @Test
    fun `startPayment for CREDIT should return DECLINED with not implemented message`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.CREDIT,
            reference = "TXN-CREDIT"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertEquals("Credit payment not implemented", payment.metadata["error"])
        verifyNoInteractions(client)
    }
    
    @Test
    fun `startPayment for VIPPS should return DECLINED with not implemented message`() {
        // Given
        val client = mock<NetsCloudClient>()
        val config = createConfig()
        val gateway = NetsCloudPaymentGateway(client, config)
        
        val request = PaymentRequest(
            amountCents = 10000,
            method = PaymentMethod.VIPPS,
            reference = "TXN-VIPPS"
        )
        
        // When
        val payment = gateway.startPayment(request)
        
        // Then
        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertEquals("Vipps payment not implemented", payment.metadata["error"])
        verifyNoInteractions(client)
    }
}
