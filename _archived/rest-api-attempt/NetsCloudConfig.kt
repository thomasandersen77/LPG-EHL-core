package no.cloudberries.lpg.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Nets Cloud Connect API
 * 
 * Nets Cloud Connect allows payment terminals to communicate with Nets cloud servers,
 * eliminating the need for direct TCP/ECR protocol handling.
 * 
 * Terminal Setup:
 * - ECR = Yes
 * - ECR IP = 3.33.230.243 (Nets Cloud)
 * - ECR Port = 6001
 * - Communication = Ethernet/WIFI
 */
@Configuration
@ConfigurationProperties(prefix = "nets.cloud-connect")
data class NetsCloudConfig(
    /**
     * Base URL for Nets Cloud Connect API
     * Example: https://api.nets.eu/terminal/v1
     */
    var baseUrl: String = "https://api.nets.eu/terminal/v1",
    
    /**
     * Authentication username provided by Nets
     */
    var username: String = "",
    
    /**
     * Authentication password provided by Nets
     */
    var password: String = "",
    
    /**
     * Terminal ID (TID) registered with Nets
     * Example: "42696609"
     */
    var terminalId: String = "",
    
    /**
     * Merchant ID provided by Nets
     */
    var merchantId: String = "",
    
    /**
     * HTTP request timeout in seconds
     */
    var timeoutSeconds: Int = 30,
    
    /**
     * Interval between status polling attempts (milliseconds)
     */
    var pollingIntervalMs: Long = 500,
    
    /**
     * Maximum number of polling attempts before timeout
     * Default: 120 attempts * 500ms = 60 seconds max wait
     */
    var maxPollAttempts: Int = 120,
    
    /**
     * Enable/disable Nets Cloud Connect integration
     */
    var enabled: Boolean = false
)
