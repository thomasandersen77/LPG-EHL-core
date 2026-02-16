package no.cloudberries.norgesgass.baxi.events

/**
 * Server-facing event interface similar to vendor IBaxiEvents.
 *
 * This is a compile-time stub to allow `lpg-ehl-service` to build.
 * Replace with the real implementation when the actual baxi-kotlin library is available.
 */
interface BaxiEventListener {
    fun onTerminalReady() {}

    fun onDisplayText(
        displayText: String,
        displayTextSourceId: Int? = null,
        displayTextId: Int? = null,
    ) {}

    fun onPrintText(printText: String) {}

    fun onError(errorCode: Int, errorString: String?) {}

    fun onLocalMode(event: LocalModeEvent) {}

    fun onLastFinancialResult(event: LastFinancialResultEvent) {}

    fun onStdRsp(stdRsp: ByteArray) {}

    fun onTldReceived(tldType: Int, tldData: ByteArray) {}

    fun onJsonReceived(json: String) {}
}

data class LocalModeEvent(
    val result: Int?,
    val responseCode: String?,
    val rejectionSource: String?,
    val rejectionReason: String?,
    val localModeResultData: String?,
    val fields: Map<String, String> = emptyMap(),
)

data class LastFinancialResultEvent(
    val result: Int?,
    val resultData: String?,
)

