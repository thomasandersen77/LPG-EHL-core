package no.cloudberries.lpg.scripts

import com.fazecast.jSerialComm.SerialPort
import no.cloudberries.lpg.communication.EhlCommunicator
import no.cloudberries.lpg.communication.RetryConfig
import no.cloudberries.lpg.communication.SerialPortConfig
import no.cloudberries.lpg.communication.SerialPortManager

object ScriptEnv {
    fun env(name: String, defaultValue: String? = null): String =
        System.getenv(name) ?: defaultValue ?: error("Missing env var: $name")

    fun envInt(name: String, defaultValue: Int? = null): Int = env(name, defaultValue?.toString()).toInt()
    fun envLong(name: String, defaultValue: Long? = null): Long = env(name, defaultValue?.toString()).toLong()

    fun envBool(name: String, defaultValue: Boolean = false): Boolean =
        (System.getenv(name) ?: defaultValue.toString())
            .trim()
            .lowercase() in setOf("1", "true", "yes", "y", "on")

    fun requireDangerAck() {
        val ok = (System.getenv("I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE") ?: "")
            .trim()
            .lowercase() == "true"
        require(ok) {
            "Refusing to run. Set env I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true to proceed."
        }
    }

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

    fun isVb6OkByte(b: Byte): Boolean {
        val x = b.toInt() and 0xFF
        // Field notes are inconsistent (0x1E vs ASCII '0' = 0x30). Accept both.
        return x == 0x1E || x == 0x30
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
}

