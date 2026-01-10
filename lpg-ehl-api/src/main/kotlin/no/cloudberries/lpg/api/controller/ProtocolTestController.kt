package no.cloudberries.lpg.api.controller

import no.cloudberries.lpg.api.service.WireTraceService
import no.cloudberries.lpg.api.service.WireTraceResult
import no.cloudberries.lpg.emulator.EhlDispenserEmulator
import no.cloudberries.lpg.protocol.EhlCommand
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Protocol Test Controller for VB6 Wire Compliance Testing.
 * 
 * KRITISK: Bruker EKTE kommunikasjonskanal (EhlCommunicator), ikke mock.
 * 
 * - Lokalt/dev: tester mot EhlDispenserEmulator via InMemorySerialPort
 * - Produksjon: tester mot ekte hardware via SerialPortManager
 * 
 * Returnerer wire trace data (TX/RX HEX) og VB6-valideringsresultater.
 */
@RestController
@RequestMapping("/api/v1/protocol/test")
class ProtocolTestController(
    private val wireTraceService: WireTraceService,
    private val emulator: EhlDispenserEmulator? = null  // Kun tilgjengelig i local/dev
) {
    private val logger = LoggerFactory.getLogger(ProtocolTestController::class.java)
    
    @PostMapping("/linetest/{address}")
    fun testLinetest(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: LINETEST til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.LINETEST, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/volume/{address}")
    fun testVolume(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: VOLUME til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.VOLUME, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/price/{address}")
    fun testPrice(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: PRICE til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.PRICE, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/state/{address}")
    fun testState(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: STATE til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.STATE, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/error/{address}")
    fun testError(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: ERROR_QUERY til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.ERROR_QUERY, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/tank/{address}")
    fun testTank(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: TANK til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.TANK, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/block/{address}")
    fun testBlock(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: BLOCK til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.BLOCK, address)
        return ResponseEntity.ok(result)
    }
    
    @PostMapping("/unblock/{address}")
    fun testUnblock(@PathVariable address: Int): ResponseEntity<WireTraceResult> {
        logger.info("🧪 VB6 Wire Test: UNBLOCK til adresse $address")
        val result = wireTraceService.executeCommand(EhlCommand.UNBLOCK, address)
        return ResponseEntity.ok(result)
    }
    
    /**
     * Kjør komplett VB6 testsekvens og stopp ved første feil.
     */
    @PostMapping("/sequence/{address}")
    fun testSequence(@PathVariable address: Int): ResponseEntity<SequenceResult> {
        logger.info("🧪 VB6 Wire Test: Kjører komplett sekvens til adresse $address")
        
        val commands = listOf(
            EhlCommand.LINETEST,
            EhlCommand.STATE,
            EhlCommand.VOLUME,
            EhlCommand.PRICE
        )
        
        val results = mutableListOf<WireTraceResult>()
        var allPassed = true
        var failedAt: String? = null
        
        for (command in commands) {
            val result = wireTraceService.executeCommand(command, address)
            results.add(result)
            
            if (!result.validation.vb6Compliant) {
                allPassed = false
                failedAt = command.name
                logger.warn("❌ VB6-sekvens feilet ved ${command.name}")
                break  // Stopp ved første feil
            }
            logger.info("✅ ${command.name} bestått")
        }
        
        return ResponseEntity.ok(SequenceResult(
            allPassed = allPassed,
            failedAt = failedAt,
            testsRun = results.size,
            totalTests = commands.size,
            results = results
        ))
    }
    
    /**
     * Hent emulator-status (kun tilgjengelig i local/dev).
     */
    @GetMapping("/emulator/status")
    fun getEmulatorStatus(): ResponseEntity<Map<String, Any>> {
        return if (emulator != null) {
            ResponseEntity.ok(mapOf(
                "tilkoblet" to true,
                "tilstand" to emulator.getCurrentState().name,
                "transaksjon" to (emulator.getCurrentTransaction()?.let {
                    mapOf(
                        "id" to it.id,
                        "volumLiter" to it.volumeLitres,
                        "beløpKr" to it.amountCents / 100.0
                    )
                } ?: "ingen")
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "tilkoblet" to false,
                "melding" to "Emulator ikke tilgjengelig - koblet til ekte hardware"
            ))
        }
    }
}

data class SequenceResult(
    val allPassed: Boolean,
    val failedAt: String?,
    val testsRun: Int,
    val totalTests: Int,
    val results: List<WireTraceResult>
)
