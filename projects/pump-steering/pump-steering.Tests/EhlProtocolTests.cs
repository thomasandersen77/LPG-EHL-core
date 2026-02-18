using FluentAssertions;
using pump_steering;

namespace pump_steering.Tests;

public class EhlProtocolTests
{
    // Frame structure: STX (0x10 controller) + LEN + ADDR + CMD + DATA... + CHK + ETX (0x36)
    private const byte Etx = 0x36;

    [Fact]
    public void BuildFrame_WithNoData_ProducesValidMinimalFrame()
    {
        byte addr = 33;
        byte cmd = EhlProtocol.CMD_STATE; // 0x4B

        var frame = EhlProtocol.BuildFrame(addr, cmd, null);

        frame.Should().NotBeNull();
        frame.Length.Should().Be(6);
        frame[0].Should().Be(0x10); // STX_CONTROLLER
        frame[1].Should().Be(6);    // length
        frame[2].Should().Be(addr);
        frame[3].Should().Be(cmd);
        frame[5].Should().Be(Etx);
        // Checksum at frame[4] should XOR to 0 with rest
        byte chk = 0;
        for (int i = 0; i < 4; i++) chk ^= frame[i];
        chk.Should().Be(frame[4]);
    }

    [Fact]
    public void BuildFrame_WithData_IncludesDataAndCorrectLength()
    {
        byte addr = 33;
        byte cmd = EhlProtocol.CMD_PROG_PRC;
        byte[] data = [0x30, 0x39, 0x35, 0x31]; // "15.90" encoded (EncodePrice order)

        var frame = EhlProtocol.BuildFrame(addr, cmd, data);

        frame.Length.Should().Be(6 + 4);
        frame[0].Should().Be(0x10);
        frame[1].Should().Be(10);
        frame[2].Should().Be(addr);
        frame[3].Should().Be(cmd);
        frame[4].Should().Be(0x30);
        frame[5].Should().Be(0x39);
        frame[6].Should().Be(0x35);
        frame[7].Should().Be(0x31);
        frame[8].Should().Be(frame.Skip(0).Take(8).Aggregate((byte)0, (a, b) => (byte)(a ^ b)));
        frame[9].Should().Be(Etx);
    }

    [Fact]
    public void ParseOneFrame_WhenBufferTooShort_ReturnsIncomplete()
    {
        var buffer = new byte[] { 0x10, 6, 33, 0x4B }; // only 4 bytes

        var (result, _, consumed) = EhlProtocol.ParseOneFrame(buffer, 0, buffer.Length);

        result.Should().Be(ParseResult.Incomplete);
        consumed.Should().Be(0);
    }

    [Fact]
    public void BuildFrame_AndParseOneFrame_RoundTripsSuccessfully()
    {
        byte addr = 33;
        byte cmd = EhlProtocol.CMD_UNBLOCK;
        byte[]? data = null;
        var sent = EhlProtocol.BuildFrame(addr, cmd, data);

        var (result, frame, consumed) = EhlProtocol.ParseOneFrame(sent, 0, sent.Length);

        result.Should().Be(ParseResult.Success);
        consumed.Should().Be(sent.Length);
        frame.Should().NotBeNull();
        frame!.Stx.Should().Be(0x10);
        frame.Length.Should().Be(6);
        frame.Addr.Should().Be(addr);
        frame.Cmd.Should().Be(cmd);
        frame.Data.Should().BeEmpty();
        frame.Etx.Should().Be(Etx);
    }

    [Fact]
    public void BuildFrame_AndParseOneFrame_WithData_RoundTripsSuccessfully()
    {
        byte addr = 33;
        byte cmd = EhlProtocol.CMD_PROG_PRC;
        byte[] data = [0x31, 0x32, 0x33, 0x34];
        var sent = EhlProtocol.BuildFrame(addr, cmd, data);

        var (result, frame, consumed) = EhlProtocol.ParseOneFrame(sent, 0, sent.Length);

        result.Should().Be(ParseResult.Success);
        consumed.Should().Be(sent.Length);
        frame.Should().NotBeNull();
        frame!.Cmd.Should().Be(cmd);
        frame.Data.Should().Equal(data);
    }

    [Fact]
    public void ParseOneFrame_WhenInvalidStx_ReturnsInvalid()
    {
        var buffer = new byte[] { 0x99, 6, 33, 0x4B, 0x00, Etx }; // bad STX

        var (result, _, consumed) = EhlProtocol.ParseOneFrame(buffer, 0, buffer.Length);

        result.Should().Be(ParseResult.Invalid);
        consumed.Should().Be(0);
    }

    [Fact]
    public void ParseOneFrame_WhenWrongEtx_ReturnsInvalid()
    {
        var validFrame = EhlProtocol.BuildFrame(33, EhlProtocol.CMD_STATE, null);
        validFrame[^1] = 0x00; // break ETX

        var (result, _, _) = EhlProtocol.ParseOneFrame(validFrame, 0, validFrame.Length);

        result.Should().Be(ParseResult.Invalid);
    }

    [Fact]
    public void ParseOneFrame_WhenChecksumWrong_ReturnsInvalid()
    {
        var validFrame = EhlProtocol.BuildFrame(33, EhlProtocol.CMD_STATE, null);
        validFrame[^2] ^= 0xFF; // corrupt checksum

        var (result, _, _) = EhlProtocol.ParseOneFrame(validFrame, 0, validFrame.Length);

        result.Should().Be(ParseResult.Invalid);
    }

    [Fact]
    public void ParseOneFrame_AcceptsDispenserStx()
    {
        // Dispenser response uses STX 0x20; build a valid frame with that STX manually
        byte addr = 33;
        byte cmd = EhlProtocol.CMD_STATE;
        byte[] frame = [0x20, 6, addr, cmd, 0x00, Etx]; // checksum 0 for first 4 bytes
        frame[4] = frame[0];
        for (int i = 1; i < 4; i++) frame[4] ^= frame[i];

        var (result, parsed, consumed) = EhlProtocol.ParseOneFrame(frame, 0, frame.Length);

        result.Should().Be(ParseResult.Success);
        parsed!.Stx.Should().Be(0x20);
        consumed.Should().Be(6);
    }

    [Theory]
    [InlineData("00.00", new byte[] { 0x30, 0x30, 0x30, 0x30 })]
    [InlineData("99.99", new byte[] { 0x39, 0x39, 0x39, 0x39 })]
    [InlineData("15.90", new byte[] { 0x30, 0x39, 0x35, 0x31 })] // digits[3],digits[2],digits[1],digits[0] = 0,9,5,1
    public void EncodePrice_WhenValidFormat_ReturnsExpectedBytes(string priceStr, byte[] expected)
    {
        var bytes = EhlProtocol.EncodePrice(priceStr);

        bytes.Should().Equal(expected);
    }

    [Fact]
    public void EncodePrice_WhenWhitespaceTrimmed_StillValid()
    {
        var bytes = EhlProtocol.EncodePrice("  12.50  ");

        bytes.Should().Equal((byte)'0', (byte)'5', (byte)'2', (byte)'1');
    }

    [Theory]
    [InlineData("1.23")]   // too short
    [InlineData("123.45")] // too long
    [InlineData("12-34")]  // wrong separator
    [InlineData("12.5")]   // only 3 digits after
    public void EncodePrice_WhenInvalidFormat_Throws(string invalid)
    {
        var act = () => EhlProtocol.EncodePrice(invalid);

        act.Should().Throw<ArgumentException>();
    }

    [Fact]
    public void InterpretPrice_WhenValidData_ReturnsFormattedString()
    {
        // InterpretPrice: chars[0]=data[3], chars[1]=data[2], '.', chars[3]=data[1], chars[4]=data[0]
        // "15.90" -> data[3]=1, data[2]=5, data[1]=9, data[0]=0 -> [0x30, 0x39, 0x35, 0x31]
        byte[] data = [0x30, 0x39, 0x35, 0x31];

        var result = EhlProtocol.InterpretPrice(data);

        result.Should().Be("15.90");
    }

    [Fact]
    public void InterpretPrice_WhenNullOrTooShort_ReturnsNull()
    {
        EhlProtocol.InterpretPrice(null).Should().BeNull();
        EhlProtocol.InterpretPrice([]).Should().BeNull();
        EhlProtocol.InterpretPrice([0x30, 0x30]).Should().BeNull();
    }

    [Fact]
    public void InterpretVolume_WhenValidData_ReturnsFormattedString()
    {
        // Format: chars from data[4], data[3], data[2], '.', data[1], data[0] -> 6 chars
        // data = [0x30,0x35,0x33,0x32,0x31] -> '1','2','3','.','5','0' = "123.50"
        byte[] data = [(byte)'0', (byte)'5', (byte)'3', (byte)'2', (byte)'1'];

        var result = EhlProtocol.InterpretVolume(data);

        result.Should().Be("123.50");
    }

    [Fact]
    public void InterpretVolume_WhenNullOrTooShort_ReturnsNull()
    {
        EhlProtocol.InterpretVolume(null).Should().BeNull();
        EhlProtocol.InterpretVolume([0x30]).Should().BeNull();
    }

    [Theory]
    [InlineData(0x00, false, false, false)]
    [InlineData(0x02, true, false, false)]   // open for delivery
    [InlineData(0x04, false, true, false)]   // start
    [InlineData(0x08, false, false, true)]  // auto
    [InlineData(0x0E, true, true, true)]
    public void InterpretState_WhenGivenStateByte_ReturnsCorrectFlags(
        byte stateByte, bool open, bool start, bool auto)
    {
        var result = EhlProtocol.InterpretState(stateByte);

        result.OpenForDelivery.Should().Be(open);
        result.StartButtonPressed.Should().Be(start);
        result.AutoMode.Should().Be(auto);
    }
}
