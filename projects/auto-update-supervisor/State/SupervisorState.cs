using System;
using System.Collections.Generic;

namespace StationSupervisor.State
{
    public class SupervisorState
    {
        public string CurrentVersion { get; set; } = "0.0.0";
        public DateTime? LastSuccessTimestamp { get; set; }
        public DateTime? LastFailureTimestamp { get; set; }
        public string? LastFailedVersion { get; set; }
        public Dictionary<string, int> AttemptCountByVersion { get; set; } = new Dictionary<string, int>();
        public DateTime? DailyAttemptResetDate { get; set; }
    }
}
