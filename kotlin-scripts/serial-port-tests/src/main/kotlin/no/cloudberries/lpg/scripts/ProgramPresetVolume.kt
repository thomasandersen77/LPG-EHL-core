package no.cloudberries.lpg.scripts

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketBuilder

/**
 * Maven run:
 *   I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
 *   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_VOLUME_6DIGITS=000500 \
 *   ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.ProgramPresetVolumeKt exec:java
 */
fun main() {
    cmdProgramPresetVolume()
}

fun cmdProgramPresetVolume() {
    ScriptEnv.requireDangerAck()

    val cfg = ScriptEnv.loadSerialConfig()
    val addr = ScriptEnv.envInt("EHL_ADDR", 1)
    val volume6 = ScriptEnv.env("EHL_VOLUME_6DIGITS", "000500")
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 1200)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }

    println("Program volume preset on ${cfg.portName} addr=$addr volume=$volume6 (timeout=${timeoutMs}ms)")
    println("Safety ack: OK (I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)")
    println()

    runBlocking {
        val req = EhlPacketBuilder.createVolumePreset(addr, volume6)
        runCatching {
            val resp = comm.sendAndReceive(req, timeoutMs)
            val ok = resp.data.firstOrNull()?.let { ScriptEnv.isVb6OkByte(it) } == true
            println("PROG_VOLUME: RX data=${fmtBytes(resp.data)} ackOk=$ok")
        }.onFailure { println("PROG_VOLUME: <no response> (${it::class.simpleName}: ${it.message})") }

        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.STATE), timeoutMs)
            val sb = resp.data.firstOrNull()?.toInt()?.and(0xFF)
            if (sb != null) {
                val s = ScriptEnv.interpretStateByte(sb)
                println("STATE: raw=0x${"%02X".format(s.raw)} bits=${s.bits} open_for_delivery=${s.openForDelivery}")
            } else {
                println("STATE: RX data=${fmtBytes(resp.data)}")
            }
        }
    }

    transport.disconnect()
}

