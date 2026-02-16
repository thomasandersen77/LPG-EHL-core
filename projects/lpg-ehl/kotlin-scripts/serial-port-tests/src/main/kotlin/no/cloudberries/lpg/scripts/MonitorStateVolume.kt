package no.cloudberries.lpg.scripts

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket

/**
 * Maven run:
 *   EHL_SERIAL_PORT=/tmp/vserial1 EHL_ADDR=1 ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.MonitorStateVolumeKt exec:java
 */
fun main() {
    cmdMonitorStateVolume()
}

fun cmdMonitorStateVolume() {
    val cfg = ScriptEnv.loadSerialConfig()
    val addr = ScriptEnv.envInt("EHL_ADDR", 1)
    val seconds = ScriptEnv.envLong("EHL_MONITOR_SECONDS", 10)
    val intervalMs = ScriptEnv.envLong("EHL_MONITOR_INTERVAL_MS", 500)
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 800)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    fun fmtState(sb: Int): String {
        val s = ScriptEnv.interpretStateByte(sb)
        return "raw=0x${"%02X".format(s.raw)} bits=${s.bits} open_for_delivery=${s.openForDelivery} start=${s.startButtonPressed} automode=${s.automode}"
    }

    fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }

    println("Monitoring STATE+VOLUME on ${cfg.portName} addr=$addr for ${seconds}s (interval=${intervalMs}ms, timeout=${timeoutMs}ms)")

    runBlocking {
        val deadline = System.currentTimeMillis() + (seconds * 1000)
        var lastState: Int? = null
        var lastVol: Double? = null

        while (System.currentTimeMillis() < deadline) {
            runCatching {
                val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.STATE), timeoutMs)
                val sb = resp.data.firstOrNull()?.toInt()?.and(0xFF)
                if (sb != null && (lastState == null || sb != lastState)) {
                    println("STATE: ${fmtState(sb)}")
                    lastState = sb
                }
            }

            runCatching {
                val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.VOLUME), timeoutMs)
                val litres = runCatching { EhlDataParser.parseVolumeDataVb6(resp.data) }.getOrNull()
                if (litres != null && (lastVol == null || litres != lastVol)) {
                    println("VOLUME: ${"%.2f".format(litres)} L (raw=${fmtBytes(resp.data)})")
                    lastVol = litres
                }
            }

            delay(intervalMs)
        }
    }

    transport.disconnect()
}

