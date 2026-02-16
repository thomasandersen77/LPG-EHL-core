#!/usr/bin/env kotlin
@file:DependsOn("no.cloudberries.lpg:lpg-transport:0.0.1-SNAPSHOT")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.13")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.RetryConfig
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket

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

fun bits8(x: Int): String = (7 downTo 0).joinToString("") { i -> if (((x ushr i) and 1) == 1) "1" else "0" }

data class InterpretedState(
    val raw: Int,
    val bits: String,
    val openForDelivery: Boolean,
    val startButtonPressed: Boolean,
    val automode: Boolean
)

fun interpretStateByte(stateByte: Int): InterpretedState {
    val sb = stateByte and 0xFF
    return InterpretedState(
        raw = sb,
        bits = bits8(sb),
        openForDelivery = (sb and 0x02) != 0,
        startButtonPressed = (sb and 0x04) != 0,
        automode = (sb and 0x08) != 0
    )
}

fun requireDangerAck() {
    val ok = (System.getenv("I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE") ?: "").trim().lowercase() == "true"
    require(ok) {
        "Refusing to run. Set env I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true to proceed."
    }
}

fun isVb6OkByte(b: Byte): Boolean {
    val x = b.toInt() and 0xFF
    // Field notes are inconsistent (0x1E vs ASCII '0' = 0x30). Accept both.
    return x == 0x1E || x == 0x30
}

requireDangerAck()

val cfg = loadSerialConfig()
val addr = envInt("EHL_ADDR", 33)
val timeoutMs = envLong("EHL_TIMEOUT_MS", 1200)
val retries = envInt("EHL_UNBLOCK_RETRIES", 3)
val holdSeconds = envLong("EHL_HOLD_SECONDS", 30)
val pollMs = envLong("EHL_POLL_MS", 400)
val verifyMs = envLong("EHL_VERIFY_MS", 4000)
val maxRetries = envInt("EHL_RETRY_MAX", 0)

val disableProductSelectFallback = envBool("EHL_DISABLE_PRODUCT_SELECT_FALLBACK", false)
val disableResetFallback = envBool("EHL_DISABLE_RESET_FALLBACK", false)

val transport = createTransport(cfg)
check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
val comm = createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }
fun nowMs(): Long = System.currentTimeMillis()

fun stateOpenForDelivery(sb: Int): Boolean = interpretStateByte(sb).openForDelivery

suspend fun pollStateOnce(): Int? {
    return runCatching {
        val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.STATE), timeoutMs)
        resp.data.firstOrNull()?.toInt()?.and(0xFF)
    }.getOrNull()
}

suspend fun pollErrorOnce(): Pair<Char, Char>? {
    return runCatching {
        val resp = comm.sendAndReceive(EhlPacket(addr, EhlCommand.ERROR_QUERY), timeoutMs)
        EhlDataParser.parseErrorData(resp.data)
    }.getOrNull()
}

suspend fun sendWithAckLabel(cmd: EhlCommand, data: ByteArray = ByteArray(0), label: String): Boolean {
    val resp = comm.sendAndReceive(EhlPacket(addr, cmd, data), timeoutMs)
    val ok = resp.data.firstOrNull()?.let { isVb6OkByte(it) } == true
    println("$label: RX ${resp.command.name} data=${fmtBytes(resp.data)} ackOk=$ok")
    return ok
}

println("UNBLOCK/HOLD/BLOCK on ${cfg.portName} addr=$addr (timeout=${timeoutMs}ms, retries=$retries, hold=${holdSeconds}s)")
println("Safety ack: OK (I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)")
println()

runBlocking {
    // Baseline STATE
    pollStateOnce()?.let { sb ->
        val s = interpretStateByte(sb)
        println("BASELINE STATE: raw=0x${"%02X".format(s.raw)} bits=${s.bits} open_for_delivery=${s.openForDelivery}")
    } ?: println("BASELINE STATE: <no response>")

    // UNBLOCK attempts
    println()
    println("=== UNBLOCK ===")
    var unblocked = false
    for (attempt in 1..maxOf(1, retries)) {
        println("UNBLOCK attempt $attempt/$retries")
        val ok = runCatching { sendWithAckLabel(EhlCommand.UNBLOCK, label = "UNBLOCK") }.getOrDefault(false)
        if (ok) {
            unblocked = true
            break
        }

        println("UNBLOCK not ACKed per VB6 semantics.")

        if (!disableProductSelectFallback) {
            println("Fallback: PRODUCT_SELECT (0xC3 data=0x30) then retry UNBLOCK.")
            runCatching { sendWithAckLabel(EhlCommand.PRODUCT_SELECT, data = byteArrayOf(0x30), label = "PRODUCT_SELECT") }
        }
        if (!disableResetFallback) {
            println("Fallback: RESET/ZER (0x81) then retry UNBLOCK.")
            runCatching { sendWithAckLabel(EhlCommand.ZER, label = "ZER") }
        }
    }

    if (!unblocked) {
        println("UNBLOCK never ACKed. Will verify by polling STATE and proceed best-effort.")
    }

    // HOLD loop
    println()
    println("=== HOLD (${holdSeconds}s) ===")
    val holdDeadline = nowMs() + holdSeconds * 1000
    val summaryEveryMs = 1000L
    var nextSummaryAt = nowMs()
    var lastState: Int? = null
    var lastErr: Pair<Char, Char>? = null

    while (nowMs() < holdDeadline) {
        pollStateOnce()?.let { sb ->
            if (lastState == null || sb != lastState) {
                val s = interpretStateByte(sb)
                println("STATE change: raw=0x${"%02X".format(s.raw)} bits=${s.bits} open_for_delivery=${s.openForDelivery} start=${s.startButtonPressed} automode=${s.automode}")
                lastState = sb
            }
        }

        pollErrorOnce()?.let { e ->
            if (lastErr == null || e != lastErr) {
                println("ERROR change: main='${e.first}' sub='${e.second}'")
                lastErr = e
            }
        }

        val now = nowMs()
        if (now >= nextSummaryAt) {
            val remaining = ((holdDeadline - now) / 1000).coerceAtLeast(0)
            val stateStr = lastState?.let { "open_for_delivery=${stateOpenForDelivery(it)}" } ?: "open_for_delivery=?"
            val errStr = lastErr?.let { "err=${it.first}-${it.second}" } ?: "err=n/a"
            println("HOLD t_remaining=${remaining}s $stateStr $errStr")
            nextSummaryAt = now + summaryEveryMs
        }

        delay(pollMs)
    }

    // BLOCK
    println()
    println("=== BLOCK ===")
    val blockAck = runCatching { sendWithAckLabel(EhlCommand.BLOCK, label = "BLOCK") }.getOrDefault(false)
    if (!blockAck) println("No VB6-style BLOCK ACK seen (will verify via STATE).")

    // VERIFY open_for_delivery clears
    println()
    println("=== VERIFY (wait up to ${verifyMs}ms for open_for_delivery=false) ===")
    val verifyDeadline = nowMs() + verifyMs
    var cleared = false
    while (nowMs() < verifyDeadline) {
        val sb = pollStateOnce()
        if (sb != null) {
            val s = interpretStateByte(sb)
            if (!s.openForDelivery) {
                println("VERIFY OK: open_for_delivery=false (STATE raw=0x${"%02X".format(s.raw)} bits=${s.bits})")
                cleared = true
                break
            }
        }
        delay(200)
    }
    if (!cleared) {
        println("VERIFY WARN: open_for_delivery did not clear within verify window.")
    }
}

transport.disconnect()

