using System;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Azure.Devices.Client;

namespace PumpSteering
{
    // Simple state holder
    public class LoopState
    {
        public decimal Price { get; set; }
        public decimal Volume { get; set; }
        public bool IsLocked { get; set; }
        public bool IsOpenForDelivery { get; set; }
    }

    public class Program
    {
        private static SerialService _serial;
        private static DeviceClient _deviceClient;
        private static LoopState _state = new LoopState();
        private static string _connectionString;
        private static string _serialPortName = "/dev/ttyS3"; // Default

        public static async Task Main(string[] args)
        {
            if (args.Length < 1)
            {
                Console.WriteLine("Usage: dotnet run -- <device_connection_string> [serial_port]");
                return;
            }

            _connectionString = args[0];
            if (args.Length >= 2) _serialPortName = args[1];

            Console.WriteLine($"Initializing Pump Steering IoT Client... Serial Port: {_serialPortName}");

            // Init Serial
            var serialConfig = new SerialConfig { PortName = _serialPortName, BaudRate = 9600 };
            _serial = new SerialService(serialConfig);
            try 
            {
                _serial.Open();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[FATAL] Could not open serial port: {ex.Message}");
                // We might want to exit, or continue in a "degraded" mode?
                // For now, let's exit as this is the core function.
                return; 
            }

            try
            {
                _deviceClient = DeviceClient.CreateFromConnectionString(_connectionString, TransportType.Mqtt);
                await _deviceClient.OpenAsync();
                Console.WriteLine("Connected to IoT Hub.");

                // Register Direct Methods
                await _deviceClient.SetMethodHandlerAsync("SetPrice", SetPriceHandler, null);
                await _deviceClient.SetMethodHandlerAsync("Unlock", UnlockHandler, null);
                await _deviceClient.SetMethodHandlerAsync("Lock", LockHandler, null);
                await _deviceClient.SetMethodHandlerAsync("Reset", ResetHandler, null);

                // Start background tasks
                var cts = new CancellationTokenSource();
                var pollingTask = PollPumpAsync(cts.Token);
                var telemetryTask = SendTelemetryAsync(cts.Token);

                Console.WriteLine("Press Control+C to quit.");
                await Task.Delay(-1, cts.Token);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
            }
            finally 
            {
                if (_deviceClient != null) await _deviceClient.CloseAsync();
                if (_serial != null) _serial.Close();
            }
        }

        private static async Task PollPumpAsync(CancellationToken ct)
        {
            // Polling loop: State -> Volume -> Price -> (Error?)
            while (!ct.IsCancellationRequested)
            {
                try 
                {
                    // 1. Poll State
                    var stateInfo = _serial.PollState();
                    if (stateInfo.HasValue)
                    {
                        var (open, start, auto) = stateInfo.Value;
                        // Map "Open for delivery" bit to Locked/Unlocked?
                        // If "open_for_delivery" is true, it means pump is delivering or ready?
                        // Actually, CMD_UNBLOCK enables "Open for Delivery" state (bit 1).
                        // So if bit 1 is set, IsLocked = false.
                        _state.IsOpenForDelivery = open;
                        _state.IsLocked = !open; 
                        
                        // Console.WriteLine($"[Poll] State: Open={open} Start={start} Auto={auto}");
                    }

                    // 2. Poll Volume (only if delivering? or always?)
                    // Always poll volume to catch updates
                    var volStr = _serial.PollVolume();
                    if (volStr != null && decimal.TryParse(volStr, out decimal v))
                    {
                        _state.Volume = v;
                    }

                    // 3. Poll Price (maybe less frequently?)
                    // For now, poll every loop
                    var priceStr = _serial.PollPrice();
                    if (priceStr != null && decimal.TryParse(priceStr, out decimal p))
                    {
                        _state.Price = p;
                    }

                    // Sleep for a bit
                    await Task.Delay(200, ct); 
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[PollLoop] Error: {ex.Message}");
                    await Task.Delay(1000, ct);
                }
            }
        }

        private static async Task SendTelemetryAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                var telemetryDataPoint = new
                {
                    price = _state.Price,
                    volume = _state.Volume,
                    is_locked = _state.IsLocked, // Derived from OpenForDelivery bit
                    is_open_for_delivery = _state.IsOpenForDelivery,
                    timestamp = DateTime.UtcNow
                };

                string messageString = JsonSerializer.Serialize(telemetryDataPoint);
                var message = new Message(Encoding.UTF8.GetBytes(messageString));
                message.ContentType = "application/json";
                message.ContentEncoding = "utf-8";

                try 
                {
                    await _deviceClient.SendEventAsync(message);
                    Console.WriteLine($"[Telemetry] Sent: {messageString}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Telemetry] Send Error: {ex.Message}");
                }

                await Task.Delay(1000, ct); // Send every second
            }
        }

        // Direct Method Handlers

        private static Task<MethodResponse> SetPriceHandler(MethodRequest methodRequest, object userContext)
        {
            try 
            {
                var payload = JsonDocument.Parse(methodRequest.DataAsJson);
                if (payload.RootElement.TryGetProperty("price", out JsonElement priceElement))
                {
                    if (priceElement.TryGetDecimal(out decimal newPrice))
                    {
                        // Call Serial Service
                        bool currentLocked = _state.IsLocked;
                        // Usually price setting requires pump to be ready or idle?
                        
                        string priceStr = newPrice.ToString("F2"); // Format XX.XX
                        // Validate simple format mapping
                        if (newPrice < 0 || newPrice > 99.99m) 
                             return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Price out of range\"}"), 400));
                        
                        // We might need to ensure formatting "15.90" not "15,90" depending on locale!
                        // Enforce dot
                        priceStr = newPrice.ToString("00.00", System.Globalization.CultureInfo.InvariantCulture);

                        bool success = _serial.SetPrice(priceStr);
                        
                        if (success)
                        {
                            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes($"{{\"result\": \"Price updated\", \"new_price\": {newPrice}}}"), 200));
                        }
                        else 
                        {
                            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Serial command failed\"}"), 500));
                        }
                    }
                }
                return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Invalid price format\"}"), 400));
            }
            catch (Exception ex)
            {
                return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes($"{{ \"error\": \"{ex.Message}\" }}"), 500));
            }
        }

        private static Task<MethodResponse> UnlockHandler(MethodRequest methodRequest, object userContext)
        {
            bool success = _serial.Unlock();
            if (success)
            {
                 // Optimistic update
                _state.IsLocked = false;
                return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"result\": \"Pump unlocked command sent\"}"), 200));
            }
            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Unlock command failed\"}"), 500));
        }

        private static Task<MethodResponse> LockHandler(MethodRequest methodRequest, object userContext)
        {
            bool success = _serial.Lock();
            if (success)
            {
                _state.IsLocked = true;
                return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"result\": \"Pump locked command sent\"}"), 200));
            }
            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Lock command failed\"}"), 500));
        }

        private static Task<MethodResponse> ResetHandler(MethodRequest methodRequest, object userContext)
        {
             bool success = _serial.Reset();
             if (success)
             {
                 return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"result\": \"Pump reset command sent\"}"), 200));
             }
             return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Reset command failed\"}"), 500));
        }
    }
}
