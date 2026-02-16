package no.cloudberries.norgesgass.baxi.client

import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import java.io.Closeable

/**
 * Minimal API surface of the baxi-kotlin library expected by `lpg-ehl-service`.
 *
 * This is a compile-time stub. The methods are intentionally lightweight and
 * are not guaranteed to talk to any real terminal.
 */
interface BaxiClient : Closeable {
    /**
     * Opens transport and starts internal loops.
     * Return 1 on accepted, 0 on immediate failure.
     * Terminal readiness is signaled via BaxiEventListener.onTerminalReady.
     */
    fun open(config: BaxiIniConfig): OpenResult

    /**
     * Mirrors vendor Close() semantics: return 1 on accepted.
     */
    fun closeTerminal(): CloseResult

    fun transferAmount(args: TransferAmountArgs): CallAcceptResult

    fun administration(args: AdministrationArgs): CallAcceptResult

    fun sendTld(args: SendTldArgs): CallAcceptResult

    fun sendJson(args: SendJsonArgs): CallAcceptResult

    fun confirm(args: ConfirmArgs): CallAcceptResult

    fun setEventListener(listener: BaxiEventListener?)
}

data class OpenResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class CloseResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class CallAcceptResult(
    val callResult: Int,
    val methodRejectCode: Int = 0,
    val methodRejectInfo: String? = null,
)

data class TransferAmountArgs(
    val operId: String,
    val type1: Int,
    val amount1: Int,
    val type2: Int,
    val amount2: Int,
    val type3: Int,
    val amount3: Int,
    val optionalData: String? = null,
)

data class AdministrationArgs(
    val admCode: Int,
    val operId: String = "0000",
    val optionalData: String? = null,
)

data class SendTldArgs(
    val tldType: String,
    val tldField: ByteArray,
)

data class SendJsonArgs(
    val jsonData: String,
)

data class ConfirmArgs(
    val id: Int,
    val allow: Boolean,
)

