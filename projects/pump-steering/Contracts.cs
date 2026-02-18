using System.Text.Json.Serialization;

namespace pump_steering;

// ===== REQUEST ENVELOPE (Cloud-to-Device) =====

public record Actor(
    [property: JsonPropertyName("type")] string Type,
    [property: JsonPropertyName("id")] string Id,
    [property: JsonPropertyName("name")] string? Name = null
);

public record MethodRequestEnvelope<TPayload>(
    [property: JsonPropertyName("cid")] string Cid,
    [property: JsonPropertyName("side")] string Side,
    [property: JsonPropertyName("actor")] Actor Actor,
    [property: JsonPropertyName("payload")] TPayload Payload
);

// ===== PAYLOAD TYPES =====

public record SetPricePayload(
    [property: JsonPropertyName("price")] decimal Price
);

public record EmptyPayload;

// ===== METHOD RESPONSE (Device-to-Cloud) =====

public record MethodResponseDto(
    [property: JsonPropertyName("cid")] string Cid,
    [property: JsonPropertyName("ok")] bool Ok,
    [property: JsonPropertyName("errorCode")] string? ErrorCode = null,
    [property: JsonPropertyName("message")] string? Message = null,
    [property: JsonPropertyName("data")] object? Data = null
)
{
    public static MethodResponseDto Success(string cid, string? message = null, object? data = null) =>
        new(Cid: cid, Ok: true, ErrorCode: null, Message: message, Data: data);

    public static MethodResponseDto Error(string cid, string? errorCode = null, string? message = null) =>
        new(Cid: cid, Ok: false, ErrorCode: errorCode, Message: message, Data: null);
}

// ===== TELEMETRY ENVELOPE (Device-to-Cloud) =====

public record TelemetryEnvelope(
    [property: JsonPropertyName("cid")] string Cid,
    [property: JsonPropertyName("side")] string Side,
    [property: JsonPropertyName("actor")] Actor Actor,
    [property: JsonPropertyName("method")] string? Method,
    [property: JsonPropertyName("address")] int Address,
    [property: JsonPropertyName("timestamp")] DateTime Timestamp,
    [property: JsonPropertyName("command")] int? Command = null,
    [property: JsonPropertyName("data")] string? Data = null,
    [property: JsonPropertyName("eventType")] string EventType = "EHL_RESPONSE",
    [property: JsonPropertyName("price")] decimal? Price = null,
    [property: JsonPropertyName("volume")] decimal? Volume = null,
    [property: JsonPropertyName("is_locked")] bool? IsLocked = null,
    [property: JsonPropertyName("is_open_for_delivery")] bool? IsOpenForDelivery = null
);

// ===== ERROR CODES =====

public static class ErrorCodes
{
    public const string ValidationError = "VALIDATION_ERROR";
    public const string EhlTimeout = "EHL_TIMEOUT";
    public const string EhlIo = "EHL_IO";
    public const string Unknown = "UNKNOWN";
    public const string SerialCommandFailed = "SERIAL_COMMAND_FAILED";
}

// ===== LEGACY SUPPORT =====

public static class LegacyDefaults
{
    public const string Side = "LEGACY";
    public const string ActorType = "SYSTEM";
    public const string ActorId = "legacy";

    public static Actor CreateLegacyActor() => new(ActorType, ActorId);
}
