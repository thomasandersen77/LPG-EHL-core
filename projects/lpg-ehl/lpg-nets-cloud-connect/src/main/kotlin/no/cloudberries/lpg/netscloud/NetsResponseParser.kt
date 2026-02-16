package no.cloudberries.lpg.netscloud

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NetsResponseParser {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun isTerminalReady(message: String): Boolean {
        // MANGLER 2: MethodRejected(7102) means terminal is already open/ready
        if (message.contains("MethodRejected")) {
            val codeMatch = Regex(""""Code"\s*:\s*"(\d+)"""").find(message)
            if (codeMatch?.groupValues?.get(1) == "7102") {
                return true
            }
        }
        return message.contains("Dfs13TerminalReady") || message.contains("ALREADY_OPEN")
    }

    fun isTransactionComplete(message: String): Boolean {
        // MANGLER 4: Dfs13LastFinancialResult is also a transaction completion signal
        return message.contains("Dfs13LocalMode") || message.contains("Dfs13LastFinancialResult")
    }

    fun isJsonReceived(message: String): Boolean =
        message.contains("Dfs13JsonReceived")
    
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

    fun parseLastFinancialResult(message: String): Dfs13LocalMode? {
        // Parse Dfs13LastFinancialResult same as LocalMode
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
            logger.error("Failed to parse LastFinancialResult", e)
            return null
        }
    }

    fun parseJsonReceivedConfirm(message: String): String? {
        // Extract ver and id for building confirm response
        try {
            val verMatch = Regex(""""ver"\s*:\s*"([^"]+)"""").find(message)
            val idMatch = Regex(""""id"\s*:\s*(\d+)""").find(message)

            if (verMatch != null && idMatch != null) {
                val ver = verMatch.groupValues[1]
                val id = idMatch.groupValues[1]
                // Return confirm JSON with allow=1 (auto-accept interactive prompts)
                return """{"confirm":{"ver":"$ver","id":$id,"allow":1}}"""
            }
            return null
        } catch (e: Exception) {
            logger.error("Failed to parse JsonReceived", e)
            return null
        }
    }

    fun extractTerminalId(message: String): String? {
        val match = Regex(""""TerminalID"\s*:\s*"([^"]+)"""").find(message)
        return match?.groupValues?.get(1)
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
