using System.Text.Json;
using pump_steering;

namespace pump_steering.Tests;

public class MethodResponseTests
{
    [Fact]
    public void MethodResponse_AlwaysEchoesCid()
    {
        // Arrange
        var expectedCid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";
        var response = new MethodResponseDto(
            Cid: expectedCid,
            Ok: true,
            Message: "Success"
        );

        // Act
        var json = JsonSerializer.Serialize(response);
        var deserialized = JsonSerializer.Deserialize<MethodResponseDto>(json);

        // Assert
        Assert.NotNull(deserialized);
        Assert.Equal(expectedCid, deserialized.Cid);
    }

    [Fact]
    public void MethodResponse_WithSuccess_HasOkTrue()
    {
        // Arrange
        var response = new MethodResponseDto(
            Cid: Guid.NewGuid().ToString(),
            Ok: true,
            Message: "Operation successful"
        );

        // Assert
        Assert.True(response.Ok);
        Assert.Null(response.ErrorCode);
    }

    [Fact]
    public void MethodResponse_WithError_HasOkFalseAndErrorCode()
    {
        // Arrange
        var response = new MethodResponseDto(
            Cid: Guid.NewGuid().ToString(),
            Ok: false,
            ErrorCode: ErrorCodes.ValidationError,
            Message: "Validation failed"
        );

        // Assert
        Assert.False(response.Ok);
        Assert.Equal(ErrorCodes.ValidationError, response.ErrorCode);
        Assert.NotNull(response.Message);
    }

    [Fact]
    public void MethodResponse_Serialization_PreservesAllFields()
    {
        // Arrange
        var cid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";
        var response = new MethodResponseDto(
            Cid: cid,
            Ok: true,
            Message: "Price updated",
            Data: new { new_price = 15.90m }
        );

        // Act
        var json = JsonSerializer.Serialize(response);
        var deserialized = JsonSerializer.Deserialize<MethodResponseDto>(json);

        // Assert
        Assert.NotNull(deserialized);
        Assert.Equal(cid, deserialized.Cid);
        Assert.True(deserialized.Ok);
        Assert.Equal("Price updated", deserialized.Message);
        Assert.NotNull(deserialized.Data);
    }
}
