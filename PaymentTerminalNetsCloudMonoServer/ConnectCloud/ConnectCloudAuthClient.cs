using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq;
using PaymentTerminalNetsCloudMonoServer.Configuration;

namespace PaymentTerminalNetsCloudMonoServer.ConnectCloud
{
    public class ConnectCloudAuthClient
    {
        public async Task<string> LoginAsync(string baseUrl, string username, string password, int timeoutSeconds)
        {
            if (string.IsNullOrWhiteSpace(baseUrl))
                throw new ArgumentException("baseUrl is required");
            if (string.IsNullOrWhiteSpace(username) || string.IsNullOrWhiteSpace(password))
                throw new ArgumentException("username and password are required");

            var url = baseUrl.TrimEnd('/') + "/v1/login";
            var payload = new { username = username, password = password };
            var json = Newtonsoft.Json.JsonConvert.SerializeObject(payload);

            using (var client = new HttpClient())
            {
                client.Timeout = TimeSpan.FromSeconds(timeoutSeconds);
                var content = new StringContent(json, Encoding.UTF8, "application/json");

                var response = await client.PostAsync(url, content).ConfigureAwait(false);

                if (response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
                    throw new InvalidOperationException("Connect@Cloud login failed: 401 Unauthorized");

                var responseBody = await response.Content.ReadAsStringAsync().ConfigureAwait(false);

                if (!response.IsSuccessStatusCode)
                    throw new InvalidOperationException($"Connect@Cloud login failed: {(int)response.StatusCode} {responseBody}");

                var obj = JObject.Parse(responseBody);
                var token = obj["token"]?.ToString();
                if (string.IsNullOrWhiteSpace(token))
                    throw new InvalidOperationException("Connect@Cloud login response missing token");

                return token;
            }
        }
    }
}
