import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { dispenserApi } from '../api/dispenser';
import type { DispenserStateDto } from '../types/api';

type PaymentMethod = 'CARD' | 'CREDIT';

export function DispenserSimulator() {
  const queryClient = useQueryClient();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [includeRoadTax, setIncludeRoadTax] = useState<boolean>(true);
  const [settlementMessage, setSettlementMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Poll state every 500ms when delivering
  const { data: state, isLoading, error } = useQuery<DispenserStateDto>({
    queryKey: ['dispenser-state'],
    queryFn: dispenserApi.getState,
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.state === 'DELIVERING' ? 500 : 2000;
    },
  });

  const handleStartWithPayment = () => {
    // Clear any previous messages
    setErrorMessage(null);
    setSettlementMessage(null);
    // For demo purposes, all payment types start the pump immediately
    // In production, CARD/CREDIT would require payment authorization first
    unblockMutation.mutate(paymentMethod);
  };

  const unblockMutation = useMutation({
    mutationFn: dispenserApi.unblock,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispenser-state'] });
      setErrorMessage(null);
    },
    onError: (error: any) => {
      // Handle unpaid transaction error
      if (error.response?.status === 409) {
        const data = error.response.data;
        setErrorMessage(data.message || 'Du må betale for forrige fylling før du kan starte på nytt');
      } else {
        setErrorMessage('Kunne ikke starte pumping');
      }
    },
  });

  const stopMutation = useMutation({
    mutationFn: dispenserApi.stop,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispenser-state'] });
    },
  });

  const settleMutation = useMutation({
    mutationFn: () => dispenserApi.settle(1, paymentMethod),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispenser-state'] });
      setSettlementMessage('Betaling fullført! ✅');
      setErrorMessage(null);
      // Clear success message after 3 seconds
      setTimeout(() => setSettlementMessage(null), 3000);
    },
    onError: (error: any) => {
      if (error.response?.status === 404) {
        setErrorMessage('Ingen ubetalt transaksjon funnet');
      } else {
        setErrorMessage('Betalingsfeil: ' + (error.response?.data?.message || 'Ukjent feil'));
      }
      setTimeout(() => setErrorMessage(null), 5000);
    },
  });

  const getStateColor = (currentState?: string) => {
    switch (currentState) {
      case 'IDLE': return 'bg-gray-500';
      case 'READY': return 'bg-yellow-500';
      case 'DELIVERING': return 'bg-green-500 animate-pulse';
      case 'FINISHED': return 'bg-blue-500';
      case 'ERROR': return 'bg-red-500';
      default: return 'bg-gray-300';
    }
  };

  const getStateText = (currentState?: string) => {
    switch (currentState) {
      case 'IDLE': return 'Inaktiv';
      case 'READY': return 'Klar';
      case 'DELIVERING': return 'Leverer drivstoff...';
      case 'FINISHED': return 'Ferdig';
      case 'ERROR': return 'Feil';
      default: return 'Ukjent';
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="text-white text-xl">Laster...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="text-red-500 text-xl">
          Feil: Kan ikke koble til API
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white p-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold mb-2">LPG Pumpe Simulator</h1>
          <p className="text-gray-400">Norges Gass - Demo System</p>
        </div>

        {/* Main Display */}
        <div className="bg-gray-800 rounded-2xl shadow-2xl p-8 mb-8 border border-gray-700">
          {/* Status Indicator */}
          <div className="flex items-center justify-center mb-8">
            <div className={`w-24 h-24 rounded-full ${getStateColor(state?.state)} flex items-center justify-center shadow-lg`}>
              <span className="text-2xl font-bold">
                {state?.state?.charAt(0)}
              </span>
            </div>
          </div>

          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold mb-2">{getStateText(state?.state)}</h2>
            {state?.connected ? (
              <span className="text-green-400 text-sm">● Tilkoblet</span>
            ) : (
              <span className="text-red-400 text-sm">● Frakoblet</span>
            )}
          </div>

          {/* Main Metrics */}
          <div className="grid grid-cols-2 gap-6 mb-8">
            <div className="bg-gray-700 rounded-xl p-6 text-center">
              <div className="text-gray-400 text-sm mb-2">Liter</div>
              <div className="text-5xl font-bold text-blue-400">
                {state?.litres.toFixed(2)}
              </div>
              <div className="text-gray-400 text-xs mt-2">L</div>
            </div>

            <div className="bg-gray-700 rounded-xl p-6 text-center">
              <div className="text-gray-400 text-sm mb-2">Beløp</div>
              <div className="text-5xl font-bold text-green-400">
                {state?.amountToPay.toFixed(2)}
              </div>
              <div className="text-gray-400 text-xs mt-2">kr</div>
            </div>
          </div>

          {/* Price Info */}
          <div className="bg-gray-700 rounded-xl p-4 mb-8">
            <div className="flex justify-between items-center">
              <span className="text-gray-400">Pris per liter (basispris):</span>
              <span className="text-xl font-bold">{state?.pricePerLitre.toFixed(2)} kr/L</span>
            </div>
            {state?.roadTaxPerLiterOre && state.roadTaxPerLiterOre > 0 && (
              <div className="mt-3 pt-3 border-t border-gray-600">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-gray-400">🚗 Veitrafikkavgift:</span>
                  <span className={`font-bold ${includeRoadTax ? 'text-yellow-400' : 'text-gray-500 line-through'}`}>
                    {(state.roadTaxPerLiterOre / 100).toFixed(2)} kr/L
                  </span>
                </div>
                <div className="text-xs text-gray-500 mt-2 pt-2 border-t border-gray-600">
                  <div className="flex justify-between">
                    <span>Totalpris:</span>
                    <span className="font-bold text-white">
                      {includeRoadTax 
                        ? (state.pricePerLitre + state.roadTaxPerLiterOre / 100).toFixed(2)
                        : state.pricePerLitre.toFixed(2)} kr/L
                    </span>
                  </div>
                  {!includeRoadTax && (
                    <div className="text-xs text-orange-400 mt-1 text-right">
                      ⚠️ Avgift ikke inkludert
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Payment Method Selector */}
          <div className="mb-6">
            <label className="block text-sm text-gray-400 mb-2">Betalingsmetode</label>
            <select
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
              disabled={state?.state === 'DELIVERING'}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-3 focus:outline-none focus:border-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <option value="CARD">💳 Kort (simulert)</option>
              <option value="CREDIT">🏪 Stasjonskreditt</option>
            </select>
          </div>

          {/* Road Tax Toggle */}
          {state?.roadTaxPerLiterOre && state.roadTaxPerLiterOre > 0 && (
            <div className="mb-6">
              <label className="flex items-center gap-3 cursor-pointer bg-gray-700 rounded-lg px-4 py-3 border border-gray-600 hover:bg-gray-650 transition">
                <input 
                  type="checkbox" 
                  checked={includeRoadTax}
                  onChange={(e) => setIncludeRoadTax(e.target.checked)}
                  disabled={state?.state === 'DELIVERING'}
                  className="w-5 h-5 rounded accent-yellow-500 disabled:opacity-50"
                />
                <div className="flex-1">
                  <span className="text-white font-medium">🚗 Inkluder veitrafikkavgift</span>
                  <div className="text-xs text-gray-400 mt-1">
                    {includeRoadTax 
                      ? `+${(state.roadTaxPerLiterOre / 100).toFixed(2)} kr/L` 
                      : 'Avgift ikke inkludert'}
                  </div>
                </div>
              </label>
            </div>
          )}

          {/* Status Indicators */}
          <div className="grid grid-cols-2 gap-4 mb-8">
            <div className={`p-3 rounded-lg text-center text-sm ${state?.dayMode ? 'bg-yellow-900/30 text-yellow-300' : 'bg-gray-700 text-gray-500'}`}>
              {state?.dayMode ? '☀️ Dagmodus' : '🌙 Nattmodus'}
            </div>
            <div className={`p-3 rounded-lg text-center text-sm ${paymentMethod === 'CARD' ? 'bg-blue-900/30 text-blue-300' : 'bg-purple-900/30 text-purple-300'}`}>
              {paymentMethod === 'CARD' ? '💳 Kort' : '🏪 Kreditt'}
            </div>
          </div>

          {/* Error Message */}
          {errorMessage && (
            <div className="mb-6 bg-red-900/30 border border-red-500 rounded-xl p-4 text-center">
              <p className="text-red-300 font-bold">⚠️ {errorMessage}</p>
            </div>
          )}

          {/* Success Message */}
          {settlementMessage && (
            <div className="mb-6 bg-green-900/30 border border-green-500 rounded-xl p-4 text-center">
              <p className="text-green-300 font-bold">{settlementMessage}</p>
            </div>
          )}

          {/* Control Buttons */}
          <div className="grid grid-cols-3 gap-4">
            <button
              onClick={handleStartWithPayment}
              disabled={state?.state === 'DELIVERING' || state?.state === 'FINISHED' || unblockMutation.isPending}
              className={`${
                state?.state === 'FINISHED' 
                  ? 'bg-red-600 cursor-not-allowed' 
                  : state?.state === 'DELIVERING' || unblockMutation.isPending
                  ? 'bg-gray-600 cursor-not-allowed'
                  : 'bg-green-600 hover:bg-green-700'
              } text-white font-bold py-4 px-6 rounded-xl transition-colors shadow-lg`}
            >
              {unblockMutation.isPending ? '...' : state?.state === 'FINISHED' ? '🚫 Betal først' : '▶ Start'}
            </button>

            <button
              onClick={() => stopMutation.mutate()}
              disabled={state?.state !== 'DELIVERING' || stopMutation.isPending}
              className="bg-red-600 hover:bg-red-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold py-4 px-6 rounded-xl transition-colors shadow-lg"
            >
              {stopMutation.isPending ? '...' : '■ Stopp'}
            </button>

            <button
              onClick={() => settleMutation.mutate()}
              disabled={state?.state === 'DELIVERING' || settleMutation.isPending}
              className="bg-yellow-600 hover:bg-yellow-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold py-4 px-6 rounded-xl transition-colors shadow-lg"
            >
              {settleMutation.isPending ? '...' : '💳 Simuler betaling'}
            </button>
          </div>
        </div>

        {/* Info Panel */}
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h3 className="text-xl font-bold mb-4">Instruksjoner</h3>
          <ul className="space-y-2 text-gray-300">
            <li>• <strong>Start:</strong> Begynn levering av drivstoff</li>
            <li>• <strong>Stopp:</strong> Avslutt leveringen og vis totalt beløp</li>
            <li>• <strong>Reset:</strong> Tilbakestill pumpen til inaktiv tilstand</li>
            <li className="text-sm text-gray-500 mt-4">
              Simulert hastighet: 0.5 L/s
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
