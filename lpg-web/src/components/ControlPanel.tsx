import { useState, useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

// API configuration
// In production (Fat JAR), use same origin. In development, use localhost:9001
const EMULATOR_BASE_URL = import.meta.env.VITE_EMULATOR_BASE_URL || 
  (import.meta.env.PROD ? window.location.origin : 'http://localhost:9001');
const WS_BASE_URL = EMULATOR_BASE_URL.replace(/^http/, 'ws');

// Types
interface PumpStatus {
  state: string;
  address: number;
  volumeLitres: number;
  amountKr: number;
  pricePerLitreKr: number;
  nozzleLifted: boolean;
  hasPendingTransaction: boolean;
}

interface LogEntry {
  channel: string;
  timestamp: string;
  level: string;
  logger: string;
  message: string;
}

type LogChannel = 'api' | 'emulator' | 'protocol';

// API functions
const pumpApi = {
  getStatus: async (address: number = 1): Promise<PumpStatus> => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/status`);
    return res.json();
  },
  unblock: async (address: number = 1) => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/unblock`, {
      method: 'POST'
    });
    return res.json();
  },
  block: async (address: number = 1) => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/block`, {
      method: 'POST'
    });
    return res.json();
  },
  settle: async (address: number = 1, method: string = 'CARD') => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/settle/${address}?method=${method}`, {
      method: 'POST'
    });
    return res.json();
  }
};

export function ControlPanel() {
  const queryClient = useQueryClient();
  
  // Pump status
  const { data: pumpStatus, isLoading } = useQuery({
    queryKey: ['pump-status'],
    queryFn: () => pumpApi.getStatus(1),
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.state === 'PUMPING' ? 500 : 2000;
    }
  });

  // Mutations
  const unblockMutation = useMutation({
    mutationFn: () => pumpApi.unblock(1),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pump-status'] })
  });

  const blockMutation = useMutation({
    mutationFn: () => pumpApi.block(1),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pump-status'] })
  });

  const settleMutation = useMutation({
    mutationFn: () => pumpApi.settle(1, 'CARD'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pump-status'] })
  });

  // Logs state
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [activeChannel, setActiveChannel] = useState<LogChannel | 'all'>('all');
  const [wsConnected, setWsConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const logsEndRef = useRef<HTMLDivElement>(null);

  // WebSocket connection
  useEffect(() => {
    const ws = new WebSocket(`${WS_BASE_URL}/ws/logs`);
    wsRef.current = ws;

    ws.onopen = () => {
      setWsConnected(true);
      // Subscribe to all channels
      ws.send(JSON.stringify({
        action: 'subscribe',
        channels: ['api', 'emulator', 'protocol']
      }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.channel) {
          setLogs(prev => [...prev.slice(-499), data as LogEntry]);
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
  }, []);

  // Auto-scroll logs
  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  // Filter logs by channel
  const filteredLogs = activeChannel === 'all' 
    ? logs 
    : logs.filter(log => log.channel === activeChannel);

  // Get state color
  const getStateColor = (state?: string) => {
    switch (state) {
      case 'IDLE': return 'bg-gray-500';
      case 'AUTHORIZED': return 'bg-yellow-500';
      case 'PUMPING': return 'bg-green-500 animate-pulse';
      case 'STOPPED': return 'bg-blue-500';
      case 'PAYMENT_PENDING': return 'bg-orange-500';
      case 'ERROR': return 'bg-red-500';
      default: return 'bg-gray-400';
    }
  };

  const getStateText = (state?: string) => {
    switch (state) {
      case 'IDLE': return 'Klar';
      case 'AUTHORIZED': return 'Autorisert';
      case 'PUMPING': return 'Leverer...';
      case 'STOPPED': return 'Stoppet';
      case 'PAYMENT_PENDING': return 'Venter betaling';
      case 'ERROR': return 'Feil';
      default: return 'Ukjent';
    }
  };

  const getLevelColor = (level: string) => {
    switch (level) {
      case 'ERROR': return 'text-red-400';
      case 'WARN': return 'text-yellow-400';
      case 'INFO': return 'text-green-400';
      case 'DEBUG': return 'text-gray-400';
      default: return 'text-gray-300';
    }
  };

  const getChannelColor = (channel: string) => {
    switch (channel) {
      case 'api': return 'bg-blue-600';
      case 'emulator': return 'bg-purple-600';
      case 'protocol': return 'bg-orange-600';
      default: return 'bg-gray-600';
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="text-white text-xl">Laster...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white p-4">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="text-center mb-6">
          <h1 className="text-4xl font-bold mb-2">🔧 Kontrollpanel</h1>
          <p className="text-gray-400">Fri Pumpe - Felt-testing</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Left column: Pump Control */}
          <div className="space-y-6">
            {/* Status Card */}
            <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold">Pumpe #1</h2>
                <div className="flex items-center gap-2">
                  <div className={`w-3 h-3 rounded-full ${wsConnected ? 'bg-green-500' : 'bg-red-500'}`} />
                  <span className="text-sm text-gray-400">
                    {wsConnected ? 'Tilkoblet' : 'Frakoblet'}
                  </span>
                </div>
              </div>

              {/* Status indicator */}
              <div className="flex items-center justify-center mb-6">
                <div className={`w-32 h-32 rounded-full ${getStateColor(pumpStatus?.state)} flex items-center justify-center shadow-lg`}>
                  <span className="text-4xl">⛽</span>
                </div>
              </div>

              <div className="text-center mb-6">
                <h3 className="text-3xl font-bold mb-2">{getStateText(pumpStatus?.state)}</h3>
                <p className="text-gray-400 text-sm">{pumpStatus?.state}</p>
              </div>

              {/* Metrics */}
              <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-gray-700 rounded-lg p-4 text-center">
                  <div className="text-gray-400 text-sm">Volum</div>
                  <div className="text-3xl font-bold text-blue-400">
                    {pumpStatus?.volumeLitres.toFixed(2)}
                  </div>
                  <div className="text-gray-400 text-xs">liter</div>
                </div>
                <div className="bg-gray-700 rounded-lg p-4 text-center">
                  <div className="text-gray-400 text-sm">Beløp</div>
                  <div className="text-3xl font-bold text-green-400">
                    {pumpStatus?.amountKr.toFixed(2)}
                  </div>
                  <div className="text-gray-400 text-xs">kr</div>
                </div>
              </div>

              {/* Price info */}
              <div className="bg-gray-700 rounded-lg p-3 mb-6 text-center">
                <span className="text-gray-400">Pris: </span>
                <span className="font-bold">{pumpStatus?.pricePerLitreKr.toFixed(2)} kr/L</span>
              </div>

              {/* Control Buttons */}
              <div className="space-y-3">
                <button
                  onClick={() => unblockMutation.mutate()}
                  disabled={pumpStatus?.state === 'PUMPING' || pumpStatus?.hasPendingTransaction || unblockMutation.isPending}
                  className={`w-full py-4 rounded-xl font-bold text-xl transition-colors ${
                    pumpStatus?.state === 'PUMPING' || pumpStatus?.hasPendingTransaction
                      ? 'bg-gray-600 cursor-not-allowed'
                      : 'bg-green-600 hover:bg-green-700'
                  }`}
                >
                  {unblockMutation.isPending ? '...' : '🔓 FRI PUMPE'}
                </button>

                <button
                  onClick={() => blockMutation.mutate()}
                  disabled={pumpStatus?.state !== 'PUMPING' || blockMutation.isPending}
                  className={`w-full py-4 rounded-xl font-bold text-xl transition-colors ${
                    pumpStatus?.state !== 'PUMPING'
                      ? 'bg-gray-600 cursor-not-allowed'
                      : 'bg-red-600 hover:bg-red-700'
                  }`}
                >
                  {blockMutation.isPending ? '...' : '🛑 STOPP'}
                </button>

                {pumpStatus?.hasPendingTransaction && (
                  <button
                    onClick={() => settleMutation.mutate()}
                    disabled={settleMutation.isPending}
                    className="w-full py-4 rounded-xl font-bold text-xl bg-yellow-600 hover:bg-yellow-700 transition-colors"
                  >
                    {settleMutation.isPending ? '...' : '💳 SIMULER BETALING'}
                  </button>
                )}
              </div>

              {/* Status badges */}
              <div className="mt-4 flex flex-wrap gap-2 justify-center">
                {pumpStatus?.nozzleLifted && (
                  <span className="px-3 py-1 bg-blue-600 rounded-full text-sm">🚰 Dyse løftet</span>
                )}
                {pumpStatus?.hasPendingTransaction && (
                  <span className="px-3 py-1 bg-orange-600 rounded-full text-sm">💳 Betaling venter</span>
                )}
              </div>
            </div>
          </div>

          {/* Right column: Logs */}
          <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold">📋 Logger</h2>
              <button
                onClick={() => setLogs([])}
                className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm"
              >
                Tøm
              </button>
            </div>

            {/* Channel tabs */}
            <div className="flex gap-2 mb-4">
              {(['all', 'api', 'emulator', 'protocol'] as const).map(channel => (
                <button
                  key={channel}
                  onClick={() => setActiveChannel(channel)}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    activeChannel === channel
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  {channel === 'all' ? 'Alle' : channel.charAt(0).toUpperCase() + channel.slice(1)}
                  <span className="ml-2 text-xs opacity-70">
                    ({channel === 'all' ? logs.length : logs.filter(l => l.channel === channel).length})
                  </span>
                </button>
              ))}
            </div>

            {/* Log entries */}
            <div className="bg-gray-900 rounded-lg p-4 h-[500px] overflow-y-auto font-mono text-sm">
              {filteredLogs.length === 0 ? (
                <div className="text-gray-500 text-center py-8">
                  Ingen logger ennå...
                </div>
              ) : (
                filteredLogs.map((log, idx) => (
                  <div key={idx} className="flex gap-2 py-1 border-b border-gray-800 hover:bg-gray-800/50">
                    <span className={`px-2 py-0.5 rounded text-xs ${getChannelColor(log.channel)}`}>
                      {log.channel}
                    </span>
                    <span className={`${getLevelColor(log.level)} w-12`}>
                      {log.level}
                    </span>
                    <span className="text-gray-500 w-24 truncate">
                      {log.logger}
                    </span>
                    <span className="text-gray-200 flex-1">
                      {log.message}
                    </span>
                  </div>
                ))
              )}
              <div ref={logsEndRef} />
            </div>
          </div>
        </div>

        {/* Instructions */}
        <div className="mt-6 bg-gray-800 rounded-xl p-6 border border-gray-700">
          <h3 className="text-xl font-bold mb-4">📖 Instruksjoner - Felt-testing</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-gray-300">
            <div>
              <h4 className="font-bold text-green-400 mb-2">🔓 Fri Pumpe</h4>
              <p className="text-sm">
                Frigir pumpen for levering. Tilsvarer UNBLOCK-kommando (0x77) i EHL-protokollen.
                Brukes når PLS/terminal ikke er tilgjengelig.
              </p>
            </div>
            <div>
              <h4 className="font-bold text-red-400 mb-2">🛑 Stopp</h4>
              <p className="text-sm">
                Stopper pågående levering. Tilsvarer BLOCK-kommando (0x69).
                Transaksjonen fryses og venter på betaling.
              </p>
            </div>
            <div>
              <h4 className="font-bold text-yellow-400 mb-2">💳 Betaling</h4>
              <p className="text-sm">
                Simulerer kortbetaling. Nullstiller transaksjonen og klargjør pumpen
                for neste kunde.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
