using System.IO.Ports;
using Microsoft.Extensions.Logging;

namespace pump_steering
{
    public class SerialConfig
    {
        public string PortName { get; set; } = "/dev/ttyS3";
        public int BaudRate { get; set; } = 9600;
        public byte Address { get; set; } = 33;
        public int TimeoutMs { get; set; } = 1200;
        public int InterCommandDelayMs { get; set; } = 120;
        public ILogger? Logger { get; set; }
    }

    public class SerialService(SerialConfig config, SerialPort port) : IDisposable
    {
        private SerialPort _port = port;
        private readonly object _openCloseLock = new object();
        private readonly SemaphoreSlim _exchangeSemaphore = new(1, 1);
        private byte[] _rxBuffer = new byte[1024];
        private int _rxBufferCount = 0;

        public SerialService(SerialConfig serialConfig) : this(serialConfig, new SerialPort())
        {
        }

        public void Open()
        {
            lock (_openCloseLock)
            {
                if (_port.IsOpen) return;

                _port = new SerialPort(config.PortName, config.BaudRate, Parity.None, 8, StopBits.One);
                _port.ReadTimeout = 100; // fast read timeout for polling loop
                _port.WriteTimeout = 500;
                try 
                {
                    _port.Open();
                    config.Logger?.LogInformation("Serial port opened: {PortName} at {BaudRate} baud", config.PortName, config.BaudRate);
                }
                catch (Exception ex)
                {
                    config.Logger?.LogError(ex, "Error opening serial port: {Message}", ex.Message);
                    throw;
                }
            }
        }

        public void Close()
        {
            lock (_openCloseLock)
            {
                if (_port.IsOpen)
                {
                    _port.Close();
                    config.Logger?.LogInformation("Serial port closed");
                }
            }
        }

        public void Dispose()
        {
            Close();
        }

        // Low-level Exchange (async, non-blocking)

        public async Task<EhlFrame?> ExchangeAsync(byte cmd, byte[]? data = null, byte? expectedCmd = null, CancellationToken ct = default)
        {
            await _exchangeSemaphore.WaitAsync(ct).ConfigureAwait(false);
            try
            {
                if (!_port.IsOpen) Open();

                await Task.Delay(config.InterCommandDelayMs, ct).ConfigureAwait(false);

                // Build Request
                byte[] frameBytes = EhlProtocol.BuildFrame(config.Address, cmd, data);
                _port.DiscardInBuffer();
                _rxBufferCount = 0;

                // Send
                try
                {
                    _port.Write(frameBytes, 0, frameBytes.Length);
                }
                catch (Exception ex)
                {
                    config.Logger?.LogError(ex, "Serial TX error: {Message}", ex.Message);
                    return null;
                }

                byte waitForCmd = expectedCmd ?? cmd;
                DateTime deadline = DateTime.UtcNow.AddMilliseconds(config.TimeoutMs);
                while (DateTime.UtcNow < deadline)
                {
                    ct.ThrowIfCancellationRequested();
                    try
                    {
                        int bytesToRead = _port.BytesToRead;
                        if (bytesToRead > 0)
                        {
                            if (_rxBufferCount + bytesToRead > _rxBuffer.Length)
                            {
                                if (_rxBuffer.Length < 4096)
                                    Array.Resize(ref _rxBuffer, _rxBuffer.Length * 2);
                                else
                                {
                                    _rxBufferCount = 0;
                                    config.Logger?.LogWarning("Serial RX buffer overflow, resetting");
                                }
                            }
                            int bytesRead = _port.Read(_rxBuffer, _rxBufferCount, _rxBuffer.Length - _rxBufferCount);
                            _rxBufferCount += bytesRead;
                        }
                    }
                    catch (TimeoutException) { }
                    catch (Exception ex)
                    {
                        config.Logger?.LogError(ex, "Serial RX error: {Message}", ex.Message);
                        break;
                    }

                    bool bufferChanged = true;
                    while (bufferChanged && _rxBufferCount > 0)
                    {
                        bufferChanged = false;
                        var (result, frame, consumed) = EhlProtocol.ParseOneFrame(_rxBuffer, 0, _rxBufferCount);

                        if (result == ParseResult.Success)
                        {
                            ShiftBuffer(consumed);
                            if (frame.Stx == EhlProtocol.STX_DISPENSER &&
                                frame.Addr == config.Address &&
                                frame.Cmd == waitForCmd)
                                return frame;
                            config.Logger?.LogDebug("Serial RX ignored frame: CMD=0x{Cmd:X2} ADDR={Addr}", frame.Cmd, frame.Addr);
                            bufferChanged = true;
                        }
                        else if (result == ParseResult.Incomplete)
                            break;
                        else
                        {
                            ShiftBuffer(1);
                            bufferChanged = true;
                        }
                    }

                    await Task.Delay(10, ct).ConfigureAwait(false);
                }

                config.Logger?.LogWarning("Serial timeout waiting for CMD 0x{Cmd:X2}", waitForCmd);
                return null;
            }
            finally
            {
                _exchangeSemaphore.Release();
            }
        }

        private void ShiftBuffer(int count)
        {
            if (count <= 0) return;
            if (count >= _rxBufferCount)
            {
                _rxBufferCount = 0;
            }
            else
            {
                Array.Copy(_rxBuffer, count, _rxBuffer, 0, _rxBufferCount - count);
                _rxBufferCount -= count;
            }
        }

        // High-level API (async)

        public async Task<(bool OpenForDelivery, bool StartButtonPressed, bool AutoMode)?> PollStateAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_STATE, null, EhlProtocol.CMD_STATE, ct).ConfigureAwait(false);
            if (frame != null && frame.Data.Length >= 1)
                return EhlProtocol.InterpretState(frame.Data[0]);
            return null;
        }

        public async Task<string?> PollVolumeAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_VOLUME, null, EhlProtocol.CMD_VOLUME, ct).ConfigureAwait(false);
            return frame != null ? (EhlProtocol.InterpretVolume(frame.Data) ?? string.Empty) : null;
        }

        public async Task<string?> PollPriceAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_PRICE, null, EhlProtocol.CMD_PRICE, ct).ConfigureAwait(false);
            return frame != null ? EhlProtocol.InterpretPrice(frame.Data) : null;
        }

        public async Task<bool> UnlockAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_UNBLOCK, null, EhlProtocol.CMD_UNBLOCK, ct).ConfigureAwait(false);
            return frame != null;
        }

        public async Task<bool> LockAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_BLOCK, null, EhlProtocol.CMD_BLOCK, ct).ConfigureAwait(false);
            return frame != null;
        }

        public async Task<bool> SetPriceAsync(string price, CancellationToken ct = default)
        {
            byte productByte = 0x30;
            await ExchangeAsync(EhlProtocol.CMD_PRODUCT_SELECT, [productByte], EhlProtocol.CMD_PRODUCT_SELECT, ct).ConfigureAwait(false);
            try
            {
                byte[] payload = EhlProtocol.EncodePrice(price);
                var frame = await ExchangeAsync(EhlProtocol.CMD_PROG_PRC, payload, EhlProtocol.CMD_PROG_PRC, ct).ConfigureAwait(false);
                return frame != null;
            }
            catch (Exception ex)
            {
                config.Logger?.LogError(ex, "Serial SetPrice error: {Message}", ex.Message);
                return false;
            }
        }

        public async Task<bool> ResetAsync(CancellationToken ct = default)
        {
            var frame = await ExchangeAsync(EhlProtocol.CMD_RESET, null, EhlProtocol.CMD_RESET, ct).ConfigureAwait(false);
            return frame != null;
        }
    }
}
