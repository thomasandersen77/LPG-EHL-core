import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { fetchDailySummary } from '../api/reports';

export function ReportsPage() {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['daily-summary', date],
    queryFn: () => fetchDailySummary(date),
  });

  if (isLoading) {
    return <div className="p-8 text-center text-gray-500">Laster rapporter...</div>;
  }

  if (isError) {
    return <div className="p-8 text-center text-red-500">Feil ved lasting av rapporter.</div>;
  }

  const totalVolume = data?.reduce((sum, item) => sum + item.totalVolumeLiters, 0) || 0;
  const totalAmount = data?.reduce((sum, item) => sum + item.totalAmountKr, 0) || 0;
  const totalTx = data?.reduce((sum, item) => sum + item.transactionCount, 0) || 0;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-slate-900">Dagsrapport</h1>
        <input 
          type="date" 
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="border rounded p-2"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-xl shadow">
          <h3 className="text-gray-500 text-sm font-medium">Totalt Volum</h3>
          <p className="text-3xl font-bold text-blue-600 mt-2">{totalVolume.toFixed(2)} L</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow">
          <h3 className="text-gray-500 text-sm font-medium">Totalt Salg</h3>
          <p className="text-3xl font-bold text-green-600 mt-2">{totalAmount.toFixed(2)} kr</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow">
          <h3 className="text-gray-500 text-sm font-medium">Antall Transaksjoner</h3>
          <p className="text-3xl font-bold text-purple-600 mt-2">{totalTx}</p>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Detaljer per pumpe</h3>
        </div>
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pumpe</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Volum</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Beløp</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Antall</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Snittpris</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data?.map((item) => (
              <tr key={item.dispenserAddress} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">#{item.dispenserAddress}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">{item.totalVolumeLiters.toFixed(2)} L</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">{item.totalAmountKr.toFixed(2)} kr</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">{item.transactionCount}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-right">{item.averagePricePerLiter?.toFixed(2)} kr/L</td>
              </tr>
            ))}
            {data?.length === 0 && (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-gray-500">Ingen data for denne datoen</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
