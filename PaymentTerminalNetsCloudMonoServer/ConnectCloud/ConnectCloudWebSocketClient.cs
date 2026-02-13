using System;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using PaymentTerminalNetsCloudMonoServer.Configuration;

namespace PaymentTerminalNetsCloudMonoServer.ConnectCloud
{
    public interface IConnectCloudWebSocketClient
    {
        WebSocketState State { get; }
        Task ConnectAsync(string bearerToken);
        Task SendAsync(string json);
        Task<string> ReceiveAsync(CancellationToken token);
        Task CloseAsync();
        Task ReconnectAsync();
    }

    public class ConnectCloudWebSocketClient : IConnectCloudWebSocketClient
    {
        private readonly ServerConfig.ConnectCloudConfig _config;
        private readonly ConnectCloudAuthClient _authClient;
        private ClientWebSocket _ws;
        private readonly object _lock = new object();
        private string _lastToken;
        private string _lastUrl;

        public string LastUrl => _lastUrl;

        public ConnectCloudWebSocketClient(ServerConfig.ConnectCloudConfig config, ConnectCloudAuthClient authClient)
        {
            _config = config ?? throw new ArgumentNullException(nameof(config));
            _authClient = authClient ?? throw new ArgumentNullException(nameof(authClient));
        }

        public WebSocketState State => _ws?.State ?? WebSocketState.None;

        private string GetBaseUrl()
        {
            if (!string.IsNullOrWhiteSpace(_config.BaseUrl))
                return _config.BaseUrl.TrimEnd('/');

            var env = ( _config.Environment ?? "QA" ).ToUpperInvariant();
            if (env == "PROD")
                return "https://connectcloud.aws.nets.eu";
            return "https://connectcloud-test.aws.nets.eu";
        }

        private string GetWebSocketUrl()
        {
            var baseUrl = GetBaseUrl();
            var uri = new Uri(baseUrl);
            var scheme = uri.Scheme == "https" ? "wss" : "ws";
            var path = ( _config.WebSocketPath ?? "/ws/json" ).TrimStart('/');
            return $"{scheme}://{uri.Host}:{(uri.Port > 0 ? uri.Port : 443)}/{path}";
        }

        public async Task ConnectAsync(string bearerToken)
        {
            lock (_lock)
            {
                if (_ws != null && _ws.State == WebSocketState.Open)
                    return;
                _ws?.Dispose();
                _ws = new ClientWebSocket();
                _ws.Options.AddSubProtocol("json");
            }

            var url = GetWebSocketUrl();
            var uri = new Uri(url);
            _lastToken = bearerToken;
            _lastUrl = url;

            _ws.Options.SetRequestHeader("Authorization", "bearer " + bearerToken);

            try
            {
                await _ws.ConnectAsync(uri, CancellationToken.None).ConfigureAwait(false);
            }
            catch
            {
                _ws?.Dispose();
                _ws = null;
                throw;
            }
        }

        private string GetStateDetails(ClientWebSocket ws)
        {
            if (ws == null) return "ws=null";
            var closeStatus = ws.CloseStatus.HasValue ? ws.CloseStatus.Value.ToString() : "null";
            var closeDesc = ws.CloseStatusDescription ?? "";
            closeDesc = closeDesc.Length > 200 ? closeDesc.Substring(0, 200) + "…" : closeDesc;
            return $"state={ws.State} closeStatus={closeStatus} closeDesc='{closeDesc}' url='{_lastUrl ?? ""}'";
        }

        public async Task SendAsync(string json)
        {
            var ws = _ws;
            if (ws == null || ws.State != WebSocketState.Open)
                throw new InvalidOperationException("WebSocket is not connected (" + GetStateDetails(ws) + ")");

            var bytes = Encoding.UTF8.GetBytes(json);
            await ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, CancellationToken.None).ConfigureAwait(false);
        }

        public async Task<string> ReceiveAsync(CancellationToken token)
        {
            var ws = _ws;
            if (ws == null || ws.State != WebSocketState.Open)
                return null;

            var buffer = new byte[65536];
            var sb = new StringBuilder();
            WebSocketReceiveResult result;

            do
            {
                result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), token).ConfigureAwait(false);
                if (result.MessageType == WebSocketMessageType.Close)
                    return null;
                sb.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
            }
            while (!result.EndOfMessage);

            return sb.ToString();
        }

        public async Task CloseAsync()
        {
            var ws = _ws;
            if (ws != null && ws.State == WebSocketState.Open)
            {
                try
                {
                    await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Close", CancellationToken.None).ConfigureAwait(false);
                }
                catch { }
            }
            _ws?.Dispose();
            _ws = null;
        }

        public async Task ReconnectAsync()
        {
            await CloseAsync().ConfigureAwait(false);

            var baseUrl = GetBaseUrl();
            var username = _config.Username ?? "";
            var password = _config.Password ?? "";
            var token = await _authClient.LoginAsync(baseUrl, username, password, _config.LoginTimeoutSeconds).ConfigureAwait(false);
            await ConnectAsync(token).ConfigureAwait(false);
        }

        public async Task ReconnectWithBackoffAsync()
        {
            var baseDelay = Math.Max(100, _config.ReconnectBaseDelayMs);
            var maxDelay = Math.Max(baseDelay, _config.ReconnectMaxDelayMs);
            var delay = baseDelay;
            var rnd = new Random();

            while (true)
            {
                try
                {
                    await ReconnectAsync().ConfigureAwait(false);
                    return;
                }
                catch (Exception ex)
                {
                    Console.Error.WriteLine($"[{DateTime.Now:O}] WebSocket reconnect failed: {ex.Message}. Retrying in {delay}ms.");
                }

                await Task.Delay(delay).ConfigureAwait(false);
                delay = Math.Min(maxDelay, delay * 2 + rnd.Next(0, 500));
            }
        }
    }
}
