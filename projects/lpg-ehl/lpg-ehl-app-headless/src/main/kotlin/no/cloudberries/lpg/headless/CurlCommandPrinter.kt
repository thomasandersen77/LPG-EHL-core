package no.cloudberries.lpg.headless

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Prints curl commands for headless debug API endpoints after the application starts.
 * 
 * VIKTIG: Dette viser BARE endepunktene som er tilgjengelige i headless-appen.
 * Webapp-endepunkter (/api/v1/\*) er IKKE tilgjengelige her.
 * 
 * Only active when the debug-api profile is enabled.
 * Uses println for direct console output (easy to copy/paste).
 * All commands include '| jq' for formatted JSON output.
 */
@Component
@Profile("debug-api")
class CurlCommandPrinter(
    @Value("\${server.port:8080}") private val port: Int
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val baseUrl = "http://localhost:$port"
        
        println()
        println("═══════════════════════════════════════════════════════════")
        println("🔧 HEADLESS DEBUG API - CURL COMMANDS")
        println("═══════════════════════════════════════════════════════════")
        println()
        
        // Pump Control Endpoints
        println("⛽ PUMP CONTROL ENDPOINTS (/api/debug):")
        println()
        println("curl $baseUrl/api/debug/health | jq")
        println("curl $baseUrl/api/debug/state/1 | jq")
        println("curl $baseUrl/api/debug/volume/1 | jq")
        println("curl $baseUrl/api/debug/raw-state/1 | jq")
        println("curl -X POST $baseUrl/api/debug/linetest/1 | jq")
        println("curl -X POST $baseUrl/api/debug/unblock/1 | jq")
        println("curl -X POST $baseUrl/api/debug/block/1 | jq")
        println("curl -X POST '$baseUrl/api/debug/settle/1?paymentMethod=CARD' | jq")
        println("curl -X POST $baseUrl/api/debug/reset/1 | jq")
        println()
        
        // Serial Diagnostics Endpoints
        println("📡 SERIAL DIAGNOSTICS (/api/debug/serial):")
        println()
        println("curl $baseUrl/api/debug/serial/ports | jq")
        println("curl $baseUrl/api/debug/serial/status | jq")
        println("curl '$baseUrl/api/debug/serial/health?address=1' | jq")
        println("curl -X POST '$baseUrl/api/debug/serial/smart-scan?timeoutMs=1000&stopOnFirst=true' | jq")
        println("curl -X POST '$baseUrl/api/debug/serial/scan-addresses?port=/dev/ttyUSB0&start=1&end=64&baud=9600&parity=NONE' | jq")
        println("curl -X POST '$baseUrl/api/debug/serial/auto-detect?port=/dev/ttyUSB0&address=1' | jq")
        println()
        
        println("═══════════════════════════════════════════════════════════")
        println("💡 Kommandoene er klare for copy/paste")
        println("💡 Alle kommandoer inkluderer '| jq' for formatert JSON")
        println("💡 Se DEBUG_API_CURL_REFERENCE.md for detaljer")
        println("═══════════════════════════════════════════════════════════")
        println()
    }
}
