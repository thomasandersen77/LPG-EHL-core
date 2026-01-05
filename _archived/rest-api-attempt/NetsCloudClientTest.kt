package no.cloudberries.lpg.api.integration

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.cloudberries.lpg.api.config.NetsCloudConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for NetsCloudClient using WireMock
 * 
 * These tests simulate the Nets Cloud Connect API without requiring
 * actual credentials or a live terminal.
 */
class NetsCloudClientTest {
    
    companion object {
        @JvmField
        @RegisterExtension
        val wireMock: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()
    }
    
    private fun createClient(): NetsCloudClient {
        val config = NetsCloudConfig(
            baseUrl = wireMock.baseUrl(),
            username = "test_user",
            password = "test_password",
            terminalId = "42696609",
            merchantId = "TEST_MERCHANT",
            timeoutSeconds = 30,
            pollingIntervalMs = 100,
            maxPollAttempts = 10,
            enabled = true
        )
        
        return NetsCloudClient(config, RestClient.builder())
    }
    
    @Test
    fun `initiateSale should return payment response with paymentId`() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/sale"))
            .withBasicAuth("test_user", "test_password")
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-123456",
                        "status": "PENDING",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val response = client.initiateSale(amountCents = 10000, reference = "TXN-001")
        
        // Then
        assertNotNull(response)
        assertEquals("PAY-123456", response.paymentId)
        assertEquals("PENDING", response.status)
        assertEquals("42696609", response.terminalId)
        assertEquals(10000L, response.amount)
        assertEquals("NOK", response.currency)
        
        // Verify request was made
        wireMock.verify(postRequestedFor(urlEqualTo("/sale"))
            .withBasicAuth(BasicCredentials("test_user", "test_password"))
            .withRequestBody(matchingJsonPath("$.terminalId", equalTo("42696609")))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("10000")))
            .withRequestBody(matchingJsonPath("$.reference", equalTo("TXN-001"))))
    }
    
    @Test
    fun `checkPaymentStatus should return status response`() {
        // Given - Payment is pending
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-123456"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-123456",
                        "status": "PENDING",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val response = client.checkPaymentStatus("PAY-123456")
        
        // Then
        assertNotNull(response)
        assertEquals("PAY-123456", response.paymentId)
        assertEquals("PENDING", response.status)
    }
    
    @Test
    fun `checkPaymentStatus should return approved status with transaction details`() {
        // Given - Payment is approved
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-123456"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-123456",
                        "status": "APPROVED",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK",
                        "authorizationCode": "AUTH-789",
                        "transactionId": "TXN-NETS-001",
                        "cardType": "VISA",
                        "maskedPan": "************1234"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val response = client.checkPaymentStatus("PAY-123456")
        
        // Then
        assertNotNull(response)
        assertEquals("APPROVED", response.status)
        assertEquals("AUTH-789", response.authorizationCode)
        assertEquals("TXN-NETS-001", response.transactionId)
        assertEquals("VISA", response.cardType)
        assertEquals("************1234", response.maskedPan)
    }
    
    @Test
    fun `checkPaymentStatus should return declined status with error`() {
        // Given - Payment is declined
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-123456"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-123456",
                        "status": "DECLINED",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK",
                        "errorCode": "51",
                        "errorMessage": "Insufficient funds"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val response = client.checkPaymentStatus("PAY-123456")
        
        // Then
        assertNotNull(response)
        assertEquals("DECLINED", response.status)
        assertEquals("51", response.errorCode)
        assertEquals("Insufficient funds", response.errorMessage)
    }
    
    @Test
    fun `cancelPayment should return success`() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/payments/PAY-123456/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "success": true,
                        "message": "Payment cancelled"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val success = client.cancelPayment("PAY-123456")
        
        // Then
        assertTrue(success)
    }
    
    @Test
    fun `cancelPayment should return false on failure`() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/payments/PAY-123456/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "success": false,
                        "message": "Payment already completed"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val success = client.cancelPayment("PAY-123456")
        
        // Then
        assertFalse(success)
    }
    
    @Test
    fun `initiateSale should handle 401 Unauthorized`() {
        // Given - Bad credentials
        wireMock.stubFor(post(urlEqualTo("/sale"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "Unauthorized",
                        "message": "Invalid credentials"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When/Then
        try {
            client.initiateSale(amountCents = 10000, reference = "TXN-001")
            throw AssertionError("Expected NetsApiException")
        } catch (e: NetsApiException) {
            assertTrue(e.message?.contains("Failed to initiate sale") == true)
        }
    }
    
    @Test
    fun `initiateSale should handle 500 Internal Server Error`() {
        // Given - Server error
        wireMock.stubFor(post(urlEqualTo("/sale"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "Internal Server Error",
                        "message": "Service temporarily unavailable"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When/Then
        try {
            client.initiateSale(amountCents = 10000, reference = "TXN-001")
            throw AssertionError("Expected NetsApiException")
        } catch (e: NetsApiException) {
            assertTrue(e.message?.contains("Failed to initiate sale") == true)
        }
    }
    
    @Test
    fun `initiateRefund should return payment response`() {
        // Given
        wireMock.stubFor(post(urlEqualTo("/refund"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "REF-789012",
                        "status": "PENDING",
                        "terminalId": "42696609",
                        "amount": 5000,
                        "currency": "NOK"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val response = client.initiateRefund(
            amountCents = 5000,
            originalTransactionId = "TXN-NETS-001"
        )
        
        // Then
        assertNotNull(response)
        assertEquals("REF-789012", response.paymentId)
        assertEquals("PENDING", response.status)
        assertEquals(5000L, response.amount)
        
        // Verify transactionType is refund
        wireMock.verify(postRequestedFor(urlEqualTo("/refund"))
            .withRequestBody(matchingJsonPath("$.transactionType", equalTo("20")))
            .withRequestBody(matchingJsonPath("$.originalTransactionId", equalTo("TXN-NETS-001"))))
    }
    
    @Test
    fun `full payment flow - pending to approved`() {
        // Given - Initial payment
        wireMock.stubFor(post(urlEqualTo("/sale"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-FLOW-001",
                        "status": "PENDING",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK"
                    }
                """.trimIndent())))
        
        // First poll - still pending
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-FLOW-001"))
            .inScenario("Payment Flow")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-FLOW-001",
                        "status": "PENDING",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK"
                    }
                """.trimIndent()))
            .willSetStateTo("Processing"))
        
        // Second poll - processing
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-FLOW-001"))
            .inScenario("Payment Flow")
            .whenScenarioStateIs("Processing")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-FLOW-001",
                        "status": "PROCESSING",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK"
                    }
                """.trimIndent()))
            .willSetStateTo("Approved"))
        
        // Third poll - approved
        wireMock.stubFor(get(urlEqualTo("/payments/PAY-FLOW-001"))
            .inScenario("Payment Flow")
            .whenScenarioStateIs("Approved")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "paymentId": "PAY-FLOW-001",
                        "status": "APPROVED",
                        "terminalId": "42696609",
                        "amount": 10000,
                        "currency": "NOK",
                        "authorizationCode": "AUTH-123",
                        "transactionId": "TXN-NETS-FLOW-001"
                    }
                """.trimIndent())))
        
        val client = createClient()
        
        // When
        val initResponse = client.initiateSale(10000, "TXN-FLOW-001")
        assertEquals("PENDING", initResponse.status)
        
        val pollResponse1 = client.checkPaymentStatus("PAY-FLOW-001")
        assertEquals("PENDING", pollResponse1.status)
        
        val pollResponse2 = client.checkPaymentStatus("PAY-FLOW-001")
        assertEquals("PROCESSING", pollResponse2.status)
        
        val pollResponse3 = client.checkPaymentStatus("PAY-FLOW-001")
        assertEquals("APPROVED", pollResponse3.status)
        assertEquals("AUTH-123", pollResponse3.authorizationCode)
        
        // Verify number of status checks
        wireMock.verify(3, getRequestedFor(urlEqualTo("/payments/PAY-FLOW-001")))
    }
}
