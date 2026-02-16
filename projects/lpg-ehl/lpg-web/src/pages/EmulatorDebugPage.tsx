import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getEmulatorStatus, setEmulatorScenario, resetEmulator } from '../api/emulator';
import type { EmulatorScenario } from '../api/emulator';

export function EmulatorDebugPage() {
  const [dispenserAddress, setDispenserAddress] = useState(1);
  const queryClient = useQueryClient();

  const { data: status, isLoading } = useQuery({
    queryKey: ['emulator-status', dispenserAddress],
    queryFn: () => getEmulatorStatus(dispenserAddress),
    refetchInterval: 2000,
  });

  const scenarioMutation = useMutation({
    mutationFn: ({ address, scenario }: { address: number; scenario: EmulatorScenario }) =>
      setEmulatorScenario(address, scenario),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emulator-status', dispenserAddress] });
    },
  });

  const resetMutation = useMutation({
    mutationFn: (address: number) => resetEmulator(address),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emulator-status', dispenserAddress] });
    },
  });

  const handleScenarioChange = (scenario: EmulatorScenario) => {
    scenarioMutation.mutate({ address: dispenserAddress, scenario });
  };

  const handleReset = () => {
    resetMutation.mutate(dispenserAddress);
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Emulator Debug</h1>
        <p className="text-slate-600">Kontroller emulator-scenarier for testing av feilsituasjoner</p>
      </div>

      {/* Dispenser Address Selector */}
      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <label className="block text-sm font-medium text-slate-700 mb-2">
          Dispenser Adresse
        </label>
        <input
          type="number"
          value={dispenserAddress}
          onChange={(e) => setDispenserAddress(parseInt(e.target.value) || 1)}
          min="1"
          max="10"
          className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
        />
      </div>

      {/* Scenario Selector */}
      <div className="bg-white rounded-xl shadow p-6 mb-6">
        <h2 className="text-xl font-bold text-slate-900 mb-4">Velg Scenario</h2>
        <div className="grid grid-cols-2 gap-4">
          <button
            onClick={() => handleScenarioChange('NORMAL')}
            disabled={scenarioMutation.isPending}
            className={`p-4 rounded-lg border-2 transition ${
              status?.scenario === 'NORMAL'
                ? 'border-green-500 bg-green-50 text-green-700'
                : 'border-slate-200 hover:border-green-300'
            }`}
          >
            <div className="text-2xl mb-2">✅</div>
            <div className="font-bold">NORMAL</div>
            <div className="text-sm text-slate-600">Vanlig respons</div>
          </button>

          <button
            onClick={() => handleScenarioChange('TIMEOUT')}
            disabled={scenarioMutation.isPending}
            className={`p-4 rounded-lg border-2 transition ${
              status?.scenario === 'TIMEOUT'
                ? 'border-yellow-500 bg-yellow-50 text-yellow-700'
                : 'border-slate-200 hover:border-yellow-300'
            }`}
          >
            <div className="text-2xl mb-2">⏱️</div>
            <div className="font-bold">TIMEOUT</div>
            <div className="text-sm text-slate-600">Ingen respons</div>
          </button>

          <button
            onClick={() => handleScenarioChange('CHECKSUM_ERROR')}
            disabled={scenarioMutation.isPending}
            className={`p-4 rounded-lg border-2 transition ${
              status?.scenario === 'CHECKSUM_ERROR'
                ? 'border-red-500 bg-red-50 text-red-700'
                : 'border-slate-200 hover:border-red-300'
            }`}
          >
            <div className="text-2xl mb-2">❌</div>
            <div className="font-bold">CHECKSUM_ERROR</div>
            <div className="text-sm text-slate-600">Ugyldig checksum</div>
          </button>

          <button
            onClick={() => handleScenarioChange('NO_CONNECTION')}
            disabled={scenarioMutation.isPending}
            className={`p-4 rounded-lg border-2 transition ${
              status?.scenario === 'NO_CONNECTION'
                ? 'border-gray-500 bg-gray-50 text-gray-700'
                : 'border-slate-200 hover:border-gray-300'
            }`}
          >
            <div className="text-2xl mb-2">🔌</div>
            <div className="font-bold">NO_CONNECTION</div>
            <div className="text-sm text-slate-600">Koblingsfeil</div>
          </button>
        </div>

        <button
          onClick={handleReset}
          disabled={resetMutation.isPending}
          className="w-full mt-4 px-4 py-2 bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition disabled:opacity-50"
        >
          {resetMutation.isPending ? 'Nullstiller...' : '↻ Nullstill Emulator'}
        </button>
      </div>

      {/* Status Display */}
      {!isLoading && status && (
        <div className="bg-white rounded-xl shadow p-6">
          <h2 className="text-xl font-bold text-slate-900 mb-4">Status</h2>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-slate-600">Scenario:</span>
              <span className="font-mono font-bold text-green-600">{status.scenario}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-600">Tilkoblet:</span>
              <span className={`font-bold ${status.connected ? 'text-green-600' : 'text-red-600'}`}>
                {status.connected ? '● Online' : '● Offline'}
              </span>
            </div>
            {status.lastMessage && (
              <div className="pt-3 border-t">
                <div className="text-sm text-slate-600 mb-1">Siste melding:</div>
                <div className="font-mono text-xs bg-slate-50 p-3 rounded overflow-x-auto">
                  {status.lastMessage}
                </div>
              </div>
            )}
            {status.lastError && (
              <div className="pt-3 border-t">
                <div className="text-sm text-red-600 mb-1">Siste feil:</div>
                <div className="font-mono text-xs bg-red-50 p-3 rounded overflow-x-auto text-red-700">
                  {status.lastError}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
