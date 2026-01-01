package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort

/**
 * Configuration for serial port communication with LPG dispensers via RS-485.
 *
 * @property portName Serial port device name (e.g., "/dev/ttyUSB0", "COM3")
 * @property baudRate Communication speed in bits per second (default: 9600 for EHL protocol)
 * @property dataBits Number of data bits (default: 8)
 * @property stopBits Number of stop bits (default: 1)
 * @property parity Parity checking mode (default: NONE)
 * @property readTimeout Read timeout in milliseconds (default: 1000ms)
 * @property writeTimeout Write timeout in milliseconds (default: 1000ms)
 */
data class SerialPortConfig(
    val portName: String,
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = SerialPort.ONE_STOP_BIT,
    val parity: Int = SerialPort.NO_PARITY,
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
         * RS-485 typically uses 9600 baud, 8 data bits, 1 stop bit, no parity.
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
                parity = SerialPort.NO_PARITY
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
                "stop=$stopBits, parity=$parity, readTimeout=${readTimeout}ms, writeTimeout=${writeTimeout}ms)"
    }
}
