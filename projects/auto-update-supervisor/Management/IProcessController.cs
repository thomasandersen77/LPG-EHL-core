using System.Threading;
using System.Threading.Tasks;

namespace StationSupervisor.Management
{
    public interface IProcessController
    {
        Task<bool> StartServiceAsync(string serviceName, CancellationToken cancellationToken);
        Task<bool> StopServiceAsync(string serviceName, CancellationToken cancellationToken);
        Task<bool> RestartServiceAsync(string serviceName, CancellationToken cancellationToken);
        Task<bool> IsServiceRunningAsync(string serviceName, CancellationToken cancellationToken);
        
        bool DirectoryExists(string path);
        void CreateDirectory(string path);
        void CreateSymlink(string targetInfo, string linkLocation);
        string? ReadSymlink(string linkLocation);
        void DeleteDirectory(string path, bool recursive);
        void MoveDirectory(string source, string dest);
    }
}
