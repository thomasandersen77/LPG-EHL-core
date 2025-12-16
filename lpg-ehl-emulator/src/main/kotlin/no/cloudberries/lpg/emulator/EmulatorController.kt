package no.cloudberries.lpg.emulator

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/emulator")
class EmulatorController(
    private val emulatorService: EmulatorService,
    private val scenarioService: EmulatorScenarioService
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
}
