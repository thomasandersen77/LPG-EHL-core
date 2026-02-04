package no.cloudberries.lpg.protocol

/**
 * VB6 Error Message Mapper
 * 
 * Maps EHL error codes (main, sub) to Norwegian and English messages.
 * Ported from Python ehl_protocol.py _VB6_ERR_TEXT_NO dictionary.
 * 
 * These messages are extracted from the legacy VB6 codebase in norgesgass_legacy/defs.bas -> logdisp_err()
 */
object EhlErrorMessages {
    
    /**
     * Error message with Norwegian and English translations
     */
    data class ErrorMessage(
        val mainCode: Int,
        val subCode: Int,
        val norwegian: String,
        val english: String
    )
    
    /**
     * Map of (mainCode, subCode) -> ErrorMessage
     * 
     * Main codes represent error categories:
     * - 1: Display communication errors
     * - 2: Pulser/flow sensor errors
     * - 3: Output/pump control errors
     * - 4: System/memory errors
     * - 5: RS-485 communication errors
     * - 6: Transaction/flow errors
     */
    private val errorMap: Map<Pair<Int, Int>, ErrorMessage> = mapOf(
        // Display errors (main=1)
        (1 to 1) to ErrorMessage(
            1, 1,
            "Ingen kommunikasjon Display<-->CPU",
            "No communication Display<-->CPU"
        ),
        (1 to 2) to ErrorMessage(
            1, 2,
            "For mange kommunikasjonsfeil Display<-->CPU",
            "Too many communication errors Display<-->CPU"
        ),
        (1 to 3) to ErrorMessage(
            1, 3,
            "Intern feil Display",
            "Internal display error"
        ),
        
        // Pulser errors (main=2)
        (2 to 1) to ErrorMessage(
            2, 1,
            "Pulser ikke tilkoblet",
            "Pulser not connected"
        ),
        (2 to 2) to ErrorMessage(
            2, 2,
            "Feil rotasjon på pulser",
            "Incorrect pulser rotation"
        ),
        (2 to 3) to ErrorMessage(
            2, 3,
            "En pulserkanal mangler",
            "One pulser channel missing"
        ),
        (2 to 4) to ErrorMessage(
            2, 4,
            "Feil serie på pulser",
            "Incorrect pulser series"
        ),
        (2 to 5) to ErrorMessage(
            2, 5,
            "Pulser buffer overflow",
            "Pulser buffer overflow"
        ),
        (2 to 6) to ErrorMessage(
            2, 6,
            "LPG flow for høy",
            "LPG flow too high"
        ),
        
        // Output/pump errors (main=3)
        (3 to 1) to ErrorMessage(
            3, 1,
            "Output overload(Para 10)",
            "Output overload (Parameter 10)"
        ),
        (3 to 2) to ErrorMessage(
            3, 2,
            "Output control failure",
            "Output control failure"
        ),
        (3 to 3) to ErrorMessage(
            3, 3,
            "Startknapp aktivert under oppstart",
            "Start button active during startup"
        ),
        (3 to 4) to ErrorMessage(
            3, 4,
            "No load detected",
            "No load detected"
        ),
        (3 to 5) to ErrorMessage(
            3, 5,
            "Termisk pumpebeskyttelse aktivert",
            "Thermal pump protection active"
        ),
        
        // System errors (main=4)
        (4 to 1) to ErrorMessage(
            4, 1,
            "Minnefeil system",
            "System memory error"
        ),
        (4 to 2) to ErrorMessage(
            4, 2,
            "Reset aktivert på hovedkort",
            "Reset activated on main board"
        ),
        (4 to 3) to ErrorMessage(
            4, 3,
            "Strømbrudd",
            "Power failure"
        ),
        (4 to 4) to ErrorMessage(
            4, 4,
            "Intern kommunikasjon CPU<-->Mainstream",
            "Internal communication CPU<-->Mainstream"
        ),
        (4 to 5) to ErrorMessage(
            4, 5,
            "Calculations owerflow",
            "Calculations overflow"
        ),
        (4 to 7) to ErrorMessage(
            4, 7,
            "Brownout reset- for lite strøm til prosessor",
            "Brownout reset - insufficient power to processor"
        ),
        (4 to 8) to ErrorMessage(
            4, 8,
            "Ingen svar fra CPU",
            "No response from CPU"
        ),
        
        // RS-485 communication errors (main=5)
        (5 to 1) to ErrorMessage(
            5, 1,
            "Ingen Rs485 kommunikasjon",
            "No RS-485 communication"
        ),
        
        // Transaction/flow errors (main=6)
        (6 to 1) to ErrorMessage(
            6, 1,
            "Fylling har pågått for lenge (Para 22)",
            "Filling has been ongoing too long (Parameter 22)"
        ),
        (6 to 2) to ErrorMessage(
            6, 2,
            "For lang tid uten pulser ( Para 24)",
            "Too long time without pulses (Parameter 24)"
        ),
        (6 to 3) to ErrorMessage(
            6, 3,
            "Flow for høy(Para 45)",
            "Flow too high (Parameter 45)"
        ),
        (6 to 4) to ErrorMessage(
            6, 4,
            "Maksimal grense for beløp nådd",
            "Maximum amount limit reached"
        ),
        (6 to 6) to ErrorMessage(
            6, 6,
            "Pris er satt til 0.00",
            "Price is set to 0.00"
        ),
        (6 to 7) to ErrorMessage(
            6, 7,
            "Flow for liten (Para 48)",
            "Flow too low (Parameter 48)"
        ),
        (6 to 8) to ErrorMessage(
            6, 8,
            "Feil transaksjon state",
            "Incorrect transaction state"
        )
    )
    
    /**
     * Get error message for given main and sub codes
     * 
     * @param mainCode Main error category (1-6)
     * @param subCode Sub error code within category
     * @return ErrorMessage or null if not found
     */
    fun getErrorMessage(mainCode: Int, subCode: Int): ErrorMessage? {
        return errorMap[mainCode to subCode]
    }
    
    /**
     * Parse error data from EHL ERROR response packet (VB6 format)
     * 
     * VB6 format: 2 ASCII bytes representing main and sub codes
     * Example: data = [0x31, 0x32] -> main=1, sub=2 -> "For mange kommunikasjonsfeil Display<-->CPU"
     * 
     * @param data Raw data bytes from ERROR packet
     * @return Parsed error with message, or null if invalid
     */
    fun parseErrorData(data: ByteArray): ParsedError? {
        if (data.size < 2) return null
        
        // VB6 uses Val(Chr(x(4))), Val(Chr(x(5))) - ASCII to integer conversion
        val mainCode = asciiToInt(data[0])
        val subCode = asciiToInt(data[1])
        
        val message = getErrorMessage(mainCode, subCode)
        
        return ParsedError(
            mainCode = mainCode,
            subCode = subCode,
            message = message,
            rawBytes = data.take(2).toByteArray()
        )
    }
    
    /**
     * Convert ASCII byte to integer (VB6 Val(Chr(x)) behavior)
     * 
     * If byte is ASCII digit ('0'-'9'), convert to numeric value
     * Otherwise return byte value directly
     */
    private fun asciiToInt(byte: Byte): Int {
        val b = byte.toInt() and 0xFF
        return if (b in 0x30..0x39) {
            b - 0x30  // ASCII '0'-'9' -> 0-9
        } else {
            b  // Raw byte value
        }
    }
    
    /**
     * Parsed error with optional message lookup
     */
    data class ParsedError(
        val mainCode: Int,
        val subCode: Int,
        val message: ErrorMessage?,
        val rawBytes: ByteArray
    ) {
        val hasMessage: Boolean get() = message != null
        
        val norwegian: String get() = message?.norwegian ?: "Ukjent feil"
        val english: String get() = message?.english ?: "Unknown error"
        
        override fun toString(): String {
            return if (hasMessage) {
                "Error($mainCode,$subCode): ${message!!.norwegian} / ${message.english}"
            } else {
                "Error($mainCode,$subCode): Unknown (raw: ${rawBytes.joinToString(" ") { "%02X".format(it) }})"
            }
        }
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as ParsedError
            
            if (mainCode != other.mainCode) return false
            if (subCode != other.subCode) return false
            if (!rawBytes.contentEquals(other.rawBytes)) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = mainCode
            result = 31 * result + subCode
            result = 31 * result + rawBytes.contentHashCode()
            return result
        }
    }
    
    /**
     * Get all registered error codes (for documentation/testing)
     */
    fun getAllErrorCodes(): List<Pair<Int, Int>> {
        return errorMap.keys.toList().sortedWith(compareBy({ it.first }, { it.second }))
    }
    
    /**
     * Get all error messages for a given main category
     */
    fun getErrorsForCategory(mainCode: Int): List<ErrorMessage> {
        return errorMap
            .filterKeys { it.first == mainCode }
            .values
            .sortedBy { it.subCode }
    }
}
