import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';

type PaymentMethod = 'CARD' | 'CREDIT';

// API base URL
// Both webapp frontend and API run on port 8080
const API_BASE_URL = import.meta.env.VITE_EMULATOR_BASE_URL || 
  (import.meta.env.PROD ? window.location.origin : 'http://localhost:8080');


// Pump API (aligned with ControlPanel)
const pumpApi = {
  getStatus: async (address: number = 1) => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/${address}/status`);
    return res.json();
  },
  cardSwipe: async (address: number = 1, maxAmountKr: number = 2000) => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/${address}/card-swipe`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        maxAmountKr, 
        triggeredBy: 'SIMULATOR_GUI', 
        paymentMethod: 'SIMULATION',
        immediate: true  // GUI-modus: Send UNBLOCK direkte
      })
    });
    return res.json();
  },
  startPumping: async (address: number = 1) => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/${address}/start-pumping`, {
      method: 'POST'
    });
    return res.json();
  },
  confirmPayment: async (address: number = 1) => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/${address}/confirm-payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ paymentMethod: 'SIMULATION' })
    });
    return res.json();
  },
  block: async (address: number = 1) => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/${address}/block`, {
      method: 'POST'
    });
    return res.json();
  }
};

export function DispenserSimulator() {
  const queryClient = useQueryClient();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [includeRoadTax, setIncludeRoadTax] = useState<boolean>(true);
  const [settlementMessage, setSettlementMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [maxAmount, setMaxAmount] = useState(2000);
  const [countdown, setCountdown] = useState<number | null>(null);

  // Poll pump status (aligned with ControlPanel)
  const { data: pumpStatus, isLoading, error } = useQuery({
    queryKey: ['pump-status-sim'],
    queryFn: () => pumpApi.getStatus(1),
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.state === 'PUMPING' ? 500 : 2000;
    }
  });
  
  // Use legacy state for backwards compatibility with UI
  const state = {
    state: pumpStatus?.state === 'PUMPING' ? 'DELIVERING' : 
           pumpStatus?.state === 'PAYMENT_PENDING' ? 'FINISHED' :
           pumpStatus?.state === 'READY_TO_PUMP' ? 'READY' :
           pumpStatus?.state || 'IDLE',
    connected: true,
    litres: pumpStatus?.volumeLitres || 0,
    amountToPay: pumpStatus?.amountKr || 0,
    pricePerLitre: pumpStatus?.pricePerLitreKr || 0,
    roadTaxPerLiterOre: 0,
    dayMode: true
  };

  // Card swipe mutation
  const cardSwipeMutation = useMutation({
    mutationFn: () => pumpApi.cardSwipe(1, maxAmount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status-sim'] });
      setErrorMessage(null);
    },
    onError: () => setErrorMessage('Kunne ikke simulere kortdragning')
  });
  
  // Start pumping mutation
  const startPumpingMutation = useMutation({
    mutationFn: () => pumpApi.startPumping(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status-sim'] });
      setErrorMessage(null);
    },
    onError: (error: any) => {
      setErrorMessage(error.message || 'Kunne ikke starte pumping');
    }
  });

  // Confirm payment mutation
  const confirmPaymentMutation = useMutation({
    mutationFn: () => pumpApi.confirmPayment(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status-sim'] });
      setSettlementMessage('Betaling bekreftet! ✅');
      setTimeout(() => setSettlementMessage(null), 3000);
    }
  });

  // Block/stop mutation
  const blockMutation = useMutation({
    mutationFn: () => pumpApi.block(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status-sim'] });
    }
  });
  
  // 60-second countdown for READY_TO_PUMP state
  useEffect(() => {
    if (pumpStatus?.state === 'READY_TO_PUMP') {
      setCountdown(60);
      
      const interval = setInterval(() => {
        setCountdown(prev => {
          if (prev === null || prev <= 1) {
            clearInterval(interval);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      
      return () => clearInterval(interval);
    } else {
      setCountdown(null);
    }
  }, [pumpStatus?.state]);

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


          {/* Control Buttons - Kun kortdragning-simulering */}
          <div className="space-y-4">
            {/* Kort swipe - Alltid tilgjengelig */}
            <div className="space-y-3">
              <div className="flex gap-3">
                <input
                  type="number"
                  value={maxAmount}
                  onChange={(e) => setMaxAmount(Number(e.target.value))}
                  className="flex-1 bg-gray-700 border border-gray-600 rounded-lg px-4 py-3 text-white"
                  placeholder="Maks beløp"
                />
                <span className="self-center text-gray-400 text-lg">kr</span>
              </div>
              <button
                onClick={() => cardSwipeMutation.mutate()}
                disabled={cardSwipeMutation.isPending || (pumpStatus?.state !== 'IDLE' && !pumpStatus?.hasPendingTransaction)}
                className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 text-white font-bold py-4 px-6 rounded-xl transition-colors shadow-lg text-xl"
              >
                {cardSwipeMutation.isPending ? '...' : '💳 SIMULER KORTDRAGNING'}
              </button>
            </div>
            
            {/* Status melding */}
            {pumpStatus?.state === 'READY_TO_PUMP' && (
              <div className="text-center py-6 bg-green-900/30 rounded-xl border border-green-500">
                <div className="text-5xl mb-3">✅</div>
                <p className="text-green-400 text-lg font-bold">Pumpe frigjort!</p>
                <p className="text-gray-400 text-sm mt-2">Gå til /control for å starte pumping</p>
              </div>
            )}
            
            {pumpStatus?.state === 'PUMPING' && (
              <div className="text-center py-6 bg-blue-900/30 rounded-xl border border-blue-500">
                <div className="text-5xl mb-3">⛽</div>
                <p className="text-blue-400 text-lg font-bold">Pumping pågår...</p>
                <p className="text-gray-400 text-sm mt-2">Gå til /control for å stoppe</p>
              </div>
            )}
            
            {pumpStatus?.state === 'PAYMENT_PENDING' && (
              <div className="text-center py-6 bg-orange-900/30 rounded-xl border border-orange-500">
                <div className="text-5xl mb-3">💳</div>
                <p className="text-orange-400 text-lg font-bold">Venter på betaling</p>
                <p className="text-gray-400 text-sm mt-2">Gå til /control eller /payment for å bekrefte</p>
              </div>
            )}
          </div>
        </div>

        {/* Info Panel */}
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h3 className="text-xl font-bold mb-4">💳 Betalingsflyt</h3>
          <div className="grid grid-cols-3 gap-4 text-center text-sm">
            <div className="p-3 bg-blue-900/30 rounded-lg border border-blue-500">
              <div className="text-2xl mb-1">1️⃣</div>
              <div className="font-bold text-blue-400">Dra kort</div>
              <div className="text-gray-400">Pumpe frigjort</div>
            </div>
            <div className="p-3 bg-green-900/30 rounded-lg border border-green-500">
              <div className="text-2xl mb-1">2️⃣</div>
              <div className="font-bold text-green-400">Fyll</div>
              <div className="text-gray-400">Ta LPG</div>
            </div>
            <div className="p-3 bg-orange-900/30 rounded-lg border border-orange-500">
              <div className="text-2xl mb-1">3️⃣</div>
              <div className="font-bold text-orange-400">Betal</div>
              <div className="text-gray-400">Avslutt</div>
            </div>
          </div>
          <p className="text-gray-500 text-sm mt-4 text-center">
            Simulert hastighet: 0.5 L/s
          </p>
        </div>
      </div>
    </div>
  );
}
