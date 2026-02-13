using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using Newtonsoft.Json;
using PaymentTerminalNetsCloudMonoServer.Configuration;
using PaymentTerminalNetsCloudMonoServer.Models;
using PaymentTerminalNetsCloudMonoServer.Persistence;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public class TerminalService : ITerminalService
    {
        private readonly IConnectCloudAdapter _adapter;
        private readonly ServerConfig _config;
        private readonly OperationLock _operationLock;
        private readonly PreAvstemmingOrchestrator _preAvstemming;
        private readonly Database _database;
        private readonly ReceiptStorage _receiptStorage;
        private readonly EventStore _eventStore;
        private bool _terminalOpen;
        private bool _terminalReady;
        private string _lastError;

        public TerminalService(
            IConnectCloudAdapter adapter,
            ServerConfig config,
            OperationLock operationLock,
            Database database,
            ReceiptStorage receiptStorage,
            EventStore eventStore)
        {
            _adapter = adapter ?? throw new ArgumentNullException(nameof(adapter));
            _config = config ?? throw new ArgumentNullException(nameof(config));
            _operationLock = operationLock ?? throw new ArgumentNullException(nameof(operationLock));
            _database = database ?? throw new ArgumentNullException(nameof(database));
            _receiptStorage = receiptStorage ?? throw new ArgumentNullException(nameof(receiptStorage));
            _eventStore = eventStore ?? throw new ArgumentNullException(nameof(eventStore));
            _preAvstemming = new PreAvstemmingOrchestrator(_adapter, _config);
        }

        public TerminalStatusResponse GetStatus()
        {
            _terminalReady = _adapter.IsTerminalReady();
            _lastError = _adapter.GetLastError();

            var identity = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
            {
                ["TerminalID"] = _config.ConnectCloud?.TerminalId ?? ""
            };

            return new TerminalStatusResponse
            {
                VendorDllLoadable = _adapter != null,
                TerminalOpen = _terminalOpen,
                TerminalReady = _terminalReady,
                ConnectionState = _adapter.GetConnectionState(),
                LastError = _lastError,
                TerminalIdentity = identity
            };
        }

        public SimpleResponse Open()
        {
            try
            {
                var result = _adapter.Open();
                if (result == 1)
                {
                    _terminalOpen = true;
                    _terminalReady = _adapter.IsTerminalReady();
                    return new SimpleResponse { Success = true, Message = "Terminal opened" };
                }
                return new SimpleResponse { Success = false, Error = _adapter.GetLastError() ?? "Open failed" };
            }
            catch (Exception ex)
            {
                return new SimpleResponse { Success = false, Error = ex.Message };
            }
        }

        public SimpleResponse Close()
        {
            try
            {
                var result = _adapter.Close();
                _terminalOpen = false;
                _terminalReady = false;
                return new SimpleResponse { Success = result == 1, Message = "Terminal closed" };
            }
            catch (Exception ex)
            {
                return new SimpleResponse { Success = false, Error = ex.Message };
            }
        }

        public OperationResponse Purchase(PurchaseRequest request)
        {
            if (!_operationLock.TryAcquire(out var operationId))
                return CreateErrorResponse(ErrorCodes.TERMINAL_BUSY, "Terminal is busy with another operation");

            try
            {
                if (!_adapter.IsTerminalReady())
                {
                    // Attempt to (re)open if WS dropped between calls.
                    _adapter.Open();
                    if (!_adapter.IsTerminalReady())
                        return CreateErrorResponse(ErrorCodes.TERMINAL_NOT_READY, _adapter.GetLastError() ?? "Terminal is not ready");
                }

                var startedAt = DateTime.UtcNow;
                var clientRequestId = request?.ClientRequestId;

                if (!string.IsNullOrWhiteSpace(clientRequestId))
                {
                    var existing = _database.GetOperationByClientRequestId(clientRequestId);
                    if (existing != null) return existing;
                }

                _eventStore.AddEvent(operationId, "OperationStarted", new { operationType = "purchase", startedAt = startedAt.ToString("O") });

                if (request?.PreAvstemming?.Enabled == true)
                {
                    var timeout = request.PreAvstemming.TimeoutSeconds.HasValue
                        ? TimeSpan.FromSeconds(request.PreAvstemming.TimeoutSeconds.Value)
                        : (TimeSpan?)null;
                    if (!_preAvstemming.RunPreAvstemmingIfRequested(true, request.PreAvstemming.Password, timeout))
                        return CreateErrorResponse(ErrorCodes.VENDOR_CALL_FAILURE, "Pre-avstemming failed");
                }

                var timeoutDuration = TimeSpan.FromSeconds(_config.FinancialOperationTimeoutSeconds);
                var captured = RunTransferAmountWithBusyRetry(
                    () => _adapter.RunTransferAmountAndWaitForLocalMode(
                        request?.OperatorId ?? _config.ConnectCloud?.OperatorIdDefault ?? "4321",
                        request.AmountMinor, 48, 0, 48, 0, 48,
                        request?.OptionalData,
                        timeoutDuration,
                        capturePrintText: true));

                var response = CreateOperationResponse(operationId, startedAt, "purchase", captured, clientRequestId);
                _eventStore.AddEvent(operationId, response.ErrorCode == ErrorCodes.OPERATION_TIMEOUT ? "OperationTimeout" : "OperationCompleted",
                    new { operationType = "purchase", completedAt = DateTime.UtcNow.ToString("O"), success = response.Success });
                if (!string.IsNullOrWhiteSpace(response.Error)) _lastError = response.Error;
                return response;
            }
            finally
            {
                _operationLock.Release(operationId);
            }
        }

        public OperationResponse Refund(RefundRequest request)
        {
            if (!_operationLock.TryAcquire(out var operationId))
                return CreateErrorResponse(ErrorCodes.TERMINAL_BUSY, "Terminal is busy with another operation");

            try
            {
                if (!_adapter.IsTerminalReady())
                {
                    _adapter.Open();
                    if (!_adapter.IsTerminalReady())
                        return CreateErrorResponse(ErrorCodes.TERMINAL_NOT_READY, _adapter.GetLastError() ?? "Terminal is not ready");
                }

                var startedAt = DateTime.UtcNow;
                var clientRequestId = request?.ClientRequestId;

                if (!string.IsNullOrWhiteSpace(clientRequestId))
                {
                    var existing = _database.GetOperationByClientRequestId(clientRequestId);
                    if (existing != null) return existing;
                }

                if (request?.PreAvstemming?.Enabled == true)
                {
                    var timeout = request.PreAvstemming.TimeoutSeconds.HasValue
                        ? TimeSpan.FromSeconds(request.PreAvstemming.TimeoutSeconds.Value)
                        : (TimeSpan?)null;
                    if (!_preAvstemming.RunPreAvstemmingIfRequested(true, request.PreAvstemming.Password, timeout))
                        return CreateErrorResponse(ErrorCodes.VENDOR_CALL_FAILURE, "Pre-avstemming failed");
                }

                _eventStore.AddEvent(operationId, "OperationStarted", new { operationType = "refund", startedAt = startedAt.ToString("O") });

                var timeoutDuration = TimeSpan.FromSeconds(_config.FinancialOperationTimeoutSeconds);
                var captured = RunTransferAmountWithBusyRetry(
                    () => _adapter.RunTransferAmountAndWaitForLocalMode(
                        request?.OperatorId ?? _config.ConnectCloud?.OperatorIdDefault ?? "4321",
                        request.AmountMinor, 49, 0, 48, 0, 48,
                        request?.OptionalData,
                        timeoutDuration,
                        capturePrintText: true));

                var response = CreateOperationResponse(operationId, startedAt, "refund", captured, clientRequestId);
                _eventStore.AddEvent(operationId, response.ErrorCode == ErrorCodes.OPERATION_TIMEOUT ? "OperationTimeout" : "OperationCompleted",
                    new { operationType = "refund", completedAt = DateTime.UtcNow.ToString("O"), success = response.Success });
                if (!string.IsNullOrWhiteSpace(response.Error)) _lastError = response.Error;
                return response;
            }
            finally
            {
                _operationLock.Release(operationId);
            }
        }

        public OperationResponse Cashback(CashbackRequest request)
        {
            if (!_operationLock.TryAcquire(out var operationId))
                return CreateErrorResponse(ErrorCodes.TERMINAL_BUSY, "Terminal is busy with another operation");

            try
            {
                if (!_adapter.IsTerminalReady())
                {
                    _adapter.Open();
                    if (!_adapter.IsTerminalReady())
                        return CreateErrorResponse(ErrorCodes.TERMINAL_NOT_READY, _adapter.GetLastError() ?? "Terminal is not ready");
                }

                var startedAt = DateTime.UtcNow;
                var total = request.PurchaseMinor + request.CashbackMinor;
                var clientRequestId = request?.ClientRequestId;

                if (!string.IsNullOrWhiteSpace(clientRequestId))
                {
                    var existing = _database.GetOperationByClientRequestId(clientRequestId);
                    if (existing != null) return existing;
                }

                _eventStore.AddEvent(operationId, "OperationStarted", new { operationType = "cashback", startedAt = startedAt.ToString("O") });

                var timeoutDuration = TimeSpan.FromSeconds(_config.FinancialOperationTimeoutSeconds);
                var captured = _adapter.RunTransferAmountAndWaitForLocalMode(
                    request?.OperatorId ?? _config.ConnectCloud?.OperatorIdDefault ?? "4321",
                    total, 51, request.CashbackMinor, 48, 0, 48,
                    request?.OptionalData,
                    timeoutDuration,
                    true);

                var response = CreateOperationResponse(operationId, startedAt, "cashback", captured, clientRequestId);
                _eventStore.AddEvent(operationId, response.ErrorCode == ErrorCodes.OPERATION_TIMEOUT ? "OperationTimeout" : "OperationCompleted",
                    new { operationType = "cashback", completedAt = DateTime.UtcNow.ToString("O"), success = response.Success });
                if (!string.IsNullOrWhiteSpace(response.Error)) _lastError = response.Error;
                return response;
            }
            finally
            {
                _operationLock.Release(operationId);
            }
        }

        public OperationResponse RunAdmin(int adminCode, string password)
        {
            if (!_operationLock.TryAcquire(out var operationId))
                return CreateErrorResponse(ErrorCodes.TERMINAL_BUSY, "Terminal is busy with another operation");

            try
            {
                if (!_adapter.IsTerminalReady())
                {
                    _adapter.Open();
                    if (!_adapter.IsTerminalReady())
                        return CreateErrorResponse(ErrorCodes.TERMINAL_NOT_READY, _adapter.GetLastError() ?? "Terminal is not ready");
                }

                var startedAt = DateTime.UtcNow;
                var timeoutDuration = TimeSpan.FromSeconds(_config.AdminOperationTimeoutSeconds);

                _eventStore.AddEvent(operationId, "OperationStarted", new { operationType = $"admin_{AdminCodeMapper.GetCodeName(adminCode)}", startedAt = startedAt.ToString("O") });

                var captured = _adapter.RunAdministrationAndCaptureReport(adminCode, password ?? "0000", timeoutDuration);

                Dictionary<string, string> reportFields = null;
                if (!string.IsNullOrWhiteSpace(captured.SanitizedPrintText))
                    reportFields = ReportParser.ParseReportFields(adminCode, captured.SanitizedPrintText);

                var response = CreateOperationResponseAdmin(operationId, startedAt, $"admin_{AdminCodeMapper.GetCodeName(adminCode)}", captured, null, reportFields);
                response.ReportFields = reportFields;

                _eventStore.AddEvent(operationId, response.ErrorCode == ErrorCodes.OPERATION_TIMEOUT ? "OperationTimeout" : "OperationCompleted",
                    new { operationType = $"admin_{AdminCodeMapper.GetCodeName(adminCode)}", completedAt = DateTime.UtcNow.ToString("O"), success = response.Success });
                if (!string.IsNullOrWhiteSpace(response.Error)) _lastError = response.Error;

                return response;
            }
            finally
            {
                _operationLock.Release(operationId);
            }
        }

        public List<EventStore.EventEnvelope> GetEvents(string since)
        {
            return _eventStore.GetEvents(since);
        }

        public object GetConnectCloudSchema()
        {
            return new { message = "Connect@Cloud adapter; use /v1/diag/sendjson or sendtld for diagnostics" };
        }

        public int SendJson(string json)
        {
            return _adapter.SendJson(json);
        }

        public int SendTld(string tldType, byte[] tldData)
        {
            return _adapter.SendTld(tldType, tldData);
        }

        private OperationResponse CreateOperationResponse(string operationId, DateTime startedAt, string operationType, CapturedLocalMode captured, string clientRequestId)
        {
            return CreateOperationResponse(operationId, startedAt, operationType, captured, clientRequestId, null);
        }

        private OperationResponse CreateOperationResponse(string operationId, DateTime startedAt, string operationType, CapturedLocalMode captured, string clientRequestId, Dictionary<string, string> reportFields)
        {
            var completedAt = DateTime.UtcNow;
            var durationMs = (int)(completedAt - startedAt).TotalMilliseconds;

            var sanitizedPrintText = string.IsNullOrWhiteSpace(captured?.RawPrintText)
                ? captured?.RawPrintText
                : ReceiptSanitizer.SanitizePrintText(captured.RawPrintText);

            string entryModeCode = "";
            string entryMode = "";
            try
            {
                var fields = ParseLocalModeResultDataFields(captured?.LocalModeResultData);
                if (fields != null && fields.Length > 2)
                {
                    entryModeCode = (fields[2] ?? "").Trim();
                    entryMode = MapEntryMode(entryModeCode);
                }
            }
            catch { }

            string receiptFileId = null;
            if (!string.IsNullOrWhiteSpace(captured.RawPrintText))
                receiptFileId = _receiptStorage.SaveReceipt(operationId, captured.RawPrintText, sanitizedPrintText);

            var localModeFieldsJson = captured.LocalModeFields == null ? null : JsonConvert.SerializeObject(captured.LocalModeFields);
            var reportFieldsJson = reportFields == null ? null : JsonConvert.SerializeObject(reportFields);

            var success = captured.CallResult == 1 && !captured.TimedOut &&
                          (captured.LocalModeResult == 0 || captured.LocalModeResponseCode == "00");

            var errorCode = captured.TimedOut ? ErrorCodes.OPERATION_TIMEOUT :
                           captured.CallResult != 1 ? ErrorTaxonomy.MapMethodRejectCode(captured.MethodRejectCode).ErrorCode : null;

            var error = captured.Error;
            if (!success && string.IsNullOrWhiteSpace(errorCode))
            {
                errorCode = ErrorCodes.OPERATION_REJECTED;
                if (string.IsNullOrWhiteSpace(error))
                    error = ("Rejected by terminal. Display='" + (captured.LastDisplayText ?? "").Trim() + "' RejectionSource=" + (captured.LocalModeRejectionSource ?? "") + " RejectionReason=" + (captured.LocalModeRejectionReason ?? "") + " ResponseCode=" + (captured.LocalModeResponseCode ?? "")).Trim();
            }

            var dbRowId = _database.SaveOperation(new OperationRecord
            {
                OperationId = operationId,
                ClientRequestId = clientRequestId,
                OperationType = operationType,
                StartedAt = startedAt,
                CompletedAt = completedAt,
                DurationMs = durationMs,
                CallResult = captured.CallResult,
                MethodRejectCode = captured.MethodRejectCode,
                MethodRejectInfo = captured.MethodRejectInfo,
                ResultEventName = captured.ResultEventName,
                LocalModeResult = captured.LocalModeResult,
                ResponseCode = captured.LocalModeResponseCode,
                RejectionSource = captured.LocalModeRejectionSource,
                RejectionReason = captured.LocalModeRejectionReason,
                PrintTextRaw = captured.RawPrintText,
                PrintTextSanitized = sanitizedPrintText,
                DisplayText = captured.LastDisplayText,
                Error = error,
                ErrorCode = errorCode,
                ReceiptFileId = receiptFileId,
                LocalModeFieldsJson = localModeFieldsJson,
                ReportFieldsJson = reportFieldsJson
            });

            return new OperationResponse
            {
                Success = success,
                OperationId = operationId,
                StartedAt = startedAt,
                CompletedAt = completedAt,
                DurationMs = durationMs,
                CallResult = captured.CallResult,
                MethodRejectCode = captured.MethodRejectCode,
                MethodRejectInfo = captured.MethodRejectInfo,
                ResultEventName = captured.ResultEventName,
                LocalModeResult = captured.LocalModeResult,
                ResponseCode = captured.LocalModeResponseCode,
                RejectionSource = captured.LocalModeRejectionSource,
                RejectionReason = captured.LocalModeRejectionReason,
                LocalModeResultData = captured.LocalModeResultData,
                LocalModeFields = captured.LocalModeFields,
                PrintTextRaw = captured.RawPrintText,
                PrintTextSanitized = sanitizedPrintText,
                LastDisplayText = captured.LastDisplayText,
                EntryMode = entryMode,
                EntryModeCode = entryModeCode,
                Error = error,
                ErrorCode = errorCode,
                DbRowId = dbRowId,
                ReceiptFileId = receiptFileId
            };
        }

        private OperationResponse CreateOperationResponseAdmin(string operationId, DateTime startedAt, string operationType, CapturedReport captured, string clientRequestId, Dictionary<string, string> reportFields)
        {
            var completedAt = DateTime.UtcNow;
            var durationMs = (int)(completedAt - startedAt).TotalMilliseconds;

            string receiptFileId = null;
            if (!string.IsNullOrWhiteSpace(captured.RawPrintText))
                receiptFileId = _receiptStorage.SaveReceipt(operationId, captured.RawPrintText, captured.SanitizedPrintText);

            var localModeFieldsJson = captured.LocalModeFields == null ? null : JsonConvert.SerializeObject(captured.LocalModeFields);
            var reportFieldsJson = reportFields == null ? null : JsonConvert.SerializeObject(reportFields);

            var dbRowId = _database.SaveOperation(new OperationRecord
            {
                OperationId = operationId,
                ClientRequestId = clientRequestId,
                OperationType = operationType,
                StartedAt = startedAt,
                CompletedAt = completedAt,
                DurationMs = durationMs,
                CallResult = captured.CallResult,
                MethodRejectCode = captured.MethodRejectCode,
                MethodRejectInfo = captured.MethodRejectInfo,
                ResultEventName = captured.ResultEventName,
                LocalModeResult = captured.LocalModeResult,
                ResponseCode = captured.LocalModeResponseCode,
                RejectionSource = captured.LocalModeRejectionSource,
                RejectionReason = captured.LocalModeRejectionReason,
                PrintTextRaw = captured.RawPrintText,
                PrintTextSanitized = captured.SanitizedPrintText,
                DisplayText = captured.LastDisplayText,
                Error = captured.Error,
                ErrorCode = captured.TimedOut ? ErrorCodes.OPERATION_TIMEOUT :
                           captured.CallResult != 1 ? ErrorTaxonomy.MapMethodRejectCode(captured.MethodRejectCode).ErrorCode : null,
                ReceiptFileId = receiptFileId,
                LocalModeFieldsJson = localModeFieldsJson,
                ReportFieldsJson = reportFieldsJson
            });

            var success = captured.CallResult == 1 && !captured.TimedOut && captured.LocalModeResult == 1;
            var errorCode = captured.TimedOut ? ErrorCodes.OPERATION_TIMEOUT :
                           captured.CallResult != 1 ? ErrorTaxonomy.MapMethodRejectCode(captured.MethodRejectCode).ErrorCode : null;

            return new OperationResponse
            {
                Success = success,
                OperationId = operationId,
                StartedAt = startedAt,
                CompletedAt = completedAt,
                DurationMs = durationMs,
                CallResult = captured.CallResult,
                MethodRejectCode = captured.MethodRejectCode,
                MethodRejectInfo = captured.MethodRejectInfo,
                ResultEventName = captured.ResultEventName,
                LocalModeResult = captured.LocalModeResult,
                ResponseCode = captured.LocalModeResponseCode,
                RejectionSource = captured.LocalModeRejectionSource,
                RejectionReason = captured.LocalModeRejectionReason,
                LocalModeFields = captured.LocalModeFields,
                PrintTextRaw = captured.RawPrintText,
                PrintTextSanitized = captured.SanitizedPrintText,
                LastDisplayText = captured.LastDisplayText,
                Error = captured.Error,
                ErrorCode = errorCode,
                DbRowId = dbRowId,
                ReceiptFileId = receiptFileId
            };
        }

        private static string[] ParseLocalModeResultDataFields(string localModeResultData)
        {
            if (string.IsNullOrWhiteSpace(localModeResultData)) return new string[0];
            var s = localModeResultData.Trim();
            if (s.StartsWith("D", StringComparison.OrdinalIgnoreCase))
                s = s.Substring(1).TrimStart();
            return s.Split(new[] { ';' }, StringSplitOptions.None)
                .Select(x => x == null ? "" : x.Trim())
                .ToArray();
        }

        private static string MapEntryMode(string entryModeCode)
        {
            if (string.IsNullOrWhiteSpace(entryModeCode)) return "";
            var c = entryModeCode.Trim();
            switch (c)
            {
                case "0": return "CHIP";
                case "2": return "CONTACTLESS";
                default: return "UNKNOWN_" + c;
            }
        }

        private CapturedLocalMode RunTransferAmountWithBusyRetry(Func<CapturedLocalMode> call)
        {
            if (call == null) throw new ArgumentNullException(nameof(call));

            var cfg = _config?.BusyRetry;
            if (cfg == null || !cfg.Enabled)
                return call();

            var maxSeconds = Math.Max(0, Math.Min(cfg.MaxRetrySeconds, 300));
            var delayMs = Math.Max(200, Math.Min(cfg.RetryDelayMs, 5000));
            var deadline = DateTime.UtcNow.AddSeconds(maxSeconds);

            while (true)
            {
                var cap = call();
                if (cap.CallResult == 1) return cap;
                if (ErrorTaxonomy.IsRetryableMethodReject(cap.MethodRejectCode) && DateTime.UtcNow < deadline)
                {
                    Thread.Sleep(delayMs);
                    continue;
                }
                return cap;
            }
        }

        private OperationResponse CreateErrorResponse(string errorCode, string message)
        {
            return new OperationResponse { Success = false, ErrorCode = errorCode, Error = message };
        }
    }
}
