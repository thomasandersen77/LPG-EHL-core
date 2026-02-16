package no.cloudberries.lpg.payment.terminal.sim.model.response

/**
 * Terminal status response (PascalCase).
 */
data class TerminalStatusResponse(
    val VendorDllLoadable: Boolean,
    val TerminalOpen: Boolean,
    val TerminalReady: Boolean,
    val ConnectionState: String? = null,
    val LastError: String? = null,
    val TerminalIdentity: Map<String, String>? = null
)
