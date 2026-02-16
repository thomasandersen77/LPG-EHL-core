using System;
using System.Threading;

namespace PaymentTerminalNetsCloudMonoServer.Services
{
    public class OperationLock : IDisposable
    {
        private readonly object _lock = new object();
        private string _currentOperationId;
        private bool _disposed;

        public bool TryAcquire(out string operationId)
        {
            operationId = null;
            lock (_lock)
            {
                if (_currentOperationId != null)
                    return false;

                _currentOperationId = Guid.NewGuid().ToString("N");
                operationId = _currentOperationId;
                return true;
            }
        }

        public void Release(string operationId)
        {
            lock (_lock)
            {
                if (_currentOperationId == operationId)
                    _currentOperationId = null;
            }
        }

        public bool IsLocked => _currentOperationId != null;
        public string CurrentOperationId => _currentOperationId;

        public void Dispose()
        {
            if (!_disposed)
            {
                lock (_lock)
                {
                    _currentOperationId = null;
                }
                _disposed = true;
            }
        }
    }
}
