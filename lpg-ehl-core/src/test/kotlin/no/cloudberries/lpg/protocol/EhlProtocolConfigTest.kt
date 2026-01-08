package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Protocol Configuration Tests")
class EhlProtocolConfigTest {
    
    @Test
    @DisplayName("Standard EHL variant uses correct STX/ETX values")
    fun testStandardEhlVariant() {
        val config = EhlProtocolConfig.standardEhl()
        
        assertEquals(0x02.toByte(), config.stxController, "STX Controller should be 0x02")
        assertEquals(0x02.toByte(), config.stxDispenser, "STX Dispenser should be 0x02 in standard")
        assertEquals(0x03.toByte(), config.etx, "ETX should be 0x03")
        assertEquals(ProtocolVariant.STANDARD_EHL, config.variant)
    }
    
    @Test
    @DisplayName("Norges Gass variant uses correct STX/ETX values")
    fun testNorgesGassVariant() {
        val config = EhlProtocolConfig.norgesGass()
        
        assertEquals(0x10.toByte(), config.stxController, "STX Controller should be 0x10")
        assertEquals(0x20.toByte(), config.stxDispenser, "STX Dispenser should be 0x20")
        assertEquals(0x36.toByte(), config.etx, "ETX should be 0x36")
        assertEquals(ProtocolVariant.NORGES_GASS, config.variant)
    }
    
    @Test
    @DisplayName("Default configuration uses Norges Gass variant")
    fun testDefaultConfiguration() {
        val config = EhlProtocolConfig()
        
        assertEquals(ProtocolVariant.NORGES_GASS, config.variant)
        assertEquals(100L, config.interCommandDelayMs, "Default delay should be 100ms")
        assertEquals(2000L, config.responseTimeoutMs, "Default timeout should be 2000ms")
        assertEquals(3, config.maxRetries, "Default retries should be 3")
    }
    
    @Test
    @DisplayName("isValidStx validates STX bytes correctly for Standard EHL")
    fun testIsValidStxStandardEhl() {
        val config = EhlProtocolConfig.standardEhl()
        
        assertTrue(config.isValidStx(0x02.toByte()), "0x02 should be valid")
        assertFalse(config.isValidStx(0x10.toByte()), "0x10 should be invalid for Standard EHL")
        assertFalse(config.isValidStx(0x20.toByte()), "0x20 should be invalid for Standard EHL")
        assertFalse(config.isValidStx(0xFF.toByte()), "0xFF should be invalid")
    }
    
    @Test
    @DisplayName("isValidStx validates STX bytes correctly for Norges Gass")
    fun testIsValidStxNorgesGass() {
        val config = EhlProtocolConfig.norgesGass()
        
        assertTrue(config.isValidStx(0x10.toByte()), "0x10 should be valid (controller)")
        assertTrue(config.isValidStx(0x20.toByte()), "0x20 should be valid (dispenser)")
        assertFalse(config.isValidStx(0x02.toByte()), "0x02 should be invalid for Norges Gass")
        assertFalse(config.isValidStx(0x03.toByte()), "0x03 should be invalid")
    }
    
    @Test
    @DisplayName("Custom inter-command delay can be configured")
    fun testCustomInterCommandDelay() {
        val config = EhlProtocolConfig(interCommandDelayMs = 50)
        
        assertEquals(50L, config.interCommandDelayMs)
    }
    
    @Test
    @DisplayName("Custom response timeout can be configured")
    fun testCustomResponseTimeout() {
        val config = EhlProtocolConfig(responseTimeoutMs = 5000)
        
        assertEquals(5000L, config.responseTimeoutMs)
    }
    
    @Test
    @DisplayName("Packet logging can be enabled")
    fun testPacketLogging() {
        val config = EhlProtocolConfig(enablePacketLogging = true)
        
        assertTrue(config.enablePacketLogging)
    }
    
    @Test
    @DisplayName("Protocol variant description is human-readable")
    fun testProtocolVariantDescriptions() {
        assertEquals("Standard EHL (0x02/0x03)", ProtocolVariant.STANDARD_EHL.description)
        assertEquals("Norges Gass Variant (0x10/0x20/0x36)", ProtocolVariant.NORGES_GASS.description)
    }
}
