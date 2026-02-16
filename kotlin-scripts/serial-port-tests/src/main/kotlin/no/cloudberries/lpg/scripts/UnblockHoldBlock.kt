package no.cloudberries.lpg.scripts

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.protocol.EhlCommand
import no.cloudberries.lpg.protocol.EhlDataParser
import no.cloudberries.lpg.protocol.EhlPacket

/**
 * Maven run:
 *   I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true \
 *   EHL_SERIAL_PORT=/dev/ttyUSB0 EHL_ADDR=33 \
 *   ./mvnw -pl kotlin-scripts -Dexec.mainClass=no.cloudberries.lpg.scripts.UnblockHoldBlockKt exec:java
 */
fun main() {
    cmdUnblockHoldBlock()
}

fun cmdUnblockHoldBlock() {
    ScriptEnv.requireDangerAck()

    val cfg = ScriptEnv.loadSerialConfig()
    val addr = ScriptEnv.envInt("EHL_ADDR", 33)
    val timeoutMs = ScriptEnv.envLong("EHL_TIMEOUT_MS", 1200)
    val retries = ScriptEnv.envInt("EHL_UNBLOCK_RETRIES", 3)
    val holdSeconds = ScriptEnv.envLong("EHL_HOLD_SECONDS", 30)
    val pollMs = ScriptEnv.envLong("EHL_POLL_MS", 400)
    val verifyMs = ScriptEnv.envLong("EHL_VERIFY_MS", 4000)
    val maxRetries = ScriptEnv.envInt("EHL_RETRY_MAX", 0)

    val disableProductSelectFallback = ScriptEnv.envBool("EHL_DISABLE_PRODUCT_SELECT_FALLBACK", false)
    val disableResetFallback = ScriptEnv.envBool("EHL_DISABLE_RESET_FALLBACK", false)

    val transport = ScriptEnv.createTransport(cfg)
    check(transport.connect()) { "Failed to connect to serial port ${cfg.portName}" }
    val comm = ScriptEnv.createCommunicator(transport, rawLogging = cfg.rawLogging, maxRetries = maxRetries)

    fun fmtBytes(data: ByteArray): String = if (data.isEmpty()) "<none>" else data.joinToString(" ") { "%02X".format(it) }
    fun nowMs(): Long = System.currentTimeMillis()

    fun stateOpenForDelivery(sb: Int): Boolean = ScriptEnv.interpretStateByte(sb).openForDelivery

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
        val ok = resp.data.firstOrNull()?.let { ScriptEnv.isVb6OkByte(it) } == true
        println("$label: RX ${resp.command.name} data=${fmtBytes(resp.data)} ackOk=$ok")
        return ok
    }

    println("UNBLOCK/HOLD/BLOCK on ${cfg.portName} addr=$addr (timeout=${timeoutMs}ms, retries=$retries, hold=${holdSeconds}s)")
    println("Safety ack: OK (I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)")
    println()

    runBlocking {
        pollStateOnce()?.let { sb ->
            val s = ScriptEnv.interpretStateByte(sb)
            println("BASELINE STATE: raw=0x${"%02X".format(s.raw)} bits=${s.bits} open_for_delivery=${s.openForDelivery}")
        } ?: println("BASELINE STATE: <no response>")

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
                runCatching {
                    sendWithAckLabel(EhlCommand.PRODUCT_SELECT, data = byteArrayOf(0x30), label = "PRODUCT_SELECT")
                }
            }
            if (!disableResetFallback) {
                println("Fallback: RESET/ZER (0x81) then retry UNBLOCK.")
                runCatching { sendWithAckLabel(EhlCommand.ZER, label = "ZER") }
            }
        }

        if (!unblocked) {
            println("UNBLOCK never ACKed. Will verify by polling STATE and proceed best-effort.")
        }

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
                    val s = ScriptEnv.interpretStateByte(sb)
                    println(
                        "STATE change: raw=0x${"%02X".format(s.raw)} bits=${s.bits} " +
                            "open_for_delivery=${s.openForDelivery} start=${s.startButtonPressed} automode=${s.automode}"
                    )
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

        println()
        println("=== BLOCK ===")
        val blockAck = runCatching { sendWithAckLabel(EhlCommand.BLOCK, label = "BLOCK") }.getOrDefault(false)
        if (!blockAck) println("No VB6-style BLOCK ACK seen (will verify via STATE).")

        println()
        println("=== VERIFY (wait up to ${verifyMs}ms for open_for_delivery=false) ===")
        val verifyDeadline = nowMs() + verifyMs
        var cleared = false
        while (nowMs() < verifyDeadline) {
            val sb = pollStateOnce()
            if (sb != null) {
                val s = ScriptEnv.interpretStateByte(sb)
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
}

