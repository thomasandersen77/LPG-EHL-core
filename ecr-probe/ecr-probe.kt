import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import kotlin.concurrent.thread

private val ISO_8859_1 = Charset.forName("ISO-8859-1")

fun main() {
    val port = 8009
    println("============================================================")
    println("ECR PROBE SERVER (Kotlin)")
    println("Listening on 0.0.0.0:$port")
    println(" - Logs HEX + ASCII")
    println(" - Responds to heartbeats ([00] or NULs)")
    println(" - Tries multiple purchase variants")
    println("============================================================")
    println("Interactive: type a line and press Enter to send it raw (with CRLF).")
    println("Example: [10;1;100;0;0]")
    println("------------------------------------------------------------")

    ServerSocket(port).use { server ->
        while (true) {
            val sock = server.accept()
            println("\n=== CONNECTED ${sock.inetAddress.hostAddress}:${sock.port} @ ${LocalDateTime.now()} ===")
            handleClient(sock)
        }
    }
}

private fun handleClient(sock: Socket) {
    sock.tcpNoDelay = true
    sock.soTimeout = 0

    val input = BufferedInputStream(sock.getInputStream())
    val output = BufferedOutputStream(sock.getOutputStream())

    @Volatile var running = true
    @Volatile var purchaseAttempt = 0
    @Volatile var lastRxWasHeartbeat = false

    fun sendBytes(bytes: ByteArray, label: String) {
        output.write(bytes)
        output.flush()
        println("TX $label | HEX=${hex(bytes)} | ASCII=${ascii(bytes)}")
    }

    fun sendText(text: String, terminator: String, label: String) {
        val payload = (text + terminator).toByteArray(ISO_8859_1)
        sendBytes(payload, "$label ('$text' + ${terminator.escapeVis()})")
    }

    // Interactive sender thread
    thread(isDaemon = true, name = "stdin-sender") {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (running) {
            val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
            if (line.isBlank()) continue
            // Default: send line with CRLF (common for bracket protocols)
            sendText(line, "\r\n", "MANUAL")
        }
    }

    // Receive loop
    val buf = ByteArray(4096)
    try {
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (n == 0) continue

            val data = buf.copyOfRange(0, n)
            val hx = hex(data)
            val asStr = ascii(data)

            // Heuristic heartbeat detection
            val onlyNuls = data.all { it.toInt() == 0 }
            val looksLikeBracketPing = asStr.contains("[00]")
            val isHeartbeat = onlyNuls || looksLikeBracketPing

            if (isHeartbeat) {
                print(".")
                System.out.flush()
                lastRxWasHeartbeat = true

                // Strategy A: respond with ACK (0x06) to any heartbeat
                sendBytes(byteArrayOf(0x06), "ACK(0x06)")

                // Strategy B: also respond with [00] variants (some expect echo)
                // Try with CRLF first (common), then plain.
                sendText("[00]", "\r\n", "PONG")
                // sendBytes("[00]".toByteArray(ISO_8859_1), "PONG-PLAIN")

                // After first few heartbeats, try purchase variants exactly once per connection
                if (purchaseAttempt < 1) {
                    purchaseAttempt++

                    println("\n\n--- Attempting PURCHASE variants (1.00 NOK) ---")
                    // Candidate commands (you can add more quickly)
                    val cmd = "[10;1;100;0;0]" // guess: cmd=10 purchase, id=1, amount=100 øre

                    // Variant 1: send as reply with CRLF
                    sendText(cmd, "\r\n", "PURCHASE-V1")

                    // Variant 2: CR only
                    sendText(cmd, "\r", "PURCHASE-V2")

                    // Variant 3: LF only
                    sendText(cmd, "\n", "PURCHASE-V3")

                    // Variant 4: no terminator (some parse on ])
                    sendBytes(cmd.toByteArray(ISO_8859_1), "PURCHASE-V4 (no terminator)")

                    println("--- Waiting for terminal response... ---\n")
                }

                continue
            } else {
                // Non-heartbeat: print full detail
                println("\nRX ${LocalDateTime.now()} | HEX=$hx | ASCII=$asStr")
                lastRxWasHeartbeat = false

                // If we see explicit errors, surface them
                if (asStr.contains("FEIL", ignoreCase = true) || asStr.startsWith("A000", ignoreCase = true)) {
                    println("!!! Terminal indicates protocol/error message: '$asStr'")
                }

                // If terminal sends bracket frames, try responding with ACK too
                if (asStr.contains("[") && asStr.contains("]")) {
                    sendBytes(byteArrayOf(0x06), "ACK(0x06) after bracket frame")
                }
            }
        }
    } catch (e: Exception) {
        println("\n!!! Exception: ${e.javaClass.simpleName}: ${e.message}")
    } finally {
        running = false
        try { sock.close() } catch (_: Exception) {}
        println("\n=== DISCONNECTED @ ${LocalDateTime.now()} ===")
    }
}

private fun hex(b: ByteArray): String =
    b.joinToString(" ") { "%02X".format(it) }

private fun ascii(b: ByteArray): String {
    val s = b.toString(ISO_8859_1)
    // Make control chars visible
    return buildString {
        for (ch in s) {
            append(
                when (ch.code) {
                    0x00 -> "␀"
                    0x02 -> "␂"
                    0x03 -> "␃"
                    0x06 -> "ACK"
                    0x0D -> "␍"
                    0x0A -> "␊"
                    else -> if (ch.isISOControl()) "␟" else ch
                }
            )
        }
    }
}

private fun String.escapeVis(): String =
    this.replace("\r", "\\r").replace("\n", "\\n")

