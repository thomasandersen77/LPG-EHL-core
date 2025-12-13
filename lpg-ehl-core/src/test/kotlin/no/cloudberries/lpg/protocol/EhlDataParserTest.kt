package no.cloudberries.lpg.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("EHL Data Parser Tests")
class EhlDataParserTest {
    
    @Test
    @DisplayName("Parse VOLUME data correctly")
    fun testParseVolumeData() {
        // Volume: 150 deciliters (15.0 liters) = 0x0096
        // Amount: 1500 øre (15.00 kr) = 0x05DC
        val data = byteArrayOf(0x00, 0x96.toByte(), 0x05, 0xDC.toByte())
        
        val (volumeLitres, amountCents) = EhlDataParser.parseVolumeData(data)
        
        assertEquals(15.0, volumeLitres, 0.01)
        assertEquals(1500, amountCents)
    }
    
    @Test
    @DisplayName("Parse VOLUME data with zero values")
    fun testParseVolumeDataZero() {
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        
        val (volumeLitres, amountCents) = EhlDataParser.parseVolumeData(data)
        
        assertEquals(0.0, volumeLitres, 0.01)
        assertEquals(0, amountCents)
    }
    
    @Test
    @DisplayName("Parse VOLUME data throws on invalid size")
    fun testParseVolumeDataInvalidSize() {
        val data = byteArrayOf(0x00, 0x96.toByte(), 0x05)
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parseVolumeData(data)
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
    @DisplayName("Parse ERROR data correctly")
    fun testParseErrorData() {
        val data = byteArrayOf(0x10)
        
        val errorCode = EhlDataParser.parseErrorData(data)
        
        assertEquals(0x10, errorCode)
    }
    
    @Test
    @DisplayName("Parse ERROR data throws on invalid size")
    fun testParseErrorDataInvalidSize() {
        val data = byteArrayOf(0x10, 0x20)
        
        assertThrows(IllegalArgumentException::class.java) {
            EhlDataParser.parseErrorData(data)
        }
    }
    
    @Test
    @DisplayName("Round-trip VOLUME encoding and parsing")
    fun testVolumeRoundTrip() {
        // Create VOLUME response from emulator
        val volumeLitres = 12.5
        val amountCents = 1250
        
        // Encode as emulator does
        val volDeci = (volumeLitres * 10).toInt()
        val encoded = ByteArray(4)
        encoded[0] = ((volDeci shr 8) and 0xFF).toByte()
        encoded[1] = (volDeci and 0xFF).toByte()
        encoded[2] = ((amountCents shr 8) and 0xFF).toByte()
        encoded[3] = (amountCents and 0xFF).toByte()
        
        // Parse back
        val (parsedVolume, parsedAmount) = EhlDataParser.parseVolumeData(encoded)
        
        assertEquals(volumeLitres, parsedVolume, 0.01)
        assertEquals(amountCents, parsedAmount)
    }
}
