package no.cloudberries.norgesgass.baxi.client

import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import no.cloudberries.norgesgass.baxi.events.LastFinancialResultEvent
import no.cloudberries.norgesgass.baxi.events.LocalModeEvent
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal implementation that speaks the frame protocol used by our
 * `lpg-ehl-payment-terminal-sim` Baxi TCP simulator (`BaxiTcpServer`).
 *
 * It is NOT a full vendor implementation, but it is good enough for:
 * - Integration tests against the simulator
 * - Local lab runs where the simulator is used
 */
class BaxiClientImpl : BaxiClient {

    private var listener: BaxiEventListener? = null
    private val running = AtomicBoolean(false)
    private val ioLock = Any()

    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var readerThread: Thread? = null

    override fun setEventListener(listener: BaxiEventListener?) {
        this.listener = listener
    }

    override fun open(config: BaxiIniConfig): OpenResult {
        if (running.get()) return OpenResult(callResult = 1)

        return try {
            val s = Socket()
            s.tcpNoDelay = true
            s.keepAlive = true
            s.connect(InetSocketAddress(config.hostIpAddress, config.hostPort), 5_000)

            synchronized(ioLock) {
                socket = s
                input = s.getInputStream()
                output = s.getOutputStream()
            }

            running.set(true)
            readerThread = Thread({ readerLoop() }, "baxi-client-reader").apply {
                isDaemon = true
                start()
            }

            // Send OPEN command (0x41). The simulator ACKs and later re-sends TERMINAL_READY.
            sendFrame(byteArrayOf(0x41))

            OpenResult(callResult = 1)
        } catch (e: Exception) {
            safeClose()
            OpenResult(
                callResult = 0,
                methodRejectCode = 1,
                methodRejectInfo = "Failed to connect/open: ${e.message}",
            )
        }
    }

    override fun closeTerminal(): CloseResult {
        return try {
            if (running.get()) {
                // Send CLOSE command (0x43). Ignore ACK.
                sendFrame(byteArrayOf(0x43))
            }
            safeClose()
            CloseResult(callResult = 1)
        } catch (e: Exception) {
            safeClose()
            CloseResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = e.message)
        }
    }

    override fun transferAmount(args: TransferAmountArgs): CallAcceptResult {
        if (!running.get()) return CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = "Not open")
        return try {
            val payload = buildString {
                append("operId=").append(args.operId).append(';')
                append("type1=").append(args.type1).append(';')
                append("amount1=").append(args.amount1).append(';')
                append("type2=").append(args.type2).append(';')
                append("amount2=").append(args.amount2).append(';')
                append("type3=").append(args.type3).append(';')
                append("amount3=").append(args.amount3).append(';')
                if (args.optionalData != null) append("optionalData=").append(args.optionalData)
            }.toByteArray(Charsets.UTF_8)

            val frame = ByteArray(1 + payload.size)
            frame[0] = 0x51
            System.arraycopy(payload, 0, frame, 1, payload.size)
            sendFrame(frame)
            CallAcceptResult(callResult = 1)
        } catch (e: Exception) {
            CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = e.message)
        }
    }

    override fun administration(args: AdministrationArgs): CallAcceptResult {
        if (!running.get()) return CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = "Not open")
        return try {
            val payload = buildString {
                append("admCode=").append(args.admCode).append(';')
                append("operId=").append(args.operId).append(';')
                if (args.optionalData != null) append("optionalData=").append(args.optionalData)
            }.toByteArray(Charsets.UTF_8)

            val frame = ByteArray(1 + payload.size)
            frame[0] = 0x53
            System.arraycopy(payload, 0, frame, 1, payload.size)
            sendFrame(frame)
            CallAcceptResult(callResult = 1)
        } catch (e: Exception) {
            CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = e.message)
        }
    }

    override fun sendTld(args: SendTldArgs): CallAcceptResult {
        return CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = "Not implemented by simulator client")
    }

    override fun sendJson(args: SendJsonArgs): CallAcceptResult {
        return CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = "Not implemented by simulator client")
    }

    override fun confirm(args: ConfirmArgs): CallAcceptResult {
        return CallAcceptResult(callResult = 0, methodRejectCode = 1, methodRejectInfo = "Not implemented by simulator client")
    }

    override fun close() {
        safeClose()
        listener = null
    }

    private fun readerLoop() {
        try {
            while (running.get()) {
                val frame = readFrame() ?: break
                dispatchFrame(frame)
            }
        } catch (e: Exception) {
            listener?.onError(1, "Reader error: ${e.message}")
        } finally {
            safeClose()
        }
    }

    private fun dispatchFrame(data: ByteArray) {
        if (data.isEmpty()) return
        val cmd = data[0].toInt() and 0xFF

        when (cmd) {
            0x49 -> {
                // TERMINAL_READY: 0x49 + "1=00000000;"
                listener?.onTerminalReady()
            }

            0x41 -> {
                // Used by simulator for both OPEN ACK (0x41,'1') and DISPLAY (0x41 + 3 spaces + text)
                if (data.size >= 5 && data[1] == 0x20.toByte() && data[2] == 0x20.toByte() && data[3] == 0x20.toByte()) {
                    val text = String(data, 4, data.size - 4, Charsets.UTF_8).trimEnd()
                    listener?.onDisplayText(text, null, null)
                }
            }

            0x42 -> {
                // PRINT: 0x42 + 3 spaces + text
                val text =
                    if (data.size >= 5 && data[1] == 0x20.toByte() && data[2] == 0x20.toByte() && data[3] == 0x20.toByte()) {
                        String(data, 4, data.size - 4, Charsets.UTF_8)
                    } else {
                        String(data, 1, data.size - 1, Charsets.UTF_8)
                    }
                listener?.onPrintText(text.trimEnd())
            }

            0x44 -> {
                val payload = String(data, 1, data.size - 1, Charsets.UTF_8)
                val tokens = payload.split(';', limit = 32)
                val result = tokens.getOrNull(0)?.toIntOrNull()
                val responseCode = tokens.getOrNull(14) ?: tokens.lastOrNull()
                listener?.onLocalMode(
                    LocalModeEvent(
                        result = result,
                        responseCode = responseCode,
                        rejectionSource = null,
                        rejectionReason = null,
                        localModeResultData = null,
                        fields = emptyMap(),
                    ),
                )
            }

            0x45 -> {
                val payload = String(data, 1, data.size - 1, Charsets.UTF_8)
                val parts = payload.split(';', limit = 2)
                listener?.onLastFinancialResult(
                    LastFinancialResultEvent(
                        result = parts.getOrNull(0)?.toIntOrNull(),
                        resultData = parts.getOrNull(1),
                    ),
                )
            }

            else -> {
                // ignore unknown frames
            }
        }
    }

    private fun readFrame(): ByteArray? {
        val inputLocal: InputStream = synchronized(ioLock) { input } ?: return null
        val h = inputLocal.read()
        if (h == -1) return null
        val l = inputLocal.read()
        if (l == -1) return null
        val len = ((h and 0xFF) shl 8) or (l and 0xFF)
        if (len <= 0 || len > 100_000) return null
        val data = ByteArray(len)
        var read = 0
        while (read < len) {
            val n = inputLocal.read(data, read, len - read)
            if (n == -1) return null
            read += n
        }
        return data
    }

    private fun sendFrame(payload: ByteArray) {
        val outLocal: OutputStream = synchronized(ioLock) { output } ?: throw IllegalStateException("Not connected")
        val len = payload.size
        outLocal.write((len shr 8) and 0xFF)
        outLocal.write(len and 0xFF)
        outLocal.write(payload)
        outLocal.flush()
    }

    private fun safeClose() {
        if (!running.getAndSet(false)) return
        synchronized(ioLock) {
            try { input?.close() } catch (_: Exception) {}
            try { output?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
            input = null
            output = null
            socket = null
        }
        readerThread = null
    }
}

