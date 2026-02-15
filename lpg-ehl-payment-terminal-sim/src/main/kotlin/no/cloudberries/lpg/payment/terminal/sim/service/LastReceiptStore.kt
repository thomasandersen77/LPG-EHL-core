package no.cloudberries.lpg.payment.terminal.sim.service

import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

/**
 * Stores the last printed receipt text for admin last-receipt (code 12604).
 * Aligns with real terminal: GET last receipt returns actual last transaction/receipt.
 */
@Service
class LastReceiptStore {

    private val lastReceipt = AtomicReference<String?>(null)

    fun set(receiptText: String) {
        lastReceipt.set(receiptText)
    }

    fun get(): String? = lastReceipt.get()

    fun clear() {
        lastReceipt.set(null)
    }
}
