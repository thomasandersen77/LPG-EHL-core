package no.cloudberries.lpg.payment.terminal.sim.model.response

import java.time.Instant

/**
 * Operation response (PascalCase).
 *
 * Used by all financial and admin operations.
 *
 * Success logic:
 * - Financial: Success = (CallResult==1 && !TimedOut && (LocalModeResult==0 || ResponseCode=="00"))
 * - Admin: Success = (CallResult==1 && !TimedOut)
 */
data class OperationResponse(
    val Success: Boolean,
    val OperationId: String?,
    val StartedAt: String,
    val CompletedAt: String? = null,
    val DurationMs: Long? = null,

    // Vendor call result
    val CallResult: Int,
    val MethodRejectCode: Int = 0,
    val MethodRejectInfo: String? = null,
    val ResultEventName: String? = null,

    // Terminal outcome
    val LocalModeResult: Int? = null,
    val ResponseCode: String? = null,
    val EntryMode: String? = null,
    val EntryModeCode: String? = null,
    val LocalModeResultData: String? = null,
    val RejectionSource: String? = null,
    val RejectionReason: String? = null,

    // Terminal identification
    val LocalModeFields: Map<String, String>? = null,

    // Evidence
    val PrintTextRaw: String? = null,
    val PrintTextSanitized: String? = null,
    val LastDisplayText: String? = null,

    // Error details
    val Error: String? = null,
    val ErrorCode: String? = null,

    // Persistence references
    val DbRowId: Long? = null,
    val ReceiptFileId: String? = null,

    // Report-specific fields
    val ReportFields: Map<String, String>? = null
) {
    companion object {
        /**
         * Create an error response with default C#-style values.
         */
        fun error(
            errorCode: String,
            error: String,
            startedAt: String = "0001-01-01T00:00:00"
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = null,
                StartedAt = startedAt,
                CallResult = 0,
                LocalModeResult = 0,
                Error = error,
                ErrorCode = errorCode
            )
        }

        /**
         * Create an approved purchase response.
         */
        fun approved(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            amountMinor: Int,
            terminalId: String,
            merchantId: String,
            receiptText: String
        ): OperationResponse {
            return OperationResponse(
                Success = true,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                MethodRejectCode = 0,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 0,
                ResponseCode = "00",
                EntryMode = "CONTACTLESS",
                EntryModeCode = "2",
                LocalModeResultData = "D  ;************8408;2;more_data",
                RejectionSource = "0",
                LocalModeFields = mapOf(
                    "terminalID" to terminalId,
                    "merchantId" to merchantId,
                    "totalAmount" to amountMinor.toString()
                ),
                PrintTextRaw = receiptText,
                PrintTextSanitized = receiptText,
                LastDisplayText = "GODKJENT",
                ReceiptFileId = "$operationId.txt"
            )
        }

        /**
         * Create a wrong PIN response (Z1).
         */
        fun wrongPin(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            receiptText: String? = null
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 2,
                ResponseCode = "Z1",
                RejectionSource = "3",
                RejectionReason = "3:2:Z1",
                PrintTextRaw = receiptText,
                PrintTextSanitized = receiptText,
                LastDisplayText = "AVVIST"
            )
        }

        /**
         * Create a user cancel response.
         */
        fun userCancel(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 2,
                ResponseCode = "",
                RejectionSource = "0",
                RejectionReason = "2:1",
                LastDisplayText = "AVBRUTT"
            )
        }

        /**
         * Create a declined response.
         */
        fun declined(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            receiptText: String? = null
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 2,
                ResponseCode = "05",
                RejectionSource = "1",
                RejectionReason = "1:2:05",
                PrintTextRaw = receiptText,
                PrintTextSanitized = receiptText,
                LastDisplayText = "AVVIST"
            )
        }

        /**
         * Create operation timeout response (no card presented).
         * Aligns with real terminal: success=false, errorCode=operation_timeout, LastDisplayText="Kortet ikke presentert".
         */
        fun timeout(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            durationMs: Long,
            receiptText: String? = null
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = durationMs,
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 2,
                RejectionReason = "4:6",
                PrintTextRaw = receiptText,
                PrintTextSanitized = receiptText,
                LastDisplayText = "Kortet ikke presentert",
                Error = "Operation timed out waiting for card",
                ErrorCode = "operation_timeout"
            )
        }

        /**
         * Create an admin operation success response.
         */
        fun adminSuccess(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            displayText: String = "OK",
            printTextRaw: String? = null,
            reportFields: Map<String, String>? = null
        ): OperationResponse {
            return OperationResponse(
                Success = true,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 1,
                LastDisplayText = displayText,
                PrintTextRaw = printTextRaw,
                PrintTextSanitized = printTextRaw,
                ReportFields = reportFields
            )
        }

        /**
         * Admin operation that returns success=false but completed (e.g. reversal with no txn).
         */
        fun adminFormatError(
            operationId: String,
            startedAt: Instant,
            completedAt: Instant,
            displayText: String = "Formatfeil",
            rejectionReason: String = "4:6"
        ): OperationResponse {
            return OperationResponse(
                Success = false,
                OperationId = operationId,
                StartedAt = startedAt.toString(),
                CompletedAt = completedAt.toString(),
                DurationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli(),
                CallResult = 1,
                ResultEventName = "OnLocalMode",
                LocalModeResult = 2,
                LastDisplayText = displayText,
                RejectionReason = rejectionReason
            )
        }
    }
}
