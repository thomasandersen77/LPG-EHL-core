import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useEffect, useState } from 'react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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
  const [hasFinished, setHasFinished] = useState(false);

  // Poll dispenser status every second
  const { data: status } = useQuery<LiveStatus>({
    queryKey: ['dispenser-live-status'],
    queryFn: async () => {
      const response = await axios.get(`${API_URL}/dispensers/status`);
      return response.data;
    },
    refetchInterval: 1000, // Poll every 1 second
  });

  // When state changes to FINISHED, wait 10 seconds then go home
  useEffect(() => {
    if (status?.state === 'FINISHED' && !hasFinished) {
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
            state === 'FINISHED' ? 'bg-blue-500 text-white' :
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
        {state === 'FINISHED' && (
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
            </summary>
            <pre className="mt-4 text-xs overflow-auto">
              {JSON.stringify(status, null, 2)}
            </pre>
          </details>
        )}
      </div>
    </div>
  );
}
