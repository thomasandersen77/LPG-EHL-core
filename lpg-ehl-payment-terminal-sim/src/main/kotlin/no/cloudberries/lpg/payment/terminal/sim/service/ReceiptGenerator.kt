package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Receipt text generator for mock terminal receipts.
 */
@Service
class ReceiptGenerator(
    private val config: SimulatorConfig
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * Generate purchase receipt text.
     */
    fun generatePurchaseReceipt(
        amountMinor: Int,
        timestamp: Instant,
        operationId: String,
        approved: Boolean = true,
        responseCode: String? = null
    ): String {
        val amountKroner = amountMinor / 100.0
        val formattedDate = dateFormatter.format(timestamp)

        return buildString {
            appendLine("NORGESGASS AS")
            appendLine("NEDRE EIKERVEI 26")
            appendLine("DRAMMEN")
            appendLine()
            appendLine("Bax: ${config.terminalId}")
            appendLine(formattedDate)
            appendLine()
            appendLine("BankAxept")
            appendLine("************8408-3")
            appendLine("AID: D5780000021010")
            appendLine("TVR: 0000248000")
            appendLine("TSI: E800")
            appendLine("IAC: 0000000000")
            appendLine("Ref.: $operationId")
            if (responseCode != null) {
                appendLine("Resp.: $responseCode")
            }
            appendLine("Overf.: 019")
            appendLine()
            appendLine("KJØP                     %.2f".format(amountKroner))
            if (approved) {
                appendLine("GODKJENT")
            } else {
                appendLine("AVVIST")
            }
            appendLine()
            appendLine()
            appendLine()
        }.trimEnd()
    }

    /**
     * Generate refund receipt text.
     */
    fun generateRefundReceipt(
        amountMinor: Int,
        timestamp: Instant,
        operationId: String
    ): String {
        val amountKroner = amountMinor / 100.0
        val formattedDate = dateFormatter.format(timestamp)

        return buildString {
            appendLine("NETS AS")
            appendLine("Terminal ID: ${config.terminalId}")
            appendLine("Merchant: ${config.merchantId}")
            appendLine()
            appendLine("REFUSJON GODKJENT")
            appendLine()
            appendLine("Refusjon")
            appendLine("Beløp: NOK %.2f".format(amountKroner))
            appendLine()
            appendLine("Dato: $formattedDate")
            appendLine("Ref: $operationId")
            appendLine()
        }.trimEnd()
    }

    /**
     * Generate Z-report text (mock).
     */
    fun generateZReport(timestamp: Instant): String {
        val formattedDate = dateFormatter.format(timestamp)

        return buildString {
            appendLine("NETS AS")
            appendLine("Terminal ID: ${config.terminalId}")
            appendLine("Merchant: ${config.merchantId}")
            appendLine()
            appendLine("Z-RAPPORT (DAGSAVSLUTNING)")
            appendLine()
            appendLine("Dato: $formattedDate")
            appendLine()
            appendLine("Antall transaksjoner: 42")
            appendLine("Totalbeløp: NOK 12500.00")
            appendLine()
            appendLine("Kjøp: 40 (NOK 12000.00)")
            appendLine("Refusjon: 2 (NOK 500.00)")
            appendLine()
        }.trimEnd()
    }

    /**
     * Generate avstemming (reconciliation) report text.
     */
    fun generateAvstemmingReport(timestamp: Instant): String {
        val formattedDate = dateFormatter.format(timestamp)

        return buildString {
            appendLine("NETS AS")
            appendLine("Terminal ID: ${config.terminalId}")
            appendLine("Merchant: ${config.merchantId}")
            appendLine()
            appendLine("AVSTEMMING")
            appendLine()
            appendLine("Dato: $formattedDate")
            appendLine()
            appendLine("Status: GODKJENT")
            appendLine()
        }.trimEnd()
    }
}
