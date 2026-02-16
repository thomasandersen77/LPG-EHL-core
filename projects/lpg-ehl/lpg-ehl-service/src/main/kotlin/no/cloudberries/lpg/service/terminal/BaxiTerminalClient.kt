package no.cloudberries.lpg.service.terminal

import no.cloudberries.norgesgass.baxi.client.BaxiClient
import no.cloudberries.norgesgass.baxi.client.BaxiClientImpl
import no.cloudberries.norgesgass.baxi.client.TransferAmountArgs
import no.cloudberries.norgesgass.baxi.client.AdministrationArgs
import no.cloudberries.norgesgass.baxi.config.BaxiIniConfig
import no.cloudberries.norgesgass.baxi.events.BaxiEventListener
import no.cloudberries.norgesgass.baxi.events.LastFinancialResultEvent
import no.cloudberries.norgesgass.baxi.events.LocalModeEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import jakarta.annotation.PreDestroy

class BaxiTerminalClient(
    private val terminalHost: String,
    private val terminalPort: Int,
    private val baxiClient: BaxiClient = BaxiClientImpl()
) : TerminalClient {

    private val log = LoggerFactory.getLogger(javaClass)
    
    private val terminalReadyFuture = AtomicReference<CompletableFuture<Boolean>>(CompletableFuture())
    private var operationFuture = AtomicReference<CompletableFuture<TerminalOperationResponse>?>(null)
    
    @Volatile
    private var isTerminalOpen = false
    @Volatile
    private var isTerminalReady = false
    @Volatile
    private var lastError: String? = null
    @Volatile
    private var lastDisplayText: String? = null
    @Volatile
    private var printTextAccumulator = StringBuilder()

    private var capturedLocalMode = AtomicReference<LocalModeEvent?>(null)
    private var capturedFinancialResult = AtomicReference<LastFinancialResultEvent?>(null)

    init {
        baxiClient.setEventListener(object : BaxiEventListener {
            override fun onTerminalReady() {
                log.info("Baxi terminal ready")
                isTerminalReady = true
                terminalReadyFuture.get().complete(true)
            }

            override fun onDisplayText(displayText: String, displayTextSourceId: Int?, displayTextId: Int?) {
                log.debug("Baxi display text: {}", displayText)
                lastDisplayText = displayText
            }

            override fun onPrintText(printText: String) {
                log.debug("Baxi print text: {}", printText)
                printTextAccumulator.append(printText).append("\n")
            }

            override fun onError(errorCode: Int, errorString: String?) {
                log.error("Baxi error: code={}, message={}", errorCode, errorString)
                lastError = "Error $errorCode: $errorString"
                
                val currentOpFuture = operationFuture.get()
                if (currentOpFuture != null && !currentOpFuture.isDone) {
                    currentOpFuture.complete(TerminalOperationResponse(
                        success = false,
                        error = errorString,
                        errorCode = errorCode.toString()
                    ))
                }
                
                if (!terminalReadyFuture.get().isDone) {
                    terminalReadyFuture.get().complete(false)
                }
            }

            override fun onLocalMode(event: LocalModeEvent) {
                log.info("Baxi local mode: {}", event)
                capturedLocalMode.set(event)
                checkOperationComplete()
            }

            override fun onLastFinancialResult(event: LastFinancialResultEvent) {
                log.info("Baxi last financial result: {}", event)
                capturedFinancialResult.set(event)
                checkOperationComplete()
            }
        })
    }

    private fun checkOperationComplete() {
        val local = capturedLocalMode.get()
        val financial = capturedFinancialResult.get()
        val currentOpFuture = operationFuture.get()
        
        if (local != null && financial != null && currentOpFuture != null && !currentOpFuture.isDone) {
            val response = TerminalOperationResponse(
                success = local.result == 1,
                operationId = local.fields["AuthCode"] ?: local.fields["SessionId"],
                callResult = local.result,
                responseCode = local.responseCode,
                rejectionReason = local.rejectionReason,
                localModeResultData = local.localModeResultData,
                localModeResult = financial.result,
                printTextRaw = printTextAccumulator.toString(),
                printTextSanitized = printTextAccumulator.toString().trim(),
                lastDisplayText = lastDisplayText
            )
            currentOpFuture.complete(response)
        }
    }

    override fun openTerminal(): TerminalSimpleResponse {
        log.info("Opening Baxi terminal at {}:{}", terminalHost, terminalPort)
        
        terminalReadyFuture.set(CompletableFuture())
        isTerminalReady = false
        
        val config = BaxiIniConfig(
            hostIpAddress = terminalHost,
            hostPort = terminalPort,
            vendorInfoExtended = "LPG-EHL-SERVICE",
            socketListenerEnabled = false,
            socketListenerPort = null
        )

        val result = baxiClient.open(config)
        log.info("BaxiClient.open result: {}", result)
        if (result.callResult != 1) {
            return TerminalSimpleResponse(
                success = false, 
                error = "Call to open failed: ${result.methodRejectCode} - ${result.methodRejectInfo}"
            )
        }

        log.info("Waiting for onTerminalReady callback...")
        return try {
            val ready = terminalReadyFuture.get().get(30, TimeUnit.SECONDS)
            if (ready) {
                isTerminalOpen = true
                TerminalSimpleResponse(success = true, message = "Baxi terminal opened and ready")
            } else {
                TerminalSimpleResponse(success = false, error = "Baxi terminal reported error during opening: $lastError")
            }
        } catch (e: TimeoutException) {
            TerminalSimpleResponse(success = false, error = "Timeout waiting for Baxi terminal ready")
        } catch (e: Exception) {
            TerminalSimpleResponse(success = false, error = "Error opening Baxi terminal: ${e.message}")
        }
    }

    override fun getHealth(): TerminalHealthResponse {
        return TerminalHealthResponse(
            status = if (isTerminalReady) "healthy" else "unhealthy",
            configLoaded = true
        )
    }

    override fun getStatus(): TerminalStatusResponse {
        return TerminalStatusResponse(
            terminalOpen = isTerminalOpen,
            terminalReady = isTerminalReady,
            connectionState = if (isTerminalReady) "CONNECTED" else "DISCONNECTED",
            lastError = lastError
        )
    }

    override fun closeTerminal(): TerminalSimpleResponse {
        log.info("Closing Baxi terminal")
        val result = baxiClient.closeTerminal()
        isTerminalOpen = false
        isTerminalReady = false
        return if (result.callResult == 1) {
            TerminalSimpleResponse(success = true, message = "Baxi terminal closed")
        } else {
            TerminalSimpleResponse(success = false, error = "Close failed: ${result.methodRejectCode}")
        }
    }

    override fun purchase(request: TerminalPurchaseRequest): TerminalOperationResponse {
        if (!isTerminalReady) {
            return TerminalOperationResponse(success = false, error = "Terminal not ready")
        }

        log.info("Starting purchase for amount: {}", request.amountMinor)
        
        capturedLocalMode.set(null)
        capturedFinancialResult.set(null)
        printTextAccumulator.setLength(0)
        
        val future = CompletableFuture<TerminalOperationResponse>()
        operationFuture.set(future)

        val args = TransferAmountArgs(
            operId = request.operatorId,
            type1 = 10, // 10 = Purchase
            amount1 = request.amountMinor,
            type2 = 0,
            amount2 = 0,
            type3 = 0,
            amount3 = 0,
            optionalData = request.optionalData
        )

        val result = baxiClient.transferAmount(args)
        if (result.callResult != 1) {
            operationFuture.set(null)
            return TerminalOperationResponse(
                success = false, 
                error = "Call to transferAmount failed: ${result.methodRejectCode} - ${result.methodRejectInfo}"
            )
        }

        return try {
            future.get(60, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            TerminalOperationResponse(success = false, error = "Purchase timed out")
        } catch (e: Exception) {
            TerminalOperationResponse(success = false, error = "Purchase failed: ${e.message}")
        } finally {
            operationFuture.set(null)
        }
    }

    override fun reversal(operationId: String?): TerminalOperationResponse {
        if (!isTerminalReady) {
            return TerminalOperationResponse(success = false, error = "Terminal not ready")
        }

        log.info("Starting reversal for operationId: {}", operationId)
        
        capturedLocalMode.set(null)
        capturedFinancialResult.set(null)
        printTextAccumulator.setLength(0)
        
        val future = CompletableFuture<TerminalOperationResponse>()
        operationFuture.set(future)

        val args = AdministrationArgs(
            admCode = 9100, // 9100 = Reversal
            operId = "0000",
            optionalData = operationId
        )

        val result = baxiClient.administration(args)
        if (result.callResult != 1) {
            operationFuture.set(null)
            return TerminalOperationResponse(
                success = false, 
                error = "Call to administration failed: ${result.methodRejectCode} - ${result.methodRejectInfo}"
            )
        }

        return try {
            future.get(30, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            TerminalOperationResponse(success = false, error = "Reversal timed out")
        } catch (e: Exception) {
            TerminalOperationResponse(success = false, error = "Reversal failed: ${e.message}")
        } finally {
            operationFuture.set(null)
        }
    }
    
    @PreDestroy
    fun cleanup() {
        baxiClient.close()
    }
}
