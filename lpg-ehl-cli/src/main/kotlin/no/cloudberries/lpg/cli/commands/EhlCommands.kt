package no.cloudberries.lpg.cli.commands

import kotlinx.coroutines.runBlocking
import no.cloudberries.lpg.service.EhlOperationsService
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

/**
 * EHL Protocol Commands for Spring Shell CLI.
 * 
 * Provides interactive terminal commands for testing and operating LPG dispensers.
 * All commands delegate to EhlOperationsService, ensuring identical behavior with API.
 * 
 * USAGE EXAMPLES:
 * ```
 * shell:> linetest 1
 * shell:> state 1
 * shell:> volume 1
 * shell:> unblock 1
 * shell:> block 1
 * shell:> run-vb6-sequence 1
 * ```
 */
@ShellComponent
class EhlCommands(
    private val operations: EhlOperationsService
) {
    
    @ShellMethod(
        value = "Test connectivity to dispenser",
        key = ["linetest", "lt"]
    )
    fun linetest(
        @ShellOption(defaultValue = "1", help = "Dispenser address (1-255)") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.linetest(address)
            """
            ✅ LINETEST SUCCESSFUL
            ═══════════════════════════════════════
            Address:  $address
            Response: ${result.command}
            Status:   Connected and responding
            ═══════════════════════════════════════
            """.trimIndent()
        } catch (e: Exception) {
            """
            ❌ LINETEST FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Query dispenser state",
        key = ["state", "st"]
    )
    fun state(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.getState(address)
            val stateCode = if (result.data.isNotEmpty()) result.data[0].toInt() and 0xFF else -1
            val stateDescription = getStateDescription(stateCode)
            
            """
            📊 DISPENSER STATE
            ═══════════════════════════════════════
            Address:     $address
            State Code:  $stateCode
            Description: $stateDescription
            ═══════════════════════════════════════
            """.trimIndent()
        } catch (e: Exception) {
            """
            ❌ STATE QUERY FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Query current volume",
        key = ["volume", "vol"]
    )
    fun volume(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.getVolume(address)
            """
            ⛽ VOLUME READING
            ═══════════════════════════════════════
            Address:     $address
            Volume:      %.2f L
            Pump:        ${result.pumpNumber}
            ═══════════════════════════════════════
            """.trimIndent().format(result.volumeLitres)
        } catch (e: Exception) {
            """
            ❌ VOLUME QUERY FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Query price setting",
        key = ["price", "pr"]
    )
    fun price(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.getPrice(address)
            """
            💰 PRICE SETTING
            ═══════════════════════════════════════
            Address:       $address
            Price:         %.2f kr/L
            Price (cents): ${result.pricePerLitreCents} øre/L
            ═══════════════════════════════════════
            """.trimIndent().format(result.pricePerLitreKr)
        } catch (e: Exception) {
            """
            ❌ PRICE QUERY FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Unblock dispenser (allow fuel delivery)",
        key = ["unblock", "ub"]
    )
    fun unblock(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.unblock(address)
            
            if (result.isSuccess) {
                """
                🔓 DISPENSER UNBLOCKED
                ═══════════════════════════════════════
                Address: $address
                Status:  Ready for fuel delivery
                Action:  Lift nozzle to begin pumping
                ═══════════════════════════════════════
                """.trimIndent()
            } else {
                """
                ❌ UNBLOCK FAILED
                ═══════════════════════════════════════
                Address: $address
                Error:   ${result.exceptionOrNull()?.message}
                ═══════════════════════════════════════
                """.trimIndent()
            }
        } catch (e: Exception) {
            """
            ❌ UNBLOCK COMMAND FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Block dispenser (stop fuel delivery)",
        key = ["block", "bl"]
    )
    fun block(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.block(address)
            
            if (result.isSuccess) {
                """
                🛑 DISPENSER BLOCKED
                ═══════════════════════════════════════
                Address: $address
                Status:  Fuel delivery stopped
                Action:  Use 'volume' to check final amount
                ═══════════════════════════════════════
                """.trimIndent()
            } else {
                """
                ❌ BLOCK FAILED
                ═══════════════════════════════════════
                Address: $address
                Error:   ${result.exceptionOrNull()?.message}
                ═══════════════════════════════════════
                """.trimIndent()
            }
        } catch (e: Exception) {
            """
            ❌ BLOCK COMMAND FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Query error status",
        key = ["error", "err"]
    )
    fun error(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.getError(address)
            
            if (!result.hasError) {
                """
                ✅ NO ERRORS
                ═══════════════════════════════════════
                Address: $address
                Status:  Dispenser operating normally
                ═══════════════════════════════════════
                """.trimIndent()
            } else {
                """
                ⚠️  ERROR DETECTED
                ═══════════════════════════════════════
                Address:     $address
                Error Code:  ${result.errorCode}
                Description: ${result.errorDescription}
                ═══════════════════════════════════════
                """.trimIndent()
            }
        } catch (e: Exception) {
            """
            ❌ ERROR QUERY FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Query tank level",
        key = ["tank", "tk"]
    )
    fun tank(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            val result = operations.getTank(address)
            val level = result.tankLevelPercent
            val bar = "█".repeat(level / 5) + "░".repeat(20 - level / 5)
            
            """
            🛢️  TANK LEVEL
            ═══════════════════════════════════════
            Address: $address
            Level:   $level%
            [$bar] $level%
            ═══════════════════════════════════════
            """.trimIndent()
        } catch (e: Exception) {
            """
            ❌ TANK QUERY FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    @ShellMethod(
        value = "Run VB6 compatibility test sequence (LINETEST→STATE→VOLUME→PRICE)",
        key = ["run-vb6-sequence", "vb6", "test"]
    )
    fun runVb6Sequence(
        @ShellOption(defaultValue = "1", help = "Dispenser address") address: Int
    ): String = runBlocking {
        return@runBlocking try {
            println("🧪 Running VB6 compatibility test sequence...")
            println("═══════════════════════════════════════════════════════")
            
            val result = operations.runVb6Sequence(address)
            
            val output = StringBuilder()
            output.appendLine("\n📋 TEST RESULTS")
            output.appendLine("═══════════════════════════════════════════════════════")
            output.appendLine("Address: $address")
            output.appendLine("Overall: ${if (result.allPassed) "✅ PASSED" else "❌ FAILED"}")
            
            if (result.failedAt != null) {
                output.appendLine("Failed at: ${result.failedAt}")
            }
            
            output.appendLine("\nSteps completed: ${result.testsRun}/${result.totalTests}")
            output.appendLine("═══════════════════════════════════════════════════════")
            output.appendLine("\nDetailed Results:")
            output.appendLine("───────────────────────────────────────────────────────")
            
            result.steps.forEach { step ->
                val status = if (step.passed) "✅" else "❌"
                val duration = "%4d ms".format(step.duration)
                output.appendLine("$status ${step.command.padEnd(12)} $duration")
                if (step.errorMessage != null) {
                    output.appendLine("   Error: ${step.errorMessage}")
                }
            }
            
            output.appendLine("═══════════════════════════════════════════════════════")
            
            if (result.allPassed) {
                output.appendLine("✅ VB6 COMPATIBILITY: VERIFIED")
                output.appendLine("   All protocol commands working correctly")
            } else {
                output.appendLine("❌ VB6 COMPATIBILITY: ISSUES DETECTED")
                output.appendLine("   Please check connection and dispenser status")
            }
            output.appendLine("═══════════════════════════════════════════════════════")
            
            output.toString()
        } catch (e: Exception) {
            """
            ❌ VB6 SEQUENCE FAILED
            ═══════════════════════════════════════
            Address: $address
            Error:   ${e.message}
            ═══════════════════════════════════════
            """.trimIndent()
        }
    }
    
    /**
     * Helper function to map state codes to descriptions.
     */
    private fun getStateDescription(code: Int): String {
        return when (code) {
            0 -> "IDLE (Ready, no nozzle activity)"
            1 -> "CALLING (Nozzle lifted, waiting for authorization)"
            2 -> "AUTHORIZED (Ready to pump)"
            3 -> "BUSY (Pumping in progress)"
            4 -> "STOPPED (Nozzle replaced, delivery complete)"
            5 -> "PAYMENT_PENDING (Awaiting payment settlement)"
            else -> "UNKNOWN ($code)"
        }
    }
}
