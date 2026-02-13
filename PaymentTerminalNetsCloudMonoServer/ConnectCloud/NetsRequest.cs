using System.Collections.Generic;
using Newtonsoft.Json;

namespace PaymentTerminalNetsCloudMonoServer.ConnectCloud
{
    public class NetsRequestRoot
    {
        [JsonProperty("NetsRequest")]
        public NetsRequest NetsRequest { get; set; }
    }

    public class NetsRequest
    {
        [JsonProperty("MessageHeader")]
        public object MessageHeader { get; set; }

        [JsonProperty("Open")]
        public object Open { get; set; }

        [JsonProperty("Dfs13TransferAmount")]
        public Dfs13TransferAmount Dfs13TransferAmount { get; set; }

        [JsonProperty("Dfs13Administration")]
        public Dfs13Administration Dfs13Administration { get; set; }

        [JsonProperty("Dfs13SendJson")]
        public Dfs13SendJson Dfs13SendJson { get; set; }

        [JsonProperty("Dfs13SendTld")]
        public Dfs13SendTld Dfs13SendTld { get; set; }
    }

    public class Dfs13TransferAmount
    {
        [JsonProperty("TransactionType")]
        public string TransactionType { get; set; }

        [JsonProperty("OperId")]
        public string OperId { get; set; }

        [JsonProperty("Amount1")]
        public string Amount1 { get; set; }

        [JsonProperty("Amount2")]
        public string Amount2 { get; set; }

        [JsonProperty("Amount3")]
        public string Amount3 { get; set; }

        [JsonProperty("Type2")]
        public string Type2 { get; set; }

        [JsonProperty("Type3")]
        public string Type3 { get; set; }

        [JsonProperty("HostData")]
        public string HostData { get; set; }

        [JsonProperty("OptionalData")]
        public string OptionalData { get; set; }
    }

    public class Dfs13Administration
    {
        [JsonProperty("OperId")]
        public string OperId { get; set; }

        [JsonProperty("AdmCode")]
        public string AdmCode { get; set; }

        [JsonProperty("OptionalData")]
        public string OptionalData { get; set; }
    }

    public class Dfs13SendJson
    {
        [JsonProperty("Data")]
        public object Data { get; set; }
    }

    public class Dfs13SendTld
    {
        [JsonProperty("TldType")]
        public string TldType { get; set; }

        [JsonProperty("Data")]
        public string Data { get; set; }
    }
}
