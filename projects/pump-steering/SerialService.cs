using System;
using System.Collections.Generic;
using System.IO.Ports;
using System.Linq;
using System.Threading;

namespace PumpSteering
{
    public class SerialConfig
    {
        public string PortName { get; set; } = "/dev/ttyS3";
        public int BaudRate { get; set; } = 9600;
        public byte Address { get; set; } = 33;
        public int TimeoutMs { get; set; } = 1200;
        public int InterCommandDelayMs { get; set; } = 120;
    }

    public class SerialService : IDisposable
    {
        private readonly SerialConfig _config;
        private SerialPort _port;
        private readonly object _lock = new object();
        private byte[] _rxBuffer = new byte[1024];
        private int _rxBufferCount = 0;

        public SerialService(SerialConfig config)
        {
            _config = config;
        }

        public void Open()
        {
            lock (_lock)
            {
                if (_port != null && _port.IsOpen) return;

                _port = new SerialPort(_config.PortName, _config.BaudRate, Parity.None, 8, StopBits.One);
                _port.ReadTimeout = 100; // fast read timeout for polling loop
                _port.WriteTimeout = 500;
                try 
                {
                    _port.Open();
                    Console.WriteLine($"[Serial] Opened {_config.PortName} at {_config.BaudRate} baud.");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Serial] Error opening port: {ex.Message}");
                    throw;
                }
            }
        }

        public void Close()
        {
            lock (_lock)
            {
                if (_port != null && _port.IsOpen)
                {
                    _port.Close();
                    Console.WriteLine("[Serial] Closed.");
                }
            }
        }

        public void Dispose()
        {
            Close();
        }

        // Low-level Exchange

        public EhlFrame Exchange(byte cmd, byte[] data = null, byte? expectedCmd = null)
        {
            lock (_lock)
            {
                if (_port == null || !_port.IsOpen) Open();

                Thread.Sleep(_config.InterCommandDelayMs);

                // Build Request
                byte[] frameBytes = EhlProtocol.BuildFrame(_config.Address, cmd, data);
                
                // Clear RX buffer? Maybe good to clear old junk if we are strictly half-duplex request-response
                // But if the device streams data (it doesn't seem to), we might lose it.
                // For this protocol, request-response is the norm.
                _port.DiscardInBuffer();
                _rxBufferCount = 0;

                // Send
                try 
                {
                    _port.Write(frameBytes, 0, frameBytes.Length);
                    // Console.WriteLine($"[Serial] TX: {BitConverter.ToString(frameBytes).Replace("-", " ")}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Serial] TX Error: {ex.Message}");
                    return null;
                }

                // Read Response
                byte waitForCmd = expectedCmd ?? cmd;
                
                DateTime deadline = DateTime.UtcNow.AddMilliseconds(_config.TimeoutMs);
                while (DateTime.UtcNow < deadline)
                {
                    // Read available
                    try 
                    {
                        int bytesToRead = _port.BytesToRead;
                        if (bytesToRead > 0)
                        {
                            // Ensure capacity
                            if (_rxBufferCount + bytesToRead > _rxBuffer.Length)
                            {
                                // Simple expansion or discard?
                                // Let's discard if overflow, as frames are small (<255).
                                // If buffer is full of garbage, we need to clear it.
                                if (_rxBuffer.Length < 4096) 
                                {
                                    Array.Resize(ref _rxBuffer, _rxBuffer.Length * 2);
                                }
                                else
                                {
                                    // Too big, reset
                                    _rxBufferCount = 0; 
                                    Console.WriteLine("[Serial] Buffer overflow, resetting.");
                                }
                            }
                            
                            int bytesRead = _port.Read(_rxBuffer, _rxBufferCount, _rxBuffer.Length - _rxBufferCount);
                            _rxBufferCount += bytesRead;
                        }
                    }
                    catch (TimeoutException) { /* ignore */ }
                    catch (Exception ex) { Console.WriteLine($"[Serial] RX Error: {ex.Message}"); break; }

                    // Process buffer
                    bool bufferChanged = true;
                    while (bufferChanged && _rxBufferCount > 0)
                    {
                        bufferChanged = false;
                        var (result, frame, consumed) = EhlProtocol.ParseOneFrame(_rxBuffer, 0, _rxBufferCount);

                        if (result == ParseResult.Success)
                        {
                            ShiftBuffer(consumed);
                            
                            // Check compatibility
                            if (frame.Stx == EhlProtocol.STX_DISPENSER && 
                                frame.Addr == _config.Address && 
                                frame.Cmd == waitForCmd)
                            {
                                return frame;
                            }
                            else 
                            {
                                Console.WriteLine($"[Serial] RX Ignored frame: CMD=0x{frame.Cmd:X2} ADDR={frame.Addr}");
                                bufferChanged = true; // Try next frame
                            }
                        }
                        else if (result == ParseResult.Incomplete)
                        {
                            // Wait for more data
                            break;
                        }
                        else // Invalid
                        {
                            // Corrupt start or checksum. Shift 1 byte to resync.
                            ShiftBuffer(1);
                            bufferChanged = true;
                        }
                    }

                    Thread.Sleep(10);
                }
                
                Console.WriteLine($"[Serial] Timeout waiting for CMD 0x{waitForCmd:X2}");
                return null;
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

        // High-level API

        public (bool OpenForDelivery, bool StartButtonPressed, bool AutoMode)? PollState()
        {
            var frame = Exchange(EhlProtocol.CMD_STATE, null, EhlProtocol.CMD_STATE);
            if (frame != null && frame.Data.Length >= 1)
            {
                return EhlProtocol.InterpretState(frame.Data[0]);
            }
            return null;
        }

        public string PollVolume()
        {
            var frame = Exchange(EhlProtocol.CMD_VOLUME, null, EhlProtocol.CMD_VOLUME);
            if (frame != null)
            {
                return EhlProtocol.InterpretVolume(frame.Data);
            }
            return null;
        }

        public string PollPrice()
        {
            var frame = Exchange(EhlProtocol.CMD_PRICE, null, EhlProtocol.CMD_PRICE);
            if (frame != null)
            {
                return EhlProtocol.InterpretPrice(frame.Data);
            }
            return null;
        }

        public bool Unlock()
        {
            // CMD_UNBLOCK (0x77)
            var frame = Exchange(EhlProtocol.CMD_UNBLOCK, null, EhlProtocol.CMD_UNBLOCK);
            // Verify by seeing if we get a response (any response is usually ACK for void commands, or echo)
            // Python implementation: exchange() returns any frame seen.
            // "We prefer a response matching ... but will also return any frames seen."
            // If we get a matching frame, success.
            return frame != null;
        }

        public bool Lock()
        {
            // CMD_BLOCK (0x69)
            var frame = Exchange(EhlProtocol.CMD_BLOCK, null, EhlProtocol.CMD_BLOCK);
            return frame != null;
        }

        public bool SetPrice(string price)
        {
            // 1. Product Select (0xC3) -> default 0x30 ('0')?
            // Config says default 0x30.
            byte productByte = 0x30;
            Exchange(EhlProtocol.CMD_PRODUCT_SELECT, new byte[] { productByte }, EhlProtocol.CMD_PRODUCT_SELECT);

            // 2. Program Price (0xA9)
            try 
            {
                byte[] payload = EhlProtocol.EncodePrice(price);
                var frame = Exchange(EhlProtocol.CMD_PROG_PRC, payload, EhlProtocol.CMD_PROG_PRC);
                return frame != null;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Serial] SetPrice Error: {ex.Message}");
                return false;
            }
        }

        public bool Reset()
        {
            // CMD_RESET (0x81)
            var frame = Exchange(EhlProtocol.CMD_RESET, null, EhlProtocol.CMD_RESET);
            return frame != null;
        }
    }
}
