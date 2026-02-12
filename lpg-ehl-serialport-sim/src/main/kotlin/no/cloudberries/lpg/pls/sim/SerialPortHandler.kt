package no.cloudberries.lpg.pls.sim

import com.fazecast.jSerialComm.SerialPort
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Handles serial port communication using jSerialComm.
 * 
 * @param plsState Optional PLS state - if null, a default state is created
 */
class SerialPortHandler(
    private val portName: String,
    private val baud: Int,
    private val parity: String = "NONE",
    private val mode: FrameMode,
    private val chunked: Boolean,
    private val latencyMs: Int,
    private val logHex: Boolean,
    plsState: PlsState? = null,
    private val fieldConfig: FieldConfig = FieldConfig()
) {
    private val log = LoggerFactory.getLogger(SerialPortHandler::class.java)
    
    private var serialPort: SerialPort? = null
    private val running = AtomicBoolean(false)
    private var readerThread: Thread? = null
    private var unsolicitedThread: Thread? = null
    
    private val state = plsState ?: PlsState()
    private val frameExtractor = FrameExtractor(mode, logHex)

    companion object {
        const val READ_TIMEOUT_MS = 100
    }

    /**
     * Open serial port and start reading.
     */
    fun start() {
        log.info("Opening serial port: {} at {} baud, parity={}, mode={}, chunked={}", portName, baud, parity, mode, chunked)

        // Debug: List all available ports
        val availablePorts = SerialPort.getCommPorts()
        if (availablePorts.isEmpty()) {
            log.warn("jSerialComm found no serial ports on this system")
        } else {
            log.info("Available serial ports detected by jSerialComm:")
            availablePorts.forEach { p ->
                log.info("  - {} ({})", p.systemPortName, p.descriptivePortName)
            }
        }

        val port = SerialPort.getCommPort(portName)
        log.info("Port object created: {} (descriptor: {})", port.systemPortName, port.descriptivePortName)
        log.info("Port exists check: isOpen={}", port.isOpen)
        
        // Detect PTY/pseudo-terminal devices (common on macOS/Linux with socat)
        // PTY devices don't support serial port configuration ioctls, which causes ENOTTY errors
        val isPtyDevice = portName.contains("/dev/tty") || portName.contains("/dev/pts/")
        
        if (isPtyDevice) {
            log.info("Detected PTY device - disabling port configuration to avoid ENOTTY errors")
            port.disablePortConfiguration()
        } else {
            // Configure port with specified parity (only for real serial ports)
            port.baudRate = baud
            port.numDataBits = 8
            port.numStopBits = SerialPort.ONE_STOP_BIT
            port.parity = when (parity.uppercase()) {
                "NONE" -> SerialPort.NO_PARITY
                "EVEN" -> SerialPort.EVEN_PARITY
                "ODD" -> SerialPort.ODD_PARITY
                "MARK" -> SerialPort.MARK_PARITY
                "SPACE" -> SerialPort.SPACE_PARITY
                else -> {
                    log.warn("Unknown parity '{}', defaulting to NONE", parity)
                    SerialPort.NO_PARITY
                }
            }
        }
        
        // Semi-blocking read with timeout
        port.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            READ_TIMEOUT_MS,
            0
        )

        log.info("Attempting to open port...")
        val openResult = port.openPort()
        log.info("Open result: {}", openResult)
        log.info("After open attempt - isOpen: {}, lastErrorCode: {}, lastErrorLocation: {}", 
            port.isOpen, port.lastErrorCode, port.lastErrorLocation)
        
        if (!openResult) {
            throw RuntimeException("Failed to open serial port: $portName (errorCode=${port.lastErrorCode}, errorLocation=${port.lastErrorLocation})")
        }

        serialPort = port
        log.info("Serial port opened successfully: {}", port.systemPortName)

        running.set(true)
        readerThread = Thread({ readLoop() }, "pls-sim-reader")
        readerThread?.start()
        startUnsolicitedVolumeThread()
    }

    /**
     * Stop reading and close port.
     */
    fun stop() {
        log.info("Stopping serial port handler...")
        running.set(false)
        readerThread?.join(1000)
        unsolicitedThread?.join(1000)
        serialPort?.closePort()
        serialPort = null
        log.info("Serial port closed")
    }

    /**
     * Force immediate disconnect (for fault injection).
     */
    fun forceDisconnect() {
        log.warn("🔌 FAULT INJECTION: Force disconnect triggered")
        running.set(false)
        serialPort?.closePort()
        serialPort = null
    }

    /**
     * Main read loop - runs in dedicated thread.
     */
    private fun readLoop() {
        val buffer = ByteArray(256)
        
        while (running.get()) {
            try {
                // Optional latency jitter
                if (latencyMs > 0) {
                    Thread.sleep(Random.nextLong(0, latencyMs.toLong() + 1))
                }

                val port = serialPort ?: break
                val bytesRead = port.readBytes(buffer, buffer.size)
                
                if (bytesRead > 0) {
                    val received = buffer.copyOfRange(0, bytesRead)
                    
                    if (logHex) {
                        log.debug("RX: {} bytes: {}", bytesRead, received.toHexString())
                    }

                    // Extract frames
                    val frames = frameExtractor.append(received)
                    
                    for (frame in frames) {
                        processFrame(frame)
                    }
                }
            } catch (e: InterruptedException) {
                log.debug("Reader thread interrupted")
                break
            } catch (e: Exception) {
                log.error("Error in read loop: {}", e.message, e)
                Thread.sleep(100)  // Avoid tight loop on error
            }
        }
        
        log.debug("Read loop exited")
    }

    /**
     * Process a complete frame.
     */
    private fun processFrame(frame: ByteArray) {
        if (mode == FrameMode.EHL) {
            // Binary EHL protocol
            val ehlFrame = EhlFrameCodec.decode(frame)
            if (ehlFrame == null) {
                log.warn("Invalid EHL frame - ignoring: {}", frame.toHexString())
                return
            }

            // INFO-level RX logging for observability
            val addrInt = ehlFrame.addr.toInt() and 0xFF
            val cmdName = cmdToName(ehlFrame.cmd)
            log.info("⬅️  RX EHL: addr={} cmd={} (0x{}) dataLen={} hex={}",
                addrInt, cmdName, ehlFrame.cmd.toHex(), ehlFrame.data.size, frame.toHexString())

            val result = state.processEhlCommand(ehlFrame)
            
            // null = don't respond (wrong address)
            if (result == null) {
                log.debug("🚫 No response (address mismatch)")
                return
            }
            
            val responseAddr = ehlFrame.addr
            val baseResponse = encodeResult(result)

            val responses = mutableListOf(ResponseFrame(
                cmd = baseResponse.cmd,
                data = baseResponse.data,
                bytes = baseResponse.bytes
            ))
            val knobs = mutableListOf<String>()

            if (fieldConfig.profile == SimProfile.FIELD && ehlFrame.cmd == EhlFrameCodec.CMD_UNBLOCK && fieldConfig.noAckOnUnblock) {
                val stateBytes = state.buildStateData()
                responses.clear()
                responses.add(ResponseFrame(EhlFrameCodec.CMD_STATE, stateBytes, EhlFrameCodec.encode(responseAddr, EhlFrameCodec.CMD_STATE, stateBytes)))
                knobs.add("noAckOnUnblock->STATE")
            }
            if (fieldConfig.profile == SimProfile.FIELD && ehlFrame.cmd == EhlFrameCodec.CMD_BLOCK && fieldConfig.noAckOnBlock) {
                val stateBytes = state.buildStateData()
                responses.clear()
                responses.add(ResponseFrame(EhlFrameCodec.CMD_STATE, stateBytes, EhlFrameCodec.encode(responseAddr, EhlFrameCodec.CMD_STATE, stateBytes)))
                knobs.add("noAckOnBlock->STATE")
            }

            if (fieldConfig.profile == SimProfile.FIELD) {
                val interleaveChance = if (state.isPaymentPendingLike()) 0.6 else 0.15
                if (Random.nextDouble() < interleaveChance) {
                    val volumeBytes = state.buildVolumeData()
                    responses.add(0, ResponseFrame(EhlFrameCodec.CMD_VOLUME, volumeBytes, EhlFrameCodec.encode(responseAddr, EhlFrameCodec.CMD_VOLUME, volumeBytes)))
                    knobs.add("interleavedVolume")
                }
            }

            if (fieldConfig.profile == SimProfile.FIELD && ehlFrame.cmd == EhlFrameCodec.CMD_STATE && Random.nextDouble() < 0.4) {
                val jitterMs = Random.nextLong(20, 81)
                knobs.add("stateJitter=${jitterMs}ms")
                Thread.sleep(jitterMs)
            }

            if (fieldConfig.profile == SimProfile.FIELD && Random.nextDouble() < fieldConfig.dropResponseProbability) {
                log.info("⚙️  FIELD dropResponse: cmd={} addr={}", cmdName, addrInt)
                return
            }

            if (fieldConfig.profile == SimProfile.FIELD && responses.size > 1 && Random.nextDouble() < fieldConfig.concatFramesProbability) {
                knobs.add("concatFrames")
                if (knobs.isNotEmpty()) {
                    log.info("⚙️  FIELD knobs: {}", knobs.joinToString(", "))
                }
                val concatenated = concatResponses(responses)
                responses.forEach { resp ->
                    val respCmdName = cmdToName(resp.cmd)
                    log.info("➡️  TX EHL: addr={} cmd={} (0x{}) dataLen={} hex={}",
                        addrInt, respCmdName, resp.cmd.toHex(), resp.data.size, resp.bytes.toHexString())
                }
                sendResponse(concatenated, knobs)
            } else {
                if (knobs.isNotEmpty()) {
                    log.info("⚙️  FIELD knobs: {}", knobs.joinToString(", "))
                }
                responses.forEach { resp ->
                    val respCmdName = cmdToName(resp.cmd)
                    log.info("➡️  TX EHL: addr={} cmd={} (0x{}) dataLen={} hex={}",
                        addrInt, respCmdName, resp.cmd.toHex(), resp.data.size, resp.bytes.toHexString())
                    sendResponse(resp.bytes, knobs)
                }
            }

        } else {
            // ASCII protocol (LINE or STX_ETX)
            val command = String(frame, Charsets.US_ASCII)
            log.info("Frame received: '{}'", command)

        val result = state.processCommand(command)

        val response = when (result) {
            is CommandResult.OK -> buildResponse("OK")
            is CommandResult.ACK -> buildResponse("ACK")
            is CommandResult.Status -> buildResponse(if (result.blocked) "BLOCKED" else "UNBLOCKED")
            is CommandResult.Ignored -> null
        }

            if (response != null) {
                sendResponse(response)
            }
        }
    }

    private fun Byte.toHex(): String = "%02X".format(this)

    /** Map command byte to readable name */
    private fun cmdToName(cmd: Byte): String = when (cmd) {
        EhlFrameCodec.CMD_OK -> "OK"
        EhlFrameCodec.CMD_ERROR_DATA -> "ERROR_DATA"
        EhlFrameCodec.CMD_LINETEST -> "LINETEST"
        EhlFrameCodec.CMD_STATE -> "STATE"
        EhlFrameCodec.CMD_ERROR_QUERY -> "ERROR_QUERY"
        EhlFrameCodec.CMD_VOLUME -> "VOLUME"
        EhlFrameCodec.CMD_PRICE -> "PRICE"
        EhlFrameCodec.CMD_PRICE_ALT -> "PRICE_ALT"
        EhlFrameCodec.CMD_BLOCK -> "BLOCK"
        EhlFrameCodec.CMD_UNBLOCK -> "UNBLOCK"
        EhlFrameCodec.CMD_STOP -> "STOP"
        EhlFrameCodec.CMD_RESET -> "RESET"
        EhlFrameCodec.CMD_TANKBIT -> "TANKBIT"
        else -> "0x${cmd.toHex()}"
    }

    /**
     * Build response according to mode (not used for EHL).
     */
    private fun buildResponse(text: String): ByteArray {
        return when (mode) {
            FrameMode.LINE -> "$text\n".toByteArray(Charsets.US_ASCII)
            FrameMode.STX_ETX -> {
                val payload = text.toByteArray(Charsets.US_ASCII)
                ByteArray(payload.size + 2).also { buf ->
                    buf[0] = FrameExtractor.STX
                    payload.copyInto(buf, 1)
                    buf[buf.size - 1] = FrameExtractor.ETX
                }
            }
            FrameMode.EHL -> {
                // EHL mode uses EhlFrameCodec.encode() directly
                throw IllegalStateException("buildResponse should not be called in EHL mode")
            }
        }
    }

    /**
     * Send response, optionally in chunks for realism.
     * For EHL mode, may corrupt checksum if fault injection is enabled.
     */
    private fun sendResponse(response: ByteArray, knobs: List<String> = emptyList()) {
        val port = serialPort ?: return
        
        // Apply checksum corruption for EHL frames if fault injection enabled
        val finalResponse = if (mode == FrameMode.EHL && response.size >= 6 && state.shouldCorruptChecksum()) {
            log.warn("⚠️  FAULT INJECTION: Corrupting checksum")
            response.clone().also { corrupted ->
                // Flip random bits in checksum byte (second-to-last byte)
                val chkIdx = corrupted.size - 2
                corrupted[chkIdx] = (corrupted[chkIdx].toInt() xor Random.nextInt(1, 256)).toByte()
            }
        } else {
            response
        }

        if (logHex) {
            log.debug("TX: {} bytes: {}", finalResponse.size, finalResponse.toHexString())
        }

        val useChunked = shouldChunkResponses()
        val useInterCharDelay = fieldConfig.profile == SimProfile.FIELD && fieldConfig.interCharacterDelayMs.max > 0

        if (useChunked && finalResponse.size > 1) {
            writeChunked(port, finalResponse, useInterCharDelay)
            if (knobs.isNotEmpty()) {
                log.debug("TX knobs: {}", knobs.joinToString(", "))
            }
        } else {
            if (useInterCharDelay) {
                writeWithInterCharacterDelay(port, finalResponse)
            } else {
                port.writeBytes(finalResponse, finalResponse.size)
            }
            if (mode != FrameMode.EHL) {
                log.debug("TX: '{}'", String(finalResponse, Charsets.US_ASCII).trim())
            } else {
                log.debug("TX: EHL frame {} bytes", finalResponse.size)
            }
        }
    }

    /**
     * Write response in random 2-5 chunks with 5-30ms delays.
     */
    private fun writeChunked(port: SerialPort, response: ByteArray, useInterCharDelay: Boolean) {
        val numChunks = Random.nextInt(2, 6).coerceAtMost(response.size)
        val chunkSize = response.size / numChunks
        
        var offset = 0
        var chunkNum = 0
        
        while (offset < response.size) {
            val remaining = response.size - offset
            val thisChunk = if (chunkNum == numChunks - 1) {
                remaining  // Last chunk gets remainder
            } else {
                chunkSize.coerceAtMost(remaining)
            }
            
            val chunk = response.copyOfRange(offset, offset + thisChunk)
            if (useInterCharDelay) {
                writeWithInterCharacterDelay(port, chunk)
            } else {
                port.writeBytes(chunk, chunk.size)
            }
            
            if (logHex) {
                log.debug("TX chunk {}: {}", chunkNum + 1, chunk.toHexString())
            }
            
            offset += thisChunk
            chunkNum++
            
            if (offset < response.size && running.get()) {
                Thread.sleep(Random.nextLong(5, 31))
            }
        }
        
        if (mode != FrameMode.EHL) {
            log.debug("TX (chunked {}x): '{}'", chunkNum, String(response, Charsets.US_ASCII).trim())
        } else {
            log.debug("TX (chunked {}x): EHL frame {} bytes", chunkNum, response.size)
        }
    }

    private fun writeWithInterCharacterDelay(port: SerialPort, response: ByteArray) {
        for (i in response.indices) {
            port.writeBytes(byteArrayOf(response[i]), 1)
            if (i < response.size - 1) {
                Thread.sleep(fieldConfig.interCharacterDelayMs.nextMs())
            }
        }
    }

    private fun shouldChunkResponses(): Boolean {
        if (fieldConfig.profile == SimProfile.FIELD) {
            return when (fieldConfig.readChunkingMode) {
                ReadChunkingMode.RANDOM -> Random.nextBoolean()
                ReadChunkingMode.OFF -> false
            }
        }
        return chunked
    }

    private fun startUnsolicitedVolumeThread() {
        if (fieldConfig.profile != SimProfile.FIELD || mode != FrameMode.EHL) {
            return
        }
        unsolicitedThread = Thread({
            try {
                while (running.get()) {
                    if (state.isPaymentPendingLike()) {
                        Thread.sleep(fieldConfig.unsolicitedVolumeIntervalMs.nextMs())
                        if (!running.get()) {
                            break
                        }
                        val addr = state.getPrimaryAddressByte()
                        val volumeBytes = state.buildVolumeData()
                        val frame = EhlFrameCodec.encode(addr, EhlFrameCodec.CMD_VOLUME, volumeBytes)
                        log.info("⚙️  FIELD unsolicited VOLUME")
                        sendResponse(frame, listOf("unsolicitedVolume"))
                    } else {
                        Thread.sleep(200)
                    }
                }
            } catch (_: InterruptedException) {
                // ignore
            }
        }, "pls-sim-unsolicited")
        unsolicitedThread?.isDaemon = true
        unsolicitedThread?.start()
    }

    private fun encodeResult(result: EhlCommandResult): ResponseFrame {
        return when (result) {
            is EhlCommandResult.OkAck -> {
                val data = byteArrayOf(0x30)
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_OK, data)
                ResponseFrame(EhlFrameCodec.CMD_OK, data, bytes)
            }
            is EhlCommandResult.LinetestResponse -> {
                val data = byteArrayOf(0x55.toByte(), 0xAA.toByte())
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_LINETEST, data)
                ResponseFrame(EhlFrameCodec.CMD_LINETEST, data, bytes)
            }
            is EhlCommandResult.StateResponse -> {
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_STATE, result.data)
                ResponseFrame(EhlFrameCodec.CMD_STATE, result.data, bytes)
            }
            is EhlCommandResult.VolumeResponse -> {
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_VOLUME, result.data)
                ResponseFrame(EhlFrameCodec.CMD_VOLUME, result.data, bytes)
            }
            is EhlCommandResult.PriceResponse -> {
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_PRICE, result.data)
                ResponseFrame(EhlFrameCodec.CMD_PRICE, result.data, bytes)
            }
            is EhlCommandResult.ErrorQueryResponse -> {
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_ERROR_DATA, result.data)
                ResponseFrame(EhlFrameCodec.CMD_ERROR_DATA, result.data, bytes)
            }
            is EhlCommandResult.TankbitResponse -> {
                val bytes = EhlFrameCodec.encode(result.addr, EhlFrameCodec.CMD_TANKBIT, result.data)
                ResponseFrame(EhlFrameCodec.CMD_TANKBIT, result.data, bytes)
            }
        }
    }

    private fun concatResponses(frames: List<ResponseFrame>): ByteArray {
        val total = frames.sumOf { it.bytes.size }
        val out = ByteArray(total)
        var offset = 0
        frames.forEach { frame ->
            frame.bytes.copyInto(out, offset)
            offset += frame.bytes.size
        }
        return out
    }

    private data class ResponseFrame(
        val cmd: Byte,
        val data: ByteArray,
        val bytes: ByteArray
    )
}
