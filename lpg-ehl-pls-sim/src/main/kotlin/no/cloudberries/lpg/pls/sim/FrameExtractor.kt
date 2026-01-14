package no.cloudberries.lpg.pls.sim

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

/**
 * Extracts frames from incoming byte stream.
 * Supports LINE mode (newline terminated) and STX/ETX mode.
 */
class FrameExtractor(
    private val mode: FrameMode,
    private val logHex: Boolean = false
) {
    private val log = LoggerFactory.getLogger(FrameExtractor::class.java)
    private val buffer = ByteArrayOutputStream()
    private var inFrame = false  // For STX/ETX mode

    companion object {
        const val STX: Byte = 0x02
        const val ETX: Byte = 0x03
        const val LF: Byte = 0x0A   // '\n'
        const val CR: Byte = 0x0D   // '\r'
    }

    /**
     * Append bytes to buffer and extract any complete frames.
     * Returns list of extracted frames (may be empty).
     */
    fun append(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()

        if (logHex) {
            log.debug("RX raw: {}", bytes.toHexString())
        }

        val frames = mutableListOf<ByteArray>()
        
        for (b in bytes) {
            when (mode) {
                FrameMode.LINE -> {
                    if (b == LF) {
                        // Frame complete
                        val frame = buffer.toByteArray()
                        buffer.reset()
                        if (frame.isNotEmpty()) {
                            frames.add(frame.stripCr())
                        }
                    } else {
                        buffer.write(b.toInt())
                    }
                }
                FrameMode.STX_ETX -> {
                    when (b) {
                        STX -> {
                            // Start of frame - reset buffer
                            buffer.reset()
                            inFrame = true
                        }
                        ETX -> {
                            if (inFrame) {
                                // Frame complete
                                val frame = buffer.toByteArray()
                                buffer.reset()
                                inFrame = false
                                if (frame.isNotEmpty()) {
                                    frames.add(frame)
                                }
                            }
                            // Ignore ETX if not in frame
                        }
                        else -> {
                            if (inFrame) {
                                buffer.write(b.toInt())
                            }
                            // Ignore bytes outside frame
                        }
                    }
                }
            }
        }

        return frames
    }

    /**
     * Clear buffer (e.g., on timeout or error).
     */
    fun clear() {
        buffer.reset()
        inFrame = false
    }
}

// Extension functions
fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }

fun ByteArray.stripCr(): ByteArray {
    return if (isNotEmpty() && last() == FrameExtractor.CR) {
        copyOfRange(0, size - 1)
    } else {
        this
    }
}
