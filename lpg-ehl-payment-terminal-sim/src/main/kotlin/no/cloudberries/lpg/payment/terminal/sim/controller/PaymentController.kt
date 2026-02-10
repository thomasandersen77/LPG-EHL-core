package no.cloudberries.lpg.payment.terminal.sim.controller

import jakarta.servlet.http.HttpServletRequest
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.domain.Scenario
import no.cloudberries.lpg.payment.terminal.sim.model.request.CashbackRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.PurchaseRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.RefundRequest
import no.cloudberries.lpg.payment.terminal.sim.model.response.OperationResponse
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioDefinition
import no.cloudberries.lpg.payment.terminal.sim.model.scenario.ScenarioFlowEvent
import no.cloudberries.lpg.payment.terminal.sim.service.EventStore
import no.cloudberries.lpg.payment.terminal.sim.service.ReceiptGenerator
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioSelection
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
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
    private val eventStore: EventStore,
    private val config: SimulatorConfig
) {
    private val log = LoggerFactory.getLogger(PaymentController::class.java)

    // Idempotency cache: ClientRequestId -> OperationResponse
    private val operationCache = ConcurrentHashMap<String, OperationResponse>()

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

        // Check idempotency
        request.clientRequestId?.let { clientId ->
            operationCache[clientId]?.let { cached ->
                log.info("Returning cached response for clientRequestId={}", clientId)
                return ResponseEntity.ok(cached)
            }
        }

        // Select scenario
        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioSelection.enumScenario
        log.debug("Selected scenario: {}", scenario)

        // Execute operation
        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            // Publish operation started event
            eventStore.publishEvent(
                eventType = "OperationStarted",
                operationId = operationId,
                payload = mapOf(
                    "type" to "purchase",
                    "amountMinor" to request.amountMinor
                )
            )

            // Simulate operation delay
            val delay = scenarioManager.getOperationDelay(scenarioSelection)
            log.debug("Simulating operation delay: {} ms", delay)

            val flow = scenarioSelection.definition?.flow?.takeIf { it.isNotEmpty() } ?: defaultPurchaseFlow()
            publishDisplayFlow(operationId, flow, delay)

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

            if (response.Success.not() && flow.none { it.displayTextId == 1001 }) {
                eventStore.publishEvent("DisplayText", operationId, mapOf("text" to "TA UT KORTET", "displayTextId" to 1001))
            }

            // Publish events
            eventStore.publishEvent(
                eventType = "DisplayText",
                operationId = operationId,
                payload = mapOf("text" to (response.LastDisplayText ?: ""))
            )

            response.PrintTextRaw?.let {
                eventStore.publishEvent(
                    eventType = "PrintText",
                    operationId = operationId,
                    payload = mapOf("text" to it)
                )
            }

            eventStore.publishEvent(
                eventType = "OperationCompleted",
                operationId = operationId,
                payload = mapOf("success" to response.Success)
            )

            // Cache for idempotency
            request.clientRequestId?.let { clientId ->
                operationCache[clientId] = response
            }

            log.info("Purchase completed: operationId={}, success={}", operationId, response.Success)
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

        // Check idempotency
        request.clientRequestId?.let { clientId ->
            operationCache[clientId]?.let { cached ->
                log.info("Returning cached response for clientRequestId={}", clientId)
                return ResponseEntity.ok(cached)
            }
        }

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioSelection.enumScenario

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventStore.publishEvent("OperationStarted", operationId,
                mapOf("type" to "refund", "amountMinor" to request.amountMinor))

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

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

            request.clientRequestId?.let { clientId ->
                operationCache[clientId] = response
            }

            log.info("Refund completed: operationId={}, success={}", operationId, response.Success)
            return ResponseEntity.ok(response)
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

        request.clientRequestId?.let { clientId ->
            operationCache[clientId]?.let { cached ->
                return ResponseEntity.ok(cached)
            }
        }

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)
        val scenario = scenarioSelection.enumScenario

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventStore.publishEvent("OperationStarted", operationId,
                mapOf("type" to "cashback", "purchaseMinor" to request.purchaseMinor, "cashbackMinor" to request.cashbackMinor))

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

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

            request.clientRequestId?.let { clientId ->
                operationCache[clientId] = response
            }

            log.info("Cashback completed: operationId={}, success={}", operationId, response.Success)
            return ResponseEntity.ok(response)
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

    private fun publishDisplayFlow(operationId: String, flow: List<ScenarioFlowEvent>, defaultDelay: Long) {
        if (flow.isEmpty()) {
            return
        }

        val fallbackDelay = if (flow.isNotEmpty()) defaultDelay / flow.size else defaultDelay
        flow.forEach { event ->
            if (event.event.equals("DisplayText", ignoreCase = true)) {
                eventStore.publishEvent(
                    "DisplayText",
                    operationId,
                    mapOf(
                        "text" to (event.text ?: ""),
                        "displayTextId" to (event.displayTextId ?: 0)
                    )
                )
            }

            val delay = event.delayMs ?: fallbackDelay
            if (delay > 0) {
                Thread.sleep(delay)
            }
        }
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
}
