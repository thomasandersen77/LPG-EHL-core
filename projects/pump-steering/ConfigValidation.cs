namespace pump_steering;

/// <summary>
/// Validates IoT Hub device connection string format before use.
/// </summary>
public static class ConfigValidation
{
    public const string ConnectionStringEnvVar = "IOT_DEVICE_CONNECTION_STRING";

    /// <summary>
    /// Validates that the connection string contains required segments per IoT Hub device connection string format.
    /// </summary>
    /// <param name="connectionString">The connection string to validate.</param>
    /// <param name="errorMessage">When validation fails, a clear error message.</param>
    /// <returns>True if valid; false otherwise.</returns>
    public static bool TryValidateConnectionString(string? connectionString, out string? errorMessage)
    {
        errorMessage = null;
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            errorMessage = "Connection string is null or empty.";
            return false;
        }

        var s = connectionString.AsSpan().Trim();
        if (!ContainsSegment(s, "HostName="))
        {
            errorMessage = "Connection string must contain 'HostName='.";
            return false;
        }
        if (!ContainsSegment(s, "DeviceId="))
        {
            errorMessage = "Connection string must contain 'DeviceId='.";
            return false;
        }
        if (!ContainsSegment(s, "SharedAccessKey=") && !ContainsSegment(s, "SharedAccessKeyName="))
        {
            errorMessage = "Connection string must contain 'SharedAccessKey=' or 'SharedAccessKeyName='.";
            return false;
        }

        return true;
    }

    private static bool ContainsSegment(ReadOnlySpan<char> value, string segment)
    {
        return value.IndexOf(segment.AsSpan(), StringComparison.OrdinalIgnoreCase) >= 0;
    }
}
