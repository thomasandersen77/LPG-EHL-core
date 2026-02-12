using System;
using System.Collections.Generic;

namespace PaymentTerminalNetsCloudMonoServer.Models
{
    public class CapturedReport
    {
        public int CallResult;
        public int LocalModeResult;
        public bool TimedOut;
        public string Error;
        public string RawPrintText;
        public string SanitizedPrintText;
        public string LastDisplayText;
        public string LocalModeResultData;
        public string LocalModeResponseCode;
        public string LocalModeRejectionSource;
        public string LocalModeRejectionReason;
        public Dictionary<string, string> LocalModeFields;
        public string LastJsonReceived;
        public string StdRsp;
        public int MethodRejectCode;
        public string MethodRejectInfo;
        public string ResultEventName;
    }

    public class CapturedLocalMode
    {
        public int CallResult;
        public int LocalModeResult;
        public bool TimedOut;
        public string Error;
        public string RawPrintText;
        public string LastDisplayText;
        public string LocalModeResultData;
        public string LocalModeResponseCode;
        public string LocalModeRejectionSource;
        public string LocalModeRejectionReason;
        public Dictionary<string, string> LocalModeFields;
        public string LastJsonReceived;
        public string StdRsp;
        public int MethodRejectCode;
        public string MethodRejectInfo;
        public string ResultEventName;
    }
}
