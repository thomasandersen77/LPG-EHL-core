using System;
using System.Collections.Generic;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public static class AdminCodeMapper
    {
        public const int AVSTEMMING = 0x3130;
        public const int CANCEL = 0x3132;
        public const int REVERSAL = 0x3134;
        public const int X_REPORT = 0x3136;
        public const int Z_REPORT = 0x3137;
        public const int LAST_RECEIPT = 0x313C;
        public const int LAST_RESULT = 0x313D;
        public const int SOFTWARE = 0x313E;
        public const int DATASET = 0x313F;

        private static readonly Dictionary<string, int> NameToCode = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase)
        {
            { "avstemming", AVSTEMMING },
            { "eod", AVSTEMMING },
            { "endofday", AVSTEMMING },
            { "cancel", CANCEL },
            { "reversal", REVERSAL },
            { "z", Z_REPORT },
            { "zrapport", Z_REPORT },
            { "x", X_REPORT },
            { "xreport", X_REPORT },
            { "lastreceipt", LAST_RECEIPT },
            { "lastfinancial", LAST_RECEIPT },
            { "lastresult", LAST_RESULT },
            { "software", SOFTWARE },
            { "sw", SOFTWARE },
            { "dataset", DATASET },
        };

        public static int? TryParseCode(string input)
        {
            if (string.IsNullOrWhiteSpace(input)) return null;
            input = input.Trim();

            if (input.StartsWith("0x", StringComparison.OrdinalIgnoreCase) || input.StartsWith("0X", StringComparison.OrdinalIgnoreCase))
                input = input.Substring(2);

            if (int.TryParse(input, System.Globalization.NumberStyles.HexNumber, null, out var hexCode))
                return hexCode;
            if (int.TryParse(input, out var decCode))
                return decCode;
            if (NameToCode.TryGetValue(input, out var code))
                return code;

            return null;
        }

        public static int ToConnectCloudDecimal(int baxiHexCode)
        {
            switch (baxiHexCode)
            {
                case 0x3130: return 12592;
                case 0x3132: return 12594;
                case 0x3134: return 12596;
                case 0x3136: return 12598;
                case 0x3137: return 12599;
                case 0x313C: return 12604;
                case 0x313D: return 12605;
                case 0x313E: return 12606;
                case 0x313F: return 12607;
                default: return baxiHexCode > 10000 ? baxiHexCode : 12592;
            }
        }

        public static string GetCodeName(int code)
        {
            if (code == AVSTEMMING || code == 12592) return "avstemming";
            if (code == CANCEL || code == 12594) return "cancel";
            if (code == REVERSAL || code == 12596) return "reversal";
            if (code == X_REPORT || code == 12598) return "x-report";
            if (code == Z_REPORT || code == 12599) return "z-report";
            if (code == LAST_RECEIPT || code == 12604) return "last-receipt";
            if (code == LAST_RESULT || code == 12605) return "last-result";
            if (code == SOFTWARE || code == 12606) return "software";
            if (code == DATASET || code == 12607) return "dataset";
            return code > 10000 ? $"adm_{code}" : $"0x{code:X4}";
        }
    }
}
