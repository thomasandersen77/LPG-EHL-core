import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useEffect, useState, useRef } from 'react';

const API_URL = import.meta.env.VITE_API_URL || '/api/v1';
const EMULATOR_BASE_URL = import.meta.env.VITE_EMULATOR_BASE_URL || '';
const WS_BASE_URL = EMULATOR_BASE_URL.replace(/^http/, 'ws');

interface LiveStatus {
  state: string;
  volumeLiters: number;
  amountKr: number;
  pricePerLiter: number;
  isActive: boolean;
  message: string;
}

export function FuelingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [hasFinished, setHasFinished] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);

  // Poll dispenser status (fallback if WS not connected)
  const { data: status } = useQuery<LiveStatus>({
    queryKey: ['dispenser-live-status'],
    queryFn: async () => {
      const response = await axios.get(`${API_URL}/dispensers/status`);
      return response.data;
    },
    refetchInterval: wsConnected ? false : 1000, // Only poll if WS not connected
  });

  // WebSocket connection for real-time updates
  useEffect(() => {
    const ws = new WebSocket(`${WS_BASE_URL}/ws/logs`);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      ws.send(JSON.stringify({
        action: 'subscribe',
        channels: ['service']
      }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        // Handle pump_update for real-time display
        if (data.type === 'pump_update') {
          queryClient.setQueryData(['dispenser-live-status'], (old: LiveStatus | undefined) => ({
            ...old,
            state: data.state === 'PUMPING' ? 'DISPENSING' : data.state,
            volumeLiters: data.volumeLitres,
            amountKr: data.amountKr,
            pricePerLiter: data.pricePerLitreKr,
            isActive: data.state === 'PUMPING',
            message: getMessageForState(data.state)
          } as LiveStatus));
        }
      } catch (e) {
        console.error('Error parsing WebSocket message:', e);
      }
    };

    ws.onclose = () => setWsConnected(false);
    ws.onerror = () => setWsConnected(false);

    return () => {
      ws.close();
    };
  }, [queryClient]);

  // Helper function to get message for state
  function getMessageForState(state: string): string {
    switch (state) {
      case 'PUMPING': return 'Fyller...';
      case 'FINISHED': 
      case 'PAYMENT_PENDING': return 'Fylling fullført';
      case 'IDLE': return 'Venter...';
      case 'READY_TO_PUMP': return 'Klar til fylling';
      default: return state;
    }
  }

  // When state changes to FINISHED or PAYMENT_PENDING, wait 10 seconds then go home
  useEffect(() => {
    const isFinished = status?.state === 'FINISHED' || status?.state === 'PAYMENT_PENDING';
    if (isFinished && !hasFinished) {
      setHasFinished(true);
      const timer = setTimeout(() => {
        navigate('/');
      }, 10000);
      return () => clearTimeout(timer);
    }
  }, [status?.state, hasFinished, navigate]);

  const volumeLiters = status?.volumeLiters || 0;
  const amountKr = status?.amountKr || 0;
  const pricePerLiter = status?.pricePerLiter || 0;
  const state = status?.state || 'IDLE';
  const message = status?.message || 'Venter...';

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
      <div className="w-full max-w-4xl">
        {/* Status Message */}
        <div className="text-center mb-8">
          <div className={`inline-block px-8 py-4 rounded-2xl text-2xl font-bold ${
            state === 'DISPENSING' ? 'bg-green-500 text-white animate-pulse' :
            (state === 'FINISHED' || state === 'PAYMENT_PENDING') ? 'bg-blue-500 text-white' :
            'bg-gray-300 text-gray-700'
          }`}>
            {message}
          </div>
        </div>

        {/* Main Display - Volume */}
        <div className="bg-white rounded-3xl shadow-2xl p-12 mb-6">
          <div className="text-center">
            <div className="text-2xl text-slate-600 mb-4">Volum</div>
            <div className="text-8xl font-bold text-slate-900 mb-2 font-mono">
              {volumeLiters.toFixed(2)}
            </div>
            <div className="text-3xl text-slate-500">liter</div>
          </div>
        </div>

        {/* Main Display - Amount */}
        <div className="bg-white rounded-3xl shadow-2xl p-12 mb-6">
          <div className="text-center">
            <div className="text-2xl text-slate-600 mb-4">Beløp</div>
            <div className="text-8xl font-bold text-green-600 mb-2 font-mono">
              {amountKr.toFixed(2)}
            </div>
            <div className="text-3xl text-slate-500">kr</div>
          </div>
        </div>

        {/* Price Info */}
        <div className="text-center text-slate-600 text-lg">
          Pris: {pricePerLiter.toFixed(2)} kr/L
        </div>

        {/* Finished State */}
        {(state === 'FINISHED' || state === 'PAYMENT_PENDING') && (
          <div className="mt-8 text-center space-y-4">
            <div className="text-3xl">✅</div>
            <div className="text-2xl font-bold text-slate-900">Takk for handelen!</div>
            <div className="text-slate-600">Går tilbake til start om 10 sekunder...</div>
            <button
              onClick={() => navigate('/')}
              className="px-8 py-4 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition font-bold"
            >
              Gå til start nå
            </button>
          </div>
        )}

        {/* Debug Info (only show in development) */}
        {import.meta.env.DEV && (
          <details className="mt-8 bg-slate-100 rounded-xl p-4">
            <summary className="cursor-pointer text-sm text-slate-600 font-mono">
              🔍 Debug Info
              <span className={`ml-2 inline-block w-2 h-2 rounded-full ${wsConnected ? 'bg-green-500' : 'bg-red-500'}`} />
              <span className="ml-1 text-xs">{wsConnected ? 'WS' : 'Polling'}</span>
            </summary>
            <pre className="mt-4 text-xs overflow-auto">
              {JSON.stringify({ ...status, wsConnected }, null, 2)}
            </pre>
          </details>
        )}
      </div>
    </div>
  );
}
