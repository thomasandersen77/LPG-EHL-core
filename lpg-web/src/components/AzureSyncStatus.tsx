import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchSyncStatus, triggerManualSync } from '../api/sync';
import { format } from 'date-fns';
import { nb } from 'date-fns/locale';

export function AzureSyncStatus() {
  const queryClient = useQueryClient();
  
  const { data: syncStatus, isLoading, isError } = useQuery({
    queryKey: ['syncStatus'],
    queryFn: fetchSyncStatus,
    refetchInterval: 5000, // Refresh every 5 seconds
  });

  const triggerSyncMutation = useMutation({
    mutationFn: triggerManualSync,
    onSuccess: () => {
      // Refetch status after triggering
      queryClient.invalidateQueries({ queryKey: ['syncStatus'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });

  if (isLoading) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
        <div className="flex items-center">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-3"></div>
          <span className="text-sm text-blue-700">Laster Azure-status...</span>
        </div>
      </div>
    );
  }

  if (isError || !syncStatus) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-6">
        <div className="flex items-center">
          <span className="text-sm text-gray-500">⚠️ Azure-synkronisering er ikke aktivert</span>
        </div>
      </div>
    );
  }

  const hasFailures = syncStatus.failedCount > 0;
  const hasPending = syncStatus.pendingCount > 0;

  return (
    <div className={`border rounded-lg p-4 mb-6 ${
      hasFailures ? 'bg-red-50 border-red-200' : 
      hasPending ? 'bg-yellow-50 border-yellow-200' : 
      'bg-green-50 border-green-200'
    }`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-6">
          {/* Status Icon */}
          <div className="flex items-center">
            {hasFailures ? (
              <span className="text-2xl">⛔</span>
            ) : hasPending ? (
              <span className="text-2xl">⏳</span>
            ) : (
              <span className="text-2xl">☁️</span>
            )}
            <span className="ml-2 font-semibold text-gray-700">Azure Synkronisering</span>
          </div>

          {/* Stats */}
          <div className="flex items-center space-x-4 text-sm">
            {hasPending && (
              <div className="flex items-center">
                <span className="font-medium text-yellow-700">
                  {syncStatus.pendingCount} venter
                </span>
              </div>
            )}
            
            <div className="flex items-center">
              <span className="text-gray-600">
                ✓ {syncStatus.syncedCount} synket
              </span>
            </div>

            {hasFailures && (
              <div className="flex items-center">
                <span className="font-medium text-red-700">
                  ❌ {syncStatus.failedCount} feilet
                </span>
              </div>
            )}
          </div>

          {/* Last Sync Time */}
          {syncStatus.lastSyncTime && (
            <div className="text-xs text-gray-500">
              Sist synket: {format(new Date(syncStatus.lastSyncTime), 'HH:mm:ss', { locale: nb })}
            </div>
          )}
        </div>

        {/* Manual Trigger Button */}
        <button
          onClick={() => triggerSyncMutation.mutate()}
          disabled={triggerSyncMutation.isPending}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {triggerSyncMutation.isPending ? (
            <span className="flex items-center">
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              Synker...
            </span>
          ) : (
            '🔄 Synk nå'
          )}
        </button>
      </div>

      {/* Detailed message for failures */}
      {hasFailures && (
        <div className="mt-3 text-sm text-red-700 border-t border-red-200 pt-3">
          ⚠️ Noen transaksjoner kunne ikke synkroniseres til Azure. Systemet vil automatisk prøve på nytt.
        </div>
      )}
    </div>
  );
}
