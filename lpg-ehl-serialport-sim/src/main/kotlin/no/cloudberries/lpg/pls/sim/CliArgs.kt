package no.cloudberries.lpg.pls.sim

/**
 * CLI arguments for PLS simulator.
 */
data class CliArgs(
    val port: String,
    val baud: Int = 9600,
    val parity: String = "NONE",  // Parity mode: NONE, EVEN, ODD (default: NONE for 8N1)
    val mode: FrameMode = FrameMode.LINE,
    val chunk: Boolean = false,
    val latencyMs: Int = 0,
    val logHex: Boolean = false,
    val dispenserAddress: Int = 1,
    val priceCents: Int = 1590,
    val initiallyBlocked: Boolean = true,
    val heartbeatIntervalMs: Long = 60000,  // Configurable heartbeat interval (default: 60 seconds)
    val legacyAddressEnabled: Boolean = true  // Also respond to address 32+n (Alejandro's finding)
) {
    companion object {
        fun parse(args: Array<String>): CliArgs {
            var port: String? = null
            var baud = 9600
            var parity = "NONE"
            var mode = FrameMode.LINE
            var chunk = false
            var latencyMs = 0
            var logHex = false
            var dispenserAddress = 1
            var priceCents = 1590
            var initiallyBlocked = true
            var heartbeatIntervalMs = 60000L
            var legacyAddressEnabled = true

            val iterator = args.iterator()
            while (iterator.hasNext()) {
                val arg = iterator.next()
                when {
                    arg.startsWith("--port=") -> port = arg.substringAfter("--port=")
                    arg.startsWith("--baud=") -> baud = arg.substringAfter("--baud=").toIntOrNull() ?: 9600
                    arg.startsWith("--parity=") -> parity = arg.substringAfter("--parity=").uppercase()
                    arg.startsWith("--mode=") -> {
                        val modeStr = arg.substringAfter("--mode=").lowercase()
                        mode = when (modeStr) {
                            "stxetx" -> FrameMode.STX_ETX
                            "ehl" -> FrameMode.EHL
                            "line" -> FrameMode.LINE
                            else -> FrameMode.LINE
                        }
                    }
                    arg.startsWith("--chunk=") -> chunk = arg.substringAfter("--chunk=").lowercase() == "true"
                    arg.startsWith("--latencyMs=") -> latencyMs = arg.substringAfter("--latencyMs=").toIntOrNull() ?: 0
                    arg.startsWith("--logHex=") -> logHex = arg.substringAfter("--logHex=").lowercase() == "true"
                    arg.startsWith("--address=") -> dispenserAddress = arg.substringAfter("--address=").toIntOrNull() ?: 1
                    arg.startsWith("--price=") -> priceCents = arg.substringAfter("--price=").toIntOrNull() ?: 1590
                    arg.startsWith("--blocked=") -> initiallyBlocked = arg.substringAfter("--blocked=").lowercase() != "false"
                    arg.startsWith("--heartbeatIntervalMs=") -> heartbeatIntervalMs = arg.substringAfter("--heartbeatIntervalMs=").toLongOrNull() ?: 60000L
                    arg.startsWith("--legacy-address=") -> legacyAddressEnabled = arg.substringAfter("--legacy-address=").lowercase() != "false"
                    arg == "--help" || arg == "-h" -> {
                        printHelp()
                        kotlin.system.exitProcess(0)
                    }
                }
            }

            if (port == null) {
                System.err.println("ERROR: --port is required")
                printHelp()
                kotlin.system.exitProcess(1)
            }

            return CliArgs(port, baud, parity, mode, chunk, latencyMs, logHex, dispenserAddress, priceCents, initiallyBlocked, heartbeatIntervalMs, legacyAddressEnabled)
        }

        private fun printHelp() {
            println("""
                |PLS Simulator - Serial Port PLS Protocol Simulator
                |
                |Supports all commands tested by Alejandro:
                |  STATE (0x4B), ERROR_QUERY (0x4C), VOLUME (0x45), TANKBIT (0xC5)
                |
                |Usage: java -jar pls-sim.jar --port=<port> [options]
                |
                |Required:
                |  --port=<port>          Serial port device (e.g., /tmp/ttyV0, /dev/ttyS0)
                |
                |Serial Options:
                |  --baud=<baud>          Baud rate (default: 9600)
                |  --parity=<parity>      Parity mode: NONE, EVEN, ODD (default: NONE for 8N1)
                |  --mode=<mode>          Frame mode: 'line', 'stxetx', or 'ehl' (default: line)
                |  --chunk=<bool>         Enable chunked responses (default: false)
                |  --latencyMs=<ms>       Add latency jitter to read loop (default: 0)
                |  --logHex=<bool>        Log raw bytes as hex (default: false)
                |
                |Dispenser Options:
                |  --address=<addr>       Dispenser address 1-8 (default: 1)
                |  --price=<cents>        Price per liter in cents, e.g. 1590 = 15.90 kr/L (default: 1590)
                |  --blocked=<bool>       Initial blocked state (default: true)
                |  --legacy-address=<bool> Also respond to 32+address (default: true)
                |                         Based on Alejandro's finding: pumps respond at 32+pump_number
                |
                |Logging Options:
                |  --heartbeatIntervalMs=<ms>  Heartbeat log interval in ms (default: 60000)
                |  --help, -h             Show this help message
                |
                |Examples:
                |  # Basic EHL mode for socat testing:
                |  java -jar pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --logHex=true
                |
                |  # Full config with legacy address (responds to addr 1 AND 33):
                |  java -jar pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --address=1 --price=2100
                |
                |  # Disable legacy address (only respond to addr 1):
                |  java -jar pls-sim.jar --port=/tmp/ttyV0 --mode=ehl --address=1 --legacy-address=false
            """.trimMargin())
        }
    }
}

enum class FrameMode {
    LINE,       // Frame terminated by '\n'
    STX_ETX,    // Frame: STX(0x02) ... ETX(0x03)
    EHL         // Frame: STX(0x10/0x20) LEN ADDR CMD [DATA...] CHK ETX(0x36)
}
