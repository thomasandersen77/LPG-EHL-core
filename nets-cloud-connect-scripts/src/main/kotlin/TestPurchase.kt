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
 * STEP 3: Test Purchase Transaction (1 krone)
 *
 * Dette scriptet:
 * 1. Logger inn og får JWT token
 * 2. Kobler til WebSocket med Bearer token
 * 3. Sender Purchase-kommando (1 krone / 100 øre)
 * 4. Lytter på transaksjonsbekreftelse
 * 5. Logger alle detaljer (beløp, korttype, kvitteringsnummer)
 */

fun main() = runBlocking {
    println("━".repeat(70))
    println("💳 STEP 3: Test Purchase Transaction (1 krone)")
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
    log("   Beløp: 1 krone (100 øre)")
    log("")

    val client = HttpClient(CIO) {
        expectSuccess = false
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    try {
        // ============================================================
        // STEG 1: Login
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
        log("")

        if (statusCode != 200) {
            log("❌ Login feilet! Avbryter.")
            return@runBlocking
        }

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
        log("   Token: ${token.take(30)}...")
        log("   Terminal ID: $terminalId")
        log("")

        // ============================================================
        // STEG 2: WebSocket
        // ============================================================
        log("🌐 STEG 2: Kobler til WebSocket...")
        val wsUrl = baseUrl.replace("https://", "wss://") + "/ws/json"
        log("   URL: $wsUrl")
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
            // STEG 3: Purchase Transaction
            // ============================================================
            log("💳 STEG 3: Sender Purchase-kommando (1 krone)...")
            log("")
            
            // Generate ECRID
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
            val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
            val ecrId = "POS-$timestamp-$suffix"
            
            val purchaseJson = """{"NetsRequest":{"MessageHeader":{"$":{"ECRID":"$ecrId","TerminalID":"$terminalId","VersionNumber":"1"}},"Dfs13TransferAmount":{"TransactionType":"48","OperId":"1","Amount1":"100","Amount2":"0","Amount3":"0","Type2":"48","Type3":"48","HostData":"","OptionalData":""}}}"""

            log("   ECRID: $ecrId")
            log("   TransactionType: 48 (ASCII '0' = Purchase)")
            log("   OperId: 1")
            log("   Amount1: 100 (1 krone)")
            log("")
            log("   JSON Payload (compact):")
            log("   ${purchaseJson.take(120)}...")
            log("")

            send(Frame.Text(purchaseJson))
            log("✅ Purchase-kommando sendt!")
            log("")

            // ============================================================
            // STEG 4: Lytt på meldinger
            // ============================================================
            log("👂 STEG 4: Venter på transaksjonsbekreftelse...")
            log("   (Terminal vil be om kort nå)")
            log("")

            var messageCount = 0
            var transactionComplete = false

            // Timeout etter 120 sekunder (2 minutter for å sette inn kort)
            withTimeoutOrNull(120_000) {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            messageCount++
                            val text = frame.readText()
                            handleMessage(text, messageCount) { msg -> log(msg) }
                            
                            if (text.contains("Dfs13TransactionConfirmed") || 
                                text.contains("Dfs13TransactionCancelled")) {
                                transactionComplete = true
                                break
                            }
                        }
                        is Frame.Binary -> {
                            messageCount++
                            val text = frame.readBytes().decodeToString()
                            handleMessage(text, messageCount) { msg -> log(msg) }
                            
                            if (text.contains("Dfs13TransactionConfirmed") || 
                                text.contains("Dfs13TransactionCancelled")) {
                                transactionComplete = true
                                break
                            }
                        }
                        is Frame.Close -> {
                            log("🔌 WebSocket lukket: ${frame.readReason()}")
                            break
                        }
                        else -> {
                            // Ignore ping/pong
                        }
                    }
                }
            } ?: run {
                log("⏰ Timeout! Ingen transaksjon fullført etter 2 minutter.")
                log("")
            }

            log("📊 Statistikk:")
            log("   Antall meldinger mottatt: $messageCount")
            log("   Transaksjon fullført: ${if (transactionComplete) "✅ JA" else "❌ NEI"}")
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
        val logFile = java.io.File("purchase-test-log.md")
        logFile.writeText("""
# Nets Cloud Connect Purchase Test Log

Testkjøring: ${Instant.now()}

## Logg

```
${logBuilder.toString()}
```

## Oppsummering

Denne testen verifiserer:
- ✅ HTTP login med JWT token
- ✅ WebSocket-tilkobling
- ✅ Purchase transaction (1 krone)
- ✅ Display-meldinger fra terminal
- ✅ Transaksjonsbekreftelse

## Purchase JSON Format

```json
{
  "NetsRequest": {
    "MessageHeader": {
      "$": {
        "ECRID": "POS-YYYYMMDDHHMMSS-randomhex",
        "TerminalID": "42696609",
        "VersionNumber": "1"
      }
    },
    "Dfs13TransferAmount": {
      "TransactionType": "0",  // 0 = Purchase
      "OperId": "1",
      "Amount1": "100",        // 1 krone = 100 øre
      "Amount2": "0",
      "Amount3": "0",
      "Type2": "0",
      "Type3": "0",
      "HostData": "",
      "OptionalData": ""
    }
  }
}
```

## Expected Response

### Display Messages (async)
```json
{
  "NetsResponse": {
    "MessageHeader": { ... },
    "Dfs13Display": {
      "Text": "INSERT CARD"
    }
  }
}
```

### Transaction Confirmed
```json
{
  "NetsResponse": {
    "MessageHeader": { ... },
    "Dfs13TransactionConfirmed": {
      "Amount": "100",
      "CardType": "...",
      "ReceiptNumber": "...",
      "AuthCode": "...",
      ...
    }
  }
}
```

        """.trimIndent())
        
        println("📝 Logg skrevet til: ${logFile.absolutePath}")
    }

    println()
    println("━".repeat(70))
}

fun handleMessage(text: String, messageCount: Int, log: (String) -> Unit) {
    log("━".repeat(70))
    log("📨 Melding #$messageCount mottatt (${text.length} bytes):")
    log("")
    
    // Pretty-print JSON (first 500 chars)
    val preview = if (text.length > 500) text.take(500) + "..." else text
    preview.lines().forEach { line ->
        log("   $line")
    }
    log("")

    // Parse specific fields
    when {
        text.contains("Dfs13Display") -> {
            val textMatch = Regex(""""Text"\s*:\s*"([^"]+)"""").find(text)
            if (textMatch != null) {
                log("📺 DISPLAY: ${textMatch.groupValues[1]}")
                log("")
            }
        }
        text.contains("Dfs13TransactionConfirmed") -> {
            log("✅ TRANSAKSJON BEKREFTET!")
            log("")
            
            // Parse details
            val amountMatch = Regex(""""Amount"\s*:\s*"([^"]+)"""").find(text)
            val cardTypeMatch = Regex(""""CardType"\s*:\s*"([^"]+)"""").find(text)
            val receiptMatch = Regex(""""ReceiptNumber"\s*:\s*"([^"]+)"""").find(text)
            val authCodeMatch = Regex(""""AuthCode"\s*:\s*"([^"]+)"""").find(text)
            
            if (amountMatch != null) {
                val amount = amountMatch.groupValues[1].toLongOrNull() ?: 0L
                log("   Beløp: ${amount / 100.0} kr")
            }
            if (cardTypeMatch != null) {
                log("   Korttype: ${cardTypeMatch.groupValues[1]}")
            }
            if (receiptMatch != null) {
                log("   Kvitteringsnummer: ${receiptMatch.groupValues[1]}")
            }
            if (authCodeMatch != null) {
                log("   Autorisasjonskode: ${authCodeMatch.groupValues[1]}")
            }
            log("")
        }
        text.contains("Dfs13TransactionCancelled") -> {
            log("❌ TRANSAKSJON KANSELLERT")
            log("")
        }
        text.contains("MethodRejected") -> {
            val codeMatch = Regex(""""Code"\s*:\s*"([^"]+)"""").find(text)
            val infoMatch = Regex(""""Info"\s*:\s*"([^"]+)"""").find(text)
            log("⚠️  METHOD REJECTED")
            if (codeMatch != null) log("   Code: ${codeMatch.groupValues[1]}")
            if (infoMatch != null) log("   Info: ${infoMatch.groupValues[1]}")
            log("")
        }
        text.contains("Dfs13Error") -> {
            val errorCodeMatch = Regex(""""ErrorCode"\s*:\s*(\d+)""").find(text)
            val errorStringMatch = Regex(""""ErrorString"\s*:\s*"([^"]+)"""").find(text)
            log("❌ FEIL")
            if (errorCodeMatch != null) log("   ErrorCode: ${errorCodeMatch.groupValues[1]}")
            if (errorStringMatch != null) log("   ErrorString: ${errorStringMatch.groupValues[1]}")
            log("")
        }
    }
}
