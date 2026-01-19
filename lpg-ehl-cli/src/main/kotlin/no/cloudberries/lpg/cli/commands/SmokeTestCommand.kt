package no.cloudberries.lpg.cli.commands

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.transport.SerialTransport
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

/**
 * Smoke Test Command for validating pump communication before field deployment.
 * 
 * Performs a non-destructive health check:
 * - Connects to serial port (via configured transport)
 * - Sends EHL POLL (STATE request) to each pump address
 * - Validates response checksum
 * - Reports status for each pump
 * 
 * USAGE:
 * ```
 * shell:> smoke
 * shell:> smoke --addresses 1,2
 * shell:> smoke --timeout 3000
 * ```
 */
@ShellComponent
class SmokeTestCommand(
    private val ehlCommunicator: EhlCommunicator,
    private val serialTransport: SerialTransport,
    @Value("\${ehl.serial.port:/dev/ttyUSB0}") private val portName: String,
    @Value("\${ehl.serial.baud-rate:9600}") private val baudRate: Int
) {
    private val logger = LoggerFactory.getLogger(SmokeTestCommand::class.java)
    
    data class PumpTestResult(
        val address: Int,
        val name: String,
        val success: Boolean,
        val statusDescription: String,
        val responseTimeMs: Long
    )
    
    @ShellMethod(
        value = "Run smoke test to verify pump communication",
        key = ["smoke", "smoketest", "healthcheck"]
    )
    fun smokeTest(
        @ShellOption(
            defaultValue = "1,2",
            help = "Comma-separated list of pump addresses to test (e.g., 1,2)"
        ) addresses: String,
        @ShellOption(
            defaultValue = "2000",
            help = "Timeout in milliseconds for each pump response"
        ) timeout: Long
    ): String = runBlocking {
        val addressList = addresses.split(",").mapNotNull { it.trim().toIntOrNull() }
        
        if (addressList.isEmpty()) {
            return@runBlocking """
                ❌ ERROR: No valid addresses provided
                Usage: smoke --addresses 1,2
            """.trimIndent()
        }
        
        val results = mutableListOf<PumpTestResult>()
        
        // Test each pump
        for (address in addressList) {
            val pumpName = getPumpName(address)
            val result = testPump(address, pumpName, timeout)
            results.add(result)
        }
        
        // Generate report
        generateReport(results)
    }
    
    private suspend fun testPump(address: Int, name: String, timeoutMs: Long): PumpTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Send STATE query (POLL)
            val statePacket = EhlPacket(
                address = address,
                command = EhlCommand.STATE,
                data = byteArrayOf()
            )
            
            val response = withTimeoutOrNull(timeoutMs) {
                ehlCommunicator.sendAndReceive(statePacket, timeoutMs)
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            
            if (response != null) {
                val statusCode = if (response.data.isNotEmpty()) {
                    response.data[0].toInt() and 0xFF
                } else {
                    -1
                }
                val statusDescription = mapStatusCode(statusCode)
                
                PumpTestResult(
                    address = address,
                    name = name,
                    success = true,
                    statusDescription = statusDescription,
                    responseTimeMs = elapsed
                )
            } else {
                PumpTestResult(
                    address = address,
                    name = name,
                    success = false,
                    statusDescription = "Timeout (no response within ${timeoutMs}ms)",
                    responseTimeMs = elapsed
                )
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            logger.error("Smoke test failed for pump $address: ${e.message}", e)
            
            PumpTestResult(
                address = address,
                name = name,
                success = false,
                statusDescription = "Error: ${e.message}",
                responseTimeMs = elapsed
            )
        }
    }
    
    private fun generateReport(results: List<PumpTestResult>): String {
        val successCount = results.count { it.success }
        val failCount = results.count { !it.success }
        
        val overallStatus = when {
            failCount == 0 -> "OK"
            successCount == 0 -> "FAIL"
            else -> "WARNING"
        }
        
        val overallEmoji = when (overallStatus) {
            "OK" -> "✅"
            "WARNING" -> "⚠️"
            else -> "❌"
        }
        
        val sb = StringBuilder()
        
        sb.appendLine()
        sb.appendLine("[SMOKE TEST]")
        sb.appendLine("Transport: $portName ($baudRate, 8E1)")
        sb.appendLine("───────────────────────────────────────")
        
        for (result in results) {
            val emoji = if (result.success) "✅" else "❌"
            val status = if (result.success) "OK" else "FAIL"
            sb.appendLine("Pumpe ${result.address} (${result.name}): $emoji $status (${result.statusDescription}) [${result.responseTimeMs}ms]")
        }
        
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("RESULTAT: $overallEmoji $overallStatus")
        sb.appendLine()
        
        if (failCount > 0) {
            sb.appendLine("💡 TIPS:")
            sb.appendLine("  - Sjekk at RS-485-kabelen er koblet til riktig port")
            sb.appendLine("  - Verifiser at pumpen er påslått")
            sb.appendLine("  - Kontroller termineringsmotstanden (120Ω)")
        }
        
        return sb.toString()
    }
    
    private fun getPumpName(address: Int): String {
        return when (address) {
            1 -> "Autogass"
            2 -> "Container"
            else -> "Pumpe #$address"
        }
    }
    
    private fun mapStatusCode(code: Int): String {
        return when (code) {
            0 -> "IDLE"
            1 -> "CALLING"
            2 -> "AUTHORIZED"
            3 -> "BUSY (Pumping)"
            4 -> "STOPPED"
            5 -> "PAYMENT_PENDING"
            -1 -> "Empty response"
            else -> "UNKNOWN ($code)"
        }
    }
}
