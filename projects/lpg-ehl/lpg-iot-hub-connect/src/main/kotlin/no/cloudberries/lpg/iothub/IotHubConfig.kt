package no.cloudberries.lpg.iothub

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(IotHubConfig::class)
@ConfigurationProperties(prefix = "iot-hub")
data class IotHubConfig(
    val connectionString: String = "",
    val enabled: Boolean = true
)
