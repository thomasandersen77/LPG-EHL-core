using System;
using System.Collections.Generic;

namespace PaymentTerminalNetsCloudMonoServer.Models
{
    public class OperationResponse
    {
        public bool Success { get; set; }
        public string OperationId { get; set; }
        public DateTime StartedAt { get; set; }
        public DateTime? CompletedAt { get; set; }
        public int? DurationMs { get; set; }
        public int CallResult { get; set; }
        public int MethodRejectCode { get; set; }
        public string MethodRejectInfo { get; set; }
        public string ResultEventName { get; set; }
        public int LocalModeResult { get; set; }
        public string ResponseCode { get; set; }
        public string RejectionSource { get; set; }
        public string RejectionReason { get; set; }
        public string LocalModeResultData { get; set; }
        public Dictionary<string, string> LocalModeFields { get; set; }
        public string PrintTextRaw { get; set; }
        public string PrintTextSanitized { get; set; }
        public string LastDisplayText { get; set; }
        public string EntryMode { get; set; }
        public string EntryModeCode { get; set; }
        public string Error { get; set; }
        public string ErrorCode { get; set; }
        public long? DbRowId { get; set; }
        public string ReceiptFileId { get; set; }
        public Dictionary<string, string> ReportFields { get; set; }
    }

    public class TerminalStatusResponse
    {
        public bool VendorDllLoadable { get; set; }
        public bool TerminalOpen { get; set; }
        public bool TerminalReady { get; set; }
        public string ConnectionState { get; set; }
        public string LastError { get; set; }
        public Dictionary<string, string> TerminalIdentity { get; set; }
    }

    public class SimpleResponse
    {
        public bool Success { get; set; }
        public string Message { get; set; }
        public string Error { get; set; }
    }
}
