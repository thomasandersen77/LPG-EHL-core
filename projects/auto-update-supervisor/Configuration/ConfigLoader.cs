using System.IO;
using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace StationSupervisor.Configuration
{
    public class ConfigLoader
    {
        private readonly string _configPath;

        public ConfigLoader(string configPath)
        {
            _configPath = configPath;
        }

        public SupervisorConfig Load()
        {
            if (!File.Exists(_configPath))
            {
                throw new FileNotFoundException($"Configuration file not found at {_configPath}");
            }

            var yaml = File.ReadAllText(_configPath);
            var deserializer = new DeserializerBuilder()
                .WithNamingConvention(CamelCaseNamingConvention.Instance)
                .Build();

            return deserializer.Deserialize<SupervisorConfig>(yaml);
        }
    }
}
