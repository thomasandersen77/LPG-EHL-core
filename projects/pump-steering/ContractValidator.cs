namespace pump_steering;

public record ValidationResult(bool IsValid, string? ErrorCode = null, string? Message = null)
{
    public static ValidationResult Valid() => new(true);
    public static ValidationResult Invalid(string errorCode, string message) => new(false, errorCode, message);
}

public static class ContractValidator
{
    /// <summary>
    /// Validate address parameter (must be 1-255)
    /// </summary>
    public static ValidationResult ValidateAddress(int? address)
    {
        if (!address.HasValue)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Address is required");
        }

        if (address.Value < 1 || address.Value > 255)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Address must be between 1 and 255, got {address.Value}");
        }

        return ValidationResult.Valid();
    }

    /// <summary>
    /// Validate price parameter (must be positive and within range)
    /// </summary>
    public static ValidationResult ValidatePrice(decimal? price)
    {
        if (!price.HasValue)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Price is required");
        }

        if (price.Value < 0)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Price must be positive, got {price.Value}");
        }

        if (price.Value > 99.99m)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Price must be <= 99.99, got {price.Value}");
        }

        return ValidationResult.Valid();
    }

    /// <summary>
    /// Validate correlation ID format
    /// </summary>
    public static ValidationResult ValidateCid(string? cid)
    {
        if (string.IsNullOrWhiteSpace(cid))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Correlation ID (cid) is required");
        }

        if (!Guid.TryParse(cid, out _))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Correlation ID (cid) must be a valid UUID, got '{cid}'");
        }

        return ValidationResult.Valid();
    }

    /// <summary>
    /// Validate side parameter
    /// </summary>
    public static ValidationResult ValidateSide(string? side)
    {
        if (string.IsNullOrWhiteSpace(side))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Side is required");
        }

        var validSides = new[] { "AZURE_BACKEND", "STATION_OWNER_UI", "ADMIN_UI", "TEST", "LEGACY" };
        if (!validSides.Contains(side))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Side must be one of: {string.Join(", ", validSides)}, got '{side}'");
        }

        return ValidationResult.Valid();
    }

    /// <summary>
    /// Validate actor parameter
    /// </summary>
    public static ValidationResult ValidateActor(Actor? actor)
    {
        if (actor == null)
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Actor is required");
        }

        if (string.IsNullOrWhiteSpace(actor.Type))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Actor.Type is required");
        }

        var validTypes = new[] { "SYSTEM", "USER", "SERVICE" };
        if (!validTypes.Contains(actor.Type))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                $"Actor.Type must be one of: {string.Join(", ", validTypes)}, got '{actor.Type}'");
        }

        if (string.IsNullOrWhiteSpace(actor.Id))
        {
            return ValidationResult.Invalid(
                ErrorCodes.ValidationError,
                "Actor.Id is required");
        }

        return ValidationResult.Valid();
    }
}
