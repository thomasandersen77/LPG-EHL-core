using FluentAssertions;
using pump_steering;

namespace pump_steering.Tests;

public class SerialServiceTests
{
    [Fact]
    public void Constructor_WithSerialConfigOnly_DoesNotThrow()
    {
        var config = new SerialConfig
        {
            PortName = "/dev/ttyS3",
            BaudRate = 9600,
            Address = 33
        };

        var act = () => new SerialService(config);

        act.Should().NotThrow();
    }

    [Fact]
    public void Constructor_WithSerialConfigOnly_ProducesDisposableInstance()
    {
        var config = new SerialConfig { PortName = "COM99", BaudRate = 9600, Address = 33 };
        using var service = new SerialService(config);

        service.Should().BeAssignableTo<IDisposable>();
    }

    [Fact]
    public void Dispose_WhenCalled_DoesNotThrow()
    {
        var config = new SerialConfig { PortName = "COM99", BaudRate = 9600, Address = 33 };
        var service = new SerialService(config);

        var act = () => service.Dispose();

        act.Should().NotThrow();
    }

    [Fact]
    public void Dispose_WhenCalledMultipleTimes_DoesNotThrow()
    {
        var config = new SerialConfig { PortName = "COM99", BaudRate = 9600, Address = 33 };
        var service = new SerialService(config);

        service.Dispose();
        var act = () => service.Dispose();

        act.Should().NotThrow();
    }

    [Fact]
    public void Open_WhenPortDoesNotExist_Throws()
    {
        var config = new SerialConfig
        {
            PortName = "/dev/nonexistent-port-xyz-12345",
            BaudRate = 9600,
            Address = 33
        };
        using var service = new SerialService(config);

        var act = () => service.Open();

        act.Should().Throw<Exception>();
    }
}
