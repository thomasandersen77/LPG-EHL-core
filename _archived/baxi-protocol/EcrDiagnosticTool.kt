package no.cloudberries.lpg.payment

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * ECR Diagnostic Tool
 * 
 * Comprehensive diagnostic tool for testing payment terminal connectivity.
 * Performs step-by-step verification and provides clear diagnostic output.
 * 
 * Usage: Run as main application with optional arguments:
 *   - First arg: Terminal IP (default: 192.168.0.4)
 *   - Second arg: Terminal port (default: 8009)
 *   - Third arg: Test amount in øre (default: 100 = 1.00 NOK)
 */
object EcrDiagnosticTool {
    
    @JvmStatic
    fun main(args: Array<String>) {
        val terminalIp = args.getOrNull(0) ?: "192.168.0.4"
        val terminalPort = args.getOrNull(1)?.toIntOrNull() ?: 8009
        val testAmount = args.getOrNull(2)?.toIntOrNull() ?: 100
        
        println()
        println("╔══════════════════════════════════════════════════════════════════╗")
        println("║          ECR DIAGNOSTIC TOOL - Terminal Connectivity Test        ║")
        println("╚══════════════════════════════════════════════════════════════════╝")
        println()
        
        val diagnostic = DiagnosticRunner(terminalIp, terminalPort, testAmount)
        diagnostic.runAll()
    }
}

/**
 * Diagnostic test runner
 */
class DiagnosticRunner(
    private val terminalIp: String,
    private val terminalPort: Int,
    private val testAmount: Int
) {
    private var stepNumber = 0
    private val results = mutableListOf<StepResult>()
    
    fun runAll() {
        printConfig()
        
        // Step 1: Check local network
        runStep("Sjekker lokalt nettverk") { checkLocalNetwork() }
        
        // Step 2: Verify terminal reachability
        runStep("Sjekker at terminal er nåbar") { checkTerminalReachable() }
        
        // Step 3: Test TCP connection
        runStep("Tester TCP-tilkobling") { testTcpConnection() }
        
        // Step 4: Send test command
        runStep("Sender test-kommando (Purchase ${testAmount/100.0} NOK)") { sendTestCommand() }
        
        // Print summary
        printSummary()
    }
    
    private fun printConfig() {
        println("┌─────────────────────────────────────────────────────────────────┐")
        println("│ KONFIGURASJON                                                   │")
        println("├─────────────────────────────────────────────────────────────────┤")
        println("│ Terminal IP:    $terminalIp".padEnd(66) + "│")
        println("│ Terminal Port:  $terminalPort".padEnd(66) + "│")
        println("│ Test beløp:     ${testAmount/100.0} NOK ($testAmount øre)".padEnd(66) + "│")
        println("└─────────────────────────────────────────────────────────────────┘")
        println()
    }
    
    private inline fun runStep(description: String, action: () -> StepResult) {
        stepNumber++
        println("═══════════════════════════════════════════════════════════════════")
        println("STEG $stepNumber: $description")
        println("═══════════════════════════════════════════════════════════════════")
        println()
        
        val result = try {
            action()
        } catch (e: Exception) {
            StepResult.Failure("Exception: ${e.message}")
        }
        
        results.add(result)
        
        when (result) {
            is StepResult.Success -> {
                println()
                println("✅ RESULTAT: ${result.message}")
            }
            is StepResult.Warning -> {
                println()
                println("⚠️  ADVARSEL: ${result.message}")
            }
            is StepResult.Failure -> {
                println()
                println("❌ FEILET: ${result.message}")
            }
        }
        println()
    }
    
    private fun checkLocalNetwork(): StepResult {
        println("Finner lokale nettverksgrensesnitt...")
        println()
        
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
        
        if (interfaces.isEmpty()) {
            return StepResult.Failure("Ingen aktive nettverksgrensesnitt funnet")
        }
        
        var foundLocalIp: String? = null
        
        for (iface in interfaces) {
            val addresses = iface.inetAddresses.toList()
                .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
            
            if (addresses.isNotEmpty()) {
                println("  Interface: ${iface.displayName}")
                for (addr in addresses) {
                    val ip = addr.hostAddress
                    println("    IP: $ip")
                    
                    // Check if this IP is in same subnet as terminal
                    if (isSameSubnet(ip, terminalIp)) {
                        foundLocalIp = ip
                        println("       ↳ ✓ Samme subnet som terminal!")
                    }
                }
            }
        }
        
        return if (foundLocalIp != null) {
            StepResult.Success("Lokalt nettverk OK. Din IP: $foundLocalIp")
        } else {
            StepResult.Warning("Fant ikke IP i samme subnet som terminal ($terminalIp)")
        }
    }
    
    private fun checkTerminalReachable(): StepResult {
        println("Prøver å nå terminal på $terminalIp...")
        
        return try {
            val reachable = InetAddress.getByName(terminalIp).isReachable(3000)
            
            if (reachable) {
                println("  ✓ Terminal svarer på ICMP ping")
                StepResult.Success("Terminal er nåbar på $terminalIp")
            } else {
                println("  ⚠ Terminal svarer ikke på ping (kan fortsatt fungere)")
                StepResult.Warning("Terminal svarer ikke på ping, men TCP kan fortsatt virke")
            }
        } catch (e: Exception) {
            StepResult.Warning("Kunne ikke pinge terminal: ${e.message}")
        }
    }
    
    private fun testTcpConnection(): StepResult {
        println("Oppretter TCP-tilkobling til $terminalIp:$terminalPort...")
        println()
        
        val socket = Socket()
        
        return try {
            socket.connect(InetSocketAddress(terminalIp, terminalPort), 5000)
            
            val localIp = socket.localAddress.hostAddress
            val localPort = socket.localPort
            
            println("  ✓ TCP-tilkobling opprettet!")
            println()
            println("  ┌───────────────────────────────────────────────────────────┐")
            println("  │ TILKOBLINGSDETALJER                                       │")
            println("  ├───────────────────────────────────────────────────────────┤")
            println("  │ Remote (Terminal): $terminalIp:$terminalPort".padEnd(60) + "│")
            println("  │ Local (Din maskin): $localIp:$localPort".padEnd(60) + "│")
            println("  └───────────────────────────────────────────────────────────┘")
            println()
            println("  ⚠️  VIKTIG: Sjekk at '$localIp' er tillatt i terminalens ECR-meny!")
            println("     Gå til: Communication > Kasse > ECR IP")
            println("     Sett ECR IP til '$localIp' eller '0.0.0.0' (tillat alle)")
            
            socket.close()
            StepResult.Success("TCP-tilkobling OK fra $localIp")
            
        } catch (e: java.net.ConnectException) {
            StepResult.Failure("Tilkobling nektet - ECR-mode ikke aktiv på terminal? (${e.message})")
        } catch (e: java.net.SocketTimeoutException) {
            StepResult.Failure("Timeout - Terminal svarer ikke på port $terminalPort")
        } catch (e: Exception) {
            StepResult.Failure("TCP-feil: ${e.message}")
        } finally {
            try { socket.close() } catch (e: Exception) { }
        }
    }
    
    private fun sendTestCommand(): StepResult {
        println("Oppretter tilkobling og sender Purchase-kommando...")
        println()
        
        return try {
            PaymentTerminalClient(terminalIp, terminalPort).use { client ->
                client.connect()
                
                println("  Lokal IP: ${client.localAddress}:${client.localPort}")
                println()
                
                // Build command
                val command = NetsBaxProtocol.createPurchaseCommand(testAmount, "1")
                
                println("  Sender kommando:")
                println("    Payload: P,1,$testAmount")
                println("    HEX:     ${command.joinToString(" ") { "%02X".format(it) }}")
                println("    Debug:   ${with(NetsBaxProtocol) { command.toDebugString() }}")
                println()
                
                // Send and receive
                val response = client.sendCommand(command)
                
                println("  ┌───────────────────────────────────────────────────────────┐")
                println("  │ RESPONS FRA TERMINAL                                      │")
                println("  ├───────────────────────────────────────────────────────────┤")
                println("  │ Bytes mottatt: ${response.rawData.size}".padEnd(60) + "│")
                println("  │ Tid:           ${response.elapsedMs}ms".padEnd(60) + "│")
                println("  │ ACK mottatt:   ${if (response.hasAck) "✅ Ja" else "❌ Nei"}".padEnd(60) + "│")
                println("  │ NAK mottatt:   ${if (response.hasNak) "❌ Ja" else "✅ Nei"}".padEnd(60) + "│")
                println("  │ Komplett frame: ${if (response.hasCompleteFrame) "✅ Ja" else "❌ Nei"}".padEnd(60) + "│")
                println("  └───────────────────────────────────────────────────────────┘")
                println()
                
                if (response.rawData.isNotEmpty()) {
                    println("  Raw HEX:   ${response.toHexString()}")
                    println("  Raw ASCII: ${response.toAsciiString()}")
                    println()
                    
                    // Parse response
                    val parsed = response.parse()
                    println("  Parsed: $parsed")
                }
                
                // Interpret result
                when {
                    response.hasAck && response.hasCompleteFrame -> {
                        StepResult.Success("Full respons mottatt! Terminal kommuniserer korrekt.")
                    }
                    response.hasAck && !response.hasCompleteFrame -> {
                        StepResult.Success("ACK mottatt - terminal aksepterte kommandoen. Sjekk terminalskjermen!")
                    }
                    response.hasNak -> {
                        StepResult.Warning("NAK mottatt - terminal avviste kommandoen. Sjekk terminalstatus.")
                    }
                    response.rawData.isEmpty() -> {
                        StepResult.Failure(
                            "Ingen respons fra terminal!\n" +
                            "     99% sannsynlig årsak: ECR IP whitelist i terminal matcher ikke ${client.localAddress}\n" +
                            "     Løsning: Sett ECR IP til '${client.localAddress}' eller '0.0.0.0' i terminalmenyen"
                        )
                    }
                    else -> {
                        StepResult.Warning("Ukjent respons: ${response.toHexString()}")
                    }
                }
            }
        } catch (e: Exception) {
            StepResult.Failure("Feil under kommunikasjon: ${e.message}")
        }
    }
    
    private fun printSummary() {
        println()
        println("╔══════════════════════════════════════════════════════════════════╗")
        println("║                         OPPSUMMERING                             ║")
        println("╠══════════════════════════════════════════════════════════════════╣")
        
        results.forEachIndexed { index, result ->
            val status = when (result) {
                is StepResult.Success -> "✅"
                is StepResult.Warning -> "⚠️ "
                is StepResult.Failure -> "❌"
            }
            val line = "║ Steg ${index + 1}: $status ${result.shortMessage()}".padEnd(67) + "║"
            println(line)
        }
        
        println("╚══════════════════════════════════════════════════════════════════╝")
        println()
        
        val failures = results.filterIsInstance<StepResult.Failure>()
        val warnings = results.filterIsInstance<StepResult.Warning>()
        
        if (failures.isEmpty() && warnings.isEmpty()) {
            println("🎉 ALLE TESTER BESTÅTT! Terminal er klar til bruk.")
        } else if (failures.isEmpty()) {
            println("⚠️  Tester bestått med advarsler. Se over før produksjonsbruk.")
        } else {
            println("❌ Noen tester feilet. Se feilmeldinger over for detaljer.")
        }
        println()
    }
    
    private fun isSameSubnet(ip1: String, ip2: String): Boolean {
        // Simple /24 subnet check
        val parts1 = ip1.split(".")
        val parts2 = ip2.split(".")
        
        if (parts1.size != 4 || parts2.size != 4) return false
        
        return parts1[0] == parts2[0] && 
               parts1[1] == parts2[1] && 
               parts1[2] == parts2[2]
    }
}

/**
 * Result of a diagnostic step
 */
sealed class StepResult {
    abstract fun shortMessage(): String
    
    data class Success(val message: String) : StepResult() {
        override fun shortMessage() = message.take(50)
    }
    
    data class Warning(val message: String) : StepResult() {
        override fun shortMessage() = message.take(50)
    }
    
    data class Failure(val message: String) : StepResult() {
        override fun shortMessage() = message.lines().first().take(50)
    }
}
