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
}
