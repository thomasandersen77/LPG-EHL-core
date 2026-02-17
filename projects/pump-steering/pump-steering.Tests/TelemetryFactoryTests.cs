using pump_steering;

namespace pump_steering.Tests;

public class TelemetryFactoryTests
{
    [Fact]
    public void CreateEhlResponseTelemetry_IncludesCidSideActor()
    {
        // Arrange
        var cid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";
        var side = "AZURE_BACKEND";
        var actor = new Actor("USER", "user123", "Test User");

        // Act
        var telemetry = TelemetryFactory.CreateEhlResponseTelemetry(
            cid: cid,
            side: side,
            actor: actor,
            method: "SetPrice",
            address: 33,
            price: 15.90m
        );

        // Assert
        Assert.Equal(cid, telemetry.Cid);
        Assert.Equal(side, telemetry.Side);
        Assert.Equal(actor, telemetry.Actor);
        Assert.Equal("SetPrice", telemetry.Method);
        Assert.Equal(33, telemetry.Address);
        Assert.Equal(15.90m, telemetry.Price);
        Assert.Equal("EHL_RESPONSE", telemetry.EventType);
    }

    [Fact]
    public void CreateErrorTelemetry_HasErrorEventType()
    {
        // Arrange
        var cid = "b3b0c3df-5fbb-45e9-9f6a-0a3c1b5f62a1";
        var side = "AZURE_BACKEND";
        var actor = new Actor("SYSTEM", "pump-steering");

        // Act
        var telemetry = TelemetryFactory.CreateErrorTelemetry(
            cid: cid,
            side: side,
            actor: actor,
            method: "SetPrice",
            address: 33,
            errorMessage: "Serial command failed"
        );

        // Assert
        Assert.Equal(cid, telemetry.Cid);
        Assert.Equal("ERROR", telemetry.EventType);
        Assert.Equal("Serial command failed", telemetry.Data);
    }

    [Fact]
    public void CreateStateTelemetry_HasStateEventType()
    {
        // Act
        var telemetry = TelemetryFactory.CreateStateTelemetry(
            address: 33,
            price: 19.50m,
            volume: 25.5m,
            isLocked: false,
            isOpenForDelivery: true
        );

        // Assert
        Assert.Equal("STATE", telemetry.EventType);
        Assert.Equal("DEVICE", telemetry.Side);
        Assert.Equal("PeriodicState", telemetry.Method);
        Assert.Equal(33, telemetry.Address);
        Assert.Equal(19.50m, telemetry.Price);
        Assert.Equal(25.5m, telemetry.Volume);
        Assert.False(telemetry.IsLocked);
        Assert.True(telemetry.IsOpenForDelivery);
        Assert.NotNull(telemetry.Cid);
        Assert.True(Guid.TryParse(telemetry.Cid, out _));
    }

    [Fact]
    public void CreateStateTelemetry_GeneratesUniqueCid()
    {
        // Act
        var telemetry1 = TelemetryFactory.CreateStateTelemetry(33, 10.0m, 5.0m, false, true);
        var telemetry2 = TelemetryFactory.CreateStateTelemetry(33, 10.0m, 5.0m, false, true);

        // Assert
        Assert.NotEqual(telemetry1.Cid, telemetry2.Cid);
    }

    [Fact]
    public void CreateEhlResponseTelemetry_WithAllOptionalFields_Success()
    {
        // Arrange
        var cid = Guid.NewGuid().ToString();
        var actor = new Actor("SERVICE", "backend-api");

        // Act
        var telemetry = TelemetryFactory.CreateEhlResponseTelemetry(
            cid: cid,
            side: "AZURE_BACKEND",
            actor: actor,
            method: "GetStatus",
            address: 33,
            command: 90,
            data: "02FF1234",
            price: 15.90m,
            volume: 10.5m,
            isLocked: true,
            isOpenForDelivery: false
        );

        // Assert
        Assert.Equal(90, telemetry.Command);
        Assert.Equal("02FF1234", telemetry.Data);
        Assert.Equal(15.90m, telemetry.Price);
        Assert.Equal(10.5m, telemetry.Volume);
        Assert.True(telemetry.IsLocked);
        Assert.False(telemetry.IsOpenForDelivery);
    }
}
