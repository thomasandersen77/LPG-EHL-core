using System;
using System.Collections.Generic;

namespace PaymentTerminalNetsCloudMonoServer.ConnectCloud
{
    public static class NetsMessageHeader
    {
        private static string _cachedEcrId;
        private static readonly object _ecrLock = new object();

        public static object BuildHeader(string ecrIdPrefix, string terminalId)
        {
            var ecrId = GetOrCreateEcrId(ecrIdPrefix);
            return new Dictionary<string, object>
            {
                ["$"] = new Dictionary<string, string>
                {
                    ["ECRID"] = ecrId,
                    ["TerminalID"] = terminalId,
                    ["VersionNumber"] = "1"
                }
            };
        }

        public static string GetOrCreateEcrId(string prefix)
        {
            lock (_ecrLock)
            {
                if (_cachedEcrId != null)
                    return _cachedEcrId;
                var p = string.IsNullOrWhiteSpace(prefix) ? "POS-" : prefix.TrimEnd('-') + "-";
                _cachedEcrId = p + DateTime.UtcNow.ToString("yyyyMMddHHmmss") + "-" + Guid.NewGuid().ToString("N").Substring(0, 8);
                return _cachedEcrId;
            }
        }
    }
}
