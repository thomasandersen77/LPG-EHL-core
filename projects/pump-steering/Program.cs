using System;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Azure.Devices.Client;

namespace PumpSteering
{
    public class PumpController
    {
        public decimal Price { get; private set; } = 15.90m;
        public decimal Volume { get; private set; } = 0.00m;
        public bool IsLocked { get; private set; } = true;
        
        private readonly object _lock = new object();

        public void SetPrice(decimal newPrice)
        {
            lock (_lock)
            {
                Price = newPrice;
                Console.WriteLine($"[Pump] Price set to {Price:F2}");
            }
        }

        public void Unlock()
        {
            lock (_lock)
            {
                if (!IsLocked) return;
                IsLocked = false;
                Console.WriteLine("[Pump] Unlocked. Ready to dispense.");
            }
        }

        public void Lock()
        {
            lock (_lock)
            {
                if (IsLocked) return;
                IsLocked = true;
                Console.WriteLine("[Pump] Locked. Dispensing stopped.");
            }
        }

        public void SimulateFueling(decimal increment)
        {
            lock (_lock)
            {
                if (!IsLocked)
                {
                    Volume += increment;
                    // Console.WriteLine($"[Pump] Fueling... Volume: {Volume:F2}");
                }
            }
        }

        public bool Reset()
        {
            lock (_lock)
            {
                if (!IsLocked) return false;
                Volume = 0.00m;
                Console.WriteLine("[Pump] Reset volume to 0.00.");
                return true;
            }
        }
    }

    public class Program
    {
        private static PumpController _pump = new PumpController();
        private static DeviceClient _deviceClient;
        
        // Connection string should be passed as an argument or environment variable
        // For simplicity in this demo, we'll take it from args
        private static string _connectionString;

        public static async Task Main(string[] args)
        {
            if (args.Length < 1)
            {
                Console.WriteLine("Usage: dotnet run -- <device_connection_string>");
                return;
            }

            _connectionString = args[0];

            Console.WriteLine("Initializing Pump Steering IoT Client...");

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
                var telemetryTask = SendTelemetryAsync(cts.Token);
                var simulationTask = SimulatePumpAsync(cts.Token);

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
            }
        }

        private static async Task SendTelemetryAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                var telemetryDataPoint = new
                {
                    price = _pump.Price,
                    volume = _pump.Volume,
                    is_locked = _pump.IsLocked,
                    timestamp = DateTime.UtcNow
                };

                string messageString = JsonSerializer.Serialize(telemetryDataPoint);
                var message = new Message(Encoding.UTF8.GetBytes(messageString));
                message.ContentType = "application/json";
                message.ContentEncoding = "utf-8";

                await _deviceClient.SendEventAsync(message);
                Console.WriteLine($"[Telemetry] Sent: {messageString}");

                await Task.Delay(1000, ct); // Send every second
            }
        }

        private static async Task SimulatePumpAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                if (!_pump.IsLocked)
                {
                    // Random increment to simulate fueling
                    _pump.SimulateFueling(0.05m); 
                }
                await Task.Delay(200, ct); // Simulate flow rate
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
                        _pump.SetPrice(newPrice);
                        string result = JsonSerializer.Serialize(new { result = "Price updated", new_price = newPrice });
                        return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes(result), 200));
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
            _pump.Unlock();
            string result = JsonSerializer.Serialize(new { result = "Pump unlocked" });
            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes(result), 200));
        }

        private static Task<MethodResponse> LockHandler(MethodRequest methodRequest, object userContext)
        {
            _pump.Lock();
            string result = JsonSerializer.Serialize(new { result = "Pump locked" });
            return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes(result), 200));
        }

        private static Task<MethodResponse> ResetHandler(MethodRequest methodRequest, object userContext)
        {
             if (_pump.Reset())
             {
                 string result = JsonSerializer.Serialize(new { result = "Pump reset" });
                 return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes(result), 200));
             }
             else 
             {
                 return Task.FromResult(new MethodResponse(Encoding.UTF8.GetBytes("{\"error\": \"Cannot reset while unlocked\"}"), 400));
             }
        }
    }
}
