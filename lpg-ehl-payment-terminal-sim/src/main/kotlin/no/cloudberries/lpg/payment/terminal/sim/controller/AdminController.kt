package no.cloudberries.lpg.payment.terminal.sim.controller

import jakarta.servlet.http.HttpServletRequest
import no.cloudberries.lpg.payment.terminal.sim.config.SimulatorConfig
import no.cloudberries.lpg.payment.terminal.sim.model.request.AdminCodeRequest
import no.cloudberries.lpg.payment.terminal.sim.model.request.AdminRequest
import no.cloudberries.lpg.payment.terminal.sim.model.response.OperationResponse
import no.cloudberries.lpg.payment.terminal.sim.service.EventStore
import no.cloudberries.lpg.payment.terminal.sim.service.ReceiptGenerator
import no.cloudberries.lpg.payment.terminal.sim.service.ScenarioManager
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
    private val eventStore: EventStore,
    private val config: SimulatorConfig
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
            eventStore.publishEvent("OperationStarted", operationId, mapOf("type" to "avstemming"))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val receipt = receiptGenerator.generateAvstemmingReport(completedAt)

            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "AVSTEMMING OK"
            ).copy(
                PrintTextRaw = receipt,
                PrintTextSanitized = receipt
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

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

        // Cancel is immediate (no state transition needed)
        val response = OperationResponse.adminSuccess(
            operationId, startedAt, completedAt, "AVBRUTT"
        )

        log.info("Cancel completed: operationId={}", operationId)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /v1/admin/reversal
     *
     * Reverse last transaction.
     */
    @PostMapping("/reversal")
    fun reversal(
        @RequestBody request: AdminRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<OperationResponse> {
        log.info("Reversal request: password={}", request.password)

        val scenarioSelection = scenarioManager.selectScenario(httpRequest)
        scenarioManager.applyScenarioTerminalState(scenarioSelection)

        val operationId = UUID.randomUUID().toString()
        val startedAt = Instant.now()

        stateManager.beginOperation(operationId)
        try {
            eventStore.publishEvent("OperationStarted", operationId, mapOf("type" to "reversal"))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "ANNULLERING OK"
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Reversal completed: operationId={}", operationId)
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
            eventStore.publishEvent("OperationStarted", operationId, mapOf("type" to "z-report"))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val receipt = receiptGenerator.generateZReport(completedAt)

            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "Z-RAPPORT OK"
            ).copy(
                PrintTextRaw = receipt,
                PrintTextSanitized = receipt
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Z-report completed: operationId={}", operationId)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }

    /**
     * POST /v1/admin/last-receipt
     *
     * Print/retrieve last receipt.
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

        val response = OperationResponse.adminSuccess(
            operationId, startedAt, completedAt, "SISTE KVITTERING"
        ).copy(
            PrintTextRaw = "SISTE KVITTERING\n(mock data)",
            PrintTextSanitized = "SISTE KVITTERING\n(mock data)"
        )

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
            eventStore.publishEvent("OperationStarted", operationId, mapOf("type" to "software"))

            // Software download takes longer
            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection) * 2)

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "SW NEDLASTNING OK"
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

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
            eventStore.publishEvent("OperationStarted", operationId, mapOf("type" to "dataset"))

            // Dataset download takes longer
            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection) * 2)

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "DATASET NEDLASTNING OK"
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

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
            eventStore.publishEvent("OperationStarted", operationId,
                mapOf("type" to "admin-code", "code" to request.code))

            Thread.sleep(scenarioManager.getOperationDelay(scenarioSelection))

            val completedAt = Instant.now()
            val response = OperationResponse.adminSuccess(
                operationId, startedAt, completedAt, "ADMIN KMD OK (${request.code})"
            )

            eventStore.publishEvent("OperationCompleted", operationId, mapOf("success" to response.Success))

            log.info("Generic admin code completed: operationId={}, code={}", operationId, request.code)
            return ResponseEntity.ok(response)
        } finally {
            stateManager.endOperation(operationId)
        }
    }
}
