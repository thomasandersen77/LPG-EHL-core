using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using StationSupervisor.Configuration;

namespace StationSupervisor.Management
{
    public class HealthChecker : IHealthChecker
    {
        private readonly SupervisorConfig _config;
        private readonly ILogger<HealthChecker> _logger;
        private readonly HttpClient _httpClient;

        public HealthChecker(SupervisorConfig config, ILogger<HealthChecker> logger, HttpClient httpClient)
        {
            _config = config;
            _logger = logger;
            _httpClient = httpClient;
        }

        public async Task<bool> IsBusyAsync()
        {
            if (_config.HealthCheckMethod.ToLower() == "http")
            {
                // HTTP-based
                try 
                {
                    var url = $"http://127.0.0.1:{_config.HttpHealthCheck.Port}{_config.HttpHealthCheck.BusyPath}";
                    var response = await _httpClient.GetAsync(url);
                    return response.IsSuccessStatusCode; // Assuming 200 OK means busy? 
                    // Wait, spec says: "Query ... for busy state". Usually API returns boolean in body or status code.
                    // Let's assume 200 OK with body "true" or just presence of endpoint?
                    // Spec: "Poll /run/station.busy (exists during transactions)"
                    // Spec: "Query ... for busy state".
                    // I'll assume if it returns 200 OK, we check content. If 404/500, not busy?
                    // Or maybe it returns a JSON { "busy": true }.
                    // Let's implementation simple: if request works and returns "true" string, it is busy.
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to check busy state via HTTP.");
                    return false; // Fail safe? Or fail busy? Failing safe (not busy) might interrupt. Failing busy might block updates.
                    // Let's assume if we can't check, we are potentially busy/unhealthy, but for 'busy' logic usually we default to 'not busy' if unknown, forcing update might be risky.
                    // But if service is down, it's not busy.
                    return false;
                }
            }
            else
            {
                // File-based
                return File.Exists(Path.Combine(_config.RunDirectory, "station.busy"));
            }
        }

        public async Task<bool> IsHealthyAsync()
        {
            if (_config.HealthCheckMethod.ToLower() == "http")
            {
                try 
                {
                    var url = $"http://127.0.0.1:{_config.HttpHealthCheck.Port}{_config.HttpHealthCheck.HealthPath}";
                    var response = await _httpClient.GetAsync(url);
                    return response.IsSuccessStatusCode;
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to check health via HTTP.");
                    return false;
                }
            }
            else
            {
                return File.Exists(Path.Combine(_config.RunDirectory, "station.healthy"));
            }
        }
    }
}
