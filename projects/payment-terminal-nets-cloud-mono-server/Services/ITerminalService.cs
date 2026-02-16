using System.Collections.Generic;
using PaymentTerminalNetsCloudMonoServer.Models;
using PaymentTerminalNetsCloudMonoServer.Persistence;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public interface ITerminalService
    {
        TerminalStatusResponse GetStatus();
        SimpleResponse Open();
        SimpleResponse Close();

        OperationResponse Purchase(PurchaseRequest request);
        OperationResponse Refund(RefundRequest request);
        OperationResponse Cashback(CashbackRequest request);

        OperationResponse RunAdmin(int adminCode, string password);

        List<EventStore.EventEnvelope> GetEvents(string since);

        object GetConnectCloudSchema();
        int SendJson(string json);
        int SendTld(string tldType, byte[] tldData);
    }
}
