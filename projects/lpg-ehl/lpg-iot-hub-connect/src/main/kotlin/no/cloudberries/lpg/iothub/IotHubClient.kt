package no.cloudberries.lpg.iothub

interface IotHubClient {
    fun connect()
    fun disconnect()
    fun sendTelemetry(message: String)
    fun setReceiveMessageCallback(callback: (String) -> Unit)
    fun setDirectMethodCallback(callback: (methodName: String, payload: String) -> String)
}
