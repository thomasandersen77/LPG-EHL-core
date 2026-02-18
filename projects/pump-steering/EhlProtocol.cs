namespace pump_steering
{
    public class EhlFrame(byte[] data)
    {
        public EhlFrame() : this(data: [])
        {
        }

        public byte Stx { get; init; }
        public byte Length { get; init; }
        public byte Addr { get; init; }
        public byte Cmd { get; init; }
        public byte[] Data { get; set; } = data;
        public byte Checksum { get; init; }
        public byte Etx { get; init; }

        public override string ToString()
        {
            string dataHex = Data.Length > 0 ? BitConverter.ToString(Data).Replace("-", " ") : "<none>";
            return $"STX=0x{Stx:X2} LEN={Length} ADDR={Addr} CMD=0x{Cmd:X2} DATA={dataHex} CHK=0x{Checksum:X2} ETX=0x{Etx:X2}";
        }
    }

    public enum ParseResult
    {
        Success,
        Incomplete,
        Invalid
    }

    public static class EhlProtocol
    {
        private const byte STX_CONTROLLER = 0x10;
        public const byte STX_DISPENSER = 0x20;
        private const byte ETX = 0x36;

        public const byte CMD_STATE = 0x4B;
        public const byte CMD_VOLUME = 0x45;
        public const byte CMD_PRICE = 0x5C;
        public const byte CMD_ERROR_QUERY = 0x4C;
        public const byte CMD_PRODUCT_SELECT = 0xC3;
        public const byte CMD_PROG_PRC = 0xA9;
        public const byte CMD_UNBLOCK = 0x77;
        public const byte CMD_BLOCK = 0x69;
        public const byte CMD_RESET = 0x81;

        private static byte ComputeChecksum(byte[] buffer, int length)
        {
            byte x = 0;
            // XOR from start (STX) up to the byte before checkum
            for (int i = 0; i < length; i++)
            {
                x ^= buffer[i];
            }
            return x;
        }

        public static byte[] BuildFrame(byte addr, byte cmd, byte[]? data = null)
        {
            data ??= [];
            
            // Frame structure: STX (1) + LEN (1) + ADDR (1) + CMD (1) + DATA (N) + CHK (1) + ETX (1)
            
            var frameLen = 6 + data.Length;
            if (frameLen > 255) throw new ArgumentException("Frame too long");

            byte[] frame = new byte[frameLen];
            frame[0] = STX_CONTROLLER;
            frame[1] = (byte)frameLen;
            frame[2] = addr;
            frame[3] = cmd;
            
            if (data.Length > 0)
            {
                Array.Copy(data, 0, frame, 4, data.Length);
            }

            // Checksum over indices 0 to (frameLen - 3) inclusive (so length - 2 bytes)
            frame[frameLen - 2] = ComputeChecksum(frame, frameLen - 2);
            frame[frameLen - 1] = ETX;

            return frame;
        }

        public static (ParseResult Result, EhlFrame Frame, int Consumed) ParseOneFrame(byte[] buffer, int offset, int count)
        {
            if (count < 6) return (ParseResult.Incomplete, null, 0)!;

            byte stx = buffer[offset];
            if (stx != STX_CONTROLLER && stx != STX_DISPENSER) return (ParseResult.Invalid, null, 0)!; 

            byte length = buffer[offset + 1];
            if (length < 6) return (ParseResult.Invalid, null, 0)!; // Invalid length byte
            if (count < length) return (ParseResult.Incomplete, null, 0)!; // Need more bytes

            if (buffer[offset + length - 1] != ETX) return (ParseResult.Invalid, null, 0)!; 

            byte checksum = buffer[offset + length - 2];
            
            // Validate checksum (XOR of 0 ... length-3)
            byte calculated = 0;
            for (int i = 0; i < length - 2; i++)
            {
                calculated ^= buffer[offset + i];
            }

            if (calculated != checksum) return (ParseResult.Invalid, null, 0)!;

            var frame = new EhlFrame
            {
                Stx = stx,
                Length = length,
                Addr = buffer[offset + 2],
                Cmd = buffer[offset + 3],
                Checksum = checksum,
                Etx = ETX
            };

            int dataLen = length - 6;
            frame.Data = new byte[dataLen];
            Array.Copy(buffer, offset + 4, frame.Data, 0, dataLen);

            return (ParseResult.Success, frame, length);
        }

        // Data Interpretation Helpers

        public static string? InterpretVolume(byte[]? data)
        {
            if (data == null || data.Length < 5) return null;
            try 
            {
                var chars = new char[6];
                chars[0] = (char)data[4];
                chars[1] = (char)data[3];
                chars[2] = (char)data[2];
                chars[3] = '.';
                chars[4] = (char)data[1];
                chars[5] = (char)data[0];
                return new string(chars);
            }
            catch { return null; }
        }

        public static string? InterpretPrice(byte[]? data)
        {
            if (data == null || data.Length < 4) return null;
            try
            {
                var chars = new char[5];
                chars[0] = (char)data[3];
                chars[1] = (char)data[2];
                chars[2] = '.';
                chars[3] = (char)data[1];
                chars[4] = (char)data[0];
                return new string(chars);
            }
            catch { return null; }
        }

        public static byte[] EncodePrice(string priceStr)
        {
             var p = priceStr.Trim();
             // Validate format XX.XX
             if (p.Length != 5 || p[2] != '.') throw new ArgumentException("Price must be XX.XX");
             var digits = p.Replace(".", "");
             return digits.Length != 4 ? throw new ArgumentException("Price must have 4 digits") : [(byte)digits[3], (byte)digits[2], (byte)digits[1], (byte)digits[0]];
        }

        public static (bool OpenForDelivery, bool StartButtonPressed, bool AutoMode) InterpretState(byte stateByte)
        {
            // bit1 (0x02) -> open
            // bit2 (0x04) -> start
            // bit3 (0x08) -> auto
            
            var open = (stateByte & 0x02) != 0;
            var start = (stateByte & 0x04) != 0;
            var auto = (stateByte & 0x08) != 0;
            return (open, start, auto);
        }
    }
}
