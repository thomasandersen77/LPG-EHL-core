import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '/api/v1';

interface PriceData {
  displayPrice: number;
  displayProductName: string;
  prices: Array<{
    productCode: string;
    productName: string;
    pricePerLiter: number;
    pricePerLiterExclVat: number;
    vatRate: number;
    currency: string;
    lastUpdated: string;
  }>;
}

export function PriceAdminPage() {
  const queryClient = useQueryClient();
  const [newPrice, setNewPrice] = useState('');
  const [newRoadTax, setNewRoadTax] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);
  const [showRoadTaxSuccess, setShowRoadTaxSuccess] = useState(false);

  const { data: priceData, isLoading } = useQuery<PriceData>({
    queryKey: ['prices'],
    queryFn: async () => {
      const response = await axios.get(`${API_URL}/prices`);
      return response.data;
    },
  });

  const { data: roadTaxData, isLoading: isLoadingRoadTax } = useQuery({
    queryKey: ['road-tax'],
    queryFn: async () => {
      const response = await axios.get(`${API_URL}/road-tax`);
      return response.data;
    },
  });

  const updatePriceMutation = useMutation({
    mutationFn: async (newPriceValue: number) => {
      const response = await axios.post(`${API_URL}/prices/update`, {
        pricePerLiter: newPriceValue,
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prices'] });
      setShowSuccess(true);
      setNewPrice('');
      setTimeout(() => setShowSuccess(false), 3000);
    },
  });

  const updateRoadTaxMutation = useMutation({
    mutationFn: async (taxPerLiterKr: number) => {
      const taxPerLiterOre = Math.round(taxPerLiterKr * 100);
      const response = await axios.post(`${API_URL}/road-tax/update`, {
        taxPerLiterOre,
        description: `Veitrafikkavgift oppdatert til ${taxPerLiterKr.toFixed(2)} kr/L`
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['road-tax'] });
      setShowRoadTaxSuccess(true);
      setNewRoadTax('');
      setTimeout(() => setShowRoadTaxSuccess(false), 3000);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const priceValue = parseFloat(newPrice);
    if (!isNaN(priceValue) && priceValue > 0) {
      updatePriceMutation.mutate(priceValue);
    }
  };

  const handleRoadTaxSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const taxValue = parseFloat(newRoadTax);
    if (!isNaN(taxValue) && taxValue >= 0) {
      updateRoadTaxMutation.mutate(taxValue);
    }
  };

  const currentPrice = priceData?.displayPrice || 0;

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">💰 Prisadministrasjon</h1>
        <p className="text-slate-600">Administrer gasspris per liter</p>
      </div>

      {/* Current Price Display */}
      <div className="bg-gradient-to-r from-blue-50 to-green-50 rounded-2xl p-8 mb-8 border-2 border-blue-200">
        <div className="text-center">
          <div className="text-sm text-slate-600 mb-2">Gjeldende pris</div>
          {isLoading ? (
            <div className="text-4xl text-slate-400">Laster...</div>
          ) : (
            <>
              <div className="text-6xl font-bold text-blue-600 mb-2">
                {currentPrice.toFixed(2)} kr
              </div>
              <div className="text-lg text-slate-600">per liter LPG</div>
              {priceData?.prices[0] && (
                <div className="mt-4 text-sm text-slate-500">
                  <div>Eks. MVA: {priceData.prices[0].pricePerLiterExclVat.toFixed(2)} kr</div>
                  <div>MVA: {(priceData.prices[0].vatRate * 100).toFixed(0)}%</div>
                  <div className="text-xs mt-2">
                    Sist oppdatert: {new Date(priceData.prices[0].lastUpdated).toLocaleString('nb-NO')}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Update Price Form */}
      <div className="bg-white rounded-2xl shadow-xl p-8 border border-slate-200">
        <h2 className="text-2xl font-bold text-slate-900 mb-6">Endre pris</h2>

        {showSuccess && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center text-green-800">
              <span className="text-2xl mr-2">✅</span>
              <div>
                <div className="font-bold">Pris oppdatert!</div>
                <div className="text-sm">Ny pris er nå aktiv</div>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="price" className="block text-sm font-medium text-slate-700 mb-2">
              Ny pris per liter (NOK)
            </label>
            <div className="relative">
              <input
                type="number"
                id="price"
                step="0.01"
                min="0"
                value={newPrice}
                onChange={(e) => setNewPrice(e.target.value)}
                placeholder="15.90"
                className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg"
                disabled={updatePriceMutation.isPending}
              />
              <span className="absolute right-4 top-3 text-slate-500 text-lg">kr</span>
            </div>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex items-start">
              <span className="text-yellow-600 mr-2">⚠️</span>
              <div className="text-sm text-yellow-800">
                <strong>OBS:</strong> Den nye prisen vil være synlig for alle kunder umiddelbart.
                Kontroller at prisen er korrekt før du lagrer.
              </div>
            </div>
          </div>

          <div className="flex gap-4">
            <button
              type="submit"
              disabled={updatePriceMutation.isPending || !newPrice}
              className="flex-1 bg-blue-600 text-white font-bold py-4 px-6 rounded-xl hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {updatePriceMutation.isPending ? '⏳ Lagrer...' : '💾 Lagre ny pris'}
            </button>
            <button
              type="button"
              onClick={() => setNewPrice('')}
              className="px-6 py-4 border-2 border-slate-300 text-slate-700 font-bold rounded-xl hover:bg-slate-50 transition"
            >
              Avbryt
            </button>
          </div>
        </form>

        {updatePriceMutation.isError && (
          <div className="mt-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center text-red-800">
              <span className="text-2xl mr-2">❌</span>
              <div>
                <div className="font-bold">Feil ved oppdatering</div>
                <div className="text-sm">
                  {updatePriceMutation.error instanceof Error
                    ? updatePriceMutation.error.message
                    : 'En ukjent feil oppstod'}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Road Tax Section */}
      <div className="mt-8 bg-white rounded-2xl shadow-xl p-8 border border-slate-200">
        <h2 className="text-2xl font-bold text-slate-900 mb-6">🚗 Veitrafikkavgift</h2>

        {/* Current Road Tax Display */}
        <div className="bg-green-50 rounded-xl p-6 mb-6 border-2 border-green-200">
          <div className="text-center">
            <div className="text-sm text-slate-600 mb-2">Gjeldende avgift</div>
            {isLoadingRoadTax ? (
              <div className="text-3xl text-slate-400">Laster...</div>
            ) : roadTaxData ? (
              <>
                <div className="text-5xl font-bold text-green-600 mb-2">
                  {roadTaxData.taxPerLiterKr.toFixed(2)} kr
                </div>
                <div className="text-lg text-slate-600">per liter LPG</div>
                <div className="text-sm text-slate-500 mt-3">
                  ({roadTaxData.taxPerLiterOre} øre per liter)
                </div>
              </>
            ) : (
              <div className="text-sm text-slate-500">Ingen avgift satt</div>
            )}
          </div>
        </div>

        {showRoadTaxSuccess && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center text-green-800">
              <span className="text-2xl mr-2">✅</span>
              <div>
                <div className="font-bold">Avgift oppdatert!</div>
                <div className="text-sm">Ny veitrafikkavgift er nå aktiv</div>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleRoadTaxSubmit} className="space-y-6">
          <div>
            <label htmlFor="roadTax" className="block text-sm font-medium text-slate-700 mb-2">
              Ny veitrafikkavgift per liter (NOK)
            </label>
            <div className="relative">
              <input
                type="number"
                id="roadTax"
                step="0.01"
                min="0"
                value={newRoadTax}
                onChange={(e) => setNewRoadTax(e.target.value)}
                placeholder="2.00"
                className="w-full px-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 text-lg"
                disabled={updateRoadTaxMutation.isPending}
              />
              <span className="absolute right-4 top-3 text-slate-500 text-lg">kr</span>
            </div>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start">
              <span className="text-blue-600 mr-2">ℹ️</span>
              <div className="text-sm text-blue-800">
                <strong>Info:</strong> Veitrafikkavgiften legges til prisen per liter. Avgiften varierer vanligvis mellom 1.50-2.50 kr/L.
              </div>
            </div>
          </div>

          <div className="flex gap-4">
            <button
              type="submit"
              disabled={updateRoadTaxMutation.isPending || !newRoadTax}
              className="flex-1 bg-green-600 text-white font-bold py-4 px-6 rounded-xl hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {updateRoadTaxMutation.isPending ? '⏳ Lagrer...' : '💾 Lagre avgift'}
            </button>
            <button
              type="button"
              onClick={() => setNewRoadTax('')}
              className="px-6 py-4 border-2 border-slate-300 text-slate-700 font-bold rounded-xl hover:bg-slate-50 transition"
            >
              Avbryt
            </button>
          </div>
        </form>

        {updateRoadTaxMutation.isError && (
          <div className="mt-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center text-red-800">
              <span className="text-2xl mr-2">❌</span>
              <div>
                <div className="font-bold">Feil ved oppdatering</div>
                <div className="text-sm">
                  {updateRoadTaxMutation.error instanceof Error
                    ? updateRoadTaxMutation.error.message
                    : 'En ukjent feil oppstod'}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Price History (Future Enhancement) */}
      <div className="mt-8 bg-slate-50 rounded-2xl p-6 border border-slate-200">
        <h3 className="font-bold text-slate-700 mb-3">📊 Prishistorikk</h3>
        <p className="text-sm text-slate-500">
          Prishistorikk vil bli tilgjengelig i en fremtidig versjon.
        </p>
      </div>
    </div>
  );
}
