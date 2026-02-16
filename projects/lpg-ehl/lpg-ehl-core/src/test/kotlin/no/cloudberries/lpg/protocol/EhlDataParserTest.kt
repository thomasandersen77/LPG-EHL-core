package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Data Parser Tests")
class EhlDataParserTest {
    
    @Test
    @DisplayName("Parse VOLUME data correctly - VB6 format")
    fun testParseVolumeDataVb6() {
        // Volume: 45.50 liters -> "04550" -> bytes ['0','5','5','4','0'] (LSB first)
        val data = byteArrayOf('0'.code.toByte(), '5'.code.toByte(), '5'.code.toByte(), '4'.code.toByte(), '0'.code.toByte())
        
        val volumeLitres = EhlDataParser.parseVolumeDataVb6(data)
        
        assertEquals(45.50, volumeLitres, 0.01)
    }
    
    @Test
    @DisplayName("Parse VOLUME data with zero values - VB6 format")
    fun testParseVolumeDataZeroVb6() {
        val data = byteArrayOf('0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte())
        
        val volumeLitres = EhlDataParser.parseVolumeDataVb6(data)
        
        assertEquals(0.0, volumeLitres, 0.01)
    }
    
    @Test
    @DisplayName("Parse VOLUME data throws on invalid size - VB6 format")
    fun testParseVolumeDataInvalidSizeVb6() {
        val data = byteArrayOf('0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte(), '0'.code.toByte())
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parseVolumeDataVb6(data)
        }
    }
    
    @Test
    @DisplayName("Parse STATE data correctly")
    fun testParseStateData() {
        val data = byteArrayOf(0x02) // DELIVERING state
        
        val state = EhlDataParser.parseStateData(data)
        
        assertEquals(2, state)
    }
    
    @Test
    @DisplayName("Parse STATE data throws on invalid size")
    fun testParseStateDataInvalidSize() {
        val data = byteArrayOf(0x02, 0x03)
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parseStateData(data)
        }
    }
    
    @Test
    @DisplayName("Parse PRICE data correctly")
    fun testParsePriceData() {
        // Price "15.90" encoded as ASCII '0', '9', '5', '1' (reversed)
        val data = byteArrayOf('0'.code.toByte(), '9'.code.toByte(), '5'.code.toByte(), '1'.code.toByte())
        
        val price = EhlDataParser.parsePriceData(data)
        
        assertEquals("15.90", price)
    }
    
    @Test
    @DisplayName("Parse PRICE data with different value")
    fun testParsePriceDataDifferentValue() {
        // Price "23.45" encoded as ASCII '5', '4', '3', '2' (reversed)
        val data = byteArrayOf('5'.code.toByte(), '4'.code.toByte(), '3'.code.toByte(), '2'.code.toByte())
        
        val price = EhlDataParser.parsePriceData(data)
        
        assertEquals("23.45", price)
    }
    
    @Test
    @DisplayName("Parse PRICE data throws on invalid ASCII")
    fun testParsePriceDataInvalidASCII() {
        // Non-digit ASCII characters
        val data = byteArrayOf(0x00, 0x09, 0x05, 0x01)
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parsePriceData(data)
        }
    }
    
    @Test
    @DisplayName("Parse PRICE data throws on invalid size")
    fun testParsePriceDataInvalidSize() {
        val data = byteArrayOf('0'.code.toByte(), '9'.code.toByte(), '5'.code.toByte())
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parsePriceData(data)
        }
    }
    
    @Test
    @DisplayName("Parse ERROR data correctly - VB6 format")
    fun testParseErrorData() {
        // VB6 format: 2 ASCII bytes (main code + sub code)
        val data = byteArrayOf('1'.code.toByte(), '5'.code.toByte())
        
        val (mainCode, subCode) = EhlDataParser.parseErrorData(data)
        
        assertEquals('1', mainCode)
        assertEquals('5', subCode)
    }
    
    @Test
    @DisplayName("Parse ERROR data throws on invalid size")
    fun testParseErrorDataInvalidSize() {
        // VB6 format requires exactly 2 bytes, test with 1 byte
        val data = byteArrayOf(0x10)
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parseErrorData(data)
        }
    }
    
    @Test
    @DisplayName("Parse ERROR data legacy format")
    fun testParseErrorDataLegacy() {
        val data = byteArrayOf(0x10)
        
        val errorCode = EhlDataParser.parseErrorDataLegacy(data)
        
        assertEquals(0x10, errorCode)
    }
    
    @Test
    @DisplayName("Round-trip VOLUME encoding and parsing - VB6 format")
    fun testVolumeRoundTripVb6() {
        val volumeLitres = 45.50
        
        // Encode as VB6 does: "04550" -> bytes ['0','5','5','4','0']
        val volStr = "04550"
        val encoded = ByteArray(5)
        for (i in 0..4) {
            encoded[i] = volStr[4 - i].code.toByte()
        }
        
        // Parse back
        val parsedVolume = EhlDataParser.parseVolumeDataVb6(encoded)
        
        assertEquals(volumeLitres, parsedVolume, 0.01)
    }
}
