package no.cloudberries.lpg.emulator

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/emulator")
class EmulatorController(
    private val emulatorService: EmulatorService,
    private val scenarioService: EmulatorScenarioService,
    private val logBuffer: LogBuffer
) {

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(emulatorService.getStatus())
    }

    @PostMapping("/reset")
    fun reset(): ResponseEntity<Map<String, String>> {
        emulatorService.reset()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }

    data class SetScenarioRequest(
        val dispenserAddress: Int,
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): ResponseEntity<EmulatorStatus> {
        scenarioService.setScenario(body.dispenserAddress, body.scenario)
        return ResponseEntity.ok(scenarioService.status(body.dispenserAddress))
    }

    @PostMapping("/reset/{address}")
    fun resetScenario(@PathVariable("address") dispenserAddress: Int): ResponseEntity<EmulatorStatus> {
        scenarioService.reset(dispenserAddress)
        return ResponseEntity.ok(scenarioService.status(dispenserAddress))
    }

    @GetMapping("/status/{address}")
    fun getScenarioStatus(@PathVariable("address") dispenserAddress: Int) =
        ResponseEntity.ok(scenarioService.status(dispenserAddress))
    
    @GetMapping("/internal/logs")
    fun getLogs(@RequestParam(defaultValue = "500") limit: Int): ResponseEntity<Map<String, Any>> {
        val logs = logBuffer.getRecentLogs(limit)
        return ResponseEntity.ok(mapOf(
            "count" to logs.size,
            "maxSize" to 1000,
            "logs" to logs
        ))
    }
    
    @DeleteMapping("/internal/logs")
    fun clearLogs(): ResponseEntity<Map<String, String>> {
        logBuffer.clear()
        return ResponseEntity.ok(mapOf("status" to "cleared"))
    }
    
    /**
     * Settle pending transaction and reset dispenser to IDLE.
     * This endpoint is called after payment is complete (e.g., card capture or credit settlement).
     * 
     * @param id Dispenser address (currently only 1 is supported)
     * @param method Payment method: "CARD" (default) or "CREDIT"
     * @return Settled transaction details or error message
     */
    @PostMapping("/settle/{id}")
    fun settle(
        @PathVariable id: Int,
        @RequestParam(defaultValue = "CARD") method: String
    ): ResponseEntity<Map<String, Any>> {
        val settledTransaction = emulatorService.settle(method)
        
        return if (settledTransaction != null) {
            ResponseEntity.ok(mapOf(
                "status" to "settled",
                "method" to method,
                "transaction" to mapOf(
                    "dispenserId" to settledTransaction.dispenserId,
                    "liters" to settledTransaction.liters,
                    "amountNok" to settledTransaction.amountNok,
                    "unitPrice" to settledTransaction.unitPrice,
                    "finishedAt" to settledTransaction.finishedAt.toString(),
                    "idempotencyKey" to settledTransaction.idempotencyKey
                )
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "status" to "no_pending_transaction",
                "message" to "No pending transaction to settle"
            ))
        }
    }
}
