using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Net.WebSockets;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using PaymentTerminalNetsCloudMonoServer.Configuration;
using PaymentTerminalNetsCloudMonoServer.ConnectCloud;
using PaymentTerminalNetsCloudMonoServer.Models;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public interface IConnectCloudAdapter
    {
        bool IsTerminalReady();
        string GetConnectionState();
        string GetLastError();
        int Open();
        int Close();
        CapturedLocalMode RunTransferAmountAndWaitForLocalMode(string operId, int amount1, int type1, int amount2, int type2, int amount3, int type3, string optionalData, TimeSpan timeout, bool capturePrintText = true);
        CapturedReport RunAdministrationAndCaptureReport(int code, string password, TimeSpan timeout, string hostData = "");
        int SendJson(string json);
        int SendTld(string tldType, byte[] tldData);
    }

    public class ConnectCloudAdapter : IConnectCloudAdapter, IDisposable
    {
        private static readonly JsonSerializerSettings NetsJsonSettings = new JsonSerializerSettings
        {
            // Connect@Cloud JSON parser can behave poorly if we send keys with explicit null values.
            // Only include the one method payload we intend to invoke.
            NullValueHandling = NullValueHandling.Ignore
        };

        private readonly ServerConfig.ConnectCloudConfig _config;
        private readonly ConnectCloudAuthClient _authClient;
        private readonly ConnectCloudWebSocketClient _wsClient;
        private readonly string _eventLogPath;
        private readonly object _lock = new object();
        private bool _terminalReady;
        private string _lastError;
        private string _lastDisplayText;
        private string _lastPrintText;
        private readonly StringBuilder _printTextBuilder = new StringBuilder();
        private bool _capturePrintText;
        private readonly ManualResetEventSlim _awaitLocalMode = new ManualResetEventSlim(false);
        private readonly ManualResetEventSlim _awaitTerminalReady = new ManualResetEventSlim(false);
        private CapturedLocalMode _lastCaptured;
        private bool _operationInProgress;

        public ConnectCloudAdapter(ServerConfig.ConnectCloudConfig config, ConnectCloudAuthClient authClient, ConnectCloudWebSocketClient wsClient, string eventLogPath)
        {
            _config = config ?? throw new ArgumentNullException(nameof(config));
            _authClient = authClient ?? throw new ArgumentNullException(nameof(authClient));
            _wsClient = wsClient ?? throw new ArgumentNullException(nameof(wsClient));
            _eventLogPath = string.IsNullOrWhiteSpace(eventLogPath) ? null : eventLogPath.Trim();

            _ = RunReceiveLoopAsync();
        }

        private async Task RunReceiveLoopAsync()
        {
            while (true)
            {
                try
                {
                    if (_wsClient.State != System.Net.WebSockets.WebSocketState.Open)
                    {
                        await Task.Delay(1000);
                        continue;
                    }

                    using (var cts = new CancellationTokenSource(TimeSpan.FromSeconds(30)))
                    {
                        var json = await _wsClient.ReceiveAsync(cts.Token).ConfigureAwait(false);
                        if (string.IsNullOrEmpty(json))
                        {
                            // Treat empty/null as a dropped/closed connection and fail any waiting operation
                            // (otherwise we risk waiting the full operation timeout with no chance of a response).
                            if (_wsClient.State != System.Net.WebSockets.WebSocketState.Open)
                            {
                                _terminalReady = false;
                                _lastError = "WebSocket disconnected";

                                if (_operationInProgress && _lastCaptured != null)
                                {
                                    _lastCaptured.CallResult = 0;
                                    _lastCaptured.Error = _lastError;
                                    _awaitLocalMode.Set();
                                }
                            }
                            continue;
                        }

                        LogEvent("IN", json);

                        try
                        {
                            var root = JObject.Parse(json);
                            var netsResponse = root["NetsResponse"] as JObject;
                            if (netsResponse == null) continue;

                            var msgTid = NetsResponseParser.GetTerminalId(netsResponse);
                            if (!string.IsNullOrEmpty(msgTid) && msgTid != _config.TerminalId)
                                continue;

                            ProcessResponse(netsResponse);
                        }
                        catch (Exception ex)
                        {
                            Console.Error.WriteLine($"[{DateTime.Now:O}] ConnectCloud parse error: {ex.Message}");
                        }
                    }
                }
                catch (OperationCanceledException) { }
                catch (Exception)
                {
                    if (_wsClient.State != System.Net.WebSockets.WebSocketState.Open)
                        await Task.Delay(1000);
                }
            }
        }

        private void ProcessResponse(JObject netsResponse)
        {
            if (NetsResponseParser.HasDfs13TerminalReady(netsResponse))
            {
                _terminalReady = true;
                _awaitTerminalReady.Set();
            }
            else if (NetsResponseParser.HasMethodRejected(netsResponse))
            {
                var code = NetsResponseParser.GetMethodRejectedCode(netsResponse);
                // If an operation is in progress, MethodRejected belongs to that operation.
                // Do NOT treat 7102 as "ready" here, otherwise we stall until timeout.
                if (_operationInProgress && _lastCaptured != null)
                {
                    _lastCaptured.MethodRejectCode = code;
                    _lastCaptured.MethodRejectInfo = $"MethodRejected {code}";
                    _lastCaptured.CallResult = 0;
                    _awaitLocalMode.Set();
                }
                else if (code == 7102)
                {
                    _terminalReady = true;
                    _awaitTerminalReady.Set();
                }
                else if (_lastCaptured != null)
                {
                    _lastCaptured.MethodRejectCode = code;
                    _lastCaptured.MethodRejectInfo = $"MethodRejected {code}";
                    _lastCaptured.CallResult = 0;
                    _awaitLocalMode.Set();
                }
            }
            else if (NetsResponseParser.HasDfs13DisplayText(netsResponse))
            {
                var (textId, text) = NetsResponseParser.GetDfs13DisplayText(netsResponse);
                _lastDisplayText = text ?? "";
            }
            else if (NetsResponseParser.HasDfs13PrintText(netsResponse))
            {
                var text = NetsResponseParser.GetDfs13PrintText(netsResponse);
                _lastPrintText = text ?? "";
                if (_capturePrintText)
                    _printTextBuilder.AppendLine(text ?? "");
            }
            else if (NetsResponseParser.HasDfs13JsonReceived(netsResponse))
            {
                // Connect@Cloud can deliver interactive prompts (e.g. PIN bypass) via Dfs13JsonReceived.
                // If we ignore these, operations can stall until timeout. Auto-confirm is a pragmatic default
                // for headless operation; if you need stricter behavior, add an explicit API for user confirmation.
                try
                {
                    var payload = ExtractDfs13JsonReceivedPayload(netsResponse);
                    if (!string.IsNullOrWhiteSpace(payload))
                    {
                        // Common shape: {"confirm":{"ver":"1.00","id":1,"desc":"Pin bypass?"}}
                        var obj = JObject.Parse(payload);
                        var confirm = obj["confirm"] as JObject;
                        if (confirm != null)
                        {
                            var idStr = confirm["id"]?.ToString();
                            var ver = confirm["ver"]?.ToString() ?? "1.00";
                            var desc = confirm["desc"]?.ToString() ?? "";
                            int.TryParse(idStr, out var id);

                            // Default allow=1 to avoid stalling the workflow.
                            var confirmJson = $"{{\"confirm\":{{\"ver\":\"{ver}\",\"id\":{id},\"allow\":1}}}}";
                            LogEvent("OUT", confirmJson);
                            _wsClient.SendAsync(confirmJson).GetAwaiter().GetResult();

                            if (!string.IsNullOrWhiteSpace(desc))
                                _lastDisplayText = desc;
                        }
                    }
                }
                catch (Exception ex)
                {
                    // Don't fail the main flow on confirm parsing issues, but record for diagnostics.
                    _lastError = "Dfs13JsonReceived handling failed: " + ex.Message;
                }
            }
            else if (NetsResponseParser.HasDfs13LocalMode(netsResponse))
            {
                var data = NetsResponseParser.GetDfs13LocalMode(netsResponse);
                ApplyLocalMode(data);
                _awaitLocalMode.Set();
            }
            else if (NetsResponseParser.HasDfs13LastFinancialResult(netsResponse))
            {
                var data = NetsResponseParser.GetDfs13LastFinancialResult(netsResponse);
                ApplyLocalMode(data);
                _awaitLocalMode.Set();
            }
            else if (NetsResponseParser.HasDfs13Error(netsResponse))
            {
                var code = NetsResponseParser.GetDfs13ErrorCode(netsResponse);
                _lastError = $"Dfs13Error {code}";
                if (_lastCaptured != null)
                {
                    _lastCaptured.CallResult = 0;
                    _lastCaptured.Error = _lastError;
                }
                _awaitLocalMode.Set();
            }
        }

        private static string ExtractDfs13JsonReceivedPayload(JObject netsResponse)
        {
            var token = netsResponse?["Dfs13JsonReceived"];
            if (token == null) return null;

            if (token.Type == JTokenType.String)
                return token.ToString();

            // Some implementations wrap it: { "Data": "<json-string>" }
            var data = token["Data"];
            if (data != null && data.Type == JTokenType.String)
                return data.ToString();

            // Fallback: attempt to stringify the token
            return token.ToString(Formatting.None);
        }

        private void ApplyLocalMode(ConnectCloud.Dfs13LocalModeData data)
        {
            if (data == null || _lastCaptured == null) return;

            _lastCaptured.LocalModeResultData = data.ResultData;
            _lastCaptured.LocalModeResponseCode = ParseResponseCode(data.ResultData);
            _lastCaptured.LocalModeRejectionReason = data.RejectionReason;
            _lastCaptured.LastDisplayText = _lastDisplayText;
            _lastCaptured.ResultEventName = "OnLocalMode";
            _lastCaptured.CallResult = 1;

            int.TryParse(data.Result, out var res);
            _lastCaptured.LocalModeResult = res;
        }

        private static string ParseResponseCode(string resultData)
        {
            if (string.IsNullOrWhiteSpace(resultData)) return null;
            var parts = resultData.Split(';');
            return parts.Length > 2 ? parts[2].Trim() : null;
        }

        private void LogEvent(string direction, string json)
        {
            if (string.IsNullOrWhiteSpace(_eventLogPath)) return;
            try
            {
                var line = $"{DateTime.UtcNow:O}\t{direction}\t{json}\n";
                System.IO.File.AppendAllText(_eventLogPath, line);
            }
            catch { }
        }

        public bool IsTerminalReady() => _terminalReady && _wsClient.State == WebSocketState.Open;
        public string GetConnectionState() => _wsClient.State.ToString();
        public string GetLastError() => _lastError;

        private bool EnsureOpenAndReady()
        {
            if (_wsClient.State == WebSocketState.Open && _terminalReady)
                return true;

            var res = Open();
            return res == 1;
        }

        public int Open()
        {
            _terminalReady = false;
            _awaitTerminalReady.Reset();

            try
            {
                var baseUrl = GetBaseUrl();
                string token;
                try
                {
                    token = _authClient.LoginAsync(baseUrl, _config.Username ?? "", _config.Password ?? "", _config.LoginTimeoutSeconds)
                        .ConfigureAwait(false).GetAwaiter().GetResult();
                }
                catch (Exception ex)
                {
                    _lastError = $"Connect@Cloud login failed (baseUrl='{baseUrl}'): {ex.Message}";
                    return 0;
                }

                try
                {
                    _wsClient.ConnectAsync(token).GetAwaiter().GetResult();
                }
                catch (Exception ex)
                {
                    var wsUrl = _wsClient.LastUrl ?? "";
                    _lastError = $"Connect@Cloud WebSocket connect failed (wsUrl='{wsUrl}'): {ex.Message}";
                    return 0;
                }

                SendOpenRequest();

                var timeout = TimeSpan.FromSeconds(_config.OpenReadyTimeoutSeconds);
                if (_awaitTerminalReady.Wait(timeout) && _terminalReady && _wsClient.State == System.Net.WebSockets.WebSocketState.Open)
                    return 1;

                SendLastResultForPriming();
                if (_awaitTerminalReady.Wait(TimeSpan.FromSeconds(10)) && _terminalReady && _wsClient.State == System.Net.WebSockets.WebSocketState.Open)
                    return 1;

                _lastError = "terminal_not_ready: Open timeout, priming did not yield TerminalReady";
                return 0;
            }
            catch (Exception ex)
            {
                _lastError = ex.Message;
                return 0;
            }
        }

        private string GetBaseUrl()
        {
            if (!string.IsNullOrWhiteSpace(_config.BaseUrl))
                return _config.BaseUrl.TrimEnd('/');
            var env = (_config.Environment ?? "QA").ToUpperInvariant();
            return env == "PROD" ? "https://connectcloud.aws.nets.eu" : "https://connectcloud-test.aws.nets.eu";
        }

        private void SendOpenRequest()
        {
            var req = BuildNetsRequest(r =>
            {
                r.Open = new object();
            });
            var json = JsonConvert.SerializeObject(req, NetsJsonSettings);
            LogEvent("OUT", json);
            _wsClient.SendAsync(json).GetAwaiter().GetResult();
        }

        private void SendLastResultForPriming()
        {
            var admCode = AdminCodeMapper.ToConnectCloudDecimal(0x313D);
            SendAdminRequest(admCode.ToString(), "");
        }

        private object BuildNetsRequest(Action<NetsRequest> configure)
        {
            var header = NetsMessageHeader.BuildHeader(_config.EcrIdPrefix, _config.TerminalId);
            var req = new NetsRequest { MessageHeader = header };
            configure(req);
            return new { NetsRequest = req };
        }

        private void SendAdminRequest(string admCode, string optionalData)
        {
            var operId = _config.OperatorIdDefault ?? "0000";
            var req = BuildNetsRequest(r =>
            {
                r.Dfs13Administration = new ConnectCloud.Dfs13Administration
                {
                    OperId = operId,
                    AdmCode = admCode,
                    OptionalData = optionalData ?? ""
                };
            });
            var json = JsonConvert.SerializeObject(req, NetsJsonSettings);
            LogEvent("OUT", json);
            _wsClient.SendAsync(json).GetAwaiter().GetResult();
        }

        public int Close()
        {
            if (_operationInProgress)
            {
                try
                {
                    var cancelCode = AdminCodeMapper.ToConnectCloudDecimal(AdminCodeMapper.CANCEL);
                    SendAdminRequest(cancelCode.ToString(), "");
                    Thread.Sleep(500);
                }
                catch { }
            }

            _wsClient.CloseAsync().GetAwaiter().GetResult();
            _terminalReady = false;
            _operationInProgress = false;
            return 1;
        }

        public CapturedLocalMode RunTransferAmountAndWaitForLocalMode(string operId, int amount1, int type1, int amount2, int type2, int amount3, int type3, string optionalData, TimeSpan timeout, bool capturePrintText = true)
        {
            _awaitLocalMode.Reset();
            _lastCaptured = new CapturedLocalMode();
            _printTextBuilder.Clear();
            _capturePrintText = capturePrintText;
            _operationInProgress = true;

            try
            {
                if (!EnsureOpenAndReady())
                {
                    _lastCaptured.CallResult = 0;
                    _lastCaptured.Error = _lastError ?? "Terminal not ready";
                    return _lastCaptured;
                }

                var req = BuildNetsRequest(r =>
                {
                    r.Dfs13TransferAmount = new ConnectCloud.Dfs13TransferAmount
                    {
                        TransactionType = type1.ToString(),
                        OperId = operId ?? _config.OperatorIdDefault ?? "0000",
                        Amount1 = amount1.ToString(),
                        Amount2 = amount2.ToString(),
                        Amount3 = amount3.ToString(),
                        Type2 = type2.ToString(),
                        Type3 = type3.ToString(),
                        HostData = "",
                        OptionalData = optionalData ?? ""
                    };
                });
                var json = JsonConvert.SerializeObject(req, NetsJsonSettings);
                LogEvent("OUT", json);
                _wsClient.SendAsync(json).GetAwaiter().GetResult();

                if (!_awaitLocalMode.Wait(timeout))
                {
                    _lastCaptured.TimedOut = true;
                    _lastCaptured.Error = "Operation timeout";
                }

                _lastCaptured.RawPrintText = _printTextBuilder.ToString();
                _lastCaptured.RawPrintText = string.IsNullOrEmpty(_lastCaptured.RawPrintText) ? _lastPrintText : _lastCaptured.RawPrintText;
                _lastCaptured.RawPrintText = _lastCaptured.RawPrintText ?? "";
                return _lastCaptured;
            }
            catch (Exception ex)
            {
                _lastError = ex.Message;
                _lastCaptured.CallResult = 0;
                _lastCaptured.Error = ex.Message;
                return _lastCaptured;
            }
            finally
            {
                _operationInProgress = false;
            }
        }

        public CapturedReport RunAdministrationAndCaptureReport(int code, string password, TimeSpan timeout, string hostData = "")
        {
            _awaitLocalMode.Reset();
            _lastCaptured = new CapturedLocalMode();
            _printTextBuilder.Clear();
            _capturePrintText = true;
            _operationInProgress = true;

            try
            {
                if (!EnsureOpenAndReady())
                {
                    _lastCaptured.CallResult = 0;
                    _lastCaptured.Error = _lastError ?? "Terminal not ready";
                    return new CapturedReport
                    {
                        CallResult = 0,
                        TimedOut = false,
                        Error = _lastCaptured.Error,
                        RawPrintText = "",
                        SanitizedPrintText = "",
                        LastDisplayText = _lastCaptured.LastDisplayText
                    };
                }

                var admCode = code > 10000 ? code : AdminCodeMapper.ToConnectCloudDecimal(code);
                SendAdminRequest(admCode.ToString(), password ?? "");

                if (!_awaitLocalMode.Wait(timeout))
                {
                    _lastCaptured.TimedOut = true;
                    _lastCaptured.Error = "Operation timeout";
                }

                var raw = _printTextBuilder.ToString();
                if (string.IsNullOrEmpty(raw)) raw = _lastPrintText ?? "";

                return new CapturedReport
                {
                    CallResult = _lastCaptured.CallResult,
                    LocalModeResult = _lastCaptured.LocalModeResult,
                    TimedOut = _lastCaptured.TimedOut,
                    Error = _lastCaptured.Error,
                    RawPrintText = raw,
                    SanitizedPrintText = ReceiptSanitizer.SanitizePrintText(raw),
                    LastDisplayText = _lastCaptured.LastDisplayText,
                    LocalModeResultData = _lastCaptured.LocalModeResultData,
                    LocalModeResponseCode = _lastCaptured.LocalModeResponseCode,
                    LocalModeRejectionSource = _lastCaptured.LocalModeRejectionSource,
                    LocalModeRejectionReason = _lastCaptured.LocalModeRejectionReason,
                    LocalModeFields = _lastCaptured.LocalModeFields,
                    MethodRejectCode = _lastCaptured.MethodRejectCode,
                    MethodRejectInfo = _lastCaptured.MethodRejectInfo,
                    ResultEventName = _lastCaptured.ResultEventName
                };
            }
            catch (Exception ex)
            {
                _lastError = ex.Message;
                return new CapturedReport
                {
                    CallResult = 0,
                    TimedOut = false,
                    Error = ex.Message,
                    RawPrintText = "",
                    SanitizedPrintText = "",
                    LastDisplayText = _lastCaptured?.LastDisplayText
                };
            }
            finally
            {
                _operationInProgress = false;
            }
        }

        public int SendJson(string json)
        {
            try
            {
                if (!EnsureOpenAndReady())
                {
                    _lastError = _lastError ?? "Terminal not ready";
                    return 0;
                }
                var req = BuildNetsRequest(r =>
                {
                    r.Dfs13SendJson = new ConnectCloud.Dfs13SendJson { Data = JObject.Parse(json) };
                });
                var reqJson = JsonConvert.SerializeObject(req, NetsJsonSettings);
                LogEvent("OUT", reqJson);
                _wsClient.SendAsync(reqJson).GetAwaiter().GetResult();
                return 1;
            }
            catch (Exception ex) { _lastError = ex.Message; return 0; }
        }

        public int SendTld(string tldType, byte[] tldData)
        {
            try
            {
                if (!EnsureOpenAndReady())
                {
                    _lastError = _lastError ?? "Terminal not ready";
                    return 0;
                }
                var data = tldData != null ? Encoding.UTF8.GetString(tldData) : "";
                var req = BuildNetsRequest(r =>
                {
                    r.Dfs13SendTld = new ConnectCloud.Dfs13SendTld { TldType = tldType ?? "REQ", Data = data };
                });
                var reqJson = JsonConvert.SerializeObject(req, NetsJsonSettings);
                LogEvent("OUT", reqJson);
                _wsClient.SendAsync(reqJson).GetAwaiter().GetResult();
                return 1;
            }
            catch (Exception ex) { _lastError = ex.Message; return 0; }
        }

        public void Dispose()
        {
            Close();
        }
    }
}
