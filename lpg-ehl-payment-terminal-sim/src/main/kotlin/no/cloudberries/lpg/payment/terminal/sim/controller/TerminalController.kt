package no.cloudberries.lpg.payment.terminal.sim.controller

import no.cloudberries.lpg.payment.terminal.sim.model.domain.TerminalState
import no.cloudberries.lpg.payment.terminal.sim.model.response.SimpleResponse
import no.cloudberries.lpg.payment.terminal.sim.model.response.TerminalStatusResponse
import no.cloudberries.lpg.payment.terminal.sim.service.TerminalStateManager
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Terminal lifecycle management endpoints.
 */
@RestController
@RequestMapping("/v1/terminal")
class TerminalController(
    private val stateManager: TerminalStateManager
) {
    private val log = LoggerFactory.getLogger(TerminalController::class.java)

    /**
     * GET /v1/terminal/status
     *
     * Get terminal status and readiness.
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<TerminalStatusResponse> {
        val state = stateManager.getState()
        val response = TerminalStatusResponse(
            VendorDllLoadable = true,
            TerminalOpen = stateManager.isOpen(),
            TerminalReady = stateManager.isReady(),
            ConnectionState = stateManager.getConnectionState(),
            LastError = null,
            TerminalIdentity = if (stateManager.isOpen()) {
                stateManager.getTerminalIdentity()
            } else {
                null
            }
        )
        log.debug("Terminal status: {}", state)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /v1/terminal/open
     *
     * Open and initialize terminal.
     */
    @PostMapping("/open")
    fun openTerminal(): ResponseEntity<SimpleResponse> {
        return try {
            stateManager.open()
            log.info("Terminal opened successfully")
            ResponseEntity.ok(
                SimpleResponse(
                    Success = true,
                    Message = "Terminal opened and ready"
                )
            )
        } catch (ex: Exception) {
            log.error("Failed to open terminal", ex)
            ResponseEntity.status(500).body(
                SimpleResponse(
                    Success = false,
                    Error = ex.message ?: "Failed to open terminal"
                )
            )
        }
    }

    /**
     * POST /v1/terminal/close
     *
     * Close terminal connection.
     */
    @PostMapping("/close")
    fun closeTerminal(): ResponseEntity<SimpleResponse> {
        return try {
            stateManager.close()
            log.info("Terminal closed successfully")
            ResponseEntity.ok(
                SimpleResponse(
                    Success = true,
                    Message = "Terminal closed"
                )
            )
        } catch (ex: Exception) {
            log.error("Failed to close terminal", ex)
            ResponseEntity.status(500).body(
                SimpleResponse(
                    Success = false,
                    Error = ex.message ?: "Failed to close terminal"
                )
            )
        }
    }
}

