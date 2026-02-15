package no.cloudberries.lpg.payment.terminal.sim.controller

import jakarta.servlet.http.HttpServletRequest
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.domain.Scenario
import no.cloudberries.lpg.payment.terminal.sim.model.request.CashbackRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.CompletionRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.PurchaseRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.RefundRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.ReservationRequest
import no.cloudberries.lpg.payment.terminal.sim.model.response.OperationResponse
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioDefinition
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioFlowEvent
import no.cloudberries.lpg.payment.terminal.sim.service.ReceiptGenerator
import no.cloudberries.lpg.payment.terminal.sim.service.ReservationStore
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioSelection
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventPublisher
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Financial operations controller (purchase, refund, cashback).
 */
@RestController
@RequestMapping("/v1/payments")
class PaymentController(
    private val stateManager: TerminalStateManager,
    private val scenarioManager: ScenarioManager,
    private val receiptGenerator: ReceiptGenerator,
    private val eventPublisher: TerminalEventPublisher,
    private val reservationStore: ReservationStore,
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    // Idempotency cache: ClientRequestId -> OperationResponse
    private val operationCache = ConcurrentHashMap<String, CachedOperation>()

    private data class CachedOperation(
        val status: Int,
        val response: OperationResponse
    )

    /**
     * POST /v1/payments/purchase
     *
     * Perform a card purchase operation.
     */
    @PostMapping("/purchase")
    fun purchase(
        @RequestBody request: PurchaseRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Purchase request: amount={}, operatorId={}, clientRequestId={}",
            request.amountMinor, request.operatorId, request.clientRequestId)

        cachedResponse(request.clientRequestId)?.let { return it }

        // Select scenario
        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioManager.selectFieldRejection(scenarioSelection) ?: scenarioSelection.enumScenario
        log.debug("Selected scenario: {}", scenario)

        // Execute operation
        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            // Publish operation started event
            eventPublisher.publish(
                eventType = "OperationStarted",
                operationId = operationId,
                payload = mapOf(
                    "OperationType" to "purchase",
                    "AmountMinor" to request.amountMinor
                )
            )

            // Simulate operation delay
            val delay = scenarioManager.getOperationDelay(scenarioSelection)
            log.debug("Simulating operation delay: {} ms", delay)

            val flow = scenarioSelection.definition?.flow?.takeIf { it.isNotEmpty() } ?: defaultPurchaseFlow()
            val flowDelay = publishDisplayFlow(operationId, flow, delay)
            val remainingDelay = delay - flowDelay
            if (remainingDelay > 0) {
                Thread.sleep(remainingDelay)
            }

            // Generate response based on scenario
            val completedAt = Instant.now()
            val response = buildFinancialResponse(
                scenarioSelection,
                scenario,
                operationId,
                startedAt,
                completedAt,
                request.amountMinor,
                defaultReceiptTemplate = "purchase"
            )
            val responseWithReceipt = ensureReceipt(response, request.amountMinor, completedAt, operationId, "purchase")
            val finalResponse = applyRejectedError(responseWithReceipt)
            val status = if (finalResponse.Success) HttpStatus.OK else HttpStatus.UNPROCESSABLE_ENTITY

            if (finalResponse.Success.not() && flow.none { it.displayTextId == 1001 }) {
                eventPublisher.publish("DisplayText", operationId, mapOf("text" to "TA UT KORTET", "displayTextId" to 1001))
            }

            // Publish events
            eventPublisher.publish(
                eventType = "DisplayText",
                operationId = operationId,
                payload = mapOf("text" to (finalResponse.LastDisplayText ?: ""))
            )

            finalResponse.PrintTextRaw?.let {
                eventPublisher.publish(
                    eventType = "PrintText",
                    operationId = operationId,
                    payload = mapOf("text" to it)
                )
            }

            eventPublisher.publish(
                eventType = "OperationCompleted",
                operationId = operationId,
                payload = mapOf(
                    "Success" to finalResponse.Success,
                    "OperationType" to "purchase",
                    "AmountMinor" to request.amountMinor
                )
            )

            // Cache for idempotency
            cacheResponse(request.clientRequestId, status, finalResponse)

            log.info("Purchase completed: operationId={}, success={}", operationId, finalResponse.Success)
            return ResponseEntity.status(status).body(finalResponse)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/payments/reservation
     *
     * Reserve an amount on the card (pre-auth). Pump is freed after approval.
     * Actual charge happens via completion when filling stops.
     */
    @PostMapping("/reservation")
    fun reservation(
        @RequestBody request: ReservationRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Reservation request: amount={} kr, operatorId={}, clientRequestId={}",
            request.amountMinor / 100.0, request.operatorId, request.clientRequestId)

        cachedResponse(request.clientRequestId)?.let { return it }

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioManager.selectFieldRejection(scenarioSelection) ?: scenarioSelection.enumScenario

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish(
                eventType = "OperationStarted",
                operationId = operationId,
                payload = mapOf(
                    "OperationType" to "reservation",
                    "AmountMinor" to request.amountMinor
                )
            )

            val delay = scenarioManager.getOperationDelay(scenarioSelection)
            val flow = scenarioSelection.definition?.flow?.takeIf { it.isNotEmpty() } ?: defaultPurchaseFlow()
            val flowDelay = publishDisplayFlow(operationId, flow, delay)
            val remainingDelay = delay - flowDelay
            if (remainingDelay > 0) {
                Thread.sleep(remainingDelay)
            }

            val completedAt = Instant.now()
            val response = buildFinancialResponse(
                scenarioSelection,
                scenario,
                operationId,
                startedAt,
                completedAt,
                request.amountMinor,
                defaultReceiptTemplate = "purchase"
            )

            val responseWithReceipt = ensureReceipt(response, request.amountMinor, completedAt, operationId, "purchase")
            val finalResponse = applyRejectedError(responseWithReceipt)
            val status = if (finalResponse.Success) HttpStatus.OK else HttpStatus.UNPROCESSABLE_ENTITY

            if (finalResponse.Success) {
                reservationStore.put(operationId, request.amountMinor)
            }

            eventPublisher.publish(
                eventType = "OperationCompleted",
                operationId = operationId,
                payload = mapOf(
                    "Success" to finalResponse.Success,
                    "OperationType" to "reservation",
                    "AmountMinor" to request.amountMinor,
                    "ResponseCode" to (finalResponse.ResponseCode ?: ""),
                    "EntryMode" to "CHIP"
                )
            )

            cacheResponse(request.clientRequestId, status, finalResponse)

            log.info("Reservation completed: operationId={}, success={}, amount={} kr",
                operationId, finalResponse.Success, request.amountMinor / 100.0)
            return ResponseEntity.status(status).body(finalResponse)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/payments/completion
     *
     * Complete a prior reservation by charging the actual amount (e.g. after filling).
     */
    @PostMapping("/completion")
    fun completion(
        @RequestBody request: CompletionRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Completion request: operationId={}, amount={} kr",
            request.operationId, request.amountMinor / 100.0)

        val reservedAmount = reservationStore.get(request.operationId)
        if (reservedAmount == null) {
            log.warn("Completion failed: no pending reservation for operationId={}", request.operationId)
            return ResponseEntity.badRequest().body(
                OperationResponse(
                    Success = false,
                    OperationId = request.operationId,
                    StartedAt = Instant.now().toString(),
                    CompletedAt = null,
                    CallResult = 0,
                    Error = "No pending reservation",
                    ErrorCode = "invalid_operation"
                )
            )
        }

        if (request.amountMinor > reservedAmount) {
            log.warn("Completion failed: amount {} exceeds reserved {}", request.amountMinor, reservedAmount)
            return ResponseEntity.badRequest().body(
                OperationResponse(
                    Success = false,
                    OperationId = request.operationId,
                    StartedAt = Instant.now().toString(),
                    CompletedAt = null,
                    CallResult = 0,
                    Error = "Amount exceeds reservation",
                    ErrorCode = "amount_exceeded"
                )
            )
        }

        val operationId = request.operationId
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            val receipt = receiptGenerator.generatePurchaseReceipt(
                request.amountMinor,
                Instant.now(),
                operationId,
                approved = true,
                responseCode = "00"
            )

            val completedAt = Instant.now()
            val response = OperationResponse.approved(
                operationId,
                startedAt,
                completedAt,
                request.amountMinor,
                config.terminalId,
                config.merchantId,
                receipt
            )

            reservationStore.remove(operationId)

            eventPublisher.publish(
                eventType = "DisplayText",
                operationId = operationId,
                payload = mapOf("text" to "GODKJENT", "amountMinor" to request.amountMinor)
            )
            response.PrintTextRaw?.let {
                eventPublisher.publish("PrintText", operationId, mapOf("text" to it))
            }
            eventPublisher.publish(
                eventType = "OperationCompleted",
                operationId = operationId,
                payload = mapOf(
                    "Success" to true,
                    "OperationType" to "completion",
                    "AmountMinor" to request.amountMinor
                )
            )

            log.info("Completion completed: operationId={}, amount={} kr", operationId, request.amountMinor / 100.0)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/payments/refund
     *
     * Perform a refund operation.
     */
    @PostMapping("/refund")
    fun refund(
        @RequestBody request: RefundRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Refund request: amount={}, operatorId={}, clientRequestId={}",
            request.amountMinor, request.operatorId, request.clientRequestId)

        cachedResponse(request.clientRequestId)?.let { return it }

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioManager.selectFieldRejection(scenarioSelection) ?: scenarioSelection.enumScenario

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId,
                mapOf("OperationType" to "refund", "AmountMinor" to request.amountMinor))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val response = buildFinancialResponse(
                scenarioSelection,
                scenario,
                operationId,
                startedAt,
                completedAt,
                request.amountMinor,
                defaultReceiptTemplate = "refund"
            )
            val responseWithReceipt = ensureReceipt(response, request.amountMinor, completedAt, operationId, "refund")
            val finalResponse = applyRejectedError(responseWithReceipt)
            val status = if (finalResponse.Success) HttpStatus.OK else HttpStatus.UNPROCESSABLE_ENTITY

            eventPublisher.publish("OperationCompleted", operationId, mapOf("Success" to finalResponse.Success))

            cacheResponse(request.clientRequestId, status, finalResponse)

            log.info("Refund completed: operationId={}, success={}", operationId, finalResponse.Success)
            return ResponseEntity.status(status).body(finalResponse)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/payments/cashback
     *
     * Perform a combined purchase + cashback operation.
     */
    @PostMapping("/cashback")
    fun cashback(
        @RequestBody request: CashbackRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Cashback request: purchase={}, cashback={}, clientRequestId={}",
            request.purchaseMinor, request.cashbackMinor, request.clientRequestId)

        cachedResponse(request.clientRequestId)?.let { return it }

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioManager.selectFieldRejection(scenarioSelection) ?: scenarioSelection.enumScenario

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId,
                mapOf("OperationType" to "cashback", "PurchaseMinor" to request.purchaseMinor, "CashbackMinor" to request.cashbackMinor))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val totalAmount = request.purchaseMinor + request.cashbackMinor
            val response = buildFinancialResponse(
                scenarioSelection,
                scenario,
                operationId,
                startedAt,
                completedAt,
                totalAmount,
                defaultReceiptTemplate = "purchase"
            )
            val responseWithReceipt = ensureReceipt(response, totalAmount, completedAt, operationId, "purchase")
            val finalResponse = applyRejectedError(responseWithReceipt)
            val status = if (finalResponse.Success) HttpStatus.OK else HttpStatus.UNPROCESSABLE_ENTITY

            eventPublisher.publish("OperationCompleted", operationId, mapOf("Success" to finalResponse.Success))

            cacheResponse(request.clientRequestId, status, finalResponse)

            log.info("Cashback completed: operationId={}, success={}", operationId, finalResponse.Success)
            return ResponseEntity.status(status).body(finalResponse)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    private fun defaultPurchaseFlow(): List<ScenarioFlowEvent> {
        return listOf(
            ScenarioFlowEvent(event = "DisplayText", text = "VENTER PÅ KORTET", displayTextId = 1011, delayMs = 500),
            ScenarioFlowEvent(event = "DisplayText", text = "SETT INN KORTET", displayTextId = 1003, delayMs = 500),
            ScenarioFlowEvent(event = "DisplayText", text = "Kode + OK", displayTextId = 1, delayMs = 1000)
        )
    }

    private fun publishDisplayFlow(operationId: String, flow: List<ScenarioFlowEvent>, defaultDelay: Long): Long {
        if (flow.isEmpty()) {
            return 0
        }

        var totalDelay = 0L
        val fallbackDelay = if (flow.isNotEmpty()) defaultDelay / flow.size else defaultDelay
        flow.forEach { event ->
            if (event.event.equals("DisplayText", ignoreCase = true)) {
                eventPublisher.publish(
                    "DisplayText",
                    operationId,
                    mapOf(
                        "text" to (event.text ?: ""),
                        "displayTextId" to (event.displayTextId ?: 0)
                    )
                )
            }

            val delay = (event.delayMs ?: fallbackDelay).coerceAtLeast(0)
            if (delay > 0) {
                Thread.sleep(delay)
            }
            totalDelay += delay
        }

        return totalDelay
    }

    private fun buildFinancialResponse(
        scenarioSelection: ScenarioSelection,
        scenario: Scenario,
        operationId: String,
        startedAt: Instant,
        completedAt: Instant,
        amountMinor: Int,
        defaultReceiptTemplate: String
    ): OperationResponse {
        val definition = scenarioSelection.definition
        val result = definition?.result
        if (definition != null && result != null) {
            val receipt = generateReceipt(definition, result, amountMinor, completedAt, operationId, defaultReceiptTemplate)
            return when {
                result.success -> OperationResponse.approved(
                    operationId,
                    startedAt,
                    completedAt,
                    amountMinor,
                    config.terminalId,
                    config.merchantId,
                    receipt ?: ""
                )
                result.rejectionReason == "2:1" -> OperationResponse.userCancel(operationId, startedAt, completedAt)
                result.responseCode == "Z1" -> OperationResponse.wrongPin(operationId, startedAt, completedAt, receipt)
                result.responseCode == "05" -> OperationResponse.declined(operationId, startedAt, completedAt, receipt)
                else -> OperationResponse(
                    Success = false,
                    OperationId = operationId,
                    StartedAt = startedAt.toString(),
                    CompletedAt = completedAt.toString(),
                    DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                    CallResult = 1,
                    ResultEventName = "OnLocalMode",
                    LocalModeResult = result.localModeResult ?: 2,
                    ResponseCode = result.responseCode,
                    RejectionSource = result.rejectionSource,
                    RejectionReason = result.rejectionReason,
                    PrintTextRaw = receipt,
                    PrintTextSanitized = receipt,
                    LastDisplayText = "AVVIST"
                )
            }
        }

        return when (scenario) {
            Scenario.APPROVED -> {
                val receipt = receiptGenerator.generatePurchaseReceipt(
                    amountMinor, completedAt, operationId, approved = true, responseCode = "00"
                )
                OperationResponse.approved(
                    operationId, startedAt, completedAt,
                    amountMinor,
                    config.terminalId, config.merchantId,
                    receipt
                )
            }
            Scenario.WRONG_PIN -> {
                val receipt = receiptGenerator.generatePurchaseReceipt(
                    amountMinor, completedAt, operationId, approved = false, responseCode = "Z1"
                )
                OperationResponse.wrongPin(operationId, startedAt, completedAt, receipt)
            }
            Scenario.USER_CANCEL -> OperationResponse.userCancel(operationId, startedAt, completedAt)
            Scenario.DECLINED -> {
                val receipt = receiptGenerator.generatePurchaseReceipt(
                    amountMinor, completedAt, operationId, approved = false, responseCode = "05"
                )
                OperationResponse.declined(operationId, startedAt, completedAt, receipt)
            }
            else -> {
                val receipt = receiptGenerator.generatePurchaseReceipt(
                    amountMinor, completedAt, operationId, approved = true, responseCode = "00"
                )
                OperationResponse.approved(
                    operationId, startedAt, completedAt,
                    amountMinor,
                    config.terminalId, config.merchantId,
                    receipt
                )
            }
        }
    }

    private fun generateReceipt(
        definition: ScenarioDefinition,
        result: no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioResult,
        amountMinor: Int,
        completedAt: Instant,
        operationId: String,
        defaultTemplate: String
    ): String? {
        val receiptConfig = definition.receipt
        val template = receiptConfig?.template ?: defaultTemplate
        val approved = receiptConfig?.approved ?: result.success
        val responseCode = receiptConfig?.responseCode ?: result.responseCode

        return when (template.lowercase()) {
            "none" -> null
            "refund" -> receiptGenerator.generateRefundReceipt(amountMinor, completedAt, operationId)
            else -> receiptGenerator.generatePurchaseReceipt(
                amountMinor,
                completedAt,
                operationId,
                approved = approved,
                responseCode = responseCode
            )
        }
    }

    private fun cachedResponse(clientRequestId: String?): ResponseEntity<OperationResponse>? {
        if (clientRequestId.isNullOrBlank()) {
            return null
        }
        val cached = operationCache[clientRequestId] ?: return null
        log.info("Returning cached response for clientRequestId={}", clientRequestId)
        return ResponseEntity.status(cached.status).body(cached.response)
    }

    private fun cacheResponse(clientRequestId: String?, status: HttpStatus, response: OperationResponse) {
        if (clientRequestId.isNullOrBlank()) {
            return
        }
        operationCache[clientRequestId] = CachedOperation(status.value(), response)
    }

    private fun ensureReceipt(
        response: OperationResponse,
        amountMinor: Int,
        completedAt: Instant,
        operationId: String,
        template: String
    ): OperationResponse {
        if (!config.field.isEnabled(config.profile)) {
            return response
        }
        if (response.PrintTextRaw != null) {
            return response
        }
        val receipt = when (template.lowercase()) {
            "refund" -> receiptGenerator.generateRefundReceipt(amountMinor, completedAt, operationId)
            else -> receiptGenerator.generatePurchaseReceipt(
                amountMinor,
                completedAt,
                operationId,
                approved = response.Success,
                responseCode = response.ResponseCode
            )
        }
        return response.copy(PrintTextRaw = receipt, PrintTextSanitized = receipt)
    }

    private fun applyRejectedError(response: OperationResponse): OperationResponse {
        if (response.Success) {
            return response
        }
        val errorCode = response.ErrorCode ?: "operation_rejected"
        val errorText = response.Error ?: "Terminal rejected the operation"
        return response.copy(Error = errorText, ErrorCode = errorCode)
    }
}
