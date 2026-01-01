package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Fault Model Tests")
class EhlFaultTest {
    
    @Test
    @DisplayName("E-01 should parse to WARNING level")
    fun testE01ParsesCorrectly() {
        val fault = EhlFault.fromDisplayCode("E-01")
        
        assertEquals(EhlFault.E_01, fault)
        assertEquals("E-01", fault.code)
        assertEquals(EhlErrorLevel.WARNING, fault.level)
        assertTrue(fault.autoRetryable)
    }
    
    @Test
    @DisplayName("E-05 should parse to CRITICAL level")
    fun testE05ParsesToCritical() {
        val fault = EhlFault.fromDisplayCode("E-05")
        
        assertEquals(EhlFault.E_05, fault)
        assertEquals("E-05", fault.code)
        assertEquals(EhlErrorLevel.CRITICAL, fault.level)
        assertFalse(fault.autoRetryable)
    }
    
    @Test
    @DisplayName("Whitespace and case variations should normalize correctly")
    fun testNormalization() {
        // Test various whitespace and case combinations
        assertEquals(EhlFault.E_05, EhlFault.fromDisplayCode(" e-05 \r\n"))
        assertEquals(EhlFault.E_01, EhlFault.fromDisplayCode("e-01"))
        assertEquals(EhlFault.E_02, EhlFault.fromDisplayCode("E 02"))
        assertEquals(EhlFault.E_06, EhlFault.fromDisplayCode("  E-06  "))
        assertEquals(EhlFault.E_07, EhlFault.fromDisplayCode("e 07"))
    }
    
    @Test
    @DisplayName("Unknown fault codes should return UNKNOWN")
    fun testUnknownFaultCodes() {
        assertEquals(EhlFault.UNKNOWN, EhlFault.fromDisplayCode("E-99"))
        assertEquals(EhlFault.UNKNOWN, EhlFault.fromDisplayCode("E-00"))
        assertEquals(EhlFault.UNKNOWN, EhlFault.fromDisplayCode("X-01"))
        assertEquals(EhlFault.UNKNOWN, EhlFault.fromDisplayCode("garbage"))
        assertEquals(EhlFault.UNKNOWN, EhlFault.fromDisplayCode(""))
    }
    
    @Test
    @DisplayName("isFaultCode should correctly identify fault patterns")
    fun testIsFaultCode() {
        // Valid fault codes
        assertTrue(EhlFault.isFaultCode("E-01"))
        assertTrue(EhlFault.isFaultCode("E-05"))
        assertTrue(EhlFault.isFaultCode("E-99"))
        assertTrue(EhlFault.isFaultCode("e-01"))
        assertTrue(EhlFault.isFaultCode("E01"))
        
        // Invalid patterns
        assertFalse(EhlFault.isFaultCode("Fn-05"))
        assertFalse(EhlFault.isFaultCode("12.50"))
        assertFalse(EhlFault.isFaultCode("E-1"))
        assertFalse(EhlFault.isFaultCode("E-"))
        assertFalse(EhlFault.isFaultCode("X-01"))
    }
    
    @Test
    @DisplayName("All critical faults should not be auto-retryable")
    fun testCriticalFaultsNotRetryable() {
        val criticalFaults = listOf(
            EhlFault.E_05, 
            EhlFault.E_06, 
            EhlFault.E_07, 
            EhlFault.E_08
        )
        
        criticalFaults.forEach { fault ->
            assertEquals(EhlErrorLevel.CRITICAL, fault.level, 
                "Expected $fault to be CRITICAL")
            assertFalse(fault.autoRetryable, 
                "Expected $fault to not be auto-retryable")
        }
    }
    
    @Test
    @DisplayName("All warning faults except UNKNOWN should be auto-retryable")
    fun testWarningFaultsRetryable() {
        val warningFaults = listOf(
            EhlFault.E_01, 
            EhlFault.E_02, 
            EhlFault.E_09
        )
        
        warningFaults.forEach { fault ->
            assertEquals(EhlErrorLevel.WARNING, fault.level,
                "Expected $fault to be WARNING")
            assertTrue(fault.autoRetryable,
                "Expected $fault to be auto-retryable")
        }
    }
    
    @Test
    @DisplayName("All fault codes should have descriptions and recommended actions")
    fun testFaultMetadataComplete() {
        EhlFault.entries.forEach { fault ->
            assertNotNull(fault.code, "Fault code should not be null")
            assertTrue(fault.code.isNotEmpty(), "Fault code should not be empty")
            assertTrue(fault.description.isNotEmpty(), 
                "Fault ${fault.code} should have description")
            assertTrue(fault.recommendedAction.isNotEmpty(), 
                "Fault ${fault.code} should have recommended action")
        }
    }
}
