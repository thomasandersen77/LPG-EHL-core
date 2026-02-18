using System.Threading.Tasks;

namespace StationSupervisor.Management
{
    public interface IHealthChecker
    {
        Task<bool> IsBusyAsync();
        Task<bool> IsHealthyAsync();
    }
}
