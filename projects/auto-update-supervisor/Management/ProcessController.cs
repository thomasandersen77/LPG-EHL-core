using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;

namespace StationSupervisor.Management
{
    public class ProcessController : IProcessController
    {
        private readonly ILogger<ProcessController> _logger;

        public ProcessController(ILogger<ProcessController> logger)
        {
            _logger = logger;
        }

        public async Task<bool> StartServiceAsync(string serviceName, CancellationToken cancellationToken)
        {
            return await RunSystemCtlAsync("start", serviceName, cancellationToken);
        }

        public async Task<bool> StopServiceAsync(string serviceName, CancellationToken cancellationToken)
        {
            return await RunSystemCtlAsync("stop", serviceName, cancellationToken);
        }

        public async Task<bool> RestartServiceAsync(string serviceName, CancellationToken cancellationToken)
        {
            return await RunSystemCtlAsync("restart", serviceName, cancellationToken);
        }

        public async Task<bool> IsServiceRunningAsync(string serviceName, CancellationToken cancellationToken)
        {
            return await RunSystemCtlAsync("is-active", serviceName, cancellationToken);
        }

        private async Task<bool> RunSystemCtlAsync(string command, string serviceName, CancellationToken cancellationToken)
        {
            try
            {
                var psi = new ProcessStartInfo("systemctl", $"{command} {serviceName}")
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process == null) return false;

                await process.WaitForExitAsync(cancellationToken);
                return process.ExitCode == 0;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to execute systemctl {Command} {Service}", command, serviceName);
                return false;
            }
        }

        public bool DirectoryExists(string path) => Directory.Exists(path);
        public void CreateDirectory(string path) => Directory.CreateDirectory(path);
        public void DeleteDirectory(string path, bool recursive) => Directory.Delete(path, recursive);
        public void MoveDirectory(string source, string dest) => Directory.Move(source, dest);

        public void CreateSymlink(string target, string link)
        {
            // Use 'ln -sfn target link'
            try
            {
                var psi = new ProcessStartInfo("ln", $"-sfn {target} {link}")
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                
                using var process = Process.Start(psi);
                process?.WaitForExit();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to create symlink {Target} -> {Link}", target, link);
                throw;
            }
        }

        public string? ReadSymlink(string link)
        {
            try
            {
                var psi = new ProcessStartInfo("readlink", link)
                {
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process == null) return null;

                var output = process.StandardOutput.ReadToEnd().Trim();
                process.WaitForExit();

                if (process.ExitCode == 0 && !string.IsNullOrEmpty(output))
                {
                    return output;
                }
                return null;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to read symlink {Link}", link);
                return null;
            }
        }
    }
}
