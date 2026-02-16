using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using System.Text.RegularExpressions;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public static class ReportParser
    {
        public static Dictionary<string, string> ParseReportFields(int adminCode, string sanitizedPrintText)
        {
            if (string.IsNullOrWhiteSpace(sanitizedPrintText))
                return new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

            var text = sanitizedPrintText.Replace("\r\n", "\n").Replace("\r", "\n");
            var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

            var mBax = Regex.Match(text, @"(?m)^\s*Bax:\s*(?<tid>\d+)\s*-\s*(?<mid>\d+)\s*$");
            if (mBax.Success)
            {
                map["reportTerminalId"] = mBax.Groups["tid"].Value;
                map["reportMerchantId"] = mBax.Groups["mid"].Value;
            }

            var mTs = Regex.Match(text, @"(?m)^\s*(?<ts>\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2})\s*$");
            if (mTs.Success)
                map["reportTimestampLocal"] = mTs.Groups["ts"].Value;

            var mCur = Regex.Match(text, @"(?m)^\s*Valuta:\s*(?<cur>[A-Z]{3})\s*$");
            if (mCur.Success)
                map["reportCurrency"] = mCur.Groups["cur"].Value;

            var mSess = Regex.Match(text, @"(?m)^\s*Sesjon\.\s*:\s*(?<sess>\d+)\s*$");
            if (mSess.Success)
                map["reportSessionNumber"] = mSess.Groups["sess"].Value.PadLeft(3, '0');

            var effectiveCode = adminCode > 10000 ? adminCode : adminCode;

            if (effectiveCode == 12599 || effectiveCode == 0x3137)
            {
                map["reportType"] = "z";
                var mZ = Regex.Match(text, @"(?m)^\s*Z-rapport:\s*(?<z>\d+)\s*$");
                if (mZ.Success)
                    map["zReportNumber"] = mZ.Groups["z"].Value.PadLeft(3, '0');
                var mLast = Regex.Match(text, @"(?ms)Siste\s+Z-Total\s*\n\s*(?<ts>\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2})");
                if (mLast.Success)
                    map["zLastTotalTimestampLocal"] = mLast.Groups["ts"].Value;
                var mCount = Regex.Match(text, @"(?m)^\s*Antall\s+(?<n>\d+)\s*$");
                if (mCount.Success)
                    map["batchTotalCount"] = mCount.Groups["n"].Value;
                var mTotal = Regex.Match(text, @"(?m)^\s*Total=\s*(?<amt>[\d\.\s]+,\d{2})\s*$");
                if (mTotal.Success)
                {
                    map["batchTotalAmount"] = NormalizeAmount(mTotal.Groups["amt"].Value);
                    var minor = TryParseNorwegianAmountMinor(mTotal.Groups["amt"].Value);
                    if (minor.HasValue)
                        map["batchTotalAmountMinor"] = minor.Value.ToString(CultureInfo.InvariantCulture);
                }
                foreach (Match sm in Regex.Matches(text, @"(?ms)^\s*(?<scheme>[A-Za-zÆØÅæøå0-9]+)\s+(?<cnt>\d+)\s*\n\s*Bel[øo]p=\s*(?<amt>[\d\.\s]+,\d{2})\s*$"))
                {
                    var scheme = NormalizeScheme(sm.Groups["scheme"].Value);
                    if (scheme.Length == 0) continue;
                    map[$"scheme_{scheme}_count"] = sm.Groups["cnt"].Value;
                    map[$"scheme_{scheme}_amount"] = NormalizeAmount(sm.Groups["amt"].Value);
                    var minor = TryParseNorwegianAmountMinor(sm.Groups["amt"].Value);
                    if (minor.HasValue)
                        map[$"scheme_{scheme}_amountMinor"] = minor.Value.ToString(CultureInfo.InvariantCulture);
                }
            }
            else if (effectiveCode == 12592 || effectiveCode == 0x3130)
            {
                map["reportType"] = "avstemming";
                var mCollected = Regex.Match(text, @"(?m)^\s*Innsamlet\s+(?<n>\d+)\s*$");
                if (mCollected.Success)
                    map["settlementCollectedCount"] = mCollected.Groups["n"].Value;
                var mTotal = Regex.Match(text, @"(?m)^\s*Total=\s*(?<amt>[\d\.\s]+,\d{2})\s*$");
                if (mTotal.Success)
                {
                    map["settlementTotalAmount"] = NormalizeAmount(mTotal.Groups["amt"].Value);
                    var minor = TryParseNorwegianAmountMinor(mTotal.Groups["amt"].Value);
                    if (minor.HasValue)
                        map["settlementTotalAmountMinor"] = minor.Value.ToString(CultureInfo.InvariantCulture);
                }
                foreach (Match sm in Regex.Matches(text, @"(?ms)^\s*(?<scheme>[A-Za-zÆØÅæøå0-9]+)\s+(?<cnt>\d+)\s*\n\s*Bel[øo]p=\s*(?<amt>[\d\.\s]+,\d{2})\s*$"))
                {
                    var scheme = NormalizeScheme(sm.Groups["scheme"].Value);
                    if (scheme.Length == 0) continue;
                    map[$"scheme_{scheme}_count"] = sm.Groups["cnt"].Value;
                    map[$"scheme_{scheme}_amount"] = NormalizeAmount(sm.Groups["amt"].Value);
                    var minor = TryParseNorwegianAmountMinor(sm.Groups["amt"].Value);
                    if (minor.HasValue)
                        map[$"scheme_{scheme}_amountMinor"] = minor.Value.ToString(CultureInfo.InvariantCulture);
                }
            }

            return map;
        }

        private static string NormalizeScheme(string s)
        {
            if (string.IsNullOrWhiteSpace(s)) return "";
            var sb = new StringBuilder();
            foreach (var ch in s.Trim())
            {
                if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9'))
                    sb.Append(ch);
            }
            return sb.ToString();
        }

        private static string NormalizeAmount(string raw)
        {
            if (string.IsNullOrWhiteSpace(raw)) return "";
            return raw.Trim().Replace(" ", "");
        }

        private static int? TryParseNorwegianAmountMinor(string raw)
        {
            if (string.IsNullOrWhiteSpace(raw)) return null;
            var s = raw.Trim().Replace(" ", "").Replace(".", "");
            var nfi = new NumberFormatInfo { NumberDecimalSeparator = ",", NumberGroupSeparator = " " };
            if (decimal.TryParse(s, NumberStyles.Number, nfi, out var d))
                return (int)decimal.Round(d * 100m, 0, MidpointRounding.AwayFromZero);
            return null;
        }
    }
}
