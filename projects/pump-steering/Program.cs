using System.Text;
using System.Text.Json;
using Microsoft.Azure.Devices.Client;

namespace pump_steering
{
    // Simple state holder
    public class LoopState
    {
        public decimal Price { get; set; }
        public decimal Volume { get; set; }
        public bool IsLocked { get; set; }
        public bool IsOpenForDelivery { get; set; }
    }

    public static class Program
    {
        private static SerialService? _serial;
        private static DeviceClient? _deviceClient;
        private static readonly LoopState State = new();
        private static string? _connectionString;
        private static string _serialPortName = "/dev/ttyS3"; // Default
        private static int _address = 33; // Default EHL address

        public static async Task Main(string[] args)
        {
            if (args.Length < 1)
            {
                Console.WriteLine("Usage: dotnet run -- <device_connection_string> [serial_port] [address]");
                return;
            }

            _connectionString = args[0];
            if (args.Length >= 2) _serialPortName = args[1];
            if (args.Length >= 3 && int.TryParse(args[2], out int addr)) _address = addr;

            Console.WriteLine($"Initializing Pump Steering IoT Client... Serial Port: {_serialPortName}, Address: {_address}");

            // Init Serial
            var serialConfig = new SerialConfig
            {
                PortName = _serialPortName,
                BaudRate = 9600,
                Address = (byte)_address
            };
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
                _serial.Close();
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
                    var stateInfo = _serial?.PollState();
                    if (stateInfo.HasValue)
                    {
                        var (open, start, auto) = stateInfo.Value;
                        // Map "Open for delivery" bit to Locked/Unlocked?
                        // If "open_for_delivery" is true, it means pump is delivering or ready?
                        // Actually, CMD_UNBLOCK enables "Open for Delivery" state (bit 1).
                        // So if bit 1 is set, IsLocked = false.
                        State.IsOpenForDelivery = open;
                        State.IsLocked = !open; 
                        
                        // Console.WriteLine($"[Poll] State: Open={open} Start={start} Auto={auto}");
                    }

                    // 2. Poll Volume (only if delivering? or always?)
                    // Always poll volume to catch updates
                    var volStr = _serial?.PollVolume();
                    if (volStr != null && decimal.TryParse(volStr, out decimal v))
                    {
                        State.Volume = v;
                    }

                    // 3. Poll Price (maybe less frequently?)
                    // For now, poll every loop
                    var priceStr = _serial?.PollPrice();
                    if (priceStr != null && decimal.TryParse(priceStr, out decimal p))
                    {
                        State.Price = p;
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
                var telemetry = TelemetryFactory.CreateStateTelemetry(
                    address: _address,
                    price: State.Price,
                    volume: State.Volume,
                    isLocked: State.IsLocked,
                    isOpenForDelivery: State.IsOpenForDelivery
                );

                string messageString = JsonSerializer.Serialize(telemetry);
                var message = new Message(Encoding.UTF8.GetBytes(messageString));
                message.ContentType = "application/json";
                message.ContentEncoding = "utf-8";

                try
                {
                    await _deviceClient?.SendEventAsync(message, ct)!;
                    Console.WriteLine($"[Telemetry] Sent: {messageString}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Telemetry] Send Error: {ex.Message}");
                }

                await Task.Delay(10000, ct); // Send every 10 seconds
            }
        }

        // ===== Direct Method Handlers =====

        private static async Task<MethodResponse> SetPriceHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                // Parse envelope or legacy payload
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<SetPricePayload>(methodRequest.DataAsJson);

                // Validate CID (warn if generated)
                var (cid, wasGenerated) = ContractParser.ValidateOrGenerateCid(envelope.Cid);
                if (wasGenerated && !isLegacy)
                {
                    Console.WriteLine($"[SetPrice] Warning: Invalid CID, generated new one: {cid}");
                }

                // Validate payload
                var priceValidation = ContractValidator.ValidatePrice(envelope.Payload.Price);
                if (!priceValidation.IsValid)
                {
                    var errorResponse = new MethodResponseDto(
                        Cid: cid,
                        Ok: false,
                        ErrorCode: priceValidation.ErrorCode,
                        Message: priceValidation.Message
                    );

                    // Send error telemetry
                    await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "SetPrice",
                        success: false, errorMessage: priceValidation.Message);

                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        400);
                }

                // Format price
                var priceStr = envelope.Payload.Price.ToString("00.00", System.Globalization.CultureInfo.InvariantCulture);

                // Execute serial command
                bool success = _serial != null && _serial.SetPrice(priceStr);

                // Send telemetry (success or failure)
                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "SetPrice",
                    success: success, errorMessage: success ? null : "Serial command failed");

                if (success)
                {
                    var response = new MethodResponseDto(
                        Cid: cid,
                        Ok: true,
                        Message: "Price updated",
                        Data: new { new_price = envelope.Payload.Price }
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = new MethodResponseDto(
                        Cid: cid,
                        Ok: false,
                        ErrorCode: ErrorCodes.SerialCommandFailed,
                        Message: "Serial command failed"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[SetPrice] Exception: {ex.Message}");
                var errorResponse = new MethodResponseDto(
                    Cid: Guid.NewGuid().ToString(),
                    Ok: false,
                    ErrorCode: ErrorCodes.Unknown,
                    Message: ex.Message
                );
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> UnlockHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _serial != null && _serial.Unlock();

                if (success)
                {
                    State.IsLocked = false; // Optimistic update
                }

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Unlock",
                    success: success, errorMessage: success ? null : "Unlock command failed");

                if (success)
                {
                    var response = new MethodResponseDto(
                        Cid: cid,
                        Ok: true,
                        Message: "Pump unlocked command sent"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = new MethodResponseDto(
                        Cid: cid,
                        Ok: false,
                        ErrorCode: ErrorCodes.SerialCommandFailed,
                        Message: "Unlock command failed"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Unlock] Exception: {ex.Message}");
                var errorResponse = new MethodResponseDto(
                    Cid: Guid.NewGuid().ToString(),
                    Ok: false,
                    ErrorCode: ErrorCodes.Unknown,
                    Message: ex.Message
                );
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> LockHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _serial != null && _serial.Lock();

                if (success)
                {
                    State.IsLocked = true; // Optimistic update
                }

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Lock",
                    success: success, errorMessage: success ? null : "Lock command failed");

                if (success)
                {
                    var response = new MethodResponseDto(
                        Cid: cid,
                        Ok: true,
                        Message: "Pump locked command sent"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = new MethodResponseDto(
                        Cid: cid,
                        Ok: false,
                        ErrorCode: ErrorCodes.SerialCommandFailed,
                        Message: "Lock command failed"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Lock] Exception: {ex.Message}");
                var errorResponse = new MethodResponseDto(
                    Cid: Guid.NewGuid().ToString(),
                    Ok: false,
                    ErrorCode: ErrorCodes.Unknown,
                    Message: ex.Message
                );
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> ResetHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _serial != null && _serial.Reset();

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Reset",
                    success: success, errorMessage: success ? null : "Reset command failed");

                if (success)
                {
                    var response = new MethodResponseDto(
                        Cid: cid,
                        Ok: true,
                        Message: "Pump reset command sent"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = new MethodResponseDto(
                        Cid: cid,
                        Ok: false,
                        ErrorCode: ErrorCodes.SerialCommandFailed,
                        Message: "Reset command failed"
                    );
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Reset] Exception: {ex.Message}");
                var errorResponse = new MethodResponseDto(
                    Cid: Guid.NewGuid().ToString(),
                    Ok: false,
                    ErrorCode: ErrorCodes.Unknown,
                    Message: ex.Message
                );
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        // Helper to send method telemetry
        private static async Task SendMethodTelemetryAsync(
            string cid,
            string side,
            Actor actor,
            string method,
            bool success,
            string? errorMessage = null)
        {
            try
            {
                TelemetryEnvelope telemetry;

                if (success)
                {
                    telemetry = TelemetryFactory.CreateEhlResponseTelemetry(
                        cid: cid,
                        side: side,
                        actor: actor,
                        method: method,
                        address: _address,
                        price: State.Price,
                        volume: State.Volume,
                        isLocked: State.IsLocked,
                        isOpenForDelivery: State.IsOpenForDelivery
                    );
                }
                else
                {
                    telemetry = TelemetryFactory.CreateErrorTelemetry(
                        cid: cid,
                        side: side,
                        actor: actor,
                        method: method,
                        address: _address,
                        errorMessage: errorMessage ?? "Unknown error"
                    );
                }

                string messageString = JsonSerializer.Serialize(telemetry);
                var message = new Message(Encoding.UTF8.GetBytes(messageString));
                message.ContentType = "application/json";
                message.ContentEncoding = "utf-8";

                await _deviceClient?.SendEventAsync(message)!;
                Console.WriteLine($"[Telemetry] Method result sent: {method} (cid={cid}, success={success})");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[Telemetry] Failed to send method telemetry: {ex.Message}");
            }
        }
    }
}
