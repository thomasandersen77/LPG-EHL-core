#!/usr/bin/env kotlin
@file:DependsOn("no.cloudberries.lpg:lpg-transport:0.0.1-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.13")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.RetryConfig
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket
import no.cloudberries.lpg.protocol.EhlPacketBuilder

fun env(name: String, defaultValue: String? = null): String =
    System.getenv(name) ?: defaultValue ?: error("Missing env var: $name")

fun envInt(name: String, defaultValue: Int? = null): Int = env(name, defaultValue?.toString()).toInt()
fun envLong(name: String, defaultValue: Long? = null): Long = env(name, defaultValue?.toString()).toLong()
fun envBool(name: String, defaultValue: Boolean = false): Boolean =
    (System.getenv(name) ?: defaultValue.toString()).trim().lowercase() in setOf("1", "true", "yes", "y", "on")

fun parseParity(s: String): Int = when (s.trim().uppercase()) {
    "NONE", "NO", "N" -> SerialPort.NO_PARITY
    "EVEN", "E" -> SerialPort.EVEN_PARITY
    "ODD", "O" -> SerialPort.ODD_PARITY
    "MARK" -> SerialPort.MARK_PARITY
    "SPACE" -> SerialPort.SPACE_PARITY
    else -> error("Unknown parity '$s' (expected NONE/EVEN/ODD/MARK/SPACE)")
}

fun parseStopBits(s: String): Int = when (s.trim()) {
    "1" -> SerialPort.ONE_STOP_BIT
    "2" -> SerialPort.TWO_STOP_BITS
    else -> error("Unknown stop bits '$s' (expected 1 or 2)")
}

data class ScriptSerialConfig(
    val portName: String,
    val baudRate: Int,
    val dataBits: Int,
    val stopBits: Int,
    val parity: Int,
    val rs485Enabled: Boolean,
    val rs485RtsBeforeMs: Int,
    val rs485RtsAfterMs: Int,
    val readTimeoutMs: Int,
    val writeTimeoutMs: Int,
    val rawLogging: Boolean
)

fun loadSerialConfig(): ScriptSerialConfig {
    val portName = env("EHL_SERIAL_PORT", "/tmp/vserial1")
    val baudRate = envInt("EHL_BAUD", 9600)
    val dataBits = envInt("EHL_DATA_BITS", 8)
    val stopBits = parseStopBits(env("EHL_STOP_BITS", "1"))
    val parity = parseParity(env("EHL_PARITY", "NONE"))

    val rs485Enabled = envBool("EHL_RS485_ENABLED", false)
    val rs485RtsBeforeMs = envInt("EHL_RS485_RTS_BEFORE_MS", 0)
    val rs485RtsAfterMs = envInt("EHL_RS485_RTS_AFTER_MS", 0)

    val readTimeoutMs = envInt("EHL_READ_TIMEOUT_MS", 3000)
    val writeTimeoutMs = envInt("EHL_WRITE_TIMEOUT_MS", 1000)
    val rawLogging = envBool("EHL_RAW_LOG", false)

    return ScriptSerialConfig(
        portName = portName,
        baudRate = baudRate,
        dataBits = dataBits,
        stopBits = stopBits,
        parity = parity,
        rs485Enabled = rs485Enabled,
        rs485RtsBeforeMs = rs485RtsBeforeMs,
        rs485RtsAfterMs = rs485RtsAfterMs,
        readTimeoutMs = readTimeoutMs,
        writeTimeoutMs = writeTimeoutMs,
        rawLogging = rawLogging
    )
}

fun createTransport(cfg: ScriptSerialConfig): SerialPortManager {
    val spCfg = SerialPortConfig(
        portName = cfg.portName,
        baudRate = cfg.baudRate,
        dataBits = cfg.dataBits,
        stopBits = cfg.stopBits,
        parity = cfg.parity,
        rs485Enabled = cfg.rs485Enabled,
        rs485DelayRtsBeforeSendMs = cfg.rs485RtsBeforeMs,
        rs485DelayRtsAfterSendMs = cfg.rs485RtsAfterMs,
        readTimeout = cfg.readTimeoutMs,
        writeTimeout = cfg.writeTimeoutMs
    )
    return SerialPortManager(spCfg)
}

fun createCommunicator(transport: SerialPortManager, rawLogging: Boolean, maxRetries: Int): EhlCommunicator {
    val retry = if (maxRetries <= 0) RetryConfig.NO_RETRY else RetryConfig(maxRetries = maxRetries)
    return EhlCommunicator(transport = transport, enableRawLogging = rawLogging, retryConfig = retry)
}

fun requireDangerAck() {
    val ok = (System.getenv("I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE") ?: "").trim().lowercase() == "true"
    require(ok) {
        "Refusing to run. Set env I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true to proceed."
    }
}

fun isVb6OkByte(b: Byte): Boolean {
    val x = b.toInt() and 0xFF
    return x == 0x1E || x == 0x30
}

requireDangerAck()

val cfg = loadSerialConfig()
val addr = envInt("EHL_ADDR", 1)
val price = env("EHL_PRICE", "15.90")  // format: XX.XX
val timeoutMs = envLong("EHL_TIMEOUT_MS", 1200)
val maxRetries = envInt("EHL_RETRY_MAX", 0)

val productByte = env("EHL_PRODUCT_BYTE", "0x30").trim().let {
    if (it.startsWith("0x", ignoreCase = true)) it.substring(2).toInt(16) else it.toInt()
}.toByte()

val transport = createTransport(cfg)
check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
val comm = createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }

println("Program price on ${cfg.portName} addr=$addr price=$price (timeout=${timeoutMs}ms)")
println("Safety ack: OK (I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)")
println()

runBlocking {
    // PRODUCT_SELECT first (VB6 fallback behavior)
    runCatching {
        val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.PRODUCT_SELECT, byteArrayOf(productByte)), timeoutMs)
        val ok = resp.data.firstOrNull()?.let { isVb6OkByte(it) } == true
        println("PRODUCT_SELECT: RX data=${fmtBytes(resp.data)} ackOk=$ok")
    }.onFailure { println("PRODUCT_SELECT: <no response> (${it::class.simpleName}: ${it.message})") }

    // PROG_PRC
    runCatching {
        val req = EhlPacketBuilder.createPriceProgram(addr, price)
        val resp = comm.sendAndReceive(req, timeoutMs)
        val ok = resp.data.firstOrNull()?.let { isVb6OkByte(it) } == true
        println("PROG_PRC: RX data=${fmtBytes(resp.data)} ackOk=$ok")
    }.onFailure { println("PROG_PRC: <no response> (${it::class.simpleName}: ${it.message})") }

    // PRICE readback (best-effort)
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

