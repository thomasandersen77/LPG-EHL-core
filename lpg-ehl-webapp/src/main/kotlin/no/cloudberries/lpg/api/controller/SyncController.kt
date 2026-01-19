package no.cloudberries.lpg.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.cloudberries.lpg.service.dto.AzureQueueByDateResponse
import no.cloudberries.lpg.service.dto.AzureQueueMessageDto
import no.cloudberries.lpg.service.dto.SyncStatusResponse
import no.cloudberries.lpg.service.azure.AzureQueueReaderService
import no.cloudberries.lpg.service.azure.AzureSyncService
import java.time.format.DateTimeFormatter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sync", description = "Azure sync management endpoints")
// @SecurityRequirement(name = "bearer-token") // Disabled for local demo testing
@ConditionalOnProperty(name = ["azure.enabled"], havingValue = "true")
class SyncController(
    private val azureSyncService: AzureSyncService,
    private val azureQueueReaderService: AzureQueueReaderService
) {

    @GetMapping("/status")
    @Operation(
        summary = "Get sync status",
        description = "Get statistics about Azure sync queue status"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getSyncStatus(): ResponseEntity<SyncStatusResponse> {
        val status = azureSyncService.getSyncStatus()
        return ResponseEntity.ok(status)
    }

    @PostMapping("/retry/{queueId}")
    @Operation(
        summary = "Retry sync for a specific item",
        description = "Manually trigger sync retry for a failed or pending item"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Retry triggered successfully"),
            ApiResponse(responseCode = "404", description = "Queue item not found"),
            ApiResponse(responseCode = "400", description = "Item already synced"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun retrySyncItem(
        @Parameter(description = "Queue item UUID")
        @PathVariable queueId: UUID
    ): ResponseEntity<Map<String, Any>> {
        val success = azureSyncService.retrySyncItem(queueId)
        
        return if (success) {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Sync retry triggered"))
        } else {
            ResponseEntity.badRequest()
                .body(mapOf("success" to false, "message" to "Item not found or already synced"))
        }
    }

    @PostMapping("/trigger")
    @Operation(
        summary = "Manually trigger sync job",
        description = "Force immediate execution of pending items sync (useful for testing)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Sync job triggered"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun triggerSync(): ResponseEntity<Map<String, String>> {
        azureSyncService.syncPendingItems()
        return ResponseEntity.ok(mapOf("message" to "Sync job triggered"))
    }

    @GetMapping("/queue/messages")
    @Operation(
        summary = "Get Azure Queue messages",
        description = "Peek at messages in Azure Storage Queue without removing them"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getQueueMessages(
        @Parameter(description = "Maximum number of messages to retrieve")
        @RequestParam(defaultValue = "32") maxMessages: Int
    ): ResponseEntity<List<AzureQueueMessageDto>> {
        val messages = azureQueueReaderService.peekMessages(maxMessages.coerceAtMost(1000))
        return ResponseEntity.ok(messages)
    }

    @GetMapping("/queue/by-date")
    @Operation(
        summary = "Get Azure Queue messages grouped by date",
        description = "Get all messages from Azure Queue grouped by insertion date"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successful operation"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getQueueMessagesByDate(): ResponseEntity<AzureQueueByDateResponse> {
        val messagesByDate = azureQueueReaderService.getMessagesByDate()
        
        // Convert LocalDate keys to String for JSON serialization
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val datesMap = messagesByDate.mapKeys { it.key.format(formatter) }
        val totalMessages = messagesByDate.values.sumOf { it.size }
        
        val response = AzureQueueByDateResponse(
            dates = datesMap,
            totalMessages = totalMessages
        )
        
        return ResponseEntity.ok(response)
    }
}
