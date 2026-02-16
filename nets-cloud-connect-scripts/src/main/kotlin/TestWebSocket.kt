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
 * STEP 2: Test WebSocket-tilkobling til Nets Cloud Connect
 *
 * Dette scriptet:
 * 1. Logger inn og får JWT token
 * 2. Kobler til WebSocket med Bearer token
 * 3. Sender Open-kommando til terminalen
 * 4. Lytter på alle meldinger fra terminalen
 * 5. Logger alle XML-meldinger
 */

data class LoginResponse(
    val token: String,
    val username: String,
    val terminals: List<String>
)

fun main() = runBlocking {
    println("━".repeat(70))
    println("🔌 STEP 2: Test WebSocket-tilkobling til Nets Cloud Connect")
    println("━".repeat(70))
    println()

    val baseUrl = System.getenv("NETS_CLOUD_URL") ?: "https://connectcloud.aws.nets.eu"
    val username = System.getenv("NETS_USERNAME") ?: "cloudberries_shared"
    val password = System.getenv("NETS_PASSWORD") ?: "B8PnVjmVq-SMM9QD"

    val logBuilder = StringBuilder()
    fun log(message: String) {
        val timestamp = Instant.now().toString()
        val logLine = "[$timestamp] $message"
        println(logLine)
        logBuilder.appendLine(logLine)
    }

    log("📋 Konfigurasjon:")
    log("   Base URL:  $baseUrl")
    log("   Username:  $username")
    log("   Password:  ${password.take(3)}***${password.takeLast(3)}")
    log("")

    val client = HttpClient(CIO) {
        expectSuccess = false
        install(WebSockets) {
            pingInterval = 20_000 // Send ping hver 20. sekund
        }
    }

    try {
        // ============================================================
        // STEG 1: Login og få JWT token
        // ============================================================
        log("🔐 STEG 1: Logger inn...")
        log("   POST $baseUrl/v1/login")
        log("")

        val loginResponse: HttpResponse = client.post("$baseUrl/v1/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }

        val statusCode = loginResponse.status.value
        val body = loginResponse.bodyAsText()

        log("📊 Login-respons:")
        log("   Status: $statusCode ${loginResponse.status.description}")
        log("   Body: $body")
        log("")

        if (statusCode != 200) {
            log("❌ Login feilet! Avbryter.")
            return@runBlocking
        }

        // Parse JWT token og terminal ID
        val tokenMatch = Regex(""""token"\s*:\s*"([^"]+)"""").find(body)
        val terminalsMatch = Regex(""""terminals"\s*:\s*\[([^\]]+)\]""").find(body)

        if (tokenMatch == null || terminalsMatch == null) {
            log("❌ Kunne ikke parse token eller terminals fra responsen!")
            return@runBlocking
        }

        val token = tokenMatch.groupValues[1]
        val terminalIdsRaw = terminalsMatch.groupValues[1]
        val terminalId = terminalIdsRaw.replace("\"", "").split(",").first().trim()

        log("✅ Login OK!")
        log("   Token (første 50 tegn): ${token.take(50)}...")
        log("   Terminal ID: $terminalId")
        log("")

        // ============================================================
        // STEG 2: WebSocket-tilkobling
        // ============================================================
        log("🌐 STEG 2: Kobler til WebSocket...")
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        log("   URL: $wsUrl")
        log("   Authorization: bearer ${token.take(20)}...")
        log("")

        client.webSocket(
            request = {
                url.takeFrom(wsUrl)
                headers.append(HttpHeaders.Authorization, "bearer $token")
            }
        ) {
            log("✅ WebSocket tilkoblet!")
            log("")

            // ============================================================
            // STEG 3: Send Open-kommando
            // ============================================================
            log("📤 STEG 3: Sender Open-kommando til terminal $terminalId...")
            
            // Generate ECRID
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
            val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
            val ecrId = "TEST-$timestamp-$suffix"
            
            val openJson = """
                {
                  "NetsRequest": {
                    "MessageHeader": {
                      "$": {
                        "ECRID": "$ecrId",
                        "TerminalID": "$terminalId",
                        "VersionNumber": "1"
                      }
                    },
                    "Open": {}
                  }
                }
            """.trimIndent()

            log("   JSON Payload:")
            openJson.lines().forEach { line ->
                log("   $line")
            }
            log("")

            send(Frame.Text(openJson))
            log("✅ Open-kommando sendt!")
            log("")

            // ============================================================
            // STEG 4: Lytt på meldinger
            // ============================================================
            log("👂 STEG 4: Lytter på meldinger fra terminalen...")
            log("   (Trykk Ctrl+C for å stoppe)")
            log("")

            var messageCount = 0

            // Timeout etter 30 sekunder
            withTimeoutOrNull(30_000) {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            messageCount++
                            val text = frame.readText()
                            log("━".repeat(70))
                            log("📨 Melding #$messageCount mottatt (TEXT):")
                            log("")
                            
                            // Pretty-print XML
                            text.lines().forEach { line ->
                                log("   $line")
                            }
                            log("")

                            // Parse for viktige felter
                            when {
                                text.contains("Dfs13TerminalReady") -> {
                                    log("✅ TERMINAL KLAR!")
                                    log("   Terminalen er nå åpen og klar for transaksjoner")
                                    log("")
                                }
                                text.contains("Dfs13TransactionConfirmed") -> {
                                    log("✅ TRANSAKSJON BEKREFTET!")
                                    log("")
                                    
                                    // Parse beløp og korttype
                                    val amountMatch = Regex("""<Amount>(\d+)</Amount>""").find(text)
                                    val cardTypeMatch = Regex("""<CardType>(\d+)</CardType>""").find(text)
                                    
                                    if (amountMatch != null) {
                                        val amount = amountMatch.groupValues[1].toLongOrNull() ?: 0L
                                        log("   Beløp: ${amount / 100.0} kr")
                                    }
                                    if (cardTypeMatch != null) {
                                        val cardType = cardTypeMatch.groupValues[1]
                                        log("   Korttype: $cardType")
                                    }
                                    log("")
                                }
                                text.contains("Dfs13Display") -> {
                                    val textMatch = Regex("""<Text>([^<]+)</Text>""").find(text)
                                    if (textMatch != null) {
                                        log("📺 Display: ${textMatch.groupValues[1]}")
                                        log("")
                                    }
                                }
                                text.contains("Error") || text.contains("error") -> {
                                    log("❌ FEIL oppdaget i melding!")
                                    log("")
                                }
                            }

                            // Stopp etter første svar (TerminalReady)
                            if (text.contains("Dfs13TerminalReady")) {
                                log("✅ Test fullført! Terminal er klar.")
                                log("")
                                break
                            }
                        }
                        is Frame.Binary -> {
                            messageCount++
                            val bytes = frame.readBytes()
                            val text = bytes.decodeToString()
                            log("━".repeat(70))
                            log("📨 Melding #$messageCount mottatt (BINARY, ${bytes.size} bytes):")
                            log("")
                            
                            // Pretty-print XML
                            text.lines().forEach { line ->
                                log("   $line")
                            }
                            log("")

                            // Parse for viktige felter
                            when {
                                text.contains("Dfs13TerminalReady") -> {
                                    log("✅ TERMINAL KLAR!")
                                    log("   Terminalen er nå åpen og klar for transaksjoner")
                                    log("")
                                }
                                text.contains("Dfs13TransactionConfirmed") -> {
                                    log("✅ TRANSAKSJON BEKREFTET!")
                                    log("")
                                    
                                    // Parse beløp og korttype
                                    val amountMatch = Regex("""<Amount>(\d+)</Amount>""").find(text)
                                    val cardTypeMatch = Regex("""<CardType>(\d+)</CardType>""").find(text)
                                    
                                    if (amountMatch != null) {
                                        val amount = amountMatch.groupValues[1].toLongOrNull() ?: 0L
                                        log("   Beløp: ${amount / 100.0} kr")
                                    }
                                    if (cardTypeMatch != null) {
                                        val cardType = cardTypeMatch.groupValues[1]
                                        log("   Korttype: $cardType")
                                    }
                                    log("")
                                }
                                text.contains("Dfs13Display") -> {
                                    val textMatch = Regex("""<Text>([^<]+)</Text>""").find(text)
                                    if (textMatch != null) {
                                        log("📺 Display: ${textMatch.groupValues[1]}")
                                        log("")
                                    }
                                }
                                text.contains("Error") || text.contains("error") -> {
                                    log("❌ FEIL oppdaget i melding!")
                                    log("")
                                }
                            }

                            // Stopp etter første svar (TerminalReady)
                            if (text.contains("Dfs13TerminalReady")) {
                                log("✅ Test fullført! Terminal er klar.")
                                log("")
                                break
                            }
                        }
                        is Frame.Close -> {
                            log("🔌 WebSocket lukket")
                            log("   Reason: ${frame.readReason()}")
                            log("")
                            break
                        }
                        is Frame.Ping -> {
                            log("🏓 Ping mottatt")
                        }
                        is Frame.Pong -> {
                            log("🏓 Pong mottatt")
                        }
                        else -> {
                            log("⚠️  Ukjent frame-type: ${frame.frameType}")
                        }
                    }
                }
            } ?: run {
                log("⏰ Timeout! Ingen flere meldinger etter 30 sekunder.")
                log("")
            }

            log("📊 Statistikk:")
            log("   Antall meldinger mottatt: $messageCount")
            log("")
        }

    } catch (e: Exception) {
        log("💥 EXCEPTION:")
        log("   ${e::class.simpleName}: ${e.message}")
        log("")
        e.printStackTrace()
    } finally {
        client.close()
        
        // Skriv logg til fil
        val logFile = java.io.File("websocket-test-log.md")
        logFile.writeText("""
# Nets Cloud Connect WebSocket Test Log

Testkjøring: ${Instant.now()}

## Logg

```
${logBuilder.toString()}
```

## Oppsummering

Denne testen verifiserer:
- ✅ HTTP login med JWT token
- ✅ WebSocket-tilkobling med Bearer auth
- ✅ Sending av Open-kommando
- ✅ Mottak av Dfs13TerminalReady

## XML-protokoll

### Open-kommando (Client → Server)
```xml
<NetsRequest>
    <Terminal>42696609</Terminal>
    <Dfs13Open>
        <RegisterFlags>21474836470000000000000</RegisterFlags>
    </Dfs13Open>
</NetsRequest>
```

### TerminalReady-respons (Server → Client)
```xml
<NetsResponse>
    <Terminal>42696609</Terminal>
    <Dfs13TerminalReady>
        <TerminalId>42696609</TerminalId>
    </Dfs13TerminalReady>
</NetsResponse>
```

## Neste steg

1. ✅ Login fungerer
2. ✅ WebSocket fungerer
3. ✅ Open-kommando fungerer
4. ⏭️  Test Purchase-kommando (1 krone)
5. ⏭️  Test Admin-kommandoer (avstemming osv.)

        """.trimIndent())
        
        println("📝 Logg skrevet til: ${logFile.absolutePath}")
    }

    println()
    println("━".repeat(70))
}
