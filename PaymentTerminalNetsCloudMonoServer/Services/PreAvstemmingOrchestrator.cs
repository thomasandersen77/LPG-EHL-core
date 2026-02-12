using System;
using System.Threading;
using PaymentTerminalNetsCloudMonoServer.Configuration;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public class PreAvstemmingOrchestrator
    {
        private readonly IConnectCloudAdapter _adapter;
        private readonly ServerConfig _config;

        public PreAvstemmingOrchestrator(IConnectCloudAdapter adapter, ServerConfig config)
        {
            _adapter = adapter ?? throw new ArgumentNullException(nameof(adapter));
            _config = config ?? throw new ArgumentNullException(nameof(config));
        }

        public bool RunPreAvstemmingIfRequested(bool requested, string password = "0000", TimeSpan? timeout = null)
        {
            if (!requested) return true;

            var actualTimeout = timeout ?? TimeSpan.FromSeconds(_config.AdminOperationTimeoutSeconds);

            Console.WriteLine($"[{DateTime.Now:O}] Pre-pay avstemming: starting (admin 12592), timeout={actualTimeout.TotalSeconds:0}s");
            var cap = _adapter.RunAdministrationAndCaptureReport(12592, password, actualTimeout);

            if (cap.TimedOut)
            {
                Console.Error.WriteLine($"[{DateTime.Now:O}] Pre-pay avstemming: TIMEOUT. Error={cap.Error ?? "(none)"}");
                return false;
            }

            if (cap.CallResult != 1)
            {
                Console.Error.WriteLine($"[{DateTime.Now:O}] Pre-pay avstemming: vendor call failed (result={cap.CallResult}). Error={cap.Error ?? "(none)"}");
                return false;
            }

            if (cap.LocalModeResult != 1)
            {
                Console.Error.WriteLine($"[{DateTime.Now:O}] Pre-pay avstemming: LocalMode.Result={cap.LocalModeResult}. Error={cap.Error ?? "(none)"}");
                return false;
            }

            Console.WriteLine($"[{DateTime.Now:O}] Pre-pay avstemming: done");

            var delayMs = Math.Max(0, _config.PreAvstemmingPostDelayMs);
            if (delayMs > 0)
                Thread.Sleep(delayMs);

            return true;
        }
    }
}
