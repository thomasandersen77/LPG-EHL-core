using System;
using System.IO;
using System.Linq;
using System.Collections.Generic;
using PaymentTerminalNetsCloudMonoServer.Configuration;

namespace PaymentTerminalNetsCloudMonoServer.Persistence
{
    public class ReceiptStorage
    {
        private readonly string _basePath;
        private readonly int _retentionDays;
        private readonly int _retentionMaxOperations;
        private DateTime _lastCleanupUtc = DateTime.MinValue;

        public ReceiptStorage(ServerConfig config)
        {
            _basePath = Path.GetFullPath(config.ReceiptStoragePath);
            Directory.CreateDirectory(_basePath);
            _retentionDays = config.ReceiptRetentionDays;
            _retentionMaxOperations = config.ReceiptRetentionMaxOperations;
        }

        public string SaveReceipt(string operationId, string rawText, string sanitizedText)
        {
            var dateDir = DateTime.UtcNow.ToString("yyyy-MM-dd");
            var receiptDir = Path.Combine(_basePath, dateDir);
            Directory.CreateDirectory(receiptDir);

            var rawPath = Path.Combine(receiptDir, $"{operationId}.raw.txt");
            var sanitizedPath = Path.Combine(receiptDir, $"{operationId}.sanitized.txt");

            File.WriteAllText(rawPath, rawText ?? "", System.Text.Encoding.UTF8);
            File.WriteAllText(sanitizedPath, sanitizedText ?? "", System.Text.Encoding.UTF8);

            CleanupIfConfigured();

            return $"{dateDir}/{operationId}";
        }

        private void CleanupIfConfigured()
        {
            if (_retentionDays <= 0 && _retentionMaxOperations <= 0) return;
            if ((DateTime.UtcNow - _lastCleanupUtc) < TimeSpan.FromMinutes(5)) return;

            _lastCleanupUtc = DateTime.UtcNow;
            try { CleanupByDays(); } catch { }
            try { CleanupByMaxOperations(); } catch { }
        }

        private void CleanupByDays()
        {
            if (_retentionDays <= 0) return;
            var cutoff = DateTime.UtcNow.Date.AddDays(-_retentionDays);
            foreach (var dir in Directory.GetDirectories(_basePath))
            {
                var name = Path.GetFileName(dir);
                if (!DateTime.TryParseExact(name, "yyyy-MM-dd", null, System.Globalization.DateTimeStyles.None, out var d))
                    continue;
                if (d.Date < cutoff)
                {
                    try { Directory.Delete(dir, recursive: true); } catch { }
                }
            }
        }

        private void CleanupByMaxOperations()
        {
            if (_retentionMaxOperations <= 0) return;

            var rawFiles = Directory.GetFiles(_basePath, "*.raw.txt", SearchOption.AllDirectories);
            var entries = new List<(string operationId, DateTime lastWriteUtc, string dir)>();

            foreach (var raw in rawFiles)
            {
                var opId = Path.GetFileNameWithoutExtension(raw);
                if (opId != null && opId.EndsWith(".raw", StringComparison.OrdinalIgnoreCase))
                    opId = opId.Substring(0, opId.Length - 4);
                DateTime lw;
                try { lw = File.GetLastWriteTimeUtc(raw); } catch { lw = DateTime.MinValue; }
                entries.Add((opId ?? "", lw, Path.GetDirectoryName(raw) ?? ""));
            }

            var ordered = entries
                .Where(e => !string.IsNullOrWhiteSpace(e.operationId))
                .OrderByDescending(e => e.lastWriteUtc)
                .ToList();

            if (ordered.Count <= _retentionMaxOperations) return;

            var toDelete = ordered.Skip(_retentionMaxOperations).ToList();
            foreach (var e in toDelete)
            {
                try
                {
                    var rawPath = Path.Combine(e.dir, $"{e.operationId}.raw.txt");
                    var sanitizedPath = Path.Combine(e.dir, $"{e.operationId}.sanitized.txt");
                    if (File.Exists(rawPath)) File.Delete(rawPath);
                    if (File.Exists(sanitizedPath)) File.Delete(sanitizedPath);
                }
                catch { }
            }
        }
    }
}
