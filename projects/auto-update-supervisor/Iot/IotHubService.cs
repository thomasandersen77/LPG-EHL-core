using System;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Azure.Devices.Client;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using StationSupervisor.Configuration;

namespace StationSupervisor.Iot
{
    public interface IIotHubService
    {
        Task ConnectAsync(CancellationToken cancellationToken);
        Task ReportUpdateStatusAsync(UpdateStatus status);
        event Func<UpdateSpec, Task> OnUpdateJsonReceived;
    }

    public class IotHubService : IIotHubService, IDisposable
    {
        private readonly SupervisorConfig _config;
        private readonly ILogger<IotHubService> _logger;
        private DeviceClient? _deviceClient;

        public event Func<UpdateSpec, Task>? OnUpdateJsonReceived;

        public IotHubService(SupervisorConfig config, ILogger<IotHubService> logger)
        {
            _config = config;
            _logger = logger;
        }

        public async Task ConnectAsync(CancellationToken cancellationToken)
        {
            if (string.IsNullOrEmpty(_config.IotHubConnectionString))
            {
                _logger.LogWarning("IoT Hub connection string is missing. Skipping IoT Hub connection.");
                return;
            }

            try
            {
                _deviceClient = DeviceClient.CreateFromConnectionString(_config.IotHubConnectionString, TransportType.Mqtt);
                await _deviceClient.OpenAsync(cancellationToken);
                _logger.LogInformation("Connected to IoT Hub.");

                await _deviceClient.SetDesiredPropertyUpdateCallbackAsync(OnDesiredPropertyChanged, null, cancellationToken);
                
                // Check initial twin state
                var twin = await _deviceClient.GetTwinAsync(cancellationToken);
                await OnDesiredPropertyChanged(twin.Properties.Desired, null);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to connect to IoT Hub.");
                // Retry logic could be added here or relied upon by the caller/Polly
            }
        }

        private async Task OnDesiredPropertyChanged(Microsoft.Azure.Devices.Shared.TwinCollection desiredProperties, object userContext)
        {
            _logger.LogInformation("Desired properties updated.");
            
            try 
            {
                // Access 'update' object directly if possible, or parse standard JSON
                // The SDK's TwinCollection is a bit dynamic
                
                JObject root = JObject.Parse(desiredProperties.ToJson());
                
                if (root.ContainsKey("update"))
                {
                    var updateToken = root["update"];
                    if (updateToken != null)
                    {
                        var updateSpec = updateToken.ToObject<UpdateSpec>();
                        if (updateSpec != null && OnUpdateJsonReceived != null)
                        {
                            await OnUpdateJsonReceived.Invoke(updateSpec);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error parsing desired properties.");
            }
        }

        public async Task ReportUpdateStatusAsync(UpdateStatus status)
        {
            if (_deviceClient == null) return;

            try
            {
                var reportedProperties = new Microsoft.Azure.Devices.Shared.TwinCollection();
                // We want to report 'updateStatus' object
                var statusJson = JsonConvert.SerializeObject(status);
                var statusObj = JObject.Parse(statusJson);
                
                // TwinCollection doesn't support nested objects easily via Add, 
                // typically we construct a JSON structure for it.
                // But let's try strict object mapping:
                reportedProperties["updateStatus"] = statusObj;

                await _deviceClient.UpdateReportedPropertiesAsync(reportedProperties);
                _logger.LogInformation("Reported update status: {Status}", status.State);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to report properties.");
            }
        }

        public void Dispose()
        {
            _deviceClient?.Dispose();
        }
    }
}
