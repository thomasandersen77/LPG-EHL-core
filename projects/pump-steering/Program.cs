using System.Text;
using System.Text.Json;
using Microsoft.Azure.Devices.Client;
using Microsoft.Extensions.Logging;

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
        private static bool _mock;
        private static ILogger? _log;

        public static async Task Main(string[] args)
        {
            using var loggerFactory = LoggerFactory.Create(builder =>
            {
                builder.AddConsole();
                builder.SetMinimumLevel(LogLevel.Information);
            });
            _log = loggerFactory.CreateLogger("pump_steering");

            var filtered = args.Where(a => a != "--mock").ToArray();
            _mock = filtered.Length < args.Length;

            // Resolve connection string: env var first, then first CLI argument
            _connectionString = Environment.GetEnvironmentVariable(ConfigValidation.ConnectionStringEnvVar);
            if (string.IsNullOrWhiteSpace(_connectionString) && filtered.Length >= 1)
                _connectionString = filtered[0];

            if (string.IsNullOrWhiteSpace(_connectionString))
            {
                _log.LogError("Missing connection string. Set {EnvVar} or pass as first argument. Usage: dotnet run -- [--mock] [device_connection_string] [serial_port] [ehl-serial-address]", ConfigValidation.ConnectionStringEnvVar);
                return;
            }

            if (!ConfigValidation.TryValidateConnectionString(_connectionString, out var validationError))
            {
                _log.LogError("Invalid connection string: {Message}", validationError);
                return;
            }

            if (filtered.Length >= 2) _serialPortName = filtered[1];
            if (filtered.Length >= 3 && int.TryParse(filtered[2], out int addr)) _address = addr;

            _log.LogInformation("Initializing Pump Steering IoT Client. Mock={Mock}, Serial Port: {Port}, Address: {Address}", _mock, _serialPortName, _address);

            // Init Serial (skip when mock)
            if (!_mock)
            {
                var serialConfig = new SerialConfig
                {
                    PortName = _serialPortName,
                    BaudRate = 9600,
                    Address = (byte)_address,
                    Logger = _log
                };
                _serial = new SerialService(serialConfig);
                try
                {
                    _serial.Open();
                }
                catch (Exception ex)
                {
                    _log.LogError(ex, "Could not open serial port: {Message}", ex.Message);
                    return;
                }
            }

            try
            {
                _deviceClient = DeviceClient.CreateFromConnectionString(_connectionString, TransportType.Mqtt);
                _deviceClient.SetConnectionStatusChangesHandler((status, reason) =>
                {
                    _log?.LogInformation("IoT Hub connection status: {Status}, reason: {Reason}", status, reason);
                    if (status == ConnectionStatus.Connected)
                    {
                        _ = RegisterMethodHandlersSafeAsync();
                    }
                });
                await _deviceClient.OpenAsync();
                _log.LogInformation("Connected to IoT Hub");

                // Register Direct Methods
                await RegisterMethodHandlersAsync();

                // Start background tasks
                var cts = new CancellationTokenSource();
                var pollingTask = PollPumpAsync(cts.Token);
                var telemetryTask = SendTelemetryAsync(cts.Token);

                // Graceful shutdown: Ctrl+C cancels and allows finally to run
                Console.CancelKeyPress += (_, e) =>
                {
                    e.Cancel = true;
                    cts.Cancel();
                };

                _log.LogInformation("Press Control+C to quit");
                try
                {
                    await Task.Delay(-1, cts.Token);
                }
                catch (OperationCanceledException) { /* expected when cancelled */ }

                // Let background tasks observe cancellation and exit
                try
                {
                    await Task.WhenAll(pollingTask, telemetryTask).WaitAsync(TimeSpan.FromSeconds(5));
                }
                catch (OperationCanceledException) { }
                catch (TimeoutException)
                {
                    _log.LogWarning("Background tasks did not exit within 5 seconds");
                }
            }
            catch (Exception ex)
            {
                _log.LogError(ex, "Error: {Message}", ex.Message);
            }
            finally
            {
                if (_deviceClient != null) await _deviceClient.CloseAsync();
                _serial?.Close();
            }
        }

        private static async Task RegisterMethodHandlersAsync()
        {
            if (_deviceClient == null) return;
            await _deviceClient.SetMethodHandlerAsync("SetPrice", SetPriceHandler, null);
            await _deviceClient.SetMethodHandlerAsync("Unlock", UnlockHandler, null);
            await _deviceClient.SetMethodHandlerAsync("Lock", LockHandler, null);
            await _deviceClient.SetMethodHandlerAsync("Reset", ResetHandler, null);
        }

        /// <summary>
        /// Re-registers direct method handlers with logging and one retry on failure (used from connection status callback).
        /// </summary>
        private static async Task RegisterMethodHandlersSafeAsync()
        {
            try
            {
                await RegisterMethodHandlersAsync().ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                _log?.LogWarning(ex, "Re-registering method handlers failed. Retrying once.");
                try
                {
                    await RegisterMethodHandlersAsync().ConfigureAwait(false);
                }
                catch (Exception ex2)
                {
                    _log?.LogError(ex2, "Re-registering method handlers failed after retry.");
                }
            }
        }

        private static async Task SendTelemetryWithRetryAsync(Message message, int maxRetries, CancellationToken ct)
        {
            if (_deviceClient == null) return;
            Exception? lastEx = null;
            for (int attempt = 1; attempt <= maxRetries; attempt++)
            {
                try
                {
                    await _deviceClient.SendEventAsync(message, ct);
                    return;
                }
                catch (Exception ex)
                {
                    lastEx = ex;
                    if (attempt < maxRetries)
                    {
                        _log?.LogWarning(ex, "Telemetry send attempt {Attempt}/{Max} failed. Retrying in 2s.", attempt, maxRetries);
                        await Task.Delay(2000, ct);
                    }
                }
            }
            if (lastEx != null) throw lastEx;
        }

        private static async Task PollPumpAsync(CancellationToken ct)
        {
            if (_mock) return; // No serial in mock mode

            // Polling loop: State -> Volume -> Price -> (Error?)
            while (!ct.IsCancellationRequested)
            {
                try
                {
                    // 1. Poll State
                    var stateInfo = _serial != null ? await _serial.PollStateAsync(ct).ConfigureAwait(false) : null;
                    if (stateInfo.HasValue)
                    {
                        var (open, start, auto) = stateInfo.Value;
                        State.IsOpenForDelivery = open;
                        State.IsLocked = !open;
                    }

                    // 2. Poll Volume
                    var volStr = _serial != null ? await _serial.PollVolumeAsync(ct).ConfigureAwait(false) : null;
                    if (volStr != null && decimal.TryParse(volStr, out decimal v))
                        State.Volume = v;

                    // 3. Poll Price
                    var priceStr = _serial != null ? await _serial.PollPriceAsync(ct).ConfigureAwait(false) : null;
                    if (priceStr != null && decimal.TryParse(priceStr, out decimal p))
                        State.Price = p;

                    await Task.Delay(200, ct).ConfigureAwait(false);
                }
                catch (Exception ex)
                {
                    _log?.LogWarning(ex, "Poll loop error: {Message}", ex.Message);
                    await Task.Delay(1000, ct);
                }
            }
        }

        private static async Task SendTelemetryAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested)
            {
                var telemetry = _mock
                    ? TelemetryFactory.CreateStateTelemetry(
                        address: _address,
                        price: 0.00m,
                        volume: 0.00m,
                        isLocked: false,
                        isOpenForDelivery: false)
                    : TelemetryFactory.CreateStateTelemetry(
                        address: _address,
                        price: State.Price,
                        volume: State.Volume,
                        isLocked: State.IsLocked,
                        isOpenForDelivery: State.IsOpenForDelivery);

                string messageString = JsonSerializer.Serialize(telemetry);
                var message = new Message(Encoding.UTF8.GetBytes(messageString));
                message.ContentType = "application/json";
                message.ContentEncoding = "utf-8";

                try
                {
                    await SendTelemetryWithRetryAsync(message, 2, ct);
                    _log?.LogDebug(_mock ? "Mock telemetry sent: {Payload}" : "Telemetry sent: {Payload}", messageString);
                }
                catch (Exception ex)
                {
                    _log?.LogWarning(ex, "Telemetry send failed after retries: {Message}", ex.Message);
                }

                await Task.Delay(10000, ct); // Send every 10 seconds
            }
        }

        // ===== Direct Method Handlers =====

        private static async Task<MethodResponse> SetPriceHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                _log?.LogInformation("SetPrice received: {Payload}", methodRequest.DataAsJson);

                // Parse envelope or legacy payload
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<SetPricePayload>(methodRequest.DataAsJson);

                // Validate CID (warn if generated)
                var (cid, wasGenerated) = ContractParser.ValidateOrGenerateCid(envelope.Cid);
                if (wasGenerated && !isLegacy)
                {
                    _log?.LogWarning("SetPrice: Invalid CID, generated new one: {Cid}", cid);
                }

                // Validate payload
                var priceValidation = ContractValidator.ValidatePrice(envelope.Payload.Price);
                if (!priceValidation.IsValid)
                {
                    var errorResponse = MethodResponseDto.Error(cid, priceValidation.ErrorCode, priceValidation.Message);

                    // Send error telemetry
                    await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "SetPrice",
                        success: false, errorMessage: priceValidation.Message);

                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        400);
                }

                // Format price
                var priceStr = envelope.Payload.Price.ToString("00.00", System.Globalization.CultureInfo.InvariantCulture);

                // Execute serial command (or simulate in mock)
                bool success = _mock || (_serial != null && await _serial.SetPriceAsync(priceStr).ConfigureAwait(false));
                if (_mock)
                    _log?.LogInformation("Mock SetPrice would send: {Price}", priceStr);

                // Send telemetry (success or failure)
                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "SetPrice",
                    success: success, errorMessage: success ? null : "Serial command failed");

                if (success)
                {
                    var response = MethodResponseDto.Success(cid, "Price updated", new { new_price = envelope.Payload.Price });
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = MethodResponseDto.Error(cid, ErrorCodes.SerialCommandFailed, "Serial command failed");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                _log?.LogError(ex, "SetPrice exception: {Message}", ex.Message);
                var errorResponse = MethodResponseDto.Error(Guid.NewGuid().ToString(), ErrorCodes.Unknown, ex.Message);
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> UnlockHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                _log?.LogInformation("Unlock received: {Payload}", methodRequest.DataAsJson);
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _mock || (_serial != null && await _serial.UnlockAsync().ConfigureAwait(false));
                if (_mock) _log?.LogInformation("Mock Unlock would be sent");

                if (success)
                {
                    State.IsLocked = false; // Optimistic update
                }

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Unlock",
                    success: success, errorMessage: success ? null : "Unlock command failed");

                if (success)
                {
                    var response = MethodResponseDto.Success(cid, "Pump unlocked command sent");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = MethodResponseDto.Error(cid, ErrorCodes.SerialCommandFailed, "Unlock command failed");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                _log?.LogError(ex, "Unlock exception: {Message}", ex.Message);
                var errorResponse = MethodResponseDto.Error(Guid.NewGuid().ToString(), ErrorCodes.Unknown, ex.Message);
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> LockHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                _log?.LogInformation("Lock received: {Payload}", methodRequest.DataAsJson);
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _mock || (_serial != null && await _serial.LockAsync().ConfigureAwait(false));
                if (_mock) _log?.LogInformation("Mock Lock would be sent");

                if (success)
                {
                    State.IsLocked = true; // Optimistic update
                }

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Lock",
                    success: success, errorMessage: success ? null : "Lock command failed");

                if (success)
                {
                    var response = MethodResponseDto.Success(cid, "Pump locked command sent");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = MethodResponseDto.Error(cid, ErrorCodes.SerialCommandFailed, "Lock command failed");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                _log?.LogError(ex, "Lock exception: {Message}", ex.Message);
                var errorResponse = MethodResponseDto.Error(Guid.NewGuid().ToString(), ErrorCodes.Unknown, ex.Message);
                return new MethodResponse(
                    Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                    500);
            }
        }

        private static async Task<MethodResponse> ResetHandler(MethodRequest methodRequest, object userContext)
        {
            try
            {
                _log?.LogInformation("Reset received: {Payload}", methodRequest.DataAsJson);
                var (envelope, isLegacy) = ContractParser.ParseMethodRequest<EmptyPayload>(methodRequest.DataAsJson);
                var (cid, _) = ContractParser.ValidateOrGenerateCid(envelope.Cid);

                bool success = _mock || (_serial != null && await _serial.ResetAsync().ConfigureAwait(false));
                if (_mock) _log?.LogInformation("Mock Reset would be sent");

                await SendMethodTelemetryAsync(cid, envelope.Side, envelope.Actor, "Reset",
                    success: success, errorMessage: success ? null : "Reset command failed");

                if (success)
                {
                    var response = MethodResponseDto.Success(cid, "Pump reset command sent");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response)),
                        200);
                }
                else
                {
                    var errorResponse = MethodResponseDto.Error(cid, ErrorCodes.SerialCommandFailed, "Reset command failed");
                    return new MethodResponse(
                        Encoding.UTF8.GetBytes(JsonSerializer.Serialize(errorResponse)),
                        500);
                }
            }
            catch (Exception ex)
            {
                _log?.LogError(ex, "Reset exception: {Message}", ex.Message);
                var errorResponse = MethodResponseDto.Error(Guid.NewGuid().ToString(), ErrorCodes.Unknown, ex.Message);
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

                if (_deviceClient != null)
                    await SendTelemetryWithRetryAsync(message, 2, CancellationToken.None);
                _log?.LogDebug("Method telemetry sent: {Method} cid={Cid} success={Success}", method, cid, success);
            }
            catch (Exception ex)
            {
                _log?.LogWarning(ex, "Failed to send method telemetry: {Message}", ex.Message);
            }
        }
    }
}
