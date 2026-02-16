package no.cloudberries.lpg.scripts

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket

/**
 * Maven run:
 *   EHL_SERIAL_PORT=/tmp/vserial1 EHL_ADDR=1 ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.BaselineSnapshotKt exec:java
 */
fun main() {
    cmdBaselineSnapshot()
}

fun cmdBaselineSnapshot() {
    val cfg = ScriptEnv.loadSerialConfig()
    val addr = ScriptEnv.envInt("EHL_ADDR", 1)
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 1200)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    fun fmtBytes(data: ByteArray): String =
        if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }

    println("Baseline snapshot on ${cfg.portName} @ ${cfg.baudRate} addr=$addr (timeout=${timeoutMs}ms, retries=$maxRetries)")
    println()

    runBlocking {
        // STATE
        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.STATE), timeoutMs)
            val sb = resp.data.firstOrNull()?.toInt()?.and(0xFF)
            if (sb != null) {
                val s = ScriptEnv.interpretStateByte(sb)
                println(
                    "STATE: raw=0x${"%02X".format(s.raw)} bits=${s.bits} " +
                        "open_for_delivery=${s.openForDelivery} start=${s.startButtonPressed} automode=${s.automode}"
                )
            } else {
                println("STATE: RX data=${fmtBytes(resp.data)}")
            }
        }.onFailure { println("STATE: <no response> (${it::class.simpleName}: ${it.message})") }

        // ERROR_QUERY
        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.ERROR_QUERY), timeoutMs)
            val vb6 = runCatching { EhlDataParser.parseErrorData(resp.data) }.getOrNull()
            if (vb6 != null) {
                val (main, sub) = vb6
                println("ERROR_QUERY: main='$main' sub='$sub' (raw=${fmtBytes(resp.data)})")
            } else {
                val legacy = runCatching { EhlDataParser.parseErrorDataLegacy(resp.data) }.getOrNull()
                if (legacy != null) {
                    println("ERROR_QUERY: legacyCode=$legacy (raw=${fmtBytes(resp.data)})")
                } else {
                    println("ERROR_QUERY: RX data=${fmtBytes(resp.data)}")
                }
            }
        }.onFailure { println("ERROR_QUERY: <no response> (${it::class.simpleName}: ${it.message})") }

        // VOLUME
        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.VOLUME), timeoutMs)
            val litres = runCatching { EhlDataParser.parseVolumeDataVb6(resp.data) }.getOrNull()
            if (litres != null) {
                println("VOLUME: ${"%.2f".format(litres)} L (raw=${fmtBytes(resp.data)})")
            } else {
                println("VOLUME: RX data=${fmtBytes(resp.data)}")
            }
        }.onFailure { println("VOLUME: <no response> (${it::class.simpleName}: ${it.message})") }

        // TANK
        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.TANK), timeoutMs)
            println("TANK: RX data=${fmtBytes(resp.data)}")
        }.onFailure { println("TANK: <no response> (${it::class.simpleName}: ${it.message})") }
    }

    transport.disconnect()
}

