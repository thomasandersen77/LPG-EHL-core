package no.cloudberries.lpg.netscloud

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NetsResponseParser {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun isTerminalReady(message: String): Boolean =
        message.contains("Dfs13TerminalReady") || message.contains("ALREADY_OPEN")
    
    fun isTransactionComplete(message: String): Boolean =
        message.contains("Dfs13LocalMode")
    
    fun isDisplayText(message: String): Boolean =
        message.contains("Dfs13DisplayText")
    
    fun isPrintText(message: String): Boolean =
        message.contains("Dfs13PrintText")
    
    fun isError(message: String): Boolean =
        message.contains("Dfs13Error") || message.contains("\"Error\"")
    
    fun parseLocalMode(message: String): Dfs13LocalMode? {
        try {
            val resultMatch = Regex(""""Result"\s*:\s*"(\d+)"""").find(message)
            val amountMatch = Regex(""""TotalAmount"\s*:\s*"(\d+)"""").find(message)
            val responseCodeMatch = Regex(""""ResponseCode"\s*:\s*"(\w+)"""").find(message)
            
            return if (resultMatch != null) {
                Dfs13LocalMode(
                    result = resultMatch.groupValues[1].toInt(),
                    totalAmount = amountMatch?.groupValues?.get(1)?.toLongOrNull(),
                    responseCode = responseCodeMatch?.groupValues?.get(1)
                )
            } else null
        } catch (e: Exception) {
            logger.error("Failed to parse LocalMode", e)
            return null
        }
    }
    
    fun parseDisplayText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
            ?.replace("\\r", "")
            ?.replace("\\n", "")
    }
    
    fun parsePrintText(message: String): String? {
        val match = Regex(""""_"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
    }
    
    fun parseError(message: String): String? {
        val match = Regex(""""ErrorCode"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
    }
}

data class Dfs13LocalMode(
    val result: Int,         // 1 = Godkjent, 2 = Avvist
    val totalAmount: Long?,
    val responseCode: String?
)
