package no.cloudberries.lpg.netscloud

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "nets-cloud-connect")
open class NetsCloudConnectConfig(
    var baseUrl: String = "https://connectcloud.aws.nets.eu",
    var username: String = "",
    var password: String = "",
    var terminalId: String = "",
    var websocket: WebSocketConfig = WebSocketConfig(),
    var timeouts: TimeoutConfig = TimeoutConfig()
)

data class WebSocketConfig(
    var pingIntervalMs: Long = 20000,
    var reconnectDelayMs: Long = 5000,
    var maxReconnectAttempts: Int = 10
)

data class TimeoutConfig(
    var loginTimeoutMs: Long = 10000,
    var openTerminalTimeoutMs: Long = 30000,
    var purchaseTimeoutMs: Long = 120000,
    var reversalTimeoutMs: Long = 60000
)
