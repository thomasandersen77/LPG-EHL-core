package no.cloudberries.lpg.emulator

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/emulator")
class EmulatorController(
    private val emulatorService: EmulatorService
) {

    data class SetScenarioRequest(
        val dispenserAddress: Int,
        val scenario: EmulatorScenario
    )

    @PostMapping("/scenario")
    fun setScenario(@RequestBody body: SetScenarioRequest): EmulatorStatus {
        emulatorService.setScenario(body.dispenserAddress, body.scenario)
        return emulatorService.status(body.dispenserAddress)
    }

    @PostMapping("/reset/{address}")
    fun reset(@PathVariable("address") dispenserAddress: Int): EmulatorStatus {
        emulatorService.reset(dispenserAddress)
        return emulatorService.status(dispenserAddress)
    }

    @GetMapping("/status/{address}")
    fun status(@PathVariable("address") dispenserAddress: Int): EmulatorStatus =
        emulatorService.status(dispenserAddress)
}
