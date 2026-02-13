using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace PaymentTerminalNetsCloudMonoServer.ConnectCloud
{
    public class NetsResponseRoot
    {
        [JsonProperty("NetsResponse")]
        public JObject NetsResponse { get; set; }
    }

    public static class NetsResponseParser
    {
        public static string GetTerminalId(JObject netsResponse)
        {
            var header = netsResponse["MessageHeader"]?["$"];
            return header?["TerminalID"]?.ToString();
        }

        public static bool HasDfs13TerminalReady(JObject netsResponse)
        {
            return netsResponse["Dfs13TerminalReady"] != null;
        }

        public static bool HasMethodRejected(JObject netsResponse)
        {
            return netsResponse["MethodRejected"] != null;
        }

        public static int GetMethodRejectedCode(JObject netsResponse)
        {
            var code = netsResponse["MethodRejected"]?["Code"]?.ToString();
            return int.TryParse(code, out var c) ? c : 0;
        }

        public static bool HasDfs13DisplayText(JObject netsResponse)
        {
            return netsResponse["Dfs13DisplayText"] != null;
        }

        public static (string textId, string text) GetDfs13DisplayText(JObject netsResponse)
        {
            var obj = netsResponse["Dfs13DisplayText"];
            if (obj == null) return (null, null);
            var attrs = obj["$"];
            var textId = attrs?["TextID"]?.ToString();
            var text = obj["_"]?.ToString();
            return (textId, text);
        }

        public static bool HasDfs13PrintText(JObject netsResponse)
        {
            return netsResponse["Dfs13PrintText"] != null;
        }

        public static string GetDfs13PrintText(JObject netsResponse)
        {
            return netsResponse["Dfs13PrintText"]?["Text"]?.ToString();
        }

        public static bool HasDfs13LocalMode(JObject netsResponse)
        {
            return netsResponse["Dfs13LocalMode"] != null;
        }

        public static Dfs13LocalModeData GetDfs13LocalMode(JObject netsResponse)
        {
            var obj = netsResponse["Dfs13LocalMode"];
            if (obj == null) return null;
            return new Dfs13LocalModeData
            {
                Result = obj["Result"]?.ToString(),
                ResultData = obj["ResultData"]?.ToString(),
                RejectionReason = obj["RejectionReason"]?.ToString(),
                CVM = obj["CVM"]?.ToString(),
                TotalAmount = obj["TotalAmount"]?.ToString(),
                TimeStamp = obj["TimeStamp"]?.ToString()
            };
        }

        public static bool HasDfs13LastFinancialResult(JObject netsResponse)
        {
            return netsResponse["Dfs13LastFinancialResult"] != null;
        }

        public static Dfs13LocalModeData GetDfs13LastFinancialResult(JObject netsResponse)
        {
            var obj = netsResponse["Dfs13LastFinancialResult"];
            if (obj == null) return null;
            return new Dfs13LocalModeData
            {
                Result = obj["Result"]?.ToString(),
                ResultData = obj["ResultData"]?.ToString(),
                RejectionReason = obj["RejectionReason"]?.ToString(),
                CVM = obj["CVM"]?.ToString(),
                TotalAmount = obj["TotalAmount"]?.ToString(),
                TimeStamp = obj["TimeStamp"]?.ToString()
            };
        }

        public static bool HasDfs13Error(JObject netsResponse)
        {
            return netsResponse["Dfs13Error"] != null;
        }

        public static int GetDfs13ErrorCode(JObject netsResponse)
        {
            var code = netsResponse["Dfs13Error"]?["ErrorCode"]?.ToString();
            return int.TryParse(code, out var c) ? c : 0;
        }

        public static bool HasDfs13TldReceived(JObject netsResponse)
        {
            return netsResponse["Dfs13TldReceived"] != null;
        }

        public static bool HasDfs13JsonReceived(JObject netsResponse)
        {
            return netsResponse["Dfs13JsonReceived"] != null;
        }
    }

    public class Dfs13LocalModeData
    {
        public string Result { get; set; }
        public string ResultData { get; set; }
        public string RejectionReason { get; set; }
        public string CVM { get; set; }
        public string TotalAmount { get; set; }
        public string TimeStamp { get; set; }
    }
}
