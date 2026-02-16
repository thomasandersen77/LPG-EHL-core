package no.cloudberries.lpg.payment.terminal.sim.service

import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Receipt text generator aligned with real Nets/Ingenico terminal format.
 */
@Service
class ReceiptGenerator(
    private val config: SimulatorConfig
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.of("Europe/Oslo"))

    /**
     * Generate purchase receipt text (real terminal style: Bax terminalId-merchantId, Ref., Overf.).
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
        val bax = "${config.terminalId}-${config.merchantId}"

        return buildString {
            appendLine("NORGESGASS AS")
            appendLine("NEDRE EIKERVEI 26")
            appendLine("DRAMMEN")
            appendLine()
            appendLine("Bax: $bax")
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
            appendLine("Overf.: 020")
            appendLine()
            appendLine("KJØP")
            appendLine("NOK                %.2f".format(amountKroner))
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
     * Generate timeout receipt (no card presented) – real terminal style.
     */
    fun generateTimeoutReceipt(
        amountMinor: Int,
        timestamp: Instant,
        operationId: String
    ): String {
        val amountKroner = amountMinor / 100.0
        val formattedDate = dateFormatter.format(timestamp)
        val bax = "${config.terminalId}-${config.merchantId}"

        return buildString {
            appendLine("KOPI")
            appendLine("NORGESGASS AS")
            appendLine("NEDRE EIKERVEI 26")
            appendLine("DRAMMEN")
            appendLine()
            appendLine("Bax: $bax")
            appendLine(formattedDate)
            appendLine()
            appendLine("Kortet ikke presentert")
            appendLine("Ref.:  ___")
            appendLine("Overf.: 020")
            appendLine()
            appendLine("KJØP")
            appendLine("NOK                %.2f".format(amountKroner))
            appendLine("Tidsavbrudd")
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
     * Generate X-report text (current totals, no reset) – real terminal style.
     */
    fun generateXReport(timestamp: Instant): String {
        val formattedDate = dateFormatter.format(timestamp)
        val bax = "${config.terminalId}-${config.merchantId}"

        return buildString {
            appendLine("Bax: $bax")
            appendLine(formattedDate)
            appendLine("Valuta: NOK")
            appendLine("Sesjon.: 001")
            appendLine("X-rapport: 001")
            appendLine()
            appendLine("X-Total")
            appendLine()
            appendLine("Siste Z-Total")
            appendLine(formattedDate)
            appendLine()
            appendLine("BankAxept              0")
            appendLine("Beløp=              0,00")
            appendLine()
            appendLine("------------------------")
            appendLine("Antall                 0")
            appendLine("Total=              0,00")
        }.trimEnd()
    }

    /**
     * Generate Z-report text (end-of-day, resets counters) – real terminal style.
     */
    fun generateZReport(timestamp: Instant): String {
        val formattedDate = dateFormatter.format(timestamp)
        val bax = "${config.terminalId}-${config.merchantId}"
        val count = 0
        val amount = "0,00"
        val zNum = "001"

        return buildString {
            appendLine("Bax: $bax")
            appendLine(formattedDate)
            appendLine("Valuta: NOK")
            appendLine("Sesjon.: 001")
            appendLine("X-rapport: $zNum")
            appendLine()
            appendLine("X-Total")
            appendLine()
            appendLine("Siste Z-Total")
            appendLine(formattedDate)
            appendLine()
            appendLine("BankAxept              $count")
            appendLine("Beløp=              $amount")
            appendLine()
            appendLine("------------------------")
            appendLine("Antall                 $count")
            appendLine("Total=              $amount")
        }.trimEnd()
    }

    /**
     * Generate avstemming (reconciliation) report – real terminal style.
     */
    fun generateAvstemmingReport(timestamp: Instant): String {
        return buildString {
            appendLine("Avstemming")
            appendLine()
            appendLine("------------------------")
            appendLine("Innsamlet              0")
            appendLine("Total=              0,00")
            appendLine()
            appendLine("Kortavtaler uten")
            appendLine("omsetning skrives")
            appendLine("ikke ut.")
        }.trimEnd()
    }
}
