using System;
using System.IO;
using PaymentTerminalNetsCloudMonoServer.Configuration;
using PaymentTerminalNetsCloudMonoServer.ConnectCloud;
using PaymentTerminalNetsCloudMonoServer.Persistence;
using PaymentTerminalNetsCloudMonoServer.Services;

namespace PaymentTerminalNetsCloudMonoServer
{
    class Program
    {
        static int Main(string[] args)
        {
            try
            {
                try
                {
                    SQLitePCL.Batteries_V2.Init();
                }
                catch { }

                var configPath = args.Length > 0 ? args[0] : "./server.json";
                var config = ServerConfig.LoadFromFile(configPath);
                config.Validate();

                Console.WriteLine($"[{DateTime.Now:O}] Payment Terminal Nets Cloud Mono Server starting...");
                Console.WriteLine($"[{DateTime.Now:O}] Configuration loaded from: {configPath}");
                Console.WriteLine($"[{DateTime.Now:O}] Bind address: {config.BindAddress}:{config.BindPort}");
                Console.WriteLine($"[{DateTime.Now:O}] Database path: {config.DatabasePath}");
                Console.WriteLine($"[{DateTime.Now:O}] Receipt storage: {config.ReceiptStoragePath}");
                if (!string.IsNullOrWhiteSpace(config.EventLogPath))
                    Console.WriteLine($"[{DateTime.Now:O}] Event timeline log: {config.EventLogPath}");

                var database = new Database(config);
                var receiptStorage = new ReceiptStorage(config);
                var eventStore = new EventStore();

                var authClient = new ConnectCloudAuthClient();
                var wsClient = new ConnectCloudWebSocketClient(config.ConnectCloud, authClient);
                var adapter = new ConnectCloudAdapter(config.ConnectCloud, authClient, wsClient, config.EventLogPath);

                var operationLock = new OperationLock();
                var terminalService = new TerminalService(
                    adapter,
                    config,
                    operationLock,
                    database,
                    receiptStorage,
                    eventStore);

                var httpServer = new HttpServer(config, terminalService);
                httpServer.Start();

                Console.WriteLine($"[{DateTime.Now:O}] Server ready. Press Ctrl+C to stop.");

                var quitEvent = new System.Threading.ManualResetEvent(false);
                Console.CancelKeyPress += (s, e) =>
                {
                    e.Cancel = true;
                    try { Console.WriteLine($"[{DateTime.Now:O}] Ctrl+C received. Shutting down..."); } catch { }
                    new System.Threading.Thread(() =>
                    {
                        try { httpServer.Stop(); } catch { }
                        try { adapter.Close(); } catch { }
                        try { adapter.Dispose(); } catch { }
                        try { Environment.Exit(0); } catch { }
                    })
                    { IsBackground = true }.Start();
                    quitEvent.Set();
                };

                quitEvent.WaitOne();

                Console.WriteLine($"[{DateTime.Now:O}] Shutting down...");
                httpServer.Stop();
                adapter.Dispose();

                return 0;
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"[{DateTime.Now:O}] FATAL ERROR:\n{ex}");
                return 1;
            }
        }
    }
}
