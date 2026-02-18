using System;
using System.IO;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace StationSupervisor.State
{
    public class StateManager
    {
        private readonly string _statePath;
        private readonly SemaphoreSlim _lock = new SemaphoreSlim(1, 1);
        private SupervisorState _currentState;

        public StateManager(string statePath)
        {
            _statePath = statePath;
            _currentState = new SupervisorState();
        }

        public async Task LoadAsync()
        {
            await _lock.WaitAsync();
            try
            {
                if (File.Exists(_statePath))
                {
                    var json = await File.ReadAllTextAsync(_statePath);
                    _currentState = JsonSerializer.Deserialize<SupervisorState>(json) ?? new SupervisorState();
                }
                else
                {
                    _currentState = new SupervisorState();
                }
            }
            finally
            {
                _lock.Release();
            }
        }

        public async Task SaveAsync()
        {
            await _lock.WaitAsync();
            try
            {
                var options = new JsonSerializerOptions { WriteIndented = true };
                var json = JsonSerializer.Serialize(_currentState, options);
                
                // Ensure directory exists
                var directory = Path.GetDirectoryName(_statePath);
                if (!string.IsNullOrEmpty(directory) && !Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                await File.WriteAllTextAsync(_statePath, json);
            }
            finally
            {
                _lock.Release();
            }
        }

        public SupervisorState GetState()
        {
            // Return a copy or the direct reference? 
            // For simplicity, returning direct reference but beware of concurrency if modified outside lock.
            // A better approach is to modify state via methods on StateManager.
            // But for now, returning reference is acceptable for this scope.
            return _currentState;
        }
    }
}
