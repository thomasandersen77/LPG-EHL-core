using System;

namespace PaymentTerminalNetsCloudMonoServer.Models
{
    public class ErrorResponse
    {
        public string Error { get; set; }
        public string ErrorCode { get; set; }
        public string OperationId { get; set; }
        public string Details { get; set; }
    }

    public static class ErrorCodes
    {
        public const string TERMINAL_BUSY = "terminal_busy";
        public const string TERMINAL_NOT_READY = "terminal_not_ready";
        public const string OPERATION_TIMEOUT = "operation_timeout";
        public const string VENDOR_CALL_FAILURE = "vendor_call_failure";
        public const string OPERATION_REJECTED = "operation_rejected";
        public const string INVALID_REQUEST = "invalid_request";
        public const string DIAGNOSTICS_DISABLED = "diagnostics_disabled";
    }
}
