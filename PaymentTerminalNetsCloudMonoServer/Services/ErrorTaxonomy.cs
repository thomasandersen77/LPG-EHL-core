using System.Collections.Generic;
using PaymentTerminalNetsCloudMonoServer.Models;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public static class ErrorTaxonomy
    {
        public const int METHOD_REJECT_PROCESSING_PREVIOUS = 7100;
        public const int METHOD_REJECT_UNABLE_TO_PROCESS = 7101;
        public const int METHOD_REJECT_ALREADY_OPEN = 7102;
        public const int METHOD_REJECT_NOT_ACTIVE = 7103;
        public const int METHOD_REJECT_TERMINAL_BUSY_ADM = 7104;

        public static ErrorMapping MapMethodRejectCode(int methodRejectCode)
        {
            return MapConnectCloudMethodRejected(methodRejectCode);
        }

        public static ErrorMapping MapConnectCloudMethodRejected(int code)
        {
            switch (code)
            {
                case 7100:
                case 7104:
                    return new ErrorMapping(409, ErrorCodes.TERMINAL_BUSY);

                case 7103:
                    return new ErrorMapping(503, ErrorCodes.TERMINAL_NOT_READY);

                case 7102:
                    return new ErrorMapping(400, ErrorCodes.INVALID_REQUEST);

                case 7101:
                case 7506:
                case 7507:
                    return new ErrorMapping(422, ErrorCodes.OPERATION_REJECTED);

                default:
                    return new ErrorMapping(500, ErrorCodes.VENDOR_CALL_FAILURE);
            }
        }

        public static ErrorMapping MapDfs13Error(int errorCode)
        {
            switch (errorCode)
            {
                case 8013:
                    return new ErrorMapping(503, ErrorCodes.TERMINAL_NOT_READY);

                case 9100:
                case 9000:
                    return new ErrorMapping(500, ErrorCodes.VENDOR_CALL_FAILURE);

                default:
                    return new ErrorMapping(500, ErrorCodes.VENDOR_CALL_FAILURE);
            }
        }

        public static bool IsRetryableMethodReject(int methodRejectCode)
        {
            return methodRejectCode == 7104;
        }

        public struct ErrorMapping
        {
            public int HttpStatusCode { get; }
            public string ErrorCode { get; }

            public ErrorMapping(int httpStatusCode, string errorCode)
            {
                HttpStatusCode = httpStatusCode;
                ErrorCode = errorCode;
            }
        }
    }
}
