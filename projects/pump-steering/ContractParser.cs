using System.Text.Json;

namespace pump_steering;

public static class ContractParser
{
    /// <summary>
    /// Parse method request with support for both envelope and legacy payload formats
    /// </summary>
    public static (MethodRequestEnvelope<TPayload> Envelope, bool IsLegacy) ParseMethodRequest<TPayload>(
        string jsonData) where TPayload : class
    {
        if (string.IsNullOrWhiteSpace(jsonData))
        {
            throw new ArgumentException("JSON data cannot be null or empty", nameof(jsonData));
        }

        try
        {
            using var document = JsonDocument.Parse(jsonData);
            var root = document.RootElement;

            // Try envelope format first (has cid, side, actor, payload)
            if (root.TryGetProperty("cid", out _) &&
                root.TryGetProperty("side", out _) &&
                root.TryGetProperty("actor", out _) &&
                root.TryGetProperty("payload", out _))
            {
                var envelope = JsonSerializer.Deserialize<MethodRequestEnvelope<TPayload>>(jsonData);
                if (envelope == null)
                {
                    throw new JsonException("Failed to deserialize envelope");
                }

                return (envelope, IsLegacy: false);
            }

            // Fallback: legacy payload format
            var payload = JsonSerializer.Deserialize<TPayload>(jsonData);
            if (payload == null)
            {
                throw new JsonException("Failed to deserialize legacy payload");
            }

            var legacyEnvelope = new MethodRequestEnvelope<TPayload>(
                Cid: Guid.NewGuid().ToString(),
                Side: LegacyDefaults.Side,
                Actor: LegacyDefaults.CreateLegacyActor(),
                Payload: payload
            );

            return (legacyEnvelope, IsLegacy: true);
        }
        catch (JsonException ex)
        {
            throw new ArgumentException($"Invalid JSON format: {ex.Message}", nameof(jsonData), ex);
        }
    }

    /// <summary>
    /// Validate and normalize correlation ID (UUID)
    /// </summary>
    public static (string Cid, bool WasGenerated) ValidateOrGenerateCid(string? cid)
    {
        if (string.IsNullOrWhiteSpace(cid))
        {
            return (Guid.NewGuid().ToString(), WasGenerated: true);
        }

        if (Guid.TryParse(cid, out _))
        {
            return (cid, WasGenerated: false);
        }

        // Invalid UUID format, generate new one
        return (Guid.NewGuid().ToString(), WasGenerated: true);
    }
}
