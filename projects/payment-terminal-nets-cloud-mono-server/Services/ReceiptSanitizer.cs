using System;
using System.Text.RegularExpressions;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public static class ReceiptSanitizer
    {
        private static readonly Regex PanFullRegex = new Regex(
            @"\b\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4}\b",
            RegexOptions.Compiled);

        public static string SanitizePrintText(string input)
        {
            if (string.IsNullOrEmpty(input)) return input;

            var s = input;
            s = s.Replace("\u001bN\u0001", "");
            s = s.Replace("\t", "");

            const string feedPrefix = "\u001b\u001e\u001b\f";
            while (true)
            {
                int idx = s.IndexOf(feedPrefix, StringComparison.Ordinal);
                if (idx < 0) break;
                int removeLen = Math.Min(5, s.Length - idx);
                s = s.Remove(idx, removeLen);
            }

            s = RedactPan(s);
            return s;
        }

        private static string RedactPan(string input)
        {
            return PanFullRegex.Replace(input, "**** **** **** ****");
        }
    }
}
