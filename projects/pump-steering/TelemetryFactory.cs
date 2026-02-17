namespace pump_steering;

public static class TelemetryFactory
{
    /// <summary>
    /// Create telemetry envelope for EHL responses
    /// </summary>
    public static TelemetryEnvelope CreateEhlResponseTelemetry(
        string cid,
        string side,
        Actor actor,
        string method,
        int address,
        int? command = null,
        string? data = null,
        decimal? price = null,
        decimal? volume = null,
        bool? isLocked = null,
        bool? isOpenForDelivery = null)
    {
        return new TelemetryEnvelope(
            Cid: cid,
            Side: side,
            Actor: actor,
            Method: method,
            Address: address,
            Timestamp: DateTime.UtcNow,
            Command: command,
            Data: data,
            EventType: "EHL_RESPONSE",
            Price: price,
            Volume: volume,
            IsLocked: isLocked,
            IsOpenForDelivery: isOpenForDelivery
        );
    }

    /// <summary>
    /// Create telemetry envelope for error events
    /// </summary>
    public static TelemetryEnvelope CreateErrorTelemetry(
        string cid,
        string side,
        Actor actor,
        string method,
        int address,
        string errorMessage)
    {
        return new TelemetryEnvelope(
            Cid: cid,
            Side: side,
            Actor: actor,
            Method: method,
            Address: address,
            Timestamp: DateTime.UtcNow,
            Data: errorMessage,
            EventType: "ERROR"
        );
    }

    /// <summary>
    /// Create periodic state telemetry (for polling loop)
    /// </summary>
    public static TelemetryEnvelope CreateStateTelemetry(
        int address,
        decimal price,
        decimal volume,
        bool isLocked,
        bool isOpenForDelivery)
    {
        return new TelemetryEnvelope(
            Cid: Guid.NewGuid().ToString(),
            Side: "DEVICE",
            Actor: new Actor("SYSTEM", "pump-steering-service", "Pump Steering Service"),
            Method: "PeriodicState",
            Address: address,
            Timestamp: DateTime.UtcNow,
            EventType: "STATE",
            Price: price,
            Volume: volume,
            IsLocked: isLocked,
            IsOpenForDelivery: isOpenForDelivery
        );
    }
}
