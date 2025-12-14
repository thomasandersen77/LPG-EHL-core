package no.cloudberries.lpg.emulator

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/emulator")
class EmulatorController(private val emulatorService: EmulatorService) {

    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(emulatorService.getStatus())
    }

    @PostMapping("/reset")
    fun reset(): ResponseEntity<Map<String, String>> {
        emulatorService.reset()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }
}
