using System;

namespace StationSupervisor.Configuration
{
    public class SupervisorConfig
    {
        public string ManifestUrl { get; set; } = string.Empty;
        public string IotHubConnectionString { get; set; } = string.Empty;
        public string ScheduleTime { get; set; } = "03:00";
        public int DrainTimeoutSeconds { get; set; } = 60;
        public int StartupTimeoutSeconds { get; set; } = 180;
        public string HealthCheckMethod { get; set; } = "file"; // "file" or "http"
        public string StationAppServiceName { get; set; } = "station-app.service";
        public string StationAppRootPath { get; set; } = "/opt/station";
        public string RunDirectory { get; set; } = "/run";
        public int MaxUpdateAttemptsPerDay { get; set; } = 3;
        public string RetryCutoffTime { get; set; } = "06:00";
        
        public HttpHealthCheckConfig HttpHealthCheck { get; set; } = new HttpHealthCheckConfig();
    }

    public class HttpHealthCheckConfig
    {
        public int Port { get; set; } = 8080;
        public string BusyPath { get; set; } = "/internal/busy";
        public string HealthPath { get; set; } = "/internal/health";
    }
}
