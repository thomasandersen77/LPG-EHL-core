import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { fetchTransactions, type TransactionFilter, type PaymentType } from '../api/transactions';
import { confirmPayment } from '../api/emulator';
import { format } from 'date-fns';
import { nb } from 'date-fns/locale';
import { AzureSyncStatus } from '../components/AzureSyncStatus';

export function TransactionsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<TransactionFilter>({
    page: 0,
    size: 20,
    paymentType: 'ALL',
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['transactions', page, filter],
    queryFn: () => fetchTransactions({ ...filter, page }),
    refetchInterval: 3000, // Auto-refresh every 3 seconds
  });

  const settleMutation = useMutation({
    mutationFn: ({ dispenserAddress, paymentMethod }: { dispenserAddress: number; paymentMethod: PaymentType }) => {
      // Convert PaymentType to accepted payment method (exclude UNKNOWN)
      const method = paymentMethod === 'UNKNOWN' ? 'CARD' : paymentMethod;
      return confirmPayment(dispenserAddress, method);
    },
    onSuccess: () => {
      // Invalidate and refetch both transactions and pump status
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
    },
    onError: (error: any) => {
      console.error('Kunne ikke bekrefte betaling:', error);
    },
  });

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handleFilterChange = (key: keyof TransactionFilter, value: any) => {
    setFilter((prev) => ({ ...prev, [key]: value }));
    setPage(0); // Reset to first page on filter change
  };

  if (isLoading) {
    return <div className="p-8 text-center text-gray-500">Laster transaksjoner...</div>;
  }

  if (isError) {
    return <div className="p-8 text-center text-red-500">Feil ved lasting av transaksjoner.</div>;
  }

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Azure Sync Status */}
      <AzureSyncStatus />

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Transaksjoner</h1>
        
        {/* Simple Filter */}
        <select 
          className="border rounded p-2"
          value={filter.paymentType}
          onChange={(e) => handleFilterChange('paymentType', e.target.value)}
        >
          <option value="ALL">Alle betalingsmetoder</option>
          <option value="CARD">Kort</option>
          <option value="CREDIT">Kreditt</option>
        </select>
      </div>

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tidspunkt</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pumpe</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Volum (L)</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Beløp (kr)</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Pris/L</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Betaling</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Handling</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {data?.content.map((tx) => (
                <tr key={tx.transactionId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {format(new Date(tx.timestamp), 'dd.MM.yyyy HH:mm:ss', { locale: nb })}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    #{tx.dispenserAddress}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-right font-medium">
                    {tx.volumeLiters.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-right font-medium">
                    {tx.amountKr.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                    {tx.pricePerLiter.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {tx.paymentStatus === 'PENDING' ? (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-600">
                        ⏳ Venter
                      </span>
                    ) : (
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                        ${tx.paymentType === 'CARD' ? 'bg-blue-100 text-blue-800' : 
                          'bg-purple-100 text-purple-800'}`}>
                        {tx.paymentType}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {tx.paymentStatus === 'PENDING' ? (
                      <span className="text-orange-600 font-medium">⏰ Venter på betaling</span>
                    ) : (
                      <span className="text-green-600 font-medium">✓ Betalt</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {tx.paymentStatus === 'PENDING' ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => settleMutation.mutate({ dispenserAddress: tx.dispenserAddress, paymentMethod: 'CARD' })}
                          disabled={settleMutation.isPending}
                          className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-xs font-medium"
                        >
                          💳 Kort
                        </button>
                        <button
                          onClick={() => settleMutation.mutate({ dispenserAddress: tx.dispenserAddress, paymentMethod: 'CREDIT' })}
                          disabled={settleMutation.isPending}
                          className="px-3 py-1 bg-purple-600 text-white rounded hover:bg-purple-700 disabled:opacity-50 text-xs font-medium"
                        >
                          🏪 Kreditt
                        </button>
                      </div>
                    ) : (
                      <span className="text-gray-400 text-xs">—</span>
                    )}
                  </td>
                </tr>
              ))}
              {data?.content.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-gray-500">
                    Ingen transaksjoner funnet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="bg-gray-50 px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
          <div className="flex-1 flex justify-between sm:hidden">
            <button
              onClick={() => handlePageChange(page - 1)}
              disabled={page === 0}
              className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              Forrige
            </button>
            <button
              onClick={() => handlePageChange(page + 1)}
              disabled={!data?.hasNext}
              className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
            >
              Neste
            </button>
          </div>
          <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Viser <span className="font-medium">{page * (filter.size || 20) + 1}</span> til <span className="font-medium">{Math.min((page + 1) * (filter.size || 20), data?.totalElements || 0)}</span> av <span className="font-medium">{data?.totalElements}</span> resultater
              </p>
            </div>
            <div>
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <button
                  onClick={() => handlePageChange(page - 1)}
                  disabled={page === 0}
                  className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                >
                  Forrige
                </button>
                <button
                  onClick={() => handlePageChange(page + 1)}
                  disabled={!data?.hasNext}
                  className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                >
                  Neste
                </button>
              </nav>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
