using System;
using System.Collections.Generic;
using System.Linq;

namespace PaymentTerminalNetsCloudMonoServer.Persistence
{
    public class EventStore
    {
        private readonly List<EventRecord> _events = new List<EventRecord>();
        private readonly object _lock = new object();
        private long _seq;

        public void AddEvent(string operationId, string eventType, object payload)
        {
            lock (_lock)
            {
                _seq++;
                var record = new EventRecord
                {
                    Seq = _seq,
                    EventId = Guid.NewGuid().ToString("N"),
                    OperationId = operationId,
                    Timestamp = DateTime.UtcNow,
                    EventType = eventType,
                    Payload = payload
                };
                _events.Add(record);
            }
        }

        public List<EventEnvelope> GetEvents(string since)
        {
            lock (_lock)
            {
                var events = _events.AsEnumerable();
                if (!string.IsNullOrWhiteSpace(since))
                {
                    if (long.TryParse(since.Trim(), out var sinceSeq))
                        events = events.Where(e => e.Seq > sinceSeq);
                    else if (DateTime.TryParse(since, null, System.Globalization.DateTimeStyles.RoundtripKind, out var sinceTime))
                        events = events.Where(e => e.Timestamp > sinceTime);
                }

                return events.OrderBy(e => e.Timestamp)
                    .Select(e => new EventEnvelope
                    {
                        Cursor = e.Seq,
                        EventId = e.EventId,
                        OperationId = e.OperationId,
                        Timestamp = e.Timestamp.ToString("O"),
                        EventType = e.EventType,
                        Payload = e.Payload
                    })
                    .ToList();
            }
        }

        public sealed class EventEnvelope
        {
            public long Cursor { get; set; }
            public string EventId { get; set; }
            public string OperationId { get; set; }
            public string Timestamp { get; set; }
            public string EventType { get; set; }
            public object Payload { get; set; }
        }

        private class EventRecord
        {
            public long Seq { get; set; }
            public string EventId { get; set; }
            public string OperationId { get; set; }
            public DateTime Timestamp { get; set; }
            public string EventType { get; set; }
            public object Payload { get; set; }
        }
    }
}
