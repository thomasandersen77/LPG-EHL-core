package no.cloudberries.lpg.scripts

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlPacketBuilder

/**
 * Maven run:
 *   EHL_SERIAL_PORT=/tmp/vserial1 ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.ScanAddressesKt exec:java
 */
fun main() {
    cmdScanAddresses()
}

fun cmdScanAddresses() {
    val cfg = ScriptEnv.loadSerialConfig()

    val range = ScriptEnv.env("EHL_ADDR_RANGE", "1-32").trim()
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 250)
    val delayMs = ScriptEnv.envLong("EHL_DELAY_MS", 20)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    fun parseRange(s: String): IntRange {
        if (!s.contains("-")) {
            val v = s.toInt()
            return v..v
        }
        val (a, b) = s.split("-", limit = 2).map { it.trim().toInt() }
        return if (a <= b) a..b else b..a
    }

    val addrRange = parseRange(range)

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    println("Scanning addresses ${addrRange.first}..${addrRange.last} on ${cfg.portName} @ ${cfg.baudRate}")
    println("Per-address timeout=${timeoutMs}ms, delay=${delayMs}ms, retries=$maxRetries")

    val found = mutableListOf<Int>()

    runBlocking {
        for (addr in addrRange) {
            val req = EhlPacketBuilder.createStateQuery(addr)
            val ok = try {
                val resp = comm.sendAndReceive(req, timeoutMs = timeoutMs)
                val stateByte = resp.data.firstOrNull()?.toInt()?.and(0xFF)
                if (stateByte != null) {
                    val s = ScriptEnv.interpretStateByte(stateByte)
                    println(
                        "ADDR $addr: STATE raw=0x${"%02X".format(s.raw)} bits=${s.bits} " +
                            "open_for_delivery=${s.openForDelivery}"
                    )
                } else {
                    println("ADDR $addr: STATE <no data>")
                }
                true
            } catch (_: Exception) {
                false
            }

            if (ok) found += addr
            delay(delayMs)
        }
    }

    transport.disconnect()

    println()
    if (found.isEmpty()) {
        println("No responding addresses found in range.")
    } else {
        println("Found responding addresses: ${found.joinToString(", ")}")
    }
}

