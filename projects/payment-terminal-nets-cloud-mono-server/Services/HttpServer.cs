using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using PaymentTerminalNetsCloudMonoServer.Configuration;
using PaymentTerminalNetsCloudMonoServer.Models;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public class HttpServer : IDisposable
    {
        private static readonly JsonSerializerSettings JsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver(),
            NullValueHandling = NullValueHandling.Include
        };

        private readonly HttpListener _listener;
        private readonly ServerConfig _config;
        private readonly ITerminalService _terminalService;
        private bool _running;
        private Thread _serverThread;

        public HttpServer(ServerConfig config, ITerminalService terminalService)
        {
            _config = config ?? throw new ArgumentNullException(nameof(config));
            _terminalService = terminalService ?? throw new ArgumentNullException(nameof(terminalService));
            _listener = new HttpListener();
            _listener.Prefixes.Add($"http://{_config.BindAddress}:{_config.BindPort}/");
        }

        public void Start()
        {
            if (_running) return;

            _listener.Start();
            _running = true;
            _serverThread = new Thread(RunServer) { IsBackground = true };
            _serverThread.Start();
            Console.WriteLine($"[{DateTime.Now:O}] HTTP server started on http://{_config.BindAddress}:{_config.BindPort}/");
        }

        public void Stop()
        {
            if (!_running) return;

            _running = false;
            _listener.Stop();
            _serverThread?.Join(TimeSpan.FromSeconds(5));
            Console.WriteLine($"[{DateTime.Now:O}] HTTP server stopped");
        }

        private void RunServer()
        {
            while (_running)
            {
                try
                {
                    var context = _listener.GetContext();
                    Task.Run(() => HandleRequest(context));
                }
                catch (HttpListenerException) { break; }
                catch (Exception ex)
                {
                    Console.Error.WriteLine($"[{DateTime.Now:O}] HTTP server error: {ex.Message}");
                }
            }
        }

        private void HandleRequest(HttpListenerContext context)
        {
            try
            {
                var request = context.Request;
                var response = context.Response;
                var path = request.Url.AbsolutePath;
                var method = request.HttpMethod;

                response.AddHeader("Access-Control-Allow-Origin", "*");
                response.AddHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                response.AddHeader("Access-Control-Allow-Headers", "Content-Type");

                if (method == "OPTIONS")
                {
                    response.StatusCode = 200;
                    response.Close();
                    return;
                }

                if (path == "/health" && method == "GET")
                    HandleHealth(response);
                else if (path == "/v1/terminal/status" && method == "GET")
                    HandleTerminalStatus(response);
                else if (path == "/v1/terminal/open" && method == "POST")
                    HandleTerminalOpen(response);
                else if (path == "/v1/terminal/close" && method == "POST")
                    HandleTerminalClose(response);
                else if (path.StartsWith("/v1/payments/"))
                    HandlePaymentEndpoint(path, method, request, response);
                else if (path.StartsWith("/v1/admin/"))
                    HandleAdminEndpoint(path, method, request, response);
                else if (path == "/v1/events" || path.StartsWith("/v1/events/"))
                    HandleEventsEndpoint(path, method, request, response);
                else if (path.StartsWith("/v1/diag/"))
                    HandleDiagnosticEndpoint(path, method, request, response);
                else
                    SendError(response, 404, ErrorCodes.INVALID_REQUEST, "Endpoint not found");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"[{DateTime.Now:O}] Request handling error: {ex.Message}");
                try { SendError(context.Response, 500, ErrorCodes.VENDOR_CALL_FAILURE, ex.Message); } catch { }
            }
        }

        private void HandleHealth(HttpListenerResponse response)
        {
            var health = new { status = "ok", timestamp = DateTime.UtcNow.ToString("O"), configLoaded = _config != null };
            SendJson(response, 200, health);
        }

        private void HandleTerminalStatus(HttpListenerResponse response)
        {
            var status = _terminalService.GetStatus();
            SendJson(response, 200, status);
        }

        private void HandleTerminalOpen(HttpListenerResponse response)
        {
            var result = _terminalService.Open();
            SendJson(response, result.Success ? 200 : 503, result);
        }

        private void HandleTerminalClose(HttpListenerResponse response)
        {
            var result = _terminalService.Close();
            SendJson(response, result.Success ? 200 : 500, result);
        }

        private void HandlePaymentEndpoint(string path, string method, HttpListenerRequest request, HttpListenerResponse response)
        {
            if (method != "POST")
            {
                SendError(response, 405, ErrorCodes.INVALID_REQUEST, "Method not allowed");
                return;
            }

            var body = ReadRequestBody(request);
            if (path == "/v1/payments/purchase")
            {
                var req = JsonConvert.DeserializeObject<PurchaseRequest>(body);
                var result = _terminalService.Purchase(req);
                SendJson(response, result.Success ? 200 : GetStatusCode(result.ErrorCode), result);
            }
            else if (path == "/v1/payments/refund")
            {
                var req = JsonConvert.DeserializeObject<RefundRequest>(body);
                var result = _terminalService.Refund(req);
                SendJson(response, result.Success ? 200 : GetStatusCode(result.ErrorCode), result);
            }
            else if (path == "/v1/payments/cashback")
            {
                var req = JsonConvert.DeserializeObject<CashbackRequest>(body);
                var result = _terminalService.Cashback(req);
                SendJson(response, result.Success ? 200 : GetStatusCode(result.ErrorCode), result);
            }
            else
                SendError(response, 404, ErrorCodes.INVALID_REQUEST, "Endpoint not found");
        }

        private void HandleAdminEndpoint(string path, string method, HttpListenerRequest request, HttpListenerResponse response)
        {
            if (method != "POST")
            {
                SendError(response, 405, ErrorCodes.INVALID_REQUEST, "Method not allowed");
                return;
            }

            var body = ReadRequestBody(request);
            object result = null;

            if (path == "/v1/admin/avstemming")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12592, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/cancel")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12594, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/reversal")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12596, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/z-report")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12599, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/last-receipt")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12604, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/software")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12606, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/dataset")
            {
                var req = JsonConvert.DeserializeObject<AdminRequest>(body ?? "{}");
                result = _terminalService.RunAdmin(12607, req?.Password ?? "0000");
            }
            else if (path == "/v1/admin/code")
            {
                var req = JsonConvert.DeserializeObject<AdminCodeRequest>(body);
                if (req?.Code == null)
                {
                    SendError(response, 400, ErrorCodes.INVALID_REQUEST, "Admin code is required");
                    return;
                }
                result = _terminalService.RunAdmin(req.Code.Value, req.Password ?? "0000");
            }
            else
            {
                SendError(response, 404, ErrorCodes.INVALID_REQUEST, "Endpoint not found");
                return;
            }

            if (result != null)
            {
                var opResult = result as OperationResponse;
                SendJson(response, opResult?.Success == true ? 200 : GetStatusCode(opResult?.ErrorCode), result);
            }
        }

        private void HandleEventsEndpoint(string path, string method, HttpListenerRequest request, HttpListenerResponse response)
        {
            if (path == "/v1/events/stream" && method == "GET")
                HandleEventStream(request, response);
            else if (path == "/v1/events" && method == "GET")
                HandleEventPolling(request, response);
            else
                SendError(response, 404, ErrorCodes.INVALID_REQUEST, "Endpoint not found");
        }

        private void HandleDiagnosticEndpoint(string path, string method, HttpListenerRequest request, HttpListenerResponse response)
        {
            if (!_config.EnableDiagnostics)
            {
                SendError(response, 403, ErrorCodes.DIAGNOSTICS_DISABLED, "Diagnostics are disabled");
                return;
            }

            if (path == "/v1/diag/schema" && method == "GET")
                HandleDiagSchema(response);
            else if (path == "/v1/diag/sendjson" && method == "POST")
                HandleDiagSendJson(request, response);
            else if (path == "/v1/diag/sendtld" && method == "POST")
                HandleDiagSendTld(request, response);
            else if (path == "/v1/diag/confirm" && method == "POST")
                HandleDiagConfirm(request, response);
            else
                SendError(response, 404, ErrorCodes.INVALID_REQUEST, "Diagnostic endpoint not found");
        }

        private void HandleDiagSchema(HttpListenerResponse response)
        {
            var schema = _terminalService.GetConnectCloudSchema();
            SendJson(response, 200, schema);
        }

        private void HandleDiagSendJson(HttpListenerRequest request, HttpListenerResponse response)
        {
            var body = ReadRequestBody(request);
            var req = JsonConvert.DeserializeObject<Dictionary<string, object>>(body ?? "{}");
            if (!req.TryGetValue("json", out var jsonObj) || jsonObj == null)
            {
                SendError(response, 400, ErrorCodes.INVALID_REQUEST, "json field is required");
                return;
            }
            var json = jsonObj.ToString();
            var result = _terminalService.SendJson(json);
            SendJson(response, 200, new { success = result == 1, callResult = result });
        }

        private void HandleDiagSendTld(HttpListenerRequest request, HttpListenerResponse response)
        {
            var body = ReadRequestBody(request);
            var req = JsonConvert.DeserializeObject<Dictionary<string, object>>(body ?? "{}");
            var tldType = req.TryGetValue("tldType", out var typeObj) && typeObj != null ? typeObj.ToString() : "custom";
            if (!req.TryGetValue("tldData", out var dataObj) || dataObj == null)
            {
                SendError(response, 400, ErrorCodes.INVALID_REQUEST, "tldData field is required");
                return;
            }
            var tldDataB64 = dataObj.ToString();
            var tldData = Convert.FromBase64String(tldDataB64);
            var result = _terminalService.SendTld(tldType, tldData);
            SendJson(response, 200, new { success = result == 1, callResult = result });
        }

        private void HandleDiagConfirm(HttpListenerRequest request, HttpListenerResponse response)
        {
            var body = ReadRequestBody(request);
            var req = JsonConvert.DeserializeObject<Dictionary<string, object>>(body ?? "{}");
            var id = req.TryGetValue("id", out var idObj) && idObj != null ? Convert.ToInt32(idObj) : 0;
            var allow = req.TryGetValue("allow", out var allowObj) && allowObj != null && Convert.ToBoolean(allowObj);
            var json = $"{{\"confirm\":{{\"ver\":\"1.00\",\"id\":{id},\"allow\":{(allow ? 1 : 0)}}}}}";
            var result = _terminalService.SendJson(json);
            SendJson(response, 200, new { success = result == 1, callResult = result });
        }

        private void HandleEventStream(HttpListenerRequest request, HttpListenerResponse response)
        {
            response.ContentType = "text/event-stream";
            response.AddHeader("Cache-Control", "no-cache");
            response.AddHeader("Connection", "keep-alive");
            response.StatusCode = 200;
            response.SendChunked = true;

            var since = request.QueryString["since"];
            if (string.IsNullOrWhiteSpace(since))
                since = DateTime.UtcNow.ToString("O");

            try
            {
                using (var writer = new StreamWriter(response.OutputStream, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false)))
                {
                    writer.NewLine = "\n";
                    writer.AutoFlush = true;

                    writer.WriteLine("event: connected");
                    writer.WriteLine($"data: {{\"since\":\"{since}\"}}");
                    writer.WriteLine();

                    while (_running)
                    {
                        var events = _terminalService.GetEvents(since);
                        if (events != null && events.Count > 0)
                        {
                            foreach (var e in events)
                            {
                                since = e.Cursor.ToString();
                                writer.WriteLine($"id: {e.Cursor}");
                                writer.WriteLine($"event: {e.EventType ?? "event"}");
                                writer.WriteLine("data: " + JsonConvert.SerializeObject(e, JsonSettings));
                                writer.WriteLine();
                            }
                        }
                        else
                        {
                            writer.WriteLine($": keepalive {DateTime.UtcNow:O}");
                            writer.WriteLine();
                        }

                        Thread.Sleep(1000);
                    }
                }
            }
            catch { }
            finally
            {
                try { response.Close(); } catch { }
            }
        }

        private void HandleEventPolling(HttpListenerRequest request, HttpListenerResponse response)
        {
            var since = request.QueryString["since"];
            var events = _terminalService.GetEvents(since);
            SendJson(response, 200, events);
        }

        private string ReadRequestBody(HttpListenerRequest request)
        {
            if (!request.HasEntityBody) return "{}";
            using (var reader = new StreamReader(request.InputStream, request.ContentEncoding))
                return reader.ReadToEnd();
        }

        private void SendJson(HttpListenerResponse response, int statusCode, object data)
        {
            response.StatusCode = statusCode;
            response.ContentType = "application/json; charset=utf-8";
            var json = JsonConvert.SerializeObject(data, JsonSettings);
            var bytes = Encoding.UTF8.GetBytes(json);
            response.ContentLength64 = bytes.Length;
            response.OutputStream.Write(bytes, 0, bytes.Length);
            response.Close();
        }

        private void SendError(HttpListenerResponse response, int statusCode, string errorCode, string message)
        {
            var error = new ErrorResponse { Error = message, ErrorCode = errorCode };
            SendJson(response, statusCode, error);
        }

        private int GetStatusCode(string errorCode)
        {
            switch (errorCode)
            {
                case ErrorCodes.TERMINAL_BUSY: return 409;
                case ErrorCodes.TERMINAL_NOT_READY: return 503;
                case ErrorCodes.OPERATION_TIMEOUT: return 408;
                case ErrorCodes.OPERATION_REJECTED: return 422;
                case ErrorCodes.VENDOR_CALL_FAILURE: return 500;
                case ErrorCodes.INVALID_REQUEST: return 400;
                case ErrorCodes.DIAGNOSTICS_DISABLED: return 403;
                default: return 500;
            }
        }

        public void Dispose()
        {
            Stop();
            _listener?.Close();
        }
    }
}
