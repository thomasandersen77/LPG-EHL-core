package no.cloudberries.lpg.pls.sim

/**
 * CLI arguments for PLS simulator.
 */
data class CliArgs(
    val port: String,
    val baud: Int = 9600,
    val mode: FrameMode = FrameMode.LINE,
    val chunk: Boolean = false,
    val latencyMs: Int = 0,
    val logHex: Boolean = false
) {
    companion object {
        fun parse(args: Array<String>): CliArgs {
            var port: String? = null
            var baud = 9600
            var mode = FrameMode.LINE
            var chunk = false
            var latencyMs = 0
            var logHex = false

            val iterator = args.iterator()
            while (iterator.hasNext()) {
                val arg = iterator.next()
                when {
                    arg.startsWith("--port=") -> port = arg.substringAfter("--port=")
                    arg.startsWith("--baud=") -> baud = arg.substringAfter("--baud=").toIntOrNull() ?: 9600
                    arg.startsWith("--mode=") -> {
                        val modeStr = arg.substringAfter("--mode=").lowercase()
                        mode = when (modeStr) {
                            "stxetx" -> FrameMode.STX_ETX
                            "line" -> FrameMode.LINE
                            else -> FrameMode.LINE
                        }
                    }
                    arg.startsWith("--chunk=") -> chunk = arg.substringAfter("--chunk=").lowercase() == "true"
                    arg.startsWith("--latencyMs=") -> latencyMs = arg.substringAfter("--latencyMs=").toIntOrNull() ?: 0
                    arg.startsWith("--logHex=") -> logHex = arg.substringAfter("--logHex=").lowercase() == "true"
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

            return CliArgs(port, baud, mode, chunk, latencyMs, logHex)
        }

        private fun printHelp() {
            println("""
                |PLS Simulator - Serial Port PLS Protocol Simulator
                |
                |Usage: java -jar pls-sim.jar --port=<port> [options]
                |
                |Required:
                |  --port=<port>       Serial port device (e.g., /dev/ttys013, /dev/ttyS0)
                |
                |Options:
                |  --baud=<baud>       Baud rate (default: 9600)
                |  --mode=<mode>       Frame mode: 'line' or 'stxetx' (default: line)
                |  --chunk=<bool>      Enable chunked responses (default: false)
                |  --latencyMs=<ms>    Add latency jitter to read loop (default: 0)
                |  --logHex=<bool>     Log raw bytes as hex (default: false)
                |  --help, -h          Show this help message
                |
                |Examples:
                |  java -jar pls-sim.jar --port=/dev/ttys013 --baud=9600 --mode=line
                |  java -jar pls-sim.jar --port=/dev/ttyS0 --mode=stxetx --chunk=true
            """.trimMargin())
        }
    }
}

enum class FrameMode {
    LINE,       // Frame terminated by '\n'
    STX_ETX     // Frame: STX(0x02) ... ETX(0x03)
}
