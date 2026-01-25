package no.cloudberries.lpg.api.config

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.QueueClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["azure.enabled"], havingValue = "true")
class AzureConfig(
    @Value("\${azure.storage.connection-string}") private val connectionString: String,
    @Value("\${azure.storage.queue-name}") private val queueName: String
) {
    private val logger = LoggerFactory.getLogger(AzureConfig::class.java)

    @Bean
    fun queueClient(): QueueClient {
        logger.info("Initializing Azure Queue client for queue: $queueName")
        
        val client = QueueClientBuilder()
            .connectionString(connectionString)
            .queueName(queueName)
            .buildClient()

        // Create queue if it doesn't exist (works with both Azurite and Azure)
        try {
            client.createIfNotExists()
            logger.info("Azure Queue '$queueName' is ready")
        } catch (e: Exception) {
            logger.warn("Could not create queue (may already exist): ${e.message}")
        }

        return client
    }
}
