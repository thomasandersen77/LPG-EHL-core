namespace PaymentTerminalNetsCloudMonoServer.Models
{
    public class PurchaseRequest
    {
        public int AmountMinor { get; set; }
        public string Currency { get; set; } = "NOK";
        public string OperatorId { get; set; } = "4321";
        public string OptionalData { get; set; }
        public PreAvstemmingConfig PreAvstemming { get; set; }
        public string ClientRequestId { get; set; }
    }

    public class RefundRequest
    {
        public int AmountMinor { get; set; }
        public string Currency { get; set; } = "NOK";
        public string OperatorId { get; set; } = "4321";
        public string OptionalData { get; set; }
        public PreAvstemmingConfig PreAvstemming { get; set; }
        public string ClientRequestId { get; set; }
    }

    public class CashbackRequest
    {
        public int PurchaseMinor { get; set; }
        public int CashbackMinor { get; set; }
        public string Currency { get; set; } = "NOK";
        public string OperatorId { get; set; } = "4321";
        public string OptionalData { get; set; }
        public string ClientRequestId { get; set; }
    }

    public class AdminRequest
    {
        public string Password { get; set; } = "0000";
    }

    public class AdminCodeRequest
    {
        public int? Code { get; set; }
        public string Password { get; set; } = "0000";
    }

    public class PreAvstemmingConfig
    {
        public bool Enabled { get; set; }
        public string Password { get; set; } = "0000";
        public int? TimeoutSeconds { get; set; }
    }
}
