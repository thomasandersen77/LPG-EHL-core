package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort

/**
 * Configuration for serial port communication with LPG dispensers via RS-485.
 *
 * @property portName Serial port device name (e.g., "/dev/ttyUSB0", "COM3")
 * @property baudRate Communication speed in bits per second (default: 9600 for EHL protocol)
 * @property dataBits Number of data bits (default: 8)
 * @property stopBits Number of stop bits (default: 1)
 * @property parity Parity checking mode.
 *                IMPORTANT: Our Python golden-reference tooling uses 8N1 (NO_PARITY).
 *                Some "standard EHL" setups use 8E1 (EVEN_PARITY). Make this explicit
 *                in app config via `ehl.serial.parity` or enable auto-detect.
 * @property readTimeout Read timeout in milliseconds (default: 1000ms)
 * @property writeTimeout Write timeout in milliseconds (default: 1000ms)
 */
data class SerialPortConfig(
    val portName: String,
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = SerialPort.ONE_STOP_BIT,
    val parity: Int = SerialPort.NO_PARITY,  // 8N1 by default (matches python-test/)
    /**
     * Enable driver-controlled RS-485 direction (RTS toggle) if the platform/driver supports it.
     *
     * This corresponds to Linux `TIOCSRS485` behavior used by the Python tools when `--rs485` is enabled.
     * Many USB-RS485 dongles do automatic direction control and do NOT need this.
     */
    val rs485Enabled: Boolean = false,
    /**
     * RS-485: Set RTS high during send (common).
     */
    val rs485RtsHighDuringSend: Boolean = true,
    /**
     * RS-485: Set RTS high after send (rare; depends on adapter/transceiver).
     */
    val rs485RtsHighAfterSend: Boolean = false,
    /**
     * RS-485: Allow RX during TX (rare; default false).
     */
    val rs485RxDuringTx: Boolean = false,
    /**
     * RS-485: Delay before send (ms) after toggling RTS.
     */
    val rs485DelayRtsBeforeSendMs: Int = 0,
    /**
     * RS-485: Delay after send (ms) before toggling RTS back.
     */
    val rs485DelayRtsAfterSendMs: Int = 0,
    val readTimeout: Int = 1000,
    val writeTimeout: Int = 1000
) {
    init {
        require(portName.isNotBlank()) { "Port name cannot be blank" }
        require(baudRate > 0) { "Baud rate must be positive" }
        require(dataBits in 5..8) { "Data bits must be between 5 and 8" }
        require(readTimeout >= 0) { "Read timeout must be non-negative" }
        require(writeTimeout >= 0) { "Write timeout must be non-negative" }
    }

    companion object {
        /**
         * Common baud rates for serial communication
         */
        const val BAUD_9600 = 9600
        const val BAUD_19200 = 19200
        const val BAUD_38400 = 38400
        const val BAUD_57600 = 57600
        const val BAUD_115200 = 115200

        /**
         * Create a configuration with default settings for EHL protocol.
         *
         * NOTE:
         * - For Norges Gass field tooling (Python reference), this is typically 9600 8N1.
         * - For some "standard EHL" hardware, this may need to be 9600 8E1.
         *
         * @param portName Serial port device name
         * @return Configuration with EHL defaults
         */
        fun forEhlProtocol(portName: String): SerialPortConfig {
            return SerialPortConfig(
                portName = portName,
                baudRate = BAUD_9600,
                dataBits = 8,
                stopBits = SerialPort.ONE_STOP_BIT,
                parity = SerialPort.NO_PARITY  // 8N1 (python-test compatible)
            )
        }

        /**
         * Auto-detect the first available serial port.
         * Returns null if no ports are found.
         */
        fun autoDetect(): SerialPortConfig? {
            val ports = SerialPort.getCommPorts()
            return ports.firstOrNull()?.let { forEhlProtocol(it.systemPortName) }
        }
    }

    override fun toString(): String {
        return "SerialPortConfig(port=$portName, baud=$baudRate, bits=$dataBits, " +
                "stop=$stopBits, parity=$parity, rs485Enabled=$rs485Enabled, " +
                "readTimeout=${readTimeout}ms, writeTimeout=${writeTimeout}ms)"
    }
}
