import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * COMPLETE FLOW: Login → WebSocket → Open → TerminalReady → Purchase (1 krone)
 *
 * This script follows the EXACT sequence that Alejandro's ConnectCloudAdapterImpl uses:
 * 1. HTTP Login → JWT
 * 2. WebSocket connect with bearer token
 * 3. Send Open → wait for TerminalReady (or handle ALREADY_OPEN)
 * 4. Send TransferAmount (Purchase, 1 krone)
 * 5. Wait for LocalMode result
 */

fun main() = runBlocking {
    println("━".repeat(70))
    println("🚀 COMPLETE FLOW: Open → Purchase (1 krone)")
    println("━".repeat(70))
    println()

    val baseUrl = System.getenv("NETS_CLOUD_URL") ?: "https://connectcloud.aws.nets.eu"
    val username = System.getenv("NETS_USERNAME") ?: "cloudberries_shared"
    val password = System.getenv("NETS_PASSWORD") ?: "B8PnVjmVq-SMM9QD"

    val logBuilder = StringBuilder()
    val allMessages = mutableListOf<String>()
    
    fun log(msg: String) {
        val line = "[${Instant.now()}] $msg"
        println(line)
        logBuilder.appendLine(line)
    }

    fun generateEcrId(): String {
        val ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC).format(Instant.now())
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "POS-$ts-$suffix"
    }

    val ecrId = generateEcrId()

    log("📋 Konfigurasjon:")
    log("   ECRID: $ecrId (brukes for hele sesjonen)")
    log("   Beløp: 1 krone (100 øre)")
    log("")

    val client = HttpClient(CIO) {
        expectSuccess = false
        install(WebSockets) { pingInterval = 20_000 }
    }

    try {
        // === LOGIN ===
        log("🔐 [1/4] Logger inn...")
        val loginResp = client.post("$baseUrl/v1/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        val body = loginResp.bodyAsText()
        if (loginResp.status.value != 200) {
            log("❌ Login feilet: ${loginResp.status} - $body")
            return@runBlocking
        }
        val token = Regex(""""token"\s*:\s*"([^"]+)"""").find(body)!!.groupValues[1]
        val terminalId = Regex(""""terminals"\s*:\s*\["([^"]+)"""").find(body)!!.groupValues[1]
        log("✅ Login OK! Terminal: $terminalId")
        log("")

        // === WEBSOCKET ===
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        log("🌐 [2/4] Kobler til WebSocket...")

        client.webSocket(
            request = {
                url.takeFrom(wsUrl)
                headers.append(HttpHeaders.Authorization, "bearer $token")
            }
        ) {
            log("✅ WebSocket tilkoblet!")
            log("")

            // Helper: read next message (binary or text)
            suspend fun readMessage(timeoutMs: Long = 30_000): String? {
                return withTimeoutOrNull(timeoutMs) {
                    for (frame in incoming) {
                        val text = when (frame) {
                            is Frame.Binary -> frame.readBytes().decodeToString()
                            is Frame.Text -> frame.readText()
                            else -> continue
                        }
                        allMessages.add(text)
                        return@withTimeoutOrNull text
                    }
                    null
                }
            }

            // === OPEN ===
            log("📤 [3/4] Sender Open-kommando...")
            val dollar = '$'
            val openJson = """{"NetsRequest":{"MessageHeader":{"${dollar}":{"ECRID":"$ecrId","TerminalID":"$terminalId","VersionNumber":"1"}},"Open":{}}}"""
            send(Frame.Text(openJson))
            log("   Sendt: ${openJson.take(80)}...")

            // Wait for TerminalReady or ALREADY_OPEN
            var terminalReady = false
            var openAttempts = 0
            
            while (!terminalReady && openAttempts < 5) {
                openAttempts++
                val msg = readMessage(15_000)
                if (msg == null) {
                    log("⏰ Timeout - ingen svar på Open")
                    break
                }
                
                log("📨 Open-svar #$openAttempts: ${msg.take(200)}...")
                
                when {
                    msg.contains("Dfs13TerminalReady") -> {
                        log("✅ Terminal KLAR!")
                        terminalReady = true
                    }
                    msg.contains("ALREADY_OPEN") -> {
                        log("✅ Terminal allerede åpen - klar!")
                        terminalReady = true
                    }
                    msg.contains("Dfs13Error") -> {
                        log("❌ Feil fra terminal under Open")
                        break
                    }
                }
            }

            if (!terminalReady) {
                log("❌ Kunne ikke åpne terminal. Avbryter.")
                return@webSocket
            }
            log("")

            // === PURCHASE ===
            log("💳 [4/4] Sender Purchase (1 krone = 100 øre)...")
            log("   ⚠️  KOLLEGA: Tapp kortet NÅ!")
            log("")
            
            val purchaseJson = """{"NetsRequest":{"MessageHeader":{"${dollar}":{"ECRID":"$ecrId","TerminalID":"$terminalId","VersionNumber":"1"}},"Dfs13TransferAmount":{"TransactionType":"48","OperId":"0001","Amount1":"100","Amount2":"0","Amount3":"0","Type2":"48","Type3":"48","HostData":"","OptionalData":""}}}"""
            send(Frame.Text(purchaseJson))
            log("✅ Purchase sendt!")
            log("")

            // Wait for transaction result (2 min timeout for card tap)
            var transactionDone = false
            var msgCount = 0
            
            withTimeoutOrNull(120_000) {
                while (!transactionDone) {
                    val msg = readMessage(120_000) ?: break
                    msgCount++
                    
                    log("━".repeat(50))
                    log("📨 Melding #$msgCount:")
                    
                    // Pretty-print (max 300 chars)
                    log("   ${msg.take(300)}")
                    if (msg.length > 300) log("   ... (${msg.length} bytes total)")
                    log("")
                    
                    when {
                        msg.contains("Dfs13DisplayText") -> {
                            val textMatch = Regex(""""_"\s*:\s*"([^"]+)"""").find(msg)
                            if (textMatch != null) {
                                val displayText = textMatch.groupValues[1].replace("\\r", "").replace("\\n", "")
                                log("📺 TERMINAL DISPLAY: $displayText")
                            }
                        }
                        msg.contains("Dfs13PrintText") -> {
                            log("🖨️  KVITTERING mottatt!")
                        }
                        msg.contains("Dfs13LocalMode") -> {
                            val resultMatch = Regex(""""Result"\s*:\s*"(\d+)"""").find(msg)
                            val result = resultMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
                            
                            if (result == 1) {
                                log("✅ TRANSAKSJON GODKJENT!")
                            } else if (result == 2) {
                                log("❌ TRANSAKSJON AVVIST (Result=$result)")
                                val resultData = Regex(""""ResultData"\s*:\s*"([^"]+)"""").find(msg)
                                if (resultData != null) log("   ResultData: ${resultData.groupValues[1]}")
                            }
                            
                            // Parse alle viktige felter
                            val fields = listOf("TruncatedPAN", "CardIssuerName", "TotalAmount", 
                                "ResponseCode", "StanAuth", "SessionNumber", "TimeStamp",
                                "RejectionSource", "RejectionReason", "AID", "TVR")
                            for (f in fields) {
                                val m = Regex(""""$f"\s*:\s*"([^"]*?)"""").find(msg)
                                if (m != null && m.groupValues[1].isNotBlank()) {
                                    log("   $f: ${m.groupValues[1]}")
                                }
                            }
                            transactionDone = true
                        }
                        msg.contains("Dfs13TerminalReady") -> {
                            log("ℹ️  Terminal klar igjen (etter transaksjon)")
                        }
                        msg.contains("Dfs13Error") -> {
                            val errStr = Regex(""""ErrorString"\s*:\s*"([^"]+)"""").find(msg)
                            log("❌ FEIL: ${errStr?.groupValues?.get(1) ?: "ukjent"}")
                            transactionDone = true
                        }
                        msg.contains("MethodRejected") -> {
                            val info = Regex(""""Info"\s*:\s*"([^"]+)"""").find(msg)
                            log("⚠️  REJECTED: ${info?.groupValues?.get(1) ?: "ukjent"}")
                            transactionDone = true
                        }
                    }
                    log("")
                }
            } ?: run {
                log("⏰ Timeout - ingen transaksjon fullført etter 2 minutter")
            }
            
            log("━".repeat(50))
            log("📊 RESULTAT:")
            log("   Meldinger mottatt: $msgCount")
            log("   Transaksjon fullført: ${if (transactionDone) "JA" else "NEI"}")
        }

    } catch (e: Exception) {
        log("💥 ${e::class.simpleName}: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
        
        // Save log
        val logFile = java.io.File("complete-flow-test-log.md")
        val content = buildString {
            appendLine("# Complete Flow Test Log")
            appendLine("Testkjøring: ${Instant.now()}")
            appendLine()
            appendLine("## Logg")
            appendLine("```")
            append(logBuilder)
            appendLine("```")
            appendLine()
            appendLine("## Alle Meldinger (Raw JSON)")
            allMessages.forEachIndexed { i, msg ->
                appendLine("### Melding ${i + 1}")
                appendLine("```json")
                appendLine(msg)
                appendLine("```")
            }
        }
        logFile.writeText(content)
        println("📝 Logg: ${logFile.absolutePath}")
    }
}
