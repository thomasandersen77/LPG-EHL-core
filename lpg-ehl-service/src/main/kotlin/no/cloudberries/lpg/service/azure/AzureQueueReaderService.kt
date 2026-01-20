package no.cloudberries.lpg.service.azure

import com.azure.storage.queue.QueueClient
import com.azure.storage.queue.models.PeekedMessageItem
import com.fasterxml.jackson.databind.ObjectMapper
import no.cloudberries.lpg.service.dto.AzureQueueMessageDto
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@ConditionalOnProperty(name = ["azure.enabled"], havingValue = "true")
class AzureQueueReaderService(
    private val queueClient: QueueClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AzureQueueReaderService::class.java)

    /**
     * Peek at messages in the Azure Queue without removing them
     */
    fun peekMessages(maxMessages: Int = 32): List<AzureQueueMessageDto> {
        logger.debug("Peeking at up to $maxMessages messages from Azure Queue")
        
        return try {
            val messages = queueClient.peekMessages(maxMessages, null, null)
            
            messages.take(maxMessages).mapNotNull { message ->
                try {
                    parseMessage(message)
                } catch (e: Exception) {
                    logger.warn("Failed to parse message: ${e.message}")
                    null
                }
            }.also {
                logger.info("Successfully peeked ${it.size} messages from Azure Queue")
            }
        } catch (e: Exception) {
            logger.error("Failed to peek messages from Azure Queue: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get messages grouped by date
     */
    fun getMessagesByDate(): Map<LocalDate, List<AzureQueueMessageDto>> {
        val messages = peekMessages(32) // Azure Queue max is 32 messages per peek
        
        return messages.groupBy { message ->
            message.insertionTime.toLocalDate()
        }.toSortedMap(reverseOrder()) // Most recent first
    }

    /**
     * Get message count by date
     */
    fun getMessageCountByDate(): Map<LocalDate, Int> {
        return getMessagesByDate().mapValues { it.value.size }
    }

    /**
     * Parse a peeked message into our DTO
     */
    private fun parseMessage(message: PeekedMessageItem): AzureQueueMessageDto {
        // Decode the base64-encoded message body
        val messageBody = message.body.toString()
        val payload = objectMapper.readValue(messageBody, Map::class.java) as Map<String, Any>
        
        // Extract transaction data from payload
        val payloadData = payload["payload"] as? Map<String, Any>
        val transactionData = payloadData?.let {
            AzureQueueMessageDto.TransactionData(
                dispenserAddress = (it["dispenserAddress"] as? Number)?.toInt(),
                volumeLiters = (it["volumeLiters"] as? Number)?.toDouble(),
                amountKr = (it["amountKr"] as? Number)?.toDouble(),
                pricePerLiter = (it["pricePerLiter"] as? Number)?.toDouble(),
                paymentType = it["paymentType"] as? String,
                paymentStatus = it["paymentStatus"] as? String,
                timestamp = it["timestamp"] as? String
            )
        }
        
        return AzureQueueMessageDto(
            messageId = message.messageId,
            insertionTime = message.insertionTime.toLocalDateTime(),
            expirationTime = message.expirationTime.toLocalDateTime(),
            dequeueCount = message.dequeueCount,
            entityType = payload["entityType"] as? String,
            entityId = payload["entityId"] as? String,
            status = payload["status"] as? String,
            retryCount = (payload["retryCount"] as? Number)?.toInt() ?: 0,
            transaction = transactionData
        )
    }
}
