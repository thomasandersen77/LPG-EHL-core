import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { useSmoothCounter } from '../hooks/useSmoothCounter';

// API base URL
const API_BASE_URL = import.meta.env.VITE_EMULATOR_BASE_URL || import.meta.env.VITE_API_BASE_URL || window.location.origin;
const WS_BASE_URL = API_BASE_URL.replace(/^http/, 'ws');

// Pump API
const pumpApi = {
  getStatus: async () => {
    const res = await fetch(`${API_BASE_URL}/api/v1/emulator/pump/status`);
    return res.json();
  }
};

export function DispenserSimulator() {
  const queryClient = useQueryClient();
  const wsRef = useRef<WebSocket | null>(null);
  const [wsConnected, setWsConnected] = useState(false);

  // Poll pump status – same query key as ControlPanel for shared cache
  const { data: pumpStatus, isLoading, error } = useQuery({
    queryKey: ['pump-status'],
    queryFn: () => pumpApi.getStatus(),
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.state === 'PUMPING' ? 500 : wsConnected ? false : 2000;
    }
  });

  // WebSocket for real-time pump_update (PLS simulator / hardware)
  useEffect(() => {
    const ws = new WebSocket(`${WS_BASE_URL}/ws/logs`);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      ws.send(JSON.stringify({ action: 'subscribe', channels: ['service'] }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'pump_update' || data.type === 'fueling_update') {
          const volumeLitres = Number(data.volumeLitres ?? data.volumeLiters ?? 0);
          const amountKr = Number(data.amountKr ?? data.amount ?? 0);
          const pricePerLitreKr = Number(data.pricePerLitreKr ?? data.pricePerLiterKr ?? 0);
          queryClient.setQueryData(['pump-status'], (old: Record<string, unknown> | undefined) => ({
            ...old,
            state: data.state ?? old?.state,
            address: data.address ?? old?.address,
            volumeLitres,
            amountKr,
            pricePerLitreKr: pricePerLitreKr || old?.pricePerLitreKr,
            nozzleLifted: data.nozzleLifted ?? old?.nozzleLifted,
            hasPendingTransaction: data.hasPendingTransaction ?? old?.hasPendingTransaction
          }));
        }
      } catch (e) {
        console.error('Error parsing WebSocket message:', e);
      }
    };

    ws.onclose = () => setWsConnected(false);
    ws.onerror = () => setWsConnected(false);

    return () => {
      ws.close();
      wsRef.current = null;
    };
  }, [queryClient]);

  const rawLitres = pumpStatus?.volumeLitres || 0;
  const rawAmount = pumpStatus?.amountKr || 0;
  const isPumping = pumpStatus?.state === 'PUMPING';

  const smoothLitres = useSmoothCounter(rawLitres, isPumping);
  const smoothAmount = useSmoothCounter(rawAmount, isPumping);

  const state = {
    state: pumpStatus?.state === 'PUMPING' ? 'DELIVERING' :
      pumpStatus?.state === 'PAYMENT_PENDING' ? 'FINISHED' :
        pumpStatus?.state === 'READY_TO_PUMP' ? 'READY' :
          pumpStatus?.state || 'IDLE',
    connected: true,
    litres: smoothLitres,
    amountToPay: smoothAmount,
    pricePerLitre: pumpStatus?.pricePerLitreKr || 0
  };

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
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold mb-2">📊 Pumpe Status</h1>
          <p className="text-gray-400">Sanntids visning - Pumpe #1</p>
        </div>

        {/* Control Link */}
        <div className="text-center mb-8">
          <Link
            to="/control"
            className="inline-flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-xl transition-colors"
          >
            🔧 Gå til Kontrollpanel
          </Link>
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
              <span className="text-gray-400">Pris per liter:</span>
              <span className="text-xl font-bold">{state?.pricePerLitre.toFixed(2)} kr/L</span>
            </div>
          </div>

          {/* Status Messages - READ ONLY */}
          <div className="space-y-4">
            {pumpStatus?.state === 'IDLE' && (
              <div className="text-center py-6 bg-gray-700/50 rounded-xl border border-gray-600">
                <div className="text-5xl mb-3">⏳</div>
                <p className="text-gray-400 text-lg font-bold">Venter på kunde</p>
                <p className="text-gray-500 text-sm mt-2">Bruk kontrollpanelet for å starte</p>
              </div>
            )}

            {pumpStatus?.state === 'READY_TO_PUMP' && (
              <div className="text-center py-6 bg-green-900/30 rounded-xl border border-green-500">
                <div className="text-5xl mb-3">✅</div>
                <p className="text-green-400 text-lg font-bold">Pumpe frigjort!</p>
                <p className="text-gray-400 text-sm mt-2">Venter på at kunde starter pumping</p>
              </div>
            )}

            {pumpStatus?.state === 'PUMPING' && (
              <div className="text-center py-6 bg-blue-900/30 rounded-xl border border-blue-500 animate-pulse">
                <div className="text-5xl mb-3">⛽</div>
                <p className="text-blue-400 text-lg font-bold">Pumping pågår...</p>
                <p className="text-gray-400 text-sm mt-2">Levering i gang</p>
              </div>
            )}

            {pumpStatus?.state === 'PAYMENT_PENDING' && (
              <div className="text-center py-6 bg-orange-900/30 rounded-xl border border-orange-500">
                <div className="text-5xl mb-3">💳</div>
                <p className="text-orange-400 text-lg font-bold">Venter på betaling</p>
                <p className="text-gray-400 text-sm mt-2">Transaksjonen venter på bekreftelse</p>
              </div>
            )}
          </div>
        </div>

        {/* Info Panel */}
        <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h3 className="text-xl font-bold mb-4">ℹ️ Om denne siden</h3>
          <p className="text-gray-400 mb-4">
            Dette er en sanntidsvisning av pumpe #1. For å kontrollere pumpen (starte, stoppe, bekrefte betaling), bruk kontrollpanelet.
          </p>
          <Link
            to="/control"
            className="block text-center bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-6 rounded-lg transition-colors"
          >
            🔧 Åpne Kontrollpanel
          </Link>
        </div>
      </div>
    </div>
  );
}
