using System;
using System.IO;
using System.Text.RegularExpressions;
using Newtonsoft.Json;

namespace PaymentTerminalNetsCloudMonoServer.Configuration
{
    public class ServerConfig
    {
        public string BindAddress { get; set; } = "127.0.0.1";
        public int BindPort { get; set; } = 8080;
        public string DatabasePath { get; set; } = "./data/payment_terminal.db";
        public string ReceiptStoragePath { get; set; } = "./receipts";
        public int ReceiptRetentionDays { get; set; } = 0;
        public int ReceiptRetentionMaxOperations { get; set; } = 0;
        public int FinancialOperationTimeoutSeconds { get; set; } = 180;
        public int AdminOperationTimeoutSeconds { get; set; } = 300;
        public bool EnableDiagnostics { get; set; } = false;
        public BusyRetryConfig BusyRetry { get; set; } = new BusyRetryConfig();
        public string EventLogPath { get; set; } = null;
        public int PreAvstemmingPostDelayMs { get; set; } = 1500;
        public ConnectCloudConfig ConnectCloud { get; set; } = new ConnectCloudConfig();

        public class BusyRetryConfig
        {
            public bool Enabled { get; set; } = false;
            public int MaxRetrySeconds { get; set; } = 20;
            public int RetryDelayMs { get; set; } = 1000;
        }

        public class ConnectCloudConfig
        {
            public string Environment { get; set; } = "QA";
            public string BaseUrl { get; set; } = null;
            public string Username { get; set; } = null;
            public string Password { get; set; } = null;
            public string TerminalId { get; set; } = "12345678";
            public string EcrIdPrefix { get; set; } = "POS-";
            public string OperatorIdDefault { get; set; } = "4321";
            public string WebSocketPath { get; set; } = "/ws/json";
            public int LoginTimeoutSeconds { get; set; } = 12;
            public int OpenReadyTimeoutSeconds { get; set; } = 60;
            public int ReconnectBaseDelayMs { get; set; } = 1000;
            public int ReconnectMaxDelayMs { get; set; } = 30000;
        }

        public static ServerConfig LoadFromFile(string path)
        {
            if (string.IsNullOrWhiteSpace(path))
                path = "./server.json";

            if (File.Exists(path))
            {
                var json = File.ReadAllText(path);
                var config = JsonConvert.DeserializeObject<ServerConfig>(json);
                if (config != null)
                {
                    config.ExpandEnvVars();
                    return config;
                }
            }

            return new ServerConfig();
        }

        private void ExpandEnvVars()
        {
            var cc = ConnectCloud;
            if (cc == null) return;

            cc.Username = ExpandEnvVar(cc.Username);
            cc.Password = ExpandEnvVar(cc.Password);
        }

        private static string ExpandEnvVar(string value)
        {
            if (string.IsNullOrWhiteSpace(value)) return value;
            var match = Regex.Match(value, @"^\$([A-Za-z_][A-Za-z0-9_]*)$");
            if (!match.Success) return value;
            var envName = match.Groups[1].Value;
            var envVal = Environment.GetEnvironmentVariable(envName);
            if (string.IsNullOrEmpty(envVal))
                Console.WriteLine($"[{DateTime.Now:O}] WARNING: Environment variable {envName} not set or empty");
            return envVal ?? value;
        }

        public void Validate()
        {
            if (string.IsNullOrWhiteSpace(BindAddress))
                throw new InvalidOperationException("BindAddress is required");
            if (BindPort < 1 || BindPort > 65535)
                throw new InvalidOperationException("BindPort must be between 1 and 65535");
            if (string.IsNullOrWhiteSpace(DatabasePath))
                throw new InvalidOperationException("DatabasePath is required");
            if (string.IsNullOrWhiteSpace(ReceiptStoragePath))
                throw new InvalidOperationException("ReceiptStoragePath is required");
            if (ReceiptRetentionDays < 0)
                throw new InvalidOperationException("ReceiptRetentionDays must be >= 0");
            if (ReceiptRetentionMaxOperations < 0)
                throw new InvalidOperationException("ReceiptRetentionMaxOperations must be >= 0");

            var cc = ConnectCloud ?? throw new InvalidOperationException("connectCloud config is required");
            if (string.IsNullOrWhiteSpace(cc.TerminalId))
                throw new InvalidOperationException("connectCloud.terminalId is required");
            if (string.IsNullOrWhiteSpace(cc.Username) || string.IsNullOrWhiteSpace(cc.Password))
                Console.WriteLine($"[{DateTime.Now:O}] WARNING: Connect@Cloud credentials are empty. Use $CONNECTCLOUD_USERNAME and $CONNECTCLOUD_PASSWORD for env var indirection.");

            var dbDir = Path.GetDirectoryName(Path.GetFullPath(DatabasePath));
            if (!string.IsNullOrWhiteSpace(dbDir))
            {
                try { Directory.CreateDirectory(dbDir); }
                catch (Exception ex) { throw new InvalidOperationException($"Cannot create database directory {dbDir}: {ex.Message}", ex); }
            }

            try { Directory.CreateDirectory(ReceiptStoragePath); }
            catch (Exception ex) { throw new InvalidOperationException($"Cannot create receipt storage directory {ReceiptStoragePath}: {ex.Message}", ex); }

            if (!string.IsNullOrWhiteSpace(EventLogPath))
            {
                var dir = Path.GetDirectoryName(Path.GetFullPath(EventLogPath));
                if (!string.IsNullOrWhiteSpace(dir))
                {
                    try { Directory.CreateDirectory(dir); }
                    catch (Exception ex) { throw new InvalidOperationException($"Cannot create event log directory {dir}: {ex.Message}", ex); }
                }
            }
        }
    }
}
