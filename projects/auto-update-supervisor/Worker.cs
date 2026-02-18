using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using StationSupervisor.Configuration;
using StationSupervisor.Iot;
using StationSupervisor.Management;
using StationSupervisor.State;

namespace StationSupervisor
{
    public class Worker : BackgroundService
    {
        private readonly ILogger<Worker> _logger;
        private readonly SupervisorConfig _config;
        private readonly UpdateManager _updateManager;
        private readonly IIotHubService _iotHubService;
        private readonly StateManager _stateManager;

        public Worker(
            ILogger<Worker> logger,
            SupervisorConfig config,
            UpdateManager updateManager,
            IIotHubService iotHubService,
            StateManager stateManager)
        {
            _logger = logger;
            _config = config;
            _updateManager = updateManager;
            _iotHubService = iotHubService;
            _stateManager = stateManager;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Station Supervisor starting...");

            // Load state
            await _stateManager.LoadAsync();

            // Connect to IoT Hub
            try 
            {
                await _iotHubService.ConnectAsync(stoppingToken);
                _iotHubService.OnUpdateJsonReceived += OnIotUpdateReceived;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "IoT Hub initial connection failed.");
            }

            // Schedule loop
            while (!stoppingToken.IsCancellationRequested)
            {
                try 
                {
                    // Check scheduled time
                    // For simplicity, we just check every minute if it matches schedule?
                    // Or calculate delay until next 03:00.
                    // If parsing fails, default to 1 hour delay.
                    
                    var now = DateTime.Now;
                    if (TimeSpan.TryParse(_config.ScheduleTime, out var scheduleTime))
                    {
                        var scheduled = now.Date.Add(scheduleTime);
                        if (now > scheduled)
                        {
                            scheduled = scheduled.AddDays(1);
                        }
                        
                        var delay = scheduled - now;
                        _logger.LogInformation("Next scheduled check at {ScheduledTime} (in {Delay}).", scheduled, delay);
                        
                        await Task.Delay(delay, stoppingToken);
                        
                        _logger.LogInformation("Executing scheduled update check...");
                        await _updateManager.CheckForUpdatesAsync(stoppingToken);
                    }
                    else
                    {
                        _logger.LogWarning("Invalid schedule time format. Defaulting to 1 hour delay.");
                        await Task.Delay(TimeSpan.FromHours(1), stoppingToken);
                    }
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error in scheduler loop.");
                    await Task.Delay(TimeSpan.FromMinutes(5), stoppingToken);
                }
            }
        }

        private async Task OnIotUpdateReceived(UpdateSpec spec)
        {
            try
            {
                _logger.LogInformation("Received update request from IoT Hub: {Mode}, {Version}", spec.Mode, spec.TargetVersion);

                if (spec.Mode?.ToLower() == "force")
                {
                    await _updateManager.CheckForUpdatesAsync(CancellationToken.None);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error handling IoT Hub update.");
            }
        }

        public override void Dispose()
        {
            if (_iotHubService is IDisposable disposable)
            {
                disposable.Dispose();
            }
            base.Dispose();
        }
    }
}
