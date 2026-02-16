package no.cloudberries.lpg.scripts

/**
 * Single entrypoint for the “kit” so the destination device only needs:
 *   java -cp "app.jar:lib/<all-jars>" no.cloudberries.lpg.scripts.SerialPortTestsMainKt <command>
 *
 * All commands read configuration from env vars (see README).
 */
fun main(args: Array<String>) {
    val cmd = args.firstOrNull()?.trim().orEmpty()

    when (cmd) {
        "list-ports" -> cmdListPorts()
        "scan-addresses" -> cmdScanAddresses()
        "baseline-snapshot" -> cmdBaselineSnapshot()
        "monitor-state-volume" -> cmdMonitorStateVolume()
        "unblock-hold-block" -> cmdUnblockHoldBlock()
        "program-price" -> cmdProgramPrice()
        "program-preset-amount" -> cmdProgramPresetAmount()
        "program-preset-volume" -> cmdProgramPresetVolume()

        "", "help", "--help", "-h" -> printHelp()
        else -> {
            System.err.println("Unknown command: '$cmd'")
            printHelp()
            kotlin.system.exitProcess(2)
        }
    }
}

private fun printHelp() {
    println(
        """
        Serial Port Tests (RS-485 / EHL)

        Usage:
          run.sh <command>

        Commands:
          list-ports
          scan-addresses
          baseline-snapshot
          monitor-state-volume
          unblock-hold-block            (requires I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)
          program-price                 (requires I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)
          program-preset-amount         (requires I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)
          program-preset-volume         (requires I_UNDERSTAND_THIS_CAN_AFFECT_REAL_HARDWARE=true)
        """.trimIndent()
    )
}

