package no.cloudberries.lpg.communication

import com.fazecast.jSerialComm.SerialPort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class SerialPortConfigTest {

    @Test
    fun `should create config with default values`() {
        val config = SerialPortConfig(portName = "/dev/ttyUSB0")
        
        assertEquals("/dev/ttyUSB0", config.portName)
        assertEquals(9600, config.baudRate)
        assertEquals(8, config.dataBits)
        assertEquals(SerialPort.ONE_STOP_BIT, config.stopBits)
        assertEquals(SerialPort.NO_PARITY, config.parity)
        assertEquals(1000, config.readTimeout)
        assertEquals(1000, config.writeTimeout)
    }

    @Test
    fun `should create config with custom values`() {
        val config = SerialPortConfig(
            portName = "COM3",
            baudRate = 19200,
            dataBits = 7,
            readTimeout = 2000,
            writeTimeout = 500
        )
        
        assertEquals("COM3", config.portName)
        assertEquals(19200, config.baudRate)
        assertEquals(7, config.dataBits)
        assertEquals(2000, config.readTimeout)
        assertEquals(500, config.writeTimeout)
    }

    @Test
    fun `should create EHL protocol config`() {
        val config = SerialPortConfig.forEhlProtocol("/dev/ttyUSB0")
        
        assertEquals("/dev/ttyUSB0", config.portName)
        assertEquals(SerialPortConfig.BAUD_9600, config.baudRate)
        assertEquals(8, config.dataBits)
        assertEquals(SerialPort.ONE_STOP_BIT, config.stopBits)
        assertEquals(SerialPort.NO_PARITY, config.parity)
    }

    @Test
    fun `should reject blank port name`() {
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "")
        }
    }

    @Test
    fun `should reject invalid baud rate`() {
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "/dev/ttyUSB0", baudRate = -1)
        }
    }

    @Test
    fun `should reject invalid data bits`() {
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "/dev/ttyUSB0", dataBits = 9)
        }
        
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "/dev/ttyUSB0", dataBits = 4)
        }
    }

    @Test
    fun `should reject negative timeout`() {
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "/dev/ttyUSB0", readTimeout = -1)
        }
        
        assertThrows<IllegalArgumentException> {
            SerialPortConfig(portName = "/dev/ttyUSB0", writeTimeout = -1)
        }
    }

    @Test
    fun `should have descriptive toString`() {
        val config = SerialPortConfig.forEhlProtocol("/dev/ttyUSB0")
        val str = config.toString()
        
        assert(str.contains("/dev/ttyUSB0"))
        assert(str.contains("9600"))
        assert(str.contains("8"))
    }

    @Test
    fun `should provide common baud rate constants`() {
        assertEquals(9600, SerialPortConfig.BAUD_9600)
        assertEquals(19200, SerialPortConfig.BAUD_19200)
        assertEquals(38400, SerialPortConfig.BAUD_38400)
        assertEquals(57600, SerialPortConfig.BAUD_57600)
        assertEquals(115200, SerialPortConfig.BAUD_115200)
    }
}
