package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Display Parser Tests")
class EhlDisplayParserTest {
    
    @Test
    @DisplayName("Parse fault code from display data")
    fun testParseFaultCode() {
        val displayData = "E-05".toByteArray()
        
        val result = EhlDisplayParser.parseDisplayData(displayData)
        
        assertTrue(result is DisplayParseResult.Fault)
        val fault = (result as DisplayParseResult.Fault).fault
        assertEquals(EhlFault.E_05, fault)
        assertEquals(EhlErrorLevel.CRITICAL, fault.level)
    }
    
    @Test
    @DisplayName("Parse E-01 warning fault")
    fun testParseWarningFault() {
        val displayData = "E-01".toByteArray()
        
        val result = EhlDisplayParser.parseDisplayData(displayData)
        
        assertTrue(result is DisplayParseResult.Fault)
        val fault = (result as DisplayParseResult.Fault).fault
        assertEquals(EhlFault.E_01, fault)
        assertEquals(EhlErrorLevel.WARNING, fault.level)
    }
    
    @Test
    @DisplayName("Parse fault with whitespace variations")
    fun testParseFaultWithWhitespace() {
        val testCases = listOf(
            "E 05",
            " E-05 ",
            "e-05",
            "E-05\r\n"
        )
        
        testCases.forEach { displayText ->
            val result = EhlDisplayParser.parseDisplayData(displayText.toByteArray())
            
            assertTrue(result is DisplayParseResult.Fault, 
                "Expected fault for: '$displayText'")
            val fault = (result as DisplayParseResult.Fault).fault
            assertEquals(EhlFault.E_05, fault, 
                "Expected E-05 for: '$displayText'")
        }
    }
    
    @Test
    @DisplayName("Normal display data should return Normal result")
    fun testNormalDisplayData() {
        val normalData = listOf(
            "15.90",
            "12.34",
            "0000",
            "Fn-05",
            "Ready"
        )
        
        normalData.forEach { text ->
            val result = EhlDisplayParser.parseDisplayData(text.toByteArray())
            
            assertTrue(result is DisplayParseResult.Normal,
                "Expected Normal for: '$text'")
        }
    }
    
    @Test
    @DisplayName("Empty display data should return Normal")
    fun testEmptyDisplayData() {
        val result = EhlDisplayParser.parseDisplayData(ByteArray(0))
        
        assertTrue(result is DisplayParseResult.Normal)
    }
    
    @Test
    @DisplayName("containsFaultCode should detect fault patterns")
    fun testContainsFaultCode() {
        assertTrue(EhlDisplayParser.containsFaultCode("E-05".toByteArray()))
        assertTrue(EhlDisplayParser.containsFaultCode("E-01".toByteArray()))
        assertTrue(EhlDisplayParser.containsFaultCode("e-05".toByteArray()))
        
        assertFalse(EhlDisplayParser.containsFaultCode("15.90".toByteArray()))
        assertFalse(EhlDisplayParser.containsFaultCode("Fn-05".toByteArray()))
        assertFalse(EhlDisplayParser.containsFaultCode(ByteArray(0)))
    }
    
    @Test
    @DisplayName("extractFaultCode should extract code string")
    fun testExtractFaultCode() {
        assertEquals("E-05", EhlDisplayParser.extractFaultCode("E-05".toByteArray()))
        assertEquals("E-01", EhlDisplayParser.extractFaultCode("E-01".toByteArray()))
        assertNotNull(EhlDisplayParser.extractFaultCode("e 05".toByteArray()))
        
        assertNull(EhlDisplayParser.extractFaultCode("15.90".toByteArray()))
        assertNull(EhlDisplayParser.extractFaultCode(ByteArray(0)))
    }
    
    @Test
    @DisplayName("Parse fault embedded in longer display string")
    fun testParseFaultInLongerString() {
        // Simulate display data that might contain fault code plus other data
        val displayData = "Price: E-05 Error".toByteArray()
        
        val result = EhlDisplayParser.parseDisplayData(displayData)
        
        assertTrue(result is DisplayParseResult.Fault)
        val fault = (result as DisplayParseResult.Fault).fault
        assertEquals(EhlFault.E_05, fault)
    }
    
    @Test
    @DisplayName("Binary data with fault code should parse correctly")
    fun testBinaryDataWithFault() {
        // Simulate binary protocol data that contains ASCII fault code
        val data = byteArrayOf(0x00, 0x45, 0x2D, 0x30, 0x36, 0x00) // \x00E-06\x00
        
        val result = EhlDisplayParser.parseDisplayData(data)
        
        assertTrue(result is DisplayParseResult.Fault)
        val fault = (result as DisplayParseResult.Fault).fault
        assertEquals(EhlFault.E_06, fault)
    }
}
