using pump_steering;

namespace pump_steering.Tests;

public class ContractValidatorTests
{
    [Fact]
    public void ValidateAddress_WhenMissing_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateAddress(null);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("required", result.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void ValidateAddress_WhenTooLow_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateAddress(0);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("between 1 and 255", result.Message);
    }

    [Fact]
    public void ValidateAddress_WhenTooHigh_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateAddress(256);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("between 1 and 255", result.Message);
    }

    [Fact]
    public void ValidateAddress_WhenValid_ReturnsSuccess()
    {
        // Act
        var result = ContractValidator.ValidateAddress(33);

        // Assert
        Assert.True(result.IsValid);
        Assert.Null(result.ErrorCode);
        Assert.Null(result.Message);
    }

    [Theory]
    [InlineData(1)]
    [InlineData(33)]
    [InlineData(255)]
    public void ValidateAddress_WhenInRange_ReturnsSuccess(int address)
    {
        // Act
        var result = ContractValidator.ValidateAddress(address);

        // Assert
        Assert.True(result.IsValid);
    }

    [Fact]
    public void SetPrice_RejectsNegativePrice()
    {
        // Act
        var result = ContractValidator.ValidatePrice(-1.50m);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("positive", result.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void ValidatePrice_WhenNull_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidatePrice(null);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("required", result.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void ValidatePrice_WhenTooHigh_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidatePrice(100.00m);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }

    [Theory]
    [InlineData(0.01)]
    [InlineData(15.90)]
    [InlineData(99.99)]
    public void ValidatePrice_WhenValid_ReturnsSuccess(decimal price)
    {
        // Act
        var result = ContractValidator.ValidatePrice(price);

        // Assert
        Assert.True(result.IsValid);
    }

    [Fact]
    public void ValidateCid_WhenValid_ReturnsSuccess()
    {
        // Arrange
        var validCid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";

        // Act
        var result = ContractValidator.ValidateCid(validCid);

        // Assert
        Assert.True(result.IsValid);
    }

    [Fact]
    public void ValidateCid_WhenNull_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateCid(null);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }

    [Fact]
    public void ValidateCid_WhenInvalidFormat_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateCid("not-a-uuid");

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
        Assert.Contains("UUID", result.Message);
    }

    [Fact]
    public void ValidateSide_WhenValid_ReturnsSuccess()
    {
        // Act
        var result = ContractValidator.ValidateSide("AZURE_BACKEND");

        // Assert
        Assert.True(result.IsValid);
    }

    [Fact]
    public void ValidateSide_WhenInvalid_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateSide("INVALID_SIDE");

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }

    [Fact]
    public void ValidateActor_WhenValid_ReturnsSuccess()
    {
        // Arrange
        var actor = new Actor("USER", "user123", "Test User");

        // Act
        var result = ContractValidator.ValidateActor(actor);

        // Assert
        Assert.True(result.IsValid);
    }

    [Fact]
    public void ValidateActor_WhenNull_ReturnsValidationError()
    {
        // Act
        var result = ContractValidator.ValidateActor(null);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }

    [Fact]
    public void ValidateActor_WhenTypeInvalid_ReturnsValidationError()
    {
        // Arrange
        var actor = new Actor("INVALID_TYPE", "user123");

        // Act
        var result = ContractValidator.ValidateActor(actor);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }

    [Fact]
    public void ValidateActor_WhenIdMissing_ReturnsValidationError()
    {
        // Arrange
        var actor = new Actor("USER", "");

        // Act
        var result = ContractValidator.ValidateActor(actor);

        // Assert
        Assert.False(result.IsValid);
        Assert.Equal(ErrorCodes.ValidationError, result.ErrorCode);
    }
}
