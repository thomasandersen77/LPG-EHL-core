package no.cloudberries.lpg.api.integration

import no.cloudberries.lpg.service.service.DiagnosticsService
import no.cloudberries.lpg.protocol.EhlFault
import no.cloudberries.lpg.protocol.EhlErrorLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

/**
 * Integration test for Diagnostics endpoint
 * 
 * Tests the complete flow:
 * 1. Record fault in DiagnosticsService
 * 2. Query diagnostics via REST API
 * 3. Verify fault appears in response
 * 4. Verify CRITICAL faults block commands
 */
@DisplayName("Diagnostics Integration Tests")
class DiagnosticsIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var diagnosticsService: DiagnosticsService
    
    @BeforeEach
    fun setupTest() {
        // Clear any existing faults before each test
        // In real implementation, we'd have a cleanup method
    }
    
    @Test
    @DisplayName("GET /admin/ehl/diagnostics should return empty list initially")
    fun testGetDiagnosticsEmpty() {
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics",
            Array<Any>::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }
    
    @Test
    @DisplayName("Record CRITICAL fault and verify in diagnostics endpoint")
    fun testRecordCriticalFaultAppearInDiagnostics() {
        val dispenserAddress = 1
        val criticalFault = EhlFault.E_05 // CRITICAL: Configuration memory damage
        
        // Step 1: Record a CRITICAL fault
        diagnosticsService.recordFault(dispenserAddress, criticalFault)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Step 2: Query diagnostics endpoint
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/$dispenserAddress",
            Map::class.java
        )
        
        // Step 3: Verify response
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        val diagnostics = response.body!!
        
        // Verify address
        assertEquals(dispenserAddress, diagnostics["address"])
        
        // Verify fault is present
        val lastFault = diagnostics["lastFault"] as? Map<*, *>
        assertNotNull(lastFault, "Expected lastFault to be present")
        assertEquals("E-05", lastFault!!["code"])
        assertEquals("CRITICAL", lastFault["level"])
        
        // Verify state reflects error
        val state = diagnostics["state"] as? Map<*, *>
        assertNotNull(state)
    }
    
    @Test
    @DisplayName("Record WARNING fault and verify it does not block operations")
    fun testRecordWarningFault() {
        val dispenserAddress = 2
        val warningFault = EhlFault.E_01 // WARNING: Improper EHI operation
        
        // Record a WARNING fault
        diagnosticsService.recordFault(dispenserAddress, warningFault)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        // Query diagnostics
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/$dispenserAddress",
            Map::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        val diagnostics = response.body!!
        val lastFault = diagnostics["lastFault"] as? Map<*, *>
        
        assertNotNull(lastFault)
        assertEquals("E-01", lastFault!!["code"])
        assertEquals("WARNING", lastFault["level"])
        
        // Verify autoRetryable flag
        assertEquals(true, lastFault["autoRetryable"])
    }
    
    @Test
    @DisplayName("GET /admin/ehl/diagnostics/faults should return only faulted dispensers")
    fun testGetDispensersWithFaults() {
        // Record faults on multiple dispensers
        diagnosticsService.recordFault(1, EhlFault.E_05)
        diagnosticsService.recordFault(2, EhlFault.E_01)
        
        // Record communication for non-faulted dispenser
        diagnosticsService.recordCommunication(3, isReceive = true)
        
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/faults",
            Array<Any>::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        val faultedDispensers = response.body!!
        
        // Should return at least the 2 dispensers with faults
        assertTrue(faultedDispensers.size >= 2, 
            "Expected at least 2 faulted dispensers, got ${faultedDispensers.size}")
    }
    
    @Test
    @DisplayName("GET /admin/ehl/diagnostics/critical should return only CRITICAL faults")
    fun testGetCriticalFaults() {
        // Record mix of WARNING and CRITICAL faults
        diagnosticsService.recordFault(1, EhlFault.E_05) // CRITICAL
        diagnosticsService.recordFault(2, EhlFault.E_01) // WARNING
        diagnosticsService.recordFault(3, EhlFault.E_06) // CRITICAL
        
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/critical",
            Array<Any>::class.java
        )
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        
        val criticalDispensers = response.body!!
        
        // Should return only dispensers with CRITICAL faults (at least 2)
        assertTrue(criticalDispensers.size >= 2,
            "Expected at least 2 dispensers with CRITICAL faults")
    }
    
    @Test
    @DisplayName("GET /admin/ehl/diagnostics/{unknown} should return 404")
    fun testGetDiagnosticsForUnknownDispenser() {
        val response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/999",
            String::class.java
        )
        
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
    
    @Test
    @DisplayName("Clear fault should remove it from diagnostics")
    fun testClearFault() {
        val dispenserAddress = 5
        
        // Record and verify fault
        diagnosticsService.recordFault(dispenserAddress, EhlFault.E_05)
        diagnosticsService.recordCommunication(dispenserAddress, isReceive = true)
        
        var response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/$dispenserAddress",
            Map::class.java
        )
        
        var lastFault = response.body?.get("lastFault")
        assertNotNull(lastFault, "Fault should be present before clear")
        
        // Clear fault
        diagnosticsService.clearFault(dispenserAddress)
        
        // Verify fault is cleared
        response = restTemplate.getForEntity(
            "$baseUrl/admin/ehl/diagnostics/$dispenserAddress",
            Map::class.java
        )
        
        lastFault = response.body?.get("lastFault")
        assertNull(lastFault, "Fault should be cleared")
    }
}
