#!/usr/bin/env kotlin
import java.net.Socket
import java.net.InetSocketAddress
import java.io.InputStream
import java.io.OutputStream
import kotlin.system.exitProcess

val HOST = System.getenv("ECR_HOST") ?: "192.168.0.41"
val PORT = (System.getenv("ECR_PORT") ?: "8009").toInt()
val TIMEOUT_MS = (System.getenv("ECR_TIMEOUT_MS") ?: "1500").toInt()

const val STX: Byte = 0x02
const val ETX: Byte = 0x03
const val ENQ: Byte = 0x05
const val ACK: Byte = 0x06
const val NAK: Byte = 0x15

fun hexdump(b: ByteArray): String = b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

fun lrc(payload: ByteArray): Byte {
    var x = 0
    for (bb in payload) x = x xor (bb.toInt() and 0xFF)
    x = x xor (ETX.toInt() and 0xFF)
    return (x and 0xFF).toByte()
}

fun frame(payloadAscii: String): ByteArray {
    val payload = payloadAscii.toByteArray(Charsets.US_ASCII)
    val lrc = lrc(payload)
    return byteArrayOf(STX) + payload + byteArrayOf(ETX, lrc)
}

fun readAvailable(input: InputStream, waitMs: Int = 300): ByteArray {
    val start = System.currentTimeMillis()
    val out = ArrayList<Byte>()
    while (System.currentTimeMillis() - start < waitMs) {
        while (input.available() > 0) {
            out += input.read().toByte()
        }
        if (out.isNotEmpty()) break
        Thread.sleep(10)
    }
    return out.toByteArray()
}

fun sendAndPeek(out: OutputStream, input: InputStream, bytes: ByteArray, label: String) {
    println("\n--- $label ---")
    println("TX: ${hexdump(bytes)}")
    out.write(bytes)
    out.flush()
    val rx = readAvailable(input, 700)
    if (rx.isEmpty()) {
        println("RX: <ingenting>")
    } else {
        println("RX: ${hexdump(rx)}")
        val first = rx[0].toInt() and 0xFF
        when (first) {
            ACK.toInt() and 0xFF -> println("Tolking: ACK (06)")
            NAK.toInt() and 0xFF -> println("Tolking: NAK (15)")
            else -> println("Tolking: Første byte = %02X".format(first))
        }
    }
}

val sock = Socket()
sock.soTimeout = TIMEOUT_MS
println("Kobler til $HOST:$PORT (timeout=${TIMEOUT_MS}ms)")
sock.connect(InetSocketAddress(HOST, PORT), TIMEOUT_MS)

sock.getInputStream().use { input ->
    sock.getOutputStream().use { out ->
        // 1) ENQ wake
        sendAndPeek(out, input, byteArrayOf(ENQ), "ENQ (05) wake")

        // 2) Status-aktig test
        sendAndPeek(out, input, frame("S"), "STX 'S' ETX LRC (Status)")

        // 3) Purchase-eksempel
        sendAndPeek(out, input, frame("P,1,100"), "STX 'P,1,100' ETX LRC (Purchase test)")
    }
}

println("\nFerdig.")
