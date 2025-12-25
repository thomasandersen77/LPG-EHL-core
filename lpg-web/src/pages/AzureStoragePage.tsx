import { useQuery } from '@tanstack/react-query';
import { fetchQueueMessagesByDate, type AzureQueueMessage } from '../api/sync';
import { format, parseISO } from 'date-fns';
import { nb } from 'date-fns/locale';
import { useState } from 'react';

export function AzureStoragePage() {
  const [expandedDates, setExpandedDates] = useState<Set<string>>(new Set());

  const { data, isLoading, isError } = useQuery({
    queryKey: ['azureQueueByDate'],
    queryFn: fetchQueueMessagesByDate,
    refetchInterval: 10000, // Refresh every 10 seconds
  });

  const toggleDate = (date: string) => {
    setExpandedDates((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(date)) {
        newSet.delete(date);
      } else {
        newSet.add(date);
      }
      return newSet;
    });
  };

  const expandAll = () => {
    if (data) {
      setExpandedDates(new Set(Object.keys(data.dates)));
    }
  };

  const collapseAll = () => {
    setExpandedDates(new Set());
  };

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-500">Laster Azure Storage data...</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <h2 className="text-xl font-semibold text-red-700 mb-2">⚠️ Kunne ikke laste data</h2>
          <p className="text-red-600">Azure Storage er ikke tilgjengelig eller ikke aktivert.</p>
        </div>
      </div>
    );
  }

  const dates = data ? Object.keys(data.dates).sort().reverse() : [];
  const totalMessages = data?.totalMessages || 0;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900 mb-2">☁️ Azure Storage Queue</h1>
            <p className="text-gray-600">
              Vis transaksjoner lagret i Azure Storage Queue (Azurite emulator)
            </p>
          </div>
          
          <div className="flex items-center space-x-3">
            <button
              onClick={expandAll}
              className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded hover:bg-blue-700 transition-colors"
            >
              Ekspander alle
            </button>
            <button
              onClick={collapseAll}
              className="px-4 py-2 bg-gray-600 text-white text-sm font-medium rounded hover:bg-gray-700 transition-colors"
            >
              Kollaps alle
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className="text-4xl font-bold text-blue-600">{totalMessages}</div>
              <div className="text-sm text-gray-600 mt-1">Totalt meldinger</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-indigo-600">{dates.length}</div>
              <div className="text-sm text-gray-600 mt-1">Unike dager</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-purple-600">
                {dates.length > 0 ? (data?.dates[dates[0]]?.length || 0) : 0}
              </div>
              <div className="text-sm text-gray-600 mt-1">I dag</div>
            </div>
          </div>
        </div>
      </div>

      {/* No messages */}
      {dates.length === 0 && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-12 text-center">
          <div className="text-6xl mb-4">📭</div>
          <h3 className="text-xl font-semibold text-gray-700 mb-2">Ingen meldinger i køen</h3>
          <p className="text-gray-600">
            Opprett transaksjoner via emulatoren eller API for å se dem her.
          </p>
        </div>
      )}

      {/* Messages by date */}
      {dates.map((date) => {
        const messages = data?.dates[date] || [];
        const isExpanded = expandedDates.has(date);
        const dateObj = parseISO(date);

        return (
          <div key={date} className="mb-4">
            {/* Date header - clickable */}
            <button
              onClick={() => toggleDate(date)}
              className="w-full bg-white border border-gray-200 rounded-lg p-4 hover:bg-gray-50 transition-colors flex items-center justify-between"
            >
              <div className="flex items-center space-x-4">
                <div className="text-2xl">
                  {isExpanded ? '📂' : '📁'}
                </div>
                <div className="text-left">
                  <h2 className="text-xl font-bold text-gray-800">
                    {format(dateObj, 'EEEE d. MMMM yyyy', { locale: nb })}
                  </h2>
                  <p className="text-sm text-gray-600">{messages.length} meldinger</p>
                </div>
              </div>
              <div className="text-gray-400">
                {isExpanded ? '▼' : '▶'}
              </div>
            </button>

            {/* Messages list */}
            {isExpanded && (
              <div className="mt-2 bg-white border border-gray-200 rounded-lg overflow-hidden">
                <div className="divide-y divide-gray-200">
                  {messages.map((message) => (
                    <MessageCard key={message.messageId} message={message} />
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

function MessageCard({ message }: { message: AzureQueueMessage }) {
  const insertionTime = parseISO(message.insertionTime);
  const tx = message.transaction;

  return (
    <div className="p-4 hover:bg-gray-50 transition-colors">
      <div className="flex items-start justify-between">
        {/* Main content */}
        <div className="flex-1">
          <div className="flex items-center space-x-3 mb-2">
            {/* Status badge */}
            <span
              className={`px-2 py-1 text-xs font-semibold rounded-full ${
                message.status === 'SYNCED'
                  ? 'bg-green-100 text-green-800'
                  : message.status === 'PENDING'
                  ? 'bg-yellow-100 text-yellow-800'
                  : message.status === 'IN_PROGRESS'
                  ? 'bg-blue-100 text-blue-800'
                  : 'bg-red-100 text-red-800'
              }`}
            >
              {message.status || 'UNKNOWN'}
            </span>

            {/* Time */}
            <span className="text-sm text-gray-500">
              {format(insertionTime, 'HH:mm:ss', { locale: nb })}
            </span>

            {/* Entity type */}
            <span className="text-xs text-gray-400">
              {message.entityType} • ID: {message.entityId?.substring(0, 8)}...
            </span>
          </div>

          {/* Transaction details */}
          {tx && (
            <div className="bg-gray-50 rounded p-3 mt-2">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                <div>
                  <span className="text-gray-500">Pumpe:</span>
                  <span className="ml-2 font-semibold text-gray-800">
                    #{tx.dispenserAddress}
                  </span>
                </div>
                <div>
                  <span className="text-gray-500">Volum:</span>
                  <span className="ml-2 font-semibold text-gray-800">
                    {tx.volumeLiters?.toFixed(2)} L
                  </span>
                </div>
                <div>
                  <span className="text-gray-500">Beløp:</span>
                  <span className="ml-2 font-semibold text-gray-800">
                    {tx.amountKr?.toFixed(2)} kr
                  </span>
                </div>
                <div>
                  <span className="text-gray-500">Betaling:</span>
                  <span
                    className={`ml-2 px-2 py-0.5 text-xs font-semibold rounded ${
                      tx.paymentType === 'CARD'
                        ? 'bg-blue-100 text-blue-800'
                        : tx.paymentType === 'CREDIT'
                        ? 'bg-purple-100 text-purple-800'
                        : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {tx.paymentType || 'N/A'}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Retry info */}
          {message.retryCount > 0 && (
            <div className="mt-2 text-xs text-orange-600">
              ⚠️ Retry count: {message.retryCount}
            </div>
          )}
        </div>

        {/* Dequeue count badge */}
        {message.dequeueCount > 0 && (
          <div className="ml-4 text-xs text-gray-500">
            Dequeue: {message.dequeueCount}
          </div>
        )}
      </div>
    </div>
  );
}
