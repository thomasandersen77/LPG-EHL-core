package no.cloudberries.lpg.scripts

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketBuilder

/**
 * Maven run:
 *   I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
 *   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 EHL_PRICE=15.90 \
 *   ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.ProgramPriceKt exec:java
 */
fun main() {
    cmdProgramPrice()
}

fun cmdProgramPrice() {
    ScriptEnv.requireDangerAck()

    val cfg = ScriptEnv.loadSerialConfig()
    val addr = ScriptEnv.envInt("EHL_ADDR", 1)
    val price = ScriptEnv.env("EHL_PRICE", "15.90") // format XX.XX
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 1200)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    val productByte = ScriptEnv.env("EHL_PRODUCT_BYTE", "0x30").trim().let {
        if (it.startsWith("0x", ignoreCase = true)) it.substring(2).toInt(16) else it.toInt()
    }.toByte()

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }

    println("Program price on ${cfg.portName} addr=$addr price=$price (timeout=${timeoutMs}ms)")
    println("Safety ack: OK (I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)")
    println()

    runBlocking {
        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.PRODUCT_SELECT, byteArrayOf(productByte)), timeoutMs)
            val ok = resp.data.firstOrNull()?.let { ScriptEnv.isVb6OkByte(it) } == true
            println("PRODUCT_SELECT: RX data=${fmtBytes(resp.data)} ackOk=$ok")
        }.onFailure { println("PRODUCT_SELECT: <no response> (${it::class.simpleName}: ${it.message})") }

        runCatching {
            val req = EhlPacketBuilder.createPriceProgram(addr, price)
            val resp = comm.sendAndReceive(req, timeoutMs)
            val ok = resp.data.firstOrNull()?.let { ScriptEnv.isVb6OkByte(it) } == true
            println("PROG_PRC: RX data=${fmtBytes(resp.data)} ackOk=$ok")
        }.onFailure { println("PROG_PRC: <no response> (${it::class.simpleName}: ${it.message})") }

        runCatching {
            val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.PRICE), timeoutMs)
            val parsed = runCatching { EhlDataParser.parsePriceData(resp.data) }.getOrNull()
            if (parsed != null) {
                println("PRICE readback: $parsed (raw=${fmtBytes(resp.data)})")
            } else {
                println("PRICE readback: RX data=${fmtBytes(resp.data)}")
            }
        }.onFailure { println("PRICE readback: <no response> (${it::class.simpleName}: ${it.message})") }
    }

    transport.disconnect()
}

