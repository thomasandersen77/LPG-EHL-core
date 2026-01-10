import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';

// API configuration
const API_BASE_URL = import.meta.env.VITE_API_URL || 
  (import.meta.env.PROD ? `${window.location.origin}/api/v1` : 'http://localhost:8080/api/v1');

// Types
interface ValidationCheck {
  name: string;
  ok: boolean;
  details: string;
}

interface WireTrace {
  txHex: string;
  rxHex: string;
  txBytes: number[];
  rxBytes: number[];
}

interface ValidationResult {
  checks: ValidationCheck[];
  vb6Compliant: boolean;
}

interface WireTraceResult {
  ok: boolean;
  command: string;
  parsed: Record<string, any>;
  wire: WireTrace;
  validation: ValidationResult;
}

interface SequenceResult {
  allPassed: boolean;
  failedAt: string | null;
  testsRun: number;
  totalTests: number;
  results: WireTraceResult[];
}

// API-funksjoner
const protocolApi = {
  testLinetest: async (address: number): Promise<WireTraceResult> => {
    const res = await fetch(`${API_BASE_URL}/protocol/test/linetest/${address}`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },
  testVolume: async (address: number): Promise<WireTraceResult> => {
    const res = await fetch(`${API_BASE_URL}/protocol/test/volume/${address}`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },
  testPrice: async (address: number): Promise<WireTraceResult> => {
    const res = await fetch(`${API_BASE_URL}/protocol/test/price/${address}`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },
  testState: async (address: number): Promise<WireTraceResult> => {
    const res = await fetch(`${API_BASE_URL}/protocol/test/state/${address}`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  },
  testSequence: async (address: number): Promise<SequenceResult> => {
    const res = await fetch(`${API_BASE_URL}/protocol/test/sequence/${address}`, { method: 'POST' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }
};

interface TestResultDisplay {
  command: string;
  result: WireTraceResult | null;
  error: string | null;
  loading: boolean;
  expanded: boolean;
}

export function WireComplianceTester() {
  const [address, setAddress] = useState(1);
  const [backendOffline, setBackendOffline] = useState(false);
  const [testResults, setTestResults] = useState<TestResultDisplay[]>([]);
  const [sequenceResult, setSequenceResult] = useState<SequenceResult | null>(null);

  const updateTestResult = (command: string, update: Partial<TestResultDisplay>) => {
    setTestResults(prev => {
      const existing = prev.find(r => r.command === command);
      if (existing) {
        return prev.map(r => r.command === command ? { ...r, ...update } : r);
      } else {
        return [{ command, result: null, error: null, loading: false, expanded: false, ...update }, ...prev];
      }
    });
  };

  const toggleExpand = (command: string) => {
    setTestResults(prev => prev.map(r => 
      r.command === command ? { ...r, expanded: !r.expanded } : r
    ));
  };

  // Individual test mutations
  const runTest = async (testName: string, testFn: () => Promise<WireTraceResult>) => {
    setBackendOffline(false);
    updateTestResult(testName, { loading: true, error: null });
    try {
      const result = await testFn();
      updateTestResult(testName, { result, loading: false, expanded: true });
    } catch (err: any) {
      if (err.message?.includes('fetch') || err.message?.includes('NetworkError')) {
        setBackendOffline(true);
        updateTestResult(testName, { error: 'Backend offline', loading: false });
      } else {
        updateTestResult(testName, { error: err.message, loading: false });
      }
    }
  };

  const sequenceMutation = useMutation({
    mutationFn: () => protocolApi.testSequence(address),
    onSuccess: (data) => {
      setSequenceResult(data);
      setBackendOffline(false);
      // Oppdater individuelle resultater
      data.results.forEach(r => {
        updateTestResult(r.command, { result: r, loading: false, expanded: true });
      });
    },
    onError: (err: any) => {
      if (err.message?.includes('fetch')) {
        setBackendOffline(true);
      }
    }
  });

  // Count verified tests
  const verifiedCount = testResults.filter(r => r.result?.validation.vb6Compliant).length;
  const failedCount = testResults.filter(r => r.result && !r.result.validation.vb6Compliant).length;

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white p-4">
      <div className="max-w-6xl mx-auto">
        {/* Overskrift */}
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold mb-2">🔬 VB6 Protokolltester</h1>
          <p className="text-gray-400">Verifiser 1:1 wire-kompatibilitet med legacy VB6-protokoll</p>
        </div>

        {/* Ingen kontakt med server */}
        {backendOffline && (
          <div className="bg-yellow-900/50 border border-yellow-500 rounded-xl p-4 mb-6 text-center">
            <span className="text-yellow-300 font-bold">⚠️ Ingen kontakt med server</span>
            <p className="text-yellow-200 text-sm mt-1">Kan ikke koble til API. Start backend med: java -jar release/lpg-ehl-monolith.jar</p>
          </div>
        )}

        {/* Samsvarsrapport */}
        <div className="bg-gray-800 rounded-xl p-4 mb-6 border border-gray-700">
          <h2 className="text-lg font-semibold mb-3">Samsvarsrapport</h2>
          <div className="flex gap-6">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-green-500"></div>
              <span>Bestått: <strong className="text-green-400">{verifiedCount}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-red-500"></div>
              <span>Feilet: <strong className="text-red-400">{failedCount}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-gray-500"></div>
              <span>Ikke testet: <strong className="text-gray-400">{4 - verifiedCount - failedCount}</strong></span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Venstre: Konfigurasjon */}
          <div className="space-y-4">
            <div className="bg-gray-800 rounded-xl p-4 border border-gray-700">
              <h3 className="font-semibold mb-3">Innstillinger</h3>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm text-gray-400 mb-1">Dispenser-adresse</label>
                  <input
                    type="number"
                    value={address}
                    onChange={(e) => setAddress(Number(e.target.value))}
                    className="w-full bg-gray-700 border border-gray-600 rounded px-3 py-2 text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Testknapper */}
            <div className="bg-gray-800 rounded-xl p-4 border border-gray-700">
              <h3 className="font-semibold mb-3">Enkelt-tester</h3>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => runTest('LINETEST', () => protocolApi.testLinetest(address))}
                  className="bg-teal-600 hover:bg-teal-700 py-2 px-3 rounded text-sm"
                >
                  🔗 Linjetest
                </button>
                <button
                  onClick={() => runTest('STATE', () => protocolApi.testState(address))}
                  className="bg-blue-600 hover:bg-blue-700 py-2 px-3 rounded text-sm"
                >
                  📊 Tilstand
                </button>
                <button
                  onClick={() => runTest('VOLUME', () => protocolApi.testVolume(address))}
                  className="bg-yellow-600 hover:bg-yellow-700 py-2 px-3 rounded text-sm"
                >
                  ⛽ Volum
                </button>
                <button
                  onClick={() => runTest('PRICE', () => protocolApi.testPrice(address))}
                  className="bg-green-600 hover:bg-green-700 py-2 px-3 rounded text-sm"
                >
                  💰 Pris
                </button>
              </div>
            </div>

            {/* Sekvenssknapp */}
            <button
              onClick={() => sequenceMutation.mutate()}
              disabled={sequenceMutation.isPending}
              className="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 py-3 rounded-xl font-bold shadow-lg"
            >
              {sequenceMutation.isPending ? '⏳ Kjører...' : '🚀 Kjør komplett VB6-sekvens'}
            </button>

            {/* Sekvensresultat */}
            {sequenceResult && (
              <div className={`rounded-xl p-4 border ${sequenceResult.allPassed ? 'bg-green-900/30 border-green-500' : 'bg-red-900/30 border-red-500'}`}>
                <div className="font-bold mb-2">
                  {sequenceResult.allPassed ? '✅ Alle tester bestått!' : `❌ Feilet ved ${sequenceResult.failedAt}`}
                </div>
                <div className="text-sm text-gray-300">
                  {sequenceResult.testsRun} av {sequenceResult.totalTests} tester kjørt
                </div>
              </div>
            )}
          </div>

          {/* Høyre: Datapakke-sporing */}
          <div className="lg:col-span-2 space-y-3">
            <h3 className="font-semibold">Datapakke-sporing (Hex)</h3>
            
            {testResults.length === 0 ? (
              <div className="bg-gray-800 rounded-xl p-6 text-center text-gray-400 border border-gray-700">
                Kjør en test for å se datapakke-sporing
              </div>
            ) : (
              testResults.map((tr) => (
                <WireTraceCard 
                  key={tr.command} 
                  testResult={tr} 
                  onToggle={() => toggleExpand(tr.command)}
                />
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// Datapakke-kort komponent
function WireTraceCard({ testResult, onToggle }: { testResult: TestResultDisplay; onToggle: () => void }) {
  const { command, result, error, loading, expanded } = testResult;
  
  if (loading) {
    return (
      <div className="bg-gray-800 rounded-xl p-4 border border-gray-700">
        <div className="flex items-center gap-2">
          <div className="animate-spin w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full"></div>
          <span className="font-mono">{command}</span>
          <span className="text-gray-400 text-sm">Kjører...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-gray-800 rounded-xl p-4 border border-red-500/50">
        <div className="flex items-center gap-2">
          <span className="text-red-400">⚠️</span>
          <span className="font-mono">{command}</span>
          <span className="text-red-300 text-sm">{error}</span>
        </div>
      </div>
    );
  }

  if (!result) return null;

  const isCompliant = result.validation.vb6Compliant;

  return (
    <div className={`bg-gray-800 rounded-xl border ${isCompliant ? 'border-green-500/50' : 'border-red-500/50'}`}>
      {/* Overskrift - klikkbar */}
      <div 
        className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-750"
        onClick={onToggle}
      >
        <div className="flex items-center gap-3">
          <span className={`text-xl ${isCompliant ? 'text-green-400' : 'text-red-400'}`}>
            {isCompliant ? '✅' : '❌'}
          </span>
          <span className="font-mono font-bold">{command}</span>
          <span className={`text-sm px-2 py-0.5 rounded ${isCompliant ? 'bg-green-900/50 text-green-300' : 'bg-red-900/50 text-red-300'}`}>
            {isCompliant ? 'VB6 OK' : 'VB6 FEIL'}
          </span>
        </div>
        <span className="text-gray-400">{expanded ? '▼' : '▶'}</span>
      </div>

      {/* Utvidet innhold */}
      {expanded && (
        <div className="border-t border-gray-700 p-4 space-y-4">
          {/* Hex-visning */}
          <div>
            <h4 className="text-sm font-semibold text-gray-400 mb-2">Hex-visning</h4>
            <div className="bg-gray-900 rounded p-3 font-mono text-sm space-y-2">
              <div>
                <span className="text-blue-400">📤 Sendt:</span>{' '}
                <span className="text-green-300">{result.wire.txHex}</span>
              </div>
              <div>
                <span className="text-purple-400">📥 Mottatt:</span>{' '}
                <span className="text-yellow-300">{result.wire.rxHex}</span>
              </div>
            </div>
          </div>

          {/* Valideringssjekkliste */}
          <div>
            <h4 className="text-sm font-semibold text-gray-400 mb-2">Valideringssjekkliste</h4>
            <div className="space-y-1">
              {result.validation.checks.map((check, i) => (
                <div key={i} className="flex items-start gap-2 text-sm">
                  <span className={check.ok ? 'text-green-400' : 'text-red-400'}>
                    {check.ok ? '✓' : '✗'}
                  </span>
                  <div>
                    <span className="font-medium">{check.name}:</span>{' '}
                    <span className="text-gray-400">{check.details}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Tolket data */}
          {Object.keys(result.parsed).length > 0 && (
            <div>
              <h4 className="text-sm font-semibold text-gray-400 mb-2">Tolket data</h4>
              <pre className="bg-gray-900 rounded p-3 text-sm text-gray-300 overflow-x-auto">
                {JSON.stringify(result.parsed, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
