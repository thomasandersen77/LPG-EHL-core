package no.cloudberries.lpg.api.service

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Wire Trace Service for VB6 Protocol Compliance Testing.
 * 
 * KRITISK: Denne tjenesten bruker den EKTE kommunikasjonskanalen (EhlCommunicator).
 * 
 * - Hvis EhlCommunicator er koblet til InMemorySerialPort -> tester mot emulator
 * - Hvis EhlCommunicator er koblet til SerialPortManager -> tester mot EKTE hardware
 * 
 * Dette sikrer at vi tester hele protokoll-pipelinen, ikke bare mock-data.
 */
@Service
class WireTraceService(
    private val communicator: EhlCommunicator
) {
    private val logger = LoggerFactory.getLogger(WireTraceService::class.java)
    
    /**
     * Utfør en protokollkommando og returner wire trace data.
     * 
     * @param command EHL-kommando å teste
     * @param address Dispenser-adresse (1-255)
     * @param timeoutMs Timeout i millisekunder
     * @return Wire trace med TX/RX HEX og validering
     */
    fun executeCommand(command: EhlCommand, address: Int, timeoutMs: Long = 3000): WireTraceResult {
        logger.info("📡 Wire Test: ${command.name} til adresse $address")
        
        // Bygg TX-pakke
        val txPacket = EhlPacket(address, command, ByteArray(0))
        val txBytes = EhlCodec.encode(txPacket, fromController = true)
        val txHex = txBytes.toHexString()
        
        // Logg TX (vil fanges av WebSocket-appender)
        logger.info("📤 TX HEX: [$txHex] -> ${command.name}")
        
        return try {
            // Send og motta via ekte kommunikasjonskanal
            val response = runBlocking {
                withTimeoutOrNull(timeoutMs) {
                    communicator.sendAndReceive(txPacket, timeoutMs)
                }
            }
            
            if (response != null) {
                // Encode response for wire trace
                val rxBytes = EhlCodec.encode(response, fromController = false)
                val rxHex = rxBytes.toHexString()
                
                logger.info("📥 RX HEX: [$rxHex] <- ${response.command.name}")
                
                // Valider wire-format
                val checks = validateWireFormat(txBytes, rxBytes, command)
                val vb6Compliant = checks.all { it.ok }
                
                WireTraceResult(
                    ok = true,
                    command = command.name,
                    parsed = parsedData(command, response.data),
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
            } else {
                // Timeout - ingen respons
                logger.warn("⏱️ Timeout: Ingen respons fra adresse $address")
                
                WireTraceResult(
                    ok = false,
                    command = command.name,
                    parsed = emptyMap(),
                    wire = WireTrace(
                        txHex = txHex,
                        rxHex = "(ingen respons)",
                        txBytes = txBytes.map { it.toInt() and 0xFF },
                        rxBytes = emptyList()
                    ),
                    validation = ValidationResult(
                        checks = listOf(
                            ValidationCheck("Respons mottatt", false, "Timeout etter ${timeoutMs}ms")
                        ),
                        vb6Compliant = false
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("❌ Kommunikasjonsfeil: ${e.message}")
            
            WireTraceResult(
                ok = false,
                command = command.name,
                parsed = emptyMap(),
                wire = WireTrace(
                    txHex = txHex,
                    rxHex = "(feil: ${e.message})",
                    txBytes = txBytes.map { it.toInt() and 0xFF },
                    rxBytes = emptyList()
                ),
                validation = ValidationResult(
                    checks = listOf(
                        ValidationCheck("Kommunikasjon", false, "Feil: ${e.message}")
                    ),
                    vb6Compliant = false
                )
            )
        }
    }
    
    /**
     * Valider wire-format iht. VB6-regler.
     */
    private fun validateWireFormat(txBytes: ByteArray, rxBytes: ByteArray, command: EhlCommand): List<ValidationCheck> {
        val checks = mutableListOf<ValidationCheck>()
        
        // Sjekk STX/ETX
        val txStx = txBytes[0].toInt() and 0xFF
        val rxStx = rxBytes[0].toInt() and 0xFF
        val txEtx = txBytes[txBytes.size - 1].toInt() and 0xFF
        val rxEtx = rxBytes[rxBytes.size - 1].toInt() and 0xFF
        
        checks.add(ValidationCheck(
            name = "STX/ETX",
            ok = txStx == 0x10 && rxStx == 0x20 && txEtx == 0x36 && rxEtx == 0x36,
            details = "TX STX=0x${"%02X".format(txStx)}, RX STX=0x${"%02X".format(rxStx)}, ETX=0x${"%02X".format(txEtx)}/0x${"%02X".format(rxEtx)}"
        ))
        
        // Sjekk LEN
        val txLen = txBytes[1].toInt() and 0xFF
        val rxLen = rxBytes[1].toInt() and 0xFF
        checks.add(ValidationCheck(
            name = "LEN korrekt",
            ok = txLen == txBytes.size && rxLen == rxBytes.size,
            details = "TX LEN=$txLen (faktisk ${txBytes.size}), RX LEN=$rxLen (faktisk ${rxBytes.size})"
        ))
        
        // Sjekk CHK XOR
        val txChecksum = calculateChecksum(txBytes)
        val rxChecksum = calculateChecksum(rxBytes)
        val txStoredChk = txBytes[txBytes.size - 2].toInt() and 0xFF
        val rxStoredChk = rxBytes[rxBytes.size - 2].toInt() and 0xFF
        checks.add(ValidationCheck(
            name = "CHK XOR",
            ok = txChecksum == txStoredChk && rxChecksum == rxStoredChk,
            details = "TX: beregnet=0x${"%02X".format(txChecksum)} lagret=0x${"%02X".format(txStoredChk)}, RX: beregnet=0x${"%02X".format(rxChecksum)} lagret=0x${"%02X".format(rxStoredChk)}"
        ))
        
        // Kommando-spesifikk VB6 payload-validering
        val payloadCheck = validateVb6Payload(command, rxBytes)
        checks.add(payloadCheck)
        
        return checks
    }
    
    /**
     * Valider VB6-spesifikt payload-format.
     */
    private fun validateVb6Payload(command: EhlCommand, rxBytes: ByteArray): ValidationCheck {
        val dataStart = 4  // Etter STX, LEN, ADDR, CMD
        val dataEnd = rxBytes.size - 2  // Før CHK, ETX
        val payload = if (dataEnd > dataStart) rxBytes.copyOfRange(dataStart, dataEnd) else ByteArray(0)
        
        return when (command) {
            EhlCommand.LINETEST -> {
                val ok = payload.size >= 2 && payload[0] == 0x55.toByte() && payload[1] == 0xAA.toByte()
                ValidationCheck(
                    name = "VB6 LINETEST payload",
                    ok = ok,
                    details = if (ok) "Magiske bytes 0x55 0xAA funnet" else "Forventet 0x55 0xAA, fikk ${payload.toHexString()}"
                )
            }
            EhlCommand.VOLUME -> {
                val ok = payload.size == 5 && payload.all { (it.toInt() and 0xFF) in 0x30..0x39 }
                val decoded = if (ok) {
                    val str = String(payload.reversed().toByteArray())
                    val liters = str.toIntOrNull()?.div(100.0) ?: 0.0
                    "$liters L"
                } else "ugyldig"
                ValidationCheck(
                    name = "VB6 VOLUME payload",
                    ok = ok,
                    details = "5 ASCII-siffer LSB-først: ${payload.toHexString()} = $decoded"
                )
            }
            EhlCommand.PRICE -> {
                val ok = payload.size == 4 && payload.all { (it.toInt() and 0xFF) in 0x30..0x39 }
                val decoded = if (ok) {
                    val str = String(payload.reversed().toByteArray())
                    val kr = str.toIntOrNull()?.div(100.0) ?: 0.0
                    "$kr kr/L"
                } else "ugyldig"
                ValidationCheck(
                    name = "VB6 PRICE payload",
                    ok = ok,
                    details = "4 ASCII-siffer LSB-først: ${payload.toHexString()} = $decoded"
                )
            }
            EhlCommand.STATE -> {
                val ok = payload.size == 1
                val decoded = if (ok) {
                    val bits = payload[0].toInt() and 0xFF
                    val flags = mutableListOf<String>()
                    if (bits and 0x80 != 0) flags.add("FEIL")
                    if (bits and 0x08 != 0) flags.add("AUTOMODUS")
                    if (bits and 0x04 != 0) flags.add("STARTKNAPP")
                    if (bits and 0x02 != 0) flags.add("ÅPEN_FOR_LEVERING")
                    if (flags.isEmpty()) "KLAR" else flags.joinToString(", ")
                } else "ugyldig"
                ValidationCheck(
                    name = "VB6 STATE payload",
                    ok = ok,
                    details = "1 byte bitfelt: 0x${"%02X".format(payload.getOrNull(0)?.toInt()?.and(0xFF) ?: 0)} = $decoded"
                )
            }
            else -> ValidationCheck(
                name = "VB6 payload format",
                ok = true,
                details = "Ingen spesifikk VB6-validering for ${command.name}"
            )
        }
    }
    
    private fun calculateChecksum(bytes: ByteArray): Int {
        var xor = 0
        for (i in 1 until bytes.size - 2) {
            xor = xor xor (bytes[i].toInt() and 0xFF)
        }
        return xor
    }
    
    private fun parsedData(command: EhlCommand, data: ByteArray): Map<String, Any> {
        return when (command) {
            EhlCommand.VOLUME -> {
                if (data.size == 5) {
                    val str = String(data.reversed().toByteArray())
                    mapOf("volumeLiter" to (str.toIntOrNull()?.div(100.0) ?: 0.0))
                } else emptyMap()
            }
            EhlCommand.PRICE -> {
                if (data.size == 4) {
                    val str = String(data.reversed().toByteArray())
                    mapOf("prisKrPerLiter" to (str.toIntOrNull()?.div(100.0) ?: 0.0))
                } else emptyMap()
            }
            EhlCommand.STATE -> {
                if (data.isNotEmpty()) {
                    val bits = data[0].toInt() and 0xFF
                    mapOf(
                        "råverdi" to bits,
                        "feil" to ((bits and 0x80) != 0),
                        "automodus" to ((bits and 0x08) != 0),
                        "startknapp" to ((bits and 0x04) != 0),
                        "åpenForLevering" to ((bits and 0x02) != 0)
                    )
                } else emptyMap()
            }
            EhlCommand.LINETEST -> {
                if (data.size >= 2) {
                    mapOf(
                        "magiskByte1" to "0x${"%02X".format(data[0])}",
                        "magiskByte2" to "0x${"%02X".format(data[1])}"
                    )
                } else emptyMap()
            }
            else -> emptyMap()
        }
    }
    
    private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}
