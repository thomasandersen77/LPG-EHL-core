using System;
using Newtonsoft.Json;

namespace StationSupervisor.Iot
{
    public class UpdateSpec
    {
        [JsonProperty("mode")]
        public string Mode { get; set; } = string.Empty; // "force"

        [JsonProperty("targetVersion")]
        public string TargetVersion { get; set; } = string.Empty;

        [JsonProperty("issuedAt")]
        public DateTime IssuedAt { get; set; }
    }

    public class UpdateStatus
    {
        [JsonProperty("state")]
        public string State { get; set; } = string.Empty; // "updating", "success", "failed"

        [JsonProperty("currentVersion")]
        public string CurrentVersion { get; set; } = string.Empty;

        [JsonProperty("lastUpdateTime")]
        public DateTime LastUpdateTime { get; set; }
        
        [JsonProperty("errorMessage")]
        public string? ErrorMessage { get; set; }
    }

    public class Manifest
    {
        [JsonProperty("version")]
        public string Version { get; set; } = string.Empty;

        [JsonProperty("artifactUrl")]
        public string ArtifactUrl { get; set; } = string.Empty;

        [JsonProperty("sha256")]
        public string Sha256 { get; set; } = string.Empty;

        [JsonProperty("forceUpdate")]
        public bool ForceUpdate { get; set; }
    }
}
