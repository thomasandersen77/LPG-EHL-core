using FluentAssertions;
using pump_steering;

namespace pump_steering.Tests;

public class ConfigValidationTests
{
    private const string ValidConnectionString =
        "HostName=myhub.azure-devices.net;DeviceId=mydevice;SharedAccessKey=abc123";

    [Fact]
    public void TryValidateConnectionString_WhenValid_ReturnsTrue()
    {
        var result = ConfigValidation.TryValidateConnectionString(ValidConnectionString, out var error);

        result.Should().BeTrue();
        error.Should().BeNull();
    }

    [Fact]
    public void TryValidateConnectionString_WhenNull_ReturnsFalse()
    {
        var result = ConfigValidation.TryValidateConnectionString(null, out var error);

        result.Should().BeFalse();
        error.Should().NotBeNullOrEmpty();
        error.Should().Contain("null");
    }

    [Fact]
    public void TryValidateConnectionString_WhenEmpty_ReturnsFalse()
    {
        var result = ConfigValidation.TryValidateConnectionString("", out var error);

        result.Should().BeFalse();
        error.Should().NotBeNullOrEmpty();
    }

    [Fact]
    public void TryValidateConnectionString_WhenMissingHostName_ReturnsFalse()
    {
        var cs = "DeviceId=mydevice;SharedAccessKey=abc123";
        var result = ConfigValidation.TryValidateConnectionString(cs, out var error);

        result.Should().BeFalse();
        error.Should().Contain("HostName");
    }

    [Fact]
    public void TryValidateConnectionString_WhenMissingDeviceId_ReturnsFalse()
    {
        var cs = "HostName=myhub.azure-devices.net;SharedAccessKey=abc123";
        var result = ConfigValidation.TryValidateConnectionString(cs, out var error);

        result.Should().BeFalse();
        error.Should().Contain("DeviceId");
    }

    [Fact]
    public void TryValidateConnectionString_WhenMissingKey_ReturnsFalse()
    {
        var cs = "HostName=myhub.azure-devices.net;DeviceId=mydevice";
        var result = ConfigValidation.TryValidateConnectionString(cs, out var error);

        result.Should().BeFalse();
        error.Should().Contain("SharedAccessKey");
    }

    [Fact]
    public void TryValidateConnectionString_WhenSharedAccessKeyNamePresent_ReturnsTrue()
    {
        var cs = "HostName=myhub.azure-devices.net;DeviceId=mydevice;SharedAccessKeyName=keyname;SharedAccessKey=secret";
        var result = ConfigValidation.TryValidateConnectionString(cs, out var error);

        result.Should().BeTrue();
        error.Should().BeNull();
    }
}
