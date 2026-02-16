package no.cloudberries.lpg.netscloud

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class NetsMessageBuilder(
    private val config: NetsCloudConnectConfig
) {
    
    fun buildOpenRequest(): String {
        val ecrId = generateEcrId()
        // Note: Using compact JSON format without whitespace for actual transmission
        return """{"NetsRequest":{"MessageHeader":{"$":{"ECRID":"$ecrId","TerminalID":"${config.terminalId}","VersionNumber":"1"}},"Open":{}}}"""
    }
    
    fun buildPurchaseRequest(
        amountMinor: Int,
        operatorId: String = "0000"
    ): String {
        val ecrId = generateEcrId()
        return """{"NetsRequest":{"MessageHeader":{"$":{"ECRID":"$ecrId","TerminalID":"${config.terminalId}","VersionNumber":"1"}},"Dfs13TransferAmount":{"TransactionType":"48","OperId":"$operatorId","Amount1":"$amountMinor","Amount2":"0","Amount3":"0","Type2":"48","Type3":"48","HostData":"","OptionalData":""}}}"""
    }
    
    fun buildReversalRequest(): String {
        val ecrId = generateEcrId()
        return """{"NetsRequest":{"MessageHeader":{"$":{"ECRID":"$ecrId","TerminalID":"${config.terminalId}","VersionNumber":"1"}},"Dfs13Reversal":{}}}"""
    }
    
    fun buildCloseRequest(): String {
        val ecrId = generateEcrId()
        return """{"NetsRequest":{"MessageHeader":{"$":{"ECRID":"$ecrId","TerminalID":"${config.terminalId}","VersionNumber":"1"}},"Close":{}}}"""
    }
    
    private fun generateEcrId(): String {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        return "POS-$timestamp-$suffix"
    }
}
