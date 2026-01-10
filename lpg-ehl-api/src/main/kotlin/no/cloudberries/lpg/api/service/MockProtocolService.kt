package no.cloudberries.lpg.api.service

import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Mock Protocol Service for simulating EHL wire frames.
 * 
 * This service generates valid EHL packets with correct wire format for testing
 * without real hardware. It logs TX/RX frames so they appear in the Control Panel.
 */
@Service
class MockProtocolService {
    private val logger = LoggerFactory.getLogger(MockProtocolService::class.java)
    
    // Simulated dispenser state
    private var currentVolumeLiters: Double = 0.0
    private var currentPriceKrPerLiter: Double = 15.90
    private var dispenserState: Int = 0x00  // IDLE
    
    /**
     * Execute a protocol command and return wire trace data.
     */
    fun executeCommand(command: EhlCommand, address: Int, data: ByteArray = ByteArray(0)): WireTraceResult {
        // Build TX packet
        val txPacket = EhlPacket(address, command, data)
        val txBytes = EhlCodec.encode(txPacket, fromController = true)
        
        // Log TX frame
        val txHex = txBytes.toHexString()
        logger.info("📤 TX HEX: [$txHex] -> ${command.name}")
        
        // Generate simulated response
        val rxBytes = generateResponse(command, address)
        val rxHex = rxBytes.toHexString()
        
        // Log RX frame  
        logger.info("📥 RX HEX: [$rxHex] <- Response")
        
        // Parse response for validation
        val parseResult = EhlCodec.decode(rxBytes)
        
        // Build validation checks
        val checks = validateWireFormat(txBytes, rxBytes, command)
        val vb6Compliant = checks.all { it.ok }
        
        return WireTraceResult(
            ok = parseResult is EhlPacketParseResult.Success,
            command = command.name,
            parsed = parsedData(command, rxBytes),
            wire = WireTrace(
                txHex = txHex,
                rxHex = rxHex,
                txBytes = txBytes.map { it.toInt() and 0xFF },
                rxBytes = rxBytes.map { it.toInt() and 0xFF }
            ),
            validation = ValidationResult(
                checks = checks,
                vb6Compliant = vb6Compliant
            )
        )
    }
    
    /**
     * Generate a simulated dispenser response for a command.
     */
    private fun generateResponse(command: EhlCommand, address: Int): ByteArray {
        val responseData = when (command) {
            EhlCommand.LINETEST -> {
                // VB6 LINETEST expects 0x55 0xAA magic bytes
                byteArrayOf(0x55, 0xAA.toByte())
            }
            EhlCommand.STATE -> {
                // STATE returns 1 byte bitfield
                byteArrayOf(dispenserState.toByte())
            }
            EhlCommand.VOLUME -> {
                // VOLUME returns 5 ASCII digits LSB-first (e.g., 45.50L = "04550" -> ['0','5','5','4','0'])
                val volumeCentiliters = (currentVolumeLiters * 100).toInt().coerceIn(0, 99999)
                val volumeStr = "%05d".format(volumeCentiliters)
                ByteArray(5) { i -> volumeStr[4 - i].code.toByte() }  // LSB-first
            }
            EhlCommand.PRICE -> {
                // PRICE returns 4 ASCII digits LSB-first (e.g., 15.90 = "1590" -> ['0','9','5','1'])
                val priceOre = (currentPriceKrPerLiter * 100).toInt().coerceIn(0, 9999)
                val priceStr = "%04d".format(priceOre)
                ByteArray(4) { i -> priceStr[3 - i].code.toByte() }  // LSB-first
            }
            EhlCommand.ERROR_QUERY -> {
                // ERROR returns 2 ASCII bytes: main code + sub code
                byteArrayOf('0'.code.toByte(), '0'.code.toByte())  // No error
            }
            EhlCommand.TANK -> {
                // TANK returns tank level info
                byteArrayOf(0x00, 0x55)  // 85% tank level
            }
            EhlCommand.OK, EhlCommand.BLOCK, EhlCommand.UNBLOCK, EhlCommand.ZER,
            EhlCommand.PROG_PRC, EhlCommand.PROG_AMOUNT, EhlCommand.PROG_VOLUME, EhlCommand.PRODUCT_SELECT -> {
                // ACK responses have no payload
                ByteArray(0)
            }
            else -> ByteArray(0)
        }
        
        // Build response packet (from dispenser)
        val responsePacket = EhlPacket(address, command, responseData)
        return EhlCodec.encode(responsePacket, fromController = false)
    }
    
    /**
     * Validate wire format according to VB6 rules.
     */
    private fun validateWireFormat(txBytes: ByteArray, rxBytes: ByteArray, command: EhlCommand): List<ValidationCheck> {
        val checks = mutableListOf<ValidationCheck>()
        
        // Check STX/ETX
        val txStx = txBytes[0].toInt() and 0xFF
        val rxStx = rxBytes[0].toInt() and 0xFF
        val txEtx = txBytes[txBytes.size - 1].toInt() and 0xFF
        val rxEtx = rxBytes[rxBytes.size - 1].toInt() and 0xFF
        
        checks.add(ValidationCheck(
            name = "STX/ETX",
            ok = txStx == 0x10 && rxStx == 0x20 && txEtx == 0x36 && rxEtx == 0x36,
            details = "TX STX=0x${"%02X".format(txStx)}, RX STX=0x${"%02X".format(rxStx)}, ETX=0x${"%02X".format(txEtx)}/0x${"%02X".format(rxEtx)}"
        ))
        
        // Check LEN
        val txLen = txBytes[1].toInt() and 0xFF
        val rxLen = rxBytes[1].toInt() and 0xFF
        checks.add(ValidationCheck(
            name = "LEN korrekt",
            ok = txLen == txBytes.size && rxLen == rxBytes.size,
            details = "TX LEN=$txLen (actual ${txBytes.size}), RX LEN=$rxLen (actual ${rxBytes.size})"
        ))
        
        // Check CHK XOR
        val txChecksum = calculateChecksum(txBytes)
        val rxChecksum = calculateChecksum(rxBytes)
        val txStoredChk = txBytes[txBytes.size - 2].toInt() and 0xFF
        val rxStoredChk = rxBytes[rxBytes.size - 2].toInt() and 0xFF
        checks.add(ValidationCheck(
            name = "CHK XOR",
            ok = txChecksum == txStoredChk && rxChecksum == rxStoredChk,
            details = "TX: calc=0x${"%02X".format(txChecksum)} stored=0x${"%02X".format(txStoredChk)}, RX: calc=0x${"%02X".format(rxChecksum)} stored=0x${"%02X".format(rxStoredChk)}"
        ))
        
        // Command-specific VB6 payload validation
        val payloadCheck = validateVb6Payload(command, rxBytes)
        checks.add(payloadCheck)
        
        return checks
    }
    
    /**
     * Validate VB6-specific payload format.
     */
    private fun validateVb6Payload(command: EhlCommand, rxBytes: ByteArray): ValidationCheck {
        val dataStart = 4  // After STX, LEN, ADDR, CMD
        val dataEnd = rxBytes.size - 2  // Before CHK, ETX
        val payload = if (dataEnd > dataStart) rxBytes.copyOfRange(dataStart, dataEnd) else ByteArray(0)
        
        return when (command) {
            EhlCommand.LINETEST -> {
                val ok = payload.size >= 2 && payload[0] == 0x55.toByte() && payload[1] == 0xAA.toByte()
                ValidationCheck(
                    name = "VB6 LINETEST payload",
                    ok = ok,
                    details = if (ok) "Magic bytes 0x55 0xAA present" else "Expected 0x55 0xAA, got ${payload.toHexString()}"
                )
            }
            EhlCommand.VOLUME -> {
                val ok = payload.size == 5 && payload.all { (it.toInt() and 0xFF) in 0x30..0x39 }
                val decoded = if (ok) {
                    val str = String(payload.reversed().toByteArray())
                    val liters = str.toIntOrNull()?.div(100.0) ?: 0.0
                    "$liters L"
                } else "invalid"
                ValidationCheck(
                    name = "VB6 VOLUME payload",
                    ok = ok,
                    details = "5 ASCII digits LSB-first: ${payload.toHexString()} = $decoded"
                )
            }
            EhlCommand.PRICE -> {
                val ok = payload.size == 4 && payload.all { (it.toInt() and 0xFF) in 0x30..0x39 }
                val decoded = if (ok) {
                    val str = String(payload.reversed().toByteArray())
                    val kr = str.toIntOrNull()?.div(100.0) ?: 0.0
                    "$kr kr/L"
                } else "invalid"
                ValidationCheck(
                    name = "VB6 PRICE payload",
                    ok = ok,
                    details = "4 ASCII digits LSB-first: ${payload.toHexString()} = $decoded"
                )
            }
            EhlCommand.STATE -> {
                val ok = payload.size == 1
                val decoded = if (ok) {
                    val bits = payload[0].toInt() and 0xFF
                    val flags = mutableListOf<String>()
                    if (bits and 0x80 != 0) flags.add("ERROR")
                    if (bits and 0x08 != 0) flags.add("AUTOMODE")
                    if (bits and 0x04 != 0) flags.add("STARTBUTTON")
                    if (bits and 0x02 != 0) flags.add("OPENFORDELIVERY")
                    if (flags.isEmpty()) "IDLE" else flags.joinToString(", ")
                } else "invalid"
                ValidationCheck(
                    name = "VB6 STATE payload",
                    ok = ok,
                    details = "1 byte bitfield: 0x${"%02X".format(payload.getOrNull(0)?.toInt()?.and(0xFF) ?: 0)} = $decoded"
                )
            }
            else -> ValidationCheck(
                name = "VB6 payload format",
                ok = true,
                details = "No specific VB6 validation for ${command.name}"
            )
        }
    }
    
    private fun calculateChecksum(bytes: ByteArray): Int {
        // XOR of bytes from LEN to just before CHK
        var xor = 0
        for (i in 1 until bytes.size - 2) {
            xor = xor xor (bytes[i].toInt() and 0xFF)
        }
        return xor
    }
    
    private fun parsedData(command: EhlCommand, rxBytes: ByteArray): Map<String, Any> {
        val dataStart = 4
        val dataEnd = rxBytes.size - 2
        val payload = if (dataEnd > dataStart) rxBytes.copyOfRange(dataStart, dataEnd) else ByteArray(0)
        
        return when (command) {
            EhlCommand.VOLUME -> {
                if (payload.size == 5) {
                    val str = String(payload.reversed().toByteArray())
                    mapOf("volumeLiters" to (str.toIntOrNull()?.div(100.0) ?: 0.0))
                } else emptyMap()
            }
            EhlCommand.PRICE -> {
                if (payload.size == 4) {
                    val str = String(payload.reversed().toByteArray())
                    mapOf("priceKrPerLiter" to (str.toIntOrNull()?.div(100.0) ?: 0.0))
                } else emptyMap()
            }
            EhlCommand.STATE -> {
                if (payload.isNotEmpty()) {
                    val bits = payload[0].toInt() and 0xFF
                    mapOf(
                        "raw" to bits,
                        "error" to ((bits and 0x80) != 0),
                        "autoMode" to ((bits and 0x08) != 0),
                        "startButton" to ((bits and 0x04) != 0),
                        "openForDelivery" to ((bits and 0x02) != 0)
                    )
                } else emptyMap()
            }
            else -> emptyMap()
        }
    }
    
    /**
     * Update simulated dispenser state.
     */
    fun setVolume(liters: Double) {
        currentVolumeLiters = liters
        logger.info("🔧 Mock dispenser volume set to $liters L")
    }
    
    fun setPrice(krPerLiter: Double) {
        currentPriceKrPerLiter = krPerLiter
        logger.info("🔧 Mock dispenser price set to $krPerLiter kr/L")
    }
    
    fun setState(state: Int) {
        dispenserState = state
        logger.info("🔧 Mock dispenser state set to 0x${"%02X".format(state)}")
    }
    
    private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}

// Data classes for wire trace response
data class WireTraceResult(
    val ok: Boolean,
    val command: String,
    val parsed: Map<String, Any>,
    val wire: WireTrace,
    val validation: ValidationResult
)

data class WireTrace(
    val txHex: String,
    val rxHex: String,
    val txBytes: List<Int>,
    val rxBytes: List<Int>
)

data class ValidationResult(
    val checks: List<ValidationCheck>,
    val vb6Compliant: Boolean
)

data class ValidationCheck(
    val name: String,
    val ok: Boolean,
    val details: String
)
