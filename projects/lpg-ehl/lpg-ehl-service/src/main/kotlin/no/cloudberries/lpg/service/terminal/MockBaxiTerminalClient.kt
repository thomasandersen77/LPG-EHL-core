package no.cloudberries.lpg.service.terminal

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

class MockBaxiTerminalClient : TerminalClient {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private var isOpen = false
    private var isReady = false
    
    override fun openTerminal(): TerminalSimpleResponse {
        log.info("MockBaxiTerminalClient: openTerminal()")
        isOpen = true
        isReady = true
        return TerminalSimpleResponse(
            success = true,
            message = "Mock Baxi terminal opened"
        )
    }
    
    override fun getHealth(): TerminalHealthResponse {
        log.info("MockBaxiTerminalClient: getHealth()")
        return TerminalHealthResponse(
            status = if (isOpen) "healthy" else "not_open",
            configLoaded = true
        )
    }
    
    override fun getStatus(): TerminalStatusResponse {
        log.info("MockBaxiTerminalClient: getStatus()")
        return TerminalStatusResponse(
            terminalOpen = isOpen,
            terminalReady = isReady,
            connectionState = if (isReady) "CONNECTED" else "DISCONNECTED"
        )
    }
    
    override fun closeTerminal(): TerminalSimpleResponse {
        log.info("MockBaxiTerminalClient: closeTerminal()")
        isOpen = false
        isReady = false
        return TerminalSimpleResponse(
            success = true,
            message = "Mock Baxi terminal closed"
        )
    }
    
    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        log.info("MockBaxiTerminalClient: purchase(amountMinor={}, operatorId={})", 
            request.amountMinor, request.operatorId)
        
        if (!isReady) {
            return TerminalOperationResponse(
                success = false,
                error = "Terminal not ready",
                errorCode = "terminal_not_ready"
            )
        }
        
        return TerminalOperationResponse(
            success = true,
            operationId = "mock-op-${UUID.randomUUID()}",
            callResult = 1,
            responseCode = "00",
            localModeResult = 1,
            printTextSanitized = "MOCK RECEIPT\nAmount: ${request.amountMinor / 100.0} NOK\nApproved",
            durationMs = 1500
        )
    }
    
    override fun reversal(operationId: String?): TerminalOperationResponse {
        log.info("MockBaxiTerminalClient: reversal(operationId={})", operationId)
        
        return TerminalOperationResponse(
            success = true,
            operationId = operationId ?: "mock-reversal-${UUID.randomUUID()}",
            callResult = 1,
            responseCode = "00",
            localModeResult = 1,
            durationMs = 1000
        )
    }
}
