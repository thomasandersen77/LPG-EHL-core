package no.cloudberries.lpg.payment.terminal.sim.service

import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Terminal report state: session number (incremented on open), X/Z report numbers,
 * batch totals (reset on Z-report). Aligns with real Nets terminal behavior.
 */
@Service
class ReportState {

    private val sessionNumber = AtomicInteger(20)
    private val xReportNumber = AtomicInteger(4)
    private val zReportNumber = AtomicInteger(4)
    private val lastZTotalTimestamp = AtomicReference<String>("09/02/2026 15:04")
    private val batchCount = AtomicInteger(0)
    private val batchAmountMinor = AtomicInteger(0)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Oslo"))

    fun incrementSession() {
        sessionNumber.incrementAndGet()
    }

    fun getSessionNumber(): Int = sessionNumber.get()
    fun getSessionFormatted(): String = "%03d".format(sessionNumber.get())

    fun getXReportNumber(): Int = xReportNumber.get()
    fun getXReportFormatted(): String = "%03d".format(xReportNumber.get())

    fun getZReportNumber(): Int = zReportNumber.get()
    fun getZReportFormatted(): String = "%03d".format(zReportNumber.get())

    fun getLastZTotalTimestamp(): String = lastZTotalTimestamp.get()

    fun addToBatch(amountMinor: Int) {
        batchCount.incrementAndGet()
        batchAmountMinor.addAndGet(amountMinor)
    }

    fun getBatchCount(): Int = batchCount.get()
    fun getBatchAmountMinor(): Int = batchAmountMinor.get()
    fun getBatchAmountFormatted(): String = "%.2f".format(batchAmountMinor.get() / 100.0)

    /** Call after Z-report: reset batch and advance Z number / timestamp. */
    fun resetAfterZReport() {
        batchCount.set(0)
        batchAmountMinor.set(0)
        zReportNumber.incrementAndGet()
        xReportNumber.incrementAndGet()
        lastZTotalTimestamp.set(dateFormatter.format(Instant.now()))
    }

    /** X-report does not reset batch; only advances X number. */
    fun advanceXReportNumber() {
        xReportNumber.incrementAndGet()
    }
}
