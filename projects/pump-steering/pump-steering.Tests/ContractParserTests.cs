using System.Text.Json;
using pump_steering;

namespace pump_steering.Tests;

public class ContractParserTests
{
    [Fact]
    public void ParseEnvelope_WhenValid_ReturnsCidSideActorAndPayload()
    {
        // Arrange
        var json = @"{
            ""cid"": ""b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1"",
            ""side"": ""AZURE_BACKEND"",
            ""actor"": {
                ""type"": ""USER"",
                ""id"": ""user123"",
                ""name"": ""Test User""
            },
            ""payload"": {
                ""price"": 15.90
            }
        }";

        // Act
        var (envelope, isLegacy) = ContractParser.ParseMethodRequest<SetPricePayload>(json);

        // Assert
        Assert.False(isLegacy);
        Assert.Equal("b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1", envelope.Cid);
        Assert.Equal("AZURE_BACKEND", envelope.Side);
        Assert.Equal("USER", envelope.Actor.Type);
        Assert.Equal("user123", envelope.Actor.Id);
        Assert.Equal("Test User", envelope.Actor.Name);
        Assert.Equal(15.90m, envelope.Payload.Price);
    }

    [Fact]
    public void ParseLegacyPayload_WhenNoCid_GeneratesCidAndDefaults()
    {
        // Arrange
        var json = @"{""price"": 19.50}";

        // Act
        var (envelope, isLegacy) = ContractParser.ParseMethodRequest<SetPricePayload>(json);

        // Assert
        Assert.True(isLegacy);
        Assert.NotNull(envelope.Cid);
        Assert.True(Guid.TryParse(envelope.Cid, out _));
        Assert.Equal(LegacyDefaults.Side, envelope.Side);
        Assert.Equal(LegacyDefaults.ActorType, envelope.Actor.Type);
        Assert.Equal(LegacyDefaults.ActorId, envelope.Actor.Id);
        Assert.Equal(19.50m, envelope.Payload.Price);
    }

    [Fact]
    public void ParseMethodRequest_WhenEmptyJson_ThrowsArgumentException()
    {
        // Act & Assert
        Assert.Throws<ArgumentException>(() =>
            ContractParser.ParseMethodRequest<SetPricePayload>(""));
    }

    [Fact]
    public void ParseMethodRequest_WhenNullJson_ThrowsArgumentException()
    {
        // Act & Assert
        Assert.Throws<ArgumentException>(() =>
            ContractParser.ParseMethodRequest<SetPricePayload>(null!));
    }

    [Fact]
    public void ParseMethodRequest_WhenInvalidJson_ThrowsArgumentException()
    {
        // Arrange
        var invalidJson = "{invalid json}";

        // Act & Assert
        Assert.Throws<ArgumentException>(() =>
            ContractParser.ParseMethodRequest<SetPricePayload>(invalidJson));
    }

    [Fact]
    public void ValidateOrGenerateCid_WhenValidUuid_ReturnsOriginal()
    {
        // Arrange
        var validCid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";

        // Act
        var (cid, wasGenerated) = ContractParser.ValidateOrGenerateCid(validCid);

        // Assert
        Assert.Equal(validCid, cid);
        Assert.False(wasGenerated);
    }

    [Fact]
    public void ValidateOrGenerateCid_WhenNull_GeneratesNewCid()
    {
        // Act
        var (cid, wasGenerated) = ContractParser.ValidateOrGenerateCid(null);

        // Assert
        Assert.NotNull(cid);
        Assert.True(Guid.TryParse(cid, out _));
        Assert.True(wasGenerated);
    }

    [Fact]
    public void ValidateOrGenerateCid_WhenInvalidFormat_GeneratesNewCid()
    {
        // Arrange
        var invalidCid = "not-a-uuid";

        // Act
        var (cid, wasGenerated) = ContractParser.ValidateOrGenerateCid(invalidCid);

        // Assert
        Assert.NotNull(cid);
        Assert.True(Guid.TryParse(cid, out _));
        Assert.True(wasGenerated);
        Assert.NotEqual(invalidCid, cid);
    }

    [Fact]
    public void ParseEmptyPayload_WhenLegacyFormat_Success()
    {
        // Arrange
        var json = @"{}";

        // Act
        var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(json);

        // Assert
        Assert.True(isLegacy);
        Assert.NotNull(envelope.Cid);
    }
}
