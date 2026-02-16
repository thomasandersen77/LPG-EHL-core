package no.cloudberries.lpg.payment.terminal.sim.controller

import jakarta.servlet.http.HttpServletRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.AdminCodeRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.AdminRequest
import no.cloudberries.lpg.payment.terminal.sim.model.response.OperationResponse
import no.cloudberries.lpg.payment.terminal.sim.service.ReceiptGenerator
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
import no.cloudberries.lpg.payment.terminal.sim.service.LastReceiptStore
import no.cloudberries.lpg.payment.terminal.sim.service.ReportState
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalEventPublisher
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

/**
 * Administrative operations controller.
 */
@RestController
@RequestMapping("/v1/admin")
class AdminController(
    private val stateManager: TerminalStateManager,
    private val scenarioManager: ScenarioManager,
    private val receiptGenerator: ReceiptGenerator,
    private val eventPublisher: TerminalEventPublisher,
    private val lastReceiptStore: LastReceiptStore,
    private val reportState: ReportState
) {
    private val log = LoggerFactory.getLogger(AdminController::class.java)

    /**
     * POST /v1/admin/avstemming
     *
     * Perform reconciliation (avstemming).
     */
    @PostMapping("/avstemming")
    fun avstemming(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Avstemming request: password={}", request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId, mapOf("type" to "avstemming"))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val receipt = receiptGenerator.generateAvstemmingReport(completedAt)

            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "AVSTEMMING OK"
            ).copy(
                PrintTextRaw = receipt,
                PrintTextSanitized = receipt
            )

            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Avstemming completed: operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/cancel
     *
     * Cancel current operation.
     */
    @PostMapping("/cancel")
    fun cancel(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Cancel request: password={}", request.password)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()
        val completedAt = Instant.now()

        // Cancel is immediate; real terminal shows "Avbrutt"
        val response = OperationResponse.adminSuccess(
            operationId, startedAt, completedAt, "Avbrutt"
        )

        log.info("Cancel completed: operationId={}", operationId)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /v1/admin/reversal
     *
     * Reverse last transaction. Simulator returns Formatfeil (no txn to reverse) like real terminal.
     */
    @PostMapping("/reversal")
    fun reversal(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Reversal request: password={}", request.password)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()
        val completedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId, mapOf("type" to "reversal"))
            val response = OperationResponse.adminFormatError(
                operationId, startedAt, completedAt,
                displayText = "Formatfeil",
                rejectionReason = "4:6"
            )
            eventPublisher.publish("DisplayText", operationId, mapOf("text" to "Formatfeil"))
            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to false))
            log.info("Reversal completed (no txn): operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/z-report
     *
     * Generate Z-report (end-of-day report).
     */
    @PostMapping("/z-report")
    fun zReport(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Z-report request: password={}", request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId, mapOf("type" to "z-report"))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val receipt = receiptGenerator.generateZReport(completedAt)
            val batchCount = 0
            val batchAmount = "0,00"
            val batchAmountMinor = 0
            val zNum = "001"
            val lastZTs = completedAt.toString()

            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "Z-RAPPORT OK",
                printTextRaw = receipt,
                reportFields = mapOf(
                    "reportType" to "z",
                    "zReportNumber" to zNum,
                    "zLastTotalTimestampLocal" to lastZTs,
                    "batchTotalCount" to batchCount.toString(),
                    "batchTotalAmount" to batchAmount,
                    "batchTotalAmountMinor" to batchAmountMinor.toString(),
                    "scheme_BankAxept_count" to batchCount.toString(),
                    "scheme_BankAxept_amount" to batchAmount
                )
            ).copy(PrintTextRaw = receipt, PrintTextSanitized = receipt)

            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Z-report completed: operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/last-receipt
     *
     * Return last printed receipt (real terminal style).
     */
    @PostMapping("/last-receipt")
    fun lastReceipt(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Last receipt request: password={}", request.password)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()
        val completedAt = Instant.now()

        val lastReceiptText = lastReceiptStore.get()
        val printText = lastReceiptText ?: "SISTE KVITTERING\n(ingen tidligere transaksjon)"

        val response = OperationResponse.adminSuccess(
            operationId, startedAt, completedAt, "SISTE KVITTERING",
            printTextRaw = printText
        ).copy(PrintTextRaw = printText, PrintTextSanitized = printText)

        log.info("Last receipt completed: operationId={}", operationId)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /v1/admin/software
     *
     * Download software update to terminal.
     */
    @PostMapping("/software")
    fun softwareDownload(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Software download request: password={}", request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId, mapOf("type" to "software"))

            // Software download takes longer
            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection) * 2)

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "SW NEDLASTNING OK"
            )

            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Software download completed: operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/dataset
     *
     * Download dataset to terminal.
     */
    @PostMapping("/dataset")
    fun datasetDownload(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Dataset download request: password={}", request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId, mapOf("type" to "dataset"))

            // Dataset download takes longer
            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection) * 2)

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "DATASET NEDLASTNING OK"
            )

            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Dataset download completed: operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/code
     *
     * Execute generic admin code.
     */
    @PostMapping("/code")
    fun genericAdminCode(
        @RequestBody request: AdminCodeRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Generic admin code request: code={}, password={}", request.code, request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventPublisher.publish("OperationStarted", operationId,
                mapOf("type" to "admin-code", "code" to request.code))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "ADMIN KMD OK (${request.code})"
            )

            eventPublisher.publish("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Generic admin code completed: operationId={}, code={}", operationId, request.code)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }
}
