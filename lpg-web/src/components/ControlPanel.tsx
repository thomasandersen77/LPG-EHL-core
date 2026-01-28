import { useState, useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
// Build: 2026-01-22T22:50 - Fixed AUTHORIZED_WAITING state display

// API configuration
// Both webapp frontend and API run on port 8080
const EMULATOR_BASE_URL = import.meta.env.VITE_EMULATOR_BASE_URL || 
  (import.meta.env.PROD ? window.location.origin : 'http://localhost:8080');
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

type LogChannel = 'api' | 'service' | 'emulator' | 'protocol';

// API functions
const pumpApi = {
  getStatus: async (address: number = 1): Promise<PumpStatus> => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/status`);
    return res.json();
  },
  cardSwipe: async (address: number = 1, maxAmountKr: number = 2000) => {
    // NOTE: immediate parameter removed - card-swipe should NEVER auto-unblock
    // User must click "FRI DISPENSER" button to send UNBLOCK
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/card-swipe`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        maxAmountKr, 
        triggeredBy: 'WEBAPP_GUI', 
        paymentMethod: 'SIMULATION'
      })
    });
    return res.json();
  },
  startPumping: async (address: number = 1) => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/start-pumping`, {
      method: 'POST'
    });
    return res.json();
  },
  confirmPayment: async (address: number = 1) => {
    const res = await fetch(`${EMULATOR_BASE_URL}/api/v1/emulator/pump/${address}/confirm-payment`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ paymentMethod: 'SIMULATION' })
    });
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
  const [maxAmount, setMaxAmount] = useState(2000);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);
  
  // Pump status
  const { data: pumpStatus, isLoading } = useQuery({
    queryKey: ['pump-status'],
    queryFn: () => pumpApi.getStatus(1),
    refetchInterval: (query) => {
      const data = query.state.data;
      return data?.state === 'PUMPING' ? 500 : 2000;
    }
  });

  // Card swipe mutation
  const cardSwipeMutation = useMutation({
    mutationFn: () => pumpApi.cardSwipe(1, maxAmount),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
      setErrorMessage(null);
    },
    onError: (error: any) => {
      setErrorMessage(error.message || 'Kunne ikke simulere kortdragning');
    }
  });
  
  // Start pumping mutation
  const startPumpingMutation = useMutation({
    mutationFn: () => pumpApi.startPumping(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
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
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
      setErrorMessage(null);
    }
  });

  // Block/stop mutation
  const blockMutation = useMutation({
    mutationFn: () => pumpApi.block(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
      queryClient.invalidateQueries({ queryKey: ['authorization'] });
    }
  });

  // Unblock mutation (FRI DISPENSER - called AFTER card swipe)
  const unblockMutation = useMutation({
    mutationFn: () => pumpApi.unblock(1),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pump-status'] });
      setErrorMessage(null);
    },
    onError: (error: any) => {
      setErrorMessage(error.message || 'Kunne ikke frigjøre pumpen');
    }
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
        channels: ['all']  // Subscribe to ALL channel to receive all logs
      }));
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        // Handle log messages
        if (data.channel) {
          setLogs(prev => [...prev.slice(-499), data as LogEntry]);
        }
        
        // Handle pump_update - update cache directly for real-time display
        if (data.type === 'pump_update') {
          queryClient.setQueryData(['pump-status'], (old: PumpStatus | undefined) => ({
            ...old,
            state: data.state,
            address: data.address,
            volumeLitres: data.volumeLitres,
            amountKr: data.amountKr,
            pricePerLitreKr: data.pricePerLitreKr,
            nozzleLifted: data.nozzleLifted,
            hasPendingTransaction: data.hasPendingTransaction
          } as PumpStatus));
        }
        
        // Handle price_update - refresh status
        if (data.type === 'price_update') {
          queryClient.invalidateQueries({ queryKey: ['pump-status'] });
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
  
  // 60-second countdown for AUTHORIZED_WAITING and READY_TO_PUMP states
  useEffect(() => {
    if (pumpStatus?.state === 'AUTHORIZED_WAITING' || pumpStatus?.state === 'READY_TO_PUMP') {
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

  // Filter logs by channel
  const filteredLogs = activeChannel === 'all' 
    ? logs 
    : logs.filter(log => log.channel === activeChannel);

  // Get state color
  const getStateColor = (state?: string) => {
    switch (state) {
      case 'IDLE': return 'bg-gray-500';
      case 'AUTHORIZED_WAITING': return 'bg-yellow-500 animate-pulse';
      case 'READY_TO_PUMP': return 'bg-green-500';
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
      case 'AUTHORIZED_WAITING': return 'Kort registrert';
      case 'READY_TO_PUMP': return 'Pumpe frigjort';
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
      case 'service': return 'bg-green-600';
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

              {/* Error message */}
              {errorMessage && (
                <div className="bg-red-900/30 border border-red-500 rounded-lg p-3 mb-4 text-center">
                  <span className="text-red-300">⚠️ {errorMessage}</span>
                </div>
              )}

              {/* Control Buttons - State-based */}
              <div className="space-y-3">
                {/* IDLE: Show card swipe button */}
                {pumpStatus?.state === 'IDLE' && !pumpStatus?.hasPendingTransaction && (
                  <div className="space-y-2">
                    <div className="flex gap-2">
                      <input
                        type="number"
                        value={maxAmount}
                        onChange={(e) => setMaxAmount(Number(e.target.value))}
                        className="flex-1 bg-gray-700 rounded-lg px-3 py-2 text-white"
                        placeholder="Maks beløp"
                      />
                      <span className="self-center text-gray-400">kr</span>
                    </div>
                    <button
                      onClick={() => cardSwipeMutation.mutate()}
                      disabled={cardSwipeMutation.isPending}
                      className="w-full py-4 rounded-xl font-bold text-xl bg-blue-600 hover:bg-blue-700 transition-colors"
                    >
                      {cardSwipeMutation.isPending ? '...' : '💳 SIMULER KORTDRAGNING'}
                    </button>
                  </div>
                )}

                {/* AUTHORIZED_WAITING: Card swiped, waiting for FRI DISPENSER */}
                {pumpStatus?.state === 'AUTHORIZED_WAITING' && (
                  <div className="space-y-3">
                    <div className="text-center py-3">
                      <div className="text-4xl mb-2">💳</div>
                      <p className="text-yellow-400 font-bold text-lg">Kort registrert!</p>
                      <p className="text-gray-400 text-sm mt-2">Reservert beløp: {maxAmount} kr</p>
                      <p className="text-gray-400 text-sm">Trykk FRI DISPENSER for å starte</p>
                      {countdown !== null && countdown > 0 && (
                        <div className="mt-3">
                          <div className="text-3xl font-bold text-yellow-400">{countdown}s</div>
                          <p className="text-xs text-gray-400 mt-1">Tid igjen før autorisasjon utløper</p>
                        </div>
                      )}
                      {countdown === 0 && (
                        <div className="mt-3 text-red-400 font-bold">
                          ⏰ Timeout - autorisasjon kansellert
                        </div>
                      )}
                    </div>
                    <button
                      onClick={() => unblockMutation.mutate()}
                      disabled={unblockMutation.isPending || countdown === 0}
                      className={`w-full py-4 rounded-xl font-bold text-xl transition-colors ${
                        countdown === 0 
                          ? 'bg-gray-600 cursor-not-allowed' 
                          : 'bg-green-600 hover:bg-green-700'
                      }`}
                    >
                      {unblockMutation.isPending ? '...' : '🔓 FRI DISPENSER'}
                    </button>
                  </div>
                )}

                {/* READY_TO_PUMP: Pump unblocked, ready for pumping */}
                {pumpStatus?.state === 'READY_TO_PUMP' && (
                  <div className="space-y-3">
                    <div className="text-center py-3">
                      <div className="text-4xl mb-2">✅</div>
                      <p className="text-green-400 font-bold text-lg">Pumpe frigjort!</p>
                      <p className="text-gray-400 text-sm mt-2">Løft dysen og start fylling</p>
                      {countdown !== null && countdown > 0 && (
                        <div className="mt-3">
                          <div className="text-3xl font-bold text-yellow-400">{countdown}s</div>
                          <p className="text-xs text-gray-400 mt-1">Tid igjen til automatisk BLOCK</p>
                        </div>
                      )}
                    </div>
                    <button
                      onClick={() => startPumpingMutation.mutate()}
                      disabled={startPumpingMutation.isPending || countdown === 0}
                      className={`w-full py-4 rounded-xl font-bold text-xl transition-colors ${
                        countdown === 0 
                          ? 'bg-gray-600 cursor-not-allowed' 
                          : 'bg-blue-600 hover:bg-blue-700'
                      }`}
                    >
                      {startPumpingMutation.isPending ? '...' : '⛽ START PUMPING (simuler)'}
                    </button>
                  </div>
                )}

                {/* PUMPING: Show stop button */}
                {pumpStatus?.state === 'PUMPING' && (
                  <button
                    onClick={() => blockMutation.mutate()}
                    disabled={blockMutation.isPending}
                    className="w-full py-4 rounded-xl font-bold text-xl bg-red-600 hover:bg-red-700 transition-colors"
                  >
                    {blockMutation.isPending ? '🛑 Stopper...' : '🛑 STOPP'}
                  </button>
                )}

                {/* PAYMENT_PENDING: Show confirm payment button */}
                {pumpStatus?.state === 'PAYMENT_PENDING' && pumpStatus?.hasPendingTransaction && (
                  <button
                    onClick={() => confirmPaymentMutation.mutate()}
                    disabled={confirmPaymentMutation.isPending}
                    className="w-full py-4 rounded-xl font-bold text-xl bg-yellow-600 hover:bg-yellow-700 transition-colors"
                  >
                    {confirmPaymentMutation.isPending ? '💳 Behandler...' : '💳 BEKREFT BETALING'}
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
            <div className="flex gap-2 mb-4 flex-wrap">
              {(['all', 'api', 'service', 'emulator', 'protocol'] as const).map(channel => (
                <button
                  key={channel}
                  onClick={() => setActiveChannel(channel)}
                  className={`px-3 py-2 rounded-lg font-medium transition-colors text-sm ${
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
          <h3 className="text-xl font-bold mb-4">📖 Bruksanvisning</h3>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-gray-300">
            <div>
              <h4 className="font-bold text-blue-400 mb-2">1️⃣ Kortdragning</h4>
              <p className="text-sm">
                Trykk "SIMULER KORTDRAGNING" for å reservere beløp (f.eks. 1500 kr).
              </p>
            </div>
            <div>
              <h4 className="font-bold text-yellow-400 mb-2">2️⃣ Fri dispenser</h4>
              <p className="text-sm">
                Trykk "FRI DISPENSER" innen 60s for å starte pumping.
              </p>
            </div>
            <div>
              <h4 className="font-bold text-green-400 mb-2">3️⃣ Pumping</h4>
              <p className="text-sm">
                Trykk "STOPP" når ønsket volum er fylt. Faktisk beløp (f.eks. 700 kr) trekkes.
              </p>
            </div>
            <div>
              <h4 className="font-bold text-orange-400 mb-2">4️⃣ Betaling</h4>
              <p className="text-sm">
                Trykk "BEKREFT BETALING" for å fullføre transaksjonen.
              </p>
            </div>
          </div>
          <div className="mt-4 p-3 bg-gray-700/50 rounded-lg">
            <p className="text-sm text-gray-400">
              <strong>💡 Tips:</strong> Du har 60 sekunder på deg etter kortdragning før pumpen automatisk blokkeres. Kun faktisk fylt volum trekkes fra kortet.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
