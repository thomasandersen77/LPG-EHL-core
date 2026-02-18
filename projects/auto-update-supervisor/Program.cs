using StationSupervisor;
using StationSupervisor.Configuration;
using StationSupervisor.Iot;
using StationSupervisor.Management;
using StationSupervisor.State;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using System;
using System.IO;

var builder = Host.CreateApplicationBuilder(args);

// Configuration
string configPath = Path.Combine(AppContext.BaseDirectory, "config.yaml"); 
// Or /etc/station-supervisor/config.yaml for production
if (File.Exists("/etc/station-supervisor/config.yaml"))
{
    configPath = "/etc/station-supervisor/config.yaml";
}

var configLoader = new ConfigLoader(configPath);
SupervisorConfig config;
try
{
    config = configLoader.Load();
}
catch (Exception ex)
{
    Console.WriteLine($"Failed to load configuration: {ex.Message}. Using defaults.");
    config = new SupervisorConfig();
}

builder.Services.AddSingleton(config);

// State
string statePath = "/var/lib/station-supervisor/state.json";
// Ensure state directory exists or use local for dev
if (!Directory.Exists("/var/lib/station-supervisor"))
{
    // Use local if not root/unable to write
    statePath = Path.Combine(AppContext.BaseDirectory, "state.json");
}

builder.Services.AddSingleton(new StateManager(statePath));

// Services
builder.Services.AddHttpClient();
builder.Services.AddSingleton<IIotHubService, IotHubService>();
builder.Services.AddSingleton<IProcessController, ProcessController>();
builder.Services.AddSingleton<IHealthChecker, HealthChecker>();
builder.Services.AddSingleton<UpdateManager>();

builder.Services.AddHostedService<Worker>();
builder.Services.AddSystemd();

var host = builder.Build();
host.Run();
